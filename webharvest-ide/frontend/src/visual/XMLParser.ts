/**
 * XMLParser - Implementation (TDD Green Phase)
 * Converts XML string to VisualNode array
 */

import type { VisualNode } from '../types';

export class XMLParser {
    /**
     * Generate unique ID for node
     */
    private generateId(): string {
        return `node-${Date.now()}-${Math.random().toString(36).substr(2, 9)}`;
    }

    /**
     * Parse XML string into VisualNode array
     */
    parse(xml: string): VisualNode[] {
        if (!xml || xml.trim() === '') {
            throw new Error('XML string cannot be empty');
        }

        // Parse XML using DOM Parser
        const parser = new DOMParser();
        const doc = parser.parseFromString(xml, 'text/xml');

        // Check for parse errors
        const parseError = doc.querySelector('parsererror');
        if (parseError) {
            throw new Error(`XML Parse Error: ${parseError.textContent}`);
        }

        // Get root element (skip <config> wrapper)
        const root = doc.documentElement;
        if (!root) {
            throw new Error('No root element found');
        }

        // Parse children of root (config)
        const nodes: VisualNode[] = [];
        for (let i = 0; i < root.children.length; i++) {
            const child = root.children[i];
            const node = this.parseElement(child);
            if (node) {
                nodes.push(node);
            }
        }

        return nodes;
    }

    /**
     * Parse single XML element to VisualNode
     */
    private parseElement(element: Element): VisualNode | null {
        if (!element.tagName) {
            return null;
        }

        // Extract attributes
        const attributes: Record<string, string> = {};
        for (let i = 0; i < element.attributes.length; i++) {
            const attr = element.attributes[i];
            attributes[attr.name] = attr.value;
        }

        // Extract body (text content or CDATA)
        let body: string | null = null;

        // Check for CDATA
        const cdataNodes = Array.from(element.childNodes).filter(
            node => node.nodeType === Node.CDATA_SECTION_NODE
        );

        if (cdataNodes.length > 0) {
            // Use CDATA content
            body = cdataNodes.map(node => node.nodeValue || '').join('');
        } else {
            // Get text content (excluding child elements)
            const textNodes = Array.from(element.childNodes).filter(
                node => node.nodeType === Node.TEXT_NODE
            );

            if (textNodes.length > 0) {
                const text = textNodes.map(node => node.nodeValue || '').join('').trim();
                body = text || null;
            }

            // If no children elements, even empty text should be empty string
            if (element.children.length === 0 && body === null) {
                const allText = element.textContent?.trim() || '';
                body = allText.length > 0 ? allText : null;
            }
        }

        // Parse children
        const children: VisualNode[] = [];
        for (let i = 0; i < element.children.length; i++) {
            const child = element.children[i];
            const childNode = this.parseElement(child);
            if (childNode) {
                children.push(childNode);
            }
        }

        return {
            id: this.generateId(),
            type: element.tagName.toLowerCase(),
            attributes,
            body,
            children,
        };
    }
}





