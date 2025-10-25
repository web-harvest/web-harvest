/**
 * Container Manager
 * 
 * Manages container nodes and parent-child relationships
 * Critical for hierarchical plugin support (loop, if, http with headers, etc.)
 */

import type { PluginMetadata } from './PluginRegistry';
import type { VisualNode } from '../types/VisualNode';

export class ContainerManager {
  private nodes: Map<string, VisualNode> = new Map();
  
  /**
   * Check if plugin can contain children
   */
  public isContainer(plugin: PluginMetadata): boolean {
    const { schema } = plugin;
    return schema.allowedSubElements && schema.allowedSubElements.length > 0;
  }
  
  /**
   * Get allowed children for a container plugin
   */
  public getAllowedChildren(plugin: PluginMetadata): string[] {
    return plugin.schema.allowedSubElements || [];
  }
  
  /**
   * Check if child can be dropped into container
   */
  public canDropIntoContainer(childType: string, containerNode: VisualNode): boolean {
    const allowedChildren = this.getAllowedChildren(containerNode.plugin);
    
    // If no restrictions, allow anything
    if (allowedChildren.length === 0) {
      return true;
    }
    
    // Check if child type is in allowed list
    return allowedChildren.includes(childType);
  }
  
  /**
   * Add child to container
   */
  public addChildToContainer(childId: string, containerId: string): boolean {
    const container = this.nodes.get(containerId);
    const child = this.nodes.get(childId);
    
    if (!container || !child) {
      console.error('Container or child node not found');
      return false;
    }
    
    // Validate
    if (!this.canDropIntoContainer(child.type, container)) {
      console.error(`Cannot add ${child.type} to ${container.type}`);
      return false;
    }
    
    // Remove from previous parent if exists
    if (child.parentId) {
      this.removeChildFromContainer(childId, child.parentId);
    }
    
    // Add to new parent
    if (!container.children.includes(childId)) {
      container.children.push(childId);
    }
    child.parentId = containerId;
    
    console.log(`✅ Added ${child.type} as child of ${container.type}`);
    return true;
  }
  
  /**
   * Remove child from container
   */
  public removeChildFromContainer(childId: string, containerId: string): boolean {
    const container = this.nodes.get(containerId);
    const child = this.nodes.get(childId);
    
    if (!container || !child) {
      return false;
    }
    
    const index = container.children.indexOf(childId);
    if (index > -1) {
      container.children.splice(index, 1);
    }
    
    child.parentId = null;
    return true;
  }
  
  /**
   * Get all children of a container
   */
  public getChildren(containerId: string): VisualNode[] {
    const container = this.nodes.get(containerId);
    if (!container) return [];
    
    return container.children
      .map(id => this.nodes.get(id))
      .filter(n => n !== undefined) as VisualNode[];
  }
  
  /**
   * Get node depth in hierarchy
   */
  public getNodeDepth(nodeId: string): number {
    const node = this.nodes.get(nodeId);
    if (!node || !node.parentId) return 0;
    
    return 1 + this.getNodeDepth(node.parentId);
  }
  
  /**
   * Register node
   */
  public registerNode(node: VisualNode): void {
    this.nodes.set(node.id, node);
  }
  
  /**
   * Unregister node
   */
  public unregisterNode(nodeId: string): void {
    const node = this.nodes.get(nodeId);
    if (!node) return;
    
    // Remove from parent
    if (node.parentId) {
      this.removeChildFromContainer(nodeId, node.parentId);
    }
    
    // Remove all children
    [...node.children].forEach(childId => {
      this.unregisterNode(childId);
    });
    
    this.nodes.delete(nodeId);
  }
  
  /**
   * Get all root nodes (no parent)
   */
  public getRootNodes(): VisualNode[] {
    return Array.from(this.nodes.values()).filter(n => !n.parentId);
  }
  
  /**
   * Get hierarchy tree
   */
  public getHierarchyTree(): any[] {
    const roots = this.getRootNodes();
    
    const buildTree = (node: VisualNode): any => {
      return {
        node,
        children: this.getChildren(node.id).map(buildTree)
      };
    };
    
    return roots.map(buildTree);
  }
}

