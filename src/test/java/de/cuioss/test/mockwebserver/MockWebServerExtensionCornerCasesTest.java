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

import de.cuioss.test.mockwebserver.mockresponse.MockResponseConfig;
import de.cuioss.tools.logging.CuiLogger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;

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

    // Constants for response headers and body
    private static final String CUSTOM_HEADER_NAME = "X-Custom-Header";
    private static final String CUSTOM_HEADER_VALUE = "custom-value";
    private static final String CONTENT_TYPE_HEADER = "Content-Type";
    private static final String JSON_CONTENT_TYPE = "application/json";
    private static final String SUCCESS_RESPONSE_BODY = "{\"status\": \"success\"}";

    @Nested
    @DisplayName("Custom Response Headers Tests")
    @EnableMockWebServer
    class CustomResponseHeadersTests {

        @Test
        @DisplayName("Should handle custom response headers")
        @MockResponseConfig(status = 200, textContent = SUCCESS_RESPONSE_BODY, headers = {
                CUSTOM_HEADER_NAME + "=" + CUSTOM_HEADER_VALUE,
                CONTENT_TYPE_HEADER + "=" + JSON_CONTENT_TYPE
        })
        void shouldHandleCustomResponseHeaders(URIBuilder uriBuilder) throws IOException, InterruptedException {

            // Create HTTP client with timeout
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(2))
                    .build();

            // Make request to test custom headers
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(uriBuilder.build())
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            // Verify response code and body
            assertEquals(200, response.statusCode(), "Should receive success response");
            assertEquals(SUCCESS_RESPONSE_BODY, response.body(), "Should receive correct response body");

            // Verify custom headers
            assertEquals(CUSTOM_HEADER_VALUE, response.headers().firstValue(CUSTOM_HEADER_NAME).orElse(null),
                    "Should have custom header");
            assertEquals(JSON_CONTENT_TYPE, response.headers().firstValue(CONTENT_TYPE_HEADER).orElse(null),
                    "Should have correct content type");

            LOGGER.info("Successfully verified custom response headers");

        }
    }

}
