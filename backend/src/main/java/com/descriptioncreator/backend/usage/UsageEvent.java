package com.descriptioncreator.backend.usage;

import com.descriptioncreator.backend.auth.AppUserEntity;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "usage_event", indexes = {
        @Index(name = "idx_usage_event_user_created", columnList = "user_id,created_at")
})
public class UsageEvent {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(optional = false, fetch = FetchType.LAZY) @JoinColumn(name = "user_id", nullable = false)
    private AppUserEntity user;
    @Enumerated(EnumType.STRING) @Column(name = "event_type", nullable = false)
    private EventType eventType;
    @Column(name = "product_id") private String productId;
    private String handle;
    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;

    protected UsageEvent() {}
    public UsageEvent(AppUserEntity user, String productId, String handle) {
        this.user = user;
        this.eventType = EventType.DESCRIPTION_GENERATED;
        this.productId = productId;
        this.handle = handle;
        this.createdAt = Instant.now();
    }
    public enum EventType { DESCRIPTION_GENERATED }
}
