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
import de.cuioss.tools.string.Joiner;
import okhttp3.tls.HandshakeCertificates;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ExtensionContext.Namespace;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;
import org.junit.platform.commons.support.AnnotationSupport;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import javax.net.ssl.SSLContext;


import mockwebserver3.MockWebServer;

/**
 * JUnit 5 extension that manages the lifecycle of {@link MockWebServer} instances.
 * <p>
 * This extension provides the following features:
 * <ul>
 *   <li>Automatic server creation and startup before each test</li>
 *   <li>Automatic server shutdown after each test</li>
 *   <li>Support for manual server control</li>
 *   <li>Parameter injection of server instances and related values (recommended approach)</li>
 *   <li>Integration with {@link MockWebServerHolder} for server access (legacy approach)</li>
 *   <li>HTTPS support with both self-signed and custom certificates</li>
 * </ul>
 * 
 * <h2>Recommended Usage: Parameter Injection</h2>
 * <p>
 * The recommended approach is to use parameter injection rather than implementing the
 * {@link MockWebServerHolder} interface:
 * 
 * <pre>
 * {@code
 * @EnableMockWebServer
 * class ParameterResolverTest {
 *     @Test
 *     void testWithServerInjection(MockWebServer server, URIBuilder uriBuilder) {
 *         // Configure responses using the Builder pattern
 *         server.setDispatcher(new Dispatcher() {
 *             @Override
 *             public MockResponse dispatch(RecordedRequest request) {
 *                 return new MockResponse.Builder()
 *                     .addHeader("Content-Type", "application/json")
 *                     .body("{\"status\": \"success\"}")
 *                     .code(HttpServletResponse.SC_OK)
 *                     .build();
 *             }
 *         });
 *         
 *         // Use the URIBuilder for request construction
 *         URI requestUri = uriBuilder.addPathSegment("api").addPathSegment("data").build();
 *     }
 * }
 * }
 * </pre>
 *
 * <p>
 * As a {@link ParameterResolver}, this extension can inject the following parameter types:
 * <ul>
 *   <li>{@link MockWebServer} - The server instance</li>
 *   <li>{@link URIBuilder} - A builder for constructing request URLs</li>
 *   <li>{@link SSLContext} - When HTTPS is enabled</li>
 * </ul>
 *
 * <p>
 * Example usage with Java's HttpClient:
 * <pre>
 * {@code
 * @EnableMockWebServer(useHttps = true)
 * class HttpsTest {
 *     @Test
 *     void testHttpsRequest(MockWebServer server, URIBuilder uriBuilder, SSLContext sslContext) throws Exception {
 *         // Set up a dispatcher for this test
 *         server.setDispatcher(new Dispatcher() {
 *             @Override
 *             public MockResponse dispatch(RecordedRequest request) {
 *                 if ("/api/data".equals(request.getPath())) {
 *                     return new MockResponse().setBody("Hello World");
 *                 }
 *                 return new MockResponse().setResponseCode(404);
 *             }
 *         });
 *         
 *         // Create HttpClient with the injected SSLContext
 *         HttpClient client = HttpClient.newBuilder()
 *             .sslContext(sslContext)
 *             .build();
 *             
 *         // Create request using the URIBuilder parameter
 *         HttpRequest request = HttpRequest.newBuilder()
 *             .uri(uriBuilder.setPath("/api/data").build())
 *             .GET()
 *             .build();
 *             
 *         // Send request and verify response
 *         HttpResponse<String> response = client.send(request, 
 *             HttpResponse.BodyHandlers.ofString());
 *         assertEquals(200, response.statusCode());
 *         assertEquals("Hello World", response.body());
 *     }
 * }
 * }
 * </pre>
 *
 *
 * @author Oliver Wolff
 * @see EnableMockWebServer
 * @see MockWebServerHolder
 * @since 1.0
 */
public class MockWebServerExtension implements AfterEachCallback, BeforeEachCallback, ParameterResolver {

    /**
     * Logger for the MockWebServerExtension class.
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
    public void beforeEach(ExtensionContext context) {
        LOGGER.debug("Setting up MockWebServer for test: %s", context.getDisplayName());

        MockWebServer server = null;
        try {
            server = new MockWebServer();
            var testInstance = context.getRequiredTestInstance();
            Optional<EnableMockWebServer> enableMockWebServerAnnotation = findEnableMockWebServerAnnotation(testInstance);

            var config = getConfig(enableMockWebServerAnnotation.orElse(null));
            LOGGER.debug("Using configuration: useHttps=%s, manualStart=%s, testClassProvidesKeyMaterial=%s",
                    config.isUseHttps(), config.isManualStart(), config.isTestClassProvidesKeyMaterial());

            if (config.isUseHttps()) {
                configureHttps(server, testInstance, context, config);
            }

            setMockWebServer(testInstance, server, context);

            if (!config.isManualStart()) {
                startServer(server);
            } else {
                ensureServerNotStarted(server);
            }

            // Store the server in context - it will be properly closed in afterEach
            put(server, context);
            // We've successfully stored the server in context, so don't close it in the finally block
            server = null;
            LOGGER.debug("MockWebServer setup completed successfully");
        } catch (Exception e) {
            if (!(e instanceof IllegalStateException)) {
                LOGGER.error("Unexpected error during MockWebServer setup", e);
            }
            throw e;
        } finally {
            // Close the server if something went wrong and we didn't store it in context
            if (server != null) {
                try {
                    server.shutdown();
                    LOGGER.info("Shutdown server due to exception during setup");
                } catch (IOException closeEx) {
                    LOGGER.warn("Failed to shutdown server during exception handling: {}", closeEx.getMessage());
                }
            }
        }
    }

    /**
     * Starts the MockWebServer instance.
     *
     * @param server the server to start
     * @throws IllegalStateException if the server cannot be started
     */
    private void startServer(MockWebServer server) {
        try {
            server.start();
            LOGGER.info("Started MockWebServer at %s", server.url("/"));
        } catch (Exception e) {
            String errorMessage = "Failed to start MockWebServer";
            LOGGER.error(errorMessage, e);
            throw new IllegalStateException(errorMessage, e);
        }
    }

    /**
     * Ensures the server is not started when manual start is requested.
     *
     * @param server the server to check
     */
    private void ensureServerNotStarted(MockWebServer server) {
        // When manual start is requested, ensure the server is not started
        if (server.getStarted()) {
            try {
                server.shutdown();
                LOGGER.info("Shutdown server to enforce manual start configuration");
            } catch (Exception e) {
                LOGGER.warn("Failed to shutdown server for manual start: {}", e.getMessage());
            }
        }
        LOGGER.info("Manual start requested, server not started");
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
            try {
                // Apply certificates to server
                server.useHttps(handshakeCertificates.get().sslSocketFactory());
                LOGGER.info("HTTPS configured for MockWebServer");

                // Share certificates with test class
                notifyTestClassAboutCertificates(testInstance, context, handshakeCertificates.get());
            } catch (Exception e) {
                String errorMessage = "Failed to configure HTTPS with available certificates";
                LOGGER.error(errorMessage, e);
                throw new IllegalStateException(errorMessage, e);
            }
        } else {
            String errorMessage = "Failed to configure HTTPS: No key material or HandshakeCertificates available";
            LOGGER.error(errorMessage);
            throw new IllegalStateException(errorMessage);
        }
    }

    /**
     * Obtains HandshakeCertificates for HTTPS configuration.
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
        Optional<HandshakeCertificates> cachedCertificates = getSelfSignedCertificatesFromContext(context);
        if (cachedCertificates.isPresent()) {
            LOGGER.info("Reusing cached self-signed HandshakeCertificates");
            return cachedCertificates;
        }

        // Strategy 3: Create new self-signed certificates
        return createAndStoreSelfSignedCertificates(context, config);
    }

    /**
     * Creates self-signed certificates and stores them in the context.
     *
     * @param context the extension context
     * @param config the configuration
     * @return an Optional containing the created HandshakeCertificates
     */
    private Optional<HandshakeCertificates> createAndStoreSelfSignedCertificates(ExtensionContext context, MockServerConfig config) {
        try {
            HandshakeCertificates certificates = KeyMaterialUtil.createSelfSignedHandshakeCertificates(
                    config.getCertificateDuration(),
                    config.getKeyAlgorithm());

            // Store in context for reuse
            storeSelfSignedCertificatesInContext(context, certificates);

            LOGGER.info("Generated and cached new self-signed HandshakeCertificates with algorithm %s and duration %s days",
                    config.getKeyAlgorithm(), config.getCertificateDuration());

            return Optional.of(certificates);
        } catch (Exception e) {
            LOGGER.error("Failed to create self-signed certificates", e);
            return Optional.empty();
        }
    }

    /**
     * Retrieves HandshakeCertificates from the test class.
     *
     * @param testInstance the test class instance
     * @param context the extension context
     * @return an Optional containing HandshakeCertificates
     */
    private Optional<HandshakeCertificates> getTestClassProvidedCertificates(Object testInstance, ExtensionContext context) {
        Optional<MockWebServerHolder> holder = findMockWebServerHolder(testInstance, context);
        if (holder.isEmpty()) {
            return Optional.empty();
        }

        MockWebServerHolder mockWebServerHolder = holder.get();
        Optional<HandshakeCertificates> handshakeCertificates = mockWebServerHolder.getTestProvidedHandshakeCertificates();

        if (handshakeCertificates.isPresent()) {
            LOGGER.debug("Using HandshakeCertificates provided by test class");
            return handshakeCertificates;
        }

        return Optional.empty();
    }

    /**
     * Stores the SSLContext for parameter resolution.
     * 
     * @param testInstance the test instance
     * @param context the extension context
     * @param handshakeCertificates the HandshakeCertificates
     */
    private void notifyTestClassAboutCertificates(Object testInstance, ExtensionContext context, HandshakeCertificates handshakeCertificates) {
        try {
            SSLContext sslContext = KeyMaterialUtil.createSslContext(handshakeCertificates);

            // Store the SSLContext in the context store for parameter resolution
            ExtensionContext rootContext = getRootContext(context);
            rootContext.getStore(NAMESPACE).put(SSL_CONTEXT_KEY, sslContext);

            LOGGER.debug("Stored SSLContext for parameter resolution");

            // Notify test class if it implements MockWebServerHolder
            Optional<MockWebServerHolder> holder = findMockWebServerHolder(testInstance, context);
            if (holder.isPresent()) {
                LOGGER.debug("Test class implements MockWebServerHolder, but receiveHandshakeCertificates method is not available");
                // The MockWebServerHolder interface doesn't have a receiveHandshakeCertificates method
                // If this functionality is needed, it should be added to the interface first
            }
        } catch (Exception e) {
            String errorMessage = "Failed to create or store SSLContext";
            LOGGER.error(errorMessage, e);
            throw new IllegalStateException(errorMessage, e);
        }
    }

    /**
     * Sets the MockWebServer instance on the test class if it implements MockWebServerHolder.
     * 
     * @param testInstance the test instance
     * @param mockWebServer the MockWebServer instance
     * @param context the extension context
     * @deprecated This method uses deprecated methods in MockWebServerHolder interface.
     * Will be removed in version 1.2 when the deprecated methods are removed.
     */
    @Deprecated(since = "1.1", forRemoval = true)
    private void setMockWebServer(Object testInstance, MockWebServer mockWebServer, ExtensionContext context) {
        Optional<MockWebServerHolder> holder = findMockWebServerHolder(testInstance, context);
        if (holder.isPresent()) {
            holder.get().setMockWebServer(mockWebServer);
            Optional.ofNullable(holder.get().getDispatcher()).ifPresent(mockWebServer::setDispatcher);
            LOGGER.info("Fulfilled interface contract of MockWebServerHolder on {}", holder.get().getClass().getName());
        } else {
            LOGGER.warn("No instance of {} found. Is this intentional?", MockWebServerHolder.class.getName());
        }
    }

    /**
     * Finds a MockWebServerHolder implementation in the test class hierarchy.
     *
     * @param testInstance the test class instance
     * @param context the extension context
     * @return an Optional containing the MockWebServerHolder
     */
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
    public void afterEach(ExtensionContext context) {
        var optionalMockWebServer = get(context);
        if (optionalMockWebServer.isPresent()) {
            var server = optionalMockWebServer.get();
            try {
                if (server.getStarted()) {
                    LOGGER.info("Shutting down MockWebServer at port %s", server.getPort());
                    server.shutdown();
                    LOGGER.debug("MockWebServer successfully shut down");
                } else {
                    LOGGER.debug("Server was not started, no shutdown needed");
                }
            } catch (IOException e) {
                LOGGER.error("Failed to shutdown MockWebServer", e);
                throw new IllegalStateException("Failed to properly shutdown MockWebServer", e);
            } finally {
                // Remove the server from the context to prevent memory leaks
                remove(context);
            }
        }
    }

    /**
     * Removes the MockWebServer instance from the extension context.
     *
     * @param context the extension context
     */
    private void remove(ExtensionContext context) {
        try {
            context.getStore(NAMESPACE).remove(context.getRequiredTestMethod());
            LOGGER.debug("Removed MockWebServer from context");
        } catch (Exception e) {
            LOGGER.warn("Failed to remove MockWebServer from context: {}", e.getMessage());
        }
    }

    /**
     * Stores the MockWebServer instance in the extension context.
     *
     * @param mockWebServer the MockWebServer instance to store
     * @param context the extension context
     */
    private static void put(MockWebServer mockWebServer, ExtensionContext context) {
        context.getStore(NAMESPACE).put(MockWebServer.class.getName(), mockWebServer);
    }

    /**
     * Retrieves the MockWebServer instance from the extension context.
     *
     * @param context the extension context
     * @return an Optional containing the MockWebServer instance if present
     */
    private Optional<MockWebServer> get(ExtensionContext context) {
        return Optional.ofNullable((MockWebServer) context.getStore(NAMESPACE).get(MockWebServer.class.getName()));
    }

    /**
     * Retrieves cached certificates from the context.
     *
     * @param context the extension context
     * @return an Optional containing HandshakeCertificates
     */
    private Optional<HandshakeCertificates> getSelfSignedCertificatesFromContext(ExtensionContext context) {
        // Get the root context to ensure certificates are shared across all tests in the class
        ExtensionContext rootContext = getRootContext(context);

        HandshakeCertificates certificates = rootContext.getStore(NAMESPACE).get(SELF_SIGNED_CERTIFICATES_KEY, HandshakeCertificates.class);
        if (certificates != null) {
            // Since we use fixed certificate parameters, we can always reuse the cached certificates
            return Optional.of(certificates);
        }

        return Optional.empty();
    }

    /**
     * Stores certificates in the extension context.
     *
     * @param context the extension context
     * @param certificates the certificates to store
     */
    private void storeSelfSignedCertificatesInContext(ExtensionContext context, HandshakeCertificates certificates) {
        // Store in the root context to ensure certificates are shared across all tests in the class
        ExtensionContext rootContext = getRootContext(context);

        // Store the certificates directly without the wrapper
        rootContext.getStore(NAMESPACE).put(SELF_SIGNED_CERTIFICATES_KEY, certificates);
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
     * Extracts the class hierarchy for the given test instance.
     *
     * @param testInstance the test instance
     * @return a list of classes in the hierarchy
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
            LOGGER.debug("Extracted class hierarchy from %s, resulting in:\n\t-%s",
                    testInstance.getClass().getName(),
                    Joiner.on("\n\t-").join(classHierarchy));
        }

        return classHierarchy;
    }

    /**
     * Adds a class and its superclasses to the hierarchy list.
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
     * Maps parameter types to resolver functions.
     */
    private final Map<Class<?>, Function<MockWebServer, Object>> parameterResolvers = Map.of(
            MockWebServer.class, this::resolveServerParameter,
            URIBuilder.class, this::resolveUrlBuilderParameter
    );

    @Override
    public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext) throws ParameterResolutionException {
        Class<?> type = parameterContext.getParameter().getType();
        boolean supported = parameterResolvers.containsKey(type) || SSLContext.class.equals(type);
        if (supported) {
            LOGGER.debug("Parameter type %s is supported", type.getName());
        }
        return supported;
    }

    @Override
    public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext) throws ParameterResolutionException {
        Class<?> type = parameterContext.getParameter().getType();
        LOGGER.debug("Resolving parameter of type %s", type.getName());

        // Handle SSLContext separately as it doesn't depend on MockWebServer
        if (SSLContext.class.equals(type)) {
            return resolveSslContextParameter(extensionContext);
        }

        // For all other parameter types, get the MockWebServer instance
        Optional<MockWebServer> server = get(extensionContext);
        if (server.isEmpty()) {
            String errorMessage = "No MockWebServer instance available";
            LOGGER.error(errorMessage);
            throw new ParameterResolutionException(errorMessage);
        }
        MockWebServer mockWebServer = server.get();

        // Use the parameter resolver map to resolve the parameter
        Function<MockWebServer, Object> resolver = parameterResolvers.get(type);
        if (resolver != null) {
            return resolver.apply(mockWebServer);
        }

        String errorMessage = "Unsupported parameter type: " + type.getName();
        LOGGER.error(errorMessage);
        throw new ParameterResolutionException(errorMessage);
    }

    private MockWebServer resolveServerParameter(MockWebServer server) {
        return server;
    }


    /**
     * Creates a URIBuilder initialized with the server's base URL.
     *
     * @param server the MockWebServer instance
     * @return a URIBuilder for the server
     * @throws ParameterResolutionException if URL conversion fails
     */
    private URIBuilder resolveUrlBuilderParameter(MockWebServer server) {
        try {
            // Check if the server is already started
            if (server.getStarted()) {
                return URIBuilder.from(server.url("/").url());
            } else {
                // For manual start configuration, create a placeholder URIBuilder
                // that will be updated when the server is actually started
                LOGGER.debug("Creating placeholder URIBuilder for non-started server");
                return URIBuilder.placeholder();
            }
        } catch (Exception e) {
            LOGGER.error("Failed to create URIBuilder from MockWebServer URL", e);
            throw new ParameterResolutionException(
                    "Failed to create URIBuilder from MockWebServer URL: " + e.getMessage(), e);
        }
    }

    /**
     * Resolves an SSLContext parameter for test methods.
     * 
     * @param context the extension context
     * @return the SSLContext for HTTPS configuration
     * @throws ParameterResolutionException if no SSLContext is available
     */
    private SSLContext resolveSslContextParameter(ExtensionContext context) {
        // Get the root context to access the store
        ExtensionContext rootContext = getRootContext(context);

        // Try to get the SSLContext from the store
        Optional<SSLContext> sslContext = Optional.ofNullable(rootContext.getStore(NAMESPACE)
                .get(SSL_CONTEXT_KEY, SSLContext.class));

        if (sslContext.isEmpty()) {
            String errorMessage = "No SSLContext available. Make sure HTTPS is enabled with @EnableMockWebServer(useHttps = true)";
            LOGGER.error(errorMessage);
            throw new ParameterResolutionException(errorMessage);
        }

        return sslContext.get();
    }


}