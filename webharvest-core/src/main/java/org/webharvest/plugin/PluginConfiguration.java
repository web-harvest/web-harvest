/*  Copyright (c) 2006-2007, Vladimir Nikic
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

import java.util.Map;
import java.util.Properties;

/**
 * Configuration for a plugin.
 * Contains all configuration parameters needed to initialize and configure a plugin.
 * 
 * @author Robert Bala
 * @since 2.1.0
 */
public interface PluginConfiguration {

    /**
     * Gets a configuration property by name.
     * 
     * @param name property name
     * @return property value, or null if not found
     */
    String getProperty(String name);

    /**
     * Gets a configuration property by name with default value.
     * 
     * @param name property name
     * @param defaultValue default value if property not found
     * @return property value or default value
     */
    String getProperty(String name, String defaultValue);

    /**
     * Gets a boolean configuration property.
     * 
     * @param name property name
     * @param defaultValue default value if property not found
     * @return boolean property value
     */
    boolean getBooleanProperty(String name, boolean defaultValue);

    /**
     * Gets an integer configuration property.
     * 
     * @param name property name
     * @param defaultValue default value if property not found
     * @return integer property value
     */
    int getIntProperty(String name, int defaultValue);

    /**
     * Gets a long configuration property.
     * 
     * @param name property name
     * @param defaultValue default value if property not found
     * @return long property value
     */
    long getLongProperty(String name, long defaultValue);

    /**
     * Gets a double configuration property.
     * 
     * @param name property name
     * @param defaultValue default value if property not found
     * @return double property value
     */
    double getDoubleProperty(String name, double defaultValue);

    /**
     * Gets all configuration properties as a map.
     * 
     * @return map of all properties
     */
    Map<String, String> getAllProperties();

    /**
     * Gets all configuration properties as Properties object.
     * 
     * @return Properties object containing all properties
     */
    Properties getProperties();

    /**
     * Checks if a property exists.
     * 
     * @param name property name
     * @return true if property exists, false otherwise
     */
    boolean hasProperty(String name);

    /**
     * Gets the plugin namespace.
     * 
     * @return plugin namespace
     */
    String getNamespace();

    /**
     * Gets the plugin element name.
     * 
     * @return element name
     */
    String getElementName();

    /**
     * Gets the plugin ID.
     * 
     * @return plugin ID
     */
    String getPluginId();
}
