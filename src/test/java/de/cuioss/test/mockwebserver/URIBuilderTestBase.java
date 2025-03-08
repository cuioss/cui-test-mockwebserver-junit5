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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Base class for URIBuilder tests with common utilities and constants.
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
     * This method properly handles the accessibility concerns and is only used
     * for testing exception handling when baseUrl is null.
     *
     * @param target the URIBuilder instance to modify
     * @throws NoSuchFieldException   if the field does not exist
     * @throws IllegalAccessException if the field cannot be accessed
     */
    protected void setBaseUrlToNull(Object target)
            throws NoSuchFieldException, IllegalAccessException {
        java.lang.reflect.Field field = target.getClass().getDeclaredField(BASE_URL_FIELD);
        boolean wasAccessible = field.canAccess(target);
        if (!wasAccessible) {
            field.setAccessible(true);
        }
        try {
            field.set(target, null);
        } finally {
            if (!wasAccessible) {
                field.setAccessible(false);
            }
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

}
