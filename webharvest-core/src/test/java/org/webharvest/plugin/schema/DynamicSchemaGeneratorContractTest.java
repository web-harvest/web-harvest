package org.webharvest.plugin.schema;

import static org.testng.Assert.*;

import java.util.List;
import org.testng.annotations.Test;
import org.webharvest.plugin.core.HttpPlugin;

/**
 * Smoke contract for legacy {@link DynamicSchemaGenerator}.
 *
 * <p>The generator is no longer on the primary registration path, but its public
 * API must still emit a usable XSD document for registered plugin metadata.</p>
 */
public class DynamicSchemaGeneratorContractTest {

    @Test
    public void generateSchemaIncludesHttpElementForCoreNamespace() {
        DynamicSchemaGenerator generator = new DynamicSchemaGenerator();
        String xsd = generator.generateSchema(
                List.of(new HttpPlugin()),
                "http://org.webharvest/schema/2.1/core");

        assertNotNull(xsd);
        assertFalse(xsd.isBlank());
        assertTrue(xsd.contains("xs:schema"), "Generated document must be an XSD schema");
        assertTrue(xsd.contains("http"), "Generated schema must include the http plugin element");
    }

    @Test
    public void generateSchemaHandlesEmptyPluginCollection() {
        DynamicSchemaGenerator generator = new DynamicSchemaGenerator();
        String xsd = generator.generateSchema(
                List.of(),
                "http://org.webharvest/schema/2.1/core");
        assertNotNull(xsd);
        assertTrue(xsd.contains("xs:schema"));
    }
}
