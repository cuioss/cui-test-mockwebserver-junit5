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

import lombok.NonNull;
import mockwebserver3.MockResponse;
import mockwebserver3.RecordedRequest;

import java.util.Optional;

import static jakarta.servlet.http.HttpServletResponse.SC_OK;

public class AllOkDispatcher implements ModuleDispatcherElement {

    public static final String BASE = "/allOk";

    private static final MockResponse OK_RESPONSE = new MockResponse.Builder().addHeader("Content-Type", "application/json").code(SC_OK).build();

    @Override
    public String getBaseUrl() {
        return BASE;
    }

    @Override
    public Optional<MockResponse> handleDelete(@NonNull RecordedRequest request) {
        return Optional.of(OK_RESPONSE);
    }

    @Override
    public Optional<MockResponse> handleGet(@NonNull RecordedRequest request) {
        return Optional.of(OK_RESPONSE);
    }

    @Override
    public Optional<MockResponse> handlePost(@NonNull RecordedRequest request) {
        return Optional.of(OK_RESPONSE);
    }

    @Override
    public Optional<MockResponse> handlePut(@NonNull RecordedRequest request) {
        return Optional.of(OK_RESPONSE);
    }

}
