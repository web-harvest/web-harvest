/**
 * LogsPanel - TDD Tests
 * Test FIRST, then implement!
 */

import { describe, it, expect, beforeEach } from '@jest/globals';
import { LogsPanel } from '../LogsPanel';
import type { LogEntry, LogType } from '../types';

describe('LogsPanel', () => {
    let panel: LogsPanel;
    let container: HTMLElement;

    beforeEach(() => {
        // Create container
        container = document.createElement('div');
        container.id = 'test-logs';
        document.body.appendChild(container);

        panel = new LogsPanel('test-logs');
    });

    afterEach(() => {
        document.body.removeChild(container);
    });

    describe('initialization', () => {
        it('should create panel instance', () => {
            expect(panel).toBeDefined();
        });

        it('should throw error if container not found', () => {
            expect(() => new LogsPanel('non-existing')).toThrow();
        });
    });

    describe('addLog', () => {
        it('should add info log', () => {
            panel.addLog('info', 'Test info message');

            const logs = panel.getLogs();
            expect(logs).toHaveLength(1);
            expect(logs[0].type).toBe('info');
            expect(logs[0].message).toBe('Test info message');
        });

        it('should add warning log', () => {
            panel.addLog('warning', 'Test warning');

            const logs = panel.getLogs();
            expect(logs[0].type).toBe('warning');
        });

        it('should add error log', () => {
            panel.addLog('error', 'Test error');

            const logs = panel.getLogs();
            expect(logs[0].type).toBe('error');
        });

        it('should add success log', () => {
            panel.addLog('success', 'Test success');

            const logs = panel.getLogs();
            expect(logs[0].type).toBe('success');
        });

        it('should assign unique IDs to logs', () => {
            panel.addLog('info', 'Log 1');
            panel.addLog('info', 'Log 2');

            const logs = panel.getLogs();
            expect(logs[0].id).not.toBe(logs[1].id);
        });

        it('should assign timestamps to logs', () => {
            const before = Date.now();
            panel.addLog('info', 'Test');
            const after = Date.now();

            const logs = panel.getLogs();
            expect(logs[0].timestamp).toBeGreaterThanOrEqual(before);
            expect(logs[0].timestamp).toBeLessThanOrEqual(after);
        });

        it('should render log in DOM', () => {
            panel.addLog('info', 'Test message');

            const logElements = container.querySelectorAll('.log-entry');
            expect(logElements.length).toBe(1);
        });
    });

    describe('clear', () => {
        it('should clear all logs', () => {
            panel.addLog('info', 'Log 1');
            panel.addLog('info', 'Log 2');

            panel.clear();

            expect(panel.getLogs()).toHaveLength(0);
        });

        it('should clear DOM', () => {
            panel.addLog('info', 'Test');

            panel.clear();

            const logElements = container.querySelectorAll('.log-entry');
            expect(logElements.length).toBe(0);
        });
    });

    describe('getLogs', () => {
        it('should return all logs', () => {
            panel.addLog('info', 'Log 1');
            panel.addLog('warning', 'Log 2');
            panel.addLog('error', 'Log 3');

            const logs = panel.getLogs();
            expect(logs).toHaveLength(3);
        });

        it('should return copy of logs array', () => {
            panel.addLog('info', 'Test');

            const logs1 = panel.getLogs();
            const logs2 = panel.getLogs();

            expect(logs1).not.toBe(logs2); // Different references
            expect(logs1).toEqual(logs2); // Same content
        });
    });

    describe('filter', () => {
        beforeEach(() => {
            panel.addLog('info', 'Info message');
            panel.addLog('warning', 'Warning message');
            panel.addLog('error', 'Error message');
            panel.addLog('success', 'Success message');
        });

        it('should filter by type', () => {
            const errors = panel.filterByType('error');

            expect(errors).toHaveLength(1);
            expect(errors[0].type).toBe('error');
        });

        it('should return all logs if no filter', () => {
            const all = panel.getLogs();

            expect(all).toHaveLength(4);
        });
    });

    describe('rendering', () => {
        it('should apply correct CSS class for info logs', () => {
            panel.addLog('info', 'Test');

            const logElement = container.querySelector('.log-entry');
            expect(logElement?.classList.contains('log-info')).toBe(true);
        });

        it('should apply correct CSS class for warning logs', () => {
            panel.addLog('warning', 'Test');

            const logElement = container.querySelector('.log-entry');
            expect(logElement?.classList.contains('log-warning')).toBe(true);
        });

        it('should apply correct CSS class for error logs', () => {
            panel.addLog('error', 'Test');

            const logElement = container.querySelector('.log-entry');
            expect(logElement?.classList.contains('log-error')).toBe(true);
        });

        it('should display log message', () => {
            panel.addLog('info', 'Test message');

            expect(container.textContent).toContain('Test message');
        });

        it('should auto-scroll to bottom when new log added', () => {
            for (let i = 0; i < 100; i++) {
                panel.addLog('info', `Log ${i}`);
            }

            // Should scroll to show latest log
            const scrollContainer = container.querySelector('.logs-list');
            expect(scrollContainer).toBeDefined();
        });
    });

    describe('badge count', () => {
        it('should update badge count', () => {
            panel.addLog('info', 'Log 1');
            panel.addLog('info', 'Log 2');

            expect(panel.getLogCount()).toBe(2);
        });

        it('should reset badge count after clear', () => {
            panel.addLog('info', 'Log 1');
            panel.clear();

            expect(panel.getLogCount()).toBe(0);
        });
    });
});







