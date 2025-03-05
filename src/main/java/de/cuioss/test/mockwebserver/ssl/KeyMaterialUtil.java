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

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Optional;

/**
 * Utility class for handling SSL/TLS certificate operations in the context of MockWebServer.
 * Provides methods for creating self-signed certificates, converting between different certificate
 * formats, and configuring SSL contexts.
 *
 * @author Oliver Wolff
 */
@UtilityClass
public class KeyMaterialUtil {

    private static final CuiLogger LOGGER = new CuiLogger(KeyMaterialUtil.class);
    private static final String CERTIFICATE_ALIAS = "mockwebserver-cert";
    private static final String UNABLE_TO_CREATE_SSL_CONTEXT = "Unable to create SSLContext";

    /**
     * Creates a self-signed certificate for use with MockWebServer HTTPS.
     *
     * @param durationDays the validity period of the certificate in days
     * @param keyAlgorithm the algorithm to use for the certificate
     * @return a KeyMaterialHolder containing the generated certificate
     */
    public static KeyMaterialHolder createSelfSignedCertificate(int durationDays, KeyAlgorithm keyAlgorithm) {
        LOGGER.debug("Creating self-signed certificate with duration %d days and algorithm %s", durationDays, keyAlgorithm);
        
        try {
            // Convert KeyAlgorithm to appropriate parameters for HeldCertificate.Builder
            String algorithm = mapKeyAlgorithm(keyAlgorithm);
            int keySize = getKeySizeForAlgorithm(keyAlgorithm);
            
            // Calculate validity dates
            Instant now = Instant.now();
            Instant validUntil = now.plus(durationDays, ChronoUnit.DAYS);
            
            // Create the certificate
            var heldCertificate = new HeldCertificate.Builder()
                    .commonName("MockWebServer")
                    .validityInterval(now.toEpochMilli(), validUntil.toEpochMilli())
                    .rsa2048()  // Default to RSA 2048 regardless of algorithm for now
                    .build();
            
            // Convert to KeyMaterialHolder
            byte[] certificateBytes = heldCertificate.certificate().getEncoded();
            
            return KeyMaterialHolder.builder()
                    .keyMaterial(certificateBytes)
                    .keyHolderType(KeyHolderType.SINGLE_KEY)
                    .keyAlgorithm(keyAlgorithm)
                    .keyAlias(CERTIFICATE_ALIAS)
                    .name("MockWebServer Self-Signed Certificate")
                    .description("Auto-generated self-signed certificate for MockWebServer HTTPS testing")
                    .build();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to create self-signed certificate", e);
        }
    }

    /**
     * Converts a KeyMaterialHolder to HandshakeCertificates for use with MockWebServer.
     *
     * @param keyMaterial the key material to convert
     * @return HandshakeCertificates configured with the provided key material
     * @throws IllegalStateException if the conversion fails
     */
    public static HandshakeCertificates convertToHandshakeCertificates(KeyMaterialHolder keyMaterial) {
        LOGGER.debug("Converting KeyMaterialHolder to HandshakeCertificates: %s", keyMaterial);
        
        try {
            if (keyMaterial.getKeyHolderType() == KeyHolderType.SINGLE_KEY) {
                // For a single certificate
                Certificate certificate = loadCertificate(keyMaterial.getKeyMaterial());
                
                return new HandshakeCertificates.Builder()
                        .addTrustedCertificate((X509Certificate) certificate)
                        .build();
            } else {
                // For a keystore
                KeyStoreProvider keyStoreProvider = KeyStoreProvider.builder()
                        .keyStoreType(KeyStoreType.KEY_STORE)
                        .key(keyMaterial)
                        .build();
                
                Optional<KeyStore> keyStoreOptional = keyStoreProvider.resolveKeyStore();
                if (keyStoreOptional.isEmpty()) {
                    throw new IllegalStateException("Failed to resolve KeyStore from KeyMaterialHolder");
                }
                
                KeyStore keyStore = keyStoreOptional.get();
                X509TrustManager trustManager = createTrustManager(keyStore);
                
                // Add each certificate individually
                HandshakeCertificates.Builder builder = new HandshakeCertificates.Builder();
                for (X509Certificate cert : trustManager.getAcceptedIssuers()) {
                    builder.addTrustedCertificate(cert);
                }
                
                return builder.build();
            }
        } catch (Exception e) {
            throw new IllegalStateException("Failed to convert KeyMaterialHolder to HandshakeCertificates", e);
        }
    }

    /**
     * Creates an SSLContext from the provided KeyMaterialHolder.
     * This can be used to configure HTTP clients for connecting to the MockWebServer.
     *
     * @param keyMaterial the key material to use
     * @return an SSLContext configured with the provided key material
     * @throws IllegalStateException if the SSLContext creation fails
     */
    public static SSLContext createSslContext(KeyMaterialHolder keyMaterial) {
        LOGGER.debug("Creating SSLContext from KeyMaterialHolder: %s", keyMaterial);
        
        try {
            KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
            keyStore.load(null, null);
            
            if (keyMaterial.getKeyHolderType() == KeyHolderType.SINGLE_KEY) {
                // For a single certificate
                Certificate certificate = loadCertificate(keyMaterial.getKeyMaterial());
                keyStore.setCertificateEntry(keyMaterial.getKeyAlias() != null ? 
                        keyMaterial.getKeyAlias() : CERTIFICATE_ALIAS, certificate);
            } else {
                // For a keystore
                KeyStoreProvider keyStoreProvider = KeyStoreProvider.builder()
                        .keyStoreType(KeyStoreType.KEY_STORE)
                        .key(keyMaterial)
                        .build();
                
                Optional<KeyStore> keyStoreOptional = keyStoreProvider.resolveKeyStore();
                if (keyStoreOptional.isPresent()) {
                    keyStore = keyStoreOptional.get();
                } else {
                    throw new IllegalStateException("Failed to resolve KeyStore from KeyMaterialHolder");
                }
            }
            
            TrustManagerFactory trustManagerFactory = 
                    TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            trustManagerFactory.init(keyStore);
            
            KeyManagerFactory keyManagerFactory = 
                    KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            keyManagerFactory.init(keyStore, new char[0]);
            
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(keyManagerFactory.getKeyManagers(), 
                    trustManagerFactory.getTrustManagers(), null);
            
            return sslContext;
        } catch (Exception e) {
            throw new IllegalStateException(UNABLE_TO_CREATE_SSL_CONTEXT, e);
        }
    }

    /**
     * Validates that the HTTPS configuration is valid.
     * When HTTPS is enabled, at least one key material provider must be specified.
     *
     * @param useHttps whether HTTPS is enabled
     * @param keyMaterialProviderIsTestClass whether the test class provides key material
     * @param keyMaterialProviderIsSelfSigned whether self-signed certificates should be generated
     * @throws IllegalStateException if the configuration is invalid
     */
    public static void validateHttpsConfiguration(boolean useHttps, 
            boolean keyMaterialProviderIsTestClass, 
            boolean keyMaterialProviderIsSelfSigned) {
        
        if (useHttps && !keyMaterialProviderIsTestClass && !keyMaterialProviderIsSelfSigned) {
            throw new IllegalStateException(
                    "When HTTPS is enabled, at least one of keyMaterialProviderIsTestClass or " +
                    "keyMaterialProviderIsSelfSigned must be true");
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
        switch (keyAlgorithm) {
            case RSA_2048:
            case RSA_RS_256:
            case RSA_RS_384:
            case RSA_RS_512:
            case RSA_PS_256:
            case RSA_PS_384:
            case RSA_PS_512:
                return "RSA";
            case ECDSA_P_256:
            case ECDSA_P_384:
            case ECDSA_P_521:
                return "EC";
            default:
                return "RSA"; // Default to RSA
        }
    }

    private static int getKeySizeForAlgorithm(KeyAlgorithm keyAlgorithm) {
        switch (keyAlgorithm) {
            case RSA_2048:
                return 2048;
            case RSA_RS_256:
            case RSA_PS_256:
                return 2048;
            case RSA_RS_384:
            case RSA_PS_384:
                return 3072;
            case RSA_RS_512:
            case RSA_PS_512:
                return 4096;
            case ECDSA_P_256:
                return 256;
            case ECDSA_P_384:
                return 384;
            case ECDSA_P_521:
                return 521;
            default:
                return 2048; // Default to 2048
        }
    }
}
