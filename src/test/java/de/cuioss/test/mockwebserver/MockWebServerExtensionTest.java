/*
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
import de.cuioss.test.mockwebserver.dispatcher.ModuleDispatcher;
import mockwebserver3.MockWebServer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for verifying that the {@link MockWebServerExtension} works correctly
 * when using {@link EnableMockWebServer}.
 * <p>
 * This class tests the basic functionality of the extension, including server startup
 * and request handling with a custom dispatcher.
 */
@DisplayName("Basic MockWebServerExtension functionality tests")
@EnableMockWebServer
@ModuleDispatcher(provider = BaseAllAcceptDispatcher.class, providerMethod = "getOptimisticAPIDispatcher")
class MockWebServerExtensionTest {

    /**
     * Verifies that the extension provides a started MockWebServer instance.
     */
    @Test
    @DisplayName("Should provide a started server instance")
    void shouldProvideStartedServer(MockWebServer mockWebServer) {
        // Arrange - handled by extension

        // Act & Assert
        assertNotNull(mockWebServer, "MockWebServer should not be null");
        assertTrue(mockWebServer.getStarted(), "MockWebServer should be started");
    }

    /**
     * Verifies that the extension correctly handles a simple HTTP request using
     * the configured dispatcher.
     */
    @Test
    @DisplayName("Should handle a simple GET request")
    void shouldHandleSimpleRequest(URIBuilder uriBuilder) throws IOException, InterruptedException {
        // Arrange
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(uriBuilder.setPath("api").build())
                .GET()
                .build();

        // Act
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        // Assert
        assertNotNull(response, "Response should not be null");
        assertEquals(200, response.statusCode(), "Status code should be 200 OK");
    }
}
