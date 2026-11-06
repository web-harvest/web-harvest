package org.webharvest.plugin.core;

import com.google.inject.Guice;
import com.google.inject.Injector;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.webharvest.ioc.ScraperModule;
import org.webharvest.plugin.DefaultPluginConfiguration;
import org.webharvest.plugin.PluginConfiguration;
import org.webharvest.definition.Config;
import org.webharvest.runtime.DynamicScopeContext;
import org.webharvest.runtime.templaters.BaseTemplater;
import org.webharvest.runtime.variables.EmptyVariable;
import org.webharvest.runtime.variables.NodeVariable;
import org.webharvest.runtime.variables.Variable;

import static org.mockito.Mockito.mock;
import static org.testng.Assert.*;

/**
 * Integration tests for WhilePlugin with SetVarPlugin to verify correct behavior together.
 * 
 * v2.2.x FIX: This test uses REAL ScraperContext (not mocks) because:
 * 1. BaseTemplater.scriptEngineFactory must be initialized via Guice
 * 2. JSRScriptEngineAdapter.copyVariables() requires context to be Iterable
 * 3. This is a true integration test of plugin interaction
 */
public class WhilePluginIntegrationTest {

    private Injector injector;
    private DynamicScopeContext context;
    private WhilePlugin whilePlugin;
    private SetVarPlugin setVarPlugin;
    private DefaultPluginConfiguration whileConfig;
    private DefaultPluginConfiguration setVarConfig;

    @BeforeMethod
    public void setUp() {
        // v2.2.x: Create Guice injector and get real ScraperContext
        injector = Guice.createInjector(new ScraperModule("."));
        context = injector.getInstance(org.webharvest.runtime.ScraperContext.class);
        
        // Setup mock config for context (needed by ScraperContext)
        Config mockConfig = mock(Config.class);
        context.setConfig(mockConfig);
        context.setScriptingLanguage(org.webharvest.runtime.scripting.ScriptingLanguage.BEANSHELL);
        
        whilePlugin = new WhilePlugin();
        setVarPlugin = new SetVarPlugin();
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
        
        // Setup initial counter variable in the real context.
        context.setLocalVar("counter", new NodeVariable("0"));
        
        // When: Execute SetVarPlugin first to increment counter
        setVarPlugin.initialize(setVarConfig);
        Variable setVarResult = setVarPlugin.execute(context);
        
        // Then: Should increment counter
        assertNotNull(setVarResult);
        assertEquals(context.getVar("counter").toString(), "1",
                "SetVarPlugin should evaluate and store the value attribute");
        
        // When: Execute WhilePlugin
        whilePlugin.initialize(whileConfig);
        Variable whileResult = whilePlugin.execute(context);

        // Then: Should execute body multiple times
        assertNotNull(whileResult);
        assertTrue(whileResult instanceof EmptyVariable,
                "An empty while body produces no list result");
        assertEquals(context.getVar("counter").toString(), "3",
                "While should re-evaluate the counter condition after each iteration");
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
        
        // Setup initial index variable on the real context.
        context.setLocalVar("i", new NodeVariable("0"));
        
        // When: Execute SetVarPlugin first to increment index
        setVarPlugin.initialize(setVarConfig);
        Variable setVarResult = setVarPlugin.execute(context);
        
        // Then: Should increment index
        assertNotNull(setVarResult);
        assertEquals(context.getVar("i").toString(), "1",
                "SetVarPlugin should update the index variable");
        
        // When: Execute WhilePlugin
        whilePlugin.initialize(whileConfig);
        Variable whileResult = whilePlugin.execute(context);

        // Then: Should execute body multiple times
        assertNotNull(whileResult);
        assertTrue(whileResult instanceof EmptyVariable,
                "An empty while body produces no list result");
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
        
        // Setup initial count variable on the real context.
        context.setLocalVar("count", new NodeVariable("0"));
        
        // When: Execute SetVarPlugin first to increment count
        setVarPlugin.initialize(setVarConfig);
        Variable setVarResult = setVarPlugin.execute(context);
        
        // Then: Should increment count
        assertNotNull(setVarResult);
        assertEquals(context.getVar("count").toString(), "1",
                "SetVarPlugin should update the count variable");
        
        // When: Execute WhilePlugin
        whilePlugin.initialize(whileConfig);
        Variable whileResult = whilePlugin.execute(context);

        // Then: Should execute body multiple times
        assertNotNull(whileResult);
        assertTrue(whileResult instanceof EmptyVariable,
                "An empty while body produces no list result");
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
        Variable setVarResult = setVarPlugin.execute(context);
        
        // Then: Should set variable
        assertNotNull(setVarResult);
        assertEquals(context.getVar("testVar").toString(), "testValue",
                "SetVarPlugin should set the requested variable value");
        
        // When: Execute WhilePlugin
        whilePlugin.initialize(whileConfig);
        Variable whileResult = whilePlugin.execute(context);

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
        
        // Setup initial flag variable on the real context.
        context.setLocalVar("flag", new NodeVariable("true"));
        
        // When: Execute SetVarPlugin first to set flag to false
        setVarPlugin.initialize(setVarConfig);
        Variable setVarResult = setVarPlugin.execute(context);
        
        // Then: Should set flag to false
        assertNotNull(setVarResult);
        assertEquals(context.getVar("flag").toString(), "false",
                "SetVarPlugin should update the flag before evaluating while");
        
        // When: Execute WhilePlugin
        whilePlugin.initialize(whileConfig);
        Variable whileResult = whilePlugin.execute(context);

        // Then: Should execute body once then stop
        assertNotNull(whileResult);
        assertTrue(whileResult instanceof EmptyVariable,
                "The false condition prevents execution of the empty body");
    }
}