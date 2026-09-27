package com.descriptioncreator.backend.shopify;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

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

    static final String PRODUCT_METAFIELDS_QUERY = """
            query ProductMetafields($identifier: ProductIdentifierInput!, $cursor: String) {
              product: productByIdentifier(identifier: $identifier) {
                id
                title
                handle
                metafields(first: 250, after: $cursor) {
                  nodes {
                    namespace
                    key
                    value
                    type
                  }
                  pageInfo {
                    hasNextPage
                    endCursor
                  }
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

    public Optional<ShopifyProductMetafieldsDto> fetchProductMetafieldsByHandle(String handle) {
        List<ShopifyProductMetafieldsDto.Metafield> metafields = new ArrayList<>();
        ShopifyProductMetafieldsGraphQlResponse.Product product = null;
        String cursor = null;

        do {
            Map<String, Object> variables = new LinkedHashMap<>();
            variables.put("identifier", Map.of("handle", handle));
            if (cursor != null) {
                variables.put("cursor", cursor);
            }

            ShopifyProductMetafieldsGraphQlResponse response = restClient.post()
                    .header("X-Shopify-Access-Token", accessToken())
                    .body(Map.of(
                            "query", PRODUCT_METAFIELDS_QUERY,
                            "variables", variables
                    ))
                    .retrieve()
                    .body(ShopifyProductMetafieldsGraphQlResponse.class);

            validateMetafieldsResponse(response);
            ShopifyProductMetafieldsGraphQlResponse.Product pageProduct = response.data().product();
            if (pageProduct == null) {
                return Optional.empty();
            }
            if (pageProduct.metafields() == null || pageProduct.metafields().nodes() == null
                    || pageProduct.metafields().pageInfo() == null) {
                throw new IllegalStateException("Shopify response did not contain product metafields");
            }

            if (product == null) {
                product = pageProduct;
            }
            metafields.addAll(pageProduct.metafields().nodes());

            ShopifyProductMetafieldsGraphQlResponse.PageInfo pageInfo = pageProduct.metafields().pageInfo();
            if (!pageInfo.hasNextPage()) {
                break;
            }
            if (pageInfo.endCursor() == null || pageInfo.endCursor().isBlank()) {
                throw new IllegalStateException("Shopify metafield pagination cursor is missing");
            }
            cursor = pageInfo.endCursor();
        } while (true);

        return Optional.of(new ShopifyProductMetafieldsDto(
                product.id(),
                product.title(),
                product.handle(),
                List.copyOf(metafields)
        ));
    }

    private String accessToken() {
        return tokenStore.accessToken()
                .orElseThrow(() -> new IllegalStateException("Shopify is not connected"));
    }

    private void validateMetafieldsResponse(ShopifyProductMetafieldsGraphQlResponse response) {
        if (response == null) {
            throw new IllegalStateException("Shopify returned an empty response");
        }
        if (response.errors() != null && !response.errors().isEmpty()) {
            String messages = response.errors().stream()
                    .map(ShopifyProductMetafieldsGraphQlResponse.GraphQlError::message)
                    .reduce((first, second) -> first + "; " + second)
                    .orElse("Unknown GraphQL error");
            throw new IllegalStateException("Shopify GraphQL request failed: " + messages);
        }
        if (response.data() == null) {
            throw new IllegalStateException("Shopify response did not contain data");
        }
    }
}
