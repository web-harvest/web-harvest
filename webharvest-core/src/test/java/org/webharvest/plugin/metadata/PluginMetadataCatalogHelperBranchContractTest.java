package org.webharvest.plugin.metadata;

import static org.testng.Assert.*;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * Catalog projection helper contracts for icon/origin/sanitize/humanize paths that
 * back {@link PluginMetadataCatalog} public descriptors.
 *
 * <p>Uses the same reflective access pattern already established for private
 * CallPlugin normalization helpers, because these helpers are only reachable
 * through classpath-bound plugin descriptors (and several icon/origin cases map
 * optional extension plugins that are absent from the core test classpath).</p>
 */
public class PluginMetadataCatalogHelperBranchContractTest {

    private Method pickIcon;
    private Method defaultIconForCategory;
    private Method inferOrigin;
    private Method sanitize3;
    private Method humanize;
    private Method hasText;
    private Method normalizeGroup;

    @BeforeClass
    public void resolveHelpers() throws Exception {
        Class<?> catalog = PluginMetadataCatalog.class;
        pickIcon = catalog.getDeclaredMethod("pickIcon", String.class, String.class);
        pickIcon.setAccessible(true);
        defaultIconForCategory = catalog.getDeclaredMethod("defaultIconForCategory", String.class);
        defaultIconForCategory.setAccessible(true);
        inferOrigin = catalog.getDeclaredMethod("inferOrigin", String.class);
        inferOrigin.setAccessible(true);
        sanitize3 = catalog.getDeclaredMethod("sanitize", String.class, String.class, String.class);
        sanitize3.setAccessible(true);
        humanize = catalog.getDeclaredMethod("humanize", String.class);
        humanize.setAccessible(true);
        hasText = catalog.getDeclaredMethod("hasText", String.class);
        hasText.setAccessible(true);
        normalizeGroup = catalog.getDeclaredMethod("normalizeGroup", String.class);
        normalizeGroup.setAccessible(true);
    }

    @DataProvider(name = "elementIcons")
    public Object[][] elementIcons() {
        return new Object[][] {
            {"get-var", "Variables", "fa-arrow-right"},
            {"ftp", "Network", "fa-server"},
            {"ftp-get", "Network", "fa-arrow-down"},
            {"ftp-put", "Network", "fa-arrow-up"},
            {"ftp-list", "Network", "fa-list-check"},
            {"mail", "Network", "fa-envelope"},
            {"database", "Storage", "fa-database"},
            {"connection", "Network", "fa-plug"},
            {"zip", "Storage", "fa-file-zipper"},
            {"zip-entry", "Storage", "fa-file-circle-plus"},
            {"webbrowser", "Network", "fa-window-restore"},
            {"unknown-plugin", "Variables", "fa-box"},
            {"unknown-plugin", "Control Flow", "fa-diagram-next"},
            {"unknown-plugin", "Data Extraction", "fa-magnifying-glass"},
            {"unknown-plugin", "Data Processing", "fa-gears"},
            {"unknown-plugin", "HTTP & Web", "fa-network-wired"},
            {"unknown-plugin", "File Operations", "fa-database"},
            {"unknown-plugin", "Configuration", "fa-gear"},
            {"unknown-plugin", null, "fa-puzzle-piece"},
            {"unknown-plugin", "Something Else", "fa-puzzle-piece"}
        };
    }

    @Test(dataProvider = "elementIcons")
    public void pickIconProjectsElementOrCategoryDefaults(String element, String category, String expected)
            throws Exception {
        assertEquals(pickIcon.invoke(null, element, category), expected);
    }

    @DataProvider(name = "origins")
    public Object[][] origins() {
        return new Object[][] {
            {null, "core"},
            {"org.webharvest.plugin.core", "core"},
            {"org.webharvest.plugin.ftp", "ftp"},
            {"org.webharvest.plugin.mail", "mail"},
            {"org.webharvest.plugin.db", "database"},
            {"org.webharvest.plugin.zip", "zip"},
            {"org.webharvest.plugin.webbrowser", "webbrowser"}
        };
    }

    @Test(dataProvider = "origins")
    public void inferOriginMapsPackageNamespaces(String packageName, String expected) throws Exception {
        assertEquals(inferOrigin.invoke(null, packageName), expected);
    }

    @Test
    public void sanitizeFallsBackThroughBlankCandidates() throws Exception {
        assertEquals(sanitize3.invoke(null, "  keep  ", "fallback", "default"), "keep");
        assertEquals(sanitize3.invoke(null, "   ", "  fallback  ", "default"), "fallback");
        assertEquals(sanitize3.invoke(null, null, null, "default"), "default");
        assertEquals(sanitize3.invoke(null, "", "", "default"), "default");
    }

    @Test
    public void humanizeHandlesBlankNamesAndSeparators() throws Exception {
        assertEquals(humanize.invoke(null, (Object) null), "Unnamed Plugin");
        assertEquals(humanize.invoke(null, "   "), "Unnamed Plugin");
        assertEquals(humanize.invoke(null, "http-header"), "Http Header");
        assertEquals(humanize.invoke(null, "a__b"), "A B");
        assertEquals(humanize.invoke(null, "x"), "X");
    }

    @Test
    public void hasTextAndNormalizeGroupCoverBlankAndAliasPaths() throws Exception {
        assertFalse((Boolean) hasText.invoke(null, (Object) null));
        assertFalse((Boolean) hasText.invoke(null, "  "));
        assertTrue((Boolean) hasText.invoke(null, "ok"));

        assertEquals(normalizeGroup.invoke(null, (Object) null), "General");
        assertEquals(normalizeGroup.invoke(null, "Control Flow"), "Control");
        assertEquals(normalizeGroup.invoke(null, "Communication"), "Network");
        assertEquals(normalizeGroup.invoke(null, "Storage"), "Storage");
        assertEquals(normalizeGroup.invoke(null, "Unknown"), "General");
    }

    @Test
    public void publicDescriptorsProjectRichSchemaContractsUsedByIde() {
        PluginDescriptor function = PluginMetadataCatalog.getDescriptor("function");
        assertNotNull(function);
        Map<?, ?> functionSchema = (Map<?, ?>) function.getExtensions().get("schema");
        Map<?, ?> name = findAttribute((List<?>) functionSchema.get("attributeDefinitions"), "name");
        assertEquals(name.get("pattern"), "^[a-zA-Z_][a-zA-Z0-9_]*$");

        PluginDescriptor whileDescriptor = PluginMetadataCatalog.getDescriptor("while");
        Map<?, ?> whileSchema = (Map<?, ?>) whileDescriptor.getExtensions().get("schema");
        Map<?, ?> maxloops = findAttribute((List<?>) whileSchema.get("attributeDefinitions"), "maxloops");
        assertEquals(maxloops.get("minValue"), 1);
        assertEquals(maxloops.get("maxValue"), 1000000);

        PluginDescriptor regexp = PluginMetadataCatalog.getDescriptor("regexp");
        Map<?, ?> regexpSchema = (Map<?, ?>) regexp.getExtensions().get("schema");
        assertEquals(regexpSchema.get("requiredSubElements"),
                List.of("regexp-pattern", "regexp-source"));

        PluginDescriptor caseDescriptor = PluginMetadataCatalog.getDescriptor("case");
        Map<?, ?> caseSchema = (Map<?, ?>) caseDescriptor.getExtensions().get("schema");
        assertEquals(caseSchema.get("requiredSubElements"), List.of("if"));

        PluginDescriptor httpHeader = PluginMetadataCatalog.getDescriptor("http-header");
        Map<?, ?> headerSchema = (Map<?, ?>) httpHeader.getExtensions().get("schema");
        assertEquals(headerSchema.get("requiredParents"), List.of("http"));
        Map<?, ?> headerName = findAttribute((List<?>) headerSchema.get("attributeDefinitions"), "name");
        assertEquals(headerName.get("pattern"), "^[a-zA-Z0-9\\-_]+$");

        PluginDescriptor getVar = PluginMetadataCatalog.getDescriptor("get-var");
        assertNotNull(getVar);
        assertEquals(getVar.getElementName(), "get");
        assertEquals(getVar.getUiHints().getIcon(), "fa-box",
                "get-var id maps to element 'get', so pickIcon falls through to Variables category");
    }

    private Map<?, ?> findAttribute(List<?> definitions, String name) {
        for (Object definition : definitions) {
            Map<?, ?> attribute = (Map<?, ?>) definition;
            if (name.equals(attribute.get("name"))) {
                return attribute;
            }
        }
        fail("Missing schema attribute: " + name);
        return Map.of();
    }
}
