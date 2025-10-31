/**
 * WebSocketManager - TDD Tests
 * Test FIRST, then implement!
 */

import { describe, it, expect, beforeEach, jest } from '@jest/globals';
import { WebSocketManager } from '../WebSocketManager';
import type { ExecutionUpdate } from '../types';

// Mock WebSocket
class MockWebSocket {
    url: string;
    onopen: ((event: Event) => void) | null = null;
    onmessage: ((event: MessageEvent) => void) | null = null;
    onerror: ((event: Event) => void) | null = null;
    onclose: ((event: CloseEvent) => void) | null = null;
    readyState: number = 0; // CONNECTING

    constructor(url: string) {
        this.url = url;
        setTimeout(() => {
            this.readyState = 1; // OPEN
            if (this.onopen) {
                this.onopen(new Event('open'));
            }
        }, 0);
    }

    send(data: string): void {
        // Mock send
    }

    close(): void {
        this.readyState = 3; // CLOSED
        if (this.onclose) {
            this.onclose(new CloseEvent('close'));
        }
    }

    // Simulate receiving message
    simulateMessage(data: any): void {
        if (this.onmessage) {
            this.onmessage(new MessageEvent('message', { data: JSON.stringify(data) }));
        }
    }
}

// Replace global WebSocket with mock
(global as any).WebSocket = MockWebSocket;

describe('WebSocketManager', () => {
    let manager: WebSocketManager;

    beforeEach(() => {
        manager = new WebSocketManager();
    });

    describe('connect', () => {
        it('should connect to WebSocket URL', async () => {
            const ws = await manager.connect('test-execution-id');

            expect(ws).toBeDefined();
        });

        it('should construct correct WebSocket URL', async () => {
            const ws = await manager.connect('abc123') as any;

            expect(ws.url).toContain('abc123');
        });

        it('should resolve when connection opens', async () => {
            await expect(manager.connect('test-id')).resolves.toBeDefined();
        });

        it('should store active connection', async () => {
            await manager.connect('test-id');

            expect(manager.isConnected()).toBe(true);
        });
    });

    describe('disconnect', () => {
        it('should close WebSocket connection', async () => {
            await manager.connect('test-id');

            manager.disconnect();

            expect(manager.isConnected()).toBe(false);
        });

        it('should do nothing if not connected', () => {
            expect(() => manager.disconnect()).not.toThrow();
        });
    });

    describe('message handling', () => {
        it('should receive execution updates', async (done) => {
            const ws = await manager.connect('test-id') as any;

            manager.onUpdate((update: ExecutionUpdate) => {
                expect(update.status).toBe('RUNNING');
                expect(update.message).toBe('Test message');
                done();
            });

            ws.simulateMessage({
                status: 'RUNNING',
                message: 'Test message',
                progress: 50,
            });
        });

        it('should parse JSON messages', async (done) => {
            const ws = await manager.connect('test-id') as any;

            manager.onUpdate((update: ExecutionUpdate) => {
                expect(update.progress).toBe(75);
                done();
            });

            ws.simulateMessage({
                status: 'RUNNING',
                message: 'Progress',
                progress: 75,
            });
        });

        it('should handle multiple update listeners', async () => {
            const ws = await manager.connect('test-id') as any;
            let count = 0;

            manager.onUpdate(() => count++);
            manager.onUpdate(() => count++);

            ws.simulateMessage({
                status: 'RUNNING',
                message: 'Test',
                progress: 0,
            });

            await new Promise(resolve => setTimeout(resolve, 10));
            expect(count).toBe(2);
        });
    });

    describe('error handling', () => {
        it('should handle connection errors', async () => {
            const ws = await manager.connect('test-id') as any;

            const errorPromise = new Promise((resolve) => {
                manager.onError((error: Event) => {
                    resolve(error);
                });
            });

            if (ws.onerror) {
                ws.onerror(new Event('error'));
            }

            await expect(errorPromise).resolves.toBeDefined();
        });

        it('should handle close events', async () => {
            const ws = await manager.connect('test-id');

            const closePromise = new Promise((resolve) => {
                manager.onClose((event: CloseEvent) => {
                    resolve(event);
                });
            });

            manager.disconnect();

            await expect(closePromise).resolves.toBeDefined();
        });
    });

    describe('reconnection', () => {
        it('should support reconnecting', async () => {
            await manager.connect('test-id-1');
            manager.disconnect();

            await expect(manager.connect('test-id-2')).resolves.toBeDefined();
        });

        it('should close previous connection when reconnecting', async () => {
            await manager.connect('test-id-1');

            expect(manager.isConnected()).toBe(true);

            await manager.connect('test-id-2');

            expect(manager.isConnected()).toBe(true);
        });
    });

    describe('getExecutionId', () => {
        it('should return current execution ID', async () => {
            await manager.connect('abc123');

            expect(manager.getExecutionId()).toBe('abc123');
        });

        it('should return null when not connected', () => {
            expect(manager.getExecutionId()).toBeNull();
        });
    });

    describe('send', () => {
        it('should send message through WebSocket', async () => {
            const ws = await manager.connect('test-id') as any;
            const sendSpy = jest.spyOn(ws, 'send');

            manager.send({ action: 'pause' });

            expect(sendSpy).toHaveBeenCalled();
        });

        it('should throw error if not connected', () => {
            expect(() => manager.send({ action: 'pause' })).toThrow();
        });
    });
});





