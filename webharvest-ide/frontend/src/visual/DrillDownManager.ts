/**
 * DrillDownManager - Implementation
 * Focus mode for drilling into containers
 */

export class DrillDownManager {
    private readonly stack: string[] = [];

    /**
     * Enter a container by ID (push onto stack)
     */
    enter(containerId: string): void {
        if (!containerId) {
            return;
        }
        const last = this.stack[this.stack.length - 1];
        if (last === containerId) {
            return; // prevent duplicate pushes
        }
        this.stack.push(containerId);
        console.log(`[DrillDownManager] Entered ${containerId} (level ${this.stack.length})`);
    }

    /**
     * Exit current container (pop from stack)
     */
    exit(): string | null {
        if (this.stack.length === 0) {
            return null;
        }
        const popped = this.stack.pop() ?? null;
        console.log(`[DrillDownManager] Exited ${popped ?? 'root'} (level ${this.stack.length})`);
        return popped;
    }

    /**
     * Navigate directly to a requested level
     * Level 0 resets to root (clears the stack)
     */
    navigateToLevel(level: number): void {
        if (level <= 0) {
            this.reset();
            return;
        }

        if (level >= this.stack.length) {
            return;
        }

        this.stack.splice(level);
        console.log(`[DrillDownManager] Navigated to level ${level}`);
    }

    /**
     * Get ID at current level or null when at root
     */
    getCurrentId(): string | null {
        if (this.stack.length === 0) {
            return null;
        }
        return this.stack[this.stack.length - 1];
    }

    /**
     * Reset state to root
     */
    reset(): void {
        this.stack.length = 0;
        console.log('[DrillDownManager] Reset to root');
    }

    /**
     * Get shallow copy of the current trail
     */
    getTrail(): string[] {
        return [...this.stack];
    }
}


