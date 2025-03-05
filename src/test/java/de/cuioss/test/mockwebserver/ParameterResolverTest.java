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

import java.net.URL;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests the ParameterResolver functionality of {@link MockWebServerExtension}.
 */
@EnableMockWebServer
class ParameterResolverTest {

    @Test
    @DisplayName("Should inject MockWebServer instance")
    void shouldInjectMockWebServer(MockWebServer server) {
        assertNotNull(server);
        assertTrue(server.getStarted());
    }

    @Test
    @DisplayName("Should inject server port as int")
    void shouldInjectServerPort(int port) {
        assertTrue(port > 0);
    }

    @Test
    @DisplayName("Should inject server port as Integer")
    void shouldInjectServerPortAsInteger(Integer port) {
        assertTrue(port > 0);
    }

    @Test
    @DisplayName("Should inject server URL")
    void shouldInjectServerUrl(URL url) {
        assertNotNull(url);
        assertEquals("http", url.getProtocol());
        assertEquals("/", url.getPath());
    }

    @Test
    @DisplayName("Should inject server URL as String")
    void shouldInjectServerUrlAsString(String url) {
        assertNotNull(url);
        assertTrue(url.startsWith("http://"));
        assertTrue(url.endsWith("/"));
    }

    @Test
    @DisplayName("Should inject multiple parameters")
    void shouldInjectMultipleParameters(MockWebServer server, int port, URL url) {
        assertNotNull(server);
        assertTrue(port > 0);
        assertEquals(port, server.getPort());
        assertEquals(port, url.getPort());
    }
}
