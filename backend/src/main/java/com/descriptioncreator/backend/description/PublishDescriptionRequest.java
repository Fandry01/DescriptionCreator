package com.descriptioncreator.backend.description;

public record PublishDescriptionRequest(
        String description,
        String expectedExistingDescriptionHtml
) {
}
