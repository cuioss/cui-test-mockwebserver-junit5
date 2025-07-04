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

import de.cuioss.test.mockwebserver.dispatcher.ModuleDispatcherElement;
import de.cuioss.tools.logging.CuiLogger;
import lombok.NonNull;
import lombok.experimental.UtilityClass;
import org.junit.jupiter.api.Nested;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * Utility class for resolving {@link MockResponseConfig} annotations from test classes
 * and converting them to {@link MockResponseConfigDispatcherElement} instances.
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
public class MockResponseConfigResolver {

    private static final CuiLogger LOGGER = new CuiLogger(MockResponseConfigResolver.class);

    /**
     * Resolves all {@link MockResponseConfig} annotations from the given test class and its methods,
     * and converts them to {@link MockResponseConfigDispatcherElement} instances.
     *
     * @param testClass the test class to resolve annotations from, must not be null
     * @return a list of {@link ModuleDispatcherElement} instances created from the annotations
     */
    public List<ModuleDispatcherElement> resolveFromAnnotations(
            @NonNull Class<?> testClass) {
        return resolveFromAnnotations(testClass, null);
    }

    /**
     * Resolves {@link MockResponseConfig} annotations from the given test class and test method context,
     * and converts them to {@link MockResponseConfigDispatcherElement} instances.
     * <p>
     * When a test method is provided, only annotations relevant to that method's context are included:
     * <ul>
     *   <li>Annotations on the test method itself</li>
     *   <li>Annotations on the test method's containing class and its parent classes</li>
     *   <li>For nested classes, annotations on the class hierarchy up to the test method's class</li>
     * </ul>
     *
     * @param testClass  the test class to resolve annotations from, must not be null
     * @param testMethod the current test method context, or null to include all annotations
     * @return a list of {@link ModuleDispatcherElement} instances created from the annotations
     */
    public List<ModuleDispatcherElement> resolveFromAnnotations(
            @NonNull Class<?> testClass, Method testMethod) {

        List<ModuleDispatcherElement> result = new ArrayList<>();

        if (testMethod == null) {
            // Legacy behavior: collect all annotations
            // Collect annotations from class hierarchy (including nested classes)
            collectFromClass(testClass, result);

            // Collect annotations from test methods
            collectFromMethods(testClass, result);
        } else {
            // Context-aware behavior: only collect annotations relevant to the test method
            // Collect annotations from the test method itself
            collectFromMethod(testMethod, result);

            // Collect annotations from the class hierarchy up to the test method's class
            Class<?> methodClass = testMethod.getDeclaringClass();
            collectFromClassHierarchy(methodClass, result);
        }

        return result;
    }

    /**
     * Collects {@link MockResponseConfig} annotations from the given class and its nested classes.
     *
     * @param clazz  the class to collect annotations from
     * @param result the list to add the created dispatcher elements to
     */
    private void collectFromClass(Class<?> clazz, List<ModuleDispatcherElement> result) {
        // Process direct annotations on the class
        MockResponseConfig[] annotations = clazz.getAnnotationsByType(MockResponseConfig.class);
        for (MockResponseConfig annotation : annotations) {
            try {
                result.add(new MockResponseConfigDispatcherElement(annotation));
            } catch (Exception e) {
                LOGGER.error(e, "Failed to create MockResponseConfigDispatcherElement from annotation on class %s: %s",
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
     * Collects {@link MockResponseConfig} annotations from the class hierarchy up to a specific class.
     * This includes the class itself and all its parent classes, but not sibling classes.
     *
     * @param clazz  the class to collect annotations from
     * @param result the list to add the created dispatcher elements to
     */
    private void collectFromClassHierarchy(Class<?> clazz, List<ModuleDispatcherElement> result) {
        // Process direct annotations on the class
        MockResponseConfig[] annotations = clazz.getAnnotationsByType(MockResponseConfig.class);
        for (MockResponseConfig annotation : annotations) {
            try {
                result.add(new MockResponseConfigDispatcherElement(annotation));
                LOGGER.debug("Added MockResponseConfig from class %s for path %s",
                        clazz.getName(), annotation.path());
            } catch (Exception e) {
                LOGGER.error(e, "Failed to create MockResponseConfigDispatcherElement from annotation on class %s: %s",
                        clazz.getName(), e.getMessage());
            }
        }

        // Process parent class if it's a nested class
        Class<?> enclosingClass = clazz.getEnclosingClass();
        if (enclosingClass != null) {
            collectFromClassHierarchy(enclosingClass, result);
        }

        // Process superclass if not Object
        Class<?> superclass = clazz.getSuperclass();
        if (superclass != null && !Object.class.equals(superclass)) {
            collectFromClassHierarchy(superclass, result);
        }
    }

    /**
     * Collects {@link MockResponseConfig} annotations from a specific method.
     *
     * @param method the method to collect annotations from
     * @param result the list to add the created dispatcher elements to
     */
    private void collectFromMethod(Method method, List<ModuleDispatcherElement> result) {
        MockResponseConfig[] annotations = method.getAnnotationsByType(MockResponseConfig.class);
        for (MockResponseConfig annotation : annotations) {
            try {
                result.add(new MockResponseConfigDispatcherElement(annotation));
                LOGGER.debug("Added MockResponseConfig from method %s.%s for path %s",
                        method.getDeclaringClass().getName(), method.getName(), annotation.path());
            } catch (Exception e) {
                LOGGER.error("Failed to create MockResponseConfigDispatcherElement from annotation on method %s.%s: %s",
                        method.getDeclaringClass().getName(), method.getName(), e.getMessage());
            }
        }
    }

    /**
     * Collects {@link MockResponseConfig} annotations from the methods of the given class.
     *
     * @param clazz  the class to collect method annotations from
     * @param result the list to add the created dispatcher elements to
     */
    private void collectFromMethods(Class<?> clazz, List<ModuleDispatcherElement> result) {
        for (Method method : clazz.getDeclaredMethods()) {
            MockResponseConfig[] annotations = method.getAnnotationsByType(MockResponseConfig.class);
            for (MockResponseConfig annotation : annotations) {
                try {
                    result.add(new MockResponseConfigDispatcherElement(annotation));
                } catch (Exception e) {
                    LOGGER.error("Failed to create MockResponseConfigDispatcherElement from annotation on method %s.%s: %s",
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
