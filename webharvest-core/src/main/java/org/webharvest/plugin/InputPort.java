/*
 * Copyright (c) 2006-2025 the original author or authors.
 *
 * Redistribution and use of this software in source and binary forms,
 * with or without modification, are permitted provided that the following
 * conditions are met:
 *
 * * Redistributions of source code must retain the above
 *   copyright notice, this list of conditions and the
 *   following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above
 *   copyright notice, this list of conditions and the
 *   following disclaimer in the documentation and/or other
 *   materials provided with the distribution.
 *
 * * The name of Web-Harvest may not be used to endorse or promote
 *   products derived from this software without specific prior
 *   written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

package org.webharvest.plugin;

/**
 * Definition of an input port for a plugin node.
 * 
 * <p>Input ports represent data inputs to a plugin, similar to Apache NiFi.
 * Each port has a name, description, data type, and optional requirement flag.</p>
 * 
 * @author Robert Bala
 * @since 2.3.0
 */
public final class InputPort {
    
    private final String name;
    private final String description;
    private final PortType type;
    private final boolean required;
    
    /**
     * Creates a new input port definition.
     * 
     * @param name port name (e.g., "url", "xml", "pattern")
     * @param description human-readable description
     * @param type data type for this port
     * @param required true if this input is required, false otherwise
     */
    public InputPort(String name, String description, PortType type, boolean required) {
        this.name = name;
        this.description = description;
        this.type = type;
        this.required = required;
    }
    
    /**
     * Gets the port name.
     * 
     * @return port name
     */
    public String getName() {
        return name;
    }
    
    /**
     * Gets the port description.
     * 
     * @return description
     */
    public String getDescription() {
        return description;
    }
    
    /**
     * Gets the data type for this port.
     * 
     * @return port type
     */
    public PortType getType() {
        return type;
    }
    
    /**
     * Checks if this input is required.
     * 
     * @return true if required, false otherwise
     */
    public boolean isRequired() {
        return required;
    }
    
    @Override
    public String toString() {
        return "InputPort{name='" + name + "', type=" + type + ", required=" + required + "}";
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        InputPort inputPort = (InputPort) o;
        return required == inputPort.required &&
               name.equals(inputPort.name) &&
               type == inputPort.type;
    }
    
    @Override
    public int hashCode() {
        int result = name.hashCode();
        result = 31 * result + type.hashCode();
        result = 31 * result + (required ? 1 : 0);
        return result;
    }
}

