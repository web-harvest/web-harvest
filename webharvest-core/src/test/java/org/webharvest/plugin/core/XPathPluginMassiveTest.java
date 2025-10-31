/*
 * Copyright (c) 2006-2025 the original author or authors.
 */

package org.webharvest.plugin.core;

import org.testng.annotations.Test;
import static org.testng.Assert.*;

/**
 * Massive test suite for {@link XPathPlugin}.
 * Target: 27.31% → 70%+ (249 instructions, +0.53% impact)
 */
public class XPathPluginMassiveTest {

    @Test public void testMeta01() { XPathPlugin p = new XPathPlugin(); assertNotNull(p.getMetadata()); }
    @Test public void testMeta02() { XPathPlugin p = new XPathPlugin(); assertTrue(p.getMetadata().isCorePlugin()); }
    @Test public void testMeta03() { XPathPlugin p = new XPathPlugin(); assertNotNull(p.getMetadata().getName()); }
    @Test public void testMeta04() { XPathPlugin p = new XPathPlugin(); assertNotNull(p.getMetadata().getId()); }
    @Test public void testMeta05() { XPathPlugin p = new XPathPlugin(); assertEquals(p.getMetadata().getVersion(), "2.1.0"); }
    @Test public void testMeta06() { XPathPlugin p = new XPathPlugin(); assertNotNull(p.getMetadata().getDescription()); }
    @Test public void testMeta07() { XPathPlugin p = new XPathPlugin(); assertEquals(p.getMetadata().getCategory(), "Extraction"); }
    @Test public void testMeta08() { XPathPlugin p = new XPathPlugin(); assertNotNull(p.getMetadata().getTags()); }
    @Test public void testMeta09() { XPathPlugin p = new XPathPlugin(); assertEquals(p.getMetadata().getElementName(), "xpath"); }
    @Test public void testMeta10() { XPathPlugin p = new XPathPlugin(); assertNotNull(p.getMetadata().getNamespace()); }
    @Test public void testMeta11() { XPathPlugin p = new XPathPlugin(); assertEquals(p.getMetadata().getClassName(), XPathPlugin.class.getName()); }
    @Test public void testMeta12() { XPathPlugin p = new XPathPlugin(); assertNotNull(p.getMetadata().getDependencies()); }
    @Test public void testMeta13() { XPathPlugin p = new XPathPlugin(); assertNotNull(p.getMetadata().getAuthor()); }
    @Test public void testMeta14() { XPathPlugin p = new XPathPlugin(); assertTrue(p.getMetadata().getDescription().length() > 0); }
    @Test public void testMeta15() { XPathPlugin p = new XPathPlugin(); assertTrue(p.getMetadata().getNamespace().contains("webharvest")); }
    
    @Test public void testSchema01() { XPathPlugin p = new XPathPlugin(); assertNotNull(p.getConfigurationSchema()); }
    @Test public void testSchema02() { XPathPlugin p = new XPathPlugin(); assertTrue(p.getConfigurationSchema().allowsBody()); }
    @Test public void testSchema03() { XPathPlugin p = new XPathPlugin(); assertNotNull(p.getConfigurationSchema().getOptionalAttributes()); }
    @Test public void testSchema04() { XPathPlugin p = new XPathPlugin(); assertNotNull(p.getConfigurationSchema().getRequiredAttributes()); }
    @Test public void testSchema05() { XPathPlugin p = new XPathPlugin(); assertNotNull(p.getConfigurationSchema().getAttributeDefinitions()); }
    @Test public void testSchema06() { XPathPlugin p = new XPathPlugin(); assertTrue(p.getConfigurationSchema().getAttributeDefinitions().length > 0); }
    @Test public void testSchema07() { XPathPlugin p = new XPathPlugin(); assertEquals(p.getConfigurationSchema().getVersion(), "2.1.0"); }
    @Test public void testSchema08() { XPathPlugin p = new XPathPlugin(); assertNotNull(p.getConfigurationSchema().getRequiredSubElements()); }
    @Test public void testSchema09() { XPathPlugin p = new XPathPlugin(); assertNotNull(p.getConfigurationSchema().getAllowedSubElements()); }
    @Test public void testSchema10() { XPathPlugin p = new XPathPlugin(); var attrs = p.getConfigurationSchema().getAttributeDefinitions(); for (var a : attrs) assertNotNull(a.getName()); }
    @Test public void testSchema11() { XPathPlugin p = new XPathPlugin(); var attrs = p.getConfigurationSchema().getAttributeDefinitions(); for (var a : attrs) assertNotNull(a.getType()); }
    @Test public void testSchema12() { XPathPlugin p = new XPathPlugin(); var attrs = p.getConfigurationSchema().getAttributeDefinitions(); for (var a : attrs) assertNotNull(a.getDescription()); }
}

