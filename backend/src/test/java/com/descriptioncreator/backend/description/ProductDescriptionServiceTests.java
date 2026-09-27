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
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

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
                new ProductDescriptionService(shopifyClient, factsMapper, generator);

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
                new ProductDescriptionService(shopifyClient, factsMapper, generator);

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
                new ProductDescriptionService(shopifyClient, factsMapper, generator);

        assertThatThrownBy(() -> service.generateDraft("jackie-1961"))
                .isSameAs(failure);
        assertThat(shopifyClient.published).isFalse();
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

    private static class TrackingShopifyClient extends ShopifyClient {

        private final Optional<ShopifyProductMetafieldsDto> product;
        private String fetchedHandle;
        private boolean published;

        TrackingShopifyClient(Optional<ShopifyProductMetafieldsDto> product) {
            super(RestClient.create(), new ShopifyTokenStore());
            this.product = product;
        }

        @Override
        public Optional<ShopifyProductMetafieldsDto> fetchProductMetafieldsByHandle(String handle) {
            fetchedHandle = handle;
            return product;
        }

        @Override
        public void updateProductDescription(String productId, String descriptionHtml) {
            published = true;
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
