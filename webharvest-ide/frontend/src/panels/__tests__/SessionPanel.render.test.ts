/**
 * Tests for SessionPanel rendering (matching old app.js behavior)
 */

import { SessionPanel } from '../SessionPanel';
import type { SessionInfo, TokenUsage } from '../../types';

describe('SessionPanel - Rendering Tests', () => {
    let container: HTMLElement;
    let panel: SessionPanel;

    beforeEach(() => {
        container = document.createElement('div');
        container.id = 'test-session-render';
        document.body.appendChild(container);
        panel = new SessionPanel('test-session-render');
    });

    afterEach(() => {
        document.body.removeChild(container);
    });

    describe('Empty state rendering', () => {
        it('should show empty state when no data', () => {
            // No setSessionInfo or setTokenUsage called
            const content = container.textContent || '';
            expect(content).toContain('No session active');
        });

        it('should clear empty state after setting data', () => {
            // Initially empty
            expect(container.textContent).toContain('No session active');

            // Set session info
            const sessionInfo: SessionInfo = {
                sessionId: 'test-123',
                status: 'COMPLETED',
                duration: 1000,
            };
            panel.setSessionInfo(sessionInfo);

            // Should NOT contain empty state anymore
            expect(container.textContent).not.toContain('No session active');
            expect(container.textContent).toContain('test-123');
        });
    });

    describe('Real execution data', () => {
        it('should render complete execution data like old app.js', () => {
            // Simulate real WebSocket update (from user's log)
            const sessionInfo: SessionInfo = {
                sessionId: '40757819-e053-4e78-b8d7-cb1783357a47',
                clientId: 'ide-client',
                status: 'COMPLETED',
                duration: 3089,
                elementsProcessed: 3,
                processingRate: 0.97,
            };

            const tokenUsage: TokenUsage = {
                httpRequests: 0,
                httpBytes: 0,
                cpuTimeMs: 0,
                memoryPeakBytes: 0,
            };

            panel.setSessionInfo(sessionInfo);
            panel.setTokenUsage(tokenUsage);

            const content = container.textContent || '';
            
            // Check session info
            expect(content).toContain('40757819'); // session ID
            expect(content).toContain('COMPLETED');
            expect(content).toContain('3'); // elements processed
            
            // Check token usage
            expect(content).toContain('0'); // httpRequests
        });

        it('should render session with httpMetrics', () => {
            const sessionInfo: SessionInfo = {
                sessionId: 'test-with-http',
                clientId: 'ide-client',
                status: 'COMPLETED',
                duration: 5000,
                elementsProcessed: 10,
                processingRate: 2.0,
                httpMetrics: {
                    requests: 5,
                    bytesDownloaded: 10240,
                    bytesUploaded: 512,
                    avgResponseTime: 250,
                },
            };

            panel.setSessionInfo(sessionInfo);

            const content = container.textContent || '';
            expect(content).toContain('HTTP Metrics');
            expect(content).toContain('5'); // requests
            expect(content).toContain('10.00 KB'); // bytes downloaded
            expect(content).toContain('250'); // avg response time
        });

        it('should render session with pluginBreakdown', () => {
            const sessionInfo: SessionInfo = {
                sessionId: 'test-with-plugins',
                clientId: 'ide-client',
                status: 'COMPLETED',
                duration: 5000,
                elementsProcessed: 10,
                processingRate: 2.0,
                pluginBreakdown: {
                    'def': 1500,
                    'http': 2000,
                    'xpath': 1000,
                    'file': 500,
                },
            };

            panel.setSessionInfo(sessionInfo);

            const content = container.textContent || '';
            expect(content).toContain('Plugin Breakdown');
            expect(content).toContain('def');
            expect(content).toContain('1500ms');
            expect(content).toContain('http');
            expect(content).toContain('2000ms');
        });
    });

    describe('Clear functionality', () => {
        it('should clear and show empty state', () => {
            const sessionInfo: SessionInfo = {
                sessionId: 'test-123',
                status: 'COMPLETED',
                duration: 1000,
            };

            panel.setSessionInfo(sessionInfo);
            expect(container.textContent).toContain('test-123');

            panel.clear();
            expect(container.textContent).toContain('No session active');
            expect(container.textContent).not.toContain('test-123');
        });
    });

    describe('Update sequence (like real execution)', () => {
        it('should handle typical execution flow', () => {
            // Step 1: Initially empty
            expect(container.textContent).toContain('No session active');

            // Step 2: Session info arrives
            const sessionInfo: SessionInfo = {
                sessionId: 'exec-001',
                clientId: 'ide-client',
                status: 'COMPLETED',
                duration: 2000,
                elementsProcessed: 5,
                processingRate: 2.5,
            };
            panel.setSessionInfo(sessionInfo);
            
            expect(container.textContent).not.toContain('No session active');
            expect(container.textContent).toContain('exec-001');

            // Step 3: Token usage arrives (can come separately)
            const tokenUsage: TokenUsage = {
                httpRequests: 3,
                httpBytes: 5120,
                cpuTimeMs: 150,
                memoryPeakBytes: 2097152,
            };
            panel.setTokenUsage(tokenUsage);

            expect(container.textContent).toContain('Token Usage');
            expect(container.textContent).toContain('3'); // httpRequests
            expect(container.textContent).toContain('5.00 KB'); // httpBytes
        });
    });
});

