package com.descriptioncreator.backend.product;

import com.descriptioncreator.backend.shopify.ShopifyProductMetafieldsDto;

import java.util.List;

public record ProductMetafieldsResponse(
        String id,
        String title,
        String handle,
        List<MetafieldResponse> metafields
) {

    static ProductMetafieldsResponse from(ShopifyProductMetafieldsDto product) {
        return new ProductMetafieldsResponse(
                product.id(),
                product.title(),
                product.handle(),
                product.metafields().stream().map(MetafieldResponse::from).toList()
        );
    }

    public record MetafieldResponse(
            String namespace,
            String key,
            String value,
            String type
    ) {

        static MetafieldResponse from(ShopifyProductMetafieldsDto.Metafield metafield) {
            return new MetafieldResponse(
                    metafield.namespace(),
                    metafield.key(),
                    metafield.value(),
                    metafield.type()
            );
        }
    }
}
