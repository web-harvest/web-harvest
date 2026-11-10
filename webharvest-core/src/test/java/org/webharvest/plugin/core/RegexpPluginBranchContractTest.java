package org.webharvest.plugin.core;

import static org.mockito.Mockito.*;
import static org.testng.Assert.*;

import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.webharvest.plugin.PluginConfiguration;
import org.webharvest.runtime.DynamicScopeContext;
import org.webharvest.runtime.variables.ListVariable;
import org.webharvest.runtime.variables.NodeVariable;
import org.webharvest.runtime.variables.Variable;

/**
 * Regexp execution contracts using hyphenated flag attributes, replace mode,
 * match limits, and capture group publication.
 */
public class RegexpPluginBranchContractTest {

    @Mock
    private PluginConfiguration configuration;

    @Mock
    private DynamicScopeContext context;

    @BeforeMethod
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        when(configuration.hasChildren()).thenReturn(false);
        when(configuration.getChildren()).thenReturn(java.util.Collections.emptyList());
    }

    private void stubCommonProperties() {
        when(configuration.getProperty("flag-caseinsensitive", "false")).thenReturn("false");
        when(configuration.getProperty("flag-multiline", "false")).thenReturn("false");
        when(configuration.getProperty("flag-dotall", "false")).thenReturn("false");
        when(configuration.getProperty("flag-unicodecase", "false")).thenReturn("false");
        when(configuration.getProperty("flag-canoneq", "false")).thenReturn("false");
    }

    @Test
    public void replaceTrueRewritesSourceUsingResultTemplate() throws Exception {
        when(configuration.getProperty("pattern", "")).thenReturn("\\d+");
        when(configuration.getProperty("source", "")).thenReturn("a12b34");
        when(configuration.getProperty("result", "")).thenReturn("#");
        when(configuration.getProperty("replace", "false")).thenReturn("true");
        when(configuration.getProperty("max", "")).thenReturn("");
        stubCommonProperties();

        RegexpPlugin plugin = new RegexpPlugin();
        plugin.initialize(configuration);
        Variable result = plugin.execute(context);

        assertTrue(result instanceof ListVariable);
        assertEquals(result.toString(), "a#b#");
    }

    @Test
    public void maxAttributeStopsAfterConfiguredMatches() throws Exception {
        when(configuration.getProperty("pattern", "")).thenReturn("\\w+");
        when(configuration.getProperty("source", "")).thenReturn("one two three");
        when(configuration.getProperty("result", "")).thenReturn("");
        when(configuration.getProperty("replace", "false")).thenReturn("false");
        when(configuration.getProperty("max", "")).thenReturn("2");
        stubCommonProperties();

        RegexpPlugin plugin = new RegexpPlugin();
        plugin.initialize(configuration);
        Variable result = plugin.execute(context);

        assertTrue(result instanceof ListVariable);
        assertEquals(((ListVariable) result).toList().size(), 2);
    }

    @Test
    public void captureGroupsArePublishedToContext() throws Exception {
        when(configuration.getProperty("pattern", "")).thenReturn("(\\w+)=(\\d+)");
        when(configuration.getProperty("source", "")).thenReturn("id=7");
        when(configuration.getProperty("result", "")).thenReturn("");
        when(configuration.getProperty("replace", "false")).thenReturn("false");
        when(configuration.getProperty("max", "")).thenReturn("");
        stubCommonProperties();

        RegexpPlugin plugin = new RegexpPlugin();
        plugin.initialize(configuration);
        plugin.execute(context);

        ArgumentCaptor<Variable> captor = ArgumentCaptor.forClass(Variable.class);
        verify(context, atLeastOnce()).setLocalVar(eq("group1"), captor.capture());
        assertEquals(captor.getValue().toString(), "id");
        verify(context, atLeastOnce()).setLocalVar(eq("group2"), captor.capture());
        assertEquals(captor.getValue().toString(), "7");
    }

    @Test
    public void caseInsensitiveFlagMatchesMixedCaseSource() throws Exception {
        when(configuration.getProperty("pattern", "")).thenReturn("abc");
        when(configuration.getProperty("source", "")).thenReturn("xx AbC yy");
        when(configuration.getProperty("result", "")).thenReturn("");
        when(configuration.getProperty("replace", "false")).thenReturn("false");
        when(configuration.getProperty("max", "")).thenReturn("");
        when(configuration.getProperty("flag-caseinsensitive", "false")).thenReturn("true");
        when(configuration.getProperty("flag-multiline", "false")).thenReturn("false");
        when(configuration.getProperty("flag-dotall", "false")).thenReturn("false");
        when(configuration.getProperty("flag-unicodecase", "false")).thenReturn("false");
        when(configuration.getProperty("flag-canoneq", "false")).thenReturn("false");

        RegexpPlugin plugin = new RegexpPlugin();
        plugin.initialize(configuration);
        Variable result = plugin.execute(context);

        assertTrue(result instanceof ListVariable);
        assertFalse(((ListVariable) result).toList().isEmpty());
    }

    @Test
    public void emptyPatternReturnsEmptyListWithoutExecutingMatcher() throws Exception {
        when(configuration.getProperty("pattern", "")).thenReturn("");
        when(configuration.getProperty("source", "")).thenReturn("text");
        when(configuration.getProperty("result", "")).thenReturn("");
        when(configuration.getProperty("replace", "false")).thenReturn("false");
        when(configuration.getProperty("max", "")).thenReturn("");
        stubCommonProperties();

        RegexpPlugin plugin = new RegexpPlugin();
        plugin.initialize(configuration);
        Variable result = plugin.execute(context);

        assertTrue(result instanceof ListVariable);
        assertTrue(((ListVariable) result).toList().isEmpty());
        verify(context, never()).setLocalVar(startsWith("group"), any());
    }
}
