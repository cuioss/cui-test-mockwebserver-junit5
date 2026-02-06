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
package de.cuioss.test.mockwebserver.mockresponse;

import de.cuioss.test.mockwebserver.dispatcher.HttpMethodMapper;
import de.cuioss.test.mockwebserver.dispatcher.ModuleDispatcherElement;
import de.cuioss.tools.string.MoreStrings;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.ToString;
import mockwebserver3.MockResponse;
import mockwebserver3.RecordedRequest;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Implementation of {@link ModuleDispatcherElement} that handles requests based on
 * the configuration from a {@link MockResponseConfig} annotation.
 * <p>
 * This class is responsible for:
 * <ul>
 *   <li>Matching requests to the configured path and HTTP method</li>
 *   <li>Generating appropriate responses with the configured status code, headers, and content</li>
 *   <li>Handling different content types (text, JSON, raw string)</li>
 * </ul>
 *
 * @author Oliver Wolff
 * @since 1.1
 */
@ToString
@EqualsAndHashCode
public class MockResponseConfigDispatcherElement implements ModuleDispatcherElement {

    private static final String CONTENT_TYPE_HEADER = "Content-Type";
    private static final String TEXT_PLAIN_CONTENT_TYPE = "text/plain";
    private static final String JSON_CONTENT_TYPE = "application/json";

    @Getter
    private final String baseUrl;

    private final HttpMethodMapper method;
    private final String responseBody;
    private final int statusCode;
    private final Map<String, String> headers;

    /**
     * Creates a new instance based on the given {@link MockResponseConfig} annotation.
     *
     * @param annotation the annotation to create the dispatcher from, must not be null
     * @throws IllegalArgumentException if the annotation is invalid (e.g., multiple content types specified)
     */
    public MockResponseConfigDispatcherElement(@NonNull MockResponseConfig annotation) {
        this.baseUrl = annotation.path();
        this.method = annotation.method();
        this.statusCode = annotation.status();
        this.headers = parseHeaders(annotation);
        this.responseBody = determineResponseBody(annotation);
    }

    @Override
    public Optional<MockResponse> handleGet(@NonNull RecordedRequest request) {
        if (method == HttpMethodMapper.GET) {
            return createResponse();
        }
        return Optional.empty();
    }

    @Override
    public Optional<MockResponse> handlePost(@NonNull RecordedRequest request) {
        if (method == HttpMethodMapper.POST) {
            return createResponse();
        }
        return Optional.empty();
    }

    @Override
    public Optional<MockResponse> handlePut(@NonNull RecordedRequest request) {
        if (method == HttpMethodMapper.PUT) {
            return createResponse();
        }
        return Optional.empty();
    }

    @Override
    public Optional<MockResponse> handleDelete(@NonNull RecordedRequest request) {
        if (method == HttpMethodMapper.DELETE) {
            return createResponse();
        }
        return Optional.empty();
    }

    /**
     * {@inheritDoc}
     * <p>
     * This implementation returns a set containing only the HTTP method specified in the
     * {@link MockResponseConfig} annotation used to create this dispatcher element.
     */
    @Override
    public @NonNull Set<HttpMethodMapper> supportedMethods() {
        Set<HttpMethodMapper> methods = new HashSet<>();
        methods.add(method);
        return methods;
    }

    /**
     * Creates a response based on the configured status code, headers, and content.
     *
     * @return an Optional containing the response
     */
    private Optional<MockResponse> createResponse() {
        var responseBuilder = new MockResponse.Builder()
                .code(statusCode);

        // Add all headers
        headers.forEach(responseBuilder::addHeader);

        // Add body if present
        if (!MoreStrings.isEmpty(responseBody)) {
            responseBuilder.body(responseBody);
        }

        return Optional.of(responseBuilder.build());
    }

    /**
     * Parses headers from the annotation.
     *
     * @param annotation the annotation to parse headers from
     * @return a map of header names to values
     */
    private Map<String, String> parseHeaders(MockResponseConfig annotation) {
        Map<String, String> headerMap = new HashMap<>();

        // Add explicit headers
        for (String header : annotation.headers()) {
            if (!MoreStrings.isEmpty(header) && header.contains("=")) {
                String[] parts = header.split("=", 2);
                headerMap.put(parts[0].trim(), parts[1].trim());
            }
        }

        // Add content type if specified
        if (!MoreStrings.isEmpty(annotation.contentType())) {
            headerMap.put(CONTENT_TYPE_HEADER, annotation.contentType());
        }

        return headerMap;
    }

    /**
     * Determines the response body based on the content type specified in the annotation.
     *
     * @param annotation the annotation to determine the response body from
     * @return the response body
     * @throws IllegalArgumentException if multiple content types are specified
     */
    private String determineResponseBody(MockResponseConfig annotation) {
        // Count how many content types are specified
        long contentTypeCount = Stream.of(
                annotation.textContent(),
                annotation.jsonContentKeyValue(),
                annotation.stringContent())
                .filter(content -> !MoreStrings.isEmpty(content))
                .count();

        if (contentTypeCount > 1) {
            throw new IllegalArgumentException(
                    "Only one of textContent, jsonContentKeyValue, or stringContent can be specified");
        }

        // Handle text content
        if (!MoreStrings.isEmpty(annotation.textContent())) {
            headers.putIfAbsent(CONTENT_TYPE_HEADER, TEXT_PLAIN_CONTENT_TYPE);
            return annotation.textContent();
        }

        // Handle JSON content
        if (!MoreStrings.isEmpty(annotation.jsonContentKeyValue())) {
            headers.putIfAbsent(CONTENT_TYPE_HEADER, JSON_CONTENT_TYPE);
            return parseJsonContent(annotation.jsonContentKeyValue());
        }

        // Handle raw string content
        if (!MoreStrings.isEmpty(annotation.stringContent())) {
            return annotation.stringContent();
        }

        // No content specified
        return "";
    }

    /**
     * Parses JSON content from the key-value format.
     *
     * @param jsonContent the JSON content in key-value format
     * @return the JSON content as a properly formatted JSON string
     */
    private String parseJsonContent(String jsonContent) {
        if (MoreStrings.isEmpty(jsonContent)) {
            return "{}";
        }

        if (isSpecialCase(jsonContent)) {
            return jsonContent;
        }

        if (isAlreadyValidJson(jsonContent)) {
            return jsonContent;
        }

        return convertKeyValuePairsToJson(jsonContent);
    }

    /**
     * Checks if the content is a special case that should be returned as-is.
     *
     * @param content the content to check
     * @return true if it's a special case
     */
    private boolean isSpecialCase(String content) {
        return "{}".equals(content) || "[]".equals(content);
    }

    /**
     * Checks if the content is already valid JSON.
     *
     * @param content the content to check
     * @return true if it's already valid JSON
     */
    private boolean isAlreadyValidJson(String content) {
        return (content.startsWith("{") && content.endsWith("}")) ||
                (content.startsWith("[") && content.endsWith("]"));
    }

    /**
     * Converts key-value pairs to a JSON object.
     *
     * @param keyValueContent the content in key-value format
     * @return a JSON string
     */
    private String convertKeyValuePairsToJson(String keyValueContent) {
        String[] pairs = keyValueContent.split(",");
        StringBuilder jsonBuilder = new StringBuilder("{");

        for (int i = 0; i < pairs.length; i++) {
            String pair = pairs[i];
            if (pair.contains("=")) {
                appendKeyValuePair(jsonBuilder, pair);

                // Add comma if not the last pair
                if (i < pairs.length - 1) {
                    jsonBuilder.append(",");
                }
            }
        }

        jsonBuilder.append("}");
        return jsonBuilder.toString();
    }

    /**
     * Appends a key-value pair to the JSON builder.
     *
     * @param jsonBuilder the JSON builder
     * @param pair        the key-value pair string
     */
    private void appendKeyValuePair(StringBuilder jsonBuilder, String pair) {
        String[] keyValue = pair.split("=", 2);
        String key = keyValue[0].trim();
        String value = keyValue[1].trim();

        // Add quotes to key
        jsonBuilder.append("\"").append(key).append("\":");

        // Add value with appropriate formatting
        appendFormattedValue(jsonBuilder, value);
    }

    /**
     * Appends a properly formatted value to the JSON builder.
     *
     * @param jsonBuilder the JSON builder
     * @param value       the value to append
     */
    private void appendFormattedValue(StringBuilder jsonBuilder, String value) {
        if (shouldBeUnquoted(value)) {
            jsonBuilder.append(value);
        } else {
            jsonBuilder.append("\"").append(value).append("\"");
        }
    }

    /**
     * Determines if a value should be unquoted in JSON.
     *
     * @param value the value to check
     * @return true if the value should be unquoted
     */
    private boolean shouldBeUnquoted(String value) {
        return "true".equals(value) ||
                "false".equals(value) ||
                "null".equals(value) ||
                value.matches("-?\\d+(\\.\\d+)?") ||
                (value.startsWith("[") && value.endsWith("]")) ||
                (value.startsWith("{") && value.endsWith("}"));
    }
}
