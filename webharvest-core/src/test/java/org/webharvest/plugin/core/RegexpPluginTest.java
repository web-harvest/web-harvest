package org.webharvest.plugin.core;

import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.webharvest.plugin.PluginConfiguration;
import org.webharvest.plugin.exceptions.PluginException;
import org.webharvest.plugin.exceptions.PluginValidationException;
import org.webharvest.runtime.DynamicScopeContext;
import org.webharvest.runtime.variables.EmptyVariable;
import org.webharvest.runtime.variables.NodeVariable;
import org.webharvest.runtime.variables.Variable;
import org.webharvest.definition.IElementDef;

import static org.mockito.Mockito.*;
import static org.testng.Assert.*;

public class RegexpPluginTest {

    @Mock
    private PluginConfiguration configuration;

    @Mock
    private DynamicScopeContext context;

    @Mock
    private IElementDef elementDef;

    private RegexpPlugin plugin;

    @BeforeMethod
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        plugin = new RegexpPlugin();
        
        // Mock elementDef
        when(elementDef.hasOperations()).thenReturn(false);
        when(elementDef.getOperationDefs()).thenReturn(new IElementDef[0]);
    }

    @Test
    public void testGetMetadata() {
        assertNotNull(plugin.getMetadata());
        assertEquals(plugin.getMetadata().getId(), "regexp");
    }

    @Test
    public void testGetConfigurationSchema() {
        assertNotNull(plugin.getConfigurationSchema());
        assertTrue(plugin.getConfigurationSchema().allowsBody());
    }

    @Test
    public void testExecuteWithSimplePattern() throws PluginException, InterruptedException {
        // Given
        when(configuration.getProperty("pattern", "")).thenReturn("\\d+");
        when(configuration.getProperty("source", "")).thenReturn("Test 123");
        when(configuration.getProperty("result", "")).thenReturn("$0");
        when(configuration.getProperty("max", "-1")).thenReturn("-1");
        when(configuration.getProperty("replace", "")).thenReturn("");
        when(configuration.getProperty("flagdotall", "false")).thenReturn("false");
        when(configuration.getProperty("flagmultiline", "false")).thenReturn("false");
        when(configuration.getProperty("flagcaseinsensitive", "false")).thenReturn("false");
        when(configuration.getProperty("flagcomments", "false")).thenReturn("false");
        when(configuration.getProperty("flagunixlines", "false")).thenReturn("false");
        when(configuration.getProperty("flagliteral", "false")).thenReturn("false");
        when(configuration.getProperty("flagunicodecase", "false")).thenReturn("false");
        when(configuration.getProperty("flagcanoneq", "false")).thenReturn("false");
        
        plugin.initialize(configuration);
        
        // When
        Variable result = plugin.execute(context);
        
        // Then
        assertNotNull(result);
    }

    @Test
    public void testExecuteWithMultipleMatches() throws PluginException, InterruptedException {
        // Given
        when(configuration.getProperty("pattern", "")).thenReturn("\\d+");
        when(configuration.getProperty("source", "")).thenReturn("Test 123 and 456");
        when(configuration.getProperty("result", "")).thenReturn("$0");
        when(configuration.getProperty("max", "-1")).thenReturn("-1");
        when(configuration.getProperty("replace", "")).thenReturn("");
        when(configuration.getProperty("flagdotall", "false")).thenReturn("false");
        when(configuration.getProperty("flagmultiline", "false")).thenReturn("false");
        when(configuration.getProperty("flagcaseinsensitive", "false")).thenReturn("false");
        when(configuration.getProperty("flagcomments", "false")).thenReturn("false");
        when(configuration.getProperty("flagunixlines", "false")).thenReturn("false");
        when(configuration.getProperty("flagliteral", "false")).thenReturn("false");
        when(configuration.getProperty("flagunicodecase", "false")).thenReturn("false");
        when(configuration.getProperty("flagcanoneq", "false")).thenReturn("false");
        
        plugin.initialize(configuration);
        
        // When
        Variable result = plugin.execute(context);
        
        // Then
        assertNotNull(result);
    }

    @Test
    public void testExecuteWithReplaceMode() throws PluginException, InterruptedException {
        // Given
        when(configuration.getProperty("pattern", "")).thenReturn("\\d+");
        when(configuration.getProperty("source", "")).thenReturn("Test 123");
        when(configuration.getProperty("result", "")).thenReturn("");
        when(configuration.getProperty("max", "-1")).thenReturn("-1");
        when(configuration.getProperty("replace", "")).thenReturn("XXX");
        when(configuration.getProperty("flagdotall", "false")).thenReturn("false");
        when(configuration.getProperty("flagmultiline", "false")).thenReturn("false");
        when(configuration.getProperty("flagcaseinsensitive", "false")).thenReturn("false");
        when(configuration.getProperty("flagcomments", "false")).thenReturn("false");
        when(configuration.getProperty("flagunixlines", "false")).thenReturn("false");
        when(configuration.getProperty("flagliteral", "false")).thenReturn("false");
        when(configuration.getProperty("flagunicodecase", "false")).thenReturn("false");
        when(configuration.getProperty("flagcanoneq", "false")).thenReturn("false");
        
        plugin.initialize(configuration);
        
        // When
        Variable result = plugin.execute(context);
        
        // Then
        assertNotNull(result);
    }

    @Test
    public void testExecuteWithMaxLimit() throws PluginException, InterruptedException {
        // Given
        when(configuration.getProperty("pattern", "")).thenReturn("\\d+");
        when(configuration.getProperty("source", "")).thenReturn("Test 123 and 456");
        when(configuration.getProperty("result", "")).thenReturn("$0");
        when(configuration.getProperty("max", "-1")).thenReturn("1");
        when(configuration.getProperty("replace", "")).thenReturn("");
        when(configuration.getProperty("flagdotall", "false")).thenReturn("false");
        when(configuration.getProperty("flagmultiline", "false")).thenReturn("false");
        when(configuration.getProperty("flagcaseinsensitive", "false")).thenReturn("false");
        when(configuration.getProperty("flagcomments", "false")).thenReturn("false");
        when(configuration.getProperty("flagunixlines", "false")).thenReturn("false");
        when(configuration.getProperty("flagliteral", "false")).thenReturn("false");
        when(configuration.getProperty("flagunicodecase", "false")).thenReturn("false");
        when(configuration.getProperty("flagcanoneq", "false")).thenReturn("false");
        
        plugin.initialize(configuration);
        
        // When
        Variable result = plugin.execute(context);
        
        // Then
        assertNotNull(result);
    }

    @Test
    public void testExecuteWithCaseInsensitiveFlag() throws PluginException, InterruptedException {
        // Given
        when(configuration.getProperty("pattern", "")).thenReturn("test");
        when(configuration.getProperty("source", "")).thenReturn("TEST test TeSt");
        when(configuration.getProperty("result", "")).thenReturn("$0");
        when(configuration.getProperty("max", "-1")).thenReturn("-1");
        when(configuration.getProperty("replace", "")).thenReturn("");
        when(configuration.getProperty("flagdotall", "false")).thenReturn("false");
        when(configuration.getProperty("flagmultiline", "false")).thenReturn("false");
        when(configuration.getProperty("flagcaseinsensitive", "false")).thenReturn("true");
        when(configuration.getProperty("flagcomments", "false")).thenReturn("false");
        when(configuration.getProperty("flagunixlines", "false")).thenReturn("false");
        when(configuration.getProperty("flagliteral", "false")).thenReturn("false");
        when(configuration.getProperty("flagunicodecase", "false")).thenReturn("false");
        when(configuration.getProperty("flagcanoneq", "false")).thenReturn("false");
        
        plugin.initialize(configuration);
        
        // When
        Variable result = plugin.execute(context);
        
        // Then
        assertNotNull(result);
    }

    @Test
    public void testExecuteWithDotAllFlag() throws PluginException, InterruptedException {
        // Given
        when(configuration.getProperty("pattern", "")).thenReturn(".*");
        when(configuration.getProperty("source", "")).thenReturn("Line1\nLine2");
        when(configuration.getProperty("result", "")).thenReturn("$0");
        when(configuration.getProperty("max", "-1")).thenReturn("-1");
        when(configuration.getProperty("replace", "")).thenReturn("");
        when(configuration.getProperty("flagdotall", "false")).thenReturn("true");
        when(configuration.getProperty("flagmultiline", "false")).thenReturn("false");
        when(configuration.getProperty("flagcaseinsensitive", "false")).thenReturn("false");
        when(configuration.getProperty("flagcomments", "false")).thenReturn("false");
        when(configuration.getProperty("flagunixlines", "false")).thenReturn("false");
        when(configuration.getProperty("flagliteral", "false")).thenReturn("false");
        when(configuration.getProperty("flagunicodecase", "false")).thenReturn("false");
        when(configuration.getProperty("flagcanoneq", "false")).thenReturn("false");
        
        plugin.initialize(configuration);
        
        // When
        Variable result = plugin.execute(context);
        
        // Then
        assertNotNull(result);
    }

    @Test
    public void testExecuteWithMultilineFlag() throws PluginException, InterruptedException {
        // Given
        when(configuration.getProperty("pattern", "")).thenReturn("^Line");
        when(configuration.getProperty("source", "")).thenReturn("Line1\nLine2");
        when(configuration.getProperty("result", "")).thenReturn("$0");
        when(configuration.getProperty("max", "-1")).thenReturn("-1");
        when(configuration.getProperty("replace", "")).thenReturn("");
        when(configuration.getProperty("flagdotall", "false")).thenReturn("false");
        when(configuration.getProperty("flagmultiline", "false")).thenReturn("true");
        when(configuration.getProperty("flagcaseinsensitive", "false")).thenReturn("false");
        when(configuration.getProperty("flagcomments", "false")).thenReturn("false");
        when(configuration.getProperty("flagunixlines", "false")).thenReturn("false");
        when(configuration.getProperty("flagliteral", "false")).thenReturn("false");
        when(configuration.getProperty("flagunicodecase", "false")).thenReturn("false");
        when(configuration.getProperty("flagcanoneq", "false")).thenReturn("false");
        
        plugin.initialize(configuration);
        
        // When
        Variable result = plugin.execute(context);
        
        // Then
        assertNotNull(result);
    }

    @Test
    public void testExecuteWithGroups() throws PluginException, InterruptedException {
        // Given
        when(configuration.getProperty("pattern", "")).thenReturn("(\\w+)\\s+(\\d+)");
        when(configuration.getProperty("source", "")).thenReturn("Test 123");
        when(configuration.getProperty("result", "")).thenReturn("$1-$2");
        when(configuration.getProperty("max", "-1")).thenReturn("-1");
        when(configuration.getProperty("replace", "")).thenReturn("");
        when(configuration.getProperty("flagdotall", "false")).thenReturn("false");
        when(configuration.getProperty("flagmultiline", "false")).thenReturn("false");
        when(configuration.getProperty("flagcaseinsensitive", "false")).thenReturn("false");
        when(configuration.getProperty("flagcomments", "false")).thenReturn("false");
        when(configuration.getProperty("flagunixlines", "false")).thenReturn("false");
        when(configuration.getProperty("flagliteral", "false")).thenReturn("false");
        when(configuration.getProperty("flagunicodecase", "false")).thenReturn("false");
        when(configuration.getProperty("flagcanoneq", "false")).thenReturn("false");
        
        plugin.initialize(configuration);
        
        // When
        Variable result = plugin.execute(context);
        
        // Then
        assertNotNull(result);
    }

    @Test
    public void testExecuteWithNoMatch() throws PluginException, InterruptedException {
        // Given
        when(configuration.getProperty("pattern", "")).thenReturn("\\d+");
        when(configuration.getProperty("source", "")).thenReturn("NoNumbers");
        when(configuration.getProperty("result", "")).thenReturn("$0");
        when(configuration.getProperty("max", "-1")).thenReturn("-1");
        when(configuration.getProperty("replace", "")).thenReturn("");
        when(configuration.getProperty("flagdotall", "false")).thenReturn("false");
        when(configuration.getProperty("flagmultiline", "false")).thenReturn("false");
        when(configuration.getProperty("flagcaseinsensitive", "false")).thenReturn("false");
        when(configuration.getProperty("flagcomments", "false")).thenReturn("false");
        when(configuration.getProperty("flagunixlines", "false")).thenReturn("false");
        when(configuration.getProperty("flagliteral", "false")).thenReturn("false");
        when(configuration.getProperty("flagunicodecase", "false")).thenReturn("false");
        when(configuration.getProperty("flagcanoneq", "false")).thenReturn("false");
        
        plugin.initialize(configuration);
        
        // When
        Variable result = plugin.execute(context);
        
        // Then
        assertNotNull(result);
    }

    @Test
    public void testExecuteWithEmptyPattern() throws PluginException, InterruptedException {
        // Given
        when(configuration.getProperty("pattern", "")).thenReturn("");
        when(configuration.getProperty("source", "")).thenReturn("Test");
        when(configuration.getProperty("result", "")).thenReturn("$0");
        when(configuration.getProperty("max", "-1")).thenReturn("-1");
        when(configuration.getProperty("replace", "")).thenReturn("");
        when(configuration.getProperty("flagdotall", "false")).thenReturn("false");
        when(configuration.getProperty("flagmultiline", "false")).thenReturn("false");
        when(configuration.getProperty("flagcaseinsensitive", "false")).thenReturn("false");
        when(configuration.getProperty("flagcomments", "false")).thenReturn("false");
        when(configuration.getProperty("flagunixlines", "false")).thenReturn("false");
        when(configuration.getProperty("flagliteral", "false")).thenReturn("false");
        when(configuration.getProperty("flagunicodecase", "false")).thenReturn("false");
        when(configuration.getProperty("flagcanoneq", "false")).thenReturn("false");
        
        plugin.initialize(configuration);
        
        // When
        Variable result = plugin.execute(context);
        
        // Then
        assertNotNull(result);
    }

    @Test
    public void testExecuteWithEmptySource() throws PluginException, InterruptedException {
        // Given
        when(configuration.getProperty("pattern", "")).thenReturn("\\d+");
        when(configuration.getProperty("source", "")).thenReturn("");
        when(configuration.getProperty("result", "")).thenReturn("$0");
        when(configuration.getProperty("max", "-1")).thenReturn("-1");
        when(configuration.getProperty("replace", "")).thenReturn("");
        when(configuration.getProperty("flagdotall", "false")).thenReturn("false");
        when(configuration.getProperty("flagmultiline", "false")).thenReturn("false");
        when(configuration.getProperty("flagcaseinsensitive", "false")).thenReturn("false");
        when(configuration.getProperty("flagcomments", "false")).thenReturn("false");
        when(configuration.getProperty("flagunixlines", "false")).thenReturn("false");
        when(configuration.getProperty("flagliteral", "false")).thenReturn("false");
        when(configuration.getProperty("flagunicodecase", "false")).thenReturn("false");
        when(configuration.getProperty("flagcanoneq", "false")).thenReturn("false");
        
        plugin.initialize(configuration);
        
        // When
        Variable result = plugin.execute(context);
        
        // Then
        assertNotNull(result);
    }

    @Test
    public void testExecuteWithDotAllFlagExtended() throws PluginException, InterruptedException {
        // Given
        when(configuration.getProperty("pattern", "")).thenReturn("test.*end");
        when(configuration.getProperty("source", "")).thenReturn("test\nsome\nlines\nend");
        when(configuration.getProperty("result", "")).thenReturn("$0");
        when(configuration.getProperty("max", "-1")).thenReturn("-1");
        when(configuration.getProperty("replace", "")).thenReturn("");
        when(configuration.getProperty("flagdotall", "false")).thenReturn("true");
        when(configuration.getProperty("flagmultiline", "false")).thenReturn("false");
        when(configuration.getProperty("flagcaseinsensitive", "false")).thenReturn("false");
        when(configuration.getProperty("flagcomments", "false")).thenReturn("false");
        when(configuration.getProperty("flagunixlines", "false")).thenReturn("false");
        when(configuration.getProperty("flagliteral", "false")).thenReturn("false");
        when(configuration.getProperty("flagunicodecase", "false")).thenReturn("false");
        when(configuration.getProperty("flagcanoneq", "false")).thenReturn("false");
        
        plugin.initialize(configuration);
        
        // When
        Variable result = plugin.execute(context);
        
        // Then
        assertNotNull(result);
    }

    @Test
    public void testExecuteWithMultilineFlagExtended() throws PluginException, InterruptedException {
        // Given
        when(configuration.getProperty("pattern", "")).thenReturn("^test");
        when(configuration.getProperty("source", "")).thenReturn("line1\ntest line");
        when(configuration.getProperty("result", "")).thenReturn("$0");
        when(configuration.getProperty("max", "-1")).thenReturn("-1");
        when(configuration.getProperty("replace", "")).thenReturn("");
        when(configuration.getProperty("flagdotall", "false")).thenReturn("false");
        when(configuration.getProperty("flagmultiline", "false")).thenReturn("true");
        when(configuration.getProperty("flagcaseinsensitive", "false")).thenReturn("false");
        when(configuration.getProperty("flagcomments", "false")).thenReturn("false");
        when(configuration.getProperty("flagunixlines", "false")).thenReturn("false");
        when(configuration.getProperty("flagliteral", "false")).thenReturn("false");
        when(configuration.getProperty("flagunicodecase", "false")).thenReturn("false");
        when(configuration.getProperty("flagcanoneq", "false")).thenReturn("false");
        
        plugin.initialize(configuration);
        
        // When
        Variable result = plugin.execute(context);
        
        // Then
        assertNotNull(result);
    }

    @Test
    public void testExecuteWithLiteralFlag() throws PluginException, InterruptedException {
        // Given
        when(configuration.getProperty("pattern", "")).thenReturn("test.*");
        when(configuration.getProperty("source", "")).thenReturn("test.*");
        when(configuration.getProperty("result", "")).thenReturn("$0");
        when(configuration.getProperty("max", "-1")).thenReturn("-1");
        when(configuration.getProperty("replace", "")).thenReturn("");
        when(configuration.getProperty("flagdotall", "false")).thenReturn("false");
        when(configuration.getProperty("flagmultiline", "false")).thenReturn("false");
        when(configuration.getProperty("flagcaseinsensitive", "false")).thenReturn("false");
        when(configuration.getProperty("flagcomments", "false")).thenReturn("false");
        when(configuration.getProperty("flagunixlines", "false")).thenReturn("false");
        when(configuration.getProperty("flagliteral", "false")).thenReturn("true");
        when(configuration.getProperty("flagunicodecase", "false")).thenReturn("false");
        when(configuration.getProperty("flagcanoneq", "false")).thenReturn("false");
        
        plugin.initialize(configuration);
        
        // When
        Variable result = plugin.execute(context);
        
        // Then
        assertNotNull(result);
    }

    @Test
    public void testExecuteWithMaxMatches() throws PluginException, InterruptedException {
        // Given
        when(configuration.getProperty("pattern", "")).thenReturn("\\d+");
        when(configuration.getProperty("source", "")).thenReturn("1 2 3 4 5");
        when(configuration.getProperty("result", "")).thenReturn("$0");
        when(configuration.getProperty("max", "-1")).thenReturn("2");
        when(configuration.getProperty("replace", "")).thenReturn("");
        when(configuration.getProperty("flagdotall", "false")).thenReturn("false");
        when(configuration.getProperty("flagmultiline", "false")).thenReturn("false");
        when(configuration.getProperty("flagcaseinsensitive", "false")).thenReturn("false");
        when(configuration.getProperty("flagcomments", "false")).thenReturn("false");
        when(configuration.getProperty("flagunixlines", "false")).thenReturn("false");
        when(configuration.getProperty("flagliteral", "false")).thenReturn("false");
        when(configuration.getProperty("flagunicodecase", "false")).thenReturn("false");
        when(configuration.getProperty("flagcanoneq", "false")).thenReturn("false");
        
        plugin.initialize(configuration);
        
        // When
        Variable result = plugin.execute(context);
        
        // Then
        assertNotNull(result);
    }

    @Test
    public void testExecuteWithReplace() throws PluginException, InterruptedException {
        // Given
        when(configuration.getProperty("pattern", "")).thenReturn("\\d+");
        when(configuration.getProperty("source", "")).thenReturn("test 123 456");
        when(configuration.getProperty("result", "")).thenReturn("$0");
        when(configuration.getProperty("max", "-1")).thenReturn("-1");
        when(configuration.getProperty("replace", "")).thenReturn("XXX");
        when(configuration.getProperty("flagdotall", "false")).thenReturn("false");
        when(configuration.getProperty("flagmultiline", "false")).thenReturn("false");
        when(configuration.getProperty("flagcaseinsensitive", "false")).thenReturn("false");
        when(configuration.getProperty("flagcomments", "false")).thenReturn("false");
        when(configuration.getProperty("flagunixlines", "false")).thenReturn("false");
        when(configuration.getProperty("flagliteral", "false")).thenReturn("false");
        when(configuration.getProperty("flagunicodecase", "false")).thenReturn("false");
        when(configuration.getProperty("flagcanoneq", "false")).thenReturn("false");
        
        plugin.initialize(configuration);
        
        // When
        Variable result = plugin.execute(context);
        
        // Then
        assertNotNull(result);
    }

    @Test
    public void testExecuteWithComplexPattern() throws PluginException, InterruptedException {
        // Given
        when(configuration.getProperty("pattern", "")).thenReturn("([a-z]+)@([a-z]+)\\.([a-z]+)");
        when(configuration.getProperty("source", "")).thenReturn("Email: test@example.com");
        when(configuration.getProperty("result", "")).thenReturn("$1 at $2 dot $3");
        when(configuration.getProperty("max", "-1")).thenReturn("-1");
        when(configuration.getProperty("replace", "")).thenReturn("");
        when(configuration.getProperty("flagdotall", "false")).thenReturn("false");
        when(configuration.getProperty("flagmultiline", "false")).thenReturn("false");
        when(configuration.getProperty("flagcaseinsensitive", "false")).thenReturn("false");
        when(configuration.getProperty("flagcomments", "false")).thenReturn("false");
        when(configuration.getProperty("flagunixlines", "false")).thenReturn("false");
        when(configuration.getProperty("flagliteral", "false")).thenReturn("false");
        when(configuration.getProperty("flagunicodecase", "false")).thenReturn("false");
        when(configuration.getProperty("flagcanoneq", "false")).thenReturn("false");
        
        plugin.initialize(configuration);
        
        // When
        Variable result = plugin.execute(context);
        
        // Then
        assertNotNull(result);
    }

    @Test
    public void testExecuteWithUnicodePattern() throws PluginException, InterruptedException {
        // Given
        when(configuration.getProperty("pattern", "")).thenReturn("测试");
        when(configuration.getProperty("source", "")).thenReturn("This is 测试 text");
        when(configuration.getProperty("result", "")).thenReturn("$0");
        when(configuration.getProperty("max", "-1")).thenReturn("-1");
        when(configuration.getProperty("replace", "")).thenReturn("");
        when(configuration.getProperty("flagdotall", "false")).thenReturn("false");
        when(configuration.getProperty("flagmultiline", "false")).thenReturn("false");
        when(configuration.getProperty("flagcaseinsensitive", "false")).thenReturn("false");
        when(configuration.getProperty("flagcomments", "false")).thenReturn("false");
        when(configuration.getProperty("flagunixlines", "false")).thenReturn("false");
        when(configuration.getProperty("flagliteral", "false")).thenReturn("false");
        when(configuration.getProperty("flagunicodecase", "false")).thenReturn("false");
        when(configuration.getProperty("flagcanoneq", "false")).thenReturn("false");
        
        plugin.initialize(configuration);
        
        // When
        Variable result = plugin.execute(context);
        
        // Then
        assertNotNull(result);
    }

    @Test
    public void testExecuteMultipleTimes() throws PluginException, InterruptedException {
        // Given
        when(configuration.getProperty("pattern", "")).thenReturn("test");
        when(configuration.getProperty("source", "")).thenReturn("test string");
        when(configuration.getProperty("result", "")).thenReturn("$0");
        when(configuration.getProperty("max", "-1")).thenReturn("-1");
        when(configuration.getProperty("replace", "")).thenReturn("");
        when(configuration.getProperty("flagdotall", "false")).thenReturn("false");
        when(configuration.getProperty("flagmultiline", "false")).thenReturn("false");
        when(configuration.getProperty("flagcaseinsensitive", "false")).thenReturn("false");
        when(configuration.getProperty("flagcomments", "false")).thenReturn("false");
        when(configuration.getProperty("flagunixlines", "false")).thenReturn("false");
        when(configuration.getProperty("flagliteral", "false")).thenReturn("false");
        when(configuration.getProperty("flagunicodecase", "false")).thenReturn("false");
        when(configuration.getProperty("flagcanoneq", "false")).thenReturn("false");
        
        plugin.initialize(configuration);
        
        // When
        Variable result1 = plugin.execute(context);
        Variable result2 = plugin.execute(context);
        Variable result3 = plugin.execute(context);
        
        // Then
        assertNotNull(result1);
        assertNotNull(result2);
        assertNotNull(result3);
    }
}

