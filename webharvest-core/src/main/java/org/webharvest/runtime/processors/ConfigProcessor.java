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

import static org.webharvest.WHConstants.XMLNS_CORE;
import static org.webharvest.WHConstants.XMLNS_CORE_10;

import org.webharvest.annotation.Definition;
import org.webharvest.definition.ConfigDef;
import org.webharvest.definition.IElementDef;
import org.webharvest.runtime.DynamicScopeContext;
import org.webharvest.runtime.processors.plugins.Autoscanned;
import org.webharvest.runtime.processors.plugins.TargetNamespace;
import org.webharvest.runtime.variables.EmptyVariable;
import org.webharvest.runtime.variables.Variable;

/**
 * @author Robert Bala
 * @since 2.1.0-SNAPSHOT
 * @version %I%, %G%
 */
// TODO Add unit test
// TODO Add javadoc
@Autoscanned
@TargetNamespace({ XMLNS_CORE, XMLNS_CORE_10 })
@Definition(value = "config", validAttributes = { "charset(*charset)", "scriptlang(beanshell;javascript;groovy)" },
        definitionClass = ConfigDef.class)
public final class ConfigProcessor extends AbstractProcessor<ConfigDef> {

    /**
     * {@inheritDoc}
     */
    @Override
    protected Variable execute(final DynamicScopeContext context)
            throws InterruptedException {

        context.setCharset(getElementDef().getCharset());
        context.setScriptingLanguage(getElementDef().getScriptingLanguage());

        // FIXME: It should be done by BodyProcessor (or some other component
        // while we'll annihilate BodyProcessor). However, due to current
        // BodyProcessor implementation, every processor is executed within new
        // context. If we had new context at this level, variables defined
        // within 'config' element body would not be available in the scraper
        // context at the end of its execution.
        for (IElementDef elementDef : getElementDef().getOperationDefs()) {
            ProcessorFactory.INSTANCE.createProcessor(context, elementDef).run(context);
        }

        return EmptyVariable.INSTANCE;
    }

}
