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
import de.cuioss.tools.net.ssl.KeyAlgorithm;
import de.cuioss.tools.net.ssl.KeyMaterialHolder;
import de.cuioss.tools.string.Joiner;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NonNull;
import lombok.ToString;
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

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

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
private Config getConfig(EnableMockWebServer enableMockWebServerAnnotation) {
    if (enableMockWebServerAnnotation == null) {
        return Config.getDefaults();
    }
    
    return new Config(
        enableMockWebServerAnnotation.manualStart(),
        enableMockWebServerAnnotation.useHttps(),
        enableMockWebServerAnnotation.keyMaterialProviderIsTestClass()
    );
}

    private void configureHttps(MockWebServer server, Object testInstance, ExtensionContext context, Config config) {
        LOGGER.info("Configuring HTTPS for MockWebServer");

        Optional<HandshakeCertificates> handshakeCertificates = getHandshakeCertificates(testInstance, context, config);

        if (handshakeCertificates.isPresent()) {
            server.useHttps(handshakeCertificates.get().sslSocketFactory());
            LOGGER.info("HTTPS configured for MockWebServer");
            notifyTestClassAboutCertificates(testInstance, context, handshakeCertificates.get());
        } else {
            LOGGER.error("Failed to configure HTTPS: No key material or HandshakeCertificates available");
            throw new IllegalStateException("Failed to configure HTTPS: No key material or HandshakeCertificates available");
        }
    }

    /**
     * Obtains HandshakeCertificates based on the configuration.
     * Will try to get certificates from the test class first if configured,
     * then fall back to self-signed certificates if enabled.
     *
     * @param testInstance the test class instance
     * @param context the extension context
     * @param config the configuration
     * @return an Optional containing HandshakeCertificates if available
     */
    private Optional<HandshakeCertificates> getHandshakeCertificates(Object testInstance, ExtensionContext context, Config config) {
        Optional<HandshakeCertificates> handshakeCertificates = Optional.empty();

        // First try to get certificates from the test class if configured
        if (config.isKeyMaterialProviderIsTestClass()) {
            handshakeCertificates = getTestClassProvidedCertificates(testInstance, context);
            if (handshakeCertificates.isEmpty()) {
                LOGGER.warn("Test class is configured to provide certificates but none were provided");
            }
        }

        // If no certificates from test class or not configured to use test class,
        // create self-signed certificates
        if (handshakeCertificates.isEmpty()) {
            // Try to get cached certificates from context first
            handshakeCertificates = getSelfSignedCertificatesFromContext(context, config);
            
            // If not found in context, create new ones and store them
            if (handshakeCertificates.isEmpty()) {
                try {
                    HandshakeCertificates certificates = KeyMaterialUtil.createSelfSignedHandshakeCertificates(
                            config.getCertificateDuration(), 
                            config.getKeyAlgorithm());
                    LOGGER.info("Generated self-signed certificates with validity of {} day(s)", config.getCertificateDuration());
                    handshakeCertificates = Optional.of(certificates);
                    
                    // Store in context for reuse
                    storeSelfSignedCertificatesInContext(context, certificates, config);
                    
                    LOGGER.info("Generated and cached new self-signed HandshakeCertificates with algorithm {} and duration {} days", 
                            config.getKeyAlgorithm(), config.getCertificateDuration());
                } catch (Exception e) {
                    LOGGER.error("Failed to create self-signed certificates", e);
                }
            } else {
                LOGGER.info("Reusing cached self-signed HandshakeCertificates");
            }
        }

        return handshakeCertificates;
    }

    /**
     * Attempts to get HandshakeCertificates from the test class.
     * First tries to get HandshakeCertificates directly, then falls back to KeyMaterialHolder.
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
        
        // First try to get HandshakeCertificates directly
        Optional<HandshakeCertificates> handshakeCertificates = mockWebServerHolder.provideHandshakeCertificates();
        if (handshakeCertificates.isPresent()) {
            LOGGER.debug("Using HandshakeCertificates provided by test class");
            return handshakeCertificates;
        }
        
        // Fall back to KeyMaterialHolder
        Optional<KeyMaterialHolder> keyMaterial = mockWebServerHolder.provideKeyMaterial();
        if (keyMaterial.isPresent()) {
            try {
                LOGGER.debug("Using key material provided by test class");
                return Optional.of(KeyMaterialUtil.convertToHandshakeCertificates(keyMaterial.get()));
            } catch (Exception e) {
                LOGGER.error("Failed to convert key material to HandshakeCertificates", e);
            }
        }
        
        return Optional.empty();
    }

    private void notifyTestClassAboutCertificates(Object testInstance, ExtensionContext context, HandshakeCertificates handshakeCertificates) {
        Optional<MockWebServerHolder> holder = findMockWebServerHolder(testInstance, context);
        if (holder.isPresent()) {
            holder.get().setSslContext(KeyMaterialUtil.createSslContext(handshakeCertificates));
            LOGGER.debug("Notified test class about HandshakeCertificates");
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
    private Optional<HandshakeCertificates> getSelfSignedCertificatesFromContext(ExtensionContext context, Config config) {
        // Get the root context to ensure certificates are shared across all tests in the class
        ExtensionContext rootContext = getRootContext(context);
        
        Object cachedValue = rootContext.getStore(NAMESPACE).get(SELF_SIGNED_CERTIFICATES_KEY);
        if (cachedValue instanceof CachedCertificates cachedCerts) {
            // Only reuse if the configuration matches
            if (cachedCerts.matches(config)) {
                return Optional.of(cachedCerts.getCertificates());
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
    private void storeSelfSignedCertificatesInContext(ExtensionContext context, HandshakeCertificates certificates, Config config) {
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
     *
     * @param testInstance the test instance to extract the class hierarchy from
     * @return a list of classes in the hierarchy, from most specific to least specific
     */
    private List<Class<?>> extractClassHierarchy(Object testInstance) {
        List<Class<?>> classHierarchy = new ArrayList<>();
        Class<?> testClass = testInstance.getClass();
        
        // Add enclosing classes if present
        if (testClass.getEnclosingClass() != null) {
            classHierarchy.addAll(extractClassHierarchyRecursive(testClass.getEnclosingClass(), new ArrayList<>()));
        }
        
        // Add the class hierarchy of the test class itself
        classHierarchy.addAll(extractClassHierarchyRecursive(testClass, new ArrayList<>()));
        
        LOGGER.debug("Extracted class hierarchy from %s, resulting in:\n\t-%s", 
                testInstance.getClass(), 
                Joiner.on("\n\t-").join(classHierarchy));
        
        return classHierarchy;
    }
    
    /**
     * Recursively extracts the class hierarchy for a class.
     *
     * @param clazz the class to extract the hierarchy from
     * @param classList the list to add classes to
     * @return the list of classes in the hierarchy
     */
    private List<Class<?>> extractClassHierarchyRecursive(Class<?> clazz, List<Class<?>> classList) {
        if (Object.class.equals(clazz)) {
            return classList; // Stop at Object class
        }
        
        classList.add(clazz);
        return extractClassHierarchyRecursive(clazz.getSuperclass(), classList);
    }

    @Override
    public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext) throws ParameterResolutionException {
        Class<?> type = parameterContext.getParameter().getType();
        return MockWebServer.class.equals(type) ||
                Integer.class.equals(type) ||
                int.class.equals(type) ||
                URL.class.equals(type) ||
                String.class.equals(type);
    }

    @Override
    public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext) throws ParameterResolutionException {
        Optional<MockWebServer> server = get(extensionContext);
        if (server.isEmpty()) {
            throw new ParameterResolutionException("No MockWebServer instance available");
        }

        Class<?> type = parameterContext.getParameter().getType();
        MockWebServer mockWebServer = server.get();

        if (MockWebServer.class.equals(type)) {
            return resolveServerParameter(mockWebServer);
        }

        if (Integer.class.equals(type) || int.class.equals(type)) {
            return resolvePortParameter(mockWebServer);
        }

        if (URL.class.equals(type)) {
            return resolveUrlParameter(mockWebServer);
        }

        if (String.class.equals(type)) {
            return resolveStringParameter(mockWebServer);
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
     * Immutable configuration class that holds all settings for the extension.
     */
    @ToString
    private static class Config {
        
        private Config(boolean manualStart, boolean useHttps, boolean keyMaterialProviderIsTestClass) {
            this.manualStart = manualStart;
            this.useHttps = useHttps;
            this.keyMaterialProviderIsTestClass = keyMaterialProviderIsTestClass;
        }
        private final boolean manualStart;
        private final boolean useHttps;
        private final boolean keyMaterialProviderIsTestClass;
        
        // Fixed values for certificate generation
        private static final int CERTIFICATE_DURATION = 1; // 1 day validity for unit tests
        private static final KeyAlgorithm KEY_ALGORITHM = KeyAlgorithm.RSA_2048;
        
        /**
         * @return default configuration with sensible defaults
         */
        static Config getDefaults() {
            return new Config(
                false,              // manualStart
                false,              // useHttps
                false               // keyMaterialProviderIsTestClass
            );
        }
        
        public boolean isManualStart() {
            return manualStart;
        }
        
        public boolean isUseHttps() {
            return useHttps;
        }
        
        public boolean isKeyMaterialProviderIsTestClass() {
            return keyMaterialProviderIsTestClass;
        }
        
        public int getCertificateDuration() {
            return CERTIFICATE_DURATION;
        }
        
        public KeyAlgorithm getKeyAlgorithm() {
            return KEY_ALGORITHM;
        }
    }

    /**
     * Class to store certificates with their configuration for caching.
     */
    private static class CachedCertificates {
        private final HandshakeCertificates certificates;
        
        CachedCertificates(HandshakeCertificates certificates) {
            this.certificates = certificates;
        }
        
        HandshakeCertificates getCertificates() {
            return certificates;
        }
        
        /**
         * Since we now use fixed certificate parameters, all cached certificates match.
         *
         * @param config the configuration to check against
         * @return always true since we use fixed certificate parameters
         */
        boolean matches(Config config) {
            return true;
        }
    }
}