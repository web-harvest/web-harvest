/**
 * XMLGenerator - Implementation (TDD Green Phase)
 * Converts VisualNode array to XML string
 */

import type { VisualNode } from '../types';

export class XMLGenerator {
    /**
     * Generate XML from VisualNode array
     */
    generate(nodes: VisualNode[], indentLevel: number = 0): string {
        if (nodes.length === 0) {
            return '';
        }

        return nodes.map(node => this.generateNode(node, indentLevel)).join('\n');
    }

    /**
     * Generate XML for single node
     */
    private generateNode(node: VisualNode, indentLevel: number): string {
        const indent = '    '.repeat(indentLevel);
        const attributes = this.generateAttributes(node.attributes);
        const tag = node.type;

        // Self-closing tag (no body, no children)
        if (node.body === null && node.children.length === 0) {
            return `${indent}<${tag}${attributes}/>`;
        }

        // Opening tag
        let xml = `${indent}<${tag}${attributes}>`;

        // Body content
        if (node.body !== null && node.body !== '') {
            if (this.needsCDATA(node.body)) {
                xml += `<![CDATA[${node.body}]]>`;
            } else {
                xml += this.escapeXML(node.body);
            }
        }

        // Children
        if (node.children.length > 0) {
            xml += '\n';
            xml += this.generate(node.children, indentLevel + 1);
            xml += '\n' + indent;
        } else if (node.body === null || node.body === '') {
            // Empty element with no children
        }

        // Closing tag
        xml += `</${tag}>`;

        return xml;
    }

    /**
     * Generate attributes string
     */
    private generateAttributes(attributes: Record<string, string>): string {
        const keys = Object.keys(attributes);

        if (keys.length === 0) {
            return '';
        }

        return ' ' + keys
            .map(key => `${key}="${this.escapeAttribute(attributes[key])}"`)
            .join(' ');
    }

    /**
     * Escape XML special characters in attributes
     */
    private escapeAttribute(value: string): string {
        return value
            .replace(/&/g, '&amp;')
            .replace(/</g, '&lt;')
            .replace(/>/g, '&gt;')
            .replace(/"/g, '&quot;')
            .replace(/'/g, '&apos;');
    }

    /**
     * Escape XML special characters in text content
     */
    private escapeXML(value: string): string {
        return value
            .replace(/&/g, '&amp;')
            .replace(/</g, '&lt;')
            .replace(/>/g, '&gt;');
    }

    /**
     * Check if content needs CDATA wrapping
     */
    private needsCDATA(content: string): boolean {
        // Wrap in CDATA if contains HTML tags or special characters
        return /<[^>]+>/.test(content) ||
            content.includes('&') ||
            content.includes('<');
    }
}


