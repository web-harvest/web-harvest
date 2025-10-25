/**
 * VariablesPanel - Implementation (TDD Green Phase)
 * Manages and displays script variables
 */

export class VariablesPanel {
    private variables: Record<string, any> = {};
    private container: HTMLElement;
    private listElement: HTMLElement;

    constructor(containerId: string) {
        const container = document.getElementById(containerId);

        if (!container) {
            throw new Error(`Container with ID "${containerId}" not found`);
        }

        this.container = container;
        this.listElement = this.createListElement();
        this.container.appendChild(this.listElement);
    }

    /**
     * Create variables list container
     */
    private createListElement(): HTMLElement {
        const list = document.createElement('div');
        list.className = 'variables-list';
        list.style.overflowY = 'auto';
        list.style.maxHeight = '100%';
        return list;
    }

    /**
     * Set all variables (replaces existing)
     */
    setVariables(variables: Record<string, any>): void {
        console.log('[VariablesPanel] setVariables called, keys:', Object.keys(variables).length);
        this.variables = { ...variables };
        this.render();
        this.updateBadge();
    }

    /**
     * Update variables badge counter
     */
    private updateBadge(): void {
        const badge = document.getElementById('varsBadge');
        if (badge) {
            const count = Object.keys(this.variables).length;
            badge.textContent = count.toString();
            console.log('[VariablesPanel] Updated badge:', count);
        }
    }

    /**
     * Set single variable
     */
    setVariable(name: string, value: any): void {
        this.variables[name] = value;
        this.render();
        this.updateBadge();
    }

    /**
     * Remove variable by name
     */
    removeVariable(name: string): boolean {
        if (!(name in this.variables)) {
            return false;
        }

        delete this.variables[name];
        this.render();
        this.updateBadge();
        return true;
    }

    /**
     * Get single variable
     */
    getVariable(name: string): any {
        return this.variables[name];
    }

    /**
     * Get all variables
     */
    getVariables(): Record<string, any> {
        return { ...this.variables }; // Return copy
    }

    /**
     * Clear all variables
     */
    clear(): void {
        this.variables = {};
        this.listElement.innerHTML = '';
        this.updateBadge();
    }

    /**
     * Get variable count
     */
    getVariableCount(): number {
        return Object.keys(this.variables).length;
    }

    /**
     * Filter variables by name
     */
    filterByName(query: string): Record<string, any> {
        if (!query) {
            return this.getVariables();
        }

        const filtered: Record<string, any> = {};
        const lowerQuery = query.toLowerCase();

        for (const [key, value] of Object.entries(this.variables)) {
            if (key.toLowerCase().includes(lowerQuery)) {
                filtered[key] = value;
            }
        }

        return filtered;
    }

    /**
     * Render all variables
     */
    private render(): void {
        // ALWAYS clear first
        this.listElement.innerHTML = '';

        const keys = Object.keys(this.variables).sort();
        console.log('[VariablesPanel] Rendering', keys.length, 'variables');

        if (keys.length === 0) {
            this.listElement.innerHTML = `
                <div class="empty-state">
                    <i class="fas fa-box"></i>
                    <p>No variables yet</p>
                    <small>Variables will appear during execution</small>
                </div>
            `;
            return;
        }

        // Render each variable
        keys.forEach(key => {
            const item = this.renderVariable(key, this.variables[key]);
            this.listElement.appendChild(item);
        });

        console.log('[VariablesPanel] Rendered', keys.length, 'variable items to DOM');
    }

    /**
     * Render single variable
     */
    private renderVariable(name: string, value: any): HTMLElement {
        const item = document.createElement('div');
        item.className = 'variable-item';
        item.style.padding = '8px';
        item.style.marginBottom = '4px';
        item.style.background = '#f9f9f9';
        item.style.borderRadius = '4px';
        item.style.border = '1px solid #e5e5e5';

        const header = document.createElement('div');
        header.style.display = 'flex';
        header.style.justifyContent = 'space-between';
        header.style.alignItems = 'center';
        header.style.marginBottom = '4px';

        // Name
        const nameEl = document.createElement('span');
        nameEl.className = 'variable-name';
        nameEl.style.fontWeight = '600';
        nameEl.style.fontSize = '13px';
        nameEl.textContent = name;
        header.appendChild(nameEl);

        // Type
        const typeEl = document.createElement('span');
        typeEl.className = 'variable-type';
        typeEl.style.fontSize = '11px';
        typeEl.style.color = '#999';
        typeEl.textContent = this.getTypeName(value);
        header.appendChild(typeEl);

        item.appendChild(header);

        // Value
        const valueEl = document.createElement('div');
        valueEl.className = 'variable-value';
        valueEl.style.fontSize = '12px';
        valueEl.style.color = '#666';
        valueEl.style.wordBreak = 'break-all';
        valueEl.textContent = this.formatValue(value);
        item.appendChild(valueEl);

        return item;
    }

    /**
     * Get type name for value
     */
    private getTypeName(value: any): string {
        if (value === null) return 'null';
        if (value === undefined) return 'undefined';
        if (Array.isArray(value)) return 'array';
        return typeof value;
    }

    /**
     * Format value for display
     */
    private formatValue(value: any): string {
        if (value === null) return 'null';
        if (value === undefined) return 'undefined';

        if (typeof value === 'object') {
            return JSON.stringify(value, null, 2);
        }

        return String(value);
    }

    /**
     * Export as JSON
     */
    exportAsJSON(): string {
        return JSON.stringify(this.variables, null, 2);
    }

    /**
     * Check if variable exists
     */
    hasVariable(name: string): boolean {
        return name in this.variables;
    }
}

