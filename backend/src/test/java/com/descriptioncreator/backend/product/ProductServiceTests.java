package com.descriptioncreator.backend.product;

import com.descriptioncreator.backend.shopify.ShopifyClient;
import com.descriptioncreator.backend.shopify.ShopifyProductDto;
import com.descriptioncreator.backend.shopify.ShopifyTokenStore;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
class ProductServiceTests {

    @Test
    void returnsAllProductsByDefault() {
        ProductService productService = productServiceWith(List.of(
                product("1", "Description"),
                product("2", "")
        ));

        List<ProductResponse> products = productService.getProducts(false);

        assertThat(products).extracting(ProductResponse::id).containsExactly("1", "2");
    }

    @Test
    void filtersProductsWithMissingOrEmptyHtmlDescriptions() {
        ProductService productService = productServiceWith(List.of(
                product("1", null),
                product("2", "  "),
                product("3", "<p><br></p>"),
                product("4", "<div>&nbsp;</div>"),
                product("5", "<p>A useful description</p>")
        ));

        List<ProductResponse> products = productService.getProducts(true);

        assertThat(products).extracting(ProductResponse::id)
                .containsExactly("1", "2", "3", "4");
    }

    private ProductService productServiceWith(List<ShopifyProductDto> products) {
        ShopifyClient shopifyClient = new ShopifyClient(
                org.springframework.web.client.RestClient.create(),
                new ShopifyTokenStore()
        ) {
            @Override
            public List<ShopifyProductDto> fetchProducts() {
                return products;
            }
        };
        return new ProductService(shopifyClient);
    }

    private ShopifyProductDto product(String id, String descriptionHtml) {
        OffsetDateTime timestamp = OffsetDateTime.parse("2026-09-19T12:00:00Z");
        return new ShopifyProductDto(
                id,
                "Product " + id,
                "product-" + id,
                descriptionHtml,
                timestamp,
                timestamp
        );
    }
}
