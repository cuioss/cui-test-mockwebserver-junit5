/*
 * Copyright © 2025 CUI-OpenSource-Software (info@cuioss.de)
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

import lombok.NonNull;
import mockwebserver3.Dispatcher;
import mockwebserver3.MockResponse;
import mockwebserver3.RecordedRequest;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Advanced test class for {@link DispatcherResolver} focusing on edge cases and uncovered paths
 * to improve code coverage intelligently.
 *
 * @author Test Coverage Enhancement
 */
@DisplayName("Advanced DispatcherResolver Tests - Coverage Enhancement")
class DispatcherResolverAdvancedTest {

    private static final DispatcherResolver resolver = new DispatcherResolver();

    @Test
    @DisplayName("Should handle annotation with constructor that throws exception")
    void shouldHandleAnnotationWithFailingConstructor() {
        // Arrange
        var testClass = TestClassWithFailingConstructorDispatcher.class;
        var testInstance = new TestClassWithFailingConstructorDispatcher();

        // Act - should not throw exception, should fall back to default
        var dispatcher = resolver.resolveDispatcher(testClass, testInstance);

        // Assert
        assertNotNull(dispatcher);
        assertInstanceOf(CombinedDispatcher.class, dispatcher);
    }

    @Test
    @DisplayName("Should handle annotation with provider method that throws exception")
    void shouldHandleAnnotationWithFailingProviderMethod() {
        // Arrange
        var testClass = TestClassWithFailingProviderMethod.class;
        var testInstance = new TestClassWithFailingProviderMethod();

        // Act - should not throw exception, should fall back to default
        var dispatcher = resolver.resolveDispatcher(testClass, testInstance);

        // Assert
        assertNotNull(dispatcher);
        assertInstanceOf(CombinedDispatcher.class, dispatcher);
    }

    @Test
    @DisplayName("Should handle annotation with non-static provider method")
    void shouldHandleAnnotationWithNonStaticProviderMethod() {
        // Arrange
        var testClass = TestClassWithNonStaticProviderMethod.class;
        var testInstance = new TestClassWithNonStaticProviderMethod();

        // Act
        var dispatcher = resolver.resolveDispatcher(testClass, testInstance);

        // Assert
        assertNotNull(dispatcher);
        assertInstanceOf(CombinedDispatcher.class, dispatcher);
    }

    @Test
    @DisplayName("Should handle annotation with provider method returning wrong type")
    void shouldHandleAnnotationWithProviderReturningWrongType() {
        // Arrange
        var testClass = TestClassWithProviderReturningWrongType.class;
        var testInstance = new TestClassWithProviderReturningWrongType();

        // Act
        var dispatcher = resolver.resolveDispatcher(testClass, testInstance);

        // Assert
        assertNotNull(dispatcher);
        assertInstanceOf(CombinedDispatcher.class, dispatcher);
    }

    @Test
    @DisplayName("Should detect and throw exception for dispatcher conflicts")
    void shouldDetectDispatcherConflicts() {
        // Arrange
        var testClass = TestClassWithConflictingDispatchers.class;
        var testInstance = new TestClassWithConflictingDispatchers();

        // Act & Assert
        var exception = assertThrows(IllegalStateException.class, () ->
                resolver.resolveDispatcher(testClass, testInstance));

        assertTrue(exception.getMessage().contains("Dispatcher conflicts found"),
                "Exception message should indicate conflicts: " + exception.getMessage());
    }

    @Test
    @DisplayName("Should handle annotation with no-arg constructor that is not accessible")
    void shouldHandleAnnotationWithInaccessibleConstructor() {
        // Arrange
        var testClass = TestClassWithInaccessibleConstructorDispatcher.class;
        var testInstance = new TestClassWithInaccessibleConstructorDispatcher();

        // Act - should not throw exception, should fall back to default
        var dispatcher = resolver.resolveDispatcher(testClass, testInstance);

        // Assert
        assertNotNull(dispatcher);
        assertInstanceOf(CombinedDispatcher.class, dispatcher);
    }

    @Test
    @DisplayName("Should handle empty dispatcher list and fall back to default")
    void shouldHandleEmptyDispatcherList() {
        // Arrange - class with no annotations or methods
        var testClass = EmptyTestClass.class;
        var testInstance = new EmptyTestClass();

        // Act
        var dispatcher = resolver.resolveDispatcher(testClass, testInstance);

        // Assert
        assertNotNull(dispatcher);
        assertInstanceOf(CombinedDispatcher.class, dispatcher);
    }

    @Test
    @DisplayName("Should handle provider method that does not exist")
    void shouldHandleNonExistentProviderMethod() {
        // Arrange
        var testClass = TestClassWithNonExistentProviderMethod.class;
        var testInstance = new TestClassWithNonExistentProviderMethod();

        // Act
        var dispatcher = resolver.resolveDispatcher(testClass, testInstance);

        // Assert
        assertNotNull(dispatcher);
        assertInstanceOf(CombinedDispatcher.class, dispatcher);
    }

    @Test
    @DisplayName("Should handle context-aware resolution with test method")
    void shouldHandleContextAwareResolution() throws NoSuchMethodException {
        // Arrange
        var testClass = TestClassWithMethod.class;
        var testInstance = new TestClassWithMethod();
        Method testMethod = this.getClass().getDeclaredMethod("shouldHandleContextAwareResolution");

        // Act
        var dispatcher = resolver.resolveDispatcher(testClass, testInstance, testMethod);

        // Assert
        assertNotNull(dispatcher);
        assertInstanceOf(CombinedDispatcher.class, dispatcher);
    }


    @Test
    @DisplayName("Should handle annotation with provider method returning direct Dispatcher")
    void shouldHandleProviderMethodReturningDirectDispatcher() {
        // Arrange
        var testClass = TestClassWithDirectDispatcherProvider.class;
        var testInstance = new TestClassWithDirectDispatcherProvider();

        // Act
        var dispatcher = resolver.resolveDispatcher(testClass, testInstance);

        // Assert
        assertNotNull(dispatcher);
        // Should return the direct dispatcher, not wrapped in CombinedDispatcher
        assertInstanceOf(TestDirectDispatcher.class, dispatcher);
    }

    @Test
    @DisplayName("Should handle public method that is not accessible due to module system")
    void shouldHandlePublicMethodNotAccessible() {
        // Arrange
        var testClass = TestClassWithPublicMethodNotAccessible.class;
        var testInstance = new TestClassWithPublicMethodNotAccessible();

        // Act
        var dispatcher = resolver.resolveDispatcher(testClass, testInstance);

        // Assert
        assertNotNull(dispatcher);
        assertInstanceOf(CombinedDispatcher.class, dispatcher);
    }

    // Test classes for various scenarios

    @ModuleDispatcher(FailingConstructorDispatcher.class)
    static class TestClassWithFailingConstructorDispatcher {
    }

    @ModuleDispatcher(provider = TestClassWithFailingProviderMethod.class, providerMethod = "failingProvider")
    static class TestClassWithFailingProviderMethod {
        public static ModuleDispatcherElement failingProvider() {
            /*~~(TODO: Throw specific not RuntimeException. Suppress: // cui-rewrite:disable InvalidExceptionUsageRecipe)~~>*/throw new RuntimeException("Provider method failed");
        }
    }

    @ModuleDispatcher(provider = TestClassWithNonStaticProviderMethod.class, providerMethod = "nonStaticProvider")
    static class TestClassWithNonStaticProviderMethod {
        public ModuleDispatcherElement nonStaticProvider() {
            return new TestDispatcherElement("/non-static");
        }
    }

    @ModuleDispatcher(provider = TestClassWithProviderReturningWrongType.class, providerMethod = "wrongTypeProvider")
    static class TestClassWithProviderReturningWrongType {
        public static String wrongTypeProvider() {
            return "Not a dispatcher";
        }
    }

    @ModuleDispatcher(ConflictingDispatcherElement.class)
    static class TestClassWithConflictingDispatchers {
        public ModuleDispatcherElement getModuleDispatcher() {
            return new ConflictingDispatcherElement();
        }
    }

    static class TestClassWithMethod {
        public ModuleDispatcherElement getModuleDispatcher() {
            return new TestDispatcherElement("/method");
        }
    }


    @ModuleDispatcher(providerMethod = "provideDirectDispatcher")
    static class TestClassWithDirectDispatcherProvider {
        public static Dispatcher provideDirectDispatcher() {
            return new TestDirectDispatcher();
        }
    }

    static class TestClassWithPublicMethodNotAccessible {
        public ModuleDispatcherElement getModuleDispatcher() {
            return new TestDispatcherElement("/public-not-accessible");
        }
    }

    @ModuleDispatcher(InaccessibleConstructorDispatcher.class)
    static class TestClassWithInaccessibleConstructorDispatcher {
    }

    static class EmptyTestClass {
        // No annotations or methods
    }

    @ModuleDispatcher(providerMethod = "nonExistentMethod")
    static class TestClassWithNonExistentProviderMethod {
    }

    // Helper classes

    static class FailingConstructorDispatcher implements ModuleDispatcherElement {
        public FailingConstructorDispatcher() {
            /*~~(TODO: Throw specific not RuntimeException. Suppress: // cui-rewrite:disable InvalidExceptionUsageRecipe)~~>*/throw new RuntimeException("Constructor failed");
        }

        @Override
        public String getBaseUrl() {
            return "/failing";
        }

        @Override
        public Optional<MockResponse> handleGet(@NotNull RecordedRequest request) {
            return Optional.empty();
        }

        @Override
        public @NonNull Set<HttpMethodMapper> supportedMethods() {
            return Set.of(HttpMethodMapper.GET);
        }
    }

    static class InaccessibleConstructorDispatcher implements ModuleDispatcherElement {
        private InaccessibleConstructorDispatcher() {
            // Private constructor to test accessibility issues
        }

        @Override
        public String getBaseUrl() {
            return "/inaccessible";
        }

        @Override
        public Optional<MockResponse> handleGet(@NotNull RecordedRequest request) {
            return Optional.empty();
        }

        @Override
        public @NonNull Set<HttpMethodMapper> supportedMethods() {
            return Set.of(HttpMethodMapper.GET);
        }
    }

    static class ConflictingDispatcherElement implements ModuleDispatcherElement {
        @Override
        public String getBaseUrl() {
            return "/conflict";
        }

        @Override
        public Optional<MockResponse> handleGet(@NotNull RecordedRequest request) {
            return Optional.of(new MockResponse.Builder()
                    .code(200)
                    .body("Conflict 1")
                    .build());
        }

        @Override
        public @NonNull Set<HttpMethodMapper> supportedMethods() {
            return Set.of(HttpMethodMapper.GET);
        }
    }

    static class TestDispatcherElement implements ModuleDispatcherElement {
        private final String baseUrl;

        public TestDispatcherElement(String baseUrl) {
            this.baseUrl = baseUrl;
        }

        @Override
        public String getBaseUrl() {
            return baseUrl;
        }

        @Override
        public Optional<MockResponse> handleGet(@NotNull RecordedRequest request) {
            return Optional.of(new MockResponse.Builder()
                    .code(200)
                    .body("Test Response")
                    .build());
        }

        @Override
        public @NonNull Set<HttpMethodMapper> supportedMethods() {
            return Set.of(HttpMethodMapper.GET);
        }
    }

    static class TestDirectDispatcher extends Dispatcher {
        @NotNull
        @Override
        public MockResponse dispatch(@NotNull RecordedRequest request) {
            return new MockResponse.Builder()
                    .code(200)
                    .body("Direct Dispatcher")
                    .build();
        }
    }
}
