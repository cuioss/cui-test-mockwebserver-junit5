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

import de.cuioss.test.mockwebserver.MockWebServerHolder;
import de.cuioss.test.mockwebserver.mockresponse.MockResponseResolver;
import de.cuioss.tools.logging.CuiLogger;
import lombok.NonNull;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.platform.commons.support.AnnotationSupport;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;


import mockwebserver3.Dispatcher;

@SuppressWarnings("deprecation") // Using deprecated methods for backward compatibility with MockWebServerHolder

/**
 * Resolves dispatchers for MockWebServer tests based on annotations and test class methods.
 * <p>
 * This class handles the resolution of dispatchers from:
 * <ul>
 *   <li>{@link ModuleDispatcher} annotations on test classes</li>
 *   <li>Methods in test classes that return {@link ModuleDispatcherElement} instances</li>
 *   <li>Legacy {@link MockWebServerHolder#getDispatcher()} implementations</li>
 * </ul>
 *
 * @author Oliver Wolff
 * @since 1.1
 */
public class DispatcherResolver {

    private static final CuiLogger LOGGER = new CuiLogger(DispatcherResolver.class);
    private static final String GET_MODULE_DISPATCHER_METHOD = "getModuleDispatcher";
    private static final String EXCEPTION_DETAILS = "Exception details";

    /**
     * Resolves the dispatcher for a test class.
     * <p>
     * This method implements the following resolution strategy:
     * <ol>
     *   <li>Check for {@link ModuleDispatcher} annotations on the test class</li>
     *   <li>Check for a {@code getModuleDispatcher()} method in the test class</li>
     *   <li>Check if the test class implements {@link MockWebServerHolder} and has a non-null dispatcher</li>
     *   <li>If no dispatcher is found, fall back to the default API dispatcher</li>
     * </ol>
     * <p>
     * If multiple dispatchers are found (e.g., from annotations and methods), they will be combined
     * using {@link CombinedDispatcher}.
     *
     * @param testClass        the class of the test
     * @param testInstance     the instance of the test
     * @param extensionContext the JUnit extension context (not used currently)
     * @return a non-null Dispatcher instance to be used with MockWebServer
     * @since 1.1
     */
    @NonNull
    public Dispatcher resolveDispatcher(Class<?> testClass, Object testInstance, ExtensionContext extensionContext) {
        LOGGER.debug("Resolving dispatcher for test class: %s", testClass.getName());

        // Collect all dispatchers from different sources
        List<ModuleDispatcherElement> dispatchers = new ArrayList<>();

        // Check for @ModuleDispatcher annotation
        Optional<ModuleDispatcher> moduleDispatcherAnnotation =
                AnnotationSupport.findAnnotation(testClass, ModuleDispatcher.class);

        if (moduleDispatcherAnnotation.isPresent()) {
            LOGGER.debug("Found @ModuleDispatcher annotation on test class: %s", testClass.getName());
            Optional<ModuleDispatcherElement> dispatcher =
                    resolveFromAnnotation(moduleDispatcherAnnotation.get());
            dispatcher.ifPresent(dispatchers::add);
        }

        // Check for getModuleDispatcher method
        Optional<ModuleDispatcherElement> methodDispatcher = resolveFromMethod(testInstance);
        methodDispatcher.ifPresent(dispatchers::add);

        // Check for @MockResponse annotations
        List<ModuleDispatcherElement> mockResponseDispatchers =
                MockResponseResolver.resolveFromAnnotations(testClass, testInstance);
        if (!mockResponseDispatchers.isEmpty()) {
            LOGGER.debug("Found %d @MockResponse annotations on test class: %s",
                    mockResponseDispatchers.size(), testClass.getName());
            dispatchers.addAll(mockResponseDispatchers);
        }

        // Legacy support: check if the test class implements MockWebServerHolder
        if (dispatchers.isEmpty() && testInstance instanceof MockWebServerHolder holder) {
            LOGGER.debug("Test class implements MockWebServerHolder, checking for dispatcher");
            // Using deprecated method for backward compatibility
            @SuppressWarnings({"deprecation", "removal"})
            Dispatcher legacyDispatcher = holder.getDispatcher();
            if (legacyDispatcher != null) {
                LOGGER.debug("Using legacy dispatcher from MockWebServerHolder.getDispatcher()");
                return legacyDispatcher;
            }
        }

        // If we have dispatchers, validate and combine them
        if (!dispatchers.isEmpty()) {
            // Validate uniqueness of path+method combinations
            validateDispatchers(dispatchers);

            // Log active dispatcher elements
            logActiveDispatchers(dispatchers);

            LOGGER.debug("Creating CombinedDispatcher with %d module dispatchers", dispatchers.size());
            CombinedDispatcher combinedDispatcher = new CombinedDispatcher();
            combinedDispatcher.addDispatcher(dispatchers);
            return combinedDispatcher;
        }

        // Fallback to default API dispatcher
        LOGGER.debug("No dispatchers found, using default API dispatcher");
        return CombinedDispatcher.createAPIDispatcher();
    }

    /**
     * Resolves a dispatcher from a {@link ModuleDispatcher} annotation.
     * <p>
     * This method attempts to create a {@link ModuleDispatcherElement} instance using one of the following strategies:
     * <ol>
     *   <li>If {@link ModuleDispatcher#value()} is specified, create an instance of that class using its no-arg constructor</li>
     *   <li>If {@link ModuleDispatcher#provider()} and {@link ModuleDispatcher#providerMethod()} are specified, invoke that method to get an instance</li>
     * </ol>
     * <p>
     * Any exceptions during resolution are logged and result in an empty Optional being returned.
     *
     * @param annotation the annotation to resolve from
     * @return an Optional containing the resolved dispatcher, or empty if resolution fails
     * @since 1.1
     */
    private Optional<ModuleDispatcherElement> resolveFromAnnotation(ModuleDispatcher annotation) {
        // Check for direct class reference
        if (annotation.value() != ModuleDispatcherElement.class) {
            try {
                LOGGER.debug("Creating dispatcher from class: %s", annotation.value().getName());
                Constructor<? extends ModuleDispatcherElement> constructor = annotation.value().getDeclaredConstructor();
                // Constructor accessibility needed for reflection
                return Optional.of(constructor.newInstance());
            } catch (Exception e) {
                LOGGER.error("Failed to instantiate dispatcher class: %s", e.getMessage());
                LOGGER.debug(EXCEPTION_DETAILS, e);
                return Optional.empty();
            }
        }

        // Check for provider class and method
        if (annotation.provider() != Object.class && !annotation.providerMethod().isEmpty()) {
            try {
                LOGGER.debug("Creating dispatcher from provider: %s.%s",
                        annotation.provider().getName(), annotation.providerMethod());

                Method providerMethod = annotation.provider().getDeclaredMethod(annotation.providerMethod());
                // Method accessibility needed for reflection

                Object result;
                if (Modifier.isStatic(providerMethod.getModifiers())) {
                    // Static method
                    result = providerMethod.invoke(null);
                } else {
                    // Instance method - try to create an instance
                    Constructor<?> constructor = annotation.provider().getDeclaredConstructor();
                    // Constructor accessibility needed for reflection
                    Object providerInstance = constructor.newInstance();
                    result = providerMethod.invoke(providerInstance);
                }

                if (result instanceof ModuleDispatcherElement moduleDispatcherElement) {
                    return Optional.of(moduleDispatcherElement);
                } else {
                    LOGGER.error("Provider method did not return a ModuleDispatcherElement: %s", result);
                    return Optional.empty();
                }
            } catch (Exception e) {
                LOGGER.error("Failed to invoke provider method: %s", e.getMessage());
                LOGGER.debug(EXCEPTION_DETAILS, e);
                return Optional.empty();
            }
        }

        return Optional.empty();
    }

    /**
     * Resolves a dispatcher from a method in the test class.
     * <p>
     * This method looks for a {@code getModuleDispatcher()} method in the test class and invokes it
     * to obtain a {@link ModuleDispatcherElement} instance. If the method doesn't exist or doesn't
     * return a {@link ModuleDispatcherElement}, an empty Optional is returned.
     * <p>
     * Any exceptions during resolution are logged and result in an empty Optional being returned.
     *
     * @param testInstance the test instance to invoke the method on
     * @return an Optional containing the resolved dispatcher, or empty if resolution fails
     * @since 1.1
     */
    private Optional<ModuleDispatcherElement> resolveFromMethod(Object testInstance) {
        try {
            Method method = testInstance.getClass().getDeclaredMethod(GET_MODULE_DISPATCHER_METHOD);
            // Method accessibility needed for reflection

            LOGGER.debug("Found getModuleDispatcher method in test class: %s",
                    testInstance.getClass().getName());

            Object result = method.invoke(testInstance);
            if (result instanceof ModuleDispatcherElement moduleDispatcherElement) {
                return Optional.of(moduleDispatcherElement);
            } else {
                LOGGER.error("getModuleDispatcher method did not return a ModuleDispatcherElement: %s", result);
                return Optional.empty();
            }
        } catch (NoSuchMethodException e) {
            // Method doesn't exist, which is fine
            LOGGER.debug("No getModuleDispatcher method found in test class: %s",
                    testInstance.getClass().getName());
            return Optional.empty();
        } catch (Exception e) {
            LOGGER.error("Failed to invoke getModuleDispatcher method: %s", e.getMessage());
            LOGGER.debug(EXCEPTION_DETAILS, e);
            return Optional.empty();
        }
    }

    /**
     * Validates that there are no conflicts between dispatchers.
     * <p>
     * A conflict occurs when two dispatchers handle the same path and HTTP method.
     *
     * @param dispatchers the list of dispatchers to validate
     * @throws IllegalStateException if conflicts are found
     * @since 1.1
     */
    private void validateDispatchers(List<ModuleDispatcherElement> dispatchers) {
        Map<String, List<ModuleDispatcherElement>> pathMethodMap = new HashMap<>();

        // Group dispatchers by path+method combination
        for (ModuleDispatcherElement dispatcher : dispatchers) {
            String baseUrl = dispatcher.getBaseUrl();
            // For each supported method, add the dispatcher to the map
            for (HttpMethodMapper method : dispatcher.supportedMethods()) {
                String key = method + " " + baseUrl;
                pathMethodMap.computeIfAbsent(key, k -> new ArrayList<>()).add(dispatcher);
            }
        }

        // Check for conflicts
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
     * Logs all active dispatcher elements, sorted by path.
     *
     * @param dispatchers the list of dispatchers to log
     * @since 1.1
     */
    private void logActiveDispatchers(List<ModuleDispatcherElement> dispatchers) {
        if (!LOGGER.isInfoEnabled() || dispatchers.isEmpty()) {
            return;
        }

        LOGGER.info("Active dispatcher elements:");

        // Create a list of log entries sorted by path
        List<String> logEntries = new ArrayList<>();

        for (ModuleDispatcherElement dispatcher : dispatchers) {
            String baseUrl = dispatcher.getBaseUrl();

            for (HttpMethodMapper method : dispatcher.supportedMethods()) {
                String dispatcherName = dispatcher.getClass().getSimpleName();
                logEntries.add(method + " " + baseUrl + " -> " + dispatcherName);
            }
        }

        // Sort by path and log
        logEntries.stream()
                .sorted(Comparator.comparing(entry -> entry.split(" -> ")[0]))
                .forEach(entry -> LOGGER.info("- %s", entry));
    }
}
