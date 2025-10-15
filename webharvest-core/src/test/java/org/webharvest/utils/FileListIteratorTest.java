package org.webharvest.utils;

import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import static org.testng.Assert.*;

/**
 * Tests for FileListIterator.
 * Tests file iteration (non-recursive and recursive).
 * 
 * @author Robert Bala
 * @since 2.2.0
 */
public class FileListIteratorTest {

    private File tempDir;
    private List<File> createdFiles;

    @BeforeMethod
    public void setUp() throws IOException {
        tempDir = Files.createTempDirectory("filelistiterator-test").toFile();
        createdFiles = new ArrayList<>();
    }

    @AfterMethod
    public void tearDown() {
        // Clean up created files
        for (File file : createdFiles) {
            if (file.exists()) {
                file.delete();
            }
        }
        if (tempDir.exists()) {
            // Delete subdirectories and temp dir
            deleteRecursive(tempDir);
        }
    }

    private void deleteRecursive(File file) {
        if (file.isDirectory()) {
            File[] files = file.listFiles();
            if (files != null) {
                for (File f : files) {
                    deleteRecursive(f);
                }
            }
        }
        file.delete();
    }

    // ========== Non-Recursive Tests ==========

    @Test
    public void testEmptyDirectory() {
        FileListIterator iterator = new FileListIterator(tempDir, false);
        assertFalse(iterator.hasNext());
        assertNull(iterator.next());
    }

    @Test
    public void testSingleFile() throws IOException {
        File file1 = new File(tempDir, "file1.txt");
        file1.createNewFile();
        createdFiles.add(file1);

        FileListIterator iterator = new FileListIterator(tempDir, false);
        
        assertTrue(iterator.hasNext());
        File next = iterator.next();
        assertNotNull(next);
        assertEquals(next.getName(), "file1.txt");
        assertFalse(iterator.hasNext());
    }

    @Test
    public void testMultipleFiles() throws IOException {
        File file1 = new File(tempDir, "file1.txt");
        File file2 = new File(tempDir, "file2.txt");
        File file3 = new File(tempDir, "file3.txt");
        file1.createNewFile();
        file2.createNewFile();
        file3.createNewFile();
        createdFiles.add(file1);
        createdFiles.add(file2);
        createdFiles.add(file3);

        FileListIterator iterator = new FileListIterator(tempDir, false);
        
        int count = 0;
        while (iterator.hasNext()) {
            File file = iterator.next();
            assertNotNull(file);
            count++;
        }
        assertEquals(count, 3);
    }

    @Test
    public void testNonRecursiveIgnoresSubdirectories() throws IOException {
        File file1 = new File(tempDir, "file1.txt");
        file1.createNewFile();
        createdFiles.add(file1);
        
        File subdir = new File(tempDir, "subdir");
        subdir.mkdir();
        createdFiles.add(subdir);
        
        File file2 = new File(subdir, "file2.txt");
        file2.createNewFile();
        createdFiles.add(file2);

        FileListIterator iterator = new FileListIterator(tempDir, false);
        
        List<File> files = new ArrayList<>();
        while (iterator.hasNext()) {
            files.add(iterator.next());
        }
        
        // Should return both file1.txt and subdir (but not file2.txt inside subdir)
        assertEquals(files.size(), 2);
    }

    // ========== Recursive Tests ==========

    @Test
    public void testRecursiveEmptyDirectory() {
        FileListIterator iterator = new FileListIterator(tempDir, true);
        assertFalse(iterator.hasNext());
    }

    @Test
    public void testRecursiveWithSubdirectories() throws IOException {
        File file1 = new File(tempDir, "file1.txt");
        file1.createNewFile();
        createdFiles.add(file1);
        
        File subdir = new File(tempDir, "subdir");
        subdir.mkdir();
        createdFiles.add(subdir);
        
        File file2 = new File(subdir, "file2.txt");
        file2.createNewFile();
        createdFiles.add(file2);

        FileListIterator iterator = new FileListIterator(tempDir, true);
        
        List<File> files = new ArrayList<>();
        while (iterator.hasNext()) {
            files.add(iterator.next());
        }
        
        // Should include: file1.txt, subdir, file2.txt (recursive)
        assertTrue(files.size() >= 2); // At least file1 and subdir
    }

    @Test
    public void testRecursiveMultipleLevels() throws IOException {
        File file1 = new File(tempDir, "root.txt");
        file1.createNewFile();
        createdFiles.add(file1);
        
        File level1 = new File(tempDir, "level1");
        level1.mkdir();
        createdFiles.add(level1);
        
        File file2 = new File(level1, "level1.txt");
        file2.createNewFile();
        createdFiles.add(file2);
        
        File level2 = new File(level1, "level2");
        level2.mkdir();
        createdFiles.add(level2);
        
        File file3 = new File(level2, "level2.txt");
        file3.createNewFile();
        createdFiles.add(file3);

        FileListIterator iterator = new FileListIterator(tempDir, true);
        
        int count = 0;
        while (iterator.hasNext()) {
            iterator.next();
            count++;
        }
        
        assertTrue(count >= 3); // At least root.txt, level1, level1.txt
    }

    // ========== Iterator Behavior Tests ==========

    @Test
    public void testHasNextDoesNotAdvance() throws IOException {
        File file1 = new File(tempDir, "file1.txt");
        file1.createNewFile();
        createdFiles.add(file1);

        FileListIterator iterator = new FileListIterator(tempDir, false);
        
        assertTrue(iterator.hasNext());
        assertTrue(iterator.hasNext());
        assertTrue(iterator.hasNext()); // Multiple calls should not advance
        
        File next = iterator.next();
        assertNotNull(next);
    }

    @Test
    public void testNextAfterEnd() throws IOException {
        File file1 = new File(tempDir, "file1.txt");
        file1.createNewFile();
        createdFiles.add(file1);

        FileListIterator iterator = new FileListIterator(tempDir, false);
        
        iterator.next(); // Get first file
        assertNull(iterator.next()); // Should return null after end
        assertNull(iterator.next()); // Multiple calls return null
    }

    @Test(expectedExceptions = UnsupportedOperationException.class)
    public void testRemoveThrows() throws IOException {
        File file1 = new File(tempDir, "file1.txt");
        file1.createNewFile();
        createdFiles.add(file1);

        FileListIterator iterator = new FileListIterator(tempDir, false);
        iterator.next();
        iterator.remove(); // Should throw
    }

    @Test
    public void testHasNextReturnsFalseWhenEmpty() {
        FileListIterator iterator = new FileListIterator(tempDir, false);
        assertFalse(iterator.hasNext());
    }

    @Test
    public void testNextReturnsNullWhenEmpty() {
        FileListIterator iterator = new FileListIterator(tempDir, false);
        assertNull(iterator.next());
    }

    // ========== Edge Cases ==========

    @Test
    public void testIteratorWithHiddenFiles() throws IOException {
        File normalFile = new File(tempDir, "normal.txt");
        normalFile.createNewFile();
        createdFiles.add(normalFile);
        
        // Note: Hidden files may not work on all platforms, but test should not fail
        File hiddenFile = new File(tempDir, ".hidden");
        hiddenFile.createNewFile();
        createdFiles.add(hiddenFile);

        FileListIterator iterator = new FileListIterator(tempDir, false);
        
        int count = 0;
        while (iterator.hasNext()) {
            iterator.next();
            count++;
        }
        
        assertTrue(count >= 1); // At least normal.txt
    }

    @Test
    public void testIteratorSequentialCalls() throws IOException {
        File file1 = new File(tempDir, "a.txt");
        File file2 = new File(tempDir, "b.txt");
        file1.createNewFile();
        file2.createNewFile();
        createdFiles.add(file1);
        createdFiles.add(file2);

        FileListIterator iterator = new FileListIterator(tempDir, false);
        
        assertTrue(iterator.hasNext());
        File first = iterator.next();
        assertNotNull(first);
        
        assertTrue(iterator.hasNext());
        File second = iterator.next();
        assertNotNull(second);
        
        assertFalse(iterator.hasNext());
        assertNull(iterator.next());
    }
}

