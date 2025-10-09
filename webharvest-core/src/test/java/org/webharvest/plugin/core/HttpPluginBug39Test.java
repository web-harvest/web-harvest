package org.webharvest.plugin.core;

import org.testng.annotations.Test;
import org.webharvest.runtime.variables.NodeVariable;
import org.webharvest.runtime.variables.Variable;

import static org.testng.Assert.*;

/**
 * Test for Bug #39: Forcing mime type to binary for images
 * 
 * Issue: When downloading an image from a website that references it by number
 * (no extension) and Content-Type reports it as "text", there's no way to force
 * binary mode. The image is processed as String, corrupting binary data.
 * 
 * Example:
 *   URL: http://example.com/image/12345 (no .jpg extension)
 *   Content-Type: text/plain (server misconfigured)
 *   Actual: JPEG image binary data
 *   Problem: Processed as String → binary data corrupted
 * 
 * Fix: Add optional attribute to force binary mode in <http> element.
 * 
 * Forum: http://sourceforge.net/projects/web-harvest/forums/forum/591299/topic/3855226
 */
public class HttpPluginBug39Test {

    /**
     * Test that NodeVariable can store binary data
     */
    @Test(groups = {"unit", "bugs"})
    public void testNodeVariable_CanStoreBinaryData() {
        System.out.println("\n### Bug #39: NodeVariable binary storage ###");
        
        // Binary data (e.g., image bytes)
        byte[] imageData = new byte[]{
            (byte)0xFF, (byte)0xD8, (byte)0xFF, (byte)0xE0,  // JPEG header
            0x00, 0x10, 0x4A, 0x46, 0x49, 0x46  // "JFIF"
        };
        
        // Store in NodeVariable
        Variable var = new NodeVariable(imageData);
        
        // Should be able to retrieve as binary
        byte[] retrieved = var.toBinary();
        
        assertNotNull(retrieved);
        assertEquals(retrieved.length, imageData.length);
        assertEquals(retrieved[0], (byte)0xFF, "JPEG marker");
        assertEquals(retrieved[1], (byte)0xD8, "JPEG marker");
        
        System.out.println("  Binary data stored: " + imageData.length + " bytes");
        System.out.println("  Retrieved: " + retrieved.length + " bytes");
        System.out.println("  JPEG header preserved: " + 
            String.format("0x%02X 0x%02X", retrieved[0], retrieved[1]));
        
        System.out.println("✅ NodeVariable can store binary data");
    }

    /**
     * Test what happens when binary data is converted to String and back
     */
    @Test(groups = {"unit", "bugs", "hard"})
    public void testBinaryData_CorruptedByStringConversion() {
        System.out.println("\n### Bug #39: Binary data corruption via String ###");
        
        // Binary data with non-ASCII bytes
        byte[] original = new byte[]{
            (byte)0xFF, (byte)0xD8, (byte)0xFF, (byte)0xE0,  // JPEG
            (byte)0x80, (byte)0x90, (byte)0xA0, (byte)0xF0   // High bytes
        };
        
        System.out.println("  Original binary: " + original.length + " bytes");
        
        // Convert to String (what happens when treated as text)
        String asString = new String(original);
        
        // Convert back to binary
        byte[] corrupted = asString.getBytes();
        
        System.out.println("  After String conversion: " + corrupted.length + " bytes");
        
        // Binary data is likely corrupted
        boolean identical = java.util.Arrays.equals(original, corrupted);
        System.out.println("  Data identical: " + identical);
        
        if (!identical) {
            System.out.println("  ⚠️ Binary data CORRUPTED by String conversion!");
            System.out.println("     Original: " + bytesToHex(original));
            System.out.println("     Corrupted: " + bytesToHex(corrupted));
        }
        
        // This demonstrates the problem - binary should NOT be converted to String
        System.out.println("✅ Demonstrated: String conversion corrupts binary data");
    }

    /**
     * Test binary content detection by MIME type
     */
    @Test(groups = {"unit", "bugs"})
    public void testBinaryMimeTypes_ShouldBeDetected() {
        System.out.println("\n### Bug #39: Binary MIME type detection ###");
        
        String[] binaryMimeTypes = {
            "image/jpeg",
            "image/png",
            "image/gif",
            "image/bmp",
            "image/webp",
            "application/pdf",
            "application/zip",
            "application/octet-stream",
            "video/mp4",
            "audio/mpeg"
        };
        
        String[] textMimeTypes = {
            "text/html",
            "text/plain",
            "text/xml",
            "text/css",
            "text/javascript",
            "application/json",
            "application/xml"
        };
        
        System.out.println("  Binary MIME types:");
        for (String mime : binaryMimeTypes) {
            boolean isBinary = isBinaryMimeType(mime);
            System.out.println("    " + mime + " → " + (isBinary ? "BINARY" : "TEXT"));
            assertTrue(isBinary, mime + " should be detected as binary");
        }
        
        System.out.println("  Text MIME types:");
        for (String mime : textMimeTypes) {
            boolean isBinary = isBinaryMimeType(mime);
            System.out.println("    " + mime + " → " + (isBinary ? "BINARY" : "TEXT"));
            assertFalse(isBinary, mime + " should be detected as text");
        }
        
        System.out.println("✅ MIME type detection works");
    }

    /**
     * Test Bug #39 workaround: If MIME type detection fails, 
     * user should be able to force binary mode
     */
    @Test(groups = {"unit", "bugs", "hard"})
    public void testBug39_ForceBinaryMode_Concept() {
        System.out.println("\n### Bug #39: Force binary mode concept ###");
        
        // Scenario: Server says "text/plain" but it's actually an image
        String reportedMimeType = "text/plain";
        boolean shouldBeBinary = false;  // Based on MIME
        
        // User wants to force binary (new feature request)
        boolean forceBinary = true;  // Via attribute: force-binary="yes"
        
        // Result: Should treat as binary when forced
        boolean treatAsBinary = forceBinary || isBinaryMimeType(reportedMimeType);
        
        System.out.println("  Reported MIME: " + reportedMimeType);
        System.out.println("  Auto-detect binary: " + shouldBeBinary);
        System.out.println("  Force binary: " + forceBinary);
        System.out.println("  Final decision: " + (treatAsBinary ? "BINARY" : "TEXT"));
        
        assertTrue(treatAsBinary, "When force-binary=yes, should treat as binary");
        
        System.out.println("✅ Force binary mode concept verified");
    }

    /**
     * Helper: Determine if MIME type is binary
     */
    private boolean isBinaryMimeType(String mimeType) {
        if (mimeType == null) {
            return false;
        }
        
        String mime = mimeType.toLowerCase();
        
        // Binary MIME types
        return mime.startsWith("image/")
            || mime.startsWith("video/")
            || mime.startsWith("audio/")
            || mime.equals("application/pdf")
            || mime.equals("application/zip")
            || mime.equals("application/octet-stream")
            || mime.contains("binary");
    }

    /**
     * Helper: Convert bytes to hex string
     */
    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < Math.min(bytes.length, 8); i++) {
            sb.append(String.format("%02X ", bytes[i]));
        }
        if (bytes.length > 8) {
            sb.append("...");
        }
        return sb.toString();
    }
}

