package com.descriptioncreator.backend.generator;

import com.descriptioncreator.backend.openai.OpenAiClient;
import com.descriptioncreator.backend.openai.OpenAiException;
import org.springframework.stereotype.Service;

@Service
public class DescriptionGeneratorService {

    private final PromptBuilder promptBuilder;
    private final OpenAiClient openAiClient;

    public DescriptionGeneratorService(
            PromptBuilder promptBuilder,
            OpenAiClient openAiClient
    ) {
        this.promptBuilder = promptBuilder;
        this.openAiClient = openAiClient;
    }

    public String generate(String title, ProductFacts productFacts) {
        String prompt = promptBuilder.build(title, productFacts);
        String description = openAiClient.generate(prompt);

        if (description == null || description.isBlank()) {
            throw new OpenAiException("OpenAI returned an empty description");
        }
        return description.trim();
    }
}
