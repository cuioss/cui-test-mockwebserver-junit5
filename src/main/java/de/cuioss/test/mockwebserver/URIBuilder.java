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

import de.cuioss.tools.net.UrlHelper;
import lombok.NonNull;

import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * A builder for creating URIs for MockWebServer tests.
 * <p>
 * This class simplifies the process of building URIs for test requests by providing
 * a fluent API for adding path segments and query parameters.
 *
 * <p>
 * Example usage:
 * <pre>
 * {@code
 * URI uri = URIBuilder.from(serverURL)
 *     .addPathSegment("api")
 *     .addPathSegment("users")
 *     .addQueryParameter("filter", "active")
 *     .build();
 *
 * // Creates a URI like: http://localhost:12345/api/users?filter=active
 * }
 * </pre>
 */
public class URIBuilder {

    private final URL baseUrl;
    private final List<String> pathSegments = new ArrayList<>();
    private final Map<String, List<String>> queryParameters = new LinkedHashMap<>();

    private URIBuilder(URL baseUrl) {
        this.baseUrl = baseUrl;
    }

    /**
     * Creates a new builder with the given base URL.
     *
     * @param baseUrl the base URL to build upon, typically from the MockWebServer
     * @return a new builder instance
     */
    public static URIBuilder from(@NonNull URL baseUrl) {
        return new URIBuilder(baseUrl);
    }

    /**
     * Adds a path segment to the URI.
     * <p>
     * Path segments are automatically separated by forward slashes.
     * Leading and trailing slashes in the segment are automatically trimmed.
     * </p>
     *
     * @param segment the path segment to add
     * @return this builder for method chaining
     */
    public URIBuilder addPathSegment(@NonNull String segment) {
        String trimmedSegment = UrlHelper.removeTrailingSlashesFromUrl(UrlHelper.removePrecedingSlashFromPath(segment));
        trimmedSegment = trimmedSegment.trim();

        if (!trimmedSegment.isEmpty()) {
            pathSegments.add(trimmedSegment);
        }
        return this;
    }

    /**
     * Adds multiple path segments to the URI.
     * <p>
     * Path segments are automatically separated by forward slashes.
     * Leading and trailing slashes in each segment are automatically trimmed.
     * </p>
     *
     * @param segments the path segments to add
     * @return this builder for method chaining
     */
    public URIBuilder addPathSegments(@NonNull String... segments) {
        for (String segment : segments) {
            addPathSegment(segment);
        }
        return this;
    }

    /**
     * Adds a query parameter to the URI.
     *
     * @param name  the parameter name
     * @param value the parameter value
     * @return this builder for method chaining
     */
    public URIBuilder addQueryParameter(@NonNull String name, @NonNull String value) {
        queryParameters.computeIfAbsent(name, k -> new ArrayList<>()).add(value);
        return this;
    }

    /**
     * Builds the URI from the configured components.
     *
     * @return the constructed URI
     */
    public URI build() {
        StringBuilder uriBuilder = new StringBuilder(baseUrl.toString());

        // Remove trailing slash from base URL if present
        if (uriBuilder.charAt(uriBuilder.length() - 1) == '/') {
            uriBuilder.deleteCharAt(uriBuilder.length() - 1);
        }

        // Add path segments
        if (!pathSegments.isEmpty()) {
            uriBuilder.append('/').append(String.join("/", pathSegments));
        }

        // Add query parameters
        if (!queryParameters.isEmpty()) {
            uriBuilder.append('?');

            String queryString = queryParameters.entrySet().stream()
                    .flatMap(entry -> entry.getValue().stream()
                            .map(value -> entry.getKey() + "=" + value))
                    .collect(Collectors.joining("&"));

            uriBuilder.append(queryString);
        }

        return URI.create(uriBuilder.toString());
    }

    /**
     * Builds the URI and returns it as a string.
     *
     * @return the constructed URI as a string
     */
    public String buildAsString() {
        return build().toString();
    }

    /**
     * Gets the path from the base URL.
     * If path segments have been added, they are not included in this result.
     *
     * @return the path from the base URL
     */
    public String getPath() {
        return baseUrl.getPath();
    }

    /**
     * Gets the scheme (protocol) from the base URL.
     *
     * @return the scheme from the base URL (e.g., "http" or "https")
     */
    public String getScheme() {
        return baseUrl.getProtocol();
    }

    /**
     * Gets the port from the base URL.
     *
     * @return the port from the base URL
     */
    public int getPort() {
        return baseUrl.getPort();
    }

    /**
     * Sets the path for this URI builder, replacing any existing path segments.
     *
     * @param path the path to set
     * @return this builder for method chaining
     */
    public URIBuilder setPath(String path) {
        // Clear existing path segments
        pathSegments.clear();

        // Add the new path as a segment
        return addPathSegment(path);
    }
}
