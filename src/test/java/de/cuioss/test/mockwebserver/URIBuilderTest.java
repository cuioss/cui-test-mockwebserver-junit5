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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link URIBuilder}.
 */
@SuppressWarnings({"DataFlowIssue"})
class URIBuilderTest {

    /**
     * Helper method to set a private field using reflection.
     * This method properly handles the accessibility concerns.
     *
     * @param target the object to modify
     * @throws NoSuchFieldException   if the field does not exist
     * @throws IllegalAccessException if the field cannot be accessed
     */
    private void setPrivateField(Object target)
            throws NoSuchFieldException, IllegalAccessException {
        java.lang.reflect.Field field = target.getClass().getDeclaredField(URIBuilderTest.BASE_URL_FIELD);
        // We need to set accessible to true to modify the private field
        // This is only used for testing exception handling when baseUrl is null
        field.setAccessible(true);
        try {
            // Set the field value
            field.set(target, null);
        } finally {
            // Always restore accessibility to its original state
            field.setAccessible(false);
        }
    }

    // Constants for commonly used string literals
    private static final String BASE_URL = "http://localhost:8080/";
    private static final String BASE_URL_NO_SLASH = "http://localhost:8080";
    private static final String API_PATH = "api";
    private static final String USERS_PATH = "users";
    private static final String ID_123 = "123";
    private static final String NAME_PARAM = "name";
    private static final String VALUE_PARAM = "value";
    private static final String VALUE1_PARAM = "value1";
    private static final String VALUE2_PARAM = "value2";

    private static final String PARAM_NAME = "param";
    private static final String BASE_URL_WITH_API = BASE_URL_NO_SLASH + "/" + API_PATH;
    private static final String BASE_URL_WITH_API_USERS_123 = BASE_URL_WITH_API + "/" + USERS_PATH + "/" + ID_123;
    private static final String BASE_URL_FIELD = "baseUrl";
    private static final String VALUE3_PARAM = "value3";

    @Test
    @DisplayName("Should create a basic URI from a URI")
    void shouldCreateBasicUri() {
        // Given
        URI baseUri = URI.create(BASE_URL);

        // When
        URI result = URIBuilder.from(baseUri).build();

        // Then
        assertEquals(BASE_URL_NO_SLASH, result.toString());
    }

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
        // Given
        URI baseUri = URI.create(baseUrlString);

        // When
        URIBuilder builder = URIBuilder.from(baseUri);
        if (pathSegments.length > 0) {
            builder.addPathSegments(pathSegments);
        }
        URI result = builder.build();

        // Then
        assertEquals(expectedResult, result.toString());
    }

    @Test
    @DisplayName("Should add multiple path segments with varargs method")
    void shouldAddMultiplePathSegmentsWithVarargs() {
        // Given
        URI baseUri = URI.create(BASE_URL);

        // When
        URI result = URIBuilder.from(baseUri)
                .addPathSegments(API_PATH, USERS_PATH, ID_123)
                .build();

        // Then
        assertEquals(BASE_URL_WITH_API_USERS_123, result.toString());
    }

    @Test
    @DisplayName("Should trim leading and trailing slashes from path segments")
    void shouldTrimSlashesFromPathSegments() {
        // Given
        URI baseUri = URI.create(BASE_URL);

        // When
        URI result = URIBuilder.from(baseUri)
                .addPathSegments("/" + API_PATH + "/", "//" + USERS_PATH + "//", "/" + ID_123 + "/")
                .build();

        // Then
        assertEquals(BASE_URL_WITH_API_USERS_123, result.toString());
    }

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
        // Given
        URI baseUri = URI.create(baseUrlString);

        // When
        URIBuilder builder = URIBuilder.from(baseUri);
        queryParams.forEach((paramName, values) -> {
            for (String value : values) {
                builder.addQueryParameter(paramName, value);
            }
        });
        URI result = builder.build();

        // Then
        // Since query parameters may be in any order, we need to verify the components separately
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

    /**
     * Parses a query string into a map of parameter names to values.
     * 
     * @param query the query string to parse
     * @return a map of parameter names to values
     */
    private Map<String, List<String>> parseQueryParams(String query) {
        if (query == null || query.isEmpty()) {
            return Map.of();
        }

        Map<String, List<String>> result = new HashMap<>();
        String[] pairs = query.split("&");

        for (String pair : pairs) {
            String[] keyValue = pair.split("=");
            String key = keyValue[0];
            String value = keyValue.length > 1 ? keyValue[1] : "";

            result.computeIfAbsent(key, k -> new ArrayList<>()).add(value);
        }

        return result;
    }

    @Test
    @DisplayName("Should combine path segments and query parameters")
    void shouldCombinePathSegmentsAndQueryParameters() {
        // Given
        URI baseUri = URI.create(BASE_URL);

        // When
        URI result = URIBuilder.from(baseUri)
                .addPathSegments(API_PATH, USERS_PATH)
                .addQueryParameter(FILTER_PARAM, "active")
                .addQueryParameter(PAGE_PARAM, "1")
                .build();

        // Then
        assertEquals(BASE_URL_NO_SLASH + "/" + API_PATH + "/" + USERS_PATH + "?filter=active&page=1", result.toString());
    }

    @Test
    @DisplayName("Should handle base URL with path")
    void shouldHandleBaseUrlWithPath() {
        // Given
        URI baseUri = URI.create(BASE_URL_WITH_BASE);

        // When
        URI result = URIBuilder.from(baseUri)
                .addPathSegment(API_PATH)
                .build();

        // Then
        assertEquals(BASE_URL_WITH_BASE + API_PATH, result.toString());
    }

    @Test
    @DisplayName("Should handle empty path segments")
    void shouldHandleEmptyPathSegments() {
        // Given
        URI baseUri = URI.create(BASE_URL);

        // When
        URI result = URIBuilder.from(baseUri)
                .addPathSegments("", "/", API_PATH)
                .build();

        // Then
        assertEquals(BASE_URL_NO_SLASH + "/" + API_PATH, result.toString());
    }

    // tag::uribuilder-placeholder-test[]
    @Test
    @DisplayName("Should create a placeholder URIBuilder")
    void shouldCreatePlaceholderURIBuilder() {
        // When
        URIBuilder placeholder = URIBuilder.placeholder();

        // Then - Default values for placeholder
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
    void testPlaceholderExceptions(String testName, Function<URIBuilder, Object> functionToCall, String expectedMessage) {
        // Given
        URIBuilder placeholder = URIBuilder.placeholder();

        // When/Then
        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> functionToCall.apply(placeholder));
        assertEquals(expectedMessage, exception.getMessage());
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
    void testPlaceholderBehavior(String testName, Consumer<URIBuilder> setup, Consumer<URIBuilder> verification) {
        // Given
        URIBuilder placeholder = URIBuilder.placeholder();

        // When
        setup.accept(placeholder);

        // Then
        verification.accept(placeholder);
    }

    /**
     * Provides test cases for URI building with different components.
     * Each test case consists of:
     * 1. A display name for the test
     * 2. The base URL to use
     * 3. A setup function that configures the builder
     * 4. The expected URI string result
     */
    static Stream<Arguments> uriBuildingTestCases() {
        return Stream.of(
                // Test returning URI as string
                Arguments.of("Should return URI as string",
                        BASE_URL,
                        (UnaryOperator<URIBuilder>) builder ->
                                builder.addPathSegment(API_PATH)
                                        .addQueryParameter(NAME_PARAM, VALUE_PARAM),
                        BASE_URL_NO_SLASH + "/" + API_PATH + "?" + NAME_PARAM + "=" + VALUE_PARAM)
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("uriBuildingTestCases")
    void testUriBuilding(String testName, String baseUrlString, Function<URIBuilder, URIBuilder> setup, String expectedResult) {
        // Given
        URI baseUri = URI.create(baseUrlString);
        URIBuilder builder = URIBuilder.from(baseUri);

        // When
        String result = setup.apply(builder).buildAsString();

        // Then
        assertEquals(expectedResult, result);
    }

    @Test
    @DisplayName("Should throw NullPointerException when base URL is null")
    void shouldThrowExceptionWhenBaseUrlIsNull() {
        // Explicitly cast null to URI to avoid ambiguous method reference
        URI nullUri = null;
        assertThrows(NullPointerException.class, () -> URIBuilder.from(nullUri));
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
    void testNullBaseUrlExceptions(String testName, Function<URIBuilder, Object> functionToCall, String expectedMessage)
            throws NoSuchFieldException, IllegalAccessException {
        // Create a URIBuilder with reflection to bypass the normal constructor checks
        URIBuilder builder = URIBuilder.from(URI.create(BASE_URL));

        // Use reflection to set the baseUrl field to null
        // This is necessary for testing exception handling when baseUrl is null
        setPrivateField(builder);

        // When/Then
        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> functionToCall.apply(builder));
        assertEquals(expectedMessage, exception.getMessage());
    }

    @Test
    @DisplayName("Should handle null baseUrl in getPort()")
    void shouldHandleNullBaseUrlInGetPort()
            throws NoSuchFieldException, IllegalAccessException {
        // Create a URIBuilder with reflection to bypass the normal constructor checks
        URIBuilder builder = URIBuilder.from(URI.create(BASE_URL));

        // Use reflection to set the baseUrl field to null
        // This is necessary for testing exception handling when baseUrl is null
        setPrivateField(builder);

        // When/Then
        IllegalStateException exception = assertThrows(IllegalStateException.class,
                builder::getPort);
        assertEquals("Cannot access port with null baseUrl. This might indicate an incorrectly initialized URIBuilder.",
                exception.getMessage());
    }

    @Test
    @DisplayName("Should throw NullPointerException when path segment is null")
    void shouldThrowExceptionWhenPathSegmentIsNull() {
        // Given
        URI baseUri = URI.create(BASE_URL);
        URIBuilder builder = URIBuilder.from(baseUri);

        // Then
        assertThrows(NullPointerException.class, () -> builder.addPathSegment(null));
    }

    @Test
    @DisplayName("Should throw NullPointerException when path segments array is null")
    void shouldThrowExceptionWhenPathSegmentsArrayIsNull() {
        // Given
        URI baseUri = URI.create(BASE_URL);
        URIBuilder builder = URIBuilder.from(baseUri);

        // Then
        assertThrows(NullPointerException.class, () -> builder.addPathSegments((String[]) null));
    }

    @Test
    @DisplayName("Should throw NullPointerException when query parameter name is null")
    void shouldThrowExceptionWhenQueryParameterNameIsNull() {
        // Given
        URI baseUri = URI.create(BASE_URL);
        URIBuilder builder = URIBuilder.from(baseUri);

        // Then
        assertThrows(NullPointerException.class, () -> builder.addQueryParameter(null, VALUE_PARAM));
    }

    @Test
    @DisplayName("Should throw NullPointerException when query parameter value is null")
    void shouldThrowExceptionWhenQueryParameterValueIsNull() {
        // Given
        URI baseUri = URI.create(BASE_URL);
        URIBuilder builder = URIBuilder.from(baseUri);

        // Then
        assertThrows(NullPointerException.class, () -> builder.addQueryParameter(NAME_PARAM, null));
    }

    @Test
    @DisplayName("Should get path from base URL")
    void shouldGetPathFromBaseUrl() {
        // Given
        URI baseUri = URI.create("http://localhost:8080/api/v1/");
        URIBuilder builder = URIBuilder.from(baseUri);

        // When
        String path = builder.getPath();

        // Then
        assertEquals("/api/v1/", path);
    }

    @Test
    @DisplayName("Should get scheme from base URL")
    void shouldGetSchemeFromBaseUrl() {
        // Given
        URI baseUri = URI.create("https://localhost:8443/");
        URIBuilder builder = URIBuilder.from(baseUri);

        // When
        String scheme = builder.getScheme();

        // Then
        assertEquals("https", scheme);
    }

    @Test
    @DisplayName("Should get port from base URL")
    void shouldGetPortFromBaseUrl() {
        // Given
        URI baseUri = URI.create("http://localhost:9090/");
        URIBuilder builder = URIBuilder.from(baseUri);

        // When
        int port = builder.getPort();

        // Then
        assertEquals(9090, port);
    }

    @Test
    @DisplayName("Should set path replacing existing path segments")
    void shouldSetPathReplacingExistingSegments() {
        // Given
        URI baseUri = URI.create(BASE_URL);
        URIBuilder builder = URIBuilder.from(baseUri)
                .addPathSegment("existing")
                .addPathSegment("segments");

        // When
        URI result = builder
                .setPath("/new/path")
                .build();

        // Then
        assertEquals(BASE_URL_NO_SLASH + "/new/path", result.toString());
    }

    @Test
    @DisplayName("Should set path and then add additional path segments")
    void shouldSetPathAndThenAddSegments() {
        // Given
        URI baseUri = URI.create(BASE_URL);

        // When
        URI result = URIBuilder.from(baseUri)
                .setPath("/" + API_PATH)
                .addPathSegment(USERS_PATH)
                .build();

        // Then
        assertEquals(BASE_URL_NO_SLASH + "/" + API_PATH + "/" + USERS_PATH, result.toString());
    }

    @Test
    @DisplayName("Should set path with query parameters")
    void shouldSetPathWithQueryParameters() {
        // Given
        URI baseUri = URI.create(BASE_URL);

        // When
        URI result = URIBuilder.from(baseUri)
                .addQueryParameter(PARAM_NAME, VALUE_PARAM)
                .setPath("/" + API_PATH + "/" + USERS_PATH)
                .build();

        // Then
        assertEquals(BASE_URL_NO_SLASH + "/" + API_PATH + "/" + USERS_PATH + "?" + PARAM_NAME + "=" + VALUE_PARAM, result.toString());
    }

    @Test
    @DisplayName("Should handle path segment with only whitespace")
    void shouldHandlePathSegmentWithOnlyWhitespace() {
        // Given
        URI baseUri = URI.create(BASE_URL);

        // When
        URI result = URIBuilder.from(baseUri)
                .addPathSegment("   ")
                .addPathSegment(API_PATH)
                .build();

        // Then
        assertEquals(BASE_URL_NO_SLASH + "/" + API_PATH, result.toString());
    }

    @Test
    @DisplayName("Should handle base URL without trailing slash")
    void shouldHandleBaseUrlWithoutTrailingSlash() {
        // Given
        URI baseUri = URI.create(BASE_URL_NO_SLASH);

        // When
        URI result = URIBuilder.from(baseUri)
                .addPathSegment(API_PATH)
                .build();

        // Then
        assertEquals(BASE_URL_NO_SLASH + "/" + API_PATH, result.toString());
    }

    @Test
    @DisplayName("Should build URI without any path segments or query parameters")
    void shouldBuildUriWithoutPathOrQuery() {
        // Given
        URI baseUri = URI.create(BASE_URL_NO_SLASH);

        // When
        URI result = URIBuilder.from(baseUri).build();

        // Then
        assertEquals(BASE_URL_NO_SLASH, result.toString());
        // Verify no changes were made to the URI
        assertEquals(baseUri.toString(), result.toString());
    }

    @Test
    @DisplayName("Should handle empty query parameters list")
    void shouldHandleEmptyQueryParametersList() {
        // Given
        URI baseUri = URI.create(BASE_URL);

        // When
        URI result = URIBuilder.from(baseUri)
                .addPathSegment(API_PATH)
                // No query parameters added
                .build();

        // Then
        assertEquals(BASE_URL_NO_SLASH + "/" + API_PATH, result.toString());
        // Verify the query string is empty
        assertNull(result.getQuery());
    }

    @Test
    @DisplayName("Should handle path segments with mixed slashes")
    void shouldHandlePathSegmentsWithMixedSlashes() {
        // Given
        URI baseUri = URI.create(BASE_URL);

        // When
        URI result = URIBuilder.from(baseUri)
                .addPathSegment(API_PATH + "/")
                .addPathSegment("/" + USERS_PATH)
                .build();

        // Then
        assertEquals(BASE_URL_NO_SLASH + "/" + API_PATH + "/" + USERS_PATH, result.toString());
    }

    @Test
    @DisplayName("Should handle path segments with only slashes")
    void shouldHandlePathSegmentsWithOnlySlashes() {
        // Given
        URI baseUri = URI.create(BASE_URL);

        // When
        URI result = URIBuilder.from(baseUri)
                .addPathSegment("/")
                .addPathSegment("///")
                .addPathSegment(API_PATH)
                .build();

        // Then
        assertEquals(BASE_URL_NO_SLASH + "/" + API_PATH, result.toString());
    }

    private static final String BASE_URL_WITH_BASE = "http://localhost:8080/base/";
    private static final String BASE_URL_WITH_BASE_NO_SLASH = "http://localhost:8080/base";

    @Test
    @DisplayName("Should handle base URL with path and trailing slash when adding segments")
    void shouldHandleBaseUrlWithPathAndTrailingSlashWhenAddingSegments() {
        // Given
        URI baseUri = URI.create(BASE_URL_WITH_BASE);

        // When
        URI result = URIBuilder.from(baseUri)
                .addPathSegment(API_PATH)
                .addPathSegment(USERS_PATH)
                .build();

        // Then
        assertEquals(BASE_URL_WITH_BASE_NO_SLASH + "/" + API_PATH + "/" + USERS_PATH, result.toString());
    }

    @Test
    @DisplayName("Should handle empty path segments array")
    void shouldHandleEmptyPathSegmentsArray() {
        // Given
        URI baseUri = URI.create(BASE_URL);

        // When
        URI result = URIBuilder.from(baseUri)
                .addPathSegments()
                .build();

        // Then
        assertEquals(BASE_URL_NO_SLASH, result.toString());
    }

    @Test
    @DisplayName("Should handle base URL without trailing slash and empty path segments")
    void shouldHandleBaseUrlWithoutTrailingSlashAndEmptyPathSegments() {
        // Given
        URI baseUri = URI.create(BASE_URL_NO_SLASH);

        // When
        URI result = URIBuilder.from(baseUri)
                // No path segments added
                .addPathSegments()
                .build();

        // Then
        assertEquals(BASE_URL_NO_SLASH, result.toString());
        // Verify the path is empty
        assertEquals("", baseUri.getPath());
    }

    @Test
    @DisplayName("Should handle base URL with trailing slash and empty path segments")
    void shouldHandleBaseUrlWithTrailingSlashAndEmptyPathSegments() {
        // Given
        URI baseUri = URI.create(BASE_URL);

        // When
        URI result = URIBuilder.from(baseUri)
                // No path segments added
                .build();

        // Then
        assertEquals(BASE_URL_NO_SLASH, result.toString());
        // Verify the path contains a slash
        assertEquals("/", baseUri.getPath());
    }

    @Test
    @DisplayName("Should handle multiple query parameters with same name and different values")
    void shouldHandleMultipleQueryParametersWithSameNameAndDifferentValues() {
        // Given
        URI baseUri = URI.create(BASE_URL);

        // When
        URI result = URIBuilder.from(baseUri)
                .addQueryParameter(PARAM_NAME, VALUE1_PARAM)
                .addQueryParameter(PARAM_NAME, VALUE2_PARAM)
                .addQueryParameter(PARAM_NAME, VALUE3_PARAM)
                .build();

        // Then
        assertEquals(BASE_URL_NO_SLASH + "?" + PARAM_NAME + "=" + VALUE1_PARAM + "&" + PARAM_NAME + "=" + VALUE2_PARAM + "&" + PARAM_NAME + "=" + VALUE3_PARAM, result.toString());
    }

    private static final String ENCODED_SPACES = "users%20with%20spaces";

    private static final String FILTER_PARAM = "filter";
    private static final String ENCODED_NAME_SPACES = "name%20with%20spaces";

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
    void testSpecialCharacterHandling(String testName, String baseUrlString, UnaryOperator<URIBuilder> setup, String expectedResult) {
        testUriBuilding(baseUrlString, setup, expectedResult);
    }

    private static final String COMPLEX_BASE_URL = "https://example.com:8443/context";
    private static final String API_V1_PATH = "api/v1";
    private static final String RESOURCES_PATH = "resources";
    private static final String PAGE_PARAM = "page";
    private static final String SIZE_PARAM = "size";
    private static final String SORT_PARAM = "sort";

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
    void testComplexUrlBuilding(String testName, String baseUrlString, UnaryOperator<URIBuilder> setup, String expectedResult) {
        testUriBuilding(baseUrlString, setup, expectedResult);
    }

    /**
     * Generic test method for URI building tests to avoid duplicate code.
     * 
     * @param baseUrlString the base URL string to use
     * @param setup the setup function to configure the builder
     * @param expectedResult the expected URI string result
     */
    private void testUriBuilding(String baseUrlString, UnaryOperator<URIBuilder> setup, String expectedResult) {
        // Given
        URI baseUri = URI.create(baseUrlString);
        URIBuilder builder = URIBuilder.from(baseUri);

        // When
        URI result = setup.apply(builder).build();

        // Then
        assertEquals(expectedResult, result.toString());
    }
}
