package com.descriptioncreator.backend.usage;

import com.descriptioncreator.backend.auth.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.time.Instant;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:usage_tests;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "app.usage.monthly-generation-limit=2"
})
@Transactional
class UsageServiceTests {
    @Autowired UsageService usage;
    @Autowired UsageEventRepository events;
    @Autowired AppUserRepository users;
    @Autowired PasswordEncoder encoder;

    @Test void countsCurrentUtcMonthPerUserAndNeverReturnsNegativeRemaining() {
        AppUserEntity first = user("first@example.com");
        AppUserEntity second = user("second@example.com");
        usage.recordGeneration(first.getEmail(), "p1", "one");
        usage.recordGeneration(first.getEmail(), "p2", "two");
        usage.recordGeneration(second.getEmail(), "p3", "three");
        UsageEvent old = new UsageEvent(first, "old", "old");
        ReflectionTestUtils.setField(old, "createdAt", Instant.parse("2020-01-01T00:00:00Z"));
        events.save(old);

        UsageService.UsageStatus status = usage.status(first.getEmail());
        assertThat(status.used()).isEqualTo(2);
        assertThat(status.limit()).isEqualTo(2);
        assertThat(status.remaining()).isZero();
        assertThat(status.period()).matches("\\d{4}-\\d{2}");
        assertThatThrownBy(() -> usage.assertGenerationAllowed(first.getEmail()))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        exception -> assertThat(exception.getStatusCode().value()).isEqualTo(429));
        assertThat(usage.status(second.getEmail()).used()).isEqualTo(1);
    }

    private AppUserEntity user(String email) {
        return users.save(new AppUserEntity(email, encoder.encode("password"), null));
    }
}
