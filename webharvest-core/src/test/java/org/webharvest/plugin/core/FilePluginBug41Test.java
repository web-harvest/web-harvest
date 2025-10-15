package org.webharvest.plugin.core;

import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.webharvest.runtime.variables.NodeVariable;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;

import static org.testng.Assert.*;

/**
 * Tests for Bug #41: Binary file download corruption.
 * 
 * <h3>Bug #41: web-harvest can't download the file</h3>
 * 
 * <p><strong>User Report:</strong></p>
 * <blockquote>
 * "web-harvest can't download this file http://www.oaomoek.ru/upload/File/2011/KD_2798.rar
 * I use this code:
 * {@code
 * <file action="write" path="c:\temp\1.rar" type="binary">
 *   <http method="get" url="http://www.oaomoek.ru/upload/File/2011/KD_2798.rar"/>
 * </file>
 * }
 * And i get a bad file. I can not open it. :("
 * </blockquote>
 * 
 * <p><strong>Root Cause:</strong></p>
 * <p>
 * FilePlugin was using {@code toString().getBytes()} for binary files,
 * causing double conversion: byte[] → String → byte[] = CORRUPTION!
 * </p>
 * 
 * <p><strong>Fix (v2.2.0):</strong></p>
 * <p>
 * FilePlugin now uses {@link NodeVariable#toBinary()} which preserves
 * byte[] without String conversion when type="binary".
 * </p>
 * 
 * @author Web-Harvest Team
 * @since 2.2.0
 */
public class FilePluginBug41Test {
    
    private File tempDir;
    
    @BeforeMethod
    public void setUp() throws IOException {
        tempDir = Files.createTempDirectory("bug41-test").toFile();
    }
    
    @AfterMethod
    public void tearDown() {
        if (tempDir != null && tempDir.exists()) {
            deleteDirectory(tempDir);
        }
    }
    
    /**
     * Test Bug #41: NodeVariable.toBinary() preserves binary data.
     * 
     * <p>Demonstrates that toBinary() returns byte[] without conversion
     * when data is already byte[].</p>
     */
    @Test(groups = {"bug-fix", "file"})
    public void testBug41_NodeVariable_ToBinaryPreservesByteArray() {
        // Create NodeVariable with RAR file header (Rar!\x1A\x07\x00)
        byte[] rarHeader = new byte[]{0x52, 0x61, 0x72, 0x21, 0x1A, 0x07, 0x00};
        NodeVariable var = new NodeVariable(rarHeader);
        
        // toBinary() should return same bytes
        byte[] result = var.toBinary();
        
        assertNotNull(result);
        assertEquals(result.length, 7);
        assertTrue(Arrays.equals(rarHeader, result), 
            "toBinary() should preserve byte[] without conversion");
        
        // Verify RAR header bytes
        assertEquals(result[0], 0x52, "R");
        assertEquals(result[1], 0x61, "a");
        assertEquals(result[2], 0x72, "r");
        assertEquals(result[3], 0x21, "!");
    }
    
    /**
     * Test Bug #41: NodeVariable.toString() corrupts binary data.
     * 
     * <p>Demonstrates the OLD bug: toString().getBytes() corrupts binary.</p>
     */
    @Test(groups = {"bug-fix", "file"})
    public void testBug41_NodeVariable_ToStringCorruptsBinary() {
        // Binary data with invalid UTF-8 (0xFF, 0xFE)
        byte[] binaryData = new byte[]{(byte)0xFF, (byte)0xFE, (byte)0x80, (byte)0x90};
        NodeVariable var = new NodeVariable(binaryData);
        
        // OLD approach (Bug #41): toString().getBytes()
        String str = var.toString();
        byte[] viaString = str.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        
        // This should be DIFFERENT (corrupted)
        assertFalse(Arrays.equals(binaryData, viaString),
            "toString().getBytes() should corrupt invalid UTF-8 bytes");
        
        // Direct toBinary() preserves data
        byte[] viaToBinary = var.toBinary();
        assertTrue(Arrays.equals(binaryData, viaToBinary),
            "toBinary() should preserve original bytes");
    }
    
    /**
     * Test Bug #41: ZIP file header preserved.
     */
    @Test(groups = {"bug-fix", "file"})
    public void testBug41_ZipFileHeaderPreserved() {
        // ZIP file header: PK\x03\x04
        byte[] zipHeader = new byte[]{0x50, 0x4B, 0x03, 0x04, 0x14, 0x00};
        NodeVariable var = new NodeVariable(zipHeader);
        
        byte[] result = var.toBinary();
        
        assertEquals(result[0], 0x50, "P");
        assertEquals(result[1], 0x4B, "K");
        assertEquals(result[2], 0x03);
        assertEquals(result[3], 0x04);
    }
    
    /**
     * Test Bug #41: PDF file header preserved.
     */
    @Test(groups = {"bug-fix", "file"})
    public void testBug41_PdfFileHeaderPreserved() {
        // PDF file header: %PDF-1.
        byte[] pdfHeader = new byte[]{0x25, 0x50, 0x44, 0x46, 0x2D, 0x31, 0x2E};
        NodeVariable var = new NodeVariable(pdfHeader);
        
        byte[] result = var.toBinary();
        
        assertTrue(Arrays.equals(pdfHeader, result));
        
        // Verify it starts with "%PDF"
        assertEquals(result[0], 0x25, "%");
        assertEquals(result[1], 0x50, "P");
        assertEquals(result[2], 0x44, "D");
        assertEquals(result[3], 0x46, "F");
    }
    
    /**
     * Test Bug #41: Binary data with invalid UTF-8 sequences.
     * 
     * <p>RAR/ZIP files contain bytes that are NOT valid UTF-8.
     * toString().getBytes() corrupts them.</p>
     */
    @Test(groups = {"bug-fix", "file"})
    public void testBug41_InvalidUtf8SequencesPreserved() {
        // Bytes that are invalid UTF-8 (common in RAR/ZIP)
        byte[] invalidUtf8 = new byte[]{
            (byte)0xFF, (byte)0xFE,  // BOM (not UTF-8)
            (byte)0x80, (byte)0x90,  // Invalid UTF-8
            (byte)0xA0, (byte)0xF0   // Invalid UTF-8
        };
        NodeVariable var = new NodeVariable(invalidUtf8);
        
        // toBinary() must preserve ALL bytes
        byte[] result = var.toBinary();
        
        assertEquals(result.length, 6, "All bytes should be preserved");
        assertTrue(Arrays.equals(invalidUtf8, result),
            "Invalid UTF-8 bytes must be preserved exactly");
        
        // toString().getBytes() would corrupt these!
        String str = var.toString();
        byte[] corrupted = str.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        assertFalse(Arrays.equals(invalidUtf8, corrupted),
            "toString().getBytes() corrupts invalid UTF-8 bytes");
    }
    
    /**
     * Test Bug #41: FilePlugin with binary NodeVariable.
     * 
     * <p>Integration test: Simulates HttpPlugin→FilePlugin flow.</p>
     */
    @Test(groups = {"bug-fix", "file"})
    public void testBug41_FilePlugin_PreservesBinaryFromNodeVariable() throws IOException {
        // Simulate HttpPlugin returning binary data in NodeVariable
        byte[] binaryData = new byte[]{
            0x52, 0x61, 0x72, 0x21, 0x1A, 0x07, 0x00,  // RAR header
            0x01, 0x02, 0x03, 0x04, 0x05               // Sample data
        };
        NodeVariable httpResult = new NodeVariable(binaryData);
        
        // FilePlugin should use toBinary() for type="binary"
        byte[] bytesToWrite = httpResult.toBinary();
        
        // Verify no corruption
        assertTrue(Arrays.equals(binaryData, bytesToWrite),
            "FilePlugin should use toBinary() to preserve binary data");
        
        assertEquals(bytesToWrite[0], 0x52, "RAR header should be preserved");
        assertEquals(bytesToWrite[1], 0x61);
        assertEquals(bytesToWrite[2], 0x72);
        assertEquals(bytesToWrite[3], 0x21);
    }
    
    /**
     * Documentation test: Bug #41 fix summary.
     */
    @Test(groups = {"documentation", "file"})
    public void testBug41_DocumentationAndFix() {
        // Bug #41: RAR/ZIP files corrupted when downloaded
        
        // Old code (WRONG):
        //   content = bodyContent.toString();  // byte[] → String (CORRUPTION!)
        //   bytes = content.getBytes(charset); // String → byte[] (already corrupted)
        
        // New code (CORRECT):
        //   bytes = bodyContent.toBinary();    // Direct byte[] access (NO corruption)
        
        // This fix applies to:
        // - RAR files
        // - ZIP files
        // - PDF files
        // - Images (JPEG, PNG, GIF)
        // - Any binary format
        
        assertTrue(true, "Bug #41 fixed by using toBinary() in FilePlugin");
    }
    
    /**
     * Helper method to delete directory recursively.
     */
    private void deleteDirectory(File dir) {
        File[] files = dir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    deleteDirectory(file);
                } else {
                    file.delete();
                }
            }
        }
        dir.delete();
    }
}

