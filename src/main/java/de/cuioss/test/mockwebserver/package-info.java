/**
 * Mock web server infrastructure for HTTP interaction testing, based on
 * {@link mockwebserver3.MockWebServer}.
 *
 * <h2>Package Organization</h2>
 * <ul>
 *   <li>Server Management
 *     <ul>
 *       <li>{@link de.cuioss.test.mockwebserver.EnableMockWebServer} - Server activation</li>
 *       <li>{@link de.cuioss.test.mockwebserver.MockWebServerExtension} - Lifecycle management</li>
 *     </ul>
 *   </li>
 *   <li>Request Handling
 *     <ul>
 *       <li>{@link de.cuioss.test.mockwebserver.dispatcher} - Custom request dispatchers</li>
 *       <li>{@link de.cuioss.test.mockwebserver.MockWebServerHolder} - Server access</li>
 *     </ul>
 *   </li>
 * </ul>
 *
 * <h2>Key Concepts</h2>
 * <ul>
 *   <li><strong>Server Lifecycle</strong> - Automated server management</li>
 *   <li><strong>Request Handling</strong> - Custom response configuration</li>
 *   <li><strong>Test Integration</strong> - JUnit 5 extension support</li>
 * </ul>
 * <p>
 * For detailed documentation and examples, see:
 * <ul>
 *   <li>{@link de.cuioss.test.mockwebserver.EnableMockWebServer} for server setup</li>
 *   <li>{@link de.cuioss.test.mockwebserver.dispatcher} for request handling</li>
 * </ul>
 *
 * @author Oliver Wolff
 * @see de.cuioss.test.mockwebserver.dispatcher
 * @see mockwebserver3.MockWebServer
 * @since 1.0
 */
package de.cuioss.test.mockwebserver;
