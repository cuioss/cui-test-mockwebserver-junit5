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
package de.cuioss.test.mockwebserver;

import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import mockwebserver3.Dispatcher;
import mockwebserver3.MockResponse;
import mockwebserver3.MockWebServer;
import mockwebserver3.RecordedRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Demonstrates the usage of the {@link URIBuilder} parameter resolving feature.
 */
@EnableMockWebServer
class UrlBuilderParameterResolvingTest implements MockWebServerHolder {

    @Getter
    @Setter
    private MockWebServer mockWebServer;

    @Test
    void shouldInjectUrlBuilder(URIBuilder urlBuilder) throws IOException, InterruptedException {
        // Verify the URL builder is injected
        assertNotNull(urlBuilder, "URL builder should be injected");

        // Build a URL with path segments and query parameters
        URI uri = urlBuilder
                .addPathSegment("api")
                .addPathSegment("users")
                .addQueryParameter("filter", "active")
                .addQueryParameter("page", "1")
                .build();

        // Use the URI for a request
        HttpClient client = HttpClient.newBuilder().build();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(uri)
                .GET()
                .build();

        // Send the request
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        // Verify the response
        assertEquals(200, response.statusCode(), "Should receive 200 OK response");
        assertEquals("Request to: /api/users?filter=active&page=1", response.body(),
                "Response body should contain the correct path and query parameters");
    }

    @Override
    public Dispatcher getDispatcher() {
        return new Dispatcher() {
            @NotNull
            @Override
            public MockResponse dispatch(@NotNull RecordedRequest request) {
                // Echo back the path and query for verification
                return new MockResponse.Builder()
                        .code(200)
                        .body("Request to: " + request.getPath())
                        .build();
            }
        };
    }
}
