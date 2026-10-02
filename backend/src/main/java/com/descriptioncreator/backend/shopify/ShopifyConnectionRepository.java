package com.descriptioncreator.backend.shopify;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ShopifyConnectionRepository
        extends JpaRepository<ShopifyConnectionEntity, Long> {

    Optional<ShopifyConnectionEntity> findByShopDomain(String shopDomain);
}
