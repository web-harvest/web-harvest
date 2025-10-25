/**
 * XML Generator - Converts Visual Nodes to XML Configuration
 * 
 * CRITICAL: Must generate valid WebHarvest XML that can be executed
 * - Preserve node order (children array order = XML element order)
 * - Include all attributes
 * - Handle CDATA for body content
 * - Proper indentation
 * - Namespace declaration
 */

import type { VisualNode } from '../types/VisualNode';

export class XMLGenerator {
  private indentSize: number = 2;
  
  /**
   * Generate XML from visual nodes
   */
  public generate(nodes: VisualNode[]): string {
    // Create XML document
    const doc = document.implementation.createDocument('', '', null);
    
    // Create root <config> element
    const root = doc.createElement('config');
    root.setAttribute('xmlns', 'http://org.webharvest/schema/2.1/core');
    root.setAttribute('charset', 'UTF-8');
    doc.appendChild(root);
    
    // Build all root nodes (no parent)
    const rootNodes = nodes.filter(n => !n.parentId);
    
    // Sort by Y position to maintain visual order
    rootNodes.sort((a, b) => a.y - b.y);
    
    rootNodes.forEach(node => {
      const element = this.buildElement(doc, node, nodes);
      if (element) {
        root.appendChild(element);
      }
    });
    
    // Serialize to string
    const serializer = new XMLSerializer();
    let xml = serializer.serializeToString(doc);
    
    // Format with indentation
    xml = this.formatXML(xml);
    
    // Add XML declaration
    xml = '<?xml version="1.0" encoding="UTF-8"?>\n' + xml;
    
    console.log(`✅ Generated XML with ${rootNodes.length} root elements`);
    return xml;
  }
  
  /**
   * Build XML element from visual node
   */
  private buildElement(
    doc: Document,
    node: VisualNode,
    allNodes: VisualNode[]
  ): Element | null {
    // Create element
    const element = doc.createElement(node.type);
    
    // Add attributes (sorted alphabetically for consistency)
    const sortedKeys = Object.keys(node.config).sort();
    sortedKeys.forEach(key => {
      const value = node.config[key];
      if (value !== null && value !== undefined && value !== '') {
        element.setAttribute(key, String(value));
      }
    });
    
    // Add child elements (in order!)
    node.children.forEach(childId => {
      const childNode = allNodes.find(n => n.id === childId);
      if (childNode) {
        const childElement = this.buildElement(doc, childNode, allNodes);
        if (childElement) {
          element.appendChild(childElement);
        }
      }
    });
    
    // Add body content
    if (node.body && node.body.trim().length > 0) {
      // Use CDATA for body content to preserve formatting
      if (node.body.includes('<') || node.body.includes('>')) {
        const cdata = doc.createCDATASection(node.body);
        element.appendChild(cdata);
      } else {
        element.textContent = node.body;
      }
    }
    
    return element;
  }
  
  /**
   * Format XML with proper indentation
   */
  private formatXML(xml: string): string {
    let formatted = '';
    let indent = 0;
    
    // Remove existing whitespace between tags
    xml = xml.replace(/>\s+</g, '><');
    
    // Split by tags
    const parts = xml.split(/(<[^>]+>)/g).filter(p => p.trim().length > 0);
    
    parts.forEach(part => {
      if (part.match(/^<\/\w/)) {
        // Closing tag
        indent = Math.max(0, indent - this.indentSize);
        formatted += ' '.repeat(indent) + part + '\n';
      } else if (part.match(/^<\w[^>]*>$/)) {
        // Opening tag
        formatted += ' '.repeat(indent) + part + '\n';
        if (!part.match(/\/>$/)) {
          indent += this.indentSize;
        }
      } else if (part.match(/^<\?/)) {
        // Processing instruction
        formatted += part + '\n';
      } else if (part.match(/^<!/)) {
        // Comment or CDATA
        formatted += ' '.repeat(indent) + part + '\n';
      } else {
        // Text content
        const trimmed = part.trim();
        if (trimmed.length > 0) {
          formatted += ' '.repeat(indent) + trimmed + '\n';
        }
      }
    });
    
    return formatted.trim();
  }
  
  /**
   * Validate XML structure before generation
   */
  public validate(nodes: VisualNode[]): string[] {
    const errors: string[] = [];
    
    nodes.forEach(node => {
      // Check required attributes
      const requiredAttrs = node.plugin.schema.requiredAttributes || [];
      requiredAttrs.forEach(attr => {
        if (!node.config[attr] || node.config[attr] === '') {
          errors.push(`${node.type} (${node.id}): Missing required attribute '${attr}'`);
        }
      });
      
      // Check allowed children
      if (node.children.length > 0) {
        const allowedChildren = node.plugin.schema.allowedSubElements || [];
        
        node.children.forEach(childId => {
          const child = nodes.find(n => n.id === childId);
          if (child) {
            if (allowedChildren.length > 0 && !allowedChildren.includes(child.type)) {
              errors.push(`${node.type} (${node.id}): Child '${child.type}' not allowed. Allowed: ${allowedChildren.join(', ')}`);
            }
          }
        });
      }
      
      // Check required children
      const requiredChildren = node.plugin.schema.requiredSubElements || [];
      if (requiredChildren.length > 0) {
        const childTypes = node.children
          .map(id => nodes.find(n => n.id === id)?.type)
          .filter(t => t !== undefined);
        
        requiredChildren.forEach(required => {
          if (!childTypes.includes(required)) {
            errors.push(`${node.type} (${node.id}): Missing required child element '${required}'`);
          }
        });
      }
    });
    
    return errors;
  }
}

