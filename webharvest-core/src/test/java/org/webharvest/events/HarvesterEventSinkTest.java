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

import org.testng.annotations.*;
import org.unitils.UnitilsTestNG;
import org.unitils.easymock.EasyMockUnitils;
import org.unitils.easymock.annotation.RegularMock;
import org.webharvest.Harvester;
import org.webharvest.Registry;

import com.google.common.eventbus.EventBus;

public class HarvesterEventSinkTest extends UnitilsTestNG {

    @RegularMock
    private HarvesterEvent mockEvent;

    @RegularMock
    private Harvester mockHarvester;

    @RegularMock
    private Registry<Harvester, EventBus> mockRegistry;

    // FIXME rbala Unsure if it is a right way to mock classes
    private EventBus mockEventBus;

    private HarvesterEventSink eventSink;

    @BeforeMethod
    public void setUp() {
        eventSink = new HarvesterEventSink(mockRegistry);
        mockEventBus = new EventBus();
    }

    @AfterMethod
    public void tearDown() {
        eventSink = null;
        mockEventBus = null;
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testPublishWithoutEvent() {
        eventSink.publish(null);
    }

    @Test(expectedExceptions = IllegalStateException.class)
    public void testPublishForNotRegisteredHarvester() {
        expect(mockEvent.getHarvester()).andReturn(mockHarvester);
        expect(mockRegistry.lookup(mockHarvester)).andReturn(null);
        EasyMockUnitils.replay();
        eventSink.publish(mockEvent);
    }

    @Test
    public void testPublishForRegisteredHarvester() {
        expect(mockEvent.getHarvester()).andReturn(mockHarvester);
        expect(mockRegistry.lookup(mockHarvester)).andReturn(mockEventBus);
        EasyMockUnitils.replay();
        eventSink.publish(mockEvent);
    }

}
