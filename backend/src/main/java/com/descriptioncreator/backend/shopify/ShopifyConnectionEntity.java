package com.descriptioncreator.backend.shopify;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;

@Entity
@Table(
        name = "shopify_connection",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_shopify_connection_shop_domain",
                columnNames = "shop_domain"
        )
)
public class ShopifyConnectionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "shop_domain", nullable = false, unique = true)
    private String shopDomain;

    @Column(name = "access_token", nullable = false, length = 512)
    private String accessToken;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected ShopifyConnectionEntity() {
    }

    ShopifyConnectionEntity(String shopDomain, String accessToken, Instant now) {
        this.shopDomain = shopDomain;
        this.accessToken = accessToken;
        this.createdAt = now;
        this.updatedAt = now;
    }

    public Long getId() {
        return id;
    }

    public String getShopDomain() {
        return shopDomain;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    void updateAccessToken(String accessToken, Instant now) {
        this.accessToken = accessToken;
        this.updatedAt = now;
    }
}
