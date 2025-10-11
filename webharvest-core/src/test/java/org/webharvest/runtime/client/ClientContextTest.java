package org.webharvest.runtime.client;

import org.testng.annotations.Test;
import java.util.HashMap;
import java.util.Map;

import static org.testng.Assert.*;

/**
 * Unit tests for {@link ClientContext}.
 * 
 * @author Robert Bala
 * @since 2.2.0
 */
public class ClientContextTest {
    
    @Test(groups = {"unit", "client"})
    public void testBuilder_MinimalContext() {
        ClientContext context = ClientContext.builder()
            .clientId("test-client")
            .build();
        
        assertEquals(context.getClientId(), "test-client");
        assertNotNull(context.getMetadata());
        assertTrue(context.getMetadata().isEmpty());
    }
    
    @Test(groups = {"unit", "client"})
    public void testBuilder_WithMetadata() {
        ClientContext context = ClientContext.builder()
            .clientId("test-client")
            .metadata("tier", "premium")
            .metadata("region", "us-east")
            .build();
        
        assertEquals(context.getClientId(), "test-client");
        assertEquals(context.getMetadata().size(), 2);
        assertEquals(context.getMetadata("tier"), "premium");
        assertEquals(context.getMetadata("region"), "us-east");
    }
    
    @Test(groups = {"unit", "client"})
    public void testBuilder_WithMetadataMap() {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("tier", "premium");
        metadata.put("accountId", 12345);
        
        ClientContext context = ClientContext.builder()
            .clientId("test-client")
            .metadata(metadata)
            .build();
        
        assertEquals(context.getMetadata().size(), 2);
        assertEquals(context.getMetadata("tier"), "premium");
        assertEquals(context.getMetadata("accountId"), 12345);
    }
    
    @Test(groups = {"unit", "client"})
    public void testBuilder_NullClientId() {
        ClientContext context = ClientContext.builder()
            .clientId(null)
            .metadata("key", "value")
            .build();
        
        assertNull(context.getClientId());
        assertEquals(context.getMetadata().size(), 1);
    }
    
    @Test(groups = {"unit", "client"}, expectedExceptions = IllegalArgumentException.class)
    public void testBuilder_NullMetadataKey_ThrowsException() {
        ClientContext.builder()
            .metadata(null, "value")
            .build();
    }
    
    @Test(groups = {"unit", "client"})
    public void testBuilder_NullMetadataValue() {
        ClientContext context = ClientContext.builder()
            .clientId("test-client")
            .metadata("key", null)
            .build();
        
        assertTrue(context.hasMetadata("key"));
        assertNull(context.getMetadata("key"));
    }
    
    @Test(groups = {"unit", "client"})
    public void testOf_SimpleFactory() {
        ClientContext context = ClientContext.of("test-client");
        
        assertEquals(context.getClientId(), "test-client");
        assertTrue(context.getMetadata().isEmpty());
    }
    
    @Test(groups = {"unit", "client"})
    public void testGetMetadataString() {
        ClientContext context = ClientContext.builder()
            .clientId("test-client")
            .metadata("tier", "premium")
            .metadata("count", 123)
            .build();
        
        assertEquals(context.getMetadataString("tier"), "premium");
        assertEquals(context.getMetadataString("count"), "123");
        assertNull(context.getMetadataString("nonexistent"));
    }
    
    @Test(groups = {"unit", "client"})
    public void testHasMetadata() {
        ClientContext context = ClientContext.builder()
            .clientId("test-client")
            .metadata("tier", "premium")
            .build();
        
        assertTrue(context.hasMetadata("tier"));
        assertFalse(context.hasMetadata("nonexistent"));
    }
    
    @Test(groups = {"unit", "client"})
    public void testMetadataImmutable() {
        ClientContext context = ClientContext.builder()
            .clientId("test-client")
            .metadata("tier", "premium")
            .build();
        
        Map<String, Object> metadata = context.getMetadata();
        
        try {
            metadata.put("newKey", "newValue");
            fail("Metadata map should be unmodifiable");
        } catch (UnsupportedOperationException e) {
            // Expected
        }
    }
    
    @Test(groups = {"unit", "client"})
    public void testMetadataDefensiveCopy() {
        Map<String, Object> original = new HashMap<>();
        original.put("key1", "value1");
        
        ClientContext context = ClientContext.builder()
            .clientId("test-client")
            .metadata(original)
            .build();
        
        // Modify original map
        original.put("key2", "value2");
        
        // Context should not be affected
        assertEquals(context.getMetadata().size(), 1);
        assertFalse(context.hasMetadata("key2"));
    }
    
    @Test(groups = {"unit", "client"})
    public void testEquals_SameContent() {
        ClientContext context1 = ClientContext.builder()
            .clientId("test-client")
            .metadata("tier", "premium")
            .build();
        
        ClientContext context2 = ClientContext.builder()
            .clientId("test-client")
            .metadata("tier", "premium")
            .build();
        
        assertEquals(context1, context2);
        assertEquals(context1.hashCode(), context2.hashCode());
    }
    
    @Test(groups = {"unit", "client"})
    public void testEquals_DifferentClientId() {
        ClientContext context1 = ClientContext.builder()
            .clientId("client1")
            .build();
        
        ClientContext context2 = ClientContext.builder()
            .clientId("client2")
            .build();
        
        assertFalse(context1.equals(context2), "Different client IDs should not be equal");
    }
    
    @Test(groups = {"unit", "client"})
    public void testEquals_DifferentMetadata() {
        ClientContext context1 = ClientContext.builder()
            .clientId("test-client")
            .metadata("tier", "premium")
            .build();
        
        ClientContext context2 = ClientContext.builder()
            .clientId("test-client")
            .metadata("tier", "basic")
            .build();
        
        assertFalse(context1.equals(context2), "Different metadata should not be equal");
    }
    
    @Test(groups = {"unit", "client"})
    public void testToString_ContainsInfo() {
        ClientContext context = ClientContext.builder()
            .clientId("test-client")
            .metadata("tier", "premium")
            .build();
        
        String str = context.toString();
        assertNotNull(str);
        assertTrue(str.contains("test-client"));
        assertTrue(str.contains("tier"));
    }
    
    @Test(groups = {"unit", "client"})
    public void testBuilder_Chaining() {
        ClientContext context = ClientContext.builder()
            .clientId("test-client")
            .metadata("key1", "value1")
            .metadata("key2", "value2")
            .metadata("key3", "value3")
            .build();
        
        assertEquals(context.getMetadata().size(), 3);
    }
    
    @Test(groups = {"unit", "client"})
    public void testBuilder_MetadataOverwrite() {
        ClientContext context = ClientContext.builder()
            .clientId("test-client")
            .metadata("tier", "basic")
            .metadata("tier", "premium")  // Overwrite
            .build();
        
        assertEquals(context.getMetadata("tier"), "premium");
    }
}

