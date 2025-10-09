/*
 Copyright (c) 2006-2012 the original author or authors.

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

package org.webharvest;

import java.util.List;

import org.webharvest.definition.IElementDef;
import org.webharvest.definition.v3.PluginConfigurationTree;

/**
 * Callback interface representing successfully loaded scraping configuration.
 * 
 * <p><strong>MODERNIZATION (v2.2.x):</strong> This interface is being modernized
 * to use {@link PluginConfigurationTree} instead of {@link IElementDef}.
 * The old method {@link #onSuccess(List)} is deprecated and will be removed in v2.3.x.
 * Use {@link #onSuccess(PluginConfigurationTree)} instead.</p>
 *
 * By default the configuration is automatically loaded when creating new
 * harvester session object. This provides opportunity to perform some actions
 * before actually executing the harvester.
 *
 * @author Robert Bala
 * @since 2.1.0-SNAPSHOT
 * @version %I%, %G%
 * @see IElementDef
 * @see PluginConfigurationTree
 * @see org.webharvest.Harvest
 */
public interface HarvestLoadCallback {

    /**
     * Invoked when successfully loaded scraping configuration (OLD API).
     *
     * @param elements loaded configuration elements as {@link IElementDef} type
     * @deprecated Use {@link #onSuccess(PluginConfigurationTree)} instead.
     *             This method will be removed in v2.3.x.
     */
    @Deprecated
    default void onSuccess(List<IElementDef> elements) {
        // Default implementation for backward compatibility
        // Override if you still need old API
    }

    /**
     * Invoked when successfully loaded scraping configuration (NEW API - v2.2.x).
     * This is the modern replacement for {@link #onSuccess(List)}.
     *
     * @param configTree loaded configuration tree
     * @since 2.2.0
     */
    default void onSuccess(PluginConfigurationTree configTree) {
        // Default implementation calls old method for backward compatibility
        // This allows gradual migration
        if (configTree != null && configTree.hasChildren()) {
            // Note: Can't easily convert PluginConfigurationTree to List<IElementDef>
            // So old implementations must override and implement new method
        }
    }

}
