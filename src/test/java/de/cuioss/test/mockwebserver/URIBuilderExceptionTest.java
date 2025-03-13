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

import java.net.URI;
import java.net.URISyntaxException;

/**
 * Tests for exception handling functionality of {@link URIBuilder}.
 */
@SuppressWarnings({"ConstantValue", "DataFlowIssue"})
class URIBuilderExceptionTest extends URIBuilderTestBase {

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

    @Test
    @DisplayName("Should throw Exception when base URL is Invalid")
    void shouldThrowExceptionWhenURIisInvalid() throws URISyntaxException {
        // Given: A null URI
        URI invalidURL = new URI("invalid://example.com");

        // When/Then: Use the utility method from the base class to test exception handling
        assertThrowsWithMessage(
                IllegalArgumentException.class,
                () -> URIBuilder.from(invalidURL),
                "Could not convert URI to URL: invalid://example.com");
    }

    @Test
    @DisplayName("Should throw IllegalStateException when baseUrl is null in non-placeholder URIBuilder")
    void shouldThrowExceptionWhenBuildingWithNullBaseUrl() throws NoSuchFieldException, IllegalAccessException {
        // Given: Create a URIBuilder and set its baseUrl field to null using reflection
        URIBuilder builder = URIBuilder.from(URI.create(BASE_URL));
        setBaseUrlToNull(builder);

        // When/Then: Use the utility method from the base class to test exception handling
        assertThrowsWithMessage(
                IllegalStateException.class,
                builder::build,
                "Cannot build URI with null baseUrl. This might indicate an incorrectly initialized URIBuilder.");
    }

    @Test
    @DisplayName("Should handle null baseUrl in getPath()")
    void shouldThrowExceptionWhenGettingPathWithNullBaseUrl() throws NoSuchFieldException, IllegalAccessException {
        // Given: Create a URIBuilder and set its baseUrl field to null using reflection
        URIBuilder builder = URIBuilder.from(URI.create(BASE_URL));
        setBaseUrlToNull(builder);

        // When/Then: Use the utility method from the base class to test exception handling
        assertThrowsWithMessage(
                IllegalStateException.class,
                builder::getPath,
                "Cannot access path with null baseUrl. This might indicate an incorrectly initialized URIBuilder.");
    }

    @Test
    @DisplayName("Should handle null baseUrl in getScheme()")
    void shouldThrowExceptionWhenGettingSchemeWithNullBaseUrl() throws NoSuchFieldException, IllegalAccessException {
        // Given: Create a URIBuilder and set its baseUrl field to null using reflection
        URIBuilder builder = URIBuilder.from(URI.create(BASE_URL));
        setBaseUrlToNull(builder);

        // When/Then: Use the utility method from the base class to test exception handling
        assertThrowsWithMessage(
                IllegalStateException.class,
                builder::getScheme,
                "Cannot access scheme with null baseUrl. This might indicate an incorrectly initialized URIBuilder.");
    }

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
