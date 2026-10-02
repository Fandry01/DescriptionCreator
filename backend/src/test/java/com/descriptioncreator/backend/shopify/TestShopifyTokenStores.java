package com.descriptioncreator.backend.shopify;

import java.lang.reflect.Proxy;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public final class TestShopifyTokenStores {

    private TestShopifyTokenStores() {
    }

    public static ShopifyTokenStore create() {
        RepositoryState state = new RepositoryState();
        return new ShopifyTokenStore(state.repository(), properties());
    }

    static ShopifyProperties properties() {
        return new ShopifyProperties(
                "example.myshopify.com",
                "client-id",
                "client-secret",
                "2026-07",
                "http://localhost:8080/api/shopify/callback"
        );
    }

    static final class RepositoryState {

        private final Map<String, ShopifyConnectionEntity> connections = new LinkedHashMap<>();
        private final ShopifyConnectionRepository repository =
                (ShopifyConnectionRepository) Proxy.newProxyInstance(
                        ShopifyConnectionRepository.class.getClassLoader(),
                        new Class<?>[]{ShopifyConnectionRepository.class},
                        (proxy, method, arguments) -> switch (method.getName()) {
                            case "findByShopDomain" -> Optional.ofNullable(
                                    connections.get((String) arguments[0])
                            );
                            case "save" -> {
                                ShopifyConnectionEntity connection =
                                        (ShopifyConnectionEntity) arguments[0];
                                connections.put(connection.getShopDomain(), connection);
                                yield connection;
                            }
                            case "count" -> (long) connections.size();
                            case "deleteAll" -> {
                                connections.clear();
                                yield null;
                            }
                            case "toString" -> "TestShopifyConnectionRepository";
                            default -> throw new UnsupportedOperationException(
                                    "Unsupported repository method: " + method.getName()
                            );
                        }
                );

        ShopifyConnectionRepository repository() {
            return repository;
        }
    }
}
