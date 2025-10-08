package org.webharvest.utils;

import org.testng.annotations.Test;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;

import static org.testng.Assert.*;

/**
 * Test for HasReader interface.
 */
public class HasReaderTest {

    /**
     * Simple implementation of HasReader for testing.
     */
    private static class TestHasReader implements HasReader {
        private final String content;
        
        public TestHasReader(String content) {
            this.content = content;
        }
        
        @Override
        public Reader getReader() throws IOException {
            return new StringReader(content);
        }
    }

    @Test
    public void testGetReader() throws IOException {
        String testContent = "Hello, World!";
        HasReader hasReader = new TestHasReader(testContent);
        
        Reader reader = hasReader.getReader();
        assertNotNull(reader);
        
        char[] buffer = new char[testContent.length()];
        int read = reader.read(buffer);
        assertEquals(read, testContent.length());
        assertEquals(new String(buffer), testContent);
        
        reader.close();
    }

    @Test
    public void testGetReaderWithEmptyContent() throws IOException {
        HasReader hasReader = new TestHasReader("");
        
        Reader reader = hasReader.getReader();
        assertNotNull(reader);
        
        int read = reader.read();
        assertEquals(read, -1); // EOF
        
        reader.close();
    }

    @Test(expectedExceptions = NullPointerException.class)
    public void testGetReaderWithNullContent() throws IOException {
        HasReader hasReader = new TestHasReader(null);
        
        Reader reader = hasReader.getReader();
        assertNotNull(reader);
        
        int read = reader.read();
        assertEquals(read, -1); // EOF
        
        reader.close();
    }

    @Test
    public void testMultipleReaders() throws IOException {
        String content1 = "First content";
        String content2 = "Second content";
        
        HasReader hasReader1 = new TestHasReader(content1);
        HasReader hasReader2 = new TestHasReader(content2);
        
        Reader reader1 = hasReader1.getReader();
        Reader reader2 = hasReader2.getReader();
        
        assertNotNull(reader1);
        assertNotNull(reader2);
        
        char[] buffer1 = new char[content1.length()];
        char[] buffer2 = new char[content2.length()];
        
        reader1.read(buffer1);
        reader2.read(buffer2);
        
        assertEquals(new String(buffer1), content1);
        assertEquals(new String(buffer2), content2);
        
        reader1.close();
        reader2.close();
    }

    @Test
    public void testReaderCanBeClosed() throws IOException {
        HasReader hasReader = new TestHasReader("Test content");
        Reader reader = hasReader.getReader();
        
        assertNotNull(reader);
        reader.close();
        
        // Should not throw exception when closed
        assertTrue(true);
    }
}
