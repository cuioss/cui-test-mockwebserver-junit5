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
package de.cuioss.test.mockwebserver.dispatcher;

import de.cuioss.test.mockwebserver.EnableMockWebServer;
import de.cuioss.test.mockwebserver.URIBuilder;
import lombok.NonNull;
import mockwebserver3.MockResponse;
import mockwebserver3.RecordedRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Optional;
import java.util.Set;
import javax.net.ssl.SSLContext;

import static jakarta.servlet.http.HttpServletResponse.SC_OK;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Test class that demonstrates using a custom dispatcher with path-based responses.
 * This implements the example shown in the README.
 */
@EnableMockWebServer(useHttps = true)
@ModuleDispatcher
// No parameters means look for getModuleDispatcher() method
@DisplayName("Path-Based Dispatcher Test")
class PathBasedDispatcherTest {

    /**
     * This method will be called to get the dispatcher.
     * It implements a custom dispatcher that returns different responses based on the request path.
     */
    ModuleDispatcherElement getModuleDispatcher() {
        return new ModuleDispatcherElement() {
            @Override
            public String getBaseUrl() {
                return "/api/users";
            }

            @Override
            public Optional<MockResponse> handleGet(@NonNull RecordedRequest request) {
                String path = request.getUrl().encodedPath();

                // Return different responses based on the path
                assert path != null;
                if (path.endsWith("/api/users/active")) {
                    return Optional.of(new MockResponse.Builder()
                            .code(SC_OK)
                            .addHeader("Content-Type", "application/json")
                            .body("{\"users\":[{\"id\":1,\"name\":\"John\",\"status\":\"active\"}]}")
                            .build());
                } else if (path.endsWith("/api/users/inactive")) {
                    return Optional.of(new MockResponse.Builder()
                            .code(SC_OK)
                            .addHeader("Content-Type", "application/json")
                            .body("{\"users\":[{\"id\":2,\"name\":\"Jane\",\"status\":\"inactive\"}]}")
                            .build());
                } else if (path.matches(".*/api/users/\\d+")) {
                    // Extract user ID from path using regex
                    String userId = path.substring(path.lastIndexOf('/') + 1);
                    return Optional.of(new MockResponse.Builder()
                            .code(SC_OK)
                            .addHeader("Content-Type", "application/json")
                            .body("{\"id\":" + userId + ",\"name\":\"User " + userId + "\"}")
                            .build());
                }

                // Default response for /api/users
                return Optional.of(new MockResponse.Builder()
                        .code(SC_OK)
                        .addHeader("Content-Type", "application/json")
                        .body("{\"users\":[]}")
                        .build());
            }

            @Override
            public @NonNull Set<HttpMethodMapper> supportedMethods() {
                return Set.of(HttpMethodMapper.GET);
            }
        };
    }

    @Test
    @DisplayName("Should return different responses based on path")
    void shouldReturnDifferentResponsesBasedOnPath(URIBuilder uriBuilder, SSLContext sslContext) throws Exception {
        // Create HttpClient with SSL context
        HttpClient client = HttpClient.newBuilder()
                .sslContext(sslContext)
                .build();

        // Test different paths

        // 1. Get all users (empty list)
        HttpRequest allUsersRequest = HttpRequest.newBuilder()
                .uri(uriBuilder.addPathSegments("api", "users").build())
                .GET()
                .build();
        HttpResponse<String> allUsersResponse = client.send(allUsersRequest,
                HttpResponse.BodyHandlers.ofString());
        assertEquals(200, allUsersResponse.statusCode());
        assertEquals("{\"users\":[]}", allUsersResponse.body());

        // 2. Get active users
        HttpRequest activeUsersRequest = HttpRequest.newBuilder()
                .uri(uriBuilder.addPathSegments("api", "users", "active").build())
                .GET()
                .build();
        HttpResponse<String> activeUsersResponse = client.send(activeUsersRequest,
                HttpResponse.BodyHandlers.ofString());
        assertEquals(200, activeUsersResponse.statusCode());
        assertEquals("{\"users\":[{\"id\":1,\"name\":\"John\",\"status\":\"active\"}]}",
                activeUsersResponse.body());

        // 3. Get inactive users
        HttpRequest inactiveUsersRequest = HttpRequest.newBuilder()
                .uri(uriBuilder.addPathSegments("api", "users", "inactive").build())
                .GET()
                .build();
        HttpResponse<String> inactiveUsersResponse = client.send(inactiveUsersRequest,
                HttpResponse.BodyHandlers.ofString());
        assertEquals(200, inactiveUsersResponse.statusCode());
        assertEquals("{\"users\":[{\"id\":2,\"name\":\"Jane\",\"status\":\"inactive\"}]}",
                inactiveUsersResponse.body());

        // 4. Get user by ID
        HttpRequest userRequest = HttpRequest.newBuilder()
                .uri(uriBuilder.addPathSegments("api", "users", "42").build())
                .GET()
                .build();
        HttpResponse<String> userResponse = client.send(userRequest,
                HttpResponse.BodyHandlers.ofString());
        assertEquals(200, userResponse.statusCode());
        assertEquals("{\"id\":42,\"name\":\"User 42\"}", userResponse.body());
    }
}
