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
import lombok.Getter;
import lombok.Setter;
import mockwebserver3.Dispatcher;
import mockwebserver3.MockWebServer;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for verifying that the {@link MockWebServerExtension} works correctly
 * when using {@link EnableMockWebServer}.
 */
@EnableMockWebServer
class MockWebServerExtensionTest implements MockWebServerHolder {

    @Getter
    @Setter
    private MockWebServer mockWebServer;

    @Test
    void shouldProvideServer() {
        assertNotNull(mockWebServer);
        assertTrue(mockWebServer.getStarted());
    }

    @Test
    void shouldHandleSimpleRequest() throws URISyntaxException, IOException, InterruptedException {
        String serverUrl = mockWebServer.url("/api").toString();
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(new URI(serverUrl))
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        assertNotNull(response);
        assertEquals(200, response.statusCode());

    }

    @Override
    public Dispatcher getDispatcher() {
        return new CombinedDispatcher(new BaseAllAcceptDispatcher("/api"));
    }
}
