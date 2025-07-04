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
package de.cuioss.test.mockwebserver.mockresponse;

import de.cuioss.test.mockwebserver.dispatcher.HttpMethodMapper;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation for defining mock responses in tests. This annotation can be applied to test classes
 * or methods to define mock responses for specific paths and HTTP methods.
 * <p>
 * For each {@code @MockResponseConfig} annotation, a single {@code ModuleDispatcherElement} is created
 * that handles requests matching the specified path and HTTP method.
 * <p>
 * This annotation can be used multiple times on the same element to define multiple mock responses.
 * It can also be combined with {@code @ModuleDispatcher} annotations for more complex scenarios.
 *
 * <h2>Content Configuration</h2>
 * <p>
 * The response content can be specified in one of the following ways:
 * <ul>
 *   <li>{@code textContent}: Sets the response body as plain text with Content-Type text/plain</li>
 *   <li>{@code jsonContentKeyValue}: Sets the response body as JSON with Content-Type application/json.
 *       Uses a simple key-value format (e.g., "key1=value1,key2=value2") that will be converted
 *       to proper JSON.</li>
 *   <li>{@code stringContent}: Sets the response body as a raw string without modifying the Content-Type</li>
 * </ul>
 *
 * <strong>Note:</strong> Only one of {@code textContent}, {@code jsonContentKeyValue}, or {@code stringContent}
 * can be specified for a single annotation.
 *
 * <h2>Examples</h2>
 *
 * Basic usage:
 * <pre>
 * &#64;EnableMockWebServer
 * &#64;MockResponseConfig(path="/api/users", method=HttpMethodMapper.GET,
 *               jsonContentKeyValue="users=[]", status=200)
 * class SimpleTest {
 *     // Test methods
 * }
 * </pre>
 *
 * Multiple responses:
 * <pre>
 * &#64;EnableMockWebServer
 * &#64;MockResponseConfig(path="/api/users", method=HttpMethodMapper.GET,
 *               jsonContentKeyValue="users=[]", status=200)
 * &#64;MockResponseConfig(path="/api/users", method=HttpMethodMapper.POST,
 *               status=201)
 * class MultiResponseTest {
 *     // Test methods
 * }
 * </pre>
 *
 * @author Oliver Wolff
 * @since 1.1
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
@Documented
@Repeatable(MockResponseConfigs.class)
public @interface MockResponseConfig {

    /**
     * The path this response will handle.
     *
     * @return the path, defaults to "/"
     */
    String path() default "/";

    /**
     * The HTTP method this response will handle.
     *
     * @return the HTTP method, defaults to GET
     */
    HttpMethodMapper method() default HttpMethodMapper.GET;

    /**
     * Plain text content (sets Content-Type to text/plain).
     * Mutually exclusive with {@link #jsonContentKeyValue()} and {@link #stringContent()}.
     *
     * @return the plain text content
     */
    String textContent() default "";

    /**
     * JSON content (sets Content-Type to application/json).
     * In the form of key-value pairs.
     * Mutually exclusive with {@link #textContent()} and {@link #stringContent()}.
     * <p>
     * Format: key1=value1,key2=value2 (will be converted to {"key1":"value1","key2":"value2"})
     * In case you need a more complex JSON structure, use {@link #stringContent()} instead.
     *
     * @return the JSON content in key-value format
     */
    String jsonContentKeyValue() default "";

    /**
     * Raw string content for the response body.
     * Mutually exclusive with {@link #textContent()} and {@link #jsonContentKeyValue()}.
     *
     * @return the raw string content
     */
    String stringContent() default "";

    /**
     * HTTP headers in format "name=value".
     * Can be used multiple times.
     *
     * @return the HTTP headers
     */
    String[] headers() default {};

    /**
     * Content type shorthand (adds Content-Type header).
     *
     * @return the content type
     */
    String contentType() default "";

    /**
     * HTTP status code for the response.
     *
     * @return the HTTP status code
     */
    int status();
}
