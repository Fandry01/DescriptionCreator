package com.descriptioncreator.backend.description;

public record RestoreDescriptionResponse(
        String productId,
        String handle,
        String publishedDescriptionHtml,
        Long restoredFromVersionId,
        Long newVersionId
) {
}
