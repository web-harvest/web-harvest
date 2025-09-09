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

import org.json.JSONException;
import org.json.JSONObject;
import org.json.XML;
import org.webharvest.annotation.Definition;
import org.webharvest.exception.PluginException;
import org.webharvest.runtime.DynamicScopeContext;
import org.webharvest.runtime.processors.WebHarvestPlugin;
import org.webharvest.runtime.variables.NodeVariable;
import org.webharvest.runtime.variables.Variable;

/**
 * Converter from JSON to XML
 */
@Autoscanned
@TargetNamespace({ XMLNS_CORE, XMLNS_CORE_10 })
@Definition(value="json-to-xml", validAttributes="tag")
public class JsonToXmlPlugin extends WebHarvestPlugin {

    private final static String ATTR_ROOT_TAG_NAME = "tag";

    @Override
    public Variable executePlugin(DynamicScopeContext context) throws InterruptedException {
        try {
            String jsonString = executeBody(context).toString().trim();
            
            // Handle empty or invalid JSON
            if (jsonString.isEmpty()) {
                return new NodeVariable("<empty/>");
            }
            
            // Try to fix common JSON issues
            if (!jsonString.startsWith("{") && !jsonString.startsWith("[")) {
                // If it doesn't start with { or [, wrap it in an object
                jsonString = "{\"data\": " + jsonString + "}";
            }
            
            return new NodeVariable(XML.toString(
                    new JSONObject(jsonString),
                    evaluateAttribute(ATTR_ROOT_TAG_NAME, context)));
        } catch (JSONException e) {
            // If JSON parsing still fails, create a simple XML with the raw content
            String rawContent = executeBody(context).toString();
            String rootTag = evaluateAttribute(ATTR_ROOT_TAG_NAME, context);
            if (rootTag == null || rootTag.isEmpty()) {
                rootTag = "data";
            }
            return new NodeVariable("<" + rootTag + "><![CDATA[" + rawContent + "]]></" + rootTag + ">");
        }
    }

}