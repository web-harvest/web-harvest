/**
 * ConnectionManager - Implementation
 * Manages manual connections between workflow nodes with validation
 */

import type { VisualNode } from '../types';

export type ConnectionType = 'sequential' | 'conditional' | 'error';

export interface Connection {
    id: string;
    from: string; // source node ID
    to: string; // target node ID
    type: ConnectionType;
    label?: string;
    condition?: string; // for conditional connections
}

export interface ConnectionValidation {
    valid: boolean;
    error?: string;
}

export class ConnectionManager {
    private connections: Map<string, Connection> = new Map();

    /**
     * Create new connection
     */
    createConnection(fromNodeId: string, toNodeId: string, type: ConnectionType, label?: string): Connection {
        const connection: Connection = {
            id: `conn-${Date.now()}-${Math.random().toString(36).substr(2, 9)}`,
            from: fromNodeId,
            to: toNodeId,
            type,
            label
        };

        this.connections.set(connection.id, connection);

        console.log(`[ConnectionManager] Created ${type} connection: ${fromNodeId} → ${toNodeId}`);

        return connection;
    }

    /**
     * Delete connection
     */
    deleteConnection(connectionId: string): boolean {
        const deleted = this.connections.delete(connectionId);

        if (deleted) {
            console.log(`[ConnectionManager] Deleted connection: ${connectionId}`);
        }

        return deleted;
    }

    /**
     * Update connection properties
     */
    updateConnection(connectionId: string, updates: Partial<Connection>): boolean {
        const connection = this.connections.get(connectionId);
        if (!connection) return false;

        const updated = { ...connection, ...updates };
        this.connections.set(connectionId, updated);

        console.log(`[ConnectionManager] Updated connection: ${connectionId}`, updates);

        return true;
    }

    /**
     * Get connection by ID
     */
    getConnection(connectionId: string): Connection | null {
        return this.connections.get(connectionId) || null;
    }

    /**
     * Get all connections
     */
    getConnections(): Connection[] {
        return Array.from(this.connections.values());
    }

    /**
     * Get connections for a specific node
     */
    getConnectionsForNode(nodeId: string, direction: 'input' | 'output'): Connection[] {
        const connections: Connection[] = [];

        this.connections.forEach(connection => {
            if (direction === 'output' && connection.from === nodeId) {
                connections.push(connection);
            } else if (direction === 'input' && connection.to === nodeId) {
                connections.push(connection);
            }
        });

        return connections;
    }

    /**
     * Validate connection before creation
     */
    validateConnection(fromNodeId: string, toNodeId: string): ConnectionValidation {
        // Rule 1: Cannot connect node to itself
        if (fromNodeId === toNodeId) {
            return {
                valid: false,
                error: 'Cannot connect node to same node'
            };
        }

        // Rule 2: Check for duplicate connection
        const exists = Array.from(this.connections.values()).some(
            conn => conn.from === fromNodeId && conn.to === toNodeId
        );

        if (exists) {
            return {
                valid: false,
                error: 'Connection already exists between these nodes'
            };
        }

        // Rule 3: Check for cycles
        if (this.wouldCreateCycle(fromNodeId, toNodeId)) {
            return {
                valid: false,
                error: 'Connection would create a cycle'
            };
        }

        return { valid: true };
    }

    /**
     * Check if connection would create cycle (DFS)
     */
    wouldCreateCycle(fromNodeId: string, toNodeId: string): boolean {
        // Build adjacency list
        const graph = new Map<string, string[]>();

        this.connections.forEach(conn => {
            if (!graph.has(conn.from)) {
                graph.set(conn.from, []);
            }
            graph.get(conn.from)!.push(conn.to);
        });

        // Add proposed connection
        if (!graph.has(fromNodeId)) {
            graph.set(fromNodeId, []);
        }
        graph.get(fromNodeId)!.push(toNodeId);

        // DFS to detect cycle
        const visited = new Set<string>();
        const recStack = new Set<string>();

        const hasCycle = (nodeId: string): boolean => {
            if (recStack.has(nodeId)) {
                return true; // Found cycle
            }
            if (visited.has(nodeId)) {
                return false; // Already checked this path
            }

            visited.add(nodeId);
            recStack.add(nodeId);

            const neighbors = graph.get(nodeId) || [];
            for (const neighbor of neighbors) {
                if (hasCycle(neighbor)) {
                    return true;
                }
            }

            recStack.delete(nodeId);
            return false;
        };

        return hasCycle(fromNodeId);
    }

    /**
     * Get execution order using topological sort (Kahn's algorithm)
     */
    getExecutionOrder(nodes: VisualNode[]): string[] {
        const inDegree = new Map<string, number>();
        const graph = new Map<string, string[]>();

        // Initialize
        nodes.forEach(node => {
            inDegree.set(node.id, 0);
            graph.set(node.id, []);
        });

        // Build graph
        this.connections.forEach(conn => {
            graph.get(conn.from)!.push(conn.to);
            inDegree.set(conn.to, (inDegree.get(conn.to) || 0) + 1);
        });

        // Kahn's algorithm
        const queue: string[] = [];
        const result: string[] = [];

        // Add nodes with no incoming edges
        inDegree.forEach((degree, nodeId) => {
            if (degree === 0) {
                queue.push(nodeId);
            }
        });

        while (queue.length > 0) {
            const nodeId = queue.shift()!;
            result.push(nodeId);

            const neighbors = graph.get(nodeId) || [];
            neighbors.forEach(neighbor => {
                const newDegree = (inDegree.get(neighbor) || 0) - 1;
                inDegree.set(neighbor, newDegree);

                if (newDegree === 0) {
                    queue.push(neighbor);
                }
            });
        }

        return result;
    }

    /**
     * Clear all connections
     */
    clear(): void {
        this.connections.clear();
        console.log('[ConnectionManager] Cleared all connections');
    }

    /**
     * Export connections to JSON
     */
    exportConnections(): string {
        const data = {
            connections: Array.from(this.connections.values())
        };
        return JSON.stringify(data, null, 2);
    }

    /**
     * Import connections from JSON
     */
    importConnections(json: string): void {
        try {
            const data = JSON.parse(json);

            this.connections.clear();

            if (data.connections && Array.isArray(data.connections)) {
                data.connections.forEach((conn: Connection) => {
                    this.connections.set(conn.id, conn);
                });
            }

            console.log(`[ConnectionManager] Imported ${this.connections.size} connections`);
        } catch (error) {
            console.error('[ConnectionManager] Failed to import connections:', error);
        }
    }

    /**
     * Get connection statistics
     */
    getStatistics(): {
        total: number;
        sequential: number;
        conditional: number;
        error: number;
    } {
        const stats = {
            total: this.connections.size,
            sequential: 0,
            conditional: 0,
            error: 0
        };

        this.connections.forEach(conn => {
            switch (conn.type) {
                case 'sequential': stats.sequential++; break;
                case 'conditional': stats.conditional++; break;
                case 'error': stats.error++; break;
            }
        });

        return stats;
    }
}


