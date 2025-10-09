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

package org.webharvest.runtime.processors;

import java.util.Map;

import org.webharvest.definition.IElementDef;
import org.webharvest.runtime.DynamicScopeContext;
import org.webharvest.runtime.variables.Variable;

/**
 * Context object that holds execution state and configuration.
 * Encapsulates execution-related data and operations.
 * 
 * @author Robert Bala
 * @since 2.1.0-SNAPSHOT
 */
public final class ProcessorExecutionContext {
    
    private final Processor<?> processor;
    private final DynamicScopeContext context;
    private final Map<String, Object> properties;
    private final long startTime;
    private final String processorId;
    
    public ProcessorExecutionContext(Processor<?> processor, 
                                   DynamicScopeContext context,
                                   Map<String, Object> properties,
                                   long startTime,
                                   String processorId) {
        this.processor = processor;
        this.context = context;
        this.properties = properties;
        this.startTime = startTime;
        this.processorId = processorId;
    }
    
    public Processor<?> getProcessor() {
        return processor;
    }
    
    public DynamicScopeContext getContext() {
        return context;
    }
    
    public Map<String, Object> getProperties() {
        return properties;
    }
    
    public long getStartTime() {
        return startTime;
    }
    
    public String getProcessorId() {
        return processorId;
    }
    
    public long getExecutionTime() {
        return System.currentTimeMillis() - startTime;
    }
}












