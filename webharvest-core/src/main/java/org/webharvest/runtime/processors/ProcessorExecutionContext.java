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




