package com.descriptioncreator.backend.shopify;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "shopify")
public record ShopifyProperties(
        String shopDomain,
        String clientId,
        String clientSecret,
        String apiVersion,
        String redirectUri
) {

    public String adminGraphqlUrl() {
        return "https://%s/admin/api/%s/graphql.json".formatted(shopDomain, apiVersion);
    }

    public String authorizationUrl() {
        return "https://%s/admin/oauth/authorize".formatted(shopDomain);
    }

    public String accessTokenUrl() {
        return "https://%s/admin/oauth/access_token".formatted(shopDomain);
    }
}
