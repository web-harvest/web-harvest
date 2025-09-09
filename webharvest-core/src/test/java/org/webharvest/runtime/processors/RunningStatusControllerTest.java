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

import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertSame;

import java.lang.reflect.Field;

import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.unitils.UnitilsTestNG;
import org.unitils.easymock.EasyMockUnitils;
import org.unitils.easymock.annotation.RegularMock;
import org.unitils.mock.Mock;
import org.webharvest.ioc.InjectorHelper;
import org.webharvest.runtime.DynamicScopeContext;
import org.webharvest.runtime.variables.EmptyVariable;
import org.webharvest.runtime.variables.Variable;

import com.google.common.util.concurrent.Monitor;
import com.google.common.util.concurrent.Monitor.Guard;
import com.google.inject.Injector;

public class RunningStatusControllerTest extends UnitilsTestNG {

    private Monitor monitor;
    private GuardMock guard;

    @RegularMock
    private DynamicScopeContext mockContext;

    @RegularMock
    private Processor mockProcessor;

    private Mock<Injector> mockInjector;

    private RunningStatusController controller;

    @BeforeMethod
    public void setUp() throws Exception {
        monitor = new Monitor();
        guard = new GuardMock(monitor);

        mockInjector.returns(monitor).getInstance(Monitor.class);
        mockInjector.returns(guard).getInstance(Monitor.Guard.class);

        // Puts Injector's mock into InjectorHelper class
        final Field injectorField = InjectorHelper.class
                .getDeclaredField("injector");
        injectorField.setAccessible(true);
        injectorField.set(null, mockInjector.getMock());

        controller = new RunningStatusController(mockProcessor);
    }

    @AfterMethod
    public void tearDown() throws Exception {
        monitor = null;
        guard = null;

        controller = null;
    }

    @Test
    public void runIfUnlockMonitor() throws Exception {
        final Variable var = EmptyVariable.INSTANCE;

        guard.setSatisfied(true);

        expect(mockProcessor.run(eq(mockContext)))
            .andReturn(var);

        EasyMockUnitils.replay();

        final Variable result = controller.run(mockContext);
        assertNotNull("Result variable is null.", result);
        assertSame("Unexpected variable.", result, var);
    }

    private class GuardMock extends Guard {

        private boolean satisfied;

        public GuardMock(final Monitor monitor) {
            super(monitor);
        }

        public void setSatisfied(final boolean satisfied) {
            this.satisfied = satisfied;
        }

        @Override
        public boolean isSatisfied() {
            return this.satisfied;
        }

    }
}
