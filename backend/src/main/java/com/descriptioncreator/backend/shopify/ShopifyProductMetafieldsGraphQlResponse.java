package com.descriptioncreator.backend.shopify;

import java.util.List;

public record ShopifyProductMetafieldsGraphQlResponse(
        Data data,
        List<GraphQlError> errors
) {

    public record Data(Product product) {
    }

    public record Product(
            String id,
            String title,
            String handle,
            Metafields metafields
    ) {
    }

    public record Metafields(
            List<ShopifyProductMetafieldsDto.Metafield> nodes,
            PageInfo pageInfo
    ) {
    }

    public record PageInfo(boolean hasNextPage, String endCursor) {
    }

    public record GraphQlError(String message) {
    }
}
