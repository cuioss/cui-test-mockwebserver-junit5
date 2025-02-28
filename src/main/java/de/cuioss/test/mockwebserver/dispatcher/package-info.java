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
