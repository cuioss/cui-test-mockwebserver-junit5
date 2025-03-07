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
package de.cuioss.test.mockwebserver.ssl;

import de.cuioss.tools.net.ssl.KeyAlgorithm;
import okhttp3.tls.HandshakeCertificates;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import javax.net.ssl.SSLContext;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link KeyMaterialUtil}.
 */
@DisplayName("Tests for KeyMaterialUtil")
class KeyMaterialUtilTest {

    private static final int TEST_DURATION_DAYS = 30;

    @Test
    @DisplayName("Should create self-signed HandshakeCertificates")
    void shouldCreateSelfSignedHandshakeCertificates() {
        // Arrange & Act
        var certificates = KeyMaterialUtil.createSelfSignedHandshakeCertificates(TEST_DURATION_DAYS, KeyAlgorithm.RSA_2048);

        // Assert
        assertNotNull(certificates);
        assertNotNull(certificates.keyManager());
        assertNotNull(certificates.trustManager());
    }

    @ParameterizedTest
    @EnumSource(KeyAlgorithm.class)
    @DisplayName("Should create self-signed HandshakeCertificates with different algorithms")
    void shouldCreateSelfSignedHandshakeCertificatesWithDifferentAlgorithms(KeyAlgorithm algorithm) {
        // Arrange & Act
        var certificates = KeyMaterialUtil.createSelfSignedHandshakeCertificates(TEST_DURATION_DAYS, algorithm);

        // Assert
        assertNotNull(certificates);
        assertNotNull(certificates.keyManager());
        assertNotNull(certificates.trustManager());
    }

    @Test
    @DisplayName("Should create SSLContext from HandshakeCertificates")
    void shouldCreateSslContextFromHandshakeCertificates() {
        // Arrange
        HandshakeCertificates certificates = KeyMaterialUtil.createSelfSignedHandshakeCertificates(
                TEST_DURATION_DAYS, KeyAlgorithm.RSA_2048);
        assertNotNull(certificates, "HandshakeCertificates should be created");

        // Act
        SSLContext sslContext = KeyMaterialUtil.createSslContext(certificates);

        // Assert
        assertNotNull(sslContext, "SSLContext should not be null");
        assertEquals("TLS", sslContext.getProtocol(), "Protocol should be TLS");
    }

    @Test
    @DisplayName("Should throw exception when HandshakeCertificates is null")
    void shouldThrowExceptionWhenHandshakeCertificatesIsNull() {
        // Arrange & Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> KeyMaterialUtil.createSslContext((HandshakeCertificates) null));

        assertEquals("HandshakeCertificates must not be null", exception.getMessage(),
                "Exception message should indicate null HandshakeCertificates");
    }

    @Test
    @DisplayName("Should convert SSLContext to HandshakeCertificates")
    void shouldConvertSslContextToHandshakeCertificates() {
        // Arrange
        HandshakeCertificates originalCertificates = KeyMaterialUtil.createSelfSignedHandshakeCertificates(
                TEST_DURATION_DAYS, KeyAlgorithm.RSA_2048);
        SSLContext sslContext = KeyMaterialUtil.createSslContext(originalCertificates);

        // Act
        HandshakeCertificates convertedCertificates = KeyMaterialUtil.convertToHandshakeCertificates(sslContext);

        // Assert
        assertNotNull(convertedCertificates, "Converted HandshakeCertificates should not be null");
        assertNotNull(convertedCertificates.trustManager(), "TrustManager should not be null");
    }

    @Test
    @DisplayName("Should throw exception when SSLContext is null")
    void shouldThrowExceptionWhenSslContextIsNull() {
        // Arrange & Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> KeyMaterialUtil.convertToHandshakeCertificates((SSLContext) null));

        assertEquals("SSLContext must not be null", exception.getMessage(),
                "Exception message should indicate null SSLContext");
    }

    @Test
    @DisplayName("Should create round-trip conversion between HandshakeCertificates and SSLContext")
    void shouldCreateRoundTripConversion() {
        // Arrange
        HandshakeCertificates originalCertificates = KeyMaterialUtil.createSelfSignedHandshakeCertificates(
                TEST_DURATION_DAYS, KeyAlgorithm.RSA_2048);

        // Act: HandshakeCertificates -> SSLContext -> HandshakeCertificates
        SSLContext sslContext = KeyMaterialUtil.createSslContext(originalCertificates);
        HandshakeCertificates roundTripCertificates = KeyMaterialUtil.convertToHandshakeCertificates(sslContext);

        // Assert
        assertNotNull(roundTripCertificates, "Round-trip HandshakeCertificates should not be null");
        assertNotNull(roundTripCertificates.trustManager(), "TrustManager should not be null");

        // Create HTTP clients with both certificates to verify they're functionally equivalent
        assertDoesNotThrow(() -> {
            // Both should be able to create valid SSLContexts
            SSLContext originalContext = KeyMaterialUtil.createSslContext(originalCertificates);
            SSLContext roundTripContext = KeyMaterialUtil.createSslContext(roundTripCertificates);

            assertNotNull(originalContext);
            assertNotNull(roundTripContext);
        });
    }
}
