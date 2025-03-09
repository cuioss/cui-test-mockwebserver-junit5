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

/**
 * Tests for special character handling functionality of {@link URIBuilder}.
 */
class URIBuilderSpecialCharacterTest extends URIBuilderTestBase {

    // tag::special-character-handling[]
    @Test
    @DisplayName("Should handle path segments with encoded special characters")
    void shouldHandlePathSegmentsWithEncodedSpecialCharacters() {
        // Use the utility method from the base class to test URI building with special characters
        assertUriBuilding(BASE_URL, 
                builder -> builder.addPathSegment(API_PATH)
                                 .addPathSegment(ENCODED_SPACES),
                BASE_URL_NO_SLASH + "/" + API_PATH + "/" + ENCODED_SPACES);
    }

    @Test
    @DisplayName("Should handle query parameters with encoded special characters")
    void shouldHandleQueryParametersWithEncodedSpecialCharacters() {
        // Use the utility method from the base class to test URI building with special characters
        assertUriBuilding(BASE_URL,
                builder -> builder.addPathSegment(API_PATH)
                                 .addQueryParameter(FILTER_PARAM, ENCODED_NAME_SPACES),
                BASE_URL_NO_SLASH + "/" + API_PATH + "?" + FILTER_PARAM + "=" + ENCODED_NAME_SPACES);
    }
    // end::special-character-handling[]

    @Test
    @DisplayName("Should handle complex URL with port, path and query parameters")
    void shouldHandleComplexUrlWithPortPathAndQueryParameters() {
        // Use the utility method from the base class to test complex URL building
        assertUriBuilding(COMPLEX_BASE_URL,
                builder -> builder.addPathSegment(API_V1_PATH)
                                 .addPathSegment(RESOURCES_PATH)
                                 .addQueryParameter(PAGE_PARAM, "1")
                                 .addQueryParameter(SIZE_PARAM, "10")
                                 .addQueryParameter(SORT_PARAM, "name,asc"),
                COMPLEX_BASE_URL + "/" + API_V1_PATH + "/" + RESOURCES_PATH + "?" +
                        PAGE_PARAM + "=1&" + SIZE_PARAM + "=10&" + SORT_PARAM + "=name,asc");
    }
}
