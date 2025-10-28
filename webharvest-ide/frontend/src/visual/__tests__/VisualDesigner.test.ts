/**
 * VisualDesigner - TDD Tests (Core Functionality)
 * Test FIRST, then implement!
 */

import { describe, it, expect, beforeEach, afterEach } from '@jest/globals';
import { VisualDesigner } from '../VisualDesigner';
import type { VisualNode, Plugin } from '../types';

describe('VisualDesigner', () => {
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

    describe('initialization', () => {
        it('should create designer instance', () => {
            expect(designer).toBeDefined();
        });

        it('should throw error if container not found', () => {
            expect(() => new VisualDesigner('non-existing')).toThrow();
        });
    });

    describe('loadNodes', () => {
        it('should load nodes from array', () => {
            const nodes: VisualNode[] = [
                {
                    id: '1',
                    type: 'var',
                    attributes: { name: 'test' },
                    body: 'value',
                    children: [],
                },
            ];

            designer.loadNodes(nodes);

            expect(designer.getNodes()).toHaveLength(1);
        });

        it('should replace previous nodes', () => {
            const nodes1: VisualNode[] = [
                { id: '1', type: 'var', attributes: {}, body: null, children: [] },
            ];
            const nodes2: VisualNode[] = [
                { id: '2', type: 'http', attributes: {}, body: null, children: [] },
            ];

            designer.loadNodes(nodes1);
            designer.loadNodes(nodes2);

            expect(designer.getNodes()).toHaveLength(1);
            expect(designer.getNodes()[0].id).toBe('2');
        });

        it('should render nodes on canvas', () => {
            const nodes: VisualNode[] = [
                { id: '1', type: 'var', attributes: { name: 'test' }, body: 'value', children: [] },
            ];

            designer.loadNodes(nodes);

            expect(container.querySelector('.visual-node')).toBeDefined();
        });
    });

    describe('addNode', () => {
        it('should add node to canvas', () => {
            const node: VisualNode = {
                id: '1',
                type: 'var',
                attributes: { name: 'test' },
                body: 'value',
                children: [],
            };

            designer.addNode(node);

            expect(designer.getNodes()).toHaveLength(1);
        });

        it('should render new node', () => {
            const node: VisualNode = {
                id: '1',
                type: 'var',
                attributes: {},
                body: null,
                children: [],
            };

            designer.addNode(node);

            expect(container.querySelector('.visual-node')).toBeDefined();
        });
    });

    describe('removeNode', () => {
        it('should remove node by ID', () => {
            const node: VisualNode = {
                id: '1',
                type: 'var',
                attributes: {},
                body: null,
                children: [],
            };

            designer.addNode(node);
            const result = designer.removeNode('1');

            expect(result).toBe(true);
            expect(designer.getNodes()).toHaveLength(0);
        });

        it('should return false for non-existing node', () => {
            const result = designer.removeNode('non-existing');

            expect(result).toBe(false);
        });

        it('should remove node from DOM', () => {
            const node: VisualNode = {
                id: '1',
                type: 'var',
                attributes: {},
                body: null,
                children: [],
            };

            designer.addNode(node);
            designer.removeNode('1');

            expect(container.querySelector('[data-node-id="1"]')).toBeNull();
        });
    });

    describe('getNodes', () => {
        it('should return all nodes', () => {
            designer.addNode({ id: '1', type: 'var', attributes: {}, body: null, children: [] });
            designer.addNode({ id: '2', type: 'http', attributes: {}, body: null, children: [] });

            expect(designer.getNodes()).toHaveLength(2);
        });

        it('should return copy of nodes', () => {
            const nodes: VisualNode[] = [
                { id: '1', type: 'var', attributes: {}, body: null, children: [] },
            ];

            designer.loadNodes(nodes);
            const copy = designer.getNodes();

            expect(copy).not.toBe(nodes);
            expect(copy).toEqual(nodes);
        });
    });

    describe('findNode', () => {
        it('should find node by ID', () => {
            const node: VisualNode = {
                id: 'test-123',
                type: 'var',
                attributes: {},
                body: null,
                children: [],
            };

            designer.addNode(node);

            expect(designer.findNode('test-123')).toEqual(node);
        });

        it('should return null for non-existing node', () => {
            expect(designer.findNode('non-existing')).toBeNull();
        });
    });

    describe('clear', () => {
        it('should clear all nodes', () => {
            designer.addNode({ id: '1', type: 'var', attributes: {}, body: null, children: [] });
            designer.addNode({ id: '2', type: 'http', attributes: {}, body: null, children: [] });

            designer.clear();

            expect(designer.getNodes()).toHaveLength(0);
        });

        it('should clear canvas DOM', () => {
            designer.addNode({ id: '1', type: 'var', attributes: {}, body: null, children: [] });

            designer.clear();

            expect(container.querySelector('.visual-node')).toBeNull();
        });
    });

    describe('updateNode', () => {
        it('should update node properties', () => {
            const node: VisualNode = {
                id: '1',
                type: 'var',
                attributes: { name: 'old' },
                body: 'old value',
                children: [],
            };

            designer.addNode(node);

            const updated = {
                ...node,
                attributes: { name: 'new' },
                body: 'new value',
            };

            const result = designer.updateNode('1', updated);

            expect(result).toBe(true);
            expect(designer.findNode('1')?.attributes.name).toBe('new');
            expect(designer.findNode('1')?.body).toBe('new value');
        });

        it('should return false for non-existing node', () => {
            const node: VisualNode = {
                id: 'non-existing',
                type: 'var',
                attributes: {},
                body: null,
                children: [],
            };

            const result = designer.updateNode('non-existing', node);

            expect(result).toBe(false);
        });
    });

    describe('node selection', () => {
        it('should select node', () => {
            designer.addNode({ id: '1', type: 'var', attributes: {}, body: null, children: [] });

            designer.selectNode('1');

            expect(designer.getSelectedNodeId()).toBe('1');
        });

        it('should deselect previous node when selecting new one', () => {
            designer.addNode({ id: '1', type: 'var', attributes: {}, body: null, children: [] });
            designer.addNode({ id: '2', type: 'http', attributes: {}, body: null, children: [] });

            designer.selectNode('1');
            designer.selectNode('2');

            expect(designer.getSelectedNodeId()).toBe('2');
        });

        it('should clear selection', () => {
            designer.addNode({ id: '1', type: 'var', attributes: {}, body: null, children: [] });

            designer.selectNode('1');
            designer.clearSelection();

            expect(designer.getSelectedNodeId()).toBeNull();
        });
    });
});


