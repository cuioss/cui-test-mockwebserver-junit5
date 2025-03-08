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

import de.cuioss.test.mockwebserver.dispatcher.BaseAllAcceptDispatcher;
import de.cuioss.test.mockwebserver.dispatcher.CombinedDispatcher;
import de.cuioss.tools.logging.CuiLogger;
import mockwebserver3.Dispatcher;
import mockwebserver3.MockWebServer;
import okhttp3.tls.HandshakeCertificates;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.net.ssl.SSLContext;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Tests for certificate handling in {@link MockWebServerExtension}.
 * Focuses on edge cases and error handling related to SSL/TLS configuration.
 */
@DisplayName("Tests for certificate handling in MockWebServerExtension")
@EnableMockWebServer(useHttps = true)
class MockWebServerExtensionCertificateTest implements MockWebServerHolder {

    private static final CuiLogger LOGGER = new CuiLogger(MockWebServerExtensionCertificateTest.class);

    // Constants for assertion messages to avoid duplication
    private static final String SERVER_SHOULD_BE_INJECTED = "Server should be injected";
    private static final String SERVER_SHOULD_BE_STARTED = "Server should be started";
    private static final String SSL_CONTEXT_SHOULD_BE_INJECTED = "SSLContext should be injected";
    private static final String REQUEST_INTERRUPTED_MESSAGE = "Request was interrupted";

    private SSLContext customSslContext;

    @Override
    public Dispatcher getDispatcher() {
        return new CombinedDispatcher(new BaseAllAcceptDispatcher("/api"));
    }

    @Override
    public Optional<HandshakeCertificates> getTestProvidedHandshakeCertificates() {
        // Return empty to test fallback to self-signed certificates
        return Optional.empty();
    }

    @Test
    @DisplayName("Should create and use self-signed certificates")
    void shouldCreateAndUseSelfSignedCertificates(MockWebServer server, SSLContext sslContext) {
        assertNotNull(server, SERVER_SHOULD_BE_INJECTED);
        assertTrue(server.getStarted(), SERVER_SHOULD_BE_STARTED);
        assertNotNull(sslContext, SSL_CONTEXT_SHOULD_BE_INJECTED);

        // Verify HTTPS is enabled by checking URL scheme
        assertTrue(server.url("/").url().toString().startsWith("https://"),
                "Server URL should use HTTPS scheme");

        LOGGER.info("Server started with self-signed certificates on port: {}", server.getPort());

        // Test a request with the provided SSL context
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
                assertEquals(200, response.statusCode(), "Request with self-signed certificates should succeed");
            } catch (InterruptedException e) {
                // Restore the interrupted status
                Thread.currentThread().interrupt();
                fail(REQUEST_INTERRUPTED_MESSAGE + ": " + e.getMessage());
            }
        });
    }

    @Test
    @DisplayName("Should resolve SSLContext parameter correctly")
    void shouldResolveSslContextParameter(MockWebServer server, SSLContext sslContext) {
        assertNotNull(server, SERVER_SHOULD_BE_INJECTED);
        assertTrue(server.getStarted(), SERVER_SHOULD_BE_STARTED);
        assertNotNull(sslContext, SSL_CONTEXT_SHOULD_BE_INJECTED);

        // Create a custom SSL context to test error handling
        assertDoesNotThrow(() -> Assertions.assertDoesNotThrow(() -> {
            customSslContext = SSLContext.getInstance("TLS");
            customSslContext.init(null, null, null);
        }, "Failed to create custom SSL context: "));

        assertNotNull(customSslContext, "Custom SSL context should be created");

        // Verify the server is working with the provided SSL context
        HttpClient client = HttpClient.newBuilder()
                .sslContext(sslContext) // Use the injected context, not our custom one
                .connectTimeout(Duration.ofSeconds(2))
                .build();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(server.url("/api").uri())
                .GET()
                .build();

        assertDoesNotThrow(() -> {
            try {
                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                assertEquals(200, response.statusCode(), "Request with injected SSL context should succeed");
            } catch (InterruptedException e) {
                // Restore the interrupted status
                Thread.currentThread().interrupt();
                fail(REQUEST_INTERRUPTED_MESSAGE + ": " + e.getMessage());
            }
        });
    }

}
