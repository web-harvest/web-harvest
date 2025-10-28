/**
 * XMLGenerator - TDD Tests
 * Test FIRST, then implement!
 */

import { describe, it, expect, beforeEach } from '@jest/globals';
import { XMLGenerator } from '../XMLGenerator';
import type { VisualNode } from '../types';

describe('XMLGenerator', () => {
    let generator: XMLGenerator;

    beforeEach(() => {
        generator = new XMLGenerator();
    });

    describe('generate simple elements', () => {
        it('should generate element with attributes and body', () => {
            const nodes: VisualNode[] = [{
                id: '1',
                type: 'var',
                attributes: { name: 'test' },
                body: 'value',
                children: [],
            }];

            const xml = generator.generate(nodes);

            expect(xml).toContain('<var name="test">value</var>');
        });

        it('should generate self-closing element', () => {
            const nodes: VisualNode[] = [{
                id: '1',
                type: 'http',
                attributes: { url: 'http://example.com' },
                body: null,
                children: [],
            }];

            const xml = generator.generate(nodes);

            expect(xml).toContain('<http url="http://example.com"/>');
        });

        it('should generate element with multiple attributes', () => {
            const nodes: VisualNode[] = [{
                id: '1',
                type: 'http',
                attributes: {
                    url: 'http://example.com',
                    method: 'POST',
                    timeout: '5000',
                },
                body: null,
                children: [],
            }];

            const xml = generator.generate(nodes);

            expect(xml).toContain('url="http://example.com"');
            expect(xml).toContain('method="POST"');
            expect(xml).toContain('timeout="5000"');
        });

        it('should generate element with empty body', () => {
            const nodes: VisualNode[] = [{
                id: '1',
                type: 'var',
                attributes: { name: 'empty' },
                body: '',
                children: [],
            }];

            const xml = generator.generate(nodes);

            expect(xml).toContain('<var name="empty"></var>');
        });
    });

    describe('generate nested elements', () => {
        it('should generate single level nesting', () => {
            const nodes: VisualNode[] = [{
                id: '1',
                type: 'loop',
                attributes: { maxloops: '10' },
                body: null,
                children: [{
                    id: '2',
                    type: 'var',
                    attributes: { name: 'x' },
                    body: '1',
                    children: [],
                }],
            }];

            const xml = generator.generate(nodes);

            expect(xml).toContain('<loop maxloops="10">');
            expect(xml).toContain('<var name="x">1</var>');
            expect(xml).toContain('</loop>');
        });

        it('should generate multiple children', () => {
            const nodes: VisualNode[] = [{
                id: '1',
                type: 'loop',
                attributes: {},
                body: null,
                children: [
                    {
                        id: '2',
                        type: 'var',
                        attributes: { name: 'a' },
                        body: '1',
                        children: [],
                    },
                    {
                        id: '3',
                        type: 'var',
                        attributes: { name: 'b' },
                        body: '2',
                        children: [],
                    },
                ],
            }];

            const xml = generator.generate(nodes);

            expect(xml).toContain('<var name="a">1</var>');
            expect(xml).toContain('<var name="b">2</var>');
        });

        it('should generate deep nesting', () => {
            const nodes: VisualNode[] = [{
                id: '1',
                type: 'loop',
                attributes: {},
                body: null,
                children: [{
                    id: '2',
                    type: 'if',
                    attributes: {},
                    body: null,
                    children: [{
                        id: '3',
                        type: 'var',
                        attributes: { name: 'test' },
                        body: 'value',
                        children: [],
                    }],
                }],
            }];

            const xml = generator.generate(nodes);

            expect(xml).toContain('<loop>');
            expect(xml).toContain('<if>');
            expect(xml).toContain('<var name="test">value</var>');
            expect(xml).toContain('</if>');
            expect(xml).toContain('</loop>');
        });
    });

    describe('handle special characters', () => {
        it('should escape quotes in attributes', () => {
            const nodes: VisualNode[] = [{
                id: '1',
                type: 'var',
                attributes: { name: 'test"quote' },
                body: 'value',
                children: [],
            }];

            const xml = generator.generate(nodes);

            expect(xml).toContain('&quot;');
        });

        it('should escape ampersands in attributes', () => {
            const nodes: VisualNode[] = [{
                id: '1',
                type: 'var',
                attributes: { name: 'test&amp' },
                body: 'value',
                children: [],
            }];

            const xml = generator.generate(nodes);

            expect(xml).toContain('&amp;');
        });

        it('should escape < and > in attributes', () => {
            const nodes: VisualNode[] = [{
                id: '1',
                type: 'var',
                attributes: { name: 'test<>value' },
                body: 'value',
                children: [],
            }];

            const xml = generator.generate(nodes);

            expect(xml).toContain('&lt;');
            expect(xml).toContain('&gt;');
        });
    });

    describe('handle CDATA', () => {
        it('should wrap HTML in CDATA', () => {
            const nodes: VisualNode[] = [{
                id: '1',
                type: 'var',
                attributes: { name: 'html' },
                body: '<div>test</div>',
                children: [],
            }];

            const xml = generator.generate(nodes);

            expect(xml).toContain('<![CDATA[<div>test</div>]]>');
        });

        it('should wrap special characters in CDATA', () => {
            const nodes: VisualNode[] = [{
                id: '1',
                type: 'var',
                attributes: { name: 'text' },
                body: 'test & <test>',
                children: [],
            }];

            const xml = generator.generate(nodes);

            expect(xml).toContain('<![CDATA[test & <test>]]>');
        });

        it('should not wrap simple text in CDATA', () => {
            const nodes: VisualNode[] = [{
                id: '1',
                type: 'var',
                attributes: { name: 'text' },
                body: 'simple text',
                children: [],
            }];

            const xml = generator.generate(nodes);

            expect(xml).not.toContain('CDATA');
            expect(xml).toContain('simple text');
        });
    });

    describe('formatting and indentation', () => {
        it('should indent nested elements', () => {
            const nodes: VisualNode[] = [{
                id: '1',
                type: 'loop',
                attributes: {},
                body: null,
                children: [{
                    id: '2',
                    type: 'var',
                    attributes: { name: 'x' },
                    body: '1',
                    children: [],
                }],
            }];

            const xml = generator.generate(nodes, 1);

            // Should have proper indentation
            const lines = xml.split('\n');
            expect(lines.length).toBeGreaterThan(2);
        });

        it('should handle custom indentation level', () => {
            const nodes: VisualNode[] = [{
                id: '1',
                type: 'var',
                attributes: { name: 'test' },
                body: 'value',
                children: [],
            }];

            const xml = generator.generate(nodes, 2);

            expect(xml).toMatch(/^\s{8}</); // 2 levels * 4 spaces
        });
    });

    describe('generate multiple root elements', () => {
        it('should generate multiple siblings', () => {
            const nodes: VisualNode[] = [
                {
                    id: '1',
                    type: 'var',
                    attributes: { name: 'a' },
                    body: '1',
                    children: [],
                },
                {
                    id: '2',
                    type: 'var',
                    attributes: { name: 'b' },
                    body: '2',
                    children: [],
                },
            ];

            const xml = generator.generate(nodes);

            expect(xml).toContain('<var name="a">1</var>');
            expect(xml).toContain('<var name="b">2</var>');
        });
    });

    describe('edge cases', () => {
        it('should handle empty nodes array', () => {
            const xml = generator.generate([]);

            expect(xml).toBe('');
        });

        it('should handle element with no attributes', () => {
            const nodes: VisualNode[] = [{
                id: '1',
                type: 'loop',
                attributes: {},
                body: null,
                children: [],
            }];

            const xml = generator.generate(nodes);

            expect(xml).toContain('<loop/>');
        });
    });
});


