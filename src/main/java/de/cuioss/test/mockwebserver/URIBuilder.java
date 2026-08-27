/*
 * Copyright © 2025-present CUI-OpenSource-Software (info@cuioss.de)
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
package de.cuioss.test.mockwebserver;

import de.cuioss.tools.net.UrlHelper;
import lombok.NonNull;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

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
 *
 * @author Oliver Wolff
 * @since 1.0
 * @see java.net.URI
 * @see mockwebserver3.MockWebServer
 */
public class URIBuilder {

    /**
     * Message used when {@link #build()} or {@link #buildAsString()} is called on a placeholder builder.
     */
    private static final String PLACEHOLDER_BUILD_MESSAGE =
            "Cannot build URI from placeholder URIBuilder. The server must be started first, and a proper " +
                    "URIBuilder must be created using URIBuilder.from(server.url(\"/\").url()).";

    /**
     * Characters allowed unencoded in a path, in addition to the RFC 3986 unreserved set. The forward
     * slash is preserved so that segments containing separators (and {@link #setPath(String)} inputs)
     * keep their structure.
     */
    private static final String PATH_ALLOWED = ":@!$&'()*+,;=/";

    /**
     * Characters allowed unencoded in a query name/value, in addition to the RFC 3986 unreserved set.
     * The structural delimiters {@code & = +} are deliberately excluded so they are percent-encoded.
     */
    private static final String QUERY_ALLOWED = ":@!$'()*,;/?";

    private URL baseUrl;
    private final Supplier<URL> baseUrlSupplier;
    private final List<String> pathSegments = new ArrayList<>();
    private final Map<String, List<String>> queryParameters = new LinkedHashMap<>();
    private final boolean placeholder;

    private URIBuilder(URL baseUrl) {
        this.baseUrl = baseUrl;
        this.baseUrlSupplier = null;
        this.placeholder = false;
    }

    private URIBuilder(Supplier<URL> baseUrlSupplier) {
        this.baseUrl = null;
        this.baseUrlSupplier = baseUrlSupplier;
        this.placeholder = false;
    }

    /**
     * Creates a placeholder URIBuilder that can be used when the server is not yet started.
     * This is useful for manual server start configurations.
     *
     * @implNote When using a placeholder URIBuilder, you must start the server before calling
     * {@link #build()} or any other method that requires the base URL.
     */
    private URIBuilder() {
        this.baseUrl = null;
        this.baseUrlSupplier = null;
        this.placeholder = true;
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
     * Creates a new builder whose base URL is resolved lazily, on first use.
     * <p>
     * This supports servers that are started after parameter injection (e.g.
     * {@code @EnableMockWebServer(manualStart = true)}): the base URL is obtained from the supplier
     * the first time {@link #build()} (or another URL-dependent method) is called.
     *
     * @param baseUrlSupplier supplies the base URL on demand, must not be null
     * @return a new builder instance
     */
    public static URIBuilder from(@NonNull Supplier<URL> baseUrlSupplier) {
        return new URIBuilder(baseUrlSupplier);
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
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException("Could not convert URI to URL: " + baseUri, e);
        }
    }

    /**
     * Resolves the base URL, invoking the supplier on first use for lazily-bound builders.
     *
     * @return the resolved base URL, or {@code null} for a placeholder builder
     */
    private URL resolveBaseUrl() {
        if (baseUrl == null && baseUrlSupplier != null) {
            baseUrl = baseUrlSupplier.get();
        }
        return baseUrl;
    }

    /**
     * Creates a placeholder URIBuilder that can be used when the server is not yet started.
     * This is useful for manual server start configurations.
     * 
     * @return a new placeholder builder instance
     * @implNote When using a placeholder URIBuilder, you must start the server before calling
     * {@link #build()} or any other method that requires the base URL.
     */
    public static URIBuilder placeholder() {
        return new URIBuilder();
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
     * @throws IllegalStateException if this is a placeholder URIBuilder
     */
    private void validateBuilderState() {
        if (placeholder) {
            throw new IllegalStateException(PLACEHOLDER_BUILD_MESSAGE);
        }
    }

    /**
     * Builds the URI with all configured path segments and query parameters.
     * <p>
     * Path segments and query parameter names/values are percent-encoded per RFC 3986, so values
     * containing spaces or reserved characters (e.g. {@code &}, {@code =}, {@code #}) are transmitted
     * literally rather than throwing or silently altering the URI structure.
     *
     * @return the constructed URI
     * @throws IllegalStateException if this is a placeholder URIBuilder
     */
    public URI build() {
        validateBuilderState();

        String baseUrlString = resolveBaseUrl().toString();
        StringBuilder uriBuilder = new StringBuilder();

        // Normalize base URL by removing trailing slash
        if (baseUrlString.endsWith("/")) {
            uriBuilder.append(baseUrlString, 0, baseUrlString.length() - 1);
        } else {
            uriBuilder.append(baseUrlString);
        }

        // Add path segments (percent-encoded, preserving embedded separators)
        if (!pathSegments.isEmpty()) {
            uriBuilder.append('/');
            for (int i = 0; i < pathSegments.size(); i++) {
                if (i > 0) {
                    uriBuilder.append('/');
                }
                uriBuilder.append(encodePathSegment(pathSegments.get(i)));
            }
        }

        // Add query parameters (percent-encoded names and values)
        if (!queryParameters.isEmpty()) {
            uriBuilder.append('?');
            boolean first = true;
            for (Map.Entry<String, List<String>> entry : queryParameters.entrySet()) {
                String encodedName = encodeQueryComponent(entry.getKey());
                for (String value : entry.getValue()) {
                    if (!first) {
                        uriBuilder.append('&');
                    }
                    uriBuilder.append(encodedName).append('=').append(encodeQueryComponent(value));
                    first = false;
                }
            }
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
        validateBuilderState();
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
        return resolveBaseUrl().getPath();
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
        return resolveBaseUrl().getProtocol();
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
        return resolveBaseUrl().getPort();
    }

    /**
     * Percent-encodes a single path segment per RFC 3986, leaving the forward slash intact so segments
     * that contain separators keep their structure.
     *
     * @param segment the raw path segment
     * @return the percent-encoded segment
     */
    private static String encodePathSegment(String segment) {
        return percentEncode(segment, PATH_ALLOWED);
    }

    /**
     * Percent-encodes a query parameter name or value per RFC 3986, encoding the structural delimiters
     * {@code & = +} and space so the query structure is preserved.
     *
     * @param component the raw query name or value
     * @return the percent-encoded component
     */
    private static String encodeQueryComponent(String component) {
        return percentEncode(component, QUERY_ALLOWED);
    }

    /**
     * Percent-encodes the input for use in a URI, leaving the RFC 3986 unreserved characters and the
     * supplied {@code allowedExtra} characters untouched.
     *
     * @param input        the raw text to encode
     * @param allowedExtra characters permitted unencoded in addition to the unreserved set
     * @return the percent-encoded text
     */
    private static String percentEncode(String input, String allowedExtra) {
        StringBuilder encoded = new StringBuilder(input.length());
        for (byte rawByte : input.getBytes(StandardCharsets.UTF_8)) {
            int value = rawByte & 0xFF;
            char c = (char) value;
            boolean unreserved = (c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z')
                    || (c >= '0' && c <= '9') || c == '-' || c == '.' || c == '_' || c == '~';
            if (unreserved || allowedExtra.indexOf(c) >= 0) {
                encoded.append(c);
            } else {
                encoded.append('%')
                        .append(Character.toUpperCase(Character.forDigit(value >> 4, 16)))
                        .append(Character.toUpperCase(Character.forDigit(value & 0xF, 16)));
            }
        }
        return encoded.toString();
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
     * @param path the path to set, split into individual segments on {@code /}
     * @return this builder for method chaining
     */
    public URIBuilder setPath(@NonNull String path) {
        // Clear existing path segments and split the incoming path into individual segments so
        // getPathSegments() stays consistent with addPathSegment semantics.
        pathSegments.clear();
        return addPathSegments(path.split("/"));
    }
}
