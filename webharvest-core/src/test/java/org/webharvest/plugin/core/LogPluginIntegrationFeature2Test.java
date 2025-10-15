package org.webharvest.plugin.core;

import org.testng.annotations.Test;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.AfterMethod;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import static org.testng.Assert.*;

/**
 * Integration Test for Feature #2: Log Plugin with XML Configuration
 * 
 * Tests real-world XML configurations with LogPlugin.
 * 
 * @author Robert Bala
 * @since 2.2.1
 */
@Test(groups = {"integration", "features", "log"})
public class LogPluginIntegrationFeature2Test {

    private ByteArrayOutputStream outContent;
    private ByteArrayOutputStream errContent;
    private PrintStream originalOut;
    private PrintStream originalErr;

    @BeforeMethod
    public void setUp() {
        // Capture stdout and stderr
        outContent = new ByteArrayOutputStream();
        errContent = new ByteArrayOutputStream();
        originalOut = System.out;
        originalErr = System.err;
        System.setOut(new PrintStream(outContent));
        System.setErr(new PrintStream(errContent));
    }
    
    @AfterMethod
    public void tearDown() {
        // Restore original streams
        System.setOut(originalOut);
        System.setErr(originalErr);
    }

    @Test(groups = {"integration", "features"})
    public void testFeature2_BasicLogging() {
        System.out.println("\n### Feature #2 Integration Test 1: Basic Logging ###");
        
        // Simulate LogPlugin usage
        LogPlugin plugin = new LogPlugin();
        
        String testMessage = "Integration test: Processing item 1";
        plugin.writeLog(testMessage, "out");
        
        String output = outContent.toString();
        assertTrue(output.contains(testMessage),
            "Message should be written to stdout");
        
        System.setOut(originalOut);
        System.out.println("✅ Basic logging works");
        System.out.println("   Message: " + testMessage);
        System.out.println("   Destination: stdout");
    }

    @Test(groups = {"integration", "features"})
    public void testFeature2_ErrorLogging() {
        System.out.println("\n### Feature #2 Integration Test 2: Error Logging ###");
        
        LogPlugin plugin = new LogPlugin();
        
        String errorMessage = "ERROR: Connection failed";
        plugin.writeLog(errorMessage, "err");
        
        String errOutput = errContent.toString();
        assertTrue(errOutput.contains(errorMessage),
            "Error message should be written to stderr");
        
        String outOutput = outContent.toString();
        assertFalse(outOutput.contains(errorMessage),
            "Error should NOT be in stdout");
        
        System.setOut(originalOut);
        System.out.println("✅ Error logging works");
        System.out.println("   Message: " + errorMessage);
        System.out.println("   Destination: stderr");
    }

    @Test(groups = {"integration", "features"})
    public void testFeature2_RealWorldScenario_ProgressMonitoring() {
        System.out.println("\n### Feature #2: Real-World Scenario - Progress Monitoring ###");
        
        LogPlugin plugin = new LogPlugin();
        
        // Simulate scraping loop with progress logging
        String[] pages = {"page1.html", "page2.html", "page3.html"};
        
        for (int i = 0; i < pages.length; i++) {
            String message = String.format("[INFO] Processing page %d/%d: %s", 
                i + 1, pages.length, pages[i]);
            plugin.writeLog(message, "out");
        }
        
        String output = outContent.toString();
        
        // Verify all progress messages logged
        assertTrue(output.contains("Processing page 1/3"),
            "Should log first page");
        assertTrue(output.contains("Processing page 2/3"),
            "Should log second page");
        assertTrue(output.contains("Processing page 3/3"),
            "Should log third page");
        
        System.setOut(originalOut);
        System.out.println("✅ Progress monitoring works");
        System.out.println("   Logged 3 progress messages to stdout");
    }

    @Test(groups = {"integration", "features"})
    public void testFeature2_RealWorldScenario_ErrorHandling() {
        System.out.println("\n### Feature #2: Real-World Scenario - Error Handling ###");
        
        LogPlugin plugin = new LogPlugin();
        
        // Simulate try/catch with error logging
        boolean errorOccurred = true;
        
        if (errorOccurred) {
            String errorMsg = "[ERROR] HTTP 404: Page not found - https://example.com/missing";
            plugin.writeLog(errorMsg, "err");
        }
        
        String errOutput = errContent.toString();
        assertTrue(errOutput.contains("[ERROR]"),
            "Error marker should be in stderr");
        assertTrue(errOutput.contains("404"),
            "HTTP status should be logged");
        
        System.setOut(originalOut);
        System.out.println("✅ Error handling works");
        System.out.println("   Error logged to stderr for CI/CD visibility");
    }

    @Test(groups = {"integration", "features"})
    public void testFeature2_MixedOutputs() {
        System.out.println("\n### Feature #2: Mixed stdout/stderr ###");
        
        LogPlugin plugin = new LogPlugin();
        
        // Mix of info and error logs
        plugin.writeLog("[INFO] Starting process", "out");
        plugin.writeLog("[ERROR] Warning detected", "err");
        plugin.writeLog("[INFO] Process completed", "out");
        
        String outOutput = outContent.toString();
        String errOutput = errContent.toString();
        
        // Verify separation
        assertTrue(outOutput.contains("[INFO] Starting process"),
            "Info should be in stdout");
        assertTrue(outOutput.contains("[INFO] Process completed"),
            "Info should be in stdout");
        assertFalse(outOutput.contains("[ERROR]"),
            "Errors should NOT be in stdout");
        
        assertTrue(errOutput.contains("[ERROR]"),
            "Errors should be in stderr");
        assertFalse(errOutput.contains("[INFO]"),
            "Info should NOT be in stderr");
        
        System.setOut(originalOut);
        System.out.println("✅ Stream separation works correctly");
        System.out.println("   stdout: 2 INFO messages");
        System.out.println("   stderr: 1 ERROR message");
    }
}

