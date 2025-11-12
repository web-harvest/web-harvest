import { describe, expect, it } from '@jest/globals';
import { XMLParser } from '../XMLParser';
import { analyzeContainer, analyzeHierarchy } from '../HierarchyInspector';
import { MasterDetailRegistry } from '../detail/MasterDetailRegistry';
import type { Plugin } from '../../types';
import type { PluginManager } from '../../core/PluginManager';

describe('HierarchyInspector', () => {
    const parser = new XMLParser();

    const sampleXml = `<?xml version="1.0" encoding="UTF-8"?>
<config xmlns="http://org.webharvest/schema/2.1/core">
    <function name="download-multipage-list">
        <return>
            <while condition="\${pageUrl.toString().length() != 0}" maxloops="\${maxloops}" index="i">
                <def var="content">
                    <html-to-xml>
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

    it('computes overall hierarchy stats for workflow', () => {
        const nodes = parser.parse(sampleXml);
        const stats = analyzeHierarchy(nodes);

        expect(stats.totalNodes).toBeGreaterThan(0);
        expect(stats.containerCount).toBeGreaterThan(0);
        expect(stats.leafCount).toBeGreaterThan(0);
    });

    it('summarises container details for while node', () => {
        const nodes = parser.parse(sampleXml);
        const functionNode = nodes[0];
        const returnNode = functionNode.children?.[0];
        const whileNode = returnNode?.children?.[0];
        expect(whileNode?.type).toBe('while');

        const summary = analyzeContainer(whileNode!);

        expect(summary.childCount).toBe(4);
        expect(summary.containerCount).toBeGreaterThanOrEqual(0);
        expect(summary.typeFrequency[0].type).toBeDefined();
        expect(summary.attributeHighlights.length).toBeGreaterThan(0);
        expect(summary.detailBreakdown.length).toBe(0);
    });

    it('reports detail breakdown for HTTP containers when registry is provided', () => {
        const nodes = parser.parse(sampleXml);
        const functionNode = nodes[0];
        const returnNode = functionNode.children?.[0];
        const whileNode = returnNode?.children?.[0];
        const defNode = whileNode?.children?.[0];
        const htmlNode = defNode?.children?.[0];
        const httpNode = htmlNode?.children?.[0];
        expect(httpNode?.type).toBe('http');

        const httpPlugin: Plugin = {
            id: 'http',
            name: 'HTTP',
            description: '',
            category: 'Network',
            icon: 'fa-globe',
            schema: {
                attributes: [],
                canHaveBody: false,
                canHaveChildren: true,
                canAppearAtRoot: true,
                requiredParents: [],
                isContextSpecific: false,
                allowedSubElements: ['http-header', 'http-param']
            },
            semantics: {
                roles: ['CONTEXT_CONTAINER'],
                canAppearAtRoot: true,
                allowsChildren: true,
                acceptsAnyChild: false,
                allowBody: false,
                allowTextContent: false,
                allowedChildTypes: ['http-header', 'http-param'],
                requiredParentTypes: []
            },
            documentation: { summary: '', examples: [] },
            dependencies: [],
            keyboardShortcuts: [],
            linkedServices: [],
            allowedChildPlugins: ['http-header', 'http-param']
        };

        const httpHeaderPlugin: Plugin = {
            id: 'http-header',
            name: 'HTTP Header',
            description: '',
            category: 'Network',
            icon: 'fa-tag',
            schema: {
                attributes: [
                    { name: 'name', type: 'string', required: true },
                    { name: 'value', type: 'string', required: true }
                ],
                canHaveBody: false,
                canHaveChildren: false,
                canAppearAtRoot: false,
                requiredParents: ['http'],
                isContextSpecific: true,
                allowedSubElements: []
            },
            semantics: {
                roles: ['DEPENDENT'],
                canAppearAtRoot: false,
                allowsChildren: false,
                acceptsAnyChild: false,
                allowBody: false,
                allowTextContent: false,
                allowedChildTypes: [],
                requiredParentTypes: ['http']
            },
            documentation: { summary: '', examples: [] },
            dependencies: [],
            keyboardShortcuts: [],
            linkedServices: []
        };

        const httpParamPlugin: Plugin = {
            id: 'http-param',
            name: 'HTTP Param',
            description: '',
            category: 'Network',
            icon: 'fa-tag',
            schema: {
                attributes: [
                    { name: 'name', type: 'string', required: true },
                    { name: 'value', type: 'string', required: true }
                ],
                canHaveBody: false,
                canHaveChildren: false,
                canAppearAtRoot: false,
                requiredParents: ['http'],
                isContextSpecific: true,
                allowedSubElements: []
            },
            semantics: {
                roles: ['DEPENDENT'],
                canAppearAtRoot: false,
                allowsChildren: false,
                acceptsAnyChild: false,
                allowBody: false,
                allowTextContent: false,
                allowedChildTypes: [],
                requiredParentTypes: ['http']
            },
            documentation: { summary: '', examples: [] },
            dependencies: [],
            keyboardShortcuts: [],
            linkedServices: []
        };

        const pluginMap: Record<string, Plugin> = {
            http: httpPlugin,
            'http-header': httpHeaderPlugin,
            'http-param': httpParamPlugin
        };

        const manager = {
            findPlugin: (id: string) => pluginMap[id] ?? null
        } as unknown as PluginManager;

        const registry = new MasterDetailRegistry(manager);
        const summary = analyzeContainer(httpNode!, registry);

        expect(summary.detailCount).toBe(3);
        expect(summary.detailBreakdown.length).toBeGreaterThan(0);
        const headers = summary.detailBreakdown.find(entry => entry.title.toLowerCase().includes('header'));
        expect(headers?.count).toBeGreaterThan(0);
        expect(headers?.preview.length).toBeGreaterThan(0);
    });
});


