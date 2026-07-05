/*
 * Copyright © 2025 CUI-OpenSource-Software (info@cuioss.de)
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

import mockwebserver3.Dispatcher;
import mockwebserver3.MockResponse;
import mockwebserver3.RecordedRequest;
import okhttp3.Headers;
import okhttp3.HttpUrl;
import okio.Buffer;
import okio.ByteString;

import java.io.IOException;
import java.util.List;

/**
 * Shared helpers for dispatcher tests: building a {@link RecordedRequest}, dispatching it, and
 * reading a {@link MockResponse} body. Centralised here so the tests do not each hand-build the
 * multi-argument {@code RecordedRequest}.
 */
public final class DispatcherTestSupport {

    private DispatcherTestSupport() {
    }

    /**
     * Creates a {@link RecordedRequest} with the given HTTP method for the given path.
     */
    public static RecordedRequest createRequest(String method, String path) {
        return new RecordedRequest(
                0, 0, null, List.of(),
                method, path, "HTTP/1.1",
                HttpUrl.parse("http://localhost" + path),
                Headers.of("Host", "localhost"),
                ByteString.EMPTY, 0, List.of(), null);
    }

    /**
     * Creates a GET {@link RecordedRequest} for the given path.
     */
    public static RecordedRequest getRequest(String path) {
        return createRequest("GET", path);
    }

    /**
     * Dispatches a GET request for the given path against the dispatcher.
     */
    public static MockResponse dispatch(Dispatcher dispatcher, String path) {
        try {
            return dispatcher.dispatch(getRequest(path));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Dispatch was interrupted", e);
        }
    }

    /**
     * Reads the body of the response as a UTF-8 string, or an empty string if there is no body.
     */
    public static String readBody(MockResponse response) {
        if (response.getBody() == null) {
            return "";
        }
        var buffer = new Buffer();
        try {
            response.getBody().writeTo(buffer);
            return buffer.readUtf8();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read response body", e);
        }
    }
}
