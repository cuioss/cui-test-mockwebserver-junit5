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
 * Test case to verify that deeply nested classes properly handle @MockResponse annotations.
 */
@EnableMockWebServer
@MockResponse(path = "/api/outer-class", status = 200, textContent = "Outer Class Response")
class DeeplyNestedClassesTest {

    private static final Duration TIMEOUT = Duration.ofSeconds(5);
    private static final String STATUS_CODE_ASSERTION_MESSAGE = "Response status code should match";
    private static final String BODY_CONTENT_ASSERTION_MESSAGE = "Response body content should match";

    @Test
    @DisplayName("Outer class should only access its own annotations")
    @MockResponse(path = "/api/outer-method", status = 200, textContent = "Outer Method Response")
    void outerClassTest(URIBuilder uriBuilder) throws IOException, InterruptedException {
        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(TIMEOUT)
                .build();

        // Test that outer class annotation works
        URI outerClassUri = uriBuilder.setPath("/api/outer-class").build();
        HttpRequest outerClassRequest = HttpRequest.newBuilder()
                .uri(outerClassUri)
                .GET()
                .build();
        HttpResponse<String> outerClassResponse = client.send(outerClassRequest, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, outerClassResponse.statusCode(), STATUS_CODE_ASSERTION_MESSAGE);
        assertEquals("Outer Class Response", outerClassResponse.body(), BODY_CONTENT_ASSERTION_MESSAGE);

        // Test that outer method annotation works
        URI outerMethodUri = uriBuilder.setPath("/api/outer-method").build();
        HttpRequest outerMethodRequest = HttpRequest.newBuilder()
                .uri(outerMethodUri)
                .GET()
                .build();
        HttpResponse<String> outerMethodResponse = client.send(outerMethodRequest, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, outerMethodResponse.statusCode(), STATUS_CODE_ASSERTION_MESSAGE);
        assertEquals("Outer Method Response", outerMethodResponse.body(), BODY_CONTENT_ASSERTION_MESSAGE);

        // Test that nested class annotations are not accessible
        URI level1ClassUri = uriBuilder.setPath("/api/level1-class").build();
        HttpRequest level1ClassRequest = HttpRequest.newBuilder()
                .uri(level1ClassUri)
                .GET()
                .build();
        HttpResponse<String> level1ClassResponse = client.send(level1ClassRequest, HttpResponse.BodyHandlers.ofString());
        assertEquals(418, level1ClassResponse.statusCode(), "Level 1 class response should not be accessible from outer class");
    }

    @Nested
    @DisplayName("Level 1 nested class")
    @MockResponse(path = "/api/level1-class", status = 200, textContent = "Level 1 Class Response")
    class Level1NestedTest {

        @Test
        @DisplayName("Level 1 nested class should access its own annotations and outer class annotations")
        @MockResponse(path = "/api/level1-method", status = 200, textContent = "Level 1 Method Response")
        void level1Test(URIBuilder uriBuilder) throws IOException, InterruptedException {
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(TIMEOUT)
                    .build();

            // Test that outer class annotation is accessible
            URI outerClassUri = uriBuilder.setPath("/api/outer-class").build();
            HttpRequest outerClassRequest = HttpRequest.newBuilder()
                    .uri(outerClassUri)
                    .GET()
                    .build();
            HttpResponse<String> outerClassResponse = client.send(outerClassRequest, HttpResponse.BodyHandlers.ofString());
            assertEquals(200, outerClassResponse.statusCode(), STATUS_CODE_ASSERTION_MESSAGE);
            assertEquals("Outer Class Response", outerClassResponse.body(), BODY_CONTENT_ASSERTION_MESSAGE);

            // Test that level 1 class annotation works
            URI level1ClassUri = uriBuilder.setPath("/api/level1-class").build();
            HttpRequest level1ClassRequest = HttpRequest.newBuilder()
                    .uri(level1ClassUri)
                    .GET()
                    .build();
            HttpResponse<String> level1ClassResponse = client.send(level1ClassRequest, HttpResponse.BodyHandlers.ofString());
            assertEquals(200, level1ClassResponse.statusCode(), STATUS_CODE_ASSERTION_MESSAGE);
            assertEquals("Level 1 Class Response", level1ClassResponse.body(), BODY_CONTENT_ASSERTION_MESSAGE);

            // Test that level 1 method annotation works
            URI level1MethodUri = uriBuilder.setPath("/api/level1-method").build();
            HttpRequest level1MethodRequest = HttpRequest.newBuilder()
                    .uri(level1MethodUri)
                    .GET()
                    .build();
            HttpResponse<String> level1MethodResponse = client.send(level1MethodRequest, HttpResponse.BodyHandlers.ofString());
            assertEquals(200, level1MethodResponse.statusCode(), STATUS_CODE_ASSERTION_MESSAGE);
            assertEquals("Level 1 Method Response", level1MethodResponse.body(), BODY_CONTENT_ASSERTION_MESSAGE);

            // Test that outer method annotation is NOT accessible
            URI outerMethodUri = uriBuilder.setPath("/api/outer-method").build();
            HttpRequest outerMethodRequest = HttpRequest.newBuilder()
                    .uri(outerMethodUri)
                    .GET()
                    .build();
            HttpResponse<String> outerMethodResponse = client.send(outerMethodRequest, HttpResponse.BodyHandlers.ofString());
            assertEquals(418, outerMethodResponse.statusCode(), "Outer method response should not be accessible from level 1 class");

            // Test that level 2 class annotations are not accessible
            URI level2ClassUri = uriBuilder.setPath("/api/level2-class").build();
            HttpRequest level2ClassRequest = HttpRequest.newBuilder()
                    .uri(level2ClassUri)
                    .GET()
                    .build();
            HttpResponse<String> level2ClassResponse = client.send(level2ClassRequest, HttpResponse.BodyHandlers.ofString());
            assertEquals(418, level2ClassResponse.statusCode(), "Level 2 class response should not be accessible from level 1 class");
        }

        @Nested
        @DisplayName("Level 2 nested class")
        @MockResponse(path = "/api/level2-class", status = 200, textContent = "Level 2 Class Response")
        class Level2NestedTest {

            @Test
            @DisplayName("Level 2 nested class should access its own annotations and all ancestor class annotations")
            @MockResponse(path = "/api/level2-method", status = 200, textContent = "Level 2 Method Response")
            void level2Test(URIBuilder uriBuilder) throws IOException, InterruptedException {
                HttpClient client = HttpClient.newBuilder()
                        .connectTimeout(TIMEOUT)
                        .build();

                // Test that outer class annotation is accessible
                URI outerClassUri = uriBuilder.setPath("/api/outer-class").build();
                HttpRequest outerClassRequest = HttpRequest.newBuilder()
                        .uri(outerClassUri)
                        .GET()
                        .build();
                HttpResponse<String> outerClassResponse = client.send(outerClassRequest, HttpResponse.BodyHandlers.ofString());
                assertEquals(200, outerClassResponse.statusCode(), STATUS_CODE_ASSERTION_MESSAGE);
                assertEquals("Outer Class Response", outerClassResponse.body(), BODY_CONTENT_ASSERTION_MESSAGE);

                // Test that level 1 class annotation is accessible
                URI level1ClassUri = uriBuilder.setPath("/api/level1-class").build();
                HttpRequest level1ClassRequest = HttpRequest.newBuilder()
                        .uri(level1ClassUri)
                        .GET()
                        .build();
                HttpResponse<String> level1ClassResponse = client.send(level1ClassRequest, HttpResponse.BodyHandlers.ofString());
                assertEquals(200, level1ClassResponse.statusCode(), STATUS_CODE_ASSERTION_MESSAGE);
                assertEquals("Level 1 Class Response", level1ClassResponse.body(), BODY_CONTENT_ASSERTION_MESSAGE);

                // Test that level 2 class annotation works
                URI level2ClassUri = uriBuilder.setPath("/api/level2-class").build();
                HttpRequest level2ClassRequest = HttpRequest.newBuilder()
                        .uri(level2ClassUri)
                        .GET()
                        .build();
                HttpResponse<String> level2ClassResponse = client.send(level2ClassRequest, HttpResponse.BodyHandlers.ofString());
                assertEquals(200, level2ClassResponse.statusCode(), STATUS_CODE_ASSERTION_MESSAGE);
                assertEquals("Level 2 Class Response", level2ClassResponse.body(), BODY_CONTENT_ASSERTION_MESSAGE);

                // Test that level 2 method annotation works
                URI level2MethodUri = uriBuilder.setPath("/api/level2-method").build();
                HttpRequest level2MethodRequest = HttpRequest.newBuilder()
                        .uri(level2MethodUri)
                        .GET()
                        .build();
                HttpResponse<String> level2MethodResponse = client.send(level2MethodRequest, HttpResponse.BodyHandlers.ofString());
                assertEquals(200, level2MethodResponse.statusCode(), STATUS_CODE_ASSERTION_MESSAGE);
                assertEquals("Level 2 Method Response", level2MethodResponse.body(), BODY_CONTENT_ASSERTION_MESSAGE);

                // Test that outer method and level 1 method annotations are NOT accessible
                URI outerMethodUri = uriBuilder.setPath("/api/outer-method").build();
                HttpRequest outerMethodRequest = HttpRequest.newBuilder()
                        .uri(outerMethodUri)
                        .GET()
                        .build();
                HttpResponse<String> outerMethodResponse = client.send(outerMethodRequest, HttpResponse.BodyHandlers.ofString());
                assertEquals(418, outerMethodResponse.statusCode(), "Outer method response should not be accessible from level 2 class");

                URI level1MethodUri = uriBuilder.setPath("/api/level1-method").build();
                HttpRequest level1MethodRequest = HttpRequest.newBuilder()
                        .uri(level1MethodUri)
                        .GET()
                        .build();
                HttpResponse<String> level1MethodResponse = client.send(level1MethodRequest, HttpResponse.BodyHandlers.ofString());
                assertEquals(418, level1MethodResponse.statusCode(), "Level 1 method response should not be accessible from level 2 class");

                // Test that level 3 class annotations are not accessible
                URI level3ClassUri = uriBuilder.setPath("/api/level3-class").build();
                HttpRequest level3ClassRequest = HttpRequest.newBuilder()
                        .uri(level3ClassUri)
                        .GET()
                        .build();
                HttpResponse<String> level3ClassResponse = client.send(level3ClassRequest, HttpResponse.BodyHandlers.ofString());
                assertEquals(418, level3ClassResponse.statusCode(), "Level 3 class response should not be accessible from level 2 class");
            }

            @Nested
            @DisplayName("Level 3 nested class (deepest level)")
            @MockResponse(path = "/api/level3-class", status = 200, textContent = "Level 3 Class Response")
            class Level3NestedTest {

                @Test
                @DisplayName("Level 3 nested class should access its own annotations and all ancestor class annotations")
                @MockResponse(path = "/api/level3-method", status = 200, textContent = "Level 3 Method Response")
                void level3Test(URIBuilder uriBuilder) throws IOException, InterruptedException {
                    HttpClient client = HttpClient.newBuilder()
                            .connectTimeout(TIMEOUT)
                            .build();

                    // Test that outer class annotation is accessible
                    URI outerClassUri = uriBuilder.setPath("/api/outer-class").build();
                    HttpRequest outerClassRequest = HttpRequest.newBuilder()
                            .uri(outerClassUri)
                            .GET()
                            .build();
                    HttpResponse<String> outerClassResponse = client.send(outerClassRequest, HttpResponse.BodyHandlers.ofString());
                    assertEquals(200, outerClassResponse.statusCode(), STATUS_CODE_ASSERTION_MESSAGE);
                    assertEquals("Outer Class Response", outerClassResponse.body(), BODY_CONTENT_ASSERTION_MESSAGE);

                    // Test that level 1 class annotation is accessible
                    URI level1ClassUri = uriBuilder.setPath("/api/level1-class").build();
                    HttpRequest level1ClassRequest = HttpRequest.newBuilder()
                            .uri(level1ClassUri)
                            .GET()
                            .build();
                    HttpResponse<String> level1ClassResponse = client.send(level1ClassRequest, HttpResponse.BodyHandlers.ofString());
                    assertEquals(200, level1ClassResponse.statusCode(), STATUS_CODE_ASSERTION_MESSAGE);
                    assertEquals("Level 1 Class Response", level1ClassResponse.body(), BODY_CONTENT_ASSERTION_MESSAGE);

                    // Test that level 2 class annotation is accessible
                    URI level2ClassUri = uriBuilder.setPath("/api/level2-class").build();
                    HttpRequest level2ClassRequest = HttpRequest.newBuilder()
                            .uri(level2ClassUri)
                            .GET()
                            .build();
                    HttpResponse<String> level2ClassResponse = client.send(level2ClassRequest, HttpResponse.BodyHandlers.ofString());
                    assertEquals(200, level2ClassResponse.statusCode(), STATUS_CODE_ASSERTION_MESSAGE);
                    assertEquals("Level 2 Class Response", level2ClassResponse.body(), BODY_CONTENT_ASSERTION_MESSAGE);

                    // Test that level 3 class annotation works
                    URI level3ClassUri = uriBuilder.setPath("/api/level3-class").build();
                    HttpRequest level3ClassRequest = HttpRequest.newBuilder()
                            .uri(level3ClassUri)
                            .GET()
                            .build();
                    HttpResponse<String> level3ClassResponse = client.send(level3ClassRequest, HttpResponse.BodyHandlers.ofString());
                    assertEquals(200, level3ClassResponse.statusCode(), STATUS_CODE_ASSERTION_MESSAGE);
                    assertEquals("Level 3 Class Response", level3ClassResponse.body(), BODY_CONTENT_ASSERTION_MESSAGE);

                    // Test that level 3 method annotation works
                    URI level3MethodUri = uriBuilder.setPath("/api/level3-method").build();
                    HttpRequest level3MethodRequest = HttpRequest.newBuilder()
                            .uri(level3MethodUri)
                            .GET()
                            .build();
                    HttpResponse<String> level3MethodResponse = client.send(level3MethodRequest, HttpResponse.BodyHandlers.ofString());
                    assertEquals(200, level3MethodResponse.statusCode(), STATUS_CODE_ASSERTION_MESSAGE);
                    assertEquals("Level 3 Method Response", level3MethodResponse.body(), BODY_CONTENT_ASSERTION_MESSAGE);

                    // Test that all method annotations from ancestor classes are NOT accessible
                    URI outerMethodUri = uriBuilder.setPath("/api/outer-method").build();
                    HttpRequest outerMethodRequest = HttpRequest.newBuilder()
                            .uri(outerMethodUri)
                            .GET()
                            .build();
                    HttpResponse<String> outerMethodResponse = client.send(outerMethodRequest, HttpResponse.BodyHandlers.ofString());
                    assertEquals(418, outerMethodResponse.statusCode(), "Outer method response should not be accessible from level 3 class");

                    URI level1MethodUri = uriBuilder.setPath("/api/level1-method").build();
                    HttpRequest level1MethodRequest = HttpRequest.newBuilder()
                            .uri(level1MethodUri)
                            .GET()
                            .build();
                    HttpResponse<String> level1MethodResponse = client.send(level1MethodRequest, HttpResponse.BodyHandlers.ofString());
                    assertEquals(418, level1MethodResponse.statusCode(), "Level 1 method response should not be accessible from level 3 class");

                    URI level2MethodUri = uriBuilder.setPath("/api/level2-method").build();
                    HttpRequest level2MethodRequest = HttpRequest.newBuilder()
                            .uri(level2MethodUri)
                            .GET()
                            .build();
                    HttpResponse<String> level2MethodResponse = client.send(level2MethodRequest, HttpResponse.BodyHandlers.ofString());
                    assertEquals(418, level2MethodResponse.statusCode(), "Level 2 method response should not be accessible from level 3 class");
                }
            }
        }
    }
}