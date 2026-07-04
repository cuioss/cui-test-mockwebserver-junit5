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
import okhttp3.Headers;
import okhttp3.HttpUrl;
import okio.Buffer;
import okio.ByteString;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Advanced tests for {@link DispatcherResolver} covering the resolution edge cases. Where a
 * dispatcher is expected to resolve, a probe request is dispatched and its response asserted so the
 * tests distinguish a genuinely resolved dispatcher from the {@code /api} fallback.
 *
 * @author Oliver Wolff
 */
@DisplayName("DispatcherResolver - Advanced Resolution Tests")
class DispatcherResolverAdvancedTest {

    private static final DispatcherResolver resolver = new DispatcherResolver();

    @Test
    @DisplayName("Should throw when the annotated dispatcher constructor fails")
    void shouldThrowOnFailingConstructor() {
        assertThrows(DispatcherResolutionException.class, () ->
                resolver.resolveDispatcher(TestClassWithFailingConstructorDispatcher.class,
                        new TestClassWithFailingConstructorDispatcher()));
    }

    @Test
    @DisplayName("Should throw when the provider method fails")
    void shouldThrowOnFailingProviderMethod() {
        assertThrows(DispatcherResolutionException.class, () ->
                resolver.resolveDispatcher(TestClassWithFailingProviderMethod.class,
                        new TestClassWithFailingProviderMethod()));
    }

    @Test
    @DisplayName("Should resolve a dispatcher from a non-static provider method")
    void shouldResolveNonStaticProviderMethod() {
        var dispatcher = resolver.resolveDispatcher(TestClassWithNonStaticProviderMethod.class,
                new TestClassWithNonStaticProviderMethod());
        assertResponseBody(dispatcher, "/non-static", "Test Response");
    }

    @Test
    @DisplayName("Should resolve a dispatcher from a static provider method (getOptimisticAPIDispatcher)")
    void shouldResolveStaticProviderMethod() {
        var dispatcher = resolver.resolveDispatcher(TestClassWithStaticProviderMethod.class,
                new TestClassWithStaticProviderMethod());
        // BaseAllAcceptDispatcher for /api answers GET with 200
        assertStatus(dispatcher, "/api", 200);
    }

    @Test
    @DisplayName("Should throw when the provider method returns the wrong type")
    void shouldThrowOnProviderReturningWrongType() {
        assertThrows(DispatcherResolutionException.class, () ->
                resolver.resolveDispatcher(TestClassWithProviderReturningWrongType.class,
                        new TestClassWithProviderReturningWrongType()));
    }

    @Test
    @DisplayName("Should throw when a provider class is given without a provider method")
    void shouldThrowOnProviderWithoutMethod() {
        assertThrows(DispatcherResolutionException.class, () ->
                resolver.resolveDispatcher(TestClassWithProviderButNoMethod.class,
                        new TestClassWithProviderButNoMethod()));
    }

    @Test
    @DisplayName("Should throw for dispatcher conflicts")
    void shouldDetectDispatcherConflicts() {
        var exception = assertThrows(IllegalStateException.class, () ->
                resolver.resolveDispatcher(TestClassWithConflictingDispatchers.class,
                        new TestClassWithConflictingDispatchers()));
        assertTrue(exception.getMessage().contains("Dispatcher conflicts found"),
                "Exception message should indicate conflicts: " + exception.getMessage());
    }

    @Test
    @DisplayName("Should throw when the annotated dispatcher constructor is inaccessible")
    void shouldThrowOnInaccessibleConstructor() {
        assertThrows(DispatcherResolutionException.class, () ->
                resolver.resolveDispatcher(TestClassWithInaccessibleConstructorDispatcher.class,
                        new TestClassWithInaccessibleConstructorDispatcher()));
    }

    @Test
    @DisplayName("Should fall back to the default /api dispatcher when nothing is configured")
    void shouldFallBackToDefault() {
        var dispatcher = resolver.resolveDispatcher(EmptyTestClass.class, new EmptyTestClass());
        assertInstanceOf(CombinedDispatcher.class, dispatcher);
        assertStatus(dispatcher, "/api", 200);
        assertStatus(dispatcher, "/somewhere-else", 418);
    }

    @Test
    @DisplayName("Should throw when a configured provider method does not exist")
    void shouldThrowOnNonExistentProviderMethod() {
        assertThrows(DispatcherResolutionException.class, () ->
                resolver.resolveDispatcher(TestClassWithNonExistentProviderMethod.class,
                        new TestClassWithNonExistentProviderMethod()));
    }

    @Test
    @DisplayName("Should resolve from getModuleDispatcher with context-aware resolution")
    void shouldResolveContextAware() throws NoSuchMethodException {
        Method testMethod = getClass().getDeclaredMethod("shouldResolveContextAware");
        var dispatcher = resolver.resolveDispatcher(TestClassWithMethod.class,
                new TestClassWithMethod(), testMethod);
        assertResponseBody(dispatcher, "/method", "Method Response");
    }

    @Test
    @DisplayName("Should resolve a getModuleDispatcher method inherited from a superclass")
    void shouldResolveInheritedModuleDispatcherMethod() {
        var dispatcher = resolver.resolveDispatcher(ConcreteTestClass.class, new ConcreteTestClass());
        assertResponseBody(dispatcher, "/inherited", "Inherited Response");
    }

    @Test
    @DisplayName("Should resolve a method-level @ModuleDispatcher taking precedence over the class")
    void shouldResolveMethodLevelAnnotation() throws NoSuchMethodException {
        Method testMethod = MethodLevelAnnotationClass.class.getDeclaredMethod("annotatedTest");
        var dispatcher = resolver.resolveDispatcher(MethodLevelAnnotationClass.class,
                new MethodLevelAnnotationClass(), testMethod);
        assertResponseBody(dispatcher, "/method-level", "Method Level Response");
    }

    @Test
    @DisplayName("Should return the raw Dispatcher provided by a provider method")
    void shouldReturnDirectDispatcher() {
        var dispatcher = resolver.resolveDispatcher(TestClassWithDirectDispatcherProvider.class,
                new TestClassWithDirectDispatcherProvider());
        assertInstanceOf(TestDirectDispatcher.class, dispatcher);
    }

    @Test
    @DisplayName("Should reject combining a raw Dispatcher provider with other sources")
    void shouldRejectDirectDispatcherCombinedWithOtherSources() {
        assertThrows(DispatcherResolutionException.class, () ->
                resolver.resolveDispatcher(TestClassWithDirectDispatcherAndMethod.class,
                        new TestClassWithDirectDispatcherAndMethod()));
    }

    @Test
    @DisplayName("Should resolve a bare @ModuleDispatcher via getModuleDispatcher")
    void shouldResolveBareAnnotationViaMethod() {
        var dispatcher = resolver.resolveDispatcher(BareAnnotationClass.class, new BareAnnotationClass());
        assertResponseBody(dispatcher, "/bare", "Bare Response");
    }

    // --- probe helpers -------------------------------------------------------------------------

    private void assertStatus(Dispatcher dispatcher, String path, int expectedStatus) {
        var response = dispatch(dispatcher, path);
        assertTrue(response.getStatus().contains(String.valueOf(expectedStatus)),
                "Expected status " + expectedStatus + " for path " + path + " but was " + response.getStatus());
    }

    private void assertResponseBody(Dispatcher dispatcher, String path, String expectedBody) {
        var response = dispatch(dispatcher, path);
        assertEquals(expectedBody, readBody(response), "Unexpected body for path " + path);
    }

    private static MockResponse dispatch(Dispatcher dispatcher, String path) {
        var request = new RecordedRequest(
                0, 0, null, Collections.emptyList(),
                "GET", path, "HTTP/1.1",
                HttpUrl.parse("http://localhost" + path),
                Headers.of("Host", "localhost"),
                ByteString.EMPTY, 0, Collections.emptyList(), null);
        try {
            return dispatcher.dispatch(request);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Dispatch was interrupted", e);
        }
    }

    private static String readBody(MockResponse response) {
        if (response.getBody() == null) {
            return "";
        }
        var buffer = new Buffer();
        try {
            response.getBody().writeTo(buffer);
            return buffer.readUtf8();
        } catch (java.io.IOException e) {
            throw new IllegalStateException("Failed to read response body", e);
        }
    }

    // --- test fixtures -------------------------------------------------------------------------

    @ModuleDispatcher(FailingConstructorDispatcher.class)
    static class TestClassWithFailingConstructorDispatcher {
    }

    @ModuleDispatcher(provider = TestClassWithFailingProviderMethod.class, providerMethod = "failingProvider")
    static class TestClassWithFailingProviderMethod {
        public static ModuleDispatcherElement failingProvider() {
            throw new IllegalStateException("Provider method failed");
        }
    }

    @ModuleDispatcher(provider = TestClassWithNonStaticProviderMethod.class, providerMethod = "nonStaticProvider")
    static class TestClassWithNonStaticProviderMethod {
        public ModuleDispatcherElement nonStaticProvider() {
            return new TestDispatcherElement("/non-static", "Test Response");
        }
    }

    @ModuleDispatcher(provider = BaseAllAcceptDispatcher.class, providerMethod = "getOptimisticAPIDispatcher")
    static class TestClassWithStaticProviderMethod {
    }

    @ModuleDispatcher(provider = TestClassWithProviderReturningWrongType.class, providerMethod = "wrongTypeProvider")
    static class TestClassWithProviderReturningWrongType {
        public static String wrongTypeProvider() {
            return "Not a dispatcher";
        }
    }

    @ModuleDispatcher(provider = TestDispatcherElement.class)
    static class TestClassWithProviderButNoMethod {
    }

    @ModuleDispatcher(ConflictingDispatcherElement.class)
    static class TestClassWithConflictingDispatchers {
        public ModuleDispatcherElement getModuleDispatcher() {
            return new ConflictingDispatcherElement();
        }
    }

    static class TestClassWithMethod {
        public ModuleDispatcherElement getModuleDispatcher() {
            return new TestDispatcherElement("/method", "Method Response");
        }
    }

    abstract static class AbstractTestClass {
        public ModuleDispatcherElement getModuleDispatcher() {
            return new TestDispatcherElement("/inherited", "Inherited Response");
        }
    }

    static class ConcreteTestClass extends AbstractTestClass {
    }

    static class MethodLevelAnnotationClass {
        @ModuleDispatcher(provider = MethodLevelAnnotationClass.class, providerMethod = "provide")
        void annotatedTest() {
            // annotation carrier only
        }

        public static ModuleDispatcherElement provide() {
            return new TestDispatcherElement("/method-level", "Method Level Response");
        }
    }

    @ModuleDispatcher(providerMethod = "provideDirectDispatcher")
    static class TestClassWithDirectDispatcherProvider {
        public static Dispatcher provideDirectDispatcher() {
            return new TestDirectDispatcher();
        }
    }

    @ModuleDispatcher(providerMethod = "provideDirectDispatcher")
    static class TestClassWithDirectDispatcherAndMethod {
        public static Dispatcher provideDirectDispatcher() {
            return new TestDirectDispatcher();
        }

        public ModuleDispatcherElement getModuleDispatcher() {
            return new TestDispatcherElement("/extra", "Extra Response");
        }
    }

    @ModuleDispatcher
    static class BareAnnotationClass {
        public ModuleDispatcherElement getModuleDispatcher() {
            return new TestDispatcherElement("/bare", "Bare Response");
        }
    }

    @ModuleDispatcher(InaccessibleConstructorDispatcher.class)
    static class TestClassWithInaccessibleConstructorDispatcher {
    }

    static class EmptyTestClass {
    }

    @ModuleDispatcher(providerMethod = "nonExistentMethod")
    static class TestClassWithNonExistentProviderMethod {
    }

    // --- helper dispatcher elements ------------------------------------------------------------

    static class FailingConstructorDispatcher implements ModuleDispatcherElement {
        public FailingConstructorDispatcher() {
            throw new IllegalStateException("Constructor failed");
        }

        @Override
        public String getBaseUrl() {
            return "/failing";
        }

        @Override
        public @NonNull Set<HttpMethodMapper> supportedMethods() {
            return Set.of(HttpMethodMapper.GET);
        }
    }

    static class InaccessibleConstructorDispatcher implements ModuleDispatcherElement {
        private InaccessibleConstructorDispatcher() {
        }

        @Override
        public String getBaseUrl() {
            return "/inaccessible";
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
        public Optional<MockResponse> handleGet(@NonNull RecordedRequest request) {
            return Optional.of(new MockResponse.Builder().code(200).body("Conflict").build());
        }

        @Override
        public @NonNull Set<HttpMethodMapper> supportedMethods() {
            return Set.of(HttpMethodMapper.GET);
        }
    }

    static class TestDispatcherElement implements ModuleDispatcherElement {
        private final String baseUrl;
        private final String body;

        @SuppressWarnings("unused") // required so the class is a valid @ModuleDispatcher value target
        public TestDispatcherElement() {
            this("/", "default");
        }

        public TestDispatcherElement(String baseUrl, String body) {
            this.baseUrl = baseUrl;
            this.body = body;
        }

        @Override
        public String getBaseUrl() {
            return baseUrl;
        }

        @Override
        public Optional<MockResponse> handleGet(@NonNull RecordedRequest request) {
            return Optional.of(new MockResponse.Builder().code(200).body(body).build());
        }

        @Override
        public @NonNull Set<HttpMethodMapper> supportedMethods() {
            return Set.of(HttpMethodMapper.GET);
        }
    }

    static class TestDirectDispatcher extends Dispatcher {
        @Override
        public @NonNull MockResponse dispatch(@NonNull RecordedRequest request) {
            return new MockResponse.Builder().code(200).body("Direct Dispatcher").build();
        }
    }
}
