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

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Grandchild class that extends the child class to test deep inheritance of @MockResponse annotations.
 */
@EnableMockWebServer
@MockResponse(path = "/api/grandchild-class", status = 200, textContent = "Grandchild Class Response")
class GrandchildInheritanceTest extends ChildInheritanceTest {

    @Test
    @DisplayName("Grandchild class should access its own annotations and all ancestor class annotations")
    @MockResponse(path = "/api/grandchild-method", status = 200, textContent = "Grandchild Method Response")
    void grandchildClassTest(URIBuilder uriBuilder) throws IOException, InterruptedException {
        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(TIMEOUT)
                .build();

        // Test that base class annotation is inherited
        URI baseClassUri = uriBuilder.setPath("/api/base-class").build();
        HttpRequest baseClassRequest = HttpRequest.newBuilder()
                .uri(baseClassUri)
                .GET()
                .build();
        HttpResponse<String> baseClassResponse = client.send(baseClassRequest, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, baseClassResponse.statusCode(), STATUS_CODE_ASSERTION_MESSAGE);
        assertEquals("Base Class Response", baseClassResponse.body(), BODY_CONTENT_ASSERTION_MESSAGE);

        // Test that child class annotation is inherited
        URI childClassUri = uriBuilder.setPath("/api/child-class").build();
        HttpRequest childClassRequest = HttpRequest.newBuilder()
                .uri(childClassUri)
                .GET()
                .build();
        HttpResponse<String> childClassResponse = client.send(childClassRequest, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, childClassResponse.statusCode(), STATUS_CODE_ASSERTION_MESSAGE);
        assertEquals("Child Class Response", childClassResponse.body(), BODY_CONTENT_ASSERTION_MESSAGE);

        // Test that grandchild class annotation works
        URI grandchildClassUri = uriBuilder.setPath("/api/grandchild-class").build();
        HttpRequest grandchildClassRequest = HttpRequest.newBuilder()
                .uri(grandchildClassUri)
                .GET()
                .build();
        HttpResponse<String> grandchildClassResponse = client.send(grandchildClassRequest, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, grandchildClassResponse.statusCode(), STATUS_CODE_ASSERTION_MESSAGE);
        assertEquals("Grandchild Class Response", grandchildClassResponse.body(), BODY_CONTENT_ASSERTION_MESSAGE);

        // Test that grandchild method annotation works
        URI grandchildMethodUri = uriBuilder.setPath("/api/grandchild-method").build();
        HttpRequest grandchildMethodRequest = HttpRequest.newBuilder()
                .uri(grandchildMethodUri)
                .GET()
                .build();
        HttpResponse<String> grandchildMethodResponse = client.send(grandchildMethodRequest, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, grandchildMethodResponse.statusCode(), STATUS_CODE_ASSERTION_MESSAGE);
        assertEquals("Grandchild Method Response", grandchildMethodResponse.body(), BODY_CONTENT_ASSERTION_MESSAGE);

        // Test that base method and child method annotations are NOT accessible
        URI baseMethodUri = uriBuilder.setPath("/api/base-method").build();
        HttpRequest baseMethodRequest = HttpRequest.newBuilder()
                .uri(baseMethodUri)
                .GET()
                .build();
        HttpResponse<String> baseMethodResponse = client.send(baseMethodRequest, HttpResponse.BodyHandlers.ofString());
        assertEquals(418, baseMethodResponse.statusCode(), "Base method response should not be accessible from grandchild class");

        URI childMethodUri = uriBuilder.setPath("/api/child-method").build();
        HttpRequest childMethodRequest = HttpRequest.newBuilder()
                .uri(childMethodUri)
                .GET()
                .build();
        HttpResponse<String> childMethodResponse = client.send(childMethodRequest, HttpResponse.BodyHandlers.ofString());
        assertEquals(418, childMethodResponse.statusCode(), "Child method response should not be accessible from grandchild class");
    }
}