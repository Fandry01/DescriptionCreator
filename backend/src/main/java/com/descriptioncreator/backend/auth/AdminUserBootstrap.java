package com.descriptioncreator.backend.auth;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class AdminUserBootstrap implements ApplicationRunner {
    private final AppUserRepository repository;
    private final PasswordEncoder passwordEncoder;
    private final String email;
    private final String password;
    private final String displayName;

    public AdminUserBootstrap(AppUserRepository repository, PasswordEncoder passwordEncoder,
            @Value("${app.admin.email:}") String email,
            @Value("${app.admin.password:}") String password,
            @Value("${app.admin.display-name:}") String displayName) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
        this.email = email;
        this.password = password;
        this.displayName = displayName;
    }

    @Override @Transactional
    public void run(ApplicationArguments args) {
        String normalized = AppUserEntity.normalizeEmail(email);
        if (normalized.isBlank() || password == null || password.isBlank()) return;
        if (repository.findByEmailIgnoreCase(normalized).isEmpty()) {
            repository.save(new AppUserEntity(normalized, passwordEncoder.encode(password), displayName));
        }
    }
}
