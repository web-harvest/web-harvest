/**
 * RightSidebar - Modern, ergonomic right panel redesign
 * 
 * Features:
 * - Split-view with resizable sections
 * - Accordion for collapsible sections
 * - Masterful data visualization
 * - Flow control and session management
 */

export type SidebarSection = 'properties' | 'execution' | 'data' | 'monitoring';

export interface RightSidebarOptions {
    onSectionChange?: (section: SidebarSection) => void;
    onResize?: (sizes: { properties: number; execution: number; data: number; monitoring: number }) => void;
}

export class RightSidebar {
    private container: HTMLElement;
    private activeSection: SidebarSection = 'execution';
    private options: RightSidebarOptions;
    private sections: Map<SidebarSection, HTMLElement> = new Map();
    private resizeHandles: HTMLElement[] = [];

    constructor(containerId: string, options: RightSidebarOptions = {}) {
        const container = document.getElementById(containerId);
        if (!container) {
            throw new Error(`Container with ID "${containerId}" not found`);
        }

        this.container = container;
        this.options = options;
        this.render();
    }

    /**
     * Render the complete sidebar structure
     */
    private render(): void {
        this.container.innerHTML = '';
        this.container.className = 'right-sidebar';
        this.container.style.cssText = `
            display: flex;
            flex-direction: column;
            width: 100%;
            height: 100%;
            background: #ffffff;
            border-left: 1px solid #e0e0e0;
            overflow: hidden;
        `;

        // Header with section tabs
        const header = this.createHeader();
        this.container.appendChild(header);

        // Main content area with split-view
        const content = this.createContentArea();
        this.container.appendChild(content);

        // Initialize default section
        this.showSection(this.activeSection);
    }

    /**
     * Create header with section navigation
     */
    private createHeader(): HTMLElement {
        const header = document.createElement('div');
        header.className = 'right-sidebar__header';
        header.style.cssText = `
            display: flex;
            align-items: center;
            gap: 4px;
            padding: 8px 12px;
            border-bottom: 1px solid #e0e0e0;
            background: #fafafa;
        `;

        const sections: Array<{ id: SidebarSection; label: string; icon: string }> = [
            { id: 'properties', label: 'Properties', icon: 'fa-cog' },
            { id: 'execution', label: 'Execution', icon: 'fa-bolt' },
            { id: 'data', label: 'Data Flow', icon: 'fa-stream' },
            { id: 'monitoring', label: 'Monitoring', icon: 'fa-chart-line' },
        ];

        sections.forEach(section => {
            const button = document.createElement('button');
            button.className = 'right-sidebar__tab';
            button.dataset.section = section.id;
            button.style.cssText = `
                display: flex;
                align-items: center;
                gap: 6px;
                padding: 6px 12px;
                border: none;
                background: transparent;
                color: #666;
                font-size: 13px;
                font-weight: 500;
                cursor: pointer;
                border-radius: 4px;
                transition: all 0.2s;
            `;

            button.innerHTML = `
                <i class="fas ${section.icon}"></i>
                <span>${section.label}</span>
            `;

            button.addEventListener('click', () => this.showSection(section.id));
            button.addEventListener('mouseenter', () => {
                if (this.activeSection !== section.id) {
                    button.style.background = '#f0f0f0';
                }
            });
            button.addEventListener('mouseleave', () => {
                if (this.activeSection !== section.id) {
                    button.style.background = 'transparent';
                }
            });

            header.appendChild(button);
        });

        return header;
    }

    /**
     * Create main content area with split-view sections
     */
    private createContentArea(): HTMLElement {
        const content = document.createElement('div');
        content.className = 'right-sidebar__content';
        content.style.cssText = `
            flex: 1;
            display: flex;
            flex-direction: column;
            overflow: hidden;
            position: relative;
        `;

        // Properties section (collapsible)
        const propertiesSection = this.createPropertiesSection();
        this.sections.set('properties', propertiesSection);
        content.appendChild(propertiesSection);

        // Execution section (main, always visible when active)
        const executionSection = this.createExecutionSection();
        this.sections.set('execution', executionSection);
        content.appendChild(executionSection);

        // Data Flow section (with visualization)
        const dataSection = this.createDataSection();
        this.sections.set('data', dataSection);
        content.appendChild(dataSection);

        // Initialize DataVisualization component
        this.initializeDataVisualization(dataSection);

        // Monitoring section (logs + metrics)
        const monitoringSection = this.createMonitoringSection();
        this.sections.set('monitoring', monitoringSection);
        content.appendChild(monitoringSection);

        return content;
    }

    /**
     * Create Properties section
     */
    private createPropertiesSection(): HTMLElement {
        const section = document.createElement('div');
        section.className = 'right-sidebar__section right-sidebar__section--properties';
        section.dataset.section = 'properties';
        section.style.cssText = `
            display: none;
            flex-direction: column;
            height: 100%;
            overflow: hidden;
        `;

        const placeholder = document.createElement('div');
        placeholder.className = 'right-sidebar__placeholder';
        placeholder.style.cssText = `
            flex: 1;
            display: flex;
            flex-direction: column;
            align-items: center;
            justify-content: center;
            padding: 24px;
            color: #999;
            text-align: center;
        `;
        placeholder.innerHTML = `
            <i class="fas fa-mouse-pointer" style="font-size: 32px; margin-bottom: 12px; opacity: 0.5;"></i>
            <p style="margin: 0; font-size: 14px;">Select a plugin on the canvas to view its properties</p>
        `;

        section.appendChild(placeholder);
        return section;
    }

    /**
     * Create Execution section
     */
    private createExecutionSection(): HTMLElement {
        const section = document.createElement('div');
        section.className = 'right-sidebar__section right-sidebar__section--execution';
        section.dataset.section = 'execution';
        section.style.cssText = `
            display: none;
            flex-direction: column;
            height: 100%;
            overflow: hidden;
        `;

        // Execution controls will be injected here by RunWorkflowPanel
        const controlsContainer = document.createElement('div');
        controlsContainer.id = 'executionControls';
        controlsContainer.style.cssText = `
            flex: 1;
            overflow-y: auto;
        `;

        section.appendChild(controlsContainer);
        return section;
    }

    /**
     * Create Data Flow section with advanced visualization
     */
    private createDataSection(): HTMLElement {
        const section = document.createElement('div');
        section.className = 'right-sidebar__section right-sidebar__section--data';
        section.dataset.section = 'data';
        section.style.cssText = `
            display: none;
            flex-direction: column;
            height: 100%;
            overflow: hidden;
        `;

        // Split view: Results tree + Variables
        const splitContainer = document.createElement('div');
        splitContainer.style.cssText = `
            flex: 1;
            display: flex;
            flex-direction: column;
            overflow: hidden;
        `;

        // Results section (top, resizable)
        const resultsHeader = document.createElement('div');
        resultsHeader.style.cssText = `
            display: flex;
            align-items: center;
            justify-content: space-between;
            padding: 8px 12px;
            border-bottom: 1px solid #e0e0e0;
            background: #fafafa;
        `;
        resultsHeader.innerHTML = `
            <h4 style="margin: 0; font-size: 13px; font-weight: 600; color: #333;">
                <i class="fas fa-table" style="margin-right: 6px;"></i>Results
            </h4>
            <button class="btn-icon" onclick="window.app.exportResults()" title="Export Results" style="
                background: none;
                border: none;
                cursor: pointer;
                color: #666;
                padding: 4px 8px;
                border-radius: 4px;
            ">
                <i class="fas fa-download"></i>
            </button>
        `;

        // Use DataVisualization component for advanced results display
        const resultsContainer = document.createElement('div');
        resultsContainer.id = 'results';
        resultsContainer.className = 'results-tree';
        resultsContainer.style.cssText = `
            flex: 1;
            overflow: hidden;
            min-height: 0;
            display: flex;
            flex-direction: column;
        `;

        const resultsSection = document.createElement('div');
        resultsSection.style.cssText = `
            flex: 1;
            display: flex;
            flex-direction: column;
            min-height: 0;
            border-bottom: 1px solid #e0e0e0;
        `;
        resultsSection.appendChild(resultsHeader);
        resultsSection.appendChild(resultsContainer);

        // Variables section (bottom)
        const variablesHeader = document.createElement('div');
        variablesHeader.style.cssText = `
            display: flex;
            align-items: center;
            justify-content: space-between;
            padding: 8px 12px;
            border-bottom: 1px solid #e0e0e0;
            background: #fafafa;
        `;
        variablesHeader.innerHTML = `
            <h4 style="margin: 0; font-size: 13px; font-weight: 600; color: #333;">
                <i class="fas fa-box" style="margin-right: 6px;"></i>Variables
            </h4>
            <span class="badge" id="varsBadge" style="
                background: #e0e0e0;
                color: #666;
                padding: 2px 6px;
                border-radius: 10px;
                font-size: 11px;
            ">0</span>
        `;

        const variablesContainer = document.createElement('div');
        variablesContainer.id = 'variables';
        variablesContainer.style.cssText = `
            flex: 1;
            overflow-y: auto;
            min-height: 0;
        `;

        const variablesSection = document.createElement('div');
        variablesSection.style.cssText = `
            flex: 1;
            display: flex;
            flex-direction: column;
            min-height: 0;
        `;
        variablesSection.appendChild(variablesHeader);
        variablesSection.appendChild(variablesContainer);

        splitContainer.appendChild(resultsSection);
        splitContainer.appendChild(variablesSection);

        // Add data visualization container
        const vizContainer = document.createElement('div');
        vizContainer.id = 'dataVisualization';
        vizContainer.style.cssText = `
            flex: 1;
            display: flex;
            flex-direction: column;
            min-height: 0;
            border-top: 1px solid #e0e0e0;
        `;
        splitContainer.appendChild(vizContainer);

        section.appendChild(splitContainer);
        return section;
    }

    /**
     * Initialize DataVisualization component
     */
    private initializeDataVisualization(dataSection: HTMLElement): void {
        // DataVisualization will be initialized by App.ts after RightSidebar is created
        // This method is a placeholder for future integration
    }

    /**
     * Create Monitoring section (logs + metrics)
     */
    private createMonitoringSection(): HTMLElement {
        const section = document.createElement('div');
        section.className = 'right-sidebar__section right-sidebar__section--monitoring';
        section.dataset.section = 'monitoring';
        section.style.cssText = `
            display: none;
            flex-direction: column;
            height: 100%;
            overflow: hidden;
        `;

        // Logs section
        const logsHeader = document.createElement('div');
        logsHeader.style.cssText = `
            display: flex;
            align-items: center;
            justify-content: space-between;
            padding: 8px 12px;
            border-bottom: 1px solid #e0e0e0;
            background: #fafafa;
        `;
        logsHeader.innerHTML = `
            <h4 style="margin: 0; font-size: 13px; font-weight: 600; color: #333;">
                <i class="fas fa-list-ul" style="margin-right: 6px;"></i>Execution Logs
            </h4>
            <div style="display: flex; align-items: center; gap: 8px;">
                <span class="badge" id="logsBadge" style="
                    background: #e0e0e0;
                    color: #666;
                    padding: 2px 6px;
                    border-radius: 10px;
                    font-size: 11px;
                ">0</span>
                <button class="btn-icon" onclick="window.app.clearLogs()" title="Clear Logs" style="
                    background: none;
                    border: none;
                    cursor: pointer;
                    color: #666;
                    padding: 4px 8px;
                    border-radius: 4px;
                ">
                    <i class="fas fa-trash"></i>
                </button>
            </div>
        `;

        const logsContainer = document.createElement('div');
        logsContainer.id = 'logs';
        logsContainer.className = 'logs-list';
        logsContainer.style.cssText = `
            flex: 1;
            overflow-y: auto;
            min-height: 0;
        `;

        section.appendChild(logsHeader);
        section.appendChild(logsContainer);

        return section;
    }

    /**
     * Show specific section
     */
    showSection(sectionId: SidebarSection): void {
        // Update active tab
        const tabs = this.container.querySelectorAll('.right-sidebar__tab');
        tabs.forEach(tabEl => {
            const tab = tabEl as HTMLElement;
            const tabSection = tab.getAttribute('data-section');
            if (tabSection === sectionId) {
                tab.style.background = '#e0e0e0';
                tab.style.color = '#333';
            } else {
                tab.style.background = 'transparent';
                tab.style.color = '#666';
            }
        });

        // Show/hide sections
        this.sections.forEach((section, id) => {
            if (id === sectionId) {
                section.style.display = 'flex';
            } else {
                section.style.display = 'none';
            }
        });

        this.activeSection = sectionId;
        this.options.onSectionChange?.(sectionId);
    }

    /**
     * Get properties container (for PropertyPanel injection)
     */
    getPropertiesContainer(): HTMLElement {
        const section = this.sections.get('properties');
        if (!section) {
            throw new Error('Properties section not found');
        }
        return section.querySelector('.right-sidebar__placeholder') || section;
    }

    /**
     * Get data section (for DataVisualization injection)
     */
    getDataSection(): HTMLElement | null {
        return this.sections.get('data') || null;
    }

    /**
     * Get active section
     */
    getActiveSection(): SidebarSection {
        return this.activeSection;
    }
}

