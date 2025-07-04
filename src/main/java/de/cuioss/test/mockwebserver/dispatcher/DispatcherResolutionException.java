/**
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

import java.io.Serial;

/**
 * Exception thrown when there is an error resolving a dispatcher for MockWebServer tests.
 * <p>
 * This exception is used to indicate problems with dispatcher resolution, such as:
 * <ul>
 *   <li>Method accessibility issues</li>
 *   <li>Invalid return types from provider methods</li>
 *   <li>Null return values from provider methods</li>
 *   <li>Exceptions thrown by provider methods</li>
 *   <li>Security violations when accessing provider methods</li>
 * </ul>
 *
 * @author Oliver Wolff
 * @since 1.1
 */
public class DispatcherResolutionException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * Constructs a new exception with the specified detail message.
     *
     * @param message the detail message
     */
    public DispatcherResolutionException(String message) {
        super(message);
    }

    /**
     * Constructs a new exception with the specified detail message and cause.
     *
     * @param message the detail message
     * @param cause   the cause
     */
    public DispatcherResolutionException(String message, Throwable cause) {
        super(message, cause);
    }
}
