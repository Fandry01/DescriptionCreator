package com.descriptioncreator.backend.shopify;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "shopify")
public record ShopifyProperties(
        String shopDomain,
        String accessToken,
        String apiVersion
) {

    public String adminGraphqlUrl() {
        return "https://%s/admin/api/%s/graphql.json".formatted(shopDomain, apiVersion);
    }
}
