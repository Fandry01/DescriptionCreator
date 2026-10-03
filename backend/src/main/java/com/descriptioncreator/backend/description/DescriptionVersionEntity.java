package com.descriptioncreator.backend.description;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(
        name = "description_version",
        indexes = {
                @Index(name = "idx_description_version_handle", columnList = "handle"),
                @Index(name = "idx_description_version_product_id", columnList = "product_id"),
                @Index(name = "idx_description_version_created_at", columnList = "created_at")
        }
)
public class DescriptionVersionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "product_id", nullable = false)
    private String productId;

    @Column(nullable = false)
    private String handle;

    @Column(name = "previous_description_html", columnDefinition = "TEXT")
    private String previousDescriptionHtml;

    @Column(name = "published_description_html", nullable = false, columnDefinition = "TEXT")
    private String publishedDescriptionHtml;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private DescriptionVersionAction action;

    @Column(name = "restored_from_version_id")
    private Long restoredFromVersionId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected DescriptionVersionEntity() {
    }

    DescriptionVersionEntity(
            String productId,
            String handle,
            String previousDescriptionHtml,
            String publishedDescriptionHtml,
            DescriptionVersionAction action,
            Long restoredFromVersionId,
            Instant createdAt
    ) {
        this.productId = productId;
        this.handle = handle;
        this.previousDescriptionHtml = previousDescriptionHtml;
        this.publishedDescriptionHtml = publishedDescriptionHtml;
        this.action = action;
        this.restoredFromVersionId = restoredFromVersionId;
        this.createdAt = createdAt;
    }

    static DescriptionVersionEntity publish(
            String productId,
            String handle,
            String previousDescriptionHtml,
            String publishedDescriptionHtml
    ) {
        return new DescriptionVersionEntity(
                productId,
                handle,
                previousDescriptionHtml,
                publishedDescriptionHtml,
                DescriptionVersionAction.PUBLISH,
                null,
                Instant.now()
        );
    }

    static DescriptionVersionEntity restore(
            String productId,
            String handle,
            String previousDescriptionHtml,
            String publishedDescriptionHtml,
            Long restoredFromVersionId
    ) {
        return new DescriptionVersionEntity(
                productId,
                handle,
                previousDescriptionHtml,
                publishedDescriptionHtml,
                DescriptionVersionAction.RESTORE,
                restoredFromVersionId,
                Instant.now()
        );
    }

    public Long getId() {
        return id;
    }

    public String getProductId() {
        return productId;
    }

    public String getHandle() {
        return handle;
    }

    public String getPreviousDescriptionHtml() {
        return previousDescriptionHtml;
    }

    public String getPublishedDescriptionHtml() {
        return publishedDescriptionHtml;
    }

    public DescriptionVersionAction getAction() {
        return action;
    }

    public Long getRestoredFromVersionId() {
        return restoredFromVersionId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
