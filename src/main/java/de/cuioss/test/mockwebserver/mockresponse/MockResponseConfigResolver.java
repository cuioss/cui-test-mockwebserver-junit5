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

import de.cuioss.test.mockwebserver.dispatcher.ModuleDispatcherElement;
import de.cuioss.tools.logging.CuiLogger;
import lombok.NonNull;
import lombok.experimental.UtilityClass;
import org.junit.jupiter.api.Nested;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
     * Resolves all {@link MockResponseConfig} annotations from the given test class and its methods.
     *
     * @param testClass the test class to resolve annotations from, must not be null
     * @return a list of {@link ModuleDispatcherElement} instances created from the annotations
     */
    public List<ModuleDispatcherElement> resolveFromAnnotations(@NonNull Class<?> testClass) {
        return resolveFromAnnotations(testClass, null);
    }

    /**
     * Resolves {@link MockResponseConfig} annotations from the given test class and test method context.
     * <p>
     * When a test method is provided, only annotations relevant to that method's context are included
     * (the method itself and the class hierarchy up to the test method's class). Annotations are
     * de-duplicated by precedence, so a method-level annotation overrides a class-level one that targets
     * the same path and HTTP method (method &gt; most-specific class &gt; least-specific class).
     *
     * @param testClass  the test class to resolve annotations from, must not be null
     * @param testMethod the current test method context, or null to include all annotations
     * @return a list of {@link ModuleDispatcherElement} instances created from the annotations
     */
    public List<ModuleDispatcherElement> resolveFromAnnotations(@NonNull Class<?> testClass, Method testMethod) {
        List<ModuleDispatcherElement> result = new ArrayList<>();

        if (testMethod == null) {
            // Legacy behavior: collect all annotations from the class hierarchy and all methods
            collectFromClass(testClass, result);
            collectFromMethods(testClass, result);
        } else {
            // Context-aware behavior: collect annotations relevant to the test method,
            // de-duplicated by precedence.
            for (MockResponseConfig annotation : collectContextAware(testMethod)) {
                addConfigElement(annotation, result);
            }
        }

        return result;
    }

    /**
     * Collects the {@link MockResponseConfig} annotations relevant to the given test method, ordered
     * and de-duplicated by precedence (method &gt; most-specific class &gt; least-specific class).
     */
    private List<MockResponseConfig> collectContextAware(Method testMethod) {
        List<LeveledConfig> leveled = new ArrayList<>();

        // Method-level annotations have the highest precedence (level 0)
        for (MockResponseConfig annotation : testMethod.getAnnotationsByType(MockResponseConfig.class)) {
            leveled.add(new LeveledConfig(annotation, 0));
        }

        // Class hierarchy: most-specific first
        int level = 1;
        for (Class<?> clazz : collectContextClasses(testMethod.getDeclaringClass())) {
            for (MockResponseConfig annotation : clazz.getAnnotationsByType(MockResponseConfig.class)) {
                leveled.add(new LeveledConfig(annotation, level));
            }
            level++;
        }

        return deduplicateByPrecedence(leveled);
    }

    /**
     * Returns the classes that make up the context of the given class, ordered most-specific first
     * (the class itself, then its enclosing classes, then its superclasses).
     */
    private List<Class<?>> collectContextClasses(Class<?> clazz) {
        List<Class<?>> classes = new ArrayList<>();
        addContextClasses(clazz, classes);
        return classes;
    }

    private void addContextClasses(Class<?> clazz, List<Class<?>> classes) {
        if (clazz == null || Object.class.equals(clazz) || classes.contains(clazz)) {
            return;
        }
        classes.add(clazz);
        addContextClasses(clazz.getEnclosingClass(), classes);
        addContextClasses(clazz.getSuperclass(), classes);
    }

    /**
     * Keeps, for every path+method combination, only the annotations declared at the highest
     * precedence (lowest level). Two annotations at the same level for the same path+method remain in
     * the result and are later reported as a genuine conflict.
     */
    private List<MockResponseConfig> deduplicateByPrecedence(List<LeveledConfig> leveled) {
        Map<String, Integer> minLevelByKey = new HashMap<>();
        for (LeveledConfig config : leveled) {
            minLevelByKey.merge(key(config.annotation()), config.level(), Math::min);
        }

        List<MockResponseConfig> kept = new ArrayList<>();
        for (LeveledConfig config : leveled) {
            if (minLevelByKey.get(key(config.annotation())) == config.level()) {
                kept.add(config.annotation());
            }
        }
        return kept;
    }

    private String key(MockResponseConfig annotation) {
        return annotation.method() + " " + annotation.path();
    }

    /**
     * Collects {@link MockResponseConfig} annotations from the given class and its nested classes.
     */
    private void collectFromClass(Class<?> clazz, List<ModuleDispatcherElement> result) {
        for (MockResponseConfig annotation : clazz.getAnnotationsByType(MockResponseConfig.class)) {
            addConfigElement(annotation, result);
        }
        for (Class<?> nestedClass : clazz.getDeclaredClasses()) {
            if (nestedClass.isAnnotationPresent(Nested.class)) {
                collectFromClass(nestedClass, result);
            }
        }
    }

    /**
     * Collects {@link MockResponseConfig} annotations from the methods of the given class and its
     * nested classes.
     */
    private void collectFromMethods(Class<?> clazz, List<ModuleDispatcherElement> result) {
        for (Method method : clazz.getDeclaredMethods()) {
            for (MockResponseConfig annotation : method.getAnnotationsByType(MockResponseConfig.class)) {
                addConfigElement(annotation, result);
            }
        }
        for (Class<?> nestedClass : clazz.getDeclaredClasses()) {
            if (nestedClass.isAnnotationPresent(Nested.class)) {
                collectFromMethods(nestedClass, result);
            }
        }
    }

    /**
     * Creates a {@link MockResponseConfigDispatcherElement} from the annotation and adds it to the
     * result list.
     */
    private void addConfigElement(MockResponseConfig annotation, List<ModuleDispatcherElement> result) {
        try {
            result.add(new MockResponseConfigDispatcherElement(annotation));
            LOGGER.debug("Added MockResponseConfig for path %s and method %s", annotation.path(), annotation.method());
        } catch (IllegalArgumentException e) {
            LOGGER.error(e, "Failed to create MockResponseConfigDispatcherElement for path %s: %s",
                    annotation.path(), e.getMessage());
        }
    }

    /**
     * A {@link MockResponseConfig} annotation together with its precedence level (lower is more specific).
     */
    private record LeveledConfig(MockResponseConfig annotation, int level) {
    }
}
