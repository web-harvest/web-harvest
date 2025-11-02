/**
 * ContainerRenderer - TDD Tests
 * Tests for expand/collapse container nodes with aggregate state
 */

import { describe, it, expect, beforeEach } from '@jest/globals';
import { ContainerRenderer, ContainerState } from '../ContainerRenderer';
import type { VisualNode } from '../../types';

describe('ContainerRenderer', () => {
    let renderer: ContainerRenderer;
    let mockContainer: VisualNode;
    let mockElement: HTMLElement;

    beforeEach(() => {
        renderer = new ContainerRenderer();

        mockContainer = {
            id: 'container-1',
            type: 'loop',
            attributes: { maxloops: '10' },
            body: null,
            children: [
                {
                    id: 'child-1',
                    type: 'http',
                    attributes: { url: 'http://example.com' },
                    body: null,
                    children: []
                },
                {
                    id: 'child-2',
                    type: 'xpath',
                    attributes: { expression: '//title' },
                    body: null,
                    children: []
                },
                {
                    id: 'child-3',
                    type: 'def',
                    attributes: { var: 'result' },
                    body: null,
                    children: []
                }
            ]
        };

        mockElement = document.createElement('div');
        mockElement.className = 'visual-node container-node';
        mockElement.dataset.nodeId = 'container-1';
    });

    describe('renderCollapsed', () => {
        it('should render collapsed container', () => {
            renderer.renderCollapsed(mockElement, mockContainer);

            const header = mockElement.querySelector('.container-header');
            expect(header).not.toBeNull();
            expect(header?.textContent).toContain('LOOP');
        });

        it('should show child count badge', () => {
            renderer.renderCollapsed(mockElement, mockContainer);

            const badge = mockElement.querySelector('.child-count-badge');
            expect(badge?.textContent).toContain('3');
        });

        it('should show expand chevron icon', () => {
            renderer.renderCollapsed(mockElement, mockContainer);

            const chevron = mockElement.querySelector('.expand-icon');
            expect(chevron).not.toBeNull();
            expect(chevron?.textContent).toContain('▼');
        });

        it('should show key attributes', () => {
            renderer.renderCollapsed(mockElement, mockContainer);

            const attrs = mockElement.querySelector('.container-attributes');
            expect(attrs?.textContent).toContain('maxloops="10"');
        });

        it('should show aggregate execution state', () => {
            const state: ContainerState = {
                collapsed: true,
                aggregateState: 'SUCCESS',
                childStates: {},
                executionTime: 3200
            };

            renderer.renderCollapsed(mockElement, mockContainer, state);

            expect(mockElement.classList.contains('container-success')).toBe(true);
        });

        it('should show execution time for completed containers', () => {
            const state: ContainerState = {
                collapsed: true,
                aggregateState: 'SUCCESS',
                childStates: {},
                executionTime: 3200
            };

            renderer.renderCollapsed(mockElement, mockContainer, state);

            const time = mockElement.querySelector('.execution-time');
            expect(time?.textContent).toContain('3.2s');
        });
    });

    describe('renderExpanded', () => {
        it('should render expanded container with children', () => {
            renderer.renderExpanded(mockElement, mockContainer);

            const childrenArea = mockElement.querySelector('.container-children');
            expect(childrenArea).not.toBeNull();
        });

        it('should show collapse chevron icon', () => {
            renderer.renderExpanded(mockElement, mockContainer);

            const chevron = mockElement.querySelector('.collapse-icon');
            expect(chevron).not.toBeNull();
            expect(chevron?.textContent).toContain('▲');
        });

        it('should render all children', () => {
            renderer.renderExpanded(mockElement, mockContainer);

            const children = mockElement.querySelectorAll('.child-node');
            expect(children.length).toBe(3);
        });

        it('should render child content', () => {
            renderer.renderExpanded(mockElement, mockContainer);

            const child = mockElement.querySelector('.child-node') as HTMLElement;
            expect(child.textContent).toContain('http');
        });
    });

    describe('toggle', () => {
        it('should toggle from collapsed to expanded', () => {
            renderer.renderCollapsed(mockElement, mockContainer);

            const isExpanded = renderer.toggle('container-1');
            expect(isExpanded).toBe(true);
        });

        it('should toggle from expanded to collapsed', () => {
            renderer.renderExpanded(mockElement, mockContainer);
            renderer.setExpanded('container-1', true);

            const isExpanded = renderer.toggle('container-1');
            expect(isExpanded).toBe(false);
        });

        it('should emit toggle event', (done) => {
            renderer.onToggle((containerId, expanded) => {
                expect(containerId).toBe('container-1');
                expect(expanded).toBe(true);
                done();
            });

            renderer.toggle('container-1');
        });
    });

    describe('isExpanded', () => {
        it('should return expansion state', () => {
            renderer.setExpanded('container-1', true);
            expect(renderer.isExpanded('container-1')).toBe(true);

            renderer.setExpanded('container-1', false);
            expect(renderer.isExpanded('container-1')).toBe(false);
        });

        it('should default to false for new containers', () => {
            expect(renderer.isExpanded('new-container')).toBe(false);
        });
    });

    describe('calculateAggregateState', () => {
        it('should return SUCCESS when all children succeed', () => {
            const childStates = {
                'child-1': 'SUCCESS',
                'child-2': 'SUCCESS',
                'child-3': 'SUCCESS'
            };

            const aggregate = renderer.calculateAggregateState(childStates);
            expect(aggregate).toBe('SUCCESS');
        });

        it('should return ERROR if any child fails', () => {
            const childStates = {
                'child-1': 'SUCCESS',
                'child-2': 'ERROR',
                'child-3': 'SUCCESS'
            };

            const aggregate = renderer.calculateAggregateState(childStates);
            expect(aggregate).toBe('ERROR');
        });

        it('should return RUNNING if any child is running', () => {
            const childStates = {
                'child-1': 'SUCCESS',
                'child-2': 'RUNNING',
                'child-3': 'IDLE'
            };

            const aggregate = renderer.calculateAggregateState(childStates);
            expect(aggregate).toBe('RUNNING');
        });

        it('should return IDLE if all children are idle', () => {
            const childStates = {
                'child-1': 'IDLE',
                'child-2': 'IDLE',
                'child-3': 'IDLE'
            };

            const aggregate = renderer.calculateAggregateState(childStates);
            expect(aggregate).toBe('IDLE');
        });

        it('should return PAUSED if container is paused', () => {
            const childStates = {
                'child-1': 'SUCCESS',
                'child-2': 'PAUSED',
                'child-3': 'IDLE'
            };

            const aggregate = renderer.calculateAggregateState(childStates);
            expect(aggregate).toBe('PAUSED');
        });
    });

    describe('getContainerState', () => {
        it('should return current container state', () => {
            const state: ContainerState = {
                collapsed: true,
                aggregateState: 'SUCCESS',
                childStates: {},
                executionTime: 1500
            };

            renderer.setContainerState('container-1', state);

            const retrieved = renderer.getContainerState('container-1');
            expect(retrieved).toEqual(state);
        });

        it('should return default state for new container', () => {
            const state = renderer.getContainerState('new-container');
            expect(state.collapsed).toBe(false);
            expect(state.aggregateState).toBe('IDLE');
        });
    });

    describe('updateChildState', () => {
        it('should update child execution state', () => {
            renderer.setContainerState('container-1', {
                collapsed: true,
                aggregateState: 'RUNNING',
                childStates: {}
            });

            renderer.updateChildState('container-1', 'child-1', 'SUCCESS');

            const state = renderer.getContainerState('container-1');
            expect(state.childStates['child-1']).toBe('SUCCESS');
        });

        it('should recalculate aggregate state', () => {
            renderer.setContainerState('container-1', {
                collapsed: true,
                aggregateState: 'RUNNING',
                childStates: {
                    'child-1': 'RUNNING',
                    'child-2': 'IDLE'
                }
            });

            renderer.updateChildState('container-1', 'child-1', 'SUCCESS');

            const state = renderer.getContainerState('container-1');
            // Should still be RUNNING because child-2 is IDLE
            expect(state.aggregateState).toBe('IDLE');
        });
    });

    describe('collapseAll', () => {
        it('should collapse all containers', () => {
            renderer.setExpanded('container-1', true);
            renderer.setExpanded('container-2', true);

            renderer.collapseAll();

            expect(renderer.isExpanded('container-1')).toBe(false);
            expect(renderer.isExpanded('container-2')).toBe(false);
        });
    });

    describe('expandAll', () => {
        it('should expand all containers', () => {
            renderer.setExpanded('container-1', false);
            renderer.setExpanded('container-2', false);
            renderer.setContainerState('container-1', {
                collapsed: false,
                aggregateState: 'IDLE',
                childStates: {},
            });
            renderer.setContainerState('container-2', {
                collapsed: false,
                aggregateState: 'IDLE',
                childStates: {},
            });

            renderer.expandAll();

            expect(renderer.isExpanded('container-1')).toBe(true);
            expect(renderer.isExpanded('container-2')).toBe(true);
        });
    });

    describe('animation', () => {
        it('should animate expand transition', (done) => {
            renderer.renderCollapsed(mockElement, mockContainer);

            renderer.onAnimationComplete(() => {
                done();
            });

            renderer.toggle('container-1');
        });
    });
});


