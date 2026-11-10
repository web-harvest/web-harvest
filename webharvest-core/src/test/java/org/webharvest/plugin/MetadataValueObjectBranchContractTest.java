package org.webharvest.plugin;

import static org.testng.Assert.*;

import org.testng.annotations.Test;

/**
 * Identity contracts for plugin metadata value objects used by IDE/catalog
 * projections (ports, shortcuts, linked services).
 */
public class MetadataValueObjectBranchContractTest {

    @Test
    public void inputPortEqualityDependsOnNameTypeAndRequired() {
        InputPort a = new InputPort("url", "Target URL", PortType.STRING, true);
        InputPort same = new InputPort("url", "ignored for equals", PortType.STRING, true);
        InputPort otherName = new InputPort("body", "Body", PortType.STRING, true);
        InputPort otherRequired = new InputPort("url", "Target URL", PortType.STRING, false);

        assertEquals(a, same);
        assertEquals(a.hashCode(), same.hashCode());
        assertFalse(a.equals(otherName));
        assertFalse(a.equals(otherRequired));
        assertFalse(a.equals(null));
        assertFalse(a.equals("url"));
        assertTrue(a.isRequired());
        assertEquals(a.getType(), PortType.STRING);
        assertTrue(a.toString().contains("url"));
    }

    @Test
    public void keyboardShortcutEqualityDependsOnKeyActionAndScope() {
        KeyboardShortcut a = new KeyboardShortcut("Cmd+N", "New", true);
        KeyboardShortcut same = new KeyboardShortcut("Cmd+N", "New", true);
        KeyboardShortcut local = new KeyboardShortcut("Cmd+N", "New", false);

        assertEquals(a, same);
        assertEquals(a.hashCode(), same.hashCode());
        assertFalse(a.equals(local));
        assertTrue(a.isGlobal());
        assertEquals(a.getKey(), "Cmd+N");
        assertTrue(a.toString().contains("Cmd+N"));
    }

    @Test
    public void linkedServiceEqualityDependsOnTypeNameAndRequired() {
        LinkedServiceMetadata a = new LinkedServiceMetadata("Database", "main", "Primary DB", true);
        LinkedServiceMetadata same = new LinkedServiceMetadata("Database", "main", "desc ignored", true);
        LinkedServiceMetadata other = new LinkedServiceMetadata("FTP", "main", "Primary DB", true);

        assertEquals(a, same);
        assertEquals(a.hashCode(), same.hashCode());
        assertFalse(a.equals(other));
        assertTrue(a.isRequired());
        assertEquals(a.getServiceType(), "Database");
        assertTrue(a.toString().contains("Database"));
    }

    @Test
    public void outputPortEqualityDependsOnNameAndType() {
        OutputPort a = new OutputPort("result", "Primary result", PortType.XML);
        OutputPort same = new OutputPort("result", "ignored", PortType.XML);
        OutputPort other = new OutputPort("error", "Errors", PortType.XML);

        assertEquals(a, same);
        assertEquals(a.hashCode(), same.hashCode());
        assertFalse(a.equals(other));
        assertFalse(a.equals(null));
        assertEquals(a.getType(), PortType.XML);
        assertTrue(a.toString().contains("result"));
    }

    @Test
    public void parentChildRelationshipEqualityDependsOnParentAndRequired() {
        ParentChildRelationship a = new ParentChildRelationship("http", true, "HTTP Request");
        ParentChildRelationship same = new ParentChildRelationship("http", true, "ignored label");
        ParentChildRelationship optional = new ParentChildRelationship("http", false, "HTTP Request");

        assertEquals(a, same);
        assertEquals(a.hashCode(), same.hashCode());
        assertFalse(a.equals(optional));
        assertTrue(a.isRequired());
        assertEquals(a.getParentElementName(), "http");
        assertEquals(a.getParentDisplayName(), "HTTP Request");
        assertTrue(a.toString().contains("http"));
    }
}
