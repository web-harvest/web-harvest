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
 * Definition of an output port for a plugin node.
 * 
 * <p>Output ports represent data outputs from a plugin, similar to Apache NiFi.
 * Each port has a name, description, and data type.</p>
 * 
 * @author Robert Bala
 * @since 2.3.0
 */
public final class OutputPort {
    
    private final String name;
    private final String description;
    private final PortType type;
    
    /**
     * Creates a new output port definition.
     * 
     * @param name port name (e.g., "output", "result", "error")
     * @param description human-readable description
     * @param type data type for this port
     */
    public OutputPort(String name, String description, PortType type) {
        this.name = name;
        this.description = description;
        this.type = type;
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
    
    @Override
    public String toString() {
        return "OutputPort{name='" + name + "', type=" + type + "}";
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        OutputPort that = (OutputPort) o;
        return name.equals(that.name) && type == that.type;
    }
    
    @Override
    public int hashCode() {
        int result = name.hashCode();
        result = 31 * result + type.hashCode();
        return result;
    }
}

