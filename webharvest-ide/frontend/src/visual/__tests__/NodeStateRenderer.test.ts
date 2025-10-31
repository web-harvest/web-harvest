/**
 * NodeStateRenderer - TDD Tests
 * Tests for visual execution state rendering on nodes
 */

import { describe, it, expect, beforeEach } from '@jest/globals';
import { NodeStateRenderer, NodeExecutionState } from '../NodeStateRenderer';

describe('NodeStateRenderer', () => {
    let renderer: NodeStateRenderer;
    let mockElement: HTMLElement;

    beforeEach(() => {
        renderer = new NodeStateRenderer();
        mockElement = document.createElement('div');
        mockElement.className = 'visual-node';
        mockElement.dataset.nodeId = 'test-node-1';
    });

    describe('applyState', () => {
        it('should apply IDLE state with gray-200 border', () => {
            renderer.applyState(mockElement, 'IDLE');

            expect(mockElement.classList.contains('node-idle')).toBe(true);
            expect(mockElement.classList.contains('node-running')).toBe(false);
        });

        it('should apply RUNNING state with lime animated border', () => {
            renderer.applyState(mockElement, 'RUNNING');

            expect(mockElement.classList.contains('node-running')).toBe(true);
            expect(mockElement.classList.contains('node-idle')).toBe(false);
        });

        it('should apply SUCCESS state with lime solid border', () => {
            renderer.applyState(mockElement, 'SUCCESS');

            expect(mockElement.classList.contains('node-success')).toBe(true);
            expect(mockElement.classList.contains('node-running')).toBe(false);
        });

        it('should apply ERROR state with magenta border', () => {
            renderer.applyState(mockElement, 'ERROR');

            expect(mockElement.classList.contains('node-error')).toBe(true);
            expect(mockElement.classList.contains('node-success')).toBe(false);
        });

        it('should apply PAUSED state with yellow border', () => {
            renderer.applyState(mockElement, 'PAUSED');

            expect(mockElement.classList.contains('node-paused')).toBe(true);
        });

        it('should apply SKIPPED state with gray-400 border and dimmed', () => {
            renderer.applyState(mockElement, 'SKIPPED');

            expect(mockElement.classList.contains('node-skipped')).toBe(true);
            expect(mockElement.style.opacity).toBe('0.5');
        });

        it('should remove previous state classes when applying new state', () => {
            renderer.applyState(mockElement, 'RUNNING');
            renderer.applyState(mockElement, 'SUCCESS');

            expect(mockElement.classList.contains('node-running')).toBe(false);
            expect(mockElement.classList.contains('node-success')).toBe(true);
        });
    });

    describe('getState', () => {
        it('should return current state of node', () => {
            renderer.applyState(mockElement, 'RUNNING');

            const state = renderer.getState(mockElement);
            expect(state).toBe('RUNNING');
        });

        it('should return IDLE for node with no state', () => {
            const state = renderer.getState(mockElement);
            expect(state).toBe('IDLE');
        });
    });

    describe('clearState', () => {
        it('should remove all state classes', () => {
            renderer.applyState(mockElement, 'RUNNING');
            renderer.clearState(mockElement);

            expect(mockElement.classList.contains('node-running')).toBe(false);
            expect(mockElement.classList.contains('node-idle')).toBe(false);
        });

        it('should reset opacity', () => {
            renderer.applyState(mockElement, 'SKIPPED');
            renderer.clearState(mockElement);

            expect(mockElement.style.opacity).toBe('');
        });
    });

    describe('addStateIcon', () => {
        it('should add success icon for SUCCESS state', () => {
            renderer.applyState(mockElement, 'SUCCESS');

            const icon = mockElement.querySelector('.state-icon');
            expect(icon).not.toBeNull();
            expect(icon?.textContent).toContain('✓');
        });

        it('should add error icon for ERROR state', () => {
            renderer.applyState(mockElement, 'ERROR');

            const icon = mockElement.querySelector('.state-icon');
            expect(icon).not.toBeNull();
            expect(icon?.textContent).toContain('✕');
        });

        it('should add spinner for RUNNING state', () => {
            renderer.applyState(mockElement, 'RUNNING');

            const spinner = mockElement.querySelector('.state-spinner');
            expect(spinner).not.toBeNull();
        });

        it('should remove previous icon when state changes', () => {
            renderer.applyState(mockElement, 'SUCCESS');
            renderer.applyState(mockElement, 'ERROR');

            const icons = mockElement.querySelectorAll('.state-icon');
            expect(icons.length).toBe(1);
        });
    });

    describe('batch operations', () => {
        it('should apply states to multiple nodes', () => {
            const nodes = [
                document.createElement('div'),
                document.createElement('div'),
                document.createElement('div')
            ];

            nodes.forEach((node, i) => {
                node.className = 'visual-node';
                node.dataset.nodeId = `node-${i}`;
            });

            renderer.applyStateBatch(nodes, 'RUNNING');

            nodes.forEach(node => {
                expect(node.classList.contains('node-running')).toBe(true);
            });
        });
    });

    describe('state transitions', () => {
        it('should transition from IDLE to RUNNING', () => {
            renderer.applyState(mockElement, 'IDLE');
            renderer.applyState(mockElement, 'RUNNING');

            expect(mockElement.classList.contains('node-idle')).toBe(false);
            expect(mockElement.classList.contains('node-running')).toBe(true);
        });

        it('should transition from RUNNING to SUCCESS', () => {
            renderer.applyState(mockElement, 'RUNNING');
            renderer.applyState(mockElement, 'SUCCESS');

            expect(mockElement.classList.contains('node-running')).toBe(false);
            expect(mockElement.classList.contains('node-success')).toBe(true);
        });

        it('should transition from RUNNING to ERROR', () => {
            renderer.applyState(mockElement, 'RUNNING');
            renderer.applyState(mockElement, 'ERROR');

            expect(mockElement.classList.contains('node-running')).toBe(false);
            expect(mockElement.classList.contains('node-error')).toBe(true);
        });

        it('should handle PAUSED and RESUMED states', () => {
            renderer.applyState(mockElement, 'RUNNING');
            renderer.applyState(mockElement, 'PAUSED');
            expect(mockElement.classList.contains('node-paused')).toBe(true);

            renderer.applyState(mockElement, 'RUNNING');
            expect(mockElement.classList.contains('node-paused')).toBe(false);
            expect(mockElement.classList.contains('node-running')).toBe(true);
        });
    });
});


