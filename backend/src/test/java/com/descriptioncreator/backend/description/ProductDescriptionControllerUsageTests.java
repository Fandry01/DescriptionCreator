package com.descriptioncreator.backend.description;

import com.descriptioncreator.backend.generator.ProductFacts;
import com.descriptioncreator.backend.usage.UsageService;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.*;

class ProductDescriptionControllerUsageTests {
    private final UsernamePasswordAuthenticationToken authentication =
            UsernamePasswordAuthenticationToken.authenticated("user@example.com", "", java.util.List.of());

    @Test void successfulGenerationRecordsExactlyOneEvent() {
        TrackingDescriptionService descriptions = new TrackingDescriptionService(false);
        TrackingUsageService usage = new TrackingUsageService(false);
        ProductDescriptionController controller = new ProductDescriptionController(descriptions, usage);

        controller.generateDescription("bag", authentication);

        assertThat(descriptions.generateCalls).isEqualTo(1);
        assertThat(usage.recordCalls).isEqualTo(1);
    }

    @Test void failedGenerationRecordsNoEvent() {
        TrackingDescriptionService descriptions = new TrackingDescriptionService(true);
        TrackingUsageService usage = new TrackingUsageService(false);
        ProductDescriptionController controller = new ProductDescriptionController(descriptions, usage);

        assertThatThrownBy(() -> controller.generateDescription("bag", authentication))
                .isInstanceOf(IllegalStateException.class);
        assertThat(usage.recordCalls).isZero();
    }

    @Test void limitRejectionDoesNotCallGenerationOrRecordUsage() {
        TrackingDescriptionService descriptions = new TrackingDescriptionService(false);
        TrackingUsageService usage = new TrackingUsageService(true);
        ProductDescriptionController controller = new ProductDescriptionController(descriptions, usage);

        assertThatThrownBy(() -> controller.generateDescription("bag", authentication))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        exception -> assertThat(exception.getStatusCode().value()).isEqualTo(429));
        assertThat(descriptions.generateCalls).isZero();
        assertThat(usage.recordCalls).isZero();
    }

    private static class TrackingUsageService extends UsageService {
        private final boolean reject;
        private int recordCalls;
        TrackingUsageService(boolean reject) { super(null, null, 500); this.reject = reject; }
        @Override public void assertGenerationAllowed(String email) {
            if (reject) throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS);
        }
        @Override public void recordGeneration(String email, String productId, String handle) {
            recordCalls++;
        }
    }

    private static class TrackingDescriptionService extends ProductDescriptionService {
        private final boolean fail;
        private int generateCalls;
        TrackingDescriptionService(boolean fail) { super(null, null, null, null); this.fail = fail; }
        @Override public ProductDescriptionDraftResponse generateDraft(String handle) {
            generateCalls++;
            if (fail) throw new IllegalStateException("OpenAI failed");
            return new ProductDescriptionDraftResponse("product-id", "Bag", handle, null,
                    "Generated", new ProductFacts("", "", "", "", "", "", "", "",
                    "", "", "", "", "", ""));
        }
    }
}
