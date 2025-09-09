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

import org.webharvest.definition.IElementDef;
import org.webharvest.ioc.InjectorHelper;
import org.webharvest.runtime.DynamicScopeContext;
import org.webharvest.runtime.Scraper;
import org.webharvest.runtime.variables.Variable;

import com.google.common.util.concurrent.Monitor;
import com.google.common.util.concurrent.Monitor.Guard;

/**
 * {@link AbstractProcessorDecorator} implementation which decorates
 * {@link Processor#run(Scraper, DynamicScopeContext)} method in the way that it
 * enters to the {@link Monitor} using {@link Guard} verifying that processing
 * is not paused. Otherwise it wait until {@link Guard} allows to continue
 * processing.
 *
 * {@link Monitor} and {@link Guard} instances are retrieved using
 * {@link InjectorHelper}.
 *
 * @author mczapiewski
 * @since 2.1-SNAPSHOT
 * @version %I%, %G%
 *
 * @param <TDef>
 *            type of definition of decorated processor
 */
public final class RunningStatusController<TDef extends IElementDef> extends
        AbstractProcessorDecorator<TDef> {

    private final Monitor monitor;
    private final Monitor.Guard runningGuard;

    /**
     * Default class constructor which accepts {@link Processor} instance which
     * is going to be decorated by this class.
     *
     * @param decoratedProcessor
     *            an instance of {@link Processor} which is going to be
     *            decorated; must be not {@code null}.
     */
    public RunningStatusController(final Processor<TDef> decoratedProcessor) {
        super(decoratedProcessor);
        this.monitor = InjectorHelper.getInjector().getInstance(Monitor.class);
        this.runningGuard = InjectorHelper.getInjector().getInstance(
                Monitor.Guard.class);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Variable run(final DynamicScopeContext context) throws InterruptedException {
        try {
            monitor.enterWhen(runningGuard);
        } finally {
            monitor.leave();
        }
        return this.decoratedProcessor.run(context);
    }

}
