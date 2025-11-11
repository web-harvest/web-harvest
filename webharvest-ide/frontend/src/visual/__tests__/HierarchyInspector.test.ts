import { describe, expect, it } from '@jest/globals';
import { XMLParser } from '../XMLParser';
import { analyzeContainer, analyzeHierarchy } from '../HierarchyInspector';

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
    });
});


