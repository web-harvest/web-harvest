package org.webharvest.definition;

import static org.testng.Assert.*;

import java.util.Set;
import org.testng.annotations.Test;
import org.webharvest.WHConstants;

/**
 * Public contracts for legacy {@link ElementInfo} attribute/tag parsing and templates.
 *
 * <p>Protects required-marker parsing ({@code !}), namespace attribute wildcards
 * ({@code p:*}/{@code v:*}), enumerated attribute values, and self-closing templates.</p>
 */
public class ElementInfoContractTest {

    @Test
    public void parsesRequiredTagsAttributesEnumsAndNamespaceWildcards() {
        ElementInfo info = new ElementInfo(
                "sample",
                null,
                null,
                "!body,item",
                "!name(value1;value2),optional,p:*,v:*",
                true,
                new Class[] {String.class});

        assertEquals(info.getName(), "sample");
        assertTrue(info.isInternal());
        assertFalse(info.areAllTagsAllowed());
        assertEquals(info.getTagsSet(), Set.of("body", "item"));
        assertEquals(info.getRequiredTagsSet(), Set.of("body"));
        assertEquals(info.getAttsSet(), Set.of("name", "optional", "p:*", "v:*"));
        assertEquals(info.getRequiredAttsSet(), Set.of("name"));
        assertEquals(info.getAttValues("name"), new String[] {"value1", "value2"});
        assertTrue(info.getNsAttsSet().contains(WHConstants.XMLNS_PARAM));
        assertTrue(info.getNsAttsSet().contains(WHConstants.XMLNS_VAR));
        assertEquals(info.getDependantProcessors().length, 1);
        assertNull(info.getDefinitionClass());
        assertNull(info.getProcessorClass());
    }

    @Test
    public void nullValidTagsAllowsAnyChildAndBuildsOpenTemplate() {
        ElementInfo info = new ElementInfo("open", null, null, null, "id", false, null);

        assertTrue(info.areAllTagsAllowed());
        assertTrue(info.getTagsSet().isEmpty());
        assertEquals(info.getTemplate(false), "<{0}open id=\"\"></{0}open>");
        assertEquals(info.getTemplate(true), "<{0}open></{0}open>");
    }

    @Test
    public void emptyValidTagsProducesSelfClosingTemplateWithRequiredAttributesOnly() {
        ElementInfo info = new ElementInfo(
                "leaf",
                null,
                null,
                "   ",
                "!src,alt",
                false,
                null);

        String requiredOnly = info.getTemplate(true);
        assertTrue(requiredOnly.startsWith("<{0}leaf "));
        assertTrue(requiredOnly.endsWith("/>"));
        assertTrue(requiredOnly.contains("src=\"\""));
        assertFalse(requiredOnly.contains("alt=\"\""));

        String allAtts = info.getTemplate(false);
        assertTrue(allAtts.contains("src=\"\""));
        assertTrue(allAtts.contains("alt=\"\""));
        assertTrue(allAtts.endsWith("/>"));
    }
}
