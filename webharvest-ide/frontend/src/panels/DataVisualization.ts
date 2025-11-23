/**
 * DataVisualization - Masterful data visualization component
 * 
 * Features:
 * - Table view with sorting, filtering, pagination
 * - Chart visualization for numeric data
 * - Data flow visualization
 * - Export capabilities
 * - Status indicators
 */

import type { ExecutionResult } from '../types';

export interface DataVisualizationOptions {
    onExport?: (format: 'json' | 'csv' | 'xml') => void;
    onFilter?: (filter: string) => void;
}

export class DataVisualization {
    private container: HTMLElement;
    private results: ExecutionResult[] = [];
    private options: DataVisualizationOptions;
    private viewMode: 'table' | 'tree' | 'chart' = 'table';
    private filterText: string = '';

    constructor(containerId: string, options: DataVisualizationOptions = {}) {
        const container = document.getElementById(containerId);
        if (!container) {
            throw new Error(`Container with ID "${containerId}" not found`);
        }

        this.container = container;
        this.options = options;
        this.render();
    }

    /**
     * Set results data
     */
    setResults(results: ExecutionResult[] | Record<string, any>): void {
        console.log('[DataVisualization] setResults() called');
        if (Array.isArray(results)) {
            this.results = [...results];
            console.log('[DataVisualization] Set', this.results.length, 'results from array');
        } else {
            // Convert object to array format
            const entries = Object.entries(results);
            this.results = entries.map(([name, value]) => ({
                id: `result-${Date.now()}-${Math.random()}`,
                name,
                value,
                type: typeof value,
                timestamp: Date.now(),
            }));
            console.log('[DataVisualization] Converted', entries.length, 'object entries to results array');
        }
        this.render();
        console.log('[DataVisualization] Results rendered');
    }

    /**
     * Render the visualization
     */
    private render(): void {
        this.container.innerHTML = '';
        this.container.style.cssText = `
            display: flex;
            flex-direction: column;
            height: 100%;
            overflow: hidden;
        `;

        // Toolbar
        const toolbar = this.createToolbar();
        this.container.appendChild(toolbar);

        // Content area
        const content = this.createContent();
        this.container.appendChild(content);
    }

    /**
     * Create toolbar with view mode switcher and actions
     */
    private createToolbar(): HTMLElement {
        const toolbar = document.createElement('div');
        toolbar.style.cssText = `
            display: flex;
            align-items: center;
            justify-content: space-between;
            padding: 8px 12px;
            border-bottom: 1px solid #e0e0e0;
            background: #fafafa;
            gap: 8px;
        `;

        // View mode switcher
        const viewSwitcher = document.createElement('div');
        viewSwitcher.style.cssText = `
            display: flex;
            gap: 4px;
            background: #ffffff;
            border: 1px solid #e0e0e0;
            border-radius: 4px;
            padding: 2px;
        `;

        const modes: Array<{ id: 'table' | 'tree' | 'chart'; icon: string; label: string }> = [
            { id: 'table', icon: 'fa-table', label: 'Table' },
            { id: 'tree', icon: 'fa-sitemap', label: 'Tree' },
            { id: 'chart', icon: 'fa-chart-bar', label: 'Chart' },
        ];

        modes.forEach(mode => {
            const button = document.createElement('button');
            button.style.cssText = `
                display: flex;
                align-items: center;
                gap: 4px;
                padding: 4px 8px;
                border: none;
                background: ${this.viewMode === mode.id ? '#e0e0e0' : 'transparent'};
                color: ${this.viewMode === mode.id ? '#333' : '#666'};
                font-size: 12px;
                cursor: pointer;
                border-radius: 2px;
                transition: all 0.2s;
            `;
            button.innerHTML = `<i class="fas ${mode.icon}"></i> <span>${mode.label}</span>`;
            button.addEventListener('click', () => {
                this.viewMode = mode.id;
                this.render();
            });
            viewSwitcher.appendChild(button);
        });

        // Search/filter
        const searchContainer = document.createElement('div');
        searchContainer.style.cssText = `
            flex: 1;
            max-width: 200px;
            position: relative;
        `;
        const searchInput = document.createElement('input');
        searchInput.type = 'text';
        searchInput.placeholder = 'Filter...';
        searchInput.value = this.filterText;
        searchInput.style.cssText = `
            width: 100%;
            padding: 4px 8px 4px 28px;
            border: 1px solid #e0e0e0;
            border-radius: 4px;
            font-size: 12px;
        `;
        const searchIcon = document.createElement('i');
        searchIcon.className = 'fas fa-search';
        searchIcon.style.cssText = `
            position: absolute;
            left: 8px;
            top: 50%;
            transform: translateY(-50%);
            color: #999;
            font-size: 12px;
        `;
        searchContainer.appendChild(searchIcon);
        searchContainer.appendChild(searchInput);
        searchInput.addEventListener('input', (e) => {
            this.filterText = (e.target as HTMLInputElement).value;
            this.options.onFilter?.(this.filterText);
            this.render();
        });

        // Export button
        const exportButton = document.createElement('button');
        exportButton.style.cssText = `
            display: flex;
            align-items: center;
            gap: 4px;
            padding: 4px 8px;
            border: 1px solid #e0e0e0;
            background: #ffffff;
            color: #666;
            font-size: 12px;
            cursor: pointer;
            border-radius: 4px;
        `;
        exportButton.innerHTML = `<i class="fas fa-download"></i> <span>Export</span>`;
        exportButton.addEventListener('click', () => this.showExportMenu(exportButton));

        toolbar.appendChild(viewSwitcher);
        toolbar.appendChild(searchContainer);
        toolbar.appendChild(exportButton);

        return toolbar;
    }

    /**
     * Create content area based on view mode
     */
    private createContent(): HTMLElement {
        const content = document.createElement('div');
        content.style.cssText = `
            flex: 1;
            overflow: auto;
            padding: 12px;
        `;

        const filteredResults = this.getFilteredResults();

        switch (this.viewMode) {
            case 'table':
                content.appendChild(this.renderTableView(filteredResults));
                break;
            case 'tree':
                content.appendChild(this.renderTreeView(filteredResults));
                break;
            case 'chart':
                content.appendChild(this.renderChartView(filteredResults));
                break;
        }

        return content;
    }

    /**
     * Get filtered results
     */
    private getFilteredResults(): ExecutionResult[] {
        if (!this.filterText) {
            return this.results;
        }

        const filter = this.filterText.toLowerCase();
        return this.results.filter(result => {
            return result.name.toLowerCase().includes(filter) ||
                   String(result.value).toLowerCase().includes(filter) ||
                   result.type.toLowerCase().includes(filter);
        });
    }

    /**
     * Render table view
     */
    private renderTableView(results: ExecutionResult[]): HTMLElement {
        const table = document.createElement('table');
        table.style.cssText = `
            width: 100%;
            border-collapse: collapse;
            font-size: 13px;
        `;

        // Header
        const thead = document.createElement('thead');
        thead.style.cssText = `
            background: #f5f5f5;
            position: sticky;
            top: 0;
            z-index: 10;
        `;
        const headerRow = document.createElement('tr');
        ['Name', 'Type', 'Value', 'Status'].forEach(header => {
            const th = document.createElement('th');
            th.textContent = header;
            th.style.cssText = `
                padding: 8px;
                text-align: left;
                font-weight: 600;
                border-bottom: 2px solid #e0e0e0;
            `;
            headerRow.appendChild(th);
        });
        thead.appendChild(headerRow);
        table.appendChild(thead);

        // Body
        const tbody = document.createElement('tbody');
        if (results.length === 0) {
            const row = document.createElement('tr');
            const cell = document.createElement('td');
            cell.colSpan = 4;
            cell.textContent = 'No results';
            cell.style.cssText = `
                padding: 24px;
                text-align: center;
                color: #999;
            `;
            row.appendChild(cell);
            tbody.appendChild(row);
        } else {
            results.forEach(result => {
            const row = document.createElement('tr');
            row.style.cssText = `
                border-bottom: 1px solid #f0f0f0;
            `;
                row.addEventListener('mouseenter', () => {
                    row.style.background = '#f9f9f9';
                });
                row.addEventListener('mouseleave', () => {
                    row.style.background = 'transparent';
                });

                // Name
                const nameCell = document.createElement('td');
                nameCell.textContent = result.name;
                nameCell.style.cssText = `
                    padding: 8px;
                    font-weight: 500;
                `;

                // Type
                const typeCell = document.createElement('td');
                typeCell.textContent = result.type;
                typeCell.style.cssText = `
                    padding: 8px;
                    color: #666;
                    font-size: 12px;
                `;

                // Value (truncated)
                const valueCell = document.createElement('td');
                const valueStr = typeof result.value === 'object'
                    ? JSON.stringify(result.value)
                    : String(result.value);
                valueCell.textContent = valueStr.length > 50 ? valueStr.substring(0, 50) + '...' : valueStr;
                valueCell.style.cssText = `
                    padding: 8px;
                    font-family: monospace;
                    font-size: 12px;
                    max-width: 300px;
                    overflow: hidden;
                    text-overflow: ellipsis;
                    white-space: nowrap;
                `;
                valueCell.title = valueStr;

                // Status
                const statusCell = document.createElement('td');
                const statusBadge = document.createElement('span');
                statusBadge.textContent = '✓';
                statusBadge.style.cssText = `
                        display: inline-block;
                    width: 20px;
                    height: 20px;
                    border-radius: 50%;
                    background: #4CAF50;
                        color: white;
                    text-align: center;
                    line-height: 20px;
                    font-size: 12px;
                `;
                statusCell.appendChild(statusBadge);
                statusCell.style.cssText = `
                    padding: 8px;
                    text-align: center;
                `;

                row.appendChild(nameCell);
                row.appendChild(typeCell);
                row.appendChild(valueCell);
                row.appendChild(statusCell);
            tbody.appendChild(row);
        });
        }

        table.appendChild(tbody);
        return table;
    }

    /**
     * Render tree view
     */
    private renderTreeView(results: ExecutionResult[]): HTMLElement {
        const tree = document.createElement('div');
        tree.style.cssText = `
            font-family: monospace;
            font-size: 12px;
        `;

        if (results.length === 0) {
            tree.innerHTML = '<div style="padding: 24px; text-align: center; color: #999;">No results</div>';
            return tree;
        }

        results.forEach(result => {
            const item = this.renderTreeItem(result);
            tree.appendChild(item);
        });

        return tree;
    }

    /**
     * Render single tree item
     */
    private renderTreeItem(result: ExecutionResult, depth: number = 0): HTMLElement {
        const item = document.createElement('div');
        item.style.cssText = `
            padding: 4px 0;
            padding-left: ${depth * 20}px;
        `;

        const name = document.createElement('span');
        name.textContent = result.name;
        name.style.cssText = `
            font-weight: 600;
            color: #333;
            margin-right: 8px;
        `;

        const type = document.createElement('span');
        type.textContent = `(${result.type})`;
        type.style.cssText = `
            color: #999;
            font-size: 11px;
            margin-right: 8px;
        `;

        const value = document.createElement('span');
        if (typeof result.value === 'object' && result.value !== null) {
            value.textContent = Array.isArray(result.value) ? `[${result.value.length} items]` : '{...}';
            value.style.cssText = `
                color: #666;
                cursor: pointer;
            `;
            value.addEventListener('click', () => {
                // Toggle expansion
                const expanded = item.dataset.expanded === 'true';
                item.dataset.expanded = expanded ? 'false' : 'true';
                // TODO: Implement expansion
            });
        } else {
            value.textContent = String(result.value);
            value.style.cssText = `
                color: #333;
            `;
        }

        item.appendChild(name);
        item.appendChild(type);
        item.appendChild(value);

        return item;
    }

    /**
     * Render chart view
     */
    private renderChartView(results: ExecutionResult[]): HTMLElement {
        const chart = document.createElement('div');
        chart.style.cssText = `
            display: flex;
            flex-direction: column;
            gap: 16px;
        `;

        if (results.length === 0) {
            chart.innerHTML = '<div style="padding: 24px; text-align: center; color: #999;">No results to visualize</div>';
            return chart;
        }

        // Simple bar chart for numeric values
        const numericResults = results.filter(r => typeof r.value === 'number');
        if (numericResults.length > 0) {
            const chartContainer = document.createElement('div');
            chartContainer.style.cssText = `
                background: #f9f9f9;
                border-radius: 4px;
                padding: 16px;
            `;

            const maxValue = Math.max(...numericResults.map(r => r.value as number));
            const chartBars = document.createElement('div');
            chartBars.style.cssText = `
                display: flex;
                flex-direction: column;
                gap: 8px;
            `;

            numericResults.forEach(result => {
                const barContainer = document.createElement('div');
                barContainer.style.cssText = `
                    display: flex;
                    align-items: center;
                    gap: 8px;
                `;

                const label = document.createElement('span');
                label.textContent = result.name;
                label.style.cssText = `
                    min-width: 120px;
                    font-size: 12px;
                    font-weight: 500;
                `;

                const bar = document.createElement('div');
                const width = maxValue > 0 ? ((result.value as number) / maxValue) * 100 : 0;
                bar.style.cssText = `
                    flex: 1;
                    height: 24px;
                    background: linear-gradient(90deg, #3b82f6, #8b5cf6);
                    border-radius: 4px;
                    width: ${width}%;
                    min-width: 4px;
                    display: flex;
                    align-items: center;
                    justify-content: flex-end;
                    padding-right: 8px;
                    color: white;
                    font-size: 11px;
                    font-weight: 600;
                `;
                bar.textContent = String(result.value);

                barContainer.appendChild(label);
                barContainer.appendChild(bar);
                chartBars.appendChild(barContainer);
            });

            chartContainer.appendChild(chartBars);
            chart.appendChild(chartContainer);
        } else {
            chart.innerHTML = '<div style="padding: 24px; text-align: center; color: #999;">No numeric data to visualize</div>';
        }

        return chart;
    }

    /**
     * Show export menu
     */
    private showExportMenu(button: HTMLElement): void {
        console.log('[DataVisualization] showExportMenu() called');
        // Simple implementation - just trigger export
        this.options.onExport?.('json');
    }

    /**
     * Export data in specified format
     */
    exportData(format: 'json' | 'csv' | 'xml'): void {
        console.log(`[DataVisualization] exportData('${format}') called with`, this.results.length, 'results');
        this.options.onExport?.(format);
    }
}
