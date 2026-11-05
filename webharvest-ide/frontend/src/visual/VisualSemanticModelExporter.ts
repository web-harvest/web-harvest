/**
 * VisualSemanticModelExporter - Eksport do formatu LLM-friendly
 * 
 * Konwertuje VisualSemanticModel do formatu JSON-friendly dla agenta AI/LLM:
 * - Map → Object
 * - Set → Array
 * - Dodaje availableActions schema
 * - Formatowanie dla czytelności LLM
 */

import type {
    VisualSemanticModel,
    VisualSemanticModelForLLM,
    VisualNodeState,
    VisualConnectionState,
    VariableDependency,
    PluginSemanticInfo,
    NodePositionState,
    ConnectionPathState,
    UIState,
    MarqueeState
} from './VisualSemanticModel';

export class VisualSemanticModelExporter {
    /**
     * Eksportuj model do formatu LLM-friendly
     */
    exportForLLM(model: VisualSemanticModel): VisualSemanticModelForLLM {
        // CRITICAL: Verify that semanticSelection metadata is present before export
        let totalNodes = 0;
        let nodesWithSelectedPoints = 0;
        let totalSelectedPoints = 0;
        let totalPointsWithMetadata = 0;
        
        model.workflow.allNodes.forEach((nodeState, nodeId) => {
            totalNodes++;
            const allPoints = [...nodeState.connectionPoints.inputs, ...nodeState.connectionPoints.outputs];
            const selected = allPoints.filter(p => p.semanticSelection && p.semanticSelection.isSelected);
            const withMetadata = allPoints.filter(p => p.semanticSelection !== undefined && p.semanticSelection !== null);
            totalPointsWithMetadata += withMetadata.length;
            if (selected.length > 0) {
                nodesWithSelectedPoints++;
                totalSelectedPoints += selected.length;
            }
        });
        
        // Always log in development, but also log in production for debugging runtime tests
        const isDevelopment = process.env.NODE_ENV === 'development';
        const shouldLog = isDevelopment || (typeof window !== 'undefined' && (window as any).__DEBUG_SEMANTIC_SELECTION__);
        if (shouldLog) {
            if (totalSelectedPoints > 0) {
                console.log(`[VisualSemanticModelExporter] ✅ Exporting model with ${nodesWithSelectedPoints}/${totalNodes} nodes having ${totalSelectedPoints} selected connection points (${totalPointsWithMetadata} total points with metadata)`);
            } else {
                console.warn(`[VisualSemanticModelExporter] ⚠️ Exporting model with NO selected connection points - ${totalPointsWithMetadata} points have metadata, but none are selected`);
            }
        }
        
        return {
            version: model.version,
            schemaVersion: model.schemaVersion,
            timestamp: model.timestamp,
            workflowId: model.workflowId,

            // Available plugins (Map → Object)
            availablePlugins: this.mapToObject(model.availablePlugins),

            // Workflow state
            workflow: {
                rootNodes: model.workflow.rootNodes,
                allNodes: this.mapToObject(model.workflow.allNodes),
                connections: this.mapToObject(model.workflow.connections),
                variableDependencies: this.mapToObject(model.workflow.variableDependencies)
            },

            // Layout state
            layout: {
                viewport: model.layout.viewport,
                nodePositions: this.mapToObject(model.layout.nodePositions),
                connectionPaths: this.mapToObject(model.layout.connectionPaths)
            },

            // UI state (Set → Array)
            ui: {
                selectedNodeIds: Array.from(model.ui.selectedNodeIds),
                hoveredNodeId: model.ui.hoveredNodeId,
                expandedContainers: Array.from(model.ui.expandedContainers),
                collapsedContainers: Array.from(model.ui.collapsedContainers),
                activeTool: model.ui.activeTool,
                marqueeSelection: model.ui.marqueeSelection
            },

            // Available actions schema
            availableActions: this.getAvailableActionsSchema()
        };
    }

    /**
     * Konwertuj Map do Object
     */
    private mapToObject<K extends string | number, V>(map: Map<K, V>): { [key: string]: V } {
        const obj: { [key: string]: V } = {};
        map.forEach((value, key) => {
            obj[String(key)] = value;
        });
        return obj;
    }

    /**
     * Schema dostępnych akcji dla agenta
     */
    private getAvailableActionsSchema(): VisualSemanticModelForLLM['availableActions'] {
        return {
            addNode: {
                description: 'Add a new node to the workflow. The node will be positioned at the specified position (or auto-positioned if not provided). If parentId is provided, the node will be added as a child of that parent.',
                parameters: {
                    pluginType: 'string - The type of plugin to add (e.g., "http", "file", "xquery")',
                    position: 'Point (optional) - { x: number, y: number } - Position on canvas. If not provided, node will be auto-positioned.',
                    parentId: 'string (optional) - ID of parent node if this should be a child node',
                    attributes: 'Record<string, string> (optional) - Initial attributes for the node'
                }
            },
            updateNode: {
                description: 'Update attributes of an existing node. Only provided attributes will be updated, others remain unchanged.',
                parameters: {
                    nodeId: 'string - ID of the node to update',
                    attributes: 'Record<string, string> - Attributes to update (key-value pairs)'
                }
            },
            removeNode: {
                description: 'Remove a node from the workflow. All connections to/from this node will also be removed. If the node has children, they will be removed as well.',
                parameters: {
                    nodeId: 'string - ID of the node to remove'
                }
            },
            connectNodes: {
                description: 'Create a connection between two nodes. The connection will be routed automatically. Sequential connections are the default type.',
                parameters: {
                    fromNodeId: 'string - ID of the source node',
                    toNodeId: 'string - ID of the target node',
                    type: 'ConnectionType (optional) - "sequential" | "conditional" | "error" (default: "sequential")'
                }
            },
            disconnectNodes: {
                description: 'Remove a connection between two nodes.',
                parameters: {
                    fromNodeId: 'string - ID of the source node',
                    toNodeId: 'string - ID of the target node'
                }
            },
            moveNode: {
                description: 'Move a node to a new position on the canvas. The node will be marked as manually positioned.',
                parameters: {
                    nodeId: 'string - ID of the node to move',
                    position: 'Point - { x: number, y: number } - New position on canvas'
                }
            },
            loadConfig: {
                description: 'Load a full XML configuration into the designer. This will replace the current workflow.',
                parameters: {
                    xml: 'string - The full XML configuration string'
                }
            },
            applyAutoLayout: {
                description: 'Apply automatic layout to all nodes. Grid layout arranges nodes in a square grid. Vertical layout arranges nodes in a single vertical column. Horizontal layout arranges nodes in a single horizontal row. Helper nodes (var, def) are positioned separately from main flow nodes.',
                parameters: {
                    layoutType: 'string - Layout type: "grid" | "vertical" | "horizontal"'
                }
            },
            startExecution: {
                description: 'Start execution of the current workflow configuration. Returns execution state with executionId.',
                parameters: {
                    configName: 'string (optional) - Name for this execution',
                    waitForWebSocket: 'boolean (optional) - Whether to wait for WebSocket connection (default: true)'
                }
            },
            stopExecution: {
                description: 'Stop a running execution. Execution cannot be resumed after stopping.',
                parameters: {
                    executionId: 'string - ID of the execution to stop'
                }
            },
            pauseExecution: {
                description: 'Pause a running execution. Execution can be resumed later.',
                parameters: {
                    executionId: 'string - ID of the execution to pause'
                }
            },
            resumeExecution: {
                description: 'Resume a paused execution. Execution continues from where it was paused.',
                parameters: {
                    executionId: 'string - ID of the execution to resume'
                }
            },
            getExecutionLogs: {
                description: 'Get execution logs. Can filter by level, nodeId, and limit results.',
                parameters: {
                    executionId: 'string - ID of the execution',
                    fromIndex: 'number (optional) - Start index for logs',
                    limit: 'number (optional) - Maximum number of logs to return',
                    level: 'string (optional) - Filter by log level: "DEBUG" | "INFO" | "WARN" | "ERROR"',
                    nodeId: 'string (optional) - Filter logs for specific node'
                }
            },
            getExecutionResults: {
                description: 'Get execution results (variables produced by execution).',
                parameters: {
                    executionId: 'string - ID of the execution',
                    variableNames: 'string[] (optional) - Specific variables to retrieve, null = all variables'
                }
            },
            playbackLogs: {
                description: 'Playback execution logs with timing simulation. Useful for debugging and understanding execution flow.',
                parameters: {
                    executionId: 'string - ID of the execution',
                    fromIndex: 'number (optional) - Start index for playback',
                    toIndex: 'number (optional) - End index for playback',
                    speed: 'number (optional) - Playback speed multiplier (1.0 = normal, 2.0 = 2x speed)',
                    onLog: 'function (optional) - Callback function called for each log during playback'
                }
            },
            stopPlayback: {
                description: 'Stop current log playback if one is running.',
                parameters: {
                    // No parameters - stops current playback
                }
            }
        };
    }
}


