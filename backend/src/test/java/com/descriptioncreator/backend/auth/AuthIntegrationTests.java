package com.descriptioncreator.backend.auth;

import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:auth_tests;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@AutoConfigureMockMvc
@Transactional
class AuthIntegrationTests {
    private static final String FRONTEND_ORIGIN = "https://descriptioncreator-frontend.onrender.com";

    @Autowired MockMvc mvc;
    @Autowired AppUserRepository users;
    @Autowired PasswordEncoder encoder;

    @BeforeEach void createUser() {
        users.save(new AppUserEntity("User@Example.com", encoder.encode("safe-password"), "Designer Stories"));
    }

    @Test void storesBcryptHashRatherThanPlainText() {
        String hash = users.findByEmailIgnoreCase("user@example.com").orElseThrow().getPasswordHash();
        assertThat(hash).isNotEqualTo("safe-password").startsWith("$2");
        assertThat(encoder.matches("safe-password", hash)).isTrue();
    }

    @Test void validLoginPersistsSessionAndLogoutInvalidatesIt() throws Exception {
        org.springframework.mock.web.MockHttpSession preLoginSession =
                new org.springframework.mock.web.MockHttpSession();
        String preLoginSessionId = preLoginSession.getId();
        HttpSession session = (HttpSession) mvc.perform(post("/api/auth/login").with(csrf())
                        .session(preLoginSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\" USER@example.com \",\"password\":\"safe-password\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.authenticated").value(true))
                .andExpect(jsonPath("$.email").value("user@example.com"))
                .andReturn().getRequest().getSession(false);

        assertThat(session.getId()).isNotEqualTo(preLoginSessionId);

        mvc.perform(get("/api/auth/me").session((org.springframework.mock.web.MockHttpSession) session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.displayName").value("Designer Stories"));

        mvc.perform(post("/api/auth/logout").with(csrf())
                        .session((org.springframework.mock.web.MockHttpSession) session))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/auth/me").session((org.springframework.mock.web.MockHttpSession) session))
                .andExpect(status().isUnauthorized());
    }

    @Test void invalidPasswordAndProtectedProductsReturnUnauthorized() throws Exception {
        mvc.perform(post("/api/auth/login").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"user@example.com\",\"password\":\"wrong\"}"))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/products")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/auth/me")).andExpect(status().isUnauthorized());
    }

    @Test void shopifyOauthEntryPointsRemainPublicWhileStatusIsProtected() throws Exception {
        mvc.perform(get("/api/shopify/auth")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/shopify/status")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/shopify/callback")).andExpect(result ->
                assertThat(result.getResponse().getStatus()).isNotEqualTo(401));
    }

    @Test void allowedFrontendOriginReceivesCredentialedCorsHeaders() throws Exception {
        mvc.perform(options("/api/auth/login")
                        .header("Origin", FRONTEND_ORIGIN)
                        .header("Access-Control-Request-Method", "POST")
                        .header("Access-Control-Request-Headers", "Content-Type, X-XSRF-TOKEN"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", FRONTEND_ORIGIN))
                .andExpect(header().string("Access-Control-Allow-Credentials", "true"))
                .andExpect(header().string("Access-Control-Allow-Methods", "GET,POST,PUT,PATCH,DELETE,OPTIONS"))
                .andExpect(header().string("Access-Control-Allow-Headers", "Content-Type, X-XSRF-TOKEN"))
                .andExpect(header().doesNotExist("Access-Control-Expose-Headers"));
    }

    @Test void disallowedOriginIsNotGrantedCorsAccess() throws Exception {
        mvc.perform(options("/api/auth/login")
                        .header("Origin", "https://untrusted.example")
                        .header("Access-Control-Request-Method", "POST"))
                .andExpect(status().isForbidden())
                .andExpect(header().doesNotExist("Access-Control-Allow-Origin"))
                .andExpect(header().doesNotExist("Access-Control-Allow-Credentials"));
    }

    @Test void corsDoesNotDisableCsrfProtection() throws Exception {
        mvc.perform(post("/api/auth/login")
                        .header("Origin", FRONTEND_ORIGIN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"user@example.com\",\"password\":\"safe-password\"}"))
                .andExpect(status().isForbidden())
                .andExpect(header().string("Access-Control-Allow-Origin", FRONTEND_ORIGIN))
                .andExpect(header().string("Access-Control-Allow-Credentials", "true"));
    }
}
