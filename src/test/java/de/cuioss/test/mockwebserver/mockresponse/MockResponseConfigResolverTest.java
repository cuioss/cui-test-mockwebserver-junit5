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
package de.cuioss.test.mockwebserver.mockresponse;

import de.cuioss.test.mockwebserver.dispatcher.ModuleDispatcherElement;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.List;

import static de.cuioss.test.mockwebserver.mockresponse.MockResponseTestUtil.DEFAULT_PATH;
import static de.cuioss.test.mockwebserver.mockresponse.MockResponseTestUtil.DEFAULT_STATUS;
import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Tests for MockResponseConfigResolver")
class MockResponseConfigResolverTest {

    // Using constants from MockResponseTestUtil where possible
    private static final String TEST_CONTENT = "Test content";
    private static final String SHOULD_RESOLVE_TWO_DISPATCHER_ELEMENTS = "Should resolve two dispatcher elements";

    @Nested
    @DisplayName("Class annotation resolution tests")
    class ClassAnnotationTests {

        @Test
        @DisplayName("Should resolve MockResponseConfig annotation on class")
        void shouldResolveClassAnnotation() {
            // Arrange
            Class<?> testClass = ClassWithMockResponse.class;

            // Act
            List<ModuleDispatcherElement> elements = MockResponseConfigResolver.resolveFromAnnotations(testClass);

            // Assert
            assertEquals(1, elements.size(), "Should resolve one dispatcher element");
            ModuleDispatcherElement element = elements.getFirst();
            assertInstanceOf(MockResponseConfigDispatcherElement.class, element, "Element should be a MockResponseConfigDispatcherElement");
            assertEquals(DEFAULT_PATH, element.getBaseUrl(),
                    "Element should have correct path");
        }

        @Test
        @DisplayName("Should resolve multiple MockResponseConfig annotations on class")
        void shouldResolveMultipleMockResponseAnnotations() {
            // Arrange
            Class<?> testClass = ClassWithMultipleMockResponses.class;

            // Act
            List<ModuleDispatcherElement> elements = MockResponseConfigResolver.resolveFromAnnotations(testClass);

            // Assert
            assertEquals(2, elements.size(), SHOULD_RESOLVE_TWO_DISPATCHER_ELEMENTS);
            assertTrue(elements.stream().allMatch(MockResponseConfigDispatcherElement.class::isInstance),
                    "All elements should be MockResponseDispatcherElements");
        }
    }

    @Nested
    @DisplayName("Method annotation resolution tests")
    class MethodAnnotationTests {

        @Test
        @DisplayName("Should resolve MockResponseConfig annotation on the context method")
        void shouldResolveMethodAnnotation() throws Exception {
            // Arrange - method-level annotations are resolved for the current test method (context aware)
            Method testMethod = ClassWithMethodAnnotation.class.getDeclaredMethod("testMethod");

            // Act
            List<ModuleDispatcherElement> elements =
                    MockResponseConfigResolver.resolveFromAnnotations(ClassWithMethodAnnotation.class, testMethod);

            // Assert
            assertEquals(1, elements.size(), "Should resolve one dispatcher element");
            ModuleDispatcherElement element = elements.getFirst();
            assertInstanceOf(MockResponseConfigDispatcherElement.class, element, "Element should be a MockResponseConfigDispatcherElement");
            assertEquals("/api/method", element.getBaseUrl(),
                    "Element should have correct path");
        }

        @Test
        @DisplayName("Should not resolve method annotations without a context method (legacy mode)")
        void shouldNotResolveMethodAnnotationInLegacyMode() {
            List<ModuleDispatcherElement> elements =
                    MockResponseConfigResolver.resolveFromAnnotations(ClassWithMethodAnnotation.class);
            assertEquals(0, elements.size(), "Legacy mode resolves class-level annotations only");
        }
    }

    @Nested
    @DisplayName("Nested class annotation resolution tests")
    class NestedClassTests {

        @Test
        @DisplayName("Should resolve annotations from the nested class and its enclosing class for a nested test method")
        void shouldResolveNestedClassAnnotations() throws Exception {
            // Arrange - resolve for a method declared in the nested class (context aware)
            Method testMethod = ClassWithNestedClassTest.NestedTestClass.class.getDeclaredMethod("nestedTest");

            // Act
            List<ModuleDispatcherElement> elements =
                    MockResponseConfigResolver.resolveFromAnnotations(ClassWithNestedClassTest.class, testMethod);

            // Assert
            assertEquals(2, elements.size(), SHOULD_RESOLVE_TWO_DISPATCHER_ELEMENTS);
            assertTrue(elements.stream()
                            .map(ModuleDispatcherElement::getBaseUrl)
                            .anyMatch(DEFAULT_PATH::equals),
                    "Should include element from enclosing class");
            assertTrue(elements.stream()
                            .map(ModuleDispatcherElement::getBaseUrl)
                            .anyMatch("/api/nested"::equals),
                    "Should include element from nested class");
        }

        @Test
        @DisplayName("Should resolve MockResponseConfig annotations on methods in nested classes")
        void shouldResolveNestedClassMethodAnnotations() throws Exception {
            // Arrange
            Method testMethod = ClassWithNestedClassMethodTest.NestedTestClass.class.getDeclaredMethod("testMethod");

            // Act
            List<ModuleDispatcherElement> elements =
                    MockResponseConfigResolver.resolveFromAnnotations(ClassWithNestedClassMethodTest.class, testMethod);

            // Assert
            assertEquals(2, elements.size(), SHOULD_RESOLVE_TWO_DISPATCHER_ELEMENTS);
            assertTrue(elements.stream()
                            .map(ModuleDispatcherElement::getBaseUrl)
                            .anyMatch("/api/nested-method"::equals),
                    "Should include element from method in nested class");
        }
    }

    @Nested
    @DisplayName("Error handling tests")
    class ErrorHandlingTests {

        @Test
        @DisplayName("Should handle invalid MockResponseConfig annotations gracefully")
        void shouldHandleInvalidAnnotations() {
            // Arrange
            Class<?> testClass = ClassWithInvalidMockResponse.class;

            // Act
            List<ModuleDispatcherElement> elements = MockResponseConfigResolver.resolveFromAnnotations(testClass);

            // Assert
            assertEquals(0, elements.size(), "Should not create elements for invalid annotations");
        }
    }

    // Test classes with various MockResponseConfig annotations

    @MockResponseConfig(path = DEFAULT_PATH, status = DEFAULT_STATUS, textContent = TEST_CONTENT)
    static class ClassWithMockResponse {
        // Empty test class
    }

    @MockResponseConfig(path = DEFAULT_PATH, status = DEFAULT_STATUS, textContent = TEST_CONTENT)
    @MockResponseConfig(path = "/api/second", status = 201, textContent = "Second response")
    static class ClassWithMultipleMockResponses {
        // Empty test class
    }

    static class ClassWithMethodAnnotation {
        @MockResponseConfig(path = "/api/method", status = DEFAULT_STATUS, textContent = TEST_CONTENT)
        @SuppressWarnings("unused")
        // Implicitly called by reflection
        void testMethod() {
            // Empty test method
        }
    }

    @MockResponseConfig(path = DEFAULT_PATH, status = DEFAULT_STATUS, textContent = TEST_CONTENT)
    static class ClassWithNestedClassTest {
        @Nested
        @MockResponseConfig(path = "/api/nested", status = DEFAULT_STATUS, textContent = "Nested content")
        class NestedTestClass {
            @SuppressWarnings("unused") // used via reflection as the context method
            void nestedTest() {
                // Empty nested test method
            }
        }
    }

    @MockResponseConfig(path = DEFAULT_PATH, status = DEFAULT_STATUS, textContent = TEST_CONTENT)
    static class ClassWithNestedClassMethodTest {
        @Nested
        class NestedTestClass {
            @MockResponseConfig(path = "/api/nested-method", status = DEFAULT_STATUS, textContent = "Nested method content")
            @SuppressWarnings("unused")
            // Implicitly called by reflection
            void testMethod() {
                // Empty test method
            }
        }
    }

    @MockResponseConfig(path = DEFAULT_PATH, status = DEFAULT_STATUS,
            textContent = TEST_CONTENT, jsonContentKeyValue = "key=value")
    static class ClassWithInvalidMockResponse {
        // Empty test class
    }
}
