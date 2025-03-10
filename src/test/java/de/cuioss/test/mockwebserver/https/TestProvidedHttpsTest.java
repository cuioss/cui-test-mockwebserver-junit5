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
package de.cuioss.test.mockwebserver.https;

import de.cuioss.test.mockwebserver.EnableMockWebServer;
import de.cuioss.test.mockwebserver.MockWebServerHolder;
import de.cuioss.test.mockwebserver.TestProvidedCertificate;
import de.cuioss.test.mockwebserver.URIBuilder;
import de.cuioss.test.mockwebserver.dispatcher.CombinedDispatcher;
import de.cuioss.test.mockwebserver.dispatcher.EndpointAnswerHandler;
import de.cuioss.test.mockwebserver.ssl.KeyMaterialUtil;
import de.cuioss.tools.net.ssl.KeyAlgorithm;
import okhttp3.tls.HandshakeCertificates;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import javax.net.ssl.SSLContext;


import mockwebserver3.Dispatcher;
import mockwebserver3.MockWebServer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Demonstrates how to access a MockWebServer with HTTPS using certificates provided by the test class.
 * 
 * <p>This test shows how to configure Java's HttpClient to trust certificates created by the test class.
 * The key to making HTTPS work with self-signed certificates is to use the same certificate material
 * for both the server and client.</p>
 * 
 * <p>This test uses the "Test → Extension" approach, where:
 * <ol>
 *   <li>The test creates a self-signed certificate</li>
 *   <li>The test provides the certificate to the extension via the getTestProvidedHandshakeCertificates method</li>
 *   <li>The extension configures the MockWebServer with this certificate</li>
 *   <li>The test configures Java's HttpClient with the same certificate</li>
 * </ol>
 * </p>
 * 
 * <p>This approach gives the test more control over certificate creation but requires more code
 * in the test class.</p>
 */
@EnableMockWebServer(useHttps = true)
@TestProvidedCertificate(methodName = "provideHandshakeCertificates")
@DisplayName("Test-Provided HTTPS Test")
class TestProvidedHttpsTest implements MockWebServerHolder {


    private HandshakeCertificates handshakeCertificates;

    /**
     * This method is called by the extension to get the certificates that should be used by the server.
     * In this case, we create self-signed certificates and provide them to the extension.
     * 
     * @return the HandshakeCertificates for the server to use
     */
    public HandshakeCertificates provideHandshakeCertificates() {
        // Create self-signed certificates with a short validity period (1 day) for unit tests
        if (null == handshakeCertificates) {
            handshakeCertificates = KeyMaterialUtil.createSelfSignedHandshakeCertificates(1, KeyAlgorithm.RSA_2048);
        }
        return handshakeCertificates;
    }

    /**
     * Tests a basic HTTPS connection to a default endpoint.
     * This demonstrates the most common use case for HTTPS testing with test-provided certificates.
     */
    @Test
    @DisplayName("Should successfully connect to HTTPS server with test-provided certificate")
    void shouldConnectToHttpsServer(MockWebServer server, URIBuilder serverURIBuilder, SSLContext sslContext) throws IOException, InterruptedException {
        // Arrange
        assertNotNull(handshakeCertificates, "HandshakeCertificates should have been created by the test");
        assertNotNull(sslContext, "SSLContext should be injected as a parameter");

        assertEquals("https", serverURIBuilder.getScheme(), "Server URL should use HTTPS");

        // Configure HttpClient with the injected SSLContext
        HttpClient client = HttpClient.newBuilder()
                .sslContext(sslContext)
                .connectTimeout(Duration.ofSeconds(10))
                .build();

        // Act: Make an HTTPS request
        HttpRequest request = HttpRequest.newBuilder()
                .uri(serverURIBuilder.setPath("/api/test").build())
                .GET()
                .build();

        // Assert: Verify successful connection and response
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(200, response.statusCode(), "Should receive 200 OK response");
        assertEquals(EndpointAnswerHandler.RESPONSE_SUCCESSFUL_BODY, response.body(), "Response body should match expected content");
    }


    /**
     * Tests the round-trip conversion between HandshakeCertificates and SSLContext.
     * This demonstrates how to convert between the two formats while maintaining compatibility.
     */
    @Test
    @DisplayName("Should support round-trip conversion between HandshakeCertificates and SSLContext")
    void shouldSupportRoundTripConversion(SSLContext sslContext) {
        // Arrange
        assertNotNull(handshakeCertificates, "HandshakeCertificates should have been created by the test");
        assertNotNull(sslContext, "SSLContext should be injected as a parameter");

        // Convert SSLContext back to HandshakeCertificates
        HandshakeCertificates convertedCertificates = KeyMaterialUtil.convertToHandshakeCertificates(sslContext);
        assertNotNull(convertedCertificates, "HandshakeCertificates should be created from SSLContext");
        assertNotNull(convertedCertificates.trustManager(), "TrustManager should not be null");

        // Verify that the converted certificates can be used to create a new SSLContext
        SSLContext roundTripContext = KeyMaterialUtil.createSslContext(convertedCertificates);
        assertNotNull(roundTripContext, "Round-trip SSLContext should be created from converted HandshakeCertificates");

        // Verify that both the original and converted certificates have trust managers
        assertNotNull(handshakeCertificates.trustManager(), "Original HandshakeCertificates should have a TrustManager");
        assertNotNull(convertedCertificates.trustManager(), "Converted HandshakeCertificates should have a TrustManager");
    }

    @Override
    public Dispatcher getDispatcher() {
        return CombinedDispatcher.createAPIDispatcher();
    }
}
