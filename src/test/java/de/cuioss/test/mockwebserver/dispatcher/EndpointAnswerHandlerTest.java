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

import mockwebserver3.MockResponse;
import org.junit.jupiter.api.Test;

import static jakarta.servlet.http.HttpServletResponse.*;
import static org.junit.jupiter.api.Assertions.*;

class EndpointAnswerHandlerTest {

    @Test
    void shouldHandleDefaults() {
        assertMockResponse(EndpointAnswerHandler.forPositiveGetRequest().getResponse(), SC_OK);
        assertMockResponse(EndpointAnswerHandler.forPositiveDeleteRequest().getResponse(), SC_NO_CONTENT);
        assertMockResponse(EndpointAnswerHandler.forPositivePutRequest().getResponse(), SC_CREATED);
        assertMockResponse(EndpointAnswerHandler.forPositivePostRequest().getResponse(), SC_OK);
    }

    @Test
    void shouldChangeExistingResponse() {
        var handler = EndpointAnswerHandler.forPositiveGetRequest();
        assertMockResponse(handler.respond().get(), SC_OK);
        handler.respondCreated();
        assertMockResponse(handler.respond().get(), SC_CREATED);
        handler.respondForbidden();
        assertMockResponse(handler.respond().get(), SC_FORBIDDEN);
        handler.respondNoContent();
        assertMockResponse(handler.respond().get(), SC_NO_CONTENT);
        handler.respondNotFound();
        assertMockResponse(handler.respond().get(), SC_NOT_FOUND);
        handler.respondNotImplemented();
        assertMockResponse(handler.respond().get(), SC_NOT_IMPLEMENTED);
        handler.respondOk();
        assertMockResponse(handler.respond().get(), SC_OK);
        handler.respondUnauthorized();
        assertMockResponse(handler.respond().get(), SC_UNAUTHORIZED);
        handler.respondMovedPermanently();
        assertMockResponse(handler.respond().get(), SC_MOVED_PERMANENTLY);
        handler.respondMovedTemporarily();
        assertMockResponse(handler.respond().get(), SC_MOVED_TEMPORARILY);
        // Now check Reset
        handler.resetToDefaultResponse();
        assertMockResponse(handler.respond().get(), SC_OK);
    }

    @Test
    void shouldHandleNullResponse() {
        var handler = EndpointAnswerHandler.noContent(HttpMethodMapper.DELETE);
        assertTrue(handler.respond().isEmpty());
        handler.respondCreated();
        assertFalse(handler.respond().isEmpty());
        handler.resetToDefaultResponse();
        assertTrue(handler.respond().isEmpty());
    }

    static void assertMockResponse(MockResponse mockResponse, int httpCode) {
        assertNotNull(mockResponse);
        assertTrue(mockResponse.getStatus().contains(String.valueOf(httpCode)),
                mockResponse.getStatus() + " does not contain " + httpCode);
    }
}
