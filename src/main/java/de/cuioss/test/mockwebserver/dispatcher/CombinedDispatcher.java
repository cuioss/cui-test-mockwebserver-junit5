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
import de.cuioss.tools.collect.CollectionLiterals;
import de.cuioss.tools.logging.CuiLogger;
import de.cuioss.tools.string.MoreStrings;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import mockwebserver3.Dispatcher;
import mockwebserver3.MockResponse;
import mockwebserver3.RecordedRequest;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Combines multiple {@link ModuleDispatcherElement}s into a single dispatcher for handling HTTP requests
 * in test scenarios. This dispatcher implements a chain-of-responsibility pattern, trying each module
 * dispatcher in sequence until one handles the request.
 *
 * <h2>Features</h2>
 * <ul>
 *   <li>Combines multiple dispatchers into a single unit</li>
 *   <li>Supports dynamic addition of dispatchers</li>
 *   <li>Configurable 404/418 response for unhandled requests</li>
 *   <li>Path-based request routing</li>
 * </ul>
 *
 * <h2>Usage Example</h2>
 * <pre>
 * var dispatcher = new CombinedDispatcher()
 *     .addDispatcher(new UserApiDispatcher())
 *     .addDispatcher(new ProductApiDispatcher())
 *     .setEndWithTeapot(false); // Use 404 for unhandled requests
 *
 * &#64;EnableMockWebServer
 * class ApiTest implements MockWebServerHolder {
 *     &#64;Override
 *     public Dispatcher getDispatcher() {
 *         return dispatcher;
 *     }
 * }
 * </pre>
 *
 * <h2>Request Handling</h2>
 * <ul>
 *   <li>Requests are matched against each dispatcher's base URL</li>
 *   <li>First matching dispatcher handles the request</li>
 *   <li>Unhandled requests return 418 (default) or 404</li>
 *   <li>Supports all standard HTTP methods</li>
 * </ul>
 *
 * @author Oliver Wolff
 * @see ModuleDispatcherElement
 * @see EnableMockWebServer
 * @since 1.0
 */
@NoArgsConstructor
public class CombinedDispatcher extends Dispatcher {

    public static final int HTTP_CODE_NOT_FOUND = 404;

    public static final int HTTP_CODE_TEAPOT = 418;

    private static final CuiLogger LOGGER = new CuiLogger(CombinedDispatcher.class);

    /**
     * If set to {@code true} on mismatch of the request will return a Http-Code
     * '418', '404' otherwise
     */
    private boolean endWithTeapot = true;

    private final List<ModuleDispatcherElement> singleDispatcher = new ArrayList<>();

    /**
     * @param dispatcherElement to be used
     */
    public CombinedDispatcher(ModuleDispatcherElement dispatcherElement) {
        singleDispatcher.add(dispatcherElement);
    }

    /**
     * @param dispatcherElement to be used
     */
    public CombinedDispatcher(ModuleDispatcherElement... dispatcherElement) {
        singleDispatcher.addAll(CollectionLiterals.mutableList(dispatcherElement));
    }

    @Override
    public @NotNull MockResponse dispatch(@NonNull RecordedRequest request) {
        var path = MoreStrings.nullToEmpty(request.getPath());
        var mapper = HttpMethodMapper.of(request);
        LOGGER.info("Processing method '{}' with path '{}'", mapper, path);

        List<ModuleDispatcherElement> filtered = new ArrayList<>();

        for (ModuleDispatcherElement dispatcher : singleDispatcher) {
            if (path.startsWith(dispatcher.getBaseUrl())) {
                filtered.add(dispatcher);
            } else {
                LOGGER.info(dispatcher.getBaseUrl());
            }
        }

        for (ModuleDispatcherElement moduleDispatcherElement : filtered) {
            var result = mapper.handleMethod(moduleDispatcherElement, request);
            if (result.isPresent()) {
                return result.get();
            }
        }
        LOGGER.info(
                "Method '{}' with path '{}' could not be processed by the configured ModuleDispatcherElements. Going to default",
                mapper, path);
        var code = HTTP_CODE_TEAPOT;
        if (!endWithTeapot) {
            code = HTTP_CODE_NOT_FOUND;
        }
        return new MockResponse(code);
    }

    /**
     * @param endWithTeapot If set to {@code true} on mismatch of the request will
     *                      return a Http-Code '418', '404' otherwise
     * @return The instance itself to use it in a builder-style
     */
    public CombinedDispatcher endWithTeapot(boolean endWithTeapot) {
        this.endWithTeapot = endWithTeapot;
        return this;
    }

    /**
     * @param dispatcherElement must not be null
     * @return The instance itself to use it in a builder-style
     */
    public CombinedDispatcher addDispatcher(@NonNull ModuleDispatcherElement dispatcherElement) {
        singleDispatcher.add(dispatcherElement);
        return this;
    }

    /**
     * @param dispatcherElements must not be null
     * @return The instance itself to use it in a builder-style
     */
    public CombinedDispatcher addDispatcher(@NonNull List<ModuleDispatcherElement> dispatcherElements) {
        singleDispatcher.addAll(dispatcherElements);
        return this;
    }

    /**
     * @param dispatcherElements to be added
     * @return The instance itself to use it in a builder-style
     */
    public CombinedDispatcher addDispatcher(ModuleDispatcherElement... dispatcherElements) {
        singleDispatcher.addAll(CollectionLiterals.mutableList(dispatcherElements));
        return this;
    }

    /**
     * @return A new instance of the CombinedDispatcher with a default configuration providing an /api endpoint
     */
    public static CombinedDispatcher createAPIDispatcher() {
        return new CombinedDispatcher(new BaseAllAcceptDispatcher("/api"));
    }

}
