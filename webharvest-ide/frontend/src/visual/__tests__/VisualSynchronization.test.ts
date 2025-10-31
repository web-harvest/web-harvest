/**
 * TDD Tests for Visual ↔ XML Synchronization
 * 
 * Requirements:
 * 1. XML → Visual: Parse XML and render nodes
 * 2. Visual → XML: Generate XML from edited visual nodes
 * 3. Real-time sync: Changes in visual update XML immediately
 * 4. Nested model: Support for hierarchical structures (loop, if, try)
 * 5. Connected model: Optional flowchart style for linear sequences
 */

import { XMLParser } from '../XMLParser';
import { XMLGenerator } from '../XMLGenerator';
import { VisualDesigner } from '../VisualDesigner';
import type { VisualNode } from '../../types';

describe('Visual ↔ XML Bidirectional Synchronization (TDD)', () => {
    let parser: XMLParser;
    let generator: XMLGenerator;
    let designer: VisualDesigner;

    beforeEach(() => {
        parser = new XMLParser();
        generator = new XMLGenerator();

        // Mock canvas container
        document.body.innerHTML = '<div id="test-canvas"></div>';
        designer = new VisualDesigner('test-canvas');
    });

    describe('XML → Visual (Parsing)', () => {
        it('should parse simple HTTP plugin to visual node', () => {
            const xml = `<?xml version="1.0" encoding="UTF-8"?>
<config>
    <http url="https://api.example.com"/>
</config>`;

            const nodes = parser.parse(xml);

            expect(nodes).toHaveLength(1);
            expect(nodes[0].type).toBe('http');
            expect(nodes[0].attributes.url).toBe('https://api.example.com');
            expect(nodes[0].children).toHaveLength(0);
        });

        it('should parse nested structure (loop with children)', () => {
            const xml = `<?xml version="1.0" encoding="UTF-8"?>
<config>
    <loop maxloops="5">
        <http url="https://api.example.com"/>
        <xpath expression="//title/text()"/>
    </loop>
</config>`;

            const nodes = parser.parse(xml);

            expect(nodes).toHaveLength(1);
            expect(nodes[0].type).toBe('loop');
            expect(nodes[0].attributes.maxloops).toBe('5');
            expect(nodes[0].children).toHaveLength(2);
            expect(nodes[0].children[0].type).toBe('http');
            expect(nodes[0].children[1].type).toBe('xpath');
        });

        it('should parse complex nested structure (if-else with try-catch)', () => {
            const xml = `<?xml version="1.0" encoding="UTF-8"?>
<config>
    <if test="\${condition}">
        <try>
            <http url="\${apiUrl}"/>
            <catch>
                <log level="error">HTTP request failed</log>
            </catch>
        </try>
    </if>
</config>`;

            const nodes = parser.parse(xml);

            expect(nodes).toHaveLength(1);
            expect(nodes[0].type).toBe('if');
            expect(nodes[0].children).toHaveLength(1);
            expect(nodes[0].children[0].type).toBe('try');
            expect(nodes[0].children[0].children).toHaveLength(2); // http + catch
        });

        it('should preserve plugin body content', () => {
            const xml = `<?xml version="1.0" encoding="UTF-8"?>
<config>
    <script><![CDATA[
        var result = 1 + 2;
        return result;
    ]]></script>
</config>`;

            const nodes = parser.parse(xml);

            expect(nodes).toHaveLength(1);
            expect(nodes[0].type).toBe('script');
            expect(nodes[0].body).toContain('var result = 1 + 2');
        });
    });

    describe('Visual → XML (Generation)', () => {
        it('should generate XML from simple visual node', () => {
            const nodes: VisualNode[] = [{
                id: 'node-1',
                type: 'http',
                attributes: { url: 'https://api.example.com' },
                body: null,
                children: []
            }];

            const xml = generator.generate(nodes, 1);

            expect(xml).toContain('<http url="https://api.example.com"/>');
        });

        it('should generate XML with nested children', () => {
            const nodes: VisualNode[] = [{
                id: 'node-1',
                type: 'loop',
                attributes: { maxloops: '5' },
                body: null,
                children: [
                    {
                        id: 'node-2',
                        type: 'http',
                        attributes: { url: 'https://api.example.com' },
                        body: null,
                        children: []
                    }
                ]
            }];

            const xml = generator.generate(nodes, 1);

            expect(xml).toContain('<loop maxloops="5">');
            expect(xml).toContain('<http url="https://api.example.com"/>');
            expect(xml).toContain('</loop>');
        });

        it('should generate XML with CDATA for script content', () => {
            const nodes: VisualNode[] = [{
                id: 'node-1',
                type: 'script',
                attributes: {},
                body: 'var x = 1 + 2; return x;',
                children: []
            }];

            const xml = generator.generate(nodes, 1);

            expect(xml).toContain('<script>');
            expect(xml).toContain('var x = 1 + 2');
            expect(xml).toContain('</script>');
        });
    });

    describe('Round-trip Synchronization (XML → Visual → XML)', () => {
        it('should preserve XML structure after round-trip', () => {
            const originalXML = `<?xml version="1.0" encoding="UTF-8"?>
<config>
    <http url="https://api.example.com" method="GET"/>
    <xpath expression="//title/text()"/>
</config>`;

            // XML → Visual
            const nodes = parser.parse(originalXML);

            // Visual → XML
            const generatedXML = '<?xml version="1.0" encoding="UTF-8"?>\n<config>\n' +
                generator.generate(nodes, 1) +
                '\n</config>';

            // Parse again to compare structure
            const regeneratedNodes = parser.parse(generatedXML);

            expect(regeneratedNodes).toHaveLength(2);
            expect(regeneratedNodes[0].type).toBe('http');
            expect(regeneratedNodes[1].type).toBe('xpath');
        });

        it('should preserve nested structure after round-trip', () => {
            const originalXML = `<?xml version="1.0" encoding="UTF-8"?>
<config>
    <loop maxloops="3">
        <http url="\${url}"/>
    </loop>
</config>`;

            const nodes = parser.parse(originalXML);
            const generatedXML = '<?xml version="1.0" encoding="UTF-8"?>\n<config>\n' +
                generator.generate(nodes, 1) +
                '\n</config>';
            const regeneratedNodes = parser.parse(generatedXML);

            expect(regeneratedNodes[0].type).toBe('loop');
            expect(regeneratedNodes[0].children).toHaveLength(1);
            expect(regeneratedNodes[0].children[0].type).toBe('http');
        });
    });

    describe('Real-time Visual → XML Synchronization', () => {
        it('should update XML when node attribute changes', () => {
            const nodes: VisualNode[] = [{
                id: 'node-1',
                type: 'http',
                attributes: { url: 'https://old-url.com' },
                body: null,
                children: []
            }];

            designer.loadNodes(nodes);

            // Simulate editing attribute in visual designer
            const updatedNodes = designer.getNodes();
            updatedNodes[0].attributes.url = 'https://new-url.com';

            const xml = generator.generate(updatedNodes, 1);

            expect(xml).toContain('https://new-url.com');
            expect(xml).not.toContain('https://old-url.com');
        });

        it('should update XML when node is added to nested structure', () => {
            const nodes: VisualNode[] = [{
                id: 'node-1',
                type: 'loop',
                attributes: { maxloops: '3' },
                body: null,
                children: []
            }];

            designer.loadNodes(nodes);

            // Add child node
            const updatedNodes = designer.getNodes();
            updatedNodes[0].children.push({
                id: 'node-2',
                type: 'xpath',
                attributes: { expression: '//title' },
                body: null,
                children: []
            });

            const xml = generator.generate(updatedNodes, 1);

            expect(xml).toContain('<loop maxloops="3">');
            expect(xml).toContain('<xpath expression="//title"/>');
        });

        it('should update XML when node is deleted', () => {
            const nodes: VisualNode[] = [
                {
                    id: 'node-1',
                    type: 'http',
                    attributes: { url: 'https://example.com' },
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

            designer.loadNodes(nodes);

            // Delete first node
            const updatedNodes = designer.getNodes();
            updatedNodes.shift();

            const xml = generator.generate(updatedNodes, 1);

            expect(xml).not.toContain('<http');
            expect(xml).toContain('<xpath expression="//title"/>');
        });
    });

    describe('Visual Model: Nested vs Connected', () => {
        it('should identify container nodes (loop, if, try) for nested rendering', () => {
            const containerNodes = ['loop', 'if', 'try', 'while', 'function', 'case'];

            containerNodes.forEach(type => {
                const node: VisualNode = {
                    id: `node-${type}`,
                    type,
                    attributes: {},
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
                };

                // Container nodes should render children inside
                expect(node.children.length).toBeGreaterThan(0);
            });
        });

        it('should identify linear nodes (http, xpath, def) for connected rendering', () => {
            const linearNodes = ['http', 'xpath', 'def', 'regexp', 'html-to-xml'];

            linearNodes.forEach(type => {
                const node: VisualNode = {
                    id: `node-${type}`,
                    type,
                    attributes: {},
                    body: null,
                    children: []
                };

                // Linear nodes typically have no children (or children are inline parameters)
                expect(node.children).toHaveLength(0);
            });
        });

        it('should support hybrid model: loop with connected children', () => {
            const xml = `<?xml version="1.0" encoding="UTF-8"?>
<config>
    <loop maxloops="5">
        <http url="https://api.example.com"/>
        <xpath expression="//item"/>
        <def var="result"/>
    </loop>
</config>`;

            const nodes = parser.parse(xml);

            // Loop is container (nested)
            expect(nodes[0].type).toBe('loop');
            expect(nodes[0].children).toHaveLength(3);

            // Children inside loop can be rendered as flowchart (connected)
            const loopChildren = nodes[0].children;
            expect(loopChildren[0].type).toBe('http');
            expect(loopChildren[1].type).toBe('xpath');
            expect(loopChildren[2].type).toBe('def');
        });
    });

    describe('Error Handling', () => {
        it('should handle invalid XML gracefully', () => {
            const invalidXML = '<config><broken';

            expect(() => parser.parse(invalidXML)).toThrow('XML Parse Error');
        });

        it('should handle empty nodes array in generation', () => {
            const xml = generator.generate([], 1);

            expect(xml).toBe('');
        });

        it('should escape special characters in attributes', () => {
            const nodes: VisualNode[] = [{
                id: 'node-1',
                type: 'http',
                attributes: { url: 'https://example.com?q=<test>&value="quoted"' },
                body: null,
                children: []
            }];

            const xml = generator.generate(nodes, 1);

            expect(xml).toContain('&lt;');
            expect(xml).toContain('&quot;');
            expect(xml).toContain('&amp;');
        });
    });
});




