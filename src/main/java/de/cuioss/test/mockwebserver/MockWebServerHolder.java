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

import de.cuioss.test.mockwebserver.ssl.KeyMaterialUtil;
import de.cuioss.tools.net.ssl.KeyMaterialHolder;
import mockwebserver3.Dispatcher;
import mockwebserver3.MockWebServer;
import okhttp3.tls.HandshakeCertificates;

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
 *     testClassProvidesKeyMaterial = true
 * )
 * class HttpsTest implements MockWebServerHolder {
 *     &#64;Getter
 *     &#64;Setter
 *     private MockWebServer server;
 *
 *     &#64;Override
 *     public Optional<KeyMaterialHolder> provideKeyMaterial() {
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
 * @see EnableMockWebServer
 * @see MockWebServerExtension
 * @since 1.0
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
     * Provides HandshakeCertificates for HTTPS configuration.
     * This method directly provides OkHttp's HandshakeCertificates, which can be used to configure both server and client.
     * <p>
     * The default implementation returns an empty Optional, meaning no HandshakeCertificates are provided.
     * Override this method to provide custom HandshakeCertificates for HTTPS.
     * </p>
     * <p>
     * This method will only be called if {@link EnableMockWebServer#useHttps()} and
     * {@link EnableMockWebServer#testClassProvidesKeyMaterial()} are both {@code true}.
     * </p>
     *
     * @return an Optional containing the HandshakeCertificates, or empty if none are provided
     * @since 1.1
     */
    default Optional<HandshakeCertificates> provideHandshakeCertificates() {
        return Optional.empty();
    }

    /**
     * Receives the SSLContext used by the extension to configure HTTPS.
     * This is called after the extension has configured the server with HTTPS.
     * <p>
     * The default implementation does nothing. Override this method to receive and
     * store the SSLContext for client configuration.
     * </p>
     * <p>
     * This method will only be called if {@link EnableMockWebServer#useHttps()} is {@code true}.
     * </p>
     *
     * @param sslContext the HandshakeCertificates used by the extension
     * @since 1.1
     */
    default void setSslContext(SSLContext sslContext) {
        // Default implementation does nothing
    }

    /**
     * Provides a custom SSLContext for client connections to the MockWebServer.
     * This is useful when the server is configured to use HTTPS with custom certificates.
     * <p>
     * The default implementation creates an SSLContext from the HandshakeCertificates provided by
     * {@link #provideHandshakeCertificates()} if available. If no certificates are provided, it returns
     * an empty Optional.
     * </p>
     * <p>
     * This method can be overridden to provide a custom SSLContext implementation,
     * but in most cases, it's better to override {@link #provideHandshakeCertificates()} instead.
     * </p>
     *
     * @return an Optional containing the SSLContext, or empty if no custom SSLContext is provided
     * @since 1.1
     */
    default Optional<SSLContext> getSSLContext() {
        return provideHandshakeCertificates()
                .map(KeyMaterialUtil::createSslContext);
    }
}
