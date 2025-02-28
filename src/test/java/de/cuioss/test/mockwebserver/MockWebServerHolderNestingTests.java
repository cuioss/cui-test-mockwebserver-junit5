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

import de.cuioss.test.juli.TestLogLevel;
import de.cuioss.test.juli.junit5.EnableTestLogger;
import lombok.Getter;
import lombok.Setter;
import mockwebserver3.MockWebServer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

@EnableMockWebServer
@EnableTestLogger(debug = MockWebServerExtension.class, rootLevel = TestLogLevel.DEBUG)
class MockWebServerHolderNestingTests implements MockWebServerHolder {

    @Getter
    @Setter
    private MockWebServer mockWebServer;


    @Test
    void shouldProvideServerOnParent() {
        assertNotNull(mockWebServer);
    }

    @Nested
    @DisplayName("Nested Tests With MockWebServer")
    class NestedTestsWithMockWebServer {

        @Test
        void shouldHaveAccessToParentMockWebServer() {
            assertNull(mockWebServer, "Parent server is null. This is expected.");
        }
    }

    @Nested
    @EnableMockWebServer
    @DisplayName("Nested Tests With MockWebServer and a local annotation")
    class NestedTestsWithMockWebServerAndAnnotation {

        @Test
        void shouldHaveAccessToParentMockWebServer() {
            assertNull(mockWebServer, "Parent server is null. This is expected.");
        }
    }

    @Nested
    @EnableMockWebServer
    @DisplayName("Nested Tests With MockWebServer and a local annotation and acting as a MockWebServerHolder")
    class NestedTestsWithMockWebServerAndAnnotationAndHolder implements MockWebServerHolder {

        @Getter
        @Setter
        private MockWebServer mockWebServer;

        @Test
        void shouldHaveAccessToParentMockWebServer() {
            assertNotNull(this.mockWebServer, "Child server should not be null");
        }

        @Nested
        @DisplayName("Third level nested tests")
        class ThirdLevelNestedTests {

            @Test
            void shouldHaveAccessToParentMockWebServer() {
                assertNull(mockWebServer, "Parent server is null. This is expected.");
            }
        }
    }
}
