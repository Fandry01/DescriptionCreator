package com.descriptioncreator.backend.shopify;

import java.util.List;

public record ShopifyGraphQlResponse(
        Data data,
        List<GraphQlError> errors
) {

    public record Data(Products products) {
    }

    public record Products(List<ShopifyProductDto> nodes) {
    }

    public record GraphQlError(String message) {
    }
}
