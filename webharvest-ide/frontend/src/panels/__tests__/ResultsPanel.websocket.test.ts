/**
 * WebSocket integration tests for ResultsPanel
 * Tests handling of results in object format (from backend)
 */

import { ResultsPanel } from '../ResultsPanel';

describe('ResultsPanel - WebSocket Integration', () => {
    let container: HTMLElement;
    let panel: ResultsPanel;

    beforeEach(() => {
        container = document.createElement('div');
        container.id = 'test-results';
        document.body.appendChild(container);
        panel = new ResultsPanel('test-results');
    });

    afterEach(() => {
        document.body.removeChild(container);
    });

    describe('WebSocket object format', () => {
        it('should handle results object from WebSocket', () => {
            const resultsFromBackend = {
                configResult: 'test result',
                title: 'Test Title',
                content: 'Test Content',
            };

            panel.setResults(resultsFromBackend);

            const content = container.textContent || '';
            expect(content).toContain('configResult');
            expect(content).toContain('title');
            expect(content).toContain('content');
        });

        it('should handle empty results object', () => {
            const resultsFromBackend = {};

            panel.setResults(resultsFromBackend);

            // Should show empty state
            const content = container.textContent || '';
            expect(content).toContain('No results');
        });

        it('should handle results with various types', () => {
            const resultsFromBackend = {
                stringVar: 'test string',
                numberVar: '123', // Backend sends as strings
                longContent: 'x'.repeat(5000), // Long content
            };

            panel.setResults(resultsFromBackend);

            const content = container.textContent || '';
            expect(content).toContain('stringVar');
            expect(content).toContain('numberVar');
            expect(content).toContain('longContent');
        });

        it('should convert object to array internally', () => {
            const resultsFromBackend = {
                var1: 'value1',
                var2: 'value2',
            };

            panel.setResults(resultsFromBackend);

            // Should have converted to array and rendered
            const items = container.querySelectorAll('.result-item');
            expect(items.length).toBe(2);
        });
    });

    describe('Array format (legacy)', () => {
        it('should still handle array format', () => {
            const resultsArray = [
                { id: '1', name: 'result1', value: 'value1', type: 'string', timestamp: Date.now() },
                { id: '2', name: 'result2', value: 'value2', type: 'string', timestamp: Date.now() },
            ];

            panel.setResults(resultsArray);

            const items = container.querySelectorAll('.result-item');
            expect(items.length).toBe(2);
        });
    });
});







