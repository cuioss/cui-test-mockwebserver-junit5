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
package de.cuioss.test.mockwebserver;

import de.cuioss.test.mockwebserver.ssl.KeyMaterialUtil;
import de.cuioss.tools.logging.CuiLogger;
import de.cuioss.tools.net.ssl.KeyMaterialHolder;
import de.cuioss.tools.string.Joiner;
import mockwebserver3.MockWebServer;
import okhttp3.tls.HandshakeCertificates;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ExtensionContext.Namespace;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;
import org.junit.platform.commons.support.AnnotationSupport;

import javax.net.ssl.SSLContext;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

/**
 * JUnit 5 extension that manages the lifecycle of {@link MockWebServer} instances.
 * <p>
 * This extension provides the following features:
 * <ul>
 *   <li>Automatic server creation and startup before each test</li>
 *   <li>Automatic server shutdown after each test</li>
 *   <li>Support for manual server control</li>
 *   <li>Integration with {@link MockWebServerHolder} for server access</li>
 *   <li>Parameter injection of server instances and related values</li>
 *   <li>HTTPS support with both self-signed and custom certificates</li>
 * </ul>
 * </p>
 * <p>
 * As a {@link ParameterResolver}, this extension can inject the following parameter types:
 * <ul>
 *   <li>{@link MockWebServer} - The server instance</li>
 *   <li>{@code int} or {@code Integer} - The server port</li>
 *   <li>{@code String} - The base URL as a string</li>
 *   <li>{@link URL} - The base URL as a java.net.URL object</li>
 * </ul>
 * </p>
 * <p>
 * See {@link EnableMockWebServer} for configuration options and usage examples.
 * </p>
 *
 * @author Oliver Wolff
 * @see EnableMockWebServer
 * @see MockWebServerHolder
 * @since 1.0
 */
public class MockWebServerExtension implements AfterEachCallback, BeforeEachCallback, ParameterResolver {

    /**
     * Logger for the MockWebServerExtension class.
     * Used to provide consistent logging throughout the extension lifecycle.
     */
    private static final CuiLogger LOGGER = new CuiLogger(MockWebServerExtension.class);

    /**
     * Identifies the {@link Namespace} under which the concrete instance of
     * {@link MockWebServer} is stored.
     */
    private static final Namespace NAMESPACE = Namespace.create(MockWebServerExtension.class);

    /**
     * Key for storing self-signed certificates in the extension context.
     */
    private static final String SELF_SIGNED_CERTIFICATES_KEY = "SELF_SIGNED_CERTIFICATES";

    /**
     * Key for storing the SSLContext in the extension context.
     */
    private static final String SSL_CONTEXT_KEY = "SSL_CONTEXT";

    @Override
    public void beforeEach(ExtensionContext context) throws Exception {
        var server = new MockWebServer();
        var testInstance = context.getRequiredTestInstance();
        Optional<EnableMockWebServer> enableMockWebServerAnnotation = findEnableMockWebServerAnnotation(testInstance);

        var config = getConfig(enableMockWebServerAnnotation.orElse(null));

        if (config.isUseHttps()) {
            configureHttps(server, testInstance, context, config);
        }

        setMockWebServer(testInstance, server, context);

        if (!config.isManualStart()) {
            server.start();
            LOGGER.info("Started MockWebServer at %s", server.url("/"));
        } else {
            LOGGER.info("Manual start requested, server not started");
        }
        put(server, context);
    }

    /**
     * Finds the first EnableMockWebServer annotation in the class hierarchy.
     *
     * @param testInstance the test instance to search for annotations
     * @return the first EnableMockWebServer annotation found, or empty if none
     */
    private Optional<EnableMockWebServer> findEnableMockWebServerAnnotation(Object testInstance) {
        List<Class<?>> classHierarchy = extractClassHierarchy(testInstance);

        return classHierarchy.stream()
                .map(clazz -> AnnotationSupport.findAnnotation(clazz, EnableMockWebServer.class))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .findFirst();
    }

    /**
     * Creates a configuration object from the annotation or default values.
     *
     * @param enableMockWebServerAnnotation the annotation or null for defaults
     * @return a configuration object with all settings
     */
    private MockServerConfig getConfig(EnableMockWebServer enableMockWebServerAnnotation) {
        if (enableMockWebServerAnnotation == null) {
            return MockServerConfig.getDefaults();
        }

        return new MockServerConfig(
                enableMockWebServerAnnotation.manualStart(),
                enableMockWebServerAnnotation.useHttps(),
                enableMockWebServerAnnotation.testClassProvidesKeyMaterial()
        );
    }

    /**
     * Configures HTTPS for the MockWebServer instance.
     * This method handles the setup of SSL/TLS for secure connections.
     *
     * @param server the MockWebServer instance to configure
     * @param testInstance the test class instance
     * @param context the extension context
     * @param config the configuration settings
     * @throws IllegalStateException if certificate material cannot be obtained
     */
    private void configureHttps(MockWebServer server, Object testInstance, ExtensionContext context, MockServerConfig config) {
        LOGGER.info("Configuring HTTPS for MockWebServer");

        // Get certificates based on configuration
        Optional<HandshakeCertificates> handshakeCertificates = getHandshakeCertificates(testInstance, context, config);

        if (handshakeCertificates.isPresent()) {
            // Apply certificates to server
            server.useHttps(handshakeCertificates.get().sslSocketFactory());
            LOGGER.info("HTTPS configured for MockWebServer");

            // Share certificates with test class
            notifyTestClassAboutCertificates(testInstance, context, handshakeCertificates.get());
        } else {
            LOGGER.error("Failed to configure HTTPS: No key material or HandshakeCertificates available");
            throw new IllegalStateException("Failed to configure HTTPS: No key material or HandshakeCertificates available");
        }
    }

    /**
     * Obtains HandshakeCertificates based on the configuration.
     * Uses a prioritized approach:
     * 1. Try to get certificates from the test class if configured
     * 2. Try to get cached certificates from context
     * 3. Create new self-signed certificates
     *
     * @param testInstance the test class instance
     * @param context the extension context
     * @param config the configuration
     * @return an Optional containing HandshakeCertificates if available
     */
    private Optional<HandshakeCertificates> getHandshakeCertificates(Object testInstance, ExtensionContext context, MockServerConfig config) {
        // Strategy 1: Get certificates from test class if configured
        if (config.isTestClassProvidesKeyMaterial()) {
            Optional<HandshakeCertificates> testClassCertificates = getTestClassProvidedCertificates(testInstance, context);
            if (testClassCertificates.isPresent()) {
                LOGGER.info("Using certificates provided by test class");
                return testClassCertificates;
            }
            LOGGER.warn("Test class is configured to provide certificates but none were provided");
        }

        // Strategy 2: Try to get cached certificates from context
        Optional<HandshakeCertificates> cachedCertificates = getSelfSignedCertificatesFromContext(context, config);
        if (cachedCertificates.isPresent()) {
            LOGGER.info("Reusing cached self-signed HandshakeCertificates");
            return cachedCertificates;
        }

        // Strategy 3: Create new self-signed certificates
        return createAndStoreSelfSignedCertificates(context, config);
    }

    /**
     * Creates new self-signed certificates and stores them in the context.
     *
     * @param context the extension context
     * @param config the configuration
     * @return an Optional containing the created HandshakeCertificates, or empty if creation failed
     */
    private Optional<HandshakeCertificates> createAndStoreSelfSignedCertificates(ExtensionContext context, MockServerConfig config) {
        try {
            HandshakeCertificates certificates = KeyMaterialUtil.createSelfSignedHandshakeCertificates(
                    config.getCertificateDuration(),
                    config.getKeyAlgorithm());

            // Store in context for reuse
            storeSelfSignedCertificatesInContext(context, certificates, config);

            LOGGER.info("Generated and cached new self-signed HandshakeCertificates with algorithm {} and duration {} days",
                    config.getKeyAlgorithm(), config.getCertificateDuration());

            return Optional.of(certificates);
        } catch (Exception e) {
            LOGGER.error("Failed to create self-signed certificates", e);
            return Optional.empty();
        }
    }

    /**
     * Gets HandshakeCertificates from the test class.
     *
     * @param testInstance the test class instance
     * @param context the extension context
     * @return an Optional containing HandshakeCertificates if the test class provided them
     */
    private Optional<HandshakeCertificates> getTestClassProvidedCertificates(Object testInstance, ExtensionContext context) {
        Optional<MockWebServerHolder> holder = findMockWebServerHolder(testInstance, context);
        if (holder.isEmpty()) {
            return Optional.empty();
        }

        MockWebServerHolder mockWebServerHolder = holder.get();
        Optional<HandshakeCertificates> handshakeCertificates = mockWebServerHolder.provideHandshakeCertificates();
        
        if (handshakeCertificates.isPresent()) {
            LOGGER.debug("Using HandshakeCertificates provided by test class");
            return handshakeCertificates;
        }

        return Optional.empty();
    }

    private void notifyTestClassAboutCertificates(Object testInstance, ExtensionContext context, HandshakeCertificates handshakeCertificates) {
        Optional<MockWebServerHolder> holder = findMockWebServerHolder(testInstance, context);
        if (holder.isPresent()) {
            SSLContext sslContext = KeyMaterialUtil.createSslContext(handshakeCertificates);
            holder.get().setSslContext(sslContext);

            // Store the SSLContext in the context store for parameter resolution
            ExtensionContext rootContext = getRootContext(context);
            rootContext.getStore(NAMESPACE).put(SSL_CONTEXT_KEY, sslContext);

            LOGGER.debug("Notified test class about HandshakeCertificates and stored SSLContext for parameter resolution");
        }
    }

    private void setMockWebServer(Object testInstance, MockWebServer mockWebServer, ExtensionContext context) {
        Optional<MockWebServerHolder> holder = findMockWebServerHolder(testInstance, context);
        if (holder.isPresent()) {
            holder.get().setMockWebServer(mockWebServer);
            Optional.ofNullable(holder.get().getDispatcher()).ifPresent(mockWebServer::setDispatcher);
            LOGGER.info("Fulfilled interface contract of MockWebServerHolder on %s", holder.get().getClass().getName());
        } else {
            LOGGER.warn("No instance of %s found. Is this intentional?", MockWebServerHolder.class.getName());
        }
    }

    private Optional<MockWebServerHolder> findMockWebServerHolder(Object testInstance, ExtensionContext context) {
        if (testInstance instanceof MockWebServerHolder holder) {
            LOGGER.debug("Found MockWebServerHolder in test instance %s", holder.getClass().getName());
            return Optional.of(holder);
        }

        Optional<ExtensionContext> parentContext = context.getParent();
        while (parentContext.isPresent()) {
            var parentTestInstanceOptional = parentContext.get().getTestInstance();
            if (parentTestInstanceOptional.isPresent()) {
                Object parentTestInstance = parentTestInstanceOptional.get();
                if (parentTestInstance instanceof MockWebServerHolder holder) {
                    LOGGER.debug("Found MockWebServerHolder in parent test instance %s", holder.getClass().getName());
                    return Optional.of(holder);
                }
            } else {
                LOGGER.debug("Parent test instance is not present although context is present %s", parentContext.get().getDisplayName());
            }
            parentContext = parentContext.get().getParent();
        }
        LOGGER.debug("Found no MockWebServerHolder in test instance %s", testInstance.getClass().getName());

        return Optional.empty();
    }

    @Override
    public void afterEach(ExtensionContext context) throws Exception {
        var optionalMockWebServer = get(context);
        if (optionalMockWebServer.isPresent()) {
            var server = optionalMockWebServer.get();
            if (optionalMockWebServer.get().getStarted()) {
                LOGGER.info("Shutting down MockWebServer at %s", server.url("/"));
                server.shutdown();
            } else {
                LOGGER.warn("Server was not started, therefore can not be shutdown");
            }
        } else {
            LOGGER.error("Server not present, therefore can not be shutdown");
        }
    }

    private static void put(MockWebServer mockWebServer, ExtensionContext context) {
        context.getStore(NAMESPACE).put(MockWebServer.class.getName(), mockWebServer);
    }

    private Optional<MockWebServer> get(ExtensionContext context) {
        return Optional.ofNullable((MockWebServer) context.getStore(NAMESPACE).get(MockWebServer.class.getName()));
    }

    /**
     * Retrieves self-signed certificates from the extension context if they exist and match the current configuration.
     *
     * @param context the extension context
     * @param config the current configuration
     * @return an Optional containing HandshakeCertificates if found in context and matching the config
     */
    private Optional<HandshakeCertificates> getSelfSignedCertificatesFromContext(ExtensionContext context, MockServerConfig config) {
        // Get the root context to ensure certificates are shared across all tests in the class
        ExtensionContext rootContext = getRootContext(context);

        Object cachedValue = rootContext.getStore(NAMESPACE).get(SELF_SIGNED_CERTIFICATES_KEY);
        if (cachedValue instanceof CachedCertificates cachedCerts) {
            // Only reuse if the configuration matches
            if (cachedCerts.matches(config)) {
                return Optional.of(cachedCerts.certificates());
            } else {
                LOGGER.debug("Cached certificates found but configuration doesn't match, creating new ones");
                return Optional.empty();
            }
        }

        return Optional.empty();
    }

    /**
     * Stores self-signed certificates in the extension context for reuse.
     *
     * @param context the extension context
     * @param certificates the certificates to store
     * @param config the current configuration
     */
    private void storeSelfSignedCertificatesInContext(ExtensionContext context, HandshakeCertificates certificates, MockServerConfig config) {
        // Store in the root context to ensure certificates are shared across all tests in the class
        ExtensionContext rootContext = getRootContext(context);

        CachedCertificates cachedCerts = new CachedCertificates(
                certificates);

        rootContext.getStore(NAMESPACE).put(SELF_SIGNED_CERTIFICATES_KEY, cachedCerts);
    }

    /**
     * Gets the root context to ensure certificates are shared across all tests in the class.
     *
     * @param context the current extension context
     * @return the root context
     */
    private ExtensionContext getRootContext(ExtensionContext context) {
        ExtensionContext current = context;
        while (current.getParent().isPresent()) {
            current = current.getParent().get();
        }
        return current;
    }

    /**
     * Extracts the class hierarchy for a test instance, including enclosing classes.
     * This implementation uses a non-recursive approach for better performance and readability.
     *
     * @param testInstance the test instance to extract the class hierarchy from
     * @return a list of classes in the hierarchy, from most specific to least specific
     */
    private List<Class<?>> extractClassHierarchy(Object testInstance) {
        List<Class<?>> classHierarchy = new ArrayList<>();
        Class<?> testClass = testInstance.getClass();

        // Add enclosing classes if present
        Class<?> enclosingClass = testClass.getEnclosingClass();
        if (enclosingClass != null) {
            addClassHierarchy(enclosingClass, classHierarchy);
        }

        // Add the class hierarchy of the test class itself
        addClassHierarchy(testClass, classHierarchy);

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Extracted class hierarchy from {}, resulting in:\n\t-{}",
                    testInstance.getClass().getName(),
                    Joiner.on("\n\t-").join(classHierarchy));
        }

        return classHierarchy;
    }

    /**
     * Adds a class and its superclasses to the hierarchy list.
     * Uses an iterative approach instead of recursion for better performance.
     *
     * @param clazz the starting class
     * @param hierarchy the list to add classes to
     */
    private void addClassHierarchy(Class<?> clazz, List<Class<?>> hierarchy) {
        Class<?> current = clazz;
        while (current != null && !Object.class.equals(current)) {
            hierarchy.add(current);
            current = current.getSuperclass();
        }
    }

    /**
     * Map of parameter types to resolver functions.
     * This approach simplifies the parameter resolution logic and makes it more maintainable.
     */
    private final Map<Class<?>, Function<MockWebServer, Object>> parameterResolvers = Map.of(
            MockWebServer.class, this::resolveServerParameter,
            Integer.class, this::resolvePortParameter,
            int.class, this::resolvePortParameter,
            URL.class, this::resolveUrlParameter,
            String.class, this::resolveStringParameter,
            URIBuilder.class, this::resolveUrlBuilderParameter
    );

    @Override
    public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext) throws ParameterResolutionException {
        Class<?> type = parameterContext.getParameter().getType();
        return parameterResolvers.containsKey(type) || SSLContext.class.equals(type);
    }

    @Override
    public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext) throws ParameterResolutionException {
        Class<?> type = parameterContext.getParameter().getType();

        // Handle SSLContext separately as it doesn't depend on MockWebServer
        if (SSLContext.class.equals(type)) {
            return resolveSslContextParameter(extensionContext);
        }

        // For all other parameter types, get the MockWebServer instance
        Optional<MockWebServer> server = get(extensionContext);
        if (server.isEmpty()) {
            throw new ParameterResolutionException("No MockWebServer instance available");
        }
        MockWebServer mockWebServer = server.get();

        // Use the parameter resolver map to resolve the parameter
        Function<MockWebServer, Object> resolver = parameterResolvers.get(type);
        if (resolver != null) {
            return resolver.apply(mockWebServer);
        }

        throw new ParameterResolutionException("Unsupported parameter type: " + type.getName());
    }

    private MockWebServer resolveServerParameter(MockWebServer server) {
        return server;
    }

    private int resolvePortParameter(MockWebServer server) {
        return server.getPort();
    }

    private URL resolveUrlParameter(MockWebServer server) {
        try {
            return server.url("/").url();
        } catch (Exception e) {
            throw new ParameterResolutionException("Failed to convert HttpUrl to URL", e);
        }
    }

    private String resolveStringParameter(MockWebServer server) {
        return server.url("/").toString();
    }

    /**
     * Resolves a URIBuilder parameter for the given MockWebServer instance.
     * <p>
     * The URIBuilder provides a convenient way to build and manipulate URIs for API requests.
     * This method creates a URIBuilder initialized with the base URL of the MockWebServer,
     * which can then be used to construct complete request URLs by adding paths, query parameters,
     * fragments, etc.
     * </p>
     * <p>
     * Example usage in tests:
     * <pre>
     * {@code
     * @Test
     * void testWithUriBuilder(URIBuilder uriBuilder) {
     *     // Add path and query parameters
     *     uriBuilder.setPath("/api/users")
     *              .addParameter("active", "true");
     *     
     *     // Use the URI in your HTTP client
     *     HttpGet request = new HttpGet(uriBuilder.build());
     *     // ...
     * }
     * }
     * </pre>
     * </p>
     *
     * @param server the MockWebServer instance
     * @return a URIBuilder initialized with the server's base URL
     * @throws ParameterResolutionException if the URL cannot be converted to a URI
     */
    private URIBuilder resolveUrlBuilderParameter(MockWebServer server) {
        try {
            return URIBuilder.from(server.url("/").url());
        } catch (Exception e) {
            throw new ParameterResolutionException(
                    "Failed to create URIBuilder from MockWebServer URL: " + e.getMessage(), e);
        }
    }

    /**
     * Resolves an SSLContext parameter for test methods.
     * This allows tests to directly inject the SSLContext without having to implement the MockWebServerHolder interface.
     * 
     * @param context the extension context
     * @return the SSLContext used for HTTPS configuration
     * @throws ParameterResolutionException if no SSLContext is available
     */
    private SSLContext resolveSslContextParameter(ExtensionContext context) {
        // Get the root context to access the store
        ExtensionContext rootContext = getRootContext(context);

        // Try to get the SSLContext from the store
        Optional<SSLContext> sslContext = Optional.ofNullable(rootContext.getStore(NAMESPACE)
                .get(SSL_CONTEXT_KEY, SSLContext.class));

        if (sslContext.isEmpty()) {
            throw new ParameterResolutionException("No SSLContext available. Make sure HTTPS is enabled with @EnableMockWebServer(useHttps = true)");
        }

        return sslContext.get();
    }

    // Configuration class has been extracted to MockServerConfig

    /**
         * Class to store certificates with their configuration for caching.
         */
        private record CachedCertificates(HandshakeCertificates certificates) {

        /**
             * Since we now use fixed certificate parameters, all cached certificates match.
             *
             * @param config the configuration to check against
             * @return always true since we use fixed certificate parameters
             */
            boolean matches(MockServerConfig config) {
            return true;
        }
    }
}