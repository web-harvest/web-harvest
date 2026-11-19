package org.webharvest.definition;

import static org.testng.Assert.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.List;
import java.util.Map;
import org.testng.annotations.Test;

/**
 * Public contracts for legacy {@link XmlNode} tree mutation and lookup helpers.
 */
public class XmlNodeContractTest {

    private static final class TestXmlNode extends XmlNode {
        TestXmlNode(String name, String qName, String uri, XmlNode parent) {
            super(name, qName, uri, parent);
        }
    }

    @Test
    public void attributesElementsAndTextRoundTrip() {
        TestXmlNode root = new TestXmlNode("root", "wh:root", "http://example", null);
        root.setLocation(3, 7);
        assertEquals(root.getLineNumber(), 3);
        assertEquals(root.getColumnNumber(), 7);
        assertEquals(root.getName(), "root");
        assertEquals(root.getQName(), "wh:root");
        assertEquals(root.getUri(), "http://example");

        root.addAttribute("id", "http://example", "1");
        root.addAttribute("lang", "", "en");
        assertEquals(root.getAttribute("http://example", "id"), "1");
        // getAttribute(name) looks up in the node's own URI, not the empty namespace.
        assertEquals(root.getAttribute("id"), "1");
        assertEquals(root.getAttribute("", "lang"), "en");
        assertNull(root.getAttribute("missing-attr"));
        Map<String, String> nsAttrs = root.getAttributes("http://example");
        assertEquals(nsAttrs.get("id"), "1");
        assertTrue(root.getAttributes("missing").isEmpty());

        // Constructor with parent already registers the child; do not double-add.
        TestXmlNode child = new TestXmlNode("item", "item", "http://example", root);
        root.addElement("plain-text");
        root.addElement(" more");
        // Flush buffered text explicitly; creating another child also flushes, but an
        // explicit flush keeps the contract independent of later sibling construction.
        root.flushText();
        new TestXmlNode("tail", "tail", "http://example", root);

        assertEquals(root.getText().trim(), "plain-text more");
        ElementName itemName = new ElementName("item", "http://example");
        assertSame(root.getFirstSubnode(itemName), child);
        List<XmlNode> items = root.getSubnodes(itemName);
        assertEquals(items.size(), 1);
        assertSame(items.get(0), child);
        assertNull(root.getFirstSubnode(new ElementName("missing", "http://example")));
        assertTrue(root.getSubnodes(new ElementName("missing", "http://example")).isEmpty());
        assertFalse(root.getElementList().isEmpty());
    }

    @Test
    public void serializableTreeRoundTripsChildrenAndBufferedText() throws Exception {
        XmlNode root = new XmlNode("root", "root", "urn:test", null);
        root.addAttribute("id", "urn:test", "root-1");
        XmlNode child = new XmlNode("child", "child", "urn:test", root);
        child.addElement("child text");
        child.flushText();

        // Keep text buffered across serialization; it must remain appendable afterwards.
        root.addElement("before\\n");

        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ObjectOutputStream output = new ObjectOutputStream(bytes)) {
            output.writeObject(root);
        }

        XmlNode restored;
        try (ObjectInputStream input = new ObjectInputStream(new ByteArrayInputStream(bytes.toByteArray()))) {
            restored = (XmlNode) input.readObject();
        }

        assertEquals(restored.getAttribute("urn:test", "id"), "root-1");
        XmlNode restoredChild = restored.getFirstSubnode(new ElementName("child", "urn:test"));
        assertNotNull(restoredChild);
        assertEquals(restoredChild.getText(), "child text");
        restoredChild.addElement(" next");
        restoredChild.flushText();
        assertEquals(restoredChild.getText(), "child text next");

        restored.addElement("\\nafter");
        restored.flushText();
        assertEquals(restored.getText(), "before\\nafter");
    }

    @Test
    public void nullSingletonIsStable() {
        assertNotNull(XmlNode.NULL);
        assertEquals(XmlNode.NULL.getQName(), "*");
        assertNull(XmlNode.NULL.getName());
    }
}
