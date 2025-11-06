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
 * Accessibility metadata for Apple HIG compliance.
 * 
 * <p>Provides accessibility information for screen readers and keyboard navigation,
 * following Apple Human Interface Guidelines and WCAG standards.</p>
 * 
 * @author Robert Bala
 * @since 2.3.0
 */
public final class AccessibilityMetadata {
    
    private final String ariaLabel;
    private final String ariaDescription;
    private final String[] keyboardNavigation;
    
    /**
     * Creates new accessibility metadata.
     * 
     * @param ariaLabel ARIA label for screen readers (should be concise)
     * @param ariaDescription extended ARIA description for screen readers
     * @param keyboardNavigation array of keyboard navigation hints (e.g., ["Tab", "Enter", "Escape"])
     */
    public AccessibilityMetadata(String ariaLabel, String ariaDescription, String[] keyboardNavigation) {
        this.ariaLabel = ariaLabel;
        this.ariaDescription = ariaDescription;
        this.keyboardNavigation = keyboardNavigation != null ? keyboardNavigation.clone() : new String[0];
    }
    
    /**
     * Gets the ARIA label.
     * 
     * @return ARIA label
     */
    public String getAriaLabel() {
        return ariaLabel;
    }
    
    /**
     * Gets the ARIA description.
     * 
     * @return ARIA description
     */
    public String getAriaDescription() {
        return ariaDescription;
    }
    
    /**
     * Gets keyboard navigation hints.
     * 
     * @return array of keyboard navigation keys
     */
    public String[] getKeyboardNavigation() {
        return keyboardNavigation.clone();
    }
    
    @Override
    public String toString() {
        return "AccessibilityMetadata{ariaLabel='" + ariaLabel + "'}";
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AccessibilityMetadata that = (AccessibilityMetadata) o;
        return ariaLabel.equals(that.ariaLabel);
    }
    
    @Override
    public int hashCode() {
        return ariaLabel.hashCode();
    }
}

