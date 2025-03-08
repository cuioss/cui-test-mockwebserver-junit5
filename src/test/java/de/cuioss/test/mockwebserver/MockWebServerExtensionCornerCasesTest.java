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
import okhttp3.tls.HandshakeCertificates;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ParameterResolver;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Optional;
import javax.net.ssl.SSLContext;


import mockwebserver3.Dispatcher;
import mockwebserver3.MockResponse;
import mockwebserver3.MockWebServer;
import mockwebserver3.RecordedRequest;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for corner cases and error handling in {@link MockWebServerExtension}.
 * This class covers edge cases to improve test coverage.
 */
@DisplayName("MockWebServerExtension Corner Cases")
class MockWebServerExtensionCornerCasesTest {

    private static final CuiLogger LOGGER = new CuiLogger(MockWebServerExtensionCornerCasesTest.class);

    // Constants for assertion messages to avoid duplication
    private static final String SERVER_SHOULD_BE_INJECTED = "Server should be injected";
    private static final String SERVER_SHOULD_BE_STARTED = "Server should be started";
    private static final String REQUEST_INTERRUPTED_MESSAGE = "Request was interrupted";
    private static final String URIBUILDER_SHOULD_BE_INJECTED = "URIBuilder should be injected";
    private static final String SSL_CONTEXT_SHOULD_BE_INJECTED = "SSL context should be injected";

    // tag::extension-corner-cases[]
    /**
     * Tests for handling of invalid parameter types in {@link ParameterResolver}.
     */
    @Nested
    @DisplayName("Parameter Resolution Tests")
    @EnableMockWebServer
    class ParameterResolutionTests {

        @Test
        @DisplayName("Should resolve all supported parameter types")
        void shouldResolveAllSupportedParameterTypes(MockWebServer server, URIBuilder uriBuilder) {
            assertNotNull(server, SERVER_SHOULD_BE_INJECTED);
            assertNotNull(uriBuilder, URIBUILDER_SHOULD_BE_INJECTED);

            // Verify the URIBuilder is properly configured with the server's URL
            URI uri = uriBuilder.build();
            assertEquals(server.getPort(), uri.getPort(), "URIBuilder should have correct port");
        }
    }

    /**
     * Tests for handling of class hierarchy and annotation inheritance.
     */
    @Nested
    @DisplayName("Class Hierarchy Tests")
    @ExtendWith(MockWebServerExtension.class)
    class ClassHierarchyTests {

        @Test
        @DisplayName("Should work with extension but no annotation")
        void shouldWorkWithExtensionButNoAnnotation(MockWebServer server) {
            // The extension is registered but there's no @EnableMockWebServer annotation
            // It should still work with default configuration
            assertNotNull(server, SERVER_SHOULD_BE_INJECTED);
            assertTrue(server.getStarted(), SERVER_SHOULD_BE_STARTED);
        }

        /**
         * Nested class to test deeper hierarchy
         */
        @Nested
        @DisplayName("Nested Class Tests")
        class DeeperNestedClass {

            @Test
            @DisplayName("Should work with extension in parent class")
            void shouldWorkWithExtensionInParentClass(MockWebServer server) {
                // The extension is registered on the parent class
                // It should still work for nested classes
                assertNotNull(server, SERVER_SHOULD_BE_INJECTED + " in nested class");
                assertTrue(server.getStarted(), SERVER_SHOULD_BE_STARTED + " in nested class");
            }
        }
    }

    /**
     * Tests for error handling during server setup.
     */
    @Nested
    @DisplayName("Error Handling Tests")
    @EnableMockWebServer
    class ErrorHandlingTests implements MockWebServerHolder {

        @Override
        public Dispatcher getDispatcher() {
            return new Dispatcher() {
                @Override
                public MockResponse dispatch(RecordedRequest request) {
                    return new MockResponse.Builder()
                            .code(500)
                            .body("Error response")
                            .build();
                }
            };
        }

        @Test
        @DisplayName("Should handle server errors gracefully")
        void shouldHandleServerErrorsGracefully(MockWebServer server, URIBuilder uriBuilder) {
            assertNotNull(server, SERVER_SHOULD_BE_INJECTED);
            assertTrue(server.getStarted(), SERVER_SHOULD_BE_STARTED);

            // Create HTTP client with timeout
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(2))
                    .build();

            try {
                // Make request that will receive a 500 error
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(uriBuilder.addPathSegment("error").build())
                        .GET()
                        .build();

                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                assertEquals(500, response.statusCode(), "Should receive error response");
                assertEquals("Error response", response.body(), "Should receive error body");

            } catch (IOException e) {
                fail("Request should not throw exception: " + e.getMessage());
            } catch (InterruptedException e) {
                // Restore the interrupted status
                Thread.currentThread().interrupt();
                fail(REQUEST_INTERRUPTED_MESSAGE + ": " + e.getMessage());
            }
        }
    }

    /**
     * Tests for HTTPS configuration with invalid certificates.
     */
    @Nested
    @DisplayName("HTTPS Configuration Tests")
    @EnableMockWebServer(useHttps = true, testClassProvidesKeyMaterial = true)
    class HttpsConfigurationTests implements MockWebServerHolder {

        @Override
        public Optional<HandshakeCertificates> getTestProvidedHandshakeCertificates() {
            // Return empty to test fallback to self-signed certificates
            return Optional.empty();
        }

        @Override
        public Dispatcher getDispatcher() {
            return CombinedDispatcher.createAPIDispatcher();
        }

        @Test
        @DisplayName("Should fallback to self-signed certificates when test class returns empty")
        void shouldFallbackToSelfSignedCertificates(MockWebServer server, SSLContext sslContext) {
            assertNotNull(server, SERVER_SHOULD_BE_INJECTED);
            assertTrue(server.getStarted(), SERVER_SHOULD_BE_STARTED);
            assertNotNull(sslContext, SSL_CONTEXT_SHOULD_BE_INJECTED);

            // The test is configured to provide certificates but returns empty
            // The extension should fall back to self-signed certificates
            LOGGER.info("Server started with fallback certificates on port: " + server.getPort());
        }
    }
    // end::extension-corner-cases[]
}
