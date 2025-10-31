/**
 * ConnectionManager - TDD Tests
 * Tests for manual connection creation and validation
 */

import { describe, it, expect, beforeEach } from '@jest/globals';
import { ConnectionManager, Connection, ConnectionType } from '../ConnectionManager';
import type { VisualNode } from '../../types';

describe('ConnectionManager', () => {
    let manager: ConnectionManager;
    let mockNodes: VisualNode[];

    beforeEach(() => {
        manager = new ConnectionManager();

        mockNodes = [
            {
                id: 'node-1',
                type: 'http',
                attributes: {},
                body: null,
                children: []
            },
            {
                id: 'node-2',
                type: 'xpath',
                attributes: {},
                body: null,
                children: []
            },
            {
                id: 'node-3',
                type: 'def',
                attributes: {},
                body: null,
                children: []
            }
        ];
    });

    describe('createConnection', () => {
        it('should create sequential connection', () => {
            const connection = manager.createConnection('node-1', 'node-2', 'sequential');

            expect(connection.from).toBe('node-1');
            expect(connection.to).toBe('node-2');
            expect(connection.type).toBe('sequential');
            expect(connection.id).toBeTruthy();
        });

        it('should create conditional connection', () => {
            const connection = manager.createConnection('node-1', 'node-2', 'conditional');

            expect(connection.type).toBe('conditional');
        });

        it('should create error connection', () => {
            const connection = manager.createConnection('node-1', 'node-2', 'error');

            expect(connection.type).toBe('error');
        });

        it('should generate unique ID for each connection', () => {
            const conn1 = manager.createConnection('node-1', 'node-2', 'sequential');
            const conn2 = manager.createConnection('node-2', 'node-3', 'sequential');

            expect(conn1.id).not.toBe(conn2.id);
        });
    });

    describe('deleteConnection', () => {
        it('should delete connection by ID', () => {
            const connection = manager.createConnection('node-1', 'node-2', 'sequential');
            const deleted = manager.deleteConnection(connection.id);

            expect(deleted).toBe(true);
            expect(manager.getConnections().length).toBe(0);
        });

        it('should return false for non-existent connection', () => {
            const deleted = manager.deleteConnection('invalid-id');
            expect(deleted).toBe(false);
        });
    });

    describe('getConnections', () => {
        it('should return all connections', () => {
            manager.createConnection('node-1', 'node-2', 'sequential');
            manager.createConnection('node-2', 'node-3', 'sequential');

            const connections = manager.getConnections();
            expect(connections.length).toBe(2);
        });

        it('should return empty array when no connections', () => {
            const connections = manager.getConnections();
            expect(connections.length).toBe(0);
        });
    });

    describe('getConnectionsForNode', () => {
        it('should return outgoing connections', () => {
            manager.createConnection('node-1', 'node-2', 'sequential');
            manager.createConnection('node-1', 'node-3', 'sequential');

            const connections = manager.getConnectionsForNode('node-1', 'output');
            expect(connections.length).toBe(2);
        });

        it('should return incoming connections', () => {
            manager.createConnection('node-1', 'node-2', 'sequential');
            manager.createConnection('node-3', 'node-2', 'sequential');

            const connections = manager.getConnectionsForNode('node-2', 'input');
            expect(connections.length).toBe(2);
        });

        it('should return empty array for node with no connections', () => {
            const connections = manager.getConnectionsForNode('node-1', 'output');
            expect(connections.length).toBe(0);
        });
    });

    describe('detectCycle', () => {
        it('should detect simple cycle', () => {
            manager.createConnection('node-1', 'node-2', 'sequential');
            manager.createConnection('node-2', 'node-3', 'sequential');

            const hasCycle = manager.wouldCreateCycle('node-3', 'node-1');
            expect(hasCycle).toBe(true);
        });

        it('should not detect cycle in valid flow', () => {
            manager.createConnection('node-1', 'node-2', 'sequential');

            const hasCycle = manager.wouldCreateCycle('node-2', 'node-3');
            expect(hasCycle).toBe(false);
        });

        it('should detect self-loop', () => {
            const hasCycle = manager.wouldCreateCycle('node-1', 'node-1');
            expect(hasCycle).toBe(true);
        });

        it('should handle complex graph', () => {
            manager.createConnection('node-1', 'node-2', 'sequential');
            manager.createConnection('node-2', 'node-3', 'sequential');
            manager.createConnection('node-3', 'node-4', 'sequential');

            const hasCycle = manager.wouldCreateCycle('node-4', 'node-2');
            expect(hasCycle).toBe(true);
        });
    });

    describe('validateConnection', () => {
        it('should validate valid connection', () => {
            const validation = manager.validateConnection('node-1', 'node-2');
            expect(validation.valid).toBe(true);
        });

        it('should reject self-connection', () => {
            const validation = manager.validateConnection('node-1', 'node-1');
            expect(validation.valid).toBe(false);
            expect(validation.error).toContain('same node');
        });

        it('should reject connection creating cycle', () => {
            manager.createConnection('node-1', 'node-2', 'sequential');
            manager.createConnection('node-2', 'node-3', 'sequential');

            const validation = manager.validateConnection('node-3', 'node-1');
            expect(validation.valid).toBe(false);
            expect(validation.error).toContain('cycle');
        });

        it('should reject duplicate connection', () => {
            manager.createConnection('node-1', 'node-2', 'sequential');

            const validation = manager.validateConnection('node-1', 'node-2');
            expect(validation.valid).toBe(false);
            expect(validation.error).toContain('already exists');
        });
    });

    describe('updateConnection', () => {
        it('should update connection type', () => {
            const connection = manager.createConnection('node-1', 'node-2', 'sequential');

            const updated = manager.updateConnection(connection.id, { type: 'conditional' });
            expect(updated).toBe(true);

            const conn = manager.getConnection(connection.id);
            expect(conn?.type).toBe('conditional');
        });

        it('should update connection label', () => {
            const connection = manager.createConnection('node-1', 'node-2', 'conditional');

            manager.updateConnection(connection.id, { label: 'true' });

            const conn = manager.getConnection(connection.id);
            expect(conn?.label).toBe('true');
        });

        it('should return false for invalid connection ID', () => {
            const updated = manager.updateConnection('invalid-id', { type: 'sequential' });
            expect(updated).toBe(false);
        });
    });

    describe('getConnection', () => {
        it('should return connection by ID', () => {
            const connection = manager.createConnection('node-1', 'node-2', 'sequential');

            const retrieved = manager.getConnection(connection.id);
            expect(retrieved).toEqual(connection);
        });

        it('should return null for invalid ID', () => {
            const retrieved = manager.getConnection('invalid-id');
            expect(retrieved).toBeNull();
        });
    });

    describe('clear', () => {
        it('should remove all connections', () => {
            manager.createConnection('node-1', 'node-2', 'sequential');
            manager.createConnection('node-2', 'node-3', 'sequential');

            manager.clear();

            const connections = manager.getConnections();
            expect(connections.length).toBe(0);
        });
    });

    describe('getExecutionOrder', () => {
        it('should return topologically sorted nodes', () => {
            manager.createConnection('node-1', 'node-2', 'sequential');
            manager.createConnection('node-2', 'node-3', 'sequential');

            const order = manager.getExecutionOrder(mockNodes);

            expect(order[0]).toBe('node-1');
            expect(order[1]).toBe('node-2');
            expect(order[2]).toBe('node-3');
        });

        it('should handle branching', () => {
            manager.createConnection('node-1', 'node-2', 'sequential');
            manager.createConnection('node-1', 'node-3', 'conditional');

            const order = manager.getExecutionOrder(mockNodes);

            expect(order[0]).toBe('node-1');
            expect(order.length).toBe(3);
        });

        it('should handle nodes with no connections', () => {
            const order = manager.getExecutionOrder(mockNodes);

            expect(order.length).toBe(3);
        });
    });

    describe('export/import', () => {
        it('should export connections to JSON', () => {
            manager.createConnection('node-1', 'node-2', 'sequential');
            manager.createConnection('node-2', 'node-3', 'conditional');

            const json = manager.exportConnections();
            const data = JSON.parse(json);

            expect(data.connections.length).toBe(2);
        });

        it('should import connections from JSON', () => {
            const json = JSON.stringify({
                connections: [
                    { id: 'conn-1', from: 'node-1', to: 'node-2', type: 'sequential' }
                ]
            });

            manager.importConnections(json);

            const connections = manager.getConnections();
            expect(connections.length).toBe(1);
        });
    });
});


