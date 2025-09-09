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

import static org.unitils.mock.ArgumentMatchers.notNull;

import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.unitils.UnitilsTestNG;
import org.unitils.mock.Mock;
import org.webharvest.annotation.Definition;
import org.webharvest.runtime.DynamicScopeContext;
import org.webharvest.runtime.processors.WebHarvestPlugin;
import org.webharvest.runtime.processors.plugins.Autoscanned;
import org.webharvest.runtime.processors.plugins.TargetNamespace;
import org.webharvest.runtime.variables.Variable;

public class AnnotatedPluginsPostProcessorTest extends UnitilsTestNG {

    private static final String TARGET_NAMESPACE =
        "http://web-harvest.sourceforge.net/schema/dummy";

    private Mock<ConfigurableResolver> mockConfigurbleResolver;

    private AnnotatedPluginsPostProcessor postProcessor;

    @BeforeMethod
    public void setUp() {
        this.postProcessor = new AnnotatedPluginsPostProcessor(
                this.getClass().getPackage().getName());
    }

    @AfterMethod
    public void tearDown() {
        this.postProcessor = null;
    }

    @Test
    public void testPostProcess() {
        postProcessor.postProcess(mockConfigurbleResolver.getMock());
        mockConfigurbleResolver.assertInvoked().registerPlugin(notNull(Class.class), TARGET_NAMESPACE);
        mockConfigurbleResolver.assertNotInvoked().registerPlugin((Class) null, null);
    }


    @Autoscanned
    @TargetNamespace(TARGET_NAMESPACE)
    @Definition("foo")
    static class ValidPlugin extends MockAbstractPlugin {
    }

    @Autoscanned
    static class MissingNamespacePlugin extends MockAbstractPlugin {
    }

    @Autoscanned
    @TargetNamespace(TARGET_NAMESPACE)
    class NotWebHarvestPlugin {
    }

    @Autoscanned
    @TargetNamespace(TARGET_NAMESPACE)
    static class MissingDefinitionForPlugin extends MockAbstractPlugin {

    }

    abstract static class MockAbstractPlugin extends WebHarvestPlugin {

        @Override
        public Variable executePlugin(final DynamicScopeContext context)
                throws InterruptedException {
            throw new UnsupportedOperationException("not supported by mock");
        }
    }
}
