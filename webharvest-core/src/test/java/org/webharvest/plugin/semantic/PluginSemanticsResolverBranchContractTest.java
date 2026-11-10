package org.webharvest.plugin.semantic;

import static org.testng.Assert.*;

import org.testng.annotations.Test;
import org.webharvest.plugin.core.CallPlugin;
import org.webharvest.plugin.core.ConstantPlugin;
import org.webharvest.plugin.core.GetVarPlugin;
import org.webharvest.plugin.core.LoopPlugin;
import org.webharvest.plugin.core.XQParamPlugin;

/**
 * Additional resolver branches beyond {@link PluginSemanticsResolverTest}.
 */
public class PluginSemanticsResolverBranchContractTest {

    @Test
    public void nullMetadataFallsBackToDefaultLeafSemantics() {
        PluginSemantics semantics = PluginSemanticsResolver.resolve(CallPlugin.class, null);
        assertNotNull(semantics);
        assertTrue(semantics.hasRole(PluginNodeRole.LEAF));
        assertFalse(semantics.allowsChildPlugins());
    }

    @Test
    public void loopPluginAllowsArbitraryChildrenWhenMetadataSaysSo() {
        PluginSemantics semantics = PluginSemanticsResolver.resolve(
                LoopPlugin.class, new LoopPlugin().getMetadata());

        assertTrue(semantics.allowsAnyChild());
        assertTrue(semantics.hasRole(PluginNodeRole.CONTEXT_CONTAINER));
        assertTrue(semantics.allowsChildPlugins());
        assertTrue(semantics.providesExecutionContext());
    }

    @Test
    public void constantPluginIsLeafWithoutChildren() {
        PluginSemantics semantics = PluginSemanticsResolver.resolve(
                ConstantPlugin.class, new ConstantPlugin().getMetadata());

        assertTrue(semantics.hasRole(PluginNodeRole.LEAF));
        assertFalse(semantics.allowsChildPlugins());
    }

    @Test
    public void callPluginRestrictsChildrenToCallParam() {
        PluginSemantics semantics = PluginSemanticsResolver.resolve(
                CallPlugin.class, new CallPlugin().getMetadata());

        assertTrue(semantics.hasRole(PluginNodeRole.BODY_EVALUATOR));
        assertFalse(semantics.allowsAnyChild());
        assertNotNull(semantics.getAllowedChildElements());
        assertTrue(semantics.getAllowedChildElements().contains("call-param"));
    }

    @Test
    public void getVarPluginDoesNotAllowChildren() {
        PluginSemantics semantics = PluginSemanticsResolver.resolve(
                GetVarPlugin.class, new GetVarPlugin().getMetadata());

        assertFalse(semantics.allowsChildPlugins());
        assertFalse(semantics.allowsAnyChild());
    }

    @Test
    public void xqParamRequiresParentAndIsDependent() {
        PluginSemantics semantics = PluginSemanticsResolver.resolve(
                XQParamPlugin.class, new XQParamPlugin().getMetadata());

        assertTrue(semantics.requiresParent());
        assertTrue(semantics.hasRole(PluginNodeRole.DEPENDENT));
        assertNotNull(semantics.getRequiredParentElements());
        assertTrue(semantics.getRequiredParentElements().contains("xquery"));
    }
}
