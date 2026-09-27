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

                Write one polished English paragraph of about 90–145 words, preferably in 3–5 natural sentences. Use natural British English and a premium, restrained luxury resale tone. The customer-facing copy should feel desirable, elegant, and editorial rather than technical, defensive, or data-driven, with the aim of attracting and converting shoppers.

                Use only the supplied product title and facts. Every concrete descriptive claim must be directly traceable to the product title or one supplied fact. Never infer or invent missing details. In particular, never invent accessories, year, material, colour, hardware, provenance, rarity, authenticity documentation, usage context, or marketing claims.

                Do not mention or infer condition, condition grades, or signs of wear. Do not infer sheen, softness, silhouette, construction, detailing, craftsmanship, styling character, use cases, or quality adjectives about physical properties. Do not infer shoulder wear or cross-body wear. Do not infer day-to-evening use, everyday use, practicality, or capacity. Do not infer a matte finish, leather grain, woven texture, shape, opening style, or structural characteristics unless directly supported by the supplied facts or unambiguously stated in the product title. Do not call included accessories original, complete, or a full set unless that wording is explicitly supplied. Do not state that information is missing.

                Mention included accessories only when the includes fact is present. Do not include measurements or length details in the editorial paragraph. Restrained subjective luxury language such as refined, elegant, distinctive, sophisticated, understated, timeless, luxurious, polished, statement piece, or iconic is acceptable to express style or desirability, but it must not imply an unsupplied physical property or add a new concrete factual claim.

                Write directly about the product as a finished customer-facing description. Do not sound as though you are verifying facts. Do not repeat the same facts, list attributes mechanically, or explain why a fact is included. Avoid restating the same colour, material, hardware, or accessory more than once. Do not refer to the source data, listing, supplied facts, confirmed details, attributes, or product information. Avoid phrases such as "as specified", "as stated", "as supplied", "the listing", "the supplied facts", "confirmed details", "stated attributes", "this entry", "product information", or "the provided information". Do not explain what information you are using or describe the generation process.

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
