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
package de.cuioss.test.mockwebserver.dispatcher;

import de.cuioss.test.mockwebserver.mockresponse.MockResponseConfig;
import de.cuioss.test.mockwebserver.mockresponse.MockResponseConfigResolver;
import de.cuioss.tools.logging.CuiLogger;
import lombok.NonNull;
import mockwebserver3.Dispatcher;
import org.junit.platform.commons.support.AnnotationSupport;
import org.junit.platform.commons.util.ReflectionUtils;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Resolves dispatchers for MockWebServer tests based on annotations and test class methods.
 * <p>
 * The resolution combines every configured source into a single {@link Dispatcher}:
 * <ol>
 *   <li>A {@link ModuleDispatcher} annotation on the current test method or on the test class
 *       hierarchy (method-level takes precedence over class-level).</li>
 *   <li>A {@code getModuleDispatcher()} method declared on the test class or any of its
 *       superclasses.</li>
 *   <li>{@link MockResponseConfig} annotations resolved for the current test context.</li>
 * </ol>
 * <p>
 * All resolved {@link ModuleDispatcherElement}s are combined into a {@link CombinedDispatcher}.
 * If none of the sources contribute a dispatcher the default {@code /api} dispatcher is used.
 * When a source is <em>explicitly configured</em> but cannot be resolved a
 * {@link DispatcherResolutionException} is thrown instead of silently falling back.
 *
 * @author Oliver Wolff
 * @since 1.1
 */
public class DispatcherResolver {

    private static final CuiLogger LOGGER = new CuiLogger(DispatcherResolver.class);
    private static final String GET_MODULE_DISPATCHER_METHOD = "getModuleDispatcher";

    /**
     * Resolves the dispatcher for a test class.
     *
     * @param testClass    the class of the test
     * @param testInstance the instance of the test, may be {@code null}
     * @return a non-null Dispatcher instance to be used with MockWebServer
     * @since 1.1
     */
    @NonNull
    public Dispatcher resolveDispatcher(Class<?> testClass, Object testInstance) {
        return resolveDispatcher(testClass, testInstance, null);
    }

    /**
     * Resolves the dispatcher for a test class with context awareness for the current test method.
     *
     * @param testClass    the class of the test
     * @param testInstance the instance of the test, may be {@code null}
     * @param testMethod   the current test method, or {@code null} to include all annotations
     * @return a non-null Dispatcher instance to be used with MockWebServer
     * @throws DispatcherResolutionException if an explicitly configured source cannot be resolved
     * @since 1.1
     */
    @NonNull
    public Dispatcher resolveDispatcher(Class<?> testClass, Object testInstance, Method testMethod) {
        LOGGER.debug("Resolving dispatcher for test class: %s", testClass.getName());

        // Resolve the @ModuleDispatcher annotation (if any) exactly once
        AnnotationResolution annotationResolution = findModuleDispatcherAnnotation(testClass, testMethod)
                .map(annotation -> resolveAnnotation(annotation, testClass))
                .orElse(AnnotationResolution.empty());

        // Collect all ModuleDispatcherElements from the configured sources
        List<ModuleDispatcherElement> elements = new ArrayList<>();
        annotationResolution.element().ifPresent(elements::add);
        if (testInstance != null) {
            resolveFromMethod(testInstance).ifPresent(elements::add);
        }
        elements.addAll(MockResponseConfigResolver.resolveFromAnnotations(testClass, testMethod));

        // A provider that returns a raw Dispatcher is exclusive by design
        if (annotationResolution.directDispatcher().isPresent()) {
            if (!elements.isEmpty()) {
                throw new DispatcherResolutionException(
                        "A @ModuleDispatcher provider returning a raw Dispatcher cannot be combined with other " +
                                "dispatcher sources (getModuleDispatcher() or @MockResponseConfig). Remove the additional " +
                                "sources or return a ModuleDispatcherElement instead.");
            }
            LOGGER.debug("Using directly provided Dispatcher");
            return annotationResolution.directDispatcher().get();
        }

        if (!elements.isEmpty()) {
            return createCombinedDispatcher(elements);
        }

        LOGGER.debug("No dispatchers configured, using default /api dispatcher");
        return CombinedDispatcher.createAPIDispatcher();
    }

    /**
     * Finds the applicable {@link ModuleDispatcher} annotation. Method-level annotations take
     * precedence over class-level annotations; the class lookup walks the superclass hierarchy
     * (via {@link AnnotationSupport}) and the enclosing-class chain for {@code @Nested} tests.
     */
    private Optional<ModuleDispatcher> findModuleDispatcherAnnotation(Class<?> testClass, Method testMethod) {
        if (testMethod != null) {
            Optional<ModuleDispatcher> onMethod = AnnotationSupport.findAnnotation(testMethod, ModuleDispatcher.class);
            if (onMethod.isPresent()) {
                LOGGER.debug("Found method-level @ModuleDispatcher on %s", testMethod.getName());
                return onMethod;
            }
        }
        Class<?> current = testClass;
        while (current != null) {
            Optional<ModuleDispatcher> onClass = AnnotationSupport.findAnnotation(current, ModuleDispatcher.class);
            if (onClass.isPresent()) {
                return onClass;
            }
            current = current.getEnclosingClass();
        }
        return Optional.empty();
    }

    /**
     * Resolves a {@link ModuleDispatcher} annotation into either a {@link ModuleDispatcherElement},
     * a raw {@link Dispatcher} (from a provider method), or nothing (bare annotation deferring to
     * {@code getModuleDispatcher()}).
     *
     * @throws DispatcherResolutionException if the explicitly configured value/provider cannot be resolved
     */
    private AnnotationResolution resolveAnnotation(ModuleDispatcher annotation, Class<?> testClass) {
        // Direct class reference
        if (annotation.value() != ModuleDispatcherElement.class) {
            return AnnotationResolution.element(instantiate(annotation.value()));
        }

        // Provider method (on the provider class, or on the test class when no provider is given)
        if (!annotation.providerMethod().isEmpty()) {
            Class<?> providerClass = annotation.provider() != Object.class ? annotation.provider() : testClass;
            Object result = invokeProviderMethod(providerClass, annotation.providerMethod());
            if (result instanceof ModuleDispatcherElement element) {
                return AnnotationResolution.element(element);
            }
            if (result instanceof Dispatcher dispatcher) {
                return AnnotationResolution.direct(dispatcher);
            }
            throw new DispatcherResolutionException(
                    "Provider method %s.%s must return a ModuleDispatcherElement or Dispatcher but returned %s"
                            .formatted(providerClass.getName(), annotation.providerMethod(),
                                    result == null ? "null" : result.getClass().getName()));
        }

        // Provider class without a method name is a misconfiguration
        if (annotation.provider() != Object.class) {
            throw new DispatcherResolutionException(
                    "@ModuleDispatcher declares provider %s but no providerMethod".formatted(annotation.provider().getName()));
        }

        // Bare @ModuleDispatcher -> defer to getModuleDispatcher()
        return AnnotationResolution.empty();
    }

    /**
     * Instantiates a {@link ModuleDispatcherElement} through its no-arg constructor.
     *
     * @throws DispatcherResolutionException if the class cannot be instantiated
     */
    private ModuleDispatcherElement instantiate(Class<? extends ModuleDispatcherElement> dispatcherClass) {
        try {
            Constructor<? extends ModuleDispatcherElement> constructor = dispatcherClass.getDeclaredConstructor();
            return constructor.newInstance();
        } catch (NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException e) {
            throw new DispatcherResolutionException(
                    "Could not instantiate ModuleDispatcherElement %s. A public no-arg constructor is required."
                            .formatted(dispatcherClass.getName()), e);
        }
    }

    /**
     * Looks up and invokes a provider method on the given class, supporting both static and
     * instance methods.
     *
     * @throws DispatcherResolutionException if the method is missing or its invocation fails
     */
    private Object invokeProviderMethod(Class<?> providerClass, String methodName) {
        Method method = ReflectionUtils.findMethod(providerClass, methodName)
                .orElseThrow(() -> new DispatcherResolutionException(
                        "Provider method %s not found on %s".formatted(methodName, providerClass.getName())));
        try {
            Object target = Modifier.isStatic(method.getModifiers()) ? null : ReflectionUtils.newInstance(providerClass);
            return ReflectionUtils.invokeMethod(method, target);
        } catch (RuntimeException e) {
            throw new DispatcherResolutionException(
                    "Provider method %s.%s could not be invoked: %s".formatted(providerClass.getName(), methodName, e.getMessage()), e);
        }
    }

    /**
     * Creates a combined dispatcher from the resolved elements after validating them.
     */
    private Dispatcher createCombinedDispatcher(List<ModuleDispatcherElement> dispatchers) {
        validateDispatchers(dispatchers);
        LOGGER.debug("Creating CombinedDispatcher with %s module dispatchers", dispatchers.size());
        return new CombinedDispatcher().addDispatcher(dispatchers);
    }

    /**
     * Resolves a dispatcher from a {@code getModuleDispatcher()} method declared on the test class
     * or any of its superclasses.
     *
     * @param testInstance the test instance to invoke the method on
     * @return an Optional containing the resolved dispatcher, or empty if the method does not exist
     * @throws DispatcherResolutionException if the method exists but cannot be invoked or returns an invalid value
     */
    private Optional<ModuleDispatcherElement> resolveFromMethod(Object testInstance) {
        Optional<Method> method = ReflectionUtils.findMethod(testInstance.getClass(), GET_MODULE_DISPATCHER_METHOD);
        if (method.isEmpty()) {
            LOGGER.debug("No getModuleDispatcher method found on %s", testInstance.getClass().getName());
            return Optional.empty();
        }
        return Optional.of(invokeModuleDispatcherMethod(testInstance, method.get()));
    }

    /**
     * Invokes the {@code getModuleDispatcher} method and validates its result. Accessibility is
     * handled by {@link ReflectionUtils#invokeMethod(Method, Object, Object...)}.
     */
    private ModuleDispatcherElement invokeModuleDispatcherMethod(Object testInstance, Method method) {
        Object result;
        try {
            result = ReflectionUtils.invokeMethod(method, testInstance);
        } catch (RuntimeException e) {
            throw new DispatcherResolutionException("getModuleDispatcher method threw an exception", e);
        }
        if (result == null) {
            throw new DispatcherResolutionException("getModuleDispatcher method returned null");
        }
        if (result instanceof ModuleDispatcherElement moduleDispatcherElement) {
            LOGGER.debug("Resolved ModuleDispatcherElement from getModuleDispatcher with base URL: %s",
                    moduleDispatcherElement.getBaseUrl());
            return moduleDispatcherElement;
        }
        throw new DispatcherResolutionException(
                "getModuleDispatcher method did not return a ModuleDispatcherElement: " + result.getClass().getName());
    }

    /**
     * Validates that no two dispatchers handle the same path and HTTP method.
     *
     * @throws IllegalStateException if conflicts are found
     */
    private void validateDispatchers(List<ModuleDispatcherElement> dispatchers) {
        Map<String, List<ModuleDispatcherElement>> pathMethodMap = new HashMap<>();
        for (ModuleDispatcherElement dispatcher : dispatchers) {
            for (HttpMethodMapper method : dispatcher.supportedMethods()) {
                pathMethodMap.computeIfAbsent(method + " " + dispatcher.getBaseUrl(), k -> new ArrayList<>()).add(dispatcher);
            }
        }

        List<String> conflicts = pathMethodMap.entrySet().stream()
                .filter(entry -> entry.getValue().size() > 1)
                .map(entry -> entry.getKey() + " handled by " + entry.getValue().size() + " dispatchers")
                .toList();

        if (!conflicts.isEmpty()) {
            String errorMessage = "Dispatcher conflicts found:\n" + String.join("\n", conflicts);
            LOGGER.error(errorMessage);
            throw new IllegalStateException(errorMessage);
        }
    }

    /**
     * The outcome of resolving a {@link ModuleDispatcher} annotation: either a
     * {@link ModuleDispatcherElement}, a raw {@link Dispatcher}, or nothing.
     */
    private record AnnotationResolution(Optional<ModuleDispatcherElement> element,
                                        Optional<Dispatcher> directDispatcher) {

        static AnnotationResolution empty() {
            return new AnnotationResolution(Optional.empty(), Optional.empty());
        }

        static AnnotationResolution element(ModuleDispatcherElement element) {
            return new AnnotationResolution(Optional.of(element), Optional.empty());
        }

        static AnnotationResolution direct(Dispatcher dispatcher) {
            return new AnnotationResolution(Optional.empty(), Optional.of(dispatcher));
        }
    }
}
