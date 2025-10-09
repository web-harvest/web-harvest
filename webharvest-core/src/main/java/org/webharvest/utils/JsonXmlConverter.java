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

package org.webharvest.utils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * JSON to XML converter for Web-Harvest.
 * 
 * <h3>Bug #61 Fix</h3>
 * <p>
 * This is a custom implementation that fixes Bug #61 where org.json.XML.toString()
 * treats the key "content" as a special case for text nodes, causing it to be
 * stripped from the XML output.
 * </p>
 * 
 * <p>
 * <strong>Problem with org.json.XML:</strong>
 * <pre>
 * Input:  {"a": {"content": {"b": "c"}}}
 * Output: &lt;a&gt;&lt;b&gt;c&lt;/b&gt;&lt;/a&gt;  (content key is lost!)
 * </pre>
 * </p>
 * 
 * <p>
 * <strong>This implementation:</strong>
 * <pre>
 * Input:  {"a": {"content": {"b": "c"}}}
 * Output: &lt;a&gt;&lt;content&gt;&lt;b&gt;c&lt;/b&gt;&lt;/content&gt;&lt;/a&gt;  (content preserved!)
 * </pre>
 * </p>
 * 
 * <h3>Design Principles</h3>
 * <ul>
 *   <li>All JSON keys are treated equally - no special cases</li>
 *   <li>Simple, predictable JSON→XML mapping</li>
 *   <li>Handles nested objects and arrays</li>
 *   <li>Properly escapes XML special characters</li>
 * </ul>
 * 
 * @author Web-Harvest Team
 * @version 2.2.1
 * @since 2.2.1 (Bug #61 fix)
 */
public class JsonXmlConverter {

    /**
     * Convert a JSONObject to XML string without special "content" handling.
     * 
     * @param jsonObject The JSON object to convert
     * @param rootTag The root tag name for the XML output
     * @return XML string representation
     * @throws JSONException if JSON processing fails
     */
    public static String toXml(JSONObject jsonObject, String rootTag) throws JSONException {
        StringBuilder xml = new StringBuilder();
        
        if (rootTag != null && !rootTag.isEmpty()) {
            xml.append("<").append(rootTag).append(">");
        }
        
        appendJsonObject(xml, jsonObject);
        
        if (rootTag != null && !rootTag.isEmpty()) {
            xml.append("</").append(rootTag).append(">");
        }
        
        return xml.toString();
    }

    /**
     * Convert a JSONArray to XML string.
     * 
     * @param jsonArray The JSON array to convert
     * @param itemTag The tag name for each array item
     * @return XML string representation
     * @throws JSONException if JSON processing fails
     */
    public static String toXml(JSONArray jsonArray, String itemTag) throws JSONException {
        StringBuilder xml = new StringBuilder();
        
        String tag = (itemTag != null && !itemTag.isEmpty()) ? itemTag : "item";
        
        for (int i = 0; i < jsonArray.length(); i++) {
            Object item = jsonArray.get(i);
            xml.append("<").append(tag).append(">");
            appendValue(xml, item);
            xml.append("</").append(tag).append(">");
        }
        
        return xml.toString();
    }

    /**
     * Append JSONObject content to XML StringBuilder.
     * All keys (including "content") are treated as regular XML elements.
     */
    private static void appendJsonObject(StringBuilder xml, JSONObject jsonObject) throws JSONException {
        String[] keys = JSONObject.getNames(jsonObject);
        
        if (keys == null || keys.length == 0) {
            return;
        }
        
        for (String key : keys) {
            Object value = jsonObject.get(key);
            
            // NO SPECIAL CASE FOR "content" - this is the Bug #61 fix!
            // All keys are treated equally as XML elements
            
            xml.append("<").append(escapeXmlTag(key)).append(">");
            appendValue(xml, value);
            xml.append("</").append(escapeXmlTag(key)).append(">");
        }
    }

    /**
     * Append a JSON value (object, array, or primitive) to XML.
     */
    private static void appendValue(StringBuilder xml, Object value) throws JSONException {
        if (value == null || value == JSONObject.NULL) {
            // Empty element for null
            return;
        } else if (value instanceof JSONObject) {
            // Nested object
            appendJsonObject(xml, (JSONObject) value);
        } else if (value instanceof JSONArray) {
            // Array - each item as separate element
            JSONArray array = (JSONArray) value;
            for (int i = 0; i < array.length(); i++) {
                xml.append("<item>");
                appendValue(xml, array.get(i));
                xml.append("</item>");
            }
        } else if (value instanceof Boolean) {
            xml.append(value.toString());
        } else if (value instanceof Number) {
            xml.append(value.toString());
        } else {
            // String - escape XML special characters
            xml.append(escapeXmlContent(value.toString()));
        }
    }

    /**
     * Escape XML tag name (remove invalid characters).
     */
    private static String escapeXmlTag(String tag) {
        if (tag == null || tag.isEmpty()) {
            return "element";
        }
        
        // Replace spaces and special chars with underscore
        return tag.replaceAll("[^a-zA-Z0-9_-]", "_");
    }

    /**
     * Escape XML content (special characters).
     */
    private static String escapeXmlContent(String content) {
        if (content == null) {
            return "";
        }
        
        return content
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;");
    }
}

