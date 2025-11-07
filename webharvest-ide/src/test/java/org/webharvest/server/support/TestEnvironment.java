package org.webharvest.server.support;

import org.webharvest.server.manager.ExecutionManager;

/**
 * Shared test environment bootstrap utilities.
 * Ensures common system properties are applied before core singletons are initialised.
 */
public final class TestEnvironment {

    private static final TestEnvironment INSTANCE = new TestEnvironment();

    private TestEnvironment() {
        if (System.getProperty(ExecutionManager.INJECTOR_OVERRIDE_MODULE_PROPERTY) == null
                || System.getProperty(ExecutionManager.INJECTOR_OVERRIDE_MODULE_PROPERTY).isBlank()) {
            System.setProperty(ExecutionManager.INJECTOR_OVERRIDE_MODULE_PROPERTY, TestHttpModule.class.getName());
        }
    }

    /**
     * Ensure the shared test environment has been initialised.
     */
    public static void ensure() {
        // Accessing INSTANCE triggers static initialisation.
        INSTANCE.toString();
    }
}


