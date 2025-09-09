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

package org.webharvest.events;

import static org.easymock.EasyMock.*;
import static org.testng.Assert.*;

import org.testng.annotations.*;
import org.unitils.UnitilsTestNG;
import org.unitils.easymock.EasyMockUnitils;
import org.unitils.easymock.annotation.Mock;
import org.unitils.easymock.annotation.RegularMock;
import org.webharvest.AlreadyBoundException;
import org.webharvest.Harvester;
import org.webharvest.Registry;

import com.google.common.eventbus.EventBus;
import com.google.inject.Provider;

public class DefaultHandlerHolderTest extends UnitilsTestNG {

    @RegularMock
    private Registry<Harvester, EventBus> mockRegistry;

    @RegularMock
    private Provider<EventBus> mockProvider;

    @RegularMock
    private Harvester mockHarvester;

    @RegularMock
    private EventHandler<?> mockHandler;

    @Mock
    private EventBus mockEventBus;

    private DefaultHandlerHolder holder;

    @BeforeMethod
    public void setUp() {
        holder = new DefaultHandlerHolder(mockRegistry, mockProvider);
    }

    @AfterMethod
    public void tearDown() {
        holder = null;
    }

    @Test(expectedExceptions=IllegalArgumentException.class)
    public void testRegisterWithoutListener() {
        holder.register(null);
    }

    @Test
    public void testOnBeforeScraping() throws Exception {
        expect(mockProvider.get()).andReturn(mockEventBus);
        mockEventBus.register(mockHandler);
        expectLastCall();
        mockRegistry.bind(mockHarvester, mockEventBus);
        expectLastCall();
        EasyMockUnitils.replay();
        holder.register(mockHandler);
        holder.onBeforeScraping(mockHarvester);
    }

    @Test(expectedExceptions=IllegalArgumentException.class)
    public void testOnBeforeScrapingWithoutgHarvester()
            throws Exception {
        holder.onBeforeScraping(null);
    }

    @Test
    public void testOnBeforeScrapingInCaseOfAlreadyBoundException()
            throws Exception {
        final AlreadyBoundException expectedException =
                new AlreadyBoundException("Foo");
        expect(mockProvider.get()).andReturn(mockEventBus);
        mockEventBus.register(mockHandler);
        expectLastCall();
        mockRegistry.bind(mockHarvester, mockEventBus);
        expectLastCall().andThrow(expectedException);
        EasyMockUnitils.replay();
        holder.register(mockHandler);
        try {
            holder.onBeforeScraping(mockHarvester);
            fail("AlreadyBoundException not thrown");
        } catch (RuntimeException e) {
            assertSame(e.getCause(), expectedException, "Unexpected exception");
        }
    }

    @Test(expectedExceptions=IllegalArgumentException.class)
    public void testOnAfterScrapingWithoutHarvester() {
        holder.onAfterScraping(null);
    }

    @Test
    public void testOnAfterScraping() throws Exception {
        expect(mockRegistry.lookup(mockHarvester)).andReturn(mockEventBus);
        mockEventBus.unregister(mockHandler);
        expectLastCall();
        mockRegistry.unbind(mockHarvester);
        expectLastCall();
        EasyMockUnitils.replay();
        holder.register(mockHandler);
        holder.onAfterScraping(mockHarvester);
    }

    @Test(expectedExceptions=IllegalStateException.class)
    public void testOnAfterScrapingInCaseOfMissingEventBus() {
        expect(mockRegistry.lookup(mockHarvester)).andReturn(null);
        EasyMockUnitils.replay();
        holder.register(mockHandler);
        holder.onAfterScraping(mockHarvester);
    }

}
