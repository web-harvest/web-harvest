/**
 * Visual Model Helper Functions
 * 
 * Implements node classification, rendering strategies, and layout algorithms
 * for the hybrid visual model (nested + connected).
 */

import type { VisualNode } from '../types';

// ============================================================================
// Node Classification
// ============================================================================

/**
 * Container nodes that use nested rendering (children inside)
 */
const CONTAINER_TYPES = [
    'loop', 'while', 'if', 'try', 'catch', 'case',
    'function', 'body', 'config', 'switch'
];

/**
 * Check if node type is a container (nested rendering)
 */
export function isContainerNode(type: string): boolean {
    return CONTAINER_TYPES.includes(type.toLowerCase());
}

/**
 * Get rendering strategy for a node
 */
export function getRenderingStrategy(node: VisualNode): 'nested' | 'connected' {
    // Container node → always use nested rendering (even without children initially)
    if (isContainerNode(node.type)) {
        return 'nested';
    }

    // Otherwise → connected rendering (flowchart)
    return 'connected';
}

// ============================================================================
// Layout Algorithms
// ============================================================================

export interface LayoutPosition {
    x: number;
    y: number;
}

export interface LinearLayoutOptions {
    startX: number;
    startY: number;
    spacing: number;
}

/**
 * Calculate positions for linear sequence (vertical flowchart)
 */
export function calculateLinearLayout(
    nodes: VisualNode[],
    options: LinearLayoutOptions
): Record<string, LayoutPosition> {
    const positions: Record<string, LayoutPosition> = {};
    let currentY = options.startY;

    nodes.forEach(node => {
        positions[node.id] = {
            x: options.startX,
            y: currentY
        };
        currentY += options.spacing;
    });

    return positions;
}

export interface NestedLayoutOptions {
    x: number;
    y: number;
    padding: number;
}

const HEADER_HEIGHT = 40;
const NODE_HEIGHT = 100;

/**
 * Calculate positions for nested container (children inside)
 */
export function calculateNestedLayout(
    node: VisualNode,
    options: NestedLayoutOptions
): Record<string, LayoutPosition> {
    const positions: Record<string, LayoutPosition> = {};

    // Container position
    positions[node.id] = { x: options.x, y: options.y };

    // Children positions (inside container, offset by padding + header)
    let childY = options.y + options.padding + HEADER_HEIGHT;
    node.children.forEach(child => {
        positions[child.id] = {
            x: options.x + options.padding,
            y: childY
        };
        childY += NODE_HEIGHT + options.padding;
    });

    return positions;
}

// ============================================================================
// Connection Lines
// ============================================================================

export interface Connection {
    from: string; // node ID
    to: string;   // node ID
    type: 'sequential' | 'conditional' | 'error';
}

/**
 * Create connection between two nodes
 */
export function createConnection(from: VisualNode, to: VisualNode): Connection {
    // Determine connection type based on context
    let connectionType: 'sequential' | 'conditional' | 'error' = 'sequential';

    if (from.type === 'if' || from.type === 'case') {
        connectionType = 'conditional';
    } else if (from.type === 'try' || to.type === 'catch') {
        connectionType = 'error';
    }

    return {
        from: from.id,
        to: to.id,
        type: connectionType
    };
}

/**
 * Generate all connections for a linear sequence
 */
export function generateConnections(nodes: VisualNode[]): Connection[] {
    const connections: Connection[] = [];

    for (let i = 0; i < nodes.length - 1; i++) {
        connections.push(createConnection(nodes[i], nodes[i + 1]));
    }

    return connections;
}

// ============================================================================
// Node Manipulation
// ============================================================================

/**
 * Reorder nodes in array
 */
export function reorderNodes(
    nodes: VisualNode[],
    nodeId: string,
    newIndex: number
): VisualNode[] {
    const nodeIndex = nodes.findIndex(n => n.id === nodeId);
    if (nodeIndex === -1) {
        return nodes;
    }

    // Create new array
    const newNodes = [...nodes];

    // Remove from old position
    const [removed] = newNodes.splice(nodeIndex, 1);

    // Insert at new position
    newNodes.splice(newIndex, 0, removed);

    return newNodes;
}

/**
 * Add node to container
 */
export function addNodeToContainer(
    container: VisualNode,
    node: VisualNode
): VisualNode {
    // Validate: cannot add node to itself
    if (container.id === node.id) {
        throw new Error('Cannot add node to itself');
    }

    // Validate: no circular nesting
    if (hasCircularReference(container, node)) {
        throw new Error('Circular nesting detected');
    }

    // Create new container with added child
    return {
        ...container,
        children: [...container.children, node]
    };
}

/**
 * Remove node from container
 */
export function removeNodeFromContainer(
    container: VisualNode,
    nodeId: string
): VisualNode {
    return {
        ...container,
        children: container.children.filter(child => child.id !== nodeId)
    };
}

/**
 * Check for circular reference (A contains B contains A)
 */
export function hasCircularReference(
    parent: VisualNode,
    child: VisualNode
): boolean {
    // Check if parent is in child's immediate children
    if (child.children.some(c => c.id === parent.id)) {
        return true;
    }

    // Recursively check child's descendants
    return child.children.some(c => hasCircularReference(parent, c));
}

// ============================================================================
// Plugin Templates
// ============================================================================

export interface PluginTemplate {
    type: string;
    category: string;
    icon: string;
    label: string;
    defaultAttributes: Record<string, string>;
    description: string;
    canAppearAtRoot?: boolean; // For semantic filtering
    // VisualPluginMetadata fields
    color?: string; // Hex color for category
    defaultWidth?: number; // Default node width in canvas
    defaultHeight?: number; // Default node height in canvas
    inputPorts?: Array<{ name: string; description?: string; type: string; required: boolean }>;
    outputPorts?: Array<{ name: string; description?: string; type: string }>;
    portType?: string; // STRING, XML, BINARY, MIXED, etc.
    tooltip?: string; // Extended tooltip
}

/**
 * Get all plugin templates for palette
 */
export function getPluginTemplates(): PluginTemplate[] {
    return [
        // ========== Network ==========
        {
            type: 'http',
            category: 'network',
            icon: 'network-icon',
            label: 'HTTP Request',
            defaultAttributes: {
                url: '',
                method: 'GET'
            },
            description: 'Fetch data from HTTP endpoint'
        },
        {
            type: 'ftp',
            category: 'network',
            icon: 'ftp-icon',
            label: 'FTP',
            defaultAttributes: {
                host: 'ftp.example.com',
                action: 'list'
            },
            description: 'FTP file transfer'
        },

        // ========== Extraction ==========
        {
            type: 'xpath',
            category: 'extraction',
            icon: 'xpath-icon',
            label: 'XPath',
            defaultAttributes: {
                expression: ''
            },
            description: 'Extract data using XPath'
        },
        {
            type: 'xquery',
            category: 'extraction',
            icon: 'xquery-icon',
            label: 'XQuery',
            defaultAttributes: {},
            description: 'Transform XML using XQuery'
        },
        {
            type: 'regexp',
            category: 'extraction',
            icon: 'regexp-icon',
            label: 'Regular Expression',
            defaultAttributes: {},
            description: 'Extract data using regex'
        },
        {
            type: 'html-to-xml',
            category: 'extraction',
            icon: 'html-icon',
            label: 'HTML to XML',
            defaultAttributes: {},
            description: 'Convert HTML to clean XML'
        },
        {
            type: 'json-to-xml',
            category: 'extraction',
            icon: 'json-icon',
            label: 'JSON to XML',
            defaultAttributes: {},
            description: 'Convert JSON to XML'
        },
        {
            type: 'xml-to-json',
            category: 'extraction',
            icon: 'json-icon',
            label: 'XML to JSON',
            defaultAttributes: {},
            description: 'Convert XML to JSON'
        },

        // ========== Variables ==========
        {
            type: 'def',
            category: 'variables',
            icon: 'var-icon',
            label: 'Define Variable',
            defaultAttributes: {
                var: ''
            },
            description: 'Define or update variable'
        },
        {
            type: 'get-var',
            category: 'variables',
            icon: 'var-icon',
            label: 'Get Variable',
            defaultAttributes: {
                var: 'myVariable'
            },
            description: 'Retrieve variable value'
        },

        // ========== Control Flow ==========
        {
            type: 'loop',
            category: 'control',
            icon: 'loop-icon',
            label: 'Loop',
            defaultAttributes: {
                maxloops: '10'
            },
            description: 'Iterate multiple times'
        },
        {
            type: 'while',
            category: 'control',
            icon: 'while-icon',
            label: 'While',
            defaultAttributes: {
                test: '${condition}',
                maxloops: '100'
            },
            description: 'Loop while condition is true'
        },
        {
            type: 'if',
            category: 'control',
            icon: 'condition-icon',
            label: 'If Condition',
            defaultAttributes: {
                test: ''
            },
            description: 'Conditional execution'
        },
        {
            type: 'try',
            category: 'control',
            icon: 'try-icon',
            label: 'Try/Catch',
            defaultAttributes: {},
            description: 'Error handling'
        },
        {
            type: 'case',
            category: 'control',
            icon: 'case-icon',
            label: 'Switch/Case',
            defaultAttributes: {},
            description: 'Multiple conditional branches'
        },

        // ========== Processing ==========
        {
            type: 'script',
            category: 'scripting',
            icon: 'code-icon',
            label: 'Script',
            defaultAttributes: {
                language: 'javascript'
            },
            description: 'Execute JavaScript/BeanShell code'
        },
        {
            type: 'template',
            category: 'processing',
            icon: 'template-icon',
            label: 'Template',
            defaultAttributes: {},
            description: 'Process template with variables'
        },
        {
            type: 'xslt',
            category: 'processing',
            icon: 'xslt-icon',
            label: 'XSLT Transform',
            defaultAttributes: {},
            description: 'Transform XML using XSLT'
        },

        // ========== Storage ==========
        {
            type: 'file',
            category: 'storage',
            icon: 'file-icon',
            label: 'File',
            defaultAttributes: {
                action: 'write',
                path: 'output.txt'
            },
            description: 'Read/write files'
        },

        // ========== Utilities ==========
        {
            type: 'log',
            category: 'utilities',
            icon: 'log-icon',
            label: 'Log',
            defaultAttributes: {
                level: 'info'
            },
            description: 'Log message to console'
        },
        {
            type: 'sleep',
            category: 'utilities',
            icon: 'sleep-icon',
            label: 'Sleep',
            defaultAttributes: {
                milliseconds: '1000'
            },
            description: 'Pause execution'
        },
        {
            type: 'return',
            category: 'utilities',
            icon: 'return-icon',
            label: 'Return',
            defaultAttributes: {},
            description: 'Return from function'
        },
        {
            type: 'exit',
            category: 'utilities',
            icon: 'exit-icon',
            label: 'Exit',
            defaultAttributes: {},
            description: 'Stop execution'
        }
    ];
}

/**
 * Create new node from plugin template
 */
export function createNodeFromTemplate(template: PluginTemplate): VisualNode {
    return {
        id: `node-${Date.now()}-${Math.random().toString(36).substr(2, 9)}`,
        type: template.type,
        attributes: { ...template.defaultAttributes },
        body: null,
        children: []
    };
}

/**
 * Get plugin template by type
 */
export function getPluginTemplate(type: string): PluginTemplate | null {
    return getPluginTemplates().find(t => t.type === type) || null;
}

/**
 * Get plugin templates by category
 */
export function getPluginTemplatesByCategory(category: string): PluginTemplate[] {
    return getPluginTemplates().filter(t => t.category === category);
}

/**
 * Get all unique categories
 */
export function getPluginCategories(): string[] {
    const categories = new Set<string>();
    getPluginTemplates().forEach(t => categories.add(t.category));
    return Array.from(categories).sort();
}

