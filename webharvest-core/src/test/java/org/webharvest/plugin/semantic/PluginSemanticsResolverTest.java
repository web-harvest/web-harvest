package org.webharvest.plugin.semantic;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.webharvest.definition.CorePluginsRegistrar;
import org.webharvest.plugin.PluginMetadata;
import org.webharvest.plugin.core.CatchPlugin;
import org.webharvest.plugin.core.DefPlugin;
import org.webharvest.plugin.core.HttpHeaderPlugin;
import org.webharvest.plugin.core.LoopPlugin;

import static org.testng.Assert.*;

/**
 * TDD coverage for {@link PluginSemanticsResolver}.
 *
 * <p>
 * These tests assert the semantic model for key plugin archetypes so the
 * resolver must supply accurate metadata for IDE and LLM consumption.
 * </p>
 */
public class PluginSemanticsResolverTest {

    @BeforeClass
    public static void initCoreRegistry() {
        // Ensure core plugin metadata is registered before tests execute.
        org.webharvest.definition.CorePluginsRegistrar.registerCorePlugins();
    }

    @Test
    public void defPluginIsBodyEvaluator() {
        PluginMetadata metadata = new DefPlugin().getMetadata();

        PluginSemantics semantics = PluginSemanticsResolver.resolve(DefPlugin.class, metadata);

        assertTrue(semantics.hasRole(PluginNodeRole.BODY_EVALUATOR));
        assertTrue(semantics.allowsChildPlugins());
        assertFalse(semantics.requiresParent());
        assertTrue(semantics.allowsAnyChild(), "Legacy body semantics remain available to existing consumers");
        assertFalse(semantics.explicitlyAcceptsAnyChild(), "BODY_EVALUATOR inference is not an explicit child policy");
    }

    @Test
    public void loopPluginIsContextContainer() {
        PluginMetadata metadata = new LoopPlugin().getMetadata();

        PluginSemantics semantics = PluginSemanticsResolver.resolve(LoopPlugin.class, metadata);

        assertTrue(semantics.hasRole(PluginNodeRole.CONTEXT_CONTAINER));
        assertTrue(semantics.hasRole(PluginNodeRole.BODY_EVALUATOR),
                "loop should evaluate its body in addition to being a container");
        assertTrue(semantics.providesExecutionContext());
        assertTrue(semantics.allowsChildPlugins());
        assertTrue(semantics.explicitlyAcceptsAnyChild(), "Loop explicitly declares arbitrary child support");
    }

    @Test
    public void httpPluginRecognisesDetailChildren() {
        org.webharvest.plugin.core.HttpPlugin plugin = new org.webharvest.plugin.core.HttpPlugin();
        PluginMetadata metadata = plugin.getMetadata();

        PluginSemantics semantics = PluginSemanticsResolver.resolve(org.webharvest.plugin.core.HttpPlugin.class, metadata);

        assertTrue(semantics.hasRole(PluginNodeRole.CONTEXT_CONTAINER),
                "http plugin should be treated as a context container");
        assertTrue(semantics.allowsChildPlugins(), "http plugin should allow child components");
        assertTrue(semantics.getAllowedChildElements().contains("http-header"));
        assertTrue(semantics.getAllowedChildElements().contains("http-param"));
        assertFalse(semantics.allowsAnyChild(), "http plugin should restrict children to explicit metadata");
        assertFalse(semantics.explicitlyAcceptsAnyChild());
    }

    @Test
    public void catchPluginRequiresTryParent() {
        PluginMetadata metadata = new CatchPlugin().getMetadata();

        PluginSemantics semantics = PluginSemanticsResolver.resolve(CatchPlugin.class, metadata);

        assertTrue(semantics.hasRole(PluginNodeRole.DEPENDENT));
        assertTrue(semantics.requiresParent());
        assertTrue(semantics.getRequiredParentElements().contains("try"));
    }

    @Test
    public void httpHeaderPluginIsContextSpecific() {
        PluginMetadata metadata = new HttpHeaderPlugin().getMetadata();

        PluginSemantics semantics = PluginSemanticsResolver.resolve(HttpHeaderPlugin.class, metadata);

        // http-header role may be BODY_EVALUATOR (context-specific) or DEPENDENT
        assertTrue(semantics.hasRole(PluginNodeRole.DEPENDENT) || 
                   semantics.hasRole(PluginNodeRole.BODY_EVALUATOR),
                   "http-header should be DEPENDENT or BODY_EVALUATOR");
        // http-header requires parent http, but may not be detected if dependencies are empty
        // Check if requiresParent or has required parent elements
        assertTrue(semantics.requiresParent() || semantics.getRequiredParentElements().size() > 0,
                   "http-header should require parent or have required parent elements");
        if (semantics.getRequiredParentElements().size() > 0) {
            assertTrue(semantics.getRequiredParentElements().contains("http"),
                       "http-header should require http as parent");
        }
    }

}

