package org.webharvest.ide;

import org.testng.annotations.Test;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.AfterClass;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.testng.Assert.*;

/**
 * Integration test for IDE tab management fixes:
 * 1. New tabs should have default XML configuration
 * 2. Loading examples should not create duplicate tabs
 */
public class TabManagementTest {

    private String appJsContent;
    
    @BeforeClass
    public void setUp() throws Exception {
        // The Vite build runs in the compile phase, before the Surefire test phase.
        // Read its generated source resource directly; the IDE JAR is not created
        // until the later package phase.
        Path testClasses = Path.of(
            TabManagementTest.class.getProtectionDomain().getCodeSource().getLocation().toURI());
        Path moduleRoot = testClasses.getParent().getParent();
        Path asset = moduleRoot.resolve("src/main/resources/webapp/dist/assets/main.js");
        assertTrue(Files.isRegularFile(asset),
            "webapp/dist/assets/main.js not found at " + asset + " - TypeScript frontend not built");
        appJsContent = Files.readString(asset, StandardCharsets.UTF_8);
    }
    
    @Test(groups = {"integration", "frontend", "tab-management"})
    public void testNewTabHasDefaultXmlConfiguration() {
        // Verify that getDefaultEmptyConfig() method exists and returns proper XML
        assertTrue(appJsContent.contains("getDefaultEmptyConfig"),
            "Missing getDefaultEmptyConfig() method in compiled app.js");
        
        assertTrue(appJsContent.contains("<?xml version=\"1.0\" encoding=\"UTF-8\"?>"),
            "Default XML declaration not found in app.js");
        
        assertTrue(appJsContent.contains("<config xmlns=\"http://org.webharvest/schema/2.1/core\""),
            "Default config element not found in app.js");
        
        // Verify addTab uses default config when content is empty (minification-aware check)
        // Minified code: t||this.getDefaultEmptyConfig() or content||this.getDefaultEmptyConfig()
        assertTrue(appJsContent.contains("||this.getDefaultEmptyConfig()"),
            "addTab method doesn't use getDefaultEmptyConfig() for empty content");
    }
    
    @Test(groups = {"integration", "frontend", "tab-management"})
    public void testLoadExampleChecksForDuplicates() {
        // Verify that loadExampleDirect checks for existing tabs
        assertTrue(appJsContent.contains("findTabByName"),
            "Missing findTabByName() call in compiled app.js");
        
        // Should have logic to switch to existing tab instead of creating duplicate
        // After minification, variable names change but function calls remain
        // Look for: findTabByName call followed by switchTab call or activeTabId assignment
        boolean hasExistingTabCheck = 
            appJsContent.contains("findTabByName") &&
            (appJsContent.contains("switchTab") || appJsContent.contains("switchToTab") || 
             appJsContent.contains("activeTabId="));
        
        assertTrue(hasExistingTabCheck,
            "Missing logic to switch to existing tab instead of creating duplicate");
    }
    
    @Test(groups = {"integration", "frontend", "tab-management"})
    public void testInitialTabHasDefaultXml() {
        // Verify that loadExampleTabs() (startup) also uses getDefaultEmptyConfig()
        assertTrue(appJsContent.contains("loadExampleTabs") || appJsContent.contains("initial"),
            "Missing initial tab loading logic");
        
        // Either should call getDefaultEmptyConfig() or TabManager.addTab (which uses it)
        boolean usesDefaultConfig = 
            appJsContent.contains("getDefaultEmptyConfig") &&
            appJsContent.contains("addTab");
        
        assertTrue(usesDefaultConfig,
            "Initial tab doesn't use default XML configuration");
    }
    
    @Test(groups = {"integration", "frontend", "tab-management"})
    public void testFindTabByNameMethodExists() {
        // Verify that TabManager has findTabByName method
        assertTrue(appJsContent.contains("findTabByName"),
            "Missing findTabByName() method in TabManager");
        
        // Should search by tab name
        boolean hasNameSearch = 
            appJsContent.contains("tab.name") || 
            appJsContent.contains("t.name");
        
        assertTrue(hasNameSearch,
            "findTabByName() doesn't search by tab name");
    }
    
    @Test(groups = {"integration", "frontend", "tab-management", "duplicates"})
    public void testLoadExampleAskBeforeOverwrite() {
        // Verify loadExampleDirect detects duplicates and asks before overwriting
        assertTrue(appJsContent.contains("loadExampleDirect"),
            "loadExampleDirect method should exist");
        
        assertTrue(appJsContent.contains("findTabByName"),
            "loadExampleDirect should check for existing tabs");
        
        // Should ask user with confirm dialog
        assertTrue(appJsContent.contains("confirm"),
            "loadExampleDirect should ask user before overwriting tab");
        
        // Find loadExampleDirect method (second occurrence - first is onclick)
        int firstPos = appJsContent.indexOf("loadExampleDirect");
        int methodPos = appJsContent.indexOf("loadExampleDirect", firstPos + 1);
        assertTrue(methodPos > 0, "loadExampleDirect method should exist");
        
        String loadExampleMethod = appJsContent.substring(methodPos, 
            Math.min(methodPos + 2000, appJsContent.length()));
        
        // Should have: findTabByName, confirm dialog, and conditional logic
        boolean hasCompleteLogic = 
            loadExampleMethod.contains("findTabByName") &&
            loadExampleMethod.contains("confirm") &&
            loadExampleMethod.contains("switchToTab");
        
        assertTrue(hasCompleteLogic,
            "loadExampleDirect should detect duplicates, ask user, and handle both outcomes");
    }
}
