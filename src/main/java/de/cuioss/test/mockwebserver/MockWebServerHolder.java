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

import mockwebserver3.Dispatcher;
import mockwebserver3.MockWebServer;

import javax.net.ssl.SSLContext;

/**
 * Interface for test classes that need access to a {@link MockWebServer} instance.
 * This interface serves as a bridge between the test infrastructure and test classes,
 * providing access to the server instance and optional request dispatching.
 *
 * <h2>Recommended Approach: Parameter Injection</h2>
 * <p>
 * While this interface is still supported, the recommended approach is to use parameter
 * injection instead of implementing this interface. See examples below.
 *
 * <h2>Basic Implementation with Parameter Injection and Dispatcher</h2>
 * <pre>
 * {@code
 * @EnableMockWebServer
 * class BasicHttpTest {
 *
 *     @Test
 *     void testHttpRequest(MockWebServer server, URIBuilder uriBuilder) throws Exception {
 *         // Create a dispatcher for this test
 *         server.setDispatcher(new Dispatcher() {
 *             @Override
 *             public MockResponse dispatch(RecordedRequest request) {
 *                 if ("/api/data".equals(request.getPath())) {
 *                     return new MockResponse.Builder()
 *                         .addHeader("Content-Type", "text/plain")
 *                         .body("Hello World")
 *                         .code(HttpServletResponse.SC_OK)
 *                         .build();
 *                 }
 *                 return new MockResponse.Builder()
 *                     .code(HttpServletResponse.SC_NOT_FOUND)
 *                     .build();
 *             }
 *         });
 *
 *         // Create HttpClient
 *         HttpClient client = HttpClient.newHttpClient();
 *
 *         // Create request using the URIBuilder parameter
 *         HttpRequest request = HttpRequest.newBuilder()
 *             .uri(uriBuilder.addPathSegment("api").addPathSegment("data").build())
 *             .GET()
 *             .build();
 *
 *         // Send request and verify response
 *         HttpResponse<String> response = client.send(request,
 *             HttpResponse.BodyHandlers.ofString());
 *         assertEquals(200, response.statusCode());
 *         assertEquals("Hello World", response.body());
 *     }
 * }
 * }
 *
 * <h2>Using EndpointAnswerHandler for Request Dispatching</h2>
 * <pre>
 * {@code
 * @EnableMockWebServer
 * class EndpointDispatcherTest {
 *
 *     @Test
 *     void testWithEndpointDispatcher(MockWebServer server, URIBuilder uriBuilder) throws Exception {
 *         // Create an EndpointAnswerHandler for this test
 *         var handler = new EndpointAnswerHandler();
 *
 *         // Configure endpoint responses
 *         handler.addAnswerFor("/api/data", new MockResponse()
 *             .setBody("{'data': 'test'}"));
 *
 *         // Set the dispatcher for this test
 *         server.setDispatcher(handler);
 *
 *         // Create HttpClient
 *         HttpClient client = HttpClient.newHttpClient();
 *
 *         // Create request using the URIBuilder parameter
 *         HttpRequest request = HttpRequest.newBuilder()
 *             .uri(uriBuilder.addPathSegment("api").addPathSegment("data").build())
 *             .GET()
 *             .build();
 *
 *         // Send request and verify response
 *         HttpResponse<String> response = client.send(request,
 *             HttpResponse.BodyHandlers.ofString());
 *         assertEquals(200, response.statusCode());
 *         assertEquals("{'data': 'test'}", response.body());
 *     }
 * }
 * }
 *
 * <h2>HTTPS Support</h2>
 * <pre>
 * {@code
 * @EnableMockWebServer(useHttps = true)
 * class HttpsTest {
 *
 *     @Test
 *     void testHttpsRequest(MockWebServer server, SSLContext sslContext, URIBuilder uriBuilder) throws Exception {
 *         // Create a dispatcher for this test
 *         server.setDispatcher(new Dispatcher() {
 *             @Override
 *             public MockResponse dispatch(RecordedRequest request) {
 *                 if ("/secure/data".equals(request.getPath())) {
 *                     return new MockResponse().setBody("Hello Secure World");
 *                 }
 *                 return new MockResponse().setResponseCode(404);
 *             }
 *         });
 *
 *         // Create HttpClient with the injected SSLContext
 *         HttpClient client = HttpClient.newBuilder()
 *             .sslContext(sslContext)
 *             .build();
 *
 *         // Create request using the URIBuilder parameter
 *         HttpRequest request = HttpRequest.newBuilder()
 *             .uri(uriBuilder.addPathSegment("secure").addPathSegment("data").build())
 *             .GET()
 *             .build();
 *
 *         // Send request and verify response
 *         HttpResponse<String> response = client.send(request,
 *             HttpResponse.BodyHandlers.ofString());
 *         assertEquals(200, response.statusCode());
 *         assertEquals("Hello Secure World", response.body());
 *     }
 * }
 * }
 *
 * <h2>Usage Notes</h2>
 * <ul>
 *   <li>Parameter injection is the recommended way to access the server instance</li>
 *   <li>Add {@link MockWebServer} as a parameter to your test method to get the server instance</li>
 *   <li>Add {@link SSLContext} as a parameter to your test method when using HTTPS</li>
 *   <li>Add {@link URIBuilder} as a parameter to your test method for easier URI construction</li>
 *   <li>Use server.setDispatcher() to configure request handling directly in test methods</li>
 *   <li>Use {@link de.cuioss.test.mockwebserver.dispatcher.EndpointAnswerHandler} for simple endpoint configuration</li>
 *   <li>The server instance is managed by {@link MockWebServerExtension}</li>
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
     * @deprecated since 1.1, will be removed in 1.2. Use parameter injection instead by adding
     * {@link MockWebServer} as a parameter to your test method.
     */
    @Deprecated(since = "1.1", forRemoval = true)
    default MockWebServer getMockWebServer() {
        return null;
    }

    /**
     * Callback method to receive the {@link MockWebServer} instance.
     * The default implementation does nothing. Override this method if you need
     * to store the server instance for later use.
     *
     * @param mockWebServer The server instance to be used
     * @deprecated since 1.1, will be removed in 1.2. Use parameter injection instead by adding
     * {@link MockWebServer} as a parameter to your test method.
     */
    @Deprecated(since = "1.1", forRemoval = true)
    default void setMockWebServer(MockWebServer mockWebServer) {
        // Default implementation does nothing
    }

    /**
     * Provides a custom {@link Dispatcher} for the {@link MockWebServer}.
     * The default implementation returns null, which means the default dispatcher will be used.
     * Override this method to provide custom request handling logic.
     *
     * @return the dispatcher to be used, or {@code null} to use the default dispatcher
     * 
     * @deprecated since 1.1, will be removed in 1.2. Use parameter injection instead and set the
     * dispatcher directly on the server instance in your test method:
     * {@code server.setDispatcher(new MyDispatcher())}
     */
    @Deprecated(since = "1.1", forRemoval = true)
    default Dispatcher getDispatcher() {
        return null;
    }


}
