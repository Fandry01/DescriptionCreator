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
        String prompt = promptBuilder.build(facts(
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
    void includesGradeLabelAndSignsOfWearWhenPresent() {
        String prompt = promptBuilder.build(facts(
                "Gucci",
                "Jackie 1961",
                "Black",
                "2",
                "Excellent Condition",
                "Leather",
                "",
                "",
                "",
                "",
                "",
                "",
                "",
                "Light corner wear"
        ));

        assertThat(prompt)
                .contains("\"gradeLabel\":\"Excellent Condition\"")
                .contains("\"signsOfWear\":\"Light corner wear\"");
    }

    @Test
    void includesAccessoriesOnlyWhenPresent() {
        String withAccessories = promptBuilder.build(facts(
                "Gucci", "", "", "", "", "", "", "  Dust bag  ",
                "", "", "", "", "", ""
        ));
        String withoutAccessories = promptBuilder.build(facts(
                "Gucci", "", "", "", "", "", "", "   ",
                "", "", "", "", "", ""
        ));

        assertThat(withAccessories).contains("\"includes\":\"Dust bag\"");
        assertThat(withoutAccessories).doesNotContain("\"includes\":");
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
