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
package de.cuioss.test.mockwebserver.https;

import de.cuioss.test.mockwebserver.EnableMockWebServer;
import de.cuioss.test.mockwebserver.TestProvidedCertificate;
import de.cuioss.test.mockwebserver.URIBuilder;
import de.cuioss.test.mockwebserver.dispatcher.BaseAllAcceptDispatcher;
import de.cuioss.test.mockwebserver.dispatcher.EndpointAnswerHandler;
import de.cuioss.test.mockwebserver.dispatcher.ModuleDispatcher;
import de.cuioss.test.mockwebserver.ssl.KeyMaterialUtil;
import de.cuioss.tools.net.ssl.KeyAlgorithm;
import mockwebserver3.MockWebServer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLHandshakeException;


import okhttp3.tls.HandshakeCertificates;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive test suite for HTTPS functionality in the MockWebServer extension.
 * <p>
 * This suite demonstrates three different approaches for HTTPS testing:
 * <ol>
 *   <li>Extension-provided certificates: The extension creates and manages certificates</li>
 *   <li>Test-provided certificates: The test class creates and provides certificates</li>
 *   <li>Provider class certificates: A separate provider class creates and provides certificates</li>
 * </ol>
 * </p>
 */
@DisplayName("HTTPS Test Suite")
class HttpsTests {

    // Constants for test assertions
    private static final String HTTPS_SCHEME = "https";
    private static final String SSL_CONTEXT_ASSERTION_MESSAGE = "SSLContext should be injected as a parameter";
    private static final String URL_BUILDER_ASSERTION_MESSAGE = "URL builder should be injected as a parameter";
    private static final String HTTPS_SCHEME_ASSERTION_MESSAGE = "Server URL should use HTTPS scheme";
    private static final String STATUS_CODE_ASSERTION_MESSAGE = "Should receive 200 OK response";
    private static final String RESPONSE_BODY_ASSERTION_MESSAGE = "Response body should match expected content";
    private static final String CERTIFICATES_SHOULD_NOT_BE_NULL = "HandshakeCertificates should not be null";
    private static final String KEY_MANAGER_ASSERTION_MESSAGE = "KeyManager should not be null";
    private static final Duration CONNECTION_TIMEOUT = Duration.ofSeconds(10);

    /**
     * Tests for extension-provided certificates.
     * <p>
     * This approach simplifies HTTPS testing by eliminating the need for the test to create
     * and manage certificates manually.
     * </p>
     * <p>
     * The extension creates a self-signed certificate with a short validity period,
     * configures the MockWebServer with this certificate, and provides the SSLContext
     * directly as a parameter.
     * </p>
     */
    @Nested
    @DisplayName("Extension-Provided Certificate Tests")
    @EnableMockWebServer(useHttps = true)
    @ModuleDispatcher(provider = BaseAllAcceptDispatcher.class, providerMethod = "getOptimisticAPIDispatcher")
    class ExtensionProvidedCertificateTests {

        @Test
        @DisplayName("Should successfully connect to HTTPS server with extension-provided certificate")
        void shouldConnectToHttpsServer(URIBuilder serverURIBuilder, SSLContext sslContext) throws IOException, InterruptedException {
            // Arrange
            assertNotNull(sslContext, SSL_CONTEXT_ASSERTION_MESSAGE);
            assertNotNull(serverURIBuilder, URL_BUILDER_ASSERTION_MESSAGE);

            // Verify the URL builder creates HTTPS URLs
            URI uri = serverURIBuilder.build();
            assertEquals(HTTPS_SCHEME, uri.getScheme(), HTTPS_SCHEME_ASSERTION_MESSAGE);

            // Configure HttpClient with the injected SSLContext
            HttpClient client = HttpClient.newBuilder()
                    .sslContext(sslContext)
                    .connectTimeout(CONNECTION_TIMEOUT)
                    .build();

            // Act: Make an HTTPS request using the URL builder
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(serverURIBuilder.addPathSegments("api", "test").build())
                    .GET()
                    .build();

            // Assert: Verify successful connection and response
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            assertEquals(200, response.statusCode(), STATUS_CODE_ASSERTION_MESSAGE);
            assertEquals(EndpointAnswerHandler.RESPONSE_SUCCESSFUL_BODY, response.body(), RESPONSE_BODY_ASSERTION_MESSAGE);
        }

        @Test
        @DisplayName("Should fail to connect with default SSL context")
        void shouldFailWithDefaultSslContext(URIBuilder serverURIBuilder) {
            // Arrange
            assertNotNull(serverURIBuilder, URL_BUILDER_ASSERTION_MESSAGE);
            assertEquals(HTTPS_SCHEME, serverURIBuilder.getScheme(), HTTPS_SCHEME_ASSERTION_MESSAGE);

            // Configure HttpClient with default SSLContext (which doesn't trust our self-signed cert)
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(CONNECTION_TIMEOUT)
                    .build();

            // Act & Assert: Verify that connection fails with default SSL context
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(serverURIBuilder.addPathSegments("api", "test").build())
                    .GET()
                    .build();

            assertThrows(SSLHandshakeException.class, () ->
                    client.send(request, HttpResponse.BodyHandlers.ofString())
            , "Connection should fail with default SSL context");
        }
    }

    /**
     * Tests for test-provided certificates.
     * <p>
     * This approach allows the test to create and provide its own certificates.
     * It gives the test more control over the certificate creation process.
     * </p>
     * <p>
     * The test creates self-signed certificates and provides them to the extension.
     * The extension then configures the MockWebServer with this certificate.
     * </p>
     */
    @Nested
    @DisplayName("Test-Provided Certificate Tests")
    @EnableMockWebServer(useHttps = true)
    @TestProvidedCertificate(methodName = "provideHandshakeCertificates")
    @ModuleDispatcher(provider = BaseAllAcceptDispatcher.class, providerMethod = "getOptimisticAPIDispatcher")
    class TestProvidedCertificateTests {

        private HandshakeCertificates handshakeCertificates;

        /**
         * This method is called by the extension to get the certificates that should be used by the server.
         * In this case, we create self-signed certificates and provide them to the extension.
         *
         * @return the HandshakeCertificates for the server to use
         */
        @SuppressWarnings("unused") // implicitly called by the test framework
        public HandshakeCertificates provideHandshakeCertificates() {
            // Create self-signed certificates with a short validity period (1 day) for unit tests
            if (null == handshakeCertificates)
                handshakeCertificates = KeyMaterialUtil.createSelfSignedHandshakeCertificates(1, KeyAlgorithm.RSA_2048);
            return handshakeCertificates;
        }

        @Test
        @DisplayName("Should successfully connect to HTTPS server with test-provided certificate")
        void shouldConnectToHttpsServer(URIBuilder serverURIBuilder, SSLContext sslContext, MockWebServer mockWebServer) throws IOException, InterruptedException {
            // Arrange
            assertNotNull(handshakeCertificates, "HandshakeCertificates should have been created by the test");
            assertNotNull(sslContext, SSL_CONTEXT_ASSERTION_MESSAGE);
            assertNotNull(mockWebServer, "MockWebServer should be injected as a parameter");

            assertEquals(HTTPS_SCHEME, serverURIBuilder.getScheme(), HTTPS_SCHEME_ASSERTION_MESSAGE);

            // Configure HttpClient with the injected SSLContext
            HttpClient client = HttpClient.newBuilder()
                    .sslContext(sslContext)
                    .connectTimeout(CONNECTION_TIMEOUT)
                    .build();

            // Act: Make an HTTPS request
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(serverURIBuilder.addPathSegments("api", "test").build())
                    .GET()
                    .build();

            // Assert: Verify successful connection and response
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            assertEquals(200, response.statusCode(), STATUS_CODE_ASSERTION_MESSAGE);
            assertEquals(EndpointAnswerHandler.RESPONSE_SUCCESSFUL_BODY, response.body(), RESPONSE_BODY_ASSERTION_MESSAGE);
        }
    }

    /**
     * Tests for provider class certificates.
     * <p>
     * This approach allows for reuse of certificate creation logic across multiple test classes.
     * It's useful when you have many tests that need the same certificates.
     * </p>
     * <p>
     * The {@link TestCertificateProvider} class creates and provides the certificates.
     * The extension then configures the MockWebServer with these certificates.
     * </p>
     */
    @Nested
    @DisplayName("Provider Class Certificate Tests")
    @EnableMockWebServer(useHttps = true)
    @TestProvidedCertificate(providerClass = TestCertificateProvider.class, methodName = "provideHandshakeCertificates")
    @ModuleDispatcher(provider = BaseAllAcceptDispatcher.class, providerMethod = "getOptimisticAPIDispatcher")
    class ProviderClassCertificateTests {

        @Test
        @DisplayName("Should successfully connect to HTTPS server with provider class certificate")
        void shouldConnectToHttpsServer(MockWebServer server, URIBuilder serverURIBuilder, SSLContext sslContext) throws IOException, InterruptedException {
            // Arrange
            assertNotNull(sslContext, SSL_CONTEXT_ASSERTION_MESSAGE);
            assertNotNull(serverURIBuilder, URL_BUILDER_ASSERTION_MESSAGE);
            assertNotNull(server, "MockWebServer should be injected as a parameter");

            assertEquals(HTTPS_SCHEME, serverURIBuilder.getScheme(), HTTPS_SCHEME_ASSERTION_MESSAGE);

            // Configure HttpClient with the injected SSLContext
            HttpClient client = HttpClient.newBuilder()
                    .sslContext(sslContext)
                    .connectTimeout(CONNECTION_TIMEOUT)
                    .build();

            // Act: Make an HTTPS request
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(serverURIBuilder.addPathSegments("api", "test").build())
                    .GET()
                    .build();

            // Assert: Verify successful connection and response
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            assertEquals(200, response.statusCode(), STATUS_CODE_ASSERTION_MESSAGE);
            assertEquals(EndpointAnswerHandler.RESPONSE_SUCCESSFUL_BODY, response.body(), RESPONSE_BODY_ASSERTION_MESSAGE);
        }

        /**
         * Tests the round-trip conversion between HandshakeCertificates and SSLContext.
         * This demonstrates how to convert between the two formats while maintaining compatibility.
         */
        @Test
        @DisplayName("Should support round-trip conversion between SSLContext and HandshakeCertificates")
        void shouldSupportRoundTripConversion(SSLContext sslContext) {
            // Arrange & Assert
            assertNotNull(sslContext, SSL_CONTEXT_ASSERTION_MESSAGE);

            // Act: Convert SSLContext back to HandshakeCertificates
            HandshakeCertificates convertedCertificates = KeyMaterialUtil.convertToHandshakeCertificates(sslContext);

            // Assert: Verify the conversion was successful
            assertNotNull(convertedCertificates, CERTIFICATES_SHOULD_NOT_BE_NULL);
            assertNotNull(convertedCertificates.keyManager(), KEY_MANAGER_ASSERTION_MESSAGE);
            assertNotNull(convertedCertificates.trustManager(), "TrustManager should not be null");
        }
    }

    /**
     * Tests for annotation-provided certificates.
     * <p>
     * This approach demonstrates how to use the {@link TestProvidedCertificate} annotation
     * to provide certificates for HTTPS testing.
     * </p>
     * <p>
     * The key advantages of this approach are:
     * <ul>
     *   <li>More flexible - certificates can be provided by methods with different signatures</li>
     *   <li>More declarative - the annotation clearly indicates the certificate provider</li>
     *   <li>Better separation of concerns - certificate provision is decoupled from test class</li>
     * </ul>
     * </p>
     */
    @Nested
    @DisplayName("Annotation-Provided Certificate Tests")
    @EnableMockWebServer(useHttps = true)
    @TestProvidedCertificate(methodName = "createTestCertificates")
    class AnnotationProvidedCertificateTests {

        private static HandshakeCertificates testCertificates;

        /**
         * Creates and returns the certificates to be used for HTTPS testing.
         * This method is referenced by the {@link TestProvidedCertificate} annotation.
         *
         * @return the HandshakeCertificates to be used for HTTPS
         */
        @SuppressWarnings("unused") // implicitly called by the test framework
        public static HandshakeCertificates createTestCertificates() {
            if (null == testCertificates)
                testCertificates = KeyMaterialUtil.createSelfSignedHandshakeCertificates(1, KeyAlgorithm.RSA_2048);
            return testCertificates;
        }

        @Test
        @DisplayName("Should provide valid SSLContext from annotation-provided certificates")
        void shouldProvideValidSslContext(SSLContext sslContext) {
            // Arrange & Assert
            assertNotNull(testCertificates, "HandshakeCertificates should have been created");
            assertNotNull(sslContext, SSL_CONTEXT_ASSERTION_MESSAGE);

            // Verify that we can convert back to HandshakeCertificates if needed
            HandshakeCertificates convertedCertificates = KeyMaterialUtil.convertToHandshakeCertificates(sslContext);
            assertNotNull(convertedCertificates, "Should be able to convert SSLContext back to HandshakeCertificates");
            assertNotNull(convertedCertificates.trustManager(), "TrustManager should not be null");
        }
    }

    /**
     * Tests for parameter resolving functionality for SSLContext.
     * <p>
     * This test demonstrates how to use the SSLContext parameter resolving feature
     * to simplify HTTPS testing with MockWebServer.
     * </p>
     */
    @Nested
    @DisplayName("SSLContext Parameter Resolving Tests")
    @EnableMockWebServer(useHttps = true)
    @ModuleDispatcher(provider = BaseAllAcceptDispatcher.class, providerMethod = "getOptimisticAPIDispatcher")
    class SslContextParameterResolvingTests {

        @Test
        @DisplayName("Should inject SSLContext parameter directly")
        void shouldInjectSslContextParameter(URIBuilder serverURIBuilder, SSLContext sslContext) throws IOException, InterruptedException {
            // Arrange
            assertNotNull(sslContext, SSL_CONTEXT_ASSERTION_MESSAGE);
            assertEquals(HTTPS_SCHEME, serverURIBuilder.getScheme(), HTTPS_SCHEME_ASSERTION_MESSAGE);

            // Configure HttpClient with the injected SSLContext
            HttpClient client = HttpClient.newBuilder()
                    .sslContext(sslContext)
                    .connectTimeout(CONNECTION_TIMEOUT)
                    .build();

            // Act: Make an HTTPS request
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(serverURIBuilder.setPath("/api/test").build())
                    .GET()
                    .build();

            // Assert: Verify successful connection and response
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            assertEquals(200, response.statusCode(), STATUS_CODE_ASSERTION_MESSAGE);
            assertEquals(EndpointAnswerHandler.RESPONSE_SUCCESSFUL_BODY, response.body(), RESPONSE_BODY_ASSERTION_MESSAGE);
        }
    }
}
