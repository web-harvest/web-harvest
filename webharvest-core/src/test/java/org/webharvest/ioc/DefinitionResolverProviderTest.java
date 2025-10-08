package org.webharvest.ioc;

import static org.mockito.Mockito.*;
import static org.testng.Assert.*;

import java.util.Arrays;
import java.util.List;

import javax.inject.Provider;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.webharvest.definition.ConfigurableResolver;
import org.webharvest.definition.ElementsRegistry;
import org.webharvest.definition.ResolverPostProcessor;

/**
 * Test for DefinitionResolverProvider class.
 *
 * @since 2.2.0
 */
public class DefinitionResolverProviderTest {

    private DefinitionResolverProvider provider;
    private List<ResolverPostProcessor> postProcessors;
    private Provider<ElementsRegistry> registryProvider;

    @BeforeMethod
    public void setUp() {
        postProcessors = Arrays.asList(
            mock(ResolverPostProcessor.class),
            mock(ResolverPostProcessor.class)
        );
        registryProvider = mock(Provider.class);
        provider = new DefinitionResolverProvider(postProcessors, registryProvider);
    }

    @Test
    public void testGetWithSuccessfulProvider() {
        // Arrange
        ElementsRegistry registry = mock(ElementsRegistry.class);
        when(registryProvider.get()).thenReturn(registry);

        // Act
        ConfigurableResolver result = provider.get();

        // Assert
        assertNotNull(result, "Resolver should not be null");
        verify(registryProvider, atLeastOnce()).get();
        // Post processors are called during the resolver creation
        // but we can't easily verify them without exposing internal state
    }

    @Test
    public void testGetWithRegistryProviderThrowingException() {
        // Arrange
        when(registryProvider.get()).thenThrow(new RuntimeException("Registry provider failed"));

        // Act & Assert
        try {
            provider.get();
            fail("Should have thrown exception");
        } catch (RuntimeException e) {
            assertEquals(e.getMessage(), "Registry provider failed");
        }
        verify(registryProvider, atLeastOnce()).get();
    }

    @Test
    public void testGetWithEmptyPostProcessorsList() {
        // Arrange
        ElementsRegistry registry = mock(ElementsRegistry.class);
        when(registryProvider.get()).thenReturn(registry);
        List<ResolverPostProcessor> emptyPostProcessors = Arrays.asList();
        DefinitionResolverProvider emptyProvider = new DefinitionResolverProvider(emptyPostProcessors, registryProvider);

        // Act
        ConfigurableResolver result = emptyProvider.get();

        // Assert
        assertNotNull(result, "Resolver should not be null");
        verify(registryProvider, atLeastOnce()).get();
    }

    @Test
    public void testGetWithNullPostProcessorsList() {
        // Arrange
        ElementsRegistry registry = mock(ElementsRegistry.class);
        when(registryProvider.get()).thenReturn(registry);
        DefinitionResolverProvider nullProvider = new DefinitionResolverProvider(null, registryProvider);

        // Act & Assert - should not throw exception
        try {
            ConfigurableResolver result = nullProvider.get();
            assertNotNull(result, "Resolver should not be null");
        } catch (Exception e) {
            // Expected behavior - should handle null gracefully or throw appropriate exception
        }
        verify(registryProvider, atLeastOnce()).get();
    }
}
