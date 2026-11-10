package org.webharvest.runtime.tracking;

import static org.testng.Assert.*;

import org.testng.annotations.Test;

/**
 * Contracts for {@link ResourceType} enum helpers used by token tracking.
 */
public class ResourceTypeBranchContractTest {

    @Test
    public void valuesExposeStableNamesUnitsAndDescriptions() {
        assertTrue(ResourceType.values().length >= 4);
        for (ResourceType type : ResourceType.values()) {
            assertNotNull(type.name());
            assertSame(ResourceType.valueOf(type.name()), type);
            assertNotNull(type.getMetricName());
            assertNotNull(type.getUnit());
            assertNotNull(type.getDescription());
            assertTrue(type.getDescription().length() > 0);
        }
        assertEquals(ResourceType.HTTP_REQUEST.getDescription(), "Number of HTTP requests");
        assertEquals(ResourceType.HTTP_BYTES.getDescription(), "Bytes downloaded via HTTP");
        assertEquals(ResourceType.CPU_TIME.getDescription(), "CPU time consumed");
        assertEquals(ResourceType.MEMORY_PEAK.getDescription(), "Peak memory usage");
    }
}
