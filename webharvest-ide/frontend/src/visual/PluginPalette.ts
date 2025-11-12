/**
 * Plugin Palette - Drag & Drop plugin templates to canvas
 * 
 * Features:
 * - Display all plugins grouped by category
 * - Search/filter plugins
 * - Drag template to canvas to create new node
 * - Keyboard navigation
 * - Category collapse/expand
 */

import type { VisualNode } from '../types';
import {
    getPluginTemplates,
    createNodeFromTemplate,
    type PluginTemplate
} from './VisualModelHelpers';
import {
    buildPaletteViewModel,
    type PaletteTemplate,
    type PaletteGroup,
    type PaletteFilterOptions
} from './PluginPaletteModel';
import {
    getDependentsHintVisibility,
    dismissDependentsHint
} from './PalettePreferences';

export class PluginPalette {
    private container: HTMLElement;
    private templates: PaletteTemplate[];
    private filteredTemplates: PaletteTemplate[];
    private primaryGroups: PaletteGroup[] = [];
    private searchTerm: string = '';
    private onAddNodeCallback: ((node: VisualNode) => void) | null = null;
    private selectedMasterPlugin: string | null = null; // Track selected master plugin for highlighting allowed children
    private filterByRole: string | null = null; // Filter by PluginSemanticRole
    private showContainersOnly: boolean = false; // Show only container plugins
    private activeSegment: 'all' | 'workflow' | 'utilities' = 'all';
    private showDependentsHint: boolean = true;

    constructor(containerId: string) {
        console.log('🔌🔌🔌 [PluginPalette] Constructor called for ID:', containerId);

        const container = document.getElementById(containerId);
        if (!container) {
            console.error(`❌❌❌ Container #${containerId} NOT FOUND in DOM!`);
            throw new Error(`Container with ID "${containerId}" not found`);
        }

        console.log('✅ [PluginPalette] Container found:', container);
        this.container = container;

        // Initialize with hardcoded templates first (will be replaced by API)
        this.templates = getPluginTemplates() as PaletteTemplate[];
        console.log('🔌 [PluginPalette] Hardcoded templates loaded:', this.templates.length);
        console.log('🔌 [PluginPalette] First template:', this.templates[0]);

        this.filteredTemplates = [...this.templates];
        console.log('🔌 [PluginPalette] Filtered templates:', this.filteredTemplates.length);

        // Render initial state
        console.log('🔌 [PluginPalette] About to call render() from constructor...');
        try {
            this.showDependentsHint = getDependentsHintVisibility();
            this.applyFilters();
            this.render();
            console.log('✅✅✅ [PluginPalette] render() completed successfully!');
        } catch (error) {
            console.error('❌❌❌ [PluginPalette] render() FAILED:', error);
            console.error('Stack trace:', error);
            throw error;
        }
        console.log('✅✅✅ [PluginPalette] Constructor COMPLETE!');
    }

    private createSegmentedControl(): HTMLElement {
        const segmented = document.createElement('div');
        segmented.className = 'plugin-palette__segments';

        const segments: Array<{ id: 'all' | 'workflow' | 'utilities'; label: string; icon: string }> = [
            { id: 'all', label: 'All', icon: 'fa-layer-group' },
            { id: 'workflow', label: 'Workflow', icon: 'fa-diagram-project' },
            { id: 'utilities', label: 'Utilities', icon: 'fa-wrench' }
        ];

        segments.forEach(segment => {
            const button = document.createElement('button');
            button.type = 'button';
            button.className = 'plugin-palette__segment';
            button.dataset.segmentId = segment.id;
            button.innerHTML = `<i class="fas ${segment.icon}"></i> ${segment.label}`;
            if (this.activeSegment === segment.id) {
                button.classList.add('is-active');
            }
            button.onclick = () => this.setActiveSegment(segment.id);
            segmented.appendChild(button);
        });

        return segmented;
    }

    /**
     * Initialize plugin palette by fetching plugins from API
     */
    async init(): Promise<void> {
        console.log('🔌 [PluginPalette] init() started - fetching from API...');

        try {
            console.log('🔌 [PluginPalette] Fetching /api/plugins/metadata...');
            const response = await fetch('/api/plugins/metadata');

            console.log('🔌 [PluginPalette] Response status:', response.status);

            if (!response.ok) {
                throw new Error(`HTTP ${response.status}: ${response.statusText}`);
            }

            const data = await response.json();
            console.log('🔌 [PluginPalette] Received data:', data.count, 'plugins');

            // Map API response to PluginTemplate[]
            // Use VisualPluginMetadata fields: group (instead of category) and color
            this.templates = data.plugins.map((p: any) => ({
                type: p.id,
                category: p.group || p.paletteGroup || p.ui?.paletteGroup || p.category || 'General', // Use VisualPluginMetadata.group
                icon: p.icon || p.ui?.icon || 'fa-puzzle-piece',
                label: p.name,
                defaultAttributes: this.extractDefaultAttributes(p.schema),
                description: p.description || '',
                canAppearAtRoot: p.semantics?.canAppearAtRoot !== false && p.schema?.canAppearAtRoot !== false,
                // VisualPluginMetadata fields
                color: p.color, // Hex color for category
                defaultWidth: p.defaultWidth,
                defaultHeight: p.defaultHeight,
                inputPorts: p.inputPorts,
                outputPorts: p.outputPorts,
                portType: p.portType,
                tooltip: p.tooltip || p.description,
                // Metadata for filtering and search
                tags: p.tags || p.ui?.tags || [],
                semantics: p.semantics,
                requiredParentTypes: p.semantics?.requiredParentTypes || p.schema?.requiredParents || [],
                allowedChildTypes: p.semantics?.allowedChildTypes || p.schema?.allowedSubElements || []
            }));

            this.filteredTemplates = [...this.templates];
            this.applyFilters();
            this.render();

            console.log(`✅ [PluginPalette] Loaded ${this.templates.length} plugins from API!`);
        } catch (error) {
            console.error('❌ [PluginPalette] Failed to load plugins from API:', error);
            console.log('⚠️ [PluginPalette] Using hardcoded plugin templates as fallback');

            // Ensure we still have templates (hardcoded fallback already loaded in constructor)
            console.log('🔌 [PluginPalette] Fallback templates count:', this.templates.length);
        }
    }

    /**
     * Extract default attributes from plugin schema
     */
    private extractDefaultAttributes(schema: any): Record<string, string> {
        if (!schema || !schema.attributes || !Array.isArray(schema.attributes)) {
            return {};
        }

        const defaults: Record<string, string> = {};

        schema.attributes.forEach((attr: any) => {
            if (attr.defaultValue) {
                defaults[attr.name] = attr.defaultValue;
            }
        });

        return defaults;
    }

    /**
     * Get all templates
     */
    getTemplates(): PluginTemplate[] {
        return this.templates;
    }

    /**
     * Get filtered templates
     */
    getFilteredTemplates(): PluginTemplate[] {
        return this.filteredTemplates;
    }

    /**
     * Get all categories from CURRENT templates (not hardcoded!)
     */
    getCategories(): string[] {
        const categories = this.filteredTemplates.map(template => template.category || 'General');
        return Array.from(new Set(categories));
    }

    /**
     * Set selected master plugin to highlight allowed children
     */
    setSelectedMasterPlugin(pluginType: string | null): void {
        this.selectedMasterPlugin = pluginType;
        this.render(); // Re-render to update highlighting
    }

    /**
     * Get allowed children for a master plugin
     */
    private getMasterPluginAllowedChildren(pluginType: string): string[] {
        const template = this.templates.find(t => t.type === pluginType);
        if (!template) return [];
        return (template as any).allowedChildTypes || [];
    }

    private filterGroupsBySegment(groups: PaletteGroup[]): PaletteGroup[] {
        const segment = this.activeSegment;
        const isWorkflowGroup = (groupName: string) => groupName === 'Workflow Blocks';

        return groups
            .filter(group => {
                if (segment === 'all') {
                    return true;
                }
                if (segment === 'workflow') {
                    return isWorkflowGroup(group.name);
                }
                return !isWorkflowGroup(group.name);
            })
            .map(group => ({
                name: group.name,
                templates: [...group.templates]
            }))
            .filter(group => group.templates.length > 0);
    }

    /**
     * Search plugins by term
     */
    search(term: string): void {
        this.searchTerm = term.toLowerCase();
        this.applyFilters();
        this.render();
    }

    setActiveSegment(segment: 'all' | 'workflow' | 'utilities'): void {
        this.activeSegment = segment;
        this.applyFilters();
        this.render();
    }

    /**
     * Clear all filters
     */
    clearFilters(): void {
        this.searchTerm = '';
        this.filterByRole = null;
        this.showContainersOnly = false;
        this.activeSegment = 'all';
        this.applyFilters();
        this.render();
    }

    /**
     * Apply current filters
     */
    private applyFilters(): void {
        const commonOptions: PaletteFilterOptions = {
            searchTerm: this.searchTerm,
            containersOnly: this.showContainersOnly,
            roleFilter: this.filterByRole
        };

        const viewModel = buildPaletteViewModel(this.templates, commonOptions);
        this.primaryGroups = this.filterGroupsBySegment(viewModel.primaryGroups);
        this.filteredTemplates = this.primaryGroups.flatMap(group => group.templates);
    }

    /**
     * Filter by semantic role
     */
    setRoleFilter(role: string | null): void {
        this.filterByRole = role;
        this.applyFilters();
        this.render();
    }

    private createDependentsInfoBanner(): HTMLElement | null {
        if (!this.showDependentsHint) {
            return null;
        }

        const banner = document.createElement('section');
        banner.className = 'plugin-palette__dependents-info';

        const icon = document.createElement('div');
        icon.className = 'plugin-palette__dependents-icon';
        icon.innerHTML = '<i class="fas fa-info-circle"></i>';

        const copy = document.createElement('div');
        copy.className = 'plugin-palette__dependents-copy';

        const heading = document.createElement('strong');
        heading.textContent = 'Looking for headers, params, or branches?';

        const body = document.createElement('p');
        body.textContent = 'Add detail components from inside their parent cards. Select a plugin and use the Properties panel to add nested items.';

        const closeButton = document.createElement('button');
        closeButton.type = 'button';
        closeButton.className = 'plugin-palette__dependents-dismiss';
        closeButton.setAttribute('aria-label', 'Hide dependent component help');
        closeButton.innerHTML = '<i class="fas fa-xmark"></i>';
        closeButton.onclick = () => {
            dismissDependentsHint();
            this.showDependentsHint = false;
            this.render();
        };

        copy.appendChild(heading);
        copy.appendChild(body);

        banner.appendChild(icon);
        banner.appendChild(copy);
        banner.appendChild(closeButton);
        return banner;
    }

    /**
     * Toggle show containers only
     */
    toggleContainersOnly(): void {
        this.showContainersOnly = !this.showContainersOnly;
        this.applyFilters();
        this.render();
    }

    render(): void {
        try {
            this.container.innerHTML = '';
            this.container.className = 'plugin-palette';
            this.container.style.display = 'flex';
            this.container.style.flexDirection = 'column';
            this.container.style.height = '100%';
            this.container.style.background = '#f8f9fb';

            const header = document.createElement('div');
            header.className = 'plugin-palette__header';
            header.appendChild(this.createSearchBox());
            header.appendChild(this.createSegmentedControl());
            this.container.appendChild(header);

            this.container.appendChild(this.createFilterControls());

            const listContainer = document.createElement('div');
            listContainer.className = 'plugin-palette__list';
            listContainer.style.flex = '1';
            listContainer.style.overflowY = 'auto';
            listContainer.style.padding = '12px';

            if (this.primaryGroups.length === 0) {
                listContainer.appendChild(this.createEmptyState());
            } else {
                this.primaryGroups.forEach(group => {
                    listContainer.appendChild(this.renderGroupSection(group, 'primary'));
                });
            }

            this.container.appendChild(listContainer);

            const dependentsBanner = this.createDependentsInfoBanner();
            if (dependentsBanner) {
                this.container.appendChild(dependentsBanner);
            }
        } catch (error) {
            console.error('❌❌❌ [PluginPalette] RENDER CRASHED:', error);
            console.error('Error details:', error);
            throw error;
        }
    }

    /**
     * Create search box
     */
    private createSearchBox(): HTMLElement {
        const searchContainer = document.createElement('div');
        searchContainer.className = 'plugin-palette__search';

        const icon = document.createElement('span');
        icon.className = 'plugin-palette__search-icon';
        icon.innerHTML = '<i class="fas fa-search"></i>';

        const input = document.createElement('input');
        input.type = 'search';
        input.placeholder = 'Search components';
        input.value = this.searchTerm;
        input.autocomplete = 'off';
        input.className = 'plugin-palette__search-input';

        input.oninput = () => {
            this.search(input.value);
        };

        const clearButton = document.createElement('button');
        clearButton.type = 'button';
        clearButton.className = 'plugin-palette__search-clear';
        clearButton.innerHTML = '<i class="fas fa-times-circle"></i>';
        clearButton.title = 'Clear search';
        clearButton.onclick = () => {
            if (this.searchTerm.length === 0) {
                return;
            }
            this.clearFilters();
        };

        searchContainer.appendChild(icon);
        searchContainer.appendChild(input);
        searchContainer.appendChild(clearButton);
        return searchContainer;
    }

    /**
     * Create filter controls for semantic roles
     */
    private createFilterControls(): HTMLElement {
        const wrapper = document.createElement('div');
        wrapper.className = 'plugin-palette__filters';

        const rolesRow = document.createElement('div');
        rolesRow.className = 'plugin-palette__role-row';

        const roles = [
            { value: null, label: 'All', icon: 'fa-layer-group' },
            { value: 'ROOT_CONTAINER', label: 'Root', icon: 'fa-house' },
            { value: 'CONTEXT_CONTAINER', label: 'Containers', icon: 'fa-folder-open' },
            { value: 'BODY_EVALUATOR', label: 'Evaluators', icon: 'fa-code' },
            { value: 'LEAF', label: 'Leaf', icon: 'fa-puzzle-piece' }
        ];

        roles.forEach(role => {
            const chip = document.createElement('button');
            chip.type = 'button';
            chip.className = 'plugin-palette__role-chip';
            chip.innerHTML = `<i class="fas ${role.icon}"></i> ${role.label}`;
            if (this.filterByRole === role.value) {
                chip.classList.add('is-active');
            }
            chip.onclick = () => this.setRoleFilter(role.value);
            rolesRow.appendChild(chip);
        });

        const togglesRow = document.createElement('div');
        togglesRow.className = 'plugin-palette__toggle-row';

        const containersButton = document.createElement('button');
        containersButton.type = 'button';
        containersButton.className = 'plugin-palette__toggle';
        containersButton.innerHTML = '<i class="fas fa-boxes-stacked"></i> Containers only';
        if (this.showContainersOnly) {
            containersButton.classList.add('is-active');
        }
        containersButton.onclick = () => this.toggleContainersOnly();
        togglesRow.appendChild(containersButton);

        wrapper.appendChild(rolesRow);
        wrapper.appendChild(togglesRow);
        return wrapper;
    }

    private createEmptyState(): HTMLElement {
        const empty = document.createElement('div');
        empty.className = 'plugin-palette__empty';
        empty.innerHTML = `
            <i class="fas fa-stars"></i>
            <p>No components match the current filters.</p>
            <button type="button" class="plugin-palette__empty-reset">
                <i class="fas fa-undo"></i> Reset filters
            </button>
        `;

        const resetButton = empty.querySelector('.plugin-palette__empty-reset') as HTMLButtonElement;
        resetButton.onclick = () => this.clearFilters();

        return empty;
    }

    private renderGroupSection(group: PaletteGroup, tone: 'primary' | 'advanced'): HTMLElement {
        const section = document.createElement('section');
        section.className = `plugin-group plugin-group--${tone}`;

        const header = document.createElement('header');
        header.className = 'plugin-group__header';
        header.innerHTML = `
            <div class="plugin-group__title">${group.name}</div>
            <span class="plugin-group__count">${group.templates.length}</span>
        `;
        section.appendChild(header);

        const grid = document.createElement('div');
        grid.className = 'plugin-group__grid';
        group.templates.forEach(template => {
            grid.appendChild(this.createPluginItem(template));
        });
        section.appendChild(grid);

        return section;
    }

    /**
     * Create plugin item (draggable)
     */
    private createPluginItem(template: PluginTemplate): HTMLElement {
        const item = document.createElement('div');
        item.className = 'plugin-item';
        item.classList.add('palette-plugin');
        item.draggable = true;
        item.tabIndex = 0;
        item.dataset.pluginType = template.type;
        item.setAttribute('role', 'button');
        item.setAttribute('aria-label', template.label || template.type);
        if (template.color) {
            item.style.setProperty('--plugin-accent', template.color);
        }

        const header = document.createElement('div');
        header.className = 'plugin-item__header';

        const icon = document.createElement('span');
        icon.className = 'plugin-item__icon';
        icon.textContent = this.getIconForType(template.type);
        if (template.color) {
            icon.style.color = template.color;
        }

        const titleBlock = document.createElement('div');
        titleBlock.className = 'plugin-item__title-block';

        const title = document.createElement('span');
        title.className = 'plugin-item__title';
        title.textContent = template.label;
        titleBlock.appendChild(title);

        const subtitleText = this.getPluginSubtitle(template);
        if (subtitleText) {
            const subtitle = document.createElement('span');
            subtitle.className = 'plugin-item__subtitle';
            subtitle.textContent = subtitleText;
            titleBlock.appendChild(subtitle);
        }

        const infoButton = document.createElement('button');
        infoButton.type = 'button';
        infoButton.className = 'plugin-item__info';
        infoButton.innerHTML = '<i class="fas fa-circle-info"></i>';
        infoButton.setAttribute('aria-label', `Learn about ${template.label}`);
        const infoText = this.getInfoText(template);
        if (infoText) {
            infoButton.title = infoText;
            infoButton.setAttribute('aria-description', infoText);
        }
        infoButton.onmousedown = event => event.stopPropagation();
        infoButton.onclick = event => event.stopPropagation();

        header.appendChild(icon);
        header.appendChild(titleBlock);
        header.appendChild(infoButton);

        const body = document.createElement('p');
        body.className = 'plugin-item__description';
        const descriptionText = this.getDescriptionText(template);
        body.textContent = descriptionText;
        if (descriptionText) {
            item.setAttribute('aria-description', descriptionText);
        } else if (infoText) {
            item.setAttribute('aria-description', infoText);
        }

        const footer = document.createElement('div');
        footer.className = 'plugin-item__footer';

        const roles = this.getTemplateRoles(template);
        if (roles.length > 0) {
            const badge = document.createElement('span');
            badge.className = 'plugin-item__badge';
            badge.textContent = this.getRoleLabel(roles[0]);
            badge.title = `Semantic role: ${roles.join(', ')}`;
            footer.appendChild(badge);
        }

        if (this.templateAllowsChildren(template)) {
            item.classList.add('plugin-item--container');

            const childHint = document.createElement('span');
            childHint.className = 'plugin-item__hint';
            childHint.innerHTML = '<i class="fas fa-layer-group"></i> Supports nested steps';
            footer.appendChild(childHint);
        }

        const allowedChildren = this.getTemplateAllowedChildren(template);
        if (allowedChildren.length > 0) {
            const compatibility = document.createElement('span');
            compatibility.className = 'plugin-item__hint';
            compatibility.textContent = `Allows ${allowedChildren.length} child type${allowedChildren.length > 1 ? 's' : ''}`;
            footer.appendChild(compatibility);
        }

        item.appendChild(header);
        if (descriptionText) {
            item.appendChild(body);
        }
        if (footer.childElementCount > 0) {
            item.appendChild(footer);
        }

        if (this.selectedMasterPlugin) {
            const masterAllowedChildren = this.getMasterPluginAllowedChildren(this.selectedMasterPlugin);
            if (masterAllowedChildren.includes(template.type)) {
                item.classList.add('plugin-item--highlight');
                item.setAttribute('aria-current', 'true');
            }
        }

        item.onmouseenter = () => item.classList.add('is-hovered');
        item.onmouseleave = () => item.classList.remove('is-hovered');

        item.ondragstart = (event) => {
            this.onDragStart(event, template);
            item.classList.add('is-dragging');
        };

        item.ondragend = () => {
            item.classList.remove('is-dragging');
        };

        item.onkeydown = (event) => {
            if (event.key === 'Enter' && this.onAddNodeCallback) {
                const newNode = createNodeFromTemplate(template);
                this.onAddNodeCallback(newNode);
            } else if (event.key === 'ArrowDown') {
                event.preventDefault();
                const next = item.nextElementSibling as HTMLElement | null;
                next?.focus();
            } else if (event.key === 'ArrowUp') {
                event.preventDefault();
                const previous = item.previousElementSibling as HTMLElement | null;
                previous?.focus();
            }
        };

        return item;
    }

    /**
     * Get human-readable label for semantic role
     */
    private getTemplateRoles(template: PaletteTemplate): string[] {
        const roles = (template as any).semantics?.roles;
        if (Array.isArray(roles) && roles.length > 0) {
            return roles;
        }
        const legacyRole = (template as any).semantics?.role;
        return legacyRole ? [legacyRole] : [];
    }

    private templateAllowsChildren(template: PaletteTemplate): boolean {
        const semantics = (template as any).semantics;
        if (semantics?.allowsAnyChild || semantics?.acceptsAnyChild) {
            return true;
        }
        const allowed = this.getTemplateAllowedChildren(template);
        if (allowed.length > 0) {
            return true;
        }
        return this.getTemplateRoles(template).includes('CONTEXT_CONTAINER');
    }

    private getTemplateAllowedChildren(template: PaletteTemplate): string[] {
        if (template.allowedChildTypes && template.allowedChildTypes.length > 0) {
            return template.allowedChildTypes;
        }
        const semantics = (template as any).semantics;
        if (semantics?.allowedChildTypes && semantics.allowedChildTypes.length > 0) {
            return semantics.allowedChildTypes;
        }
        return [];
    }

    private getPluginSubtitle(template: PaletteTemplate): string | null {
        const roles = this.getTemplateRoles(template);
        if (roles.includes('ROOT_CONTAINER')) {
            return 'Workflow entry';
        }
        if (roles.includes('CONTEXT_CONTAINER')) {
            return 'Container';
        }
        if (roles.includes('BODY_EVALUATOR')) {
            return 'Evaluator';
        }
        if (roles.includes('LEAF')) {
            return 'Action';
        }
        if (roles.includes('DEPENDENT')) {
            return 'Detail';
        }
        return null;
    }

    private getInfoText(template: PaletteTemplate): string {
        const parts: string[] = [];
        if (template.description) {
            parts.push(template.description.trim());
        }
        const requiredParents = (template as any).requiredParentTypes ?? [];
        if (requiredParents.length > 0) {
            parts.push(`Requires parent: ${requiredParents.join(', ')}`);
        }
        const allowedChildren = this.getTemplateAllowedChildren(template);
        if (allowedChildren.length > 0) {
            parts.push(`Available inside: ${allowedChildren.join(', ')}`);
        } else if (this.templateAllowsChildren(template)) {
            parts.push('Accepts nested steps via Properties panel');
        }
        return parts.join('. ');
    }

    private getDescriptionText(template: PaletteTemplate): string {
        if (!template.description) {
            return '';
        }
        const trimmed = template.description.trim();
        if (trimmed.length <= 120) {
            return trimmed;
        }
        return `${trimmed.slice(0, 117)}…`;
    }

    private getRoleLabel(role: string): string {
        const labels: Record<string, string> = {
            'ROOT_CONTAINER': 'Root flow',
            'CONTEXT_CONTAINER': 'Container',
            'BODY_EVALUATOR': 'Evaluator',
            'DEPENDENT': 'Detail',
            'LEAF': 'Action'
        };
        return labels[role] || role;
    }

    /**
     * Get emoji icon for plugin type
     */
    private getIconForType(type: string): string {
        const iconMap: Record<string, string> = {
            'http': '🌐',
            'ftp': '📁',
            'mail': '📧',
            'xpath': '🔍',
            'xquery': '🔎',
            'regexp': '📝',
            'html-to-xml': '📄',
            'json-to-xml': '📊',
            'xml-to-json': '📊',
            'def': '💾',
            'get-var': '📥',
            'loop': '🔄',
            'while': '⭮',
            'if': '❓',
            'try': '🛡️',
            'case': '🔀',
            'script': '💻',
            'template': '📋',
            'xslt': '🔄',
            'file': '📂',
            'log': '📝',
            'sleep': '⏱️',
            'return': '↩️',
            'exit': '🚪'
        };

        return iconMap[type] || '📦';
    }

    /**
     * Handle drag start
     */
    onDragStart(event: DragEvent, template: PluginTemplate): void {
        if (event.dataTransfer) {
            event.dataTransfer.setData('application/json', JSON.stringify(template));
            event.dataTransfer.effectAllowed = 'copy';
        }
    }

    /**
     * Create node from drag event
     */
    createNodeFromDragEvent(event: DragEvent): VisualNode | null {
        if (!event.dataTransfer) {
            return null;
        }

        try {
            const templateJSON = event.dataTransfer.getData('application/json');
            const template = JSON.parse(templateJSON) as PluginTemplate;
            return createNodeFromTemplate(template);
        } catch (error) {
            console.error('Failed to parse drag data:', error);
            return null;
        }
    }

    /**
     * Create node from template directly
     */
    createNodeFromTemplate(template: PluginTemplate): VisualNode {
        return createNodeFromTemplate(template);
    }

    /**
     * Set callback for adding node
     */
    onAddNode(callback: (node: VisualNode) => void): void {
        this.onAddNodeCallback = callback;
    }
}

