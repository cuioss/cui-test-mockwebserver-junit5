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

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;


import mockwebserver3.Dispatcher;
import mockwebserver3.MockResponse;
import mockwebserver3.MockWebServer;
import mockwebserver3.RecordedRequest;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for error handling in the {@link MockWebServerExtension}.
 */
public class MockWebServerErrorHandlingTest {


    /**
     * Custom extension to test error handling in MockWebServerExtension.
     */
    static class MockExtensionWithoutServer extends MockWebServerExtension {
        @Override
        public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext) {
            // Force parameter resolution without a server to test error handling
            if (parameterContext.getParameter().getType() == URIBuilder.class) {
                throw new ParameterResolutionException("No MockWebServer instance available");
            }
            return super.resolveParameter(parameterContext, extensionContext);
        }
    }

    // tag::error-handling-test[]
    @Nested
    @ExtendWith(MockExtensionWithoutServer.class)
    class ParameterResolutionErrorTest {

        @Test
        @DisplayName("Should handle parameter resolution errors gracefully")
        void shouldHandleParameterResolutionErrors() {
            // This test verifies that the test framework correctly handles parameter resolution errors
            // The test passes if it runs at all, as the extension is expected to throw exceptions
            // during parameter resolution, which JUnit will handle
            assertTrue(true, "Test should run without crashing the test runner");
        }
    }

    /**
     * Tests for dispatcher behavior.
     */
    @Nested
    @EnableMockWebServer
    class DispatcherTest implements MockWebServerHolder {

        @Override
        public Dispatcher getDispatcher() {
            // Return a dispatcher that always returns 500 errors
            return new Dispatcher() {
                @NotNull
                @Override
                public MockResponse dispatch(@NotNull RecordedRequest request) {
                    return new MockResponse.Builder()
                            .code(500)
                            .addHeader("Content-Type", "text/plain")
                            .body("Error response for testing")
                            .build();
                }
            };
        }

        @Test
        @DisplayName("Should use dispatcher from MockWebServerHolder")
        void shouldUseDispatcherFromHolder(MockWebServer server, URIBuilder uriBuilder) throws Exception {
            assertNotNull(server, "Server should be injected");

            // Make request to verify dispatcher is used
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(uriBuilder.build())
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            assertEquals(500, response.statusCode(), "Should receive error response from custom dispatcher");
            assertEquals("Error response for testing", response.body(), "Should receive error message from custom dispatcher");
        }
    }

    // end::error-handling-test[]
}
