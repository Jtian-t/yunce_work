package com.recruit.platform.agent;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.recruit.platform.config.AppAgentProperties;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
class ParsePdfAgentClient {

    private final ObjectMapper objectMapper;
    private final AppAgentProperties agentProperties;

    <T> T post(String path, Object body, Class<T> responseType) {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(agentProperties.connectTimeoutMs()))
                .build();
        try {
            String payload = objectMapper.writeValueAsString(body);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(agentProperties.serviceBaseUrl() + path))
                    .timeout(Duration.ofMillis(agentProperties.readTimeoutMs()))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(payload))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalStateException("Agent service returned status " + response.statusCode() + ": " + response.body());
            }
            return objectMapper.readValue(response.body(), responseType);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to serialize/deserialize agent payload", exception);
        } catch (IOException exception) {
            throw new IllegalStateException("Agent service unavailable", exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Agent request interrupted", exception);
        }
    }
}
