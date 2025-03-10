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

import de.cuioss.tools.logging.CuiLogger;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import mockwebserver3.Dispatcher;
import mockwebserver3.MockResponse;
import mockwebserver3.MockWebServer;
import mockwebserver3.RecordedRequest;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for unique corner cases in {@link MockWebServerExtension} that aren't covered elsewhere.
 * <p>
 * Note: Most corner cases and error handling tests have been consolidated into
 * {@link MockWebServerExtensionErrorHandlingTest} and {@link MockWebServerExtensionCertificateTest}.
 * This class only contains tests for specific edge cases not covered by those classes.
 */
@DisplayName("MockWebServerExtension Unique Corner Cases")
class MockWebServerExtensionCornerCasesTest {

    private static final CuiLogger LOGGER = new CuiLogger(MockWebServerExtensionCornerCasesTest.class);

    // Constants for assertion messages to avoid duplication
    private static final String SERVER_SHOULD_BE_INJECTED = "Server should be injected";
    private static final String SERVER_SHOULD_BE_STARTED = "Server should be started";
    private static final String REQUEST_INTERRUPTED_MESSAGE = "Request was interrupted";

    // tag::extension-corner-cases[]
    /**
     * Tests for handling of custom response headers.
     */
    @Nested
    @DisplayName("Custom Response Headers Tests")
    @EnableMockWebServer
    class CustomResponseHeadersTests implements MockWebServerHolder {

        @Override
        public Dispatcher getDispatcher() {
            return new Dispatcher() {
                @NotNull
                @Override
                public MockResponse dispatch(@NotNull RecordedRequest request) {
                    return new MockResponse.Builder()
                            .code(200)
                            .addHeader("X-Custom-Header", "custom-value")
                            .addHeader("Content-Type", "application/json")
                            .body("{\"status\": \"success\"}")
                            .build();
                }
            };
        }

        @Test
        @DisplayName("Should handle custom response headers")
        void shouldHandleCustomResponseHeaders(MockWebServer server, URIBuilder uriBuilder) {
            assertNotNull(server, SERVER_SHOULD_BE_INJECTED);
            assertTrue(server.getStarted(), SERVER_SHOULD_BE_STARTED);

            // Create HTTP client with timeout
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(2))
                    .build();

            try {
                // Make request to test custom headers
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(uriBuilder.build())
                        .GET()
                        .build();

                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

                // Verify response code and body
                assertEquals(200, response.statusCode(), "Should receive success response");
                assertEquals("{\"status\": \"success\"}", response.body(), "Should receive correct response body");

                // Verify custom headers
                assertEquals("custom-value", response.headers().firstValue("X-Custom-Header").orElse(null),
                        "Should have custom header");
                assertEquals("application/json", response.headers().firstValue("Content-Type").orElse(null),
                        "Should have correct content type");

                LOGGER.info("Successfully verified custom response headers");

            } catch (IOException e) {
                fail("Request should not throw exception: " + e.getMessage());
            } catch (InterruptedException e) {
                // Restore the interrupted status and throw a dedicated exception
                Thread.currentThread().interrupt();
                throw new MockWebServerTestException(REQUEST_INTERRUPTED_MESSAGE, e);
            }
        }
    }
    // end::extension-corner-cases[]
}
