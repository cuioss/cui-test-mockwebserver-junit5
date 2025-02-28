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
package de.cuioss.test.mockwebserver;

import lombok.Getter;
import lombok.Setter;
import mockwebserver3.Dispatcher;
import mockwebserver3.MockResponse;
import mockwebserver3.MockWebServer;
import mockwebserver3.RecordedRequest;
import okhttp3.Headers;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@EnableMockWebServer(manualStart = true)
class ManualStartMockWebServerTest implements MockWebServerHolder {

    @Getter
    @Setter
    private MockWebServer mockWebServer;

    @Override
    public Dispatcher getDispatcher() {
        return new Dispatcher() {
            @Override
            public @NotNull MockResponse dispatch(final @NotNull RecordedRequest request) {
                return new MockResponse(200, Headers.of("Content-Type", "text/plain"), "OK");
            }
        };
    }

    @Test
    void shouldInjectServerForManualStart() {
        assertNotNull(mockWebServer, "Server should be injected even for manual start");
    }
}
