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
 * Linked service metadata for Azure Data Factory-style integration.
 * 
 * <p>Defines external services that can be linked to plugins, similar to
 * Azure Data Factory linked services concept.</p>
 * 
 * @author Robert Bala
 * @since 2.3.0
 */
public final class LinkedServiceMetadata {
    
    private final String serviceType;
    private final String serviceName;
    private final String description;
    private final boolean required;
    
    /**
     * Creates new linked service metadata.
     * 
     * @param serviceType type of service (e.g., "HTTP", "Database", "FTP")
     * @param serviceName name of the service
     * @param description service description
     * @param required true if this service is required for the plugin
     */
    public LinkedServiceMetadata(String serviceType, String serviceName, String description, boolean required) {
        this.serviceType = serviceType;
        this.serviceName = serviceName;
        this.description = description;
        this.required = required;
    }
    
    /**
     * Gets the service type.
     * 
     * @return service type
     */
    public String getServiceType() {
        return serviceType;
    }
    
    /**
     * Gets the service name.
     * 
     * @return service name
     */
    public String getServiceName() {
        return serviceName;
    }
    
    /**
     * Gets the service description.
     * 
     * @return description
     */
    public String getDescription() {
        return description;
    }
    
    /**
     * Checks if this service is required.
     * 
     * @return true if required, false otherwise
     */
    public boolean isRequired() {
        return required;
    }
    
    @Override
    public String toString() {
        return "LinkedServiceMetadata{serviceType='" + serviceType + "', serviceName='" + serviceName + "'}";
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LinkedServiceMetadata that = (LinkedServiceMetadata) o;
        return required == that.required &&
               serviceType.equals(that.serviceType) &&
               serviceName.equals(that.serviceName);
    }
    
    @Override
    public int hashCode() {
        int result = serviceType.hashCode();
        result = 31 * result + serviceName.hashCode();
        result = 31 * result + (required ? 1 : 0);
        return result;
    }
}

