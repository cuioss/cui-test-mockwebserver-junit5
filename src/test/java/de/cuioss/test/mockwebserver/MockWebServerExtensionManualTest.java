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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;


import mockwebserver3.Dispatcher;
import mockwebserver3.MockWebServer;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for verifying that the {@link MockWebServerExtension} works correctly
 * when manually registered with {@link ExtendWith}.
 */
@ExtendWith(MockWebServerExtension.class)
@EnableMockWebServer(manualStart = true)
class MockWebServerExtensionManualTest implements MockWebServerHolder {

    @Test
    void shouldProvideServerNotStartedServer(MockWebServer mockWebServer) {
        assertNotNull(mockWebServer, "Server should be injected even for manual start");
        assertFalse(mockWebServer.getStarted(), "Server should not be started");
        // Now start the server manually
        assertDoesNotThrow(() -> mockWebServer.start());
        assertTrue(mockWebServer.getStarted(), "Server should be started");
    }

    @Test
    void shouldHandleSimpleRequest(MockWebServer mockWebServer) throws IOException, InterruptedException {

        assertDoesNotThrow(() -> mockWebServer.start());

        // Create a proper URIBuilder now that the server is started
        URIBuilder properUriBuilder = URIBuilder.from(mockWebServer.url("/").url());

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(properUriBuilder.setPath("api").build())
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        assertNotNull(response);
        assertEquals(200, response.statusCode());

    }

    @Override
    public Dispatcher getDispatcher() {
        return CombinedDispatcher.createAPIDispatcher();
    }
}
