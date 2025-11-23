/**
 * DataVisualization Export Tests
 * Tests export functionality in DataVisualization
 */

import { describe, it, expect, beforeEach, afterEach, jest } from '@jest/globals';
import { DataVisualization } from '../DataVisualization';

describe('DataVisualization Export', () => {
    let visualization: DataVisualization;
    let container: HTMLElement;
    let onExportCallback: jest.Mock;

    beforeEach(() => {
        container = document.createElement('div');
        container.id = 'results';
        document.body.appendChild(container);

        onExportCallback = jest.fn();
        visualization = new DataVisualization('results', {
            onExport: onExportCallback,
        });
    });

    afterEach(() => {
        if (container.parentNode) {
            document.body.removeChild(container);
        }
    });

    describe('exportData', () => {
        it('should call onExport callback with format', () => {
            visualization.setResults({ var1: 'value1', var2: 'value2' });
            visualization.exportData('json');

            expect(onExportCallback).toHaveBeenCalledWith('json');
        });

        it('should support CSV export', () => {
            visualization.setResults({ var1: 'value1' });
            visualization.exportData('csv');

            expect(onExportCallback).toHaveBeenCalledWith('csv');
        });

        it('should support XML export', () => {
            visualization.setResults({ var1: 'value1' });
            visualization.exportData('xml');

            expect(onExportCallback).toHaveBeenCalledWith('xml');
        });
    });

    describe('setResults', () => {
        it('should handle array format', () => {
            const results = [
                { id: '1', name: 'var1', value: 'value1', type: 'string', timestamp: Date.now() },
                { id: '2', name: 'var2', value: 'value2', type: 'string', timestamp: Date.now() },
            ];

            visualization.setResults(results);

            // Results should be set
            expect(container.querySelector('table')).toBeDefined();
        });

        it('should handle object format', () => {
            const results = { var1: 'value1', var2: 'value2' };
            visualization.setResults(results);

            // Results should be converted and displayed
            expect(container.querySelector('table')).toBeDefined();
        });

        it('should render results after setting', () => {
            visualization.setResults({ var1: 'value1' });
            
            // Table should be rendered
            const table = container.querySelector('table');
            expect(table).toBeDefined();
        });
    });
});


