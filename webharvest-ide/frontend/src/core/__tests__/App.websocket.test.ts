/**
 * Integration tests for App WebSocket handling
 * Tests the complete flow: WebSocket update → Panel rendering
 */

import { describe, it, expect, beforeEach, afterEach, jest } from '@jest/globals';
import { PluginManager } from '../PluginManager';
import { App } from '../App';
import type { ExecutionUpdate } from '../types';

// Mock Monaco
const mockMonaco = {
    editor: {
        create: jest.fn(() => ({
            getValue: jest.fn(() => '<config></config>'),
            setValue: jest.fn(),
            onDidChangeModelContent: jest.fn(),
            getModel: jest.fn(() => ({
                onDidChangeContent: jest.fn(),
            })),
            updateOptions: jest.fn(),
        })),
        defineTheme: jest.fn(),
        setTheme: jest.fn(),
    },
};

(global as any).monaco = mockMonaco;

// Mock fetch
global.fetch = jest.fn((url: string) => {
    if (url.includes('/api/plugins')) {
        return Promise.resolve({
            ok: true,
            json: () => Promise.resolve([]),
        } as Response);
    }
    return Promise.reject(new Error('Unknown URL'));
});

// Mock WebSocket
class MockWebSocket {
    public onopen: ((event: Event) => void) | null = null;
    public onmessage: ((event: MessageEvent) => void) | null = null;
    public onerror: ((event: Event) => void) | null = null;
    public onclose: ((event: CloseEvent) => void) | null = null;

    constructor(public url: string) {
        setTimeout(() => {
            if (this.onopen) {
                this.onopen(new Event('open'));
            }
        }, 0);
    }

    send(data: string): void {
        // Mock send
    }

    close(): void {
        if (this.onclose) {
            this.onclose(new CloseEvent('close', { code: 1000 }));
        }
    }

    // Helper to simulate receiving messages
    simulateMessage(data: any): void {
        if (this.onmessage) {
            this.onmessage(new MessageEvent('message', { data: JSON.stringify(data) }));
        }
    }
}

(global as any).WebSocket = MockWebSocket;

describe('App WebSocket Integration', () => {
    let app: App;
    let mockWebSocket: MockWebSocket;

    beforeEach(() => {
        // Setup DOM
        document.body.innerHTML = `
            <div id="editor"></div>
            <div id="logs"></div>
            <div id="results"></div>
            <div id="variables"></div>
            <div id="session"></div>
            <div id="canvas-area"></div>
            <div id="visual-canvas"></div>
            <div id="propertyPanel"></div>
            <div id="plugin-palette"></div>
            <div id="tabs-container"></div>
        `;

        // Capture WebSocket instance
        const OriginalWebSocket = (global as any).WebSocket;
        (global as any).WebSocket = function (url: string) {
            mockWebSocket = new MockWebSocket(url);
            return mockWebSocket;
        };

        app = new App();
    });

    afterEach(() => {
        jest.clearAllMocks();
    });

    describe('WebSocket Update Handling', () => {
        it('should update Results panel when receiving results object', (done) => {
            const update: ExecutionUpdate = {
                status: 'COMPLETED',
                message: 'Execution completed',
                progress: 100,
                results: {
                    configResult: 'test result',
                    title: 'Test Title',
                    content: 'Test Content',
                },
            };

            // Connect WebSocket
            app['wsManager']['connect']('test-execution-id');

            setTimeout(() => {
                mockWebSocket.simulateMessage(update);

                setTimeout(() => {
                    const resultsPanel = document.getElementById('results');
                    expect(resultsPanel).toBeTruthy();
                    expect(resultsPanel!.textContent).toContain('configResult');
                    done();
                }, 100);
            }, 100);
        });

        it('should update Variables panel when receiving results', (done) => {
            const update: ExecutionUpdate = {
                status: 'COMPLETED',
                message: 'Execution completed',
                progress: 100,
                results: {
                    var1: 'value1',
                    var2: 'value2',
                },
            };

            app['wsManager']['connect']('test-execution-id');

            setTimeout(() => {
                mockWebSocket.simulateMessage(update);

                setTimeout(() => {
                    const variablesPanel = document.getElementById('variables');
                    expect(variablesPanel).toBeTruthy();
                    expect(variablesPanel!.textContent).toContain('var1');
                    expect(variablesPanel!.textContent).toContain('var2');
                    done();
                }, 100);
            }, 100);
        });

        it('should update Session panel when receiving sessionInfo', (done) => {
            const update: ExecutionUpdate = {
                status: 'COMPLETED',
                message: 'Execution completed',
                progress: 100,
                results: {},
                sessionInfo: {
                    sessionId: 'session-123',
                    clientId: 'ide-client',
                    status: 'COMPLETED',
                    duration: 5000,
                    elementsProcessed: 10,
                    processingRate: 2.0,
                },
            };

            app['wsManager']['connect']('test-execution-id');

            setTimeout(() => {
                mockWebSocket.simulateMessage(update);

                setTimeout(() => {
                    const sessionPanel = document.getElementById('session');
                    expect(sessionPanel).toBeTruthy();
                    expect(sessionPanel!.textContent).toContain('session-123');
                    done();
                }, 100);
            }, 100);
        });

        it('should update Session panel when receiving tokenUsage', (done) => {
            const update: ExecutionUpdate = {
                status: 'COMPLETED',
                message: 'Execution completed',
                progress: 100,
                results: {},
                tokenUsage: {
                    httpRequests: 5,
                    httpBytes: 1024,
                    cpuTimeMs: 200,
                    memoryPeakBytes: 524288,
                },
            };

            app['wsManager']['connect']('test-execution-id');

            setTimeout(() => {
                mockWebSocket.simulateMessage(update);

                setTimeout(() => {
                    const sessionPanel = document.getElementById('session');
                    expect(sessionPanel).toBeTruthy();
                    expect(sessionPanel!.textContent).toContain('5');
                    done();
                }, 100);
            }, 100);
        });

        it('should add logs when receiving log messages', (done) => {
            const update: ExecutionUpdate = {
                message: 'Starting execution...',
            };

            app['wsManager']['connect']('test-execution-id');

            setTimeout(() => {
                mockWebSocket.simulateMessage(update);

                setTimeout(() => {
                    const logsPanel = document.getElementById('logs');
                    expect(logsPanel).toBeTruthy();
                    expect(logsPanel!.textContent).toContain('Starting execution');
                    done();
                }, 100);
            }, 100);
        });

        it('should handle complete execution update with all fields', (done) => {
            const update: ExecutionUpdate = {
                status: 'COMPLETED',
                message: 'Execution completed successfully',
                progress: 100,
                results: {
                    configResult: 'final result',
                },
                sessionInfo: {
                    sessionId: 'session-456',
                    clientId: 'ide-client',
                    status: 'COMPLETED',
                    duration: 3000,
                    elementsProcessed: 5,
                    processingRate: 1.67,
                    httpMetrics: {
                        requests: 3,
                        bytesDownloaded: 2048,
                        bytesUploaded: 512,
                        avgResponseTime: 150,
                    },
                    pluginBreakdown: {
                        'http': 3,
                        'xpath': 2,
                    },
                },
                tokenUsage: {
                    httpRequests: 3,
                    httpBytes: 2560,
                    cpuTimeMs: 300,
                    memoryPeakBytes: 1048576,
                },
            };

            app['wsManager']['connect']('test-execution-id');

            setTimeout(() => {
                mockWebSocket.simulateMessage(update);

                setTimeout(() => {
                    // Check all panels were updated
                    const logsPanel = document.getElementById('logs');
                    const resultsPanel = document.getElementById('results');
                    const variablesPanel = document.getElementById('variables');
                    const sessionPanel = document.getElementById('session');

                    expect(logsPanel!.textContent).toContain('completed successfully');
                    expect(resultsPanel!.textContent).toContain('configResult');
                    expect(variablesPanel!.textContent).toContain('configResult');
                    expect(sessionPanel!.textContent).toContain('session-456');
                    done();
                }, 100);
            }, 100);
        });
    });

    describe('Error Handling', () => {
        it('should handle results parsing errors gracefully', (done) => {
            const consoleError = jest.spyOn(console, 'error').mockImplementation();

            const update = {
                status: 'COMPLETED',
                message: 'Test',
                results: 'invalid-should-be-object', // Invalid format
            };

            app['wsManager']['connect']('test-execution-id');

            setTimeout(() => {
                mockWebSocket.simulateMessage(update);

                setTimeout(() => {
                    consoleError.mockRestore();
                    done();
                }, 100);
            }, 100);
        });

        it('should handle empty results object', (done) => {
            const update: ExecutionUpdate = {
                status: 'COMPLETED',
                message: 'No results',
                progress: 100,
                results: {},
            };

            app['wsManager']['connect']('test-execution-id');

            setTimeout(() => {
                mockWebSocket.simulateMessage(update);

                setTimeout(() => {
                    const resultsPanel = document.getElementById('results');
                    expect(resultsPanel).toBeTruthy();
                    // Should render but show no items
                    done();
                }, 100);
            }, 100);
        });
    });
});





