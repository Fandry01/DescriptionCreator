package com.descriptioncreator.backend.shopify;

import java.util.List;

public record ShopifyProductMetafieldsDto(
        String id,
        String title,
        String handle,
        String vendor,
        List<Metafield> metafields
) {

    public record Metafield(
            String namespace,
            String key,
            String value,
            String type
    ) {
    }
}
