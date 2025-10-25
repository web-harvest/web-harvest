/**
 * Simple EventEmitter implementation for TypeScript
 * 
 * Provides event-driven architecture for components
 */

type EventHandler = (...args: any[]) => void;

export class EventEmitter {
  private events: Map<string, EventHandler[]> = new Map();
  
  /**
   * Register an event handler
   */
  public on(event: string, handler: EventHandler): void {
    if (!this.events.has(event)) {
      this.events.set(event, []);
    }
    this.events.get(event)!.push(handler);
  }
  
  /**
   * Unregister an event handler
   */
  public off(event: string, handler: EventHandler): void {
    if (!this.events.has(event)) return;
    
    const handlers = this.events.get(event)!;
    const index = handlers.indexOf(handler);
    
    if (index !== -1) {
      handlers.splice(index, 1);
    }
  }
  
  /**
   * Emit an event
   */
  public emit(event: string, ...args: any[]): void {
    if (!this.events.has(event)) return;
    
    const handlers = this.events.get(event)!;
    for (const handler of handlers) {
      try {
        handler(...args);
      } catch (error) {
        console.error(`Error in event handler for "${event}":`, error);
      }
    }
  }
  
  /**
   * Register a one-time event handler
   */
  public once(event: string, handler: EventHandler): void {
    const onceHandler = (...args: any[]) => {
      handler(...args);
      this.off(event, onceHandler);
    };
    this.on(event, onceHandler);
  }
  
  /**
   * Remove all handlers for an event
   */
  public removeAllListeners(event?: string): void {
    if (event) {
      this.events.delete(event);
    } else {
      this.events.clear();
    }
  }
}


