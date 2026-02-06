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

import de.cuioss.test.mockwebserver.dispatcher.CombinedDispatcher;
import de.cuioss.test.mockwebserver.dispatcher.ModuleDispatcher;
import de.cuioss.tools.logging.CuiLogger;
import mockwebserver3.MockWebServer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for manual server start configuration in {@link MockWebServerExtension}.
 * This class consolidates all manual start tests to avoid duplication
 * across multiple test classes while ensuring comprehensive coverage.
 */
@EnableMockWebServer(manualStart = true)
@DisplayName("MockWebServer Manual Start Tests")
@SuppressWarnings("java:S1612")
// Suppress "Lambdas should be replaced with method references"
// Cannot be done here, start() is ambiguous
@ModuleDispatcher(provider = CombinedDispatcher.class, providerMethod = "createAPIDispatcher")
class MockWebServerManualStartTest {

    private static final CuiLogger LOGGER = new CuiLogger(MockWebServerManualStartTest.class);

    // Constants for assertion messages to avoid duplication
    private static final String SERVER_SHOULD_BE_INJECTED = "Server should be injected";
    private static final String SERVER_SHOULD_NOT_BE_STARTED = "Server should not be started";
    private static final String SERVER_SHOULD_BE_STARTED = "Server should be started after manual start";
    private static final String REQUEST_INTERRUPTED_MESSAGE = "Request was interrupted";

    // The dispatcher is now provided via the @ModuleDispatcher annotation

    // tag::manual-start-test[]
    @Test
    @DisplayName("Server should not be started automatically when manualStart=true")
    void shouldNotStartServerAutomatically(MockWebServer server) {
        assertNotNull(server, SERVER_SHOULD_BE_INJECTED);
        assertFalse(server.getStarted(), SERVER_SHOULD_NOT_BE_STARTED);

        // Start the server manually
        assertDoesNotThrow(() -> server.start());
        assertTrue(server.getStarted(), SERVER_SHOULD_BE_STARTED);

        // Verify server is accessible with timeout
        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(2))
                .build();

        // Create a proper URIBuilder now that the server is started
        URIBuilder properUriBuilder = URIBuilder.from(server.url("/").url());

        // Use the proper URIBuilder to construct the URI
        HttpRequest request = HttpRequest.newBuilder()
                .uri(properUriBuilder.addPathSegments("api", "test").build())
                .timeout(Duration.ofSeconds(2))
                .GET()
                .build();
        // end::manual-start-test[]
            
        // tag::manual-start-test-response[]
        assertDoesNotThrow(() -> {
            try {
                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                assertEquals(200, response.statusCode(), "Should receive OK response");
                LOGGER.info("Successfully received response from manually started server: " + response.body());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                /*~~(TODO: Throw specific not RuntimeException. Suppress: // cui-rewrite:disable InvalidExceptionUsageRecipe)~~>*/throw new RuntimeException(REQUEST_INTERRUPTED_MESSAGE, e);
            }
        });
        // end::manual-start-test-response[]
    }

    @Test
    @DisplayName("Should be able to start server on specific port")
    void shouldStartServerOnSpecificPort(MockWebServer server) {
        assertNotNull(server, SERVER_SHOULD_BE_INJECTED);

        // Try to start on a specific port
        try {
            int specificPort = 8090;
            server.start(specificPort);
            assertTrue(server.getStarted(), "Server should be started");
            assertEquals(specificPort, server.getPort(), "Server should use specified port");

        } catch (IOException e) {
            // Port might be in use, which is acceptable in a test environment
            // This is a corner case test, so we don't fail the test if the port is unavailable
            LOGGER.info("Could not start on specific port, likely already in use: " + e.getMessage());
        } finally {
            // Clean up
            try {
                if (server.getStarted()) {
                    server.close();
                }
            } /*~~(TODO: Catch specific not Exception. Suppress: // cui-rewrite:disable InvalidExceptionUsageRecipe)~~>*/catch (Exception e) {
                // Ignore shutdown errors in tests
            }
        }
    }

    @Test
    @DisplayName("Should handle simple request after manual start")
    void shouldHandleSimpleRequest(MockWebServer server) {
        assertNotNull(server, SERVER_SHOULD_BE_INJECTED);
        assertFalse(server.getStarted(), SERVER_SHOULD_NOT_BE_STARTED);

        // Start the server manually
        assertDoesNotThrow(() -> server.start());
        assertTrue(server.getStarted(), SERVER_SHOULD_BE_STARTED);

        // Create a proper URIBuilder now that the server is started
        URIBuilder properUriBuilder = URIBuilder.from(server.url("/").url());

        // Make a simple request to verify server functionality
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(properUriBuilder.setPath("api").build())
                .GET()
                .build();

        assertDoesNotThrow(() -> {
            try {
                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                assertNotNull(response, "Response should not be null");
                assertEquals(200, response.statusCode(), "Should receive OK response");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                /*~~(TODO: Throw specific not RuntimeException. Suppress: // cui-rewrite:disable InvalidExceptionUsageRecipe)~~>*/throw new RuntimeException(REQUEST_INTERRUPTED_MESSAGE, e);
            }
        });
    }
}
