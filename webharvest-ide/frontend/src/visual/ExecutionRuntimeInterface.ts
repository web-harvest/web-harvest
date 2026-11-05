/**
 * ExecutionRuntimeInterface - Interfejs do kontrolowania runtime przez LLM
 * 
 * Zapewnia dostęp do:
 * - Uruchamiania/zatrzymywania wykonania
 * - Pobierania logów i wyników
 * - Odgrywania logów (playback)
 * - Stanu wykonania węzłów
 */

import type {
    ExecutionState,
    ExecutionLog,
    NodeExecutionState,
    GetExecutionLogsParams,
    GetExecutionResultsParams,
    PlaybackLogsParams
} from './VisualSemanticModel';

export interface ExecutionRuntimeInterface {
    /**
     * Uruchom wykonanie aktualnej konfiguracji
     */
    startExecution(configName?: string, waitForWebSocket?: boolean): Promise<ExecutionState>;

    /**
     * Zatrzymaj wykonanie
     */
    stopExecution(executionId: string): Promise<boolean>;

    /**
     * Wstrzymaj wykonanie
     */
    pauseExecution(executionId: string): Promise<boolean>;

    /**
     * Wznów wykonanie
     */
    resumeExecution(executionId: string): Promise<boolean>;

    /**
     * Pobierz logi wykonania
     */
    getExecutionLogs(params: GetExecutionLogsParams): Promise<ExecutionLog[]>;

    /**
     * Pobierz wyniki wykonania
     */
    getExecutionResults(params: GetExecutionResultsParams): Promise<Map<string, string>>;

    /**
     * Pobierz pełny stan wykonania
     */
    getExecutionState(executionId: string): Promise<ExecutionState | null>;

    /**
     * Pobierz stan wykonania dla konkretnego węzła
     */
    getNodeExecutionState(executionId: string, nodeId: string): Promise<NodeExecutionState | null>;

    /**
     * Odgryj logi wykonania (playback)
     */
    playbackLogs(params: PlaybackLogsParams): Promise<void>;

    /**
     * Zatrzymaj odgrywanie logów
     */
    stopPlayback(): Promise<void>;

    /**
     * Subskrybuj na aktualizacje stanu wykonania
     */
    subscribeToExecutionUpdates(
        executionId: string,
        callback: (state: ExecutionState) => void
    ): () => void; // Zwraca funkcję unsubscribe

    /**
     * Subskrybuj na logi wykonania w czasie rzeczywistym
     */
    subscribeToExecutionLogs(
        executionId: string,
        callback: (log: ExecutionLog) => void
    ): () => void; // Zwraca funkcję unsubscribe
}

/**
 * Implementacja ExecutionRuntimeInterface używająca REST API i WebSocket
 */
export class ExecutionRuntimeManager implements ExecutionRuntimeInterface {
    private baseUrl: string;
    private wsUrl: string;
    private ws: WebSocket | null = null;
    private executionSubscriptions: Map<string, Set<(state: ExecutionState) => void>> = new Map();
    private logSubscriptions: Map<string, Set<(log: ExecutionLog) => void>> = new Map();
    private playbackTimer: number | null = null;
    private currentPlayback: {
        executionId: string;
        logs: ExecutionLog[];
        currentIndex: number;
        speed: number;
        onLog?: (log: ExecutionLog) => void;
    } | null = null;

    constructor(baseUrl: string = '') {
        this.baseUrl = baseUrl || window.location.origin;
        const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
        this.wsUrl = `${protocol}//${window.location.host}/ws/execution`;
    }

    async startExecution(configName?: string, waitForWebSocket: boolean = true): Promise<ExecutionState> {
        // Pobierz aktualną konfigurację XML z VisualDesigner
        // TODO: Integracja z VisualDesigner do pobrania XML
        const configXml = ''; // Placeholder - będzie pobierane z VisualDesigner
        
        const response = await fetch(`${this.baseUrl}/api/execute`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                configXml,
                configName: configName || 'visual-workflow',
                waitForWebSocket
            })
        });

        if (!response.ok) {
            throw new Error(`Failed to start execution: ${response.statusText}`);
        }

        const data = await response.json();
        const executionId = data.executionId;

        // Połącz WebSocket dla real-time updates
        if (waitForWebSocket) {
            await this.connectWebSocket(executionId);
        }

        // Pobierz początkowy stan
        return await this.getExecutionState(executionId) || {
            executionId,
            configName: configName || 'visual-workflow',
            status: 'PENDING',
            progress: 0,
            startTime: null,
            endTime: null,
            duration: null,
            logs: [],
            logCount: 0,
            results: new Map(),
            resultCount: 0,
            nodeStates: new Map()
        };
    }

    async stopExecution(executionId: string): Promise<boolean> {
        const response = await fetch(`${this.baseUrl}/api/execution/${executionId}/stop`, {
            method: 'POST'
        });

        if (!response.ok) {
            throw new Error(`Failed to stop execution: ${response.statusText}`);
        }

        const data = await response.json();
        return data.success === true;
    }

    async pauseExecution(executionId: string): Promise<boolean> {
        const response = await fetch(`${this.baseUrl}/api/execution/${executionId}/pause`, {
            method: 'POST'
        });

        if (!response.ok) {
            throw new Error(`Failed to pause execution: ${response.statusText}`);
        }

        const data = await response.json();
        return data.success === true;
    }

    async resumeExecution(executionId: string): Promise<boolean> {
        const response = await fetch(`${this.baseUrl}/api/execution/${executionId}/resume`, {
            method: 'POST'
        });

        if (!response.ok) {
            throw new Error(`Failed to resume execution: ${response.statusText}`);
        }

        const data = await response.json();
        return data.success === true;
    }

    async getExecutionLogs(params: GetExecutionLogsParams): Promise<ExecutionLog[]> {
        let url = `${this.baseUrl}/api/execution/${params.executionId}/logs`;
        const queryParams: string[] = [];
        
        if (params.fromIndex !== undefined) {
            queryParams.push(`fromIndex=${params.fromIndex}`);
        }
        if (params.limit !== undefined) {
            queryParams.push(`limit=${params.limit}`);
        }
        if (params.level) {
            queryParams.push(`level=${params.level}`);
        }
        if (params.nodeId) {
            queryParams.push(`nodeId=${params.nodeId}`);
        }
        
        if (queryParams.length > 0) {
            url += '?' + queryParams.join('&');
        }

        const response = await fetch(url);
        if (!response.ok) {
            throw new Error(`Failed to get logs: ${response.statusText}`);
        }

        const data = await response.json();
        return (data.logs || []).map((log: any) => ({
            timestamp: log.timestamp || Date.now(),
            level: log.level || 'INFO',
            message: log.message || '',
            nodeId: log.nodeId,
            executionId: params.executionId
        }));
    }

    async getExecutionResults(params: GetExecutionResultsParams): Promise<Map<string, string>> {
        let url = `${this.baseUrl}/api/execution/${params.executionId}/results`;
        
        if (params.variableNames && params.variableNames.length > 0) {
            url += '?variables=' + params.variableNames.join(',');
        }

        const response = await fetch(url);
        if (!response.ok) {
            throw new Error(`Failed to get results: ${response.statusText}`);
        }

        const data = await response.json();
        const results = new Map<string, string>();
        
        if (data.results) {
            Object.entries(data.results).forEach(([key, value]) => {
                results.set(key, String(value));
            });
        }

        return results;
    }

    async getExecutionState(executionId: string): Promise<ExecutionState | null> {
        try {
            const [infoResponse, logsResponse, resultsResponse, sessionResponse] = await Promise.all([
                fetch(`${this.baseUrl}/api/execution/${executionId}/info`),
                fetch(`${this.baseUrl}/api/execution/${executionId}/logs`),
                fetch(`${this.baseUrl}/api/execution/${executionId}/results`),
                fetch(`${this.baseUrl}/api/execution/${executionId}/session`)
            ]);

            if (!infoResponse.ok) {
                return null;
            }

            const info = await infoResponse.json();
            const logs = logsResponse.ok ? await logsResponse.json() : { logs: [] };
            const results = resultsResponse.ok ? await resultsResponse.json() : { results: {} };
            const session = sessionResponse.ok ? await sessionResponse.json() : { session: null, tokens: null };

            const executionLogs: ExecutionLog[] = (logs.logs || []).map((log: any) => ({
                timestamp: log.timestamp || Date.now(),
                level: log.level || 'INFO',
                message: log.message || '',
                nodeId: log.nodeId,
                executionId
            }));

            const resultsMap = new Map<string, string>();
            if (results.results) {
                Object.entries(results.results).forEach(([key, value]) => {
                    resultsMap.set(key, String(value));
                });
            }

            return {
                executionId,
                configName: info.configName || 'unknown',
                status: info.status || 'UNKNOWN',
                progress: info.progress || 0,
                startTime: session.session?.startTime || null,
                endTime: session.session?.endTime || null,
                duration: session.session?.duration || null,
                session: session.session ? {
                    sessionId: session.session.sessionId,
                    clientId: session.session.clientId || '',
                    status: session.session.status || 'UNKNOWN',
                    startTime: session.session.startTime,
                    endTime: session.session.endTime || null,
                    duration: session.session.duration || 0,
                    elementsProcessed: session.session.elementsProcessed || 0,
                    processingRate: session.session.processingRate || 0
                } : undefined,
                tokens: session.tokens ? {
                    httpRequests: session.tokens.httpRequests || 0,
                    httpBytes: session.tokens.httpBytes || 0,
                    cpuTimeMs: session.tokens.cpuTimeMs || 0,
                    memoryPeakBytes: session.tokens.memoryPeakBytes || 0
                } : undefined,
                logs: executionLogs,
                logCount: executionLogs.length,
                results: resultsMap,
                resultCount: resultsMap.size,
                nodeStates: new Map() // TODO: Pobierz z backendu jeśli dostępne
            };
        } catch (error) {
            console.error('[ExecutionRuntimeManager] Failed to get execution state:', error);
            return null;
        }
    }

    async getNodeExecutionState(executionId: string, nodeId: string): Promise<NodeExecutionState | null> {
        // TODO: Implementacja gdy backend będzie wspierał node-level state
        // Na razie filtrujemy logi dla konkretnego węzła
        const logs = await this.getExecutionLogs({ executionId, nodeId });
        
        if (logs.length === 0) {
            return null;
        }

        const errorLog = logs.find(log => log.level === 'ERROR');
        
        return {
            nodeId,
            status: errorLog ? 'failed' : 'completed',
            startTime: logs[0]?.timestamp || null,
            endTime: logs[logs.length - 1]?.timestamp || null,
            duration: logs.length > 1 ? (logs[logs.length - 1].timestamp - logs[0].timestamp) : null,
            logs,
            variables: new Map(), // TODO: Pobierz z backendu
            error: errorLog?.message || null
        };
    }

    async playbackLogs(params: PlaybackLogsParams): Promise<void> {
        // Zatrzymaj poprzedni playback jeśli istnieje
        await this.stopPlayback();

        // Pobierz logi
        const logs = await this.getExecutionLogs({
            executionId: params.executionId,
            fromIndex: params.fromIndex,
            limit: params.toIndex !== undefined ? (params.toIndex - (params.fromIndex || 0)) : undefined
        });

        if (logs.length === 0) {
            return;
        }

        const speed = params.speed || 1.0;
        this.currentPlayback = {
            executionId: params.executionId,
            logs,
            currentIndex: 0,
            speed,
            onLog: params.onLog
        };

        // Rozpocznij playback
        this.playNextLog();
    }

    private playNextLog(): void {
        if (!this.currentPlayback) {
            return;
        }

        const { logs, currentIndex, speed, onLog } = this.currentPlayback;

        if (currentIndex >= logs.length) {
            // Playback zakończony
            this.currentPlayback = null;
            return;
        }

        const log = logs[currentIndex];
        if (onLog) {
            onLog(log);
        }

        this.currentPlayback.currentIndex++;

        // Oblicz delay do następnego loga (symulacja czasu rzeczywistego)
        const delay = currentIndex < logs.length - 1
            ? Math.max(100, (logs[currentIndex + 1].timestamp - log.timestamp) / speed)
            : 100;

        this.playbackTimer = window.setTimeout(() => {
            this.playNextLog();
        }, delay);
    }

    async stopPlayback(): Promise<void> {
        if (this.playbackTimer !== null) {
            clearTimeout(this.playbackTimer);
            this.playbackTimer = null;
        }
        this.currentPlayback = null;
    }

    subscribeToExecutionUpdates(
        executionId: string,
        callback: (state: ExecutionState) => void
    ): () => void {
        if (!this.executionSubscriptions.has(executionId)) {
            this.executionSubscriptions.set(executionId, new Set());
        }
        this.executionSubscriptions.get(executionId)!.add(callback);

        // Połącz WebSocket jeśli nie jest połączony
        this.connectWebSocket(executionId);

        // Zwróć funkcję unsubscribe
        return () => {
            const callbacks = this.executionSubscriptions.get(executionId);
            if (callbacks) {
                callbacks.delete(callback);
                if (callbacks.size === 0) {
                    this.executionSubscriptions.delete(executionId);
                }
            }
        };
    }

    subscribeToExecutionLogs(
        executionId: string,
        callback: (log: ExecutionLog) => void
    ): () => void {
        if (!this.logSubscriptions.has(executionId)) {
            this.logSubscriptions.set(executionId, new Set());
        }
        this.logSubscriptions.get(executionId)!.add(callback);

        // Połącz WebSocket jeśli nie jest połączony
        this.connectWebSocket(executionId);

        // Zwróć funkcję unsubscribe
        return () => {
            const callbacks = this.logSubscriptions.get(executionId);
            if (callbacks) {
                callbacks.delete(callback);
                if (callbacks.size === 0) {
                    this.logSubscriptions.delete(executionId);
                }
            }
        };
    }

    private async connectWebSocket(executionId: string): Promise<void> {
        if (this.ws && this.ws.readyState === WebSocket.OPEN) {
            return; // Już połączony
        }

        return new Promise((resolve, reject) => {
            const url = `${this.wsUrl}?id=${executionId}`;
            this.ws = new WebSocket(url);

            this.ws.onopen = () => {
                console.log('[ExecutionRuntimeManager] WebSocket connected for execution:', executionId);
                resolve();
            };

            this.ws.onmessage = (event) => {
                try {
                    const data = JSON.parse(event.data);
                    this.handleWebSocketMessage(executionId, data);
                } catch (error) {
                    console.error('[ExecutionRuntimeManager] Failed to parse WebSocket message:', error);
                }
            };

            this.ws.onerror = (error) => {
                console.error('[ExecutionRuntimeManager] WebSocket error:', error);
                reject(error);
            };

            this.ws.onclose = () => {
                console.log('[ExecutionRuntimeManager] WebSocket closed');
                this.ws = null;
            };
        });
    }

    private handleWebSocketMessage(executionId: string, data: any): void {
        if (data.type === 'log') {
            const log: ExecutionLog = {
                timestamp: data.timestamp || Date.now(),
                level: data.level || 'INFO',
                message: data.message || '',
                nodeId: data.nodeId,
                executionId
            };

            const callbacks = this.logSubscriptions.get(executionId);
            if (callbacks) {
                callbacks.forEach(cb => cb(log));
            }
        } else if (data.type === 'status') {
            // Aktualizuj stan wykonania
            this.getExecutionState(executionId).then(state => {
                if (state) {
                    const callbacks = this.executionSubscriptions.get(executionId);
                    if (callbacks) {
                        callbacks.forEach(cb => cb(state));
                    }
                }
            });
        }
    }
}









