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

import java.lang.reflect.Method;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.google.inject.TypeLiteral;
import com.google.inject.spi.InjectionListener;
import com.google.inject.spi.TypeEncounter;
import com.google.inject.spi.TypeListener;

/**
 * Guice {@link TypeListener} implementation that is responsible for
 * registration of objects managed by Guice in singleton {@link EventBus}.
 * Each newly created object is treated as potential event subscriber.
 *
 * @author Robert Bala
 * @since 2.1.0-SNAPSHOT
 * @version %I%, %G%
 * @see TypeListener
 * @see EventBus
 */
public final class EventBusTypeListener implements TypeListener {

    private static final Logger LOG = LoggerFactory.
        getLogger(EventBusTypeListener.class);

    /**
     * {@inheritDoc}
     */
    @Override
    public <I> void hear(final TypeLiteral<I> typeLiteral,
             final TypeEncounter<I> typeEncounter) {
        // TODO rbala Refactor as soon as possible. Ugly code (the whole method)
        boolean qualifies = false;
        for (final Method method : typeLiteral.getRawType().
                getDeclaredMethods()) {
            // If one of the class's methods is annotated to receive events then
            // register it to event bus
            qualifies = method.isAnnotationPresent(Subscribe.class);
            if (qualifies) {
                break;
            }

        }
        if (qualifies) {
            typeEncounter.register(new InjectionListener<I>() {
                public void afterInjection(final I i) {
                    InjectorHelper.getInjector().
                        getInstance(EventBus.class).register(i);
                    LOG.info("Subscribed to events {}", i);
                }
            });
        }
     }

}
