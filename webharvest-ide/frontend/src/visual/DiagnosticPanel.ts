/**
 * DiagnosticPanel - UI Panel do wyświetlania diagnostyki
 * 
 * Wyświetla:
 * - Stan połączenia
 * - Stan modelu
 * - Historia akcji
 * - Błędy i ostrzeżenia
 * - Metryki wydajności
 * - Health status
 */

import type { DiagnosticInfo } from './DiagnosticInterface';

export class DiagnosticPanel {
    private container: HTMLElement;
    private diagnosticInfo: DiagnosticInfo | null = null;
    private updateInterval: number | null = null;
    
    constructor(container: HTMLElement) {
        this.container = container;
        this.render();
    }
    
    /**
     * Zaktualizuj informacje diagnostyczne
     */
    update(diagnosticInfo: DiagnosticInfo): void {
        this.diagnosticInfo = diagnosticInfo;
        this.render();
    }
    
    /**
     * Renderuj panel
     */
    private render(): void {
        if (!this.diagnosticInfo) {
            this.container.innerHTML = '<div class="diagnostic-panel-loading">Loading diagnostics...</div>';
            return;
        }
        
        const info = this.diagnosticInfo;
        
        this.container.innerHTML = `
            <div class="diagnostic-panel">
                <div class="diagnostic-panel-header">
                    <h3>🔍 Diagnostics</h3>
                    <button class="diagnostic-panel-refresh" onclick="location.reload()">🔄 Refresh</button>
                </div>
                
                <!-- Health Status -->
                <div class="diagnostic-section">
                    <h4>Health Status</h4>
                    <div class="diagnostic-health health-${info.health.status}">
                        <span class="health-indicator"></span>
                        <span class="health-status">${info.health.status.toUpperCase()}</span>
                        ${info.health.issues.length > 0 ? `
                            <ul class="health-issues">
                                ${info.health.issues.map(issue => `<li>${issue}</li>`).join('')}
                            </ul>
                        ` : ''}
                    </div>
                </div>
                
                <!-- Connection State -->
                <div class="diagnostic-section">
                    <h4>Connection</h4>
                    <div class="diagnostic-connection">
                        <div class="connection-item">
                            <span class="connection-label">WebSocket:</span>
                            <span class="connection-value ${info.connection.websocket.connected ? 'connected' : 'disconnected'}">
                                ${info.connection.websocket.connected ? '✅ Connected' : '❌ Disconnected'}
                            </span>
                        </div>
                        ${info.connection.websocket.lastConnectTime ? `
                            <div class="connection-item">
                                <span class="connection-label">Last Connect:</span>
                                <span class="connection-value">${new Date(info.connection.websocket.lastConnectTime).toLocaleTimeString()}</span>
                            </div>
                        ` : ''}
                        ${info.connection.websocket.reconnectAttempts > 0 ? `
                            <div class="connection-item">
                                <span class="connection-label">Reconnect Attempts:</span>
                                <span class="connection-value">${info.connection.websocket.reconnectAttempts}</span>
                            </div>
                        ` : ''}
                        ${info.connection.websocket.lastError ? `
                            <div class="connection-item">
                                <span class="connection-label">Last Error:</span>
                                <span class="connection-value error">${info.connection.websocket.lastError}</span>
                            </div>
                        ` : ''}
                    </div>
                </div>
                
                <!-- Model State -->
                <div class="diagnostic-section">
                    <h4>Model State</h4>
                    <div class="diagnostic-model">
                        <div class="model-item">
                            <span class="model-label">Available:</span>
                            <span class="model-value ${info.model.available ? 'available' : 'unavailable'}">
                                ${info.model.available ? '✅ Yes' : '❌ No'}
                            </span>
                        </div>
                        ${info.model.lastUpdateTime ? `
                            <div class="model-item">
                                <span class="model-label">Last Update:</span>
                                <span class="model-value">${new Date(info.model.lastUpdateTime).toLocaleTimeString()}</span>
                            </div>
                        ` : ''}
                        ${info.model.buildTime !== null ? `
                            <div class="model-item">
                                <span class="model-label">Build Time:</span>
                                <span class="model-value">${info.model.buildTime.toFixed(2)}ms</span>
                            </div>
                        ` : ''}
                        <div class="model-item">
                            <span class="model-label">Root Nodes:</span>
                            <span class="model-value">${info.model.size.rootNodes}</span>
                        </div>
                        <div class="model-item">
                            <span class="model-label">Total Nodes:</span>
                            <span class="model-value">${info.model.size.totalNodes}</span>
                        </div>
                        <div class="model-item">
                            <span class="model-label">Connections:</span>
                            <span class="model-value">${info.model.size.connections}</span>
                        </div>
                        ${info.model.size.jsonSize > 0 ? `
                            <div class="model-item">
                                <span class="model-label">JSON Size:</span>
                                <span class="model-value">${(info.model.size.jsonSize / 1024).toFixed(2)} KB</span>
                            </div>
                        ` : ''}
                        <div class="model-health">
                            <div class="model-health-item ${info.model.health.hasPositions ? 'ok' : 'error'}">
                                Positions: ${info.model.health.hasPositions ? '✅' : '❌'}
                            </div>
                            <div class="model-health-item ${!info.model.health.hasCollisions ? 'ok' : 'error'}">
                                Collisions: ${!info.model.health.hasCollisions ? '✅' : '❌'}
                            </div>
                            <div class="model-health-item ${!info.model.health.hasInvalidConnections ? 'ok' : 'error'}">
                                Connections: ${!info.model.health.hasInvalidConnections ? '✅' : '❌'}
                            </div>
                            <div class="model-health-item ${!info.model.health.hasVariableErrors ? 'ok' : 'error'}">
                                Variables: ${!info.model.health.hasVariableErrors ? '✅' : '❌'}
                            </div>
                        </div>
                    </div>
                </div>
                
                <!-- Agent Actions -->
                <div class="diagnostic-section">
                    <h4>Agent Actions</h4>
                    <div class="diagnostic-actions">
                        <div class="actions-stats">
                            <div class="actions-stat">
                                <span class="stat-label">Total:</span>
                                <span class="stat-value">${info.actions.total}</span>
                            </div>
                            <div class="actions-stat">
                                <span class="stat-label">Successful:</span>
                                <span class="stat-value success">${info.actions.successful}</span>
                            </div>
                            <div class="actions-stat">
                                <span class="stat-label">Failed:</span>
                                <span class="stat-value error">${info.actions.failed}</span>
                            </div>
                        </div>
                        ${info.actions.lastAction.type ? `
                            <div class="actions-last">
                                <div class="last-action-label">Last Action:</div>
                                <div class="last-action-details">
                                    <span class="last-action-type">${info.actions.lastAction.type}</span>
                                    <span class="last-action-status ${info.actions.lastAction.success ? 'success' : 'error'}">
                                        ${info.actions.lastAction.success ? '✅' : '❌'}
                                    </span>
                                    ${info.actions.lastAction.timestamp ? `
                                        <span class="last-action-time">${new Date(info.actions.lastAction.timestamp).toLocaleTimeString()}</span>
                                    ` : ''}
                                </div>
                                ${info.actions.lastAction.error ? `
                                    <div class="last-action-error">${info.actions.lastAction.error}</div>
                                ` : ''}
                            </div>
                        ` : ''}
                        ${info.actions.history.length > 0 ? `
                            <div class="actions-history">
                                <div class="history-header">Recent Actions (last ${Math.min(10, info.actions.history.length)})</div>
                                <div class="history-list">
                                    ${info.actions.history.slice(-10).reverse().map(action => `
                                        <div class="history-item ${action.success ? 'success' : 'error'}">
                                            <span class="history-type">${action.type}</span>
                                            <span class="history-status">${action.success ? '✅' : '❌'}</span>
                                            <span class="history-time">${new Date(action.timestamp).toLocaleTimeString()}</span>
                                            <span class="history-duration">${action.duration.toFixed(0)}ms</span>
                                            ${action.error ? `
                                                <div class="history-error">${action.error}</div>
                                            ` : ''}
                                        </div>
                                    `).join('')}
                                </div>
                            </div>
                        ` : ''}
                    </div>
                </div>
                
                <!-- Performance Metrics -->
                <div class="diagnostic-section">
                    <h4>Performance</h4>
                    <div class="diagnostic-performance">
                        ${info.performance.averageModelBuildTime > 0 ? `
                            <div class="performance-item">
                                <span class="performance-label">Avg Model Build:</span>
                                <span class="performance-value">${info.performance.averageModelBuildTime.toFixed(2)}ms</span>
                            </div>
                        ` : ''}
                        ${info.performance.averageRenderTime > 0 ? `
                            <div class="performance-item">
                                <span class="performance-label">Avg Render:</span>
                                <span class="performance-value">${info.performance.averageRenderTime.toFixed(2)}ms</span>
                            </div>
                        ` : ''}
                        ${info.performance.averageWebSocketLatency > 0 ? `
                            <div class="performance-item">
                                <span class="performance-label">Avg WebSocket Latency:</span>
                                <span class="performance-value">${info.performance.averageWebSocketLatency.toFixed(2)}ms</span>
                            </div>
                        ` : ''}
                    </div>
                </div>
                
                <!-- Errors and Warnings -->
                ${info.errors.length > 0 ? `
                    <div class="diagnostic-section">
                        <h4>Recent Errors & Warnings</h4>
                        <div class="diagnostic-errors">
                            ${info.errors.slice(-10).reverse().map(error => `
                                <div class="error-item error-${error.type}">
                                    <span class="error-time">${new Date(error.timestamp).toLocaleTimeString()}</span>
                                    <span class="error-message">${error.message}</span>
                                    ${error.context ? `
                                        <div class="error-context">${JSON.stringify(error.context, null, 2)}</div>
                                    ` : ''}
                                </div>
                            `).join('')}
                        </div>
                    </div>
                ` : ''}
            </div>
        `;
    }
    
    /**
     * Rozpocznij automatyczne odświeżanie
     */
    startAutoRefresh(interval: number = 1000): void {
        this.stopAutoRefresh();
        this.updateInterval = window.setInterval(() => {
            // Panel będzie aktualizowany przez update()
        }, interval);
    }
    
    /**
     * Zatrzymaj automatyczne odświeżanie
     */
    stopAutoRefresh(): void {
        if (this.updateInterval !== null) {
            clearInterval(this.updateInterval);
            this.updateInterval = null;
        }
    }
    
    /**
     * Zniszcz panel
     */
    destroy(): void {
        this.stopAutoRefresh();
        this.container.innerHTML = '';
    }
}










