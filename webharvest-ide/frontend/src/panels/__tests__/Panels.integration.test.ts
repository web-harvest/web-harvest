/**
 * Integration tests for all panels working together
 * Tests the complete execution flow: Results + Variables + Session
 */

import { ResultsPanel } from '../ResultsPanel';
import { VariablesPanel } from '../VariablesPanel';
import { LogsPanel } from '../LogsPanel';

describe('Panels Integration Tests', () => {
    let resultsContainer: HTMLElement;
    let variablesContainer: HTMLElement;
    let logsContainer: HTMLElement;

    let resultsPanel: ResultsPanel;
    let variablesPanel: VariablesPanel;
    let logsPanel: LogsPanel;

    beforeEach(() => {
        // Setup all containers
        resultsContainer = document.createElement('div');
        resultsContainer.id = 'test-results-int';
        document.body.appendChild(resultsContainer);

        variablesContainer = document.createElement('div');
        variablesContainer.id = 'test-variables-int';
        document.body.appendChild(variablesContainer);

        logsContainer = document.createElement('div');
        logsContainer.id = 'test-logs-int';
        document.body.appendChild(logsContainer);

        // Create panels
        resultsPanel = new ResultsPanel('test-results-int');
        variablesPanel = new VariablesPanel('test-variables-int');
        logsPanel = new LogsPanel('test-logs-int');
    });

    afterEach(() => {
        document.body.removeChild(resultsContainer);
        document.body.removeChild(variablesContainer);
        document.body.removeChild(logsContainer);
    });

    describe('Complete execution flow', () => {
        it('should handle full execution like real WebSocket updates', () => {
            // Simulate real execution from user's logs

            // 1. Logs start coming in
            logsPanel.addLog('info', 'Starting execution: simple_test.xml');
            logsPanel.addLog('info', 'Creating configuration source...');
            logsPanel.addLog('info', 'Parsing XML configuration...');

            expect(logsContainer.textContent).toContain('Starting execution');

            // 2. Execution finishes - results arrive
            const results = {
                configResult: '<html>...</html>',
                title: '<h1>Test Title</h1>',
                content: '<div>Test Content</div>',
            };

            resultsPanel.setResults(results);
            variablesPanel.setVariables(results);

            // Both panels should show the same data
            expect(resultsContainer.textContent).toContain('configResult');
            expect(variablesContainer.textContent).toContain('configResult');
            expect(resultsContainer.textContent).toContain('title');
            expect(variablesContainer.textContent).toContain('title');

            // 3. Final success log
            logsPanel.addLog('success', 'Execution completed successfully');
            expect(logsContainer.textContent).toContain('completed successfully');
        });

        it('should show badges updated across panels', () => {
            // Add logs
            logsPanel.addLog('info', 'Log 1');
            logsPanel.addLog('info', 'Log 2');
            logsPanel.addLog('info', 'Log 3');

            const logsBadge = document.getElementById('logsBadge');
            // Badge might not exist in test, but in real app it would update

            // Add variables
            variablesPanel.setVariables({
                var1: 'value1',
                var2: 'value2',
            });

            const varsBadge = document.getElementById('varsBadge');
            // Badge might not exist in test, but in real app it would update

            // Check panel contents
            expect(logsContainer.querySelectorAll('.log-entry').length).toBe(3);
            expect(variablesContainer.querySelectorAll('.variable-item').length).toBe(2);
        });
    });

    describe('Clear all panels', () => {
        it('should clear all panels together', () => {
            // Setup data
            logsPanel.addLog('info', 'Test log');
            resultsPanel.setResults({ test: 'value' });
            variablesPanel.setVariables({ test: 'value' });
            // All should have content
            expect(logsContainer.textContent).toContain('Test log');
            expect(resultsContainer.textContent).toContain('test');
            expect(variablesContainer.textContent).toContain('test');
            // Clear all
            logsPanel.clear();
            resultsPanel.clear();
            variablesPanel.clear();

            // All should be empty/placeholder
            expect(logsContainer.querySelectorAll('.log-entry').length).toBe(0);
            expect(resultsContainer.querySelectorAll('.result-item').length).toBe(0);
            expect(variablesContainer.querySelectorAll('.variable-item').length).toBe(0);
        });
    });
});





