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
import de.cuioss.test.mockwebserver.dispatcher.CombinedDispatcher;
import de.cuioss.test.mockwebserver.dispatcher.EndpointAnswerHandler;
import de.cuioss.tools.net.ssl.KeyAlgorithm;
import lombok.Getter;
import lombok.Setter;
import mockwebserver3.Dispatcher;
import mockwebserver3.MockWebServer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.net.ssl.SSLContext;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Demonstrates how to access a MockWebServer with HTTPS using Java's HttpClient.
 *
 * <p>This test shows how to configure Java's HttpClient to trust certificates provided by the extension.
 * The key to making HTTPS work with self-signed certificates is to use the same certificate material
 * for both the server and client.</p>
 *
 * <p>This test uses the "Extension → Test" approach with extension-provided certificates, where:
 * <ol>
 *   <li>The extension creates a self-signed certificate with a short validity period</li>
 *   <li>The extension configures the MockWebServer with this certificate</li>
 *   <li>The extension provides the certificate to the test via the setSslContext method</li>
 *   <li>The test configures Java's HttpClient with the same certificate</li>
 * </ol>
 * </p>
 *
 * <p>This approach simplifies HTTPS testing by eliminating the need for the test to create
 * and manage certificates manually.</p>
 */
@EnableMockWebServer(
        useHttps = true
)
@DisplayName("HttpClient HTTPS Test")
class ExtensionProvidedHttpsTest implements MockWebServerHolder {

    @Getter
    @Setter
    private MockWebServer mockWebServer;

    @Setter
    private SSLContext sslContext;

    /**
     * Tests a basic HTTPS connection to a default endpoint.
     * This demonstrates the most common use case for HTTPS testing.
     */
    @Test
    @DisplayName("Should successfully connect to HTTPS server with extension-provided certificate")
    void shouldConnectToHttpsServer(URL serverURL) throws IOException, InterruptedException, URISyntaxException {
        // Arrange
        assertNotNull(sslContext, "sslContext should be received from the extension");

        assertEquals("https", serverURL.getProtocol(), "Server URL should use HTTPS");

        // Configure HttpClient with the extension-provided certificates
        HttpClient client = HttpClient.newBuilder()
                .sslContext(sslContext)
                .connectTimeout(Duration.ofSeconds(10))
                .build();

        // Act: Make an HTTPS request
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(serverURL.toString() + "api/test"))
                .GET()
                .build();

        // Assert: Verify successful connection and response
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(200, response.statusCode(), "Should receive 200 OK response");
        assertEquals(EndpointAnswerHandler.RESPONSE_SUCCESSFUL_BODY, response.body(), "Response body should match expected content");
    }

    @Override
    public Dispatcher getDispatcher() {
        return CombinedDispatcher.createAPIDispatcher();
    }
}
