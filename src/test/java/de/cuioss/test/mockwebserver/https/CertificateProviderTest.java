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
 * Demonstrates how to use the {@link TestProvidedCertificate} annotation with a provider class
 * to provide certificates for HTTPS testing.
 * 
 * <p>This test shows how to use a separate certificate provider class, which allows for
 * better reuse of certificate creation logic across multiple test classes.</p>
 */
// tag::provider-class-example[]
@EnableMockWebServer(useHttps = true)
@TestProvidedCertificate(providerClass = TestCertificateProvider.class)
@DisplayName("Certificate Provider Class Test")
class CertificateProviderTest {

    /**
     * Tests a basic HTTPS connection using certificates provided by the provider class.
     */
    @Test
    @DisplayName("Should successfully connect to HTTPS server with provider class certificate")
    void shouldConnectToHttpsServer(MockWebServer server, URIBuilder serverURIBuilder, SSLContext sslContext)
            throws IOException, InterruptedException {
        // Arrange
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
}

// end::provider-class-example[]

// tag::certificate-provider[]
/**
 * Certificate provider class that demonstrates how to create a reusable certificate provider.
 * This class can be referenced by multiple test classes to share certificate creation logic.
 */
class TestCertificateProvider {

    private static HandshakeCertificates certificates;

    /**
     * Private constructor to prevent instantiation.
     */
    private TestCertificateProvider() {
        // Utility class should not be instantiated
    }

    /**
     * Provides HandshakeCertificates for HTTPS testing.
     * This method will be called by the CertificateResolver when resolving certificates.
     * 
     * @return HandshakeCertificates to be used for HTTPS testing
     */
    public static HandshakeCertificates provideHandshakeCertificates() {
        if (certificates == null) {
            // Create self-signed certificates with a short validity period (1 day) for unit tests
            certificates = KeyMaterialUtil.createSelfSignedHandshakeCertificates(1, KeyAlgorithm.RSA_2048);
        }
        return certificates;
    }
}
// end::certificate-provider[]
