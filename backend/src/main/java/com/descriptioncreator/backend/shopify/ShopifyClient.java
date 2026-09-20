package com.descriptioncreator.backend.shopify;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@Component
public class ShopifyClient {

    static final String PRODUCTS_QUERY = """
            query Products {
              products(first: 50, sortKey: CREATED_AT, reverse: true) {
                nodes {
                  id
                  title
                  handle
                  descriptionHtml
                  createdAt
                  updatedAt
                }
              }
            }
            """;

    private final RestClient restClient;
    private final ShopifyTokenStore tokenStore;

    public ShopifyClient(
            @Qualifier("shopifyRestClient") RestClient restClient,
            ShopifyTokenStore tokenStore
    ) {
        this.restClient = restClient;
        this.tokenStore = tokenStore;
    }

    public List<ShopifyProductDto> fetchProducts() {
        ShopifyGraphQlResponse response = restClient.post()
                .header("X-Shopify-Access-Token", tokenStore.accessToken()
                        .orElseThrow(() -> new IllegalStateException("Shopify is not connected")))
                .body(Map.of("query", PRODUCTS_QUERY))
                .retrieve()
                .body(ShopifyGraphQlResponse.class);

        if (response == null) {
            throw new IllegalStateException("Shopify returned an empty response");
        }
        if (response.errors() != null && !response.errors().isEmpty()) {
            String messages = response.errors().stream()
                    .map(ShopifyGraphQlResponse.GraphQlError::message)
                    .reduce((first, second) -> first + "; " + second)
                    .orElse("Unknown GraphQL error");
            throw new IllegalStateException("Shopify GraphQL request failed: " + messages);
        }
        if (response.data() == null || response.data().products() == null
                || response.data().products().nodes() == null) {
            throw new IllegalStateException("Shopify response did not contain products");
        }

        return response.data().products().nodes();
    }
}
