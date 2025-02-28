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
package de.cuioss.test.mockwebserver.dispatcher;

import de.cuioss.test.mockwebserver.EnableMockWebServer;
import lombok.NonNull;
import mockwebserver3.MockResponse;
import mockwebserver3.RecordedRequest;

import java.util.Optional;

/**
 * Interface for modular HTTP request dispatching in test scenarios. Enables reusable
 * request handling components that can be combined to create complex test behaviors.
 *
 * <h2>Key Concepts</h2>
 * <ul>
 *   <li>Base URL filtering for request routing</li>
 *   <li>Method-specific request handling</li>
 *   <li>Optional responses for chain-of-responsibility pattern</li>
 *   <li>Modular and reusable components</li>
 * </ul>
 *
 * <h2>Usage Example</h2>
 * <pre>
 * public class UserApiDispatcher implements ModuleDispatcherElement {
 *     &#64;Override
 *     public String getBaseUrl() {
 *         return "/api/users";
 *     }
 *
 *     &#64;Override
 *     public Optional&lt;MockResponse&gt; handleGet(RecordedRequest request) {
 *         if (request.getPath().equals("/api/users/123")) {
 *             return Optional.of(new MockResponse()
 *                 .setResponseCode(200)
 *                 .setBody("{'id': '123', 'name': 'John'}"));
 *         }
 *         return Optional.empty(); // Let other handlers try
 *     }
 * }
 * </pre>
 *
 * <h2>Integration with MockWebServer</h2>
 * <pre>
 * &#64;EnableMockWebServer
 * class ApiTest implements MockWebServerHolder {
 *     private final ModuleDispatcherElement dispatcher = new UserApiDispatcher();
 *
 *     &#64;Override
 *     public Dispatcher getDispatcher() {
 *         return new CombinedDispatcher(dispatcher);
 *     }
 * }
 * </pre>
 *
 * <h2>Design Notes</h2>
 * <ul>
 *   <li>Each dispatcher handles a specific URL path prefix</li>
 *   <li>Return {@link Optional#empty()} to allow other dispatchers to handle the request</li>
 *   <li>Can be combined using {@link CombinedDispatcher}</li>
 *   <li>Supports all standard HTTP methods (GET, POST, PUT, DELETE)</li>
 * </ul>
 *
 * @author Oliver Wolff
 * @since 1.0
 * @see CombinedDispatcher
 * @see EnableMockWebServer
 */
public interface ModuleDispatcherElement {

    /**
     * Returns the base URL path that this dispatcher handles.
     * The runtime ensures that only requests matching this base URL are passed to this dispatcher.
     *
     * @return the base URL path, use "/" to handle all paths
     */
    String getBaseUrl();

    /**
     * Handles HTTP GET requests.
     *
     * @param request the incoming request with path, headers, and body
     * @return {@link Optional} containing the response if this dispatcher can handle the request,
     *         or {@link Optional#empty()} to let other dispatchers handle it
     */
    default Optional<MockResponse> handleGet(@NonNull RecordedRequest request) {
        return Optional.empty();
    }

    /**
     * Handles HTTP POST requests.
     *
     * @param request the incoming request with path, headers, and body
     * @return {@link Optional} containing the response if this dispatcher can handle the request,
     *         or {@link Optional#empty()} to let other dispatchers handle it
     */
    default Optional<MockResponse> handlePost(@NonNull RecordedRequest request) {
        return Optional.empty();
    }

    /**
     * Handles HTTP PUT requests.
     *
     * @param request the incoming request with path, headers, and body
     * @return {@link Optional} containing the response if this dispatcher can handle the request,
     *         or {@link Optional#empty()} to let other dispatchers handle it
     */
    default Optional<MockResponse> handlePut(@NonNull RecordedRequest request) {
        return Optional.empty();
    }

    /**
     * Handles HTTP DELETE requests.
     *
     * @param request the incoming request with path, headers, and body
     * @return {@link Optional} containing the response if this dispatcher can handle the request,
     *         or {@link Optional#empty()} to let other dispatchers handle it
     */
    default Optional<MockResponse> handleDelete(@NonNull RecordedRequest request) {
        return Optional.empty();
    }
}
