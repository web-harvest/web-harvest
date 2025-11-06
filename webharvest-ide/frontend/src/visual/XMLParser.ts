/**
 * XMLParser - Implementation (TDD Green Phase)
 * Converts XML string to VisualNode array with automatic positioning using VisualPluginMetadata
 */

import type { VisualNode } from '../types';
import type { PluginManager } from '../core/PluginManager';

export class XMLParser {
    private pluginManager: PluginManager | null = null;
    private readonly DEFAULT_X = 50;
    private readonly DEFAULT_Y = 50;
    private readonly DEFAULT_SPACING = 150;
    private readonly NESTED_SPACING = 30;

    constructor(pluginManager?: PluginManager | null) {
        this.pluginManager = pluginManager || null;
    }

    /**
     * Generate unique ID for node
     */
    private generateId(): string {
        return `node-${Date.now()}-${Math.random().toString(36).substr(2, 9)}`;
    }

    /**
     * Parse XML string into VisualNode array with automatic positioning
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

        // Auto-position nodes using VisualPluginMetadata
        this.autoPositionNodes(nodes);

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

        const node: VisualNode = {
            id: this.generateId(),
            type: element.tagName.toLowerCase(),
            attributes,
            body,
            children,
        };

        // Auto-position children recursively
        if (children.length > 0) {
            this.autoPositionNodes(children);
        }

        return node;
    }

    /**
     * Auto-position nodes using VisualPluginMetadata
     * Uses defaultWidth/defaultHeight from metadata for intelligent layout
     */
    private autoPositionNodes(nodes: VisualNode[], startX: number = this.DEFAULT_X, startY: number = this.DEFAULT_Y): void {
        if (!this.pluginManager || nodes.length === 0) {
            // Fallback: simple vertical layout if no plugin manager
            nodes.forEach((node, index) => {
                if (!node.position) {
                    node.position = {
                        x: startX,
                        y: startY + index * this.DEFAULT_SPACING
                    };
                }
            });
            return;
        }

        let currentY = startY;
        const groupMap = new Map<string, number>(); // Track Y position per group

        nodes.forEach((node, index) => {
            if (node.position) {
                // Node already has position, skip auto-positioning
                return;
            }

            const plugin = this.pluginManager!.findPlugin(node.type);
            const group = plugin?.group || 'General';
            const dimensions = this.getNodeDimensions(node.type);

            // Use group-based positioning for better visual organization
            let x = startX;
            if (groupMap.has(group)) {
                // Same group - position horizontally
                x = groupMap.get(group)! + dimensions.width + 20;
            }

            node.position = {
                x,
                y: currentY
            };

            // Update group map
            groupMap.set(group, x);

            // Position children within parent node (nested)
            if (node.children && node.children.length > 0) {
                const childStartX = x + this.NESTED_SPACING;
                const childStartY = currentY + dimensions.height + 20;
                this.autoPositionNodes(node.children, childStartX, childStartY);
            }

            // Move to next row if node is large or if we've positioned many nodes horizontally
            const nextNodeY = currentY + dimensions.height + this.DEFAULT_SPACING;
            if (index < nodes.length - 1) {
                const nextNode = nodes[index + 1];
                const nextPlugin = this.pluginManager!.findPlugin(nextNode.type);
                const nextGroup = nextPlugin?.group || 'General';

                // Start new row if different group or if we've positioned too many horizontally
                if (nextGroup !== group || (index > 0 && index % 5 === 0)) {
                    currentY = nextNodeY;
                    groupMap.clear(); // Reset group map for new row
                } else {
                    currentY = Math.max(currentY, nextNodeY);
                }
            }
        });
    }

    /**
     * Get node dimensions from VisualPluginMetadata or use defaults
     */
    private getNodeDimensions(nodeType: string): { width: number; height: number } {
        if (this.pluginManager) {
            const plugin = this.pluginManager.findPlugin(nodeType);
            if (plugin) {
                return {
                    width: plugin.defaultWidth || 250,
                    height: plugin.defaultHeight || 100
                };
            }
        }
        return { width: 250, height: 100 };
    }
}





