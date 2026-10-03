package com.descriptioncreator.backend.usage;

import org.springframework.data.jpa.repository.JpaRepository;
import java.time.Instant;

public interface UsageEventRepository extends JpaRepository<UsageEvent, Long> {
    long countByUserIdAndEventTypeAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(
            Long userId, UsageEvent.EventType eventType, Instant start, Instant end);
}
