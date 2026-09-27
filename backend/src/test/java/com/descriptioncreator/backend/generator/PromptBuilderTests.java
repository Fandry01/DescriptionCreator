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
                .contains("Every concrete descriptive claim must be directly traceable")
                .contains("Do not mention or infer condition, condition grades, or signs of wear")
                .contains("Do not infer sheen, softness, silhouette, construction, detailing, craftsmanship, styling character, use cases, or quality adjectives about physical properties")
                .contains("preferably in 3–5 natural sentences")
                .contains("desirable, elegant, and editorial rather than technical, defensive, or data-driven")
                .contains("refined, elegant, distinctive, sophisticated, understated, timeless, luxurious, polished, statement piece, or iconic")
                .contains("must not imply an unsupplied physical property or add a new concrete factual claim")
                .contains("Do not infer shoulder wear or cross-body wear")
                .contains("Do not infer day-to-evening use, everyday use, practicality, or capacity")
                .contains("Do not state that information is missing")
                .contains("Do not call included accessories original, complete, or a full set")
                .contains("Write directly about the product as a finished customer-facing description")
                .contains("Do not sound as though you are verifying facts")
                .contains("Do not repeat the same facts, list attributes mechanically, or explain why a fact is included")
                .contains("Avoid restating the same colour, material, hardware, or accessory more than once")
                .contains("Do not refer to the source data, listing, supplied facts, confirmed details, attributes, or product information")
                .contains("\"as specified\", \"as stated\", \"as supplied\", \"the listing\", \"the supplied facts\", \"confirmed details\", \"stated attributes\", \"this entry\", \"product information\", or \"the provided information\"")
                .contains("Do not explain what information you are using or describe the generation process");
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
