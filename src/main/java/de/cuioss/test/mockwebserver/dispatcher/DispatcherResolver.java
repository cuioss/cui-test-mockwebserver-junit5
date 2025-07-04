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
package de.cuioss.test.mockwebserver.dispatcher;

import de.cuioss.test.mockwebserver.MockWebServerHolder;
import de.cuioss.test.mockwebserver.mockresponse.MockResponseConfig;
import de.cuioss.test.mockwebserver.mockresponse.MockResponseConfigResolver;
import de.cuioss.tools.logging.CuiLogger;
import lombok.NonNull;
import mockwebserver3.Dispatcher;
import org.junit.platform.commons.support.AnnotationSupport;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;


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
     * @param testClass    the class of the test
     * @param testInstance the instance of the test
     * @return a non-null Dispatcher instance to be used with MockWebServer
     * @since 1.1
     */
    @NonNull
    public Dispatcher resolveDispatcher(Class<?> testClass, Object testInstance) {
        return resolveDispatcher(testClass, testInstance, null);
    }

    /**
     * Resolves the dispatcher for a test class with context awareness for the current test method.
     * <p>
     * This method implements the same resolution strategy as {@link #resolveDispatcher(Class, Object)},
     * but with additional context awareness for {@link MockResponseConfig} annotations:
     * <ul>
     *   <li>When a test method is provided, only annotations relevant to that method's context are included</li>
     *   <li>This includes annotations on the test method itself and its containing classes</li>
     *   <li>For nested classes, only annotations in the direct hierarchy are included</li>
     * </ul>
     *
     * @param testClass    the class of the test
     * @param testInstance the instance of the test
     * @param testMethod   the current test method, or null to include all annotations
     * @return a non-null Dispatcher instance to be used with MockWebServer
     * @since 1.1
     */
    @NonNull
    public Dispatcher resolveDispatcher(Class<?> testClass, Object testInstance, Method testMethod) {
        LOGGER.info("Resolving dispatcher for test class: %s", testClass.getName());
        if (testMethod != null) {
            LOGGER.info("Using context-aware resolution for test method: %s", testMethod.getName());
        }

        // Try to resolve from annotation first (highest priority)
        Optional<Dispatcher> annotationDispatcher = resolveFromAnnotationSource(testClass);
        if (annotationDispatcher.isPresent()) {
            return annotationDispatcher.get();
        }

        // Collect all dispatchers from different sources
        List<ModuleDispatcherElement> dispatchers = collectDispatchers(testClass, testInstance, testMethod);

        // Check for legacy dispatcher if no other dispatchers found
        if (dispatchers.isEmpty()) {
            Optional<Dispatcher> legacyDispatcher = resolveLegacyDispatcher(testInstance);
            if (legacyDispatcher.isPresent()) {
                return legacyDispatcher.get();
            }
        }

        // If we have dispatchers, validate and combine them
        if (!dispatchers.isEmpty()) {
            return createCombinedDispatcher(dispatchers);
        }

        // Fallback to default API dispatcher
        LOGGER.debug("No dispatchers found, using default API dispatcher");
        return CombinedDispatcher.createAPIDispatcher();
    }

    /**
     * Attempts to resolve a dispatcher from the ModuleDispatcher annotation.
     *
     * @param testClass the test class
     * @return an Optional containing the resolved dispatcher, or empty if none found
     */
    private Optional<Dispatcher> resolveFromAnnotationSource(Class<?> testClass) {
        Optional<ModuleDispatcher> moduleDispatcherAnnotation =
                AnnotationSupport.findAnnotation(testClass, ModuleDispatcher.class);

        if (moduleDispatcherAnnotation.isEmpty()) {
            LOGGER.info("No @ModuleDispatcher annotation found on test class: %s", testClass.getName());
            return Optional.empty();
        }

        LOGGER.info("Found @ModuleDispatcher annotation on test class: %s", testClass.getName());
        ModuleDispatcher annotation = moduleDispatcherAnnotation.get();

        // First try to resolve as ModuleDispatcherElement
        Optional<ModuleDispatcherElement> dispatcher = resolveFromAnnotation(annotation);

        // If a provider method is specified and no dispatcher was resolved directly,
        // try to resolve using the provider method
        if (dispatcher.isEmpty() && !annotation.providerMethod().isEmpty()) {
            LOGGER.info("Attempting to resolve dispatcher from provider method: %s",
                    annotation.providerMethod());
            Optional<Dispatcher> directDispatcher =
                    resolveDirectDispatcher(testClass, annotation.providerMethod());
            if (directDispatcher.isPresent()) {
                LOGGER.info("Successfully resolved direct dispatcher from provider method");
                return directDispatcher;
            }
            LOGGER.info("Could not resolve direct dispatcher from provider method");
        }

        return Optional.empty();
    }

    /**
     * Collects dispatchers from various sources with context awareness for the current test method.
     *
     * @param testClass    the test class
     * @param testInstance the test instance
     * @param testMethod   the current test method, or null to include all annotations
     * @return a list of collected dispatchers
     */
    private List<ModuleDispatcherElement> collectDispatchers(Class<?> testClass, Object testInstance, Method testMethod) {
        List<ModuleDispatcherElement> dispatchers = new ArrayList<>();

        // Add dispatcher from annotation if present
        Optional<ModuleDispatcher> moduleDispatcherAnnotation =
                AnnotationSupport.findAnnotation(testClass, ModuleDispatcher.class);
        moduleDispatcherAnnotation.flatMap(this::resolveFromAnnotation).ifPresent(dispatcher -> {
            LOGGER.info("Successfully resolved dispatcher from annotation");
            dispatchers.add(dispatcher);
        });

        // Add dispatcher from method if present
        LOGGER.info("Checking for getModuleDispatcher method in test class: %s", testClass.getName());
        resolveFromMethod(testInstance).ifPresent(dispatcher -> {
            LOGGER.info("Successfully resolved dispatcher from getModuleDispatcher method");
            dispatchers.add(dispatcher);
        });

        // Add dispatchers from MockResponseConfig annotations
        List<ModuleDispatcherElement> mockResponseDispatchers =
                MockResponseConfigResolver.resolveFromAnnotations(testClass, testMethod);
        if (!mockResponseDispatchers.isEmpty()) {
            LOGGER.debug("Found %d @MockResponseConfig annotations for context: %s",
                    mockResponseDispatchers.size(), testMethod != null ? testMethod.getName() : "all");
            dispatchers.addAll(mockResponseDispatchers);
        }

        return dispatchers;
    }

    /**
     * Resolves a legacy dispatcher from a MockWebServerHolder instance.
     *
     * @param testInstance the test instance
     * @return an Optional containing the legacy dispatcher, or empty if none found
     */
    private Optional<Dispatcher> resolveLegacyDispatcher(Object testInstance) {
        if (testInstance instanceof MockWebServerHolder holder) {
            LOGGER.debug("Test class implements MockWebServerHolder, checking for dispatcher");
            // Using deprecated method for backward compatibility
            @SuppressWarnings({"removal"}) Dispatcher legacyDispatcher = holder.getDispatcher();
            if (legacyDispatcher != null) {
                LOGGER.debug("Using legacy dispatcher from MockWebServerHolder.getDispatcher()");
                return Optional.of(legacyDispatcher);
            }
        }
        return Optional.empty();
    }

    /**
     * Creates a combined dispatcher from the given list of dispatchers.
     *
     * @param dispatchers the list of dispatchers to combine
     * @return the combined dispatcher
     */
    private Dispatcher createCombinedDispatcher(List<ModuleDispatcherElement> dispatchers) {
        // Validate uniqueness of path+method combinations
        validateDispatchers(dispatchers);

        // Log active dispatcher elements
        logActiveDispatchers(dispatchers);

        LOGGER.debug("Creating CombinedDispatcher with %d module dispatchers", dispatchers.size());
        CombinedDispatcher combinedDispatcher = new CombinedDispatcher();
        combinedDispatcher.addDispatcher(dispatchers);
        return combinedDispatcher;
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
                } else if (result instanceof Dispatcher) {
                    // The provider method returned a Dispatcher directly
                    // This is for backward compatibility with the refactored code
                    LOGGER.debug("Provider method returned a Dispatcher directly, not wrapping it");
                    // We'll handle this in the resolveDispatcher method
                    return Optional.empty();
                } else {
                    LOGGER.error("Provider method did not return a ModuleDispatcherElement or Dispatcher: %s", result);
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
     * to obtain a {@link ModuleDispatcherElement} instance. If the method doesn't exist, an empty Optional is returned.
     * <p>
     * If the method exists but there are issues with invocation or the return type, appropriate exceptions are thrown:
     * <ul>
     *   <li>If the method returns null, an IllegalStateException is thrown</li>
     *   <li>If the method returns a non-ModuleDispatcherElement, an IllegalStateException is thrown</li>
     *   <li>If the method throws an exception, an IllegalStateException wrapping the original exception is thrown</li>
     *   <li>If the method cannot be accessed, an IllegalStateException is thrown</li>
     * </ul>
     *
     * @param testInstance the test instance to invoke the method on
     * @return an Optional containing the resolved dispatcher, or empty if the method doesn't exist
     * @throws DispatcherResolutionException if there are issues with method invocation or return type
     * @since 1.1
     */
    private Optional<ModuleDispatcherElement> resolveFromMethod(Object testInstance) {
        LOGGER.info("Attempting to resolve dispatcher from method for class: %s",
                testInstance.getClass().getName());
        try {
            Method method = testInstance.getClass().getDeclaredMethod(GET_MODULE_DISPATCHER_METHOD);

            LOGGER.info("Found getModuleDispatcher method in test class: %s",
                    testInstance.getClass().getName());

            ModuleDispatcherElement result = invokeModuleDispatcherMethod(testInstance, method);
            LOGGER.info("Successfully resolved dispatcher from method");
            return Optional.of(result);

        } catch (NoSuchMethodException e) {
            // Method doesn't exist, which is fine
            LOGGER.info("No getModuleDispatcher method found in test class: %s",
                    testInstance.getClass().getName());
            return Optional.empty();
        } catch (SecurityException e) {
            LOGGER.error(e, "Security violation accessing getModuleDispatcher method");
            throw new DispatcherResolutionException("Security violation accessing getModuleDispatcher method", e);
        }
    }

    /**
     * Invokes the getModuleDispatcher method on the test instance.
     * This method respects Java's access control rules - if the method is private or otherwise
     * not accessible, an IllegalAccessException will be thrown and converted to a
     * DispatcherResolutionException.
     *
     * @param testInstance the test instance to invoke the method on
     * @param method       the method to invoke
     * @return the ModuleDispatcherElement if successfully resolved
     * @throws DispatcherResolutionException if there are issues with method invocation or return type
     */
    @SuppressWarnings("java:S3011") // owolff: Setting accessibility is ok for test methods
    private ModuleDispatcherElement invokeModuleDispatcherMethod(Object testInstance, Method method) {
        LOGGER.info("Invoking getModuleDispatcher method on instance of class: {}",
                testInstance.getClass().getName());
        try {
            // Check if the method is public but still not accessible (due to Java module system or other reasons)
            // We only make public methods accessible, but leave private methods inaccessible
            // This allows tests to verify that inaccessible private methods are properly handled
            if (Modifier.isPublic(method.getModifiers()) && !method.canAccess(testInstance)) {
                LOGGER.info("Making public getModuleDispatcher method accessible");
                method.setAccessible(true);
            }

            Object result = method.invoke(testInstance);
            if (result == null) {
                LOGGER.error("getModuleDispatcher method returned null");
                throw new DispatcherResolutionException("getModuleDispatcher method returned null");
            }

            LOGGER.info("getModuleDispatcher method returned an object of type: {}",
                    result.getClass().getName());

            // Check if the result implements ModuleDispatcherElement interface
            if (result instanceof ModuleDispatcherElement moduleDispatcherElement) {
                LOGGER.info("Successfully resolved ModuleDispatcherElement with base URL: {}",
                        moduleDispatcherElement.getBaseUrl());

                // Log supported methods for debugging
                LOGGER.info("ModuleDispatcherElement supports methods: {}",
                        moduleDispatcherElement.supportedMethods());

                return moduleDispatcherElement;
            } else {
                LOGGER.error("getModuleDispatcher method returned an object of type {} which is not a ModuleDispatcherElement",
                        result.getClass().getName());
                throw new DispatcherResolutionException(
                        "getModuleDispatcher method did not return a ModuleDispatcherElement: " +
                                result.getClass().getName());
            }
        } catch (IllegalAccessException e) {
            LOGGER.error(e, "Cannot access getModuleDispatcher method");
            throw new DispatcherResolutionException("Cannot access getModuleDispatcher method", e);
        } catch (InvocationTargetException e) {
            LOGGER.error(e.getCause(), "getModuleDispatcher method threw an exception");
            throw new DispatcherResolutionException("getModuleDispatcher method threw an exception", e.getCause());
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
     * Attempts to resolve a direct Dispatcher from a provider method.
     *
     * @param testClass  the test class containing the provider method
     * @param methodName the name of the provider method
     * @return an Optional containing the Dispatcher if found
     * @since 1.1
     */
    private Optional<Dispatcher> resolveDirectDispatcher(Class<?> testClass, String methodName) {
        try {
            Method providerMethod = testClass.getDeclaredMethod(methodName);
            Object result = providerMethod.invoke(null); // Assuming static method

            if (result instanceof Dispatcher directDispatcher) {
                LOGGER.debug("Found direct Dispatcher from provider method: %s", methodName);
                return Optional.of(directDispatcher);
            }
        } catch (Exception e) {
            LOGGER.debug("Failed to resolve direct Dispatcher: %s", e.getMessage());
        }
        return Optional.empty();
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
