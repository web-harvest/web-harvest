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
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;
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
        assertTrue(context.getVar("configResult") instanceof org.webharvest.runtime.variables.EmptyVariable,
            "A clean exit must leave an empty root result");
        assertFalse(Thread.currentThread().isInterrupted(),
            "A requested exit must not leave the worker thread interrupted");
    }

    @Test(groups = {"integration", "core-plugins"})
    public void falseConditionDoesNotPublishExitAndAllowsFollowingSiblings() throws Exception {
        ExitObserver observer = new ExitObserver();

        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
            + "<config xmlns=\"http://org.webharvest/schema/2.1/core\">"
            + "  <exit condition=\"false\" message=\"must-not-stop\"/>"
            + "  <def var=\"afterExit\" value=\"continued\"/>"
            + "</config>";

        DynamicScopeContext context = executeXmlStringWithObserver(xml, observer);

        assertNull(observer.event.get(), "False Exit condition must not publish an event");
        assertEquals(context.getVar("afterExit").toString(), "continued");
    }

    @Test(groups = {"integration", "core-plugins"})
    public void trueConditionInsideLoopStopsRemainingIterationsAndRootSiblings() throws Exception {
        ExitObserver observer = new ExitObserver();
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
            + "<config xmlns=\"http://org.webharvest/schema/2.1/core\">"
            + "  <loop item=\"item\" items=\"one,two\" delimiter=\",\">"
            + "    <exit condition=\"true\" message=\"loop-stop\"/>"
            + "    <def var=\"afterLoopBody\" value=\"must-not-run\"/>"
            + "  </loop>"
            + "  <def var=\"afterLoop\" value=\"must-not-run\"/>"
            + "</config>";

        DynamicScopeContext context = executeXmlStringWithObserver(xml, observer);

        assertNotNull(observer.event.get(), "Exit inside Loop must publish the exit event");
        assertEquals(observer.event.get().getMessage(), "loop-stop");
        assertNull(context.getVar("afterLoopBody"),
            "Siblings after Exit inside Loop must not execute");
        assertNull(context.getVar("afterLoop"),
            "Root siblings after a Loop containing Exit must not execute");
    }

    @Test(groups = {"integration", "core-plugins"})
    public void trueConditionInsideWhileStopsRemainingIterationsAndRootSiblings() throws Exception {
        ExitObserver observer = new ExitObserver();
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
            + "<config xmlns=\"http://org.webharvest/schema/2.1/core\">"
            + "  <while condition=\"true\" maxloops=\"3\">"
            + "    <exit condition=\"true\" message=\"while-stop\"/>"
            + "    <def var=\"afterWhileBody\" value=\"must-not-run\"/>"
            + "  </while>"
            + "  <def var=\"afterWhile\" value=\"must-not-run\"/>"
            + "</config>";

        DynamicScopeContext context = executeXmlStringWithObserver(xml, observer);

        assertNotNull(observer.event.get(), "Exit inside While must publish the exit event");
        assertEquals(observer.event.get().getMessage(), "while-stop");
        assertNull(context.getVar("afterWhileBody"),
            "Siblings after Exit inside While must not execute");
        assertNull(context.getVar("afterWhile"),
            "Root siblings after a While containing Exit must not execute");
    }

    @Test(groups = {"integration", "core-plugins"})
    public void trueConditionInsideFunctionStopsCallAndRootSiblings() throws Exception {
        ExitObserver observer = new ExitObserver();
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
            + "<config xmlns=\"http://org.webharvest/schema/2.1/core\">"
            + "  <function name=\"stopper\">"
            + "    <exit condition=\"true\" message=\"function-stop\"/>"
            + "    <def var=\"afterFunctionExit\" value=\"must-not-run\"/>"
            + "  </function>"
            + "  <call name=\"stopper\"/>"
            + "  <def var=\"afterCallExit\" value=\"must-not-run\"/>"
            + "</config>";

        DynamicScopeContext context = executeXmlStringWithObserver(xml, observer);

        assertNotNull(observer.event.get(), "Exit inside a called function must publish the exit event");
        assertEquals(observer.event.get().getMessage(), "function-stop");
        assertNull(context.getVar("afterFunctionExit"),
            "Siblings after Exit inside the function must not execute");
        assertNull(context.getVar("afterCallExit"),
            "Root siblings after a function call containing Exit must not execute");
    }

    @Test(groups = {"integration", "core-plugins"})
    public void trueConditionInsideTryBypassesCatchAndStopsRootSiblings() throws Exception {
        ExitObserver observer = new ExitObserver();
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
            + "<config xmlns=\"http://org.webharvest/schema/2.1/core\">"
            + "  <try>"
            + "    <exit condition=\"true\" message=\"try-stop\"/>"
            + "    <def var=\"afterTryExit\" value=\"must-not-run\"/>"
            + "  </try>"
            + "  <catch><def var=\"catchMarker\" value=\"must-not-run\"/></catch>"
            + "  <def var=\"afterTryCatch\" value=\"must-not-run\"/>"
            + "</config>";

        DynamicScopeContext context = executeXmlStringWithObserver(xml, observer);

        assertNotNull(observer.event.get(), "Exit inside Try must publish the exit event");
        assertEquals(observer.event.get().getMessage(), "try-stop");
        assertNull(context.getVar("afterTryExit"),
            "Siblings after Exit inside Try must not execute");
        assertNull(context.getVar("catchMarker"),
            "Catch must not intercept a requested scraper exit");
        assertNull(context.getVar("afterTryCatch"),
            "Root siblings after a Try containing Exit must not execute");
    }

    private DynamicScopeContext executeXmlStringWithObserver(String xml, ExitObserver observer) throws Exception {
        ConfigSource configSource = new BufferConfigSource(xml);
        Harvester harvester = harvest.getHarvester(configSource, new org.webharvest.HarvestLoadCallback() {
            @Override
            public void onSuccess(org.webharvest.definition.PluginConfigurationTree configTree) {
                // Configuration is loaded before the context callback registers the observer.
            }
        });
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
