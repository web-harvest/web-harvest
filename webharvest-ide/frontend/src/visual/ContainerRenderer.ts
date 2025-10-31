/**
 * ContainerRenderer - Implementation
 * Expand/collapse container nodes with aggregate execution state
 */

import type { VisualNode } from '../types';
import type { NodeExecutionState } from './NodeStateRenderer';

export interface ContainerState {
    collapsed: boolean;
    aggregateState: NodeExecutionState;
    childStates: Record<string, NodeExecutionState>;
    executionTime?: number;
}

export class ContainerRenderer {
    private containerStates: Map<string, ContainerState> = new Map();
    private expandedContainers: Set<string> = new Set();
    private onToggleCallback: ((containerId: string, expanded: boolean) => void) | null = null;
    private onAnimationCompleteCallback: (() => void) | null = null;

    /**
     * Render container in collapsed state
     */
    renderCollapsed(element: HTMLElement, container: VisualNode, state?: ContainerState): void {
        element.innerHTML = '';
        element.className = 'visual-node container-node container-collapsed';

        // Apply aggregate state styling
        if (state?.aggregateState) {
            element.classList.add(`container-${state.aggregateState.toLowerCase()}`);
        }

        // Container header
        const header = document.createElement('div');
        header.className = 'container-header';
        header.style.cssText = `
            display: flex;
            align-items: center;
            justify-content: space-between;
            padding: 12px;
            background: linear-gradient(135deg, var(--purple), var(--magenta));
            color: white;
            font-weight: 600;
            font-size: 14px;
            border-radius: 6px 6px 0 0;
            cursor: pointer;
        `;

        // Left: Type + Chevron
        const leftSection = document.createElement('div');
        leftSection.style.cssText = 'display: flex; align-items: center; gap: 8px;';

        const chevron = document.createElement('span');
        chevron.className = 'expand-icon';
        chevron.textContent = '▼';
        chevron.style.fontSize = '12px';
        leftSection.appendChild(chevron);

        const typeName = document.createElement('span');
        typeName.textContent = container.type.toUpperCase();
        leftSection.appendChild(typeName);

        header.appendChild(leftSection);

        // Right: Child count badge
        const badge = document.createElement('span');
        badge.className = 'child-count-badge';
        badge.textContent = `${container.children.length} plugins`;
        badge.style.cssText = `
            background: rgba(255, 255, 255, 0.3);
            padding: 4px 8px;
            border-radius: 12px;
            font-size: 11px;
        `;
        header.appendChild(badge);

        // Click to expand
        header.addEventListener('click', () => {
            this.toggle(container.id);
        });

        element.appendChild(header);

        // Body section
        const body = document.createElement('div');
        body.style.cssText = 'padding: 12px; background: white;';

        // Attributes
        if (Object.keys(container.attributes).length > 0) {
            const attrs = document.createElement('div');
            attrs.className = 'container-attributes';
            attrs.style.cssText = 'font-size: 12px; color: var(--gray-700); margin-bottom: 8px;';

            Object.entries(container.attributes).slice(0, 3).forEach(([key, value]) => {
                const attr = document.createElement('div');
                attr.textContent = `${key}="${value}"`;
                attrs.appendChild(attr);
            });

            body.appendChild(attrs);
        }

        // Execution status (if completed)
        if (state) {
            const statusDiv = document.createElement('div');
            statusDiv.style.cssText = 'display: flex; align-items: center; gap: 8px; margin-top: 8px;';

            // State icon
            const stateIcon = this.getStateIcon(state.aggregateState);
            statusDiv.appendChild(stateIcon);

            // Execution time
            if (state.executionTime) {
                const timeSpan = document.createElement('span');
                timeSpan.className = 'execution-time';
                timeSpan.textContent = this.formatTime(state.executionTime);
                timeSpan.style.cssText = 'font-size: 12px; color: var(--gray-600);';
                statusDiv.appendChild(timeSpan);
            }

            body.appendChild(statusDiv);
        }

        element.appendChild(body);
    }

    /**
     * Render container in expanded state
     */
    renderExpanded(element: HTMLElement, container: VisualNode): void {
        element.innerHTML = '';
        element.className = 'visual-node container-node container-expanded';

        // Container header
        const header = document.createElement('div');
        header.className = 'container-header';
        header.style.cssText = `
            display: flex;
            align-items: center;
            justify-content: space-between;
            padding: 12px;
            background: linear-gradient(135deg, var(--purple), var(--magenta));
            color: white;
            font-weight: 600;
            font-size: 14px;
            border-radius: 6px 6px 0 0;
            cursor: pointer;
        `;

        // Left: Type + Chevron
        const leftSection = document.createElement('div');
        leftSection.style.cssText = 'display: flex; align-items: center; gap: 8px;';

        const chevron = document.createElement('span');
        chevron.className = 'collapse-icon';
        chevron.textContent = '▲';
        chevron.style.fontSize = '12px';
        leftSection.appendChild(chevron);

        const typeName = document.createElement('span');
        typeName.textContent = container.type.toUpperCase();
        leftSection.appendChild(typeName);

        header.appendChild(leftSection);

        // Attributes (compact)
        if (Object.keys(container.attributes).length > 0) {
            const attrsText = Object.entries(container.attributes)
                .slice(0, 2)
                .map(([k, v]) => `${k}="${v}"`)
                .join(' ');

            const attrs = document.createElement('span');
            attrs.style.cssText = 'font-size: 11px; opacity: 0.9;';
            attrs.textContent = attrsText;
            header.appendChild(attrs);
        }

        // Click to collapse
        header.addEventListener('click', () => {
            this.toggle(container.id);
        });

        element.appendChild(header);

        // Children area
        const childrenArea = document.createElement('div');
        childrenArea.className = 'container-children';
        childrenArea.style.cssText = `
            padding: 16px;
            background: rgba(123, 31, 162, 0.05);
            border: 2px dashed var(--purple);
            border-top: none;
            border-radius: 0 0 6px 6px;
            display: flex;
            flex-direction: column;
            gap: 12px;
        `;

        // Render children (simplified)
        container.children.forEach(child => {
            const childNode = document.createElement('div');
            childNode.className = 'child-node';
            childNode.dataset.nodeId = child.id;
            childNode.style.cssText = `
                padding: 10px;
                background: white;
                border: 2px solid var(--gray-300);
                border-radius: 4px;
                font-size: 13px;
            `;
            childNode.textContent = `${child.type} - ${Object.entries(child.attributes)[0]?.[1] || 'no attrs'}`;

            childrenArea.appendChild(childNode);
        });

        element.appendChild(childrenArea);
    }

    /**
     * Toggle container expansion
     */
    toggle(containerId: string): boolean {
        const isCurrentlyExpanded = this.expandedContainers.has(containerId);
        const newState = !isCurrentlyExpanded;

        if (newState) {
            this.expandedContainers.add(containerId);
        } else {
            this.expandedContainers.delete(containerId);
        }

        if (this.onToggleCallback) {
            this.onToggleCallback(containerId, newState);
        }

        console.log(`[ContainerRenderer] Toggled ${containerId}: ${newState ? 'expanded' : 'collapsed'}`);

        return newState;
    }

    /**
     * Check if container is expanded
     */
    isExpanded(containerId: string): boolean {
        return this.expandedContainers.has(containerId);
    }

    /**
     * Set expansion state
     */
    setExpanded(containerId: string, expanded: boolean): void {
        if (expanded) {
            this.expandedContainers.add(containerId);
        } else {
            this.expandedContainers.delete(containerId);
        }
    }

    /**
     * Calculate aggregate state from child states
     */
    calculateAggregateState(childStates: Record<string, NodeExecutionState>): NodeExecutionState {
        const states = Object.values(childStates);

        if (states.length === 0) return 'IDLE';

        // Priority: ERROR > RUNNING > PAUSED > SUCCESS > IDLE > SKIPPED
        if (states.some(s => s === 'ERROR')) return 'ERROR';
        if (states.some(s => s === 'RUNNING')) return 'RUNNING';
        if (states.some(s => s === 'PAUSED')) return 'PAUSED';
        if (states.every(s => s === 'SUCCESS')) return 'SUCCESS';
        if (states.every(s => s === 'IDLE')) return 'IDLE';
        if (states.every(s => s === 'SKIPPED')) return 'SKIPPED';

        return 'IDLE'; // Mixed states default to IDLE
    }

    /**
     * Get container state
     */
    getContainerState(containerId: string): ContainerState {
        return this.containerStates.get(containerId) || {
            collapsed: false,
            aggregateState: 'IDLE',
            childStates: {}
        };
    }

    /**
     * Set container state
     */
    setContainerState(containerId: string, state: ContainerState): void {
        this.containerStates.set(containerId, state);
    }

    /**
     * Update child execution state
     */
    updateChildState(containerId: string, childId: string, state: NodeExecutionState): void {
        const containerState = this.getContainerState(containerId);
        containerState.childStates[childId] = state;

        // Recalculate aggregate
        containerState.aggregateState = this.calculateAggregateState(containerState.childStates);

        this.setContainerState(containerId, containerState);
    }

    /**
     * Collapse all containers
     */
    collapseAll(): void {
        this.expandedContainers.clear();
        console.log('[ContainerRenderer] Collapsed all containers');
    }

    /**
     * Expand all containers
     */
    expandAll(): void {
        this.containerStates.forEach((_, containerId) => {
            this.expandedContainers.add(containerId);
        });
        console.log('[ContainerRenderer] Expanded all containers');
    }

    /**
     * Get state icon
     */
    private getStateIcon(state: NodeExecutionState): HTMLElement {
        const icon = document.createElement('span');
        icon.style.cssText = 'font-size: 16px;';

        switch (state) {
            case 'SUCCESS':
                icon.textContent = '✅';
                break;
            case 'ERROR':
                icon.textContent = '❌';
                break;
            case 'RUNNING':
                icon.textContent = '🔄';
                break;
            case 'PAUSED':
                icon.textContent = '⏸';
                break;
            default:
                icon.textContent = '⚪';
        }

        return icon;
    }

    /**
     * Format execution time
     */
    private formatTime(ms: number): string {
        if (ms < 1000) return `${ms}ms`;
        const seconds = (ms / 1000).toFixed(1);
        return `${seconds}s`;
    }

    /**
     * Register toggle callback
     */
    onToggle(callback: (containerId: string, expanded: boolean) => void): void {
        this.onToggleCallback = callback;
    }

    /**
     * Register animation complete callback
     */
    onAnimationComplete(callback: () => void): void {
        this.onAnimationCompleteCallback = callback;

        // Simulate animation (300ms transition)
        setTimeout(() => {
            if (this.onAnimationCompleteCallback) {
                this.onAnimationCompleteCallback();
            }
        }, 300);
    }

    /**
     * Clear all container states
     */
    clear(): void {
        this.containerStates.clear();
        this.expandedContainers.clear();
    }
}


