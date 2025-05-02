/*
 * Copyright 2023 the original author or authors.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.cuioss.test.mockwebserver.mockresponse;

import de.cuioss.test.mockwebserver.EnableMockWebServer;
import de.cuioss.test.mockwebserver.URIBuilder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Test case to demonstrate context-aware MockResponse annotation processing.
 * Each test method should only have access to its own annotations and the class-level ones.
 */
@EnableMockWebServer
@MockResponse(path = "/api/class-level", status = 200, textContent = "Class Level Response")
class ContextAwareMockResponseTest {

    private static final Duration TIMEOUT = Duration.ofSeconds(5);
    private static final String STATUS_CODE_ASSERTION_MESSAGE = "Response status code should match";
    private static final String BODY_CONTENT_ASSERTION_MESSAGE = "Response body content should match";

    @Test
    @DisplayName("Should only access method A and class-level responses")
    @MockResponse(path = "/api/method-a", status = 200, textContent = "Method A Response")
    void shouldOnlyAccessMethodAAndClassLevelResponses(URIBuilder uriBuilder) throws IOException, InterruptedException {
        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(TIMEOUT)
                .build();

        // Test that /api/class-level works
        URI classLevelUri = uriBuilder.setPath("/api/class-level").build();
        HttpRequest classLevelRequest = HttpRequest.newBuilder()
                .uri(classLevelUri)
                .GET()
                .build();
        HttpResponse<String> classLevelResponse = client.send(classLevelRequest, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, classLevelResponse.statusCode(), STATUS_CODE_ASSERTION_MESSAGE);
        assertEquals("Class Level Response", classLevelResponse.body(), BODY_CONTENT_ASSERTION_MESSAGE);

        // Test that /api/method-a works
        URI methodAUri = uriBuilder.setPath("/api/method-a").build();
        HttpRequest methodARequest = HttpRequest.newBuilder()
                .uri(methodAUri)
                .GET()
                .build();
        HttpResponse<String> methodAResponse = client.send(methodARequest, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, methodAResponse.statusCode(), STATUS_CODE_ASSERTION_MESSAGE);
        assertEquals("Method A Response", methodAResponse.body(), BODY_CONTENT_ASSERTION_MESSAGE);

        // Test that /api/method-b does NOT work (should return 404)
        URI methodBUri = uriBuilder.setPath("/api/method-b").build();
        HttpRequest methodBRequest = HttpRequest.newBuilder()
                .uri(methodBUri)
                .GET()
                .build();
        HttpResponse<String> methodBResponse = client.send(methodBRequest, HttpResponse.BodyHandlers.ofString());
        assertEquals(418, methodBResponse.statusCode(), "Method B response should not be accessible from Method A test");
    }

    @Test
    @DisplayName("Should only access method B and class-level responses")
    @MockResponse(path = "/api/method-b", status = 200, textContent = "Method B Response")
    void shouldOnlyAccessMethodBAndClassLevelResponses(URIBuilder uriBuilder) throws IOException, InterruptedException {
        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(TIMEOUT)
                .build();

        // Test that /api/class-level works
        URI classLevelUri = uriBuilder.setPath("/api/class-level").build();
        HttpRequest classLevelRequest = HttpRequest.newBuilder()
                .uri(classLevelUri)
                .GET()
                .build();
        HttpResponse<String> classLevelResponse = client.send(classLevelRequest, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, classLevelResponse.statusCode(), STATUS_CODE_ASSERTION_MESSAGE);
        assertEquals("Class Level Response", classLevelResponse.body(), BODY_CONTENT_ASSERTION_MESSAGE);

        // Test that /api/method-b works
        URI methodBUri = uriBuilder.setPath("/api/method-b").build();
        HttpRequest methodBRequest = HttpRequest.newBuilder()
                .uri(methodBUri)
                .GET()
                .build();
        HttpResponse<String> methodBResponse = client.send(methodBRequest, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, methodBResponse.statusCode(), STATUS_CODE_ASSERTION_MESSAGE);
        assertEquals("Method B Response", methodBResponse.body(), BODY_CONTENT_ASSERTION_MESSAGE);

        // Test that /api/method-a does NOT work (should return 404)
        URI methodAUri = uriBuilder.setPath("/api/method-a").build();
        HttpRequest methodARequest = HttpRequest.newBuilder()
                .uri(methodAUri)
                .GET()
                .build();
        HttpResponse<String> methodAResponse = client.send(methodARequest, HttpResponse.BodyHandlers.ofString());
        assertEquals(418, methodAResponse.statusCode(), "Method A response should not be accessible from Method B test");
    }

    @Nested
    @DisplayName("Nested test class with @MockResponse")
    @MockResponse(path = "/api/nested-class", status = 200, textContent = "Nested Class Response")
    class NestedTest {
        @Test
        @DisplayName("Should only access nested method and parent responses")
        @MockResponse(path = "/api/nested-method", status = 200, textContent = "Nested Method Response")
        void shouldOnlyAccessNestedMethodAndParentResponses(URIBuilder uriBuilder) throws IOException, InterruptedException {
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(TIMEOUT)
                    .build();

            // Test that /api/class-level works
            URI classLevelUri = uriBuilder.setPath("/api/class-level").build();
            HttpRequest classLevelRequest = HttpRequest.newBuilder()
                    .uri(classLevelUri)
                    .GET()
                    .build();
            HttpResponse<String> classLevelResponse = client.send(classLevelRequest, HttpResponse.BodyHandlers.ofString());
            assertEquals(200, classLevelResponse.statusCode(), STATUS_CODE_ASSERTION_MESSAGE);
            assertEquals("Class Level Response", classLevelResponse.body(), BODY_CONTENT_ASSERTION_MESSAGE);

            // Test that /api/nested-class works
            URI nestedClassUri = uriBuilder.setPath("/api/nested-class").build();
            HttpRequest nestedClassRequest = HttpRequest.newBuilder()
                    .uri(nestedClassUri)
                    .GET()
                    .build();
            HttpResponse<String> nestedClassResponse = client.send(nestedClassRequest, HttpResponse.BodyHandlers.ofString());
            assertEquals(200, nestedClassResponse.statusCode(), STATUS_CODE_ASSERTION_MESSAGE);
            assertEquals("Nested Class Response", nestedClassResponse.body(), BODY_CONTENT_ASSERTION_MESSAGE);

            // Test that /api/nested-method works
            URI nestedMethodUri = uriBuilder.setPath("/api/nested-method").build();
            HttpRequest nestedMethodRequest = HttpRequest.newBuilder()
                    .uri(nestedMethodUri)
                    .GET()
                    .build();
            HttpResponse<String> nestedMethodResponse = client.send(nestedMethodRequest, HttpResponse.BodyHandlers.ofString());
            assertEquals(200, nestedMethodResponse.statusCode(), STATUS_CODE_ASSERTION_MESSAGE);
            assertEquals("Nested Method Response", nestedMethodResponse.body(), BODY_CONTENT_ASSERTION_MESSAGE);

            // Test that /api/method-a and /api/method-b do NOT work (should return 404)
            URI methodAUri = uriBuilder.setPath("/api/method-a").build();
            HttpRequest methodARequest = HttpRequest.newBuilder()
                    .uri(methodAUri)
                    .GET()
                    .build();
            HttpResponse<String> methodAResponse = client.send(methodARequest, HttpResponse.BodyHandlers.ofString());
            assertEquals(418, methodAResponse.statusCode(), "Method A response should not be accessible from nested test");

            URI methodBUri = uriBuilder.setPath("/api/method-b").build();
            HttpRequest methodBRequest = HttpRequest.newBuilder()
                    .uri(methodBUri)
                    .GET()
                    .build();
            HttpResponse<String> methodBResponse = client.send(methodBRequest, HttpResponse.BodyHandlers.ofString());
            assertEquals(418, methodBResponse.statusCode(), "Method B response should not be accessible from nested test");
        }
    }
}
