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

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation for configuring {@link ModuleDispatcherElement} instances for tests.
 * <p>
 * This annotation can be used at the class level to specify which dispatcher(s)
 * should be used for handling HTTP requests in tests. It provides several ways
 * to configure dispatchers:
 * <ul>
 *   <li>Direct class reference: {@code @ModuleDispatcher(UserApiDispatcher.class)}</li>
 *   <li>Provider class and method: {@code @ModuleDispatcher(provider=DispatcherFactory.class, providerMethod="createApiDispatcher")}</li>
 *   <li>Test class method: When used without parameters, looks for a {@code getModuleDispatcher()} method in the test class</li>
 * </ul>
 * 
 * <h2>Basic Usage</h2>
 * <pre>
 * {@code
 * @EnableMockWebServer
 * @ModuleDispatcher(UserApiDispatcher.class)
 * class SimpleTest {
 *     // Test methods
 * }
 * }
 * </pre>
 * 
 * <h2>Method Provider</h2>
 * <pre>
 * {@code
 * @EnableMockWebServer
 * @ModuleDispatcher(provider = DispatcherFactory.class, providerMethod = "createApiDispatcher")
 * class FactoryTest {
 *     // Test methods
 * }
 * }
 * </pre>
 * 
 * <h2>Custom Method in Test Class</h2>
 * <pre>
 * {@code
 * @EnableMockWebServer
 * @ModuleDispatcher
 * class CustomTest {
 *     // This method will be called to get the dispatcher
 *     ModuleDispatcherElement getModuleDispatcher() {
 *         return new UserApiDispatcher();
 *     }
 *     
 *     // Test methods
 * }
 * }
 * </pre>
 *
 * @author Oliver Wolff
 * @since 1.1
 * @see ModuleDispatcherElement
 * @see CombinedDispatcher
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
@Documented
public @interface ModuleDispatcher {

    /**
     * The dispatcher class to use (must have a no-arg constructor).
     * If not specified, the test class must provide a getModuleDispatcher() method.
     * 
     * @return the dispatcher class
     */
    Class<? extends ModuleDispatcherElement> value() default ModuleDispatcherElement.class;

    /**
     * Optional provider class that has a method to create the dispatcher.
     * 
     * @return the provider class
     */
    Class<?> provider() default Object.class;

    /**
     * Optional provider method name (static or instance method).
     * <p>
     * This method must return a {@link ModuleDispatcherElement} instance and must be present
     * in the class specified by {@link #provider()}. If the method is not static, a no-arg
     * constructor must be available for the provider class.
     * <p>
     * Only used when {@link #provider()} is specified.
     * 
     * @return the provider method name
     * @since 1.1
     */
    String providerMethod() default "";
}
