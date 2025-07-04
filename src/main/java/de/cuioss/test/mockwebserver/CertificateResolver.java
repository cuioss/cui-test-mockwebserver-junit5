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
package de.cuioss.test.mockwebserver;

import de.cuioss.test.mockwebserver.ssl.KeyMaterialUtil;
import de.cuioss.tools.logging.CuiLogger;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.platform.commons.util.ReflectionUtils;

import java.lang.reflect.Method;
import java.util.Optional;
import javax.net.ssl.SSLContext;


import okhttp3.tls.HandshakeCertificates;

/**
 * Package-private companion class for {@link MockWebServerExtension} that handles
 * certificate resolution and management for HTTPS testing.
 * <p>
 * This class is responsible for:
 * <ul>
 *   <li>Finding and invoking the appropriate method that provides HandshakeCertificates based on the {@link TestProvidedCertificate} annotation</li>
 *   <li>Creating self-signed certificates when none are provided</li>
 *   <li>Caching certificates in the extension context</li>
 *   <li>Converting between HandshakeCertificates and SSLContext</li>
 * </ul>
 */
class CertificateResolver {

    private static final CuiLogger LOGGER = new CuiLogger(CertificateResolver.class);
    private static final String DEFAULT_PROVIDER_METHOD_NAME = "provideHandshakeCertificates";
    private static final String SELF_SIGNED_CERTIFICATES_KEY = "self-signed-certificates";
    private static final String SSL_CONTEXT_KEY = "ssl-context";

    /**
     * Identifies the {@link ExtensionContext.Namespace} under which the Resolver stores its data.
     */
    static final ExtensionContext.Namespace NAMESPACE = ExtensionContext.Namespace.create(CertificateResolver.class);

    /**
     * Determines the HandshakeCertificates from the test class or provider based on
     * the {@link TestProvidedCertificate} annotation.
     *
     * @param context the extension context
     * @return Optional containing HandshakeCertificates if configured, empty otherwise
     */
    Optional<HandshakeCertificates> determineTestProvidedHandshakeCertificates(ExtensionContext context) {
        Optional<Class<?>> testClass = context.getTestClass();
        if (testClass.isEmpty()) {
            return Optional.empty();
        }

        // Check for TestProvidedCertificate annotation on the class
        Optional<TestProvidedCertificate> classAnnotation =
                Optional.ofNullable(testClass.get().getAnnotation(TestProvidedCertificate.class));

        // Check for TestProvidedCertificate annotation on the test method
        Optional<TestProvidedCertificate> methodAnnotation = context.getTestMethod()
                .map(method -> method.getAnnotation(TestProvidedCertificate.class));

        // Method annotation takes precedence over class annotation
        Optional<TestProvidedCertificate> annotation = methodAnnotation.isPresent() ?
                methodAnnotation : classAnnotation;

        if (annotation.isEmpty()) {
            return Optional.empty();
        }

        // Get the provider class from the annotation or use the test class
        Class<?> providerClass = annotation.get().providerClass() != Void.class ?
                annotation.get().providerClass() : testClass.get();

        // Get the method name from the annotation or use the default
        String methodName = annotation.get().methodName();

        return getCertificatesFromProvider(providerClass, methodName, context);
    }

    /**
     * Gets HandshakeCertificates from the specified provider class.
     *
     * @param providerClass the class that provides certificates
     * @param methodName    the name of the method that provides certificates
     * @param context       the extension context
     * @return Optional containing HandshakeCertificates if available
     */
    @SuppressWarnings("java:S3655")
    // owolff: False positive: context.getTestClass().isPresent() is called before
    Optional<HandshakeCertificates> getCertificatesFromProvider(Class<?> providerClass, String methodName, ExtensionContext context) {
        try {
            // Look for the certificate method in the provider class
            Optional<Method> method = ReflectionUtils.findMethod(providerClass, methodName);
            if (method.isEmpty() && !DEFAULT_PROVIDER_METHOD_NAME.equals(methodName)) {
                // Try the default provider method name as a fallback
                method = ReflectionUtils.findMethod(providerClass, DEFAULT_PROVIDER_METHOD_NAME);
            }
            if (method.isEmpty()) {
                return Optional.empty();
            }

            // Create an instance of the provider class if it's the test class
            Object providerInstance = null;

            if (context.getTestClass().isPresent() && context.getTestClass().get().equals(providerClass)) {
                providerInstance = context.getTestInstance().orElse(null);
            }

            // If no instance is available (static method or external provider), create a new instance
            if (providerInstance == null && !ReflectionUtils.isStatic(method.get())) {
                providerInstance = createProviderInstance(providerClass);
            }

            // Invoke the method to get the certificates
            Object result = ReflectionUtils.invokeMethod(method.get(), providerInstance);
            if (result instanceof HandshakeCertificates certificates) {
                return Optional.of(certificates);
            }

            return Optional.empty();
        } catch (Exception e) {
            throw new IllegalStateException(
                    "Error resolving HandshakeCertificates from provider " + providerClass.getName(), e);
        }
    }

    /**
     * Creates a new instance of the provider class.
     *
     * @param providerClass the class to instantiate
     * @return a new instance of the provider class
     * @throws IllegalStateException if the instance cannot be created
     */
    Object createProviderInstance(Class<?> providerClass) {
        try {
            return ReflectionUtils.newInstance(providerClass);
        } catch (Exception e) {
            throw new IllegalStateException(
                    "Could not create instance of provider class " + providerClass.getName(), e);
        }
    }

    /**
     * Gets HandshakeCertificates for HTTPS configuration, using a multi-strategy approach:
     * <ol>
     *   <li>Try to get certificates from test class using the @TestProvidedCertificate annotation</li>
     *   <li>Try to get cached certificates from context</li>
     *   <li>Create new self-signed certificates</li>
     * </ol>
     *
     * @param context the extension context
     * @param config  the configuration
     * @return an Optional containing HandshakeCertificates if available
     */
    public Optional<HandshakeCertificates> getHandshakeCertificates(ExtensionContext context, MockServerConfig config) {
        // Strategy 1: Get certificates from test class using the annotation
        Optional<HandshakeCertificates> testProvidedCertificates = determineTestProvidedHandshakeCertificates(context);
        if (testProvidedCertificates.isPresent()) {
            LOGGER.info("Using certificates provided by @TestProvidedCertificate");
            return testProvidedCertificates;
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
     * @param config  the configuration
     * @return an Optional containing the created HandshakeCertificates
     */
    Optional<HandshakeCertificates> createAndStoreSelfSignedCertificates(ExtensionContext context, MockServerConfig config) {
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
     * Retrieves cached certificates from the context.
     *
     * @param context the extension context
     * @return an Optional containing HandshakeCertificates
     */
    Optional<HandshakeCertificates> getSelfSignedCertificatesFromContext(ExtensionContext context) {
        // Get the root context to ensure certificates are shared across all tests in the class
        ExtensionContext rootContext = getRootContext(context);

        HandshakeCertificates certificates = rootContext.getStore(NAMESPACE)
                .get(SELF_SIGNED_CERTIFICATES_KEY, HandshakeCertificates.class);
        if (certificates != null) {
            // Since we use fixed certificate parameters, we can always reuse the cached certificates
            return Optional.of(certificates);
        }

        return Optional.empty();
    }

    /**
     * Stores certificates in the extension context.
     *
     * @param context      the extension context
     * @param certificates the certificates to store
     */
    void storeSelfSignedCertificatesInContext(ExtensionContext context, HandshakeCertificates certificates) {
        // Store in the root context to ensure certificates are shared across all tests in the class
        ExtensionContext rootContext = getRootContext(context);

        // Store the certificates directly without the wrapper
        rootContext.getStore(NAMESPACE).put(SELF_SIGNED_CERTIFICATES_KEY, certificates);
    }

    /**
     * Stores the SSLContext for parameter resolution.
     *
     * @param context               the extension context
     * @param handshakeCertificates the HandshakeCertificates
     * @return the created SSLContext
     */
    public SSLContext createAndStoreSSLContext(ExtensionContext context, HandshakeCertificates handshakeCertificates) {
        try {
            SSLContext sslContext = KeyMaterialUtil.createSslContext(handshakeCertificates);

            // Store the SSLContext in the context store for parameter resolution
            ExtensionContext rootContext = getRootContext(context);
            rootContext.getStore(NAMESPACE).put(SSL_CONTEXT_KEY, sslContext);

            LOGGER.debug("Stored SSLContext for parameter resolution");

            return sslContext;
        } catch (Exception e) {
            String errorMessage = "Failed to create or store SSLContext";
            LOGGER.error(errorMessage, e);
            throw new IllegalStateException(errorMessage, e);
        }
    }

    /**
     * Resolves an SSLContext parameter for test methods.
     *
     * @param context the extension context
     * @return the SSLContext for HTTPS configuration
     */
    public Optional<SSLContext> getSSLContext(ExtensionContext context) {
        // Get the root context to access the store
        ExtensionContext rootContext = getRootContext(context);

        // Try to get the SSLContext from the store
        return Optional.ofNullable(rootContext.getStore(NAMESPACE)
                .get(SSL_CONTEXT_KEY, SSLContext.class));
    }

    /**
     * Gets the root context to ensure certificates are shared across all tests in the class.
     *
     * @param context the current extension context
     * @return the root context
     */
    @SuppressWarnings("java:S3655")
    // This is a false positive, the check for present is in the while loop
    ExtensionContext getRootContext(ExtensionContext context) {
        ExtensionContext current = context;
        while (current.getParent().isPresent()) {
            current = current.getParent().get();
        }
        return current;
    }
}
