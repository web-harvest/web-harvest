package org.webharvest.plugin.semantic;

import static org.testng.Assert.*;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;
import org.testng.annotations.Test;
import org.webharvest.plugin.PluginConfiguration;
import org.webharvest.plugin.PluginConfigurationSchema;
import org.webharvest.plugin.PluginMetadata;
import org.webharvest.plugin.core.CallPlugin;
import org.webharvest.plugin.core.ConfigPlugin;
import org.webharvest.plugin.core.TextPlugin;
import org.webharvest.plugin.exceptions.PluginValidationException;

/**
 * Resolver branches driven by crafted metadata (empty element names, body/schema,
 * required parents, declared roles).
 */
public class PluginSemanticsResolverMetadataBranchContractTest {

    private static PluginConfigurationSchema schema(boolean allowsBody, String[] allowedChildren, String[] requiredParents) {
        return new PluginConfigurationSchema() {
            @Override public AttributeDefinition[] getAttributeDefinitions() { return new AttributeDefinition[0]; }
            @Override public String[] getRequiredAttributes() { return new String[0]; }
            @Override public String[] getOptionalAttributes() { return new String[0]; }
            @Override public void validate(PluginConfiguration configuration) throws PluginValidationException { }
            @Override public AttributeDefinition getAttribute(String name) { return null; }
            @Override public String[] getAllowedSubElements() { return allowedChildren; }
            @Override public String[] getRequiredSubElements() { return new String[0]; }
            @Override public boolean allowsBody() { return allowsBody; }
            @Override public String getVersion() { return "1"; }
            @Override public String[] getRequiredParents() { return requiredParents; }
        };
    }

    private static PluginMetadata metadata(String elementName, PluginConfigurationSchema schema,
                                           Set<PluginNodeRole> roles) {
        return new PluginMetadata() {
            @Override public String getId() { return elementName == null || elementName.isEmpty() ? "anon" : elementName; }
            @Override public String getName() { return "Meta"; }
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
        };
    }

    @Test
    public void emptyElementNameFallsBackToSimpleClassName() {
        PluginSemantics semantics = PluginSemanticsResolver.resolve(
                CallPlugin.class, metadata("", null, Collections.emptySet()));
        assertNotNull(semantics);
        assertTrue(semantics.hasRole(PluginNodeRole.LEAF));
    }

    @Test
    public void schemaAllowsBodyPromotesBodyEvaluatorRole() {
        PluginSemantics semantics = PluginSemanticsResolver.resolve(
                TextPlugin.class,
                metadata("bodyish", schema(true, new String[0], new String[0]), Collections.emptySet()));
        assertTrue(semantics.hasRole(PluginNodeRole.BODY_EVALUATOR));
        assertTrue(semantics.allowsTextContent());
        assertTrue(semantics.allowsChildPlugins());
    }

    @Test
    public void schemaRequiredParentsMarkDependent() {
        PluginSemantics semantics = PluginSemanticsResolver.resolve(
                CallPlugin.class,
                metadata("childish", schema(false, new String[0], new String[] {"parent"}), Collections.emptySet()));
        assertTrue(semantics.requiresParent());
        assertTrue(semantics.hasRole(PluginNodeRole.DEPENDENT));
        assertTrue(semantics.getRequiredParentElements().contains("parent"));
    }

    @Test
    public void allowedChildrenPromoteContextContainer() {
        PluginSemantics semantics = PluginSemanticsResolver.resolve(
                CallPlugin.class,
                metadata("master", schema(false, new String[] {"child-a", "child-b"}, new String[0]),
                        Collections.emptySet()));
        assertTrue(semantics.hasRole(PluginNodeRole.CONTEXT_CONTAINER));
        assertTrue(semantics.allowsChildPlugins());
        assertTrue(semantics.getAllowedChildElements().contains("child-a"));
    }

    @Test
    public void declaredDependentRoleIsPreserved() {
        PluginSemantics semantics = PluginSemanticsResolver.resolve(
                CallPlugin.class,
                metadata("dep", null, EnumSet.of(PluginNodeRole.DEPENDENT)));
        // Role is preserved even when no concrete parent element names are declared.
        assertTrue(semantics.hasRole(PluginNodeRole.DEPENDENT));
        assertFalse(semantics.requiresParent(),
                "requiresParent() is true only when requiredParentElements is non-empty");
    }

    @Test
    public void configPluginMetadataResolvesWithoutError() {
        PluginSemantics semantics = PluginSemanticsResolver.resolve(
                ConfigPlugin.class, new ConfigPlugin().getMetadata());
        assertNotNull(semantics);
        assertTrue(semantics.allowsChildPlugins() || semantics.hasRole(PluginNodeRole.ROOT_CONTAINER)
                || semantics.hasRole(PluginNodeRole.CONTEXT_CONTAINER)
                || semantics.hasRole(PluginNodeRole.LEAF));
    }
}
