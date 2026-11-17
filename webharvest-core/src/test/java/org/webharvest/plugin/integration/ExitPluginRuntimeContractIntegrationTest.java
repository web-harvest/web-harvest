/*
 * Copyright (c) 2006-2025 the original author or authors.
 *
 * Redistribution and use of this software in source and binary forms,
 * with or without modification, are permitted provided that the following
 * conditions are met:
 *
 * * Redistributions of source code must retain the above
 *   copyright notice, this list of conditions and the following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above
 *   copyright notice, this list of conditions and the following disclaimer
 *   in the documentation and/or other materials provided with the distribution.
 *
 * * The name of Web-Harvest may not be used to endorse or promote
 *   products derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package org.webharvest.plugin.integration;

import com.google.common.eventbus.Subscribe;
import org.testng.annotations.Test;
import org.webharvest.events.ScraperExecutionExitEvent;
import org.webharvest.definition.BufferConfigSource;
import org.webharvest.definition.ConfigSource;
import org.webharvest.Harvester;
import org.webharvest.runtime.DynamicScopeContext;

import java.util.concurrent.atomic.AtomicReference;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNull;

/**
 * Runtime control-flow contracts for the Exit plugin.
 */
public class ExitPluginRuntimeContractIntegrationTest extends XmlBasedIntegrationTestBase {

    @Test(groups = {"integration", "core-plugins"})
    public void trueConditionPublishesExitAndPreventsFollowingSiblings() throws Exception {
        ExitObserver observer = new ExitObserver();

        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
            + "<config xmlns=\"http://org.webharvest/schema/2.1/core\">"
            + "  <exit condition=\"true\" message=\"stop-now\"/>"
            + "  <def var=\"afterExit\" value=\"must-not-run\"/>"
            + "</config>";

        DynamicScopeContext context = executeXmlStringWithObserver(xml, observer);

        assertNotNull(observer.event.get(), "Exit must publish ScraperExecutionExitEvent");
        assertEquals(observer.event.get().getMessage(), "stop-now");
        assertNull(context.getVar("afterExit"),
            "No sibling after a true Exit condition may execute");
    }

    @Test(groups = {"integration", "core-plugins"})
    public void falseConditionDoesNotPublishExitAndAllowsFollowingSiblings() throws Exception {
        ExitObserver observer = new ExitObserver();
        injector.getInstance(EventBus.class).register(observer);

        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
            + "<config xmlns=\"http://org.webharvest/schema/2.1/core\">"
            + "  <exit condition=\"false\" message=\"must-not-stop\"/>"
            + "  <def var=\"afterExit\" value=\"continued\"/>"
            + "</config>";

        DynamicScopeContext context = executeXmlString(xml);

        assertNull(observer.event.get(), "False Exit condition must not publish an event");
        assertEquals(context.getVar("afterExit").toString(), "continued");
    }

    private DynamicScopeContext executeXmlStringWithObserver(String xml, ExitObserver observer) {
        ConfigSource configSource = new BufferConfigSource(xml);
        Harvester harvester = harvest.getHarvester(configSource, null);
        return harvester.execute(new Harvester.ContextInitCallback() {
            @Override
            public void onSuccess(DynamicScopeContext context) {
                context.getEventBus().register(observer);
            }
        });
    }

    private static final class ExitObserver {
        private final AtomicReference<ScraperExecutionExitEvent> event = new AtomicReference<>();

        @Subscribe
        public void onExit(ScraperExecutionExitEvent exitEvent) {
            event.set(exitEvent);
        }
    }
}
