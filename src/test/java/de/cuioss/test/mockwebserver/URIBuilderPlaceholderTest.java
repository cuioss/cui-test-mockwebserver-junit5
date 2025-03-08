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

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for placeholder functionality of {@link URIBuilder}.
 */
class URIBuilderPlaceholderTest extends URIBuilderTestBase {

    // tag::uribuilder-placeholder-test[]
    @Test
    @DisplayName("Should create a placeholder URIBuilder")
    void shouldCreatePlaceholderURIBuilder() {
        // When: Creating a placeholder URIBuilder
        URIBuilder placeholder = URIBuilder.placeholder();

        // Then: Default values for placeholder
        assertEquals("/", placeholder.getPath());
        assertEquals("http", placeholder.getScheme());
        assertEquals(-1, placeholder.getPort());
    }

    // end::uribuilder-placeholder-test[]
    
    // tag::uribuilder-placeholder-exception-test[]
    /**
     * Provides test cases for placeholder exception handling.
     * Each test case consists of:
     * 1. A display name for the test
     * 2. A function to call on the placeholder that should throw an exception
     * 3. The expected exception message
     */
    static Stream<Arguments> placeholderExceptionTestCases() {
        return Stream.of(
                // Test building URI from placeholder
                Arguments.of("Should throw IllegalStateException when building URI from placeholder",
                        (Function<URIBuilder, Object>) URIBuilder::build,
                        "Cannot build URI from placeholder URIBuilder. " +
                                "The server must be started first, and a proper URIBuilder must be created using URIBuilder.from(server.url('/')).url())"),

                // Test building string from placeholder
                Arguments.of("Should throw IllegalStateException when building string from placeholder",
                        (Function<URIBuilder, Object>) URIBuilder::buildAsString,
                        "Cannot build URI from placeholder URIBuilder. " +
                                "The server must be started first, and a proper URIBuilder must be created using URIBuilder.from(server.url('/').url())")
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("placeholderExceptionTestCases")
    void placeholderExceptions(String testName, Function<URIBuilder, Object> functionToCall, String expectedMessage) {
        // Given: A placeholder URIBuilder
        URIBuilder placeholder = URIBuilder.placeholder();

        // When/Then: Use the utility method from the base class to test exception handling
        assertThrowsWithMessage(
                IllegalStateException.class,
                () -> functionToCall.apply(placeholder),
                expectedMessage);
    }

    // end::uribuilder-placeholder-exception-test[]

    /**
     * Provides test cases for placeholder behavior.
     * Each test case consists of:
     * 1. A display name for the test
     * 2. A setup function that configures the placeholder
     * 3. A verification function that checks the expected behavior
     */
    static Stream<Arguments> placeholderBehaviorTestCases() {
        return Stream.of(
                // Test adding path segments to placeholder
                Arguments.of("Should allow adding path segments to placeholder without error",
                        (Consumer<URIBuilder>) builder -> {
                            builder.addPathSegment(API_PATH);
                            builder.addPathSegments(USERS_PATH, ID_123);
                        },
                        (Consumer<URIBuilder>) builder -> {
                            assertEquals(3, builder.getPathSegments().size());
                            List<String> pathSegments = builder.getPathSegments();
                            assertTrue(pathSegments.contains(API_PATH));
                            assertTrue(pathSegments.contains(USERS_PATH));
                            assertTrue(pathSegments.contains(ID_123));
                            assertEquals(API_PATH, pathSegments.get(0));
                            assertEquals(USERS_PATH, pathSegments.get(1));
                            assertEquals(ID_123, pathSegments.get(2));
                        }),

                // Test adding query parameters to placeholder
                Arguments.of("Should allow adding query parameters to placeholder without error",
                        (Consumer<URIBuilder>) builder ->
                                builder.addQueryParameter(NAME_PARAM, VALUE_PARAM),
                        (Consumer<URIBuilder>) builder -> {
                            assertEquals(1, builder.getQueryParameters().size());
                            assertTrue(builder.getQueryParameters().containsKey(NAME_PARAM));
                            List<String> values = builder.getQueryParameters().get(NAME_PARAM);
                            assertNotNull(values);
                            assertEquals(1, values.size());
                            assertEquals(VALUE_PARAM, values.get(0));
                        })
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("placeholderBehaviorTestCases")
    @SuppressWarnings("java:S2699") // owolff: The assertion is in the lambda
    void placeholderBehavior(String testName, Consumer<URIBuilder> setup, Consumer<URIBuilder> verification) {
        // Given: A placeholder URIBuilder
        URIBuilder placeholder = URIBuilder.placeholder();

        // When: Applying the setup function
        setup.accept(placeholder);

        // Then: Verify the expected behavior
        verification.accept(placeholder);
    }
}
