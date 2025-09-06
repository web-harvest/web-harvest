package org.webharvest.definition;

import static org.testng.AssertJUnit.assertFalse;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.unitils.UnitilsTestNG;
import org.unitils.mock.Mock;
import org.webharvest.exception.PluginException;

import javax.inject.Provider;
import java.util.Set;

public class AbstractRefreshableResolverTest extends UnitilsTestNG {

    private Mock<ResolverPostProcessor> postProcessor;
    private Mock<ElementsRegistry> elementsRegistry;
    private AbstractRefreshableResolver resolver;

    @BeforeMethod
    public void setUp() {
        resolver = new MockAbstractConfigurableResolver(new Provider<ElementsRegistry>() {
            @Override
            public ElementsRegistry get() {
                return new ElementsRegistryImpl();
            }
        });
    }

    @Test
    public void postProcessorExecutedOnRefresh() {
        resolver.addPostProcessor(postProcessor.getMock());

        resolver.refresh();

        postProcessor.assertInvoked().postProcess(resolver);
        postProcessor.assertNotInvoked().postProcess(null);
    }

    @Test
    public void createsNewElementsRegistryOnRefresh() {
        final ElementsRegistry previous = resolver.getElementsRegistry();
        resolver.refresh();
        final ElementsRegistry current = resolver.getElementsRegistry();

        assertFalse("New instance of registry expected", previous == current);
    }

    private class MockAbstractConfigurableResolver
        extends AbstractRefreshableResolver {

        public MockAbstractConfigurableResolver(Provider<ElementsRegistry> registryProvider) {
            super(registryProvider);
        }

        @Override
        public void registerPlugin(final ElementInfo elementInfo,
                final String namespace) {
            throw new UnsupportedOperationException("TEST MOCK");
        }

        @Override
        public void registerPlugin(Class pluginClass, String uri) {
            throw new UnsupportedOperationException("TEST MOCK");
        }

        @Override
        public void registerPlugin(String className, String uri) throws PluginException {
            throw new UnsupportedOperationException("TEST MOCK");
        }

        @Override
        public void unregisterPlugin(String className, String uri) {
            throw new UnsupportedOperationException("TEST MOCK");
        }

        @Override
        public boolean isPluginRegistered(String className, String uri) {
            throw new UnsupportedOperationException("TEST MOCK");
        }

        @Override
        public IElementDef createElementDefinition(XmlNode node) {
             throw new UnsupportedOperationException("TEST MOCK");
        }

        @Override
        public Set<ElementName> getElementNames() {
            throw new UnsupportedOperationException("TEST MOCK");
        }

        @Override
        public ElementInfo getElementInfo(String name, String uri) {
            throw new UnsupportedOperationException("TEST MOCK");
        }

    }
}
