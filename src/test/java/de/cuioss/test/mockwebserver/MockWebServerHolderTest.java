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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNull;

@DisplayName("Tests for MockWebServerHolder default implementations")
class MockWebServerHolderTest {

    private MockWebServerHolder holder;

    @BeforeEach
    void setUp() {
        // Create a minimal implementation of the interface
        holder = new MockWebServerHolder() {
        };
    }

    @Nested
    @DisplayName("Default implementation tests")
    class DefaultImplementationTests {

        /**
         * Tests the default implementation of getMockWebServer() which is deprecated but still needs to be tested
         * until it's removed in version 1.2
         */
        @Test
        @DisplayName("Should return null for getMockWebServer")
        @SuppressWarnings("deprecation") // Testing deprecated method
        void shouldReturnNullForGetMockWebServer() {
            // tag::test-get-mockwebserver[]
            // Test the default implementation of getMockWebServer()
            assertNull(holder.getMockWebServer(), "Default implementation should return null");
            // end::test-get-mockwebserver[]
        }

        /**
         * Tests the default implementation of setMockWebServer() which is deprecated but still needs to be tested
         * until it's removed in version 1.2
         */
        @Test
        @DisplayName("Should do nothing for setMockWebServer")
        @SuppressWarnings("deprecation") // Testing deprecated method
        void shouldDoNothingForSetMockWebServer() {
            // tag::test-set-mockwebserver[]
            // Test the default implementation of setMockWebServer()
            try (MockWebServer server = new MockWebServer()) {
                assertDoesNotThrow(() -> holder.setMockWebServer(server),
                        "Default implementation should not throw an exception");
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            // end::test-set-mockwebserver[]
        }

        /**
         * Tests the default implementation of getDispatcher() which is deprecated but still needs to be tested
         * until it's removed in version 1.2
         */
        @Test
        @DisplayName("Should return null for getDispatcher")
        @SuppressWarnings("deprecation") // Testing deprecated method
        void shouldReturnNullForGetDispatcher() {
            // tag::test-get-dispatcher[]
            // Test the default implementation of getDispatcher()
            assertNull(holder.getDispatcher(), "Default implementation should return null");
            // end::test-get-dispatcher[]
        }
    }


}