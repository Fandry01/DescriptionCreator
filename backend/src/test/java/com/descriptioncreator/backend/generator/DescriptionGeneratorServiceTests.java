package com.descriptioncreator.backend.generator;

import com.descriptioncreator.backend.openai.OpenAiClient;
import com.descriptioncreator.backend.openai.OpenAiException;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DescriptionGeneratorServiceTests {

    private final PromptBuilder promptBuilder = new PromptBuilder();
    private final ProductFacts productFacts = new ProductFacts(
            "Gucci", "Jackie 1961", "Black", "2", "Excellent Condition",
            "Leather", "Gold-tone", "Dust bag", "", "", "", "", "2022", ""
    );

    @Test
    void buildsAndSendsPromptToOpenAi() {
        AtomicReference<String> receivedPrompt = new AtomicReference<>();
        OpenAiClient openAiClient = prompt -> {
            receivedPrompt.set(prompt);
            return "Generated description";
        };
        DescriptionGeneratorService service =
                new DescriptionGeneratorService(promptBuilder, openAiClient);

        service.generate("Gucci Jackie 1961", productFacts);

        assertThat(receivedPrompt.get())
                .isEqualTo(promptBuilder.build("Gucci Jackie 1961", productFacts));
    }

    @Test
    void trimsGeneratedDescription() {
        DescriptionGeneratorService service = new DescriptionGeneratorService(
                promptBuilder,
                prompt -> "  A polished description.\n"
        );

        assertThat(service.generate("Gucci Jackie 1961", productFacts))
                .isEqualTo("A polished description.");
    }

    @Test
    void doesNotAppendConditionPhrasesToGeneratedText() {
        DescriptionGeneratorService service = new DescriptionGeneratorService(
                promptBuilder,
                prompt -> "Editorial description only."
        );

        assertThat(service.generate("Gucci Jackie 1961", productFacts))
                .isEqualTo("Editorial description only.")
                .doesNotContain("Excellent Condition", "condition", "signs of wear");
    }

    @Test
    void rejectsBlankGeneratedDescription() {
        DescriptionGeneratorService service = new DescriptionGeneratorService(
                promptBuilder,
                prompt -> "   \n"
        );

        assertThatThrownBy(() -> service.generate("Gucci Jackie 1961", productFacts))
                .isInstanceOf(OpenAiException.class)
                .hasMessage("OpenAI returned an empty description");
    }

    @Test
    void propagatesOpenAiFailure() {
        OpenAiException failure = new OpenAiException("OpenAI Responses API request failed");
        DescriptionGeneratorService service = new DescriptionGeneratorService(
                promptBuilder,
                prompt -> {
                    throw failure;
                }
        );

        assertThatThrownBy(() -> service.generate("Gucci Jackie 1961", productFacts))
                .isSameAs(failure);
    }
}
