package com.descriptioncreator.backend.shopify;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.UriComponentsBuilder;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.HexFormat;
import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class ShopifyOAuthServiceTests {

    private static final String CLIENT_SECRET = "client-secret";

    @Test
    void createsAuthorizationUrlAndExchangesValidCallback() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        ShopifyTokenStore tokenStore = new ShopifyTokenStore();
        ShopifyOAuthService service = service(builder.build(), tokenStore);

        String authorizationUrl = service.createAuthorizationUrl();
        MultiValueMap<String, String> authorizationParameters = UriComponentsBuilder
                .fromUriString(authorizationUrl)
                .build()
                .getQueryParams();

        assertThat(authorizationUrl).startsWith(
                "https://example.myshopify.com/admin/oauth/authorize?"
        );
        assertThat(authorizationParameters.getFirst("client_id")).isEqualTo("client-id");
        assertThat(authorizationParameters.getFirst("scope")).isEqualTo("read_products");

        MultiValueMap<String, String> callback = callbackParameters(
                authorizationParameters.getFirst("state"),
                "example.myshopify.com"
        );

        server.expect(requestTo("https://example.myshopify.com/admin/oauth/access_token"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().string(containsString("client_id=client-id")))
                .andExpect(content().string(containsString("client_secret=client-secret")))
                .andExpect(content().string(containsString("code=authorization-code")))
                .andRespond(withSuccess("""
                        {
                          "access_token": "shpat_test_token",
                          "scope": "read_products"
                        }
                        """, MediaType.APPLICATION_JSON));

        service.completeAuthorization(callback);

        assertThat(service.status().connected()).isTrue();
        assertThat(service.status().shop()).isEqualTo("example.myshopify.com");
        assertThat(tokenStore.accessToken()).contains("shpat_test_token");
        server.verify();
    }

    @Test
    void rejectsReusedState() {
        ShopifyOAuthService service = service(RestClient.create(), new ShopifyTokenStore());
        String state = UriComponentsBuilder.fromUriString(service.createAuthorizationUrl())
                .build()
                .getQueryParams()
                .getFirst("state");
        MultiValueMap<String, String> callback = callbackParameters(state, "example.myshopify.com");
        callback.set("hmac", "0".repeat(64));

        assertThatThrownBy(() -> service.completeAuthorization(callback))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        exception -> assertThat(exception.getReason()).isEqualTo("Invalid Shopify HMAC"));

        assertThatThrownBy(() -> service.completeAuthorization(callback))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        exception -> {
                            assertThat(exception.getStatusCode()).isEqualTo(FORBIDDEN);
                            assertThat(exception.getReason()).isEqualTo("Invalid or expired OAuth state");
                        });
    }

    @Test
    void rejectsUnexpectedShopBeforeTokenExchange() {
        ShopifyOAuthService service = service(RestClient.create(), new ShopifyTokenStore());
        String state = UriComponentsBuilder.fromUriString(service.createAuthorizationUrl())
                .build()
                .getQueryParams()
                .getFirst("state");

        assertThatThrownBy(() -> service.completeAuthorization(
                callbackParameters(state, "attacker.myshopify.com")
        )).isInstanceOfSatisfying(ResponseStatusException.class,
                exception -> assertThat(exception.getStatusCode()).isEqualTo(FORBIDDEN));
    }

    private ShopifyOAuthService service(RestClient restClient, ShopifyTokenStore tokenStore) {
        ShopifyProperties properties = new ShopifyProperties(
                "example.myshopify.com",
                "client-id",
                CLIENT_SECRET,
                "2026-07",
                "http://localhost:8080/api/shopify/callback"
        );
        return new ShopifyOAuthService(properties, tokenStore, restClient, new SecureRandom());
    }

    private MultiValueMap<String, String> callbackParameters(String state, String shop) {
        MultiValueMap<String, String> parameters = new LinkedMultiValueMap<>();
        parameters.add("code", "authorization-code");
        parameters.add("shop", shop);
        parameters.add("state", state);
        parameters.add("timestamp", "1789898400");
        parameters.add("hmac", hmac(parameters));
        return parameters;
    }

    private String hmac(MultiValueMap<String, String> parameters) {
        String message = parameters.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> entry.getKey() + "=" + String.join(",", entry.getValue()))
                .collect(Collectors.joining("&"));
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(CLIENT_SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(message.getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException(exception);
        }
    }
}
