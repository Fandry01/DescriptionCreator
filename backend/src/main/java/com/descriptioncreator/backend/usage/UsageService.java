package com.descriptioncreator.backend.usage;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.descriptioncreator.backend.auth.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import java.time.*;

@Service
public class UsageService {
    private final UsageEventRepository events;
    private final AppUserRepository users;
    private final long limit;

    public UsageService(UsageEventRepository events, AppUserRepository users,
            @Value("${app.usage.monthly-generation-limit:500}") long limit) {
        this.events = events;
        this.users = users;
        this.limit = Math.max(0, limit);
    }

    public UsageStatus status(String email) {
        AppUserEntity user = user(email);
        YearMonth month = YearMonth.now(ZoneOffset.UTC);
        Instant start = month.atDay(1).atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant end = month.plusMonths(1).atDay(1).atStartOfDay(ZoneOffset.UTC).toInstant();
        long used = events.countByUserIdAndEventTypeAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(
                user.getId(), UsageEvent.EventType.DESCRIPTION_GENERATED, start, end);
        return new UsageStatus(used, limit, Math.max(0, limit - used), month.toString());
    }

    public void assertGenerationAllowed(String email) {
        if (!status(email).allowed()) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                    "Monthly description generation limit reached");
        }
    }

    public void recordGeneration(String email, String productId, String handle) {
        events.save(new UsageEvent(user(email), productId, handle));
    }

    private AppUserEntity user(String email) {
        return users.findByEmailIgnoreCase(AppUserEntity.normalizeEmail(email))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
    }

    public record UsageStatus(long used, long limit, long remaining, String period) {
        @JsonIgnore public boolean allowed() { return used < limit; }
    }
}
