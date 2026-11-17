/*
 * Copyright (c) 2006-2025 the original author or authors.
 */

package org.webharvest.plugin.zip;

import java.io.IOException;
import java.io.OutputStream;
import java.util.zip.ZipOutputStream;

import org.testng.annotations.Test;
import org.webharvest.plugin.PluginConfiguration;
import org.webharvest.runtime.DynamicScopeContext;
import org.webharvest.runtime.variables.NodeVariable;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

/**
 * Unit contracts for ZIP entry execution context.
 *
 * @author WebHarvest Team
 * @since 2.2.0
 */
public class ZipEntryPluginTest {

    /** Rejects a ZIP entry when no parent archive is active. */
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
    @Test
    public void testActiveZipContextRejectsWrongVariableType() {
        DynamicScopeContext context = mock(DynamicScopeContext.class);
        when(context.getVar(ZipPlugin.ZIP_OUTPUT_STREAM_CONTEXT_KEY))
                .thenReturn(new NodeVariable("not a ZIP stream"));

        try {
            ZipPlugin.getActiveZipOutputStream(context);
            fail("A non-stream context value must be rejected");
        } catch (IllegalStateException expected) {
            assertTrue(expected.getMessage().contains("no ZIP output stream"));
        }
    }

    @Test
    public void testZipEntryReportsArchiveWriteFailure() throws Exception {
        PluginConfiguration configuration = mock(PluginConfiguration.class);
        when(configuration.getProperty("name")).thenReturn("broken.txt");

        ZipEntryPlugin plugin = new ZipEntryPlugin() {
            @Override
            protected org.webharvest.runtime.variables.Variable executeBody(DynamicScopeContext context) {
                return new NodeVariable("payload");
            }
        };
        plugin.initialize(configuration);
        plugin.validateConfiguration(configuration);

        OutputStream failingOutput = new OutputStream() {
            @Override
            public void write(int value) throws IOException {
                throw new IOException("simulated ZIP write failure");
            }
        };
        ZipOutputStream zipOutputStream = new ZipOutputStream(failingOutput);
        DynamicScopeContext context = mock(DynamicScopeContext.class);
        when(context.getVar(ZipPlugin.ZIP_OUTPUT_STREAM_CONTEXT_KEY))
                .thenReturn(new NodeVariable(zipOutputStream));
        when(context.getCharset()).thenReturn("UTF-8");

        try {
            plugin.execute(context);
            fail("A failed ZIP write must be reported");
        } catch (RuntimeException exception) {
            Throwable cause = exception;
            while (cause != null && !(cause instanceof IOException)) {
                cause = cause.getCause();
            }
            assertTrue(cause instanceof IOException, "The original I/O failure should remain in the cause chain");
        } finally {
            try {
                zipOutputStream.close();
            } catch (IOException ignored) {
                // The test stream deliberately fails on every write.
            }
        }
    }

}
