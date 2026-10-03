package com.descriptioncreator.backend.description;

import java.time.Instant;

public record DescriptionVersionResponse(
        Long id,
        String productId,
        String handle,
        String previousDescriptionHtml,
        String publishedDescriptionHtml,
        DescriptionVersionAction action,
        Long restoredFromVersionId,
        Instant createdAt
) {

    static DescriptionVersionResponse from(DescriptionVersionEntity version) {
        return new DescriptionVersionResponse(
                version.getId(),
                version.getProductId(),
                version.getHandle(),
                version.getPreviousDescriptionHtml(),
                version.getPublishedDescriptionHtml(),
                version.getAction(),
                version.getRestoredFromVersionId(),
                version.getCreatedAt()
        );
    }
}
