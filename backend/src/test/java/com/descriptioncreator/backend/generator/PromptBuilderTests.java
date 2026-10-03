package com.descriptioncreator.backend.generator;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

class PromptBuilderTests {

    private final PromptBuilder promptBuilder = new PromptBuilder();

    @Test
    void omitsBlankFacts() {
        Map<String, String> facts = promptBuilder.buildFactsMap(facts(
                "  Gucci  ",
                "   ",
                "Black",
                "",
                "",
                "",
                "",
                "",
                "",
                "",
                "",
                "",
                "",
                ""
        ));

        assertThat(facts).containsOnly(
                entry("brand", "Gucci"),
                entry("color", "Black")
        );
    }

    @Test
    void excludesMeasurementFactsFromPrompt() {
        String prompt = promptBuilder.build("Gucci Jackie 1961", facts(
                "Gucci",
                "Jackie 1961",
                "Black",
                "",
                "Excellent Condition",
                "Leather",
                "Gold-tone",
                "Dust bag",
                "SIZE_SENTINEL",
                "DIMENSIONS_SENTINEL",
                "CHAIN_SENTINEL",
                "STRAP_SENTINEL",
                "2022",
                ""
        ));

        assertThat(prompt)
                .doesNotContain("SIZE_SENTINEL")
                .doesNotContain("DIMENSIONS_SENTINEL")
                .doesNotContain("CHAIN_SENTINEL")
                .doesNotContain("STRAP_SENTINEL");
    }

    @Test
    void includesOnlyEditorialFactsAndExcludesConditionData() {
        ProductFacts productFacts = facts(
                "Gucci",
                "Jackie 1961",
                "Black",
                "GRADE_SENTINEL",
                "GRADE_LABEL_SENTINEL",
                "Leather",
                "Gold-tone",
                "Dust bag",
                "",
                "",
                "",
                "",
                "2022",
                "WEAR_SENTINEL"
        );
        String prompt = promptBuilder.build("Gucci Jackie 1961", productFacts);

        assertThat(prompt)
                .contains("\"brand\":\"Gucci\"")
                .contains("\"model\":\"Jackie 1961\"")
                .contains("\"color\":\"Black\"")
                .contains("\"material\":\"Leather\"")
                .contains("\"hardware\":\"Gold-tone\"")
                .contains("\"year\":\"2022\"")
                .contains("\"includes\":\"Dust bag\"")
                .doesNotContain("GRADE_SENTINEL")
                .doesNotContain("GRADE_LABEL_SENTINEL")
                .doesNotContain("WEAR_SENTINEL")
                .doesNotContain("Mention the condition naturally near the end")
                .doesNotContain("Mention signs of wear");
        assertThat(promptBuilder.buildFactsMap(productFacts)).containsExactly(
                entry("brand", "Gucci"),
                entry("model", "Jackie 1961"),
                entry("color", "Black"),
                entry("material", "Leather"),
                entry("hardware", "Gold-tone"),
                entry("year", "2022"),
                entry("includes", "Dust bag")
        );
    }

    @Test
    void includesAccessoriesOnlyWhenPresent() {
        String withAccessories = promptBuilder.build("Gucci bag", facts(
                "Gucci", "", "", "", "", "", "", "  Dust bag  ",
                "", "", "", "", "", ""
        ));
        String withoutAccessories = promptBuilder.build("Gucci bag", facts(
                "Gucci", "", "", "", "", "", "", "   ",
                "", "", "", "", "", ""
        ));

        assertThat(withAccessories).contains("\"includes\":\"Dust bag\"");
        assertThat(withoutAccessories).doesNotContain("\"includes\":");
    }

    @Test
    void includesShopifyTitleAndStrictFactualSafetyRules() {
        String prompt = promptBuilder.build("  Gucci Jackie 1961 shoulder bag  ", facts(
                "Gucci", "Jackie 1961", "", "", "", "", "", "",
                "", "", "", "", "", ""
        ));

        assertThat(prompt)
                .contains("Shopify product title:\n\"Gucci Jackie 1961 shoulder bag\"")
                .contains("Every concrete claim about the product must be directly traceable")
                .contains("Do not mention condition, grade or signs of wear")
                .contains("Do not infer unsupplied:")
                .contains("- sheen")
                .contains("- softness")
                .contains("- silhouette")
                .contains("- construction")
                .contains("- craftsmanship details")
                .contains("- use cases")
                .contains("in 3–5 natural sentences")
                .contains("refined, desirable and editorial")
                .contains("refined, elegant, distinctive, sophisticated, understated, timeless, luxurious, polished, iconic or statement piece")
                .contains("must not imply a concrete physical property that is not supplied")
                .contains("Do not infer shoulder wear, cross-body wear, day-to-evening use, everyday use, practicality or storage capacity")
                .contains("Do not call accessories original, complete or a full set")
                .contains("Do not list attributes mechanically")
                .contains("Do not repeat the same colour, material, hardware or accessory more than once")
                .contains("Do not refer to the listing, supplied facts, source data, confirmed details, attributes, product information or generation process")
                .contains("- \"as specified\"")
                .contains("- \"the provided information\"");
    }

    private ProductFacts facts(
            String brand,
            String model,
            String color,
            String grade,
            String gradeLabel,
            String material,
            String hardware,
            String includes,
            String size,
            String dimensions,
            String chainLength,
            String strapLength,
            String year,
            String signsOfWear
    ) {
        return new ProductFacts(
                brand,
                model,
                color,
                grade,
                gradeLabel,
                material,
                hardware,
                includes,
                size,
                dimensions,
                chainLength,
                strapLength,
                year,
                signsOfWear
        );
    }
}
