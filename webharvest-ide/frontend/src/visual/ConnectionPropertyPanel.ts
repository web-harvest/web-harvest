/**
 * ConnectionPropertyPanel - Implementation
 * Edit connection labels, conditions, source, target, and transformations
 */

import type { Connection } from './ConnectionManager';
import type { VisualNode } from '../types';
import type { PortSystem, PortType } from './PortSystem';
import type { ConnectionManager } from './ConnectionManager';

export interface ConnectionPropertyPanelOptions {
    nodes: VisualNode[];
    portSystem?: PortSystem;
    connectionManager?: ConnectionManager;
    onValidationError?: (error: string) => void;
}

export class ConnectionPropertyPanel {
    private container: HTMLElement | null = null;
    private currentConnection: Connection | null = null;
    private onChangeCallback: ((connection: Connection) => void) | null = null;
    private options: ConnectionPropertyPanelOptions | null = null;

    show(container: HTMLElement, connection: Connection, options?: ConnectionPropertyPanelOptions): void {
        this.container = container;
        this.currentConnection = connection;
        this.options = options || null;

        const nodes = options?.nodes || [];
        const portSystem = options?.portSystem;
        const connectionManager = options?.connectionManager;

        container.innerHTML = '';
        container.innerHTML = `
            <div style="padding: 16px;">
                <h3 style="margin: 0 0 16px 0; color: var(--magenta, #D81B60);">Connection Properties</h3>
                
                <label style="margin-bottom: 16px; display: block;">
                    <div style="font-weight: 600; margin-bottom: 4px;">Source Node:</div>
                    <select class="conn-source" style="width: 100%; padding: 8px; margin-top: 4px; border: 1px solid var(--gray-300); border-radius: 4px;">
                        ${this.renderNodeOptions(nodes, connection.from)}
                    </select>
                </label>
                
                <label style="margin-bottom: 16px; display: block;">
                    <div style="font-weight: 600; margin-bottom: 4px;">Target Node:</div>
                    <select class="conn-target" style="width: 100%; padding: 8px; margin-top: 4px; border: 1px solid var(--gray-300); border-radius: 4px;">
                        ${this.renderNodeOptions(nodes, connection.to)}
                    </select>
                </label>
                
                <label style="margin-bottom: 16px; display: block;">
                    <div style="font-weight: 600; margin-bottom: 4px;">Connection Type:</div>
                    <select class="conn-type" style="width: 100%; padding: 8px; margin-top: 4px; border: 1px solid var(--gray-300); border-radius: 4px;">
                        <option value="sequential" ${connection.type === 'sequential' ? 'selected' : ''}>Sequential</option>
                        <option value="conditional" ${connection.type === 'conditional' ? 'selected' : ''}>Conditional</option>
                        <option value="error" ${connection.type === 'error' ? 'selected' : ''}>Error</option>
                    </select>
                </label>
                
                <label style="margin-bottom: 16px; display: block;">
                    <div style="font-weight: 600; margin-bottom: 4px;">Label:</div>
                    <input type="text" value="${connection.label || ''}" class="conn-label" 
                           style="width: 100%; padding: 8px; margin-top: 4px; border: 1px solid var(--gray-300); border-radius: 4px;" 
                           placeholder="Optional connection label" />
                </label>
                
                <label style="margin-bottom: 16px; display: block;">
                    <div style="font-weight: 600; margin-bottom: 4px;">Condition:</div>
                    <textarea class="conn-condition" rows="3" 
                              style="width: 100%; padding: 8px; margin-top: 4px; border: 1px solid var(--gray-300); border-radius: 4px; font-family: monospace;"
                              placeholder="Optional condition expression">${connection.condition || ''}</textarea>
                </label>
            </div>
        `;

        const sourceSelect = container.querySelector('.conn-source') as HTMLSelectElement;
        const targetSelect = container.querySelector('.conn-target') as HTMLSelectElement;
        const typeSelect = container.querySelector('.conn-type') as HTMLSelectElement;
        const labelInput = container.querySelector('.conn-label') as HTMLInputElement;
        const conditionInput = container.querySelector('.conn-condition') as HTMLTextAreaElement;

        // Handle source/target changes with validation
        sourceSelect?.addEventListener('change', () => {
            if (!this.currentConnection || !connectionManager) return;

            const newFrom = sourceSelect.value;
            
            // Step 1: Basic connection validation
            const validation = connectionManager.validateConnection(
                newFrom,
                this.currentConnection.to,
                this.currentConnection.type as any,
                this.currentConnection.id
            );

            if (!validation.valid) {
                options?.onValidationError?.(validation.error || 'Invalid connection');
                sourceSelect.value = this.currentConnection.from; // Revert
                return;
            }

            // Step 2: Validate port compatibility if PortSystem is available
            let sourcePortType: PortType = this.currentConnection.type as PortType;
            let targetPortType: PortType | null = null;
            
            if (portSystem && options?.nodes) {
                const fromNode = options.nodes.find(n => n.id === newFrom);
                const toNode = options.nodes.find(n => n.id === this.currentConnection!.to);
                
                if (fromNode && toNode) {
                    // Find source output port (using connection type)
                    const fromNodeElement = document.querySelector(`[data-node-id="${newFrom}"]`) as HTMLElement;
                    const sourceOutputPort = fromNodeElement ? portSystem.findPortElement(fromNodeElement, 'output', this.currentConnection.type as PortType) : null;
                    sourcePortType = sourceOutputPort ? portSystem.getPortTypeFromElement(sourceOutputPort) : (this.currentConnection.type as PortType);
                    
                    // Find target input port
                    const toNodeElement = document.querySelector(`[data-node-id="${this.currentConnection.to}"]`) as HTMLElement;
                    const targetInputPort = toNodeElement ? portSystem.findPortElement(toNodeElement, 'input') : null;
                    targetPortType = targetInputPort ? portSystem.getPortTypeFromElement(targetInputPort) : null;
                    
                    const portValidation = portSystem.validatePortTypeCompatibility(sourcePortType, targetPortType);
                    
                    if (!portValidation.valid) {
                        options?.onValidationError?.(portValidation.error || 'Port types incompatible');
                        sourceSelect.value = this.currentConnection.from; // Revert
                        return;
                    }
                }
            }

            // Step 3: Validate semantic compatibility
            const semanticValidation = connectionManager.validateReconnectSemantics(
                newFrom,
                this.currentConnection.to,
                sourcePortType,
                targetPortType,
                options?.nodes || [],
                (nodeId: string) => {
                    const node = options?.nodes?.find(n => n.id === nodeId);
                    return node?.type || null;
                }
            );

            if (!semanticValidation.valid) {
                options?.onValidationError?.(semanticValidation.error || 'Semantic validation failed');
                sourceSelect.value = this.currentConnection.from; // Revert
                return;
            }

            // Step 4: Additional semantic checks (parent-child relationship)
            if (options?.nodes) {
                const fromNode = options.nodes.find(n => n.id === newFrom);
                const toNode = options.nodes.find(n => n.id === this.currentConnection!.to);
                
                if (fromNode && toNode) {
                    // Check if nodes are related (parent-child)
                    const areRelated = this.areNodesRelated(fromNode, toNode, options.nodes);
                    if (areRelated) {
                        options?.onValidationError?.('Cannot connect nodes that are in a parent-child relationship. Use nesting instead.');
                        sourceSelect.value = this.currentConnection.from; // Revert
                        return;
                    }
                }
            }

            this.currentConnection.from = newFrom;
            this.onChangeCallback?.(this.currentConnection);
        });

        targetSelect?.addEventListener('change', () => {
            if (!this.currentConnection || !connectionManager) return;

            const newTo = targetSelect.value;
            
            // Step 1: Basic connection validation
            const validation = connectionManager.validateConnection(
                this.currentConnection.from,
                newTo,
                this.currentConnection.type as any,
                this.currentConnection.id
            );

            if (!validation.valid) {
                options?.onValidationError?.(validation.error || 'Invalid connection');
                targetSelect.value = this.currentConnection.to; // Revert
                return;
            }

            // Step 2: Validate port compatibility if PortSystem is available
            let sourcePortType: PortType = this.currentConnection.type as PortType;
            let targetPortType: PortType | null = null;
            
            if (portSystem && options?.nodes) {
                const fromNode = options.nodes.find(n => n.id === this.currentConnection!.from);
                const toNode = options.nodes.find(n => n.id === newTo);
                
                if (fromNode && toNode) {
                    // Find source output port
                    const fromNodeElement = document.querySelector(`[data-node-id="${this.currentConnection.from}"]`) as HTMLElement;
                    const sourceOutputPort = fromNodeElement ? portSystem.findPortElement(fromNodeElement, 'output', this.currentConnection.type as PortType) : null;
                    sourcePortType = sourceOutputPort ? portSystem.getPortTypeFromElement(sourceOutputPort) : (this.currentConnection.type as PortType);
                    
                    // Find target input port
                    const toNodeElement = document.querySelector(`[data-node-id="${newTo}"]`) as HTMLElement;
                    const targetInputPort = toNodeElement ? portSystem.findPortElement(toNodeElement, 'input') : null;
                    targetPortType = targetInputPort ? portSystem.getPortTypeFromElement(targetInputPort) : null;
                    
                    const portValidation = portSystem.validatePortTypeCompatibility(sourcePortType, targetPortType);
                    
                    if (!portValidation.valid) {
                        options?.onValidationError?.(portValidation.error || 'Port types incompatible');
                        targetSelect.value = this.currentConnection.to; // Revert
                        return;
                    }
                }
            }

            // Step 3: Validate semantic compatibility
            const semanticValidation = connectionManager.validateReconnectSemantics(
                this.currentConnection.from,
                newTo,
                sourcePortType,
                targetPortType,
                options?.nodes || [],
                (nodeId: string) => {
                    const node = options?.nodes?.find(n => n.id === nodeId);
                    return node?.type || null;
                }
            );

            if (!semanticValidation.valid) {
                options?.onValidationError?.(semanticValidation.error || 'Semantic validation failed');
                targetSelect.value = this.currentConnection.to; // Revert
                return;
            }

            // Step 4: Additional semantic checks (parent-child relationship)
            if (options?.nodes) {
                const fromNode = options.nodes.find(n => n.id === this.currentConnection!.from);
                const toNode = options.nodes.find(n => n.id === newTo);
                
                if (fromNode && toNode) {
                    // Check if nodes are related (parent-child)
                    const areRelated = this.areNodesRelated(fromNode, toNode, options.nodes);
                    if (areRelated) {
                        options?.onValidationError?.('Cannot connect nodes that are in a parent-child relationship. Use nesting instead.');
                        targetSelect.value = this.currentConnection.to; // Revert
                        return;
                    }
                }
            }

            this.currentConnection.to = newTo;
            this.onChangeCallback?.(this.currentConnection);
        });

        typeSelect?.addEventListener('change', () => {
            if (this.currentConnection && this.onChangeCallback) {
                this.currentConnection.type = typeSelect.value as any;
                this.onChangeCallback(this.currentConnection);
            }
        });

        labelInput?.addEventListener('input', () => {
            if (this.currentConnection && this.onChangeCallback) {
                this.currentConnection.label = labelInput.value;
                this.onChangeCallback(this.currentConnection);
            }
        });

        conditionInput?.addEventListener('input', () => {
            if (this.currentConnection && this.onChangeCallback) {
                this.currentConnection.condition = conditionInput.value;
                this.onChangeCallback(this.currentConnection);
            }
        });
    }

    private renderNodeOptions(nodes: VisualNode[], selectedNodeId: string): string {
        const collectNodes = (nodeList: VisualNode[], level: number = 0): Array<{ node: VisualNode; label: string }> => {
            const result: Array<{ node: VisualNode; label: string }> = [];
            
            nodeList.forEach(node => {
                const indent = '  '.repeat(level);
                const label = node.attributes?.name || node.type || node.id;
                result.push({ node, label: `${indent}${label} (${node.type})` });
                
                if (node.children && node.children.length > 0) {
                    result.push(...collectNodes(node.children, level + 1));
                }
            });
            
            return result;
        };

        const allNodes = collectNodes(nodes);
        
        return allNodes.map(({ node, label }) => 
            `<option value="${node.id}" ${node.id === selectedNodeId ? 'selected' : ''}>${this.escapeHtml(label)}</option>`
        ).join('\n');
    }

    private escapeHtml(text: string): string {
        const div = document.createElement('div');
        div.textContent = text;
        return div.innerHTML;
    }

    /**
     * Check if two nodes are in a parent-child relationship
     */
    private areNodesRelated(node1: VisualNode, node2: VisualNode, allNodes: VisualNode[]): boolean {
        // Check if node2 is a descendant of node1
        if (this.isNodeInSubtree(node1, node2.id)) {
            return true;
        }
        
        // Check if node1 is a descendant of node2
        if (this.isNodeInSubtree(node2, node1.id)) {
            return true;
        }
        
        return false;
    }

    /**
     * Check if a node with given ID exists in the subtree of the given node
     */
    private isNodeInSubtree(node: VisualNode, targetId: string): boolean {
        // Check children recursively
        if (node.children && node.children.length > 0) {
            for (const child of node.children) {
                if (child.id === targetId) {
                    return true;
                }
                // Recursively check subtree
                if (this.isNodeInSubtree(child, targetId)) {
                    return true;
                }
            }
        }
        
        return false;
    }

    onChange(callback: (connection: Connection) => void): void {
        this.onChangeCallback = callback;
    }

    hide(): void {
        if (this.container) {
            this.container.innerHTML = '';
        }
        this.currentConnection = null;
        this.options = null;
    }
}


