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

import de.cuioss.tools.net.ssl.KeyAlgorithm;

/**
 * Immutable configuration class that holds all settings for the MockWebServerExtension.
 * <p>
 * This class encapsulates all configuration options for the MockWebServerExtension,
 * providing type-safe access to configuration values and sensible defaults.
 * </p>
 *
 * @param manualStart controls whether the server starts automatically or manually
 * @param useHttps controls whether the server uses HTTPS instead of HTTP
 */
record MockServerConfig(boolean manualStart, boolean useHttps) {
    // Fixed values for certificate generation
    private static final int CERTIFICATE_DURATION = 1; // 1 day validity for unit tests
    private static final KeyAlgorithm KEY_ALGORITHM = KeyAlgorithm.RSA_2048;

    /**
     * Creates a configuration with sensible defaults:
     * <ul>
     *   <li>manualStart = false (server starts automatically)</li>
     *   <li>useHttps = false (server uses HTTP)</li>
     * </ul>
     *
     * @return a new configuration instance with default values
     */
    static MockServerConfig getDefaults() {
        return new MockServerConfig(false, false);
    }

    /**
     * @return the validity duration in days for generated certificates
     */
    public int getCertificateDuration() {
        return CERTIFICATE_DURATION;
    }

    /**
     * @return the key algorithm to use for certificate generation
     */
    public KeyAlgorithm getKeyAlgorithm() {
        return KEY_ALGORITHM;
    }
}
