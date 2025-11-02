/**
 * XMLParser - TDD Tests
 * Test FIRST, then implement!
 */

import { describe, it, expect, beforeEach } from '@jest/globals';
import { XMLParser } from '../XMLParser';
import type { VisualNode } from '../types';

describe('XMLParser', () => {
    let parser: XMLParser;

    beforeEach(() => {
        parser = new XMLParser();
    });

    describe('parse simple elements', () => {
        it('should parse single element with attributes', () => {
            const xml = '<config><var name="test">value</var></config>';
            const nodes = parser.parse(xml);

            expect(nodes).toHaveLength(1);
            expect(nodes[0].type).toBe('var');
            expect(nodes[0].attributes.name).toBe('test');
            expect(nodes[0].body).toBe('value');
            expect(nodes[0].children).toEqual([]);
        });

        it('should parse self-closing element', () => {
            const xml = '<config><http url="http://example.com"/></config>';
            const nodes = parser.parse(xml);

            expect(nodes).toHaveLength(1);
            expect(nodes[0].type).toBe('http');
            expect(nodes[0].attributes.url).toBe('http://example.com');
            expect(nodes[0].body).toBeNull();
        });

        it('should parse multiple attributes', () => {
            const xml = '<config><http url="http://example.com" method="POST" timeout="5000"/></config>';
            const nodes = parser.parse(xml);

            expect(nodes[0].attributes.url).toBe('http://example.com');
            expect(nodes[0].attributes.method).toBe('POST');
            expect(nodes[0].attributes.timeout).toBe('5000');
        });

        it('should parse element with empty body', () => {
            const xml = '<config><var name="empty"></var></config>';
            const nodes = parser.parse(xml);

            expect(nodes[0].body).toBeNull();
        });
    });

    describe('parse nested elements', () => {
        it('should parse single level nesting', () => {
            const xml = '<config><loop><var name="x">1</var></loop></config>';
            const nodes = parser.parse(xml);

            expect(nodes).toHaveLength(1);
            expect(nodes[0].type).toBe('loop');
            expect(nodes[0].children).toHaveLength(1);
            expect(nodes[0].children[0].type).toBe('var');
        });

        it('should parse multiple children', () => {
            const xml = `<config>
        <loop>
          <var name="a">1</var>
          <var name="b">2</var>
          <var name="c">3</var>
        </loop>
      </config>`;
            const nodes = parser.parse(xml);

            expect(nodes[0].children).toHaveLength(3);
        });

        it('should parse deep nesting', () => {
            const xml = `<config>
        <loop>
          <if>
            <var name="test">value</var>
          </if>
        </loop>
      </config>`;
            const nodes = parser.parse(xml);

            expect(nodes[0].type).toBe('loop');
            expect(nodes[0].children[0].type).toBe('if');
            expect(nodes[0].children[0].children[0].type).toBe('var');
        });
    });

    describe('parse CDATA', () => {
        it('should parse CDATA content', () => {
            const xml = '<config><var><![CDATA[<html>test</html>]]></var></config>';
            const nodes = parser.parse(xml);

            expect(nodes[0].body).toBe('<html>test</html>');
        });

        it('should preserve special characters in CDATA', () => {
            const xml = '<config><var><![CDATA[test & <test>]]></var></config>';
            const nodes = parser.parse(xml);

            expect(nodes[0].body).toBe('test & <test>');
        });
    });

    describe('parse multiple root elements', () => {
        it('should parse multiple sibling elements', () => {
            const xml = `<config>
        <var name="a">1</var>
        <var name="b">2</var>
      </config>`;
            const nodes = parser.parse(xml);

            expect(nodes).toHaveLength(2);
        });
    });

    describe('handle invalid XML', () => {
        it('should throw error for malformed XML', () => {
            const xml = '<config><var name="test"></config>';

            expect(() => parser.parse(xml)).toThrow();
        });

        it('should throw error for empty string', () => {
            expect(() => parser.parse('')).toThrow();
        });
    });

    describe('assign IDs', () => {
        it('should assign unique IDs to all nodes', () => {
            const xml = `<config>
        <var name="a">1</var>
        <loop>
          <var name="b">2</var>
        </loop>
      </config>`;
            const nodes = parser.parse(xml);

            const ids = new Set<string>();
            const collectIds = (nodes: VisualNode[]) => {
                nodes.forEach(node => {
                    ids.add(node.id);
                    if (node.children.length > 0) {
                        collectIds(node.children);
                    }
                });
            };
            collectIds(nodes);

            expect(ids.size).toBe(3); // 3 unique IDs
        });
    });
});

