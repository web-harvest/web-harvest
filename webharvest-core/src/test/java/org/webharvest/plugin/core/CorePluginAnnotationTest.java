/*
 * Copyright (c) 2006-2025 the original author or authors.
 */

package org.webharvest.plugin.core;

import org.testng.annotations.Test;
import org.webharvest.plugin.annotations.CorePlugin;

import static org.testng.Assert.*;

/**
 * TDD Test for @CorePlugin annotation with category.
 * 
 * <p>
 * Ensures all plugins have proper category in @CorePlugin annotation.
 * This eliminates need for hardcoded inferPluginCategory() logic.
 * </p>
 * 
 * @author Robert Bala
 * @since 2.3.1
 */
public class CorePluginAnnotationTest {
    
    @Test(groups = {"unit", "annotation", "category"})
    public void testHttpPluginHasCategoryInAnnotation() {
        CorePlugin annotation = HttpPlugin.class.getAnnotation(CorePlugin.class);
        assertNotNull(annotation, "HttpPlugin should have @CorePlugin annotation");
        assertEquals(annotation.category(), "Network",
            "HttpPlugin @CorePlugin should specify category='Network'");
    }
    
    @Test(groups = {"unit", "annotation", "category"})
    public void testXPathPluginHasCategoryInAnnotation() {
        CorePlugin annotation = XPathPlugin.class.getAnnotation(CorePlugin.class);
        assertNotNull(annotation);
        assertEquals(annotation.category(), "Extraction",
            "XPathPlugin @CorePlugin should specify category='Extraction'");
    }
    
    @Test(groups = {"unit", "annotation", "category"})
    public void testDefPluginHasCategoryInAnnotation() {
        CorePlugin annotation = DefPlugin.class.getAnnotation(CorePlugin.class);
        assertNotNull(annotation);
        assertEquals(annotation.category(), "Variables",
            "DefPlugin @CorePlugin should specify category='Variables'");
    }
    
    @Test(groups = {"unit", "annotation", "category"})
    public void testLoopPluginHasCategoryInAnnotation() {
        CorePlugin annotation = LoopPlugin.class.getAnnotation(CorePlugin.class);
        assertNotNull(annotation);
        assertEquals(annotation.category(), "Control",
            "LoopPlugin @CorePlugin should specify category='Control'");
    }
    
    @Test(groups = {"unit", "annotation", "category"})
    public void testScriptPluginHasCategoryInAnnotation() {
        CorePlugin annotation = ScriptPlugin.class.getAnnotation(CorePlugin.class);
        assertNotNull(annotation);
        assertEquals(annotation.category(), "Processing",
            "ScriptPlugin @CorePlugin should specify category='Processing'");
    }
    
    @Test(groups = {"unit", "annotation", "category"})
    public void testFilePluginHasCategoryInAnnotation() {
        CorePlugin annotation = FilePlugin.class.getAnnotation(CorePlugin.class);
        assertNotNull(annotation);
        assertEquals(annotation.category(), "Storage",
            "FilePlugin @CorePlugin should specify category='Storage'");
    }
    
    @Test(groups = {"unit", "annotation", "category"})
    public void testLogPluginHasCategoryInAnnotation() {
        CorePlugin annotation = LogPlugin.class.getAnnotation(CorePlugin.class);
        assertNotNull(annotation);
        assertEquals(annotation.category(), "Utilities",
            "LogPlugin @CorePlugin should specify category='Utilities'");
    }
}




