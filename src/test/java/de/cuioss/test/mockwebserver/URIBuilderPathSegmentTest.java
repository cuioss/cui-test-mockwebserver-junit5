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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.net.URI;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests for path segment handling functionality of {@link URIBuilder}.
 */
class URIBuilderPathSegmentTest extends URIBuilderTestBase {

    // tag::path-segment-handling[]
    /**
     * Provides test cases for path segment handling.
     * Each test case consists of:
     * 1. A display name for the test
     * 2. The base URL to use
     * 3. An array of path segments to add
     * 4. The expected URI string result
     */
    static Stream<Arguments> pathSegmentTestCases() {
        return Stream.of(
                // Single path segment
                Arguments.of("Should add a single path segment",
                        BASE_URL,
                        new String[]{API_PATH},
                        BASE_URL_NO_SLASH + "/" + API_PATH),

                // Multiple path segments
                Arguments.of("Should add multiple path segments",
                        BASE_URL,
                        new String[]{API_PATH, USERS_PATH, ID_123},
                        BASE_URL_NO_SLASH + "/" + API_PATH + "/" + USERS_PATH + "/" + ID_123),

                // Trim slashes from path segments
                Arguments.of("Should trim slashes from path segments",
                        BASE_URL,
                        new String[]{"/" + API_PATH + "/", "//" + USERS_PATH + "//", "/" + ID_123 + "/"},
                        BASE_URL_NO_SLASH + "/" + API_PATH + "/" + USERS_PATH + "/" + ID_123),

                // Handle empty path segments
                Arguments.of("Should handle empty path segments",
                        BASE_URL,
                        new String[]{"", "/", API_PATH},
                        BASE_URL_NO_SLASH + "/" + API_PATH),

                // Handle path segments with mixed slashes
                Arguments.of("Should handle path segments with mixed slashes",
                        BASE_URL,
                        new String[]{API_PATH + "/", "/" + USERS_PATH},
                        BASE_URL_NO_SLASH + "/" + API_PATH + "/" + USERS_PATH),

                // Handle path segments with only slashes
                Arguments.of("Should handle path segments with only slashes",
                        BASE_URL,
                        new String[]{"/", "///", API_PATH},
                        BASE_URL_NO_SLASH + "/" + API_PATH),

                // Handle path segment with only whitespace
                Arguments.of("Should handle path segment with only whitespace",
                        BASE_URL,
                        new String[]{"   ", API_PATH},
                        BASE_URL_NO_SLASH + "/" + API_PATH),

                // Handle base URL without trailing slash
                Arguments.of("Should handle base URL without trailing slash",
                        BASE_URL_NO_SLASH,
                        new String[]{API_PATH},
                        BASE_URL_NO_SLASH + "/" + API_PATH),

                // Handle base URL with path and trailing slash
                Arguments.of("Should handle base URL with path and trailing slash",
                        BASE_URL_WITH_BASE,
                        new String[]{API_PATH, USERS_PATH},
                        BASE_URL_WITH_BASE_NO_SLASH + "/" + API_PATH + "/" + USERS_PATH),

                // Handle empty path segments array
                Arguments.of("Should handle empty path segments array",
                        BASE_URL,
                        new String[]{},
                        BASE_URL_NO_SLASH),

                // Handle path segments with encoded special characters
                Arguments.of("Should handle path segments with special characters",
                        BASE_URL,
                        new String[]{API_PATH, ENCODED_SPACES},
                        BASE_URL_NO_SLASH + "/" + API_PATH + "/" + ENCODED_SPACES)
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("pathSegmentTestCases")
    void testPathSegmentHandling(String testName, String baseUrlString, String[] pathSegments, String expectedResult) {
        // Given: Create a URIBuilder with the specified base URL
        URI baseUri = URI.create(baseUrlString);
        URIBuilder builder = URIBuilder.from(baseUri);

        // When: Add path segments if any and build the URI
        if (pathSegments.length > 0) {
            builder.addPathSegments(pathSegments);
        }
        URI result = builder.build();

        // Then: Verify the result matches the expected URI string
        assertEquals(expectedResult, result.toString());
    }

    // end::path-segment-handling[]

    @Test
    @DisplayName("Should add multiple path segments with varargs method")
    void shouldAddMultiplePathSegmentsWithVarargs() {
        // Given: A base URI
        URI baseUri = URI.create(BASE_URL);

        // When: Adding multiple path segments with varargs and building the URI
        URI result = URIBuilder.from(baseUri)
                .addPathSegments(API_PATH, USERS_PATH, ID_123)
                .build();

        // Then: Verify the result has all path segments
        assertEquals(BASE_URL_WITH_API_USERS_123, result.toString());
    }

    @Test
    @DisplayName("Should trim leading and trailing slashes from path segments")
    void shouldTrimSlashesFromPathSegments() {
        // Given: A base URI
        URI baseUri = URI.create(BASE_URL);

        // When: Adding path segments with extra slashes
        URI result = URIBuilder.from(baseUri)
                .addPathSegments("/" + API_PATH + "/", "//" + USERS_PATH + "//", "/" + ID_123 + "/")
                .build();

        // Then: Verify slashes are properly trimmed
        assertEquals(BASE_URL_WITH_API_USERS_123, result.toString());
    }

    @Test
    @DisplayName("Should handle empty path segments")
    void shouldHandleEmptyPathSegments() {
        // Given: A base URI
        URI baseUri = URI.create(BASE_URL);

        // When: Adding empty path segments and a valid one
        URI result = URIBuilder.from(baseUri)
                .addPathSegments("", "/", API_PATH)
                .build();

        // Then: Verify empty segments are ignored
        assertEquals(BASE_URL_NO_SLASH + "/" + API_PATH, result.toString());
    }

    @Test
    @DisplayName("Should handle path segment with only whitespace")
    void shouldHandlePathSegmentWithOnlyWhitespace() {
        // Given: A base URI
        URI baseUri = URI.create(BASE_URL);

        // When: Adding a whitespace path segment and a valid one
        URI result = URIBuilder.from(baseUri)
                .addPathSegment("   ")
                .addPathSegment(API_PATH)
                .build();

        // Then: Verify whitespace segments are ignored
        assertEquals(BASE_URL_NO_SLASH + "/" + API_PATH, result.toString());
    }

    @Test
    @DisplayName("Should handle base URL without trailing slash")
    void shouldHandleBaseUrlWithoutTrailingSlash() {
        // Given: A base URI without trailing slash
        URI baseUri = URI.create(BASE_URL_NO_SLASH);

        // When: Adding a path segment
        URI result = URIBuilder.from(baseUri)
                .addPathSegment(API_PATH)
                .build();

        // Then: Verify slash is added between base URL and path segment
        assertEquals(BASE_URL_NO_SLASH + "/" + API_PATH, result.toString());
    }

    @Test
    @DisplayName("Should handle path segments with mixed slashes")
    void shouldHandlePathSegmentsWithMixedSlashes() {
        // Given: A base URI
        URI baseUri = URI.create(BASE_URL);

        // When: Adding path segments with mixed slashes
        URI result = URIBuilder.from(baseUri)
                .addPathSegment(API_PATH + "/")
                .addPathSegment("/" + USERS_PATH)
                .build();

        // Then: Verify slashes are properly handled
        assertEquals(BASE_URL_NO_SLASH + "/" + API_PATH + "/" + USERS_PATH, result.toString());
    }

    @Test
    @DisplayName("Should handle path segments with only slashes")
    void shouldHandlePathSegmentsWithOnlySlashes() {
        // Given: A base URI
        URI baseUri = URI.create(BASE_URL);

        // When: Adding path segments with only slashes
        URI result = URIBuilder.from(baseUri)
                .addPathSegment("/")
                .addPathSegment("///")
                .addPathSegment(API_PATH)
                .build();

        // Then: Verify segments with only slashes are ignored
        assertEquals(BASE_URL_NO_SLASH + "/" + API_PATH, result.toString());
    }

    @Test
    @DisplayName("Should handle base URL with path and trailing slash when adding segments")
    void shouldHandleBaseUrlWithPathAndTrailingSlashWhenAddingSegments() {
        // Given: A base URI with path and trailing slash
        URI baseUri = URI.create(BASE_URL_WITH_BASE);

        // When: Adding path segments
        URI result = URIBuilder.from(baseUri)
                .addPathSegment(API_PATH)
                .addPathSegment(USERS_PATH)
                .build();

        // Then: Verify path segments are properly added
        assertEquals(BASE_URL_WITH_BASE_NO_SLASH + "/" + API_PATH + "/" + USERS_PATH, result.toString());
    }

    @Test
    @DisplayName("Should handle empty path segments array")
    void shouldHandleEmptyPathSegmentsArray() {
        // Given: A base URI
        URI baseUri = URI.create(BASE_URL);

        // When: Adding an empty path segments array
        URI result = URIBuilder.from(baseUri)
                .addPathSegments()
                .build();

        // Then: Verify the URI is unchanged
        assertEquals(BASE_URL_NO_SLASH, result.toString());
    }

    @Test
    @DisplayName("Should throw NullPointerException when path segment is null")
    void shouldThrowExceptionWhenPathSegmentIsNull() {
        // Given: A base URI
        URI baseUri = URI.create(BASE_URL);
        URIBuilder builder = URIBuilder.from(baseUri);

        // Then: Verify NullPointerException is thrown
        assertThrows(NullPointerException.class, () -> builder.addPathSegment(null));
    }

    @Test
    @DisplayName("Should throw NullPointerException when path segments array is null")
    void shouldThrowExceptionWhenPathSegmentsArrayIsNull() {
        // Given: A base URI
        URI baseUri = URI.create(BASE_URL);
        URIBuilder builder = URIBuilder.from(baseUri);

        // Then: Verify NullPointerException is thrown
        assertThrows(NullPointerException.class, () -> builder.addPathSegments((String[]) null));
    }
}
