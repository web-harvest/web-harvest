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
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF THE POSSIBILITY
 * OF SUCH DAMAGE.
 */

package org.webharvest.definition;

import org.webharvest.plugin.Plugin;
import org.webharvest.runtime.processors.Processor;

/**
 * Extended ElementInfo for core plugins that stores the actual plugin class.
 * This allows the system to know which specific plugin to instantiate.
 *
 * @author Robert Bala
 * @since 2.1.0
 * @version %I%, %G%
 */
public class CorePluginElementInfo extends ElementInfo {

    private final Class<? extends Plugin> pluginClass;

    /**
     * Creates a new CorePluginElementInfo.
     *
     * @param name the element name
     * @param definitionClass the definition class
     * @param processorClass the processor class
     * @param validTags valid tags
     * @param validAtts valid attributes
     * @param isInternal whether this is an internal plugin
     * @param dependantProcessors dependent processors
     * @param pluginClass the actual plugin class
     */
    public CorePluginElementInfo(String name,
                                Class<? extends IElementDef> definitionClass,
                                Class<? extends Processor> processorClass,
                                String validTags, String validAtts,
                                boolean isInternal,
                                Class[] dependantProcessors,
                                Class<? extends Plugin> pluginClass) {
        super(name, definitionClass, processorClass, validTags, validAtts, isInternal, dependantProcessors);
        this.pluginClass = pluginClass;
    }

    /**
     * Gets the actual plugin class.
     *
     * @return the plugin class
     */
    public Class<? extends Plugin> getPluginClass() {
        return pluginClass;
    }
}

















