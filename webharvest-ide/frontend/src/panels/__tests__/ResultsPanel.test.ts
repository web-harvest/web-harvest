/**
 * ResultsPanel - TDD Tests
 * Test FIRST, then implement!
 */

import { describe, it, expect, beforeEach, afterEach } from '@jest/globals';
import { ResultsPanel } from '../ResultsPanel';
import type { ExecutionResult } from '../types';

describe('ResultsPanel', () => {
    let panel: ResultsPanel;
    let container: HTMLElement;

    beforeEach(() => {
        container = document.createElement('div');
        container.id = 'test-results';
        document.body.appendChild(container);

        panel = new ResultsPanel('test-results');
    });

    afterEach(() => {
        document.body.removeChild(container);
    });

    describe('initialization', () => {
        it('should create panel instance', () => {
            expect(panel).toBeDefined();
        });

        it('should throw error if container not found', () => {
            expect(() => new ResultsPanel('non-existing')).toThrow();
        });
    });

    describe('setResults', () => {
        it('should set results array', () => {
            const results: ExecutionResult[] = [
                { id: '1', name: 'result1', value: 'value1', type: 'string', timestamp: Date.now() },
            ];

            panel.setResults(results);

            expect(panel.getResults()).toHaveLength(1);
        });

        it('should replace previous results', () => {
            const results1: ExecutionResult[] = [
                { id: '1', name: 'result1', value: 'value1', type: 'string', timestamp: Date.now() },
            ];
            const results2: ExecutionResult[] = [
                { id: '2', name: 'result2', value: 'value2', type: 'string', timestamp: Date.now() },
            ];

            panel.setResults(results1);
            panel.setResults(results2);

            expect(panel.getResults()).toHaveLength(1);
            expect(panel.getResults()[0].id).toBe('2');
        });

        it('should render results in DOM', () => {
            const results: ExecutionResult[] = [
                { id: '1', name: 'test', value: 'value', type: 'string', timestamp: Date.now() },
            ];

            panel.setResults(results);

            expect(container.querySelector('.result-item')).toBeDefined();
        });
    });

    describe('clear', () => {
        it('should clear all results', () => {
            const results: ExecutionResult[] = [
                { id: '1', name: 'result1', value: 'value1', type: 'string', timestamp: Date.now() },
            ];

            panel.setResults(results);
            panel.clear();

            expect(panel.getResults()).toHaveLength(0);
        });

        it('should clear DOM', () => {
            const results: ExecutionResult[] = [
                { id: '1', name: 'test', value: 'value', type: 'string', timestamp: Date.now() },
            ];

            panel.setResults(results);
            panel.clear();

            expect(container.querySelector('.result-item')).toBeNull();
        });
    });

    describe('getResults', () => {
        it('should return all results', () => {
            const results: ExecutionResult[] = [
                { id: '1', name: 'result1', value: 'value1', type: 'string', timestamp: Date.now() },
                { id: '2', name: 'result2', value: 'value2', type: 'number', timestamp: Date.now() },
            ];

            panel.setResults(results);

            expect(panel.getResults()).toHaveLength(2);
        });

        it('should return copy of results', () => {
            const results: ExecutionResult[] = [
                { id: '1', name: 'test', value: 'value', type: 'string', timestamp: Date.now() },
            ];

            panel.setResults(results);
            const copy = panel.getResults();

            expect(copy).not.toBe(results);
            expect(copy).toEqual(results);
        });
    });

    describe('rendering different types', () => {
        it('should render string values', () => {
            const results: ExecutionResult[] = [
                { id: '1', name: 'text', value: 'hello', type: 'string', timestamp: Date.now() },
            ];

            panel.setResults(results);

            expect(container.textContent).toContain('hello');
        });

        it('should render number values', () => {
            const results: ExecutionResult[] = [
                { id: '1', name: 'count', value: 42, type: 'number', timestamp: Date.now() },
            ];

            panel.setResults(results);

            expect(container.textContent).toContain('42');
        });

        it('should render boolean values', () => {
            const results: ExecutionResult[] = [
                { id: '1', name: 'flag', value: true, type: 'boolean', timestamp: Date.now() },
            ];

            panel.setResults(results);

            expect(container.textContent).toContain('true');
        });

        it('should render array values', () => {
            const results: ExecutionResult[] = [
                { id: '1', name: 'items', value: [1, 2, 3], type: 'array', timestamp: Date.now() },
            ];

            panel.setResults(results);

            expect(container.textContent).toContain('1');
            expect(container.textContent).toContain('2');
            expect(container.textContent).toContain('3');
        });

        it('should render object values', () => {
            const results: ExecutionResult[] = [
                { id: '1', name: 'obj', value: { a: 1, b: 2 }, type: 'object', timestamp: Date.now() },
            ];

            panel.setResults(results);

            expect(container.textContent).toContain('a');
            expect(container.textContent).toContain('b');
        });
    });

    describe('export', () => {
        it('should export results as JSON', () => {
            const results: ExecutionResult[] = [
                { id: '1', name: 'result1', value: 'value1', type: 'string', timestamp: Date.now() },
            ];

            panel.setResults(results);
            const json = panel.exportAsJSON();

            expect(json).toContain('result1');
            expect(json).toContain('value1');
        });

        it('should export results as CSV', () => {
            const results: ExecutionResult[] = [
                { id: '1', name: 'result1', value: 'value1', type: 'string', timestamp: Date.now() },
            ];

            panel.setResults(results);
            const csv = panel.exportAsCSV();

            expect(csv).toContain('result1');
            expect(csv).toContain('value1');
        });
    });

    describe('tree rendering', () => {
        it('should render results as tree structure', () => {
            const results: ExecutionResult[] = [
                { id: '1', name: 'parent', value: { child: 'value' }, type: 'object', timestamp: Date.now() },
            ];

            panel.setResults(results);

            expect(container.querySelector('.result-tree')).toBeDefined();
        });

        it('should support expand/collapse', () => {
            const results: ExecutionResult[] = [
                { id: '1', name: 'obj', value: { nested: { deep: 'value' } }, type: 'object', timestamp: Date.now() },
            ];

            panel.setResults(results);

            const expandBtn = container.querySelector('.expand-btn');
            expect(expandBtn).toBeDefined();
        });
    });
});








