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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;


import mockwebserver3.MockWebServer;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the default implementations in {@link MockWebServerHolder}.
 */
class MockWebServerHolderTest {

    private MockWebServerHolder holder;

    @BeforeEach
    void setUp() {
        // Create a minimal implementation of the interface
        holder = new MockWebServerHolder() {
        };
    }

    @Test
    void shouldReturnNullForGetMockWebServer() {
        // tag::test-get-mockwebserver[]
        // Test the default implementation of getMockWebServer()
        assertNull(holder.getMockWebServer(), "Default implementation should return null");
        // end::test-get-mockwebserver[]
    }

    @Test
    void shouldDoNothingForSetMockWebServer() {
        // tag::test-set-mockwebserver[]
        // Test the default implementation of setMockWebServer()
        MockWebServer server = new MockWebServer();
        assertDoesNotThrow(() -> holder.setMockWebServer(server),
                "Default implementation should not throw an exception");
        // end::test-set-mockwebserver[]
    }

    @Test
    void shouldDoNothingForReceiveHandshakeCertificates() {
        // tag::test-receive-certificates[]
        // Test the default implementation of receiveHandshakeCertificates()
        assertDoesNotThrow(() -> holder.receiveHandshakeCertificates(null),
                "Default implementation should not throw an exception even with null input");
        // end::test-receive-certificates[]
    }

    @Test
    void shouldReturnNullForGetDispatcher() {
        // tag::test-get-dispatcher[]
        // Test the default implementation of getDispatcher()
        assertNull(holder.getDispatcher(), "Default implementation should return null");
        // end::test-get-dispatcher[]
    }

    @Test
    void shouldReturnEmptyOptionalForGetTestProvidedHandshakeCertificates() {
        // tag::test-get-certificates[]
        // Test the default implementation of getTestProvidedHandshakeCertificates()
        var certificates = holder.getTestProvidedHandshakeCertificates();
        assertFalse(certificates.isPresent(), "Default implementation should return an empty Optional");
        // end::test-get-certificates[]
    }
}