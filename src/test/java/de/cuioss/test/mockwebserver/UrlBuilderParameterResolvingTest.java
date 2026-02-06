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
package de.cuioss.test.mockwebserver;

import de.cuioss.test.mockwebserver.dispatcher.HttpMethodMapper;
import de.cuioss.test.mockwebserver.dispatcher.ModuleDispatcher;
import de.cuioss.test.mockwebserver.dispatcher.ModuleDispatcherElement;
import lombok.NonNull;
import mockwebserver3.MockResponse;
import mockwebserver3.RecordedRequest;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;


@EnableMockWebServer
@ModuleDispatcher()
class UrlBuilderParameterResolvingTest {

    /**
     * Provides a dispatcher for the mock web server
     *
     * @return a dispatcher that handles API requests
     */
    @SuppressWarnings("unused") // implicitly called by the test framework
    public ModuleDispatcherElement getModuleDispatcher() {
        // Create a direct implementation of Dispatcher that echoes back the request path
        // This is critical for the test to verify the correct path construction
        return new ModuleDispatcherElement() {
            @Override
            public String getBaseUrl() {
                return "/";
            }

            @Override
            public Optional<MockResponse> handleGet(@NonNull RecordedRequest request) {
                return Optional.of(new MockResponse.Builder()
                        .code(200)
                        .body("Request to: " + request.getUrl().encodedPath()
                                + (request.getUrl().encodedQuery() != null ? "?" + request.getUrl().encodedQuery() : ""))
                        .build());
            }

            @Override
            public @NonNull Set<HttpMethodMapper> supportedMethods() {
                return Set.of(HttpMethodMapper.GET);
            }
        };

    }


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


}
