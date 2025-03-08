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
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * Tests for exception handling functionality of {@link URIBuilder}.
 */
@SuppressWarnings({"ConstantValue", "DataFlowIssue"})
class URIBuilderExceptionTest extends URIBuilderTestBase {

    // tag::exception-handling[]
    @Test
    @DisplayName("Should throw NullPointerException when base URL is null")
    void shouldThrowExceptionWhenBaseUrlIsNull() {
        // Given: A null URI
        URI nullUri = null;

        // When/Then: Use the utility method from the base class to test exception handling
        assertThrowsWithMessage(
                NullPointerException.class,
                () -> URIBuilder.from(nullUri),
                "baseUri is marked non-null but is null");
    }

    /**
     * Provides test cases for null base URL exception handling.
     * Each test case consists of:
     * 1. A display name for the test
     * 2. A function to call on the builder that should throw an exception
     * 3. The expected exception message
     */
    static Stream<Arguments> nullBaseUrlExceptionTestCases() {
        return Stream.of(
                // Test building URI with null baseUrl
                Arguments.of("Should throw IllegalStateException when baseUrl is null in non-placeholder URIBuilder",
                        (Function<URIBuilder, Object>) URIBuilder::build,
                        "Cannot build URI with null baseUrl. This might indicate an incorrectly initialized URIBuilder."),

                // Test getting path with null baseUrl
                Arguments.of("Should handle null baseUrl in getPath()",
                        (Function<URIBuilder, Object>) URIBuilder::getPath,
                        "Cannot access path with null baseUrl. This might indicate an incorrectly initialized URIBuilder."),

                // Test getting scheme with null baseUrl
                Arguments.of("Should handle null baseUrl in getScheme()",
                        (Function<URIBuilder, Object>) URIBuilder::getScheme,
                        "Cannot access scheme with null baseUrl. This might indicate an incorrectly initialized URIBuilder.")
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("nullBaseUrlExceptionTestCases")
    void nullBaseUrlExceptions(String testName, Function<URIBuilder, Object> functionToCall, String expectedMessage)
            throws NoSuchFieldException, IllegalAccessException {
        // Given: Create a URIBuilder and set its baseUrl field to null using reflection
        URIBuilder builder = URIBuilder.from(URI.create(BASE_URL));
        setBaseUrlToNull(builder);

        // When/Then: Use the utility method from the base class to test exception handling
        assertThrowsWithMessage(
                IllegalStateException.class,
                () -> functionToCall.apply(builder),
                expectedMessage);
    }

    // end::exception-handling[]

    @Test
    @DisplayName("Should handle null baseUrl in getPort()")
    void shouldHandleNullBaseUrlInGetPort()
            throws NoSuchFieldException, IllegalAccessException {
        // Given: Create a URIBuilder and set its baseUrl field to null using reflection
        URIBuilder builder = URIBuilder.from(URI.create(BASE_URL));
        setBaseUrlToNull(builder);

        // When/Then: Use the utility method from the base class to test exception handling
        assertThrowsWithMessage(
                IllegalStateException.class,
                builder::getPort,
                "Cannot access port with null baseUrl. This might indicate an incorrectly initialized URIBuilder.");
    }
}
