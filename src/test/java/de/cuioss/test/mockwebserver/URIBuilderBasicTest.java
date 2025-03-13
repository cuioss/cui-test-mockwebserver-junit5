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

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.net.URI;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests for basic URI construction functionality of {@link URIBuilder}.
 */
class URIBuilderBasicTest extends URIBuilderTestBase {

    @Test
    @DisplayName("Should create a basic URI from a URI")
    void shouldCreateBasicUri() {
        // Given: A base URI
        URI baseUri = URI.create(BASE_URL);

        // When: Creating a URIBuilder and building a URI
        URI result = URIBuilder.from(baseUri).build();

        // Then: The result should match the expected URI
        assertEquals(BASE_URL_NO_SLASH, result.toString());
    }

    @Test
    @DisplayName("Should build URI without any path segments or query parameters")
    void shouldBuildUriWithoutPathOrQuery() {
        // Given: A base URI without trailing slash
        URI baseUri = URI.create(BASE_URL_NO_SLASH);

        // When: Building a URI without modifications
        URI result = URIBuilder.from(baseUri).build();

        // Then: The result should match the original URI
        assertEquals(BASE_URL_NO_SLASH, result.toString());
        // Verify no changes were made to the URI
        assertEquals(baseUri.toString(), result.toString());
    }

    @Test
    @DisplayName("Should get path from base URL")
    void shouldGetPathFromBaseUrl() {
        // Given: A base URI with a path
        URI baseUri = URI.create("http://localhost:8080/api/v1/");
        URIBuilder builder = URIBuilder.from(baseUri);

        // When: Getting the path
        String path = builder.getPath();

        // Then: The path should match the expected value
        assertEquals("/api/v1/", path);
    }

    @Test
    @DisplayName("Should get scheme from base URL")
    void shouldGetSchemeFromBaseUrl() {
        // Given: A base URI with HTTPS scheme
        URI baseUri = URI.create("https://localhost:8443/");
        URIBuilder builder = URIBuilder.from(baseUri);

        // When: Getting the scheme
        String scheme = builder.getScheme();

        // Then: The scheme should match the expected value
        assertEquals("https", scheme);
    }

    @Test
    @DisplayName("Should get port from base URL")
    void shouldGetPortFromBaseUrl() {
        // Given: A base URI with a non-default port
        URI baseUri = URI.create("http://localhost:9090/");
        URIBuilder builder = URIBuilder.from(baseUri);

        // When: Getting the port
        int port = builder.getPort();

        // Then: The port should match the expected value
        assertEquals(9090, port);
    }

    @Test
    @DisplayName("Should set path replacing existing path segments")
    void shouldSetPathReplacingExistingSegments() {
        // Given: A URIBuilder with existing path segments
        URI baseUri = URI.create(BASE_URL);
        URIBuilder builder = URIBuilder.from(baseUri)
                .addPathSegment("existing")
                .addPathSegment("segments");

        // When: Setting a new path
        URI result = builder
                .setPath("/new/path")
                .build();

        // Then: The result should have the new path
        assertEquals(BASE_URL_NO_SLASH + "/new/path", result.toString());
    }

    @Test
    @DisplayName("Should set path and then add additional path segments")
    void shouldSetPathAndThenAddSegments() {
        // Given: A base URI
        URI baseUri = URI.create(BASE_URL);

        // When: Setting a path and adding a segment
        URI result = URIBuilder.from(baseUri)
                .setPath("/" + API_PATH)
                .addPathSegment(USERS_PATH)
                .build();

        // Then: The result should have both the set path and added segment
        assertEquals(BASE_URL_NO_SLASH + "/" + API_PATH + "/" + USERS_PATH, result.toString());
    }

    @Test
    @DisplayName("Should set path with query parameters")
    void shouldSetPathWithQueryParameters() {
        // Given: A base URI
        URI baseUri = URI.create(BASE_URL);

        // When: Adding a query parameter and setting a path
        URI result = URIBuilder.from(baseUri)
                .addQueryParameter(PARAM_NAME, VALUE_PARAM)
                .setPath("/" + API_PATH + "/" + USERS_PATH)
                .build();

        // Then: The result should have both the path and query parameter
        assertEquals(BASE_URL_NO_SLASH + "/" + API_PATH + "/" + USERS_PATH + "?" + PARAM_NAME + "=" + VALUE_PARAM, result.toString());
    }
}
