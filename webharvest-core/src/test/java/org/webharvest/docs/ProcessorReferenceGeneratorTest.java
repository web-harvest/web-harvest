/*
 * Copyright (c) 2006-2025 the original author or authors.
 *
 * Redistribution and use of this software in source and binary forms,
 * with or without modification, are permitted provided that the following
 * conditions are met:
 *
 * * Redistributions of source code must retain the above
 *   copyright notice, this list of conditions and the
 *   following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above
 *   copyright notice, this list of conditions and the
 *   following disclaimer in the documentation and/or other
 *   materials provided with the distribution.
 *
 * * The name of Web-Harvest may not be used to endorse or promote
 *   products derived from this software without specific prior
 *   written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

package org.webharvest.docs;

import static org.testng.Assert.*;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;

/**
 * Unit tests for ProcessorReferenceGenerator.
 * 
 * @author Robert Bala
 * @since 2.2.0-SNAPSHOT
 */
public class ProcessorReferenceGeneratorTest {
    
    private ProcessorReferenceGenerator generator;
    private File tempDir;
    
    @BeforeMethod
    public void setUp() throws IOException {
        generator = new ProcessorReferenceGenerator();
        tempDir = File.createTempFile("processor-ref-test", "");
        tempDir.delete();
        tempDir.mkdirs();
    }
    
    @Test
    public void testGenerateProcessorReference() throws IOException {
        generator.generateProcessorReference(tempDir.getAbsolutePath());
        
        // Check that files were created
        assertTrue(new File(tempDir, "processors.json").exists(), "JSON reference should be created");
        assertTrue(new File(tempDir, "reference.html").exists(), "HTML reference should be created");
        assertTrue(new File(tempDir, "individual").exists(), "Individual pages directory should be created");
        assertTrue(new File(tempDir, "individual/index.html").exists(), "Individual pages index should be created");
    }
    
    @Test
    public void testGenerateProcessorReferenceWithNonExistentDirectory() throws IOException {
        File nonExistentDir = new File(tempDir, "non-existent");
        generator.generateProcessorReference(nonExistentDir.getAbsolutePath());
        
        assertTrue(nonExistentDir.exists(), "Directory should be created");
        assertTrue(new File(nonExistentDir, "processors.json").exists(), "JSON reference should be created");
    }
    
    @Test
    public void testMainMethodWithNoArguments() throws NoSuchMethodException {
        // Test that main method handles no arguments correctly
        // This will call System.exit(1), so we can't easily test it
        // But we can verify the method exists and is static
        assertNotNull(ProcessorReferenceGenerator.class.getMethod("main", String[].class));
    }
    
    @Test
    public void testMainMethodWithArguments() throws IOException {
        // Test main method with valid arguments
        String[] args = {tempDir.getAbsolutePath()};
        
        // We can't easily test main method due to System.exit, but we can test the underlying method
        generator.generateProcessorReference(args[0]);
        
        assertTrue(new File(tempDir, "processors.json").exists(), "JSON reference should be created");
    }
    
    @Test
    public void testGenerateJsonReference() throws IOException {
        generator.generateProcessorReference(tempDir.getAbsolutePath());
        
        File jsonFile = new File(tempDir, "processors.json");
        assertTrue(jsonFile.exists(), "JSON file should exist");
        assertTrue(jsonFile.length() > 0, "JSON file should not be empty");
    }
    
    @Test
    public void testGenerateHtmlReference() throws IOException {
        generator.generateProcessorReference(tempDir.getAbsolutePath());
        
        File htmlFile = new File(tempDir, "reference.html");
        assertTrue(htmlFile.exists(), "HTML file should exist");
        assertTrue(htmlFile.length() > 0, "HTML file should not be empty");
    }
    
    @Test
    public void testGenerateIndividualPages() throws IOException {
        generator.generateProcessorReference(tempDir.getAbsolutePath());
        
        File individualDir = new File(tempDir, "individual");
        assertTrue(individualDir.exists(), "Individual directory should exist");
        assertTrue(individualDir.isDirectory(), "Individual should be a directory");
        
        File indexFile = new File(individualDir, "index.html");
        assertTrue(indexFile.exists(), "Index file should exist");
        assertTrue(indexFile.length() > 0, "Index file should not be empty");
    }
}
