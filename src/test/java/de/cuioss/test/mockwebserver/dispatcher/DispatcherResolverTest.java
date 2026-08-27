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
package de.cuioss.test.mockwebserver.dispatcher;

import de.cuioss.tools.logging.CuiLogger;
import lombok.NonNull;
import mockwebserver3.Dispatcher;
import mockwebserver3.MockResponse;
import mockwebserver3.RecordedRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for {@link DispatcherResolver}
 * <p>
 * This class tests the various ways to resolve dispatchers using the DispatcherResolver,
 * including annotation-based resolution, method-based resolution, and fallback behavior.
 *
 * @author Oliver Wolff
 */
@DisplayName("DispatcherResolver - Resolution Strategy Tests")
class DispatcherResolverTest {

    private static final CuiLogger LOGGER = new CuiLogger(DispatcherResolverTest.class);

    private static final DispatcherResolver resolver = new DispatcherResolver();


    /**
     * Tests that the resolver correctly resolves a dispatcher from a class annotated
     * with @ModuleDispatcher that directly references a dispatcher class.
     */
    @Test
    @DisplayName("Should resolve dispatcher from @ModuleDispatcher annotation with direct class reference")
    void shouldResolveFromAnnotationWithClass() {
        // Arrange
        var testClass = TestClassWithDispatcherAnnotation.class;
        var testInstance = new TestClassWithDispatcherAnnotation();

        // Act
        var dispatcher = resolver.resolveDispatcher(testClass, testInstance);

        // Assert
        assertNotNull(dispatcher, "Resolved dispatcher should not be null");
        assertInstanceOf(CombinedDispatcher.class, dispatcher, "Dispatcher should be a CombinedDispatcher");

        // Verify the dispatcher works as expected
        var response = DispatcherTestSupport.dispatch(dispatcher, TEST_PATH);
        LOGGER.debug("TEST_PATH Response Status: %s", response.getStatus());
        LOGGER.debug("TEST_PATH Response Body: %s", response.getBody());
        assertTrue(response.getStatus().contains(String.valueOf(200)),
                response.getStatus() + STATUS_ERROR_MESSAGE);
        assertTrue(DispatcherTestSupport.readBody(response).contains("Test Dispatcher"),
                "Response body should contain expected content");
    }


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
        var response = DispatcherTestSupport.dispatch(dispatcher, METHOD_PATH);
        LOGGER.debug("METHOD_PATH Response Status: %s", response.getStatus());
        LOGGER.debug("METHOD_PATH Response Body: %s", response.getBody());
        assertTrue(response.getStatus().contains(String.valueOf(200)),
                response.getStatus() + STATUS_ERROR_MESSAGE);
        assertTrue(DispatcherTestSupport.readBody(response).contains("Method Dispatcher"));
    }

    @Test
    @DisplayName("Should use dispatcher from ModuleDispatcher annotation with provider method")
    void shouldUseProviderMethodDispatcher() {
        // Arrange
        var testClass = TestClassWithLegacyDispatcher.class;

        // Act
        var dispatcher = resolver.resolveDispatcher(testClass, null);

        // Assert
        assertNotNull(dispatcher);
        // The dispatcher should be the TestDispatcher directly
        assertInstanceOf(TestDispatcher.class, dispatcher);

        // Verify the dispatcher works as expected
        var response = DispatcherTestSupport.dispatch(dispatcher, LEGACY_PATH);
        LOGGER.debug("LEGACY_PATH Response Status: %s", response.getStatus());
        LOGGER.debug("LEGACY_PATH Response Body: %s", response.getBody());
        assertTrue(response.getStatus().contains(String.valueOf(200)),
                response.getStatus() + STATUS_ERROR_MESSAGE);
        assertTrue(DispatcherTestSupport.readBody(response).contains("Legacy Dispatcher"));
    }

    @Test
    @DisplayName("Should fallback to default API dispatcher if no dispatcher is found")
    void shouldFallbackToDefaultDispatcher() {
        // Arrange
        var testClass = Object.class;
        var testInstance = new Object();

        // Act
        var dispatcher = resolver.resolveDispatcher(testClass, testInstance);

        // Assert
        assertNotNull(dispatcher);
        assertInstanceOf(CombinedDispatcher.class, dispatcher);
    }

    // Constants for test paths
    private static final String TEST_PATH = "/test";
    private static final String METHOD_PATH = "/method";
    private static final String LEGACY_PATH = "/legacy";
    private static final String STATUS_ERROR_MESSAGE = " does not contain 200";


    // Test classes for the tests

    // Test class with @ModuleDispatcher annotation
    @ModuleDispatcher(TestDispatcherElement.class)
    static class TestClassWithDispatcherAnnotation {
        // Using the default TestDispatcherElement with baseUrl="/"

    }

    // Test class with getModuleDispatcher method
    static class TestClassWithMethod {
        @SuppressWarnings("unused") // Implicitly called by the test framework
        public ModuleDispatcherElement getModuleDispatcher() {
            return new TestDispatcherElement(METHOD_PATH);
        }
    }

    // Test class with ModuleDispatcher annotation using provider method
    @ModuleDispatcher(providerMethod = "provideDispatcher")
    static class TestClassWithLegacyDispatcher {

        // Constructor needs to be accessible for test
        TestClassWithLegacyDispatcher() {
            // Package-private constructor for testing
        }

        /**
         * Provides a test dispatcher for the mock web server
         *
         * @return a test dispatcher
         */
        @SuppressWarnings("unused") // implicitly called by the test framework
        static Dispatcher provideDispatcher() {
            return new TestDispatcher();
        }

    }

    // Test legacy dispatcher implementation
    static class TestDispatcher extends Dispatcher {
        @NonNull
        @Override
        public MockResponse dispatch(@NonNull RecordedRequest request) {
            // Log the incoming request path
            LOGGER.debug("Legacy dispatcher received request with path: %s", request.getUrl().encodedPath());

            // For legacy test, we need to handle the LEGACY_PATH
            if (request.getUrl().encodedPath() != null && request.getUrl().encodedPath().contains(LEGACY_PATH)) {
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
