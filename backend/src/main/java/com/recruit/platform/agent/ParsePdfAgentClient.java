package com.recruit.platform.agent;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.recruit.platform.config.AppAgentProperties;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
class ParsePdfAgentClient {

    private final ObjectMapper objectMapper;
    private final AppAgentProperties agentProperties;

    <T> T post(String path, Object body, Class<T> responseType) {
        HttpClient httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofMillis(agentProperties.connectTimeoutMs()))
                .build();
        try {
            String payload = objectMapper.writeValueAsString(body);
            String url = agentProperties.serviceBaseUrl() + path;
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofMillis(agentProperties.readTimeoutMs()))
                    .header("Content-Type", "application/json; charset=UTF-8")
                    .header("Accept", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofByteArray(payload.getBytes(StandardCharsets.UTF_8)))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalStateException(
                        "Agent service returned status " + response.statusCode()
                                + ", url=" + url
                                + ", payload=" + payload
                                + ", response=" + response.body()
                );
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

    <T> T postBinary(String path, byte[] body, Map<String, String> headers, Class<T> responseType) {
        HttpClient httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofMillis(agentProperties.connectTimeoutMs()))
                .build();
        try {
            String url = agentProperties.serviceBaseUrl() + path;
            HttpRequest.Builder builder = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofMillis(agentProperties.readTimeoutMs()))
                    .header("Content-Type", "application/octet-stream")
                    .header("Accept", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofByteArray(body));

            if (headers != null) {
                headers.forEach((key, value) -> {
                    if (value != null) {
                        builder.header(key, value);
                    }
                });
            }

            HttpResponse<String> response = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalStateException(
                        "Agent binary service returned status " + response.statusCode()
                                + ", url=" + url
                                + ", response=" + response.body()
                );
            }
            return objectMapper.readValue(response.body(), responseType);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to deserialize binary agent payload", exception);
        } catch (IOException exception) {
            throw new IllegalStateException("Agent binary service unavailable", exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Agent binary request interrupted", exception);
        }
    }
}
