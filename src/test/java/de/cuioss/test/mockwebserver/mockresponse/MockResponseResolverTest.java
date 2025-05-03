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
package de.cuioss.test.mockwebserver.mockresponse;

import de.cuioss.test.mockwebserver.dispatcher.ModuleDispatcherElement;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static de.cuioss.test.mockwebserver.mockresponse.MockResponseTestUtil.DEFAULT_PATH;
import static de.cuioss.test.mockwebserver.mockresponse.MockResponseTestUtil.DEFAULT_STATUS;
import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Tests for MockResponseResolver")
class MockResponseResolverTest {

    // Using constants from MockResponseTestUtil where possible
    private static final String TEST_CONTENT = "Test content";
    private static final String SHOULD_RESOLVE_TWO_DISPATCHER_ELEMENTS = "Should resolve two dispatcher elements";

    @Nested
    @DisplayName("Class annotation resolution tests")
    class ClassAnnotationTests {

        @Test
        @DisplayName("Should resolve MockResponse annotation on class")
        void shouldResolveClassAnnotation() {
            // Arrange
            Class<?> testClass = ClassWithMockResponse.class;

            // Act
            List<ModuleDispatcherElement> elements = MockResponseResolver.resolveFromAnnotations(testClass);

            // Assert
            assertEquals(1, elements.size(), "Should resolve one dispatcher element");
            ModuleDispatcherElement element = elements.get(0);
            assertInstanceOf(MockResponseDispatcherElement.class, element, "Element should be a MockResponseDispatcherElement");
            assertEquals(DEFAULT_PATH, element.getBaseUrl(),
                    "Element should have correct path");
        }

        @Test
        @DisplayName("Should resolve multiple MockResponse annotations on class")
        void shouldResolveMultipleMockResponseAnnotations() {
            // Arrange
            Class<?> testClass = ClassWithMultipleMockResponses.class;

            // Act
            List<ModuleDispatcherElement> elements = MockResponseResolver.resolveFromAnnotations(testClass);

            // Assert
            assertEquals(2, elements.size(), SHOULD_RESOLVE_TWO_DISPATCHER_ELEMENTS);
            assertTrue(elements.stream().allMatch(MockResponseDispatcherElement.class::isInstance),
                    "All elements should be MockResponseDispatcherElements");
        }
    }

    @Nested
    @DisplayName("Method annotation resolution tests")
    class MethodAnnotationTests {

        @Test
        @DisplayName("Should resolve MockResponse annotation on method")
        void shouldResolveMethodAnnotation() {
            // Arrange
            Class<?> testClass = ClassWithMethodAnnotation.class;

            // Act
            List<ModuleDispatcherElement> elements = MockResponseResolver.resolveFromAnnotations(testClass);

            // Assert
            assertEquals(1, elements.size(), "Should resolve one dispatcher element");
            ModuleDispatcherElement element = elements.get(0);
            assertInstanceOf(MockResponseDispatcherElement.class, element, "Element should be a MockResponseDispatcherElement");
            assertEquals("/api/method", element.getBaseUrl(),
                    "Element should have correct path");
        }
    }

    @Nested
    @DisplayName("Nested class annotation resolution tests")
    class NestedClassTests {

        @Test
        @DisplayName("Should resolve MockResponse annotations on nested classes")
        void shouldResolveNestedClassAnnotations() {
            // Arrange
            Class<?> testClass = ClassWithNestedClassTest.class;

            // Act
            List<ModuleDispatcherElement> elements = MockResponseResolver.resolveFromAnnotations(testClass);

            // Assert
            assertEquals(2, elements.size(), SHOULD_RESOLVE_TWO_DISPATCHER_ELEMENTS);
            assertTrue(elements.stream().allMatch(MockResponseDispatcherElement.class::isInstance),
                    "All elements should be MockResponseDispatcherElements");

            // Verify paths from both parent and nested class
            assertTrue(elements.stream()
                            .map(ModuleDispatcherElement::getBaseUrl)
                            .anyMatch(DEFAULT_PATH::equals),
                    "Should include element from parent class");
            assertTrue(elements.stream()
                            .map(ModuleDispatcherElement::getBaseUrl)
                            .anyMatch("/api/nested"::equals),
                    "Should include element from nested class");
        }

        @Test
        @DisplayName("Should resolve MockResponse annotations on methods in nested classes")
        void shouldResolveNestedClassMethodAnnotations() {
            // Arrange
            Class<?> testClass = ClassWithNestedClassMethodTest.class;

            // Act
            List<ModuleDispatcherElement> elements = MockResponseResolver.resolveFromAnnotations(testClass);

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
        @DisplayName("Should handle invalid MockResponse annotations gracefully")
        void shouldHandleInvalidAnnotations() {
            // Arrange
            Class<?> testClass = ClassWithInvalidMockResponse.class;

            // Act
            List<ModuleDispatcherElement> elements = MockResponseResolver.resolveFromAnnotations(testClass);

            // Assert
            assertEquals(0, elements.size(), "Should not create elements for invalid annotations");
        }
    }

    // Test classes with various MockResponse annotations

    @MockResponse(path = DEFAULT_PATH, status = DEFAULT_STATUS, textContent = TEST_CONTENT)
    static class ClassWithMockResponse {
        // Empty test class
    }

    @MockResponse(path = DEFAULT_PATH, status = DEFAULT_STATUS, textContent = TEST_CONTENT)
    @MockResponse(path = "/api/second", status = 201, textContent = "Second response")
    static class ClassWithMultipleMockResponses {
        // Empty test class
    }

    static class ClassWithMethodAnnotation {
        @MockResponse(path = "/api/method", status = DEFAULT_STATUS, textContent = TEST_CONTENT)
        @SuppressWarnings("unused")
        // Implicitly called by reflection
        void testMethod() {
            // Empty test method
        }
    }

    @MockResponse(path = DEFAULT_PATH, status = DEFAULT_STATUS, textContent = TEST_CONTENT)
    static class ClassWithNestedClassTest {
        @Nested
        @MockResponse(path = "/api/nested", status = DEFAULT_STATUS, textContent = "Nested content")
        class NestedTestClass {
            // Empty nested test class
        }
    }

    @MockResponse(path = DEFAULT_PATH, status = DEFAULT_STATUS, textContent = TEST_CONTENT)
    static class ClassWithNestedClassMethodTest {
        @Nested
        class NestedTestClass {
            @MockResponse(path = "/api/nested-method", status = DEFAULT_STATUS, textContent = "Nested method content")
            @SuppressWarnings("unused")
            // Implicitly called by reflection
            void testMethod() {
                // Empty test method
            }
        }
    }

    @MockResponse(path = DEFAULT_PATH, status = DEFAULT_STATUS,
            textContent = TEST_CONTENT, jsonContentKeyValue = "key=value")
    static class ClassWithInvalidMockResponse {
        // Empty test class
    }
}
