package com.descriptioncreator.backend.description;

public record PublishDescriptionResponse(
        String productId,
        String handle,
        String publishedDescriptionHtml
) {
}
