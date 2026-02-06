/*
 * Copyright © 2025 CUI-OpenSource-Software (info@cuioss.de)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.cuioss.test.mockwebserver.https;

import de.cuioss.test.mockwebserver.ssl.KeyMaterialUtil;
import de.cuioss.tools.net.ssl.KeyAlgorithm;

import okhttp3.tls.HandshakeCertificates;

/**
 * Certificate provider class that demonstrates how to create a reusable certificate provider.
 * This class can be referenced by multiple test classes to share certificate creation logic.
 */
public class TestCertificateProvider {

    private static HandshakeCertificates certificates;

    /**
     * Private constructor to prevent instantiation.
     */
    private TestCertificateProvider() {
        // Utility class should not be instantiated
    }

    /**
     * Provides HandshakeCertificates for HTTPS testing.
     * This method will be called by the CertificateResolver when resolving certificates.
     *
     * @return HandshakeCertificates to be used for HTTPS testing
     */
    @SuppressWarnings("unused") // implicitly called by the test framework
    public static HandshakeCertificates provideHandshakeCertificates() {
        if (certificates == null) {
            // Create self-signed certificates with a short validity period (1 day) for unit tests
            certificates = KeyMaterialUtil.createSelfSignedHandshakeCertificates(1, KeyAlgorithm.RSA_2048);
        }
        return certificates;
    }
}
