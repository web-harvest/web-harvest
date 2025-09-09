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

package org.webharvest.runtime.processors.plugins;

import static org.webharvest.WHConstants.XMLNS_CORE;
import static org.webharvest.WHConstants.XMLNS_CORE_10;

import org.webharvest.annotation.Definition;
import org.webharvest.runtime.DynamicScopeContext;
import org.webharvest.runtime.processors.WebHarvestPlugin;
import org.webharvest.runtime.variables.ListVariable;
import org.webharvest.runtime.variables.NodeVariable;
import org.webharvest.runtime.variables.Variable;
import org.webharvest.utils.CommonUtil;

/**
 * Support for database operations.
 */
@Autoscanned
@TargetNamespace({ XMLNS_CORE, XMLNS_CORE_10 })
@Definition("tokenize")
public class TokenizePlugin extends WebHarvestPlugin {

    public String getName() {
        return "tokenize";
    }

    public Variable executePlugin(DynamicScopeContext context) throws InterruptedException {
        String delimiters = evaluateAttribute("delimiters", context);
        if ( delimiters == null || "".equals(delimiters) ) {
            delimiters = "\n\r";
        }
        boolean trimTokens = evaluateAttributeAsBoolean("trimtokens", true, context);
        boolean allowWmptyTokens = evaluateAttributeAsBoolean("allowemptytokens", false, context);
        String text =  executeBody(context).toString();

        this.setProperty("Delimiters", delimiters);
        this.setProperty("Trim tokens", trimTokens);
        this.setProperty("Allow empty tokens", allowWmptyTokens);

        String tokens[] = CommonUtil.tokenize(text, delimiters, trimTokens, allowWmptyTokens);

        ListVariable listVariable = new ListVariable();
        for (String token: tokens) {
            listVariable.addVariable(new NodeVariable(token));
        }

        return listVariable;
    }

    public String[] getValidAttributes() {
        return new String[] {
                "delimiters",
                "trimtokens",
                "allowemptytokens"
        };
    }

    public String[] getAttributeValueSuggestions(String attributeName) {
        if ("trimtokens".equalsIgnoreCase(attributeName)) {
            return new String[] {"true", "false"};
        } else if ("allowemptytokens".equalsIgnoreCase(attributeName)) {
            return new String[] {"true", "false"};
        }
        return null;
    }

}