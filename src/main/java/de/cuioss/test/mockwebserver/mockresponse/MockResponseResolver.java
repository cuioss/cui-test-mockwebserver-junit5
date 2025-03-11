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
package de.cuioss.test.mockwebserver.mockresponse;

import de.cuioss.test.mockwebserver.dispatcher.ModuleDispatcherElement;
import de.cuioss.tools.logging.CuiLogger;
import lombok.NonNull;
import lombok.experimental.UtilityClass;
import org.junit.jupiter.api.Nested;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * Utility class for resolving {@link MockResponse} annotations from test classes
 * and converting them to {@link MockResponseDispatcherElement} instances.
 * <p>
 * This class collects annotations from:
 * <ul>
 *   <li>The test class itself</li>
 *   <li>Any nested test classes (annotated with {@link Nested})</li>
 *   <li>Test methods</li>
 * </ul>
 *
 * @author Oliver Wolff
 * @since 1.1
 */
@UtilityClass
public class MockResponseResolver {

    private static final CuiLogger LOGGER = new CuiLogger(MockResponseResolver.class);

    /**
     * Resolves all {@link MockResponse} annotations from the given test class and its methods,
     * and converts them to {@link MockResponseDispatcherElement} instances.
     *
     * @param testClass    the test class to resolve annotations from, must not be null
     * @param testInstance the test instance, must not be null
     * @return a list of {@link ModuleDispatcherElement} instances created from the annotations
     */
    public List<ModuleDispatcherElement> resolveFromAnnotations(
            @NonNull Class<?> testClass,
            @NonNull Object testInstance) {

        List<ModuleDispatcherElement> result = new ArrayList<>();

        // Collect annotations from class hierarchy (including nested classes)
        collectFromClass(testClass, result);

        // Collect annotations from test methods
        collectFromMethods(testClass, result);

        return result;
    }

    /**
     * Collects {@link MockResponse} annotations from the given class and its nested classes.
     *
     * @param clazz  the class to collect annotations from
     * @param result the list to add the created dispatcher elements to
     */
    private void collectFromClass(Class<?> clazz, List<ModuleDispatcherElement> result) {
        // Process direct annotations on the class
        MockResponse[] annotations = clazz.getAnnotationsByType(MockResponse.class);
        for (MockResponse annotation : annotations) {
            try {
                result.add(new MockResponseDispatcherElement(annotation));
            } catch (Exception e) {
                LOGGER.error(e, "Failed to create MockResponseDispatcherElement from annotation on class %s: %s",
                        clazz.getName(), e.getMessage());
            }
        }

        // Process nested classes
        for (Class<?> nestedClass : clazz.getDeclaredClasses()) {
            if (nestedClass.isAnnotationPresent(Nested.class)) {
                collectFromClass(nestedClass, result);
            }
        }
    }

    /**
     * Collects {@link MockResponse} annotations from the methods of the given class.
     *
     * @param clazz  the class to collect method annotations from
     * @param result the list to add the created dispatcher elements to
     */
    private void collectFromMethods(Class<?> clazz, List<ModuleDispatcherElement> result) {
        for (Method method : clazz.getDeclaredMethods()) {
            MockResponse[] annotations = method.getAnnotationsByType(MockResponse.class);
            for (MockResponse annotation : annotations) {
                try {
                    result.add(new MockResponseDispatcherElement(annotation));
                } catch (Exception e) {
                    LOGGER.error("Failed to create MockResponseDispatcherElement from annotation on method %s.%s: %s",
                            clazz.getName(), method.getName(), e.getMessage());
                }
            }
        }

        // Process methods in nested classes
        for (Class<?> nestedClass : clazz.getDeclaredClasses()) {
            if (nestedClass.isAnnotationPresent(Nested.class)) {
                collectFromMethods(nestedClass, result);
            }
        }
    }
}
