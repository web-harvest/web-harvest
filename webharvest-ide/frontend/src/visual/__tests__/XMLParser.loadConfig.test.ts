/**
 * XMLParser + VisualDesigner Integration Tests
 * Test loadConfig scenario: XML -> Parser -> Designer -> Model
 */

import { describe, it, expect, beforeEach, afterEach } from '@jest/globals';
import { XMLParser } from '../XMLParser';
import { VisualDesigner } from '../VisualDesigner';
import type { VisualNode, Plugin } from '../types';
import type { PluginManager } from '../../core/PluginManager';

class MockPluginManager implements Partial<PluginManager> {
    private plugins: Map<string, Plugin> = new Map();

    findPlugin(type: string): Plugin | null {
        return this.plugins.get(type) || null;
    }

    getPlugins(): Plugin[] {
        return Array.from(this.plugins.values());
    }

    getSchemaVersion(): string | null {
        return null;
    }

    addPlugin(plugin: Plugin): void {
        this.plugins.set(plugin.id, plugin);
    }
}

describe('XMLParser + VisualDesigner Integration', () => {
    let parser: XMLParser;
    let designer: VisualDesigner;
    let container: HTMLElement;
    let pluginManager: MockPluginManager;

    beforeEach(() => {
        container = document.createElement('div');
        container.id = 'test-canvas';
        document.body.appendChild(container);

        parser = new XMLParser();
        designer = new VisualDesigner('test-canvas');
        
        // Initialize PluginManager for semantic model
        pluginManager = new MockPluginManager() as any;
        pluginManager.addPlugin({
            id: 'def',
            name: 'Def',
            category: 'variable',
            icon: 'fa-code',
            defaultWidth: 200,
            defaultHeight: 100,
            semantics: { canAppearAtRoot: true, allowsChildren: true }
        } as Plugin);
        pluginManager.addPlugin({
            id: 'var',
            name: 'Var',
            category: 'variable',
            icon: 'fa-code',
            defaultWidth: 200,
            defaultHeight: 100,
            semantics: { canAppearAtRoot: true, allowsChildren: false }
        } as Plugin);
        pluginManager.addPlugin({
            id: 'http',
            name: 'HTTP',
            category: 'http',
            icon: 'fa-globe',
            defaultWidth: 200,
            defaultHeight: 100,
            semantics: { canAppearAtRoot: true, allowsChildren: false }
        } as Plugin);
        pluginManager.addPlugin({
            id: 'loop',
            name: 'Loop',
            category: 'control',
            icon: 'fa-repeat',
            defaultWidth: 200,
            defaultHeight: 100,
            semantics: { canAppearAtRoot: true, allowsChildren: true }
        } as Plugin);
        
        designer.setPluginManager(pluginManager as any);
    });

    afterEach(() => {
        if (container && container.parentNode) {
            document.body.removeChild(container);
        }
    });

    describe('loadConfig scenario', () => {
        it('should parse XML with def and var elements', () => {
            const xml = `<config>
    <def var="title">
        <http url="https://example.com"/>
    </def>
    <var name="result">\${title}</var>
</config>`;

            const nodes = parser.parse(xml);

            expect(nodes).toHaveLength(2);
            expect(nodes[0].type).toBe('def');
            expect(nodes[0].attributes.var).toBe('title');
            expect(nodes[0].children).toHaveLength(1);
            expect(nodes[0].children[0].type).toBe('http');
            expect(nodes[1].type).toBe('var');
            expect(nodes[1].attributes.name).toBe('result');
        });

        it('should load parsed nodes into designer', () => {
            const xml = `<config>
    <def var="title">
        <http url="https://example.com"/>
    </def>
    <var name="result">\${title}</var>
</config>`;

            const nodes = parser.parse(xml);
            designer.loadNodes(nodes);

            const loadedNodes = designer.getNodes();
            expect(loadedNodes).toHaveLength(2);
            expect(loadedNodes[0].type).toBe('def');
            expect(loadedNodes[1].type).toBe('var');
        });

        it('should build semantic model after loadNodes', async () => {
            const xml = `<config>
    <def var="title">
        <http url="https://example.com"/>
    </def>
    <var name="result">\${title}</var>
</config>`;

            const nodes = parser.parse(xml);
            designer.loadNodes(nodes);

            // Wait for async operations (auto-layout, model build)
            await new Promise(resolve => setTimeout(resolve, 100));

            const model = designer.exportModelForLLM();
            
            expect(model).toBeDefined();
            expect(model.workflow).toBeDefined();
            expect(model.workflow.rootNodes.length).toBeGreaterThan(0);
            expect(Object.keys(model.workflow.allNodes).length).toBeGreaterThan(0);
        });

        it('should have correct node structure in semantic model', async () => {
            const xml = `<config>
    <def var="title">
        <http url="https://example.com"/>
    </def>
    <var name="result">\${title}</var>
</config>`;

            const nodes = parser.parse(xml);
            designer.loadNodes(nodes);

            // Wait for async operations
            await new Promise(resolve => setTimeout(resolve, 100));

            const model = designer.exportModelForLLM();
            const allNodes = model.workflow.allNodes;
            
            // Check that we have nodes in the model
            const nodeIds = Object.keys(allNodes);
            expect(nodeIds.length).toBeGreaterThan(0);
            
            // Check that nodes have correct structure
            const firstNodeId = nodeIds[0];
            const firstNode = allNodes[firstNodeId];
            expect(firstNode).toBeDefined();
            expect(firstNode.nodeId).toBeDefined();
            expect(firstNode.pluginType).toBeDefined();
            expect(firstNode.position).toBeDefined();
            expect(firstNode.dimensions).toBeDefined();
        });

        it('should parse empty config without errors', () => {
            const xml = '<config></config>';
            const nodes = parser.parse(xml);
            
            expect(nodes).toHaveLength(0);
            
            designer.loadNodes(nodes);
            expect(designer.getNodes()).toHaveLength(0);
        });

        it('should parse complex nested structure', () => {
            const xml = `<config>
    <loop list="items">
        <def var="item">
            <http url="https://example.com/\${item}"/>
        </def>
        <var name="result">\${item}</var>
    </loop>
</config>`;

            const nodes = parser.parse(xml);
            
            expect(nodes).toHaveLength(1);
            expect(nodes[0].type).toBe('loop');
            expect(nodes[0].children).toHaveLength(2);
            expect(nodes[0].children[0].type).toBe('def');
            expect(nodes[0].children[1].type).toBe('var');
        });
    });

    describe('XMLParser edge cases', () => {
        it('should parse def element with var attribute', () => {
            const xml = '<config><def var="test"><http url="https://example.com"/></def></config>';
            const nodes = parser.parse(xml);
            
            expect(nodes).toHaveLength(1);
            expect(nodes[0].type).toBe('def');
            expect(nodes[0].attributes.var).toBe('test');
        });

        it('should parse var element with name attribute', () => {
            const xml = '<config><var name="test">value</var></config>';
            const nodes = parser.parse(xml);
            
            expect(nodes).toHaveLength(1);
            expect(nodes[0].type).toBe('var');
            expect(nodes[0].attributes.name).toBe('test');
            expect(nodes[0].body).toBe('value');
        });

        it('should handle whitespace in XML', () => {
            const xml = `<config>
    
    <def var="title">
        <http url="https://example.com"/>
    </def>
    
    <var name="result">\${title}</var>
    
</config>`;

            const nodes = parser.parse(xml);
            expect(nodes).toHaveLength(2);
        });
    });
});

