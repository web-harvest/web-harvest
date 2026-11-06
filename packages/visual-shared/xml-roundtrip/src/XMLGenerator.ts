/**
 * XMLGenerator - Implementation (TDD Green Phase)
 * Converts VisualNode array to XML string
 */

import type { VisualContentPart, VisualNode } from './visualNode.js';

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
        options: ResolvedOptions,
        includeIndent: boolean = true
    ): string {
        const indent = includeIndent ? '    '.repeat(indentLevel) : '';
        const attributes = this.generateAttributes(node.attributes);
        const dimensionsAttrs = this.generateDimensionsAttributes(node, options);
        const tag = node.type;
        const contentSequence = this.getValidContentSequence(node);

        // Self-closing tag (no body, no children, and no preserved mixed content).
        if (!contentSequence && node.body === null && node.children.length === 0) {
            return indent + '<' + tag + attributes + dimensionsAttrs + '/>';
        }

        let xml = indent + '<' + tag + attributes + dimensionsAttrs + '>';

        if (contentSequence) {
            const childrenById = new Map(node.children.map(child => [child.id, child]));
            contentSequence.forEach(part => {
                if (part.kind === 'text') {
                    if (part.value.length === 0) {
                        return;
                    }
                    xml += this.needsCDATA(part.value)
                        ? '<![CDATA[' + part.value + ']]>'
                        : this.escapeXML(part.value);
                    return;
                }

                const child = childrenById.get(part.nodeId);
                if (child) {
                    // Indentation belongs to the surrounding text segments in
                    // mixed content; do not inject extra whitespace here.
                    xml += this.generateNode(child, indentLevel + 1, options, false);
                }
            });
        } else {
            // Legacy body + children representation for ordinary nodes.
            if (node.body !== null && node.body !== '') {
                xml += this.needsCDATA(node.body)
                    ? '<![CDATA[' + node.body + ']]>'
                    : this.escapeXML(node.body);
            }

            if (node.children.length > 0) {
                xml += '\n';
                xml += this.generateInternal(node.children, indentLevel + 1, options, false);
                xml += '\n' + indent;
            }
        }

        return xml + '</' + tag + '>';
    }

    private getValidContentSequence(node: VisualNode): VisualContentPart[] | null {
        const sequence = node.contentSequence;
        if (!sequence || sequence.length === 0) {
            return null;
        }

        const sequenceText = sequence
            .filter((part): part is Extract<VisualContentPart, { kind: 'text' }> => part.kind === 'text')
            .map(part => part.value)
            .join('');
        if (sequenceText !== (node.body ?? '')) {
            return null;
        }

        const sequenceChildIds = sequence
            .filter((part): part is Extract<VisualContentPart, { kind: 'child' }> => part.kind === 'child')
            .map(part => part.nodeId);
        if (
            sequenceChildIds.length !== node.children.length ||
            sequenceChildIds.some((id, index) => id !== node.children[index]?.id)
        ) {
            return null;
        }

        return sequence;
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






