package com.descriptioncreator.backend.shopify;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:shopify_token_store;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@Transactional
class ShopifyTokenStoreTests {

    @Autowired
    private ShopifyConnectionRepository repository;

    @Test
    void storeInsertsNewConnection() {
        ShopifyTokenStore tokenStore = tokenStore();

        tokenStore.store("  EXAMPLE.myshopify.com  ", "first-token");

        assertThat(repository.count()).isEqualTo(1);
        ShopifyConnectionEntity connection = repository
                .findByShopDomain("example.myshopify.com")
                .orElseThrow();
        assertThat(connection.getShopDomain()).isEqualTo("example.myshopify.com");
        assertThat(connection.getAccessToken()).isEqualTo("first-token");
        assertThat(connection.getCreatedAt()).isNotNull();
        assertThat(connection.getUpdatedAt()).isNotNull();
    }

    @Test
    void secondStoreUpdatesExistingConnectionWithoutDuplicate() {
        ShopifyTokenStore tokenStore = tokenStore();
        tokenStore.store("example.myshopify.com", "first-token");
        ShopifyConnectionEntity original = repository
                .findByShopDomain("example.myshopify.com")
                .orElseThrow();

        tokenStore.store("EXAMPLE.MYSHOPIFY.COM", "replacement-token");

        assertThat(repository.count()).isEqualTo(1);
        ShopifyConnectionEntity updated = repository
                .findByShopDomain("example.myshopify.com")
                .orElseThrow();
        assertThat(updated).isSameAs(original);
        assertThat(updated.getAccessToken()).isEqualTo("replacement-token");
        assertThat(updated.getCreatedAt()).isEqualTo(original.getCreatedAt());
        assertThat(updated.getUpdatedAt().isBefore(updated.getCreatedAt())).isFalse();
    }

    @Test
    void accessTokenSurvivesCreatingNewStoreAgainstSameRepositoryState() {
        tokenStore().store("example.myshopify.com", "persisted-token");

        ShopifyTokenStore restartedStore = tokenStore();

        assertThat(restartedStore.accessToken()).contains("persisted-token");
    }

    @Test
    void statusIsConnectedForPersistedTokenWithoutExposingIt() {
        ShopifyTokenStore tokenStore = tokenStore();
        tokenStore.store("example.myshopify.com", "secret-token-value");

        ShopifyTokenStore.Status status = tokenStore.status("example.myshopify.com");

        assertThat(status.connected()).isTrue();
        assertThat(status.shop()).isEqualTo("example.myshopify.com");
        assertThat(status.toString()).doesNotContain("secret-token-value");
    }

    @Test
    void statusIsDisconnectedAndStillReturnsConfiguredShopWhenAbsent() {
        ShopifyTokenStore.Status status = tokenStore()
                .status("example.myshopify.com");

        assertThat(status.connected()).isFalse();
        assertThat(status.shop()).isEqualTo("example.myshopify.com");
    }

    private ShopifyTokenStore tokenStore() {
        return new ShopifyTokenStore(
                repository,
                TestShopifyTokenStores.properties()
        );
    }
}
