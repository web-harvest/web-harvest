/**
 * XMLGenerator - Implementation (TDD Green Phase)
 * Converts VisualNode array to XML string
 */

import type { VisualNode } from './visualNode.js';

export interface XMLGeneratorOptions {
    indentLevel?: number;
    includeLayoutMetadata?: boolean;
    metadataComment?: string;
}

interface ResolvedOptions {
    indentLevel: number;
    includeLayoutMetadata: boolean;
    metadataComment?: string;
}

export class XMLGenerator {
    /**
     * Generate XML from VisualNode array
     */
    generate(
        nodes: VisualNode[],
        optionsOrIndent?: number | XMLGeneratorOptions
    ): string {
        if (nodes.length === 0) {
            return '';
        }

        const options = this.resolveOptions(optionsOrIndent);
        return this.generateInternal(nodes, options.indentLevel, options, true);
    }

    private generateInternal(
        nodes: VisualNode[],
        indentLevel: number,
        options: ResolvedOptions,
        includeComment: boolean
    ): string {
        if (nodes.length === 0) {
            return '';
        }

        const lines: string[] = [];

        if (includeComment && options.metadataComment) {
            const indent = '    '.repeat(indentLevel);
            lines.push(`${indent}<!-- ${options.metadataComment} -->`);
        }

        nodes.forEach(node => {
            lines.push(this.generateNode(node, indentLevel, options));
        });

        return lines.join('\n');
    }

    /**
     * Generate XML for single node
     */
    private generateNode(
        node: VisualNode,
        indentLevel: number,
        options: ResolvedOptions
    ): string {
        const indent = '    '.repeat(indentLevel);
        const attributes = this.generateAttributes(node.attributes);
        const dimensionsAttrs = this.generateDimensionsAttributes(node, options);
        const tag = node.type;

        // Self-closing tag (no body, no children)
        if (node.body === null && node.children.length === 0) {
            return `${indent}<${tag}${attributes}${dimensionsAttrs}/>`;
        }

        // Opening tag
        let xml = `${indent}<${tag}${attributes}${dimensionsAttrs}>`;

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
            xml += this.generateInternal(node.children, indentLevel + 1, options, false);
            xml += '\n' + indent;
        }

        // Closing tag
        xml += `</${tag}>`;

        return xml;
    }

    /**
     * Generate attributes string (sorted alphabetically, skipping empty values)
     */
    private generateAttributes(attributes: Record<string, string>): string {
        const keys = Object.keys(attributes)
            .filter(key => this.isAttributeValueValid(attributes[key]))
            .sort((a, b) => a.localeCompare(b));

        if (keys.length === 0) {
            return '';
        }

        return ' ' + keys
            .map(key => `${key}="${this.escapeAttribute(attributes[key])}"`)
            .join(' ');
    }

    /**
     * Generate dimensions attributes (if custom dimensions are set)
     */
    private generateDimensionsAttributes(
        node: VisualNode,
        options: ResolvedOptions
    ): string {
        if (!options.includeLayoutMetadata || !node.dimensions) {
            return '';
        }

        const attrs: string[] = [];
        if (node.dimensions.width) {
            attrs.push(`data-width="${node.dimensions.width}"`);
        }
        if (node.dimensions.height) {
            attrs.push(`data-height="${node.dimensions.height}"`);
        }
        return attrs.length > 0 ? ' ' + attrs.join(' ') : '';
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

    private resolveOptions(
        optionsOrIndent?: number | XMLGeneratorOptions
    ): ResolvedOptions {
        if (typeof optionsOrIndent === 'number') {
            return {
                indentLevel: optionsOrIndent,
                includeLayoutMetadata: false
            };
        }

        const options = optionsOrIndent || {};
        return {
            indentLevel: options.indentLevel ?? 0,
            includeLayoutMetadata: options.includeLayoutMetadata ?? false,
            metadataComment: options.metadataComment
        };
    }

    private isAttributeValueValid(value: string | undefined): value is string {
        if (value === undefined || value === null) {
            return false;
        }
        return value.trim().length > 0;
    }
}






