/**
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
 * Mock web server infrastructure for HTTP and HTTPS interaction testing, based on
 * {@link mockwebserver3.MockWebServer}.
 *
 * <h2>Package Organization</h2>
 * <ul>
 *   <li>Server Management
 *     <ul>
 *       <li>{@link de.cuioss.test.mockwebserver.EnableMockWebServer} - Server activation and configuration</li>
 *       <li>{@link de.cuioss.test.mockwebserver.MockWebServerExtension} - Lifecycle management and parameter resolution</li>
 *     </ul>
 *   </li>
 *   <li>Request Handling
 *     <ul>
 *       <li>{@link de.cuioss.test.mockwebserver.dispatcher} - Custom request dispatchers</li>
 *       <li>{@link de.cuioss.test.mockwebserver.MockWebServerHolder} - Custom dispatcher configuration</li>
 *     </ul>
 *   </li>
 *   <li>HTTPS Support
 *     <ul>
 *       <li>{@link de.cuioss.test.mockwebserver.ssl} - SSL/TLS certificate management</li>
 *     </ul>
 *   </li>
 * </ul>
 *
 * <h2>Key Concepts</h2>
 * <ul>
 *   <li><strong>Parameter Injection</strong> - Inject server instances and related objects directly into test methods</li>
 *   <li><strong>Server Lifecycle</strong> - Automated server management</li>
 *   <li><strong>Request Handling</strong> - Custom response configuration</li>
 *   <li><strong>HTTPS Support</strong> - Self-signed and custom certificate handling</li>
 *   <li><strong>Test Integration</strong> - JUnit 5 extension support</li>
 * </ul>
 * 
 * <h2>Best Practices</h2>
 * <ul>
 *   <li><strong>Parameter Injection</strong> - Use parameter injection instead of implementing MockWebServerHolder</li>
 *   <li><strong>MockResponse Creation</strong> - Always use the Builder pattern for creating MockResponse objects:
 *     <pre>
 *     MockResponse response = new MockResponse.Builder()
 *             .addHeader("Content-Type", "application/json")
 *             .body(jsonContentKeyValue)
 *             .code(HttpServletResponse.SC_OK)
 *             .build();
 *     </pre>
 *   </li>
 *   <li><strong>Content-Type Headers</strong> - Always include appropriate Content-Type headers in responses</li>
 *   <li><strong>URI Construction</strong> - Use URIBuilder for constructing request URIs</li>
 * </ul>
 *
 * <h2>Parameter Resolution</h2>
 * <p>
 * The extension can inject the following parameter types into test methods:
 * <ul>
 *   <li>{@link mockwebserver3.MockWebServer} - The server instance</li>
 *   <li>{@link de.cuioss.test.mockwebserver.URIBuilder} - A builder for constructing request URLs</li>
 *   <li>{@link javax.net.ssl.SSLContext} - When HTTPS is enabled</li>
 * </ul>
 * 
 * <h2>Documentation and Examples</h2>
 * <p>
 * For detailed documentation and examples, see:
 * <ul>
 *   <li>{@link de.cuioss.test.mockwebserver.EnableMockWebServer} for server setup</li>
 *   <li>{@link de.cuioss.test.mockwebserver.MockWebServerExtension} for parameter injection</li>
 *   <li>{@link de.cuioss.test.mockwebserver.dispatcher} for request handling</li>
 *   <li>{@link de.cuioss.test.mockwebserver.ssl} for HTTPS configuration</li>
 * </ul>
 *
 * @author Oliver Wolff
 * @see de.cuioss.test.mockwebserver.dispatcher
 * @see de.cuioss.test.mockwebserver.ssl
 * @see mockwebserver3.MockWebServer
 * @since 1.0
 */
package de.cuioss.test.mockwebserver;
