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

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Default implementation of PluginConfiguration.
 * Provides functionality to store and retrieve plugin configuration properties.
 * 
 * @author Robert Bala
 * @since 2.1.0
 */
public class DefaultPluginConfiguration implements PluginConfiguration {

    private final Map<String, String> properties;
    private final String namespace;
    private final String elementName;
    private final String pluginId;

    /**
     * Constructor for creating a plugin configuration.
     * 
     * @param pluginId the plugin ID
     * @param namespace the plugin namespace
     * @param elementName the element name
     */
    public DefaultPluginConfiguration(String pluginId, String namespace, String elementName) {
        this.pluginId = pluginId;
        this.namespace = namespace;
        this.elementName = elementName;
        this.properties = new HashMap<>();
    }

    /**
     * Constructor for creating a plugin configuration with initial properties.
     * 
     * @param pluginId the plugin ID
     * @param namespace the plugin namespace
     * @param elementName the element name
     * @param properties initial properties
     */
    public DefaultPluginConfiguration(String pluginId, String namespace, String elementName, 
                                    Map<String, String> properties) {
        this.pluginId = pluginId;
        this.namespace = namespace;
        this.elementName = elementName;
        this.properties = new HashMap<>(properties);
    }

    /**
     * Constructor for creating a plugin configuration from Properties.
     * 
     * @param pluginId the plugin ID
     * @param namespace the plugin namespace
     * @param elementName the element name
     * @param properties Properties object
     */
    public DefaultPluginConfiguration(String pluginId, String namespace, String elementName, 
                                    Properties properties) {
        this.pluginId = pluginId;
        this.namespace = namespace;
        this.elementName = elementName;
        this.properties = new HashMap<>();
        
        if (properties != null) {
            for (String key : properties.stringPropertyNames()) {
                this.properties.put(key, properties.getProperty(key));
            }
        }
    }

    @Override
    public String getProperty(String name) {
        return properties.get(name);
    }

    @Override
    public String getProperty(String name, String defaultValue) {
        String value = properties.get(name);
        return value != null ? value : defaultValue;
    }

    @Override
    public boolean getBooleanProperty(String name, boolean defaultValue) {
        String value = properties.get(name);
        if (value == null) {
            return defaultValue;
        }
        
        try {
            return Boolean.parseBoolean(value);
        } catch (Exception e) {
            return defaultValue;
        }
    }

    @Override
    public int getIntProperty(String name, int defaultValue) {
        String value = properties.get(name);
        if (value == null) {
            return defaultValue;
        }
        
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    @Override
    public long getLongProperty(String name, long defaultValue) {
        String value = properties.get(name);
        if (value == null) {
            return defaultValue;
        }
        
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    @Override
    public double getDoubleProperty(String name, double defaultValue) {
        String value = properties.get(name);
        if (value == null) {
            return defaultValue;
        }
        
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    @Override
    public Map<String, String> getAllProperties() {
        return new HashMap<>(properties);
    }

    @Override
    public Properties getProperties() {
        Properties props = new Properties();
        props.putAll(properties);
        return props;
    }

    @Override
    public boolean hasProperty(String name) {
        return properties.containsKey(name);
    }

    @Override
    public String getNamespace() {
        return namespace;
    }

    @Override
    public String getElementName() {
        return elementName;
    }

    @Override
    public String getPluginId() {
        return pluginId;
    }

    /**
     * Sets a property value.
     * 
     * @param name property name
     * @param value property value
     */
    public void setProperty(String name, String value) {
        properties.put(name, value);
    }

    /**
     * Sets a boolean property value.
     * 
     * @param name property name
     * @param value property value
     */
    public void setProperty(String name, boolean value) {
        properties.put(name, String.valueOf(value));
    }

    /**
     * Sets an integer property value.
     * 
     * @param name property name
     * @param value property value
     */
    public void setProperty(String name, int value) {
        properties.put(name, String.valueOf(value));
    }

    /**
     * Sets a long property value.
     * 
     * @param name property name
     * @param value property value
     */
    public void setProperty(String name, long value) {
        properties.put(name, String.valueOf(value));
    }

    /**
     * Sets a double property value.
     * 
     * @param name property name
     * @param value property value
     */
    public void setProperty(String name, double value) {
        properties.put(name, String.valueOf(value));
    }

    /**
     * Removes a property.
     * 
     * @param name property name
     * @return the removed value, or null if not found
     */
    public String removeProperty(String name) {
        return properties.remove(name);
    }

    /**
     * Clears all properties.
     */
    public void clear() {
        properties.clear();
    }

    /**
     * Gets the number of properties.
     * 
     * @return number of properties
     */
    public int size() {
        return properties.size();
    }

    /**
     * Checks if the configuration is empty.
     * 
     * @return true if empty, false otherwise
     */
    public boolean isEmpty() {
        return properties.isEmpty();
    }

    @Override
    public String toString() {
        return "DefaultPluginConfiguration{" +
                "pluginId='" + pluginId + '\'' +
                ", namespace='" + namespace + '\'' +
                ", elementName='" + elementName + '\'' +
                ", properties=" + properties +
                '}';
    }
}


