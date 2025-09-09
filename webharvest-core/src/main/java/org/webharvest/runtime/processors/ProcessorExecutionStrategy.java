package org.webharvest.runtime.processors;

import org.webharvest.runtime.DynamicScopeContext;
import org.webharvest.runtime.variables.Variable;

/**
 * Strategy interface for processor execution.
 * Allows different execution strategies to be applied.
 * 
 * @author Robert Bala
 * @since 2.1.0-SNAPSHOT
 */
public interface ProcessorExecutionStrategy {
    
    /**
     * Executes the processor with the given context.
     * 
     * @param processor the processor to execute
     * @param context the execution context
     * @return the execution result
     * @throws InterruptedException if execution is interrupted
     */
    Variable execute(Processor<?> processor, DynamicScopeContext context) throws InterruptedException;
}




