/**
 * VariablesPanel Integration Tests
 * Tests VariablesPanel integration with RightSidebar
 */

import { describe, it, expect, beforeEach, afterEach } from '@jest/globals';
import { VariablesPanel } from '../VariablesPanel';

describe('VariablesPanel Integration', () => {
    let panel: VariablesPanel;
    let container: HTMLElement;
    let targetContainer: HTMLElement;

    beforeEach(() => {
        // Create initial container
        container = document.createElement('div');
        container.id = 'variables';
        document.body.appendChild(container);

        // Create target container (simulating RightSidebar)
        targetContainer = document.createElement('div');
        targetContainer.id = 'target-variables';
        document.body.appendChild(targetContainer);

        panel = new VariablesPanel('variables');
    });

    afterEach(() => {
        document.body.removeChild(container);
        if (targetContainer.parentNode) {
            document.body.removeChild(targetContainer);
        }
    });

    describe('renderInto', () => {
        it('should move list element to new container', () => {
            // Set some variables first
            panel.setVariables({ var1: 'value1', var2: 'value2' });

            // Render into target container
            panel.renderInto(targetContainer);

            // Check that list element is now in target container
            expect(targetContainer.querySelector('.variables-list')).toBeDefined();
            expect(container.querySelector('.variables-list')).toBeNull();
        });

        it('should update container reference', () => {
            panel.setVariables({ var1: 'value1' });
            panel.renderInto(targetContainer);

            // Variables should still be accessible
            expect(panel.getVariableCount()).toBe(1);
        });

        it('should preserve variables when rendering into new container', () => {
            panel.setVariables({ var1: 'value1', var2: 'value2' });
            panel.renderInto(targetContainer);

            const variables = panel.getVariables();
            expect(Object.keys(variables)).toHaveLength(2);
            expect(variables.var1).toBe('value1');
            expect(variables.var2).toBe('value2');
        });
    });

    describe('badge update', () => {
        it('should update badge when variables are set', () => {
            // Create badge element in target container
            const badge = document.createElement('span');
            badge.id = 'varsBadge';
            targetContainer.appendChild(badge);

            panel.renderInto(targetContainer);
            panel.setVariables({ var1: 'value1', var2: 'value2' });

            expect(badge.textContent).toBe('2');
        });

        it('should find badge in container or document', () => {
            // Badge in document (not in container)
            const badge = document.createElement('span');
            badge.id = 'varsBadge';
            document.body.appendChild(badge);

            panel.setVariables({ var1: 'value1' });

            expect(badge.textContent).toBe('1');

            document.body.removeChild(badge);
        });
    });
});


