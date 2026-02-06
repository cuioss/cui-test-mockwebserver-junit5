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
package de.cuioss.test.mockwebserver;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.net.URI;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DisplayName("URIBuilder Path Segment Handling Tests")
class URIBuilderPathSegmentTest extends URIBuilderTestBase {

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
    void pathSegmentHandling(String testName, String baseUrlString, String[] pathSegments, String expectedResult) {
        // Use the utility method from the base class to test path segment handling
        // This provides a consistent way to test URI building across all test classes
        assertUriPathBuilding(baseUrlString, builder -> {
            if (pathSegments.length > 0) {
                builder.addPathSegments(pathSegments);
            }
            return builder;
        }, expectedResult);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("additionalPathSegmentTestCases")
    @DisplayName("Additional path segment handling tests")
    void additionalPathSegmentHandlingTests(String testName, String baseUrlString, UnaryOperator<URIBuilder> builderFunction, String expectedResult) {
        // Use the utility method from the base class to test path segment handling
        assertUriPathBuilding(baseUrlString, builderFunction, expectedResult);
    }

    static Stream<Arguments> additionalPathSegmentTestCases() {
        return Stream.of(
                Arguments.of("Should add multiple path segments with varargs method",
                        BASE_URL,
                        (UnaryOperator<URIBuilder>) builder -> builder.addPathSegments(API_PATH, USERS_PATH, ID_123),
                        BASE_URL_WITH_API_USERS_123),

                Arguments.of("Should trim leading and trailing slashes from path segments",
                        BASE_URL,
                        (UnaryOperator<URIBuilder>) builder -> builder.addPathSegments("/" + API_PATH + "/", "//" + USERS_PATH + "//", "/" + ID_123 + "/"),
                        BASE_URL_WITH_API_USERS_123),

                Arguments.of("Should handle empty path segments",
                        BASE_URL,
                        (UnaryOperator<URIBuilder>) builder -> builder.addPathSegments("", "/", API_PATH),
                        BASE_URL_NO_SLASH + "/" + API_PATH)
        );
    }

    @Test
    @DisplayName("Should handle path segment with only whitespace")
    void shouldHandlePathSegmentWithOnlyWhitespace() {
        // Use the utility method from the base class to test whitespace path segment handling
        assertUriPathBuilding(BASE_URL,
                builder -> builder.addPathSegment("   ").addPathSegment(API_PATH),
                BASE_URL_NO_SLASH + "/" + API_PATH);
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
        // Use the utility method from the base class to test mixed slashes handling
        assertUriPathBuilding(BASE_URL,
                builder -> builder
                        .addPathSegment(API_PATH + "/")
                        .addPathSegment("/" + USERS_PATH),
                BASE_URL_NO_SLASH + "/" + API_PATH + "/" + USERS_PATH);
    }

    @Test
    @DisplayName("Should handle path segments with only slashes")
    void shouldHandlePathSegmentsWithOnlySlashes() {
        // Use the utility method from the base class to test handling of path segments with only slashes
        assertUriPathBuilding(BASE_URL,
                builder -> builder
                        .addPathSegment("/")
                        .addPathSegment("///")
                        .addPathSegment(API_PATH),
                BASE_URL_NO_SLASH + "/" + API_PATH);
    }

    @Test
    @DisplayName("Should handle base URL with path and trailing slash when adding segments")
    void shouldHandleBaseUrlWithPathAndTrailingSlashWhenAddingSegments() {
        // Use the utility method from the base class to test base URL with path and trailing slash
        assertUriPathBuilding(BASE_URL_WITH_BASE,
                builder -> builder
                        .addPathSegment(API_PATH)
                        .addPathSegment(USERS_PATH),
                BASE_URL_WITH_BASE_NO_SLASH + "/" + API_PATH + "/" + USERS_PATH);
    }

    @Test
    @DisplayName("Should handle empty path segments array")
    void shouldHandleEmptyPathSegmentsArray() {
        // Use the utility method from the base class to test empty path segments array
        assertUriPathBuilding(BASE_URL,
                URIBuilder::addPathSegments,
                BASE_URL_NO_SLASH);
    }

    @Test
    @DisplayName("Should throw NullPointerException when path segment is null")
    void shouldThrowExceptionWhenPathSegmentIsNull() {
        // Given: A base URI
        URI baseUri = URI.create(BASE_URL);
        URIBuilder builder = URIBuilder.from(baseUri);

        // Then: Use the utility method from the base class to test exception handling
        assertThrowsWithMessage(
                NullPointerException.class,
                () -> builder.addPathSegment(null),
                "segment is marked non-null but is null");
    }

    @Test
    @DisplayName("Should throw NullPointerException when path segments array is null")
    void shouldThrowExceptionWhenPathSegmentsArrayIsNull() {
        // Given: A base URI
        URI baseUri = URI.create(BASE_URL);
        URIBuilder builder = URIBuilder.from(baseUri);

        // Then: Use the utility method from the base class to test exception handling
        assertThrowsWithMessage(
                NullPointerException.class,
                () -> builder.addPathSegments((String[]) null),
                "segments is marked non-null but is null");
    }
}
