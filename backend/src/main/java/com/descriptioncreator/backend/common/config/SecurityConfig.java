package com.descriptioncreator.backend.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(csrf -> csrf.ignoringRequestMatchers(
                        "/api/products/handle/*/generate-description",
                        "/api/products/handle/*/publish-description"
                ))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/api/shopify/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/products/**").permitAll()
                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/products/handle/*/generate-description",
                                "/api/products/handle/*/publish-description"
                        ).permitAll()
                        .anyRequest().authenticated()
                )
                .build();
    }
}
