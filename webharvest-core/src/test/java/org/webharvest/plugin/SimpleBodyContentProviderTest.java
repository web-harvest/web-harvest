package org.webharvest.plugin;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.webharvest.definition.IElementDef;
import org.webharvest.definition.WebHarvestPluginDef;
import org.webharvest.definition.XmlNode;
import org.webharvest.runtime.DynamicScopeContext;
import org.webharvest.runtime.variables.EmptyVariable;
import org.webharvest.runtime.variables.NodeVariable;
import org.webharvest.runtime.variables.Variable;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.*;
import static org.testng.Assert.*;

/**
 * Test for SimpleBodyContentProvider.
 */
public class SimpleBodyContentProviderTest {

    private SimpleBodyContentProvider provider;
    private IElementDef mockElementDef;
    private WebHarvestPluginDef mockPluginDef;
    private XmlNode mockXmlNode;
    private DynamicScopeContext mockContext;

    @BeforeMethod
    public void setUp() {
        mockElementDef = mock(IElementDef.class);
        mockPluginDef = mock(WebHarvestPluginDef.class);
        mockXmlNode = mock(XmlNode.class);
        mockContext = mock(DynamicScopeContext.class);
    }

    @Test
    public void testConstructor() {
        provider = new SimpleBodyContentProvider(mockElementDef);
        assertNotNull(provider);
    }

    @Test
    public void testConstructorWithNullElementDef() {
        provider = new SimpleBodyContentProvider(null);
        assertNotNull(provider);
    }

    @Test
    public void testExecuteBodyWithNullElementDef() throws InterruptedException {
        provider = new SimpleBodyContentProvider(null);
        
        Variable result = provider.executeBody(mockContext);
        assertEquals(result, EmptyVariable.INSTANCE);
    }

    @Test
    public void testExecuteBodyWithNoOperations() throws InterruptedException {
        when(mockElementDef.hasOperations()).thenReturn(false);
        provider = new SimpleBodyContentProvider(mockElementDef);
        
        Variable result = provider.executeBody(mockContext);
        assertEquals(result, EmptyVariable.INSTANCE);
        verify(mockElementDef).hasOperations();
    }

    @Test
    public void testExecuteBodyWithOperations() throws InterruptedException {
        when(mockElementDef.hasOperations()).thenReturn(true);
        provider = new SimpleBodyContentProvider(mockElementDef);
        
        Variable result = provider.executeBody(mockContext);
        assertEquals(result, EmptyVariable.INSTANCE);
        verify(mockElementDef).hasOperations();
    }

    @Test
    public void testGetBodyTextContentWithNullElementDef() throws InterruptedException {
        provider = new SimpleBodyContentProvider(null);
        
        Variable result = provider.getBodyTextContent(mockContext);
        assertEquals(result, EmptyVariable.INSTANCE);
    }

    @Test
    public void testGetBodyTextContentWithNonPluginDef() throws InterruptedException {
        provider = new SimpleBodyContentProvider(mockElementDef);
        
        Variable result = provider.getBodyTextContent(mockContext);
        assertEquals(result, EmptyVariable.INSTANCE);
    }

    @Test
    public void testGetBodyTextContentWithNullXmlNode() throws InterruptedException {
        when(mockPluginDef.getNode()).thenReturn(null);
        
        provider = new SimpleBodyContentProvider(mockPluginDef);
        
        Variable result = provider.getBodyTextContent(mockContext);
        assertEquals(result, EmptyVariable.INSTANCE);
    }

    @Test
    public void testGetBodyTextContentWithEmptyText() throws InterruptedException {
        when(mockPluginDef.getNode()).thenReturn(mockXmlNode);
        when(mockXmlNode.getText()).thenReturn("");
        when(mockXmlNode.getElementList()).thenReturn(new ArrayList<>());
        
        provider = new SimpleBodyContentProvider(mockPluginDef);
        
        Variable result = provider.getBodyTextContent(mockContext);
        assertEquals(result, EmptyVariable.INSTANCE);
    }

    @Test
    public void testGetBodyTextContentWithText() throws InterruptedException {
        String textContent = "Hello World";
        when(mockPluginDef.getNode()).thenReturn(mockXmlNode);
        when(mockXmlNode.getText()).thenReturn(textContent);
        
        provider = new SimpleBodyContentProvider(mockPluginDef);
        
        Variable result = provider.getBodyTextContent(mockContext);
        assertTrue(result instanceof NodeVariable);
        assertEquals(result.toString(), textContent.trim());
    }

    @Test
    public void testGetBodyTextContentWithWhitespaceText() throws InterruptedException {
        String textContent = "   \n\t   ";
        when(mockPluginDef.getNode()).thenReturn(mockXmlNode);
        when(mockXmlNode.getText()).thenReturn(textContent);
        
        provider = new SimpleBodyContentProvider(mockPluginDef);
        
        Variable result = provider.getBodyTextContent(mockContext);
        assertEquals(result, EmptyVariable.INSTANCE);
    }

    @Test
    public void testGetBodyTextContentWithChildElements() throws InterruptedException {
        String childText = "Child content";
        List<Serializable> elementList = new ArrayList<>();
        elementList.add(childText);
        
        when(mockPluginDef.getNode()).thenReturn(mockXmlNode);
        when(mockXmlNode.getText()).thenReturn("");
        when(mockXmlNode.getElementList()).thenReturn(elementList);
        
        provider = new SimpleBodyContentProvider(mockPluginDef);
        
        Variable result = provider.getBodyTextContent(mockContext);
        assertTrue(result instanceof NodeVariable);
        assertEquals(result.toString(), childText);
    }

    @Test
    public void testGetBodyTextContentWithChildXmlNodes() throws InterruptedException {
        XmlNode childNode = mock(XmlNode.class);
        List<Serializable> elementList = new ArrayList<>();
        elementList.add(childNode);
        
        when(mockPluginDef.getNode()).thenReturn(mockXmlNode);
        when(mockXmlNode.getText()).thenReturn("");
        when(mockXmlNode.getElementList()).thenReturn(elementList);
        when(childNode.getName()).thenReturn("child");
        when(childNode.getUri()).thenReturn("http://test.com");
        when(childNode.getAttributes("http://test.com")).thenReturn(new HashMap<>());
        when(childNode.getElementList()).thenReturn(new ArrayList<>());
        
        provider = new SimpleBodyContentProvider(mockPluginDef);
        
        Variable result = provider.getBodyTextContent(mockContext);
        assertTrue(result instanceof NodeVariable);
        assertEquals(result.toString(), "<child></child>");
    }

    @Test
    public void testGetBodyTextContentWithChildXmlNodesWithAttributes() throws InterruptedException {
        XmlNode childNode = mock(XmlNode.class);
        List<Serializable> elementList = new ArrayList<>();
        elementList.add(childNode);
        
        Map<String, String> attributes = new HashMap<>();
        attributes.put("attr1", "value1");
        attributes.put("attr2", "value2");
        
        when(mockPluginDef.getNode()).thenReturn(mockXmlNode);
        when(mockXmlNode.getText()).thenReturn("");
        when(mockXmlNode.getElementList()).thenReturn(elementList);
        when(childNode.getName()).thenReturn("child");
        when(childNode.getUri()).thenReturn("http://test.com");
        when(childNode.getAttributes("http://test.com")).thenReturn(attributes);
        when(childNode.getElementList()).thenReturn(new ArrayList<>());
        
        provider = new SimpleBodyContentProvider(mockPluginDef);
        
        Variable result = provider.getBodyTextContent(mockContext);
        assertTrue(result instanceof NodeVariable);
        String resultString = result.toString();
        assertTrue(resultString.contains("<child"));
        assertTrue(resultString.contains("attr1=\"value1\""));
        assertTrue(resultString.contains("attr2=\"value2\""));
        assertTrue(resultString.contains("</child>"));
    }

    @Test
    public void testGetBodyTextContentWithNestedChildNodes() throws InterruptedException {
        XmlNode childNode = mock(XmlNode.class);
        XmlNode grandChildNode = mock(XmlNode.class);
        List<Serializable> elementList = new ArrayList<>();
        elementList.add(childNode);
        List<Serializable> childElementList = new ArrayList<>();
        childElementList.add(grandChildNode);
        
        when(mockPluginDef.getNode()).thenReturn(mockXmlNode);
        when(mockXmlNode.getText()).thenReturn("");
        when(mockXmlNode.getElementList()).thenReturn(elementList);
        when(childNode.getName()).thenReturn("child");
        when(childNode.getUri()).thenReturn("http://test.com");
        when(childNode.getAttributes("http://test.com")).thenReturn(new HashMap<>());
        when(childNode.getElementList()).thenReturn(childElementList);
        when(grandChildNode.getName()).thenReturn("grandchild");
        when(grandChildNode.getUri()).thenReturn("http://test.com");
        when(grandChildNode.getAttributes("http://test.com")).thenReturn(new HashMap<>());
        when(grandChildNode.getElementList()).thenReturn(new ArrayList<>());
        
        provider = new SimpleBodyContentProvider(mockPluginDef);
        
        Variable result = provider.getBodyTextContent(mockContext);
        assertTrue(result instanceof NodeVariable);
        String resultString = result.toString();
        assertTrue(resultString.contains("<child>"));
        assertTrue(resultString.contains("<grandchild></grandchild>"));
        assertTrue(resultString.contains("</child>"));
    }

    @Test
    public void testGetBodyTextContentWithMixedContent() throws InterruptedException {
        XmlNode childNode = mock(XmlNode.class);
        String textContent = "Some text";
        List<Serializable> elementList = new ArrayList<>();
        elementList.add(textContent);
        elementList.add(childNode);
        elementList.add("More text");
        
        when(mockPluginDef.getNode()).thenReturn(mockXmlNode);
        when(mockXmlNode.getText()).thenReturn("");
        when(mockXmlNode.getElementList()).thenReturn(elementList);
        when(childNode.getName()).thenReturn("child");
        when(childNode.getUri()).thenReturn("http://test.com");
        when(childNode.getAttributes("http://test.com")).thenReturn(new HashMap<>());
        when(childNode.getElementList()).thenReturn(new ArrayList<>());
        
        provider = new SimpleBodyContentProvider(mockPluginDef);
        
        Variable result = provider.getBodyTextContent(mockContext);
        assertTrue(result instanceof NodeVariable);
        String resultString = result.toString();
        assertTrue(resultString.contains("Some text"));
        assertTrue(resultString.contains("<child></child>"));
        assertTrue(resultString.contains("More text"));
    }

    @Test
    public void testExtractTextContentWithNullNode() throws InterruptedException {
        provider = new SimpleBodyContentProvider(mockPluginDef);
        
        // This tests the private extractTextContent method indirectly
        when(mockPluginDef.getNode()).thenReturn(null);
        
        Variable result = provider.getBodyTextContent(mockContext);
        assertEquals(result, EmptyVariable.INSTANCE);
    }

    @Test
    public void testConstructXmlStringWithNullNode() throws InterruptedException {
        provider = new SimpleBodyContentProvider(mockPluginDef);
        
        // This tests the private constructXmlString method indirectly
        when(mockPluginDef.getNode()).thenReturn(mockXmlNode);
        when(mockXmlNode.getText()).thenReturn("");
        
        XmlNode childNode = mock(XmlNode.class);
        List<Serializable> elementList = new ArrayList<>();
        elementList.add(childNode);
        when(mockXmlNode.getElementList()).thenReturn(elementList);
        
        // Set up child node to return null for name, which should be handled gracefully
        when(childNode.getName()).thenReturn(null);
        when(childNode.getUri()).thenReturn("http://test.com");
        when(childNode.getAttributes("http://test.com")).thenReturn(new HashMap<>());
        when(childNode.getElementList()).thenReturn(new ArrayList<>());
        
        Variable result = provider.getBodyTextContent(mockContext);
        assertNotNull(result);
    }
}
