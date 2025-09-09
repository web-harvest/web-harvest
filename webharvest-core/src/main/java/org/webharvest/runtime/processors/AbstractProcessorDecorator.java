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

/**
 * Abstract implementation of {@link Processor} interface which realizes
 * Decorator design pattern and overrides almost all {@link Processor}'s methods
 * (without #run() method) in the default way. It means that invocation of
 * {@link Processor}'s method is delegated to decorated processor.
 *
 * This class could be really helpful when only #run() method should be
 * decorated in special way.
 *
 * @author mczapiewski
 * @since 2.1-SNAPSHOT
 * @version %I%, %G%
 *
 * @param <TDef>
 *            type of definition of decorated processor
 */
public abstract class AbstractProcessorDecorator<TDef extends IElementDef>
        implements Processor<TDef> {

    protected final Processor<TDef> decoratedProcessor;

    public AbstractProcessorDecorator(final Processor<TDef> decoratedProcessor) {
        if (decoratedProcessor == null) {
            throw new IllegalArgumentException(
                    "Decorated processor is required.");
        }
        this.decoratedProcessor = decoratedProcessor;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setElementDef(final TDef elementDef) {
        this.decoratedProcessor.setElementDef(elementDef);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public TDef getElementDef() {
        return this.decoratedProcessor.getElementDef();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Processor getParentProcessor() {
        return this.decoratedProcessor.getParentProcessor();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setParentProcessor(final Processor parentProcessor) {
        this.decoratedProcessor.setParentProcessor(parentProcessor);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getRunningLevel() {
        return this.decoratedProcessor.getRunningLevel();
    }

}
