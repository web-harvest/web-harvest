package org.webharvest.plugin.metadata;

import static org.testng.Assert.*;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import org.testng.annotations.Test;
import org.webharvest.plugin.Plugin;
import org.webharvest.plugin.annotations.MasterDetailConstraint;

/**
 * Behavioral contracts for single and repeatable master/detail annotations
 * projected into the public plugin metadata catalog.
 */
public class PluginMetadataCatalogConstraintBranchContractTest {

    @MasterDetailConstraint(
            detailType = "single-detail",
            maxOccurs = 1,
            minOccurs = 1,
            sectionTitle = "Details",
            addLabel = "Add detail",
            emptyLabel = "No details",
            primaryAttribute = "name",
            secondaryAttribute = "type",
            usesBody = true)
    private interface SingleConstraintFixture extends Plugin {
    }

    @MasterDetailConstraint(detailType = "first-detail", maxOccurs = 1, minOccurs = 0)
    @MasterDetailConstraint(detailType = "many-details", maxOccurs = -1, minOccurs = 2)
    private interface RepeatableConstraintFixture extends Plugin {
    }

    private interface UnannotatedFixture extends Plugin {
    }

    @Test
    public void extractsEveryOptionalFieldForSingleConstraint() throws Exception {
        List<Map<String, Object>> constraints = extract(SingleConstraintFixture.class);

        assertEquals(constraints.size(), 1);
        Map<String, Object> constraint = constraints.get(0);
        assertEquals(constraint.get("detailType"), "single-detail");
        assertEquals(constraint.get("maxOccurs"), 1);
        assertEquals(constraint.get("minOccurs"), 1);
        assertEquals(constraint.get("sectionTitle"), "Details");
        assertEquals(constraint.get("addLabel"), "Add detail");
        assertEquals(constraint.get("emptyLabel"), "No details");
        assertEquals(constraint.get("primaryAttribute"), "name");
        assertEquals(constraint.get("secondaryAttribute"), "type");
        assertEquals(constraint.get("usesBody"), Boolean.TRUE);
    }

    @Test
    public void extractsRepeatableConstraintsAndOmitsDefaultOptionalFields() throws Exception {
        List<Map<String, Object>> constraints = extract(RepeatableConstraintFixture.class);

        assertEquals(constraints.size(), 2);
        assertEquals(constraints.get(0).get("detailType"), "first-detail");
        assertEquals(constraints.get(0).get("maxOccurs"), 1);
        assertEquals(constraints.get(0).get("minOccurs"), 0);
        assertEquals(constraints.get(1).get("detailType"), "many-details");
        assertEquals(constraints.get(1).get("maxOccurs"), -1);
        assertEquals(constraints.get(1).get("minOccurs"), 2);

        for (Map<String, Object> constraint : constraints) {
            assertFalse(constraint.containsKey("sectionTitle"));
            assertFalse(constraint.containsKey("addLabel"));
            assertFalse(constraint.containsKey("emptyLabel"));
            assertFalse(constraint.containsKey("primaryAttribute"));
            assertFalse(constraint.containsKey("secondaryAttribute"));
            assertFalse(constraint.containsKey("usesBody"));
        }
    }

    @Test
    public void returnsEmptyListWhenPluginDeclaresNoConstraints() throws Exception {
        assertTrue(extract(UnannotatedFixture.class).isEmpty());
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> extract(Class<? extends Plugin> pluginClass) throws Exception {
        Method method = PluginMetadataCatalog.class.getDeclaredMethod(
                "extractMasterDetailConstraints", Class.class);
        method.setAccessible(true);
        return (List<Map<String, Object>>) method.invoke(null, pluginClass);
    }
}
