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

package org.webharvest.runtime.processors.plugins.db;

import static org.webharvest.WHConstants.XMLNS_CORE;
import static org.webharvest.WHConstants.XMLNS_CORE_10;

import org.webharvest.annotation.Definition;
import org.webharvest.exception.PluginException;
import org.webharvest.runtime.DynamicScopeContext;
import org.webharvest.runtime.processors.Processor;
import org.webharvest.runtime.processors.WebHarvestPlugin;
import org.webharvest.runtime.processors.plugins.Autoscanned;
import org.webharvest.runtime.processors.plugins.TargetNamespace;
import org.webharvest.runtime.variables.ListVariable;
import org.webharvest.runtime.variables.NodeVariable;
import org.webharvest.runtime.variables.Variable;
import org.webharvest.utils.CommonUtil;

/**
 * DB param plugin - can be used only inside database plugin.
 */
@Autoscanned
@TargetNamespace({ XMLNS_CORE, XMLNS_CORE_10 })
@Definition(value = "db-param", validAttributes = { "type" })
public final class DbParamPlugin extends WebHarvestPlugin {

    public String getName() {
        return "db-param";
    }

    public Variable executePlugin(DynamicScopeContext context) throws InterruptedException {
        Processor processor = getParentProcessor();
        if (processor != null) {
            AbstractDatabasePlugin databasePlugin = (AbstractDatabasePlugin) processor;
            String type = evaluateAttribute("type", context);
            Variable body = executeBody(context);
            if (CommonUtil.isEmptyString(type)) {
                type = "text";
                if ( body.getWrappedObject() instanceof byte[] ) {
                    type = "binary";
                } else if (body instanceof ListVariable) {
                    ListVariable list = (ListVariable) body;
                    if (list.toList().size() == 1 && list.get(0).getWrappedObject() instanceof byte[]) {
                        type = "binary";
                    }
                }
            }
            databasePlugin.addDbParam(body, type);
            return new NodeVariable("?");
        } else {
            throw new PluginException("Cannot use db-param attach plugin out of database plugin context!");
        }
    }

    public String[] getValidAttributes() {
        return new String[] {"type"};
    }

    public String[] getAttributeValueSuggestions(String attributeName) {
        if ("type".equalsIgnoreCase(attributeName)) {
            return new String[] {"int", "long", "double", "text", "binary"};
        }
        return null;
    }


}