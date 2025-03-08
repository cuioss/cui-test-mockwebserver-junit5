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

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.function.UnaryOperator;
import java.util.stream.Stream;

/**
 * Tests for special character handling functionality of {@link URIBuilder}.
 */
class URIBuilderSpecialCharacterTest extends URIBuilderTestBase {

    // tag::special-character-handling[]
    /**
     * Provides test cases for special character handling in URIs.
     * Each test case consists of:
     * 1. A display name for the test
     * 2. The base URL to use
     * 3. A setup function that configures the builder with special characters
     * 4. The expected URI string result
     */
    static Stream<Arguments> specialCharacterTestCases() {
        return Stream.of(
                // Test path segments with encoded special characters
                Arguments.of("Should handle path segments with encoded special characters",
                        BASE_URL,
                        (UnaryOperator<URIBuilder>) builder ->
                                builder.addPathSegment(API_PATH)
                                        .addPathSegment(ENCODED_SPACES),
                        BASE_URL_NO_SLASH + "/" + API_PATH + "/" + ENCODED_SPACES),

                // Test query parameters with encoded special characters
                Arguments.of("Should handle query parameters with encoded special characters",
                        BASE_URL,
                        (UnaryOperator<URIBuilder>) builder ->
                                builder.addPathSegment(API_PATH)
                                        .addQueryParameter(FILTER_PARAM, ENCODED_NAME_SPACES),
                        BASE_URL_NO_SLASH + "/" + API_PATH + "?" + FILTER_PARAM + "=" + ENCODED_NAME_SPACES)
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("specialCharacterTestCases")
    void specialCharacterHandling(String testName, String baseUrlString, UnaryOperator<URIBuilder> setup, String expectedResult) {
        // Use the utility method from the base class to test URI building with special characters
        // This handles both path segments and query parameters in a consistent way
        assertUriBuilding(baseUrlString, setup, expectedResult);
    }

    // end::special-character-handling[]

    /**
     * Provides test cases for complex URL building.
     * Each test case consists of:
     * 1. A display name for the test
     * 2. The base URL to use
     * 3. A setup function that configures the builder with multiple components
     * 4. The expected URI string result
     */
    static Stream<Arguments> complexUrlBuildingTestCases() {
        return Stream.of(
                // Test complex URL with port, path and query parameters
                Arguments.of("Should handle complex URL with port, path and query parameters",
                        COMPLEX_BASE_URL,
                        (UnaryOperator<URIBuilder>) builder ->
                                builder.addPathSegment(API_V1_PATH)
                                        .addPathSegment(RESOURCES_PATH)
                                        .addQueryParameter(PAGE_PARAM, "1")
                                        .addQueryParameter(SIZE_PARAM, "10")
                                        .addQueryParameter(SORT_PARAM, "name,asc"),
                        COMPLEX_BASE_URL + "/" + API_V1_PATH + "/" + RESOURCES_PATH + "?" +
                                PAGE_PARAM + "=1&" + SIZE_PARAM + "=10&" + SORT_PARAM + "=name,asc")
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("complexUrlBuildingTestCases")
    void complexUrlBuilding(String testName, String baseUrlString, UnaryOperator<URIBuilder> setup, String expectedResult) {
        // Use the utility method from the base class to test complex URL building
        // This handles both path segments and query parameters in a consistent way
        assertUriBuilding(baseUrlString, setup, expectedResult);
    }
}
