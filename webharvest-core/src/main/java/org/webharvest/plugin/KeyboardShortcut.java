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
 * Keyboard shortcut definition for Apple HIG compliance.
 * 
 * <p>Defines keyboard shortcuts for plugin actions, following Apple Human Interface
 * Guidelines for keyboard navigation and accessibility.</p>
 * 
 * @author Robert Bala
 * @since 2.3.0
 */
public final class KeyboardShortcut {
    
    private final String key;
    private final String action;
    private final boolean isGlobal;
    
    /**
     * Creates a new keyboard shortcut definition.
     * 
     * @param key key combination (e.g., "Cmd+N", "Cmd+Shift+H", "Ctrl+Enter")
     * @param action action description (e.g., "Add HTTP plugin", "Execute")
     * @param isGlobal true if global shortcut, false if context-specific
     */
    public KeyboardShortcut(String key, String action, boolean isGlobal) {
        this.key = key;
        this.action = action;
        this.isGlobal = isGlobal;
    }
    
    /**
     * Gets the key combination.
     * 
     * @return key combination string
     */
    public String getKey() {
        return key;
    }
    
    /**
     * Gets the action description.
     * 
     * @return action description
     */
    public String getAction() {
        return action;
    }
    
    /**
     * Checks if this is a global shortcut.
     * 
     * @return true if global, false if context-specific
     */
    public boolean isGlobal() {
        return isGlobal;
    }
    
    @Override
    public String toString() {
        return "KeyboardShortcut{key='" + key + "', action='" + action + "', global=" + isGlobal + "}";
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        KeyboardShortcut that = (KeyboardShortcut) o;
        return isGlobal == that.isGlobal &&
               key.equals(that.key) &&
               action.equals(that.action);
    }
    
    @Override
    public int hashCode() {
        int result = key.hashCode();
        result = 31 * result + action.hashCode();
        result = 31 * result + (isGlobal ? 1 : 0);
        return result;
    }
}

