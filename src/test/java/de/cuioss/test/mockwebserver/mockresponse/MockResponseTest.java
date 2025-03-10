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
import de.cuioss.test.mockwebserver.dispatcher.HttpMethodMapper;
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
 * Tests for the {@link MockResponse} annotation.
 *
 * @author Oliver Wolff
 */
@DisplayName("Tests for the @MockResponse annotation")
class MockResponseTest {

    // Constants to avoid duplication
    private static final String API_USERS_PATH = "/api/users";
    private static final String CONTENT_TYPE_HEADER = "Content-Type";
    private static final Duration TIMEOUT = Duration.ofSeconds(10);

    /**
     * Tests basic functionality of the @MockResponse annotation.
     */
    @Nested
    @EnableMockWebServer
    @MockResponse(path = "/api/users", status = 200, textContent = "Hello, World!")
    @DisplayName("Basic @MockResponse functionality")
    class BasicMockResponseTest {

        @Test
        void shouldReturnConfiguredResponse(URIBuilder uriBuilder) throws IOException, InterruptedException {
            // Arrange
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(TIMEOUT)
                    .build();
            URI uri = uriBuilder.addPathSegment(API_USERS_PATH).build();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(uri)
                    .GET()
                    .build();

            // Act
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            // Assert
            assertEquals(200, response.statusCode());
            assertEquals("Hello, World!", response.body());
            assertEquals("text/plain", response.headers().firstValue(CONTENT_TYPE_HEADER).orElse(null));
        }
    }

    /**
     * Tests multiple @MockResponse annotations on a class.
     */
    @Nested
    @EnableMockWebServer
    @MockResponse(path = "/api/users", method = HttpMethodMapper.GET, status = 200,
            jsonContent = "users=[]", contentType = "application/json; charset=utf-8")
    @MockResponse(path = "/api/users", method = HttpMethodMapper.POST, status = 201)
    @DisplayName("Multiple @MockResponse annotations")
    class MultipleMockResponseTest {

        @Test
        void shouldHandleGetRequest(URIBuilder uriBuilder) throws IOException, InterruptedException {
            // Arrange
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(TIMEOUT)
                    .build();
            URI uri = uriBuilder.addPathSegment(API_USERS_PATH).build();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(uri)
                    .GET()
                    .build();

            // Act
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            // Assert
            assertEquals(200, response.statusCode());
            assertEquals("{\"users\":[]}", response.body());
            assertEquals("application/json; charset=utf-8", response.headers().firstValue(CONTENT_TYPE_HEADER).orElse(null));
        }

        @Test
        void shouldHandlePostRequest(URIBuilder uriBuilder) throws IOException, InterruptedException {
            // Arrange
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(TIMEOUT)
                    .build();
            URI uri = uriBuilder.addPathSegment(API_USERS_PATH).build();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(uri)
                    .POST(HttpRequest.BodyPublishers.noBody())
                    .build();

            // Act
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            // Assert
            assertEquals(201, response.statusCode());
        }
    }

    /**
     * Tests @MockResponse annotations with custom headers.
     */
    @Nested
    @EnableMockWebServer
    @MockResponse(
            path = "/api/data",
            status = 200,
            jsonContent = "key1=value1,key2=value2",
            headers = {"X-Custom-Header=Custom Value", "Cache-Control=no-cache"}
    )
    @DisplayName("@MockResponse with custom headers")
    class HeadersMockResponseTest {

        @Test
        void shouldIncludeCustomHeaders(URIBuilder uriBuilder) throws IOException, InterruptedException {
            // Arrange
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(TIMEOUT)
                    .build();
            URI uri = uriBuilder.addPathSegment("/api/data").build();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(uri)
                    .GET()
                    .build();

            // Act
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            // Assert
            assertEquals(200, response.statusCode());
            assertEquals("{\"key1\":\"value1\",\"key2\":\"value2\"}", response.body());
            assertEquals("Custom Value", response.headers().firstValue("X-Custom-Header").orElse(null));
            assertEquals("no-cache", response.headers().firstValue("Cache-Control").orElse(null));
            assertEquals("application/json", response.headers().firstValue(CONTENT_TYPE_HEADER).orElse(null));
        }
    }

    /**
     * Tests @MockResponse annotations on nested test classes.
     */
    @EnableMockWebServer
    @Nested
    @DisplayName("@MockResponse on nested test classes")
    class NestedMockResponseTest {

        @Nested
        @MockResponse(path = "/nested", status = 200, textContent = "Nested Response")
        @DisplayName("Nested test class with @MockResponse")
        class NestedTest {

            @Test
            void shouldHandleNestedClassAnnotation(URIBuilder uriBuilder) throws IOException, InterruptedException {
                // Arrange
                HttpClient client = HttpClient.newBuilder()
                        .connectTimeout(TIMEOUT)
                        .build();
                URI uri = uriBuilder.addPathSegment("/nested").build();
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(uri)
                        .GET()
                        .build();

                // Act
                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

                // Assert
                assertEquals(200, response.statusCode());
                assertEquals("Nested Response", response.body());
            }
        }
    }

    /**
     * Tests @MockResponse annotations on test methods.
     */
    @Nested
    @EnableMockWebServer
    @DisplayName("@MockResponse on test methods")
    class MethodMockResponseTest {

        @Test
        @MockResponse(path = "/method", status = 200, textContent = "Method Response")
        void shouldHandleMethodAnnotation(URIBuilder uriBuilder) throws IOException, InterruptedException {
            // Arrange
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(TIMEOUT)
                    .build();
            URI uri = uriBuilder.addPathSegment("/method").build();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(uri)
                    .GET()
                    .build();

            // Act
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            // Assert
            assertEquals(200, response.statusCode());
            assertEquals("Method Response", response.body());
        }
    }
}
