/**
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
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Base class for testing inheritance hierarchy with @MockResponseConfig annotations.
 */
@EnableMockWebServer
@MockResponseConfig(path = "/api/base-class", status = 200, textContent = "Base Class Response")
class InheritanceHierarchyTest {

    protected static final Duration TIMEOUT = Duration.ofSeconds(5);
    protected static final String STATUS_CODE_ASSERTION_MESSAGE = "Response status code should match";
    protected static final String BODY_CONTENT_ASSERTION_MESSAGE = "Response body content should match";

    @Test
    @DisplayName("Base class should only access its own annotations")
    @MockResponseConfig(path = "/api/base-method", status = 200, textContent = "Base Method Response")
    void baseClassTest(URIBuilder uriBuilder) throws IOException, InterruptedException {
        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(TIMEOUT)
                .build();

        // Test that base class annotation works
        URI baseClassUri = uriBuilder.setPath("/api/base-class").build();
        HttpRequest baseClassRequest = HttpRequest.newBuilder()
                .uri(baseClassUri)
                .GET()
                .build();
        HttpResponse<String> baseClassResponse = client.send(baseClassRequest, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, baseClassResponse.statusCode(), STATUS_CODE_ASSERTION_MESSAGE);
        assertEquals("Base Class Response", baseClassResponse.body(), BODY_CONTENT_ASSERTION_MESSAGE);

        // Test that base method annotation works
        URI baseMethodUri = uriBuilder.setPath("/api/base-method").build();
        HttpRequest baseMethodRequest = HttpRequest.newBuilder()
                .uri(baseMethodUri)
                .GET()
                .build();
        HttpResponse<String> baseMethodResponse = client.send(baseMethodRequest, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, baseMethodResponse.statusCode(), STATUS_CODE_ASSERTION_MESSAGE);
        assertEquals("Base Method Response", baseMethodResponse.body(), BODY_CONTENT_ASSERTION_MESSAGE);

        // Test that child class annotations are not accessible
        URI childClassUri = uriBuilder.setPath("/api/child-class").build();
        HttpRequest childClassRequest = HttpRequest.newBuilder()
                .uri(childClassUri)
                .GET()
                .build();
        HttpResponse<String> childClassResponse = client.send(childClassRequest, HttpResponse.BodyHandlers.ofString());
        assertEquals(418, childClassResponse.statusCode(), "Child class response should not be accessible from base class");

        URI childMethodUri = uriBuilder.setPath("/api/child-method").build();
        HttpRequest childMethodRequest = HttpRequest.newBuilder()
                .uri(childMethodUri)
                .GET()
                .build();
        HttpResponse<String> childMethodResponse = client.send(childMethodRequest, HttpResponse.BodyHandlers.ofString());
        assertEquals(418, childMethodResponse.statusCode(), "Child method response should not be accessible from base class");
    }
}
