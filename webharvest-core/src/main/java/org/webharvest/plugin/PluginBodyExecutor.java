/*  Copyright (c) 2006-2025, Robert Bala
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
*/
package org.webharvest.plugin;

import org.webharvest.runtime.DynamicScopeContext;
import org.webharvest.runtime.variables.Variable;

/**
 * Interface for executing plugin body content in the new architecture (v2.2.x).
 * 
 * v2.2.x ULTRA-LEAN: Uses PluginConfiguration.getChildren() directly
 * instead of IElementDef.getOperationDefs().
 * 
 * @author Robert Bala
 * @since 2.2.0
 */
public interface PluginBodyExecutor {
    
    /**
     * Executes the body content of a plugin by running all child plugins.
     * 
     * @param config plugin configuration containing children
     * @param context scraper execution context
     * @return result of body execution (usually ListVariable with child results)
     * @throws InterruptedException if execution is interrupted
     */
    Variable executeBody(PluginConfiguration config, DynamicScopeContext context) 
            throws InterruptedException;
    
    /**
     * Gets the body content as text (concatenates text from all children).
     * 
     * @param config plugin configuration containing children
     * @param context scraper execution context
     * @return text content of body
     * @throws InterruptedException if execution is interrupted
     */
    Variable getBodyTextContent(PluginConfiguration config, DynamicScopeContext context) 
            throws InterruptedException;
}


