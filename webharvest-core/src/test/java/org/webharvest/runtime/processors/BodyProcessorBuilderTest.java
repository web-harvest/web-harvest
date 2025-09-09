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

package org.webharvest.runtime.processors;

import org.testng.Assert;
import org.testng.annotations.Test;
import org.unitils.mock.Mock;
import org.webharvest.UnitilsTestNGExtension;
import org.webharvest.definition.IElementDef;
import org.webharvest.definition.WebHarvestPluginDef;
import org.webharvest.definition.XmlNode;
import org.webharvest.definition.XmlNodeTestUtils;
import org.webharvest.runtime.processors.BodyProcessor.Builder;

public class BodyProcessorBuilderTest extends UnitilsTestNGExtension {

        private Mock<Processor> parentProcessor;

        @Test
        public void testBuilder() {
            final Processor parent = parentProcessor.getMock();
            final IElementDef def = new MockElementDef();

            final Builder builder = new Builder(def).
                setParentProcessor(parent);
            final BodyProcessor processor = builder.build();

            Assert.assertNotNull(processor, "Processor is null.");
            Assert.assertNotNull(processor.getElementDef(),
                "Elemenet definition is null.");
            Assert.assertSame(processor.getElementDef(), def,
                "Unexpected element definition.");
            Assert.assertNotNull(processor.getParentProcessor(),
                "Parent processor is null.");
            Assert.assertSame(processor.getParentProcessor(), parent,
                "Unexpected parent processor.");
        }

        private class MockElementDef extends WebHarvestPluginDef {

            public MockElementDef() {
                super(XmlNodeTestUtils.createXmlNode("<empty/>",
                        XmlNodeTestUtils.NAMESPACE_21), BodyProcessor.class);
            }

        }


}
