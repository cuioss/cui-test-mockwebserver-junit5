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
/**
 * SSL/TLS key material support for HTTPS testing with
 * {@link de.cuioss.test.mockwebserver.MockWebServerExtension}.
 *
 * <h2>Contents</h2>
 * <ul>
 *   <li>{@link de.cuioss.test.mockwebserver.ssl.KeyMaterialUtil} - generates self-signed
 *       {@code HandshakeCertificates} (honoring the requested key algorithm) and converts them to an
 *       {@link javax.net.ssl.SSLContext} for client configuration.</li>
 * </ul>
 *
 * @author Oliver Wolff
 * @see de.cuioss.test.mockwebserver
 * @since 1.1
 */
package de.cuioss.test.mockwebserver.ssl;
