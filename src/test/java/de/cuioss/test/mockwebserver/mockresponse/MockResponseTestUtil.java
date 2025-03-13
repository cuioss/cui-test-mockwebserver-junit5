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
import lombok.experimental.UtilityClass;

import static org.easymock.EasyMock.*;

/**
 * Utility class for creating mock instances of {@link MockResponse} annotation
 * for testing purposes.
 * 
 * @author Oliver Wolff
 */
@UtilityClass
public class MockResponseTestUtil {

    /** Default test path */
    public static final String DEFAULT_PATH = "/api/test";

    /** Default status code */
    public static final int DEFAULT_STATUS = 200;

    /** Default text content */
    public static final String DEFAULT_TEXT_CONTENT = "Hello World";

    /** Default JSON content */
    public static final String DEFAULT_JSON_CONTENT = "key=value";

    /** Default string content */
    public static final String DEFAULT_STRING_CONTENT = "Raw content";

    /** Content-Type header name */
    public static final String CONTENT_TYPE_HEADER = "Content-Type";

    /**
     * Builder class for creating mock {@link MockResponse} annotations.
     * This helps reduce the number of parameters in method calls.
     */
    public static class MockResponseBuilder {
        private String path = DEFAULT_PATH;
        private HttpMethodMapper method = HttpMethodMapper.GET;
        private int status = DEFAULT_STATUS;
        private String textContent = "";
        private String jsonContent = "";
        private String stringContent = "";
        private String[] headers = new String[0];
        private String contentType = "";

        /**
         * Sets the path for the mock response.
         * 
         * @param path the path
         * @return this builder
         */
        public MockResponseBuilder withPath(String path) {
            this.path = path;
            return this;
        }

        /**
         * Sets the HTTP method for the mock response.
         * 
         * @param method the HTTP method
         * @return this builder
         */
        public MockResponseBuilder withMethod(HttpMethodMapper method) {
            this.method = method;
            return this;
        }

        /**
         * Sets the status code for the mock response.
         * 
         * @param status the status code
         * @return this builder
         */
        public MockResponseBuilder withStatus(int status) {
            this.status = status;
            return this;
        }

        /**
         * Sets the text content for the mock response.
         * 
         * @param textContent the text content
         * @return this builder
         */
        public MockResponseBuilder withTextContent(String textContent) {
            this.textContent = textContent;
            return this;
        }

        /**
         * Sets the JSON content for the mock response.
         * 
         * @param jsonContent the JSON content
         * @return this builder
         */
        public MockResponseBuilder withJsonContent(String jsonContent) {
            this.jsonContent = jsonContent;
            return this;
        }

        /**
         * Sets the string content for the mock response.
         * 
         * @param stringContent the string content
         * @return this builder
         */
        public MockResponseBuilder withStringContent(String stringContent) {
            this.stringContent = stringContent;
            return this;
        }

        /**
         * Sets the headers for the mock response.
         * 
         * @param headers the headers
         * @return this builder
         */
        public MockResponseBuilder withHeaders(String[] headers) {
            this.headers = headers;
            return this;
        }

        /**
         * Sets the content type for the mock response.
         * 
         * @param contentType the content type
         * @return this builder
         */
        public MockResponseBuilder withContentType(String contentType) {
            this.contentType = contentType;
            return this;
        }

        /**
         * Builds the mock response annotation.
         * 
         * @return the mock response annotation
         */
        public MockResponse build() {
            MockResponse annotation = createMock(MockResponse.class);
            expect(annotation.path()).andReturn(path).anyTimes();
            expect(annotation.method()).andReturn(method).anyTimes();
            expect(annotation.status()).andReturn(status).anyTimes();
            expect(annotation.textContent()).andReturn(textContent).anyTimes();
            expect(annotation.jsonContentKeyValue()).andReturn(jsonContent).anyTimes();
            expect(annotation.stringContent()).andReturn(stringContent).anyTimes();
            expect(annotation.headers()).andReturn(headers).anyTimes();
            expect(annotation.contentType()).andReturn(contentType).anyTimes();
            replay(annotation);

            return annotation;
        }
    }

    /**
     * Creates a new builder for mock {@link MockResponse} annotations.
     * 
     * @return a new builder
     */
    public static MockResponseBuilder builder() {
        return new MockResponseBuilder();
    }

    /**
     * Creates a mock {@link MockResponse} annotation with default empty values.
     * 
     * @param path the path this response will handle
     * @param method the HTTP method this response will handle
     * @param status the HTTP status code for the response
     * @return a mocked {@link MockResponse} annotation
     */
    public static MockResponse createMockResponse(String path, HttpMethodMapper method, int status) {
        return builder()
                .withPath(path)
                .withMethod(method)
                .withStatus(status)
                .build();
    }

    /**
     * Creates a mock {@link MockResponse} annotation with text content.
     * 
     * @param path the path this response will handle
     * @param method the HTTP method this response will handle
     * @param status the HTTP status code for the response
     * @param textContent the text content
     * @return a mocked {@link MockResponse} annotation
     */
    public static MockResponse createMockResponseWithTextContent(
            String path, HttpMethodMapper method, int status, String textContent) {
        return builder()
                .withPath(path)
                .withMethod(method)
                .withStatus(status)
                .withTextContent(textContent)
                .build();
    }

    /**
     * Creates a mock {@link MockResponse} annotation with JSON content.
     * 
     * @param path the path this response will handle
     * @param method the HTTP method this response will handle
     * @param status the HTTP status code for the response
     * @param jsonContent the JSON content
     * @return a mocked {@link MockResponse} annotation
     */
    public static MockResponse createMockResponseWithJsonContent(
            String path, HttpMethodMapper method, int status, String jsonContent) {
        return builder()
                .withPath(path)
                .withMethod(method)
                .withStatus(status)
                .withJsonContent(jsonContent)
                .build();
    }

    /**
     * Creates a mock {@link MockResponse} annotation with string content.
     * 
     * @param path the path this response will handle
     * @param method the HTTP method this response will handle
     * @param status the HTTP status code for the response
     * @param stringContent the string content
     * @return a mocked {@link MockResponse} annotation
     */
    public static MockResponse createMockResponseWithStringContent(
            String path, HttpMethodMapper method, int status, String stringContent) {
        return builder()
                .withPath(path)
                .withMethod(method)
                .withStatus(status)
                .withStringContent(stringContent)
                .build();
    }

    /**
     * Creates a mock {@link MockResponse} annotation with custom headers.
     * 
     * @param path the path this response will handle
     * @param method the HTTP method this response will handle
     * @param status the HTTP status code for the response
     * @param headers the HTTP headers
     * @return a mocked {@link MockResponse} annotation
     */
    public static MockResponse createMockResponseWithHeaders(
            String path, HttpMethodMapper method, int status, String[] headers) {
        return builder()
                .withPath(path)
                .withMethod(method)
                .withStatus(status)
                .withHeaders(headers)
                .build();
    }

    /**
     * Creates a mock {@link MockResponse} annotation with a custom content type.
     * 
     * @param path the path this response will handle
     * @param method the HTTP method this response will handle
     * @param status the HTTP status code for the response
     * @param textContent the text content
     * @param contentType the content type
     * @return a mocked {@link MockResponse} annotation
     */
    public static MockResponse createMockResponseWithContentType(
            String path, HttpMethodMapper method, int status, String textContent, String contentType) {
        return builder()
                .withPath(path)
                .withMethod(method)
                .withStatus(status)
                .withTextContent(textContent)
                .withContentType(contentType)
                .build();
    }
}
