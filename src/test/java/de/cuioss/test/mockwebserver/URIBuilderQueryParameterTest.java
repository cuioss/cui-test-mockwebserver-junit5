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
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for query parameter handling functionality of {@link URIBuilder}.
 */
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
        URI result = builder.build();

        // Then: Verify components separately since query parameter order may vary
        URI expectedUri = URI.create(expectedResult);

        // Verify scheme, host, port, and path are the same
        assertEquals(expectedUri.getScheme(), result.getScheme());
        assertEquals(expectedUri.getHost(), result.getHost());
        assertEquals(expectedUri.getPort(), result.getPort());
        assertEquals(expectedUri.getPath(), result.getPath());

        // Verify query parameters (order-independent)
        Map<String, List<String>> expectedParams = parseQueryParams(expectedUri.getQuery());
        Map<String, List<String>> actualParams = parseQueryParams(result.getQuery());
        assertEquals(expectedParams, actualParams, "Query parameters don't match");
    }

    // end::query-parameter-handling[]

    @Test
    @DisplayName("Should combine path segments and query parameters")
    void shouldCombinePathSegmentsAndQueryParameters() {
        // Given: A base URI
        URI baseUri = URI.create(BASE_URL);

        // When: Adding path segments and query parameters
        URI result = URIBuilder.from(baseUri)
                .addPathSegments(API_PATH, USERS_PATH)
                .addQueryParameter(FILTER_PARAM, "active")
                .addQueryParameter(PAGE_PARAM, "1")
                .build();

        // Then: Verify the result contains both path segments and query parameters
        assertEquals(BASE_URL_NO_SLASH + "/" + API_PATH + "/" + USERS_PATH + "?filter=active&page=1", result.toString());
    }

    @Test
    @DisplayName("Should handle empty query parameters list")
    void shouldHandleEmptyQueryParametersList() {
        // Given: A base URI
        URI baseUri = URI.create(BASE_URL);

        // When: Adding path segment but no query parameters
        URI result = URIBuilder.from(baseUri)
                .addPathSegment(API_PATH)
                // No query parameters added
                .build();

        // Then: Verify the result has no query string
        assertEquals(BASE_URL_NO_SLASH + "/" + API_PATH, result.toString());
        // Verify the query string is empty
        assertNull(result.getQuery());
    }

    @Test
    @DisplayName("Should handle multiple query parameters with same name and different values")
    void shouldHandleMultipleQueryParametersWithSameNameAndDifferentValues() {
        // Given: A base URI
        URI baseUri = URI.create(BASE_URL);

        // When: Adding multiple query parameters with the same name
        URI result = URIBuilder.from(baseUri)
                .addQueryParameter(PARAM_NAME, VALUE1_PARAM)
                .addQueryParameter(PARAM_NAME, VALUE2_PARAM)
                .addQueryParameter(PARAM_NAME, VALUE3_PARAM)
                .build();

        // Then: Verify all parameters are included
        String expectedUri = BASE_URL_NO_SLASH + "?" + PARAM_NAME + "=" + VALUE1_PARAM +
                "&" + PARAM_NAME + "=" + VALUE2_PARAM +
                "&" + PARAM_NAME + "=" + VALUE3_PARAM;

        // Compare using the query parameter map to handle order differences
        URI expectedUriObj = URI.create(expectedUri);
        Map<String, List<String>> expectedParams = parseQueryParams(expectedUriObj.getQuery());
        Map<String, List<String>> actualParams = parseQueryParams(result.getQuery());
        assertEquals(expectedParams, actualParams, "Query parameters don't match");
    }

    @Test
    @DisplayName("Should throw NullPointerException when query parameter name is null")
    void shouldThrowExceptionWhenQueryParameterNameIsNull() {
        // Given: A base URI
        URI baseUri = URI.create(BASE_URL);
        URIBuilder builder = URIBuilder.from(baseUri);

        // Then: Verify NullPointerException is thrown
        assertThrows(NullPointerException.class, () -> builder.addQueryParameter(null, VALUE_PARAM));
    }

    @Test
    @DisplayName("Should throw NullPointerException when query parameter value is null")
    void shouldThrowExceptionWhenQueryParameterValueIsNull() {
        // Given: A base URI
        URI baseUri = URI.create(BASE_URL);
        URIBuilder builder = URIBuilder.from(baseUri);

        // Then: Verify NullPointerException is thrown
        assertThrows(NullPointerException.class, () -> builder.addQueryParameter(NAME_PARAM, null));
    }
}
