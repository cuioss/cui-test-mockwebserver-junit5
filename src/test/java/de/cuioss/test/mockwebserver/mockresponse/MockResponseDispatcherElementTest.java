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
package de.cuioss.test.mockwebserver.mockresponse;

import de.cuioss.test.mockwebserver.dispatcher.HttpMethodMapper;
import mockwebserver3.RecordedRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.net.Socket;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Stream;


import okio.Buffer;
import okhttp3.Headers;

import static de.cuioss.test.mockwebserver.mockresponse.MockResponseTestUtil.*;
import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Tests for MockResponseDispatcherElement")
class MockResponseDispatcherElementTest {

    // Using constants from MockResponseTestUtil

    @Nested
    @DisplayName("Constructor tests")
    class ConstructorTests {

        @Test
        @DisplayName("Should create instance with minimal configuration")
        void shouldCreateWithMinimalConfig() {
            // Arrange
            var annotation = createMockResponse(DEFAULT_PATH, HttpMethodMapper.GET, DEFAULT_STATUS);

            // Act
            var element = new MockResponseDispatcherElement(annotation);

            // Assert
            assertEquals(DEFAULT_PATH, element.getBaseUrl(), "Base URL should match the path from annotation");
            assertEquals(Set.of(HttpMethodMapper.GET), element.supportedMethods(), "Should support GET method");
        }

        @Test
        @DisplayName("Should throw exception when multiple content types are specified")
        void shouldThrowExceptionForMultipleContentTypes() {
            // Arrange
            var annotation = builder()
                    .withPath(DEFAULT_PATH)
                    .withMethod(HttpMethodMapper.GET)
                    .withStatus(DEFAULT_STATUS)
                    .withTextContent(DEFAULT_TEXT_CONTENT)
                    .withJsonContent(DEFAULT_JSON_CONTENT)
                    .build();

            // Act & Assert
            assertThrows(IllegalArgumentException.class, () -> new MockResponseDispatcherElement(annotation),
                    "Should throw exception when multiple content types are specified");
        }
    }

    @Nested
    @DisplayName("Content type handling tests")
    class ContentTypeTests {

        @Test
        @DisplayName("Should set text/plain content type for textContent")
        void shouldSetTextPlainContentType() {
            // Arrange
            var annotation = createMockResponseWithTextContent(DEFAULT_PATH, HttpMethodMapper.GET, DEFAULT_STATUS,
                    DEFAULT_TEXT_CONTENT);

            // Act
            var element = new MockResponseDispatcherElement(annotation);
            RecordedRequest request = createTestRequest();
            var response = element.handleGet(request).orElseThrow();

            // Assert
            assertEquals("text/plain", response.getHeaders().get(CONTENT_TYPE_HEADER),
                    "Content-Type should be text/plain for textContent");
            assertEquals(DEFAULT_TEXT_CONTENT, getBodyContent(response),
                    "Response body should match textContent");
        }

        @Test
        @DisplayName("Should set application/json content type for jsonContentKeyValue")
        void shouldSetJsonContentType() {
            // Arrange
            var annotation = createMockResponseWithJsonContent(
                    DEFAULT_PATH, HttpMethodMapper.GET, DEFAULT_STATUS, DEFAULT_JSON_CONTENT);

            // Act
            var element = new MockResponseDispatcherElement(annotation);
            RecordedRequest request = createTestRequest();
            var response = element.handleGet(request).orElseThrow();

            // Assert
            assertEquals("application/json", response.getHeaders().get(CONTENT_TYPE_HEADER),
                    "Content-Type should be application/json for jsonContentKeyValue");
            assertEquals("{\"key\":\"value\"}", getBodyContent(response),
                    "Response body should be properly formatted JSON");
        }

        @Test
        @DisplayName("Should not set content type for stringContent")
        void shouldNotSetContentTypeForStringContent() {
            // Arrange
            var annotation = createMockResponseWithStringContent(
                    DEFAULT_PATH, HttpMethodMapper.GET, DEFAULT_STATUS, DEFAULT_STRING_CONTENT);

            // Act
            var element = new MockResponseDispatcherElement(annotation);
            RecordedRequest request = createTestRequest();
            var response = element.handleGet(request).orElseThrow();

            // Assert
            assertNull(response.getHeaders().get(CONTENT_TYPE_HEADER),
                    "Content-Type should not be set for stringContent");
            assertEquals(DEFAULT_STRING_CONTENT, getBodyContent(response),
                    "Response body should match stringContent");
        }

        @Test
        @DisplayName("Should use explicit content type when provided")
        void shouldUseExplicitContentType() {
            // Arrange
            var annotation = createMockResponseWithContentType(
                    DEFAULT_PATH, HttpMethodMapper.GET, DEFAULT_STATUS, DEFAULT_TEXT_CONTENT, "text/html");

            // Act
            var element = new MockResponseDispatcherElement(annotation);
            RecordedRequest request = createTestRequest();
            var response = element.handleGet(request).orElseThrow();

            // Assert
            assertEquals("text/html", response.getHeaders().get(CONTENT_TYPE_HEADER),
                    "Content-Type should match explicitly provided value");
        }
    }

    @Nested
    @DisplayName("HTTP method handling tests")
    class HttpMethodTests {

        @ParameterizedTest
        @MethodSource("httpMethodTestCases")
        @DisplayName("Should handle different HTTP methods correctly")
        void shouldHandleHttpMethods(HttpMethodMapper method,
                RecordedRequest request,
                boolean shouldHandleGet,
                boolean shouldHandlePost,
                boolean shouldHandlePut,
                boolean shouldHandleDelete) {
            // Arrange
            var annotation = createMockResponseWithStringContent(
                    DEFAULT_PATH, method, DEFAULT_STATUS, "Test content");

            // Act
            var element = new MockResponseDispatcherElement(annotation);
            var getResponse = element.handleGet(request);
            var postResponse = element.handlePost(request);
            var putResponse = element.handlePut(request);
            var deleteResponse = element.handleDelete(request);

            // Assert
            assertEquals(shouldHandleGet, getResponse.isPresent(), "GET handling should match expected value");
            assertEquals(shouldHandlePost, postResponse.isPresent(), "POST handling should match expected value");
            assertEquals(shouldHandlePut, putResponse.isPresent(), "PUT handling should match expected value");
            assertEquals(shouldHandleDelete, deleteResponse.isPresent(), "DELETE handling should match expected value");
        }

        static Stream<Arguments> httpMethodTestCases() {
            RecordedRequest request = createTestRequest();
            return Stream.of(
                    Arguments.of(HttpMethodMapper.GET, request, true, false, false, false),
                    Arguments.of(HttpMethodMapper.POST, request, false, true, false, false),
                    Arguments.of(HttpMethodMapper.PUT, request, false, false, true, false),
                    Arguments.of(HttpMethodMapper.DELETE, request, false, false, false, true)
            );
        }
    }

    @Nested
    @DisplayName("Header handling tests")
    class HeaderTests {

        @Test
        @DisplayName("Should add custom headers to response")
        void shouldAddCustomHeaders() {
            // Arrange
            var annotation = createMockResponseWithHeaders(
                    DEFAULT_PATH, HttpMethodMapper.GET, DEFAULT_STATUS,
                    new String[]{"X-Custom=Value", "Cache-Control=no-cache"});

            // Act
            var element = new MockResponseDispatcherElement(annotation);
            RecordedRequest request = createTestRequest();
            var response = element.handleGet(request).orElseThrow();

            // Assert
            assertEquals("Value", response.getHeaders().get("X-Custom"),
                    "Custom header should be present in response");
            assertEquals("no-cache", response.getHeaders().get("Cache-Control"),
                    "Cache-Control header should be present in response");
        }

        @Test
        @DisplayName("Should ignore malformed headers")
        void shouldIgnoreMalformedHeaders() {
            // Arrange
            var annotation = createMockResponseWithHeaders(
                    DEFAULT_PATH, HttpMethodMapper.GET, DEFAULT_STATUS,
                    new String[]{"InvalidHeader", "X-Valid=Value"});

            // Act
            var element = new MockResponseDispatcherElement(annotation);
            RecordedRequest request = createTestRequest();
            var response = element.handleGet(request).orElseThrow();

            // Assert
            assertNull(response.getHeaders().get("InvalidHeader"),
                    "Malformed header should not be present in response");
            assertEquals("Value", response.getHeaders().get("X-Valid"),
                    "Valid header should be present in response");
        }
    }

    @Nested
    @DisplayName("JSON content parsing tests")
    class JsonContentTests {

        @ParameterizedTest
        @MethodSource("jsonContentTestCases")
        @DisplayName("Should parse different JSON content formats correctly")
        void shouldParseJsonContent(String jsonContent, String expectedResult) {
            // Arrange
            var annotation = createMockResponseWithJsonContent(
                    DEFAULT_PATH, HttpMethodMapper.GET, DEFAULT_STATUS, jsonContent);

            // Act
            var element = new MockResponseDispatcherElement(annotation);
            RecordedRequest request = createTestRequest();
            var response = element.handleGet(request).orElseThrow();

            // Assert
            assertEquals(expectedResult, getBodyContent(response),
                    "JSON content should be parsed correctly");
        }

        static Stream<Arguments> jsonContentTestCases() {
            return Stream.of(
                    Arguments.of(DEFAULT_JSON_CONTENT, "{\"key\":\"value\"}"),
                    Arguments.of("key1=value1,key2=value2", "{\"key1\":\"value1\",\"key2\":\"value2\"}"),
                    Arguments.of("key=123", "{\"key\":123}"),
                    Arguments.of("key=true", "{\"key\":true}"),
                    Arguments.of("key=null", "{\"key\":null}"),
                    // Using a single-element array to avoid comma splitting issues
                    Arguments.of("key=[42]", "{\"key\":[42]}"),
                    Arguments.of("key={\"nested\":\"value\"}", "{\"key\":{\"nested\":\"value\"}}"),
                    Arguments.of("{}", "{}"),
                    Arguments.of("[]", "[]"),
                    Arguments.of("{\"already\":\"valid\"}", "{\"already\":\"valid\"}")
            );
        }
    }

    // Using MockResponseTestUtil instead of local helper method
    
    /**
     * Helper method to convert a MockResponse body to a string for assertion.
     *
     * @param response the MockResponse to extract the body from
     * @return the body as a string, or an error message if extraction fails
     */
    private String getBodyContent(mockwebserver3.MockResponse response) {
        Buffer buffer = new Buffer();
        try {
            if (response.getBody() != null) {
                response.getBody().writeTo(buffer);
                return buffer.readUtf8();
            }
            return "";
        } catch (Exception e) {
            return "Error reading body: " + e.getMessage();
        }
    }

    /**
     * Creates a test RecordedRequest object for testing.
     * 
     * @return a RecordedRequest instance
     */
    private static RecordedRequest createTestRequest() {
        return new RecordedRequest("GET " + DEFAULT_PATH + " HTTP/1.1",
                Headers.of("key=value", "key2=value2"), Collections.emptyList(),
                0, new Buffer(), 0, new Socket(), null);
    }
}
