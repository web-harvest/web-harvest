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

    describe('breadcrumb navigation', () => {
        it('renders breadcrumb trail for drill-down navigation', () => {
            const designerAny = designer as unknown as Record<string, any>;
            const nodes: VisualNode[] = [
                {
                    id: 'loop-1',
                    type: 'loop',
                    attributes: { name: 'Items' },
                    body: null,
                    children: [
                        {
                            id: 'child-1',
                            type: 'http',
                            attributes: {},
                            body: null,
                            children: []
                        }
                    ]
                }
            ];

            designer.loadNodes(nodes);

            const initialCrumbs = Array.from(
                container.querySelectorAll('.breadcrumb-nav .breadcrumb-item')
            ).map(el => el.textContent?.trim());
            expect(initialCrumbs).toEqual(['Workflow']);

            designerAny.enterContainer('loop-1');

            const drillDownCrumbs = Array.from(
                container.querySelectorAll('.breadcrumb-nav .breadcrumb-item')
            ).map(el => el.textContent?.trim());
            expect(drillDownCrumbs).toEqual(['Workflow', 'Loop: Items']);

            const rootCrumb = container.querySelector('.breadcrumb-nav .breadcrumb-item');
            expect(rootCrumb).toBeTruthy();
            rootCrumb?.dispatchEvent(new Event('click'));

            const afterNavigate = Array.from(
                container.querySelectorAll('.breadcrumb-nav .breadcrumb-item')
            ).map(el => el.textContent?.trim());
            expect(afterNavigate).toEqual(['Workflow']);
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
            expect(copy).toHaveLength(nodes.length);
            expect(copy[0]).toMatchObject(nodes[0]);
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

            expect(designer.findNode('test-123')).toMatchObject(node);
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

    describe('generateXML', () => {
        it('should produce config document with nested nodes and CDATA-wrapped bodies', () => {
            const nodes: VisualNode[] = [
                {
                    id: 'var-1',
                    type: 'var',
                    attributes: { name: 'foo' },
                    body: 'value',
                    children: [],
                },
                {
                    id: 'http-1',
                    type: 'http',
                    attributes: { url: 'https://example.com' },
                    body: null,
                    children: [
                        {
                            id: 'xpath-1',
                            type: 'xpath',
                            attributes: { expr: '//a' },
                            body: '<div>Content</div>',
                            children: [],
                        },
                    ],
                },
            ];

            (designer as unknown as { nodes: VisualNode[] }).nodes = nodes;

            const xml = designer.generateXML();

            const expected = [
                '<?xml version="1.0" encoding="UTF-8"?>',
                '<config xmlns="http://org.webharvest/schema/2.1/core">',
                '    <var name="foo">value</var>',
                '    <http url="https://example.com">',
                '        <xpath expr="//a"><![CDATA[<div>Content</div>]]></xpath>',
                '    </http>',
                '</config>',
            ].join('\n');

            expect(xml).toBe(expected);
        });

        it('should escape attributes and self-close leaf nodes', () => {
            const nodes: VisualNode[] = [
                {
                    id: 'var-1',
                    type: 'var',
                    attributes: {
                        name: 'foo & bar',
                        title: 'Use "quotes" & <tags>',
                    },
                    body: null,
                    children: [],
                },
            ];

            (designer as unknown as { nodes: VisualNode[] }).nodes = nodes;

            const xml = designer.generateXML();

            const expected = [
                '<?xml version="1.0" encoding="UTF-8"?>',
                '<config xmlns="http://org.webharvest/schema/2.1/core">',
                '    <var name="foo &amp; bar" title="Use &quot;quotes&quot; &amp; &lt;tags&gt;"/>',
                '</config>',
            ].join('\n');

            expect(xml).toBe(expected);
            expect(nodes[0].attributes.name).toBe('foo & bar');
            expect(nodes[0].attributes.title).toBe('Use "quotes" & <tags>');
        });

        it('should order top-level nodes based on manual connections', () => {
            const nodes: VisualNode[] = [
                {
                    id: 'node-a',
                    type: 'http',
                    attributes: { url: 'https://first.example.com' },
                    body: null,
                    children: [],
                },
                {
                    id: 'node-b',
                    type: 'http',
                    attributes: { url: 'https://second.example.com' },
                    body: null,
                    children: [],
                },
                {
                    id: 'node-c',
                    type: 'xpath',
                    attributes: { expression: '//item' },
                    body: null,
                    children: [],
                },
            ];

            designer.loadNodes(nodes);
            designer.createManualConnection('node-a', 'node-c');
            designer.createManualConnection('node-c', 'node-b');

            const manualConnections = designer.getManualConnections().map(conn => [conn.from, conn.to]);
            expect(manualConnections).toEqual([
                ['node-a', 'node-c'],
                ['node-c', 'node-b']
            ]);

            const xml = designer.generateXML();

            const indexA = xml.indexOf('https://first.example.com');
            const indexC = xml.indexOf('//item');
            const indexB = xml.indexOf('https://second.example.com');

            expect(indexA).toBeGreaterThan(-1);
            expect(indexC).toBeGreaterThan(-1);
            expect(indexB).toBeGreaterThan(-1);
            expect(indexA).toBeLessThan(indexC);
            expect(indexC).toBeLessThan(indexB);
        });

        it('should apply connection ordering inside container nodes', () => {
            const nodes: VisualNode[] = [
                {
                    id: 'loop-1',
                    type: 'loop',
                    attributes: { maxloops: '3' },
                    body: null,
                    children: [
                        {
                            id: 'child-a',
                            type: 'http',
                            attributes: { url: 'https://first.example.com' },
                            body: null,
                            children: [],
                        },
                        {
                            id: 'child-b',
                            type: 'xpath',
                            attributes: { expression: '//item' },
                            body: null,
                            children: [],
                        },
                        {
                            id: 'child-c',
                            type: 'def',
                            attributes: { var: 'result' },
                            body: null,
                            children: [],
                        },
                    ],
                },
            ];

            designer.loadNodes(nodes);
            designer.createManualConnection('child-c', 'child-a');
            designer.createManualConnection('child-a', 'child-b');

            const xml = designer.generateXML();

            const loopBody = xml.split('\n').filter(line => line.includes('<loop'));
            expect(loopBody).toHaveLength(1);

            const childAIndex = xml.indexOf('https://first.example.com');
            const childBIndex = xml.indexOf('//item');
            const childCIndex = xml.indexOf('var="result"');

            expect(childCIndex).toBeGreaterThan(-1);
            expect(childAIndex).toBeGreaterThan(-1);
            expect(childBIndex).toBeGreaterThan(-1);
            expect(childCIndex).toBeLessThan(childAIndex);
            expect(childAIndex).toBeLessThan(childBIndex);
        });
    });

    describe('configuration export', () => {
        it('should emit saveAsRequested with connection-ordered node clones', async () => {
            const nodes: VisualNode[] = [
                {
                    id: 'node-a',
                    type: 'http',
                    attributes: { url: 'https://first.example.com' },
                    body: null,
                    children: [],
                },
                {
                    id: 'node-b',
                    type: 'xpath',
                    attributes: { expression: '//item' },
                    body: null,
                    children: [],
                },
                {
                    id: 'node-c',
                    type: 'def',
                    attributes: { var: 'result' },
                    body: null,
                    children: [],
                },
            ];

            designer.loadNodes(nodes);
            const originalConn1 = designer.createManualConnection('node-c', 'node-a');
            const originalConn2 = designer.createManualConnection('node-a', 'node-b');

            const manualConnectionOrder = designer.getManualConnections().map(conn => [conn.from, conn.to]);
            expect(manualConnectionOrder).toEqual([
                ['node-c', 'node-a'],
                ['node-a', 'node-b']
            ]);

            await new Promise<void>((resolve, reject) => {
                designer.addEventListener('saveAsRequested', (payload) => {
                    try {
                        expect(payload.filename).toBe('test.xml');
                        expect(payload.nodes.map((node: VisualNode) => node.id)).toEqual(['node-c', 'node-a', 'node-b']);
                        // Ensure nodes are clones
                        expect(payload.nodes).not.toBe(nodes);
                        expect(payload.nodes[0]).not.toBe(nodes[0]);

                        // Connections should also be cloned
                        expect(payload.connections).toHaveLength(2);
                        expect(payload.connections[0]).not.toBe(originalConn1);
                        expect(payload.connections[1]).not.toBe(originalConn2);

                        expect(typeof payload.xml).toBe('string');
                        expect(payload.xml).toContain('<config xmlns="http://org.webharvest/schema/2.1/core">');
                        expect(payload.xml.indexOf('https://first.example.com')).toBeLessThan(payload.xml.indexOf('//item'));

                        resolve();
                    } catch (error) {
                        reject(error);
                    }
                });

                void designer.saveConfigurationAs('test.xml');
            });
        });

        it('should emit saveRequested with ordered nodes, connections, and XML', async () => {
            const nodes: VisualNode[] = [
                {
                    id: 'node-a',
                    type: 'http',
                    attributes: { url: 'https://first.example.com' },
                    body: null,
                    children: [],
                },
                {
                    id: 'node-b',
                    type: 'xpath',
                    attributes: { expression: '//item' },
                    body: null,
                    children: [],
                },
                {
                    id: 'node-c',
                    type: 'def',
                    attributes: { var: 'result' },
                    body: null,
                    children: [],
                },
            ];

            designer.loadNodes(nodes);
            designer.createManualConnection('node-c', 'node-a');
            designer.createManualConnection('node-a', 'node-b');

            await new Promise<void>((resolve, reject) => {
                designer.addEventListener('saveRequested', (payload) => {
                    try {
                        expect(payload.nodes.map((node: VisualNode) => node.id)).toEqual(['node-c', 'node-a', 'node-b']);
                        expect(payload.connections).toHaveLength(2);
                        expect(payload.xml).toContain('<config xmlns="http://org.webharvest/schema/2.1/core">');
                        expect(payload.xml.indexOf('https://first.example.com')).toBeLessThan(payload.xml.indexOf('//item'));
                        resolve();
                    } catch (error) {
                        reject(error);
                    }
                });

                void (designer as unknown as { saveConfiguration: () => Promise<boolean> }).saveConfiguration();
            });
        });
    });
});





