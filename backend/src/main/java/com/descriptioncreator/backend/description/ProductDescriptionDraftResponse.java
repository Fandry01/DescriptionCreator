package com.descriptioncreator.backend.description;

import com.descriptioncreator.backend.generator.ProductFacts;

public record ProductDescriptionDraftResponse(
        String productId,
        String title,
        String handle,
        String existingDescriptionHtml,
        String generatedDescription,
        ProductFacts facts
) {
}
