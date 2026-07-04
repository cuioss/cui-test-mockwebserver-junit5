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

import mockwebserver3.RecordedRequest;
import org.junit.jupiter.api.Test;

import okhttp3.Headers;
import okhttp3.HttpUrl;
import okio.ByteString;

import java.util.Collections;

import static de.cuioss.test.mockwebserver.dispatcher.CombinedDispatcher.HTTP_CODE_NOT_FOUND;
import static de.cuioss.test.mockwebserver.dispatcher.CombinedDispatcher.HTTP_CODE_TEAPOT;
import static de.cuioss.tools.collect.CollectionLiterals.mutableList;
import static jakarta.servlet.http.HttpServletResponse.SC_OK;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
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

    @Test
    void shouldRejectNullRequest() {
        var dispatcher = new CombinedDispatcher(okDispatcher);
        assertThrows(NullPointerException.class, () -> dispatcher.dispatch(null));
    }

    @Test
    void shouldReturnDefaultForUnsupportedMethod() {
        // PATCH is not a supported HttpMethodMapper: it must fall through to the default response
        // instead of throwing out of dispatch().
        var dispatcher = new CombinedDispatcher(okDispatcher);
        var teapot = assertDoesNotThrow(() -> dispatcher.dispatch(createRequest("PATCH", AllOkDispatcher.BASE + "/x")));
        assertTrue(teapot.getStatus().contains(String.valueOf(HTTP_CODE_TEAPOT)),
                "Unsupported method should yield the teapot default, was: " + teapot.getStatus());

        dispatcher.endWithTeapot(false);
        var notFound = assertDoesNotThrow(() -> dispatcher.dispatch(createRequest("OPTIONS", AllOkDispatcher.BASE + "/x")));
        assertTrue(notFound.getStatus().contains(String.valueOf(HTTP_CODE_NOT_FOUND)),
                "Unsupported method should yield 404 when teapot is disabled, was: " + notFound.getStatus());
    }

    @Test
    void shouldMatchOnSegmentBoundary() {
        var dispatcher = new CombinedDispatcher(new BaseAllAcceptDispatcher("/api"));
        var matched = assertDoesNotThrow(() -> dispatcher.dispatch(createRequest("GET", "/api/users")));
        assertTrue(matched.getStatus().contains(String.valueOf(SC_OK)),
                "/api/users should match base /api, was: " + matched.getStatus());

        var notMatched = assertDoesNotThrow(() -> dispatcher.dispatch(createRequest("GET", "/apiary/list")));
        assertTrue(notMatched.getStatus().contains(String.valueOf(HTTP_CODE_TEAPOT)),
                "/apiary should not match base /api, was: " + notMatched.getStatus());
    }

    private void assertDispatchWithCode(CombinedDispatcher dispatcher, int httpCode, String urlPart) {
        for (HttpMethodMapper mapper : HttpMethodMapper.values()) {
            var request = createRequest(mapper.name(), urlPart + "/someResource");
            assertDoesNotThrow(() -> {
                var result = dispatcher.dispatch(request);
                assertTrue(result.getStatus().contains(String.valueOf(httpCode)),
                        "Status was '" + result.getStatus() + "', expected was: " + httpCode);
            });
        }

    }

    static RecordedRequest createRequest(String method, String target) {
        return new RecordedRequest(
                0, 0, null, Collections.emptyList(),
                method, target, "HTTP/1.1",
                HttpUrl.parse("http://localhost" + target),
                Headers.of("key", "value", "key2", "value2"),
                ByteString.EMPTY, 0, Collections.emptyList(), null);
    }

}
