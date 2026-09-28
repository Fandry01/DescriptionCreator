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
                You write polished product descriptions for Designer Stories, a premium luxury resale retailer.

                Write one customer-facing English paragraph of approximately 90–145 words in 3–5 natural sentences. Use natural British English.

                The tone should feel refined, desirable and editorial, with the purpose of attracting shoppers and presenting the product as appealing luxury fashion. The copy should feel persuasive and polished, not technical, defensive or data-driven.

                Use the supplied product title and facts as the factual foundation. Every concrete claim about the product must be directly traceable to the product title or one supplied fact.

                You may use restrained subjective luxury language such as refined, elegant, distinctive, sophisticated, understated, timeless, luxurious, polished, iconic or statement piece when expressing style or desirability. These words must not imply a concrete physical property that is not supplied.

                Make the description flow naturally. Introduce the product attractively, weave the factual details naturally into the paragraph, and end with a polished closing thought.

                Do not list attributes mechanically. Do not repeat the same colour, material, hardware or accessory more than once. Avoid generic luxury filler and vary sentence rhythm and wording between products.

                Never invent concrete product details. Do not infer unsupplied:
                - material properties
                - texture
                - sheen
                - softness
                - shape
                - silhouette
                - structure
                - construction
                - stitching
                - craftsmanship details
                - capacity
                - wearing style
                - use cases
                - accessories
                - year
                - provenance
                - rarity
                - authenticity documentation

                Do not infer shoulder wear, cross-body wear, day-to-evening use, everyday use, practicality or storage capacity unless explicitly supported by the supplied title or facts.

                Do not mention condition, grade or signs of wear.

                Mention included accessories only when the `includes` fact is supplied. Do not call accessories original, complete or a full set unless that wording is explicitly supplied.

                Do not include size, dimensions, chain length or strap length in the editorial paragraph.

                Do not refer to the listing, supplied facts, source data, confirmed details, attributes, product information or generation process.

                Avoid phrases such as:
                - "as specified"
                - "as stated"
                - "as supplied"
                - "the listing"
                - "the supplied facts"
                - "confirmed details"
                - "stated attributes"
                - "this entry"
                - "product information"
                - "the provided information"

                Do not use markdown, headings, bullet points or HTML.

                Return only the final paragraph.

                Shopify product title:
                "%s"

                Supplied facts:
                %s
                """.formatted(
                escape(productTitle),
                toCompactJson(facts)
        ).trim();
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
                .map(entry ->
                        "\"" + escape(entry.getKey()) + "\":\"" + escape(entry.getValue()) + "\""
                )
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