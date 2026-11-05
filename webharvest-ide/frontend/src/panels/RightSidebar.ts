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
    private isResizing: boolean = false;
    private resizeStartX: number = 0;
    private resizeStartWidth: number = 0;

    constructor(containerId: string, options: RightSidebarOptions = {}) {
        const container = document.getElementById(containerId);
        if (!container) {
            throw new Error(`Container with ID "${containerId}" not found`);
        }

        this.container = container;
        this.options = options;
        
        // Ensure sidebar-section is hidden before rendering
        // Container itself may have sidebar-section class
        let sidebarSection: HTMLElement | null = null;
        if (container.classList.contains('sidebar-section')) {
            sidebarSection = container;
        } else {
            sidebarSection = container.closest('.sidebar-section') as HTMLElement;
        }
        
        if (sidebarSection) {
            sidebarSection.classList.remove('sidebar-section--visible');
            sidebarSection.style.setProperty('display', 'none', 'important');
            sidebarSection.style.setProperty('width', '0', 'important');
            sidebarSection.style.setProperty('min-width', '0', 'important');
            sidebarSection.style.setProperty('max-width', '0', 'important');
            sidebarSection.style.setProperty('flex', '0 0 0', 'important');
        }
        
        this.render();
        this.setupResizeHandle();
        
        // Ensure sidebar is still hidden after rendering
        if (sidebarSection) {
            sidebarSection.classList.remove('sidebar-section--visible');
            sidebarSection.style.setProperty('display', 'none', 'important');
            sidebarSection.style.setProperty('width', '0', 'important');
            sidebarSection.style.setProperty('min-width', '0', 'important');
            sidebarSection.style.setProperty('max-width', '0', 'important');
            sidebarSection.style.setProperty('flex', '0 0 0', 'important');
        }
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

        // Create container for PropertyPanel (use propertyPanel ID for compatibility)
        const propertyContainer = document.createElement('div');
        propertyContainer.id = 'propertyPanel';
        propertyContainer.style.cssText = `
            flex: 1;
            display: flex;
            flex-direction: column;
            overflow: hidden;
            min-height: 0;
        `;

        // Add placeholder initially
        const placeholder = document.createElement('div');
        placeholder.className = 'right-sidebar__placeholder';
        placeholder.id = 'property-panel-placeholder';
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

        propertyContainer.appendChild(placeholder);
        section.appendChild(propertyContainer);
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
            padding: 20px 24px;
            box-sizing: border-box;
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
            overflow-y: auto;
            min-height: 0;
            display: flex;
            flex-direction: column;
            padding: 16px 24px;
            box-sizing: border-box;
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
            padding: 16px 24px;
            box-sizing: border-box;
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
            padding: 16px 24px;
            box-sizing: border-box;
        `;

        section.appendChild(logsHeader);
        section.appendChild(logsContainer);

        return section;
    }

    /**
     * Activate a section (show it, hide others) - alias for showSection for compatibility
     */
    activateSection(sectionId: SidebarSection): void {
        this.showSection(sectionId);
    }

    /**
     * Get the currently active section
     */
    getActiveSection(): SidebarSection {
        return this.activeSection;
    }

    /**
     * Check if a section is currently active
     */
    isSectionActive(sectionId: SidebarSection): boolean {
        return this.activeSection === sectionId;
    }

    /**
     * Show specific section
     */
    showSection(sectionId: SidebarSection): void {
        console.log(`[RightSidebar] showSection('${sectionId}') called`);
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
                console.log(`[RightSidebar] Showing section '${id}'`);
            } else {
                section.style.display = 'none';
            }
        });

        this.activeSection = sectionId;
        this.options.onSectionChange?.(sectionId);
        console.log(`[RightSidebar] Section '${sectionId}' activated`);
    }

    /**
     * Get properties container (for PropertyPanel injection)
     */
    getPropertiesContainer(): HTMLElement {
        const section = this.sections.get('properties');
        if (!section) {
            throw new Error('Properties section not found');
        }
        // Return the propertyPanel container (ID for compatibility with PropertyPanel)
        const container = section.querySelector('#propertyPanel') as HTMLElement;
        return container || section;
    }
    
    /**
     * Show the sidebar
     */
    show(): void {
        // Try both: container itself (if it has sidebar-section class) and closest parent
        let sidebar = this.container.classList.contains('sidebar-section') 
            ? this.container as HTMLElement 
            : this.container.closest('.sidebar-section') as HTMLElement;
        
        if (!sidebar) {
            // Fallback: use container directly
            sidebar = this.container;
        }
        
        if (sidebar) {
            console.log('[RightSidebar] show() called - showing sidebar');
            sidebar.classList.add('sidebar-section--visible');
            sidebar.style.setProperty('display', 'flex', 'important');
            sidebar.style.setProperty('width', '320px', 'important');
            sidebar.style.setProperty('min-width', '280px', 'important');
            sidebar.style.setProperty('max-width', '400px', 'important');
            sidebar.style.setProperty('flex', '0 0 320px', 'important');
            // Force grid update
            const ideMain = sidebar.closest('.ide-main') as HTMLElement;
            if (ideMain) {
                ideMain.style.setProperty('grid-template-columns', 'auto 1fr 320px', 'important');
            }
            console.log('[RightSidebar] Sidebar shown, classes:', sidebar.classList.toString());
        } else {
            console.error('[RightSidebar] show() - sidebar element not found!');
        }
    }
    
    /**
     * Hide the sidebar
     */
    hide(): void {
        // Try both: container itself (if it has sidebar-section class) and closest parent
        let sidebar = this.container.classList.contains('sidebar-section') 
            ? this.container as HTMLElement 
            : this.container.closest('.sidebar-section') as HTMLElement;
        
        if (!sidebar) {
            // Fallback: use container directly
            sidebar = this.container;
        }
        
        if (sidebar) {
            console.log('[RightSidebar] hide() called - hiding sidebar');
            sidebar.classList.remove('sidebar-section--visible');
            // Use !important to override CSS
            sidebar.style.setProperty('display', 'none', 'important');
            sidebar.style.setProperty('width', '0', 'important');
            sidebar.style.setProperty('min-width', '0', 'important');
            sidebar.style.setProperty('max-width', '0', 'important');
            sidebar.style.setProperty('flex', '0 0 0', 'important');
            // Force grid update
            const ideMain = sidebar.closest('.ide-main') as HTMLElement;
            if (ideMain) {
                ideMain.style.setProperty('grid-template-columns', 'auto 1fr 0', 'important');
            }
            console.log('[RightSidebar] Sidebar hidden, computed display:', window.getComputedStyle(sidebar).display);
        } else {
            console.error('[RightSidebar] hide() - sidebar element not found!');
        }
    }
    
    /**
     * Check if sidebar is visible
     */
    isVisible(): boolean {
        // Check both: container itself (if it has sidebar-section class) and closest parent
        let sidebar = this.container.classList.contains('sidebar-section') 
            ? this.container as HTMLElement 
            : this.container.closest('.sidebar-section') as HTMLElement;
        
        if (!sidebar) {
            // Fallback: use container directly
            sidebar = this.container;
        }
        
        if (sidebar) {
            const hasVisible = sidebar.classList.contains('sidebar-section--visible');
            const computedDisplay = window.getComputedStyle(sidebar).display;
            const computedWidth = window.getComputedStyle(sidebar).width;
            const widthNum = parseInt(computedWidth.replace('px', '')) || 0;
            
            // Panel is visible if it has --visible class OR display is flex AND width > 0
            return hasVisible || (computedDisplay === 'flex' && widthNum > 10);
        }
        return false;
    }

    /**
     * Get data section (for DataVisualization injection)
     */
    getDataSection(): HTMLElement | null {
        return this.sections.get('data') || null;
    }

    /**
     * Get monitoring section (for LogsPanel injection)
     */
    getMonitoringSection(): HTMLElement | null {
        return this.sections.get('monitoring') || null;
    }

    /**
     * Setup resize handle for sidebar
     */
    private setupResizeHandle(): void {
        const resizeHandle = document.createElement('div');
        resizeHandle.className = 'sidebar-resize-handle';
        resizeHandle.style.cssText = `
            position: absolute;
            left: 0;
            top: 0;
            bottom: 0;
            width: 4px;
            cursor: ew-resize;
            z-index: 1000;
            background: transparent;
            transition: background 0.2s;
        `;

        resizeHandle.addEventListener('mouseenter', () => {
            resizeHandle.style.background = 'rgba(59, 130, 246, 0.5)';
        });

        resizeHandle.addEventListener('mouseleave', () => {
            if (!this.isResizing) {
                resizeHandle.style.background = 'transparent';
            }
        });

        resizeHandle.addEventListener('mousedown', (e) => {
            e.preventDefault();
            this.isResizing = true;
            this.resizeStartX = e.clientX;
            this.resizeStartWidth = this.container.offsetWidth;
            resizeHandle.style.background = 'rgba(59, 130, 246, 0.8)';

            const onMouseMove = (e: MouseEvent) => {
                if (!this.isResizing) return;
                const deltaX = this.resizeStartX - e.clientX; // Inverted because we're resizing from left
                const newWidth = Math.max(280, Math.min(window.innerWidth * 0.20, this.resizeStartWidth + deltaX));
                this.container.style.width = `${newWidth}px`;
                this.container.style.flex = `0 0 ${newWidth}px`;
                
                // Update parent sidebar-section if it exists
                const sidebarSection = this.container.closest('.sidebar-section') as HTMLElement;
                if (sidebarSection) {
                    sidebarSection.style.width = `${newWidth}px`;
                    sidebarSection.style.flex = `0 0 ${newWidth}px`;
                }
            };

            const onMouseUp = () => {
                this.isResizing = false;
                resizeHandle.style.background = 'transparent';
                document.removeEventListener('mousemove', onMouseMove);
                document.removeEventListener('mouseup', onMouseUp);
            };

            document.addEventListener('mousemove', onMouseMove);
            document.addEventListener('mouseup', onMouseUp);
        });

        this.container.style.position = 'relative';
        this.container.appendChild(resizeHandle);
    }
}

