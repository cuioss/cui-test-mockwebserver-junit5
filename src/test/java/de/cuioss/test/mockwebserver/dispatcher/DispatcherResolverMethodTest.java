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

import de.cuioss.tools.logging.CuiLogger;
import lombok.NonNull;
import mockwebserver3.Dispatcher;
import mockwebserver3.MockResponse;
import mockwebserver3.RecordedRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.net.Socket;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;


import okio.Buffer;
import okhttp3.Headers;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for {@link DispatcherResolver} focusing on method resolution
 *
 * @author Oliver Wolff
 */
@DisplayName("Tests the DispatcherResolver method resolution")
class DispatcherResolverMethodTest {

    private static final CuiLogger LOGGER = new CuiLogger(DispatcherResolverMethodTest.class);

    private static final DispatcherResolver resolver = new DispatcherResolver();

    // Constants for test paths
    private static final String METHOD_PATH = "/method";
    private static final String STATUS_ERROR_MESSAGE = " does not contain 200";

    @Test
    @DisplayName("Should resolve dispatcher from getModuleDispatcher method")
    void shouldResolveFromMethod() {
        // Arrange
        var testClass = TestClassWithMethod.class;
        var testInstance = new TestClassWithMethod();

        // Act
        var dispatcher = resolver.resolveDispatcher(testClass, testInstance);

        // Assert
        assertNotNull(dispatcher);
        assertInstanceOf(CombinedDispatcher.class, dispatcher);

        // Verify the dispatcher works as expected
        var response = dispatchRequest(dispatcher);
        LOGGER.debug("METHOD_PATH Response Status: {}", response.getStatus());
        LOGGER.debug("METHOD_PATH Response Body: {}", response.getBody());
        assertTrue(response.getStatus().contains(String.valueOf(200)),
                response.getStatus() + STATUS_ERROR_MESSAGE);
        assertTrue(writeBodyToString(response).contains("Method Dispatcher"));
    }

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

    /**
     * Helper method to dispatch a request to a dispatcher
     */
    private static MockResponse dispatchRequest(Dispatcher dispatcher) {
        try {
            // Create a request with the path directly
            var request = new RecordedRequest("GET " + DispatcherResolverMethodTest.METHOD_PATH + " HTTP/1.1",
                    Headers.of("Host", "localhost"),
                    Collections.emptyList(),
                    0L,
                    new Buffer(),
                    0,
                    new Socket(),
                    null);
            return dispatcher.dispatch(request);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to dispatch request", e);
        }
    }

    private String writeBodyToString(MockResponse response) {
        Buffer buffer = new Buffer();
        try {
            assert response.getBody() != null;
            response.getBody().writeTo(buffer);
            return buffer.readUtf8();
        } catch (Exception e) {
            return "Failed to read response body";
        }
    }

    // Test classes for the tests

    // Test class with getModuleDispatcher method
    static class TestClassWithMethod {
        @SuppressWarnings("unused") // implicitly called by the test framework
        public ModuleDispatcherElement getModuleDispatcher() {
            return new TestDispatcherElement(METHOD_PATH);
        }
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
