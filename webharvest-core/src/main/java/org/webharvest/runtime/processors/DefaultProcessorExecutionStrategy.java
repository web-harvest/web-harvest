package org.webharvest.runtime.processors;

import org.webharvest.runtime.DynamicScopeContext;
import org.webharvest.runtime.variables.Variable;

/**
 * Default implementation of processor execution strategy.
 * Provides standard execution flow with logging and event handling.
 * 
 * @author Robert Bala
 * @since 2.1.0-SNAPSHOT
 */
public class DefaultProcessorExecutionStrategy implements ProcessorExecutionStrategy {
    
    @Override
    public Variable execute(Processor<?> processor, DynamicScopeContext context) throws InterruptedException {
        // Check for interruption
        if (Thread.currentThread().isInterrupted()) {
            throw new InterruptedException();
        }
        
        // Execute the processor's main logic
        if (processor instanceof AbstractProcessor) {
            return ((AbstractProcessor<?>) processor).execute(context);
        } else {
            // Fallback for other processor types
            return processor.run(context);
        }
    }
}
