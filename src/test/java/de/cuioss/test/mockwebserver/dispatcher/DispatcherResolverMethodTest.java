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
package de.cuioss.test.mockwebserver.dispatcher;

import lombok.NonNull;
import mockwebserver3.MockResponse;
import mockwebserver3.RecordedRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test class for {@link DispatcherResolver} focusing on method resolution
 *
 * @author Oliver Wolff
 */
@DisplayName("Tests the DispatcherResolver method resolution")
class DispatcherResolverMethodTest {

    private static final DispatcherResolver resolver = new DispatcherResolver();

    // Constants for test paths
    private static final String METHOD_PATH = "/method";

    // Note: Basic test for resolving dispatcher from getModuleDispatcher method
    // has been moved to DispatcherResolverTest.shouldResolveFromMethod to avoid duplication.
    // This class focuses on more specific method resolution test cases.

    @Test
    @DisplayName("Should return empty when getModuleDispatcher method doesn't exist")
    void shouldReturnEmptyWhenMethodDoesNotExist() {
        // Arrange
        var testClass = Object.class; // Object class doesn't have getModuleDispatcher method
        var testInstance = new Object();

        // Act
        var dispatcher = resolver.resolveDispatcher(testClass, testInstance);

        // Assert
        assertNotNull(dispatcher);
        // Should fall back to default dispatcher
        assertInstanceOf(CombinedDispatcher.class, dispatcher);
    }

    @Test
    @DisplayName("Should throw exception when getModuleDispatcher method returns null")
    void shouldThrowExceptionWhenMethodReturnsNull() {
        // Arrange
        var testClass = TestClassWithNullMethod.class;
        var testInstance = new TestClassWithNullMethod();

        // Act & Assert
        var exception = assertThrows(DispatcherResolutionException.class, () ->
                resolver.resolveDispatcher(testClass, testInstance));

        assertTrue(exception.getMessage().contains("returned null"),
                "Exception message should indicate null return: " + exception.getMessage());
    }

    @Test
    @DisplayName("Should throw exception when getModuleDispatcher method returns wrong type")
    void shouldThrowExceptionWhenMethodReturnsWrongType() {
        // Arrange
        var testClass = TestClassWithWrongReturnType.class;
        var testInstance = new TestClassWithWrongReturnType();

        // Act & Assert
        var exception = assertThrows(DispatcherResolutionException.class, () ->
                resolver.resolveDispatcher(testClass, testInstance));

        assertTrue(exception.getMessage().contains("did not return a ModuleDispatcherElement"),
                "Exception message should indicate wrong return type: " + exception.getMessage());
    }

    @Test
    @DisplayName("Should throw exception when getModuleDispatcher method throws exception")
    void shouldThrowExceptionWhenMethodThrowsException() {
        // Arrange
        var testClass = TestClassWithThrowingMethod.class;
        var testInstance = new TestClassWithThrowingMethod();

        // Act & Assert
        var exception = assertThrows(DispatcherResolutionException.class, () ->
                resolver.resolveDispatcher(testClass, testInstance));

        assertTrue(exception.getMessage().contains("threw an exception"),
                "Exception message should indicate method threw exception: " + exception.getMessage());
        assertNotNull(exception.getCause(), "Exception should have a cause");
        assertEquals("Test exception", exception.getCause().getMessage(),
                "Cause should be the original exception");
    }

    @Test
    @DisplayName("Should throw exception when getModuleDispatcher method is not accessible")
    void shouldThrowExceptionWhenMethodIsNotAccessible() {
        // Arrange
        var testClass = TestClassWithPrivateMethod.class;
        var testInstance = new TestClassWithPrivateMethod();

        // Act & Assert
        var exception = assertThrows(DispatcherResolutionException.class, () ->
                resolver.resolveDispatcher(testClass, testInstance));

        assertTrue(exception.getMessage().contains("Cannot access"),
                "Exception message should indicate access issue: " + exception.getMessage());
    }

    // Test dispatcher element implementation
    private static final class TestDispatcherElement implements ModuleDispatcherElement {
        private final String baseUrl;

        @SuppressWarnings("unused") // implicitly called by the test framework
        public TestDispatcherElement() {
            this.baseUrl = "/"; // Default to handle all paths
        }

        public TestDispatcherElement(String baseUrl) {
            this.baseUrl = baseUrl;
        }

        @Override
        public String getBaseUrl() {
            return baseUrl;
        }

        @Override
        public Optional<MockResponse> handleGet(RecordedRequest request) {
            if (request.getPath() != null) {
                if (request.getPath().startsWith(METHOD_PATH)) {
                    return Optional.of(new MockResponse.Builder()
                            .code(200)
                            .body("Method Dispatcher")
                            .build());
                } else if (request.getPath().startsWith(baseUrl)) {
                    return Optional.of(new MockResponse.Builder()
                            .code(200)
                            .body("Default Dispatcher Response")
                            .build());
                }
            }
            return Optional.empty();
        }

        @Override
        public @NonNull Set<HttpMethodMapper> supportedMethods() {
            return Set.of(HttpMethodMapper.GET);
        }
    }

    // Test class with getModuleDispatcher method that returns null
    static class TestClassWithNullMethod {
        @SuppressWarnings("unused") // implicitly called by the test framework
        public ModuleDispatcherElement getModuleDispatcher() {
            return null;
        }
    }

    // Test class with getModuleDispatcher method that returns wrong type
    static class TestClassWithWrongReturnType {
        @SuppressWarnings("unused") // implicitly called by the test framework
        public Object getModuleDispatcher() {
            return "Not a ModuleDispatcherElement";
        }
    }

    // Test class with getModuleDispatcher method that throws exception
    static class TestClassWithThrowingMethod {
        @SuppressWarnings("unused") // implicitly called by the test framework
        public ModuleDispatcherElement getModuleDispatcher() {
            throw new RuntimeException("Test exception");
        }
    }

    // Test class with private getModuleDispatcher method
    static class TestClassWithPrivateMethod {
        // This method is intentionally private to test access issues
        // The warning about unused method is suppressed because it's used indirectly in the test
        @SuppressWarnings("unused")
        private ModuleDispatcherElement getModuleDispatcher() {
            return new TestDispatcherElement(METHOD_PATH);
        }
    }
}
