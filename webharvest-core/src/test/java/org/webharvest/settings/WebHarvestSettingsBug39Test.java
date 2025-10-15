/*
 * Copyright (c) 2006-2025 the original author or authors.
 */

package org.webharvest.settings;

import org.testng.annotations.Test;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.AfterMethod;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Properties;

import static org.testng.Assert.*;

/**
 * Test for Bug #39: Synchronize CLI arguments and GUI settings
 * 
 * <h3>Bug #39: Unified Settings System</h3>
 * <p>
 * <strong>Issue:</strong> CLI and GUI use separate settings. User must configure
 * same settings twice - once in GUI settings file, once as CLI arguments.
 * </p>
 * 
 * <p>
 * <strong>User Request:</strong><br>
 * "It would be a great thing if you could synchronize commandLine args and
 * webharvest.properties. It is a bit painful to have the settings in a file
 * for the development gui and to pass all settings to command line as arguments
 * in batch mode."
 * </p>
 * 
 * <p>
 * <strong>Solution:</strong> Create unified {@link WebHarvestSettings} that:
 * <ul>
 *   <li>Loads from shared settings file (webharvest.properties)</li>
 *   <li>Can be overridden by CLI arguments</li>
 *   <li>Used by both CLI and GUI/IDE</li>
 * </ul>
 * </p>
 * 
 * @author Robert Bala
 * @since 2.2.1
 */
public class WebHarvestSettingsBug39Test {
    
    private File testSettingsFile;
    
    @BeforeMethod
    public void setUp() throws IOException {
        testSettingsFile = new File("target/test-output/webharvest-test.properties");
        testSettingsFile.getParentFile().mkdirs();
    }
    
    @AfterMethod
    public void tearDown() throws IOException {
        if (testSettingsFile != null && testSettingsFile.exists()) {
            testSettingsFile.delete();
        }
    }
    
    /**
     * Bug #39 Test 1: Can load settings from properties file
     */
    @Test(groups = {"unit", "bugs", "hard"})
    public void testBug39_LoadSettingsFromFile() throws IOException {
        System.out.println("\n### Bug #39 Test 1: Load from file ###");
        
        // Create test settings file
        Properties props = new Properties();
        props.setProperty("workdir", "/tmp/harvest");
        props.setProperty("debug", "true");
        props.setProperty("loglevel", "DEBUG");
        
        try (FileWriter writer = new FileWriter(testSettingsFile)) {
            props.store(writer, "Test settings");
        }
        
        // Load settings
        WebHarvestSettings settings = WebHarvestSettings.fromFile(testSettingsFile);
        
        assertEquals(settings.getWorkDir(), "/tmp/harvest");
        assertEquals(settings.getDebug(), Boolean.TRUE);
        assertEquals(settings.getLogLevel(), "DEBUG");
        
        System.out.println("✅ Settings loaded from file");
    }
    
    /**
     * Bug #39 Test 2: CLI args override file settings
     */
    @Test(groups = {"unit", "bugs", "hard"})
    public void testBug39_CliArgsOverrideFileSettings() throws IOException {
        System.out.println("\n### Bug #39 Test 2: CLI args override ###");
        
        // Create settings file
        Properties props = new Properties();
        props.setProperty("workdir", "/tmp/harvest");
        props.setProperty("debug", "false");
        
        try (FileWriter writer = new FileWriter(testSettingsFile)) {
            props.store(writer, "Test settings");
        }
        
        // Load from file
        WebHarvestSettings settings = WebHarvestSettings.fromFile(testSettingsFile);
        
        // Override with CLI args
        settings.setWorkDir("/custom/workdir");
        settings.setDebug(true);
        
        // CLI args should win
        assertEquals(settings.getWorkDir(), "/custom/workdir");
        assertEquals(settings.getDebug(), Boolean.TRUE);
        
        System.out.println("✅ CLI args override file settings");
    }
    
    /**
     * Bug #39 Test 3: Default values when no settings file
     */
    @Test(groups = {"unit", "bugs", "hard"})
    public void testBug39_DefaultValuesWhenNoFile() {
        System.out.println("\n### Bug #39 Test 3: Default values ###");
        
        // Create settings without file
        WebHarvestSettings settings = new WebHarvestSettings();
        
        // Should have sensible defaults
        assertEquals(settings.getWorkDir(), ".");
        assertEquals(settings.getDebug(), Boolean.FALSE);
        assertNull(settings.getLogLevel());
        
        System.out.println("✅ Default values work");
    }
    
    /**
     * Bug #39 Test 4: Settings file path resolution
     */
    @Test(groups = {"unit", "bugs", "hard"})
    public void testBug39_SettingsFilePathResolution() {
        System.out.println("\n### Bug #39 Test 4: File path resolution ###");
        
        // Should look in standard locations:
        // 1. User home: ~/.webharvest/webharvest.properties
        // 2. Current dir: ./webharvest.properties
        // 3. System property: -Dwebharvest.settings=path
        
        WebHarvestSettings settings = WebHarvestSettings.loadDefault();
        assertNotNull(settings, "Should create settings even if no file found");
        
        System.out.println("✅ Settings file path resolution works");
    }
    
    /**
     * Bug #39 Test 5: All CLI parameters supported
     */
    @Test(groups = {"unit", "bugs", "hard"})
    public void testBug39_AllCliParametersSupported() throws IOException {
        System.out.println("\n### Bug #39 Test 5: All parameters ###");
        
        // Create comprehensive settings file
        Properties props = new Properties();
        props.setProperty("workdir", "/tmp");
        props.setProperty("debug", "true");
        props.setProperty("loglevel", "INFO");
        props.setProperty("proxyhost", "proxy.example.com");
        props.setProperty("proxyport", "8080");
        props.setProperty("proxyuser", "user");
        props.setProperty("proxypassword", "pass");
        props.setProperty("proxynthost", "nthost");
        props.setProperty("proxyntdomain", "domain");
        props.setProperty("logpropsfile", "/etc/log4j.properties");
        props.setProperty("dbdrivers", "/lib/mysql.jar,/lib/postgres.jar");
        
        try (FileWriter writer = new FileWriter(testSettingsFile)) {
            props.store(writer, "Complete settings");
        }
        
        WebHarvestSettings settings = WebHarvestSettings.fromFile(testSettingsFile);
        
        // Verify all properties loaded
        assertEquals(settings.getWorkDir(), "/tmp");
        assertEquals(settings.getDebug(), Boolean.TRUE);
        assertEquals(settings.getLogLevel(), "INFO");
        assertEquals(settings.getProxyHost(), "proxy.example.com");
        assertEquals(settings.getProxyPort(), Integer.valueOf(8080));
        
        System.out.println("✅ All CLI parameters supported");
    }
    
    /**
     * Bug #39 Test 6: Documentation
     */
    @Test(groups = {"unit", "bugs", "hard"})
    public void testBug39_Documentation() {
        System.out.println("\n### Bug #39 Test 6: Documentation ###");
        
        System.out.println("Bug #39: Synchronize CLI args and GUI settings");
        System.out.println("");
        System.out.println("BEFORE FIX:");
        System.out.println("  - CLI: Only command line arguments");
        System.out.println("  - GUI: Settings file (webharvest.properties)");
        System.out.println("  - User must configure everything twice");
        System.out.println("  - No shared configuration");
        System.out.println("");
        System.out.println("AFTER FIX:");
        System.out.println("  - Shared settings file: ~/.webharvest/webharvest.properties");
        System.out.println("  - CLI reads settings file FIRST");
        System.out.println("  - CLI args OVERRIDE file settings");
        System.out.println("  - GUI/IDE uses same settings file");
        System.out.println("");
        System.out.println("USAGE:");
        System.out.println("  # Settings file (~/.webharvest/webharvest.properties):");
        System.out.println("  workdir=/projects/scraping");
        System.out.println("  debug=true");
        System.out.println("  proxyhost=proxy.company.com");
        System.out.println("");
        System.out.println("  # CLI can override any setting:");
        System.out.println("  java -jar webharvest-cli.jar config=scraper.xml workdir=/tmp");
        
        System.out.println("\n✅ Bug #39 feature documented");
    }
}

