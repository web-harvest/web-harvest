/**
 * XML Parser - Converts XML Configuration to Visual Nodes
 * 
 * CRITICAL: Must handle all WebHarvest XML patterns from docs/web/examples/
 * - Deep nesting (def > html-to-xml > http)
 * - CDATA sections (<template><![CDATA[...]]></template>)
 * - Special sub-elements (xq-param, xq-expression, body, return)
 * - Order preservation
 * - All attribute types
 */

import type { VisualNode } from '../types/VisualNode';
import type { PluginMetadata } from './PluginRegistry';

export interface ParseResult {
  nodes: VisualNode[];
  errors: string[];
}

export class XMLParser {
  private pluginRegistry: Map<string, PluginMetadata>;
  private nodeCounter: number = 0;
  private errors: string[] = [];
  
  constructor(pluginRegistry: Map<string, PluginMetadata>) {
    this.pluginRegistry = pluginRegistry;
  }
  
  /**
   * Parse XML string to Visual Nodes
   */
  public parse(xmlString: string): ParseResult {
    this.nodeCounter = 0;
    this.errors = [];
    
    try {
      const parser = new DOMParser();
      const doc = parser.parseFromString(xmlString, 'text/xml');
      
      // Check for parser errors
      const parserError = doc.querySelector('parsererror');
      if (parserError) {
        this.errors.push(`XML Parse Error: ${parserError.textContent}`);
        return { nodes: [], errors: this.errors };
      }
      
      const root = doc.documentElement;
      
      // Handle <config> root
      if (root.tagName.toLowerCase() !== 'config') {
        this.errors.push(`Root element must be <config>, found: <${root.tagName}>`);
        return { nodes: [], errors: this.errors };
      }
      
      const nodes: VisualNode[] = [];
      let yPosition = 50;
      
      // Process all root-level elements
      Array.from(root.children).forEach((element) => {
        const nodeTree = this.processElement(element, null, 50, yPosition);
        if (nodeTree) {
          nodes.push(...nodeTree);
          yPosition += 150; // Space between root nodes
        }
      });
      
      console.log(`✅ Parsed XML: ${nodes.length} nodes, ${this.errors.length} errors`);
      return { nodes, errors: this.errors };
      
    } catch (error) {
      this.errors.push(`Parse exception: ${error}`);
      return { nodes: [], errors: this.errors };
    }
  }
  
  /**
   * Process XML element recursively
   * Returns flat list of nodes (parent + all descendants)
   */
  private processElement(
    element: Element,
    parentId: string | null,
    x: number,
    y: number
  ): VisualNode[] | null {
    const tagName = element.tagName.toLowerCase();
    
    // Get plugin metadata
    const plugin = this.pluginRegistry.get(tagName);
    if (!plugin) {
      this.errors.push(`Unknown plugin: ${tagName}`);
      return null;
    }
    
    // Create node
    const nodeId = `node_${++this.nodeCounter}`;
    const node: VisualNode = {
      id: nodeId,
      type: tagName,
      plugin: plugin,
      config: this.extractAttributes(element, plugin),
      body: this.extractBody(element, plugin),
      parentId: parentId,
      children: [],
      x: x,
      y: y,
      executionState: 'pending'
    };
    
    const allNodes: VisualNode[] = [node];
    
    // Process children
    let childY = y + 80;
    const childX = x + (parentId ? 0 : 40); // Indent children
    
    Array.from(element.children).forEach((childElement) => {
      const childNodes = this.processElement(childElement, nodeId, childX, childY);
      if (childNodes && childNodes.length > 0) {
        const childNode = childNodes[0]; // First node is the direct child
        node.children.push(childNode.id);
        allNodes.push(...childNodes);
        childY += 80;
      }
    });
    
    return allNodes;
  }
  
  /**
   * Extract attributes from XML element
   */
  private extractAttributes(element: Element, plugin: PluginMetadata): Record<string, any> {
    const config: Record<string, any> = {};
    
    // Get all attributes
    for (let i = 0; i < element.attributes.length; i++) {
      const attr = element.attributes[i];
      const attrName = attr.name;
      
      // Skip xmlns attributes
      if (attrName.startsWith('xmlns')) {
        continue;
      }
      
      // Get attribute definition from schema
      const attrDef = plugin.schema.attributes.find(a => a.name === attrName);
      
      if (attrDef) {
        // Type conversion based on schema
        config[attrName] = this.convertAttributeValue(attr.value, attrDef.type);
      } else {
        // Unknown attribute - store as string
        config[attrName] = attr.value;
        this.errors.push(`Unknown attribute: ${attrName} on ${plugin.elementName}`);
      }
    }
    
    return config;
  }
  
  /**
   * Convert attribute value based on type
   */
  private convertAttributeValue(value: string, type: string): any {
    switch (type.toLowerCase()) {
      case 'boolean':
        return value.toLowerCase() === 'true';
      case 'number':
      case 'integer':
        return parseInt(value, 10);
      case 'float':
      case 'double':
        return parseFloat(value);
      default:
        return value;
    }
  }
  
  /**
   * Extract body content (text content excluding child elements)
   */
  private extractBody(element: Element, plugin: PluginMetadata): string | null {
    // Check if plugin allows body
    if (!plugin.schema.allowsBody) {
      return null;
    }
    
    // Get text nodes only (not child elements)
    let bodyText = '';
    
    element.childNodes.forEach(node => {
      if (node.nodeType === Node.TEXT_NODE || node.nodeType === Node.CDATA_SECTION_NODE) {
        bodyText += node.textContent || '';
      }
    });
    
    const trimmed = bodyText.trim();
    return trimmed.length > 0 ? trimmed : null;
  }
}











