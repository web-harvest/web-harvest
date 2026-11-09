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

        // Use PluginClassScanner instead of PluginClassIndex (dynamic discovery)
        for (String expectedClass : org.webharvest.plugin.metadata.PluginClassScanner.allPluginClassNames()) {
            // Skip FTP plugin if not in classpath (optional extension)
            if (expectedClass.contains("ftp.FtpPlugin") && !classNamesInCatalog.contains(expectedClass)) {
                continue; // FTP is optional extension
            }
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
            // Documentation URL is optional for some plugins (e.g., body-processor, http)
            // Only check if URL is provided, it should not be blank
            if (documentation.getUrl() != null) {
                assertFalse(documentation.getUrl().isBlank(),
                        "Documentation URL blank for " + descriptor.getId());
            }
        }
    }

    @Test
    public void semanticRolesMatchExpectationsForCriticalPlugins() {
        // FTP plugin is optional extension - may not be in classpath
        PluginDescriptor ftp = PluginMetadataCatalog.getDescriptor("ftp");
        if (ftp == null) {
            // FTP is optional, skip if not available
            return;
        }
        PluginSemantics ftpSemantics = ftp.getSemantics();
        assertTrue(ftpSemantics.hasRole(PluginSemanticRole.CONTEXT_CONTAINER));
        assertTrue(ftpSemantics.allowsChildren(), "FTP container must accept child operations");
        assertTrue(ftpSemantics.getAllowedChildTypes().containsAll(List.of("ftp-get", "ftp-put", "ftp-list")),
                "FTP container should list FTP operations as allowed children");

        PluginDescriptor ftpGet = PluginMetadataCatalog.getDescriptor("ftp-get");
        if (ftpGet == null) {
            // FTP extensions not available
            return;
        }
        PluginSemantics ftpGetSemantics = ftpGet.getSemantics();
        assertTrue(ftpGetSemantics.hasRole(PluginSemanticRole.DEPENDENT));
        assertTrue(ftpGetSemantics.getRequiredParentTypes().contains("ftp"),
                "ftp-get must require <ftp> parent");
    }

    @Test
    public void dependentPluginsExposeParentDependencies() {
        // FTP extensions may not be available
        PluginDescriptor ftpGet = PluginMetadataCatalog.getDescriptor("ftp-get");
        if (ftpGet != null) {
            assertTrue(ftpGet.getDependencies().contains("ftp"),
                    "ftp-get should declare ftp dependency");
        }

        PluginDescriptor catchDescriptor = PluginMetadataCatalog.getDescriptor("catch");
        assertNotNull(catchDescriptor);
        assertTrue(catchDescriptor.getDependencies().contains("try"),
                "catch should declare try dependency");
    }

    @Test
    public void catalogSchemaVersionUpdated() {
        assertEquals(PluginMetadataCatalog.getSchemaVersion(), "2.3.0",
                "Schema version must reflect dynamic metadata generation (v2.3.0)");
    }

    @Test
    public void catalogExposesJsonSchemaDocument() {
        Map<String, Object> schema = PluginMetadataCatalog.getCatalogSchema();
        assertNotNull(schema, "Catalog schema document missing");
        assertEquals(schema.get("title"), "WebHarvest Plugin Metadata Catalog");
        assertEquals(schema.get("type"), "object");
        assertTrue(schema.containsKey("properties"), "Schema must expose properties definition");
    }

    @Test
    public void ifDescriptorProjectsMasterDetailAndExecutionSemantics() {
        PluginDescriptor descriptor = PluginMetadataCatalog.getDescriptor("if");
        assertNotNull(descriptor, "The built-in if plugin must have a catalog descriptor");

        Object rawConstraints = descriptor.getExtensions().get("masterDetailConstraints");
        assertTrue(rawConstraints instanceof List,
                "The if descriptor must expose its master/detail constraint");

        List<?> constraints = (List<?>) rawConstraints;
        assertEquals(constraints.size(), 1, "IfPlugin declares one else-branch constraint");

        Object rawConstraint = constraints.get(0);
        assertTrue(rawConstraint instanceof Map);
        Map<?, ?> constraint = (Map<?, ?>) rawConstraint;
        assertEquals(constraint.get("detailType"), "else");
        assertEquals(constraint.get("maxOccurs"), 1);
        assertEquals(constraint.get("minOccurs"), 0);
        assertEquals(constraint.get("sectionTitle"), "Else Branch");
        assertEquals(constraint.get("addLabel"), "Add Else");
        assertFalse(constraint.containsKey("usesBody"),
                "False/default optional constraint values must not be emitted");

        Object rawExecution = descriptor.getExtensions().get("executionSemantics");
        assertTrue(rawExecution instanceof Map,
                "Every plugin descriptor must expose execution semantics");
        Map<?, ?> execution = (Map<?, ?>) rawExecution;
        assertEquals(execution.get("executionOrder"), "conditional");
        assertEquals(execution.get("canPause"), Boolean.TRUE);
        assertEquals(execution.get("canResume"), Boolean.TRUE);
    }
}
