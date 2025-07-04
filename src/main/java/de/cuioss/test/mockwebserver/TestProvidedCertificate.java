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

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to indicate that a test class or method provides custom certificate material
 * for HTTPS testing with MockWebServer.
 * <p>
 * When this annotation is present, the extension will look for a method that provides
 * {@code HandshakeCertificates} for HTTPS testing.
 * <p>
 * This method can be provided in one of two ways:
 * <ol>
 *   <li>Directly in the test class using a method specified by {@link #methodName()} (default behavior)</li>
 *   <li>In a separate provider class specified by the {@link #providerClass()} attribute</li>
 * </ol>
 * <p>
 * This annotation replaces the previous approach of implementing the
 * {@code MockWebServerHolder#getTestProvidedHandshakeCertificates()} method and setting
 * {@code EnableMockWebServer#testClassProvidesKeyMaterial = true}.
 * <p>
 * Example usage on a test class:
 * <pre>
 * {@code
 * @EnableMockWebServer(useHttps = true)
 * @TestProvidedCertificate(methodName = "createTestCertificates")
 * class HttpsTest {
 *     
 *     public static HandshakeCertificates createTestCertificates() {
 *         // Create and return custom certificates
 *         return new HandshakeCertificates.Builder()
 *             .addTrustedCertificate(...)
 *             .build();
 *     }
 *     
 *     @Test
 *     void testHttpsRequest(MockWebServer server, SSLContext sslContext) {
 *         // Test with custom certificates
 *     }
 * }
 * }
 * </pre>
 * <p>
 * Example usage with a provider class:
 * <pre>
 * {@code
 * @EnableMockWebServer(useHttps = true)
 * @TestProvidedCertificate(providerClass = MyCertificateProvider.class)
 * class HttpsTest {
 *     
 *     @Test
 *     void testHttpsRequest(MockWebServer server, SSLContext sslContext) {
 *         // Test with custom certificates from MyCertificateProvider
 *     }
 * }
 * 
 * class MyCertificateProvider {
 *     public static HandshakeCertificates provideHandshakeCertificates() {
 *         // Create and return custom certificates
 *         return new HandshakeCertificates.Builder()
 *             .addTrustedCertificate(...)
 *             .build();
 *     }
 * }
 * }
 * </pre>
 *
 * @author Oliver Wolff
 * @since 1.1
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface TestProvidedCertificate {

    /**
     * Optional name of the method that provides the HandshakeCertificates.
     * If not specified, the default method name "getTestProvidedHandshakeCertificates" will be used.
     * The method can be an instance method or a static method, but must return
     * {@code okhttp3.tls.HandshakeCertificates}.
     * 
     * @return the name of the method that provides the certificates
     */
    String methodName() default "getTestProvidedHandshakeCertificates";

    /**
     * Optional class that provides the HandshakeCertificates.
     * The class must have a method that returns {@code okhttp3.tls.HandshakeCertificates}.
     * By default, the method name is "provideHandshakeCertificates", but this can be
     * overridden using the {@link #methodName()} attribute.
     * This can be an instance method or a static method.
     * 
     * @return the class that provides the certificates
     */
    Class<?> providerClass() default Void.class;
}
