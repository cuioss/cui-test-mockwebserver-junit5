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

import de.cuioss.tools.logging.CuiLogger;
import de.cuioss.tools.string.Joiner;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NonNull;
import lombok.ToString;
import mockwebserver3.MockWebServer;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ExtensionContext.Namespace;
import org.junit.platform.commons.support.AnnotationSupport;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Handle the lifetime of an instance of {@link MockWebServer}, see
 * {@link EnableMockWebServer} for details on using
 *
 * @author Oliver Wolff
 */
public class MockWebServerExtension implements AfterEachCallback, BeforeEachCallback {

    private static final CuiLogger LOGGER = new CuiLogger(MockWebServerExtension.class);

    @Override
    @SuppressWarnings({"squid:S2095"}) // owolff: Will be closed after all tests
    public void beforeEach(ExtensionContext context) throws Exception {
        var server = new MockWebServer();

        var testInstance = context.getRequiredTestInstance();

        var classModel = extractTestClasses(testInstance);
        Optional<EnableMockWebServer> enableMockWebServerAnnotation = classModel.stream().filter(holder -> holder.getAnnotation().isPresent()).findFirst().map(holder -> holder.getAnnotation().get());

        boolean manualStart = false;
        if (enableMockWebServerAnnotation.isPresent()) {
            manualStart = enableMockWebServerAnnotation.get().manualStart();
        }
        setMockWebServer(testInstance, server, context);

        if (!manualStart) {
            server.start();
            LOGGER.info("Started MockWebServer at %s", server.url("/"));
        } else {
            LOGGER.info("Manual start requested, server not started");
        }
        put(server, context);
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
                /*
                 * According Copilot:
                 * The issue you're encountering is related to how JUnit handles nested test classes and their contexts.
                 * Specifically, the parentContext.get().getTestInstance() returning an empty Optional despite parentContext.isPresent()
                 * being true can be confusing.
                 * This behavior is not necessarily a bug but rather a consequence of how JUnit manages test instances and their lifecycle.
                 * In JUnit 5, nested test classes are treated as separate test instances, and their contexts are managed independently.
                 * This can lead to situations where the parent context is present, but the test instance is not yet available or initialized.*/
                LOGGER.debug("Parent test instance is not present although context is present %s", parentContext.get().getDisplayName());
            }
            parentContext = parentContext.get().getParent();
        }
        LOGGER.debug("Found no MockWebServerHolder in test instance %s", testInstance.getClass().getName());

        return Optional.empty();
    }

    /**
     * Identifies the {@link Namespace} under which the concrete instance of
     * {@link MockWebServer} is stored.
     */
    public static final Namespace NAMESPACE = Namespace.create("test", "portal", "MockWebServer");

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


    private static List<TestClassHolder> extractTestClasses(Object testInstance) {
        List<TestClassHolder> parentClassmodel = new ArrayList<>();
        if (null != testInstance.getClass().getEnclosingClass()) {
            parentClassmodel = extractTestClassesRecursive(testInstance.getClass().getEnclosingClass(), new ArrayList<>());
        }
        var model = extractTestClassesRecursive(testInstance.getClass(), new ArrayList<>());

        parentClassmodel.addAll(model);
        LOGGER.debug("Extracted model form %s, resulting in:\n\t-%s", testInstance.getClass(), Joiner.on("\n\t-").join(parentClassmodel));
        return parentClassmodel;
    }

    private static List<TestClassHolder> extractTestClassesRecursive(Class<?> testClass, List<TestClassHolder> holderList) {
        LOGGER.debug("Extract TestClassHolder %s", testClass);
        if (Object.class.equals(testClass)) {
            LOGGER.debug("Reached java.lang.Object, returning list");
            return holderList;
        }
        LOGGER.debug("Extracting TestClassHolder for %s", testClass);
        holderList.add(TestClassHolder.from(testClass));
        return extractTestClassesRecursive(testClass.getSuperclass(), holderList);
    }


    /**
     * Represents a tuple of the concrete class and the optional annotation
     */
    @AllArgsConstructor
    @ToString
    private static class TestClassHolder {
        @NonNull
        @Getter
        Class<?> testInstance;
        EnableMockWebServer annotation;

        Optional<EnableMockWebServer> getAnnotation() {
            return Optional.ofNullable(annotation);
        }

        static TestClassHolder from(Class<?> testClass) {
            EnableMockWebServer annotation = AnnotationSupport.findAnnotation(testClass,
                    EnableMockWebServer.class).orElse(null);
            return new TestClassHolder(testClass, annotation);
        }
    }

}