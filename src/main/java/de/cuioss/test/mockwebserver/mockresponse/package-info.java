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
/**
 * Declarative mock-response configuration via the
 * {@link de.cuioss.test.mockwebserver.mockresponse.MockResponseConfig} annotation.
 *
 * <h2>Contents</h2>
 * <ul>
 *   <li>{@link de.cuioss.test.mockwebserver.mockresponse.MockResponseConfig} - annotation declaring a
 *       response for a specific path and HTTP method; repeatable via
 *       {@link de.cuioss.test.mockwebserver.mockresponse.MockResponseConfigs}.</li>
 *   <li>{@link de.cuioss.test.mockwebserver.mockresponse.MockResponseConfigResolver} - collects the
 *       (class- and method-level) annotations for the current test.</li>
 *   <li>{@link de.cuioss.test.mockwebserver.mockresponse.MockResponseConfigDispatcherElement} - serves
 *       the configured responses as a {@code ModuleDispatcherElement}.</li>
 * </ul>
 *
 * @author Oliver Wolff
 * @see de.cuioss.test.mockwebserver
 * @since 1.1
 */
package de.cuioss.test.mockwebserver.mockresponse;
