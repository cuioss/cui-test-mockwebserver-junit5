/*
 * Copyright © 2025-present CUI-OpenSource-Software (info@cuioss.de)
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

import java.net.URI;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests for percent-encoding and complex URL handling in {@link URIBuilder}.
 */
class URIBuilderEncodingTest extends URIBuilderTestBase {

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

    @Test
    @DisplayName("Should percent-encode a space in a query value instead of throwing")
    void shouldEncodeSpaceInQueryValue() {
        URI uri = URIBuilder.from(URI.create(BASE_URL))
                .addQueryParameter("q", "a b")
                .build();
        assertEquals(BASE_URL_NO_SLASH + "?q=a%20b", uri.toString());
    }

    @Test
    @DisplayName("Should percent-encode reserved characters in a query value")
    void shouldEncodeReservedCharactersInQueryValue() {
        URI uri = URIBuilder.from(URI.create(BASE_URL))
                .addQueryParameter("q", "a&b=c#d")
                .build();
        assertEquals(BASE_URL_NO_SLASH + "?q=a%26b%3Dc%23d", uri.toString());
    }

    @Test
    @DisplayName("Should percent-encode reserved characters in a query name")
    void shouldEncodeReservedCharactersInQueryName() {
        URI uri = URIBuilder.from(URI.create(BASE_URL))
                .addQueryParameter("a&b", "v")
                .build();
        assertEquals(BASE_URL_NO_SLASH + "?a%26b=v", uri.toString());
    }

    @Test
    @DisplayName("Should percent-encode a space in a path segment")
    void shouldEncodeSpaceInPathSegment() {
        URI uri = URIBuilder.from(URI.create(BASE_URL))
                .addPathSegment("a b")
                .build();
        assertEquals(BASE_URL_NO_SLASH + "/a%20b", uri.toString());
    }

    @Test
    @DisplayName("Should preserve sub-delimiters that are legal in a query value")
    void shouldPreserveLegalSubDelimitersInQueryValue() {
        // A comma is a valid query character and need not be percent-encoded
        URI uri = URIBuilder.from(URI.create(BASE_URL))
                .addQueryParameter(SORT_PARAM, "name,asc")
                .build();
        assertEquals(BASE_URL_NO_SLASH + "?" + SORT_PARAM + "=name,asc", uri.toString());
    }
}
