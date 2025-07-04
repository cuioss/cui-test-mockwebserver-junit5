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
package de.cuioss.test.mockwebserver;

import de.cuioss.test.mockwebserver.dispatcher.BaseAllAcceptDispatcher;
import de.cuioss.test.mockwebserver.dispatcher.CombinedDispatcher;
import de.cuioss.test.mockwebserver.dispatcher.ModuleDispatcher;
import de.cuioss.test.mockwebserver.ssl.KeyMaterialUtil;
import de.cuioss.tools.logging.CuiLogger;
import de.cuioss.tools.net.ssl.KeyAlgorithm;
import mockwebserver3.MockWebServer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Objects;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLHandshakeException;


import okhttp3.tls.HandshakeCertificates;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for certificate handling in {@link MockWebServerExtension}.
 * Focuses on edge cases and error handling related to SSL/TLS configuration.
 * <p>
 * This class consolidates all certificate-related tests to avoid duplication
 * across multiple test classes while ensuring comprehensive coverage. It tests:
 * <ul>
 *   <li>Self-signed certificate generation and usage</li>
 *   <li>Custom certificate handling</li>
 *   <li>Error handling for invalid certificates</li>
 *   <li>Certificate validation behavior</li>
 * </ul>
 *
 * @author Oliver Wolff
 */
@DisplayName("Certificate Handling - MockWebServerExtension")
@EnableMockWebServer(useHttps = true)
@TestProvidedCertificate
@ModuleDispatcher(provider = BaseAllAcceptDispatcher.class, providerMethod = "getOptimisticAPIDispatcher")
class MockWebServerExtensionCertificateTest {


    // Logger for test diagnostics - used in test setup and teardown
    private static final CuiLogger LOGGER = new CuiLogger(MockWebServerExtensionCertificateTest.class);

    // Constants for assertion messages to avoid duplication
    private static final String SERVER_SHOULD_BE_INJECTED = "Server should be injected";
    private static final String SERVER_SHOULD_BE_STARTED = "Server should be started";
    private static final String SSL_CONTEXT_SHOULD_BE_INJECTED = "SSLContext should be injected";
    private static final String REQUEST_INTERRUPTED_MESSAGE = "Request was interrupted";
    private static final String STATUS_CODE_ASSERTION_MESSAGE = "Response status code should match expected value";

    // Common test timeouts
    private static final Duration CONNECTION_TIMEOUT = Duration.ofSeconds(2);

    private SSLContext customSslContext;


    /**
     * Tests that the extension correctly creates and uses self-signed certificates.
     * This verifies that the basic HTTPS functionality works with the generated certificates.
     */
    @Test
    @DisplayName("Should create and use self-signed certificates")
    void shouldCreateAndUseSelfSignedCertificates(MockWebServer server, SSLContext sslContext) {
        // Arrange
        assertNotNull(server, SERVER_SHOULD_BE_INJECTED);
        assertTrue(server.getStarted(), SERVER_SHOULD_BE_STARTED);
        assertNotNull(sslContext, SSL_CONTEXT_SHOULD_BE_INJECTED);

        // Verify HTTPS is enabled by checking URL scheme
        assertTrue(server.url("/").url().toString().startsWith("https://"),
                "Server URL should use HTTPS scheme");

        LOGGER.debug("Server started with self-signed certificates on port: {}", server.getPort());

        // Create HTTP client with the provided SSL context
        HttpClient client = HttpClient.newBuilder()
                .sslContext(sslContext)
                .connectTimeout(CONNECTION_TIMEOUT)
                .build();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(server.url("/api").uri())
                .GET()
                .build();

        // Act & Assert
        assertDoesNotThrow(() -> {
            try {
                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                assertEquals(200, response.statusCode(), STATUS_CODE_ASSERTION_MESSAGE);
            } catch (InterruptedException e) {
                // Restore the interrupted status
                Thread.currentThread().interrupt();
                fail(REQUEST_INTERRUPTED_MESSAGE + ": " + e.getMessage());
            }
        });
    }

    /**
     * Tests that the extension correctly resolves and injects the SSLContext parameter.
     * This verifies that parameter resolution works as expected for SSL-related parameters.
     */
    @Test
    @DisplayName("Should resolve SSLContext parameter correctly")
    void shouldResolveSslContextParameter(MockWebServer server, SSLContext sslContext) {
        // Arrange
        assertNotNull(server, SERVER_SHOULD_BE_INJECTED);
        assertTrue(server.getStarted(), SERVER_SHOULD_BE_STARTED);
        assertNotNull(sslContext, SSL_CONTEXT_SHOULD_BE_INJECTED);

        // Create a custom SSL context to test error handling
        assertDoesNotThrow(() -> {
            customSslContext = SSLContext.getInstance("TLS");
            customSslContext.init(null, null, null);
        }, "Failed to create custom SSL context");

        assertNotNull(customSslContext, "Custom SSL context should be created");

        // Create HTTP client with the provided SSL context
        HttpClient client = HttpClient.newBuilder()
                .sslContext(sslContext) // Use the injected context, not our custom one
                .connectTimeout(CONNECTION_TIMEOUT)
                .build();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(server.url("/api").uri())
                .GET()
                .build();

        // Act & Assert
        assertDoesNotThrow(() -> {
            try {
                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                assertEquals(200, response.statusCode(), STATUS_CODE_ASSERTION_MESSAGE);
            } catch (InterruptedException e) {
                // Restore the interrupted status
                Thread.currentThread().interrupt();
                fail(REQUEST_INTERRUPTED_MESSAGE + ": " + e.getMessage());
            }
        }, "Request with injected SSL context should not throw an exception");
    }

    @Test
    @DisplayName("Should fail HTTPS connection without proper SSL context")
    void shouldFailWithoutProperSslContext(MockWebServer server, URIBuilder uriBuilder) {

        assertNotNull(server, SERVER_SHOULD_BE_INJECTED);
        assertTrue(server.getStarted(), SERVER_SHOULD_BE_STARTED);

        // Create default HTTPS client without the proper SSL context but with timeout
        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();

        // Make HTTPS request - should fail with SSL handshake exception
        assertDoesNotThrow(() -> {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(uriBuilder.addPathSegments("api", "test").build())
                    .GET()
                    .build();

            // Verify it's an SSL handshake issue (or caused by one)
            Exception exception = assertThrows(Exception.class, () -> client.send(request, HttpResponse.BodyHandlers.ofString()));

            // The exception should be related to SSL handshake
            Throwable cause = exception;
            boolean foundSslHandshakeException = false;

            // Look through the exception chain for SSLHandshakeException
            while (cause != null) {
                if (cause instanceof SSLHandshakeException) {
                    foundSslHandshakeException = true;
                    break;
                }
                cause = cause.getCause();
            }

            assertTrue(foundSslHandshakeException || Objects.requireNonNull(exception).getMessage().contains("SSL") ||
                    exception.getMessage().contains("certificate"),
                    "Exception should be related to SSL handshake: " + exception);

            LOGGER.info("Expected SSL handshake exception: {}", exception.getMessage());
        });
    }

    // end::certificate-tests[]

    /**
     * Test class for custom certificates provided by the test class.
     */
    @Nested
    @DisplayName("Custom Certificate Tests")
    @EnableMockWebServer(useHttps = true)
    @TestProvidedCertificate
    @ModuleDispatcher(provider = CombinedDispatcher.class, providerMethod = "createAPIDispatcher")
    class CustomCertificateTests {

        private HandshakeCertificates certificates;

        @BeforeEach
        void setUp() {
            try {
                // Create custom certificates before the test runs
                certificates = KeyMaterialUtil.createSelfSignedHandshakeCertificates(2, KeyAlgorithm.RSA_2048);
                LOGGER.info("Custom certificates created successfully");
            } catch (Exception e) {
                LOGGER.error("Failed to create custom certificates", e);
                fail("Failed to create custom certificates: " + e.getMessage());
            }
        }

        /**
         * Method to provide custom certificates for HTTPS testing.
         * This is used by the @TestProvidedCertificate annotation.
         * 
         * @return the HandshakeCertificates to be used
         */
        @SuppressWarnings("unused") // implicitly called by the test framework
        public HandshakeCertificates provideCertificates() {
            return certificates;
        }


        @Test
        @DisplayName("Should use certificates provided by test class")
        void shouldUseCustomCertificates(MockWebServer server, SSLContext sslContext, URIBuilder uriBuilder) {

            assertNotNull(server, SERVER_SHOULD_BE_INJECTED);
            assertTrue(server.getStarted(), SERVER_SHOULD_BE_STARTED);
            assertNotNull(sslContext, SSL_CONTEXT_SHOULD_BE_INJECTED);

            // Create HTTPS client with the injected SSL context and timeout
            HttpClient client = HttpClient.newBuilder()
                    .sslContext(sslContext)
                    .connectTimeout(Duration.ofSeconds(2))
                    .build();

            try {
                // Make HTTPS request with timeout using URIBuilder
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(uriBuilder.addPathSegments("api", "test").build())
                        .timeout(Duration.ofSeconds(2))
                        .GET()
                        .build();

                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                assertEquals(200, response.statusCode(), "Should receive OK response over HTTPS with custom certificates");
                LOGGER.info("Successfully received HTTPS response with custom certificates: " + response.body());
            } catch (InterruptedException e) {
                // Restore the interrupted status
                Thread.currentThread().interrupt();
                LOGGER.error(REQUEST_INTERRUPTED_MESSAGE, e);
                fail(REQUEST_INTERRUPTED_MESSAGE + ": " + e.getMessage());
            } catch (Exception e) {
                LOGGER.error("Failed to make HTTPS request with custom certificates", e);
                fail("Failed to make HTTPS request with custom certificates: " + e.getMessage());
            }
        }
    }

    /**
     * Test class for certificate fallback behavior.
     */
    @Nested
    @DisplayName("Certificate Fallback Tests")
    @EnableMockWebServer(useHttps = true)
    @TestProvidedCertificate
    @ModuleDispatcher(provider = CombinedDispatcher.class, providerMethod = "createAPIDispatcher")
    class CertificateFallbackTests {

        /**
         * Method to provide custom certificates for HTTPS testing.
         * This is used by the @TestProvidedCertificate annotation.
         * Returns null to simulate an error in certificate provision.
         * 
         * @return null to test fallback behavior
         */
        @SuppressWarnings("unused") // implicitly called by the test framework
        public HandshakeCertificates provideCertificates() {
            // Return null to simulate an error in certificate provision
            return null;
        }


        @Test
        @DisplayName("Should fallback to self-signed certificates when test class returns empty")
        void shouldFallbackToSelfSignedCertificates(MockWebServer server, SSLContext sslContext) {

            assertNotNull(server, SERVER_SHOULD_BE_INJECTED);
            assertTrue(server.getStarted(), SERVER_SHOULD_BE_STARTED);
            assertNotNull(sslContext, SSL_CONTEXT_SHOULD_BE_INJECTED);

            // The test is configured to provide certificates but returns empty
            // The extension should fall back to self-signed certificates
            LOGGER.info("Server started with fallback certificates on port: {}", server.getPort());

            // Verify the server is working with the fallback certificates
            HttpClient client = HttpClient.newBuilder()
                    .sslContext(sslContext)
                    .connectTimeout(Duration.ofSeconds(2))
                    .build();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(server.url("/api").uri())
                    .GET()
                    .build();

            assertDoesNotThrow(() -> {
                try {
                    HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                    assertEquals(200, response.statusCode(), "Request with fallback certificates should succeed");
                } catch (InterruptedException e) {
                    // Restore the interrupted status and rethrow with runtime exception
                    Thread.currentThread().interrupt();
                    throw new RuntimeException(REQUEST_INTERRUPTED_MESSAGE, e);
                }
            });
        }
    }
}
