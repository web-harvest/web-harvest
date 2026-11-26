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
import com.google.inject.Provider;
import com.google.inject.TypeLiteral;
import com.google.inject.spi.InjectionListener;
import com.google.inject.spi.TypeEncounter;
import com.google.inject.spi.TypeListener;

/**
 * Guice TypeListener that registers injected subscriber instances with the
 * EventBus belonging to the injector that encountered the instance.
 *
 * <p>The provider is captured from the TypeEncounter. No process-wide static
 * Injector lookup is used, so independent injectors cannot cross-register
 * subscribers with each other's buses.</p>
 */
public final class EventBusTypeListener implements TypeListener {

    private static final Logger LOG = LoggerFactory.getLogger(EventBusTypeListener.class);

    @Override
    public <I> void hear(final TypeLiteral<I> typeLiteral,
                        final TypeEncounter<I> typeEncounter) {
        if (!hasSubscriberMethod(typeLiteral.getRawType())) {
            return;
        }

        final Provider<EventBus> eventBusProvider = typeEncounter.getProvider(EventBus.class);
        typeEncounter.register(new InjectionListener<I>() {
            @Override
            public void afterInjection(final I instance) {
                eventBusProvider.get().register(instance);
                LOG.info("Subscribed to events {}", instance);
            }
        });
    }

    private static boolean hasSubscriberMethod(Class<?> type) {
        if (type == null) {
            return false;
        }
        for (Method method : type.getDeclaredMethods()) {
            if (method.isAnnotationPresent(Subscribe.class)) {
                return true;
            }
        }
        for (Class<?> interfaceType : type.getInterfaces()) {
            if (hasSubscriberMethod(interfaceType)) {
                return true;
            }
        }
        return hasSubscriberMethod(type.getSuperclass());
    }
}
