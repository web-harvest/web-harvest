/*
 * Copyright (c) 2006-2025 the original author or authors.
 */

package org.webharvest.plugin.zip;

import org.testng.annotations.Test;
import org.webharvest.plugin.PluginConfiguration;
import org.webharvest.runtime.DynamicScopeContext;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

/**
 * Unit contracts for ZIP entry execution context.
 */
public class ZipEntryPluginTest {

    @Test
    public void testZipEntryOutsideActiveZipContextIsRejected() throws Exception {
        PluginConfiguration configuration = mock(PluginConfiguration.class);
        when(configuration.getProperty("name")).thenReturn("orphan.txt");

        ZipEntryPlugin plugin = new ZipEntryPlugin();
        plugin.initialize(configuration);
        plugin.validateConfiguration(configuration);

        DynamicScopeContext context = mock(DynamicScopeContext.class);
        try {
            plugin.execute(context);
            fail("A ZIP entry without an active ZIP output stream must fail");
        } catch (RuntimeException exception) {
            assertTrue(exception.getCause() instanceof IllegalStateException,
                    "The failure should identify the missing active ZIP context");
        }
    }
}
