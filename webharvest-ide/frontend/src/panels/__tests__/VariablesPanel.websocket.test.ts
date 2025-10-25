/**
 * WebSocket integration tests for VariablesPanel
 * Tests handling of variables in object format (from backend)
 */

import { VariablesPanel } from '../VariablesPanel';

describe('VariablesPanel - WebSocket Integration', () => {
    let container: HTMLElement;
    let panel: VariablesPanel;

    beforeEach(() => {
        container = document.createElement('div');
        container.id = 'test-variables';
        document.body.appendChild(container);
        panel = new VariablesPanel('test-variables');
    });

    afterEach(() => {
        document.body.removeChild(container);
    });

    describe('WebSocket object format', () => {
        it('should handle variables object from WebSocket', () => {
            const variablesFromBackend = {
                configResult: '<html>...</html>',
                title: 'Page Title',
                content: 'Page Content',
            };

            panel.setVariables(variablesFromBackend);

            const content = container.textContent || '';
            expect(content).toContain('configResult');
            expect(content).toContain('title');
            expect(content).toContain('content');
        });

        it('should handle empty variables object', () => {
            const variablesFromBackend = {};

            panel.setVariables(variablesFromBackend);

            // Should show empty state
            const content = container.textContent || '';
            expect(content).toContain('No variables');
        });

        it('should truncate long variable values', () => {
            const variablesFromBackend = {
                longVar: 'x'.repeat(500), // Very long value
            };

            panel.setVariables(variablesFromBackend);

            const content = container.textContent || '';
            expect(content).toContain('longVar');
            // Implementation should truncate or handle long values
        });

        it('should render multiple variables', () => {
            const variablesFromBackend = {
                var1: 'value1',
                var2: 'value2',
                var3: 'value3',
            };

            panel.setVariables(variablesFromBackend);

            const items = container.querySelectorAll('.variable-item');
            expect(items.length).toBe(3);
        });
    });

    describe('Update existing variables', () => {
        it('should replace existing variables on setVariables', () => {
            panel.setVariables({ var1: 'old' });
            panel.setVariables({ var2: 'new' });

            const content = container.textContent || '';
            expect(content).not.toContain('var1'); // Old should be gone
            expect(content).toContain('var2'); // New should be present
        });

        it('should update single variable', () => {
            panel.setVariables({ var1: 'value1' });
            panel.setVariable('var1', 'updated');

            const content = container.textContent || '';
            expect(content).toContain('updated');
        });
    });
});

