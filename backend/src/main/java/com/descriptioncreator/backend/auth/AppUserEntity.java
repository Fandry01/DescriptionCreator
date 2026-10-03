package com.descriptioncreator.backend.auth;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.Locale;

@Entity
@Table(name = "app_user", uniqueConstraints = @UniqueConstraint(name = "uk_app_user_email", columnNames = "email"))
public class AppUserEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false) private String email;
    @Column(name = "password_hash", nullable = false) private String passwordHash;
    @Column(name = "display_name") private String displayName;
    @Column(nullable = false) private boolean enabled = true;
    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;

    protected AppUserEntity() {}

    public AppUserEntity(String email, String passwordHash, String displayName) {
        this.email = normalizeEmail(email);
        this.passwordHash = passwordHash;
        this.displayName = displayName == null || displayName.isBlank() ? null : displayName.trim();
        this.createdAt = Instant.now();
    }

    public static String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }

    public Long getId() { return id; }
    public String getEmail() { return email; }
    public String getPasswordHash() { return passwordHash; }
    public String getDisplayName() { return displayName; }
    public boolean isEnabled() { return enabled; }
    public Instant getCreatedAt() { return createdAt; }
}
