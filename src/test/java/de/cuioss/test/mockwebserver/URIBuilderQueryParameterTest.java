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
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Tests for query parameter handling functionality of {@link URIBuilder}.
 */
@SuppressWarnings("DataFlowIssue")
class URIBuilderQueryParameterTest extends URIBuilderTestBase {

    // tag::query-parameter-handling[]
    /**
     * Provides test cases for query parameter handling.
     * Each test case consists of:
     * 1. A display name for the test
     * 2. The base URL to use
     * 3. A map of parameter names to arrays of values
     * 4. The expected URI string result
     */
    static Stream<Arguments> queryParameterTestCases() {
        return Stream.of(
                // Single query parameter
                Arguments.of("Should add a single query parameter",
                        BASE_URL,
                        Map.of(NAME_PARAM, new String[]{VALUE_PARAM}),
                        BASE_URL_NO_SLASH + "?" + NAME_PARAM + "=" + VALUE_PARAM),

                // Multiple query parameters
                Arguments.of("Should add multiple query parameters",
                        BASE_URL,
                        Map.of(
                                "name1", new String[]{VALUE1_PARAM},
                                "name2", new String[]{VALUE2_PARAM},
                                "name3", new String[]{VALUE3_PARAM}
                        ),
                        BASE_URL_NO_SLASH + "?name1=" + VALUE1_PARAM + "&name2=" + VALUE2_PARAM + "&name3=" + VALUE3_PARAM),

                // Multiple values for the same query parameter
                Arguments.of("Should add multiple values for the same query parameter",
                        BASE_URL,
                        Map.of(NAME_PARAM, new String[]{VALUE1_PARAM, VALUE2_PARAM}),
                        BASE_URL_NO_SLASH + "?" + NAME_PARAM + "=" + VALUE1_PARAM + "&" + NAME_PARAM + "=" + VALUE2_PARAM),

                // Handle multiple query parameters with same name and different values
                Arguments.of("Should handle multiple query parameters with same name and different values",
                        BASE_URL,
                        Map.of(PARAM_NAME, new String[]{VALUE1_PARAM, VALUE2_PARAM, VALUE3_PARAM}),
                        BASE_URL_NO_SLASH + "?" + PARAM_NAME + "=" + VALUE1_PARAM + "&" + PARAM_NAME + "=" + VALUE2_PARAM + "&" + PARAM_NAME + "=" + VALUE3_PARAM),

                // Handle query parameters with encoded special characters
                Arguments.of("Should handle query parameters with special characters",
                        BASE_URL_NO_SLASH + "/" + API_PATH,
                        Map.of(FILTER_PARAM, new String[]{ENCODED_NAME_SPACES}),
                        BASE_URL_NO_SLASH + "/" + API_PATH + "?" + FILTER_PARAM + "=" + ENCODED_NAME_SPACES)
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("queryParameterTestCases")
    void testQueryParameterHandling(String testName, String baseUrlString, Map<String, String[]> queryParams, String expectedResult) {
        // Given: Create a URIBuilder with the specified base URL
        URI baseUri = URI.create(baseUrlString);
        URIBuilder builder = URIBuilder.from(baseUri);

        // When: Add query parameters and build the URI
        queryParams.forEach((paramName, values) -> {
            for (String value : values) {
                builder.addQueryParameter(paramName, value);
            }
        });

        // Then: Use the utility method from the base class to verify the result
        // This handles query parameter order differences automatically
        assertUriBuilding(baseUrlString, b -> builder, expectedResult);
    }

    // end::query-parameter-handling[]

    @Test
    @DisplayName("Should combine path segments and query parameters")
    void shouldCombinePathSegmentsAndQueryParameters() {
        // Given: Expected URI with path segments and query parameters
        String expectedUri = BASE_URL_NO_SLASH + "/" + API_PATH + "/" + USERS_PATH + "?filter=active&page=1";

        // When/Then: Use the utility method from the base class to verify the result
        assertUriBuilding(BASE_URL, builder -> builder
                        .addPathSegments(API_PATH, USERS_PATH)
                        .addQueryParameter(FILTER_PARAM, "active")
                        .addQueryParameter(PAGE_PARAM, "1"),
                expectedUri);
    }

    @Test
    @DisplayName("Should handle empty query parameters list")
    void shouldHandleEmptyQueryParametersList() {
        // Given: Expected URI with only path segment
        String expectedUri = BASE_URL_NO_SLASH + "/" + API_PATH;

        // When/Then: Use the utility method from the base class to verify the result
        assertUriPathBuilding(BASE_URL, builder -> builder
                        .addPathSegment(API_PATH),
                expectedUri);

        // Additional verification for null query
        URI result = URIBuilder.from(URI.create(BASE_URL))
                .addPathSegment(API_PATH)
                .build();
        assertNull(result.getQuery(), "Query string should be null");
    }

    @Test
    @DisplayName("Should handle multiple query parameters with same name and different values")
    void shouldHandleMultipleQueryParametersWithSameNameAndDifferentValues() {
        // Given: Expected URI with multiple query parameters with the same name
        String expectedUri = BASE_URL_NO_SLASH + "?" + PARAM_NAME + "=" + VALUE1_PARAM +
                "&" + PARAM_NAME + "=" + VALUE2_PARAM +
                "&" + PARAM_NAME + "=" + VALUE3_PARAM;

        // When/Then: Use the utility method from the base class to verify the result
        assertUriBuilding(BASE_URL, builder -> builder
                        .addQueryParameter(PARAM_NAME, VALUE1_PARAM)
                        .addQueryParameter(PARAM_NAME, VALUE2_PARAM)
                        .addQueryParameter(PARAM_NAME, VALUE3_PARAM),
                expectedUri);
    }

    @Test
    @DisplayName("Should throw NullPointerException when query parameter name is null")
    void shouldThrowExceptionWhenQueryParameterNameIsNull() {
        // Given: A base URI
        URI baseUri = URI.create(BASE_URL);
        URIBuilder builder = URIBuilder.from(baseUri);

        // Then: Verify NullPointerException is thrown using the utility method
        assertThrowsWithMessage(
                NullPointerException.class,
                () -> builder.addQueryParameter(null, VALUE_PARAM),
                "name is marked non-null but is null");
    }

    @Test
    @DisplayName("Should throw NullPointerException when query parameter value is null")
    void shouldThrowExceptionWhenQueryParameterValueIsNull() {
        // Given: A base URI
        URI baseUri = URI.create(BASE_URL);
        URIBuilder builder = URIBuilder.from(baseUri);

        // Then: Verify NullPointerException is thrown using the utility method
        assertThrowsWithMessage(
                NullPointerException.class,
                () -> builder.addQueryParameter(NAME_PARAM, null),
                "value is marked non-null but is null");
    }
}
