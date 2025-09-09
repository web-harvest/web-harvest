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

package org.webharvest.gui;

/**
 * Class definining elements in list of plugins. Each item has name (fully qualified class name)
 * and optional error message telling why plugin cannot be registered.
 */
public class PluginInfo {

    private String className;
    private String uri;
    private String errorMessage;

    public PluginInfo(String className, String uri, String errorMessage) {
        this.className = className;
        this.uri = uri;
        this.errorMessage = errorMessage;
    }

    public boolean isValid() {
        return errorMessage == null;
    }

    public String getClassName() {
        return className;
    }

    public String getUri() {
        return uri;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public String toString() {
        return className;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof PluginInfo) {
            PluginInfo pluginInfo = (PluginInfo) obj;
            return className != null && className.equals(pluginInfo.className) &&
                   uri != null && uri.equals(pluginInfo.uri);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return className != null ? className.hashCode() : 0;
    }
}