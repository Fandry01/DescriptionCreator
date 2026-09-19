package com.descriptioncreator.backend.shopify;

import java.time.OffsetDateTime;

public record ShopifyProductDto(
        String id,
        String title,
        String handle,
        String descriptionHtml,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
