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
package de.cuioss.test.mockwebserver.dispatcher;

import de.cuioss.test.mockwebserver.EnableMockWebServer;
import de.cuioss.test.mockwebserver.MockWebServerHolder;
import de.cuioss.test.mockwebserver.URIBuilder;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;


import mockwebserver3.Dispatcher;
import mockwebserver3.MockResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Demonstrates how to use EndpointAnswerHandler for configuring responses in tests.
 */
// tag::dispatcher-example[]
@EnableMockWebServer
@DisplayName("Endpoint Dispatcher Example")
class EndpointDispatcherExampleTest implements MockWebServerHolder {

    @Test
    @DisplayName("Should handle requests using EndpointAnswerHandler")
    void testWithEndpointDispatcher(URIBuilder uriBuilder) throws IOException, InterruptedException {

        // Create HttpClient
        HttpClient client = HttpClient.newHttpClient();

        // Create request using the URIBuilder parameter
        HttpRequest request = HttpRequest.newBuilder()
                .uri(uriBuilder.addPathSegments("api", "data").build())
                .GET()
                .build();

        // Send request and verify response
        HttpResponse<String> response = client.send(request,
                HttpResponse.BodyHandlers.ofString());
        assertEquals(200, response.statusCode());
        assertEquals("{\"data\": \"test\"}", response.body());
    }

    @Override
    public Dispatcher getDispatcher() {
        // Create a dispatcher for the /api path
        var apiDispatcher = new BaseAllAcceptDispatcher("/api");

        // Configure the GET response handler with proper JSON and Content-Type
        apiDispatcher.getGetResult().setResponse(new MockResponse.Builder()
                .addHeader("Content-Type", "application/json")
                .body("{\"data\": \"test\"}")
                .code(HttpServletResponse.SC_OK)
                .build());

        // Return the dispatcher
        return new CombinedDispatcher(apiDispatcher);
    }
}
// end::dispatcher-example[]
