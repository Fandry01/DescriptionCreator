package com.descriptioncreator.backend.generator;

import com.descriptioncreator.backend.shopify.ShopifyProductMetafieldsDto;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ProductFactsMapperTests {

    private final ProductFactsMapper mapper = new ProductFactsMapper(new GradeMapper());

    @Test
    void mapsConfirmedFieldsAndTrimsValues() {
        ShopifyProductMetafieldsDto product = product(
                "  Gucci  ",
                List.of(
                        metafield("custom", "model", "  Jackie 1961  "),
                        metafield("custom", "grade", "  2  "),
                        metafield("custom", "strap_length", "  45 cm  "),
                        metafield("custom", "signs_of_wear", "  Light corner wear  "),
                        metafield("unrelated", "model", "Ignored"),
                        metafield("custom", "unknown_field", "Ignored")
                )
        );

        ProductFacts facts = mapper.map(product);

        assertThat(facts.brand()).isEqualTo("Gucci");
        assertThat(facts.model()).isEqualTo("Jackie 1961");
        assertThat(facts.grade()).isEqualTo("2");
        assertThat(facts.gradeLabel()).isEqualTo("Excellent Condition");
        assertThat(facts.strapLength()).isEqualTo("45 cm");
        assertThat(facts.signsOfWear()).isEqualTo("Light corner wear");
    }

    @Test
    void leavesMissingMetafieldsEmpty() {
        ProductFacts facts = mapper.map(product(null, List.of()));

        assertThat(facts.brand()).isEmpty();
        assertThat(facts.model()).isEmpty();
        assertThat(facts.color()).isEmpty();
        assertThat(facts.grade()).isEmpty();
        assertThat(facts.gradeLabel()).isEmpty();
        assertThat(facts.material()).isEmpty();
        assertThat(facts.hardware()).isEmpty();
        assertThat(facts.includes()).isEmpty();
        assertThat(facts.size()).isEmpty();
        assertThat(facts.dimensions()).isEmpty();
        assertThat(facts.chainLength()).isEmpty();
        assertThat(facts.strapLength()).isEmpty();
        assertThat(facts.year()).isEmpty();
        assertThat(facts.signsOfWear()).isEmpty();
    }

    private ShopifyProductMetafieldsDto product(
            String vendor,
            List<ShopifyProductMetafieldsDto.Metafield> metafields
    ) {
        return new ShopifyProductMetafieldsDto(
                "gid://shopify/Product/1",
                "Test product",
                "test-product",
                vendor,
                metafields
        );
    }

    private ShopifyProductMetafieldsDto.Metafield metafield(
            String namespace,
            String key,
            String value
    ) {
        return new ShopifyProductMetafieldsDto.Metafield(
                namespace,
                key,
                value,
                "single_line_text_field"
        );
    }
}
