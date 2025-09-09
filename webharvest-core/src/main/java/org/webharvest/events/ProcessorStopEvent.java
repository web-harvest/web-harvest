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

import java.util.Map;

import org.webharvest.runtime.processors.Processor;

/**
 * Event informing that the specified {@link Processor} has successfully
 * finished its work.
 *
 * @see ProcessorStopEvent
 *
 * @author mczapiewski
 * @since 2.1-SNAPSHOT
 * @version %I%, %G%
 */
public final class ProcessorStopEvent {

    private final Processor processor;

    private final Map properties;

    /**
     * Default class constructor which accepts not {@code null} reference to
     * {@link Processor} and its properties.
     *
     * @param processor
     *            reference to {@link Processor} which has been stopped; must
     *            not be null
     * @param properties
     *            all properties which has been completed by the
     *            {@link Processor}; must not be null
     */
    public ProcessorStopEvent(final Processor processor, final Map properties) {
        if (processor == null) {
            throw new IllegalArgumentException("Processor is mandatory.");
        }
        if (properties == null) {
            throw new IllegalArgumentException("Properties are required.");
        }
        this.processor = processor;
        this.properties = properties;
    }

    /**
     * Returns reference to {@link Processor} which has been stopped.
     *
     * @return reference to {@link Processor} which has been stopped.
     */
    public Processor getProcessor() {
        return processor;
    }

    /**
     * Returns properties of the {@link Processor} which has been stopped.
     *
     * @return properties of the {@link Processor} which has been stopped.
     */
    public Map getProperties() {
        return properties;
    }

}
