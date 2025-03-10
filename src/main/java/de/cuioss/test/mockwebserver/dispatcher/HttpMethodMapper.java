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

import de.cuioss.tools.string.MoreStrings;

import java.util.Optional;


import mockwebserver3.MockResponse;
import mockwebserver3.RecordedRequest;

/**
 * Maps HTTP methods to their corresponding handler methods in {@link ModuleDispatcherElement}.
 * This enum provides a type-safe way to handle different HTTP methods and route requests
 * to the appropriate handler method.
 *
 * <h2>Features</h2>
 * <ul>
 *   <li>Type-safe HTTP method mapping</li>
 *   <li>Automatic method routing</li>
 *   <li>Support for GET, POST, PUT, DELETE methods</li>
 *   <li>Null-safe request handling</li>
 * </ul>
 *
 * <h2>Usage Example</h2>
 * <pre>
 * RecordedRequest request = // ... from MockWebServer
 * ModuleDispatcherElement dispatcher = // ... your dispatcher
 *
 * // Get the appropriate method mapper
 * HttpMethodMapper mapper = HttpMethodMapper.of(request);
 *
 * // Handle the request with the correct method
 * Optional&lt;MockResponse&gt; response = mapper.handleMethod(dispatcher, request);
 * </pre>
 *
 * @author Oliver Wolff
 * @see ModuleDispatcherElement
 * @since 1.0
 */
public enum HttpMethodMapper {

    /**
     * Handles HTTP GET requests by delegating to {@link ModuleDispatcherElement#handleGet}.
     */
    GET {
        @Override
        public Optional<MockResponse> handleMethod(ModuleDispatcherElement dispatcherElement, RecordedRequest request) {
            return dispatcherElement.handleGet(request);
        }
    },
    /**
     * Handles HTTP POST requests by delegating to {@link ModuleDispatcherElement#handlePost}.
     */
    POST {
        @Override
        public Optional<MockResponse> handleMethod(ModuleDispatcherElement dispatcherElement, RecordedRequest request) {
            return dispatcherElement.handlePost(request);
        }
    },
    /**
     * Handles HTTP PUT requests by delegating to {@link ModuleDispatcherElement#handlePut}.
     */
    PUT {
        @Override
        public Optional<MockResponse> handleMethod(ModuleDispatcherElement dispatcherElement, RecordedRequest request) {
            return dispatcherElement.handlePut(request);
        }
    },
    /**
     * Handles HTTP DELETE requests by delegating to {@link ModuleDispatcherElement#handleDelete}.
     */
    DELETE {
        @Override
        public Optional<MockResponse> handleMethod(ModuleDispatcherElement dispatcherElement, RecordedRequest request) {
            return dispatcherElement.handleDelete(request);
        }
    };

    /**
     * Handles a request using the appropriate method from the dispatcher element.
     *
     * @param dispatcherElement the dispatcher to handle the request, must not be null
     * @param request           the request to handle, must not be null
     * @return an Optional containing the response if the dispatcher can handle it,
     * or empty if the request should be handled by another dispatcher
     * @throws NullPointerException if any parameter is null
     */
    public abstract Optional<MockResponse> handleMethod(ModuleDispatcherElement dispatcherElement,
                                                        RecordedRequest request);

    /**
     * Creates an HttpMethodMapper from a RecordedRequest's method.
     *
     * @param request the request containing the HTTP method, must not be null
     * @return the corresponding HttpMethodMapper for the request's method
     * @throws IllegalArgumentException if the method is not supported
     * @throws NullPointerException     if request is null
     */
    public static HttpMethodMapper of(RecordedRequest request) {
        return HttpMethodMapper.valueOf(MoreStrings.nullToEmpty(request.getMethod()).toUpperCase());
    }
}
