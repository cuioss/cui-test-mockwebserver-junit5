/*
 * Copyright © 2025-present CUI-OpenSource-Software (info@cuioss.de)
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
package de.cuioss.test.mockwebserver.dispatcher;

import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import mockwebserver3.MockResponse;
import mockwebserver3.RecordedRequest;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static de.cuioss.tools.collect.CollectionLiterals.mutableSet;
import static de.cuioss.tools.collect.CollectionLiterals.mutableSortedSet;

/**
 * Base dispatcher implementation that provides positive default responses for all supported HTTP methods.
 * This dispatcher is useful for testing scenarios where you need basic HTTP endpoint simulation
 * with the ability to customize responses per method.
 *
 * <h2>Features</h2>
 * <ul>
 *   <li>Pre-configured positive responses for GET, POST, PUT, DELETE, HEAD, PATCH, OPTIONS</li>
 *   <li>Per-method response customization</li>
 *   <li>Base URL path matching</li>
 *   <li>Response reset capability</li>
 * </ul>
 *
 * <h2>Usage Example</h2>
 * <pre>
 * // Create dispatcher for /api endpoints
 * var dispatcher = new BaseAllAcceptDispatcher("/api");
 *
 * // Customize GET response
 * dispatcher.getGetResult().setResponse(
 *     new MockResponse.Builder().body("{'data': 'test'}").build()
 * );
 *
 * // Reset to default responses
 * dispatcher.reset();
 * </pre>
 *
 * <h2>Default Responses</h2>
 * <ul>
 *   <li>GET: 200 OK with empty body</li>
 *   <li>POST: 200 OK</li>
 *   <li>PUT: 201 Created</li>
 *   <li>DELETE: 204 No Content</li>
 *   <li>HEAD: 200 OK</li>
 *   <li>PATCH: 200 OK</li>
 *   <li>OPTIONS: 200 OK</li>
 * </ul>
 *
 * @author Oliver Wolff
 * @see EndpointAnswerHandler
 * @see ModuleDispatcherElement
 * @since 1.0
 */
@RequiredArgsConstructor
public class BaseAllAcceptDispatcher implements ModuleDispatcherElement {

    @Getter
    private final String baseUrl;

    @Getter
    private final EndpointAnswerHandler getResult = EndpointAnswerHandler.forPositiveGetRequest();

    @Getter
    private final EndpointAnswerHandler postResult = EndpointAnswerHandler.forPositivePostRequest();

    @Getter
    private final EndpointAnswerHandler putResult = EndpointAnswerHandler.forPositivePutRequest();

    @Getter
    private final EndpointAnswerHandler deleteResult = EndpointAnswerHandler.forPositiveDeleteRequest();

    /**
     * Handles HEAD requests, defaulting to a 200 OK response.
     */
    @Getter
    private final EndpointAnswerHandler headResult = EndpointAnswerHandler.forPositiveGetRequest();

    /**
     * Handles PATCH requests, defaulting to a 200 OK response.
     */
    @Getter
    private final EndpointAnswerHandler patchResult = EndpointAnswerHandler.forPositivePatchRequest();

    /**
     * Handles OPTIONS requests, defaulting to a 200 OK response.
     */
    @Getter
    private final EndpointAnswerHandler optionsResult = EndpointAnswerHandler.forPositiveOptionsRequest();

    /**
     * Registry binding every {@link HttpMethodMapper} constant to its handler. Declared after the
     * per-method fields so they are initialised first. {@link #buildHandlerRegistry()} fails fast when
     * a constant has no handler, so adding an {@link HttpMethodMapper} value without wiring it here is
     * a construction-time error rather than a silent gap.
     */
    private final Map<HttpMethodMapper, EndpointAnswerHandler> handlerRegistry = buildHandlerRegistry();

    private Map<HttpMethodMapper, EndpointAnswerHandler> buildHandlerRegistry() {
        Map<HttpMethodMapper, EndpointAnswerHandler> registry = new EnumMap<>(HttpMethodMapper.class);
        registry.put(HttpMethodMapper.GET, getResult);
        registry.put(HttpMethodMapper.POST, postResult);
        registry.put(HttpMethodMapper.PUT, putResult);
        registry.put(HttpMethodMapper.DELETE, deleteResult);
        registry.put(HttpMethodMapper.HEAD, headResult);
        registry.put(HttpMethodMapper.PATCH, patchResult);
        registry.put(HttpMethodMapper.OPTIONS, optionsResult);

        Set<HttpMethodMapper> missing = EnumSet.allOf(HttpMethodMapper.class);
        missing.removeAll(registry.keySet());
        if (!missing.isEmpty()) {
            throw new IllegalStateException(
                    "No EndpointAnswerHandler registered for HTTP method(s): " + missing
                            + ". Every HttpMethodMapper constant must be wired in buildHandlerRegistry().");
        }
        return registry;
    }

    /**
     * Resets all contained {@link EndpointAnswerHandler}s to their default responses.
     * This is useful when you need to clear any custom responses between tests.
     */
    public void reset() {
        handlerRegistry.values().forEach(EndpointAnswerHandler::resetToDefaultResponse);
    }

    @Override
    public Optional<MockResponse> handleGet(@NonNull RecordedRequest request) {
        return getResult.respond();
    }

    @Override
    public Optional<MockResponse> handlePost(@NonNull RecordedRequest request) {
        return postResult.respond();
    }

    @Override
    public Optional<MockResponse> handleDelete(@NonNull RecordedRequest request) {
        return deleteResult.respond();
    }

    @Override
    public @NonNull Set<HttpMethodMapper> supportedMethods() {
        return Set.of(HttpMethodMapper.values());
    }

    @Override
    public Optional<MockResponse> handlePut(@NonNull RecordedRequest request) {
        return putResult.respond();
    }

    @Override
    public Optional<MockResponse> handleHead(@NonNull RecordedRequest request) {
        return headResult.respond();
    }

    @Override
    public Optional<MockResponse> handlePatch(@NonNull RecordedRequest request) {
        return patchResult.respond();
    }

    @Override
    public Optional<MockResponse> handleOptions(@NonNull RecordedRequest request) {
        return optionsResult.respond();
    }

    /**
     * Sets the result for a certain method
     *
     * @param mapper       One or more mapper to identify the corresponding
     *                     {@link HttpMethodMapper}
     * @param mockResponse maybe null
     */
    public void setMethodToResult(MockResponse mockResponse, HttpMethodMapper... mapper) {
        for (HttpMethodMapper element : mapper) {
            handlerRegistry.get(element).setResponse(mockResponse);
        }
    }

    /**
     * Sets the given MockResponse for all HTTP methods EXCEPT the specified ones.
     * This is useful when you want to configure a default response for most methods
     * while handling specific methods differently.
     *
     * @param mockResponse The MockResponse to set for all non-specified methods, may be null
     * @param mapper       One or more HTTP methods to exclude from this configuration
     */
    public void setAllButGivenMethodToResult(MockResponse mockResponse, HttpMethodMapper... mapper) {
        Set<HttpMethodMapper> all = mutableSet(HttpMethodMapper.values());
        all.removeAll(mutableSortedSet(mapper));
        setMethodToResult(mockResponse, all.toArray(new HttpMethodMapper[0]));
    }

    /**
     * @return A new instance of the BaseAllAcceptDispatcher with a default configuration providing an /api endpoint.
     * This is {@code static} so it can be referenced as a {@code @ModuleDispatcher(providerMethod = ...)} target.
     */
    public static ModuleDispatcherElement getOptimisticAPIDispatcher() {
        return new BaseAllAcceptDispatcher("/api");
    }
}
