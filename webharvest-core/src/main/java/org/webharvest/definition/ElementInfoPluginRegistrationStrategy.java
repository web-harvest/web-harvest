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

package org.webharvest.definition;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.webharvest.runtime.processors.Processor;
import org.webharvest.annotation.Definition;
import org.webharvest.exception.PluginException;

/**
 * ElementInfo-based implementation of PluginRegistrationStrategy.
 * This strategy uses the newer ElementInfo approach for registering plugins.
 * 
 * @author Robert Bala
 * @since 2.1.0-SNAPSHOT
 */
public class ElementInfoPluginRegistrationStrategy implements PluginRegistrationStrategy {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(ElementInfoPluginRegistrationStrategy.class);
    
    @Override
    public void registerPlugin(Class<? extends Processor> plugin, 
                              ConfigurableResolver resolver) throws PluginException {
        final Definition definition = plugin.getAnnotation(Definition.class);
        
        if (definition == null) {
            throw new PluginException("Plugin " + plugin.getCanonicalName() + 
                                    " does not have @Definition annotation");
        }
        
        final ElementInfo elementInfo = new ElementInfo(
                definition.value(), 
                WebHarvestPluginDef.class, 
                plugin, 
                null, 
                null, 
                false, 
                null);

        // Register plugin under default namespace
        LOGGER.info("Registering plugin {} under default namespace",
                plugin.getCanonicalName());
        resolver.registerPlugin(elementInfo, "http://org.webharvest/schema/2.1/core");
    }
}
