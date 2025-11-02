package org.webharvest.plugin.semantic;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.webharvest.definition.CorePluginsRegistrar;
import org.webharvest.plugin.PluginMetadata;
import org.webharvest.plugin.core.CatchPlugin;
import org.webharvest.plugin.core.ConfigPlugin;
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
    public void configPluginIsRootContainer() {
        PluginMetadata metadata = new ConfigPlugin().getMetadata();

        PluginSemantics semantics = PluginSemanticsResolver.resolve(ConfigPlugin.class, metadata);

        assertEquals(semantics.getRole(), PluginNodeRole.ROOT_CONTAINER);
        assertTrue(semantics.allowsChildPlugins());
        assertFalse(semantics.isExecutable());
    }

    @Test
    public void defPluginIsBodyEvaluator() {
        PluginMetadata metadata = new DefPlugin().getMetadata();

        PluginSemantics semantics = PluginSemanticsResolver.resolve(DefPlugin.class, metadata);

        assertEquals(semantics.getRole(), PluginNodeRole.BODY_EVALUATOR);
        assertTrue(semantics.allowsChildPlugins());
        assertFalse(semantics.requiresParent());
    }

    @Test
    public void loopPluginIsContextContainer() {
        PluginMetadata metadata = new LoopPlugin().getMetadata();

        PluginSemantics semantics = PluginSemanticsResolver.resolve(LoopPlugin.class, metadata);

        assertEquals(semantics.getRole(), PluginNodeRole.CONTEXT_CONTAINER);
        assertTrue(semantics.providesExecutionContext());
        assertTrue(semantics.allowsChildPlugins());
    }

    @Test
    public void catchPluginRequiresTryParent() {
        PluginMetadata metadata = new CatchPlugin().getMetadata();

        PluginSemantics semantics = PluginSemanticsResolver.resolve(CatchPlugin.class, metadata);

        assertEquals(semantics.getRole(), PluginNodeRole.DEPENDENT);
        assertTrue(semantics.requiresParent());
        assertTrue(semantics.getRequiredParentElements().contains("try"));
    }

    @Test
    public void httpHeaderPluginIsContextSpecific() {
        PluginMetadata metadata = new HttpHeaderPlugin().getMetadata();

        PluginSemantics semantics = PluginSemanticsResolver.resolve(HttpHeaderPlugin.class, metadata);

        assertEquals(semantics.getRole(), PluginNodeRole.DEPENDENT);
        assertTrue(semantics.requiresParent());
        assertEquals(semantics.getRequiredParentElements().size(), 1);
        assertTrue(semantics.getRequiredParentElements().contains("http"));
    }

}

