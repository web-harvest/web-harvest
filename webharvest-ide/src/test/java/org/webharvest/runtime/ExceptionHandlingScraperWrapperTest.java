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

package org.webharvest.runtime;

import static org.testng.AssertJUnit.assertSame;
import static org.testng.AssertJUnit.assertNotNull;
import static org.easymock.EasyMock.*;

import org.easymock.IAnswer;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.unitils.UnitilsTestNG;
import org.unitils.easymock.EasyMockUnitils;
import org.unitils.easymock.annotation.RegularMock;
import org.unitils.mock.Mock;
import org.webharvest.events.ScraperExecutionErrorEvent;

import com.google.common.eventbus.EventBus;

public class ExceptionHandlingScraperWrapperTest extends UnitilsTestNG {

    @RegularMock
    private EventBus eventBus;

    @RegularMock
    private WebScraper webScraper;

    private Mock<DynamicScopeContext> mockContext;

    private ExceptionHandlingScraperWrapper wrapper;

    @BeforeMethod
    public void setUp() throws Exception {
        wrapper = new ExceptionHandlingScraperWrapper(webScraper, eventBus);
    }

    @AfterMethod
    public void tearDown() throws Exception {
        wrapper = null;
    }

    @Test(expectedExceptions=IllegalArgumentException.class)
    public void constructorIfNullWebScraper() {
        new ExceptionHandlingScraperWrapper(null, eventBus);
    }

    @Test(expectedExceptions=IllegalArgumentException.class)
    public void constructorIfNullEventBus() {
        new ExceptionHandlingScraperWrapper(webScraper, null);
    }

    @Test
    public void execute() {
        final DynamicScopeContext context = mockContext.getMock();

        webScraper.execute(same(context));
        expectLastCall();

        EasyMockUnitils.replay();

        wrapper.execute(context);
    }

    @Test
    public void executeIfRuntimeException() {
        final Capture capture = new Capture();
        final RuntimeException exception = new RuntimeException();
        final DynamicScopeContext context = mockContext.getMock();

        webScraper.execute(same(context));
        expectLastCall().andThrow(exception);

        eventBus.post(isA(ScraperExecutionErrorEvent.class));
        expectLastCall().andAnswer(new IAnswer<Object>() {
            public Object answer() throws Throwable {
                capture.setCaptured(getCurrentArguments()[0]);
                return null;
            }
        });

        EasyMockUnitils.replay();

        wrapper.execute(context);

        final ScraperExecutionErrorEvent event =
            capture.<ScraperExecutionErrorEvent>getCaptured();
        assertNotNull("Event is null.", event);
        final Exception ex = event.getException();
        assertNotNull("Exception from event is null.", ex);
        assertSame("Unexpected exception.", exception, ex);
    }



    // TODO mczapiewski Duplicate code with SpringAwareTypeListener
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

}
