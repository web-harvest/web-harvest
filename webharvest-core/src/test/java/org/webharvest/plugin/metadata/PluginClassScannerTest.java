package org.webharvest.plugin.metadata;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.List;
import java.util.Set;

import static org.testng.Assert.*;

/**
 * Tests for PluginClassScanner dynamic plugin discovery.
 */
public class PluginClassScannerTest {

    @BeforeMethod
    public void setUp() {
        // Clear cache before each test
        PluginClassScanner.clearCache();
    }

    @Test
    public void testAllPluginClassNamesReturnsList() {
        List<String> classNames = PluginClassScanner.allPluginClassNames();
        
        assertNotNull(classNames);
        assertFalse(classNames.isEmpty(), "Should discover at least some plugins");
    }

    @Test
    public void testAllPluginClassNamesContainsCorePlugins() {
        List<String> classNames = PluginClassScanner.allPluginClassNames();
        
        assertTrue(classNames.contains("org.webharvest.plugin.core.HttpPlugin"),
            "Should contain HttpPlugin");
        assertTrue(classNames.contains("org.webharvest.plugin.core.TryPlugin"),
            "Should contain TryPlugin");
        assertTrue(classNames.contains("org.webharvest.plugin.core.LoopPlugin"),
            "Should contain LoopPlugin");
    }

    @Test
    public void testPackageScanFailureUsesCompleteFallbackInsteadOfPartialResults() {
        List<String> fallback = List.of("fallback.Plugin");

        List<String> result = PluginClassScanner.scanWithFallback(
                new String[] {"core", "ftp"},
                packageName -> {
                    if ("ftp".equals(packageName)) {
                        throw new IllegalStateException("simulated package scan failure");
                    }
                    return Set.<Class<?>>of(org.webharvest.plugin.core.HttpPlugin.class);
                },
                fallback);

        assertSame(result, fallback,
                "A failed package scan must discard partial discovery and use the complete fallback list");
    }

    @Test
    public void testAbsentOptionalPackageDoesNotTriggerFallback() {
        List<String> fallback = List.of("fallback.Plugin");

        List<String> result = PluginClassScanner.scanWithFallback(
                new String[] {"core", "optional"},
                packageName -> "core".equals(packageName)
                        ? Set.<Class<?>>of(org.webharvest.plugin.core.HttpPlugin.class)
                        : Set.<Class<?>>of(),
                fallback);

        assertEquals(result, List.of(org.webharvest.plugin.core.HttpPlugin.class.getName()),
                "An absent optional package represented by an empty scan result must be skipped");
        assertNotSame(result, fallback,
                "An empty optional package is not a scan failure");
    }

    @Test
    public void testEmptyRequiredCoreScanUsesFallback() {
        List<String> fallback = List.of("fallback.Plugin");

        List<String> result = PluginClassScanner.scanWithFallback(
                new String[] {"org.webharvest.plugin.core", "org.webharvest.plugin.ftp"},
                packageName -> Set.<Class<?>>of(),
                fallback);

        assertSame(result, fallback,
                "An empty scan of the required Core package must not be cached as a complete catalog");
    }

    @Test
    public void testFallbackIsNotCachedAndNextCallRetriesDiscovery() {
        List<String> fallback = List.of("fallback.Plugin");

        List<String> first = PluginClassScanner.allPluginClassNames(
                new String[] {"org.webharvest.plugin.core"},
                packageName -> {
                    throw new IllegalStateException("temporary scanner failure");
                },
                fallback);

        assertSame(first, fallback, "A failed scan should return the compatibility fallback");

        List<String> second = PluginClassScanner.allPluginClassNames(
                new String[] {"org.webharvest.plugin.core"},
                packageName -> Set.<Class<?>>of(org.webharvest.plugin.core.HttpPlugin.class),
                fallback);

        assertEquals(second, List.of(org.webharvest.plugin.core.HttpPlugin.class.getName()),
                "A subsequent call should retry discovery instead of reusing the fallback");
        assertNotSame(second, fallback);
    }

    @Test
    public void testAllPluginClassNamesResultsAreCached() {
        List<String> first = PluginClassScanner.allPluginClassNames();
        List<String> second = PluginClassScanner.allPluginClassNames();
        
        // Should return same instance (cached)
        assertSame(first, second, "Results should be cached");
    }

    @Test
    public void testAllPluginClassNamesAreSorted() {
        List<String> classNames = PluginClassScanner.allPluginClassNames();
        
        // Check that list is sorted
        for (int i = 1; i < classNames.size(); i++) {
            assertTrue(classNames.get(i - 1).compareTo(classNames.get(i)) <= 0,
                "Class names should be sorted: " + classNames.get(i - 1) + " > " + classNames.get(i));
        }
    }

    @Test
    public void testAllPluginClassNamesAreUnique() {
        List<String> classNames = PluginClassScanner.allPluginClassNames();
        
        long uniqueCount = classNames.stream().distinct().count();
        assertEquals(uniqueCount, classNames.size(),
            "All class names should be unique");
    }

    @Test
    public void testClearCacheForcesRescan() {
        List<String> first = PluginClassScanner.allPluginClassNames();
        
        PluginClassScanner.clearCache();
        
        List<String> second = PluginClassScanner.allPluginClassNames();
        
        // Should have same content but different instance
        assertNotSame(first, second, "Cache clear should force new scan");
        assertEquals(first, second, "Content should be the same after rescan");
    }

    @Test
    public void testAllPluginClassNamesContainsExtensionModules() {
        List<String> classNames = PluginClassScanner.allPluginClassNames();
        
        // Check for extension modules (may not be present in test classpath)
        boolean hasFtp = classNames.stream()
            .anyMatch(name -> name.startsWith("org.webharvest.plugin.ftp"));
        boolean hasMail = classNames.stream()
            .anyMatch(name -> name.startsWith("org.webharvest.plugin.mail"));
        boolean hasDatabase = classNames.stream()
            .anyMatch(name -> name.startsWith("org.webharvest.plugin.db"));
        
        // At least core plugins should be present
        assertTrue(classNames.size() >= 39, 
            "Should discover at least 39 core plugins");
    }
}

