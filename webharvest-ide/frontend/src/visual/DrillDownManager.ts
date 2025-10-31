/**
 * DrillDownManager - Implementation
 * Focus mode for drilling into containers
 */

import type { VisualNode } from '../types';

export interface DrillDownState {
    containerId: string;
    containerNode: VisualNode;
    level: number;
}

export class DrillDownManager {
    private navigationStack: DrillDownState[] = [];
    private currentLevel: number = 0;

    drillInto(container: VisualNode): void {
        this.navigationStack.push({
            containerId: container.id,
            containerNode: container,
            level: this.currentLevel++
        });
        console.log(`[DrillDownManager] Drilled into ${container.type} (level ${this.currentLevel})`);
    }

    drillOut(): DrillDownState | null {
        const popped = this.navigationStack.pop();
        if (popped) {
            this.currentLevel--;
        }
        return popped || null;
    }

    getCurrentContainer(): VisualNode | null {
        if (this.navigationStack.length === 0) return null;
        return this.navigationStack[this.navigationStack.length - 1].containerNode;
    }

    getLevel(): number {
        return this.currentLevel;
    }

    reset(): void {
        this.navigationStack = [];
        this.currentLevel = 0;
    }

    getStack(): DrillDownState[] {
        return [...this.navigationStack];
    }
}


