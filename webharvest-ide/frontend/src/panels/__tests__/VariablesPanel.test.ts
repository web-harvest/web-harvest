/**
 * VariablesPanel - TDD Tests
 * Test FIRST, then implement!
 */

import { describe, it, expect, beforeEach, afterEach } from '@jest/globals';
import { VariablesPanel } from '../VariablesPanel';

describe('VariablesPanel', () => {
    let panel: VariablesPanel;
    let container: HTMLElement;

    beforeEach(() => {
        container = document.createElement('div');
        container.id = 'test-variables';
        document.body.appendChild(container);

        panel = new VariablesPanel('test-variables');
    });

    afterEach(() => {
        document.body.removeChild(container);
    });

    describe('initialization', () => {
        it('should create panel instance', () => {
            expect(panel).toBeDefined();
        });

        it('should throw error if container not found', () => {
            expect(() => new VariablesPanel('non-existing')).toThrow();
        });
    });

    describe('setVariables', () => {
        it('should set variables object', () => {
            const vars = { test: 'value', count: 42 };

            panel.setVariables(vars);

            expect(panel.getVariables()).toEqual(vars);
        });

        it('should replace previous variables', () => {
            panel.setVariables({ a: 1 });
            panel.setVariables({ b: 2 });

            const vars = panel.getVariables();
            expect(vars.a).toBeUndefined();
            expect(vars.b).toBe(2);
        });

        it('should render variables in DOM', () => {
            panel.setVariables({ test: 'value' });

            expect(container.textContent).toContain('test');
            expect(container.textContent).toContain('value');
        });
    });

    describe('clear', () => {
        it('should clear all variables', () => {
            panel.setVariables({ test: 'value' });
            panel.clear();

            expect(panel.getVariables()).toEqual({});
        });

        it('should clear DOM', () => {
            panel.setVariables({ test: 'value' });
            panel.clear();

            expect(container.querySelector('.variable-item')).toBeNull();
        });
    });

    describe('getVariables', () => {
        it('should return all variables', () => {
            const vars = { a: 1, b: 2, c: 3 };

            panel.setVariables(vars);

            expect(panel.getVariables()).toEqual(vars);
        });

        it('should return copy of variables', () => {
            const vars = { test: 'value' };

            panel.setVariables(vars);
            const copy = panel.getVariables();

            expect(copy).not.toBe(vars);
            expect(copy).toEqual(vars);
        });
    });

    describe('getVariable', () => {
        it('should get single variable by name', () => {
            panel.setVariables({ test: 'value' });

            expect(panel.getVariable('test')).toBe('value');
        });

        it('should return undefined for non-existing variable', () => {
            expect(panel.getVariable('nonexisting')).toBeUndefined();
        });
    });

    describe('setVariable', () => {
        it('should set single variable', () => {
            panel.setVariable('test', 'value');

            expect(panel.getVariable('test')).toBe('value');
        });

        it('should update existing variable', () => {
            panel.setVariable('test', 'old');
            panel.setVariable('test', 'new');

            expect(panel.getVariable('test')).toBe('new');
        });

        it('should render new variable in DOM', () => {
            panel.setVariable('test', 'value');

            expect(container.textContent).toContain('test');
        });
    });

    describe('removeVariable', () => {
        it('should remove variable by name', () => {
            panel.setVariables({ test: 'value', other: 'data' });

            const result = panel.removeVariable('test');

            expect(result).toBe(true);
            expect(panel.getVariable('test')).toBeUndefined();
            expect(panel.getVariable('other')).toBe('data');
        });

        it('should return false for non-existing variable', () => {
            const result = panel.removeVariable('nonexisting');

            expect(result).toBe(false);
        });
    });

    describe('rendering different types', () => {
        it('should render string variables', () => {
            panel.setVariable('text', 'hello');

            expect(container.textContent).toContain('hello');
        });

        it('should render number variables', () => {
            panel.setVariable('count', 42);

            expect(container.textContent).toContain('42');
        });

        it('should render boolean variables', () => {
            panel.setVariable('flag', true);

            expect(container.textContent).toContain('true');
        });

        it('should render complex objects', () => {
            panel.setVariable('obj', { a: 1, b: 2 });

            expect(container.textContent).toContain('a');
        });
    });

    describe('search/filter', () => {
        beforeEach(() => {
            panel.setVariables({
                userName: 'John',
                userAge: 25,
                adminFlag: true,
                sessionId: 'abc123',
            });
        });

        it('should filter variables by name', () => {
            const filtered = panel.filterByName('user');

            expect(Object.keys(filtered)).toHaveLength(2);
            expect(filtered.userName).toBe('John');
            expect(filtered.userAge).toBe(25);
        });

        it('should return all if filter is empty', () => {
            const filtered = panel.filterByName('');

            expect(Object.keys(filtered)).toHaveLength(4);
        });
    });

    describe('variable count', () => {
        it('should return variable count', () => {
            panel.setVariables({ a: 1, b: 2, c: 3 });

            expect(panel.getVariableCount()).toBe(3);
        });

        it('should update count when variables change', () => {
            panel.setVariables({ a: 1 });
            expect(panel.getVariableCount()).toBe(1);

            panel.setVariable('b', 2);
            expect(panel.getVariableCount()).toBe(2);

            panel.removeVariable('a');
            expect(panel.getVariableCount()).toBe(1);
        });
    });

    describe('export', () => {
        it('should export variables as JSON', () => {
            panel.setVariables({ test: 'value', count: 42 });

            const json = panel.exportAsJSON();

            expect(json).toContain('test');
            expect(json).toContain('value');
            expect(json).toContain('42');
        });
    });
});


