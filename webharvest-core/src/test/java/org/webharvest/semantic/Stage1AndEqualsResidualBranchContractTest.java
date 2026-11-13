package org.webharvest.semantic;

import static org.testng.Assert.*;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.testng.annotations.Test;
import org.webharvest.plugin.KeyboardShortcut;
import org.webharvest.plugin.LinkedServiceMetadata;
import org.webharvest.plugin.metadata.PluginDescriptor;

/**
 * Residual equals / Stage-1 catalog projection contracts for uncovered
 * identity and schema-mapping branches.
 */
public class Stage1AndEqualsResidualBranchContractTest {

    @Test
    public void optionalProfileEntryEqualsCoversIdentityMatrix() {
        OptionalPluginProfile.Entry base = new OptionalPluginProfile.Entry(
                "http://ns", "ftp", "org.example.Ftp");
        OptionalPluginProfile.Entry same = new OptionalPluginProfile.Entry(
                "http://ns", "ftp", "org.example.Ftp");
        OptionalPluginProfile.Entry otherNs = new OptionalPluginProfile.Entry(
                "http://other", "ftp", "org.example.Ftp");
        OptionalPluginProfile.Entry otherLocal = new OptionalPluginProfile.Entry(
                "http://ns", "mail", "org.example.Ftp");
        OptionalPluginProfile.Entry otherClass = new OptionalPluginProfile.Entry(
                "http://ns", "ftp", "org.example.Other");

        assertTrue(base.equals(same));
        assertEquals(base.hashCode(), same.hashCode());
        assertFalse(base.equals(null));
        assertFalse(base.equals("not-an-entry"));
        assertFalse(base.equals(otherNs));
        assertFalse(base.equals(otherLocal));
        assertFalse(base.equals(otherClass));
        assertTrue(base.toString().contains("ftp"));
    }

    @Test
    public void linkedServiceAndKeyboardShortcutEqualsCoverRequiredFlags() {
        LinkedServiceMetadata a = new LinkedServiceMetadata("http", "client", "desc", true);
        LinkedServiceMetadata b = new LinkedServiceMetadata("http", "client", "desc", true);
        LinkedServiceMetadata c = new LinkedServiceMetadata("http", "client", "desc", false);
        LinkedServiceMetadata d = new LinkedServiceMetadata("ftp", "client", "desc", true);
        LinkedServiceMetadata e = new LinkedServiceMetadata("http", "other", "desc", true);

        assertTrue(a.equals(a));
        assertTrue(a.equals(b));
        assertEquals(a.hashCode(), b.hashCode());
        assertFalse(a.equals(null));
        assertFalse(a.equals("x"));
        assertFalse(a.equals(c));
        assertFalse(a.equals(d));
        assertFalse(a.equals(e));

        KeyboardShortcut k1 = new KeyboardShortcut("Ctrl+S", "save", true);
        KeyboardShortcut k2 = new KeyboardShortcut("Ctrl+S", "save", true);
        KeyboardShortcut k3 = new KeyboardShortcut("Ctrl+S", "save", false);
        KeyboardShortcut k4 = new KeyboardShortcut("Ctrl+O", "save", true);
        KeyboardShortcut k5 = new KeyboardShortcut("Ctrl+S", "open", true);

        assertTrue(k1.equals(k1));
        assertTrue(k1.equals(k2));
        assertEquals(k1.hashCode(), k2.hashCode());
        assertFalse(k1.equals(null));
        assertFalse(k1.equals("x"));
        assertFalse(k1.equals(k3));
        assertFalse(k1.equals(k4));
        assertFalse(k1.equals(k5));
    }

    @Test
    public void stage1FromCatalogHandlesMissingAndPresentSchemaShapes() {
        PluginDescriptor bare = new PluginDescriptor(
                "bare", "bare", "Bare", "core", "org.example.Bare", "misc", "desc",
                null, null, null, List.of(),
                Collections.singletonMap("namespace", Integer.valueOf(7)));

        Stage1PluginDescriptor withoutSchema = Stage1PluginDescriptor.fromCatalog(bare);
        assertFalse(withoutSchema.hasReliableSchema());
        assertEquals(withoutSchema.getNamespaceUri(), "");
        assertFalse(withoutSchema.getAllowsBody().isPresent());

        Map<String, Object> attr = new HashMap<>();
        attr.put("name", "href");
        attr.put("required", Boolean.TRUE);
        attr.put("type", "string");

        Map<String, Object> schema = new HashMap<>();
        schema.put("requiredAttributes", List.of("href"));
        schema.put("optionalAttributes", List.of("id"));
        schema.put("allowedSubElements", List.of("param"));
        schema.put("requiredSubElements", List.of());
        schema.put("requiredParents", List.of("config"));
        schema.put("allowsBody", Boolean.TRUE);
        schema.put("canAppearAtRoot", Boolean.FALSE);
        schema.put("attributeDefinitions", List.of(attr, "ignore-me"));

        Map<String, Object> extensions = new HashMap<>();
        extensions.put("namespace", "http://org.webharvest/schema/2.1/core");
        extensions.put("schema", schema);

        PluginDescriptor rich = new PluginDescriptor(
                "http", "http", "HTTP", "core", "org.webharvest.plugin.core.HttpPlugin",
                "network", "desc", null, null, null, List.of(), extensions);

        Stage1PluginDescriptor withSchema = Stage1PluginDescriptor.fromCatalog(rich);
        assertTrue(withSchema.hasReliableSchema());
        assertEquals(withSchema.getNamespaceUri(), "http://org.webharvest/schema/2.1/core");
        assertEquals(withSchema.getRequiredAttributes(), List.of("href"));
        assertEquals(withSchema.getOptionalAttributes(), List.of("id"));
        assertTrue(withSchema.getAllowsBody().isPresent());
        assertTrue(withSchema.getAllowsBody().get());
        assertTrue(withSchema.getCanAppearAtRoot().isPresent());
        assertFalse(withSchema.getCanAppearAtRoot().get());
        assertEquals(withSchema.getAttributeDefinitions().size(), 1);
        assertEquals(withSchema.getAttributeDefinitions().get(0).getName(), "href");
    }
}
