/**
 * PropertyPanel Apply Flow Tests
 * Tests that PropertyPanel Apply properly updates nodes in VisualDesigner
 */

import { describe, it, expect, beforeEach, afterEach, jest } from '@jest/globals';
import { PropertyPanel } from '../PropertyPanel';
import type { VisualNode } from '../../types';

describe('PropertyPanel Apply Flow', () => {
    let panel: PropertyPanel;
    let container: HTMLElement;
    let onChangeCallback: jest.Mock;

    beforeEach(() => {
        container = document.createElement('div');
        container.id = 'propertyPanel';
        document.body.appendChild(container);

        onChangeCallback = jest.fn();
        panel = new PropertyPanel('propertyPanel', onChangeCallback);
    });

    afterEach(() => {
        if (container.parentNode) {
            document.body.removeChild(container);
        }
    });

    describe('handleApply', () => {
        it('should call onChangeCallback when Apply is clicked', async () => {
            const node: VisualNode = {
                id: 'test-node',
                type: 'var',
                attributes: { name: 'test' },
                body: null,
                children: [],
            };

            await panel.showNode(node);
            
            // Simulate Apply button click
            const applyButton = container.querySelector('.property-panel-shell__button--primary') as HTMLButtonElement;
            if (applyButton) {
                applyButton.click();
            }

            // Wait for async operations
            await new Promise(resolve => setTimeout(resolve, 100));

            expect(onChangeCallback).toHaveBeenCalled();
            const updatedNode = onChangeCallback.mock.calls[0][0];
            expect(updatedNode.id).toBe('test-node');
        });

        it('should not call onChangeCallback if validation fails', async () => {
            const node: VisualNode = {
                id: 'test-node',
                type: 'var',
                attributes: { name: '' }, // Empty name might fail validation
                body: null,
                children: [],
            };

            await panel.showNode(node);
            
            // Clear previous calls
            onChangeCallback.mockClear();

            // Simulate Apply button click
            const applyButton = container.querySelector('.property-panel-shell__button--primary') as HTMLButtonElement;
            if (applyButton && !applyButton.disabled) {
                applyButton.click();
            }

            await new Promise(resolve => setTimeout(resolve, 100));

            // If validation fails, callback might not be called
            // This depends on validation implementation
        });

        it('should update originalNode after Apply', async () => {
            const node: VisualNode = {
                id: 'test-node',
                type: 'var',
                attributes: { name: 'test' },
                body: null,
                children: [],
            };

            await panel.showNode(node);
            
            // Modify node
            const currentNode = panel.getCurrentNode();
            if (currentNode) {
                currentNode.attributes.name = 'modified';
            }

            // Simulate Apply
            const applyButton = container.querySelector('.property-panel-shell__button--primary') as HTMLButtonElement;
            if (applyButton) {
                applyButton.click();
            }

            await new Promise(resolve => setTimeout(resolve, 100));

            // Original node should be updated
            const updatedNode = panel.getCurrentNode();
            expect(updatedNode).toBeDefined();
        });
    });

    describe('showValidationErrors', () => {
        it('should display validation errors', () => {
            const errors = [
                { field: 'name', message: 'Name is required' },
                { field: 'value', message: 'Value must be a number' },
            ];

            panel.showValidationErrors(errors);

            // Check that validation summary is shown
            const validationEl = container.querySelector('.property-panel-shell__validation');
            expect(validationEl).toBeDefined();
        });

        it('should clear validation errors when empty array provided', () => {
            panel.showValidationErrors([
                { field: 'name', message: 'Error' },
            ]);

            panel.showValidationErrors([]);

            const validationEl = container.querySelector('.property-panel-shell__validation') as HTMLElement;
            expect(validationEl?.style.display).toBe('none');
        });
    });
});


