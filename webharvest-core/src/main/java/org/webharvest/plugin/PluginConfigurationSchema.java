/*  Copyright (c) 2006-2025 the original author or authors.
    All rights reserved.

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

    You can contact Vladimir Nikic by sending e-mail to
    nikic_vladimir@yahoo.com. Please include the word "Web-Harvest" in the
    subject line.
*/
package org.webharvest.plugin;

import java.util.List;
import java.util.Map;
import org.webharvest.plugin.exceptions.PluginValidationException;

/**
 * Schema definition for plugin configuration.
 * Defines the structure, validation rules, and constraints for plugin configuration.
 * 
 * @author Robert Bala
 * @since 2.1.0
 */
public interface PluginConfigurationSchema {

    /**
     * Gets all defined attributes.
     * 
     * @return array of attribute definitions
     */
    AttributeDefinition[] getAttributeDefinitions();

    /**
     * Gets attribute definition by name.
     * 
     * @param name attribute name
     * @return attribute definition, or null if not found
     */
    AttributeDefinition getAttribute(String name);

    /**
     * Gets required attributes.
     * 
     * @return array of required attribute names
     */
    String[] getRequiredAttributes();

    /**
     * Gets optional attributes.
     * 
     * @return array of optional attribute names
     */
    String[] getOptionalAttributes();

    /**
     * Gets allowed sub-elements.
     * 
     * @return array of allowed sub-element names
     */
    String[] getAllowedSubElements();

    /**
     * Gets required sub-elements.
     * 
     * @return array of required sub-element names
     */
    String[] getRequiredSubElements();

    /**
     * Checks if the schema allows body content.
     * 
     * @return true if body content is allowed, false otherwise
     */
    boolean allowsBody();

    /**
     * Gets the schema version.
     * 
     * @return schema version
     */
    String getVersion();

    /**
     * Validates a configuration against this schema.
     * 
     * @param configuration configuration to validate
     * @throws PluginValidationException if validation fails
     */
    void validate(PluginConfiguration configuration) throws PluginValidationException;

    /**
     * Definition of a configuration attribute.
     */
    public static interface AttributeDefinition {
        /**
         * Gets the attribute name.
         * 
         * @return attribute name
         */
        String getName();

        /**
         * Gets the attribute type.
         * 
         * @return attribute type
         */
        AttributeType getType();

        /**
         * Checks if the attribute is required.
         * 
         * @return true if required, false otherwise
         */
        boolean isRequired();

        /**
         * Gets the default value.
         * 
         * @return default value, or null if none
         */
        String getDefaultValue();

        /**
         * Gets the attribute description.
         * 
         * @return attribute description
         */
        String getDescription();

        /**
         * Gets the allowed values (for enum types).
         * 
         * @return array of allowed values, or null if not applicable
         */
        String[] getAllowedValues();

        /**
         * Gets the minimum value (for numeric types).
         * 
         * @return minimum value, or null if not applicable
         */
        Number getMinValue();

        /**
         * Gets the maximum value (for numeric types).
         * 
         * @return maximum value, or null if not applicable
         */
        Number getMaxValue();

        /**
         * Gets the regular expression pattern (for string types).
         * 
         * @return regex pattern, or null if not applicable
         */
        String getPattern();
    }

    /**
     * Supported attribute types.
     */
    public static enum AttributeType {
        STRING,
        INTEGER,
        LONG,
        DOUBLE,
        BOOLEAN,
        ENUM,
        URL,
        FILE_PATH,
        REGEX
    }
}
