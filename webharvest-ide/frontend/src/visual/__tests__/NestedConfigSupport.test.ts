/**
 * Nested Configuration Support - TDD Tests
 * Tests for enhanced nested configuration handling in Visual Designer
 */

import { describe, it, expect, beforeEach, afterEach } from '@jest/globals';
import { VisualDesigner } from '../VisualDesigner';
import { XMLParser } from '../XMLParser';
import type { VisualNode } from '../types';

describe('Nested Configuration Support', () => {
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

    describe('Root Node Reordering', () => {
        it('should move root node up', () => {
            const node1: VisualNode = {
                id: '1',
                type: 'def',
                attributes: { var: 'var1' },
                body: null,
                children: [],
                position: { x: 0, y: 0 }
            };
            const node2: VisualNode = {
                id: '2',
                type: 'def',
                attributes: { var: 'var2' },
                body: null,
                children: [],
                position: { x: 0, y: 100 }
            };

            designer.loadNodes([node1, node2]);
            expect(designer.getNodes()[0].id).toBe('1');

            const result = designer.reorderRootNode('2', 'up');
            expect(result).toBe(true);
            expect(designer.getNodes()[0].id).toBe('2');
            expect(designer.getNodes()[1].id).toBe('1');
        });

        it('should move root node down', () => {
            const node1: VisualNode = {
                id: '1',
                type: 'def',
                attributes: { var: 'var1' },
                body: null,
                children: [],
                position: { x: 0, y: 0 }
            };
            const node2: VisualNode = {
                id: '2',
                type: 'def',
                attributes: { var: 'var2' },
                body: null,
                children: [],
                position: { x: 0, y: 100 }
            };

            designer.loadNodes([node1, node2]);
            expect(designer.getNodes()[0].id).toBe('1');

            const result = designer.reorderRootNode('1', 'down');
            expect(result).toBe(true);
            expect(designer.getNodes()[0].id).toBe('2');
            expect(designer.getNodes()[1].id).toBe('1');
        });

        it('should not move first node up', () => {
            const node1: VisualNode = {
                id: '1',
                type: 'def',
                attributes: {},
                body: null,
                children: [],
                position: { x: 0, y: 0 }
            };

            designer.loadNodes([node1]);
            const result = designer.reorderRootNode('1', 'up');
            expect(result).toBe(false);
        });

        it('should not move last node down', () => {
            const node1: VisualNode = {
                id: '1',
                type: 'def',
                attributes: {},
                body: null,
                children: [],
                position: { x: 0, y: 0 }
            };

            designer.loadNodes([node1]);
            const result = designer.reorderRootNode('1', 'down');
            expect(result).toBe(false);
        });

        it('should return false for non-root node', () => {
            const container: VisualNode = {
                id: '1',
                type: 'def',
                attributes: {},
                body: null,
                children: [{
                    id: '2',
                    type: 'http',
                    attributes: {},
                    body: null,
                    children: [],
                    position: { x: 10, y: 10 }
                }],
                position: { x: 0, y: 0 }
            };

            designer.loadNodes([container]);
            const result = designer.reorderRootNode('2', 'up');
            expect(result).toBe(false);
        });
    });

    describe('Container Movement with Children', () => {
        it('should move container and all nested children', () => {
            const child1: VisualNode = {
                id: 'child1',
                type: 'http',
                attributes: {},
                body: null,
                children: [],
                position: { x: 10, y: 10 }
            };
            const child2: VisualNode = {
                id: 'child2',
                type: 'xpath',
                attributes: {},
                body: null,
                children: [],
                position: { x: 10, y: 50 }
            };
            const container: VisualNode = {
                id: 'container',
                type: 'def',
                attributes: { var: 'result' },
                body: null,
                children: [child1, child2],
                position: { x: 0, y: 0 }
            };

            designer.loadNodes([container]);
            designer.render();

            // Move container
            designer.moveNode('container', { x: 100, y: 200 });

            const updatedContainer = designer.findNode('container');
            const updatedChild1 = designer.findNode('child1');
            const updatedChild2 = designer.findNode('child2');

            expect(updatedContainer?.position?.x).toBe(100);
            expect(updatedContainer?.position?.y).toBe(200);

            // Children should move by offset (100, 200)
            expect(updatedChild1?.position?.x).toBe(110); // 10 + 100
            expect(updatedChild1?.position?.y).toBe(210); // 10 + 200
            expect(updatedChild2?.position?.x).toBe(110); // 10 + 100
            expect(updatedChild2?.position?.y).toBe(250); // 50 + 200
        });

        it('should move deeply nested children recursively', () => {
            const grandchild: VisualNode = {
                id: 'grandchild',
                type: 'get',
                attributes: { var: 'var1' },
                body: null,
                children: [],
                position: { x: 20, y: 20 }
            };
            const child: VisualNode = {
                id: 'child',
                type: 'html-to-xml',
                attributes: {},
                body: null,
                children: [grandchild],
                position: { x: 10, y: 10 }
            };
            const container: VisualNode = {
                id: 'container',
                type: 'def',
                attributes: {},
                body: null,
                children: [child],
                position: { x: 0, y: 0 }
            };

            designer.loadNodes([container]);
            designer.render();

            // Move container
            designer.moveNode('container', { x: 50, y: 50 });

            const updatedGrandchild = designer.findNode('grandchild');
            expect(updatedGrandchild?.position?.x).toBe(70); // 20 + 50
            expect(updatedGrandchild?.position?.y).toBe(70); // 20 + 50
        });
    });

    describe('Connection Selection and Context Menu', () => {
        it('should select connection on click', () => {
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
            designer.render();

            // Create manual connection
            const connection = designer.createManualConnection('1', '2', 'sequential');
            expect(connection).toBeDefined();

            // Simulate connection click (via internal method)
            (designer as any).handleConnectionClick(connection.id);

            // Connection should be selected
            expect((designer as any).selectedConnectionId).toBe(connection.id);
        });

        it('should deselect node when selecting connection', () => {
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
            designer.selectNode('1');
            expect(designer.getSelectedNodeId()).toBe('1');

            const connection = designer.createManualConnection('1', '2', 'sequential');
            (designer as any).handleConnectionClick(connection.id);

            expect(designer.getSelectedNodeId()).toBeNull();
            expect((designer as any).selectedConnectionId).toBe(connection.id);
        });

        it('should delete connection from context menu', () => {
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
            const connection = designer.createManualConnection('1', '2', 'sequential');

            expect(designer.getManualConnections()).toHaveLength(1);

            const deleted = designer.deleteConnection(connection.id);
            expect(deleted).toBe(true);
            expect(designer.getManualConnections()).toHaveLength(0);
        });
    });

    describe('Nested Configuration Parsing', () => {
        it('should parse nested def with http and html-to-xml', () => {
            const xml = `<?xml version="1.0" encoding="UTF-8"?>
<config xmlns="http://org.webharvest/schema/2.1/core">
    <def var="target_url">https://httpbin.org/html</def>
    <def var="rawHtml">
        <http url="\${target_url}" method="GET">
            <http-header name="User-Agent" value="Web-Harvest Bot 2.2.0"/>
        </http>
    </def>
    <def var="xmlContent">
        <html-to-xml>
            <get var="rawHtml"/>
        </html-to-xml>
    </def>
</config>`;

            const parser = new XMLParser();
            const nodes = parser.parse(xml);
            expect(nodes).toHaveLength(3);

            const defRawHtml = nodes.find(n => n.type === 'def' && n.attributes.var === 'rawHtml');
            expect(defRawHtml).toBeDefined();
            expect(defRawHtml?.children).toHaveLength(1);
            expect(defRawHtml?.children[0].type).toBe('http');

            const httpNode = defRawHtml?.children[0];
            expect(httpNode?.children).toHaveLength(1);
            expect(httpNode?.children[0].type).toBe('http-header');

            const defXmlContent = nodes.find(n => n.type === 'def' && n.attributes.var === 'xmlContent');
            expect(defXmlContent).toBeDefined();
            expect(defXmlContent?.children).toHaveLength(1);
            expect(defXmlContent?.children[0].type).toBe('html-to-xml');
            expect(defXmlContent?.children[0].children).toHaveLength(1);
            expect(defXmlContent?.children[0].children[0].type).toBe('get');
        });

        it('maps multi-level function/while/http stack for focus workflows', () => {
            const xml = `<?xml version="1.0" encoding="UTF-8"?>
<config xmlns="http://org.webharvest/schema/2.1/core">
    <function name="download-multipage-list">
        <return>
            <while condition="\${pageUrl.toString().length() != 0}" maxloops="\${maxloops}" index="i">
                <def var="content">
                    <html-to-xml advancedxmlescape="true" specialentities="true" unicodechars="true" allowmultiwordattributes="true" omitunknowntags="true">
                        <http url="\${pageUrl}" method="GET">
                            <http-header id="" name="x-foo1"/>
                            <http-header id="" name="x-foo2"/>
                            <http-param id="" name="asas" isfile="false" filename="" contenttype=""/>
                        </http>
                    </html-to-xml>
                </def>
                <def var="nextLinkUrl">
                    <xpath expression="\${nextXPath}">
                        <get var="content"/>
                    </xpath>
                </def>
                <def var="pageUrl">
                    <template>\${sys.fullUrl(pageUrl.toString(), nextLinkUrl.toString())}</template>
                </def>
                <xpath expression="\${itemXPath}">
                    <get var="content"/>
                </xpath>
            </while>
        </return>
    </function>
</config>`;

            const parser = new XMLParser();
            const nodes = parser.parse(xml);
            expect(nodes).toHaveLength(1);

            const functionNode = nodes[0];
            expect(functionNode.type).toBe('function');
            expect(functionNode.children).toHaveLength(1);
            expect(functionNode.children?.[0].type).toBe('return');

            const returnNode = functionNode.children?.[0];
            expect(returnNode?.children).toHaveLength(1);
            expect(returnNode?.children?.[0].type).toBe('while');

            const whileNode = returnNode?.children?.[0];
            expect(whileNode?.children).toHaveLength(4);

            const defContent = whileNode?.children?.[0];
            expect(defContent?.type).toBe('def');
            expect(defContent?.children).toHaveLength(1);
            expect(defContent?.children?.[0].type).toBe('html-to-xml');

            const htmlToXmlNode = defContent?.children?.[0];
            expect(htmlToXmlNode?.children).toHaveLength(1);
            expect(htmlToXmlNode?.children?.[0].type).toBe('http');

            const httpNode = htmlToXmlNode?.children?.[0];
            expect(httpNode?.children).toHaveLength(3);
            const childTypes = httpNode?.children?.map(child => child.type);
            expect(childTypes).toEqual(['http-header', 'http-header', 'http-param']);
        });
    });
});

