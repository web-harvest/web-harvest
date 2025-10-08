package org.webharvest.utils;

import org.testng.annotations.Test;
import org.webharvest.plugin.annotations.CorePlugin;

import java.lang.annotation.Annotation;
import java.util.Set;

import static org.testng.Assert.*;

/**
 * Test for ClassPathScanner interface.
 */
public class ClassPathScannerTest {

    @Test
    public void testClassPathScannerImplImplementsInterface() {
        ClassPathScanner scanner = new ClassPathScannerImpl("org.webharvest.plugin.core");
        assertNotNull(scanner);
        assertTrue(scanner instanceof ClassPathScannerImpl);
    }

    @Test
    public void testGetTypesAnnotatedWithCorePlugin() {
        ClassPathScanner scanner = new ClassPathScannerImpl("org.webharvest.plugin.core");
        Set<Class<?>> types = scanner.getTypesAnnotatedWith(CorePlugin.class);
        assertNotNull(types);
        // Should find core plugins
        assertTrue(types.size() > 0);
        
        // Verify that found classes are actually annotated with CorePlugin
        for (Class<?> type : types) {
            assertTrue(type.isAnnotationPresent(CorePlugin.class));
        }
    }

    @Test
    public void testGetTypesAnnotatedWithAnnotation() {
        ClassPathScanner scanner = new ClassPathScannerImpl("org.webharvest.annotation");
        Set<Class<?>> types = scanner.getTypesAnnotatedWith(Annotation.class);
        assertNotNull(types);
        // Should find annotation classes
    }

    @Test
    public void testGetTypesAnnotatedWithNonExistentPackage() {
        ClassPathScanner scanner = new ClassPathScannerImpl("com.nonexistent.package");
        Set<Class<?>> types = scanner.getTypesAnnotatedWith(CorePlugin.class);
        assertNotNull(types);
        // Should be empty for non-existent package
        assertEquals(types.size(), 0);
    }
}
