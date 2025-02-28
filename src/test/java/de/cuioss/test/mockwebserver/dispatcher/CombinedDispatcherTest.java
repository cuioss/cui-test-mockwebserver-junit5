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

import mockwebserver3.RecordedRequest;
import okhttp3.Headers;
import okio.Buffer;
import org.junit.jupiter.api.Test;

import java.net.Socket;
import java.util.Collections;

import static de.cuioss.test.mockwebserver.dispatcher.CombinedDispatcher.HTTP_CODE_NOT_FOUND;
import static de.cuioss.test.mockwebserver.dispatcher.CombinedDispatcher.HTTP_CODE_TEAPOT;
import static de.cuioss.tools.collect.CollectionLiterals.mutableList;
import static jakarta.servlet.http.HttpServletResponse.SC_OK;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CombinedDispatcherTest {

    private static final AllOkDispatcher okDispatcher = new AllOkDispatcher();

    private static final PassThroughDispatcher passDispatcher = new PassThroughDispatcher();

    @Test
    void shouldHandleConstructor() {
        var dispatcher = new CombinedDispatcher(passDispatcher);
        assertDispatchWithCode(dispatcher, HTTP_CODE_TEAPOT, passDispatcher.getBaseUrl());

        dispatcher = new CombinedDispatcher(passDispatcher).endWithTeapot(false);
        assertDispatchWithCode(dispatcher, HTTP_CODE_NOT_FOUND, passDispatcher.getBaseUrl());

        dispatcher = new CombinedDispatcher(okDispatcher, okDispatcher);
        assertDispatchWithCode(dispatcher, SC_OK, okDispatcher.getBaseUrl());
    }

    @Test
    void shouldHandleBuilderVariants() {
        var dispatcher = new CombinedDispatcher();
        assertDispatchWithCode(dispatcher, HTTP_CODE_TEAPOT, passDispatcher.getBaseUrl());

        dispatcher = new CombinedDispatcher().addDispatcher(okDispatcher);
        assertDispatchWithCode(dispatcher, SC_OK, okDispatcher.getBaseUrl());

        dispatcher = new CombinedDispatcher().addDispatcher(okDispatcher, okDispatcher);
        assertDispatchWithCode(dispatcher, SC_OK, okDispatcher.getBaseUrl());

        dispatcher = new CombinedDispatcher().addDispatcher(mutableList(okDispatcher, okDispatcher));
        assertDispatchWithCode(dispatcher, SC_OK, okDispatcher.getBaseUrl());

    }

    @Test
    void shouldHandleMissingFilter() {
        var dispatcher = new CombinedDispatcher().addDispatcher(okDispatcher);
        assertDispatchWithCode(dispatcher, HTTP_CODE_TEAPOT, "/notThere");
    }

    private void assertDispatchWithCode(CombinedDispatcher dispatcher, int httpCode, String urlPart) {
        for (HttpMethodMapper mapper : HttpMethodMapper.values()) {
            var request = createRequestFor(mapper, urlPart);
            assertDoesNotThrow(() -> {
                var result = dispatcher.dispatch(request);
                assertTrue(result.getStatus().contains(String.valueOf(httpCode)),
                        "Status was '" + result.getStatus() + "', expected was: " + httpCode);
            });
        }

    }

    static RecordedRequest createRequestFor(HttpMethodMapper mapper, String urlPart) {
        return new RecordedRequest(mapper.name() + " " + urlPart + "someResource  HTTP/1.1",
                Headers.of("key=value", "key2=value2"), Collections.emptyList(), 0, new Buffer(), 0, new Socket(), null);
    }

}
