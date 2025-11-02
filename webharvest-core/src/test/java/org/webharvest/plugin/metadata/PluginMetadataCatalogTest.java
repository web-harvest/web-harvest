package org.webharvest.plugin.metadata;

import static org.testng.Assert.*;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.testng.annotations.Test;

/**
 * TDD coverage for {@link PluginMetadataCatalog} ensuring the semantic catalog
 * stays complete and rich enough for the redesigned IDE/LLM experiences.
 */
public class PluginMetadataCatalogTest {

    @Test
    public void allIndexedPluginClassesAreRepresentedInCatalog() {
        Set<String> classNamesInCatalog = PluginMetadataCatalog.getAllDescriptors().stream()
                .map(PluginDescriptor::getClassName)
                .collect(Collectors.toSet());

        for (String expectedClass : PluginClassIndex.allPluginClassNames()) {
            assertTrue(classNamesInCatalog.contains(expectedClass),
                    "Missing metadata descriptor for plugin class " + expectedClass);
        }
    }

    @Test
    public void everyDescriptorHasDocumentationSummaryAndUrl() {
        for (PluginDescriptor descriptor : PluginMetadataCatalog.getAllDescriptors()) {
            PluginDocumentation documentation = descriptor.getDocumentation();
            assertNotNull(documentation, "Descriptor lacks documentation for " + descriptor.getId());
            assertNotNull(documentation.getSummary(),
                    "Documentation summary missing for " + descriptor.getId());
            assertFalse(documentation.getSummary().isBlank(),
                    "Documentation summary blank for " + descriptor.getId());
            assertNotNull(documentation.getUrl(),
                    "Documentation URL missing for " + descriptor.getId());
            assertFalse(documentation.getUrl().isBlank(),
                    "Documentation URL blank for " + descriptor.getId());
        }
    }

    @Test
    public void semanticRolesMatchExpectationsForCriticalPlugins() {
        PluginDescriptor ftp = PluginMetadataCatalog.getDescriptor("ftp");
        assertNotNull(ftp, "FTP descriptor missing");
        PluginSemantics ftpSemantics = ftp.getSemantics();
        assertEquals(ftpSemantics.getRole(), PluginSemanticRole.CONTEXT_CONTAINER);
        assertTrue(ftpSemantics.allowsChildren(), "FTP container must accept child operations");
        assertTrue(ftpSemantics.getAllowedChildTypes().containsAll(List.of("ftp-get", "ftp-put", "ftp-list")),
                "FTP container should list FTP operations as allowed children");

        PluginDescriptor ftpGet = PluginMetadataCatalog.getDescriptor("ftp-get");
        assertNotNull(ftpGet, "ftp-get descriptor missing");
        PluginSemantics ftpGetSemantics = ftpGet.getSemantics();
        assertEquals(ftpGetSemantics.getRole(), PluginSemanticRole.DEPENDENT);
        assertTrue(ftpGetSemantics.getRequiredParentTypes().contains("ftp"),
                "ftp-get must require <ftp> parent");
    }

    @Test
    public void dependentPluginsExposeParentDependencies() {
        PluginDescriptor ftpGet = PluginMetadataCatalog.getDescriptor("ftp-get");
        assertNotNull(ftpGet);
        assertTrue(ftpGet.getDependencies().contains("ftp"),
                "ftp-get should declare ftp dependency");

        PluginDescriptor catchDescriptor = PluginMetadataCatalog.getDescriptor("catch");
        assertNotNull(catchDescriptor);
        assertTrue(catchDescriptor.getDependencies().contains("try"),
                "catch should declare try dependency");
    }

    @Test
    public void catalogSchemaVersionUpdated() {
        assertEquals(PluginMetadataCatalog.getSchemaVersion(), "1.1.0",
                "Schema version must reflect semantic metadata overhaul");
    }

    @Test
    public void catalogExposesJsonSchemaDocument() {
        Map<String, Object> schema = PluginMetadataCatalog.getCatalogSchema();
        assertNotNull(schema, "Catalog schema document missing");
        assertEquals(schema.get("title"), "WebHarvest Plugin Metadata Catalog");
        assertEquals(schema.get("type"), "object");
        assertTrue(schema.containsKey("properties"), "Schema must expose properties definition");
    }
}


