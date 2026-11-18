/*
 * Copyright (c) 2006-2025 the original author or authors.
 */

package org.webharvest.plugin.core;

import org.testng.annotations.Test;
import static org.testng.Assert.*;

/**
 * Contract tests for core plugin metadata schema projections and runtime dependencies.
 */
public class CorePluginMetadataProjectionContractTest {

    // IncludePlugin metadata projections
    @Test public void testInclude01() { assertNotNull(new IncludePlugin().getMetadata().getConfigurationSchema().getAllowedSubElements()); }
    @Test public void testInclude02() { assertNotNull(new IncludePlugin().getMetadata().getConfigurationSchema().getRequiredSubElements()); }
    @Test public void testInclude03() { assertNotNull(new IncludePlugin().getMetadata().getDependencies()); }
    
    // XQExpressionPlugin metadata projections
    @Test public void testXQExpr01() { assertNotNull(new XQExpressionPlugin().getMetadata().getConfigurationSchema().getAllowedSubElements()); }
    @Test public void testXQExpr02() { assertNotNull(new XQExpressionPlugin().getMetadata().getConfigurationSchema().getRequiredSubElements()); }
    @Test public void testXQExpr03() { assertNotNull(new XQExpressionPlugin().getMetadata().getDependencies()); }
    
    // VarPlugin metadata projections
    @Test public void testVar01() { assertNotNull(new VarPlugin().getMetadata().getConfigurationSchema().getAllowedSubElements()); }
    @Test public void testVar02() { assertNotNull(new VarPlugin().getMetadata().getConfigurationSchema().getRequiredSubElements()); }
    @Test public void testVar03() { assertNotNull(new VarPlugin().getMetadata().getDependencies()); }
    @Test public void testVar04() { assertEquals(new VarPlugin().getMetadata().getElementName(), "var"); }
    @Test public void testVar05() { assertNotNull(new VarPlugin().getMetadata().getClassName()); }
    
    // GetVarPlugin metadata projections
    @Test public void testGetVar01() { assertNotNull(new GetVarPlugin().getMetadata().getConfigurationSchema().getAllowedSubElements()); }
    @Test public void testGetVar02() { assertNotNull(new GetVarPlugin().getMetadata().getConfigurationSchema().getRequiredSubElements()); }
    @Test public void testGetVar03() { assertNotNull(new GetVarPlugin().getMetadata().getDependencies()); }
    @Test public void testGetVar04() { assertNotNull(new GetVarPlugin().getMetadata().getElementName()); }
    @Test public void testGetVar05() { assertNotNull(new GetVarPlugin().getMetadata().getClassName()); }
    
    // DefPlugin metadata projections
    @Test public void testDef01() { assertNotNull(new DefPlugin().getMetadata().getConfigurationSchema().getAllowedSubElements()); }
    @Test public void testDef02() { assertNotNull(new DefPlugin().getMetadata().getConfigurationSchema().getRequiredSubElements()); }
    @Test public void testDef03() { assertNotNull(new DefPlugin().getMetadata().getDependencies()); }
    @Test public void testDef04() { assertEquals(new DefPlugin().getMetadata().getElementName(), "def"); }
    @Test public void testDef05() { assertNotNull(new DefPlugin().getMetadata().getClassName()); }
    @Test public void testDef06() { assertNotNull(new DefPlugin().getMetadata().getConfigurationSchema().getAttributeDefinitions()); }
    @Test public void testDef07() { assertTrue(new DefPlugin().getMetadata().getConfigurationSchema().allowsBody()); }
    @Test public void testDef08() { assertEquals(new DefPlugin().getMetadata().getConfigurationSchema().getVersion(), "2.3.0"); }
    
    // XMLPlugin metadata projections
    @Test public void testXML01() { assertNotNull(new XMLPlugin().getMetadata().getConfigurationSchema().getAllowedSubElements()); }
    @Test public void testXML02() { assertNotNull(new XMLPlugin().getMetadata().getConfigurationSchema().getRequiredSubElements()); }
    @Test public void testXML03() { assertNotNull(new XMLPlugin().getMetadata().getDependencies()); }
    @Test public void testXML04() { assertEquals(new XMLPlugin().getMetadata().getElementName(), "xml"); }
    @Test public void testXML05() { assertNotNull(new XMLPlugin().getMetadata().getClassName()); }
    @Test public void testXML06() { assertNotNull(new XMLPlugin().getMetadata().getConfigurationSchema().getAttributeDefinitions()); }
    @Test public void testXML07() { assertTrue(new XMLPlugin().getMetadata().getConfigurationSchema().allowsBody()); }
    @Test public void testXML08() { assertEquals(new XMLPlugin().getMetadata().getConfigurationSchema().getVersion(), "2.3.0"); }
    @Test public void testXML09() { assertEquals(new XMLPlugin().getMetadata().getCategory(), "Core"); }
    @Test public void testXML10() { assertNotNull(new XMLPlugin().getMetadata().getTags()); }
    
    // HttpParamPlugin metadata projections
    @Test public void testHttpParam01() { assertNotNull(new HttpParamPlugin().getMetadata().getConfigurationSchema().getAllowedSubElements()); }
    @Test public void testHttpParam02() { assertNotNull(new HttpParamPlugin().getMetadata().getConfigurationSchema().getRequiredSubElements()); }
    @Test public void testHttpParam03() { assertNotNull(new HttpParamPlugin().getMetadata().getDependencies()); }
    @Test public void testHttpParam04() { assertEquals(new HttpParamPlugin().getMetadata().getElementName(), "http-param"); }
    @Test public void testHttpParam05() { assertNotNull(new HttpParamPlugin().getMetadata().getClassName()); }
    @Test public void testHttpParam06() { assertNotNull(new HttpParamPlugin().getMetadata().getConfigurationSchema().getAttributeDefinitions()); }
    @Test public void testHttpParam07() { assertTrue(new HttpParamPlugin().getMetadata().getConfigurationSchema().allowsBody()); }
    @Test public void testHttpParam08() { assertEquals(new HttpParamPlugin().getMetadata().getConfigurationSchema().getVersion(), "2.3.0"); }
    @Test public void testHttpParam09() { assertEquals(new HttpParamPlugin().getMetadata().getCategory(), "Core"); }
    @Test public void testHttpParam10() { assertNotNull(new HttpParamPlugin().getMetadata().getTags()); }
    @Test public void testHttpParam11() { assertNotNull(new HttpParamPlugin().getMetadata().getDescription()); }
    @Test public void testHttpParam12() { assertNotNull(new HttpParamPlugin().getMetadata().getAuthor()); }
}

