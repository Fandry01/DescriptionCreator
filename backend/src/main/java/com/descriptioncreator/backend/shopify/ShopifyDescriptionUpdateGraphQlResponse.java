package com.descriptioncreator.backend.shopify;

import java.util.List;

record ShopifyDescriptionUpdateGraphQlResponse(
        Data data,
        List<GraphQlError> errors
) {

    record Data(ProductUpdate productUpdate) {
    }

    record ProductUpdate(Product product, List<UserError> userErrors) {
    }

    record Product(String id) {
    }

    record UserError(List<String> field, String message) {
    }

    record GraphQlError(String message) {
    }
}
