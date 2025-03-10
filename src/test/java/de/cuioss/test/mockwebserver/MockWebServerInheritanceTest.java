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

import de.cuioss.test.mockwebserver.dispatcher.CombinedDispatcher;
import de.cuioss.tools.logging.CuiLogger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.net.URI;

import mockwebserver3.Dispatcher;
import mockwebserver3.MockWebServer;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for inheritance behavior with the {@link MockWebServerExtension}.
 */
@EnableMockWebServer
class MockWebServerInheritanceTest implements MockWebServerHolder {

    private static final CuiLogger LOGGER = new CuiLogger(MockWebServerInheritanceTest.class);

    @Override
    public Dispatcher getDispatcher() {
        return CombinedDispatcher.createAPIDispatcher();
    }

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
     * Nested test class to verify that the MockWebServerHolder interface is not inherited.
     */
    @Nested
    class NestedTest {

        @Test
        @DisplayName("Nested test should not inherit MockWebServerHolder implementation")
        void nestedShouldNotInheritHolder(MockWebServer nestedServer, URIBuilder uriBuilder) {
            // The nested class doesn't implement MockWebServerHolder
            // But it can still get the server via parameter injection
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
