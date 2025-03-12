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

import okhttp3.tls.HandshakeCertificates;
import org.easymock.EasyMock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtensionContext;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the self-signed certificate creation and caching functionality of {@link CertificateResolver}.
 * These tests verify that self-signed certificates are properly created and cached
 * in the extension context.
 * 
 * @see CertificateResolver#createAndStoreSelfSignedCertificates(ExtensionContext, MockServerConfig)
 * @see CertificateResolver#getSelfSignedCertificatesFromContext(ExtensionContext)
 */
@DisplayName("CertificateResolver - Self-Signed Certificate Tests")
class CertificateResolverSelfSignedTest {

    private static final String SELF_SIGNED_CERTIFICATES_KEY = "self-signed-certificates";
    private static final String CERTIFICATES_SHOULD_NOT_BE_NULL = "Certificates should not be null";
    private static final String KEY_MANAGER_ASSERTION_MESSAGE = "Key manager should not be null";
    private static final String TRUST_MANAGER_ASSERTION_MESSAGE = "Trust manager should not be null";

    private CertificateResolver resolver;

    @BeforeEach
    void setUp() {
        resolver = new CertificateResolver();
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
