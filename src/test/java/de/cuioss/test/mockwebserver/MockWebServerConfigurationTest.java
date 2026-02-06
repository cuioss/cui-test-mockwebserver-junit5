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
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import javax.net.ssl.SSLContext;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for configuration options in {@link MockWebServerExtension}.
 * This class covers various configuration combinations to improve test coverage.
 */
@DisplayName("MockWebServerExtension Configuration")
class MockWebServerConfigurationTest {

    private static final CuiLogger LOGGER = new CuiLogger(MockWebServerConfigurationTest.class);

    // Constants for assertion messages to avoid duplication
    private static final String SERVER_SHOULD_BE_INJECTED = "Server should be injected";
    private static final String SERVER_SHOULD_BE_STARTED = "Server should be started";
    private static final String SERVER_SHOULD_NOT_BE_STARTED = "Server should not be started automatically";
    private static final String SSL_CONTEXT_SHOULD_BE_INJECTED = "SSL context should be injected";
    private static final String REQUEST_INTERRUPTED_MESSAGE = "Request was interrupted";

    // tag::extension-configuration[]
    /**
     * Tests for default configuration.
     */
    @Nested
    @DisplayName("Default Configuration Tests")
    @EnableMockWebServer
    @ModuleDispatcher(provider = CombinedDispatcher.class, providerMethod = "createAPIDispatcher")
    class DefaultConfigurationTests {

        @Test
        @DisplayName("Should use default configuration")
        void shouldUseDefaultConfiguration(MockWebServer server, URIBuilder uriBuilder) {
            assertNotNull(server, SERVER_SHOULD_BE_INJECTED);
            assertTrue(server.getStarted(), SERVER_SHOULD_BE_STARTED + " with default auto-start");

            // Verify HTTP is used (not HTTPS) by checking URL scheme
            assertTrue(server.url("/").url().toString().startsWith("http://"),
                    "Server URL should use HTTP scheme by default");

            // Test a basic request to verify the server is working
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(2))
                    .build();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(uriBuilder.addPathSegment("api").build())
                    .GET()
                    .build();

            assertDoesNotThrow(() -> {
                try {
                    HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                    assertEquals(200, response.statusCode(), "Default server should handle requests");
                } catch (InterruptedException e) {
                    // Restore the interrupted status
                    Thread.currentThread().interrupt();
                    /*~~(TODO: Throw specific not RuntimeException. Suppress: // cui-rewrite:disable InvalidExceptionUsageRecipe)~~>*/throw new RuntimeException(REQUEST_INTERRUPTED_MESSAGE, e);
                }
            });
        }
    }

    /**
     * Tests for HTTPS configuration without custom certificates.
     */
    @Nested
    @DisplayName("HTTPS Configuration Tests")
    @EnableMockWebServer(useHttps = true)
    @ModuleDispatcher(provider = CombinedDispatcher.class, providerMethod = "createAPIDispatcher")
    class HttpsConfigurationTests {

        @Test
        @DisplayName("Should use HTTPS with self-signed certificates")
        void shouldUseHttpsWithSelfSignedCertificates(MockWebServer server, SSLContext sslContext, URIBuilder uriBuilder) {
            assertNotNull(server, SERVER_SHOULD_BE_INJECTED);
            assertTrue(server.getStarted(), SERVER_SHOULD_BE_STARTED);
            assertNotNull(sslContext, SSL_CONTEXT_SHOULD_BE_INJECTED);

            // Verify HTTPS is enabled by checking URL scheme
            assertTrue(server.url("/").url().toString().startsWith("https://"),
                    "Server URL should use HTTPS scheme");

            // Test a request with the provided SSL context
            HttpClient client = HttpClient.newBuilder()
                    .sslContext(sslContext)
                    .connectTimeout(Duration.ofSeconds(2))
                    .build();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(uriBuilder.addPathSegment("api").build())
                    .GET()
                    .build();

            assertDoesNotThrow(() -> {
                try {
                    HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                    assertEquals(200, response.statusCode(), "HTTPS server should handle requests");
                } catch (InterruptedException e) {
                    // Restore the interrupted status
                    Thread.currentThread().interrupt();
                    /*~~(TODO: Throw specific not RuntimeException. Suppress: // cui-rewrite:disable InvalidExceptionUsageRecipe)~~>*/throw new RuntimeException(REQUEST_INTERRUPTED_MESSAGE, e);
                }
            });
        }
    }

    /**
     * Tests for manual server control configuration.
     */
    @Nested
    @DisplayName("Manual Control Tests")
    @EnableMockWebServer(manualStart = true)
    @ModuleDispatcher(provider = CombinedDispatcher.class, providerMethod = "createAPIDispatcher")
    class ManualControlTests {

        @Test
        @DisplayName("Should not start server automatically")
        void shouldNotStartServerAutomatically(MockWebServer server) {
            assertNotNull(server, SERVER_SHOULD_BE_INJECTED);
            assertFalse(server.getStarted(), SERVER_SHOULD_NOT_BE_STARTED);

            // Start the server manually
            assertDoesNotThrow(() -> {
                server.start();
                assertTrue(server.getStarted(), "Server should be started after manual start");

                // Get the port after starting
                int port = server.getPort();
                LOGGER.info("Server manually started on port: " + port);

                // Create a URIBuilder manually since it's not injected before server start
                URIBuilder uriBuilder = URIBuilder.from(server.url("/").url());

                // Test a basic request
                HttpClient client = HttpClient.newBuilder()
                        .connectTimeout(Duration.ofSeconds(2))
                        .build();

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(uriBuilder.addPathSegment("api").build())
                        .GET()
                        .build();

                try {
                    HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                    assertEquals(200, response.statusCode(), "Manually started server should handle requests");
                } catch (InterruptedException e) {
                    // Restore the interrupted status
                    Thread.currentThread().interrupt();
                    /*~~(TODO: Throw specific not RuntimeException. Suppress: // cui-rewrite:disable InvalidExceptionUsageRecipe)~~>*/throw new RuntimeException(REQUEST_INTERRUPTED_MESSAGE, e);
                }
            });
        }
    }

    /**
     * Tests for combined configuration options.
     */
    @Nested
    @DisplayName("Combined Configuration Tests")
    @EnableMockWebServer(useHttps = true, manualStart = true)
    @ModuleDispatcher(provider = CombinedDispatcher.class, providerMethod = "createAPIDispatcher")
    class CombinedConfigurationTests {

        @Test
        @DisplayName("Should handle combined HTTPS and manual start")
        void shouldHandleCombinedHttpsAndManualStart(MockWebServer server, SSLContext sslContext) {
            assertNotNull(server, SERVER_SHOULD_BE_INJECTED);
            assertFalse(server.getStarted(), SERVER_SHOULD_NOT_BE_STARTED);
            assertNotNull(sslContext, SSL_CONTEXT_SHOULD_BE_INJECTED + " even before server start");

            // Start the server manually
            assertDoesNotThrow(() -> {
                server.start();
                assertTrue(server.getStarted(), "Server should be started after manual start");

                // Verify HTTPS is enabled
                assertTrue(server.url("/").url().toString().startsWith("https://"),
                        "Server URL should use HTTPS scheme");

                // Create a URIBuilder manually
                URIBuilder uriBuilder = URIBuilder.from(server.url("/").url());

                // Test a request with the provided SSL context
                HttpClient client = HttpClient.newBuilder()
                        .sslContext(sslContext)
                        .connectTimeout(Duration.ofSeconds(2))
                        .build();

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(uriBuilder.addPathSegment("api").build())
                        .GET()
                        .build();

                try {
                    HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                    assertEquals(200, response.statusCode(), "HTTPS server with manual start should handle requests");
                } catch (InterruptedException e) {
                    // Restore the interrupted status
                    Thread.currentThread().interrupt();
                    /*~~(TODO: Throw specific not RuntimeException. Suppress: // cui-rewrite:disable InvalidExceptionUsageRecipe)~~>*/throw new RuntimeException(REQUEST_INTERRUPTED_MESSAGE, e);
                }
            });
        }
    }
    // end::extension-configuration[]
}
