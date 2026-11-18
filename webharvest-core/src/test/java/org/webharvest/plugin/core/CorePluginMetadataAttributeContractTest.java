/*
 * Copyright (c) 2006-2025 the original author or authors.
 */

package org.webharvest.plugin.core;

import org.testng.annotations.Test;

import static org.testng.Assert.*;
import static org.webharvest.plugin.core.MetadataTestUtils.expectedCategory;

/**
 * Contract tests for core plugin metadata identities and declared schema attributes.
 */
public class CorePluginMetadataAttributeContractTest {

    // TextPlugin - 41.48%
    @Test public void textPlugin01() { assertNotNull(new TextPlugin().getMetadata().getConfigurationSchema().getAttribute("id")); }
    @Test public void textPlugin02() { assertNotNull(new TextPlugin().getMetadata().getConfigurationSchema().getAttribute("delimiter")); }
    @Test public void textPlugin03() { assertNotNull(new TextPlugin().getMetadata().getConfigurationSchema().getAttribute("charset")); }
    @Test public void textPlugin04() { assertEquals(new TextPlugin().getMetadata().getId(), "text"); }
    @Test public void textPlugin05() { assertEquals(new TextPlugin().getMetadata().getElementName(), "text"); }
    
    // LoopPlugin - 49.36%
    @Test public void loopPlugin01() { assertNotNull(new LoopPlugin().getMetadata().getConfigurationSchema().getAttribute("maxloops")); }
    @Test public void loopPlugin02() { assertNotNull(new LoopPlugin().getMetadata().getConfigurationSchema().getAttribute("index")); }
    @Test public void loopPlugin03() { assertNotNull(new LoopPlugin().getMetadata().getConfigurationSchema().getAttribute("id")); }
    @Test public void loopPlugin04() { assertEquals(new LoopPlugin().getMetadata().getId(), "loop"); }
    @Test public void loopPlugin05() { assertEquals(new LoopPlugin().getMetadata().getElementName(), "loop"); }
    
    // TryPlugin - 48.23%
    @Test public void tryPlugin01() { assertNotNull(new TryPlugin().getMetadata().getConfigurationSchema().getAttribute("id")); }
    @Test public void tryPlugin02() { assertEquals(new TryPlugin().getMetadata().getId(), "try"); }
    @Test public void tryPlugin03() { assertEquals(new TryPlugin().getMetadata().getElementName(), "try"); }
    @Test public void tryPlugin04() { assertNotNull(new TryPlugin().getMetadata().getConfigurationSchema().getAllowedSubElements()); }
    @Test public void tryPlugin05() { assertNotNull(new TryPlugin().getMetadata().getConfigurationSchema().getRequiredSubElements()); }
    
    // TokenizePlugin - 58.00%
    @Test public void tokenizePlugin01() { assertNotNull(new TokenizePlugin().getMetadata().getConfigurationSchema().getAttribute("delimiters")); }
    @Test public void tokenizePlugin02() { assertNotNull(new TokenizePlugin().getMetadata().getConfigurationSchema().getAttribute("id")); }
    @Test public void tokenizePlugin03() { assertEquals(new TokenizePlugin().getMetadata().getId(), "tokenize"); }
    @Test public void tokenizePlugin04() { assertEquals(new TokenizePlugin().getMetadata().getElementName(), "tokenize"); }
    @Test public void tokenizePlugin05() { assertNotNull(new TokenizePlugin().getMetadata().getConfigurationSchema().getAttributeDefinitions()); }
    
    // ReturnPlugin - 57.50%
    @Test public void returnPlugin01() { assertNotNull(new ReturnPlugin().getMetadata().getConfigurationSchema().getAttribute("id")); }
    @Test public void returnPlugin02() { assertEquals(new ReturnPlugin().getMetadata().getId(), "return"); }
    @Test public void returnPlugin03() { assertEquals(new ReturnPlugin().getMetadata().getElementName(), "return"); }
    @Test public void returnPlugin04() { assertNotNull(new ReturnPlugin().getMetadata().getConfigurationSchema().getAllowedSubElements()); }
    @Test public void returnPlugin05() { assertNotNull(new ReturnPlugin().getMetadata().getConfigurationSchema().getRequiredSubElements()); }
    
    // XQueryPlugin - 24.64%
    @Test public void xqueryPlugin01() { assertNotNull(new XQueryPlugin().getMetadata().getConfigurationSchema().getAttribute("id")); }
    @Test public void xqueryPlugin02() { assertEquals(new XQueryPlugin().getMetadata().getId(), "xquery"); }
    @Test public void xqueryPlugin03() { assertEquals(new XQueryPlugin().getMetadata().getElementName(), "xquery"); }
    @Test public void xqueryPlugin04() { assertNotNull(new XQueryPlugin().getMetadata().getConfigurationSchema()); }
    @Test public void xqueryPlugin05() { assertNotNull(new XQueryPlugin().getMetadata().getConfigurationSchema().getAllowedSubElements()); }
    @Test public void xqueryPlugin06() { assertNotNull(new XQueryPlugin().getMetadata().getConfigurationSchema().getRequiredSubElements()); }
    @Test public void xqueryPlugin07() { assertTrue(new XQueryPlugin().getMetadata().getConfigurationSchema().allowsBody()); }
    @Test public void xqueryPlugin08() { assertNotNull(new XQueryPlugin().getMetadata().getConfigurationSchema().getAttributeDefinitions()); }
    @Test public void xqueryPlugin09() { assertNotNull(new XQueryPlugin().getMetadata().getTags()); }
    @Test public void xqueryPlugin10() { assertNotNull(new XQueryPlugin().getMetadata().getDependencies()); }
    
    // JsonToXmlPlugin - 33.04%
    @Test public void jsonPlugin01() { assertNotNull(new JsonToXmlPlugin().getMetadata().getConfigurationSchema().getAttribute("id")); }
    @Test public void jsonPlugin02() { assertEquals(new JsonToXmlPlugin().getMetadata().getId(), "json-to-xml"); }
    @Test public void jsonPlugin03() { assertEquals(new JsonToXmlPlugin().getMetadata().getElementName(), "json-to-xml"); }
    @Test public void jsonPlugin04() { assertNotNull(new JsonToXmlPlugin().getMetadata().getConfigurationSchema()); }
    @Test public void jsonPlugin05() { assertTrue(new JsonToXmlPlugin().getMetadata().getConfigurationSchema().allowsBody()); }
    @Test public void jsonPlugin06() { assertNotNull(new JsonToXmlPlugin().getMetadata().getTags()); }
    @Test public void jsonPlugin07() { assertNotNull(new JsonToXmlPlugin().getMetadata().getDependencies()); }
    @Test public void jsonPlugin08() { assertEquals(new JsonToXmlPlugin().getMetadata().getCategory(), expectedCategory(JsonToXmlPlugin.class)); }
    @Test public void jsonPlugin09() { assertNotNull(new JsonToXmlPlugin().getMetadata().getClassName()); }
    @Test public void jsonPlugin10() { assertNotNull(new JsonToXmlPlugin().getMetadata().getNamespace()); }
    
    // ScriptPlugin - 21.74%  
    @Test public void scriptPlugin01() { assertNotNull(new ScriptPlugin().getMetadata().getConfigurationSchema().getAttribute("language")); }
    @Test public void scriptPlugin02() { assertNotNull(new ScriptPlugin().getMetadata().getConfigurationSchema().getAttribute("return")); }
    @Test public void scriptPlugin03() { assertNotNull(new ScriptPlugin().getMetadata().getConfigurationSchema().getAttribute("id")); }
    @Test public void scriptPlugin04() { assertEquals(new ScriptPlugin().getMetadata().getId(), "script"); }
    @Test public void scriptPlugin05() { assertEquals(new ScriptPlugin().getMetadata().getElementName(), "script"); }
    @Test public void scriptPlugin06() { assertNotNull(new ScriptPlugin().getMetadata().getTags()); }
    @Test public void scriptPlugin07() { assertTrue(new ScriptPlugin().getMetadata().getTags().length > 0); }
    @Test public void scriptPlugin08() { assertNotNull(new ScriptPlugin().getMetadata().getDependencies()); }
    @Test public void scriptPlugin09() { assertEquals(new ScriptPlugin().getMetadata().getCategory(), expectedCategory(ScriptPlugin.class)); }
    @Test public void scriptPlugin10() { assertNotNull(new ScriptPlugin().getMetadata().getClassName()); }
}

