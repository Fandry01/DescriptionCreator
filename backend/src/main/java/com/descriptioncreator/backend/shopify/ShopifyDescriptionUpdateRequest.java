package com.descriptioncreator.backend.shopify;

public record ShopifyDescriptionUpdateRequest(
        String id,
        String descriptionHtml
) {
}
