/**
 * NodeStateRenderer - Implementation
 * Renders visual execution states on workflow nodes
 */

export type NodeExecutionState =
    | 'IDLE'
    | 'RUNNING'
    | 'SUCCESS'
    | 'ERROR'
    | 'PAUSED'
    | 'SKIPPED';

const STATE_CLASS_MAP: Record<NodeExecutionState, string> = {
    'IDLE': 'node-idle',
    'RUNNING': 'node-running',
    'SUCCESS': 'node-success',
    'ERROR': 'node-error',
    'PAUSED': 'node-paused',
    'SKIPPED': 'node-skipped'
};

const ALL_STATE_CLASSES = Object.values(STATE_CLASS_MAP);

export class NodeStateRenderer {
    /**
     * Apply execution state to node element
     */
    applyState(element: HTMLElement, state: NodeExecutionState): void {
        // Remove all previous state classes
        ALL_STATE_CLASSES.forEach(cls => element.classList.remove(cls));

        // Remove previous state icons
        this.removeStateIndicators(element);

        // Add new state class
        const stateClass = STATE_CLASS_MAP[state];
        element.classList.add(stateClass);

        // Store state as data attribute
        element.dataset.executionState = state;

        // Apply state-specific styling
        switch (state) {
            case 'SKIPPED':
                element.style.opacity = '0.5';
                break;
            case 'IDLE':
            case 'RUNNING':
            case 'SUCCESS':
            case 'ERROR':
            case 'PAUSED':
                element.style.opacity = '';
                break;
        }

        // Add state indicators
        this.addStateIndicator(element, state);
    }

    /**
     * Get current state of node
     */
    getState(element: HTMLElement): NodeExecutionState {
        return (element.dataset.executionState as NodeExecutionState) || 'IDLE';
    }

    /**
     * Clear all state styling from node
     */
    clearState(element: HTMLElement): void {
        ALL_STATE_CLASSES.forEach(cls => element.classList.remove(cls));
        this.removeStateIndicators(element);
        element.style.opacity = '';
        delete element.dataset.executionState;
    }

    /**
     * Apply state to multiple nodes at once
     */
    applyStateBatch(elements: HTMLElement[], state: NodeExecutionState): void {
        elements.forEach(element => this.applyState(element, state));
    }

    /**
     * Add state indicator icon/spinner to node
     */
    private addStateIndicator(element: HTMLElement, state: NodeExecutionState): void {
        let indicator: HTMLElement;

        switch (state) {
            case 'RUNNING':
                indicator = this.createSpinner();
                indicator.className = 'state-spinner';
                break;

            case 'SUCCESS':
                indicator = this.createIcon('✓', 'state-icon state-icon-success');
                break;

            case 'ERROR':
                indicator = this.createIcon('✕', 'state-icon state-icon-error');
                break;

            case 'PAUSED':
                indicator = this.createIcon('⏸', 'state-icon state-icon-paused');
                break;

            default:
                return; // No indicator for IDLE/SKIPPED
        }

        // Position indicator in top-right corner
        indicator.style.position = 'absolute';
        indicator.style.top = '8px';
        indicator.style.right = '8px';
        indicator.style.zIndex = '10';

        // Ensure node has relative positioning
        if (element.style.position !== 'absolute' && element.style.position !== 'relative') {
            element.style.position = 'relative';
        }

        element.appendChild(indicator);
    }

    /**
     * Remove state indicators from node
     */
    private removeStateIndicators(element: HTMLElement): void {
        const indicators = element.querySelectorAll('.state-icon, .state-spinner');
        indicators.forEach(indicator => indicator.remove());
    }

    /**
     * Create icon element
     */
    private createIcon(symbol: string, className: string): HTMLElement {
        const icon = document.createElement('div');
        icon.className = className;
        icon.textContent = symbol;
        icon.style.fontSize = '18px';
        icon.style.fontWeight = 'bold';
        icon.style.lineHeight = '1';
        return icon;
    }

    /**
     * Create spinner element for RUNNING state
     */
    private createSpinner(): HTMLElement {
        const spinner = document.createElement('div');
        spinner.className = 'state-spinner';
        spinner.innerHTML = `
            <svg width="20" height="20" viewBox="0 0 20 20" style="animation: spin 1s linear infinite;">
                <circle cx="10" cy="10" r="8" fill="none" stroke="var(--lime)" stroke-width="2" 
                        stroke-dasharray="25 15" stroke-linecap="round"/>
            </svg>
        `;
        return spinner;
    }
}


