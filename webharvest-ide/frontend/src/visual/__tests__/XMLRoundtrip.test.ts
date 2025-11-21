import { describe, it, expect } from '@jest/globals';
import { XMLParser } from '../XMLParser';
import { XMLGenerator } from '../XMLGenerator';
import type { VisualNode } from '../types';

function normalize(nodes: VisualNode[]): any {
    return nodes.map(node => ({
        type: node.type,
        attributes: node.attributes,
        body: node.body,
        dimensions: node.dimensions ? { ...node.dimensions } : undefined,
        children: normalize(node.children || [])
    }));
}

describe('XML round-trip', () => {
    it('preserves structure through parse → generate → parse', () => {
        const xml = `
            <config xmlns="http://org.webharvest/schema/2.1/core">
                <loop maxloops="3" data-width="320">
                    <http url="https://example.com" method="GET">
                        <http-header name="User-Agent" value="WebHarvest" />
                    </http>
                    <var name="title"><![CDATA[Example]]></var>
                </loop>
            </config>
        `;

        const parser = new XMLParser();
        const initialNodes = parser.parse(xml);

        const generator = new XMLGenerator();
        const regeneratedBody = generator.generate(initialNodes, {
            indentLevel: 1,
            includeLayoutMetadata: true
        });

        const secondPassNodes = parser.parse(`<config>${regeneratedBody}</config>`);

        expect(normalize(secondPassNodes)).toEqual(normalize(initialNodes));
    });

    it('drops layout metadata when flag disabled', () => {
        const xml = '<config><http url="https://example.com" data-width="400" data-height="200"/></config>';
        const parser = new XMLParser();
        const nodes = parser.parse(xml);
        const generator = new XMLGenerator();

        const body = generator.generate(nodes, { indentLevel: 1 });

        expect(body).not.toContain('data-width');
        expect(body).not.toContain('data-height');
    });
});


