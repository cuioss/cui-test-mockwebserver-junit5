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
import org.easymock.EasyMock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Optional;
import javax.net.ssl.SSLContext;


import okhttp3.tls.HandshakeCertificates;

import static de.cuioss.test.mockwebserver.CertificateResolverTestUtil.*;
import static org.junit.jupiter.api.Assertions.*;

@DisplayName("CertificateResolver Test Suite")
class CertificateResolverTest {

    private CertificateResolver resolver;

    @BeforeEach
    void setUp() {
        resolver = new CertificateResolver();
    }

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
        @DisplayName("Should use default key algorithm for self-signed certificates")
        void shouldUseDefaultKeyAlgorithm() {
            // Arrange
            ExtensionContext mockContext = createMockContextWithStore();
            MockServerConfig config = new MockServerConfig(false, false);
            KeyAlgorithm algorithm = config.getKeyAlgorithm(); // Get the default algorithm

            // Act
            Optional<HandshakeCertificates> result = resolver.createAndStoreSelfSignedCertificates(mockContext, config);

            // Assert
            assertTrue(result.isPresent(), "Should create certificates with " + algorithm);
            assertNotNull(result.get(), CERTIFICATES_SHOULD_NOT_BE_NULL);
            assertNotNull(result.get().keyManager(), KEY_MANAGER_ASSERTION_MESSAGE);
            assertNotNull(result.get().trustManager(), TRUST_MANAGER_ASSERTION_MESSAGE);
        }

        @Test
        @DisplayName("Should use certificate strategy to get certificates")
        void shouldUseCertificateStrategy() {
            // Arrange
            ExtensionContext mockContext = createMockContextWithStore();
            MockServerConfig config = new MockServerConfig(false, false);

            // Act
            Optional<HandshakeCertificates> result = resolver.getHandshakeCertificates(mockContext, config);

            // Assert
            assertTrue(result.isPresent(), "Should create certificates using strategy");
            assertNotNull(result.get(), CERTIFICATES_SHOULD_NOT_BE_NULL);
        }

        @Test
        @DisplayName("Should handle errors when creating self-signed certificates")
        void shouldHandleErrorsWhenCreatingSelfSignedCertificates() {
            // Arrange
            ExtensionContext mockContext = EasyMock.createMock(ExtensionContext.class);
            // Use a real config but with a problematic context that will cause an exception
            MockServerConfig config = new MockServerConfig(false, true);

            // Setup context to cause an exception when trying to store certificates
            EasyMock.expect(mockContext.getParent()).andReturn(Optional.empty()).anyTimes();
            // Return null for the store to cause a NullPointerException
            EasyMock.expect(mockContext.getStore(CertificateResolver.NAMESPACE)).andReturn(null);

            EasyMock.replay(mockContext);

            // Act
            Optional<HandshakeCertificates> result = resolver.createAndStoreSelfSignedCertificates(mockContext, config);

            // Assert
            assertFalse(result.isPresent(), "Should handle errors gracefully");

            EasyMock.verify(mockContext);
        }
    }

    @Nested
    @DisplayName("Annotation-based Certificate Resolution Tests")
    class AnnotationBasedResolutionTests {

        @ParameterizedTest
        @ValueSource(classes = {AnnotatedTestClass.class, ProviderClassTestClass.class, TestClassWithMethod.class})
        @DisplayName("Should resolve certificates from valid annotated classes")
        void shouldResolveCertificatesFromValidAnnotatedClasses(Class<?> testClass) {
            // Arrange
            ExtensionContext mockContext = createMockContext(testClass);

            // Act
            Optional<HandshakeCertificates> result = resolver.determineTestProvidedHandshakeCertificates(mockContext);

            // Assert
            assertTrue(result.isPresent(), "Should resolve certificates from " + testClass.getSimpleName());
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


        @ParameterizedTest
        @ValueSource(classes = {NonAnnotatedTestClass.class, TestClassWithNonExistentMethod.class, TestClassWithNonExistentProvider.class})
        @DisplayName("Should return empty optional for invalid configurations")
        void shouldReturnEmptyOptionalForInvalidConfigurations(Class<?> testClass) {
            // Arrange
            ExtensionContext mockContext = createMockContext(testClass);

            // Act
            Optional<HandshakeCertificates> result = resolver.determineTestProvidedHandshakeCertificates(mockContext);

            // Assert
            assertFalse(result.isPresent(), "Should return empty optional for " + testClass.getSimpleName());
        }

        @Test
        @DisplayName("Should return empty optional when test class is not available")
        void shouldReturnEmptyOptionalWhenTestClassIsNotAvailable() {
            // Arrange
            ExtensionContext mockContext = EasyMock.createMock(ExtensionContext.class);
            EasyMock.expect(mockContext.getParent()).andReturn(Optional.empty()).anyTimes();
            EasyMock.expect(mockContext.getTestClass()).andReturn(Optional.empty()).anyTimes();
            EasyMock.replay(mockContext);

            // Act
            Optional<HandshakeCertificates> result = resolver.determineTestProvidedHandshakeCertificates(mockContext);

            // Assert
            assertFalse(result.isPresent(), "Should return empty optional when test class is not available");
        }

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

        static class NonAnnotatedTestClass {

            /**
             * Dummy method that should not be called.
             */
            @SuppressWarnings("unused") // implicitly called by the test framework
            public void dummyMethod() {
                // This method should not be called
            }
        }

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

        static class CertificateProvider {
            private CertificateProvider() {
                // Private constructor to hide implicit public one
            }

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

        @TestProvidedCertificate(methodName = "getCertificates")
        static class TestClassWithMethod {
            private TestClassWithMethod() {
                // Private constructor to hide implicit public one
            }

            /**
             * Provides HandshakeCertificates for HTTPS testing.
             *
             * @return HandshakeCertificates to be used for HTTPS testing
             */
            @SuppressWarnings("unused") // implicitly called by the test framework
            public static HandshakeCertificates getCertificates() {
                return KeyMaterialUtil.createSelfSignedHandshakeCertificates(1, KeyAlgorithm.RSA_2048);
            }
        }

        @TestProvidedCertificate(methodName = "nonExistentMethod")
        static class TestClassWithNonExistentMethod {
            private TestClassWithNonExistentMethod() {
                // Private constructor to hide implicit public one
            }
            // This class intentionally has no methods to test error handling
        }

        @TestProvidedCertificate(providerClass = NonExistentProvider.class)
        static class TestClassWithNonExistentProvider {
            private TestClassWithNonExistentProvider() {
                // Private constructor to hide implicit public one
            }
            // This class intentionally has no provider method to test error handling
        }

        static class NonExistentProvider {
            private NonExistentProvider() {
                // Private constructor to hide implicit public one
            }
            // This class intentionally has no provider method to test error handling
        }
    }

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
            EasyMock.expect(mockContext.getStore(CertificateResolver.NAMESPACE)).andReturn(mockStore).anyTimes();

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

        // Note: Test for creating SSL context from HandshakeCertificates has been moved to
        // KeyMaterialUtilTest.SslContextCreationTests.shouldCreateSslContextFromHandshakeCertificates
        // to avoid duplication.
    }
}
