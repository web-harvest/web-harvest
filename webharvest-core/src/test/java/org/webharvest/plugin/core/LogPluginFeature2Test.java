package org.webharvest.plugin.core;

import org.testng.annotations.Test;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.AfterMethod;
import org.webharvest.runtime.DynamicScopeContext;
import org.webharvest.runtime.variables.Variable;
import org.webharvest.plugin.PluginConfiguration;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.Map;

import static org.testng.Assert.*;
import static org.mockito.Mockito.*;

/**
 * TDD Test for Feature Request #2: Log Plugin
 * 
 * Plugin to write text to stdout or stderr.
 * 
 * @author Robert Bala
 * @since 2.2.0
 */
public class LogPluginFeature2Test {

    private LogPlugin plugin;
    
    @Mock
    private DynamicScopeContext context;
    
    @Mock
    private PluginConfiguration config;
    
    private ByteArrayOutputStream outContent;
    private ByteArrayOutputStream errContent;
    private PrintStream originalOut;
    private PrintStream originalErr;

    @BeforeMethod
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        plugin = new LogPlugin();
        
        // Capture stdout and stderr
        outContent = new ByteArrayOutputStream();
        errContent = new ByteArrayOutputStream();
        originalOut = System.out;
        originalErr = System.err;
        System.setOut(new PrintStream(outContent));
        System.setErr(new PrintStream(errContent));
        
        when(context.getCharset()).thenReturn("UTF-8");
    }
    
    @AfterMethod
    public void tearDown() {
        // Restore original streams
        System.setOut(originalOut);
        System.setErr(originalErr);
    }

    @Test
    public void testCycle1_PluginExists() {
        // TDD Cycle 1: RED - Does the class exist?
        assertNotNull(plugin, "LogPlugin should exist");
    }
    
    @Test
    public void testCycle2_WritesToStdoutByDefault() throws Exception {
        // TDD Cycle 2: RED - Should write to stdout by default
        
        String testMessage = "Test log message";
        
        Map<String, String> props = new HashMap<>();
        props.put("dest", "out"); // or no dest = default stdout
        
        when(config.getProperty("dest")).thenReturn("out");
        when(config.getAllProperties()).thenReturn(props);
        
        plugin.initialize(config);
        
        // Mock executeBody to return test message
        Variable result = plugin.writeLog(testMessage, "out");
        
        // Verify message written to stdout
        String output = outContent.toString();
        assertTrue(output.contains(testMessage),
            "Message should be written to stdout");
        
        // Verify NOT written to stderr
        String errOutput = errContent.toString();
        assertFalse(errOutput.contains(testMessage),
            "Message should NOT be written to stderr");
    }
    
    @Test
    public void testCycle3_WritesToStderr() throws Exception {
        // TDD Cycle 3: RED - Should write to stderr when dest="err"
        
        String testMessage = "Error log message";
        
        Map<String, String> props = new HashMap<>();
        props.put("dest", "err");
        
        when(config.getProperty("dest")).thenReturn("err");
        when(config.getAllProperties()).thenReturn(props);
        
        plugin.initialize(config);
        
        Variable result = plugin.writeLog(testMessage, "err");
        
        // Verify message written to stderr
        String errOutput = errContent.toString();
        assertTrue(errOutput.contains(testMessage),
            "Message should be written to stderr");
        
        // Verify NOT written to stdout
        String outOutput = outContent.toString();
        assertFalse(outOutput.contains(testMessage),
            "Message should NOT be written to stdout");
    }
    
    @Test
    public void testCycle4_DefaultDestIsOut() throws Exception {
        // TDD Cycle 4: REGRESSION - No dest = stdout (default)
        
        String testMessage = "Default destination test";
        
        Map<String, String> props = new HashMap<>();
        // No dest property
        
        when(config.getProperty("dest")).thenReturn(null);
        when(config.getAllProperties()).thenReturn(props);
        
        plugin.initialize(config);
        
        Variable result = plugin.writeLog(testMessage, null);
        
        // Should default to stdout
        String output = outContent.toString();
        assertTrue(output.contains(testMessage),
            "Default should be stdout");
    }
}

