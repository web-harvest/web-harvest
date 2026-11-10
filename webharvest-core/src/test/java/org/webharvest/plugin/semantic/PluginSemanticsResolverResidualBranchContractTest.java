package org.webharvest.plugin.semantic;

import static org.testng.Assert.*;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;
import org.testng.annotations.Test;
import org.webharvest.plugin.AccessibilityMetadata;
import org.webharvest.plugin.InputPort;
import org.webharvest.plugin.KeyboardShortcut;
import org.webharvest.plugin.LinkedServiceMetadata;
import org.webharvest.plugin.OutputPort;
import org.webharvest.plugin.ParentChildRelationship;
import org.webharvest.plugin.PluginConfigurationSchema;
import org.webharvest.plugin.PortType;
import org.webharvest.plugin.VisualPluginMetadata;
import org.webharvest.plugin.core.CallPlugin;
import org.webharvest.plugin.core.ConfigPlugin;

/**
 * Residual resolver contracts for required parent relationships and ROOT_CONTAINER
 * execution-context projection.
 */
public class PluginSemanticsResolverResidualBranchContractTest {

    @Test
    public void requiredParentRelationshipWithNullParentNameStillMarksDependent() {
        VisualPluginMetadata metadata = visual(
                "detail",
                null,
                new ParentChildRelationship(null, true, "Required parent"),
                false,
                null,
                EnumSet.noneOf(PluginNodeRole.class));

        PluginSemantics semantics = PluginSemanticsResolver.resolve(CallPlugin.class, metadata);

        assertTrue(semantics.hasRole(PluginNodeRole.DEPENDENT));
        assertTrue(semantics.getRequiredParentElements() == null
                        || semantics.getRequiredParentElements().isEmpty(),
                "null parent element name must not invent a concrete parent id");
    }

    @Test
    public void rootContainerRoleProvidesExecutionContextWithoutContextContainer() {
        VisualPluginMetadata metadata = visual(
                "rootish",
                null,
                null,
                false,
                null,
                EnumSet.of(PluginNodeRole.ROOT_CONTAINER));

        PluginSemantics semantics = PluginSemanticsResolver.resolve(ConfigPlugin.class, metadata);

        assertTrue(semantics.hasRole(PluginNodeRole.ROOT_CONTAINER));
        assertTrue(semantics.allowsChildPlugins());
        assertTrue(semantics.providesExecutionContext());
        assertFalse(semantics.isExecutable());
    }

    @Test
    public void visualAllowedChildrenMergeWithSchemaChildren() {
        PluginConfigurationSchema schema = new PluginConfigurationSchema() {
            @Override public AttributeDefinition[] getAttributeDefinitions() { return new AttributeDefinition[0]; }
            @Override public String[] getRequiredAttributes() { return new String[0]; }
            @Override public String[] getOptionalAttributes() { return new String[0]; }
            @Override public void validate(org.webharvest.plugin.PluginConfiguration configuration) { }
            @Override public AttributeDefinition getAttribute(String name) { return null; }
            @Override public String[] getAllowedSubElements() { return new String[] {"schema-child"}; }
            @Override public String[] getRequiredSubElements() { return new String[0]; }
            @Override public boolean allowsBody() { return false; }
            @Override public String getVersion() { return "1"; }
            @Override public String[] getRequiredParents() { return new String[0]; }
        };
        VisualPluginMetadata metadata = visual(
                "merged",
                schema,
                null,
                false,
                new String[] {"visual-child"},
                EnumSet.noneOf(PluginNodeRole.class));

        PluginSemantics semantics = PluginSemanticsResolver.resolve(CallPlugin.class, metadata);
        assertTrue(semantics.getAllowedChildElements().contains("schema-child"));
        assertTrue(semantics.getAllowedChildElements().contains("visual-child"));
        assertTrue(semantics.hasRole(PluginNodeRole.CONTEXT_CONTAINER));
    }

    private static VisualPluginMetadata visual(
            String elementName,
            PluginConfigurationSchema schema,
            ParentChildRelationship parentRelationship,
            boolean acceptsArbitraryChildren,
            String[] allowedChildPlugins,
            Set<PluginNodeRole> roles) {
        return new VisualPluginMetadata() {
            @Override public String getId() { return elementName; }
            @Override public String getName() { return elementName; }
            @Override public String getDescription() { return "d"; }
            @Override public String getVersion() { return "1"; }
            @Override public String getAuthor() { return "test"; }
            @Override public String getCategory() { return "Core"; }
            @Override public String[] getTags() { return new String[0]; }
            @Override public String[] getDependencies() { return new String[0]; }
            @Override public String getElementName() { return elementName; }
            @Override public String getNamespace() { return "n"; }
            @Override public boolean isCorePlugin() { return true; }
            @Override public String getClassName() { return CallPlugin.class.getName(); }
            @Override public PluginConfigurationSchema getConfigurationSchema() { return schema; }
            @Override public Set<PluginNodeRole> getDeclaredRoles() {
                return roles == null ? Collections.emptySet() : roles;
            }
            @Override public String getIcon() { return "icon"; }
            @Override public String getColor() { return "#000000"; }
            @Override public String getGroup() { return "Core"; }
            @Override public int getDefaultWidth() { return 100; }
            @Override public int getDefaultHeight() { return 40; }
            @Override public InputPort[] getInputPorts() { return new InputPort[0]; }
            @Override public OutputPort[] getOutputPorts() { return new OutputPort[0]; }
            @Override public ParentChildRelationship getParentRelationship() { return parentRelationship; }
            @Override public String[] getAllowedChildPlugins() { return allowedChildPlugins; }
            @Override public boolean acceptsArbitraryChildren() { return acceptsArbitraryChildren; }
            @Override public PortType getPortType() { return PortType.MIXED; }
            @Override public String getTooltip() { return "tooltip"; }
            @Override public AccessibilityMetadata getAccessibility() { return null; }
            @Override public KeyboardShortcut[] getKeyboardShortcuts() { return new KeyboardShortcut[0]; }
            @Override public LinkedServiceMetadata[] getLinkedServices() { return new LinkedServiceMetadata[0]; }
        };
    }
}
