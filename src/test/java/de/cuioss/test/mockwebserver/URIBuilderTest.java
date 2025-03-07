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
import java.net.URL;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Unit tests for {@link URIBuilder}.
 */
class URIBuilderTest {

    @Test
    @DisplayName("Should create a basic URI from a URL")
    void shouldCreateBasicUri() throws Exception {
        // Given
        URL baseUrl = new URL("http://localhost:8080/");

        // When
        URI result = URIBuilder.from(baseUrl).build();

        // Then
        assertEquals("http://localhost:8080", result.toString());
    }

    @Test
    @DisplayName("Should add a single path segment")
    void shouldAddSinglePathSegment() throws Exception {
        // Given
        URL baseUrl = new URL("http://localhost:8080/");

        // When
        URI result = URIBuilder.from(baseUrl)
                .addPathSegment("api")
                .build();

        // Then
        assertEquals("http://localhost:8080/api", result.toString());
    }

    @Test
    @DisplayName("Should add multiple path segments")
    void shouldAddMultiplePathSegments() throws Exception {
        // Given
        URL baseUrl = new URL("http://localhost:8080/");

        // When
        URI result = URIBuilder.from(baseUrl)
                .addPathSegment("api")
                .addPathSegment("users")
                .addPathSegment("123")
                .build();

        // Then
        assertEquals("http://localhost:8080/api/users/123", result.toString());
    }

    @Test
    @DisplayName("Should add multiple path segments with varargs method")
    void shouldAddMultiplePathSegmentsWithVarargs() throws Exception {
        // Given
        URL baseUrl = new URL("http://localhost:8080/");

        // When
        URI result = URIBuilder.from(baseUrl)
                .addPathSegments("api", "users", "123")
                .build();

        // Then
        assertEquals("http://localhost:8080/api/users/123", result.toString());
    }

    @Test
    @DisplayName("Should trim leading and trailing slashes from path segments")
    void shouldTrimSlashesFromPathSegments() throws Exception {
        // Given
        URL baseUrl = new URL("http://localhost:8080/");

        // When
        URI result = URIBuilder.from(baseUrl)
                .addPathSegment("/api/")
                .addPathSegment("//users//")
                .addPathSegment("/123/")
                .build();

        // Then
        assertEquals("http://localhost:8080/api/users/123", result.toString());
    }

    @Test
    @DisplayName("Should add a single query parameter")
    void shouldAddSingleQueryParameter() throws Exception {
        // Given
        URL baseUrl = new URL("http://localhost:8080/");

        // When
        URI result = URIBuilder.from(baseUrl)
                .addQueryParameter("name", "value")
                .build();

        // Then
        assertEquals("http://localhost:8080?name=value", result.toString());
    }

    @Test
    @DisplayName("Should add multiple query parameters")
    void shouldAddMultipleQueryParameters() throws Exception {
        // Given
        URL baseUrl = new URL("http://localhost:8080/");

        // When
        URI result = URIBuilder.from(baseUrl)
                .addQueryParameter("name1", "value1")
                .addQueryParameter("name2", "value2")
                .addQueryParameter("name3", "value3")
                .build();

        // Then
        assertEquals("http://localhost:8080?name1=value1&name2=value2&name3=value3", result.toString());
    }

    @Test
    @DisplayName("Should add multiple values for the same query parameter")
    void shouldAddMultipleValuesForSameQueryParameter() throws Exception {
        // Given
        URL baseUrl = new URL("http://localhost:8080/");

        // When
        URI result = URIBuilder.from(baseUrl)
                .addQueryParameter("name", "value1")
                .addQueryParameter("name", "value2")
                .build();

        // Then
        assertEquals("http://localhost:8080?name=value1&name=value2", result.toString());
    }

    @Test
    @DisplayName("Should combine path segments and query parameters")
    void shouldCombinePathSegmentsAndQueryParameters() throws Exception {
        // Given
        URL baseUrl = new URL("http://localhost:8080/");

        // When
        URI result = URIBuilder.from(baseUrl)
                .addPathSegment("api")
                .addPathSegment("users")
                .addQueryParameter("filter", "active")
                .addQueryParameter("page", "1")
                .build();

        // Then
        assertEquals("http://localhost:8080/api/users?filter=active&page=1", result.toString());
    }

    @Test
    @DisplayName("Should handle base URL with path")
    void shouldHandleBaseUrlWithPath() throws Exception {
        // Given
        URL baseUrl = new URL("http://localhost:8080/base/");

        // When
        URI result = URIBuilder.from(baseUrl)
                .addPathSegment("api")
                .build();

        // Then
        assertEquals("http://localhost:8080/base/api", result.toString());
    }

    @Test
    @DisplayName("Should handle empty path segments")
    void shouldHandleEmptyPathSegments() throws Exception {
        // Given
        URL baseUrl = new URL("http://localhost:8080/");

        // When
        URI result = URIBuilder.from(baseUrl)
                .addPathSegment("")
                .addPathSegment("/")
                .addPathSegment("api")
                .build();

        // Then
        assertEquals("http://localhost:8080/api", result.toString());
    }

    @Test
    @DisplayName("Should return URI as string")
    void shouldReturnUriAsString() throws Exception {
        // Given
        URL baseUrl = new URL("http://localhost:8080/");

        // When
        String result = URIBuilder.from(baseUrl)
                .addPathSegment("api")
                .addQueryParameter("name", "value")
                .buildAsString();

        // Then
        assertEquals("http://localhost:8080/api?name=value", result);
    }

    @Test
    @DisplayName("Should throw NullPointerException when base URL is null")
    void shouldThrowExceptionWhenBaseUrlIsNull() {
        assertThrows(NullPointerException.class, () -> URIBuilder.from(null));
    }

    @Test
    @DisplayName("Should throw NullPointerException when path segment is null")
    void shouldThrowExceptionWhenPathSegmentIsNull() throws Exception {
        // Given
        URL baseUrl = new URL("http://localhost:8080/");
        URIBuilder builder = URIBuilder.from(baseUrl);

        // Then
        assertThrows(NullPointerException.class, () -> builder.addPathSegment(null));
    }

    @Test
    @DisplayName("Should throw NullPointerException when path segments array is null")
    void shouldThrowExceptionWhenPathSegmentsArrayIsNull() throws Exception {
        // Given
        URL baseUrl = new URL("http://localhost:8080/");
        URIBuilder builder = URIBuilder.from(baseUrl);

        // Then
        assertThrows(NullPointerException.class, () -> builder.addPathSegments((String[]) null));
    }

    @Test
    @DisplayName("Should throw NullPointerException when query parameter name is null")
    void shouldThrowExceptionWhenQueryParameterNameIsNull() throws Exception {
        // Given
        URL baseUrl = new URL("http://localhost:8080/");
        URIBuilder builder = URIBuilder.from(baseUrl);

        // Then
        assertThrows(NullPointerException.class, () -> builder.addQueryParameter(null, "value"));
    }

    @Test
    @DisplayName("Should throw NullPointerException when query parameter value is null")
    void shouldThrowExceptionWhenQueryParameterValueIsNull() throws Exception {
        // Given
        URL baseUrl = new URL("http://localhost:8080/");
        URIBuilder builder = URIBuilder.from(baseUrl);

        // Then
        assertThrows(NullPointerException.class, () -> builder.addQueryParameter("name", null));
    }

    @Test
    @DisplayName("Should get path from base URL")
    void shouldGetPathFromBaseUrl() throws Exception {
        // Given
        URL baseUrl = new URL("http://localhost:8080/api/v1/");
        URIBuilder builder = URIBuilder.from(baseUrl);

        // When
        String path = builder.getPath();

        // Then
        assertEquals("/api/v1/", path);
    }

    @Test
    @DisplayName("Should get scheme from base URL")
    void shouldGetSchemeFromBaseUrl() throws Exception {
        // Given
        URL baseUrl = new URL("https://localhost:8443/");
        URIBuilder builder = URIBuilder.from(baseUrl);

        // When
        String scheme = builder.getScheme();

        // Then
        assertEquals("https", scheme);
    }

    @Test
    @DisplayName("Should get port from base URL")
    void shouldGetPortFromBaseUrl() throws Exception {
        // Given
        URL baseUrl = new URL("http://localhost:9090/");
        URIBuilder builder = URIBuilder.from(baseUrl);

        // When
        int port = builder.getPort();

        // Then
        assertEquals(9090, port);
    }

    @Test
    @DisplayName("Should set path replacing existing path segments")
    void shouldSetPathReplacingExistingSegments() throws Exception {
        // Given
        URL baseUrl = new URL("http://localhost:8080/");
        URIBuilder builder = URIBuilder.from(baseUrl)
                .addPathSegment("existing")
                .addPathSegment("segments");

        // When
        URI result = builder
                .setPath("/new/path")
                .build();

        // Then
        assertEquals("http://localhost:8080/new/path", result.toString());
    }

    @Test
    @DisplayName("Should set path and then add additional path segments")
    void shouldSetPathAndThenAddSegments() throws Exception {
        // Given
        URL baseUrl = new URL("http://localhost:8080/");

        // When
        URI result = URIBuilder.from(baseUrl)
                .setPath("/api")
                .addPathSegment("users")
                .build();

        // Then
        assertEquals("http://localhost:8080/api/users", result.toString());
    }

    @Test
    @DisplayName("Should set path with query parameters")
    void shouldSetPathWithQueryParameters() throws Exception {
        // Given
        URL baseUrl = new URL("http://localhost:8080/");

        // When
        URI result = URIBuilder.from(baseUrl)
                .addQueryParameter("param", "value")
                .setPath("/api/users")
                .build();

        // Then
        assertEquals("http://localhost:8080/api/users?param=value", result.toString());
    }
}
