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

import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.same;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertSame;

import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.unitils.UnitilsTestNG;
import org.unitils.easymock.EasyMockUnitils;
import org.unitils.easymock.annotation.RegularMock;
import org.webharvest.definition.IElementDef;
import org.webharvest.runtime.DynamicScopeContext;
import org.webharvest.runtime.variables.Variable;

public class AbstractProcessorDecoratorTest extends UnitilsTestNG {

    @RegularMock
    private IElementDef mockElement;

    @RegularMock
    private Processor mockProcessor;

    private MockDecorator decorator;

    @BeforeMethod
    public void setUp() throws Exception {
        decorator = new MockDecorator(mockProcessor);
    }

    @AfterMethod
    public void tearDown() throws Exception {
        decorator = null;
    }

    @Test(expectedExceptions=IllegalArgumentException.class)
    public void constructorIfNullProcessor() {
        new MockDecorator(null);
    }

    @Test
    public void setElementDef() {
        mockProcessor.setElementDef(same(mockElement));
        expectLastCall();

        EasyMockUnitils.replay();

        decorator.setElementDef(mockElement);
    }

    @Test
    public void getElementDef() {
        expect(mockProcessor.getElementDef()).andReturn(mockElement);

        EasyMockUnitils.replay();

        final IElementDef element = decorator.getElementDef();
        assertNotNull("Returned element is null.", element);
        assertSame("Unexpected element.", mockElement, element);
    }

    @Test
    public void getParentProcessor() {
        expect(mockProcessor.getParentProcessor()).andReturn(mockProcessor);

        EasyMockUnitils.replay();

        final Processor parent = decorator.getParentProcessor();
        assertNotNull("Parent processor is null.", parent);
        assertSame("Unexpected parent processor.", mockProcessor, parent);
    }

    @Test
    public void setParentProcessor() {
        mockProcessor.setParentProcessor(same(mockProcessor));
        expectLastCall();

        EasyMockUnitils.replay();

        decorator.setParentProcessor(mockProcessor);
    }

    @Test
    public void getRunningLevel() {
        expect(mockProcessor.getRunningLevel()).andReturn(2);

        EasyMockUnitils.replay();

        final int level = decorator.getRunningLevel();
        assertEquals("Unexpected element.", 2, level);
    }


    private class MockDecorator extends AbstractProcessorDecorator {

        public MockDecorator(final Processor decoratedProcessor) {
            super(decoratedProcessor);
        }

        @Override
        public Variable run(DynamicScopeContext context)
                throws InterruptedException {
            return null;
        }

    }
}
