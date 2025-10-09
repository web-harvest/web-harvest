package org.webharvest.definition;

import org.testng.annotations.Test;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import static org.testng.Assert.*;

/**
 * Test for Bug #47: Character set problem in CLI mode
 * 
 * Issue: FileConfigSource used FileReader which relies on platform default charset
 * (e.g., WIN1250 on Windows instead of UTF-8), causing encoding problems.
 * 
 * Fix: Changed to InputStreamReader with explicit UTF-8 charset.
 */
public class FileConfigSourceBug47Test {

    /**
     * Test Bug #47: Config files should be read as UTF-8, not platform default
     */
    @Test(groups = {"unit", "bugs", "hard"})
    public void testConfigFileReadAsUtf8_NotPlatformDefault() throws Exception {
        // Create test file with UTF-8 characters
        File testFile = new File("target/test-output/bug47_utf8_config.xml");
        testFile.getParentFile().mkdirs();
        
        String xmlContent = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<config xmlns=\"http://org.webharvest/schema/2.1/core\">\n" +
                "  <!-- UTF-8 characters: Polish ąćęłńóśźż, Chinese 中文, Russian Русский -->\n" +
                "  <def var=\"test\">UTF-8: ąćęłńóśźż 中文 Русский</def>\n" +
                "</config>";
        
        // Write as UTF-8
        Files.write(testFile.toPath(), xmlContent.getBytes(StandardCharsets.UTF_8));
        
        // Read using FileConfigSource (should use UTF-8, not platform default)
        FileConfigSource source = new FileConfigSource(testFile);
        Reader reader = source.getReader();
        
        // Read content
        StringBuilder content = new StringBuilder();
        char[] buffer = new char[1024];
        int charsRead;
        while ((charsRead = reader.read(buffer)) != -1) {
            content.append(buffer, 0, charsRead);
        }
        reader.close();
        
        String readContent = content.toString();
        
        System.out.println("Bug #47 - UTF-8 test:");
        System.out.println("  Original: " + xmlContent.substring(0, 100) + "...");
        System.out.println("  Read:     " + readContent.substring(0, 100) + "...");
        
        // Verify UTF-8 characters are preserved
        assertTrue(readContent.contains("ąćęłńóśźż"), "Polish chars should be preserved");
        assertTrue(readContent.contains("中文"), "Chinese chars should be preserved");
        assertTrue(readContent.contains("Русский"), "Russian chars should be preserved");
        
        // Verify no garbled characters (would happen if read with wrong charset)
        assertFalse(readContent.contains("Ä"), "Should not have garbled chars from wrong encoding");
        
        System.out.println("✅ Bug #47: Config file read as UTF-8");
        
        // Cleanup
        testFile.delete();
    }

    /**
     * HARD TEST: File written in WIN1250, read with UTF-8
     * This simulates the OLD bug behavior
     */
    @Test(groups = {"unit", "bugs", "hard", "edge-case"})
    public void testWin1250FileWithUtf8Reader_CharactersHandled() throws Exception {
        // This test documents what happens when file is actually WIN1250
        // but we read it as UTF-8 (which is better than platform-dependent behavior)
        
        File testFile = new File("target/test-output/bug47_win1250.xml");
        testFile.getParentFile().mkdirs();
        
        String xmlContent = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<config xmlns=\"http://org.webharvest/schema/2.1/core\">\n" +
                "  <def var=\"test\">Test</def>\n" +
                "</config>";
        
        // Write as WIN1250 (simulate old Windows file)
        try (OutputStreamWriter writer = new OutputStreamWriter(
                new FileOutputStream(testFile), Charset.forName("windows-1250"))) {
            writer.write(xmlContent);
        }
        
        // Read using FileConfigSource (uses UTF-8)
        FileConfigSource source = new FileConfigSource(testFile);
        Reader reader = source.getReader();
        
        // Should read without exception (may have encoding issues for special chars)
        char[] buffer = new char[1024];
        int charsRead = reader.read(buffer);
        assertTrue(charsRead > 0, "Should read content");
        reader.close();
        
        System.out.println("✅ WIN1250 file handled (may have encoding issues for special chars)");
        
        // Cleanup
        testFile.delete();
    }

    /**
     * REGRESSION: ASCII-only config files should work perfectly
     */
    @Test(groups = {"unit", "bugs", "regression"})
    public void testAsciiConfigFile_NoRegression() throws Exception {
        File testFile = new File("target/test-output/bug47_ascii_config.xml");
        testFile.getParentFile().mkdirs();
        
        String xmlContent = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<config xmlns=\"http://org.webharvest/schema/2.1/core\">\n" +
                "  <def var=\"test\">ASCII only content 123 ABC</def>\n" +
                "</config>";
        
        // Write as UTF-8
        Files.write(testFile.toPath(), xmlContent.getBytes(StandardCharsets.UTF_8));
        
        // Read
        FileConfigSource source = new FileConfigSource(testFile);
        Reader reader = source.getReader();
        
        StringBuilder content = new StringBuilder();
        char[] buffer = new char[1024];
        int charsRead;
        while ((charsRead = reader.read(buffer)) != -1) {
            content.append(buffer, 0, charsRead);
        }
        reader.close();
        
        // ASCII should be identical
        assertEquals(content.toString(), xmlContent);
        
        System.out.println("✅ ASCII config files work perfectly");
        
        // Cleanup
        testFile.delete();
    }

    /**
     * REGRESSION: Empty files should not cause errors
     */
    @Test(groups = {"unit", "bugs", "regression", "edge-case"})
    public void testEmptyConfigFile_NoException() throws Exception {
        File testFile = new File("target/test-output/bug47_empty.xml");
        testFile.getParentFile().mkdirs();
        testFile.createNewFile();
        
        FileConfigSource source = new FileConfigSource(testFile);
        Reader reader = source.getReader();
        
        // Should not throw exception
        char[] buffer = new char[1024];
        int charsRead = reader.read(buffer);
        
        assertEquals(charsRead, -1, "Empty file should return -1");
        reader.close();
        
        System.out.println("✅ Empty file handled gracefully");
        
        // Cleanup
        testFile.delete();
    }
}

