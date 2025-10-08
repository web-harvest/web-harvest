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
import org.webharvest.runtime.variables.EmptyVariable;
import org.webharvest.runtime.variables.ListVariable;
import org.webharvest.runtime.variables.Variable;
import org.webharvest.utils.CommonUtil;

import java.util.concurrent.Callable;

/**
 * Processor which executes only body and returns variables list.
 */
public class BodyProcessor extends AbstractProcessor<IElementDef> {

    public Variable execute(final DynamicScopeContext context) 
            throws InterruptedException {
        final IElementDef[] defs = elementDef.getOperationDefs();

        if (defs.length == 1) {
            return context.executeWithinNewContext(new Callable<Variable>() {
                @Override
                public Variable call() throws Exception {
                    final Processor processor = ProcessorFactory.INSTANCE.createProcessor(context, defs[0]);
                    processor.setParentProcessor(getParentProcessor());
                    return CommonUtil.createVariable(processor.run(context));
                }
            });
        }

        return context.executeWithinNewContext(new Callable<Variable>() {
            @Override
            public Variable call() throws Exception {
                final ListVariable result = new ListVariable();
                for (IElementDef def : defs) {
                    final Processor processor = ProcessorFactory.INSTANCE.createProcessor(context, def);
                    processor.setParentProcessor(getParentProcessor());
                    final Variable variable = processor.run(context);
                    if (!variable.isEmpty()) {
                        result.addVariable(variable);
                    }
                }
                return result.isEmpty() ? EmptyVariable.INSTANCE : result
                        .getList().size() == 1 ? CommonUtil
                        .createVariable(result.get(0)) : result;
            }
        });
    }

    /**
     * A builder responsible for creating instance of {@link BodyProcessor} and
     * completing it with appropriate {@link IElementDef}.
     *
     * @author mczapiewski
     * @since 2.1-SNAPSHOT
     * @version %I%, %G%
     */
    public static final class Builder {

        private final IElementDef elementDef;

        private Processor parentProcessor;

        /**
         * Default builder constructor which accepts {@link IElementDef} for
         * {@link BodyProcessor}. Specified element definition should not be
         * null.
         *
         * @param elementDef
         *            an instance of {@link IElementDef}
         */
        public Builder(final IElementDef elementDef) {
            this.elementDef = elementDef;
        }

        /**
         * Sets reference to the parent {@link Processor}.
         *
         * @param processor
         *            reference to parent {@link Processor}
         * @return an instance of this {@link Builder}
         */
        public Builder setParentProcessor(final Processor processor) {
            this.parentProcessor = processor;
            return this;
        }

        /**
         * Returns an instance of {@link BodyProcessor} which is completed with
         * element definition.
         *
         * @return an instance of {@link BodyProcessor}
         */
        public BodyProcessor build() {
            final BodyProcessor processor = new BodyProcessor();
            processor.setElementDef(elementDef);
            processor.setParentProcessor(parentProcessor);

            //FIXME: Because BodyProcessor is not instantiated from definition,
            //its (or AbstractProcessor) dependencies must be injected right
            //here. It is not perfect solution, but temporarily it works.
            InjectorHelper.getInjector().injectMembers(processor);

            return processor;
        }

    }
}