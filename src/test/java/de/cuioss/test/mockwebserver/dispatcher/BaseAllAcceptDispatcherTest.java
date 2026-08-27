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

import mockwebserver3.RecordedRequest;
import org.junit.jupiter.api.Test;

import static de.cuioss.test.mockwebserver.dispatcher.EndpointAnswerHandlerTest.assertMockResponse;
import static de.cuioss.test.mockwebserver.dispatcher.HttpMethodMapper.*;
import static jakarta.servlet.http.HttpServletResponse.*;

class BaseAllAcceptDispatcherTest {

    private static final String DEFAULT_PATH = "/hello";
    static final RecordedRequest DUMMY = DispatcherTestSupport.createRequest("GET", "/");

    @Test
    void shouldDefaultToPositiveResponse() {
        var dispatcher = new BaseAllAcceptDispatcher(DEFAULT_PATH);
        assertMockResponse(dispatcher.handleDelete(DUMMY).get(), SC_NO_CONTENT);
        assertMockResponse(dispatcher.handleGet(DUMMY).get(), SC_OK);
        assertMockResponse(dispatcher.handlePut(DUMMY).get(), SC_CREATED);
        assertMockResponse(dispatcher.handlePost(DUMMY).get(), SC_OK);
        assertMockResponse(dispatcher.handleHead(DUMMY).get(), SC_OK);
    }

    @Test
    void shouldKeepDefaultsWhenResultSetToNull() {
        var dispatcher = new BaseAllAcceptDispatcher(DEFAULT_PATH);

        // Passing a null result leaves every method at its default response
        dispatcher.setMethodToResult(null);
        assertMockResponse(dispatcher.handleDelete(DUMMY).get(), SC_NO_CONTENT);
        assertMockResponse(dispatcher.handleGet(DUMMY).get(), SC_OK);
        assertMockResponse(dispatcher.handlePut(DUMMY).get(), SC_CREATED);
        assertMockResponse(dispatcher.handlePost(DUMMY).get(), SC_OK);
        assertMockResponse(dispatcher.handleHead(DUMMY).get(), SC_OK);
    }

    @Test
    void shouldForbidOnlyTheGivenSubset() {
        var dispatcher = new BaseAllAcceptDispatcher(DEFAULT_PATH);

        // Forbid only GET; every other method must keep its default response
        dispatcher.setMethodToResult(EndpointAnswerHandler.RESPONSE_FORBIDDEN, GET);
        assertMockResponse(dispatcher.handleGet(DUMMY).get(), SC_FORBIDDEN);
        assertMockResponse(dispatcher.handleDelete(DUMMY).get(), SC_NO_CONTENT);
        assertMockResponse(dispatcher.handlePut(DUMMY).get(), SC_CREATED);
        assertMockResponse(dispatcher.handlePost(DUMMY).get(), SC_OK);
        assertMockResponse(dispatcher.handleHead(DUMMY).get(), SC_OK);
    }

    @Test
    void shouldModifyMethodsToForbidden() {
        var dispatcher = new BaseAllAcceptDispatcher(DEFAULT_PATH);

        dispatcher.setMethodToResult(EndpointAnswerHandler.RESPONSE_FORBIDDEN, DELETE, GET, POST, PUT, HEAD);
        assertMockResponse(dispatcher.handleDelete(DUMMY).get(), SC_FORBIDDEN);
        assertMockResponse(dispatcher.handleGet(DUMMY).get(), SC_FORBIDDEN);
        assertMockResponse(dispatcher.handlePut(DUMMY).get(), SC_FORBIDDEN);
        assertMockResponse(dispatcher.handlePost(DUMMY).get(), SC_FORBIDDEN);
        assertMockResponse(dispatcher.handleHead(DUMMY).get(), SC_FORBIDDEN);
    }

    @Test
    void shouldModifyOtherMethodsWithNull() {
        var dispatcher = new BaseAllAcceptDispatcher(DEFAULT_PATH);

        dispatcher.setAllButGivenMethodToResult(EndpointAnswerHandler.RESPONSE_NOT_IMPLEMENTED);
        assertMockResponse(dispatcher.handleDelete(DUMMY).get(), SC_NOT_IMPLEMENTED);
        assertMockResponse(dispatcher.handleGet(DUMMY).get(), SC_NOT_IMPLEMENTED);
        assertMockResponse(dispatcher.handlePut(DUMMY).get(), SC_NOT_IMPLEMENTED);
        assertMockResponse(dispatcher.handlePost(DUMMY).get(), SC_NOT_IMPLEMENTED);
        assertMockResponse(dispatcher.handleHead(DUMMY).get(), SC_NOT_IMPLEMENTED);

        // Reset
        dispatcher.reset();
        assertMockResponse(dispatcher.handleDelete(DUMMY).get(), SC_NO_CONTENT);
        assertMockResponse(dispatcher.handleGet(DUMMY).get(), SC_OK);
        assertMockResponse(dispatcher.handlePut(DUMMY).get(), SC_CREATED);
        assertMockResponse(dispatcher.handlePost(DUMMY).get(), SC_OK);
        assertMockResponse(dispatcher.handleHead(DUMMY).get(), SC_OK);
    }

    @Test
    void shouldKeepDefaultsWhenAllMethodsAreExcluded() {
        var dispatcher = new BaseAllAcceptDispatcher(DEFAULT_PATH);

        // When every method is listed as "given", none fall into the "all but given" set: defaults stay
        dispatcher.setAllButGivenMethodToResult(EndpointAnswerHandler.RESPONSE_FORBIDDEN, DELETE, GET, POST, PUT, HEAD);
        assertMockResponse(dispatcher.handleDelete(DUMMY).get(), SC_NO_CONTENT);
        assertMockResponse(dispatcher.handleGet(DUMMY).get(), SC_OK);
        assertMockResponse(dispatcher.handlePut(DUMMY).get(), SC_CREATED);
        assertMockResponse(dispatcher.handlePost(DUMMY).get(), SC_OK);
        assertMockResponse(dispatcher.handleHead(DUMMY).get(), SC_OK);
    }

    @Test
    void shouldForbidEveryMethodExceptTheGivenSubset() {
        var dispatcher = new BaseAllAcceptDispatcher(DEFAULT_PATH);

        // Forbid all methods except GET; GET keeps its default, the rest become forbidden
        dispatcher.setAllButGivenMethodToResult(EndpointAnswerHandler.RESPONSE_FORBIDDEN, GET);
        assertMockResponse(dispatcher.handleGet(DUMMY).get(), SC_OK);
        assertMockResponse(dispatcher.handleDelete(DUMMY).get(), SC_FORBIDDEN);
        assertMockResponse(dispatcher.handlePut(DUMMY).get(), SC_FORBIDDEN);
        assertMockResponse(dispatcher.handlePost(DUMMY).get(), SC_FORBIDDEN);
        assertMockResponse(dispatcher.handleHead(DUMMY).get(), SC_FORBIDDEN);
    }

}
