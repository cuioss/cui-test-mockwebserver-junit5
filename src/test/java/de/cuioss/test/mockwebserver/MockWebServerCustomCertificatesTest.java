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
import de.cuioss.test.mockwebserver.dispatcher.ModuleDispatcher;
import de.cuioss.test.mockwebserver.ssl.KeyMaterialUtil;
import de.cuioss.tools.logging.CuiLogger;
import de.cuioss.tools.net.ssl.KeyAlgorithm;
import okhttp3.tls.HandshakeCertificates;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import javax.net.ssl.SSLContext;


import mockwebserver3.MockWebServer;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for HTTPS configuration with custom certificates provided by the test class.
 */
@EnableMockWebServer(useHttps = true)
@TestProvidedCertificate(methodName = "provideHandshakeCertificates")
@ModuleDispatcher(provider = CombinedDispatcher.class, providerMethod = "createAPIDispatcher")
class MockWebServerCustomCertificatesTest {

    private static final CuiLogger LOGGER = new CuiLogger(MockWebServerCustomCertificatesTest.class);

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
     * Provides custom certificates for HTTPS testing.
     * This method is called by the MockWebServerExtension via the @TestProvidedCertificate annotation.
     * 
     * @return the HandshakeCertificates to be used for HTTPS testing
     */
    public HandshakeCertificates provideHandshakeCertificates() {
        return certificates;
    }

    // The dispatcher is now provided via the @ModuleDispatcher annotation

    // tag::custom-certificates-test[]
    @Test
    @DisplayName("Should use certificates provided by test class")
    void shouldUseCustomCertificates(MockWebServer server, SSLContext sslContext, URIBuilder uriBuilder) {
        assertNotNull(server, "Server should be injected");
        assertTrue(server.getStarted(), "Server should be started");
        assertNotNull(sslContext, "SSL context should be injected");

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

        } catch (Exception e) {
            LOGGER.error("Failed to make HTTPS request with custom certificates", e);
            fail("Failed to make HTTPS request with custom certificates: " + e.getMessage());
        }
    }
    // end::custom-certificates-test[]
}
