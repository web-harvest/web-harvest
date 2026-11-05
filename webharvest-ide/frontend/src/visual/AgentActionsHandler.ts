/**
 * AgentActionsHandler - Obsługa akcji od agenta AI
 * 
 * Wykonuje akcje od agenta (addNode, updateNode, removeNode, connectNodes, etc.)
 * z walidacją przed wykonaniem i zwracaniem wyników.
 */

import type { VisualDesigner } from './VisualDesigner';
import type { PluginManager } from '../core/PluginManager';
import type { VisualNode } from '../types';
import type {
    AgentAction,
    ActionResult,
    AddNodeParams,
    UpdateNodeParams,
    RemoveNodeParams,
    ConnectNodesParams,
    DisconnectNodesParams,
    MoveNodeParams,
    LoadConfigParams,
    ApplyAutoLayoutParams
} from './VisualSemanticModel';
import { XMLParser } from './XMLParser';

export class AgentActionsHandler {
    private designer: VisualDesigner;
    private pluginManager: PluginManager;
    private xmlParser: XMLParser;

    constructor(designer: VisualDesigner, pluginManager: PluginManager) {
        this.designer = designer;
        this.pluginManager = pluginManager;
        this.xmlParser = new XMLParser(pluginManager);
    }

    /**
     * Wykonaj akcję od agenta
     */
    async executeAction(action: AgentAction): Promise<ActionResult> {
        try {
            switch (action.type) {
                case 'addNode':
                    return this.addNode(action.parameters as AddNodeParams);
                case 'updateNode':
                    return this.updateNode(action.parameters as UpdateNodeParams);
                case 'removeNode':
                    return this.removeNode(action.parameters as RemoveNodeParams);
                case 'connectNodes':
                    return this.connectNodes(action.parameters as ConnectNodesParams);
                case 'disconnectNodes':
                    return this.disconnectNodes(action.parameters as DisconnectNodesParams);
                case 'moveNode':
                    return this.moveNode(action.parameters as MoveNodeParams);
                case 'loadConfig':
                    return this.loadConfig(action.parameters as LoadConfigParams);
                case 'applyAutoLayout':
                    return this.applyAutoLayout(action.parameters as ApplyAutoLayoutParams);
                default:
                    return {
                        success: false,
                        error: `Unknown action type: ${(action as any).type}`
                    };
            }
        } catch (error) {
            return {
                success: false,
                error: error instanceof Error ? error.message : String(error)
            };
        }
    }

    /**
     * Dodaj węzeł
     */
    private addNode(params: AddNodeParams): ActionResult {
        console.log('[AgentActionsHandler] 🔧 addNode called:', params);
        // Validate plugin type
        const plugin = this.pluginManager.findPluginById(params.pluginType);
        if (!plugin) {
            console.error(`[AgentActionsHandler] ❌ Plugin '${params.pluginType}' not found`);
            return {
                success: false,
                error: `Plugin type '${params.pluginType}' not found`
            };
        }
        console.log(`[AgentActionsHandler] ✅ Plugin '${params.pluginType}' found`);

        // Validate parent if provided
        if (params.parentId) {
            const parent = (this.designer as any).getNodeById(params.parentId);
            if (!parent) {
                return {
                    success: false,
                    error: `Parent node '${params.parentId}' not found`
                };
            }

            // Validate parent allows children
            const parentPlugin = this.pluginManager.findPluginById(parent.type);
            if (!parentPlugin?.semantics?.allowsChildren) {
                return {
                    success: false,
                    error: `Parent node '${params.parentId}' (type: ${parent.type}) does not allow children`
                };
            }

            // Validate child type is allowed
            if (!parentPlugin.semantics.acceptsAnyChild) {
                const allowedTypes = parentPlugin.semantics.allowedChildTypes || [];
                if (!allowedTypes.includes(params.pluginType)) {
                    return {
                        success: false,
                        error: `Plugin type '${params.pluginType}' is not allowed as child of '${parent.type}'. Allowed types: ${allowedTypes.join(', ')}`
                    };
                }
            }
        }

        // Create node
        const node: VisualNode = {
            id: this.generateNodeId(),
            type: params.pluginType,
            attributes: params.attributes || {},
            body: null,
            children: [],
            position: params.position
        };

        // Add node
        if (params.parentId) {
            // Add as child
            const parent = (this.designer as any).getNodeById(params.parentId);
            if (!parent) {
                return {
                    success: false,
                    error: `Parent node '${params.parentId}' not found`
                };
            }
            if (!parent.children) {
                parent.children = [];
            }
            parent.children.push(node);
            console.log(`[AgentActionsHandler] ✅ Added child node ${node.id} to parent ${params.parentId}`);
            this.designer['render'](); // Call render via private access
        } else {
            // Add as root node
            console.log(`[AgentActionsHandler] ✅ Adding root node ${node.id} via addNodeAtPosition`);
            // If position is (0,0) or not provided, treat as no position and apply auto-layout
            const hasRealPosition = params.position && 
                (params.position.x !== 0 || params.position.y !== 0);
            (this.designer as any).addNodeAtPosition(node, hasRealPosition ? params.position : undefined, {
                applyAutoLayout: !hasRealPosition // Auto-layout if no real position provided
            });
            console.log(`[AgentActionsHandler] ✅ Root node ${node.id} added, nodes count: ${this.designer.getNodes().length}, applyAutoLayout: ${!hasRealPosition}`);
        }

        return {
            success: true,
            nodeId: node.id
        };
    }

    /**
     * Zaktualizuj węzeł
     */
    private updateNode(params: UpdateNodeParams): ActionResult {
        const node = (this.designer as any).getNodeById(params.nodeId);
        if (!node) {
            return {
                success: false,
                error: `Node '${params.nodeId}' not found`
            };
        }

        // Validate attributes against plugin schema
        const plugin = this.pluginManager.findPluginById(node.type);
        if (plugin) {
            const validationErrors: string[] = [];
            const attributeDefinitions = plugin.schema?.attributes || plugin.schema?.attributeDefinitions || [];
            Object.keys(params.attributes).forEach(attrName => {
                const attrDef = attributeDefinitions.find((a: any) => a.name === attrName);
                if (!attrDef) {
                    // Attribute not in schema - allow but warn
                    console.warn(`[AgentActionsHandler] Attribute '${attrName}' not in schema for plugin '${node.type}'`);
                } else {
                    // Validate type and allowed values
                    const value = params.attributes[attrName];
                    if (attrDef.allowedValues && !attrDef.allowedValues.includes(value)) {
                        validationErrors.push(`Attribute '${attrName}' value '${value}' not in allowed values: ${attrDef.allowedValues.join(', ')}`);
                    }
                }
            });

            if (validationErrors.length > 0) {
                return {
                    success: false,
                    error: `Validation errors: ${validationErrors.join('; ')}`
                };
            }
        }

        // Update attributes
        Object.assign(node.attributes, params.attributes);

        // Update node in designer
        this.designer.updateNode(params.nodeId, node);

        return {
            success: true,
            nodeId: params.nodeId
        };
    }

    /**
     * Usuń węzeł
     */
    private removeNode(params: RemoveNodeParams): ActionResult {
        const node = (this.designer as any).getNodeById(params.nodeId);
        if (!node) {
            return {
                success: false,
                error: `Node '${params.nodeId}' not found`
            };
        }

        const removed = (this.designer as any).removeNodeById(params.nodeId);
        if (!removed) {
            return {
                success: false,
                error: `Failed to remove node '${params.nodeId}'`
            };
        }

        return {
            success: true,
            nodeId: params.nodeId
        };
    }

    /**
     * Połącz węzły
     */
    private connectNodes(params: ConnectNodesParams): ActionResult {
        const fromNode = (this.designer as any).getNodeById(params.fromNodeId);
        const toNode = (this.designer as any).getNodeById(params.toNodeId);

        if (!fromNode) {
            return {
                success: false,
                error: `Source node '${params.fromNodeId}' not found`
            };
        }

        if (!toNode) {
            return {
                success: false,
                error: `Target node '${params.toNodeId}' not found`
            };
        }

        // Connect nodes
        (this.designer as any).connectNodes(params.fromNodeId, params.toNodeId, params.type || 'sequential');

        return {
            success: true,
            connectionId: `conn-${params.fromNodeId}-${params.toNodeId}`
        };
    }

    /**
     * Rozłącz węzły
     */
    private disconnectNodes(params: DisconnectNodesParams): ActionResult {
        const fromNode = (this.designer as any).getNodeById(params.fromNodeId);
        const toNode = (this.designer as any).getNodeById(params.toNodeId);

        if (!fromNode) {
            return {
                success: false,
                error: `Source node '${params.fromNodeId}' not found`
            };
        }

        if (!toNode) {
            return {
                success: false,
                error: `Target node '${params.toNodeId}' not found`
            };
        }

        // Disconnect nodes
            (this.designer as any).disconnectNodes?.(params.fromNodeId, params.toNodeId);

        return {
            success: true
        };
    }

    /**
     * Przenieś węzeł
     */
    private moveNode(params: MoveNodeParams): ActionResult {
        const node = (this.designer as any).getNodeById(params.nodeId);
        if (!node) {
            return {
                success: false,
                error: `Node '${params.nodeId}' not found`
            };
        }

        // Move node
            (this.designer as any).moveNodeToPosition?.(params.nodeId, params.position);

        return {
            success: true,
            nodeId: params.nodeId
        };
    }

    /**
     * Załaduj konfigurację XML
     */
    private loadConfig(params: LoadConfigParams): ActionResult {
        console.log('[AgentActionsHandler] 🔧 loadConfig called');
        console.log(`[AgentActionsHandler] XML length: ${params.xml?.length || 0}`);
        
        if (!params.xml || params.xml.trim() === '') {
            console.error('[AgentActionsHandler] ❌ XML is empty');
            return {
                success: false,
                error: 'XML configuration cannot be empty'
            };
        }

        try {
            // Parse XML to VisualNode array
            console.log(`[AgentActionsHandler] Parsing XML...`);
            console.log(`[AgentActionsHandler] XML content (first 200 chars): ${params.xml.substring(0, 200)}`);
            
            const nodes = this.xmlParser.parse(params.xml);
            console.log(`[AgentActionsHandler] ✅ Parsed ${nodes.length} nodes from XML`);
            
            if (nodes.length === 0) {
                console.error('[AgentActionsHandler] ❌ Parser returned 0 nodes!');
                console.error('[AgentActionsHandler] XML was:', params.xml);
                return {
                    success: false,
                    error: 'Parser returned 0 nodes. Check XML format.'
                };
            }
            
            if (nodes.length > 0) {
                console.log(`[AgentActionsHandler] First node: id=${nodes[0].id}, type=${nodes[0].type}, attributes=${JSON.stringify(nodes[0].attributes)}`);
            }

            // Load nodes into designer (this will trigger auto-layout)
            console.log(`[AgentActionsHandler] Calling designer.loadNodes(${nodes.length} nodes)...`);
            this.designer.loadNodes(nodes);
            console.log(`[AgentActionsHandler] ✅ loadNodes completed`);
            
            // Sprawdź czy węzły są w designerze - czekaj chwilę na async operations
            setTimeout(() => {
                const designerNodes = this.designer.getNodes();
                console.log(`[AgentActionsHandler] Designer now has ${designerNodes.length} nodes (after timeout)`);
                if (designerNodes.length === 0) {
                    console.error('[AgentActionsHandler] ❌ Designer has 0 nodes after loadNodes!');
                }
            }, 100);

            return {
                success: true,
                nodeId: undefined // No single node ID for loadConfig
            };
        } catch (error) {
            console.error('[AgentActionsHandler] ❌ Failed to load config:', error);
            console.error('[AgentActionsHandler] Error stack:', error instanceof Error ? error.stack : 'No stack');
            return {
                success: false,
                error: error instanceof Error ? error.message : String(error)
            };
        }
    }

    /**
     * Zastosuj auto-layout
     */
    private applyAutoLayout(params: ApplyAutoLayoutParams): ActionResult {
        console.log(`[AgentActionsHandler] 🔧 applyAutoLayout called: ${params.layoutType}`);
        
        if (!params.layoutType || !['grid', 'vertical', 'horizontal'].includes(params.layoutType)) {
            return {
                success: false,
                error: `Invalid layout type: ${params.layoutType}. Must be 'grid', 'vertical', or 'horizontal'`
            };
        }

        try {
            // Wywołaj auto-layout w designerze
            this.designer.autoLayout(params.layoutType);
            
            console.log(`[AgentActionsHandler] ✅ Auto-layout '${params.layoutType}' applied successfully`);
            
            return {
                success: true
            };
        } catch (error) {
            console.error(`[AgentActionsHandler] ❌ Failed to apply auto-layout:`, error);
            return {
                success: false,
                error: error instanceof Error ? error.message : String(error)
            };
        }
    }

    /**
     * Generuj unikalny ID dla węzła
     */
    private generateNodeId(): string {
        return `node-${Date.now()}-${Math.random().toString(36).substr(2, 9)}`;
    }
}

