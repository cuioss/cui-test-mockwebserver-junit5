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

import de.cuioss.test.mockwebserver.dispatcher.CombinedDispatcher;
import de.cuioss.tools.logging.CuiLogger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;


import mockwebserver3.Dispatcher;
import mockwebserver3.MockWebServer;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for manual server start configuration in {@link MockWebServerExtension}.
 */
@EnableMockWebServer(manualStart = true)
class MockWebServerManualStartTest implements MockWebServerHolder {

    private static final CuiLogger LOGGER = new CuiLogger(MockWebServerManualStartTest.class);

    @Override
    public Dispatcher getDispatcher() {
        return CombinedDispatcher.createAPIDispatcher();
    }

    // tag::manual-start-test[]
    @Test
    @DisplayName("Server should not be started automatically when manualStart=true")
    void shouldNotStartServerAutomatically(MockWebServer server) {
        assertNotNull(server, "Server should be injected");
        assertFalse(server.getStarted(), "Server should not be started automatically");

        // Start the server manually
        try {
            server.start();
            assertTrue(server.getStarted(), "Server should be started after manual start");

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
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            assertEquals(200, response.statusCode(), "Should receive OK response");
            LOGGER.info("Successfully received response from manually started server: " + response.body());
            // end::manual-start-test-response[]
            
        } catch (Exception e) {
            LOGGER.error("Failed to start or use server", e);
            fail("Failed to start or use server: " + e.getMessage());
        } finally {
            // Clean up
            try {
                if (server.getStarted()) {
                    server.shutdown();
                }
            } catch (IOException e) {
                // Ignore shutdown errors in tests
            }
        }
    }

    @Test
    @DisplayName("Should be able to start server on specific port")
    void shouldStartServerOnSpecificPort(MockWebServer server) {
        assertNotNull(server, "Server should be injected");

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
                    server.shutdown();
                }
            } catch (IOException e) {
                // Ignore shutdown errors in tests
            }
        }
    }
}
