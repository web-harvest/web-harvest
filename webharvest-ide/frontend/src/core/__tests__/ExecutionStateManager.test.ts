/**
 * ExecutionStateManager - TDD Tests
 * Tests for managing per-node execution state
 */

import { describe, it, expect, beforeEach } from '@jest/globals';
import { ExecutionStateManager, NodeExecutionUpdate, NodeStateInfo } from '../ExecutionStateManager';
import type { VisualNode } from '../../types';

describe('ExecutionStateManager', () => {
    let manager: ExecutionStateManager;
    let mockNodes: VisualNode[];

    beforeEach(() => {
        manager = new ExecutionStateManager();

        mockNodes = [
            {
                id: 'node-1',
                type: 'http',
                attributes: { url: 'http://example.com' },
                body: null,
                children: []
            },
            {
                id: 'node-2',
                type: 'xpath',
                attributes: { expression: '//title' },
                body: null,
                children: []
            }
        ];
    });

    describe('registerNodes', () => {
        it('should register visual nodes for execution tracking', () => {
            manager.registerNodes(mockNodes);

            const states = manager.getAllStates();
            expect(states['node-1']).toBeDefined();
            expect(states['node-1'].status).toBe('IDLE');
        });

        it('should clear previous registrations', () => {
            manager.registerNodes(mockNodes);
            manager.registerNodes([mockNodes[0]]);

            const states = manager.getAllStates();
            expect(Object.keys(states).length).toBe(1);
        });
    });

    describe('updateNodeState', () => {
        beforeEach(() => {
            manager.registerNodes(mockNodes);
        });

        it('should update node execution state', () => {
            const update: NodeExecutionUpdate = {
                nodeId: 'node-1',
                pluginType: 'http',
                status: 'RUNNING',
                startTime: Date.now()
            };

            manager.updateNodeState(update);

            const state = manager.getNodeState('node-1');
            expect(state?.status).toBe('RUNNING');
        });

        it('should calculate elapsed time for running nodes', () => {
            const startTime = Date.now() - 2000;
            const update: NodeExecutionUpdate = {
                nodeId: 'node-1',
                pluginType: 'http',
                status: 'RUNNING',
                startTime
            };

            manager.updateNodeState(update);

            const state = manager.getNodeState('node-1');
            expect(state?.elapsedTime).toBeGreaterThan(1900);
        });

        it('should store metrics data', () => {
            const update: NodeExecutionUpdate = {
                nodeId: 'node-1',
                pluginType: 'http',
                status: 'RUNNING',
                metrics: {
                    dataProcessed: 131072,
                    httpRequests: 3
                }
            };

            manager.updateNodeState(update);

            const state = manager.getNodeState('node-1');
            expect(state?.metrics?.dataProcessed).toBe(131072);
            expect(state?.metrics?.httpRequests).toBe(3);
        });

        it('should store error information', () => {
            const update: NodeExecutionUpdate = {
                nodeId: 'node-1',
                pluginType: 'http',
                status: 'ERROR',
                error: {
                    message: 'Connection timeout',
                    stackTrace: 'at HttpPlugin.execute...'
                }
            };

            manager.updateNodeState(update);

            const state = manager.getNodeState('node-1');
            expect(state?.error?.message).toBe('Connection timeout');
        });

        it('should handle completion with end time', () => {
            const startTime = Date.now() - 3000;
            const endTime = Date.now();

            manager.updateNodeState({
                nodeId: 'node-1',
                pluginType: 'http',
                status: 'RUNNING',
                startTime
            });

            manager.updateNodeState({
                nodeId: 'node-1',
                pluginType: 'http',
                status: 'SUCCESS',
                endTime
            });

            const state = manager.getNodeState('node-1');
            expect(state?.status).toBe('SUCCESS');
            expect(state?.endTime).toBe(endTime);
        });
    });

    describe('getNodeState', () => {
        it('should return null for unregistered node', () => {
            const state = manager.getNodeState('unknown-node');
            expect(state).toBeNull();
        });

        it('should return current state for registered node', () => {
            manager.registerNodes(mockNodes);
            const state = manager.getNodeState('node-1');

            expect(state).not.toBeNull();
            expect(state?.nodeId).toBe('node-1');
        });
    });

    describe('getAllStates', () => {
        it('should return empty object when no nodes registered', () => {
            const states = manager.getAllStates();
            expect(Object.keys(states).length).toBe(0);
        });

        it('should return all node states', () => {
            manager.registerNodes(mockNodes);
            const states = manager.getAllStates();

            expect(Object.keys(states).length).toBe(2);
            expect(states['node-1']).toBeDefined();
            expect(states['node-2']).toBeDefined();
        });
    });

    describe('reset', () => {
        it('should clear all node states', () => {
            manager.registerNodes(mockNodes);
            manager.reset();

            const states = manager.getAllStates();
            expect(Object.keys(states).length).toBe(0);
        });
    });

    describe('mapXPathToNodeId', () => {
        it('should map XPath expression to node ID', () => {
            manager.registerNodes(mockNodes);

            const nodeId = manager.mapXPathToNodeId('/config/http[1]');
            expect(nodeId).toBe('node-1');
        });

        it('should return null for unmapped XPath', () => {
            const nodeId = manager.mapXPathToNodeId('/config/unknown[1]');
            expect(nodeId).toBeNull();
        });
    });

    describe('execution progress tracking', () => {
        beforeEach(() => {
            manager.registerNodes(mockNodes);
        });

        it('should calculate overall progress', () => {
            manager.updateNodeState({
                nodeId: 'node-1',
                pluginType: 'http',
                status: 'SUCCESS'
            });

            const progress = manager.getOverallProgress();
            expect(progress).toBe(50); // 1 of 2 nodes complete
        });

        it('should return 0 for no completed nodes', () => {
            const progress = manager.getOverallProgress();
            expect(progress).toBe(0);
        });

        it('should return 100 for all completed nodes', () => {
            manager.updateNodeState({
                nodeId: 'node-1',
                pluginType: 'http',
                status: 'SUCCESS'
            });
            manager.updateNodeState({
                nodeId: 'node-2',
                pluginType: 'xpath',
                status: 'SUCCESS'
            });

            const progress = manager.getOverallProgress();
            expect(progress).toBe(100);
        });
    });

    describe('variable tracking', () => {
        it('should store variables at node execution point', () => {
            manager.registerNodes(mockNodes);

            manager.updateNodeState({
                nodeId: 'node-1',
                pluginType: 'http',
                status: 'SUCCESS',
                variables: {
                    httpResponse: '<html>...</html>',
                    statusCode: '200'
                }
            });

            const state = manager.getNodeState('node-1');
            expect(state?.variables?.httpResponse).toBe('<html>...</html>');
        });
    });
});

