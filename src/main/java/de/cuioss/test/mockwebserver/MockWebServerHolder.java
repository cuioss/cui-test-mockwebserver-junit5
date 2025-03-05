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

import de.cuioss.tools.net.ssl.KeyMaterialHolder;
import mockwebserver3.Dispatcher;
import mockwebserver3.MockWebServer;

import javax.net.ssl.SSLContext;
import java.util.Optional;

/**
 * Interface for test classes that need access to a {@link MockWebServer} instance.
 * This interface serves as a bridge between the test infrastructure and test classes,
 * providing access to the server instance and optional request dispatching.
 *
 * <h2>Basic Implementation</h2>
 * <pre>
 * &#64;EnableMockWebServer
 * class BasicHttpTest implements MockWebServerHolder {
 *     &#64;Getter
 *     &#64;Setter
 *     private MockWebServer server;
 *
 * }
 * </pre>
 *
 * <h2>Custom Request Dispatching</h2>
 * <pre>
 * &#64;EnableMockWebServer
 * class CustomDispatchTest implements MockWebServerHolder {
 *     &#64;Getter
 *     &#64;Setter
 *     private MockWebServer server;
 *
 *     &#64;Override
 *     public Dispatcher getDispatcher() {
 *         return new Dispatcher() {
 *             &#64;Override
 *             public MockResponse dispatch(RecordedRequest request) {
 *                 if ("/api/data".equals(request.getPath())) {
 *                     return new MockResponse().setBody("{'data': 'test'}");
 *                 }
 *                 return new MockResponse().setResponseCode(404);
 *             }
 *         };
 *     }
 * }
 * </pre>
 *
 * <h2>HTTPS Support</h2>
 * <pre>
 * &#64;EnableMockWebServer(
 *     useHttps = true,
 *     keyMaterialProviderIsTestClass = true
 * )
 * class HttpsTest implements MockWebServerHolder {
 *     &#64;Getter
 *     &#64;Setter
 *     private MockWebServer server;
 *
 *     &#64;Override
 *     public KeyMaterialHolder provideKeyMaterial() {
 *         // Return custom key material for HTTPS
 *         return KeyMaterialHolder.builder()
 *                 .keyMaterial(myCertificateBytes)
 *                 .keyHolderType(KeyHolderType.SINGLE_KEY)
 *                 .keyAlias("my-cert")
 *                 .build();
 *     }
 * }
 * </pre>
 *
 * <h2>Usage Notes</h2>
 * <ul>
 *   <li>The {@link #setMockWebServer(MockWebServer)} method must be implemented to receive the server instance</li>
 *   <li>The {@link #getMockWebServer()} method must be implemented to retrieve the server instance</li>
 *   <li>Implement {@link #getDispatcher()} to provide custom request handling logic</li>
 *   <li>Implement {@link #provideKeyMaterial()} to provide custom certificates for HTTPS</li>
 *   <li>Implement {@link #getSSLContext()} to provide a custom SSLContext for client connections</li>
 *   <li>The server instance is managed by {@link MockWebServerExtension}</li>
 *   <li>Default dispatcher returns null, meaning requests are handled by the default MockWebServer behavior</li>
 * </ul>
 *
 * @author Oliver Wolff
 * @since 1.0
 * @see EnableMockWebServer
 * @see MockWebServerExtension
 */
public interface MockWebServerHolder {

    /**
     * Returns the {@link MockWebServer} instance used in tests.
     * The default implementation returns null. This method can be overridden
     * to provide custom access to the server instance.
     *
     * @return the server instance, may be {@code null} if not yet initialized
     */
    default MockWebServer getMockWebServer() {
        return null;
    }

    /**
     * Callback method to receive the {@link MockWebServer} instance.
     * The default implementation does nothing. Override this method if you need
     * to store the server instance for later use.
     *
     * @param mockWebServer The server instance to be used
     */
    default void setMockWebServer(MockWebServer mockWebServer) {
        // Default implementation does nothing
    }

    /**
     * Provides a custom {@link Dispatcher} for the {@link MockWebServer}.
     * The default implementation returns null, which means the default dispatcher will be used.
     * Override this method to provide custom request handling logic.
     *
     * @return the dispatcher to be used, or {@code null} to use the default dispatcher
     */
    default Dispatcher getDispatcher() {
        return null;
    }
    
    /**
     * Provides key material for HTTPS configuration when {@link EnableMockWebServer#keyMaterialProviderIsTestClass()}
     * is set to {@code true}. This method allows tests to provide custom certificates for the MockWebServer.
     * <p>
     * The default implementation returns an empty Optional, meaning no key material is provided.
     * Override this method to provide custom key material for HTTPS.
     * </p>
     * <p>
     * This method will only be called if {@link EnableMockWebServer#useHttps()} and
     * {@link EnableMockWebServer#keyMaterialProviderIsTestClass()} are both {@code true}.
     * </p>
     *
     * @return an Optional containing the key material, or empty if no key material is provided
     * @since 1.1
     */
    default Optional<KeyMaterialHolder> provideKeyMaterial() {
        return Optional.empty();
    }
    
    /**
     * Provides a custom SSLContext for client connections to the MockWebServer.
     * This is useful when the server is configured to use HTTPS with custom certificates.
     * <p>
     * The default implementation creates an SSLContext from the key material provided by
     * {@link #provideKeyMaterial()} if available. If no key material is provided, it returns
     * an empty Optional.
     * </p>
     * <p>
     * This method can be overridden to provide a custom SSLContext implementation,
     * but in most cases, it's better to override {@link #provideKeyMaterial()} instead.
     * </p>
     *
     * @return an Optional containing the SSLContext, or empty if no custom SSLContext is provided
     * @since 1.1
     */
    default Optional<SSLContext> getSSLContext() {
        return provideKeyMaterial()
                .map(keyMaterial -> de.cuioss.test.mockwebserver.ssl.KeyMaterialUtil.createSslContext(keyMaterial));
    }
}
