/**
 * TDD Tests for Visual Model Architecture
 * 
 * Decision: Hybrid Model
 * - NESTED: Container plugins (loop, if, try, while, function, case) render children inside
 * - CONNECTED: Linear plugins render as flowchart nodes connected by arrows
 * 
 * Advantages:
 * ✅ Natural representation - containers show hierarchy
 * ✅ Clear flow - linear sequences show data flow
 * ✅ Flexible - best of both worlds like Azure Logic Apps
 */

import type { VisualNode } from '../../types';
import {
    isContainerNode,
    getRenderingStrategy,
    calculateLinearLayout,
    calculateNestedLayout,
    createConnection,
    getPluginTemplates,
    createNodeFromTemplate,
    reorderNodes,
    addNodeToContainer,
    hasCircularReference,
    type PluginTemplate
} from '../VisualModelHelpers';

describe('Visual Model Architecture (TDD)', () => {
    describe('Node Classification', () => {
        it('should classify container nodes correctly', () => {
            const containerTypes = [
                'loop', 'while', 'if', 'try', 'catch', 'case',
                'function', 'body', 'config'
            ];

            containerTypes.forEach(type => {
                expect(isContainerNode(type)).toBe(true);
            });
        });

        it('should classify linear/action nodes correctly', () => {
            const linearTypes = [
                'http', 'xpath', 'xquery', 'def', 'regexp',
                'html-to-xml', 'json-to-xml', 'xml-to-json',
                'file', 'script', 'template', 'text'
            ];

            linearTypes.forEach(type => {
                expect(isContainerNode(type)).toBe(false);
            });
        });
    });

    describe('Rendering Strategy', () => {
        it('should use nested rendering for loop with children', () => {
            const node: VisualNode = {
                id: 'loop-1',
                type: 'loop',
                attributes: { maxloops: '5' },
                body: null,
                children: [
                    {
                        id: 'http-1',
                        type: 'http',
                        attributes: { url: 'https://api.com' },
                        body: null,
                        children: []
                    },
                    {
                        id: 'xpath-1',
                        type: 'xpath',
                        attributes: { expression: '//title' },
                        body: null,
                        children: []
                    }
                ]
            };

            const strategy = getRenderingStrategy(node);

            expect(strategy).toBe('nested');
            expect(node.children.length).toBe(2);
        });

        it('should use connected rendering for linear sequence', () => {
            const nodes: VisualNode[] = [
                {
                    id: 'http-1',
                    type: 'http',
                    attributes: { url: 'https://api.com' },
                    body: null,
                    children: []
                },
                {
                    id: 'xpath-1',
                    type: 'xpath',
                    attributes: { expression: '//title' },
                    body: null,
                    children: []
                },
                {
                    id: 'def-1',
                    type: 'def',
                    attributes: { var: 'title' },
                    body: null,
                    children: []
                }
            ];

            // Linear sequence at same level - connected rendering
            nodes.forEach(node => {
                expect(getRenderingStrategy(node)).toBe('connected');
            });
        });

        it('should use hybrid rendering for nested containers with linear children', () => {
            const node: VisualNode = {
                id: 'loop-1',
                type: 'loop',
                attributes: {},
                body: null,
                children: [
                    { id: 'http-1', type: 'http', attributes: {}, body: null, children: [] },
                    { id: 'xpath-1', type: 'xpath', attributes: {}, body: null, children: [] },
                    { id: 'def-1', type: 'def', attributes: {}, body: null, children: [] }
                ]
            };

            // Loop itself: nested
            expect(getRenderingStrategy(node)).toBe('nested');

            // Children inside loop: connected (flowchart within container)
            node.children.forEach(child => {
                expect(getRenderingStrategy(child)).toBe('connected');
            });
        });
    });

    describe('Visual Layout', () => {
        it('should calculate positions for linear sequence (vertical flowchart)', () => {
            const nodes: VisualNode[] = [
                { id: '1', type: 'http', attributes: {}, body: null, children: [] },
                { id: '2', type: 'xpath', attributes: {}, body: null, children: [] },
                { id: '3', type: 'def', attributes: {}, body: null, children: [] }
            ];

            const positions = calculateLinearLayout(nodes, { startX: 100, startY: 100, spacing: 120 });

            expect(positions['1']).toEqual({ x: 100, y: 100 });
            expect(positions['2']).toEqual({ x: 100, y: 220 }); // 100 + 120
            expect(positions['3']).toEqual({ x: 100, y: 340 }); // 220 + 120
        });

        it('should calculate positions for nested container', () => {
            const node: VisualNode = {
                id: 'loop-1',
                type: 'loop',
                attributes: {},
                body: null,
                children: [
                    { id: 'http-1', type: 'http', attributes: {}, body: null, children: [] },
                    { id: 'xpath-1', type: 'xpath', attributes: {}, body: null, children: [] }
                ]
            };

            const positions = calculateNestedLayout(node, { x: 100, y: 100, padding: 20 });

            // Container
            expect(positions['loop-1']).toEqual({ x: 100, y: 100 });

            // Children inside container (offset by padding)
            expect(positions['http-1']).toEqual({ x: 120, y: 140 }); // 100+20, 100+20+20(header)
            expect(positions['xpath-1']).toEqual({ x: 120, y: 260 }); // next row
        });
    });

    describe('Connection Lines (for connected model)', () => {
        it('should generate connection between sequential nodes', () => {
            const from: VisualNode = {
                id: 'http-1',
                type: 'http',
                attributes: {},
                body: null,
                children: [],
                position: { x: 100, y: 100 }
            };

            const to: VisualNode = {
                id: 'xpath-1',
                type: 'xpath',
                attributes: {},
                body: null,
                children: [],
                position: { x: 100, y: 220 }
            };

            const connection = createConnection(from, to);

            expect(connection.from).toBe('http-1');
            expect(connection.to).toBe('xpath-1');
            expect(connection.type).toBe('sequential');
        });

        it('should generate connection from container output to next node', () => {
            const container: VisualNode = {
                id: 'loop-1',
                type: 'loop',
                attributes: {},
                body: null,
                children: [
                    { id: 'http-1', type: 'http', attributes: {}, body: null, children: [] }
                ],
                position: { x: 100, y: 100 }
            };

            const next: VisualNode = {
                id: 'def-1',
                type: 'def',
                attributes: {},
                body: null,
                children: [],
                position: { x: 100, y: 300 }
            };

            const connection = createConnection(container, next);

            expect(connection.from).toBe('loop-1');
            expect(connection.to).toBe('def-1');
            expect(connection.type).toBe('sequential');
        });
    });

    describe('Drag & Drop for Connected Model', () => {
        it('should allow reordering linear nodes', () => {
            const nodes: VisualNode[] = [
                { id: '1', type: 'http', attributes: {}, body: null, children: [] },
                { id: '2', type: 'xpath', attributes: {}, body: null, children: [] },
                { id: '3', type: 'def', attributes: {}, body: null, children: [] }
            ];

            // Drag node '2' (xpath) before node '1' (http)
            const reordered = reorderNodes(nodes, '2', 0);

            expect(reordered[0].id).toBe('2'); // xpath first
            expect(reordered[1].id).toBe('1'); // http second
            expect(reordered[2].id).toBe('3'); // def last
        });

        it('should allow adding node to container', () => {
            const container: VisualNode = {
                id: 'loop-1',
                type: 'loop',
                attributes: {},
                body: null,
                children: []
            };

            const newNode: VisualNode = {
                id: 'http-1',
                type: 'http',
                attributes: { url: 'https://api.com' },
                body: null,
                children: []
            };

            const updated = addNodeToContainer(container, newNode);

            expect(updated.children).toHaveLength(1);
            expect(updated.children[0].id).toBe('http-1');
        });

        it('should prevent adding container to itself', () => {
            const container: VisualNode = {
                id: 'loop-1',
                type: 'loop',
                attributes: {},
                body: null,
                children: []
            };

            expect(() => addNodeToContainer(container, container)).toThrow('Cannot add node to itself');
        });

        it('should prevent circular nesting', () => {
            const parent: VisualNode = {
                id: 'loop-1',
                type: 'loop',
                attributes: {},
                body: null,
                children: []
            };

            const child: VisualNode = {
                id: 'if-1',
                type: 'if',
                attributes: {},
                body: null,
                children: [parent] // Circular reference!
            };

            expect(() => addNodeToContainer(parent, child)).toThrow('Circular nesting detected');
        });
    });

    describe('Plugin Palette', () => {
        it('should provide drag templates for all plugin types', () => {
            const templates = getPluginTemplates();

            expect(templates).toContainEqual(expect.objectContaining({
                type: 'http',
                category: 'network',
                icon: 'network-icon',
                defaultAttributes: { url: '', method: 'GET' }
            }));

            expect(templates).toContainEqual(expect.objectContaining({
                type: 'loop',
                category: 'control',
                icon: 'loop-icon',
                defaultAttributes: { maxloops: '10' }
            }));

            expect(templates).toContainEqual(expect.objectContaining({
                type: 'xpath',
                category: 'extraction',
                icon: 'xpath-icon',
                defaultAttributes: { expression: '' }
            }));
        });

        it('should create new node from template on drag', () => {
            const template = {
                type: 'http',
                category: 'network',
                icon: 'network-icon',
                defaultAttributes: { url: '', method: 'GET' }
            };

            const newNode = createNodeFromTemplate(template);

            expect(newNode.type).toBe('http');
            expect(newNode.attributes).toEqual({ url: '', method: 'GET' });
            expect(newNode.id).toMatch(/^node-/);
        });
    });
});

