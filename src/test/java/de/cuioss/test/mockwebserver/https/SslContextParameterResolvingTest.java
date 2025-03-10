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
import de.cuioss.test.mockwebserver.URIBuilder;
import de.cuioss.test.mockwebserver.dispatcher.CombinedDispatcher;
import de.cuioss.test.mockwebserver.dispatcher.EndpointAnswerHandler;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import javax.net.ssl.SSLContext;

import mockwebserver3.Dispatcher;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Tests the parameter resolving functionality for SSLContext.
 * <p>
 * This test demonstrates how to use the SSLContext parameter resolving feature
 * to simplify HTTPS testing with MockWebServer.
 * </p>
 */
@EnableMockWebServer(
        useHttps = true
)
@DisplayName("SSLContext Parameter Resolving Test")
class SslContextParameterResolvingTest implements MockWebServerHolder {

    /**
     * Tests that the SSLContext can be directly injected as a parameter.
     * This demonstrates the simplest way to use the SSLContext parameter resolving feature.
     */
    @Test
    @DisplayName("Should inject SSLContext parameter directly")
    void shouldInjectSslContextParameter(URIBuilder serverURIBuilder, SSLContext sslContext) throws IOException, InterruptedException {
        // Arrange
        assertNotNull(sslContext, "SSLContext should be injected");
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

    @Override
    public Dispatcher getDispatcher() {
        return CombinedDispatcher.createAPIDispatcher();
    }
}
