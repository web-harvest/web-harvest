/**
 * XMLParser - Implementation (TDD Green Phase)
 * Converts XML string to VisualNode array with automatic positioning using VisualPluginMetadata
 */

import type { PluginLookup } from '@webharvest/metadata-client';
import type { VisualNode } from './visualNode.js';
import { resolveLegacyMapping, type LegacyNotice } from './legacyMappings.js';

export class XMLParser {
    private pluginManager: PluginLookup | null = null;
    private readonly DEFAULT_X = 50;
    private readonly DEFAULT_Y = 50;
    private readonly DEFAULT_SPACING = 150;
    private readonly NESTED_SPACING = 30;
    private legacyNotices: LegacyNotice[] = [];

    constructor(pluginManager?: PluginLookup | null) {
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

        this.legacyNotices = [];

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
        console.log(`[XMLParser] Root element: ${root.tagName}, children count: ${root.children.length}`);
        for (let i = 0; i < root.children.length; i++) {
            const child = root.children[i];
            console.log(`[XMLParser] Parsing child ${i}: tagName=${child.tagName}, attributes=${Array.from(child.attributes).map(a => a.name + '=' + a.value).join(', ')}`);
            const node = this.parseElement(child);
            if (node) {
                console.log(`[XMLParser] ✅ Parsed node: id=${node.id}, type=${node.type}`);
                nodes.push(node);
            } else {
                console.warn(`[XMLParser] ⚠️ parseElement returned null for ${child.tagName}`);
            }
        }
        console.log(`[XMLParser] Total nodes parsed: ${nodes.length}`);

        // NIE ustawiaj pozycji automatycznie - pozwól auto-layoutowi w VisualDesigner to zrobić
        // Auto-layout w VisualDesigner.loadNodes() ustawi pozycje po renderowaniu
        // this.autoPositionNodes(nodes);

        return nodes;
    }

    /**
     * Parse single XML element to VisualNode
     */
    private parseElement(element: Element): VisualNode | null {
        if (!element.tagName) {
            console.warn(`[XMLParser] parseElement: element has no tagName`);
            return null;
        }
        let tagName = element.tagName.toLowerCase();
        console.log(`[XMLParser] parseElement: tagName=${tagName}`);
        // Extract attributes
        const attributes: Record<string, string> = {};
        let dimensions: { width: number; height: number } | undefined = undefined;
        
        for (let i = 0; i < element.attributes.length; i++) {
            const attr = element.attributes[i];
            
            // Handle dimension attributes (data-width, data-height)
            if (attr.name === 'data-width' || attr.name === 'data-height') {
                if (!dimensions) {
                    dimensions = { width: 0, height: 0 };
                }
                if (attr.name === 'data-width') {
                    dimensions.width = parseInt(attr.value, 10) || 0;
                } else if (attr.name === 'data-height') {
                    dimensions.height = parseInt(attr.value, 10) || 0;
                }
            } else {
                // Regular attributes
                attributes[attr.name] = attr.value;
            }
        }

        const legacyRule = resolveLegacyMapping(tagName);
        if (legacyRule) {
            legacyRule.transform?.({ attributes, element });
            this.legacyNotices.push({
                legacyTag: tagName,
                replacement: legacyRule.replacement,
                message: legacyRule.note
            });
            tagName = legacyRule.replacement;
        }

        // Preserve direct text and CDATA nodes in their original order.
        // Trimming here corrupts meaningful whitespace in text/template nodes,
        // while preferring CDATA alone drops adjacent text nodes.
        const bodyNodes = Array.from(element.childNodes).filter(
            node => node.nodeType === Node.TEXT_NODE ||
                node.nodeType === Node.CDATA_SECTION_NODE
        );
        let body: string | null = null;

        if (bodyNodes.length > 0) {
            const text = bodyNodes.map(node => node.nodeValue || '').join('');
            const indentationOnly = element.children.length > 0 && text.trim().length === 0;
            body = indentationOnly || text.length === 0 ? null : text;
        }

        // Empty elements with no text nodes remain null-bodied.
        if (element.children.length === 0 && body === null) {
            const allText = element.textContent || '';
            body = allText.length > 0 ? allText : null;
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
            type: tagName,
            attributes,
            body,
            children,
            dimensions: dimensions // Include custom dimensions if present
        };

        // NIE ustawiaj pozycji dzieci automatycznie - pozwól auto-layoutowi w VisualDesigner to zrobić
        // Auto-layout w VisualDesigner.loadNodes() ustawi pozycje po renderowaniu
        // if (children.length > 0) {
        //     this.autoPositionNodes(children);
        // }

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

            // NIE ustawiaj pozycji dzieci automatycznie - pozwól auto-layoutowi w VisualDesigner to zrobić
            // Auto-layout w VisualDesigner.loadNodes() ustawi pozycje po renderowaniu
            // if (node.children && node.children.length > 0) {
            //     const childStartX = x + this.NESTED_SPACING;
            //     const childStartY = currentY + dimensions.height + 20;
            //     this.autoPositionNodes(node.children, childStartX, childStartY);
            // }

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

    getLegacyNotices(): LegacyNotice[] {
        return [...this.legacyNotices];
    }
}





