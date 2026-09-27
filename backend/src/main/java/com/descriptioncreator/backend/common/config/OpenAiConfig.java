package com.descriptioncreator.backend.common.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties(OpenAiProperties.class)
public class OpenAiConfig {

    @Bean
    @Qualifier("openAiRestClient")
    public RestClient openAiRestClient(OpenAiProperties properties) {
        return buildOpenAiRestClient(RestClient.builder(), properties);
    }

    public RestClient buildOpenAiRestClient(
            RestClient.Builder builder,
            OpenAiProperties properties
    ) {
        builder.baseUrl("https://api.openai.com/v1");
        if (properties.apiKey() != null && !properties.apiKey().isBlank()) {
            builder.defaultHeader(
                    HttpHeaders.AUTHORIZATION,
                    "Bearer " + properties.apiKey().trim()
            );
        }
        return builder.build();
    }
}
