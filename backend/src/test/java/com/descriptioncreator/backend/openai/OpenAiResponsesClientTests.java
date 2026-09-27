package com.descriptioncreator.backend.openai;

import com.descriptioncreator.backend.common.config.OpenAiConfig;
import com.descriptioncreator.backend.common.config.OpenAiProperties;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;

class OpenAiResponsesClientTests {

    @Test
    void sendsResponsesApiRequestAndParsesOutputText() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        OpenAiProperties properties = new OpenAiProperties("test-api-key", "gpt-5-mini");
        RestClient restClient = new OpenAiConfig().buildOpenAiRestClient(builder, properties);
        OpenAiResponsesClient client = new OpenAiResponsesClient(restClient, properties);

        server.expect(requestTo("https://api.openai.com/v1/responses"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer test-api-key"))
                .andExpect(content().string(containsString("\"model\":\"gpt-5-mini\"")))
                .andExpect(content().string(containsString("\"input\":\"Write one paragraph\"")))
                .andExpect(content().string(not(containsString("\"reasoning\""))))
                .andRespond(withSuccess("""
                        {
                          "id": "resp_test",
                          "output": [
                            {"type": "reasoning", "content": []},
                            {
                              "type": "message",
                              "content": [
                                {
                                  "type": "output_text",
                                  "text": "  Generated description  "
                                }
                              ]
                            }
                          ]
                        }
                        """, MediaType.APPLICATION_JSON));

        assertThat(client.generate("Write one paragraph"))
                .isEqualTo("Generated description");
        server.verify();
    }

    @Test
    void rejectsMissingApiKeyBeforeRequest() {
        OpenAiProperties properties = new OpenAiProperties(" ", "gpt-5-mini");
        OpenAiResponsesClient client = new OpenAiResponsesClient(
                RestClient.create("https://api.openai.com/v1"),
                properties
        );

        assertThatThrownBy(() -> client.generate("Prompt"))
                .isInstanceOf(OpenAiException.class)
                .hasMessage("OPENAI_API_KEY is not configured");
    }

    @Test
    void wrapsResponsesApiFailure() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        OpenAiProperties properties = new OpenAiProperties("test-api-key", "gpt-5-mini");
        RestClient restClient = new OpenAiConfig().buildOpenAiRestClient(builder, properties);
        OpenAiResponsesClient client = new OpenAiResponsesClient(restClient, properties);

        server.expect(requestTo("https://api.openai.com/v1/responses"))
                .andRespond(withServerError());

        assertThatThrownBy(() -> client.generate("Prompt"))
                .isInstanceOf(OpenAiException.class)
                .hasMessage("OpenAI Responses API request failed");
        server.verify();
    }

    @Test
    void rejectsBlankOutputText() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        OpenAiProperties properties = new OpenAiProperties("test-api-key", "gpt-5-mini");
        RestClient restClient = new OpenAiConfig().buildOpenAiRestClient(builder, properties);
        OpenAiResponsesClient client = new OpenAiResponsesClient(restClient, properties);

        server.expect(requestTo("https://api.openai.com/v1/responses"))
                .andRespond(withSuccess("""
                        {
                          "output": [{
                            "type": "message",
                            "content": [{"type": "output_text", "text": "   "}]
                          }]
                        }
                        """, MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> client.generate("Prompt"))
                .isInstanceOf(OpenAiException.class)
                .hasMessage("OpenAI returned an empty description");
        server.verify();
    }
}
