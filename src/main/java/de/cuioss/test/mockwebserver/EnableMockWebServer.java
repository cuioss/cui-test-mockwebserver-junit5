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
import mockwebserver3.MockWebServer;
import org.junit.jupiter.api.extension.ExtendWith;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * JUnit 5 annotation that enables and configures a {@link MockWebServer} instance for HTTP interaction testing.
 * The test class must implement {@link MockWebServerHolder} to receive the server instance.
 *
 * <h2>Basic Usage</h2>
 * <pre>
 * &#64;EnableMockWebServer
 * class SimpleHttpTest implements MockWebServerHolder {
 *     private MockWebServer server;
 *
 *     &#64;Override
 *     public void setMockWebServer(MockWebServer mockWebServer) {
 *         this.server = mockWebServer;
 *     }
 * }
 * </pre>
 *
 * <h2>Manual Server Control</h2>
 * <pre>
 * &#64;EnableMockWebServer(manualStart = true)
 * class ControlledHttpTest implements MockWebServerHolder {
 *     private MockWebServer server;
 *
 *     &#64;Test
 *     void shouldTestWithCustomPort() {
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
 * class HttpsTest implements MockWebServerHolder {
 *     private MockWebServer server;
 *
 *     &#64;Override
 *     public void setMockWebServer(MockWebServer mockWebServer) {
 *         this.server = mockWebServer;
 *     }
 * }
 * </pre>
 *
 * <h2>Features</h2>
 * <ul>
 *   <li>Automatic server startup before each test (default behavior)</li>
 *   <li>Manual server control with {@link #manualStart()}</li>
 *   <li>Integration with {@link MockWebServerHolder} for server access</li>
 *   <li>Support for custom {@link mockwebserver3.Dispatcher} implementations</li>
 *   <li>HTTPS support with both self-signed and custom certificates</li>
 * </ul>
 *
 * <h2>MockWebServerHolder Nesting</h2>
 * <em>Caution: </em> In case of Nesting unit-tests, the {@link MockWebServerHolder} interface is not inherited by the nested classes.
 * Therefore, you must implement it in each nested class that should receive the server instance.
 *
 * @author Oliver Wolff
 * @see MockWebServerHolder
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
     *   <li>Use certificates provided by the test class if {@link #keyMaterialProviderIsTestClass()} is {@code true}</li>
     *   <li>Automatically generate self-signed certificates if {@link #keyMaterialProviderIsTestClass()} is {@code false}</li>
     * </ul>
     *
     * @return {@code true} if the server should use HTTPS, {@code false} for HTTP (default)
     */
    boolean useHttps() default false;

    /**
     * Indicates that the test class provides key material through the {@link MockWebServerHolder#provideKeyMaterial()}
     * method. When {@code true}, the extension will call this method to obtain the key material for HTTPS configuration.
     * 
     * <p>This approach gives tests full control over certificate generation/loading.</p>
     * 
     * <p>When {@code false}, the extension will automatically generate self-signed certificates
     * with a short validity period suitable for unit tests.</p>
     *
     * @return {@code true} if the test class provides key material, {@code false} otherwise (default)
     */
    boolean keyMaterialProviderIsTestClass() default false;




}
