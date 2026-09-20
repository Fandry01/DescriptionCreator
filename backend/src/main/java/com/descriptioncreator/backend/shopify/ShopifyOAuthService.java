package com.descriptioncreator.backend.shopify;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.UriComponentsBuilder;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.springframework.http.HttpStatus.BAD_GATEWAY;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.FORBIDDEN;

@Service
public class ShopifyOAuthService {

    private static final String REQUIRED_SCOPE = "read_products";
    private static final Duration STATE_LIFETIME = Duration.ofMinutes(10);
    private static final Pattern SHOP_DOMAIN = Pattern.compile(
            "^[a-zA-Z0-9][a-zA-Z0-9-]*\\.myshopify\\.com$"
    );

    private final ShopifyProperties properties;
    private final ShopifyTokenStore tokenStore;
    private final RestClient restClient;
    private final SecureRandom secureRandom;
    private final Map<String, Instant> pendingStates = new ConcurrentHashMap<>();

    @Autowired
    public ShopifyOAuthService(
            ShopifyProperties properties,
            ShopifyTokenStore tokenStore,
            @Qualifier("shopifyOAuthRestClient") RestClient restClient
    ) {
        this(properties, tokenStore, restClient, new SecureRandom());
    }

    ShopifyOAuthService(
            ShopifyProperties properties,
            ShopifyTokenStore tokenStore,
            RestClient restClient,
            SecureRandom secureRandom
    ) {
        this.properties = properties;
        this.tokenStore = tokenStore;
        this.restClient = restClient;
        this.secureRandom = secureRandom;
    }

    public String createAuthorizationUrl() {
        validateConfiguration();
        pendingStates.entrySet().removeIf(entry -> entry.getValue().isBefore(Instant.now()));

        byte[] randomBytes = new byte[32];
        secureRandom.nextBytes(randomBytes);
        String state = Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
        pendingStates.put(state, Instant.now().plus(STATE_LIFETIME));

        return UriComponentsBuilder.fromUriString(properties.authorizationUrl())
                .queryParam("client_id", properties.clientId())
                .queryParam("scope", REQUIRED_SCOPE)
                .queryParam("redirect_uri", properties.redirectUri())
                .queryParam("state", state)
                .build()
                .encode()
                .toUriString();
    }

    public void completeAuthorization(MultiValueMap<String, String> queryParameters) {
        String state = requiredParameter(queryParameters, "state");
        String hmac = requiredParameter(queryParameters, "hmac");
        String shop = requiredParameter(queryParameters, "shop").toLowerCase(Locale.ROOT);
        String code = requiredParameter(queryParameters, "code");

        validateState(state);
        validateHmac(queryParameters, hmac);
        validateShop(shop);

        TokenResponse tokenResponse;
        try {
            tokenResponse = restClient.post()
                    .uri(properties.accessTokenUrl())
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .accept(MediaType.APPLICATION_JSON)
                    .body(tokenRequest(code))
                    .retrieve()
                    .body(TokenResponse.class);
        } catch (RuntimeException exception) {
            throw new ResponseStatusException(BAD_GATEWAY, "Shopify token exchange failed", exception);
        }

        if (tokenResponse == null || isBlank(tokenResponse.access_token())) {
            throw new ResponseStatusException(BAD_GATEWAY, "Shopify did not return an access token");
        }
        if (!hasRequiredScope(tokenResponse.scope())) {
            throw new ResponseStatusException(FORBIDDEN, "Shopify did not grant read_products");
        }

        tokenStore.store(shop, tokenResponse.access_token());
    }

    public ShopifyTokenStore.Status status() {
        return tokenStore.status(normalizedConfiguredShop());
    }

    private MultiValueMap<String, String> tokenRequest(String code) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("client_id", properties.clientId());
        form.add("client_secret", properties.clientSecret());
        form.add("code", code);
        return form;
    }

    private void validateState(String state) {
        Instant expiresAt = pendingStates.remove(state);
        if (expiresAt == null || expiresAt.isBefore(Instant.now())) {
            throw new ResponseStatusException(FORBIDDEN, "Invalid or expired OAuth state");
        }
    }

    private void validateHmac(MultiValueMap<String, String> parameters, String suppliedHmac) {
        String message = parameters.entrySet().stream()
                .filter(entry -> !entry.getKey().equals("hmac") && !entry.getKey().equals("signature"))
                .sorted(Map.Entry.comparingByKey(Comparator.naturalOrder()))
                .map(entry -> entry.getKey() + "=" + String.join(",", entry.getValue()))
                .collect(Collectors.joining("&"));

        String expectedHmac;
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(
                    properties.clientSecret().getBytes(StandardCharsets.UTF_8),
                    "HmacSHA256"
            ));
            expectedHmac = HexFormat.of().formatHex(mac.doFinal(message.getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("HMAC-SHA256 is unavailable", exception);
        }

        boolean valid = suppliedHmac.matches("(?i)^[0-9a-f]{64}$")
                && MessageDigest.isEqual(
                expectedHmac.getBytes(StandardCharsets.US_ASCII),
                suppliedHmac.toLowerCase(Locale.ROOT).getBytes(StandardCharsets.US_ASCII)
        );
        if (!valid) {
            throw new ResponseStatusException(FORBIDDEN, "Invalid Shopify HMAC");
        }
    }

    private void validateShop(String shop) {
        if (!SHOP_DOMAIN.matcher(shop).matches()) {
            throw new ResponseStatusException(BAD_REQUEST, "Invalid Shopify shop domain");
        }
        if (!MessageDigest.isEqual(
                normalizedConfiguredShop().getBytes(StandardCharsets.UTF_8),
                shop.getBytes(StandardCharsets.UTF_8)
        )) {
            throw new ResponseStatusException(FORBIDDEN, "Unexpected Shopify shop");
        }
    }

    private void validateConfiguration() {
        if (!SHOP_DOMAIN.matcher(normalizedConfiguredShop()).matches()
                || isBlank(properties.clientId())
                || isBlank(properties.clientSecret())
                || isBlank(properties.redirectUri())) {
            throw new ResponseStatusException(
                    BAD_REQUEST,
                    "Shopify OAuth environment variables are not configured"
            );
        }
    }

    private String normalizedConfiguredShop() {
        return properties.shopDomain() == null
                ? ""
                : properties.shopDomain().trim().toLowerCase(Locale.ROOT);
    }

    private String requiredParameter(MultiValueMap<String, String> parameters, String name) {
        String value = parameters.getFirst(name);
        if (isBlank(value)) {
            throw new ResponseStatusException(BAD_REQUEST, "Missing OAuth parameter: " + name);
        }
        return value;
    }

    private boolean hasRequiredScope(String scopes) {
        if (isBlank(scopes)) {
            return false;
        }
        return List.of(scopes.split(",")).stream().map(String::trim)
                .anyMatch(REQUIRED_SCOPE::equals);
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    record TokenResponse(String access_token, String scope) {
    }
}
