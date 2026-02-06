/*
 * Copyright © 2025 CUI-OpenSource-Software (info@cuioss.de)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
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
 * Child class that extends the base class to test inheritance of @MockResponseConfig annotations.
 */
@EnableMockWebServer
@MockResponseConfig(path = "/api/child-class", status = 200, textContent = "Child Class Response")
class ChildInheritanceTest extends InheritanceHierarchyTest {

    @Test
    @DisplayName("Child class should access its own annotations and parent class annotations")
    @MockResponseConfig(path = "/api/child-method", status = 200, textContent = "Child Method Response")
    void childClassTest(URIBuilder uriBuilder) throws IOException, InterruptedException {
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

        // Test that child class annotation works
        URI childClassUri = uriBuilder.setPath("/api/child-class").build();
        HttpRequest childClassRequest = HttpRequest.newBuilder()
                .uri(childClassUri)
                .GET()
                .build();
        HttpResponse<String> childClassResponse = client.send(childClassRequest, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, childClassResponse.statusCode(), STATUS_CODE_ASSERTION_MESSAGE);
        assertEquals("Child Class Response", childClassResponse.body(), BODY_CONTENT_ASSERTION_MESSAGE);

        // Test that child method annotation works
        URI childMethodUri = uriBuilder.setPath("/api/child-method").build();
        HttpRequest childMethodRequest = HttpRequest.newBuilder()
                .uri(childMethodUri)
                .GET()
                .build();
        HttpResponse<String> childMethodResponse = client.send(childMethodRequest, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, childMethodResponse.statusCode(), STATUS_CODE_ASSERTION_MESSAGE);
        assertEquals("Child Method Response", childMethodResponse.body(), BODY_CONTENT_ASSERTION_MESSAGE);

        // Test that base method annotation is NOT accessible (only class-level annotations are inherited)
        URI baseMethodUri = uriBuilder.setPath("/api/base-method").build();
        HttpRequest baseMethodRequest = HttpRequest.newBuilder()
                .uri(baseMethodUri)
                .GET()
                .build();
        HttpResponse<String> baseMethodResponse = client.send(baseMethodRequest, HttpResponse.BodyHandlers.ofString());
        assertEquals(418, baseMethodResponse.statusCode(), "Base method response should not be accessible from child class");
    }
}