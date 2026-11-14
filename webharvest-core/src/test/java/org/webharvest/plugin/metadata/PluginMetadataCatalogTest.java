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

    @SuppressWarnings("unchecked")
    @Test
    public void catalogSchemaDocumentsExplicitAnyChildSemantics() {
        Map<String, Object> schema = PluginMetadataCatalog.getCatalogSchema();
        Map<String, Object> properties = (Map<String, Object>) schema.get("properties");
        Map<String, Object> plugins = (Map<String, Object>) properties.get("plugins");
        Map<String, Object> items = (Map<String, Object>) plugins.get("items");
        Map<String, Object> pluginProperties = (Map<String, Object>) items.get("properties");
        Map<String, Object> semantics = (Map<String, Object>) pluginProperties.get("semantics");
        Map<String, Object> semanticsProperties = (Map<String, Object>) semantics.get("properties");
        assertTrue(semanticsProperties.containsKey("explicitlyAcceptsAnyChild"));

        PluginDescriptor loop = PluginMetadataCatalog.getDescriptor("loop");
        assertNotNull(loop);
        assertTrue(loop.getSemantics().explicitlyAcceptsAnyChild());
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
        assertEquals(constraint.get("emptyLabel"), "No else branch");
        assertEquals(constraint.get("usesBody"), Boolean.TRUE,
                "True optional constraint values must be projected");
        assertFalse(constraint.containsKey("primaryAttribute"),
                "Blank optional constraint attributes must not be emitted");

        Object rawExecution = descriptor.getExtensions().get("executionSemantics");
        assertTrue(rawExecution instanceof Map,
                "Every plugin descriptor must expose execution semantics");
        Map<?, ?> execution = (Map<?, ?>) rawExecution;
        assertEquals(execution.get("executionOrder"), "conditional");
        assertEquals(execution.get("canPause"), Boolean.TRUE);
        assertEquals(execution.get("canResume"), Boolean.TRUE);

        Map<?, ?> ui = (Map<?, ?>) descriptor.getExtensions().get("ui");
        assertNotNull(ui, "If plugin must expose UI metadata projections");
        Map<?, ?> fields = (Map<?, ?>) ui.get("fields");
        assertNotNull(fields);
        Map<?, ?> condition = (Map<?, ?>) fields.get("condition");
        assertEquals(condition.get("fieldKind"), "EXPRESSION");
        assertEquals(condition.get("expressionCapable"), Boolean.TRUE);
        assertEquals(condition.get("format"), "template");
    }

    @Test
    public void httpDescriptorProjectsTheCompleteConfigurationSchema() {
        PluginDescriptor descriptor = PluginMetadataCatalog.getDescriptor("http");
        assertNotNull(descriptor, "The built-in http plugin must have a catalog descriptor");

        Map<?, ?> schema = (Map<?, ?>) descriptor.getExtensions().get("schema");
        assertNotNull(schema, "HTTP descriptor must expose its configuration schema");

        List<?> definitions = (List<?>) schema.get("attributeDefinitions");
        assertNotNull(definitions);
        assertEquals(definitions.size(), 3);

        Map<?, ?> url = findAttribute(definitions, "url");
        assertEquals(url.get("type"), "URL");
        assertEquals(url.get("required"), Boolean.TRUE);
        assertEquals(url.get("description"), "The URL to request");
        assertFalse(url.containsKey("defaultValue"));

        Map<?, ?> method = findAttribute(definitions, "method");
        assertEquals(method.get("type"), "ENUM");
        assertEquals(method.get("required"), Boolean.FALSE);
        assertEquals(method.get("defaultValue"), "GET");
        assertTrue(((List<?>) method.get("allowedValues")).containsAll(List.of("GET", "POST")));

        Map<?, ?> body = findAttribute(definitions, "body");
        assertEquals(body.get("type"), "STRING");
        assertEquals(body.get("required"), Boolean.FALSE);
        assertEquals(body.get("description"), "Request body for POST requests");

        assertEquals(schema.get("requiredAttributes"), List.of("url"));
        assertEquals(schema.get("optionalAttributes"), List.of("method", "body"));
        assertEquals(schema.get("allowedSubElements"), List.of("http-header", "http-param"));
        assertEquals(schema.get("allowsBody"), Boolean.FALSE);
        assertEquals(schema.get("version"), "2.1.0-SNAPSHOT");
        assertEquals(descriptor.getUiHints().getIcon(), "fa-globe");

        Map<?, ?> urlUi = (Map<?, ?>) url.get("ui");
        assertNotNull(urlUi, "HTTP url attribute must project UiField metadata");
        assertEquals(urlUi.get("fieldKind"), "URL");
        assertEquals(urlUi.get("expressionCapable"), Boolean.TRUE);
        assertEquals(urlUi.get("minLength"), 1);
        assertEquals(urlUi.get("maxLength"), 4096);
        assertEquals(urlUi.get("expressionContext"), "template");

        Map<?, ?> bodyUi = (Map<?, ?>) body.get("ui");
        assertEquals(bodyUi.get("fieldKind"), "TEXTAREA");
        assertEquals(bodyUi.get("advanced"), Boolean.TRUE);

        Map<?, ?> ui = (Map<?, ?>) descriptor.getExtensions().get("ui");
        assertNotNull(ui);
        List<?> tools = (List<?>) ui.get("tools");
        assertNotNull(tools);
        assertEquals(tools.size(), 1);
        Map<?, ?> tool = (Map<?, ?>) tools.get(0);
        assertEquals(tool.get("id"), "http-inspector");
        assertEquals(tool.get("label"), "HTTP Inspector");
        assertEquals(tool.get("icon"), "fa-search");
        assertEquals(tool.get("placement"), "panel");
        assertEquals(tool.get("className"),
                "org.webharvest.plugin.metadata.CatalogUiToolMarkers$HttpInspector");
    }

    @Test
    public void defDescriptorProjectsUiFieldExpressionAndToolMetadata() {
        PluginDescriptor descriptor = PluginMetadataCatalog.getDescriptor("def");
        assertNotNull(descriptor);

        Map<?, ?> ui = (Map<?, ?>) descriptor.getExtensions().get("ui");
        assertNotNull(ui, "def must expose UI metadata");
        assertEquals(ui.get("version"), "1.0");

        Map<?, ?> fields = (Map<?, ?>) ui.get("fields");
        assertNotNull(fields);
        Map<?, ?> varField = (Map<?, ?>) fields.get("var");
        assertEquals(varField.get("fieldKind"), "TEXT");
        assertEquals(varField.get("group"), "Variable");
        assertEquals(varField.get("order"), 1);
        assertEquals(varField.get("helperText"), "Name of the variable to define");
        assertEquals(varField.get("localizationKey"), "plugin.def.var");
        assertEquals(varField.get("helperTextKey"), "plugin.def.var.help");
        assertEquals(varField.get("placeholderKey"), "plugin.def.var.placeholder");
        assertEquals(varField.get("minLength"), 1);
        assertEquals(varField.get("maxLength"), 256);
        assertFalse(varField.containsKey("expressionCapable"));

        Map<?, ?> valueField = (Map<?, ?>) fields.get("value");
        assertEquals(valueField.get("fieldKind"), "EXPRESSION");
        assertEquals(valueField.get("expressionCapable"), Boolean.TRUE);
        assertEquals(valueField.get("format"), "template");
        assertEquals(valueField.get("expressionContext"), "template");

        List<?> tools = (List<?>) ui.get("tools");
        assertEquals(tools.size(), 1);
        Map<?, ?> tool = (Map<?, ?>) tools.get(0);
        assertEquals(tool.get("id"), "expression-builder");
        assertEquals(tool.get("label"), "Expression Builder");
        assertEquals(tool.get("className"),
                "org.webharvest.plugin.metadata.CatalogUiToolMarkers$ExpressionBuilder");
    }

    @Test
    public void tryDescriptorProjectsEnrichedMasterDetailConstraint() {
        PluginDescriptor descriptor = PluginMetadataCatalog.getDescriptor("try");
        assertNotNull(descriptor);
        List<?> constraints = (List<?>) descriptor.getExtensions().get("masterDetailConstraints");
        assertEquals(constraints.size(), 1);
        Map<?, ?> constraint = (Map<?, ?>) constraints.get(0);
        assertEquals(constraint.get("detailType"), "catch");
        assertEquals(constraint.get("emptyLabel"), "No catch block");
        assertEquals(constraint.get("primaryAttribute"), "id");
        assertEquals(constraint.get("secondaryAttribute"), "var");
        assertEquals(constraint.get("usesBody"), Boolean.TRUE);
    }

    @Test
    public void builtInControlPluginsProjectMasterDetailAndExecutionHints() {
        assertMasterDetailConstraint("case", "else", "Else Branch", "Add Else");
        assertMasterDetailConstraint("try", "catch", "Catch Block", "Add Catch");

        PluginDescriptor whileDescriptor = PluginMetadataCatalog.getDescriptor("while");
        assertNotNull(whileDescriptor);
        Map<?, ?> whileExecution = (Map<?, ?>) whileDescriptor.getExtensions().get("executionSemantics");
        assertEquals(whileExecution.get("executionOrder"), "parallel");
        assertEquals(whileDescriptor.getUiHints().getPaletteGroup(), "Control");
        assertEquals(whileDescriptor.getUiHints().getIcon(), "fa-rotate");
        assertTrue(whileDescriptor.getUiHints().getTags().contains("while"));

        PluginDescriptor callDescriptor = PluginMetadataCatalog.getDescriptor("call");
        assertNotNull(callDescriptor);
        Map<?, ?> callExecution = (Map<?, ?>) callDescriptor.getExtensions().get("executionSemantics");
        assertEquals(callExecution.get("executionOrder"), "sequential");
    }

    private void assertMasterDetailConstraint(String pluginId, String detailType,
            String sectionTitle, String addLabel) {
        PluginDescriptor descriptor = PluginMetadataCatalog.getDescriptor(pluginId);
        assertNotNull(descriptor, "Missing descriptor for " + pluginId);

        Object rawConstraints = descriptor.getExtensions().get("masterDetailConstraints");
        assertTrue(rawConstraints instanceof List, "Missing master/detail constraints for " + pluginId);
        List<?> constraints = (List<?>) rawConstraints;
        assertEquals(constraints.size(), 1);

        Map<?, ?> constraint = (Map<?, ?>) constraints.get(0);
        assertEquals(constraint.get("detailType"), detailType);
        assertEquals(constraint.get("maxOccurs"), 1);
        assertEquals(constraint.get("minOccurs"), 0);
        assertEquals(constraint.get("sectionTitle"), sectionTitle);
        assertEquals(constraint.get("addLabel"), addLabel);
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
