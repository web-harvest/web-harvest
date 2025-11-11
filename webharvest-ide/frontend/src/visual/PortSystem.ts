/**
 * PortSystem - Implementation
 * Manages input/output ports on workflow nodes for manual connections
 */

import type { VisualNode } from '../types';

export type PortType = 'sequential' | 'conditional' | 'error';

export interface Port {
    nodeId: string;
    type: PortType;
    direction: 'input' | 'output';
    label?: string;
    position?: { x: number; y: number };
    element?: HTMLElement;
}

export interface NodePorts {
    input: Port;
    output?: Port;
    outputs?: Port[];
}

export class PortSystem {
    private ports: Map<string, Port[]> = new Map(); // nodeId -> ports
    private highlightedPorts: HTMLElement[] = [];

    /**
     * Add ports to node element
     */
    addPorts(element: HTMLElement, node: VisualNode): void {
        const nodeId = node.id;
        const portsConfig = this.getPortsForNodeType(node.type);

        if (!element.style.position || element.style.position === 'static') {
            element.style.position = 'relative';
        }

        const nodePorts: Port[] = [];

        const inputPortElement = this.createPortElement('input', 'sequential', nodeId);
        this.positionPort(inputPortElement, 'top', '50%');
        element.appendChild(inputPortElement);

        nodePorts.push({
            nodeId,
            type: 'sequential',
            direction: 'input',
            element: inputPortElement
        });

        if (portsConfig.outputs && portsConfig.outputs.length > 1) {
            const outputCount = portsConfig.outputs.length;
            portsConfig.outputs.forEach((portConfig, index) => {
                const portElement = this.createPortElement('output', portConfig.type, nodeId, portConfig.label);
                const leftPercent = ((index + 1) / (outputCount + 1)) * 100;
                this.positionPort(portElement, 'bottom', `${leftPercent}%`);
                element.appendChild(portElement);

                nodePorts.push({
                    nodeId,
                    type: portConfig.type,
                    direction: 'output',
                    label: portConfig.label,
                    element: portElement
                });
            });
        } else {
            const outputPortElement = this.createPortElement('output', 'sequential', nodeId);
            this.positionPort(outputPortElement, 'bottom', '50%');
            element.appendChild(outputPortElement);

            nodePorts.push({
                nodeId,
                type: 'sequential',
                direction: 'output',
                element: outputPortElement
            });
        }

        this.ports.set(nodeId, nodePorts);
    }

    /**
     * Create port element
     */
    private createPortElement(
        direction: 'input' | 'output',
        type: PortType,
        nodeId: string,
        label?: string
    ): HTMLElement {
        const port = document.createElement('div');
        port.className = `port port-${direction}`;
        port.dataset.nodeId = nodeId;
        port.dataset.portType = direction;
        port.dataset.portConnectionType = type;

        // Port styling
        port.style.cssText = `
            position: absolute;
            width: 12px;
            height: 12px;
            border-radius: 50%;
            background: var(--gray-400);
            border: 2px solid white;
            cursor: crosshair;
            z-index: 15 !important;
            transition: all 0.2s;
            pointer-events: auto !important;
        `;

        // Type-specific colors
        switch (type) {
            case 'conditional':
                port.style.background = 'var(--lime)';
                break;
            case 'error':
                port.style.background = 'var(--magenta)';
                port.className += ' port-error';
                break;
            case 'sequential':
            default:
                port.style.background = 'var(--gray-600)';
                break;
        }

        // Label (for conditional/error ports)
        if (label) {
            const labelElement = document.createElement('div');
            labelElement.className = 'port-label';
            labelElement.textContent = label;
            labelElement.style.cssText = `
                position: absolute;
                font-size: 10px;
                color: var(--text-primary);
                white-space: nowrap;
                background: rgba(255, 255, 255, 0.9);
                padding: 2px 6px;
                border-radius: 3px;
                pointer-events: none;
            `;

            if (direction === 'output') {
                labelElement.style.top = '16px';
                labelElement.style.left = '50%';
                labelElement.style.transform = 'translateX(-50%)';
            }

            port.appendChild(labelElement);
        }

        // Hover effects
        port.addEventListener('mouseenter', () => {
            port.classList.add('port-hover');
            port.style.width = '16px';
            port.style.height = '16px';
            port.style.boxShadow = '0 0 8px rgba(0,0,0,0.3)';
        });

        port.addEventListener('mouseleave', () => {
            port.classList.remove('port-hover');
            port.style.width = '12px';
            port.style.height = '12px';
            port.style.boxShadow = '';
        });

        return port;
    }

    /**
     * Position port on node
     */
    private positionPort(port: HTMLElement, verticalPos: 'top' | 'bottom', horizontalPos: string): void {
        if (verticalPos === 'top') {
            port.style.top = '0px';
            port.style.transform = 'translate(-50%, -50%)';
        } else {
            port.style.bottom = '0px';
            port.style.transform = 'translate(-50%, 50%)';
        }

        port.style.left = horizontalPos;
    }

    /**
     * Get port configuration for node type
     */
    getPortsForNodeType(nodeType: string): NodePorts {
        const type = nodeType.toLowerCase();

        switch (type) {
            case 'if':
                return {
                    input: { nodeId: '', type: 'sequential', direction: 'input' },
                    outputs: [
                        { nodeId: '', type: 'conditional', direction: 'output', label: 'true' },
                        { nodeId: '', type: 'conditional', direction: 'output', label: 'false' }
                    ]
                };

            case 'case':
            case 'switch':
                return {
                    input: { nodeId: '', type: 'sequential', direction: 'input' },
                    outputs: [
                        { nodeId: '', type: 'conditional', direction: 'output', label: 'case 1' },
                        { nodeId: '', type: 'conditional', direction: 'output', label: 'case 2' },
                        { nodeId: '', type: 'conditional', direction: 'output', label: 'default' }
                    ]
                };

            case 'try':
                return {
                    input: { nodeId: '', type: 'sequential', direction: 'input' },
                    outputs: [
                        { nodeId: '', type: 'sequential', direction: 'output', label: 'success' },
                        { nodeId: '', type: 'error', direction: 'output', label: 'error' }
                    ]
                };

            default:
                // Standard action node: 1 input, 1 output
                return {
                    input: { nodeId: '', type: 'sequential', direction: 'input' },
                    output: { nodeId: '', type: 'sequential', direction: 'output' }
                };
        }
    }

    /**
     * Get port position in canvas coordinates
     */
    getPortPosition(element: HTMLElement, portDirection: string): { x: number; y: number } | null {
        const port = element.querySelector(`.port-${portDirection}`) as HTMLElement;
        if (!port) return null;

        const rect = element.getBoundingClientRect();
        const portRect = port.getBoundingClientRect();

        return {
            x: portRect.left - rect.left + portRect.width / 2,
            y: portRect.top - rect.top + portRect.height / 2
        };
    }

    /**
     * Highlight compatible target ports during connection drag
     */
    highlightCompatiblePorts(sourceElement: HTMLElement, sourcePortDirection: string): void {
        this.clearHighlights();

        // Find all nodes in canvas
        const canvas = sourceElement.parentElement;
        if (!canvas) return;

        const allNodes = canvas.querySelectorAll('.visual-node');
        allNodes.forEach(nodeElement => {
            if (nodeElement === sourceElement) return; // Skip source node

            // Highlight compatible ports
            const ports = nodeElement.querySelectorAll('.port');
            ports.forEach(portElement => {
                const port = portElement as HTMLElement;
                const portType = port.dataset.portType;

                if (this.arePortsCompatible(sourcePortDirection, portType || '')) {
                    port.classList.add('port-highlighted');
                    port.style.background = 'var(--lime)';
                    port.style.boxShadow = '0 0 12px var(--lime)';
                    this.highlightedPorts.push(port);
                }
            });
        });
    }

    /**
     * Clear all port highlights
     */
    clearHighlights(): void {
        this.highlightedPorts.forEach(port => {
            port.classList.remove('port-highlighted');
            port.style.background = '';
            port.style.boxShadow = '';
        });
        this.highlightedPorts = [];
    }

    /**
     * Check if two ports are compatible for connection (basic check)
     */
    arePortsCompatible(sourcePort: string, targetPort: string): boolean {
        // Output ports can connect to input ports
        if (sourcePort.startsWith('output') && targetPort.startsWith('input')) {
            return true;
        }

        // Special case: error output to error input (try/catch)
        if (sourcePort === 'output-error' && targetPort === 'input-error') {
            return true;
        }

        return false;
    }

    /**
     * Validate port type compatibility for connection
     * Checks if source port type (sequential/conditional/error) matches target port requirements
     */
    validatePortTypeCompatibility(sourcePortType: PortType, targetPortType: PortType | null): { valid: boolean; error?: string } {
        // If target port type is not specified, assume sequential input (most common case)
        if (!targetPortType) {
            // Sequential outputs can connect to any input
            if (sourcePortType === 'sequential') {
                return { valid: true };
            }
            // Conditional and error outputs require specific input types
            return {
                valid: false,
                error: `Cannot connect ${sourcePortType} output to standard sequential input`
            };
        }

        // Sequential outputs can connect to sequential inputs
        if (sourcePortType === 'sequential' && targetPortType === 'sequential') {
            return { valid: true };
        }

        // Conditional outputs can connect to conditional inputs (for if/case nodes)
        if (sourcePortType === 'conditional' && targetPortType === 'conditional') {
            return { valid: true };
        }

        // Error outputs can connect to error inputs (for try/catch)
        if (sourcePortType === 'error' && targetPortType === 'error') {
            return { valid: true };
        }

        // Sequential outputs can connect to conditional inputs (normal flow to conditional branches)
        if (sourcePortType === 'sequential' && targetPortType === 'conditional') {
            return { valid: true };
        }

        // Error outputs cannot connect to sequential/conditional inputs
        if (sourcePortType === 'error' && (targetPortType === 'sequential' || targetPortType === 'conditional')) {
            return {
                valid: false,
                error: `Cannot connect error output to ${targetPortType} input. Use error input port.`
            };
        }

        // Conditional outputs cannot connect to sequential inputs (requires explicit handling)
        if (sourcePortType === 'conditional' && targetPortType === 'sequential') {
            return {
                valid: false,
                error: `Cannot connect conditional output to sequential input. Use conditional input port or merge node.`
            };
        }

        // All other combinations are invalid
        return {
            valid: false,
            error: `Incompatible port types: ${sourcePortType} output cannot connect to ${targetPortType} input`
        };
    }

    /**
     * Get port type from port element
     */
    getPortTypeFromElement(portElement: HTMLElement): PortType {
        const portTypeAttr = portElement.dataset.portConnectionType;
        if (portTypeAttr === 'conditional' || portTypeAttr === 'error' || portTypeAttr === 'sequential') {
            return portTypeAttr;
        }
        return 'sequential'; // Default
    }

    /**
     * Find port element by type and direction for a node
     */
    findPortElement(nodeElement: HTMLElement, direction: 'input' | 'output', portType?: PortType): HTMLElement | null {
        const ports = nodeElement.querySelectorAll(`.port-${direction}`);
        
        if (!portType) {
            // Return first port of the requested direction
            return ports.length > 0 ? (ports[0] as HTMLElement) : null;
        }

        // Find port with matching type
        for (let i = 0; i < ports.length; i++) {
            const port = ports[i] as HTMLElement;
            const portTypeAttr = port.dataset.portConnectionType;
            if (portTypeAttr === portType) {
                return port;
            }
        }

        return null;
    }

    /**
     * Get all ports for a node element
     */
    getPortsForNode(element: HTMLElement): Port[] {
        const nodeId = element.dataset.nodeId;
        if (!nodeId) return [];

        return this.ports.get(nodeId) || [];
    }

    /**
     * Remove ports from node
     */
    removePorts(element: HTMLElement): void {
        const nodeId = element.dataset.nodeId;
        if (nodeId) {
            this.ports.delete(nodeId);
        }

        const ports = element.querySelectorAll('.port');
        ports.forEach(port => port.remove());
    }

    /**
     * Update port positions after node move
     */
    updatePortPositions(element: HTMLElement): void {
        const nodeId = element.dataset.nodeId;
        if (!nodeId) return;

        const nodePorts = this.ports.get(nodeId);
        if (!nodePorts) return;

        nodePorts.forEach(port => {
            if (port.element) {
                const position = this.calculatePortPosition(element, port);
                port.position = position;
            }
        });
    }

    /**
     * Calculate port position in absolute coordinates
     */
    private calculatePortPosition(element: HTMLElement, port: Port): { x: number; y: number } {
        const rect = element.getBoundingClientRect();
        const width = parseFloat(element.style.width) || rect.width;
        const height = parseFloat(element.style.height) || rect.height;

        if (port.direction === 'input') {
            return { x: width / 2, y: 0 };
        } else {
            return { x: width / 2, y: height };
        }
    }
}


