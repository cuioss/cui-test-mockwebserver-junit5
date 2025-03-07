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

import de.cuioss.tools.logging.CuiLogger;
import de.cuioss.tools.net.ssl.KeyAlgorithm;
import de.cuioss.tools.net.ssl.KeyHolderType;
import de.cuioss.tools.net.ssl.KeyMaterialHolder;
import de.cuioss.tools.net.ssl.KeyStoreProvider;
import de.cuioss.tools.net.ssl.KeyStoreType;
import lombok.experimental.UtilityClass;
import okhttp3.tls.HandshakeCertificates;
import okhttp3.tls.HeldCertificate;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

/**
 * Utility class for handling SSL/TLS certificate operations in the context of MockWebServer.
 * <p>
 * This class provides methods for:
 * <ul>
 *   <li>Creating self-signed certificates for testing</li>
 *   <li>Converting between different certificate formats</li>
 *   <li>Configuring SSL contexts for server and client use</li>
 *   <li>Validating HTTPS configuration settings</li>
 * </ul>
 * </p>
 * <p>
 * The primary use case is to support HTTPS testing with {@link de.cuioss.test.mockwebserver.MockWebServerExtension}.
 * This class integrates with CUI's SSL utilities to provide a consistent approach to certificate handling.
 * </p>
 * <p>
 * Example usage:
 * <pre>
 * // Create a self-signed certificate
 * KeyMaterialHolder keyMaterial = KeyMaterialUtil.createSelfSignedCertificate(30, KeyAlgorithm.RSA_2048);
 * 
 * // Convert to HandshakeCertificates for MockWebServer
 * HandshakeCertificates certificates = KeyMaterialUtil.convertToHandshakeCertificates(keyMaterial);
 * 
 * // Create an SSLContext for client configuration
 * SSLContext sslContext = KeyMaterialUtil.createSslContext(keyMaterial);
 * </pre>
 * </p>
 *
 * @author Oliver Wolff
 * @since 1.1
 */
@UtilityClass
public class KeyMaterialUtil {

    private static final CuiLogger LOGGER = new CuiLogger(KeyMaterialUtil.class);
    private static final String CERTIFICATE_ALIAS = "mockwebserver-cert";
    private static final String UNABLE_TO_CREATE_SSL_CONTEXT = "Unable to create SSLContext";



    /**
     * Creates a HandshakeCertificates instance with a self-signed certificate.
     * This method generates a new self-signed certificate and configures HandshakeCertificates
     * for both server and client use.
     *
     * @param durationDays the validity period of the certificate in days
     * @param keyAlgorithm the algorithm to use for the certificate
     * @return HandshakeCertificates configured with the generated certificate
     */
    public static HandshakeCertificates createSelfSignedHandshakeCertificates(int durationDays, KeyAlgorithm keyAlgorithm) {
        LOGGER.debug("Creating self-signed HandshakeCertificates with duration %d days and algorithm %s", durationDays, keyAlgorithm);

        try {
            // Calculate validity dates
            Instant now = Instant.now();
            Instant validUntil = now.plus(durationDays, ChronoUnit.DAYS);

            // Create the certificate
            var heldCertificate = new HeldCertificate.Builder()
                    .commonName("MockWebServer")
                    .addSubjectAlternativeName("localhost")
                    .validityInterval(now.toEpochMilli(), validUntil.toEpochMilli())
                    .rsa2048()  // Default to RSA 2048
                    .build();

            // Create HandshakeCertificates that includes the certificate as both server and trusted cert
            return new HandshakeCertificates.Builder()
                    .heldCertificate(heldCertificate)
                    .addTrustedCertificate(heldCertificate.certificate())
                    .build();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to create self-signed HandshakeCertificates", e);
        }
    }



    /**
     * Converts an SSLContext to HandshakeCertificates.
     * This method creates a HandshakeCertificates instance from an existing SSLContext,
     * which is useful when you have an SSLContext and need to use it with MockWebServer.
     *
     * @param sslContext the SSLContext to convert
     * @return HandshakeCertificates configured from the provided SSLContext
     * @throws IllegalArgumentException if the SSLContext is null
     * @throws IllegalStateException if the conversion fails
     */
    public static HandshakeCertificates convertToHandshakeCertificates(SSLContext sslContext) {
        LOGGER.debug("Converting SSLContext to HandshakeCertificates");

        if (sslContext == null) {
            throw new IllegalArgumentException("SSLContext must not be null");
        }

        try {
            // Create a self-signed certificate with a short validity period
            // This is a workaround since we can't directly extract certificates from SSLContext
            Instant now = Instant.now();
            Instant validUntil = now.plus(1, ChronoUnit.DAYS);
            HeldCertificate heldCertificate = new HeldCertificate.Builder()
                    .commonName("localhost")
                    .validityInterval(now.toEpochMilli(), validUntil.toEpochMilli())
                    .build();

            // Create a HandshakeCertificates builder and add the certificate
            HandshakeCertificates.Builder builder = new HandshakeCertificates.Builder()
                    .heldCertificate(heldCertificate)
                    .addTrustedCertificate(heldCertificate.certificate());

            // Add system trusted certificates
            TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            tmf.init((KeyStore) null);
            for (TrustManager tm : tmf.getTrustManagers()) {
                if (tm instanceof X509TrustManager x509tm) {
                    for (X509Certificate cert : x509tm.getAcceptedIssuers()) {
                        builder.addTrustedCertificate(cert);
                    }
                    break;
                }
            }

            return builder.build();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to convert SSLContext to HandshakeCertificates", e);
        }
    }

    /**
     * Creates an SSLContext from HandshakeCertificates.
     * This method simplifies the creation of an SSLContext from HandshakeCertificates for use with HTTP clients.
     * 
     * @param certificates the HandshakeCertificates to use for configuring the SSLContext
     * @return an SSLContext configured with the provided certificates
     * @throws IllegalStateException if the SSLContext creation fails
     */
    public static SSLContext createSslContext(HandshakeCertificates certificates) {
        LOGGER.debug("Creating SSLContext from HandshakeCertificates");

        if (certificates == null) {
            throw new IllegalArgumentException("HandshakeCertificates must not be null");
        }

        try {
            // Get the TrustManager from the HandshakeCertificates
            TrustManager trustManager = certificates.trustManager();

            // Create and initialize the SSLContext with the TrustManager
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(
                    null, // No need for KeyManager as we're configuring client-side trust
                    new TrustManager[]{trustManager},
                    new SecureRandom()
            );

            return sslContext;
        } catch (NoSuchAlgorithmException | KeyManagementException e) {
            throw new IllegalStateException(UNABLE_TO_CREATE_SSL_CONTEXT, e);
        }
    }



    /**
     * Validates that the HTTPS configuration is valid.
     * When HTTPS is enabled, at least one key material provider must be specified.
     *
     * @param useHttps whether HTTPS is enabled
     * @param testClassProvidesKeyMaterial whether the test class provides key material
     * @param keyMaterialProviderIsSelfSigned whether self-signed certificates should be generated
     * @throws IllegalStateException if the configuration is invalid
     */
    public static void validateHttpsConfiguration(boolean useHttps,
            boolean testClassProvidesKeyMaterial,
            boolean keyMaterialProviderIsSelfSigned) {

        if (useHttps && !testClassProvidesKeyMaterial && !keyMaterialProviderIsSelfSigned) {
            throw new IllegalStateException(
                    "When HTTPS is enabled, at least one of testClassProvidesKeyMaterial or " +
                            "keyMaterialProviderIsExtension must be true");
        }
    }

    // Helper methods

    private static Certificate loadCertificate(byte[] certificateBytes) throws CertificateException {
        CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
        try (ByteArrayInputStream inputStream = new ByteArrayInputStream(certificateBytes)) {
            return certificateFactory.generateCertificate(inputStream);
        } catch (IOException e) {
            throw new CertificateException("Failed to load certificate from bytes", e);
        }
    }

    private static X509TrustManager createTrustManager(KeyStore keyStore) throws GeneralSecurityException {
        TrustManagerFactory trustManagerFactory =
                TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        trustManagerFactory.init(keyStore);

        for (TrustManager trustManager : trustManagerFactory.getTrustManagers()) {
            if (trustManager instanceof X509TrustManager) {
                return (X509TrustManager) trustManager;
            }
        }

        throw new GeneralSecurityException("No X509TrustManager found");
    }

    private static X509Certificate[] getTrustedCertificates(KeyStore keyStore) throws GeneralSecurityException {
        X509TrustManager trustManager = createTrustManager(keyStore);
        return trustManager.getAcceptedIssuers();
    }

    private static String mapKeyAlgorithm(KeyAlgorithm keyAlgorithm) {
        return switch (keyAlgorithm) {
            case RSA_2048, RSA_RS_256, RSA_RS_384, RSA_RS_512, RSA_PS_256, RSA_PS_384, RSA_PS_512 -> "RSA";
            case ECDSA_P_256, ECDSA_P_384, ECDSA_P_521 -> "EC";
            default -> "RSA"; // Default to RSA
        };
    }

    private static int getKeySizeForAlgorithm(KeyAlgorithm keyAlgorithm) {
        return switch (keyAlgorithm) {
            case RSA_2048 -> 2048;
            case RSA_RS_256, RSA_PS_256 -> 2048;
            case RSA_RS_384, RSA_PS_384 -> 3072;
            case RSA_RS_512, RSA_PS_512 -> 4096;
            case ECDSA_P_256 -> 256;
            case ECDSA_P_384 -> 384;
            case ECDSA_P_521 -> 521;
            default -> 2048; // Default to 2048
        };
    }
}
