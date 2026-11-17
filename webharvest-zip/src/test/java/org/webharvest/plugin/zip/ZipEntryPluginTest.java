/*
 * Copyright (c) 2006-2025 the original author or authors.
 */

package org.webharvest.plugin.zip;

import org.testng.annotations.Test;
import org.webharvest.plugin.PluginConfiguration;
import org.webharvest.runtime.DynamicScopeContext;

import static org.mockito.Mockito.mock;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

/**
 * Unit contracts for ZIP entry execution context.
 */
public class ZipEntryPluginTest {

    @Test
    public void testZipEntryOutsideActiveZipContextIsRejected() throws Exception {
        ZipEntryPlugin plugin = new ZipEntryPlugin();
        plugin.initialize(mock(PluginConfiguration.class));

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
