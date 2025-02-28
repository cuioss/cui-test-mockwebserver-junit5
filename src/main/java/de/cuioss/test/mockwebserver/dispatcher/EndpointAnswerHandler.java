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

import jakarta.servlet.http.HttpServletResponse;
import lombok.Getter;
import lombok.Setter;
import mockwebserver3.MockResponse;

import java.util.Optional;

/**
 * Utility class for managing HTTP responses in test scenarios. Provides pre-configured responses
 * and methods for customizing response behavior during tests.
 *
 * <h2>Common Response Types</h2>
 * <ul>
 *   <li>{@link #RESPONSE_OK} - 200 OK response</li>
 *   <li>{@link #RESPONSE_NO_CONTENT} - 204 No Content response</li>
 *   <li>{@link #RESPONSE_FORBIDDEN} - 403 Forbidden response</li>
 *   <li>{@link #RESPONSE_UNAUTHORIZED} - 401 Unauthorized response</li>
 * </ul>
 *
 * <h2>Usage Examples</h2>
 * <pre>
 * // Create handler with default GET response
 * var handler = EndpointAnswerHandler.forPositiveGetRequest();
 *
 * // Set custom response
 * handler.setResponse(new MockResponse()
 *     .setResponseCode(200)
 *     .setBody("{'status': 'success'}")
 * );
 *
 * // Reset to default
 * handler.resetToDefaultResponse();
 *
 * // Create handler with specific default response
 * var customHandler = EndpointAnswerHandler.builder()
 *     .defaultResponse(RESPONSE_NO_CONTENT)
 *     .build();
 * </pre>
 *
 * <h2>Factory Methods</h2>
 * <ul>
 *   <li>{@link #forPositiveGetRequest()} - Handler for GET requests (200 OK)</li>
 *   <li>{@link #forPositivePostRequest()} - Handler for POST requests (201 Created)</li>
 *   <li>{@link #forPositivePutRequest()} - Handler for PUT requests (204 No Content)</li>
 *   <li>{@link #forPositiveDeleteRequest()} - Handler for DELETE requests (204 No Content)</li>
 * </ul>
 *
 * @author Oliver Wolff
 * @since 1.0
 */
public class EndpointAnswerHandler {

    /**
     * Empty response with status code 403 Forbidden.
     * Use this for endpoints that require authentication/authorization.
     */
    public static final MockResponse RESPONSE_FORBIDDEN = new MockResponse(HttpServletResponse.SC_FORBIDDEN);

    /**
     * Empty response with status code 401 Unauthorized.
     * Use this for endpoints that require valid credentials.
     */
    public static final MockResponse RESPONSE_UNAUTHORIZED = new MockResponse(HttpServletResponse.SC_UNAUTHORIZED);

    /**
     * Empty response with status code 200 OK.
     * Standard success response for GET requests.
     */
    public static final MockResponse RESPONSE_OK = new MockResponse(HttpServletResponse.SC_OK);

    /**
     * Empty response with status code 204 No Content.
     * Typically used for successful PUT/DELETE operations.
     */
    public static final MockResponse RESPONSE_NO_CONTENT = new MockResponse(HttpServletResponse.SC_NO_CONTENT);

    /**
     * Empty response transporting {@link HttpServletResponse#SC_NOT_FOUND}
     */
    public static final MockResponse RESPONSE_NOT_FOUND = new MockResponse(HttpServletResponse.SC_NOT_FOUND);

    /**
     * Empty response transporting {@link HttpServletResponse#SC_NOT_IMPLEMENTED}
     */
    public static final MockResponse RESPONSE_NOT_IMPLEMENTED = new MockResponse(HttpServletResponse.SC_NOT_IMPLEMENTED);

    /**
     * Empty response transporting {@link HttpServletResponse#SC_CREATED}
     */
    public static final MockResponse RESPONSE_CREATED = new MockResponse(HttpServletResponse.SC_CREATED);

    /**
     * Empty response transporting {@link HttpServletResponse#SC_MOVED_PERMANENTLY}
     */
    public static final MockResponse RESPONSE_MOVED_PERMANENTLY = new MockResponse(HttpServletResponse.SC_MOVED_PERMANENTLY);

    /**
     * Empty response transporting {@link HttpServletResponse#SC_MOVED_TEMPORARILY}
     */
    public static final MockResponse RESPONSE_MOVED_TEMPORARILY = new MockResponse(HttpServletResponse.SC_MOVED_TEMPORARILY);

    @Getter
    @Setter
    private MockResponse defaultResponse;

    @Getter
    private final HttpMethodMapper httpMethod;

    @Getter
    @Setter
    private MockResponse response;

    /**
     * Default Constructor
     *
     * @param defaultResponse
     * @param httpMethod
     */
    public EndpointAnswerHandler(MockResponse defaultResponse, HttpMethodMapper httpMethod) {
        this.httpMethod = httpMethod;
        this.defaultResponse = defaultResponse;
    }

    /**
     * @return the currently configured response
     */
    public Optional<MockResponse> respond() {
        return Optional.ofNullable(response);
    }

    /**
     * Resets the current answer to the default response
     *
     * @return The current instance of this Handler
     * @see #getDefaultResponse()
     */
    public EndpointAnswerHandler resetToDefaultResponse() {
        response = defaultResponse;
        return this;
    }

    /**
     * Sets the current answer to {@link #RESPONSE_FORBIDDEN}
     *
     * @return The current instance of this Handler
     */
    public EndpointAnswerHandler respondForbidden() {
        response = RESPONSE_FORBIDDEN;
        return this;
    }

    /**
     * Sets the current answer to {@link #RESPONSE_UNAUTHORIZED}
     *
     * @return The current instance of this Handler
     */
    public EndpointAnswerHandler respondUnauthorized() {
        response = RESPONSE_UNAUTHORIZED;
        return this;
    }

    /**
     * Sets the current answer to {@link #RESPONSE_OK}
     *
     * @return The current instance of this Handler
     */
    public EndpointAnswerHandler respondOk() {
        response = RESPONSE_OK;
        return this;
    }

    /**
     * Sets the current answer to {@link #RESPONSE_NO_CONTENT}
     *
     * @return The current instance of this Handler
     */
    public EndpointAnswerHandler respondNoContent() {
        response = RESPONSE_NO_CONTENT;
        return this;
    }

    /**
     * Sets the current answer to {@link #RESPONSE_NOT_FOUND}
     *
     * @return The current instance of this Handler
     */
    public EndpointAnswerHandler respondNotFound() {
        response = RESPONSE_NOT_FOUND;
        return this;
    }

    /**
     * Sets the current answer to {@link #RESPONSE_NOT_IMPLEMENTED}
     *
     * @return The current instance of this Handler
     */
    public EndpointAnswerHandler respondNotImplemented() {
        response = RESPONSE_NOT_IMPLEMENTED;
        return this;
    }

    /**
     * Sets the current answer to {@link #RESPONSE_CREATED}
     *
     * @return The current instance of this Handler
     */
    public EndpointAnswerHandler respondCreated() {
        response = RESPONSE_CREATED;
        return this;
    }

    /**
     * Sets the current answer to {@link #RESPONSE_MOVED_PERMANENTLY}
     *
     * @return The current instance of this Handler
     */
    public EndpointAnswerHandler respondMovedPermanently() {
        response = RESPONSE_MOVED_PERMANENTLY;
        return this;
    }

    /**
     * Sets the current answer to {@link #RESPONSE_MOVED_TEMPORARILY}
     *
     * @return The current instance of this Handler
     */
    public EndpointAnswerHandler respondMovedTemporarily() {
        response = RESPONSE_MOVED_TEMPORARILY;
        return this;
    }

    /**
     * @return An {@link EndpointAnswerHandler} initialized with
     * {@link #RESPONSE_OK}
     */
    public static EndpointAnswerHandler forPositiveGetRequest() {
        return new EndpointAnswerHandler(RESPONSE_OK, HttpMethodMapper.GET).resetToDefaultResponse();
    }

    /**
     * @return An {@link EndpointAnswerHandler} initialized with
     * {@link #RESPONSE_OK}
     */
    public static EndpointAnswerHandler forPositivePostRequest() {
        return new EndpointAnswerHandler(RESPONSE_OK, HttpMethodMapper.POST).resetToDefaultResponse();
    }

    /**
     * @return An {@link EndpointAnswerHandler} initialized with
     * {@link #RESPONSE_CREATED}
     */
    public static EndpointAnswerHandler forPositivePutRequest() {
        return new EndpointAnswerHandler(RESPONSE_CREATED, HttpMethodMapper.PUT).resetToDefaultResponse();
    }

    /**
     * @return An {@link EndpointAnswerHandler} initialized with
     * {@link #RESPONSE_NO_CONTENT}
     */
    public static EndpointAnswerHandler forPositiveDeleteRequest() {
        return new EndpointAnswerHandler(RESPONSE_NO_CONTENT, HttpMethodMapper.DELETE).resetToDefaultResponse();
    }

    /**
     * @return An {@link EndpointAnswerHandler} resulting {@link #respond()} to
     * return {@link Optional#empty()}
     */
    public static EndpointAnswerHandler noContent(HttpMethodMapper httpMethod) {
        return new EndpointAnswerHandler(null, httpMethod);
    }
}
