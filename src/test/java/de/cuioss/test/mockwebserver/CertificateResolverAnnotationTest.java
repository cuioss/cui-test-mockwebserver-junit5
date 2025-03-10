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

import de.cuioss.test.mockwebserver.ssl.KeyMaterialUtil;
import de.cuioss.tools.net.ssl.KeyAlgorithm;
import okhttp3.tls.HandshakeCertificates;
import org.easymock.EasyMock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtensionContext;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the annotation-based certificate resolution functionality of {@link CertificateResolver}.
 * These tests verify that certificates can be properly resolved from test classes
 * using the {@link TestProvidedCertificate} annotation.
 */
@DisplayName("CertificateResolver - Annotation-based Certificate Resolution Tests")
class CertificateResolverAnnotationTest {

    private CertificateResolver resolver;

    @BeforeEach
    void setUp() {
        resolver = new CertificateResolver();
    }

    /**
     * Creates a mock ExtensionContext for testing purposes.
     *
     * @param testClass the test class to be returned by the mock
     * @return a mocked ExtensionContext
     */
    private ExtensionContext createMockContext(Class<?> testClass) {
        ExtensionContext mockContext = EasyMock.createMock(ExtensionContext.class);

        // Setup parent context for getRootContext method
        EasyMock.expect(mockContext.getParent()).andReturn(Optional.empty()).anyTimes();

        // Setup test class behavior
        EasyMock.expect(mockContext.getTestClass()).andReturn(Optional.of(testClass)).anyTimes();

        // Setup test method behavior - important for annotation resolution
        EasyMock.expect(mockContext.getTestMethod()).andReturn(Optional.empty()).anyTimes();
        EasyMock.expect(mockContext.getTestInstance()).andReturn(Optional.empty()).anyTimes();

        // Setup store behavior
        ExtensionContext.Store mockStore = EasyMock.createMock(ExtensionContext.Store.class);
        EasyMock.expect(mockContext.getStore(MockWebServerExtension.NAMESPACE)).andReturn(mockStore).anyTimes();

        EasyMock.replay(mockContext, mockStore);
        return mockContext;
    }

    /**
     * Test class with a method that returns HandshakeCertificates.
     */
    @TestProvidedCertificate(methodName = "getCertificates")
    static class TestClassWithMethod {

        /**
         * Private constructor to prevent instantiation.
         */
        private TestClassWithMethod() {
            // Utility class should not be instantiated
        }

        public static HandshakeCertificates getCertificates() {
            return KeyMaterialUtil.createSelfSignedHandshakeCertificates(1, KeyAlgorithm.RSA_2048);
        }
    }

    /**
     * Test class with a provider class.
     */
    @TestProvidedCertificate(providerClass = CertificateProvider.class)
    static class TestClassWithProvider {

        /**
         * Private constructor to prevent instantiation.
         */
        private TestClassWithProvider() {
            // Utility class should not be instantiated
        }
    }

    /**
     * Certificate provider class.
     */
    static class CertificateProvider {
        /**
         * Private constructor to prevent instantiation.
         */
        private CertificateProvider() {
            // Utility class should not be instantiated
        }

        public static HandshakeCertificates provideHandshakeCertificates() {
            return KeyMaterialUtil.createSelfSignedHandshakeCertificates(1, KeyAlgorithm.RSA_2048);
        }
    }

    /**
     * Test class with no annotation.
     */
    static class TestClassWithoutAnnotation {

        /**
         * Private constructor to prevent instantiation.
         */
        private TestClassWithoutAnnotation() {
            // Utility class should not be instantiated
        }
    }

    @Test
    @DisplayName("Should resolve certificates from test class method")
    void shouldResolveCertificatesFromTestClassMethod() {
        // Arrange
        ExtensionContext mockContext = createMockContext(TestClassWithMethod.class);

        // Act
        Optional<HandshakeCertificates> result = resolver.determineTestProvidedHandshakeCertificates(mockContext);

        // Assert
        assertTrue(result.isPresent(), "Should resolve certificates from test class method");
        assertNotNull(result.get(), "Certificates should not be null");
        assertNotNull(result.get().keyManager(), "Key manager should not be null");
        assertNotNull(result.get().trustManager(), "Trust manager should not be null");
    }

    @Test
    @DisplayName("Should resolve certificates from provider class")
    void shouldResolveCertificatesFromProviderClass() {
        // Arrange
        ExtensionContext mockContext = createMockContext(TestClassWithProvider.class);

        // Act
        Optional<HandshakeCertificates> result = resolver.determineTestProvidedHandshakeCertificates(mockContext);

        // Assert
        assertTrue(result.isPresent(), "Should resolve certificates from provider class");
        assertNotNull(result.get(), "Certificates should not be null");
        assertNotNull(result.get().keyManager(), "Key manager should not be null");
        assertNotNull(result.get().trustManager(), "Trust manager should not be null");
    }

    @Test
    @DisplayName("Should return empty optional when no annotation is present")
    void shouldReturnEmptyOptionalWhenNoAnnotationIsPresent() {
        // Arrange
        ExtensionContext mockContext = createMockContext(TestClassWithoutAnnotation.class);

        // Act
        Optional<HandshakeCertificates> result = resolver.determineTestProvidedHandshakeCertificates(mockContext);

        // Assert
        assertFalse(result.isPresent(), "Should return empty optional when no annotation is present");
    }

    @Test
    @DisplayName("Should return empty optional when test class is not available")
    void shouldReturnEmptyOptionalWhenTestClassIsNotAvailable() {
        // Arrange
        ExtensionContext mockContext = EasyMock.createMock(ExtensionContext.class);

        // Setup parent context for getRootContext method
        EasyMock.expect(mockContext.getParent()).andReturn(Optional.empty()).anyTimes();

        // Setup test class behavior - return empty to simulate no test class
        EasyMock.expect(mockContext.getTestClass()).andReturn(Optional.empty()).anyTimes();

        EasyMock.replay(mockContext);

        // Act
        Optional<HandshakeCertificates> result = resolver.determineTestProvidedHandshakeCertificates(mockContext);

        // Assert
        assertFalse(result.isPresent(), "Should return empty optional when test class is not available");
    }
}
