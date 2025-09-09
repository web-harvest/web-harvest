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

package org.webharvest.ioc;

import static org.easymock.EasyMock.*;
import static org.testng.AssertJUnit.*;

import org.easymock.IAnswer;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.unitils.UnitilsTestNG;
import org.unitils.easymock.EasyMockUnitils;
import org.unitils.easymock.annotation.RegularMock;
import org.unitils.inject.annotation.InjectIntoStatic;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.google.inject.Injector;
import com.google.inject.TypeLiteral;
import com.google.inject.spi.InjectionListener;
import com.google.inject.spi.TypeEncounter;

public class EventBusTypeListenerTest extends UnitilsTestNG {

    private MockTypeLiteral mockTypeLiteral;

    @RegularMock
    private TypeEncounter<ApplicableSubscriber> mockEncounter;

    @RegularMock
    private ApplicableSubscriber mockSubscriber;

    @RegularMock
    private EventBus mockEventBus;

    @RegularMock
    @InjectIntoStatic(target = InjectorHelper.class, property = "injector")
    private Injector mockInjector;

    private EventBusTypeListener listener;

    @BeforeMethod
    public void setUp() {
        mockTypeLiteral = new MockTypeLiteral();
        listener = new EventBusTypeListener();
    }

    @AfterMethod
    public void tearDown() {
        listener = null;
        mockTypeLiteral = null;
    }

    @Test
    public void testApplyApplicableInjection() {
        final Capture capture = new Capture();
        mockEncounter.register(isA(InjectionListener.class));
        expectLastCall().andAnswer(new IAnswer<Object>() {
            public Object answer() throws Throwable {
                capture.setCaptured(getCurrentArguments()[0]);
                return null;
            }
        });
        expect(mockInjector.getInstance(EventBus.class)).
                andReturn(mockEventBus);
        mockEventBus.register(mockSubscriber);
        EasyMockUnitils.replay();
        listener.hear(mockTypeLiteral, mockEncounter);
        final InjectionListener<Object> injectionListener =
                capture.getCaptured();
        assertNotNull(injectionListener);
        injectionListener.afterInjection(mockSubscriber);
    }

    // TODO rbala Duplicate code with SpringAwareTypeListener
    private final class Capture {

        private Object captured;

        @SuppressWarnings("unchecked")
        public <T> T getCaptured() {
            return (T) captured;
        }

        private void setCaptured(final Object captured) {
            this.captured = captured;
        }

    }

    interface ApplicableSubscriber {

        @Subscribe
        public void handle(String message);

    }

    /**
     * Handy trick. Since we can not mock the TypeLiteral with EasyMock (even
     * with help of cglib - missing public constructor). We just simply
     * create subclass
     */
    // TODO rbala Duplicate code with SpringAwareTypeListenerTest
    static final class MockTypeLiteral extends
            TypeLiteral<ApplicableSubscriber> {

    }

}
