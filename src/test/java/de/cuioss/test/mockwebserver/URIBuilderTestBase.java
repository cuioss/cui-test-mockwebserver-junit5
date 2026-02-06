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

import org.junit.jupiter.api.function.Executable;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.UnaryOperator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Base class for URIBuilder tests with common utilities and constants.
 * Provides helper methods to simplify test cases and reduce duplication.
 */
abstract class URIBuilderTestBase {

    // Constants for commonly used string literals
    protected static final String BASE_URL = "http://localhost:8080/";
    protected static final String BASE_URL_NO_SLASH = "http://localhost:8080";
    protected static final String API_PATH = "api";
    protected static final String USERS_PATH = "users";
    protected static final String ID_123 = "123";
    protected static final String NAME_PARAM = "name";
    protected static final String VALUE_PARAM = "value";
    protected static final String VALUE1_PARAM = "value1";
    protected static final String VALUE2_PARAM = "value2";
    protected static final String VALUE3_PARAM = "value3";
    protected static final String PARAM_NAME = "param";
    protected static final String BASE_URL_WITH_API = BASE_URL_NO_SLASH + "/" + API_PATH;
    protected static final String BASE_URL_WITH_API_USERS_123 = BASE_URL_WITH_API + "/" + USERS_PATH + "/" + ID_123;
    protected static final String BASE_URL_FIELD = "baseUrl";
    protected static final String BASE_URL_WITH_BASE = "http://localhost:8080/base/";
    protected static final String BASE_URL_WITH_BASE_NO_SLASH = "http://localhost:8080/base";
    protected static final String ENCODED_SPACES = "users%20with%20spaces";
    protected static final String FILTER_PARAM = "filter";
    protected static final String ENCODED_NAME_SPACES = "name%20with%20spaces";
    protected static final String COMPLEX_BASE_URL = "https://example.com:8443/context";
    protected static final String API_V1_PATH = "api/v1";
    protected static final String RESOURCES_PATH = "resources";
    protected static final String PAGE_PARAM = "page";
    protected static final String SIZE_PARAM = "size";
    protected static final String SORT_PARAM = "sort";

    /**
     * Helper method to set the baseUrl field to null using reflection.
     * This method is only used for testing exception handling when baseUrl is null.
     * 
     * @param target the URIBuilder instance to modify
     * @throws NoSuchFieldException   if the field does not exist
     * @throws IllegalAccessException if the field cannot be accessed
     */
    @SuppressWarnings("java:S3011") // Suppressing warning about accessibility as this is necessary for testing
    protected void setBaseUrlToNull(Object target)
            throws NoSuchFieldException, IllegalAccessException {
        java.lang.reflect.Field field = target.getClass().getDeclaredField(BASE_URL_FIELD);
        // Using setAccessible is necessary for testing in this specific case
        // as we need to simulate a null baseUrl which can't happen through normal API usage
        field.setAccessible(true);
        try {
            field.set(target, null);
        } finally {
            field.setAccessible(false);
        }
    }

    /**
     * Parses a query string into a map of parameter names to values.
     * This helper method is used to compare query parameters in a way that
     * doesn't depend on their order in the URI string.
     * 
     * @param query the query string to parse
     * @return a map of parameter names to their values
     */
    protected Map<String, List<String>> parseQueryParams(String query) {
        if (query == null || query.isEmpty()) {
            return Map.of();
        }

        Map<String, List<String>> result = new HashMap<>();
        String[] pairs = query.split("&");

        for (String pair : pairs) {
            String[] keyValue = pair.split("=", 2); // Limit to 2 parts to handle values containing '='
            String key = keyValue[0];
            String value = keyValue.length > 1 ? keyValue[1] : "";

            result.computeIfAbsent(key, k -> new ArrayList<>()).add(value);
        }

        return result;
    }

    /**
     * Utility method for testing URI building with path segments and query parameters.
     * This method verifies the built URI against the expected result in a way that
     * is not dependent on query parameter order.
     *
     * @param baseUrlString The base URL to use
     * @param setup The setup function that configures the builder
     * @param expectedResult The expected URI string result
     */
    protected void assertUriBuilding(String baseUrlString, UnaryOperator<URIBuilder> setup, String expectedResult) {
        // Given: Create a URIBuilder with the specified base URL
        URI baseUri = URI.create(baseUrlString);
        URIBuilder builder = URIBuilder.from(baseUri);

        // When: Apply the setup function and build the URI
        URI result = setup.apply(builder).build();

        // Then: Verify components separately since query parameter order may vary
        URI expectedUri = URI.create(expectedResult);

        // Verify scheme, host, port, and path are the same
        assertEquals(expectedUri.getScheme(), result.getScheme(), "URI scheme doesn't match");
        assertEquals(expectedUri.getHost(), result.getHost(), "URI host doesn't match");
        assertEquals(expectedUri.getPort(), result.getPort(), "URI port doesn't match");
        assertEquals(expectedUri.getPath(), result.getPath(), "URI path doesn't match");

        // Verify query parameters (order-independent)
        Map<String, List<String>> expectedParams = parseQueryParams(expectedUri.getQuery());
        Map<String, List<String>> actualParams = parseQueryParams(result.getQuery());
        assertEquals(expectedParams, actualParams, "Query parameters don't match");
    }

    /**
     * Utility method for testing URI building with path segments only (no query parameters).
     * This method provides a simpler verification when query parameters are not involved.
     *
     * @param baseUrlString The base URL to use
     * @param setup The setup function that configures the builder
     * @param expectedResult The expected URI string result
     */
    protected void assertUriPathBuilding(String baseUrlString, UnaryOperator<URIBuilder> setup, String expectedResult) {
        // Given: Create a URIBuilder with the specified base URL
        URI baseUri = URI.create(baseUrlString);
        URIBuilder builder = URIBuilder.from(baseUri);

        // When: Apply the setup function and build the URI
        URI result = setup.apply(builder).build();

        // Then: Verify the result matches the expected URI string
        assertEquals(expectedResult, result.toString(), "URI doesn't match expected result");
    }

    /**
     * Utility method for testing exception cases in URIBuilder.
     * This method verifies that the expected exception is thrown with the correct message.
     *
     * @param <T> The type of exception expected
     * @param exceptionClass The class of the exception expected
     * @param operation The operation that should throw the exception
     * @param expectedMessage The expected exception message
     */
    protected <T extends Throwable> void assertThrowsWithMessage(
            Class<T> exceptionClass, Executable operation, String expectedMessage) {
        T exception = assertThrows(exceptionClass, operation, "Expected " + exceptionClass.getSimpleName() + " was not thrown");
        assertEquals(expectedMessage, exception.getMessage(), "Exception message doesn't match expected message");
    }

}
