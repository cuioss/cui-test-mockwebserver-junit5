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
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLHandshakeException;


import mockwebserver3.Dispatcher;
import mockwebserver3.MockWebServer;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for HTTPS configuration with self-signed certificates in {@link MockWebServerExtension}.
 */
@EnableMockWebServer(useHttps = true)
class MockWebServerHttpsTest implements MockWebServerHolder {

    private static final CuiLogger LOGGER = new CuiLogger(MockWebServerHttpsTest.class);

    @Override
    public Dispatcher getDispatcher() {
        return CombinedDispatcher.createAPIDispatcher();
    }

    // tag::https-test[]
    @Test
    @DisplayName("Should configure HTTPS with self-signed certificates")
    void shouldConfigureHttps(MockWebServer server, SSLContext sslContext, URIBuilder uriBuilder) {
        assertNotNull(server, "Server should be injected");
        assertTrue(server.getStarted(), "Server should be started");

        // Verify SSL context is available
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
            assertEquals(200, response.statusCode(), "Should receive OK response over HTTPS");
            LOGGER.info("Successfully received HTTPS response: " + response.body());

        } catch (Exception e) {
            LOGGER.error("Failed to make HTTPS request", e);
            fail("Failed to make HTTPS request: " + e.getMessage());
        }
    }

    @Test
    @DisplayName("Should fail HTTPS connection without proper SSL context")
    void shouldFailWithoutProperSslContext(MockWebServer server, URIBuilder uriBuilder) {
        assertNotNull(server, "Server should be injected");
        assertTrue(server.getStarted(), "Server should be started");

        // Create default HTTPS client without the proper SSL context but with timeout
        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();

        // Make HTTPS request - should fail with SSL handshake exception
        Assertions.assertDoesNotThrow(() -> {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(uriBuilder.addPathSegments("api", "test").build())
                    .GET()
                    .build();

            // Verify it's an SSL handshake issue (or caused by one)
            Throwable cause = assertThrows(Exception.class, () -> client.send(request, HttpResponse.BodyHandlers.ofString()), "Request should fail with SSL handshake exception");
            boolean isHandshakeException = false;
            while (cause != null) {
                if (cause instanceof SSLHandshakeException) {
                    isHandshakeException = true;
                    break;
                }
                cause = cause.getCause();
            }

            assertTrue(isHandshakeException, "Exception should be related to SSL handshake failure");

        }, "Failed to create request or execute test: ");
    }
    // end::https-test[]
}
