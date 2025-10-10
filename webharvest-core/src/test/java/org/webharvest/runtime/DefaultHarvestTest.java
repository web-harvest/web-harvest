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

import static org.easymock.EasyMock.*;
import static org.testng.AssertJUnit.*;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import org.testng.annotations.*;
import org.unitils.UnitilsTestNG;
import org.unitils.easymock.EasyMockUnitils;
import org.unitils.easymock.annotation.RegularMock;
import org.webharvest.HarvestLoadCallback;
import org.webharvest.Harvester;
import org.webharvest.definition.Config;
import org.webharvest.definition.ConfigFactory;
import org.webharvest.definition.ConfigSource;
// REMOVED in v2.2.x - old architecture classes no longer exist
// import org.webharvest.definition.IElementDef;
import org.webharvest.definition.PluginConfigurationTree;
import org.webharvest.events.EventHandler;
import org.webharvest.events.EventSink;
import org.webharvest.events.HandlerHolder;
import org.webharvest.events.HarvesterEvent;
import org.webharvest.ioc.HarvesterFactory;

public class DefaultHarvestTest extends UnitilsTestNG {

    @RegularMock
    private HarvesterFactory mockFactory;

    @RegularMock
    private HandlerHolder mockHandlerHolder;

    @RegularMock
    private HarvestLoadCallback mockLoadCallback;

    @RegularMock
    private EventSink mockEventSink;

    @RegularMock
    private HarvesterEvent mockEvent;

    @RegularMock
    private Harvester mockHarvester;

    @RegularMock
    private ConfigSource mockConfigSource;

    @RegularMock
    private ConfigFactory mockConfigFactory;

    @RegularMock
    private Config mockConfig;

    // REMOVED in v2.2.x - old architecture mocks no longer needed
    // @RegularMock
    // private IElementDef mockElementDef;
    
    @RegularMock
    private PluginConfigurationTree mockConfigTree;

    private DefaultHarvest harvest;

    @BeforeMethod
    public void setUp() {
        harvest = new DefaultHarvest(mockConfigFactory, mockFactory,
                mockHandlerHolder, mockEventSink);
    }

    @AfterMethod
    public void tearDown() {
        harvest = null;
    }

    @Test
    public void testGetHarvester() throws IOException {
        // MODERNIZATION v2.2.x: Use new API (PluginConfigurationTree)
        expect(mockConfigFactory.create(mockConfigSource)).andReturn(mockConfig);
        expect(mockFactory.create(mockConfig)).
            andReturn(mockHarvester);
        mockConfig.reload();
        expectLastCall();
        expect(mockConfig.getConfigurationTree()).andReturn(mockConfigTree);
        mockLoadCallback.onSuccess(mockConfigTree);
        expectLastCall();
        EasyMockUnitils.replay();
        final Harvester harvester = harvest.getHarvester(mockConfigSource,
                mockLoadCallback);
        assertNotNull(harvester);
        assertSame(mockHarvester, harvester);
    }

    @Test
    public void testAddEventHandler() {
        final EventHandler<HarvesterEvent> handler =
                new EventHandler<HarvesterEvent>() {

                    @Override
                    public void handle(final HarvesterEvent event) {
                        // Do nothing
                    }

                };
        mockHandlerHolder.register(handler);
        expectLastCall();
        EasyMockUnitils.replay();
        harvest.addEventHandler(handler);
    }

    @Test
    public void testPostEvent() {
        mockEventSink.publish(mockEvent);
        EasyMockUnitils.replay();
        harvest.postEvent(mockEvent);
    }

}
