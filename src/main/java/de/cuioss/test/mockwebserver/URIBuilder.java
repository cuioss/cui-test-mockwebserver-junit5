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
 * // tag::uribuilder-basic-usage[]
 * URI uri = URIBuilder.from(serverURL)
 *     .addPathSegment("api")
 *     .addPathSegment("users")
 *     .addQueryParameter("filter", "active")
 *     .build();
 *
 * // Creates a URI like: http://localhost:12345/api/users?filter=active
 * // end::uribuilder-basic-usage[]
 * }
 * </pre>
 */
public class URIBuilder {

    private final URL baseUrl;
    private final List<String> pathSegments = new ArrayList<>();
    private final Map<String, List<String>> queryParameters = new LinkedHashMap<>();
    private final boolean placeholder;

    private URIBuilder(URL baseUrl) {
        this.baseUrl = baseUrl;
        this.placeholder = false;
    }

    /**
     * Creates a placeholder URIBuilder that can be used when the server is not yet started.
     * This is useful for manual server start configurations.
     * 
     * @implNote When using a placeholder URIBuilder, you must start the server before calling
     * {@link #build()} or any other method that requires the base URL.
     */
    // tag::uribuilder-placeholder-constructor[]
    private URIBuilder() {
        this.baseUrl = null;
        this.placeholder = true;
    }

    // end::uribuilder-placeholder-constructor[]

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
     * Creates a new builder with the given base URI.
     * This method converts the URI to a URL internally.
     *
     * @param baseUri the base URI to build upon
     * @return a new builder instance
     * @throws IllegalArgumentException if the URI cannot be converted to a URL
     */
    public static URIBuilder from(@NonNull URI baseUri) {
        try {
            return new URIBuilder(baseUri.toURL());
        } catch (Exception e) {
            throw new IllegalArgumentException("Could not convert URI to URL: " + baseUri, e);
        }
    }

    /**
     * Creates a placeholder URIBuilder that can be used when the server is not yet started.
     * This is useful for manual server start configurations.
     * 
     * @return a new placeholder builder instance
     * @implNote When using a placeholder URIBuilder, you must start the server before calling
     * {@link #build()} or any other method that requires the base URL.
     */
    // tag::uribuilder-placeholder[]
    public static URIBuilder placeholder() {
        return new URIBuilder();
    }

    // end::uribuilder-placeholder[]

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
     * Returns an unmodifiable view of the path segments.
     * 
     * @return the list of path segments
     */
    public List<String> getPathSegments() {
        return List.copyOf(pathSegments);
    }

    /**
     * Returns an unmodifiable view of the query parameters.
     * 
     * @return the map of query parameters
     */
    public Map<String, List<String>> getQueryParameters() {
        return Map.copyOf(queryParameters);
    }

    /**
     * Validates that the URIBuilder is in a valid state for building URIs.
     * 
     * @param forBuildAsString whether the validation is for buildAsString() method
     * @throws IllegalStateException if this is a placeholder URIBuilder or if baseUrl is null
     */
    private void validateBuilderState(boolean forBuildAsString) {
        if (placeholder) {
            if (forBuildAsString) {
                throw new IllegalStateException("Cannot build URI from placeholder URIBuilder. " +
                        "The server must be started first, and a proper URIBuilder must be created using URIBuilder.from(server.url('/').url())");
            } else {
                throw new IllegalStateException("Cannot build URI from placeholder URIBuilder. " +
                        "The server must be started first, and a proper URIBuilder must be created using URIBuilder.from(server.url('/')).url())");
            }
        }

        if (baseUrl == null) {
            throw new IllegalStateException("Cannot build URI with null baseUrl. This might indicate an incorrectly initialized URIBuilder.");
        }
    }

    public URI build() {
        validateBuilderState(false);

        String baseUrlString = baseUrl.toString();
        StringBuilder uriBuilder = new StringBuilder();

        // Normalize base URL by removing trailing slash
        if (baseUrlString.endsWith("/")) {
            uriBuilder.append(baseUrlString, 0, baseUrlString.length() - 1);
        } else {
            uriBuilder.append(baseUrlString);
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
     * @throws IllegalStateException if this is a placeholder URIBuilder
     */
    public String buildAsString() {
        validateBuilderState(true);
        return build().toString();
    }

    /**
     * Gets the path from the base URL.
     * If path segments have been added, they are not included in this result.
     *
     * @return the path from the base URL
     */
    public String getPath() {
        if (placeholder) {
            return "/";
        }
        if (baseUrl == null) {
            throw new IllegalStateException("Cannot access path with null baseUrl. This might indicate an incorrectly initialized URIBuilder.");
        }
        return baseUrl.getPath();
    }

    /**
     * Gets the scheme (protocol) from the base URL.
     *
     * @return the scheme from the base URL (e.g., "http" or "https")
     */
    public String getScheme() {
        if (placeholder) {
            return "http";
        }
        if (baseUrl == null) {
            throw new IllegalStateException("Cannot access scheme with null baseUrl. This might indicate an incorrectly initialized URIBuilder.");
        }
        return baseUrl.getProtocol();
    }

    /**
     * Gets the port from the base URL.
     *
     * @return the port from the base URL
     */
    public int getPort() {
        if (placeholder) {
            return -1; // -1 indicates no port is explicitly set
        }
        if (baseUrl == null) {
            throw new IllegalStateException("Cannot access port with null baseUrl. This might indicate an incorrectly initialized URIBuilder.");
        }
        return baseUrl.getPort();
    }

    /**
     * Sets the path for this URI builder, replacing any existing path segments.
     * <p>
     * This method provides several benefits compared to using {@link #addPathSegment(String)}:
     * <ul>
     *   <li>Complete path replacement: Clears all existing path segments and sets a new path</li>
     *   <li>Convenience for complete paths: When you already have a complete path string (like "/api/users")</li>
     *   <li>Working with existing path strings: Allows direct use of paths from other sources</li>
     *   <li>Compatibility with APIs that return full paths: Use complete paths as-is</li>
     * </ul>
     * <p>
     * Example usage:
     * <pre>
     * {@code
     * // Using a complete path directly
     * String apiPath = "/api/v2/users";
     * URIBuilder.from(baseUrl).setPath(apiPath).build();
     * 
     * // Replacing an existing path
     * builder.setPath("/new/path").build();
     * }
     * </pre>
     *
     * In contrast, {@link #addPathSegment(String)} is preferred when:
     * <ul>
     *   <li>Building paths from logical components with clear segment separation</li>
     *   <li>Adding path parts conditionally or incrementally</li>
     *   <li>Avoiding manual path string manipulation and slash handling</li>
     * </ul>
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
