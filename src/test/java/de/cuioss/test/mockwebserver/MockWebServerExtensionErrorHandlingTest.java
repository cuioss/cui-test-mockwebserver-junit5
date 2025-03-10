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
import org.jetbrains.annotations.NotNull;
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
import javax.net.ssl.SSLContext;


import mockwebserver3.Dispatcher;
import mockwebserver3.MockResponse;
import mockwebserver3.MockWebServer;
import mockwebserver3.RecordedRequest;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for error handling in {@link MockWebServerExtension}.
 * This class covers additional edge cases to improve test coverage.
 * <p>
 * This class consolidates all error handling and corner case tests to avoid duplication
 * across multiple test classes while ensuring comprehensive coverage.
 */
@DisplayName("MockWebServerExtension Error Handling")
class MockWebServerExtensionErrorHandlingTest {

    private static final CuiLogger LOGGER = new CuiLogger(MockWebServerExtensionErrorHandlingTest.class);

    // Constants for assertion messages to avoid duplication
    private static final String SERVER_SHOULD_BE_INJECTED = "Server should be injected";
    private static final String SERVER_SHOULD_BE_STARTED = "Server should be started";
    private static final String SSL_CONTEXT_SHOULD_BE_INJECTED = "SSL context should be injected";
    private static final String REQUEST_INTERRUPTED_MESSAGE = "Request was interrupted";
    private static final String URIBUILDER_SHOULD_BE_INJECTED = "URIBuilder should be injected";

    // tag::extension-error-handling[]
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
    @DisplayName("Server Error Tests")
    @EnableMockWebServer
    class ServerErrorTests {

        /**
         * Sets up the dispatcher for the test.
         * 
         * @param server the MockWebServer instance to configure
         */
        private void setupDispatcher(MockWebServer server) {
            server.setDispatcher(new Dispatcher() {
                @NotNull
                @Override
                public MockResponse dispatch(@NotNull RecordedRequest request) {
                    return new MockResponse.Builder()
                            .code(500)
                            .body("Error response")
                            .build();
                }
            });
        }

        @Test
        @DisplayName("Should handle server errors gracefully")
        void shouldHandleServerErrorsGracefully(MockWebServer server, URIBuilder uriBuilder) {
            // Setup the dispatcher directly instead of using the deprecated getDispatcher() method
            setupDispatcher(server);
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
                // Restore the interrupted status and throw a dedicated exception
                Thread.currentThread().interrupt();
                throw new MockWebServerTestException(REQUEST_INTERRUPTED_MESSAGE, e);
            }
        }
    }

    /**
     * Tests for handling of multiple annotations in the class hierarchy.
     */
    @Nested
    @DisplayName("Multiple Annotations Tests")
    @EnableMockWebServer(useHttps = true)
    class MultipleAnnotationsTests {

        @Test
        @DisplayName("Should use the first annotation found in hierarchy")
        void shouldUseFirstAnnotationFound(MockWebServer server, SSLContext sslContext) {
            assertNotNull(server, SERVER_SHOULD_BE_INJECTED);
            assertTrue(server.getStarted(), SERVER_SHOULD_BE_STARTED);
            assertNotNull(sslContext, SSL_CONTEXT_SHOULD_BE_INJECTED);

            // Verify HTTPS is enabled by checking the URL scheme
            assertTrue(server.url("/").url().toString().startsWith("https://"),
                    "Server URL should use HTTPS scheme");

            LOGGER.info("Server started with HTTPS on port: " + server.getPort());
        }

        /**
         * Nested class with a different annotation configuration.
         * The parent class annotation should take precedence.
         */
        @Nested
        @DisplayName("Nested With Different Config")
        @EnableMockWebServer(useHttps = false)
        class NestedWithDifferentConfig {

            @Test
            @DisplayName("Should use parent class annotation")
            void shouldUseParentClassAnnotation(MockWebServer server, SSLContext sslContext) {
                assertNotNull(server, SERVER_SHOULD_BE_INJECTED);
                assertTrue(server.getStarted(), SERVER_SHOULD_BE_STARTED);
                assertNotNull(sslContext, SSL_CONTEXT_SHOULD_BE_INJECTED);

                // Even though this class has useHttps=false, it should inherit useHttps=true from parent
                assertTrue(server.url("/").url().toString().startsWith("https://"),
                        "Server URL should use HTTPS scheme from parent config");
            }
        }
    }

    /**
     * Tests for handling of custom certificate errors.
     */
    @Nested
    @DisplayName("Certificate Error Tests")
    @EnableMockWebServer(useHttps = true)
    @TestProvidedCertificate
    class CertificateErrorTests {

        /**
         * Method to provide custom certificates for HTTPS testing.
         * This is used by the @TestProvidedCertificate annotation.
         * Returns null to simulate an error in certificate provision.
         * 
         * @return null to test fallback behavior
         */
        @TestProvidedCertificate
        public HandshakeCertificates provideCertificates() {
            // Return null to simulate an error in certificate provision
            return null;
        }

        /**
         * Sets up the dispatcher for the test.
         * 
         * @param server the MockWebServer instance to configure
         */
        private void setupDispatcher(MockWebServer server) {
            server.setDispatcher(CombinedDispatcher.createAPIDispatcher());
        }

        @Test
        @DisplayName("Should handle empty certificates gracefully")
        void shouldHandleEmptyCertificatesGracefully(MockWebServer server, SSLContext sslContext) {
            // Setup the dispatcher directly instead of using the deprecated getDispatcher() method
            setupDispatcher(server);
            assertNotNull(server, SERVER_SHOULD_BE_INJECTED + " despite certificate issues");
            assertTrue(server.getStarted(), SERVER_SHOULD_BE_STARTED);
            assertNotNull(sslContext, SSL_CONTEXT_SHOULD_BE_INJECTED);

            // The test returns empty for certificates, but the extension should fall back to self-signed
            LOGGER.info("Server started with fallback certificates on port: %s", server.getPort());
        }
    }

    /**
     * Tests for handling of server startup errors.
     */
    @Nested
    @DisplayName("Server Startup Tests")
    @EnableMockWebServer
    class ServerStartupTests {

        /**
         * Sets up the dispatcher for the test.
         * 
         * @param server the MockWebServer instance to configure
         */
        private void setupDispatcher(MockWebServer server) {
            server.setDispatcher(new Dispatcher() {
                @NotNull
                @Override
                public MockResponse dispatch(@NotNull RecordedRequest request) {
                    return new MockResponse.Builder()
                            .code(200)
                            .body("Success")
                            .build();
                }
            });
        }

        @Test
        @DisplayName("Should handle server port conflicts")
        void shouldHandleServerPortConflicts(MockWebServer server, URIBuilder uriBuilder) {
            // Setup the dispatcher directly instead of using the deprecated getDispatcher() method
            setupDispatcher(server);
            assertNotNull(server, SERVER_SHOULD_BE_INJECTED);
            assertTrue(server.getStarted(), SERVER_SHOULD_BE_STARTED);

            // Create a second server on a specific port to test port conflict handling
            try (MockWebServer secondServer = new MockWebServer()) {
                // Try to start on the same port - this should fail but be handled gracefully
                Exception exception = assertThrows(Exception.class, () ->
                        secondServer.start(server.getPort())
                );

                LOGGER.info("Expected port conflict exception: %s", exception.getMessage());

                // The original server should still be functional
                HttpClient client = HttpClient.newBuilder()
                        .connectTimeout(Duration.ofSeconds(2))
                        .build();

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(uriBuilder.build())
                        .GET()
                        .build();

                assertDoesNotThrow(() -> {
                    HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                    assertEquals(200, response.statusCode(), "Original server should still work");
                });
            } catch (Exception e) {
                LOGGER.error("Error in second server test", e);
            }
        }
    }
    // end::extension-error-handling[]
}
