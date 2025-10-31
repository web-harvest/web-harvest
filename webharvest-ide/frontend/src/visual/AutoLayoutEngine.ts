/**
 * AutoLayoutEngine - Implementation
 * Automatic sequential flow with manual override option
 */

import type { VisualNode } from '../types';
import type { Connection } from './ConnectionManager';

export class AutoLayoutEngine {
    private autoMode: boolean = true;
    private connections: Connection[] = [];

    autoConnect(nodes: VisualNode[]): Connection[] {
        this.connections = [];

        for (let i = 0; i < nodes.length - 1; i++) {
            this.connections.push({
                id: `auto-conn-${i}`,
                from: nodes[i].id,
                to: nodes[i + 1].id,
                type: 'sequential'
            });
        }

        return this.connections;
    }

    convertToManual(): Connection[] {
        this.autoMode = false;
        return [...this.connections];
    }

    isAutoMode(): boolean {
        return this.autoMode;
    }
}


