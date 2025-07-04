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

import org.easymock.EasyMock;
import org.junit.jupiter.api.extension.ExtensionContext;

import java.util.Optional;
import javax.net.ssl.SSLContext;


import okhttp3.tls.HandshakeCertificates;

/**
 * Utility class providing common test methods and constants for CertificateResolver tests.
 */
final class CertificateResolverTestUtil {

    static final String SELF_SIGNED_CERTIFICATES_KEY = "self-signed-certificates";
    static final String SSL_CONTEXT_KEY = "ssl-context";
    static final String CERTIFICATES_SHOULD_NOT_BE_NULL = "Certificates should not be null";
    static final String KEY_MANAGER_ASSERTION_MESSAGE = "Key manager should not be null";
    static final String TRUST_MANAGER_ASSERTION_MESSAGE = "Trust manager should not be null";

    private CertificateResolverTestUtil() {
        // Utility class
    }

    static ExtensionContext createMockContext(Class<?> testClass) {
        ExtensionContext mockContext = EasyMock.createMock(ExtensionContext.class);
        EasyMock.expect(mockContext.getParent()).andReturn(Optional.empty()).anyTimes();
        EasyMock.expect(mockContext.getTestClass()).andReturn(Optional.of(testClass)).anyTimes();
        EasyMock.expect(mockContext.getTestMethod()).andReturn(Optional.empty()).anyTimes();
        EasyMock.expect(mockContext.getTestInstance()).andReturn(Optional.empty()).anyTimes();
        EasyMock.replay(mockContext);
        return mockContext;
    }

    static ExtensionContext createMockContextWithStore() {
        ExtensionContext mockContext = EasyMock.createMock(ExtensionContext.class);
        ExtensionContext.Store mockStore = EasyMock.createMock(ExtensionContext.Store.class);

        EasyMock.expect(mockContext.getParent()).andReturn(Optional.empty()).anyTimes();
        EasyMock.expect(mockContext.getTestClass()).andReturn(Optional.empty()).anyTimes();
        EasyMock.expect(mockContext.getTestMethod()).andReturn(Optional.empty()).anyTimes();
        EasyMock.expect(mockContext.getTestInstance()).andReturn(Optional.empty()).anyTimes();
        EasyMock.expect(mockContext.getStore(CertificateResolver.NAMESPACE)).andReturn(mockStore).anyTimes();

        final HandshakeCertificates[] storedCertificates = new HandshakeCertificates[1];
        final SSLContext[] storedContext = new SSLContext[1];

        EasyMock.expect(mockStore.get(SELF_SIGNED_CERTIFICATES_KEY, HandshakeCertificates.class))
                .andAnswer(() -> storedCertificates[0])
                .anyTimes();

        EasyMock.expect(mockStore.get(SSL_CONTEXT_KEY, SSLContext.class))
                .andAnswer(() -> storedContext[0])
                .anyTimes();

        mockStore.put(EasyMock.eq(SELF_SIGNED_CERTIFICATES_KEY), EasyMock.anyObject(HandshakeCertificates.class));
        EasyMock.expectLastCall().andAnswer(() -> {
            storedCertificates[0] = (HandshakeCertificates) EasyMock.getCurrentArguments()[1];
            return null;
        }).anyTimes();

        mockStore.put(EasyMock.eq(SSL_CONTEXT_KEY), EasyMock.anyObject(SSLContext.class));
        EasyMock.expectLastCall().andAnswer(() -> {
            storedContext[0] = (SSLContext) EasyMock.getCurrentArguments()[1];
            return null;
        }).anyTimes();

        mockStore.put(EasyMock.anyObject(), EasyMock.anyObject());
        EasyMock.expectLastCall().anyTimes();

        EasyMock.replay(mockContext, mockStore);
        return mockContext;
    }
}
