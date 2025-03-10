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


import mockwebserver3.MockWebServer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Demonstrates how to use the {@link TestProvidedCertificate} annotation to provide 
 * certificates for HTTPS testing.
 * 
 * <p>This test shows how to use the new annotation-based approach for certificate provision,
 * which replaces the deprecated {@code testClassProvidesKeyMaterial} attribute and
 * {@code getTestProvidedHandshakeCertificates()} method.</p>
 * 
 * <p>The key advantages of this approach are:
 * <ul>
 *   <li>More flexible - certificates can be provided by methods with different signatures</li>
 *   <li>More declarative - the annotation clearly indicates the certificate provider</li>
 *   <li>Better separation of concerns - certificate provision is decoupled from test class</li>
 * </ul>
 * </p>
 */
// tag::annotation-certificate-example[]
@EnableMockWebServer(useHttps = true)
@TestProvidedCertificate(methodName = "createTestCertificates")
@DisplayName("Annotation-Provided Certificate Test")
class AnnotationProvidedCertificateTest {

    private static HandshakeCertificates testCertificates;

    /**
     * Creates and returns the certificates to be used for HTTPS testing.
     * This method is referenced by the {@link TestProvidedCertificate} annotation.
     * 
     * @return the HandshakeCertificates to be used for HTTPS
     */
    public static HandshakeCertificates createTestCertificates() {
        if (null == testCertificates) {
            // Create self-signed certificates with a short validity period (1 day) for unit tests
            testCertificates = KeyMaterialUtil.createSelfSignedHandshakeCertificates(1, KeyAlgorithm.RSA_2048);
        }
        return testCertificates;
    }

    /**
     * Tests a basic HTTPS connection using certificates provided via the annotation.
     * This demonstrates the most common use case for HTTPS testing with annotation-provided certificates.
     */
    @Test
    @DisplayName("Should successfully connect to HTTPS server with annotation-provided certificate")
    void shouldConnectToHttpsServer(MockWebServer server, URIBuilder serverURIBuilder, SSLContext sslContext)
            throws IOException, InterruptedException {
        // Arrange
        assertNotNull(testCertificates, "HandshakeCertificates should have been created");
        assertNotNull(sslContext, "SSLContext should be injected as a parameter");
        assertNotNull(server, "MockWebServer should be injected as a parameter");

        assertEquals("https", serverURIBuilder.getScheme(), "Server URL should use HTTPS");

        // Configure a dispatcher for the test
        server.setDispatcher(CombinedDispatcher.createAPIDispatcher());

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
        assertEquals(EndpointAnswerHandler.RESPONSE_SUCCESSFUL_BODY, response.body(),
                "Response body should match expected content");
    }

    /**
     * Tests that the SSLContext created from the annotation-provided certificates works correctly.
     */
    @Test
    @DisplayName("Should provide valid SSLContext from annotation-provided certificates")
    void shouldProvideValidSslContext(SSLContext sslContext) {
        // Arrange & Assert
        assertNotNull(testCertificates, "HandshakeCertificates should have been created");
        assertNotNull(sslContext, "SSLContext should be injected as a parameter");

        // Verify that we can convert back to HandshakeCertificates if needed
        HandshakeCertificates convertedCertificates = KeyMaterialUtil.convertToHandshakeCertificates(sslContext);
        assertNotNull(convertedCertificates, "Should be able to convert SSLContext back to HandshakeCertificates");
        assertNotNull(convertedCertificates.trustManager(), "TrustManager should not be null");
    }
}
// end::annotation-certificate-example[]
