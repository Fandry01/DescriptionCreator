package com.descriptioncreator.backend.description;

import com.descriptioncreator.backend.generator.DescriptionGeneratorService;
import com.descriptioncreator.backend.generator.GradeMapper;
import com.descriptioncreator.backend.generator.ProductFacts;
import com.descriptioncreator.backend.generator.ProductFactsMapper;
import com.descriptioncreator.backend.generator.PromptBuilder;
import com.descriptioncreator.backend.openai.OpenAiException;
import com.descriptioncreator.backend.shopify.ShopifyClient;
import com.descriptioncreator.backend.shopify.ShopifyProductMetafieldsDto;
import com.descriptioncreator.backend.shopify.ShopifyTokenStore;
import com.descriptioncreator.backend.shopify.TestShopifyTokenStores;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProductDescriptionServiceTests {

    @Test
    void fetchesMapsAndGeneratesDraftWithoutPublishing() {
        ShopifyProductMetafieldsDto product = product();
        ProductFacts facts = facts();
        TrackingShopifyClient shopifyClient = new TrackingShopifyClient(Optional.of(product));
        TrackingProductFactsMapper factsMapper = new TrackingProductFactsMapper(facts);
        TrackingDescriptionGenerator generator =
                new TrackingDescriptionGenerator("Generated draft", null);
        ProductDescriptionService service =
                new ProductDescriptionService(shopifyClient, factsMapper, generator, repository());

        ProductDescriptionDraftResponse response = service.generateDraft("jackie-1961");

        assertThat(response).isEqualTo(new ProductDescriptionDraftResponse(
                "gid://shopify/Product/1",
                "Gucci Jackie 1961",
                "jackie-1961",
                "<p>Existing description</p>",
                "Generated draft",
                facts
        ));
        assertThat(shopifyClient.fetchedHandle).isEqualTo("jackie-1961");
        assertThat(factsMapper.mappedProduct).isSameAs(product);
        assertThat(generator.title).isEqualTo(product.title());
        assertThat(generator.facts).isSameAs(facts);
        assertThat(response.facts().grade()).isEqualTo("2");
        assertThat(response.facts().gradeLabel()).isEqualTo("Excellent Condition");
        assertThat(shopifyClient.published).isFalse();
    }

    @Test
    void returnsNotFoundWhenShopifyProductDoesNotExist() {
        TrackingShopifyClient shopifyClient = new TrackingShopifyClient(Optional.empty());
        TrackingProductFactsMapper factsMapper = new TrackingProductFactsMapper(facts());
        TrackingDescriptionGenerator generator =
                new TrackingDescriptionGenerator("Generated draft", null);
        ProductDescriptionService service =
                new ProductDescriptionService(shopifyClient, factsMapper, generator, repository());

        assertThatThrownBy(() -> service.generateDraft("missing"))
                .isInstanceOfSatisfying(ResponseStatusException.class, exception ->
                        assertThat(exception.getStatusCode().value()).isEqualTo(404)
                );
        assertThat(factsMapper.mappedProduct).isNull();
        assertThat(generator.title).isNull();
        assertThat(shopifyClient.published).isFalse();
    }

    @Test
    void propagatesOpenAiFailureWithoutPublishing() {
        ShopifyProductMetafieldsDto product = product();
        ProductFacts facts = facts();
        OpenAiException failure = new OpenAiException("OpenAI Responses API request failed");
        TrackingShopifyClient shopifyClient = new TrackingShopifyClient(Optional.of(product));
        TrackingProductFactsMapper factsMapper = new TrackingProductFactsMapper(facts);
        TrackingDescriptionGenerator generator = new TrackingDescriptionGenerator(null, failure);
        ProductDescriptionService service =
                new ProductDescriptionService(shopifyClient, factsMapper, generator, repository());

        assertThatThrownBy(() -> service.generateDraft("jackie-1961"))
                .isSameAs(failure);
        assertThat(shopifyClient.published).isFalse();
    }

    @Test
    void publishesEscapedSingleParagraphAfterRefetchingCurrentDescription() {
        TrackingShopifyClient shopifyClient = new TrackingShopifyClient(Optional.of(product()));
        TrackingDescriptionGenerator generator =
                new TrackingDescriptionGenerator("Must not be generated", null);
        TrackingDescriptionVersionRepository history = new TrackingDescriptionVersionRepository();
        ProductDescriptionService service = new ProductDescriptionService(
                shopifyClient,
                new TrackingProductFactsMapper(facts()),
                generator,
                history.repository
        );

        PublishDescriptionResponse response = service.publishDescription(
                "jackie-1961",
                new PublishDescriptionRequest(
                        "  Elegant <bag> & \"icon\".\n  Polished choice.  ",
                        "<p>Existing description</p>"
                )
        );

        String expectedHtml = "<p>Elegant &lt;bag&gt; &amp; &quot;icon&quot;. Polished choice.</p>";
        assertThat(response).isEqualTo(new PublishDescriptionResponse(
                "gid://shopify/Product/1",
                "jackie-1961",
                expectedHtml
        ));
        assertThat(shopifyClient.events).containsExactly("fetch", "update");
        assertThat(shopifyClient.updateCount).isEqualTo(1);
        assertThat(shopifyClient.updatedProductId).isEqualTo("gid://shopify/Product/1");
        assertThat(shopifyClient.updatedDescriptionHtml).isEqualTo(expectedHtml);
        assertThat(generator.title).isNull();
        assertThat(history.saved).hasSize(1);
    }

    @Test
    void rejectsBlankApprovedDescriptionWithoutUpdatingShopify() {
        TrackingShopifyClient shopifyClient = new TrackingShopifyClient(Optional.of(product()));
        ProductDescriptionService service = new ProductDescriptionService(
                shopifyClient,
                new TrackingProductFactsMapper(facts()),
                new TrackingDescriptionGenerator("Must not be generated", null),
                repository()
        );

        assertThatThrownBy(() -> service.publishDescription(
                "jackie-1961",
                new PublishDescriptionRequest("  \n ", "<p>Existing description</p>")
        )).isInstanceOfSatisfying(ResponseStatusException.class, exception ->
                assertThat(exception.getStatusCode().value()).isEqualTo(400)
        );
        assertThat(shopifyClient.events).containsExactly("fetch");
        assertThat(shopifyClient.updateCount).isZero();
    }

    @Test
    void returnsConflictWhenShopifyDescriptionChangedAndDoesNotPublish() {
        TrackingShopifyClient shopifyClient = new TrackingShopifyClient(Optional.of(product()));
        TrackingDescriptionGenerator generator =
                new TrackingDescriptionGenerator("Must not be generated", null);
        TrackingDescriptionVersionRepository history = new TrackingDescriptionVersionRepository();
        ProductDescriptionService service = new ProductDescriptionService(
                shopifyClient,
                new TrackingProductFactsMapper(facts()),
                generator,
                history.repository
        );

        assertThatThrownBy(() -> service.publishDescription(
                "jackie-1961",
                new PublishDescriptionRequest("Approved description", "<p>Older description</p>")
        )).isInstanceOfSatisfying(ResponseStatusException.class, exception -> {
            assertThat(exception.getStatusCode().value()).isEqualTo(409);
            assertThat(exception.getReason())
                    .isEqualTo("The Shopify description changed after the draft was generated");
        });
        assertThat(shopifyClient.events).containsExactly("fetch");
        assertThat(shopifyClient.updateCount).isZero();
        assertThat(generator.title).isNull();
        assertThat(history.saved).isEmpty();
    }

    @Test
    void recordsPublishHistoryOnlyAfterShopifyUpdateSucceeds() {
        TrackingShopifyClient shopifyClient = new TrackingShopifyClient(Optional.of(product()));
        TrackingDescriptionVersionRepository history = new TrackingDescriptionVersionRepository();
        ProductDescriptionService service = service(shopifyClient, history.repository);

        service.publishDescription(
                "jackie-1961",
                new PublishDescriptionRequest("Approved description", "<p>Existing description</p>")
        );

        assertThat(history.saved).hasSize(1);
        DescriptionVersionEntity saved = history.saved.getFirst();
        assertThat(saved.getProductId()).isEqualTo("gid://shopify/Product/1");
        assertThat(saved.getHandle()).isEqualTo("jackie-1961");
        assertThat(saved.getPreviousDescriptionHtml()).isEqualTo("<p>Existing description</p>");
        assertThat(saved.getPublishedDescriptionHtml()).isEqualTo("<p>Approved description</p>");
        assertThat(saved.getAction()).isEqualTo(DescriptionVersionAction.PUBLISH);
        assertThat(saved.getRestoredFromVersionId()).isNull();
        assertThat(shopifyClient.events).containsExactly("fetch", "update");
    }

    @Test
    void failedShopifyPublishCreatesNoHistory() {
        TrackingShopifyClient shopifyClient = new TrackingShopifyClient(Optional.of(product()));
        shopifyClient.updateFailure = new IllegalStateException("Shopify unavailable");
        TrackingDescriptionVersionRepository history = new TrackingDescriptionVersionRepository();
        ProductDescriptionService service = service(shopifyClient, history.repository);

        assertThatThrownBy(() -> service.publishDescription(
                "jackie-1961",
                new PublishDescriptionRequest("Approved", "<p>Existing description</p>")
        )).isSameAs(shopifyClient.updateFailure);

        assertThat(history.saved).isEmpty();
    }

    @Test
    void restoresExactSelectedHtmlAndCreatesAppendOnlyRestoreVersion() {
        DescriptionVersionEntity source = version(
                41L,
                "jackie-1961",
                "<p>Before selected version</p>",
                "<p>Selected historic description</p>"
        );
        TrackingShopifyClient shopifyClient = new TrackingShopifyClient(Optional.of(product()));
        TrackingDescriptionVersionRepository history = new TrackingDescriptionVersionRepository();
        history.addExisting(source);
        history.nextId = 42L;
        ProductDescriptionService service = service(shopifyClient, history.repository);

        RestoreDescriptionResponse response = service.restoreDescription(
                "jackie-1961",
                41L,
                new RestoreDescriptionRequest("<p>Existing description</p>")
        );

        assertThat(response).isEqualTo(new RestoreDescriptionResponse(
                "gid://shopify/Product/1",
                "jackie-1961",
                "<p>Selected historic description</p>",
                41L,
                42L
        ));
        assertThat(shopifyClient.updatedDescriptionHtml)
                .isEqualTo("<p>Selected historic description</p>");
        assertThat(history.saved).hasSize(1);
        DescriptionVersionEntity restore = history.saved.getFirst();
        assertThat(restore.getPreviousDescriptionHtml()).isEqualTo("<p>Existing description</p>");
        assertThat(restore.getPublishedDescriptionHtml())
                .isEqualTo("<p>Selected historic description</p>");
        assertThat(restore.getAction()).isEqualTo(DescriptionVersionAction.RESTORE);
        assertThat(restore.getRestoredFromVersionId()).isEqualTo(41L);
    }

    @Test
    void unknownOrWrongHandleVersionReturnsNotFoundWithoutShopifyWrite() {
        TrackingShopifyClient shopifyClient = new TrackingShopifyClient(Optional.of(product()));
        TrackingDescriptionVersionRepository history = new TrackingDescriptionVersionRepository();
        ProductDescriptionService service = service(shopifyClient, history.repository);

        assertThatThrownBy(() -> service.restoreDescription(
                "other-handle", 99L, new RestoreDescriptionRequest("current")
        )).isInstanceOfSatisfying(ResponseStatusException.class, exception ->
                assertThat(exception.getStatusCode().value()).isEqualTo(404));

        assertThat(shopifyClient.events).isEmpty();
        assertThat(history.saved).isEmpty();
    }

    @Test
    void restoreConflictCreatesNoShopifyWriteAndNoHistory() {
        DescriptionVersionEntity source = version(41L, "jackie-1961", "old", "selected");
        TrackingShopifyClient shopifyClient = new TrackingShopifyClient(Optional.of(product()));
        TrackingDescriptionVersionRepository history = new TrackingDescriptionVersionRepository();
        history.addExisting(source);
        ProductDescriptionService service = service(shopifyClient, history.repository);

        assertThatThrownBy(() -> service.restoreDescription(
                "jackie-1961", 41L, new RestoreDescriptionRequest("<p>stale</p>")
        )).isInstanceOfSatisfying(ResponseStatusException.class, exception ->
                assertThat(exception.getStatusCode().value()).isEqualTo(409));

        assertThat(shopifyClient.events).containsExactly("fetch");
        assertThat(history.saved).isEmpty();
    }

    @Test
    void failedShopifyRestoreCreatesNoHistory() {
        DescriptionVersionEntity source = version(41L, "jackie-1961", "old", "selected");
        TrackingShopifyClient shopifyClient = new TrackingShopifyClient(Optional.of(product()));
        shopifyClient.updateFailure = new IllegalStateException("Shopify unavailable");
        TrackingDescriptionVersionRepository history = new TrackingDescriptionVersionRepository();
        history.addExisting(source);
        ProductDescriptionService service = service(shopifyClient, history.repository);

        assertThatThrownBy(() -> service.restoreDescription(
                "jackie-1961", 41L,
                new RestoreDescriptionRequest("<p>Existing description</p>")
        )).isSameAs(shopifyClient.updateFailure);

        assertThat(history.saved).isEmpty();
    }

    @Test
    void shopifyClientExposesNoInventoryOrVariantUpdatePath() {
        List<String> updateMethods = Arrays.stream(ShopifyClient.class.getDeclaredMethods())
                .map(method -> method.getName())
                .filter(name -> name.startsWith("update"))
                .toList();

        assertThat(updateMethods).containsExactly("updateProductDescription");
        assertThat(updateMethods).noneMatch(name ->
                name.toLowerCase().contains("inventory")
                        || name.toLowerCase().contains("variant")
        );
    }

    private ShopifyProductMetafieldsDto product() {
        return new ShopifyProductMetafieldsDto(
                "gid://shopify/Product/1",
                "Gucci Jackie 1961",
                "jackie-1961",
                "Gucci",
                "<p>Existing description</p>",
                List.of()
        );
    }

    private ProductFacts facts() {
        return new ProductFacts(
                "Gucci", "Jackie 1961", "Black", "2", "Excellent Condition",
                "Leather", "Gold-tone", "Dust bag", "", "", "", "", "2022", ""
        );
    }

    private ProductDescriptionService service(
            TrackingShopifyClient shopifyClient,
            DescriptionVersionRepository repository
    ) {
        return new ProductDescriptionService(
                shopifyClient,
                new TrackingProductFactsMapper(facts()),
                new TrackingDescriptionGenerator("Must not be generated", null),
                repository
        );
    }

    private DescriptionVersionRepository repository() {
        return new TrackingDescriptionVersionRepository().repository;
    }

    private DescriptionVersionEntity version(
            Long id,
            String handle,
            String previousDescriptionHtml,
            String publishedDescriptionHtml
    ) {
        DescriptionVersionEntity version = new DescriptionVersionEntity(
                "gid://shopify/Product/1",
                handle,
                previousDescriptionHtml,
                publishedDescriptionHtml,
                DescriptionVersionAction.PUBLISH,
                null,
                Instant.parse("2026-01-01T00:00:00Z")
        );
        ReflectionTestUtils.setField(version, "id", id);
        return version;
    }

    private static class TrackingDescriptionVersionRepository {

        private final List<DescriptionVersionEntity> saved = new ArrayList<>();
        private final Map<Long, DescriptionVersionEntity> existing = new HashMap<>();
        private long nextId = 1L;
        private final DescriptionVersionRepository repository =
                (DescriptionVersionRepository) Proxy.newProxyInstance(
                        DescriptionVersionRepository.class.getClassLoader(),
                        new Class<?>[]{DescriptionVersionRepository.class},
                        (proxy, method, arguments) -> switch (method.getName()) {
                            case "saveAndFlush" -> save((DescriptionVersionEntity) arguments[0]);
                            case "findByIdAndHandle" -> find(
                                    (Long) arguments[0],
                                    (String) arguments[1]
                            );
                            case "findAllByHandleOrderByCreatedAtDescIdDesc" -> existing.values()
                                    .stream()
                                    .filter(version -> version.getHandle().equals(arguments[0]))
                                    .sorted((left, right) -> {
                                        int byCreatedAt = right.getCreatedAt()
                                                .compareTo(left.getCreatedAt());
                                        return byCreatedAt != 0
                                                ? byCreatedAt
                                                : right.getId().compareTo(left.getId());
                                    })
                                    .toList();
                            case "toString" -> "TrackingDescriptionVersionRepository";
                            default -> throw new UnsupportedOperationException(method.getName());
                        }
                );

        private DescriptionVersionEntity save(DescriptionVersionEntity version) {
            if (version.getId() == null) {
                ReflectionTestUtils.setField(version, "id", nextId++);
            }
            saved.add(version);
            existing.put(version.getId(), version);
            return version;
        }

        private Optional<DescriptionVersionEntity> find(Long id, String handle) {
            return Optional.ofNullable(existing.get(id))
                    .filter(version -> version.getHandle().equals(handle));
        }

        private void addExisting(DescriptionVersionEntity version) {
            existing.put(version.getId(), version);
        }
    }

    private static class TrackingShopifyClient extends ShopifyClient {

        private final Optional<ShopifyProductMetafieldsDto> product;
        private final List<String> events = new ArrayList<>();
        private String fetchedHandle;
        private boolean published;
        private int updateCount;
        private String updatedProductId;
        private String updatedDescriptionHtml;
        private RuntimeException updateFailure;

        TrackingShopifyClient(Optional<ShopifyProductMetafieldsDto> product) {
            super(RestClient.create(), TestShopifyTokenStores.create());
            this.product = product;
        }

        @Override
        public Optional<ShopifyProductMetafieldsDto> fetchProductMetafieldsByHandle(String handle) {
            events.add("fetch");
            fetchedHandle = handle;
            return product;
        }

        @Override
        public void updateProductDescription(String productId, String descriptionHtml) {
            events.add("update");
            if (updateFailure != null) {
                throw updateFailure;
            }
            published = true;
            updateCount++;
            updatedProductId = productId;
            updatedDescriptionHtml = descriptionHtml;
        }
    }

    private static class TrackingProductFactsMapper extends ProductFactsMapper {

        private final ProductFacts mappedFacts;
        private ShopifyProductMetafieldsDto mappedProduct;

        TrackingProductFactsMapper(ProductFacts mappedFacts) {
            super(new GradeMapper());
            this.mappedFacts = mappedFacts;
        }

        @Override
        public ProductFacts map(ShopifyProductMetafieldsDto product) {
            mappedProduct = product;
            return mappedFacts;
        }
    }

    private static class TrackingDescriptionGenerator extends DescriptionGeneratorService {

        private final String generatedDescription;
        private final OpenAiException failure;
        private String title;
        private ProductFacts facts;

        TrackingDescriptionGenerator(String generatedDescription, OpenAiException failure) {
            super(new PromptBuilder(), prompt -> "");
            this.generatedDescription = generatedDescription;
            this.failure = failure;
        }

        @Override
        public String generate(String title, ProductFacts productFacts) {
            this.title = title;
            this.facts = productFacts;
            if (failure != null) {
                throw failure;
            }
            return generatedDescription;
        }
    }
}
