/**
 * ExecutionStateManager - Implementation
 * Manages per-node execution state and integrates with WebSocket updates
 */

import type { VisualNode } from '../types';
import type { NodeExecutionState as ExecutionStatus } from '../visual/NodeStateRenderer';

export interface NodeExecutionUpdate {
    nodeId: string;
    pluginType: string;
    status: ExecutionStatus;
    progress?: number;
    startTime?: number;
    endTime?: number;
    metrics?: {
        dataProcessed?: number;
        httpRequests?: number;
        memoryUsed?: number;
    };
    variables?: Record<string, any>;
    error?: {
        message: string;
        stackTrace: string;
    };
}

export interface NodeStateInfo {
    nodeId: string;
    pluginType: string;
    status: ExecutionStatus;
    progress?: number;
    startTime?: number;
    endTime?: number;
    elapsedTime?: number;
    metrics?: {
        dataProcessed?: number;
        httpRequests?: number;
        memoryUsed?: number;
    };
    variables?: Record<string, any>;
    error?: {
        message: string;
        stackTrace: string;
    };
}

export class ExecutionStateManager {
    private nodeStates: Map<string, NodeStateInfo> = new Map();
    private nodeXPathMap: Map<string, string> = new Map(); // XPath -> nodeId
    private registeredNodes: VisualNode[] = [];

    /**
     * Register visual nodes for execution tracking
     */
    registerNodes(nodes: VisualNode[]): void {
        this.nodeStates.clear();
        this.nodeXPathMap.clear();
        this.registeredNodes = [...nodes];

        // Initialize state for each node
        nodes.forEach((node, index) => {
            this.nodeStates.set(node.id, {
                nodeId: node.id,
                pluginType: node.type,
                status: 'IDLE'
            });

            // Create XPath mapping (simplified: /config/pluginType[index])
            const xpath = `/config/${node.type}[${index + 1}]`;
            this.nodeXPathMap.set(xpath, node.id);
        });

        console.log(`[ExecutionStateManager] Registered ${nodes.length} nodes for tracking`);
    }

    /**
     * Update node execution state
     */
    updateNodeState(update: NodeExecutionUpdate): void {
        const currentState = this.nodeStates.get(update.nodeId);

        if (!currentState) {
            console.warn(`[ExecutionStateManager] Node ${update.nodeId} not registered`);
            return;
        }

        // Calculate elapsed time if running
        let elapsedTime = currentState.elapsedTime;
        if (update.startTime) {
            elapsedTime = Date.now() - update.startTime;
        } else if (currentState.startTime && !update.endTime) {
            elapsedTime = Date.now() - currentState.startTime;
        }

        // Merge update with current state
        const newState: NodeStateInfo = {
            ...currentState,
            ...update,
            elapsedTime,
            metrics: {
                ...currentState.metrics,
                ...update.metrics
            },
            variables: {
                ...currentState.variables,
                ...update.variables
            }
        };

        this.nodeStates.set(update.nodeId, newState);

        console.log(`[ExecutionStateManager] Updated ${update.nodeId}: ${update.status}`, newState);
    }

    /**
     * Get current state of a node
     */
    getNodeState(nodeId: string): NodeStateInfo | null {
        return this.nodeStates.get(nodeId) || null;
    }

    /**
     * Get all node states
     */
    getAllStates(): Record<string, NodeStateInfo> {
        const states: Record<string, NodeStateInfo> = {};
        this.nodeStates.forEach((state, nodeId) => {
            states[nodeId] = state;
        });
        return states;
    }

    /**
     * Reset all node states to IDLE
     */
    reset(): void {
        this.nodeStates.clear();
        this.nodeXPathMap.clear();
        this.registeredNodes = [];
        console.log('[ExecutionStateManager] Reset all states');
    }

    /**
     * Map XPath expression to node ID
     * Used to correlate XML execution events with visual nodes
     */
    mapXPathToNodeId(xpath: string): string | null {
        return this.nodeXPathMap.get(xpath) || null;
    }

    /**
     * Calculate overall execution progress (% of nodes completed)
     */
    getOverallProgress(): number {
        if (this.nodeStates.size === 0) return 0;

        let completed = 0;
        this.nodeStates.forEach(state => {
            if (state.status === 'SUCCESS' || state.status === 'ERROR' || state.status === 'SKIPPED') {
                completed++;
            }
        });

        return Math.round((completed / this.nodeStates.size) * 100);
    }

    /**
     * Get currently running nodes
     */
    getRunningNodes(): string[] {
        const running: string[] = [];
        this.nodeStates.forEach((state, nodeId) => {
            if (state.status === 'RUNNING') {
                running.push(nodeId);
            }
        });
        return running;
    }

    /**
     * Get failed nodes
     */
    getFailedNodes(): string[] {
        const failed: string[] = [];
        this.nodeStates.forEach((state, nodeId) => {
            if (state.status === 'ERROR') {
                failed.push(nodeId);
            }
        });
        return failed;
    }

    /**
     * Get execution summary statistics
     */
    getSummary(): {
        total: number;
        idle: number;
        running: number;
        success: number;
        error: number;
        paused: number;
        skipped: number;
    } {
        const summary = {
            total: this.nodeStates.size,
            idle: 0,
            running: 0,
            success: 0,
            error: 0,
            paused: 0,
            skipped: 0
        };

        this.nodeStates.forEach(state => {
            switch (state.status) {
                case 'IDLE': summary.idle++; break;
                case 'RUNNING': summary.running++; break;
                case 'SUCCESS': summary.success++; break;
                case 'ERROR': summary.error++; break;
                case 'PAUSED': summary.paused++; break;
                case 'SKIPPED': summary.skipped++; break;
            }
        });

        return summary;
    }
}

