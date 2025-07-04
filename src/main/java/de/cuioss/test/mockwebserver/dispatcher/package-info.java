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
/**
 * Request dispatching components for MockWebServer testing, providing modular
 * and configurable HTTP request handling.
 *
 * <h2>Package Organization</h2>
 * <ul>
 *   <li>Base Components
 *     <ul>
 *       <li>{@link de.cuioss.test.mockwebserver.dispatcher.BaseAllAcceptDispatcher} - Default response handling</li>
 *       <li>{@link de.cuioss.test.mockwebserver.dispatcher.ModuleDispatcherElement} - Dispatcher interface</li>
 *     </ul>
 *   </li>
 *   <li>Response Configuration
 *     <ul>
 *       <li>{@link de.cuioss.test.mockwebserver.dispatcher.EndpointAnswerHandler} - Response configuration</li>
 *       <li>{@link de.cuioss.test.mockwebserver.dispatcher.CombinedDispatcher} - Multi-dispatcher support</li>
 *     </ul>
 *   </li>
 * </ul>
 *
 * <h2>Key Concepts</h2>
 * <ul>
 *   <li><strong>Modular Design</strong> - Composable dispatcher elements</li>
 *   <li><strong>Response Handling</strong> - Flexible response configuration</li>
 *   <li><strong>Method Support</strong> - Complete HTTP method coverage</li>
 * </ul>
 * 
 * <h2>Creating MockResponse Objects</h2>
 * <p>
 * Always use the Builder pattern for creating MockResponse objects and include appropriate Content-Type headers:
 * <pre>
 * MockResponse response = new MockResponse.Builder()
 *         .addHeader("Content-Type", "application/json")
 *         .body(jsonContentKeyValue)
 *         .code(HttpServletResponse.SC_OK)
 *         .build();
 * </pre>
 * 
 * <p>The {@link de.cuioss.test.mockwebserver.dispatcher.EndpointAnswerHandler} class provides pre-configured
 * responses like {@code RESPONSE_OK}, {@code RESPONSE_NO_CONTENT}, etc. that follow this pattern.
 * <p>
 * For detailed documentation and examples, see:
 * <ul>
 *   <li>{@link de.cuioss.test.mockwebserver.dispatcher.BaseAllAcceptDispatcher} for basic usage</li>
 *   <li>{@link de.cuioss.test.mockwebserver.dispatcher.EndpointAnswerHandler} for response configuration</li>
 * </ul>
 *
 * @author Oliver Wolff
 * @see de.cuioss.test.mockwebserver
 * @see mockwebserver3.Dispatcher
 * @since 1.0
 */
package de.cuioss.test.mockwebserver.dispatcher;
