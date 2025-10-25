/**
 * WebSocketManager - Implementation (TDD Green Phase)
 * Manages WebSocket connections for real-time execution updates
 */

import type { ExecutionUpdate } from '../types';

type UpdateCallback = (update: ExecutionUpdate) => void;
type ErrorCallback = (error: Event) => void;
type CloseCallback = (event: CloseEvent) => void;

export class WebSocketManager {
    private webSocket: WebSocket | null = null;
    private executionId: string | null = null;
    private updateCallbacks: UpdateCallback[] = [];
    private errorCallbacks: ErrorCallback[] = [];
    private closeCallbacks: CloseCallback[] = [];

    /**
     * Connect to WebSocket for execution
     */
    async connect(executionId: string): Promise<WebSocket> {
        // Close previous connection if exists
        if (this.webSocket) {
            this.disconnect();
        }

        this.executionId = executionId;

        // Construct WebSocket URL
        const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
        const host = window.location.host;
        const wsUrl = `${protocol}//${host}/ws/execution/${executionId}`;

        return new Promise((resolve, reject) => {
            try {
                this.webSocket = new WebSocket(wsUrl);

                this.webSocket.onopen = () => {
                    console.log('✅ WebSocket connected:', executionId);
                    if (this.webSocket) {
                        resolve(this.webSocket);
                    }
                };

                this.webSocket.onmessage = (event: MessageEvent) => {
                    try {
                        const update: ExecutionUpdate = JSON.parse(event.data);
                        this.notifyUpdateCallbacks(update);
                    } catch (error) {
                        console.error('Failed to parse WebSocket message:', error);
                    }
                };

                this.webSocket.onerror = (error: Event) => {
                    console.error('❌ WebSocket error:', error);
                    this.notifyErrorCallbacks(error);
                    reject(error);
                };

        this.webSocket.onclose = (event: CloseEvent) => {
          console.log('🔌 WebSocket closed:', event.code, event.reason);
          console.log('🔌 Was clean?', event.wasClean);
          console.log('🔌 Close code explanations:');
          console.log('   1000 = Normal closure');
          console.log('   1005 = No status code (abnormal)');
          console.log('   1006 = Abnormal closure');
          
          if (event.code === 1005) {
            console.warn('⚠️ WebSocket closed with code 1005 - server may not be sending updates');
          }
          
          this.notifyCloseCallbacks(event);
          this.webSocket = null;
          this.executionId = null;
        };
            } catch (error) {
                reject(error);
            }
        });
    }

    /**
     * Disconnect from WebSocket
     */
    disconnect(): void {
        if (this.webSocket) {
            this.webSocket.close();
            this.webSocket = null;
            this.executionId = null;
        }
    }

    /**
     * Check if connected
     */
    isConnected(): boolean {
        return this.webSocket !== null && this.webSocket.readyState === WebSocket.OPEN;
    }

    /**
     * Get current execution ID
     */
    getExecutionId(): string | null {
        return this.executionId;
    }

    /**
     * Send message through WebSocket
     */
    send(message: any): void {
        if (!this.webSocket || !this.isConnected()) {
            throw new Error('WebSocket not connected');
        }

        this.webSocket.send(JSON.stringify(message));
    }

    /**
     * Register update callback
     */
    onUpdate(callback: UpdateCallback): void {
        this.updateCallbacks.push(callback);
    }

    /**
     * Register error callback
     */
    onError(callback: ErrorCallback): void {
        this.errorCallbacks.push(callback);
    }

    /**
     * Register close callback
     */
    onClose(callback: CloseCallback): void {
        this.closeCallbacks.push(callback);
    }

    /**
     * Notify all update callbacks
     */
    private notifyUpdateCallbacks(update: ExecutionUpdate): void {
        this.updateCallbacks.forEach(callback => {
            try {
                callback(update);
            } catch (error) {
                console.error('Error in update callback:', error);
            }
        });
    }

    /**
     * Notify all error callbacks
     */
    private notifyErrorCallbacks(error: Event): void {
        this.errorCallbacks.forEach(callback => {
            try {
                callback(error);
            } catch (err) {
                console.error('Error in error callback:', err);
            }
        });
    }

    /**
     * Notify all close callbacks
     */
    private notifyCloseCallbacks(event: CloseEvent): void {
        this.closeCallbacks.forEach(callback => {
            try {
                callback(event);
            } catch (error) {
                console.error('Error in close callback:', error);
            }
        });
    }

    /**
     * Clear all callbacks
     */
    clearCallbacks(): void {
        this.updateCallbacks = [];
        this.errorCallbacks = [];
        this.closeCallbacks = [];
    }

    /**
     * Get WebSocket ready state
     */
    getReadyState(): number {
        return this.webSocket?.readyState ?? WebSocket.CLOSED;
    }
}

