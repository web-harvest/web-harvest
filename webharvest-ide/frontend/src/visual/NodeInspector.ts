/**
 * NodeInspector - Implementation
 * Full observability inspector panel for workflow nodes
 */

import type { NodeExecutionState } from './NodeStateRenderer';

export interface InspectorData {
    nodeId: string;
    pluginType: string;
    status: NodeExecutionState;
    variables?: Record<string, any>;
    dataPreview?: {
        type: 'xml' | 'json' | 'html' | 'text';
        content: string;
    };
    performance?: {
        parseTime?: number;
        networkTime?: number;
        processingTime?: number;
        totalTime?: number;
    };
    httpDetails?: {
        requestUrl?: string;
        requestMethod?: string;
        requestHeaders?: Record<string, string>;
        responseStatus?: number;
        responseHeaders?: Record<string, string>;
        responseTime?: number;
    };
    error?: {
        message: string;
        stackTrace?: string;
        lineNumber?: number;
    };
}

export class NodeInspector {
    private container: HTMLElement | null = null;
    private currentTab: string = 'overview';
    private readonly MAX_PREVIEW_LENGTH = 5000;

    /**
     * Show inspector panel
     */
    show(container: HTMLElement, data: InspectorData): void {
        this.container = container;
        container.innerHTML = '';

        const panel = document.createElement('div');
        panel.className = 'inspector-panel';
        panel.style.cssText = `
            background: white;
            border: 2px solid var(--gray-300);
            border-radius: 8px;
            box-shadow: var(--shadow-lg);
            max-width: 600px;
            max-height: 600px;
            overflow: hidden;
            display: flex;
            flex-direction: column;
        `;

        // Header
        const header = this.createHeader(data);
        panel.appendChild(header);

        // Tabs
        const tabs = this.createTabs(data);
        panel.appendChild(tabs);

        // Content area
        const content = this.createContent(data);
        panel.appendChild(content);

        container.appendChild(panel);
    }

    /**
     * Show inspector near node position
     */
    showNearNode(container: HTMLElement, data: InspectorData, nodePosition: { x: number; y: number }): void {
        this.show(container, data);

        const panel = container.querySelector('.inspector-panel') as HTMLElement;
        if (!panel) return;

        // Position inspector to the right of node
        let left = nodePosition.x + 270; // NODE_WIDTH + 20px gap
        let top = nodePosition.y;

        // Avoid canvas edges
        const canvasWidth = container.clientWidth;
        const canvasHeight = container.clientHeight;

        if (left + 600 > canvasWidth) {
            left = nodePosition.x - 620; // Position to left instead
        }

        if (top + 600 > canvasHeight) {
            top = canvasHeight - 620;
        }

        panel.style.position = 'absolute';
        panel.style.left = `${Math.max(20, left)}px`;
        panel.style.top = `${Math.max(20, top)}px`;
        panel.style.zIndex = '1000';
    }

    /**
     * Hide inspector
     */
    hide(): void {
        if (this.container) {
            this.container.innerHTML = '';
        }
    }

    /**
     * Create header
     */
    private createHeader(data: InspectorData): HTMLElement {
        const header = document.createElement('div');
        header.style.cssText = `
            display: flex;
            align-items: center;
            justify-content: space-between;
            padding: 12px 16px;
            background: linear-gradient(135deg, var(--purple), var(--magenta));
            color: white;
        `;

        const info = document.createElement('div');
        info.className = 'inspector-info';
        info.innerHTML = `<strong>${data.pluginType.toUpperCase()}</strong>`;
        header.appendChild(info);

        const closeBtn = document.createElement('button');
        closeBtn.textContent = '✕';
        closeBtn.style.cssText = `
            background: none;
            border: none;
            color: white;
            font-size: 18px;
            cursor: pointer;
        `;
        closeBtn.addEventListener('click', () => this.hide());
        header.appendChild(closeBtn);

        return header;
    }

    /**
     * Create tabs
     */
    private createTabs(data: InspectorData): HTMLElement {
        const tabsContainer = document.createElement('div');
        tabsContainer.style.cssText = `
            display: flex;
            background: var(--gray-100);
            border-bottom: 1px solid var(--gray-300);
        `;

        const tabs = [
            { id: 'overview', label: 'Overview', condition: true },
            { id: 'variables', label: 'Variables', condition: !!data.variables },
            { id: 'data', label: 'Data', condition: !!data.dataPreview },
            { id: 'performance', label: 'Performance', condition: !!data.performance },
            { id: 'http', label: 'HTTP', condition: !!data.httpDetails },
            { id: 'error', label: 'Error', condition: data.status === 'ERROR' }
        ];

        tabs.forEach(tab => {
            if (!tab.condition) return;

            const tabBtn = document.createElement('button');
            tabBtn.className = `inspector-tab ${tab.id === this.currentTab ? 'active' : ''}`;
            tabBtn.textContent = tab.label;
            tabBtn.style.cssText = `
                padding: 10px 16px;
                border: none;
                background: ${tab.id === this.currentTab ? 'white' : 'transparent'};
                cursor: pointer;
                font-size: 13px;
                font-weight: 600;
                border-bottom: 2px solid ${tab.id === this.currentTab ? 'var(--magenta)' : 'transparent'};
                transition: all 0.2s;
            `;

            tabBtn.addEventListener('click', () => {
                this.switchTab(tab.id);
            });

            tabsContainer.appendChild(tabBtn);
        });

        return tabsContainer;
    }

    /**
     * Create content area
     */
    private createContent(data: InspectorData): HTMLElement {
        const content = document.createElement('div');
        content.className = 'inspector-content';
        content.style.cssText = `
            padding: 16px;
            overflow-y: auto;
            max-height: 450px;
        `;

        switch (this.currentTab) {
            case 'overview':
                content.appendChild(this.createOverview(data));
                break;
            case 'variables':
                content.appendChild(this.createVariablesView(data));
                break;
            case 'data':
                content.appendChild(this.createDataPreview(data));
                break;
            case 'performance':
                content.appendChild(this.createPerformanceView(data));
                break;
            case 'http':
                content.appendChild(this.createHttpView(data));
                break;
            case 'error':
                content.appendChild(this.createErrorView(data));
                break;
        }

        return content;
    }

    /**
     * Create overview tab
     */
    private createOverview(data: InspectorData): HTMLElement {
        const overview = document.createElement('div');
        overview.innerHTML = `
            <div style="margin-bottom: 12px;">
                <strong>Status:</strong> <span style="color: var(--lime);">${data.status}</span>
            </div>
            <div style="margin-bottom: 12px;">
                <strong>Plugin Type:</strong> ${data.pluginType}
            </div>
            <div style="margin-bottom: 12px;">
                <strong>Node ID:</strong> <code style="font-size: 11px;">${data.nodeId}</code>
            </div>
        `;
        return overview;
    }

    /**
     * Create variables view
     */
    private createVariablesView(data: InspectorData): HTMLElement {
        const view = document.createElement('div');
        view.className = 'inspector-variables';

        if (!data.variables || Object.keys(data.variables).length === 0) {
            view.textContent = 'No variables available';
            view.style.cssText = 'color: var(--gray-600); font-style: italic;';
            return view;
        }

        Object.entries(data.variables).forEach(([key, value]) => {
            const varItem = document.createElement('div');
            varItem.style.cssText = 'margin-bottom: 12px; padding: 8px; background: var(--gray-50); border-radius: 4px;';

            const varName = document.createElement('div');
            varName.innerHTML = `<strong>${key}:</strong>`;
            varName.style.cssText = 'font-size: 12px; margin-bottom: 4px; color: var(--purple);';
            varItem.appendChild(varName);

            const varValue = document.createElement('div');
            const valueStr = typeof value === 'string' ? value : JSON.stringify(value, null, 2);
            varValue.textContent = valueStr.length > 200 ? valueStr.substring(0, 200) + '...' : valueStr;
            varValue.style.cssText = 'font-family: monospace; font-size: 11px; color: var(--gray-700); white-space: pre-wrap;';
            varItem.appendChild(varValue);

            view.appendChild(varItem);
        });

        return view;
    }

    /**
     * Create data preview
     */
    private createDataPreview(data: InspectorData): HTMLElement {
        const view = document.createElement('div');
        view.className = 'data-preview';

        if (!data.dataPreview) {
            view.textContent = 'No data preview available';
            view.style.cssText = 'color: var(--gray-600); font-style: italic;';
            return view;
        }

        const preview = document.createElement('pre');
        preview.style.cssText = `
            background: var(--gray-50);
            padding: 12px;
            border-radius: 6px;
            font-family: monospace;
            font-size: 11px;
            overflow-x: auto;
            margin: 0;
        `;

        const formatted = this.formatDataPreview(data.dataPreview.type, data.dataPreview.content);
        preview.innerHTML = formatted;

        view.appendChild(preview);

        return view;
    }

    /**
     * Create performance view
     */
    private createPerformanceView(data: InspectorData): HTMLElement {
        const view = document.createElement('div');
        view.className = 'performance-breakdown';

        if (!data.performance) {
            view.textContent = 'No performance data available';
            view.style.cssText = 'color: var(--gray-600); font-style: italic;';
            return view;
        }

        const items = [
            { label: 'Parse Time', value: data.performance.parseTime },
            { label: 'Network Time', value: data.performance.networkTime },
            { label: 'Processing Time', value: data.performance.processingTime },
            { label: 'Total Time', value: data.performance.totalTime }
        ];

        items.forEach(item => {
            if (item.value !== undefined) {
                const perfItem = document.createElement('div');
                perfItem.style.cssText = 'margin-bottom: 8px; display: flex; justify-content: space-between;';

                perfItem.innerHTML = `
                    <span>${item.label}:</span>
                    <strong>${item.value}ms</strong>
                `;

                view.appendChild(perfItem);
            }
        });

        return view;
    }

    /**
     * Create HTTP details view
     */
    private createHttpView(data: InspectorData): HTMLElement {
        const view = document.createElement('div');
        view.className = 'http-details';

        if (!data.httpDetails) {
            view.textContent = 'No HTTP details available';
            return view;
        }

        const details = data.httpDetails;

        view.innerHTML = `
            <div style="margin-bottom: 12px;">
                <strong>Request:</strong>
                <div style="font-family: monospace; font-size: 11px; margin-top: 4px; color: var(--gray-700);">
                    ${details.requestMethod || 'GET'} ${details.requestUrl || 'N/A'}
                </div>
            </div>
            <div style="margin-bottom: 12px;">
                <strong>Response:</strong>
                <div style="margin-top: 4px;">
                    Status: <span style="color: var(--lime); font-weight: 600;">${details.responseStatus || 'N/A'}</span>
                </div>
                <div>
                    Time: ${details.responseTime || 0}ms
                </div>
            </div>
        `;

        if (details.responseHeaders) {
            const headersDiv = document.createElement('div');
            headersDiv.innerHTML = '<strong>Response Headers:</strong>';
            headersDiv.style.marginTop = '12px';

            const headersList = document.createElement('pre');
            headersList.style.cssText = `
                background: var(--gray-50);
                padding: 8px;
                border-radius: 4px;
                font-size: 11px;
                margin-top: 4px;
            `;
            headersList.textContent = JSON.stringify(details.responseHeaders, null, 2);
            headersDiv.appendChild(headersList);

            view.appendChild(headersDiv);
        }

        return view;
    }

    /**
     * Create error view
     */
    private createErrorView(data: InspectorData): HTMLElement {
        const view = document.createElement('div');
        view.className = 'error-details';

        if (!data.error) {
            view.textContent = 'No error information';
            return view;
        }

        view.innerHTML = `
            <div style="padding: 12px; background: rgba(216, 27, 96, 0.1); border-left: 4px solid var(--magenta); border-radius: 4px; margin-bottom: 12px;">
                <strong style="color: var(--magenta);">Error Message:</strong>
                <div style="margin-top: 6px; font-size: 13px;">${data.error.message}</div>
            </div>
        `;

        if (data.error.stackTrace) {
            const stackTrace = document.createElement('div');
            stackTrace.innerHTML = '<strong>Stack Trace:</strong>';
            stackTrace.style.marginTop = '12px';

            const trace = document.createElement('pre');
            trace.style.cssText = `
                background: var(--gray-50);
                padding: 12px;
                border-radius: 6px;
                font-size: 10px;
                overflow-x: auto;
                margin-top: 6px;
                color: var(--magenta);
            `;
            trace.textContent = data.error.stackTrace;
            stackTrace.appendChild(trace);

            view.appendChild(stackTrace);
        }

        return view;
    }

    /**
     * Switch tab
     */
    switchTab(tabId: string): void {
        this.currentTab = tabId;

        if (!this.container) return;

        // Update tab buttons
        const tabs = this.container.querySelectorAll('.inspector-tab');
        tabs.forEach(tab => {
            tab.classList.remove('active');
            (tab as HTMLElement).style.background = 'transparent';
            (tab as HTMLElement).style.borderBottom = '2px solid transparent';
        });

        const activeTab = this.container.querySelector(`.inspector-tab[data-tab="${tabId}"]`) as HTMLElement;
        if (activeTab) {
            activeTab.classList.add('active');
            activeTab.style.background = 'white';
            activeTab.style.borderBottom = '2px solid var(--magenta)';
        }
    }

    /**
     * Format data preview with syntax highlighting
     */
    formatDataPreview(type: string, content: string): string {
        // Truncate if too large
        if (content.length > this.MAX_PREVIEW_LENGTH) {
            content = content.substring(0, this.MAX_PREVIEW_LENGTH) + '\n\n... (truncated)';
        }

        switch (type) {
            case 'xml':
                return this.highlightXML(content);

            case 'json':
                return this.highlightJSON(content);

            case 'html':
                return this.highlightHTML(content);

            case 'text':
            default:
                return content;
        }
    }

    /**
     * Highlight XML syntax
     */
    private highlightXML(xml: string): string {
        return xml
            .replace(/</g, '&lt;')
            .replace(/>/g, '&gt;')
            .replace(/&lt;(\/?[\w-]+)/g, '<span style="color: var(--purple); font-weight: 600;">&lt;$1</span>')
            .replace(/([\w-]+)=/g, '<span style="color: var(--lime);">$1</span>=')
            .replace(/"([^"]*)"/g, '<span style="color: var(--magenta);">"$1"</span>');
    }

    /**
     * Highlight JSON syntax
     */
    private highlightJSON(json: string): string {
        try {
            const parsed = JSON.parse(json);
            const formatted = JSON.stringify(parsed, null, 2);

            return formatted
                .replace(/"([^"]+)":/g, '<span style="color: var(--purple);">"$1"</span>:')
                .replace(/: "([^"]*)"/g, ': <span style="color: var(--magenta);">"$1"</span>')
                .replace(/: (\d+)/g, ': <span style="color: var(--lime);">$1</span>')
                .replace(/: (true|false|null)/g, ': <span style="color: var(--yellow);">$1</span>');
        } catch {
            return json;
        }
    }

    /**
     * Highlight HTML syntax
     */
    private highlightHTML(html: string): string {
        return this.highlightXML(html); // Same as XML
    }
}


