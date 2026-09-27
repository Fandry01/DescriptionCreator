package com.descriptioncreator.backend.shopify;

import com.descriptioncreator.backend.common.config.ShopifyConfig;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class ShopifyClientTests {

    @Test
    void fetchesNewestProductsFromShopify() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        ShopifyProperties properties = new ShopifyProperties(
                "example.myshopify.com",
                "client-id",
                "client-secret",
                "2026-07",
                "http://localhost:8080/api/shopify/callback"
        );
        RestClient restClient = new ShopifyConfig().buildShopifyRestClient(builder, properties);
        ShopifyTokenStore tokenStore = new ShopifyTokenStore();
        tokenStore.store("example.myshopify.com", "test-token");
        ShopifyClient client = new ShopifyClient(restClient, tokenStore);

        server.expect(requestTo("https://example.myshopify.com/admin/api/2026-07/graphql.json"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("X-Shopify-Access-Token", "test-token"))
                .andExpect(content().string(containsString("products(first: 50, sortKey: CREATED_AT, reverse: true)")))
                .andRespond(withSuccess("""
                        {
                          "data": {
                            "products": {
                              "nodes": [
                                {
                                  "id": "gid://shopify/Product/1",
                                  "title": "Newest product",
                                  "handle": "newest-product",
                                  "descriptionHtml": "<p>Description</p>",
                                  "createdAt": "2026-09-19T12:00:00Z",
                                  "updatedAt": "2026-09-19T13:00:00Z"
                                }
                              ]
                            }
                          }
                        }
                        """, MediaType.APPLICATION_JSON));

        List<ShopifyProductDto> products = client.fetchProducts();

        assertThat(products).hasSize(1);
        assertThat(products.getFirst().title()).isEqualTo("Newest product");
        server.verify();
    }

    @Test
    void fetchesAllMetafieldPagesForProductHandle() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        ShopifyProperties properties = new ShopifyProperties(
                "example.myshopify.com",
                "client-id",
                "client-secret",
                "2026-07",
                "http://localhost:8080/api/shopify/callback"
        );
        ShopifyTokenStore tokenStore = new ShopifyTokenStore();
        tokenStore.store("example.myshopify.com", "test-token");
        ShopifyClient client = new ShopifyClient(
                new ShopifyConfig().buildShopifyRestClient(builder, properties),
                tokenStore
        );

        server.expect(requestTo("https://example.myshopify.com/admin/api/2026-07/graphql.json"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("X-Shopify-Access-Token", "test-token"))
                .andExpect(content().string(containsString("productByIdentifier")))
                .andExpect(content().string(containsString("\"handle\":\"test-product\"")))
                .andRespond(withSuccess(productMetafieldsPage(
                        "custom",
                        "material",
                        "Leather",
                        "single_line_text_field",
                        true,
                        "next-page"
                ), MediaType.APPLICATION_JSON));

        server.expect(requestTo("https://example.myshopify.com/admin/api/2026-07/graphql.json"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().string(containsString("\"cursor\":\"next-page\"")))
                .andRespond(withSuccess(productMetafieldsPage(
                        "seo",
                        "subtitle",
                        "A subtitle",
                        "single_line_text_field",
                        false,
                        null
                ), MediaType.APPLICATION_JSON));

        Optional<ShopifyProductMetafieldsDto> result =
                client.fetchProductMetafieldsByHandle("test-product");

        assertThat(result).isPresent();
        assertThat(result.orElseThrow().metafields())
                .extracting(ShopifyProductMetafieldsDto.Metafield::key)
                .containsExactly("material", "subtitle");
        server.verify();
    }

    private String productMetafieldsPage(
            String namespace,
            String key,
            String value,
            String type,
            boolean hasNextPage,
            String endCursor
    ) {
        String cursor = endCursor == null ? "null" : "\"" + endCursor + "\"";
        return """
                {
                  "data": {
                    "product": {
                      "id": "gid://shopify/Product/1",
                      "title": "Test product",
                      "handle": "test-product",
                      "metafields": {
                        "nodes": [
                          {
                            "namespace": "%s",
                            "key": "%s",
                            "value": "%s",
                            "type": "%s"
                          }
                        ],
                        "pageInfo": {
                          "hasNextPage": %s,
                          "endCursor": %s
                        }
                      }
                    }
                  }
                }
                """.formatted(namespace, key, value, type, hasNextPage, cursor);
    }
}
