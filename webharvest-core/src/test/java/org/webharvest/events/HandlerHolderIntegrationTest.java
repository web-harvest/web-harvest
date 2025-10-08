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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import org.webharvest.Harvest;
import org.webharvest.Harvester;
import org.webharvest.Harvester.ContextInitCallback;
import org.webharvest.Registry;
import org.webharvest.ioc.HttpModule;
import org.webharvest.ioc.ScraperModule;
import org.webharvest.ioc.Scraping;
import org.webharvest.runtime.DynamicScopeContext;
import org.webharvest.runtime.WebScraper;
import org.webharvest.runtime.web.HttpClientManager.ProxySettings;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;

public class HandlerHolderIntegrationTest {

    private static final Logger LOG =
        LoggerFactory.getLogger(HandlerHolderIntegrationTest.class);

    private Injector injector;

    private Harvest harvest;

    @BeforeClass
    public void setUp() {
        injector = Guice.createInjector(new ScraperModule("."), new HttpModule(
                ProxySettings.NO_PROXY_SET));
        harvest = injector.getInstance(Harvest.class);
        harvest.addEventHandler(new EventHandler<DummyEvent>() {

            @Override
            @Subscribe
            public void handle(final DummyEvent event) {
                LOG.info("Received event from thred [{}]", event.getId());
            }

        });
    }

    @AfterClass
    public void tearDown() {
        harvest = null;
        injector = null;
    }

    @Test(threadPoolSize = 5, invocationCount = 100)
    public void test() {
        injector.getInstance(MockHarvester.class).execute(
                new ContextInitCallback() {

                    @Override
                    public void onSuccess(final DynamicScopeContext context) {
                        // Do nothing
                    }

                });
    }

    public static class MockHarvester implements Harvester {

        private final Registry<Harvester, EventBus> registry;


        @Inject
        public MockHarvester(final Registry<Harvester, EventBus> registry) {
            this.registry = registry;
        }

        @Override
        @Scraping
        public DynamicScopeContext execute(final ContextInitCallback callback) {
            registry.lookup(this).post(
                    new DummyEvent(Thread.currentThread().getId()));

            return null;
        }

    }

    public static final class DummyEvent {

        private final long id;

        public DummyEvent(final long id) {
            this.id = id;
        }

        public long getId() {
            return id;
        }

    }

}
