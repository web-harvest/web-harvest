/**
 * VariablesPanel - Implementation (TDD Green Phase)
 * Manages and displays script variables with separate sections for user and system variables
 */

interface SystemVariableInfo {
    name: string;
    description: string;
    source: string;
}

export class VariablesPanel {
    private variables: Record<string, any> = {};
    private container: HTMLElement;
    private listElement: HTMLElement;
    
    // System variables that are automatically created by the framework
    private static readonly SYSTEM_VARIABLES: SystemVariableInfo[] = [
        {
            name: 'configResult',
            description: 'Result of the entire configuration execution. Always available after execution completes.',
            source: 'RootConfigurationExecutor'
        },
        {
            name: 'functionResult',
            description: 'Return value from a function when using <return> element. Only available when function is called with <return>.',
            source: 'ReturnPlugin'
        },
        {
            name: 'tryResult',
            description: 'Result of <try> block execution. Contains the successful execution result.',
            source: 'TryPlugin'
        },
        {
            name: 'listResult',
            description: 'Result of <list> element execution. Contains the list of items.',
            source: 'ListPlugin'
        },
        {
            name: 'sys',
            description: 'System utilities object providing helper functions (e.g., sys.fullUrl, sys.currentDate).',
            source: 'ScraperContext'
        },
        {
            name: 'error',
            description: 'Error message from <try> block when an exception occurs. Only available in catch block.',
            source: 'TryPlugin'
        },
        {
            name: 'errorType',
            description: 'Type of error from <try> block when an exception occurs. Only available in catch block.',
            source: 'TryPlugin'
        }
    ];
    
    // Pattern for regexp group variables (group0, group1, etc.)
    private static readonly REGEXP_GROUP_PATTERN = /^group\d+$/;

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
        const keyCount = Object.keys(variables).length;
        console.log('[VariablesPanel] setVariables() called with', keyCount, 'variables');
        this.variables = { ...variables };
        this.render();
        this.updateBadge();
        console.log('[VariablesPanel] Variables updated and rendered');
    }

    /**
     * Update variables badge counter
     */
    private updateBadge(): void {
        // Try to find badge in current container or document
        const badge = this.container.querySelector('#varsBadge') as HTMLElement || 
                      document.getElementById('varsBadge');
        if (badge) {
            const count = Object.keys(this.variables).length;
            badge.textContent = count.toString();
            console.log('[VariablesPanel] Updated badge:', count);
        } else {
            console.warn('[VariablesPanel] Badge element #varsBadge not found');
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
     * Check if variable is a system variable
     */
    private isSystemVariable(name: string): boolean {
        // Check explicit system variables
        if (VariablesPanel.SYSTEM_VARIABLES.some(sv => sv.name === name)) {
            return true;
        }
        
        // Check regexp group variables (group0, group1, etc.)
        if (VariablesPanel.REGEXP_GROUP_PATTERN.test(name)) {
            return true;
        }
        
        return false;
    }
    
    /**
     * Get system variable info
     */
    private getSystemVariableInfo(name: string): SystemVariableInfo | null {
        // Check explicit system variables
        const info = VariablesPanel.SYSTEM_VARIABLES.find(sv => sv.name === name);
        if (info) {
            return info;
        }
        
        // Handle regexp group variables
        if (VariablesPanel.REGEXP_GROUP_PATTERN.test(name)) {
            return {
                name,
                description: `Regexp capture group from <regexp> element. Contains the matched text for this group.`,
                source: 'RegexpPlugin'
            };
        }
        
        return null;
    }
    
    /**
     * Separate variables into user and system
     */
    private separateVariables(): { user: Record<string, any>; system: Record<string, any> } {
        const user: Record<string, any> = {};
        const system: Record<string, any> = {};
        
        for (const [key, value] of Object.entries(this.variables)) {
            if (this.isSystemVariable(key)) {
                system[key] = value;
            } else {
                user[key] = value;
            }
        }
        
        return { user, system };
    }
    
    /**
     * Render all variables
     */
    private render(): void {
        // ALWAYS clear first
        this.listElement.innerHTML = '';

        const keys = Object.keys(this.variables);
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

        // Separate user and system variables
        const { user, system } = this.separateVariables();
        const userKeys = Object.keys(user).sort();
        const systemKeys = Object.keys(system).sort();

        // Render User Variables section
        if (userKeys.length > 0) {
            const userSection = this.createSection('User Variables', userKeys.length, false);
            const userContent = (userSection as any).__content;
            userKeys.forEach(key => {
                const item = this.renderVariable(key, user[key], false);
                userContent.appendChild(item);
            });
            this.listElement.appendChild(userSection);
        }

        // Render System Variables section
        if (systemKeys.length > 0) {
            const systemSection = this.createSection('System Variables', systemKeys.length, true);
            const systemContent = (systemSection as any).__content;
            systemKeys.forEach(key => {
                const item = this.renderVariable(key, system[key], true);
                systemContent.appendChild(item);
            });
            this.listElement.appendChild(systemSection);
        }

        // If no variables in either section, show empty state
        if (userKeys.length === 0 && systemKeys.length === 0) {
            this.listElement.innerHTML = `
                <div class="empty-state">
                    <i class="fas fa-box"></i>
                    <p>No variables yet</p>
                    <small>Variables will appear during execution</small>
                </div>
            `;
        }

        console.log('[VariablesPanel] Rendered', userKeys.length, 'user and', systemKeys.length, 'system variables');
    }
    
    /**
     * Create section header
     */
    private createSection(title: string, count: number, isSystem: boolean): HTMLElement {
        const section = document.createElement('div');
        section.className = 'variables-section';
        section.style.marginBottom = '16px';
        
        const header = document.createElement('div');
        header.className = 'variables-section__header';
        header.style.display = 'flex';
        header.style.alignItems = 'center';
        header.style.justifyContent = 'space-between';
        header.style.marginBottom = '8px';
        header.style.paddingBottom = '8px';
        header.style.borderBottom = isSystem ? '1px solid #e0e0e0' : '1px solid #d0d0d0';
        
        const titleContainer = document.createElement('div');
        titleContainer.style.display = 'flex';
        titleContainer.style.alignItems = 'center';
        titleContainer.style.gap = '8px';
        
        const icon = document.createElement('i');
        icon.className = isSystem ? 'fas fa-cog' : 'fas fa-user';
        icon.style.fontSize = '14px';
        icon.style.color = isSystem ? '#666' : '#333';
        
        const titleEl = document.createElement('h3');
        titleEl.className = 'variables-section__title';
        titleEl.style.margin = '0';
        titleEl.style.fontSize = '13px';
        titleEl.style.fontWeight = '600';
        titleEl.style.color = '#333';
        titleEl.textContent = title;
        
        const badge = document.createElement('span');
        badge.className = 'variables-section__badge';
        badge.style.padding = '2px 8px';
        badge.style.borderRadius = '12px';
        badge.style.fontSize = '11px';
        badge.style.fontWeight = '600';
        badge.style.background = isSystem ? 'rgba(59, 130, 246, 0.1)' : 'rgba(34, 197, 94, 0.1)';
        badge.style.color = isSystem ? '#3b82f6' : '#22c55e';
        badge.textContent = count.toString();
        
        titleContainer.appendChild(icon);
        titleContainer.appendChild(titleEl);
        header.appendChild(titleContainer);
        header.appendChild(badge);
        
        const content = document.createElement('div');
        content.className = 'variables-section__content';
        content.style.display = 'flex';
        content.style.flexDirection = 'column';
        content.style.gap = '4px';
        
        section.appendChild(header);
        section.appendChild(content);
        
        // Store content reference for appending variables
        (section as any).__content = content;
        
        return section;
    }

    /**
     * Render single variable
     */
    private renderVariable(name: string, value: any, isSystem: boolean): HTMLElement {
        const item = document.createElement('div');
        item.className = isSystem ? 'variable-item variable-item--system' : 'variable-item variable-item--user';
        item.style.padding = '10px';
        item.style.marginBottom = '4px';
        item.style.background = isSystem ? '#f0f7ff' : '#f9f9f9';
        item.style.borderRadius = '6px';
        item.style.border = isSystem ? '1px solid #bfdbfe' : '1px solid #e5e5e5';
        item.style.transition = 'all 0.2s ease';

        const header = document.createElement('div');
        header.style.display = 'flex';
        header.style.justifyContent = 'space-between';
        header.style.alignItems = 'center';
        header.style.marginBottom = '6px';

        const nameContainer = document.createElement('div');
        nameContainer.style.display = 'flex';
        nameContainer.style.alignItems = 'center';
        nameContainer.style.gap = '6px';

        // System badge
        if (isSystem) {
            const systemBadge = document.createElement('span');
            systemBadge.className = 'variable-badge variable-badge--system';
            systemBadge.style.padding = '2px 6px';
            systemBadge.style.borderRadius = '4px';
            systemBadge.style.fontSize = '10px';
            systemBadge.style.fontWeight = '600';
            systemBadge.style.background = '#3b82f6';
            systemBadge.style.color = 'white';
            systemBadge.style.textTransform = 'uppercase';
            systemBadge.style.letterSpacing = '0.5px';
            systemBadge.textContent = 'System';
            nameContainer.appendChild(systemBadge);
        }

        // Name
        const nameEl = document.createElement('span');
        nameEl.className = 'variable-name';
        nameEl.style.fontWeight = '600';
        nameEl.style.fontSize = '13px';
        nameEl.style.color = isSystem ? '#1e40af' : '#333';
        nameEl.textContent = name;
        
        // Add tooltip for system variables
        if (isSystem) {
            const info = this.getSystemVariableInfo(name);
            if (info) {
                nameEl.title = `${info.description}\n\nSource: ${info.source}`;
                nameEl.style.cursor = 'help';
            }
        } else {
            nameEl.title = 'User-defined variable';
        }
        
        nameContainer.appendChild(nameEl);
        header.appendChild(nameContainer);

        // Type
        const typeEl = document.createElement('span');
        typeEl.className = 'variable-type';
        typeEl.style.fontSize = '11px';
        typeEl.style.color = isSystem ? '#60a5fa' : '#999';
        typeEl.style.fontWeight = '500';
        typeEl.textContent = this.getTypeName(value);
        header.appendChild(typeEl);

        item.appendChild(header);

        // Value
        const valueEl = document.createElement('div');
        valueEl.className = 'variable-value';
        valueEl.style.fontSize = '12px';
        valueEl.style.color = isSystem ? '#1e3a8a' : '#666';
        valueEl.style.wordBreak = 'break-all';
        valueEl.style.lineHeight = '1.5';
        valueEl.style.maxHeight = '200px';
        valueEl.style.overflowY = 'auto';
        valueEl.textContent = this.formatValue(value);
        item.appendChild(valueEl);

        // Hover effect
        item.addEventListener('mouseenter', () => {
            item.style.background = isSystem ? '#e0f2fe' : '#f5f5f5';
            item.style.borderColor = isSystem ? '#93c5fd' : '#d1d5db';
        });
        item.addEventListener('mouseleave', () => {
            item.style.background = isSystem ? '#f0f7ff' : '#f9f9f9';
            item.style.borderColor = isSystem ? '#bfdbfe' : '#e5e5e5';
        });

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

    /**
     * Render into a different container (for RightSidebar integration)
     */
    renderInto(container: HTMLElement): void {
        // Move list element to new container
        if (this.listElement.parentNode) {
            this.listElement.parentNode.removeChild(this.listElement);
        }
        container.appendChild(this.listElement);
        this.container = container;
    }
}

