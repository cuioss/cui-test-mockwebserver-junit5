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
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Test case to verify that multiple @MockResponseConfig annotations on the same method work correctly.
 */
@EnableMockWebServer
@MockResponseConfig(path = "/api/class-level", status = 200, textContent = "Class Level Response")
class MultipleAnnotationsTest {

    private static final Duration TIMEOUT = Duration.ofSeconds(5);
    private static final String STATUS_CODE_ASSERTION_MESSAGE = "Response status code should match";
    private static final String BODY_CONTENT_ASSERTION_MESSAGE = "Response body content should match";

    @Test
    @DisplayName("Should handle multiple @MockResponseConfig annotations on the same method")
    @MockResponseConfig(path = "/api/method-1", status = 200, textContent = "Method 1 Response")
    @MockResponseConfig(path = "/api/method-2", status = 201, textContent = "Method 2 Response")
    @MockResponseConfig(path = "/api/method-3", status = 202, textContent = "Method 3 Response")
    void shouldHandleMultipleAnnotations(URIBuilder uriBuilder) throws IOException, InterruptedException {
        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(TIMEOUT)
                .build();

        // Test that class-level annotation works
        URI classLevelUri = uriBuilder.setPath("/api/class-level").build();
        HttpRequest classLevelRequest = HttpRequest.newBuilder()
                .uri(classLevelUri)
                .GET()
                .build();
        HttpResponse<String> classLevelResponse = client.send(classLevelRequest, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, classLevelResponse.statusCode(), STATUS_CODE_ASSERTION_MESSAGE);
        assertEquals("Class Level Response", classLevelResponse.body(), BODY_CONTENT_ASSERTION_MESSAGE);

        // Test that first method annotation works
        URI method1Uri = uriBuilder.setPath("/api/method-1").build();
        HttpRequest method1Request = HttpRequest.newBuilder()
                .uri(method1Uri)
                .GET()
                .build();
        HttpResponse<String> method1Response = client.send(method1Request, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, method1Response.statusCode(), STATUS_CODE_ASSERTION_MESSAGE);
        assertEquals("Method 1 Response", method1Response.body(), BODY_CONTENT_ASSERTION_MESSAGE);

        // Test that second method annotation works
        URI method2Uri = uriBuilder.setPath("/api/method-2").build();
        HttpRequest method2Request = HttpRequest.newBuilder()
                .uri(method2Uri)
                .GET()
                .build();
        HttpResponse<String> method2Response = client.send(method2Request, HttpResponse.BodyHandlers.ofString());
        assertEquals(201, method2Response.statusCode(), STATUS_CODE_ASSERTION_MESSAGE);
        assertEquals("Method 2 Response", method2Response.body(), BODY_CONTENT_ASSERTION_MESSAGE);

        // Test that third method annotation works
        URI method3Uri = uriBuilder.setPath("/api/method-3").build();
        HttpRequest method3Request = HttpRequest.newBuilder()
                .uri(method3Uri)
                .GET()
                .build();
        HttpResponse<String> method3Response = client.send(method3Request, HttpResponse.BodyHandlers.ofString());
        assertEquals(202, method3Response.statusCode(), STATUS_CODE_ASSERTION_MESSAGE);
        assertEquals("Method 3 Response", method3Response.body(), BODY_CONTENT_ASSERTION_MESSAGE);
    }

    @Test
    @DisplayName("Should not have access to annotations from other methods")
    @MockResponseConfig(path = "/api/other-method", status = 200, textContent = "Other Method Response")
    void shouldNotHaveAccessToOtherMethodAnnotations(URIBuilder uriBuilder) throws IOException, InterruptedException {
        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(TIMEOUT)
                .build();

        // Test that class-level annotation works
        URI classLevelUri = uriBuilder.setPath("/api/class-level").build();
        HttpRequest classLevelRequest = HttpRequest.newBuilder()
                .uri(classLevelUri)
                .GET()
                .build();
        HttpResponse<String> classLevelResponse = client.send(classLevelRequest, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, classLevelResponse.statusCode(), STATUS_CODE_ASSERTION_MESSAGE);
        assertEquals("Class Level Response", classLevelResponse.body(), BODY_CONTENT_ASSERTION_MESSAGE);

        // Test that this method's annotation works
        URI otherMethodUri = uriBuilder.setPath("/api/other-method").build();
        HttpRequest otherMethodRequest = HttpRequest.newBuilder()
                .uri(otherMethodUri)
                .GET()
                .build();
        HttpResponse<String> otherMethodResponse = client.send(otherMethodRequest, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, otherMethodResponse.statusCode(), STATUS_CODE_ASSERTION_MESSAGE);
        assertEquals("Other Method Response", otherMethodResponse.body(), BODY_CONTENT_ASSERTION_MESSAGE);

        // Test that annotations from the other method are not accessible
        URI method1Uri = uriBuilder.setPath("/api/method-1").build();
        HttpRequest method1Request = HttpRequest.newBuilder()
                .uri(method1Uri)
                .GET()
                .build();
        HttpResponse<String> method1Response = client.send(method1Request, HttpResponse.BodyHandlers.ofString());
        assertEquals(418, method1Response.statusCode(), "Method 1 response should not be accessible from this test");

        URI method2Uri = uriBuilder.setPath("/api/method-2").build();
        HttpRequest method2Request = HttpRequest.newBuilder()
                .uri(method2Uri)
                .GET()
                .build();
        HttpResponse<String> method2Response = client.send(method2Request, HttpResponse.BodyHandlers.ofString());
        assertEquals(418, method2Response.statusCode(), "Method 2 response should not be accessible from this test");

        URI method3Uri = uriBuilder.setPath("/api/method-3").build();
        HttpRequest method3Request = HttpRequest.newBuilder()
                .uri(method3Uri)
                .GET()
                .build();
        HttpResponse<String> method3Response = client.send(method3Request, HttpResponse.BodyHandlers.ofString());
        assertEquals(418, method3Response.statusCode(), "Method 3 response should not be accessible from this test");
    }
}