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
package de.cuioss.test.mockwebserver.mockresponse;

import de.cuioss.test.mockwebserver.dispatcher.DispatcherTestSupport;
import de.cuioss.test.mockwebserver.dispatcher.HttpMethodMapper;
import de.cuioss.test.mockwebserver.dispatcher.ModuleDispatcherElement;
import mockwebserver3.MockResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Verifies that context-aware {@link MockResponseConfig} resolution de-duplicates by precedence so a
 * method-level annotation overrides a class-level one for the same path and HTTP method (finding C4).
 *
 * @author Oliver Wolff
 */
@DisplayName("MockResponseConfig precedence resolution")
class MockResponseConfigPrecedenceTest {

    @MockResponseConfig(path = "/data", method = HttpMethodMapper.GET, textContent = "class-level", status = 200)
    static class OverrideFixture {
        @MockResponseConfig(path = "/data", method = HttpMethodMapper.GET, textContent = "method-level", status = 200)
        void overriding() {
            // annotation carrier only
        }
    }

    @MockResponseConfig(path = "/class-only", method = HttpMethodMapper.GET, textContent = "class", status = 200)
    static class DistinctFixture {
        @MockResponseConfig(path = "/method-only", method = HttpMethodMapper.GET, textContent = "method", status = 200)
        void distinct() {
            // annotation carrier only
        }
    }

    @Test
    @DisplayName("Method-level annotation overrides class-level for the same path and method")
    void methodOverridesClass() throws NoSuchMethodException {
        Method method = OverrideFixture.class.getDeclaredMethod("overriding");
        List<ModuleDispatcherElement> elements =
                MockResponseConfigResolver.resolveFromAnnotations(OverrideFixture.class, method);

        assertEquals(1, elements.size(), "Duplicate path+method must be de-duplicated to a single element");
        assertEquals("method-level", body(elements.get(0), "/data"),
                "The method-level annotation must win over the class-level one");
    }

    @Test
    @DisplayName("Distinct path+method combinations are all retained")
    void distinctCombinationsRetained() throws NoSuchMethodException {
        Method method = DistinctFixture.class.getDeclaredMethod("distinct");
        List<ModuleDispatcherElement> elements =
                MockResponseConfigResolver.resolveFromAnnotations(DistinctFixture.class, method);

        assertEquals(2, elements.size(), "Distinct path+method combinations must be kept");
    }

    @Test
    @DisplayName("Legacy resolution (no test method) collects class and method annotations")
    void legacyCollectsAll() {
        List<ModuleDispatcherElement> elements =
                MockResponseConfigResolver.resolveFromAnnotations(DistinctFixture.class);

        assertEquals(2, elements.size(), "Legacy mode collects both the class and method annotation");
    }

    private static String body(ModuleDispatcherElement element, String path) {
        MockResponse response = element.handleGet(DispatcherTestSupport.getRequest(path)).orElseThrow();
        return DispatcherTestSupport.readBody(response);
    }
}
