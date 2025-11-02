/**
 * End-to-End Integration Tests for Visual Designer
 * 
 * Tests complete workflow:
 * 1. Load XML → Parse to visual
 * 2. Edit nodes in visual designer
 * 3. Generate XML from visual
 * 4. Verify round-trip integrity
 */

import { XMLParser } from '../XMLParser';
import { XMLGenerator } from '../XMLGenerator';
import { VisualDesigner } from '../VisualDesigner';
import { PropertyPanel } from '../PropertyPanel';
import { PluginPalette } from '../PluginPalette';
import { createNodeFromTemplate, getPluginTemplates } from '../VisualModelHelpers';

describe('Visual Designer E2E Integration (TDD)', () => {
    let parser: XMLParser;
    let generator: XMLGenerator;
    let designer: VisualDesigner;
    let propertyPanel: PropertyPanel;
    let palette: PluginPalette;

    beforeEach(() => {
        // Setup DOM
        document.body.innerHTML = `
            <div id="canvas-area"></div>
            <div id="propertyPanel"></div>
            <div id="plugin-palette"></div>
        `;

        // Initialize components
        parser = new XMLParser();
        generator = new XMLGenerator();
        designer = new VisualDesigner('canvas-area');
        propertyPanel = new PropertyPanel('propertyPanel');
        palette = new PluginPalette('plugin-palette');
    });

    describe('Complete Workflow: Load → Edit → Save', () => {
        it('should load XML, edit in visual, and generate correct XML', () => {
            // Step 1: Load XML
            const originalXML = `<?xml version="1.0" encoding="UTF-8"?>
<config xmlns="http://org.webharvest/schema/2.1/core">
    <http url="https://api.example.com" method="GET"/>
    <xpath expression="//title/text()"/>
    <def var="title"/>
</config>`;

            // Step 2: Parse to visual
            const nodes = parser.parse(originalXML);
            expect(nodes).toHaveLength(3);

            // Step 3: Load into designer
            designer.loadNodes(nodes);
            const loadedNodes = designer.getNodes();
            expect(loadedNodes).toHaveLength(3);

            // Step 4: Edit node (change HTTP URL)
            const httpNode = loadedNodes.find(n => n.type === 'http');
            expect(httpNode).toBeTruthy();
            httpNode!.attributes.url = 'https://new-api.example.com';
            designer.updateNode(httpNode!.id, httpNode!);

            // Step 5: Generate XML
            const updatedNodes = designer.getNodes();
            const generatedXML = generator.generate(updatedNodes, 1);

            // Step 6: Verify XML contains changes
            expect(generatedXML).toContain('https://new-api.example.com');
            expect(generatedXML).not.toContain('https://api.example.com');
            expect(generatedXML).toContain('xpath');
            expect(generatedXML).toContain('def');
        });

        it('should handle nested structure (loop with children)', () => {
            // Step 1: Load XML with loop
            const originalXML = `<?xml version="1.0" encoding="UTF-8"?>
<config xmlns="http://org.webharvest/schema/2.1/core">
    <loop maxloops="5">
        <http url="https://api.example.com"/>
        <xpath expression="//item"/>
    </loop>
</config>`;

            // Step 2: Parse and load
            const nodes = parser.parse(originalXML);
            designer.loadNodes(nodes);

            // Step 3: Verify structure
            const loadedNodes = designer.getNodes();
            expect(loadedNodes).toHaveLength(1);
            expect(loadedNodes[0].type).toBe('loop');
            expect(loadedNodes[0].children).toHaveLength(2);

            // Step 4: Generate XML
            const generatedXML = generator.generate(loadedNodes, 1);

            // Step 5: Verify nested structure preserved
            expect(generatedXML).toContain('<loop maxloops="5">');
            expect(generatedXML).toContain('http url="https://api.example.com"');
            expect(generatedXML).toContain('</loop>');
        });

        it('should add new node from palette and sync to XML', () => {
            // Step 1: Start with simple config
            const originalXML = `<?xml version="1.0" encoding="UTF-8"?>
<config xmlns="http://org.webharvest/schema/2.1/core">
    <http url="https://api.example.com"/>
</config>`;

            // Step 2: Load
            const nodes = parser.parse(originalXML);
            designer.loadNodes(nodes);

            // Step 3: Add new node from palette
            const xpathTemplate = getPluginTemplates().find(t => t.type === 'xpath')!;
            const newNode = createNodeFromTemplate(xpathTemplate);
            newNode.attributes.expression = '//title/text()';
            designer.addNode(newNode);

            // Step 4: Generate XML
            const updatedNodes = designer.getNodes();
            const generatedXML = generator.generate(updatedNodes, 1);

            // Step 5: Verify new node in XML
            expect(generatedXML).toContain('http');
            expect(generatedXML).toContain('xpath');
            expect(generatedXML).toContain('//title/text()');
        });

        it('should handle script body content with CDATA', () => {
            // Step 1: Load script
            const originalXML = `<?xml version="1.0" encoding="UTF-8"?>
<config xmlns="http://org.webharvest/schema/2.1/core">
    <script><![CDATA[
        var result = 1 + 2;
        return result;
    ]]></script>
</config>`;

            // Step 2: Parse and load
            const nodes = parser.parse(originalXML);
            designer.loadNodes(nodes);

            // Step 3: Verify body preserved
            const scriptNode = designer.getNodes()[0];
            expect(scriptNode.type).toBe('script');
            expect(scriptNode.body).toContain('var result = 1 + 2');

            // Step 4: Edit body
            scriptNode.body = 'var x = 5;\nreturn x * 2;';
            designer.updateNode(scriptNode.id, scriptNode);

            // Step 5: Generate XML
            const generatedXML = generator.generate(designer.getNodes(), 1);

            // Step 6: Verify CDATA and new body
            expect(generatedXML).toContain('var x = 5');
            expect(generatedXML).toContain('return x * 2');
        });
    });

    describe('Complex Scenarios', () => {
        it('should handle deeply nested structure (if > try > loop)', () => {
            const xml = `<?xml version="1.0" encoding="UTF-8"?>
<config xmlns="http://org.webharvest/schema/2.1/core">
    <if test="\${condition}">
        <try>
            <loop maxloops="3">
                <http url="https://api.example.com"/>
            </loop>
            <catch>
                <log level="error">Failed</log>
            </catch>
        </try>
    </if>
</config>`;

            // Parse and load
            const nodes = parser.parse(xml);
            designer.loadNodes(nodes);

            // Verify structure
            const ifNode = designer.getNodes()[0];
            expect(ifNode.type).toBe('if');
            expect(ifNode.children[0].type).toBe('try');
            expect(ifNode.children[0].children[0].type).toBe('loop');
            expect(ifNode.children[0].children[0].children[0].type).toBe('http');

            // Generate and verify
            const generatedXML = generator.generate(designer.getNodes(), 1);
            expect(generatedXML).toContain('<if test="${condition}">');
            expect(generatedXML).toContain('<try>');
            expect(generatedXML).toContain('<loop maxloops="3">');
        });

        it('should handle XQuery example with multiple params', () => {
            const xml = `<?xml version="1.0" encoding="UTF-8"?>
<config xmlns="http://org.webharvest/schema/2.1/core">
    <def var="result">
        <xquery>
            <xq-param name="value" type="integer">42</xq-param>
            <xq-expression><![CDATA[
                declare variable $value as xs:integer external;
                <result>{$value * 2}</result>
            ]]></xq-expression>
        </xquery>
    </def>
</config>`;

            // Parse and load
            const nodes = parser.parse(xml);
            designer.loadNodes(nodes);

            // Verify structure
            const defNode = designer.getNodes()[0];
            expect(defNode.type).toBe('def');
            expect(defNode.children[0].type).toBe('xquery');
            expect(defNode.children[0].children.length).toBeGreaterThan(0);

            // Generate and verify
            const generatedXML = generator.generate(designer.getNodes(), 1);
            expect(generatedXML).toContain('xquery');
            expect(generatedXML).toContain('xq-param');
        });
    });

    describe('Error Handling', () => {
        it('should handle invalid XML gracefully', () => {
            const invalidXML = '<config><broken';

            expect(() => {
                parser.parse(invalidXML);
            }).toThrow();
        });

        it('should handle empty canvas', () => {
            const emptyNodes = designer.getNodes();
            expect(emptyNodes).toHaveLength(0);

            const xml = generator.generate(emptyNodes, 1);
            expect(xml).toBe('');
        });

        it('should preserve unknown attributes', () => {
            const xml = `<?xml version="1.0" encoding="UTF-8"?>
<config xmlns="http://org.webharvest/schema/2.1/core">
    <http url="https://api.example.com" custom-attr="value"/>
</config>`;

            const nodes = parser.parse(xml);
            designer.loadNodes(nodes);

            const generatedXML = generator.generate(designer.getNodes(), 1);
            expect(generatedXML).toContain('custom-attr="value"');
        });
    });

    describe('Real-time Synchronization', () => {
        it('should update XML immediately when node attribute changes', () => {
            const xml = `<?xml version="1.0" encoding="UTF-8"?>
<config xmlns="http://org.webharvest/schema/2.1/core">
    <http url="https://old-url.com"/>
</config>`;

            // Load
            const nodes = parser.parse(xml);
            designer.loadNodes(nodes);

            // Edit
            const httpNode = designer.getNodes()[0];
            httpNode.attributes.url = 'https://new-url.com';
            designer.updateNode(httpNode.id, httpNode);

            // Generate immediately (no debounce in tests)
            const updatedXML = generator.generate(designer.getNodes(), 1);
            expect(updatedXML).toContain('https://new-url.com');
        });

        it('should update XML when nodes are reordered', () => {
            const xml = `<?xml version="1.0" encoding="UTF-8"?>
<config xmlns="http://org.webharvest/schema/2.1/core">
    <http url="https://api.example.com"/>
    <xpath expression="//title"/>
    <def var="result"/>
</config>`;

            // Load
            const nodes = parser.parse(xml);
            designer.loadNodes(nodes);

            // Reorder: move def to first position
            const allNodes = designer.getNodes();
            const defNode = allNodes.find(n => n.type === 'def')!;
            const httpNode = allNodes.find(n => n.type === 'http')!;
            const xpathNode = allNodes.find(n => n.type === 'xpath')!;

            const reordered = [defNode, httpNode, xpathNode];
            designer.loadNodes(reordered);

            // Generate
            const generatedXML = generator.generate(designer.getNodes(), 1);
            const lines = generatedXML.split('\n');

            // Verify order (def should be first)
            const defIndex = lines.findIndex(l => l.includes('<def'));
            const httpIndex = lines.findIndex(l => l.includes('<http'));
            expect(defIndex).toBeLessThan(httpIndex);
        });
    });
});




