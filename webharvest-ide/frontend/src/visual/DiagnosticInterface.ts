/**
 * DiagnosticInterface - System diagnostyczny dla Visual Semantic Model
 * 
 * Zbiera i prezentuje informacje diagnostyczne:
 * - Stan połączenia WebSocket
 * - Stan modelu (aktualność, rozmiar, węzły)
 * - Historia akcji agenta
 * - Błędy i ostrzeżenia
 * - Metryki wydajności
 * - Health checks
 */

import type { VisualSemanticModel, AgentAction, ActionResult } from './VisualSemanticModel';

export interface DiagnosticInfo {
    timestamp: number;
    version: string;
    
    // Connection state
    connection: {
        websocket: {
            connected: boolean;
            lastConnectTime: number | null;
            lastDisconnectTime: number | null;
            reconnectAttempts: number;
            lastError: string | null;
        };
        backend: {
            reachable: boolean;
            lastCheckTime: number | null;
            responseTime: number | null;
        };
    };
    
    // Model state
    model: {
        available: boolean;
        lastUpdateTime: number | null;
        buildTime: number | null; // Czas budowania modelu w ms
        size: {
            rootNodes: number;
            totalNodes: number;
            connections: number;
            variableDependencies: number;
            jsonSize: number; // Rozmiar JSON w bajtach
        };
        health: {
            hasPositions: boolean; // Czy wszystkie węzły mają pozycje
            hasCollisions: boolean; // Czy są kolizje
            hasInvalidConnections: boolean; // Czy są nieprawidłowe połączenia
            hasVariableErrors: boolean; // Czy są błędy zmiennych
        };
    };
    
    // Agent actions
    actions: {
        total: number;
        successful: number;
        failed: number;
        lastAction: {
            type: string | null;
            timestamp: number | null;
            success: boolean | null;
            error: string | null;
        };
        history: Array<{
            id: string;
            type: string;
            timestamp: number;
            success: boolean;
            error?: string;
            duration: number; // Czas wykonania w ms
        }>;
    };
    
    // Performance metrics
    performance: {
        modelBuildTime: number[]; // Historia czasów budowania modelu
        renderTime: number[]; // Historia czasów renderowania
        websocketLatency: number[]; // Historia opóźnień WebSocket
        averageModelBuildTime: number;
        averageRenderTime: number;
        averageWebSocketLatency: number;
    };
    
    // Errors and warnings
    errors: Array<{
        id: string;
        timestamp: number;
        type: 'error' | 'warning' | 'info';
        message: string;
        context?: Record<string, any>;
    }>;
    
    // Health status
    health: {
        status: 'healthy' | 'degraded' | 'unhealthy';
        issues: string[];
        lastCheckTime: number;
    };
}

export class DiagnosticInterface {
    private diagnosticInfo: DiagnosticInfo;
    private updateListeners: Set<(info: DiagnosticInfo) => void> = new Set();
    private maxHistorySize = 100;
    
    constructor() {
        this.diagnosticInfo = this.createEmptyDiagnosticInfo();
    }
    
    /**
     * Utwórz pusty obiekt diagnostyczny
     */
    private createEmptyDiagnosticInfo(): DiagnosticInfo {
        return {
            timestamp: Date.now(),
            version: '1.0.0',
            connection: {
                websocket: {
                    connected: false,
                    lastConnectTime: null,
                    lastDisconnectTime: null,
                    reconnectAttempts: 0,
                    lastError: null
                },
                backend: {
                    reachable: false,
                    lastCheckTime: null,
                    responseTime: null
                }
            },
            model: {
                available: false,
                lastUpdateTime: null,
                buildTime: null,
                size: {
                    rootNodes: 0,
                    totalNodes: 0,
                    connections: 0,
                    variableDependencies: 0,
                    jsonSize: 0
                },
                health: {
                    hasPositions: false,
                    hasCollisions: false,
                    hasInvalidConnections: false,
                    hasVariableErrors: false
                }
            },
            actions: {
                total: 0,
                successful: 0,
                failed: 0,
                lastAction: {
                    type: null,
                    timestamp: null,
                    success: null,
                    error: null
                },
                history: []
            },
            performance: {
                modelBuildTime: [],
                renderTime: [],
                websocketLatency: [],
                averageModelBuildTime: 0,
                averageRenderTime: 0,
                averageWebSocketLatency: 0
            },
            errors: [],
            health: {
                status: 'unhealthy',
                issues: [],
                lastCheckTime: Date.now()
            }
        };
    }
    
    /**
     * Zaktualizuj stan połączenia WebSocket
     */
    updateWebSocketState(connected: boolean, error?: string): void {
        const ws = this.diagnosticInfo.connection.websocket;
        
        if (connected) {
            ws.connected = true;
            ws.lastConnectTime = Date.now();
            ws.lastError = null;
        } else {
            ws.connected = false;
            ws.lastDisconnectTime = Date.now();
            if (error) {
                ws.lastError = error;
            }
        }
        
        this.updateHealth();
        this.notifyUpdate();
    }
    
    /**
     * Zaktualizuj liczbę prób ponownego połączenia
     */
    updateReconnectAttempts(attempts: number): void {
        this.diagnosticInfo.connection.websocket.reconnectAttempts = attempts;
        this.notifyUpdate();
    }
    
    /**
     * Zaktualizuj stan modelu
     */
    updateModelState(model: VisualSemanticModel | null, buildTime?: number, jsonSize?: number): void {
        const modelState = this.diagnosticInfo.model;
        
        if (model) {
            modelState.available = true;
            modelState.lastUpdateTime = Date.now();
            modelState.buildTime = buildTime || null;
            
            modelState.size = {
                rootNodes: model.workflow.rootNodes.length,
                totalNodes: model.workflow.allNodes.size,
                connections: model.workflow.connections.size,
                variableDependencies: model.workflow.variableDependencies.size,
                jsonSize: jsonSize || 0
            };
            
            // Health checks
            const allNodes = Array.from(model.workflow.allNodes.values());
            modelState.health.hasPositions = allNodes.every(n => n.position && n.position.x !== undefined && n.position.y !== undefined);
            modelState.health.hasCollisions = this.checkCollisions(allNodes);
            modelState.health.hasInvalidConnections = this.checkInvalidConnections(model);
            modelState.health.hasVariableErrors = Array.from(model.workflow.variableDependencies.values())
                .some(vd => !vd.isValid || (vd.errors && vd.errors.length > 0));
            
            // Performance metrics
            if (buildTime !== undefined) {
                this.addPerformanceMetric('modelBuildTime', buildTime);
            }
        } else {
            modelState.available = false;
        }
        
        this.updateHealth();
        this.notifyUpdate();
    }
    
    /**
     * Zarejestruj akcję agenta
     */
    recordAction(action: AgentAction, result: ActionResult, duration: number): void {
        const actions = this.diagnosticInfo.actions;
        
        actions.total++;
        if (result.success) {
            actions.successful++;
        } else {
            actions.failed++;
        }
        
        actions.lastAction = {
            type: action.type,
            timestamp: Date.now(),
            success: result.success,
            error: result.error || null
        };
        
        // Dodaj do historii
        const historyItem = {
            id: `action-${Date.now()}-${Math.random().toString(36).substr(2, 9)}`,
            type: action.type,
            timestamp: Date.now(),
            success: result.success,
            error: result.error,
            duration
        };
        
        actions.history.push(historyItem);
        
        // Ogranicz rozmiar historii
        if (actions.history.length > this.maxHistorySize) {
            actions.history.shift();
        }
        
        this.updateHealth();
        this.notifyUpdate();
    }
    
    /**
     * Zarejestruj błąd lub ostrzeżenie
     */
    recordError(type: 'error' | 'warning' | 'info', message: string, context?: Record<string, any>): void {
        const error = {
            id: `error-${Date.now()}-${Math.random().toString(36).substr(2, 9)}`,
            timestamp: Date.now(),
            type,
            message,
            context
        };
        
        this.diagnosticInfo.errors.push(error);
        
        // Ogranicz rozmiar historii błędów
        if (this.diagnosticInfo.errors.length > this.maxHistorySize) {
            this.diagnosticInfo.errors.shift();
        }
        
        this.updateHealth();
        this.notifyUpdate();
    }
    
    /**
     * Zarejestruj metrykę wydajności
     */
    addPerformanceMetric(metric: 'modelBuildTime' | 'renderTime' | 'websocketLatency', value: number): void {
        const metrics = this.diagnosticInfo.performance[metric];
        metrics.push(value);
        
        // Ogranicz rozmiar historii
        if (metrics.length > this.maxHistorySize) {
            metrics.shift();
        }
        
        // Calculate average
        const avg = metrics.reduce((sum, v) => sum + v, 0) / metrics.length;
        const avgKey = `average${metric.charAt(0).toUpperCase() + metric.slice(1)}` as 'averageModelBuildTime' | 'averageRenderTime' | 'averageWebSocketLatency';
        if (avgKey === 'averageModelBuildTime') {
            this.diagnosticInfo.performance.averageModelBuildTime = avg;
        } else if (avgKey === 'averageRenderTime') {
            this.diagnosticInfo.performance.averageRenderTime = avg;
        } else if (avgKey === 'averageWebSocketLatency') {
            this.diagnosticInfo.performance.averageWebSocketLatency = avg;
        }
        
        this.notifyUpdate();
    }
    
    /**
     * Sprawdź kolizje między węzłami
     */
    private checkCollisions(nodes: Array<{ position: { x: number; y: number }; dimensions: { width: number; height: number } }>): boolean {
        const margin = 20;
        
        for (let i = 0; i < nodes.length; i++) {
            const n1 = nodes[i];
            for (let j = i + 1; j < nodes.length; j++) {
                const n2 = nodes[j];
                
                const overlapX = !(n1.position.x + n1.dimensions.width + margin < n2.position.x || 
                                  n1.position.x - margin > n2.position.x + n2.dimensions.width);
                const overlapY = !(n1.position.y + n1.dimensions.height + margin < n2.position.y || 
                                  n1.position.y - margin > n2.position.y + n2.dimensions.height);
                
                if (overlapX && overlapY) {
                    return true;
                }
            }
        }
        
        return false;
    }
    
    /**
     * Sprawdź nieprawidłowe połączenia
     */
    private checkInvalidConnections(model: VisualSemanticModel): boolean {
        const nodeIds = new Set(Array.from(model.workflow.allNodes.keys()));
        
        for (const conn of model.workflow.connections.values()) {
            if (!nodeIds.has(conn.fromNodeId) || !nodeIds.has(conn.toNodeId)) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Zaktualizuj health status
     */
    private updateHealth(): void {
        const issues: string[] = [];
        const health = this.diagnosticInfo.health;
        
        // Sprawdź połączenie
        if (!this.diagnosticInfo.connection.websocket.connected) {
            issues.push('WebSocket not connected');
        }
        
        // Sprawdź model
        if (!this.diagnosticInfo.model.available) {
            issues.push('Model not available');
        }
        
        // Sprawdź health modelu
        if (this.diagnosticInfo.model.health.hasCollisions) {
            issues.push('Nodes have collisions');
        }
        if (this.diagnosticInfo.model.health.hasInvalidConnections) {
            issues.push('Invalid connections detected');
        }
        if (this.diagnosticInfo.model.health.hasVariableErrors) {
            issues.push('Variable dependency errors');
        }
        
        // Sprawdź błędy
        const recentErrors = this.diagnosticInfo.errors.filter(e => 
            e.type === 'error' && Date.now() - e.timestamp < 60000 // Ostatnia minuta
        );
        if (recentErrors.length > 5) {
            issues.push(`Too many recent errors: ${recentErrors.length}`);
        }
        
        health.issues = issues;
        health.lastCheckTime = Date.now();
        
        // Określ status
        if (issues.length === 0) {
            health.status = 'healthy';
        } else if (issues.length <= 2) {
            health.status = 'degraded';
        } else {
            health.status = 'unhealthy';
        }
    }
    
    /**
     * Pobierz aktualne informacje diagnostyczne
     */
    getDiagnosticInfo(): DiagnosticInfo {
        this.diagnosticInfo.timestamp = Date.now();
        return { ...this.diagnosticInfo };
    }
    
    /**
     * Subskrybuj zmiany diagnostyczne
     */
    subscribe(listener: (info: DiagnosticInfo) => void): () => void {
        this.updateListeners.add(listener);
        return () => {
            this.updateListeners.delete(listener);
        };
    }
    
    /**
     * Powiadom listenerów o zmianie
     */
    private notifyUpdate(): void {
        this.diagnosticInfo.timestamp = Date.now();
        this.updateListeners.forEach(listener => {
            try {
                listener(this.getDiagnosticInfo());
            } catch (error) {
                console.error('[DiagnosticInterface] Error in update listener:', error);
            }
        });
    }
    
    /**
     * Wyczyść historię
     */
    clearHistory(): void {
        this.diagnosticInfo.actions.history = [];
        this.diagnosticInfo.errors = [];
        this.diagnosticInfo.performance.modelBuildTime = [];
        this.diagnosticInfo.performance.renderTime = [];
        this.diagnosticInfo.performance.websocketLatency = [];
        this.notifyUpdate();
    }
}

