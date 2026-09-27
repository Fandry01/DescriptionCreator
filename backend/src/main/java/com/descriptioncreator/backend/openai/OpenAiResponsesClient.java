package com.descriptioncreator.backend.openai;

import com.descriptioncreator.backend.common.config.OpenAiProperties;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class OpenAiResponsesClient implements OpenAiClient {

    private final RestClient restClient;
    private final OpenAiProperties properties;

    public OpenAiResponsesClient(
            @Qualifier("openAiRestClient") RestClient restClient,
            OpenAiProperties properties
    ) {
        this.restClient = restClient;
        this.properties = properties;
    }

    @Override
    public String generate(String prompt) {
        validateConfiguration();

        OpenAiResponse response;
        try {
            response = restClient.post()
                    .uri("/responses")
                    .body(requestBody(prompt))
                    .retrieve()
                    .body(OpenAiResponse.class);
        } catch (RestClientException exception) {
            throw new OpenAiException("OpenAI Responses API request failed", exception);
        }

        if (response == null) {
            throw new OpenAiException("OpenAI Responses API returned an empty response");
        }

        String outputText = response.outputText().trim();
        if (outputText.isBlank()) {
            throw new OpenAiException("OpenAI returned an empty description");
        }
        return outputText;
    }

    private Map<String, Object> requestBody(String prompt) {
        return Map.of(
                "model", properties.model().trim(),
                "input", prompt
        );
    }

    private void validateConfiguration() {
        if (properties.apiKey() == null || properties.apiKey().isBlank()) {
            throw new OpenAiException("OPENAI_API_KEY is not configured");
        }
        if (properties.model() == null || properties.model().isBlank()) {
            throw new OpenAiException("OPENAI_MODEL is not configured");
        }
    }

    record OpenAiResponse(List<OutputItem> output) {

        String outputText() {
            if (output == null) {
                return "";
            }
            return output.stream()
                    .filter(item -> "message".equals(item.type()) && item.content() != null)
                    .flatMap(item -> item.content().stream())
                    .filter(content -> "output_text".equals(content.type()))
                    .map(Content::text)
                    .filter(text -> text != null && !text.isBlank())
                    .collect(Collectors.joining("\n"));
        }
    }

    record OutputItem(String type, List<Content> content) {
    }

    record Content(String type, String text) {
    }
}
