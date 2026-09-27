package com.descriptioncreator.backend.generator;

import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Component
public class PromptBuilder {

    public String build(String title, ProductFacts productFacts) {
        Map<String, String> facts = buildFactsMap(productFacts);
        String productTitle = title == null ? "" : title.trim();

        return """
                You write product descriptions for Designer Stories, a premium luxury resale catalogue.

                Write one polished English paragraph of about 90–145 words. Use natural British English and a premium, restrained luxury resale tone.

                Use only the supplied product title and facts. Never infer or invent missing details. In particular, never invent accessories, year, material, colour, hardware, provenance, rarity, authenticity documentation, usage context, or marketing claims.

                Do not infer shoulder wear or cross-body wear. Do not infer day-to-evening use, everyday use, practicality, or capacity. Do not infer a matte finish, leather grain, woven texture, shape, opening style, or structural characteristics unless directly supported by the supplied facts or unambiguously stated in the product title. Do not call included accessories original, complete, or a full set unless that wording is explicitly supplied. Do not state that information is missing.

                Mention included accessories only when the includes fact is present. Mention the condition naturally near the end when gradeLabel is present. Mention signs of wear only when signsOfWear is present. Do not include measurements or length details in the editorial paragraph.

                Do not use markdown, headings, bullet points, or HTML. Return only the final paragraph.

                Shopify product title:
                "%s"

                Supplied facts:
                %s
                """.formatted(escape(productTitle), toCompactJson(facts)).trim();
    }

    Map<String, String> buildFactsMap(ProductFacts productFacts) {
        Objects.requireNonNull(productFacts, "productFacts must not be null");

        Map<String, String> facts = new LinkedHashMap<>();
        putIfPresent(facts, "brand", productFacts.brand());
        putIfPresent(facts, "model", productFacts.model());
        putIfPresent(facts, "color", productFacts.color());
        putIfPresent(facts, "material", productFacts.material());
        putIfPresent(facts, "hardware", productFacts.hardware());
        putIfPresent(facts, "year", productFacts.year());
        putIfPresent(facts, "gradeLabel", productFacts.gradeLabel());
        putIfPresent(facts, "signsOfWear", productFacts.signsOfWear());
        putIfPresent(facts, "includes", productFacts.includes());
        return Collections.unmodifiableMap(facts);
    }

    private void putIfPresent(Map<String, String> facts, String key, String value) {
        if (value != null && !value.isBlank()) {
            facts.put(key, value.trim());
        }
    }

    private String toCompactJson(Map<String, String> facts) {
        return facts.entrySet().stream()
                .map(entry -> "\"" + escape(entry.getKey()) + "\":\"" + escape(entry.getValue()) + "\"")
                .collect(Collectors.joining(",", "{", "}"));
    }

    private String escape(String value) {
        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\r", "\\r")
                .replace("\n", "\\n")
                .replace("\t", "\\t");
    }
}
