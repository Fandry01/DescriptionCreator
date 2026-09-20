package com.descriptioncreator.backend.common.config;

import com.descriptioncreator.backend.shopify.ShopifyProperties;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties(ShopifyProperties.class)
public class ShopifyConfig {

    @Bean
    @Qualifier("shopifyRestClient")
    public RestClient shopifyRestClient(ShopifyProperties properties) {
        return buildShopifyRestClient(RestClient.builder(), properties);
    }

    public RestClient buildShopifyRestClient(RestClient.Builder builder, ShopifyProperties properties) {
        return builder
                .baseUrl(properties.adminGraphqlUrl())
                .build();
    }

    @Bean
    @Qualifier("shopifyOAuthRestClient")
    public RestClient shopifyOAuthRestClient() {
        return RestClient.create();
    }
}
