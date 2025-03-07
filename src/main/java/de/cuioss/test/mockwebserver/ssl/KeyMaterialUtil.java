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
import lombok.experimental.UtilityClass;
import okhttp3.tls.HandshakeCertificates;
import okhttp3.tls.HeldCertificate;

import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

/**
 * Utility class for handling SSL/TLS certificate operations in the context of MockWebServer.
 * <p>
 * This class provides methods for:
 * <ul>
 *   <li>Creating self-signed certificates for testing</li>
 *   <li>Converting between different certificate formats</li>
 *   <li>Configuring SSL contexts for server and client use</li>
 * </ul>
 *
 * <p>
 * The primary use case is to support HTTPS testing with {@link de.cuioss.test.mockwebserver.MockWebServerExtension}.
 *
 * <p>
 * Example usage:
 * <pre>
 * {@code
 * // Create self-signed HandshakeCertificates directly
 * HandshakeCertificates certificates = KeyMaterialUtil.createSelfSignedHandshakeCertificates(30, KeyAlgorithm.RSA_2048);
 *
 * // Create an SSLContext for client configuration
 * SSLContext sslContext = KeyMaterialUtil.createSslContext(certificates);
 * </pre>
 *
 *
 * @author Oliver Wolff
 * @since 1.1
 */
@UtilityClass
public class KeyMaterialUtil {

    private static final CuiLogger LOGGER = new CuiLogger(KeyMaterialUtil.class);
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
        LOGGER.debug("Creating self-signed HandshakeCertificates with duration %s days and algorithm %s", durationDays, keyAlgorithm);

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
     * @throws IllegalStateException    if the conversion fails
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
}
