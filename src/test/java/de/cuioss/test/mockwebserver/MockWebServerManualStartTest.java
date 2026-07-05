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

import java.net.ServerSocket;
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
                throw new MockWebServerTestException(REQUEST_INTERRUPTED_MESSAGE, e);
            }
        });
        // end::manual-start-test-response[]
    }

    @Test
    @DisplayName("Should be able to start server on a specific port")
    void shouldStartServerOnSpecificPort(MockWebServer server) throws Exception {
        assertNotNull(server, SERVER_SHOULD_BE_INJECTED);

        // Reserve a currently-free port so the test is deterministic instead of relying on a fixed one.
        int freePort;
        try (ServerSocket socket = new ServerSocket(0)) {
            freePort = socket.getLocalPort();
        }

        // Start on the reserved port; any IOException here is a real failure and must fail the test.
        // The extension's afterEach closes the server.
        server.start(freePort);
        assertTrue(server.getStarted(), SERVER_SHOULD_BE_STARTED);
        assertEquals(freePort, server.getPort(), "Server should use the reserved port");
    }

    @Test
    @DisplayName("Injected URIBuilder should bind lazily and work after manual start")
    void shouldUseInjectedUriBuilderAfterManualStart(MockWebServer server, URIBuilder injectedUriBuilder) {
        assertNotNull(server, SERVER_SHOULD_BE_INJECTED);
        assertNotNull(injectedUriBuilder, "URIBuilder should be injected");
        assertFalse(server.getStarted(), SERVER_SHOULD_NOT_BE_STARTED);

        // Start the server manually; the injected builder must resolve the base URL lazily at build() time
        assertDoesNotThrow(() -> server.start());
        assertTrue(server.getStarted(), SERVER_SHOULD_BE_STARTED);

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(injectedUriBuilder.addPathSegments("api", "test").build())
                .GET()
                .build();

        assertDoesNotThrow(() -> {
            try {
                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                assertEquals(200, response.statusCode(), "Should receive OK response via injected URIBuilder");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new MockWebServerTestException(REQUEST_INTERRUPTED_MESSAGE, e);
            }
        });
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
                throw new MockWebServerTestException(REQUEST_INTERRUPTED_MESSAGE, e);
            }
        });
    }
}
