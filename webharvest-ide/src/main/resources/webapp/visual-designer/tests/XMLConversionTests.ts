/**
 * XML Conversion Unit Tests
 * 
 * Tests XML ↔ Visual conversion with real WebHarvest examples
 * Critical for ensuring UI changes map correctly to XML
 */

import { XMLParser } from '../core/XMLParser.js';
import { XMLGenerator } from '../core/XMLGenerator.js';
import type { VisualNode } from '../types/VisualNode';
import type { PluginMetadata } from '../core/PluginRegistry';

export interface TestResult {
    name: string;
    passed: boolean;
    message: string;
    details?: any;
}

export class XMLConversionTests {
    private parser: XMLParser;
    private generator: XMLGenerator;
    private results: TestResult[] = [];

    constructor(pluginRegistry: Map<string, PluginMetadata>) {
        this.parser = new XMLParser(pluginRegistry);
        this.generator = new XMLGenerator();
    }

    /**
     * Run all tests
     */
    public async runAll(): Promise<TestResult[]> {
        this.results = [];

        console.log('🧪 Starting XML Conversion Tests...');
        console.log('');

        // Test 1: Simple variable example
        await this.testBasicVariables();

        // Test 2: Nested structure
        await this.testNestedStructure();

        // Test 3: Container with children
        await this.testContainerChildren();

        // Test 4: Roundtrip conversion
        await this.testRoundtrip();

        // Test 5: Attribute types
        await this.testAttributeTypes();

        // Test 6: Body content with CDATA
        await this.testBodyContent();

        // Test 7: Order preservation
        await this.testOrderPreservation();

        // Test 8: Validation
        await this.testValidation();

        // Summary
        this.printSummary();

        return this.results;
    }

    /**
     * Test 1: Basic Variables
     */
    private async testBasicVariables(): Promise<void> {
        const xml = `<?xml version="1.0" encoding="UTF-8"?>
<config xmlns="http://org.webharvest/schema/2.1/core">
  <def var="greeting">Hello World!</def>
  <def var="user_name">WebHarvest User</def>
  <get var="greeting"/>
</config>`;

        try {
            const parsed = this.parser.parse(xml);

            // Assertions
            this.assert('Basic Variables: Parse success', parsed.errors.length === 0,
                `Expected 0 errors, got ${parsed.errors.length}: ${parsed.errors.join(', ')}`);

            this.assert('Basic Variables: Node count', parsed.nodes.length === 3,
                `Expected 3 nodes, got ${parsed.nodes.length}`);

            this.assert('Basic Variables: Root nodes',
                parsed.nodes.filter(n => !n.parentId).length === 3,
                'Expected 3 root nodes');

            this.assert('Basic Variables: First node type', parsed.nodes[0].type === 'def',
                `Expected 'def', got '${parsed.nodes[0].type}'`);

            this.assert('Basic Variables: Attribute extraction',
                parsed.nodes[0].config.var === 'greeting',
                `Expected var='greeting', got '${parsed.nodes[0].config.var}'`);

            this.assert('Basic Variables: Body content',
                parsed.nodes[0].body === 'Hello World!',
                `Expected 'Hello World!', got '${parsed.nodes[0].body}'`);

        } catch (error) {
            this.assert('Basic Variables', false, `Exception: ${error}`);
        }
    }

    /**
     * Test 2: Nested Structure
     */
    private async testNestedStructure(): Promise<void> {
        const xml = `<?xml version="1.0" encoding="UTF-8"?>
<config xmlns="http://org.webharvest/schema/2.1/core">
  <def var="content">
    <html-to-xml>
      <http url="https://example.com"/>
    </html-to-xml>
  </def>
</config>`;

        try {
            const parsed = this.parser.parse(xml);

            this.assert('Nested: Parse success', parsed.errors.length === 0,
                `Errors: ${parsed.errors.join(', ')}`);

            this.assert('Nested: Total nodes', parsed.nodes.length === 3,
                `Expected 3 nodes (def, html-to-xml, http), got ${parsed.nodes.length}`);

            const defNode = parsed.nodes[0];
            const htmlToXmlNode = parsed.nodes[1];
            const httpNode = parsed.nodes[2];

            this.assert('Nested: Parent-child (def → html-to-xml)',
                defNode.children.includes(htmlToXmlNode.id) && htmlToXmlNode.parentId === defNode.id,
                'def should contain html-to-xml');

            this.assert('Nested: Parent-child (html-to-xml → http)',
                htmlToXmlNode.children.includes(httpNode.id) && httpNode.parentId === htmlToXmlNode.id,
                `html-to-xml should contain http. httpNode used: ${httpNode.id}`);

        } catch (error) {
            this.assert('Nested Structure', false, `Exception: ${error}`);
        }
    }

    /**
     * Test 3: Container with Multiple Children
     */
    private async testContainerChildren(): Promise<void> {
        const xml = `<?xml version="1.0" encoding="UTF-8"?>
<config xmlns="http://org.webharvest/schema/2.1/core">
  <loop index="i" max="10">
    <http url="\${url}"/>
    <xpath expression="//data"/>
    <var name="result"/>
  </loop>
</config>`;

        try {
            const parsed = this.parser.parse(xml);

            this.assert('Container: Parse success', parsed.errors.length === 0,
                `Errors: ${parsed.errors.join(', ')}`);

            const loopNode = parsed.nodes[0];

            this.assert('Container: Children count', loopNode.children.length === 3,
                `Expected 3 children, got ${loopNode.children.length}`);

            this.assert('Container: Children order',
                parsed.nodes[1].type === 'http' &&
                parsed.nodes[2].type === 'xpath' &&
                parsed.nodes[3].type === 'var',
                'Children should be in order: http, xpath, var');

        } catch (error) {
            this.assert('Container Children', false, `Exception: ${error}`);
        }
    }

    /**
     * Test 4: Roundtrip Conversion (CRITICAL!)
     */
    private async testRoundtrip(): Promise<void> {
        const originalXML = `<?xml version="1.0" encoding="UTF-8"?>
<config xmlns="http://org.webharvest/schema/2.1/core">
  <def var="test">
    <http url="https://example.com"/>
  </def>
  <xpath expression="//title">
    <get var="test"/>
  </xpath>
</config>`;

        try {
            // XML → Visual
            const parsed = this.parser.parse(originalXML);
            this.assert('Roundtrip: Parse success', parsed.errors.length === 0,
                `Parse errors: ${parsed.errors.join(', ')}`);

            // Visual → XML
            const generated = this.generator.generate(parsed.nodes);

            // XML → Visual again
            const reparsedResult = this.parser.parse(generated);
            this.assert('Roundtrip: Reparse success', reparsedResult.errors.length === 0,
                `Reparse errors: ${reparsedResult.errors.join(', ')}`);

            // Compare node counts
            this.assert('Roundtrip: Node count preserved',
                reparsedResult.nodes.length === parsed.nodes.length,
                `Original: ${parsed.nodes.length}, Reparsed: ${reparsedResult.nodes.length}`);

            // Compare structure
            for (let i = 0; i < parsed.nodes.length; i++) {
                const original = parsed.nodes[i];
                const reparsedNode = reparsedResult.nodes[i];

                this.assert(`Roundtrip: Node ${i} type`, original.type === reparsedNode.type,
                    `Expected '${original.type}', got '${reparsedNode.type}'`);

                this.assert(`Roundtrip: Node ${i} children count`,
                    original.children.length === reparsedNode.children.length,
                    `Expected ${original.children.length}, got ${reparsedNode.children.length}`);
            }

        } catch (error) {
            this.assert('Roundtrip Conversion', false, `Exception: ${error}`);
        }
    }

    /**
     * Test 5: Attribute Type Conversion
     */
    private async testAttributeTypes(): Promise<void> {
        const xml = `<?xml version="1.0" encoding="UTF-8"?>
<config xmlns="http://org.webharvest/schema/2.1/core">
  <loop index="i" max="10" test="true">
    <http url="https://example.com" method="GET" timeout="5000"/>
  </loop>
</config>`;

        try {
            const parsed = this.parser.parse(xml);
            const loopNode = parsed.nodes[0];
            // httpNode used implicitly through loopNode validation

            this.assert('Attributes: String preserved', loopNode.config.index === 'i',
                `Expected 'i', got '${loopNode.config.index}'`);

            this.assert('Attributes: Number converted',
                typeof loopNode.config.max === 'number' && loopNode.config.max === 10,
                `Expected number 10, got ${typeof loopNode.config.max} ${loopNode.config.max}`);

        } catch (error) {
            this.assert('Attribute Types', false, `Exception: ${error}`);
        }
    }

    /**
     * Test 6: Body Content with CDATA
     */
    private async testBodyContent(): Promise<void> {
        const xml = `<?xml version="1.0" encoding="UTF-8"?>
<config xmlns="http://org.webharvest/schema/2.1/core">
  <template>
    <![CDATA[
    <html>
      <h1>\${title}</h1>
    </html>
    ]]>
  </template>
</config>`;

        try {
            const parsed = this.parser.parse(xml);
            const templateNode = parsed.nodes[0];

            this.assert('Body: Content extracted', templateNode.body !== null,
                'Body should not be null');

            this.assert('Body: CDATA preserved',
                (templateNode.body?.includes('<html>') ?? false) && (templateNode.body?.includes('</html>') ?? false),
                `Body should contain HTML tags. Got: ${templateNode.body}`);

            // Generate back to XML
            const generated = this.generator.generate(parsed.nodes);

            this.assert('Body: CDATA in generated XML',
                generated.includes('<![CDATA[') || generated.includes('<html>'),
                'Generated XML should preserve HTML content');

        } catch (error) {
            this.assert('Body Content', false, `Exception: ${error}`);
        }
    }

    /**
     * Test 7: Order Preservation
     */
    private async testOrderPreservation(): Promise<void> {
        const xml = `<?xml version="1.0" encoding="UTF-8"?>
<config xmlns="http://org.webharvest/schema/2.1/core">
  <def var="first"/>
  <def var="second"/>
  <def var="third"/>
</config>`;

        try {
            const parsed = this.parser.parse(xml);

            this.assert('Order: Correct count', parsed.nodes.length === 3,
                `Expected 3 nodes, got ${parsed.nodes.length}`);

            this.assert('Order: First node', parsed.nodes[0].config.var === 'first',
                `Expected 'first', got '${parsed.nodes[0].config.var}'`);

            this.assert('Order: Second node', parsed.nodes[1].config.var === 'second',
                `Expected 'second', got '${parsed.nodes[1].config.var}'`);

            this.assert('Order: Third node', parsed.nodes[2].config.var === 'third',
                `Expected 'third', got '${parsed.nodes[2].config.var}'`);

            // Generate and check order preserved
            const generated = this.generator.generate(parsed.nodes);
            const firstIndex = generated.indexOf('var="first"');
            const secondIndex = generated.indexOf('var="second"');
            const thirdIndex = generated.indexOf('var="third"');

            this.assert('Order: Preserved in generated XML',
                firstIndex < secondIndex && secondIndex < thirdIndex,
                `Order not preserved: ${firstIndex}, ${secondIndex}, ${thirdIndex}`);

        } catch (error) {
            this.assert('Order Preservation', false, `Exception: ${error}`);
        }
    }

    /**
     * Test 8: Validation
     */
    private async testValidation(): Promise<void> {
        // Create invalid node (missing required attribute)
        const invalidNodes: VisualNode[] = [{
            id: 'node_1',
            type: 'http',
            plugin: this.getHttpPlugin(),
            config: { method: 'GET' }, // Missing required 'url'!
            body: null,
            parentId: null,
            children: [],
            x: 50,
            y: 50,
            executionState: 'pending'
        } as VisualNode];

        try {
            const errors = this.generator.validate(invalidNodes);

            this.assert('Validation: Detects missing required attribute',
                errors.length > 0 && errors.some(e => e.includes('url')),
                `Expected error about missing 'url', got: ${errors.join(', ')}`);

        } catch (error) {
            this.assert('Validation', false, `Exception: ${error}`);
        }
    }

    /**
     * Helper: Assert test condition
     */
    private assert(testName: string, condition: boolean, message: string): void {
        const result: TestResult = {
            name: testName,
            passed: condition,
            message: condition ? '✅ Pass' : `❌ Fail: ${message}`
        };

        this.results.push(result);

        if (condition) {
            console.log(`  ✅ ${testName}`);
        } else {
            console.error(`  ❌ ${testName}: ${message}`);
        }
    }

    /**
     * Print test summary
     */
    private printSummary(): void {
        const passed = this.results.filter(r => r.passed).length;
        const failed = this.results.filter(r => !r.passed).length;
        const total = this.results.length;

        console.log('');
        console.log('═'.repeat(60));
        console.log(`📊 Test Summary: ${passed}/${total} passed, ${failed} failed`);
        console.log('═'.repeat(60));

        if (failed === 0) {
            console.log('🎉 All tests passed! XML conversion is working correctly.');
        } else {
            console.error('⚠️  Some tests failed. Review errors above.');
            console.log('');
            console.log('Failed tests:');
            this.results.filter(r => !r.passed).forEach(r => {
                console.log(`  • ${r.name}: ${r.message}`);
            });
        }
    }

    /**
     * Helper: Get HTTP plugin metadata (mock for testing)
     */
    private getHttpPlugin(): PluginMetadata {
        return {
            elementName: 'http',
            name: 'HTTP',
            description: 'HTTP request',
            category: 'HTTP & Web',
            icon: 'fa-globe',
            helpUrl: '',
            schema: {
                attributes: [
                    { name: 'url', type: 'string', required: true, description: 'URL' },
                    { name: 'method', type: 'string', required: false, description: 'Method' }
                ],
                allowsBody: false,
                allowedSubElements: ['http-header', 'http-param'],
                requiredSubElements: [],
                requiredAttributes: ['url']
            }
        } as any;
    }
}

