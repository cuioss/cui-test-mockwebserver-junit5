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

import de.cuioss.test.mockwebserver.MockWebServerHolder;
import de.cuioss.tools.logging.CuiLogger;
import lombok.NonNull;
import okhttp3.Headers;
import okio.Buffer;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtensionContext;

import java.net.Socket;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;


import mockwebserver3.Dispatcher;
import mockwebserver3.MockResponse;
import mockwebserver3.RecordedRequest;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for {@link DispatcherResolver}
 *
 * @author Oliver Wolff
 */
@DisplayName("Tests the DispatcherResolver class")
class DispatcherResolverTest {

    private static final CuiLogger LOGGER = new CuiLogger(DispatcherResolverTest.class);

    private static final DispatcherResolver resolver = new DispatcherResolver();
    // Using null for ExtensionContext since it's not used in the resolver
    private static final ExtensionContext extensionContext = null;

    // tag::test-annotation-class[]
    @Test
    @DisplayName("Should resolve dispatcher from @ModuleDispatcher annotation with direct class reference")
    void shouldResolveFromAnnotationWithClass() {
        // Arrange
        var testClass = TestClassWithDispatcherAnnotation.class;
        var testInstance = new TestClassWithDispatcherAnnotation();

        // Act
        var dispatcher = resolver.resolveDispatcher(testClass, testInstance, extensionContext);

        // Assert
        assertNotNull(dispatcher);
        assertInstanceOf(CombinedDispatcher.class, dispatcher);

        // Verify the dispatcher works as expected
        var response = dispatchRequest(dispatcher, TEST_PATH);
        LOGGER.debug("TEST_PATH Response Status: {}", response.getStatus());
        LOGGER.debug("TEST_PATH Response Body: {}", response.getBody());
        assertTrue(response.getStatus().contains(String.valueOf(200)),
                response.getStatus() + STATUS_ERROR_MESSAGE);
        assertTrue(writeBodyToString(response).contains("Test Dispatcher"));
    }

    private String writeBodyToString(MockResponse response) {
        Buffer buffer = new Buffer();
        try {
            response.getBody().writeTo(buffer);
            return buffer.readUtf8();
        } catch (Exception e) {
            return "Failed to read response body";
        }
    }

    // end::test-annotation-class[]

    @Test
    @DisplayName("Should resolve dispatcher from getModuleDispatcher method")
    void shouldResolveFromMethod() {
        // Arrange
        var testClass = TestClassWithMethod.class;
        var testInstance = new TestClassWithMethod();

        // Act
        var dispatcher = resolver.resolveDispatcher(testClass, testInstance, extensionContext);

        // Assert
        assertNotNull(dispatcher);
        assertInstanceOf(CombinedDispatcher.class, dispatcher);

        // Verify the dispatcher works as expected
        var response = dispatchRequest(dispatcher, METHOD_PATH);
        LOGGER.debug("METHOD_PATH Response Status: {}", response.getStatus());
        LOGGER.debug("METHOD_PATH Response Body: {}", response.getBody());
        assertTrue(response.getStatus().contains(String.valueOf(200)),
                response.getStatus() + STATUS_ERROR_MESSAGE);
        assertTrue(writeBodyToString(response).contains("Method Dispatcher"));
    }

    @Test
    @DisplayName("Should use legacy dispatcher from MockWebServerHolder if no other dispatcher is found")
    void shouldUseLegacyDispatcher() {
        // Arrange
        var testClass = TestClassWithLegacyDispatcher.class;
        var testInstance = new TestClassWithLegacyDispatcher();

        // Act
        var dispatcher = resolver.resolveDispatcher(testClass, testInstance, extensionContext);

        // Assert
        assertNotNull(dispatcher);
        assertInstanceOf(TestDispatcher.class, dispatcher);

        // Verify the dispatcher works as expected
        var response = dispatchRequest(dispatcher, LEGACY_PATH);
        LOGGER.debug("LEGACY_PATH Response Status: {}", response.getStatus());
        LOGGER.debug("LEGACY_PATH Response Body: {}", response.getBody());
        assertTrue(response.getStatus().contains(String.valueOf(200)),
                response.getStatus() + STATUS_ERROR_MESSAGE);
        assertTrue(writeBodyToString(response).contains("Legacy Dispatcher"));
    }

    @Test
    @DisplayName("Should fallback to default API dispatcher if no dispatcher is found")
    void shouldFallbackToDefaultDispatcher() {
        // Arrange
        var testClass = Object.class;
        var testInstance = new Object();

        // Act
        var dispatcher = resolver.resolveDispatcher(testClass, testInstance, extensionContext);

        // Assert
        assertNotNull(dispatcher);
        assertTrue(dispatcher instanceof CombinedDispatcher);
    }

    // Constants for test paths
    private static final String TEST_PATH = "/test";
    private static final String METHOD_PATH = "/method";
    private static final String LEGACY_PATH = "/legacy";
    private static final String STATUS_ERROR_MESSAGE = " does not contain 200";

    /**
     * Helper method to dispatch a request to a dispatcher
     */
    private static MockResponse dispatchRequest(Dispatcher dispatcher, String path) {
        try {
            // Create a request with the path directly
            var request = new RecordedRequest("GET " + path + " HTTP/1.1",
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

    /**
     * Custom exception for dispatch errors
     */
    private static class DispatchException extends RuntimeException {
        private static final long serialVersionUID = 1L;

        public DispatchException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    // Test classes for the tests

    // Test class with @ModuleDispatcher annotation
    @ModuleDispatcher(TestDispatcherElement.class)
    static class TestClassWithDispatcherAnnotation {
        // Using the default TestDispatcherElement with baseUrl="/"

    }

    // Test class with getModuleDispatcher method
    static class TestClassWithMethod {
        public ModuleDispatcherElement getModuleDispatcher() {
            return new TestDispatcherElement(METHOD_PATH);
        }
    }

    // Test class with legacy dispatcher
    @SuppressWarnings("deprecation")
    static class TestClassWithLegacyDispatcher implements MockWebServerHolder {

        @Override
        @SuppressWarnings({"deprecation", "removal"})
        public Dispatcher getDispatcher() {
            return new TestDispatcher();
        }
    }

    // Test dispatcher element implementation
    @SuppressWarnings("deprecation")
    private static final class TestDispatcherElement implements ModuleDispatcherElement {
        private final String baseUrl;

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
                // For the TestClassWithDispatcherAnnotation, we need to handle the TEST_PATH
                if (request.getPath().startsWith(TEST_PATH)) {
                    return Optional.of(new MockResponse.Builder()
                            .code(200)
                            .body("Test Dispatcher")
                            .build());
                } else if (request.getPath().startsWith(METHOD_PATH)) {
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

    // Test legacy dispatcher implementation
    static class TestDispatcher extends Dispatcher {
        @NotNull
        @Override
        public MockResponse dispatch(@NotNull RecordedRequest request) {
            // Log the incoming request path
            LOGGER.debug("Legacy dispatcher received request with path: {}", request.getPath());

            // For legacy test, we need to handle the LEGACY_PATH
            if (request.getPath() != null && request.getPath().contains(LEGACY_PATH)) {
                return new MockResponse.Builder()
                        .code(200)
                        .body("Legacy Dispatcher")
                        .build();
            }
            // For any other path, return 404
            return new MockResponse.Builder().code(404).build();
        }
    }
}
