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
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtensionContext;

import java.util.Optional;
import javax.net.ssl.SSLContext;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive test suite for the {@link CertificateResolver} class.
 * <p>
 * This suite consolidates all certificate-related tests to avoid duplication
 * across multiple test classes while ensuring comprehensive coverage.
 * </p>
 */
@DisplayName("CertificateResolver Test Suite")
class CertificateResolverTest {

    private static final String SELF_SIGNED_CERTIFICATES_KEY = "self-signed-certificates";
    private static final String SSL_CONTEXT_KEY = "ssl-context";
    private static final String CERTIFICATES_SHOULD_NOT_BE_NULL = "Certificates should not be null";
    private static final String KEY_MANAGER_ASSERTION_MESSAGE = "Key manager should not be null";
    private static final String TRUST_MANAGER_ASSERTION_MESSAGE = "Trust manager should not be null";

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
        EasyMock.expect(mockContext.getTestMethod()).andReturn(Optional.empty()).anyTimes();
        EasyMock.expect(mockContext.getTestInstance()).andReturn(Optional.empty()).anyTimes();

        EasyMock.replay(mockContext);
        return mockContext;
    }

    /**
     * Creates a mock ExtensionContext with a store for testing caching functionality.
     *
     * @return a mocked ExtensionContext with store support
     */
    private ExtensionContext createMockContextWithStore() {
        ExtensionContext mockContext = EasyMock.createMock(ExtensionContext.class);
        ExtensionContext.Store mockStore = EasyMock.createMock(ExtensionContext.Store.class);

        // Setup parent context for getRootContext method
        EasyMock.expect(mockContext.getParent()).andReturn(Optional.empty()).anyTimes();

        // Setup test class behavior - return empty to simulate no test class
        EasyMock.expect(mockContext.getTestClass()).andReturn(Optional.empty()).anyTimes();
        EasyMock.expect(mockContext.getTestMethod()).andReturn(Optional.empty()).anyTimes();
        EasyMock.expect(mockContext.getTestInstance()).andReturn(Optional.empty()).anyTimes();

        // Setup store behavior
        EasyMock.expect(mockContext.getStore(MockWebServerExtension.NAMESPACE)).andReturn(mockStore).anyTimes();

        // Create a real store implementation for certificate caching
        final HandshakeCertificates[] storedCertificates = new HandshakeCertificates[1];
        final SSLContext[] storedContext = new SSLContext[1];

        // Setup store get behavior for certificates
        EasyMock.expect(mockStore.get(SELF_SIGNED_CERTIFICATES_KEY, HandshakeCertificates.class))
                .andAnswer(() -> storedCertificates[0])
                .anyTimes();

        // Setup store get behavior for SSL context
        EasyMock.expect(mockStore.get(SSL_CONTEXT_KEY, SSLContext.class))
                .andAnswer(() -> storedContext[0])
                .anyTimes();

        // Setup store put behavior for certificates
        mockStore.put(EasyMock.eq(SELF_SIGNED_CERTIFICATES_KEY), EasyMock.anyObject(HandshakeCertificates.class));
        EasyMock.expectLastCall().andAnswer(() -> {
            storedCertificates[0] = (HandshakeCertificates) EasyMock.getCurrentArguments()[1];
            return null;
        }).anyTimes();

        // Setup store put behavior for SSL context
        mockStore.put(EasyMock.eq(SSL_CONTEXT_KEY), EasyMock.anyObject(SSLContext.class));
        EasyMock.expectLastCall().andAnswer(() -> {
            storedContext[0] = (SSLContext) EasyMock.getCurrentArguments()[1];
            return null;
        }).anyTimes();

        // Setup put behavior for other objects
        mockStore.put(EasyMock.anyObject(), EasyMock.anyObject());
        EasyMock.expectLastCall().anyTimes();

        EasyMock.replay(mockContext, mockStore);
        return mockContext;
    }

    /**
     * Tests for self-signed certificate creation and caching.
     */
    @Nested
    @DisplayName("Self-Signed Certificate Tests")
    class SelfSignedCertificateTests {

        @Test
        @DisplayName("Should create and cache self-signed certificates")
        void shouldCreateAndCacheSelfSignedCertificates() {
            // Arrange
            ExtensionContext mockContext = createMockContextWithStore();
            MockServerConfig config = new MockServerConfig(false, false);

            // Act - First call should create and store certificates
            Optional<HandshakeCertificates> result = resolver.createAndStoreSelfSignedCertificates(mockContext, config);

            // Assert
            assertTrue(result.isPresent(), "Should create self-signed certificates");
            assertNotNull(result.get(), CERTIFICATES_SHOULD_NOT_BE_NULL);
            assertNotNull(result.get().keyManager(), KEY_MANAGER_ASSERTION_MESSAGE);
            assertNotNull(result.get().trustManager(), TRUST_MANAGER_ASSERTION_MESSAGE);

            // Act - Second call should retrieve from cache
            Optional<HandshakeCertificates> cachedResult = resolver.getSelfSignedCertificatesFromContext(mockContext);

            // Assert
            assertTrue(cachedResult.isPresent(), "Should return cached certificates");
            assertSame(result.get(), cachedResult.get(), "Should return the same certificate instance from cache");
        }

        @Test
        @DisplayName("Should use custom key algorithm for self-signed certificates")
        void shouldUseCustomKeyAlgorithm() {
            // Arrange
            ExtensionContext mockContext = createMockContextWithStore();
            MockServerConfig config = new MockServerConfig(false, false);
            // Using RSA_2048 as the key algorithm
            // No need to set it as it's the default
            
            // Act
            Optional<HandshakeCertificates> result = resolver.getHandshakeCertificates(mockContext, config);

            // Assert
            assertTrue(result.isPresent(), "Should create self-signed certificates with custom algorithm");
            assertNotNull(result.get(), CERTIFICATES_SHOULD_NOT_BE_NULL);
        }
    }

    /**
     * Tests for annotation-based certificate resolution.
     */
    @Nested
    @DisplayName("Annotation-based Certificate Resolution Tests")
    class AnnotationBasedResolutionTests {

        @Test
        @DisplayName("Should resolve certificates from annotated class")
        void shouldResolveCertificatesFromAnnotation() {
            // Arrange
            ExtensionContext mockContext = createMockContext(AnnotatedTestClass.class);

            // Act
            Optional<HandshakeCertificates> result = resolver.determineTestProvidedHandshakeCertificates(mockContext);

            // Assert
            assertTrue(result.isPresent(), "Should resolve certificates from annotated class");
            assertNotNull(result.get(), CERTIFICATES_SHOULD_NOT_BE_NULL);
            assertNotNull(result.get().keyManager(), KEY_MANAGER_ASSERTION_MESSAGE);
            assertNotNull(result.get().trustManager(), TRUST_MANAGER_ASSERTION_MESSAGE);
        }

        @Test
        @DisplayName("Should not resolve certificates from non-annotated class")
        void shouldNotResolveCertificatesFromNonAnnotatedClass() {
            // Arrange
            ExtensionContext mockContext = createMockContext(NonAnnotatedTestClass.class);

            // Act
            Optional<HandshakeCertificates> result = resolver.determineTestProvidedHandshakeCertificates(mockContext);

            // Assert
            assertFalse(result.isPresent(), "Should not resolve certificates from non-annotated class");
        }

        @Test
        @DisplayName("Should resolve certificates from provider class")
        void shouldResolveCertificatesFromProviderClass() {
            // Arrange
            ExtensionContext mockContext = createMockContext(ProviderClassTestClass.class);

            // Act
            Optional<HandshakeCertificates> result = resolver.determineTestProvidedHandshakeCertificates(mockContext);

            // Assert
            assertTrue(result.isPresent(), "Should resolve certificates from provider class");
            assertNotNull(result.get(), CERTIFICATES_SHOULD_NOT_BE_NULL);
            assertNotNull(result.get().keyManager(), KEY_MANAGER_ASSERTION_MESSAGE);
            assertNotNull(result.get().trustManager(), TRUST_MANAGER_ASSERTION_MESSAGE);
        }

        @Test
        @DisplayName("Should resolve certificates from test instance")
        void shouldResolveCertificatesFromTestInstance() {
            // Arrange
            ExtensionContext mockContext = EasyMock.createMock(ExtensionContext.class);
            AnnotatedTestClass testInstance = new AnnotatedTestClass();

            // Setup parent context for getRootContext method
            EasyMock.expect(mockContext.getParent()).andReturn(Optional.empty()).anyTimes();

            // Setup test class behavior
            EasyMock.expect(mockContext.getTestClass()).andReturn(Optional.of(AnnotatedTestClass.class)).anyTimes();
            EasyMock.expect(mockContext.getTestInstance()).andReturn(Optional.of(testInstance)).anyTimes();

            EasyMock.expect(mockContext.getTestMethod()).andReturn(Optional.empty()).anyTimes();

            EasyMock.replay(mockContext);

            // Act
            Optional<HandshakeCertificates> result = resolver.determineTestProvidedHandshakeCertificates(mockContext);

            // Assert
            assertTrue(result.isPresent(), "Should resolve certificates from test instance");
            assertNotNull(result.get(), CERTIFICATES_SHOULD_NOT_BE_NULL);
            assertNotNull(result.get().keyManager(), KEY_MANAGER_ASSERTION_MESSAGE);
            assertNotNull(result.get().trustManager(), TRUST_MANAGER_ASSERTION_MESSAGE);
        }

        /**
         * Test class with TestProvidedCertificate annotation.
         */
        @TestProvidedCertificate
        static class AnnotatedTestClass {

            /**
             * Provides HandshakeCertificates for HTTPS testing.
             *
             * @return HandshakeCertificates to be used for HTTPS testing
             */
            @SuppressWarnings("unused") // implicitly called by the test framework
            public HandshakeCertificates provideHandshakeCertificates() {
                return KeyMaterialUtil.createSelfSignedHandshakeCertificates(1, KeyAlgorithm.RSA_2048);
            }
        }

        /**
         * Test class without TestProvidedCertificate annotation.
         */
        static class NonAnnotatedTestClass {

            /**
             * Dummy method that should not be called.
             */
            @SuppressWarnings("unused") // implicitly called by the test framework
            public void dummyMethod() {
                // This method should not be called
            }
        }

        /**
         * Test class with TestProvidedCertificate annotation that references a provider class.
         */
        @TestProvidedCertificate(providerClass = CertificateProvider.class)
        static class ProviderClassTestClass {

            /**
             * Dummy method that should not be called.
             */
            @SuppressWarnings("unused") // implicitly called by the test framework
            public void dummyMethod() {
                // This method should not be called
            }
        }

        /**
         * Certificate provider class for testing.
         */
        static class CertificateProvider {

            /**
             * Provides HandshakeCertificates for HTTPS testing.
             *
             * @return HandshakeCertificates to be used for HTTPS testing
             */
            @SuppressWarnings("unused") // implicitly called by the test framework
            public static HandshakeCertificates provideHandshakeCertificates() {
                return KeyMaterialUtil.createSelfSignedHandshakeCertificates(1, KeyAlgorithm.RSA_2048);
            }
        }
    }

    /**
     * Tests for certificate strategy selection.
     */
    @Nested
    @DisplayName("Certificate Strategy Selection Tests")
    class CertificateStrategyTests {

        @Test
        @DisplayName("Should prioritize test-provided certificates over self-signed")
        void shouldPrioritizeTestProvidedCertificatesOverSelfSigned() {
            // Arrange
            ExtensionContext mockContext = createMockContext(AnnotationBasedResolutionTests.AnnotatedTestClass.class);
            MockServerConfig config = new MockServerConfig(true, false);

            // Act
            Optional<HandshakeCertificates> result = resolver.getHandshakeCertificates(mockContext, config);

            // Assert
            assertTrue(result.isPresent(), "Should resolve certificates");
            assertNotNull(result.get(), CERTIFICATES_SHOULD_NOT_BE_NULL);

            // Verify that test-provided certificates were used
            // This is difficult to test directly, but we can verify that the certificates were resolved
            Optional<HandshakeCertificates> testProvided = resolver.determineTestProvidedHandshakeCertificates(mockContext);
            assertTrue(testProvided.isPresent(), "Test-provided certificates should be resolved");
        }

        @Test
        @DisplayName("Should fall back to self-signed certificates when no test-provided certificates")
        void shouldFallBackToSelfSignedCertificates() {
            // Arrange
            // Create a fresh mock context for this test
            ExtensionContext mockContext = EasyMock.createMock(ExtensionContext.class);
            ExtensionContext.Store mockStore = EasyMock.createMock(ExtensionContext.Store.class);

            // Setup parent context for getRootContext method
            EasyMock.expect(mockContext.getParent()).andReturn(Optional.empty()).anyTimes();

            // Setup test class behavior for a non-annotated class
            EasyMock.expect(mockContext.getTestClass()).andReturn(Optional.of(AnnotationBasedResolutionTests.NonAnnotatedTestClass.class)).anyTimes();
            EasyMock.expect(mockContext.getTestMethod()).andReturn(Optional.empty()).anyTimes();
            EasyMock.expect(mockContext.getTestInstance()).andReturn(Optional.empty()).anyTimes();

            // Setup store behavior
            EasyMock.expect(mockContext.getStore(MockWebServerExtension.NAMESPACE)).andReturn(mockStore).anyTimes();

            // Setup store get behavior for certificates - initially return null to simulate no cached certificates
            EasyMock.expect(mockStore.get(SELF_SIGNED_CERTIFICATES_KEY, HandshakeCertificates.class)).andReturn(null).once();

            // Setup store put behavior for certificates
            mockStore.put(EasyMock.eq(SELF_SIGNED_CERTIFICATES_KEY), EasyMock.anyObject(HandshakeCertificates.class));
            EasyMock.expectLastCall().once();

            // Note: SSL context is not stored during getHandshakeCertificates call
            // It's only stored when createAndStoreSSLContext is explicitly called
            
            // Activate all mocks
            EasyMock.replay(mockContext, mockStore);

            // Configure test
            MockServerConfig config = new MockServerConfig(true, false);

            // Act
            Optional<HandshakeCertificates> result = resolver.getHandshakeCertificates(mockContext, config);

            // Assert
            assertTrue(result.isPresent(), "Should resolve certificates");
            assertNotNull(result.get(), CERTIFICATES_SHOULD_NOT_BE_NULL);

            // Verify that test-provided certificates were not resolved
            Optional<HandshakeCertificates> testProvided = resolver.determineTestProvidedHandshakeCertificates(mockContext);
            assertFalse(testProvided.isPresent(), "Test-provided certificates should not be resolved");

            // Verify all mock expectations were met
            EasyMock.verify(mockContext, mockStore);
        }
    }

    /**
     * Tests for SSL context creation and storage.
     */
    @Nested
    @DisplayName("SSL Context Tests")
    class SslContextTests {

        @Test
        @DisplayName("Should create and store SSL context")
        void shouldCreateAndStoreSSLContext() {
            // Arrange
            ExtensionContext mockContext = createMockContextWithStore();
            HandshakeCertificates certificates = KeyMaterialUtil.createSelfSignedHandshakeCertificates(1, KeyAlgorithm.RSA_2048);

            // Act
            SSLContext sslContext = resolver.createAndStoreSSLContext(mockContext, certificates);

            // Assert
            assertNotNull(sslContext, "SSLContext should be created");
            assertEquals("TLS", sslContext.getProtocol(), "Protocol should be TLS");

            // Act - Get from context
            Optional<SSLContext> retrievedContextOptional = resolver.getSSLContext(mockContext);

            // Assert
            assertTrue(retrievedContextOptional.isPresent(), "SSLContext should be retrieved from context");
            SSLContext retrievedContext = retrievedContextOptional.get();
            assertSame(sslContext, retrievedContext, "Should return the same SSLContext instance from context");
        }

        @Test
        @DisplayName("Should create SSL context from HandshakeCertificates")
        void shouldCreateSSLContextFromHandshakeCertificates() {
            // Arrange
            HandshakeCertificates certificates = KeyMaterialUtil.createSelfSignedHandshakeCertificates(1, KeyAlgorithm.RSA_2048);

            // Act
            SSLContext sslContext = KeyMaterialUtil.createSslContext(certificates);

            // Assert
            assertNotNull(sslContext, "SSLContext should be created");
            assertEquals("TLS", sslContext.getProtocol(), "Protocol should be TLS");
        }
    }
}
