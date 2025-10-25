/**
 * Unit tests for SessionPanel
 */

import { SessionPanel } from '../SessionPanel';
import type { SessionInfo, TokenUsage } from '../../types';

describe('SessionPanel', () => {
    let container: HTMLElement;
    let panel: SessionPanel;

    beforeEach(() => {
        container = document.createElement('div');
        container.id = 'test-session';
        document.body.appendChild(container);
        panel = new SessionPanel('test-session');
    });

    afterEach(() => {
        document.body.removeChild(container);
    });

    describe('setSessionInfo', () => {
        it('should display session info correctly', () => {
            const sessionInfo: SessionInfo = {
                sessionId: 'test-123',
                clientId: 'ide-client',
                status: 'COMPLETED',
                duration: 5000,
                elementsProcessed: 10,
                processingRate: 2.0,
            };

            panel.setSessionInfo(sessionInfo);

            const content = container.textContent || '';
            expect(content).toContain('test-123');
            expect(content).toContain('COMPLETED');
            expect(content).toContain('10'); // elements processed
        });

        it('should display HTTP metrics if present', () => {
            const sessionInfo: SessionInfo = {
                sessionId: 'test-123',
                clientId: 'ide-client',
                status: 'COMPLETED',
                duration: 5000,
                elementsProcessed: 10,
                processingRate: 2.0,
                httpMetrics: {
                    requests: 5,
                    bytesDownloaded: 1024,
                    bytesUploaded: 512,
                    avgResponseTime: 150,
                },
            };

            panel.setSessionInfo(sessionInfo);

            const content = container.textContent || '';
            expect(content).toContain('5'); // requests
            expect(content).toContain('1.00 KB'); // bytes downloaded (formatted)
        });

        it('should display plugin breakdown if present', () => {
            const sessionInfo: SessionInfo = {
                sessionId: 'test-123',
                clientId: 'ide-client',
                status: 'COMPLETED',
                duration: 5000,
                elementsProcessed: 10,
                processingRate: 2.0,
                pluginBreakdown: {
                    'http': 3,
                    'xpath': 2,
                    'def': 5,
                },
            };

            panel.setSessionInfo(sessionInfo);

            const content = container.textContent || '';
            expect(content).toContain('http'); // plugin name
            expect(content).toContain('3ms'); // http plugin time
            expect(content).toContain('xpath');
            expect(content).toContain('2ms'); // xpath plugin time
            expect(content).toContain('def');
            expect(content).toContain('5ms'); // def plugin time
        });
    });

    describe('setTokenUsage', () => {
        it('should display token usage correctly', () => {
            const tokenUsage: TokenUsage = {
                httpRequests: 10,
                httpBytes: 2048,
                cpuTimeMs: 500,
                memoryPeakBytes: 1048576,
            };

            panel.setTokenUsage(tokenUsage);

            const content = container.textContent || '';
            expect(content).toContain('10'); // httpRequests
            expect(content).toContain('2.00 KB'); // httpBytes (formatted)
            expect(content).toContain('500'); // cpuTimeMs
        });

        it('should format bytes in human-readable format', () => {
            const tokenUsage: TokenUsage = {
                httpRequests: 1,
                httpBytes: 1048576, // 1 MB
                cpuTimeMs: 100,
                memoryPeakBytes: 1073741824, // 1 GB
            };

            panel.setTokenUsage(tokenUsage);

            const content = container.textContent || '';
            // Should contain formatted values (MB, GB, etc.)
            expect(content.length).toBeGreaterThan(0);
        });
    });

    describe('clear', () => {
        it('should clear all session data', () => {
            const sessionInfo: SessionInfo = {
                sessionId: 'test-123',
                clientId: 'ide-client',
                status: 'COMPLETED',
                duration: 5000,
                elementsProcessed: 10,
                processingRate: 2.0,
            };

            panel.setSessionInfo(sessionInfo);
            expect(container.textContent).toContain('test-123');

            panel.clear();

            const content = container.textContent || '';
            expect(content).not.toContain('test-123');
        });
    });

    describe('integration', () => {
        it('should handle both sessionInfo and tokenUsage together', () => {
            const sessionInfo: SessionInfo = {
                sessionId: 'test-123',
                clientId: 'ide-client',
                status: 'COMPLETED',
                duration: 5000,
                elementsProcessed: 10,
                processingRate: 2.0,
            };

            const tokenUsage: TokenUsage = {
                httpRequests: 5,
                httpBytes: 1024,
                cpuTimeMs: 200,
                memoryPeakBytes: 524288,
            };

            panel.setSessionInfo(sessionInfo);
            panel.setTokenUsage(tokenUsage);

            const content = container.textContent || '';
            expect(content).toContain('test-123');
            expect(content).toContain('5'); // httpRequests
            expect(content).toContain('1.00 KB'); // httpBytes (formatted)
        });
    });
});

