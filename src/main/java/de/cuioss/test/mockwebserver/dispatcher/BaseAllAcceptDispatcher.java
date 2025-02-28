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

import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import mockwebserver3.MockResponse;
import mockwebserver3.RecordedRequest;

import java.util.Arrays;
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
 *   <li>Pre-configured positive responses for GET, POST, PUT, DELETE</li>
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
 *     new MockResponse().setBody("{'data': 'test'}")
 * );
 *
 * // Reset to default responses
 * dispatcher.reset();
 * </pre>
 *
 * <h2>Default Responses</h2>
 * <ul>
 *   <li>GET: 200 OK with empty body</li>
 *   <li>POST: 201 Created</li>
 *   <li>PUT: 204 No Content</li>
 *   <li>DELETE: 204 No Content</li>
 * </ul>
 *
 * @author Oliver Wolff
 * @since 1.0
 * @see EndpointAnswerHandler
 * @see ModuleDispatcherElement
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
     * Resets all contained {@link EndpointAnswerHandler}s to their default responses.
     * This is useful when you need to clear any custom responses between tests.
     */
    public void reset() {
        getResult.resetToDefaultResponse();
        postResult.resetToDefaultResponse();
        putResult.resetToDefaultResponse();
        deleteResult.resetToDefaultResponse();
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
    public Optional<MockResponse> handlePut(@NonNull RecordedRequest request) {
        return putResult.respond();
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
            switch (element) {
                case GET:
                    getResult.setResponse(mockResponse);
                    break;
                case POST:
                    postResult.setResponse(mockResponse);
                    break;
                case PUT:
                    putResult.setResponse(mockResponse);
                    break;
                case DELETE:
                    deleteResult.setResponse(mockResponse);
                    break;
                default:
                    throw new IllegalArgumentException("Unknown method: " + Arrays.toString(mapper));
            }
        }
    }

    /**
     * Sets the result for all but the given
     *
     * @param mapper       One or more mapper to identify the corresponding
     *                     {@link HttpMethodMapper}
     * @param mockResponse maybe null
     */
    public void setAllButGivenMethodToResult(MockResponse mockResponse, HttpMethodMapper... mapper) {
        Set<HttpMethodMapper> all = mutableSet(HttpMethodMapper.values());
        all.removeAll(mutableSortedSet(mapper));
        setMethodToResult(mockResponse, all.toArray(new HttpMethodMapper[0]));
    }
}
