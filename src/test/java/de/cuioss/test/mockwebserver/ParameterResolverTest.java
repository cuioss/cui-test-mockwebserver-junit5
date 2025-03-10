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

import mockwebserver3.MockWebServer;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests the ParameterResolver functionality of {@link MockWebServerExtension}.
 */
// tag::parameter-resolver-example[]
@EnableMockWebServer
class ParameterResolverTest {

    @Test
    @DisplayName("Should inject MockWebServer instance")
    void shouldInjectMockWebServer(MockWebServer server) {
        assertNotNull(server);
        assertTrue(server.getStarted());
    }

    // Tests for port parameter resolver removed as it's redundant with URIBuilder

    @Test
    @DisplayName("Should inject URIBuilder")
    void shouldInjectUriBuilder(URIBuilder uriBuilder) {
        assertNotNull(uriBuilder);
        assertEquals("http", uriBuilder.build().getScheme());
        assertEquals("/", uriBuilder.getPath());
    }

    // Test for String parameter resolver removed as it's redundant with URIBuilder

    @Test
    @DisplayName("Should inject multiple parameters")
    void shouldInjectMultipleParameters(MockWebServer server, URIBuilder uriBuilder) {
        assertNotNull(server);
        assertTrue(server.getPort() > 0);
        assertEquals(server.getPort(), uriBuilder.getPort());
    }
}
// end::parameter-resolver-example[]
