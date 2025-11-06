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
 * Parent-child relationship definition for master/detail plugin patterns.
 * 
 * <p>Defines relationships between parent (master) and child (detail) plugins.
 * For example, {@code <http>} is a master plugin that allows {@code <http-header>}
 * and {@code <http-param>} as detail plugins.</p>
 * 
 * <p>This integrates with existing {@code PluginSemantics} for consistency.</p>
 * 
 * @author Robert Bala
 * @since 2.3.0
 */
public final class ParentChildRelationship {
    
    private final String parentElementName;
    private final boolean required;
    private final String parentDisplayName;
    
    /**
     * Creates a new parent-child relationship definition.
     * 
     * @param parentElementName XML element name of the parent plugin (e.g., "http", "xquery")
     * @param required true if this parent is required (detail plugin cannot exist standalone)
     * @param parentDisplayName human-readable display name of the parent (e.g., "HTTP Request")
     */
    public ParentChildRelationship(String parentElementName, boolean required, String parentDisplayName) {
        this.parentElementName = parentElementName;
        this.required = required;
        this.parentDisplayName = parentDisplayName;
    }
    
    /**
     * Gets the parent element name.
     * 
     * @return XML element name of parent
     */
    public String getParentElementName() {
        return parentElementName;
    }
    
    /**
     * Checks if the parent is required.
     * 
     * @return true if required, false if optional
     */
    public boolean isRequired() {
        return required;
    }
    
    /**
     * Gets the parent display name.
     * 
     * @return human-readable display name
     */
    public String getParentDisplayName() {
        return parentDisplayName;
    }
    
    @Override
    public String toString() {
        return "ParentChildRelationship{parent='" + parentElementName + "', required=" + required + "}";
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ParentChildRelationship that = (ParentChildRelationship) o;
        return required == that.required &&
               parentElementName.equals(that.parentElementName);
    }
    
    @Override
    public int hashCode() {
        int result = parentElementName.hashCode();
        result = 31 * result + (required ? 1 : 0);
        return result;
    }
}

