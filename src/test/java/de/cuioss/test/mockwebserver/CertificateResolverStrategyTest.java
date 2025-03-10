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
 * Tests for the certificate strategy selection functionality of {@link CertificateResolver}.
 * These tests verify that the resolver correctly prioritizes different certificate
 * sources according to the defined strategy.
 */
@DisplayName("CertificateResolver - Certificate Strategy Selection Tests")
class CertificateResolverStrategyTest {

    private static final String SELF_SIGNED_CERTIFICATES_KEY = "self-signed-certificates";
    private static final String CERTIFICATES_SHOULD_NOT_BE_NULL = "Certificates should not be null";

    private CertificateResolver resolver;

    @BeforeEach
    void setUp() {
        resolver = new CertificateResolver();
    }

    /**
     * Test class with a method that returns HandshakeCertificates.
     */
    @TestProvidedCertificate(methodName = "getCertificates")
    static class TestClassWithMethod {
        public static HandshakeCertificates getCertificates() {
            return KeyMaterialUtil.createSelfSignedHandshakeCertificates(1, KeyAlgorithm.RSA_2048);
        }
    }

    /**
     * Creates a mock ExtensionContext for testing the certificate resolution strategy.
     * 
     * @return a mocked ExtensionContext with store support
     */
    private ExtensionContext createMockContextWithStoreAndTestClass() {
        ExtensionContext mockContext = EasyMock.createMock(ExtensionContext.class);
        ExtensionContext.Store mockStore = EasyMock.createMock(ExtensionContext.Store.class);

        // Setup parent context for getRootContext method
        EasyMock.expect(mockContext.getParent()).andReturn(Optional.empty()).anyTimes();

        // Setup test class behavior
        EasyMock.expect(mockContext.getTestClass()).andReturn(Optional.of(TestClassWithMethod.class)).anyTimes();

        // Setup test method behavior - important for annotation resolution
        EasyMock.expect(mockContext.getTestMethod()).andReturn(Optional.empty()).anyTimes();
        EasyMock.expect(mockContext.getTestInstance()).andReturn(Optional.empty()).anyTimes();

        // Setup store behavior
        EasyMock.expect(mockContext.getStore(MockWebServerExtension.NAMESPACE)).andReturn(mockStore).anyTimes();

        // Create a real store implementation for certificate caching
        final HandshakeCertificates[] storedCertificates = new HandshakeCertificates[1];

        // Setup store get behavior for certificates
        EasyMock.expect(mockStore.get(SELF_SIGNED_CERTIFICATES_KEY, HandshakeCertificates.class))
                .andAnswer(() -> storedCertificates[0])
                .anyTimes();

        // Setup store put behavior for certificates
        mockStore.put(EasyMock.eq(SELF_SIGNED_CERTIFICATES_KEY), EasyMock.anyObject(HandshakeCertificates.class));
        EasyMock.expectLastCall().andAnswer(() -> {
            storedCertificates[0] = (HandshakeCertificates) EasyMock.getCurrentArguments()[1];
            return null;
        }).anyTimes();

        // Setup put behavior
        mockStore.put(EasyMock.anyObject(), EasyMock.anyObject());
        EasyMock.expectLastCall().anyTimes();

        EasyMock.replay(mockContext, mockStore);
        return mockContext;
    }

    @Test
    @DisplayName("Should prioritize annotation-based certificates over self-signed")
    void shouldPrioritizeAnnotationBasedCertificatesOverSelfSigned() {
        // Arrange
        ExtensionContext mockContext = createMockContextWithStoreAndTestClass();
        MockServerConfig config = new MockServerConfig(false, false);

        // Act - First call should create certificates from annotation
        Optional<HandshakeCertificates> result = resolver.getHandshakeCertificates(mockContext, config);

        // Assert
        assertTrue(result.isPresent(), "Should resolve certificates");
        assertNotNull(result.get(), CERTIFICATES_SHOULD_NOT_BE_NULL);

        // Verify that it came from the annotation by checking that it's not in the store
        // (because annotation-based certificates aren't stored in the context)
        Optional<HandshakeCertificates> storedCertificates = resolver.getSelfSignedCertificatesFromContext(mockContext);
        assertFalse(storedCertificates.isPresent(), "Should not store annotation-based certificates in context");
    }
}
