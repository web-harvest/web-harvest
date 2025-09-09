/*
 Copyright (c) 2006-2025 the original author or authors.

 Redistribution and use of this software in source and binary forms,
 with or without modification, are permitted provided that the following
 conditions are met:

 * Redistributions of source code must retain the above
   copyright notice, this list of conditions and the
   following disclaimer.

 * Redistributions in binary form must reproduce the above
   copyright notice, this list of conditions and the
   following disclaimer in the documentation and/or other
   materials provided with the distribution.

 * The name of Web-Harvest may not be used to endorse or promote
   products derived from this software without specific prior
   written permission.

 THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 POSSIBILITY OF SUCH DAMAGE.
 */

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
