package com.descriptioncreator.backend.shopify;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Locale;
import java.util.Optional;

@Component
public class ShopifyTokenStore {

    private final ShopifyConnectionRepository repository;
    private final ShopifyProperties properties;

    public ShopifyTokenStore(
            ShopifyConnectionRepository repository,
            ShopifyProperties properties
    ) {
        this.repository = repository;
        this.properties = properties;
    }

    @Transactional
    public void store(String shop, String accessToken) {
        String normalizedShop = normalizeShop(shop);
        Instant now = Instant.now();
        ShopifyConnectionEntity connection = repository.findByShopDomain(normalizedShop)
                .map(existing -> {
                    existing.updateAccessToken(accessToken, now);
                    return existing;
                })
                .orElseGet(() -> new ShopifyConnectionEntity(
                        normalizedShop,
                        accessToken,
                        now
                ));
        repository.save(connection);
    }

    @Transactional(readOnly = true)
    public Optional<String> accessToken() {
        return repository.findByShopDomain(normalizeShop(properties.shopDomain()))
                .map(ShopifyConnectionEntity::getAccessToken)
                .filter(token -> !token.isBlank());
    }

    @Transactional(readOnly = true)
    public Status status(String configuredShop) {
        String normalizedShop = normalizeShop(configuredShop);
        boolean connected = repository.findByShopDomain(normalizedShop)
                .map(ShopifyConnectionEntity::getAccessToken)
                .filter(token -> !token.isBlank())
                .isPresent();
        return new Status(connected, normalizedShop);
    }

    private String normalizeShop(String shop) {
        return shop == null ? "" : shop.trim().toLowerCase(Locale.ROOT);
    }

    public record Status(boolean connected, String shop) {
    }
}
