package com.descriptioncreator.backend.product;

import com.descriptioncreator.backend.shopify.ShopifyProductDto;

import java.time.OffsetDateTime;

public record ProductResponse(
        String id,
        String title,
        String handle,
        String descriptionHtml,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {

    static ProductResponse from(ShopifyProductDto product) {
        return new ProductResponse(
                product.id(),
                product.title(),
                product.handle(),
                product.descriptionHtml(),
                product.createdAt(),
                product.updatedAt()
        );
    }
}
