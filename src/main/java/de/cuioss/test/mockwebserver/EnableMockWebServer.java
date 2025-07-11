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
package de.cuioss.test.mockwebserver;

import mockwebserver3.MockWebServer;
import org.junit.jupiter.api.extension.ExtendWith;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * JUnit 5 annotation that enables and configures a {@link MockWebServer} instance for HTTP interaction testing.
 * The server instance is automatically injected into test methods via parameter injection.
 *
 * <h2>Basic Usage</h2>
 * <pre>
 * &#64;EnableMockWebServer
 * class SimpleHttpTest {
 *
 *     &#64;Test
 *     void shouldTestWithServer(MockWebServer server) {
 *         // Use the server directly in your test
 *         server.enqueue(new MockResponse().setBody("Hello World"));
 *         // Make HTTP requests to server.url("/")
 *     }
 * }
 * </pre>
 *
 * <h2>Manual Server Control</h2>
 * <pre>
 * &#64;EnableMockWebServer(manualStart = true)
 * class ControlledHttpTest {
 *
 *     &#64;Test
 *     void shouldTestWithCustomPort(MockWebServer server) {
 *         server.start(8080);
 *         // Test with specific port
 *         server.shutdown();
 *     }
 * }
 * </pre>
 *
 * <h2>HTTPS Support</h2>
 * <pre>
 * &#64;EnableMockWebServer(
 *     useHttps = true
 * )
 * class HttpsTest {
 *
 *     &#64;Test
 *     void shouldTestHttps(MockWebServer server, SSLContext sslContext) {
 *         // Use the server and SSL context directly
 *     }
 * }
 * </pre>
 *
 * <h2>Parameter Resolving</h2>
 * <p>The extension can automatically inject various parameters into your test methods:</p>
 *
 * <h3>URL Builder Parameter</h3>
 * <p>The extension provides a convenient URL builder for constructing request URLs:</p>
 * <pre>
 * &#64;EnableMockWebServer
 * class UrlBuilderTest {
 *
 *     &#64;Test
 *     void shouldUseUrlBuilder(URIBuilder urlBuilder) throws IOException {
 *         // Build a URL with path segments and query parameters
 *         URI uri = urlBuilder
 *                 .addPathSegment("api")
 *                 .addPathSegment("users")
 *                 .addQueryParameter("filter", "active")
 *                 .build();
 *
 *         // Use the URI for requests
 *         HttpRequest request = HttpRequest.newBuilder()
 *                 .uri(uri)
 *                 .GET()
 *                 .build();
 *     }
 * }
 * </pre>
 *
 * <h3>SSLContext Parameter</h3>
 * <p>When HTTPS is enabled, the extension automatically makes the SSLContext available for parameter injection:</p>
 * <pre>
 * &#64;EnableMockWebServer(
 *     useHttps = true
 * )
 * class SslContextTest {
 *
 *     &#64;Test
 *     void shouldConnectSecurely(SSLContext sslContext) throws IOException {
 *         // The SSLContext is automatically injected
 *         HttpClient client = HttpClient.newBuilder()
 *                 .sslContext(sslContext)
 *                 .build();
 *
 *         // Make secure requests with the client
 *     }
 * }
 * </pre>
 *
 * <h2>Features</h2>
 * <ul>
 *   <li>Automatic server startup before each test (default behavior)</li>
 *   <li>Manual server control with {@link #manualStart()}</li>
 *   <li>Support for custom {@link mockwebserver3.Dispatcher} implementations</li>
 *   <li>HTTPS support with both self-signed and custom certificates</li>
 *   <li>Parameter resolving for {@link MockWebServer}, port, URL, {@link URIBuilder}, and {@link javax.net.ssl.SSLContext}</li>
 * </ul>
 *
 * @author Oliver Wolff
 * @see MockWebServerExtension
 * @since 1.0
 */
@Documented
@Retention(RUNTIME)
@Target(TYPE)
@ExtendWith(MockWebServerExtension.class)
public @interface EnableMockWebServer {

    /**
     * Controls the automatic startup behavior of the MockWebServer.
     *
     * @return {@code true} if the server should be started manually, {@code false} for automatic
     * startup before each test (default)
     */
    boolean manualStart() default false;

    /**
     * Controls whether the MockWebServer should use HTTPS instead of HTTP.
     * When set to {@code true}, the extension will either:
     * <ul>
     *   <li>Use certificates provided by the test class if it is annotated with {@link TestProvidedCertificate}</li>
     *   <li>Automatically generate self-signed certificates if no {@link TestProvidedCertificate} annotation is present</li>
     * </ul>
     *
     * @return {@code true} if the server should use HTTPS, {@code false} for HTTP (default)
     * @see TestProvidedCertificate
     */
    boolean useHttps() default false;


}
