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

import mockwebserver3.MockWebServer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.net.URI;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for parameter resolution functionality in {@link MockWebServerExtension}.
 */
@EnableMockWebServer
class MockWebServerParameterResolutionTest {

    @Test
    @DisplayName("Should resolve MockWebServer parameter")
    void shouldResolveMockWebServerParameter(MockWebServer server) {
        assertNotNull(server, "MockWebServer parameter should be resolved");
        assertTrue(server.getStarted(), "Server should be started");
    }

    @Test
    @DisplayName("Should resolve URIBuilder parameter")
    void shouldResolveUriBuilderParameter(URIBuilder uriBuilder) {
        assertNotNull(uriBuilder, "URIBuilder parameter should be resolved");

        // Verify the builder is correctly configured with server URL
        URI uri = uriBuilder.build();
        assertNotNull(uri, "Built URI should not be null");
        assertEquals("http", uri.getScheme(), "URI scheme should be HTTP");
        assertTrue(uri.getPort() > 0, "URI should have a valid port");
    }

    @Test
    @DisplayName("Should resolve multiple parameters in same method")
    void shouldResolveMultipleParameters(MockWebServer server, URIBuilder uriBuilder) {
        assertNotNull(server, "MockWebServer parameter should be resolved");
        assertNotNull(uriBuilder, "URIBuilder parameter should be resolved");

        // Verify they work together
        URI uri = uriBuilder.build();
        assertEquals("localhost", uri.getHost(), "URI host should be localhost");
        assertEquals(server.getPort(), uri.getPort(), "URI port should match server port");
    }
}
