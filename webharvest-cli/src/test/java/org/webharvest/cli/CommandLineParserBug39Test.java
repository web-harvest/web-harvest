/*
 * Copyright (c) 2006-2025 the original author or authors.
 */

package org.webharvest.cli;

import org.testng.annotations.Test;
import org.webharvest.settings.WebHarvestSettings;

import static org.testng.Assert.*;

/**
 * Test for Bug #39: Apache Commons CLI Integration
 * 
 * <h3>Bug #39: Modern CLI Argument Parsing</h3>
 * <p>
 * Tests the integration of Apache Commons CLI for better command line parsing
 * with support for short options (-h), long options (--help), and standard
 * argument formats.
 * </p>
 * 
 * @author Robert Bala
 * @since 2.2.1
 */
public class CommandLineParserBug39Test {
    
    /**
     * Bug #39 Test 1: Parse short options
     */
    @Test(groups = {"unit", "bugs", "hard"})
    public void testBug39_ParseShortOptions() {
        System.out.println("\n### Bug #39 Test 1: Short options ###");
        
        String[] args = {"-h"};
        CommandLineParser parser = new CommandLineParser();
        
        WebHarvestSettings settings = parser.parse(args);
        
        assertTrue(parser.isHelpRequested(), "Should recognize -h as help");
        
        System.out.println("✅ Short options parsed");
    }
    
    /**
     * Bug #39 Test 2: Parse long options
     */
    @Test(groups = {"unit", "bugs", "hard"})
    public void testBug39_ParseLongOptions() {
        System.out.println("\n### Bug #39 Test 2: Long options ###");
        
        String[] args = {"--help"};
        CommandLineParser parser = new CommandLineParser();
        
        WebHarvestSettings settings = parser.parse(args);
        
        assertTrue(parser.isHelpRequested(), "Should recognize --help");
        
        System.out.println("✅ Long options parsed");
    }
    
    /**
     * Bug #39 Test 3: Parse config file argument
     */
    @Test(groups = {"unit", "bugs", "hard"})
    public void testBug39_ParseConfigArgument() {
        System.out.println("\n### Bug #39 Test 3: Config argument ###");
        
        String[] args = {"-c", "myconfig.xml"};
        CommandLineParser parser = new CommandLineParser();
        
        WebHarvestSettings settings = parser.parse(args);
        
        assertEquals(parser.getConfigFile(), "myconfig.xml");
        
        System.out.println("✅ Config argument parsed");
    }
    
    /**
     * Bug #39 Test 4: Parse multiple arguments
     */
    @Test(groups = {"unit", "bugs", "hard"})
    public void testBug39_ParseMultipleArguments() {
        System.out.println("\n### Bug #39 Test 4: Multiple arguments ###");
        
        String[] args = {
            "-c", "config.xml",
            "-w", "/tmp/harvest",
            "-d", // debug flag
            "--loglevel", "DEBUG"
        };
        CommandLineParser parser = new CommandLineParser();
        
        WebHarvestSettings settings = parser.parse(args);
        
        assertEquals(parser.getConfigFile(), "config.xml");
        assertEquals(settings.getWorkDir(), "/tmp/harvest");
        assertEquals(settings.getDebug(), Boolean.TRUE);
        assertEquals(settings.getLogLevel(), "DEBUG");
        
        System.out.println("✅ Multiple arguments parsed");
    }
    
    /**
     * Bug #39 Test 5: Backward compatibility with old format
     */
    @Test(groups = {"unit", "bugs", "hard"})
    public void testBug39_BackwardCompatibility() {
        System.out.println("\n### Bug #39 Test 5: Backward compatibility ###");
        
        // Old format: config=file.xml workdir=/tmp
        String[] args = {"config=file.xml", "workdir=/tmp"};
        CommandLineParser parser = new CommandLineParser();
        
        WebHarvestSettings settings = parser.parse(args);
        
        assertEquals(parser.getConfigFile(), "file.xml");
        assertEquals(settings.getWorkDir(), "/tmp");
        
        System.out.println("✅ Backward compatible with old format");
    }
    
    /**
     * Bug #39 Test 6: Settings file + CLI args override
     */
    @Test(groups = {"unit", "bugs", "hard"})
    public void testBug39_SettingsFileWithCliOverride() {
        System.out.println("\n### Bug #39 Test 6: File + CLI override ###");
        
        // Simulate: settings file has workdir=/default
        // CLI provides: -w /override
        // Result: workdir should be /override
        
        WebHarvestSettings fileSettings = new WebHarvestSettings();
        fileSettings.setWorkDir("/default");
        fileSettings.setDebug(false);
        
        String[] args = {"-w", "/override", "-d"};
        CommandLineParser parser = new CommandLineParser(fileSettings);
        
        WebHarvestSettings finalSettings = parser.parse(args);
        
        // CLI should override
        assertEquals(finalSettings.getWorkDir(), "/override");
        assertEquals(finalSettings.getDebug(), Boolean.TRUE);
        
        System.out.println("✅ CLI args override file settings");
    }
}

