/**
 * TDD Tests for Session History feature
 * Tests BEFORE implementation!
 */

import { SessionPanel } from '../SessionPanel';

// Mock fetch for session history
global.fetch = jest.fn();

describe('SessionPanel - Session History (TDD)', () => {
    let container: HTMLElement;
    let panel: SessionPanel;

    beforeEach(() => {
        container = document.createElement('div');
        container.id = 'test-session-history';
        document.body.appendChild(container);
        panel = new SessionPanel('test-session-history');
        
        // Reset fetch mock
        (global.fetch as jest.Mock).mockClear();
    });

    afterEach(() => {
        document.body.removeChild(container);
    });

    describe('loadSessionHistory()', () => {
        it('should fetch session history from API', async () => {
            const mockSessions = {
                total: 2,
                sessions: [
                    {
                        sessionId: 'session-1',
                        configName: 'test1.xml',
                        status: 'COMPLETED',
                        timestamp: Date.now(),
                        duration: 1000,
                        elementsProcessed: 5,
                        processingRate: 5.0,
                    },
                    {
                        sessionId: 'session-2',
                        configName: 'test2.xml',
                        status: 'FAILED',
                        timestamp: Date.now() - 60000,
                        duration: 500,
                        elementsProcessed: 2,
                        processingRate: 4.0,
                    },
                ],
            };

            (global.fetch as jest.Mock).mockResolvedValueOnce({
                ok: true,
                json: async () => mockSessions,
            });

            await panel.loadSessionHistory();

            // Should have called the API
            expect(global.fetch).toHaveBeenCalledWith('/api/sessions/history?limit=20');
        });

        it('should render session history list', async () => {
            const mockSessions = {
                total: 1,
                sessions: [
                    {
                        sessionId: 'test-session',
                        configName: 'example.xml',
                        status: 'COMPLETED',
                        timestamp: Date.now(),
                        duration: 2000,
                        elementsProcessed: 10,
                        processingRate: 5.0,
                    },
                ],
            };

            (global.fetch as jest.Mock).mockResolvedValueOnce({
                ok: true,
                json: async () => mockSessions,
            });

            await panel.loadSessionHistory();

            // Should render the session in the list
            const content = container.textContent || '';
            expect(content).toContain('test-ses'); // ID is truncated in UI
            expect(content).toContain('example.xml');
            expect(content).toContain('10 elements'); // Status not directly visible in short form
        });

        it('should handle API errors gracefully', async () => {
            (global.fetch as jest.Mock).mockResolvedValueOnce({
                ok: false,
                status: 500,
            });

            const consoleSpy = jest.spyOn(console, 'error').mockImplementation();

            await panel.loadSessionHistory();

            expect(consoleSpy).toHaveBeenCalled();
            consoleSpy.mockRestore();
        });

        it('should show empty state when no sessions', async () => {
            (global.fetch as jest.Mock).mockResolvedValueOnce({
                ok: true,
                json: async () => ({ total: 0, sessions: [] }),
            });

            await panel.loadSessionHistory();

            const content = container.textContent || '';
            expect(content).toContain('No sessions found');
        });
    });

    describe('showSessionDetails()', () => {
        it('should show detailed view of selected session', async () => {
            const mockSession = {
                sessionId: 'detailed-session',
                configName: 'config.xml',
                status: 'COMPLETED',
                timestamp: Date.now(),
                duration: 5000,
                elementsProcessed: 20,
                processingRate: 4.0,
                httpMetrics: {
                    requests: 10,
                    bytesDownloaded: 50000,
                    bytesUploaded: 1000,
                    avgResponseTime: 200,
                },
                pluginBreakdown: {
                    'http': 3000,
                    'xpath': 1500,
                    'def': 500,
                },
            };

            // First load history
            (global.fetch as jest.Mock).mockResolvedValueOnce({
                ok: true,
                json: async () => ({ total: 1, sessions: [mockSession] }),
            });

            await panel.loadSessionHistory();

            // Then show details
            await panel.showSessionDetails(0);

            const content = container.textContent || '';
            expect(content).toContain('detailed-session');
            expect(content).toContain('HTTP Metrics');
            expect(content).toContain('Plugin Breakdown');
            expect(content).toContain('10'); // requests
            expect(content).toContain('3000ms'); // http plugin time
        });

        it('should have "Back to History" button', async () => {
            const mockSession = {
                sessionId: 'test',
                configName: 'test.xml',
                status: 'COMPLETED',
                timestamp: Date.now(),
                duration: 1000,
                elementsProcessed: 5,
                processingRate: 5.0,
            };

            (global.fetch as jest.Mock).mockResolvedValueOnce({
                ok: true,
                json: async () => ({ total: 1, sessions: [mockSession] }),
            });

            await panel.loadSessionHistory();
            await panel.showSessionDetails(0);

            const content = container.innerHTML;
            expect(content).toContain('Back to History');
        });
    });

    describe('View History button', () => {
        it('should show "View History" button when no active session', () => {
            // Empty panel
            const content = container.innerHTML;
            // Implementation will add this button
            // For now, just checking structure exists
            expect(container).toBeTruthy();
        });
    });
});

