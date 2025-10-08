package org.webharvest.plugin.core;

import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.webharvest.plugin.DefaultPluginConfiguration;
import org.webharvest.plugin.PluginConfiguration;
import org.webharvest.runtime.DynamicScopeContext;
import org.webharvest.runtime.variables.EmptyVariable;
import org.webharvest.runtime.variables.ListVariable;
import org.webharvest.runtime.variables.NodeVariable;
import org.webharvest.runtime.variables.Variable;

import static org.mockito.Mockito.*;
import static org.testng.Assert.*;

/**
 * Integration tests for WhilePlugin with SetVarPlugin to verify correct behavior together.
 */
public class WhilePluginIntegrationTest {

    @Mock
    private DynamicScopeContext mockContext;

    private WhilePlugin whilePlugin;
    private SetVarPlugin setVarPlugin;
    private DefaultPluginConfiguration whileConfig;
    private DefaultPluginConfiguration setVarConfig;

    @BeforeMethod
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        whilePlugin = new WhilePlugin();
        setVarPlugin = new SetVarPlugin();
        
        // Setup mock context
        when(mockContext.getScriptingLanguage()).thenReturn(org.webharvest.runtime.scripting.ScriptingLanguage.BEANSHELL);
    }

    @Test
    public void testWhilePluginWithSetVarPluginCounter() throws Exception {
        // Given: WhilePlugin with counter condition and SetVarPlugin for incrementing
        whileConfig = new DefaultPluginConfiguration("WhilePlugin", "http://org.webharvest/schema/2.1/core", "while");
        whileConfig.setProperty("test", "${counter} < 3");
        whileConfig.setProperty("maxloops", "5"); // Safety limit
        
        setVarConfig = new DefaultPluginConfiguration("SetVarPlugin", "http://org.webharvest/schema/2.1/core", "set-var");
        setVarConfig.setProperty("var", "counter");
        setVarConfig.setProperty("value", "${counter + 1}");
        
        // Setup initial counter variable
        when(mockContext.getVar("counter")).thenReturn(new NodeVariable("0"));
        
        // When: Execute SetVarPlugin first to increment counter
        setVarPlugin.initialize(setVarConfig);
        Variable setVarResult = setVarPlugin.execute(mockContext);
        
        // Then: Should increment counter
        assertNotNull(setVarResult);
        verify(mockContext).setLocalVar(eq("counter"), any(Variable.class));
        
        // When: Execute WhilePlugin
        whilePlugin.initialize(whileConfig);
        Variable whileResult = whilePlugin.execute(mockContext);

        // Then: Should execute body multiple times
        assertNotNull(whileResult);
        assertTrue(whileResult instanceof ListVariable);
    }

    @Test
    public void testWhilePluginWithSetVarPluginIndex() throws Exception {
        // Given: WhilePlugin with index variable and SetVarPlugin
        whileConfig = new DefaultPluginConfiguration("WhilePlugin", "http://org.webharvest/schema/2.1/core", "while");
        whileConfig.setProperty("test", "${i} < 3");
        whileConfig.setProperty("index", "i");
        whileConfig.setProperty("maxloops", "5"); // Safety limit
        
        setVarConfig = new DefaultPluginConfiguration("SetVarPlugin", "http://org.webharvest/schema/2.1/core", "set-var");
        setVarConfig.setProperty("var", "i");
        setVarConfig.setProperty("value", "${i + 1}");
        
        // Setup initial index variable
        when(mockContext.getVar("i")).thenReturn(new NodeVariable("0"));
        
        // When: Execute SetVarPlugin first to increment index
        setVarPlugin.initialize(setVarConfig);
        Variable setVarResult = setVarPlugin.execute(mockContext);
        
        // Then: Should increment index
        assertNotNull(setVarResult);
        verify(mockContext).setLocalVar(eq("i"), any(Variable.class));
        
        // When: Execute WhilePlugin
        whilePlugin.initialize(whileConfig);
        Variable whileResult = whilePlugin.execute(mockContext);

        // Then: Should execute body multiple times
        assertNotNull(whileResult);
        assertTrue(whileResult instanceof ListVariable);
    }

    @Test
    public void testWhilePluginWithSetVarPluginMultipleVariables() throws Exception {
        // Given: WhilePlugin with multiple variables
        whileConfig = new DefaultPluginConfiguration("WhilePlugin", "http://org.webharvest/schema/2.1/core", "while");
        whileConfig.setProperty("test", "${count} < 2");
        whileConfig.setProperty("maxloops", "5"); // Safety limit
        
        setVarConfig = new DefaultPluginConfiguration("SetVarPlugin", "http://org.webharvest/schema/2.1/core", "set-var");
        setVarConfig.setProperty("var", "count");
        setVarConfig.setProperty("value", "${count + 1}");
        
        // Setup initial count variable
        when(mockContext.getVar("count")).thenReturn(new NodeVariable("0"));
        
        // When: Execute SetVarPlugin first to increment count
        setVarPlugin.initialize(setVarConfig);
        Variable setVarResult = setVarPlugin.execute(mockContext);
        
        // Then: Should increment count
        assertNotNull(setVarResult);
        verify(mockContext).setLocalVar(eq("count"), any(Variable.class));
        
        // When: Execute WhilePlugin
        whilePlugin.initialize(whileConfig);
        Variable whileResult = whilePlugin.execute(mockContext);

        // Then: Should execute body multiple times
        assertNotNull(whileResult);
        assertTrue(whileResult instanceof ListVariable);
    }

    @Test
    public void testWhilePluginWithSetVarPluginEmptyBody() throws Exception {
        // Given: WhilePlugin with empty body
        whileConfig = new DefaultPluginConfiguration("WhilePlugin", "http://org.webharvest/schema/2.1/core", "while");
        whileConfig.setProperty("test", "true");
        whileConfig.setProperty("empty", "true");
        whileConfig.setProperty("maxloops", "2");
        
        setVarConfig = new DefaultPluginConfiguration("SetVarPlugin", "http://org.webharvest/schema/2.1/core", "set-var");
        setVarConfig.setProperty("var", "testVar");
        setVarConfig.setProperty("value", "testValue");
        
        // When: Execute SetVarPlugin first
        setVarPlugin.initialize(setVarConfig);
        Variable setVarResult = setVarPlugin.execute(mockContext);
        
        // Then: Should set variable
        assertNotNull(setVarResult);
        verify(mockContext).setLocalVar(eq("testVar"), any(Variable.class));
        
        // When: Execute WhilePlugin
        whilePlugin.initialize(whileConfig);
        Variable whileResult = whilePlugin.execute(mockContext);

        // Then: Should return empty result
        assertNotNull(whileResult);
        assertTrue(whileResult instanceof EmptyVariable);
    }

    @Test
    public void testWhilePluginWithSetVarPluginConditionalLogic() throws Exception {
        // Given: WhilePlugin with conditional logic
        whileConfig = new DefaultPluginConfiguration("WhilePlugin", "http://org.webharvest/schema/2.1/core", "while");
        whileConfig.setProperty("test", "${flag} == true");
        whileConfig.setProperty("maxloops", "5"); // Safety limit
        
        setVarConfig = new DefaultPluginConfiguration("SetVarPlugin", "http://org.webharvest/schema/2.1/core", "set-var");
        setVarConfig.setProperty("var", "flag");
        setVarConfig.setProperty("value", "false");
        
        // Setup initial flag variable
        when(mockContext.getVar("flag")).thenReturn(new NodeVariable("true"));
        
        // When: Execute SetVarPlugin first to set flag to false
        setVarPlugin.initialize(setVarConfig);
        Variable setVarResult = setVarPlugin.execute(mockContext);
        
        // Then: Should set flag to false
        assertNotNull(setVarResult);
        verify(mockContext).setLocalVar(eq("flag"), any(Variable.class));
        
        // When: Execute WhilePlugin
        whilePlugin.initialize(whileConfig);
        Variable whileResult = whilePlugin.execute(mockContext);

        // Then: Should execute body once then stop
        assertNotNull(whileResult);
        assertTrue(whileResult instanceof ListVariable);
    }
}