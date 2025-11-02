/**
 * NestedReordering - TDD Tests
 * Test reordering functionality for nested plugins
 */

import { describe, it, expect, beforeEach, afterEach } from '@jest/globals';
import { VisualDesigner } from '../VisualDesigner';
import type { VisualNode } from '../../types';

describe('NestedReordering', () => {
    let designer: VisualDesigner;
    let container: HTMLElement;

    beforeEach(() => {
        container = document.createElement('div');
        container.id = 'test-canvas';
        document.body.appendChild(container);

        designer = new VisualDesigner('test-canvas');
    });

    afterEach(() => {
        document.body.removeChild(container);
    });

    describe('reorderChild', () => {
        it('should reorder child within container', () => {
            const tryNode: VisualNode = {
                id: 'try-1',
                type: 'try',
                attributes: {},
                body: null,
                children: []
            };

            const log1: VisualNode = {
                id: 'log-1',
                type: 'log',
                attributes: { message: 'First' },
                body: null,
                children: []
            };

            const log2: VisualNode = {
                id: 'log-2',
                type: 'log',
                attributes: { message: 'Second' },
                body: null,
                children: []
            };

            designer.addNode(tryNode);
            designer.addChildToContainer('try-1', log1);
            designer.addChildToContainer('try-1', log2);

            // Verify initial order
            const initial = designer.findNode('try-1');
            expect(initial?.children).toHaveLength(2);
            expect(initial?.children[0].id).toBe('log-1');
            expect(initial?.children[1].id).toBe('log-2');

            // Reorder
            designer.reorderChild('try-1', 0, 1);

            // Verify new order
            const updated = designer.findNode('try-1');
            expect(updated?.children).toHaveLength(2);
            expect(updated?.children[0].id).toBe('log-2');
            expect(updated?.children[1].id).toBe('log-1');
        });

        it('should return false for invalid container', () => {
            const result = designer.reorderChild('invalid', 0, 1);
            expect(result).toBe(false);
        });

        it('should return false for invalid indices', () => {
            const tryNode: VisualNode = {
                id: 'try-1',
                type: 'try',
                attributes: {},
                body: null,
                children: []
            };

            designer.addNode(tryNode);

            expect(designer.reorderChild('try-1', 0, 0)).toBe(false); // Same index
            expect(designer.reorderChild('try-1', -1, 0)).toBe(false); // Negative index
            expect(designer.reorderChild('try-1', 0, 10)).toBe(false); // Out of bounds
        });
    });

    describe('moveChildUp', () => {
        it('should move child up one position', () => {
            const tryNode: VisualNode = {
                id: 'try-1',
                type: 'try',
                attributes: {},
                body: null,
                children: []
            };

            const log1: VisualNode = { id: 'log-1', type: 'log', attributes: {}, body: null, children: [] };
            const log2: VisualNode = { id: 'log-2', type: 'log', attributes: {}, body: null, children: [] };
            const log3: VisualNode = { id: 'log-3', type: 'log', attributes: {}, body: null, children: [] };

            designer.addNode(tryNode);
            designer.addChildToContainer('try-1', log1);
            designer.addChildToContainer('try-1', log2);
            designer.addChildToContainer('try-1', log3);

            // Move middle child up
            designer.moveChildUp('try-1', 'log-2');

            const updated = designer.findNode('try-1');
            expect(updated?.children).toHaveLength(3);
            expect(updated?.children[0].id).toBe('log-2');
            expect(updated?.children[1].id).toBe('log-1');
            expect(updated?.children[2].id).toBe('log-3');
        });

        it('should return false for first child', () => {
            const tryNode: VisualNode = {
                id: 'try-1',
                type: 'try',
                attributes: {},
                body: null,
                children: []
            };

            const log1: VisualNode = { id: 'log-1', type: 'log', attributes: {}, body: null, children: [] };

            designer.addNode(tryNode);
            designer.addChildToContainer('try-1', log1);

            const result = designer.moveChildUp('try-1', 'log-1');
            expect(result).toBe(false);
        });

        it('should return false for invalid child', () => {
            const tryNode: VisualNode = {
                id: 'try-1',
                type: 'try',
                attributes: {},
                body: null,
                children: []
            };

            designer.addNode(tryNode);

            const result = designer.moveChildUp('try-1', 'non-existent');
            expect(result).toBe(false);
        });
    });

    describe('moveChildDown', () => {
        it('should move child down one position', () => {
            const tryNode: VisualNode = {
                id: 'try-1',
                type: 'try',
                attributes: {},
                body: null,
                children: []
            };

            const log1: VisualNode = { id: 'log-1', type: 'log', attributes: {}, body: null, children: [] };
            const log2: VisualNode = { id: 'log-2', type: 'log', attributes: {}, body: null, children: [] };
            const log3: VisualNode = { id: 'log-3', type: 'log', attributes: {}, body: null, children: [] };

            designer.addNode(tryNode);
            designer.addChildToContainer('try-1', log1);
            designer.addChildToContainer('try-1', log2);
            designer.addChildToContainer('try-1', log3);

            // Move middle child down
            designer.moveChildDown('try-1', 'log-2');

            const updated = designer.findNode('try-1');
            expect(updated?.children).toHaveLength(3);
            expect(updated?.children[0].id).toBe('log-1');
            expect(updated?.children[1].id).toBe('log-3');
            expect(updated?.children[2].id).toBe('log-2');
        });

        it('should return false for last child', () => {
            const tryNode: VisualNode = {
                id: 'try-1',
                type: 'try',
                attributes: {},
                body: null,
                children: []
            };

            const log1: VisualNode = { id: 'log-1', type: 'log', attributes: {}, body: null, children: [] };
            const log2: VisualNode = { id: 'log-2', type: 'log', attributes: {}, body: null, children: [] };

            designer.addNode(tryNode);
            designer.addChildToContainer('try-1', log1);
            designer.addChildToContainer('try-1', log2);

            const result = designer.moveChildDown('try-1', 'log-2');
            expect(result).toBe(false);
        });
    });

    describe('history tracking', () => {
        it('should push history snapshot on reorder', () => {
            const tryNode: VisualNode = {
                id: 'try-1',
                type: 'try',
                attributes: {},
                body: null,
                children: []
            };

            const log1: VisualNode = { id: 'log-1', type: 'log', attributes: {}, body: null, children: [] };
            const log2: VisualNode = { id: 'log-2', type: 'log', attributes: {}, body: null, children: [] };

            designer.addNode(tryNode);
            designer.addChildToContainer('try-1', log1);
            designer.addChildToContainer('try-1', log2);

            const initialSnapshot = designer['undoStack'].length;

            designer.reorderChild('try-1', 0, 1);

            expect(designer['undoStack'].length).toBe(initialSnapshot + 1);
        });

        it('should emit childReordered event', () => {
            const tryNode: VisualNode = {
                id: 'try-1',
                type: 'try',
                attributes: {},
                body: null,
                children: []
            };

            const log1: VisualNode = { id: 'log-1', type: 'log', attributes: {}, body: null, children: [] };
            const log2: VisualNode = { id: 'log-2', type: 'log', attributes: {}, body: null, children: [] };

            designer.addNode(tryNode);
            designer.addChildToContainer('try-1', log1);
            designer.addChildToContainer('try-1', log2);

            let eventEmitted = false;
            designer.on('childReordered', () => {
                eventEmitted = true;
            });

            designer.reorderChild('try-1', 0, 1);

            expect(eventEmitted).toBe(true);
        });
    });
});

