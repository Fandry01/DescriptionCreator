package com.descriptioncreator.backend.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;

@Configuration
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        CookieCsrfTokenRepository csrf = CookieCsrfTokenRepository.withHttpOnlyFalse();
        return http
                .csrf(configurer -> configurer.csrfTokenRepository(csrf))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/error", "/api/auth/login", "/api/auth/csrf").permitAll()
                        .requestMatchers("/api/shopify/auth", "/api/shopify/callback").permitAll()
                        .anyRequest().authenticated()
                )
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
                .securityContext(context -> context
                        .securityContextRepository(securityContextRepository()))
                .build();
    }

    @Bean PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(); }
    @Bean AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }
    @Bean SecurityContextRepository securityContextRepository() {
        return new HttpSessionSecurityContextRepository();
    }
}
