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