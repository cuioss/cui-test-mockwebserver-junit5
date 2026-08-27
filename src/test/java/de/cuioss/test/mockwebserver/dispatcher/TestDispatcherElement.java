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

import lombok.NonNull;
import mockwebserver3.MockResponse;
import mockwebserver3.RecordedRequest;

import java.util.Optional;
import java.util.Set;

/**
 * Shared, package-private {@link ModuleDispatcherElement} test fixture used across the
 * {@code DispatcherResolver*} test classes.
 * <p>
 * The fixture supports two modes:
 * <ul>
 *     <li><em>Routing mode</em> (no-arg or {@code (String baseUrl)} constructor): routes GET
 *     requests by path prefix - {@link #TEST_PATH} → "Test Dispatcher", {@link #METHOD_PATH} →
 *     "Method Dispatcher", the configured base URL → "Default Dispatcher Response", otherwise
 *     {@link Optional#empty()}.</li>
 *     <li><em>Fixed-body mode</em> ({@code (String baseUrl, String body)} constructor): every GET
 *     request returns a 200 response carrying the configured body regardless of path.</li>
 * </ul>
 *
 * @author Oliver Wolff
 */
class TestDispatcherElement implements ModuleDispatcherElement {

    static final String TEST_PATH = "/test";
    static final String METHOD_PATH = "/method";

    private final String baseUrl;
    private final String fixedResponseBody;
    private final boolean fixedBody;

    @SuppressWarnings("unused") // Implicitly called by the test framework
    TestDispatcherElement() {
        this("/");
    }

    TestDispatcherElement(String baseUrl) {
        this.baseUrl = baseUrl;
        this.fixedResponseBody = null;
        this.fixedBody = false;
    }

    TestDispatcherElement(String baseUrl, String body) {
        this.baseUrl = baseUrl;
        this.fixedResponseBody = body;
        this.fixedBody = true;
    }

    @Override
    public String getBaseUrl() {
        return baseUrl;
    }

    @Override
    public Optional<MockResponse> handleGet(@NonNull RecordedRequest request) {
        if (fixedBody) {
            return Optional.of(new MockResponse.Builder()
                    .code(200)
                    .body(fixedResponseBody)
                    .build());
        }
        var path = request.getUrl().encodedPath();
        if (path != null) {
            if (path.startsWith(TEST_PATH)) {
                return Optional.of(new MockResponse.Builder()
                        .code(200)
                        .body("Test Dispatcher")
                        .build());
            } else if (path.startsWith(METHOD_PATH)) {
                return Optional.of(new MockResponse.Builder()
                        .code(200)
                        .body("Method Dispatcher")
                        .build());
            } else if (path.startsWith(baseUrl)) {
                return Optional.of(new MockResponse.Builder()
                        .code(200)
                        .body("Default Dispatcher Response")
                        .build());
            }
        }
        return Optional.empty();
    }

    @Override
    public @NonNull Set<HttpMethodMapper> supportedMethods() {
        return Set.of(HttpMethodMapper.GET);
    }
}
