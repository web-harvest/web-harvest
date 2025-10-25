/**
 * ResultsPanel - Implementation (TDD Green Phase)
 * Displays execution results in tree format
 */

import type { ExecutionResult } from '../types';

export class ResultsPanel {
    private results: ExecutionResult[] = [];
    private container: HTMLElement;
    private treeElement: HTMLElement;

    constructor(containerId: string) {
        const container = document.getElementById(containerId);

        if (!container) {
            throw new Error(`Container with ID "${containerId}" not found`);
        }

        this.container = container;
        this.treeElement = this.createTreeElement();
        this.container.appendChild(this.treeElement);
    }

    /**
     * Create tree container
     */
    private createTreeElement(): HTMLElement {
        const tree = document.createElement('div');
        tree.className = 'result-tree';
        tree.style.overflowY = 'auto';
        tree.style.maxHeight = '100%';
        return tree;
    }

    /**
     * Set results (replaces existing)
     * Accepts both array format and object format (from WebSocket)
     */
    setResults(results: ExecutionResult[] | Record<string, any>): void {
        console.log('[ResultsPanel] setResults called, type:', Array.isArray(results) ? 'array' : 'object');

        // Convert object to array if needed
        if (!Array.isArray(results)) {
            // Results from WebSocket are objects: {varName: value, ...}
            const keys = Object.keys(results);
            console.log('[ResultsPanel] Converting object with', keys.length, 'keys');

            if (keys.length === 0) {
                this.results = [];
            } else {
                const resultsArray: ExecutionResult[] = Object.entries(results).map(([name, value]) => ({
                    id: `result-${Date.now()}-${Math.random()}`,
                    name: name,
                    value: value,
                    type: typeof value,
                    timestamp: Date.now(),
                }));
                this.results = resultsArray;
            }
        } else {
            this.results = [...results];
        }

        this.render();
    }

    /**
     * Clear all results
     */
    clear(): void {
        this.results = [];
        this.treeElement.innerHTML = '';
    }

    /**
     * Get all results
     */
    getResults(): ExecutionResult[] {
        return [...this.results]; // Return copy
    }

    /**
     * Render results tree
     */
    private render(): void {
        // ALWAYS clear first
        this.treeElement.innerHTML = '';
        console.log('[ResultsPanel] Rendering', this.results.length, 'results');

        if (this.results.length === 0) {
            this.treeElement.innerHTML = `
                <div class="empty-state">
                    <i class="fas fa-inbox"></i>
                    <p>No results yet</p>
                    <small>Results will appear after execution</small>
                </div>
            `;
            return;
        }

        // Render each result
        this.results.forEach(result => {
            const item = this.renderResult(result);
            this.treeElement.appendChild(item);
        });

        console.log('[ResultsPanel] Rendered', this.results.length, 'result items to DOM');
    }

    /**
     * Render single result
     */
    private renderResult(result: ExecutionResult): HTMLElement {
        const item = document.createElement('div');
        item.className = 'result-item';
        item.dataset.resultId = result.id;

        const header = document.createElement('div');
        header.className = 'result-header';
        header.style.display = 'flex';
        header.style.alignItems = 'center';
        header.style.gap = '8px';
        header.style.padding = '8px';
        header.style.cursor = 'pointer';

        // Expand/collapse button for complex types
        if (this.isComplexType(result.value)) {
            const expandBtn = document.createElement('button');
            expandBtn.className = 'expand-btn';
            expandBtn.textContent = '▶';
            expandBtn.style.background = 'none';
            expandBtn.style.border = 'none';
            expandBtn.style.cursor = 'pointer';
            expandBtn.style.fontSize = '12px';

            expandBtn.onclick = (e) => {
                e.stopPropagation();
                const body = item.querySelector('.result-body');
                if (body) {
                    const isHidden = (body as HTMLElement).style.display === 'none';
                    (body as HTMLElement).style.display = isHidden ? 'block' : 'none';
                    expandBtn.textContent = isHidden ? '▼' : '▶';
                }
            };

            header.appendChild(expandBtn);
        }

        // Name
        const name = document.createElement('span');
        name.className = 'result-name';
        name.style.fontWeight = '600';
        name.textContent = result.name;
        header.appendChild(name);

        // Type
        const type = document.createElement('span');
        type.className = 'result-type';
        type.style.fontSize = '11px';
        type.style.color = '#999';
        type.textContent = `(${result.type})`;
        header.appendChild(type);

        item.appendChild(header);

        // Value body
        const body = document.createElement('div');
        body.className = 'result-body';
        body.style.paddingLeft = '24px';
        body.style.paddingRight = '8px';
        body.style.paddingBottom = '8px';

        if (this.isComplexType(result.value)) {
            body.style.display = 'none'; // Start collapsed
            body.appendChild(this.renderValue(result.value));
        } else {
            body.textContent = String(result.value);
        }

        item.appendChild(body);

        return item;
    }

    /**
     * Check if value is complex (object/array)
     */
    private isComplexType(value: any): boolean {
        return typeof value === 'object' && value !== null;
    }

    /**
     * Render value (recursive for objects/arrays)
     */
    private renderValue(value: any): HTMLElement {
        const container = document.createElement('div');

        if (Array.isArray(value)) {
            value.forEach((item, index) => {
                const itemEl = document.createElement('div');
                itemEl.style.marginBottom = '4px';

                if (this.isComplexType(item)) {
                    itemEl.textContent = `[${index}]:`;
                    itemEl.appendChild(this.renderValue(item));
                } else {
                    itemEl.textContent = `[${index}]: ${String(item)}`;
                }

                container.appendChild(itemEl);
            });
        } else if (typeof value === 'object') {
            Object.keys(value).forEach(key => {
                const propEl = document.createElement('div');
                propEl.style.marginBottom = '4px';

                if (this.isComplexType(value[key])) {
                    const keySpan = document.createElement('span');
                    keySpan.style.fontWeight = '600';
                    keySpan.textContent = `${key}:`;
                    propEl.appendChild(keySpan);
                    propEl.appendChild(this.renderValue(value[key]));
                } else {
                    propEl.innerHTML = `<span style="font-weight: 600;">${key}:</span> ${String(value[key])}`;
                }

                container.appendChild(propEl);
            });
        } else {
            container.textContent = String(value);
        }

        return container;
    }

    /**
     * Export as JSON
     */
    exportAsJSON(): string {
        return JSON.stringify(this.results, null, 2);
    }

    /**
     * Export as CSV
     */
    exportAsCSV(): string {
        if (this.results.length === 0) {
            return '';
        }

        const header = 'ID,Name,Value,Type,Timestamp\n';
        const rows = this.results.map(result => {
            const value = typeof result.value === 'object'
                ? JSON.stringify(result.value).replace(/"/g, '""')
                : String(result.value);

            return `"${result.id}","${result.name}","${value}","${result.type}",${result.timestamp}`;
        }).join('\n');

        return header + rows;
    }

    /**
     * Find result by ID
     */
    findById(id: string): ExecutionResult | null {
        return this.results.find(r => r.id === id) || null;
    }

    /**
     * Get result count
     */
    getResultCount(): number {
        return this.results.length;
    }
}

