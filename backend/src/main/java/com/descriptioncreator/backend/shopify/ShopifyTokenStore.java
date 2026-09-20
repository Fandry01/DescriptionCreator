package com.descriptioncreator.backend.shopify;

import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

@Component
public class ShopifyTokenStore {

    private final AtomicReference<Connection> connection = new AtomicReference<>();

    public void store(String shop, String accessToken) {
        connection.set(new Connection(shop, accessToken));
    }

    public Optional<String> accessToken() {
        return Optional.ofNullable(connection.get()).map(Connection::accessToken);
    }

    public Status status(String configuredShop) {
        Connection current = connection.get();
        return new Status(current != null, current == null ? configuredShop : current.shop());
    }

    private record Connection(String shop, String accessToken) {
    }

    public record Status(boolean connected, String shop) {
    }
}
