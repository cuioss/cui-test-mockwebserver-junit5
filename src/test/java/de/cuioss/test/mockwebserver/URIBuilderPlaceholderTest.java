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

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for placeholder functionality of {@link URIBuilder}.
 */
class URIBuilderPlaceholderTest extends URIBuilderTestBase {

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

    @Test
    @DisplayName("Should throw IllegalStateException when building URI from placeholder")
    void shouldThrowExceptionWhenBuildingUriFromPlaceholder() {
        // Given: A placeholder URIBuilder
        URIBuilder placeholder = URIBuilder.placeholder();

        // When/Then: the message identifies the placeholder problem; the exact wording is not pinned
        IllegalStateException exception = assertThrows(IllegalStateException.class, placeholder::build);
        assertTrue(exception.getMessage().contains("Cannot build URI from placeholder URIBuilder"),
                "Message should identify the placeholder problem");
    }

    @Test
    @DisplayName("Should throw IllegalStateException when building string from placeholder")
    void shouldThrowExceptionWhenBuildingStringFromPlaceholder() {
        // Given: A placeholder URIBuilder
        URIBuilder placeholder = URIBuilder.placeholder();

        // When/Then: the message identifies the placeholder problem; the exact wording is not pinned
        IllegalStateException exception = assertThrows(IllegalStateException.class, placeholder::buildAsString);
        assertTrue(exception.getMessage().contains("Cannot build URI from placeholder URIBuilder"),
                "Message should identify the placeholder problem");
    }

    @Test
    @DisplayName("Should allow adding path segments to placeholder without error")
    void shouldAllowAddingPathSegmentsToPlaceholder() {
        // Given: A placeholder URIBuilder
        URIBuilder placeholder = URIBuilder.placeholder();

        // When: Adding path segments
        placeholder.addPathSegment(API_PATH);
        placeholder.addPathSegments(USERS_PATH, ID_123);

        // Then: Verify path segments were added correctly
        assertEquals(3, placeholder.getPathSegments().size());
        List<String> pathSegments = placeholder.getPathSegments();
        assertTrue(pathSegments.contains(API_PATH));
        assertTrue(pathSegments.contains(USERS_PATH));
        assertTrue(pathSegments.contains(ID_123));
        assertEquals(API_PATH, pathSegments.getFirst());
        assertEquals(USERS_PATH, pathSegments.get(1));
        assertEquals(ID_123, pathSegments.get(2));
    }

    @Test
    @DisplayName("Should allow adding query parameters to placeholder without error")
    void shouldAllowAddingQueryParametersToPlaceholder() {
        // Given: A placeholder URIBuilder
        URIBuilder placeholder = URIBuilder.placeholder();

        // When: Adding a query parameter
        placeholder.addQueryParameter(NAME_PARAM, VALUE_PARAM);

        // Then: Verify query parameter was added correctly
        assertEquals(1, placeholder.getQueryParameters().size());
        assertTrue(placeholder.getQueryParameters().containsKey(NAME_PARAM));
        List<String> values = placeholder.getQueryParameters().get(NAME_PARAM);
        assertNotNull(values);
        assertEquals(1, values.size());
        assertEquals(VALUE_PARAM, values.getFirst());
    }
}
