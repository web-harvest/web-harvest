/**
 * VisualModelWebSocketManager - Frontend WebSocket Manager dla Visual Semantic Model
 * 
 * Zarządza połączeniem WebSocket z backendem dla:
 * - Wysyłania updates modelu do backendu
 * - Odbierania akcji od agenta AI
 * - Real-time synchronization
 */

import type { VisualSemanticModel, VisualSemanticModelForLLM, AgentAction } from '../visual/VisualSemanticModel';
import { VisualSemanticModelExporter } from '../visual/VisualSemanticModelExporter';

export class VisualModelWebSocketManager {
    private ws: WebSocket | null = null;
    private reconnectAttempts = 0;
    private maxReconnectAttempts = 5;
    private reconnectDelay = 1000; // Start with 1 second
    private reconnectTimer: number | null = null;
    private isManualDisconnect = false;
    private agentActionCallbacks: Set<(action: AgentAction) => void> = new Set();
    private connectionChangeCallbacks: Set<(connected: boolean) => void> = new Set();
    private exporter: VisualSemanticModelExporter;

    constructor() {
        this.exporter = new VisualSemanticModelExporter();
    }

    /**
     * Połącz z backendem przez WebSocket
     */
    connect(): Promise<void> {
        return new Promise((resolve, reject) => {
            if (this.ws && this.ws.readyState === WebSocket.OPEN) {
                console.log('[VisualModelWebSocketManager] Already connected');
                resolve();
                return;
            }

            this.isManualDisconnect = false;

            try {
                // Determine WebSocket URL
                const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
                const host = window.location.host;
                const wsUrl = `${protocol}//${host}/ws/visual/model?clientType=frontend`;

                console.log('[VisualModelWebSocketManager] 🔌 Attempting to connect to:', wsUrl);
                console.log('[VisualModelWebSocketManager] Current location:', window.location.href);

                this.ws = new WebSocket(wsUrl);

                this.ws.onopen = () => {
                    console.log('[VisualModelWebSocketManager] ✅✅✅ Connected to backend as FRONTEND');
                    console.log('[VisualModelWebSocketManager] WebSocket readyState:', this.ws?.readyState);
                    this.reconnectAttempts = 0;
                    this.reconnectDelay = 1000;
                    this.notifyConnectionChange(true);
                    resolve();
                };

                this.ws.onmessage = (event) => {
                    this.handleMessage(event.data);
                };

                this.ws.onerror = (error) => {
                    console.error('[VisualModelWebSocketManager] WebSocket error:', error);
                    this.notifyConnectionChange(false);
                    if (this.reconnectAttempts === 0) {
                        reject(error);
                    }
                };

                this.ws.onclose = (event) => {
                    console.log('[VisualModelWebSocketManager] WebSocket closed:', event.code, event.reason);
                    this.notifyConnectionChange(false);
                    this.ws = null;

                    // Auto-reconnect if not manual disconnect
                    if (!this.isManualDisconnect && this.reconnectAttempts < this.maxReconnectAttempts) {
                        this.scheduleReconnect();
                    }
                };
            } catch (error) {
                console.error('[VisualModelWebSocketManager] Failed to create WebSocket:', error);
                reject(error);
            }
        });
    }

    /**
     * Rozłącz WebSocket
     */
    disconnect(): void {
        this.isManualDisconnect = true;
        if (this.reconnectTimer) {
            clearTimeout(this.reconnectTimer);
            this.reconnectTimer = null;
        }
        if (this.ws) {
            this.ws.close();
            this.ws = null;
        }
    }

    /**
     * Wyślij update modelu do backendu
     */
    sendModelUpdate(model: VisualSemanticModel): void {
        if (!this.ws || this.ws.readyState !== WebSocket.OPEN) {
            console.warn('[VisualModelWebSocketManager] ⚠️  Cannot send model update - WebSocket not connected');
            return;
        }

        console.log('[VisualModelWebSocketManager] 🔄 Exporting model for LLM...');
        try {
            // CRITICAL: Verify semanticSelection metadata before export
            let totalSelectedBeforeExport = 0;
            let totalPointsBeforeExport = 0;
            model.workflow.allNodes.forEach((nodeState, nodeId) => {
                const allPoints = [...nodeState.connectionPoints.inputs, ...nodeState.connectionPoints.outputs];
                totalPointsBeforeExport += allPoints.length;
                const selected = allPoints.filter(p => p.semanticSelection && p.semanticSelection.isSelected);
                totalSelectedBeforeExport += selected.length;
            });
            
            const shouldLog = process.env.NODE_ENV === 'development' || (typeof window !== 'undefined' && (window as any).__DEBUG_SEMANTIC_SELECTION__);
            if (shouldLog) {
                console.log(`[VisualModelWebSocketManager] Before export: ${totalSelectedBeforeExport}/${totalPointsBeforeExport} connection points have semanticSelection.isSelected=true`);
            }
            
            // Convert to LLM-friendly format
            const llmModel = this.exporter.exportForLLM(model);
            
            // CRITICAL: Verify semanticSelection metadata after export (before JSON.stringify)
            let totalSelectedAfterExport = 0;
            let totalPointsAfterExport = 0;
            Object.values(llmModel.workflow.allNodes).forEach((nodeState: any) => {
                const allPoints = [...(nodeState.connectionPoints?.inputs || []), ...(nodeState.connectionPoints?.outputs || [])];
                totalPointsAfterExport += allPoints.length;
                const selected = allPoints.filter((p: any) => p.semanticSelection && p.semanticSelection.isSelected);
                totalSelectedAfterExport += selected.length;
            });
            
            if (shouldLog) {
                console.log(`[VisualModelWebSocketManager] After export: ${totalSelectedAfterExport}/${totalPointsAfterExport} connection points have semanticSelection.isSelected=true`);
                if (totalSelectedBeforeExport > 0 && totalSelectedAfterExport === 0) {
                    console.error(`[VisualModelWebSocketManager] ❌ CRITICAL: semanticSelection metadata lost during export! Had ${totalSelectedBeforeExport} selected points before, 0 after`);
                }
            }
            
            console.log(`[VisualModelWebSocketManager] ✅ Model exported: ${llmModel.workflow.rootNodes.length} root nodes, ${Object.keys(llmModel.workflow.allNodes).length} total nodes`);

            const message = {
                type: 'modelUpdate',
                model: llmModel,
                timestamp: Date.now()
            };

            const messageStr = JSON.stringify(message);
            this.ws.send(messageStr);
            console.log('[VisualModelWebSocketManager] ✅✅✅ Sent model update to backend (message size: ' + messageStr.length + ' bytes)');
        } catch (error) {
            console.error('[VisualModelWebSocketManager] ❌ Failed to send model update:', error);
        }
    }

    /**
     * Wyślij wiadomość do backendu (generic)
     */
    sendMessage(message: string): void {
        if (!this.ws || this.ws.readyState !== WebSocket.OPEN) {
            console.warn('[VisualModelWebSocketManager] Cannot send message: WebSocket not connected');
            return;
        }

        try {
            this.ws.send(message);
        } catch (error) {
            console.error('[VisualModelWebSocketManager] Failed to send message:', error);
        }
    }

    /**
     * Subskrybuj akcje od agenta
     */
    onAgentAction(callback: (action: AgentAction) => void): () => void {
        this.agentActionCallbacks.add(callback);
        return () => {
            this.agentActionCallbacks.delete(callback);
        };
    }

    /**
     * Subskrybuj zmiany połączenia
     */
    onConnectionChange(callback: (connected: boolean) => void): () => void {
        this.connectionChangeCallbacks.add(callback);
        return () => {
            this.connectionChangeCallbacks.delete(callback);
        };
    }

    /**
     * Sprawdź czy WebSocket jest połączony
     */
    isConnected(): boolean {
        return this.ws !== null && this.ws.readyState === WebSocket.OPEN;
    }

    /**
     * Obsłuż wiadomość od backendu
     */
    private handleMessage(data: string): void {
        try {
            const message = JSON.parse(data);

            switch (message.type) {
                case 'action':
                    // Akcja od agenta
                    this.handleAgentAction(message.action as AgentAction);
                    break;
                case 'modelUpdate':
                    // Update modelu (od backendu - broadcast do innych klientów)
                    console.log('[VisualModelWebSocketManager] Received model update from backend');
                    // Frontend nie potrzebuje tego - model jest budowany lokalnie
                    break;
                case 'error':
                    console.error('[VisualModelWebSocketManager] Error from backend:', message.error);
                    break;
                default:
                    console.warn('[VisualModelWebSocketManager] Unknown message type:', message.type);
            }
        } catch (error) {
            console.error('[VisualModelWebSocketManager] Failed to parse message:', error, data);
        }
    }

    /**
     * Obsłuż akcję od agenta
     */
    private handleAgentAction(action: AgentAction): void {
        console.log('[VisualModelWebSocketManager] Received agent action:', action);
        this.agentActionCallbacks.forEach(callback => {
            try {
                callback(action);
            } catch (error) {
                console.error('[VisualModelWebSocketManager] Error in agent action callback:', error);
            }
        });
    }

    /**
     * Powiadom o zmianie połączenia
     */
    private notifyConnectionChange(connected: boolean): void {
        this.connectionChangeCallbacks.forEach(callback => {
            try {
                callback(connected);
            } catch (error) {
                console.error('[VisualModelWebSocketManager] Error in connection change callback:', error);
            }
        });
    }

    /**
     * Zaplanuj ponowne połączenie
     */
    private scheduleReconnect(): void {
        if (this.reconnectTimer) {
            return; // Already scheduled
        }

        this.reconnectAttempts++;
        const delay = Math.min(this.reconnectDelay * Math.pow(2, this.reconnectAttempts - 1), 30000); // Max 30 seconds

        console.log(`[VisualModelWebSocketManager] Scheduling reconnect attempt ${this.reconnectAttempts}/${this.maxReconnectAttempts} in ${delay}ms`);

        this.reconnectTimer = window.setTimeout(() => {
            this.reconnectTimer = null;
            this.connect().catch(error => {
                console.error('[VisualModelWebSocketManager] Reconnect failed:', error);
            });
        }, delay);
    }
}

