package org.webharvest.runtime;

import static org.mockito.Mockito.*;
import static org.testng.Assert.*;

import java.util.Collections;
import java.util.List;
import org.testng.annotations.Test;
import org.webharvest.definition.PluginConfigurationTree;
import org.webharvest.plugin.DefaultPluginConfiguration;
import org.webharvest.plugin.PluginBodyExecutor;
import org.webharvest.plugin.PluginConfiguration;
import org.webharvest.runtime.scripting.ScriptingLanguage;
import org.webharvest.runtime.variables.EmptyVariable;
import org.webharvest.runtime.variables.NodeVariable;
import org.webharvest.runtime.variables.Variable;

/**
 * Contracts for root {@code <config>} runtime settings application and null-safe execution.
 */
public class RootConfigurationExecutorBranchContractTest {

    @Test
    public void nullRootTreeIsIgnored() throws Exception {
        RootConfigurationExecutor executor = new RootConfigurationExecutor(mock(PluginBodyExecutor.class));
        ScraperContext context = new ScraperContext();
        executor.execute(null, context);
        assertNull(context.getVar("configResult"));
    }

    @Test
    public void nullConfigurationIsIgnored() throws Exception {
        PluginConfigurationTree tree = mock(PluginConfigurationTree.class);
        when(tree.getConfiguration()).thenReturn(null);
        when(tree.getChildCount()).thenReturn(0);
        when(tree.getChildren()).thenReturn(Collections.emptyList());

        RootConfigurationExecutor executor = new RootConfigurationExecutor(mock(PluginBodyExecutor.class));
        ScraperContext context = new ScraperContext();
        executor.execute(tree, context);
        assertNull(context.getVar("configResult"));
    }

    @Test
    public void appliesCharsetAndScriptLanguageWithFallbacks() throws Exception {
        DefaultPluginConfiguration configuration = new DefaultPluginConfiguration(
                "config", "http://org.webharvest/schema/2.1/core", "config");
        configuration.setProperty("charset", "NO-SUCH-CHARSET");
        configuration.setProperty("scriptlang", "ruby");

        PluginConfigurationTree tree = mock(PluginConfigurationTree.class);
        when(tree.getConfiguration()).thenReturn(configuration);
        when(tree.getChildCount()).thenReturn(0);
        when(tree.hasChildren()).thenReturn(false);
        when(tree.getChildren()).thenReturn(Collections.emptyList());

        PluginBodyExecutor bodyExecutor = mock(PluginBodyExecutor.class);
        when(bodyExecutor.executeBody(any(PluginConfiguration.class), any(DynamicScopeContext.class)))
                .thenReturn(new NodeVariable("ok"));

        ScraperContext context = new ScraperContext();
        new RootConfigurationExecutor(bodyExecutor).execute(tree, context);

        assertEquals(context.getCharset(), "UTF-8");
        assertEquals(context.getScriptingLanguage(), ScriptingLanguage.BEANSHELL);
        Variable result = context.getVar("configResult");
        assertEquals(result.toString(), "ok");
    }

    @Test
    public void blankCharsetDefaultsToUtf8AndNullBodyResultBecomesEmpty() throws Exception {
        DefaultPluginConfiguration configuration = new DefaultPluginConfiguration(
                "config", "http://org.webharvest/schema/2.1/core", "config");
        configuration.setProperty("charset", "   ");
        configuration.setProperty("scriptlang", "javascript");

        PluginConfigurationTree tree = mock(PluginConfigurationTree.class);
        when(tree.getConfiguration()).thenReturn(configuration);
        when(tree.getChildCount()).thenReturn(0);
        when(tree.hasChildren()).thenReturn(false);
        when(tree.getChildren()).thenReturn(Collections.emptyList());

        PluginBodyExecutor bodyExecutor = mock(PluginBodyExecutor.class);
        when(bodyExecutor.executeBody(any(PluginConfiguration.class), any(DynamicScopeContext.class)))
                .thenReturn(null);

        ScraperContext context = new ScraperContext();
        new RootConfigurationExecutor(bodyExecutor).execute(tree, context);

        assertEquals(context.getCharset(), "UTF-8");
        assertEquals(context.getScriptingLanguage(), ScriptingLanguage.JAVASCRIPT);
        assertTrue(context.getVar("configResult") instanceof EmptyVariable);
    }

    @Test
    public void syncsChildConfigsFromTreeWhenConfigurationChildrenEmpty() throws Exception {
        DefaultPluginConfiguration configuration = new DefaultPluginConfiguration(
                "config", "http://org.webharvest/schema/2.1/core", "config");
        configuration.setProperty("charset", "UTF-8");
        configuration.setProperty("scriptlang", "beanshell");
        configuration.setChildren(List.of());

        DefaultPluginConfiguration child = new DefaultPluginConfiguration(
                "text", "http://org.webharvest/schema/2.1/core", "text");
        PluginConfigurationTree childTree = mock(PluginConfigurationTree.class);
        when(childTree.getConfiguration()).thenReturn(child);

        PluginConfigurationTree tree = mock(PluginConfigurationTree.class);
        when(tree.getConfiguration()).thenReturn(configuration);
        when(tree.getChildCount()).thenReturn(1);
        when(tree.hasChildren()).thenReturn(true);
        when(tree.getChildren()).thenReturn(List.of(childTree));

        PluginBodyExecutor bodyExecutor = mock(PluginBodyExecutor.class);
        when(bodyExecutor.executeBody(any(PluginConfiguration.class), any(DynamicScopeContext.class)))
                .thenReturn(EmptyVariable.INSTANCE);

        ScraperContext context = new ScraperContext();
        new RootConfigurationExecutor(bodyExecutor).execute(tree, context);

        assertEquals(configuration.getChildren().size(), 1);
        assertSame(configuration.getChildren().get(0), child);
    }
}
