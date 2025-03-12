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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtensionContext;

import java.util.Optional;
import javax.net.ssl.SSLContext;


import okhttp3.tls.HandshakeCertificates;

import static de.cuioss.test.mockwebserver.CertificateResolverTestUtil.createMockContextWithStore;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the SSL context creation and storage functionality of {@link CertificateResolver}.
 */
@DisplayName("CertificateResolver - SSLContext Tests")
class CertificateResolverSslContextTest {

    private CertificateResolver resolver;

    @BeforeEach
    void setUp() {
        resolver = new CertificateResolver();
    }

    @Test
    @DisplayName("Should create and store SSLContext")
    void shouldCreateAndStoreSSLContext() {
        // Arrange
        ExtensionContext mockContext = createMockContextWithStore();
        HandshakeCertificates certificates = KeyMaterialUtil.createSelfSignedHandshakeCertificates(1, KeyAlgorithm.RSA_2048);

        // Act
        SSLContext sslContext = resolver.createAndStoreSSLContext(mockContext, certificates);

        // Assert
        assertNotNull(sslContext, "Should create SSLContext");

        // Verify it was stored correctly
        Optional<SSLContext> retrievedContext = resolver.getSSLContext(mockContext);
        assertTrue(retrievedContext.isPresent(), "Should retrieve stored SSLContext");
        assertSame(sslContext, retrievedContext.get(), "Should retrieve the same SSLContext instance");
    }

    @Test
    @DisplayName("Should return empty optional when no SSLContext is stored")
    void shouldReturnEmptyOptionalWhenNoSslContextStored() {
        // Arrange
        ExtensionContext mockContext = createMockContextWithStore();

        // Act
        Optional<SSLContext> result = resolver.getSSLContext(mockContext);

        // Assert
        assertFalse(result.isPresent(), "Should return empty optional when no SSLContext is stored");
    }
}
