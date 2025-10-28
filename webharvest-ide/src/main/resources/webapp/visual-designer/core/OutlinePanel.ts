/**
 * Outline Panel - Hierarchy Tree View
 * Shows complete node structure with parent-child relationships
 * Syncs with canvas selection
 */

import type { VisualNode } from '../types/VisualNode';
import { renderIcon } from '../utils/IconRenderer.js';

export class OutlinePanel {
  private container: HTMLElement;
  private onNodeSelect: (nodeId: string) => void;
  
  constructor(containerId: string, onNodeSelect: (nodeId: string) => void) {
    const element = document.getElementById(containerId);
    if (!element) {
      throw new Error(`Outline container not found: ${containerId}`);
    }
    this.container = element;
    this.onNodeSelect = onNodeSelect;
    
    console.log('🌲 Outline Panel initialized');
  }
  
  /**
   * Update outline with current node tree
   */
  public update(nodes: Map<string, VisualNode>): void {
    // Build hierarchy tree
    const rootNodes = Array.from(nodes.values()).filter(n => !n.parentId);
    
    if (rootNodes.length === 0) {
      this.showEmpty();
      return;
    }
    
    // Render tree
    const html = rootNodes.map(node => this.renderNode(node, nodes)).join('');
    this.container.innerHTML = html;
    
    // Setup event handlers
    this.setupEventHandlers();
    
    console.log(`🌲 Outline updated with ${rootNodes.length} root nodes`);
  }
  
  /**
   * Show empty state
   */
  private showEmpty(): void {
    this.container.innerHTML = `
      <div class="outline-empty">
        <i class="fas fa-folder-open"></i>
        <p>No nodes on canvas</p>
      </div>
    `;
  }
  
  /**
   * Render a node in outline tree
   */
  private renderNode(node: VisualNode, allNodes: Map<string, VisualNode>, depth: number = 0): string {
    const hasChildren = node.children.length > 0;
    const isContainer = hasChildren || (node.plugin.schema?.allowedSubElements?.length ?? 0) > 0;
    
    let html = `
      <div class="outline-node${isContainer ? ' container' : ''}" 
           data-node-id="${node.id}"
           style="margin-left: ${depth * 16}px">
        ${hasChildren ? '<i class="fas fa-chevron-down outline-toggle"></i>' : '<span style="width: 14px;"></span>'}
        <span class="outline-icon">${renderIcon(node.plugin.icon)}</span>
        <span class="outline-label">${node.plugin.name}</span>
        <span class="outline-badge">${node.executionState !== 'pending' ? this.getStateBadge(node.executionState) : ''}</span>
      </div>
    `;
    
    // Render children
    if (hasChildren) {
      const childrenHtml = node.children
        .map(childId => allNodes.get(childId))
        .filter(child => child !== undefined)
        .map(child => this.renderNode(child as VisualNode, allNodes, depth + 1))
        .join('');
      
      html += `<div class="outline-children">${childrenHtml}</div>`;
    }
    
    return html;
  }
  
  /**
   * Get state badge icon
   */
  private getStateBadge(state: string): string {
    switch (state) {
      case 'running': return '<i class="fas fa-circle-notch fa-spin" style="color: var(--yellow-primary); font-size: 10px;"></i>';
      case 'completed': return '<i class="fas fa-check-circle" style="color: var(--lime-success); font-size: 10px;"></i>';
      case 'error': return '<i class="fas fa-times-circle" style="color: #F44336; font-size: 10px;"></i>';
      case 'paused': return '<i class="fas fa-pause-circle" style="color: var(--gray-400); font-size: 10px;"></i>';
      default: return '';
    }
  }
  
  /**
   * Setup event handlers for outline nodes
   */
  private setupEventHandlers(): void {
    // Node selection
    this.container.querySelectorAll('.outline-node').forEach(node => {
      node.addEventListener('click', (e) => {
        e.stopPropagation();
        const nodeId = (node as HTMLElement).dataset.nodeId;
        if (nodeId) {
          this.selectNode(nodeId);
          this.onNodeSelect(nodeId);
        }
      });
    });
    
    // Toggle collapse
    this.container.querySelectorAll('.outline-toggle').forEach(toggle => {
      toggle.addEventListener('click', (e) => {
        e.stopPropagation();
        const node = (e.target as HTMLElement).closest('.outline-node');
        node?.classList.toggle('collapsed');
      });
    });
  }
  
  /**
   * Select node in outline
   */
  public selectNode(nodeId: string): void {
    // Deselect all
    this.container.querySelectorAll('.outline-node').forEach(n => {
      n.classList.remove('selected');
    });
    
    // Select this node
    const node = this.container.querySelector(`[data-node-id="${nodeId}"]`);
    if (node) {
      node.classList.add('selected');
      node.scrollIntoView({ behavior: 'smooth', block: 'nearest' });
    }
  }
}


