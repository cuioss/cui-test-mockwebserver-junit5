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

/**
 * This package contains test classes that demonstrate different approaches to testing HTTPS with MockWebServer.
 * <h2>Test Classes Overview</h2>
 * 
 * <h3>SelfSignedHttpsAccessTest</h3>
 * <p>
 * Demonstrates how to access a MockWebServer with HTTPS using self-signed certificates provided by the extension.
 * <ul>
 *   <li>Uses the "Extension → Test" approach</li>
 *   <li>The extension creates and manages the certificates</li>
 *   <li>The test receives the certificates via {@code setSslContext()}</li>
 *   <li>The test configures Java's HttpClient to trust these certificates</li>
 * </ul>
 * </p>
 * 
 * <h3>CustomCertificateHttpsAccessTest</h3>
 * <p>
 * Similar to SelfSignedHttpsAccessTest, but demonstrates a scenario with custom certificate properties.
 * <ul>
 *   <li>Uses the "Extension → Test" approach</li>
 *   <li>The extension creates and manages the certificates</li>
 *   <li>The test receives the certificates via {@code setSslContext()}</li>
 *   <li>The test configures Java's HttpClient to trust these certificates</li>
 * </ul>
 * </p>
 * 
 * <h3>TestProvidedHttpsTest</h3>
 * <p>
 * Originally demonstrated the "Test → Extension" approach, but has been refactored to use 
 * the "Extension → Test" approach for consistency and reliability.
 * <ul>
 *   <li>Now uses the "Extension → Test" approach</li>
 *   <li>The extension creates and manages the certificates</li>
 *   <li>The test receives the certificates via {@code setSslContext()}</li>
 *   <li>The test configures Java's HttpClient to trust these certificates</li>
 * </ul>
 * </p>
 * 
 * <h2>Key Concepts</h2>
 * <p>
 * All tests use the HandshakeCertificates exchange mechanism, which ensures that both the server 
 * and client use the same certificate material. This is crucial for avoiding SSL handshake failures.
 * </p>
 * <p>
 * The key to making HTTPS work with self-signed certificates is to ensure that:
 * <ol>
 *   <li>The server is configured with the certificate</li>
 *   <li>The client trusts the same certificate</li>
 *   <li>The hostname verification is properly configured</li>
 * </ol>
 * </p>
 * 
 * <h2>Configuration</h2>
 * <p>
 * All tests use the {@code @EnableMockWebServer} annotation with:
 * <ul>
 *   <li>{@code useHttps = true} to enable HTTPS</li>
 *   <li>{@code testClassProvidesKeyMaterial = false} (default) to use self-signed certificates</li>
 * </ul>
 * <p>
 * The extension automatically creates certificates with a short validity period (1 day)
 * suitable for unit tests.
 * </p>
 * </p>
 * 
 * <h2>Client Configuration</h2>
 * <p>
 * All tests configure Java's HttpClient with:
 * <ul>
 *   <li>A custom SSLContext with a TrustManager that trusts all certificates</li>
 *   <li>SSL verification settings that accept all server certificates (for testing purposes only)</li>
 *   <li>Appropriate connection timeouts</li>
 * </ul>
 * </p>
 */
package de.cuioss.test.mockwebserver.https;
