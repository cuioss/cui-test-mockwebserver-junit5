/**
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

import de.cuioss.test.mockwebserver.dispatcher.CombinedDispatcher;
import de.cuioss.test.mockwebserver.dispatcher.ModuleDispatcher;
import de.cuioss.tools.logging.CuiLogger;
import mockwebserver3.MockWebServer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.net.URI;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for inheritance behavior with the {@link MockWebServerExtension}.
 */
@EnableMockWebServer
@ModuleDispatcher(provider = CombinedDispatcher.class, providerMethod = "createAPIDispatcher")
class MockWebServerInheritanceTest {

    private static final CuiLogger LOGGER = new CuiLogger(MockWebServerInheritanceTest.class);

    // tag::inheritance-test[]
    @Test
    @DisplayName("Parent test should have access to server")
    void parentShouldHaveServer(MockWebServer server, URIBuilder uriBuilder) {
        assertNotNull(server, "Parent should have access to server");
        assertTrue(server.getStarted(), "Server should be started");

        // Verify the server is accessible via parameter injection
        assertNotNull(uriBuilder, "URIBuilder should be injected");
        URI uri = uriBuilder.addPathSegment("api").build();
        assertEquals("localhost", uri.getHost(), "URI host should be localhost");
        assertEquals(server.getPort(), uri.getPort(), "URI port should match server port");
    }

    /**
     * Nested test class to verify that parameter injection works correctly in nested tests.
     */
    @Nested
    class NestedTest {

        @Test
        @DisplayName("Nested test should receive server via parameter injection")
        void nestedShouldReceiveServerViaParameterInjection(MockWebServer nestedServer, URIBuilder uriBuilder) {
            // Nested tests can get the server via parameter injection
            assertNotNull(nestedServer, "Nested test should get server via parameter injection");
            assertTrue(nestedServer.getStarted(), "Server should be started");

            // Verify the URIBuilder is also injected and properly configured
            assertNotNull(uriBuilder, "URIBuilder should be injected");
            URI uri = uriBuilder.addPathSegment("api").build();
            assertEquals("localhost", uri.getHost(), "URI host should be localhost");
            assertEquals(nestedServer.getPort(), uri.getPort(), "URI port should match server port");

            LOGGER.info("Nested test received server via parameter injection with port: " + nestedServer.getPort());
        }
    }
    // end::inheritance-test[]
}
