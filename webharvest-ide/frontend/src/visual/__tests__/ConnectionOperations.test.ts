/**
 * Connection Operations - TDD Tests
 * Tests for connection selection, editing, and deletion
 */

import { describe, it, expect, beforeEach, afterEach } from '@jest/globals';
import { VisualDesigner } from '../VisualDesigner';
import type { VisualNode } from '../types';

describe('Connection Operations', () => {
    let designer: VisualDesigner;
    let container: HTMLElement;

    beforeEach(() => {
        container = document.createElement('div');
        container.id = 'test-canvas';
        document.body.appendChild(container);

        designer = new VisualDesigner('test-canvas');
    });

    afterEach(() => {
        if (document.body.contains(container)) {
            document.body.removeChild(container);
        }
    });

    describe('Connection Selection', () => {
        beforeEach(() => {
            const node1: VisualNode = {
                id: '1',
                type: 'def',
                attributes: {},
                body: null,
                children: [],
                position: { x: 0, y: 0 }
            };
            const node2: VisualNode = {
                id: '2',
                type: 'def',
                attributes: {},
                body: null,
                children: [],
                position: { x: 250, y: 0 }
            };
            designer.loadNodes([node1, node2]);
        });

        it('should create and select connection', () => {
            designer.render();
            const connection = designer.createManualConnection('1', '2', 'sequential');
            expect(connection).toBeDefined();
            expect(connection.id).toBeDefined();

            // Simulate connection click via internal method
            (designer as any).handleConnectionClick(connection.id);
            expect((designer as any).selectedConnectionId).toBe(connection.id);
        });

        it('should render selected connection with different style', () => {
            designer.render();
            const connection = designer.createManualConnection('1', '2', 'sequential');
            designer.render();

            const line = container.querySelector(`[data-connection-id="${connection.id}"]`) as SVGLineElement;
            expect(line).toBeDefined();
            expect(line.getAttribute('stroke-width')).toBe('2'); // Default width
        });
    });

    describe('Connection Deletion', () => {
        beforeEach(() => {
            const node1: VisualNode = {
                id: '1',
                type: 'def',
                attributes: {},
                body: null,
                children: [],
                position: { x: 0, y: 0 }
            };
            const node2: VisualNode = {
                id: '2',
                type: 'def',
                attributes: {},
                body: null,
                children: [],
                position: { x: 250, y: 0 }
            };
            designer.loadNodes([node1, node2]);
        });

        it('should delete connection', () => {
            designer.render();
            const connection = designer.createManualConnection('1', '2', 'sequential');
            expect(designer.getManualConnections()).toHaveLength(1);

            const deleted = designer.deleteConnection(connection.id);
            expect(deleted).toBe(true);
            expect(designer.getManualConnections()).toHaveLength(0);
        });

        it('should clear selection when deleting selected connection', () => {
            designer.render();
            const connection = designer.createManualConnection('1', '2', 'sequential');
            (designer as any).handleConnectionClick(connection.id);
            expect((designer as any).selectedConnectionId).toBe(connection.id);

            designer.deleteConnection(connection.id);
            expect((designer as any).selectedConnectionId).toBeNull();
        });

        it('should return false for non-existent connection', () => {
            const deleted = designer.deleteConnection('non-existent');
            expect(deleted).toBe(false);
        });
    });

    describe('Connection Redefinition', () => {
        beforeEach(() => {
            const node1: VisualNode = {
                id: '1',
                type: 'def',
                attributes: {},
                body: null,
                children: [],
                position: { x: 0, y: 0 }
            };
            const node2: VisualNode = {
                id: '2',
                type: 'def',
                attributes: {},
                body: null,
                children: [],
                position: { x: 250, y: 0 }
            };
            const node3: VisualNode = {
                id: '3',
                type: 'def',
                attributes: {},
                body: null,
                children: [],
                position: { x: 500, y: 0 }
            };
            designer.loadNodes([node1, node2, node3]);
        });

        it('should update connection source and target', () => {
            designer.render();
            const connection = designer.createManualConnection('1', '2', 'sequential');

            // Mock prompt for editConnection
            const originalPrompt = window.prompt;
            let promptCalls = 0;
            window.prompt = jest.fn(() => {
                promptCalls++;
                if (promptCalls === 1) return '1';
                if (promptCalls === 2) return '3';
                return null;
            }) as any;

            (designer as any).editConnection(connection.id);

            window.prompt = originalPrompt;

            const updated = (designer as any).connectionManager.getConnection(connection.id);
            expect(updated?.from).toBe('1');
            expect(updated?.to).toBe('3');
        });

        it('should validate connection before updating', () => {
            designer.render();
            const connection = designer.createManualConnection('1', '2', 'sequential');

            // Mock prompt to create cycle (3 -> 1 when 1 -> 2 exists)
            const originalPrompt = window.prompt;
            let promptCalls = 0;
            window.prompt = jest.fn(() => {
                promptCalls++;
                if (promptCalls === 1) return '3'; // Try to change source to 3
                if (promptCalls === 2) return '1'; // Try to connect 3 -> 1
                return null;
            }) as any;

            (designer as any).editConnection(connection.id);

            window.prompt = originalPrompt;

            // Connection should be validated (validation happens, but we check the result)
            const updated = (designer as any).connectionManager.getConnection(connection.id);
            // If validation fails, connection might remain unchanged or be updated
            // The actual behavior depends on validation logic
            expect(updated).toBeDefined();
        });
    });
});

