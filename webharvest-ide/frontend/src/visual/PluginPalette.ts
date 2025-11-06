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
    getPluginCategories,
    getPluginTemplatesByCategory,
    createNodeFromTemplate,
    type PluginTemplate
} from './VisualModelHelpers';

export class PluginPalette {
    private container: HTMLElement;
    private templates: PluginTemplate[];
    private filteredTemplates: PluginTemplate[];
    private searchTerm: string = '';
    private selectedCategory: string | null = null;
    private collapsedCategories: Set<string> = new Set();
    private onAddNodeCallback: ((node: VisualNode) => void) | null = null;
    private selectedMasterPlugin: string | null = null; // Track selected master plugin for highlighting allowed children

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
        this.templates = getPluginTemplates();
        console.log('🔌 [PluginPalette] Hardcoded templates loaded:', this.templates.length);
        console.log('🔌 [PluginPalette] First template:', this.templates[0]);

        this.filteredTemplates = [...this.templates];
        console.log('🔌 [PluginPalette] Filtered templates:', this.filteredTemplates.length);

        // Render initial state
        console.log('🔌 [PluginPalette] About to call render() from constructor...');
        try {
            this.render();
            console.log('✅✅✅ [PluginPalette] render() completed successfully!');
        } catch (error) {
            console.error('❌❌❌ [PluginPalette] render() FAILED:', error);
            console.error('Stack trace:', error);
            throw error;
        }
        console.log('✅✅✅ [PluginPalette] Constructor COMPLETE!');
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

            console.log('🔌 [PluginPalette] Mapped to', this.templates.length, 'templates');
            console.log('🔌 [PluginPalette] Categories:', this.getCategories());

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
        const categories = new Set<string>();
        this.templates.forEach(t => categories.add(t.category));
        const result = Array.from(categories).sort();
        console.log('📂 [PluginPalette] getCategories() from current templates:', result);
        return result;
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

    /**
     * Search plugins by term
     */
    search(term: string): void {
        this.searchTerm = term.toLowerCase();
        this.applyFilters();
        this.render();
    }

    /**
     * Filter by category
     */
    filterByCategory(category: string): void {
        this.selectedCategory = category;
        this.applyFilters();
        this.render();
    }

    /**
     * Clear all filters
     */
    clearFilters(): void {
        this.searchTerm = '';
        this.selectedCategory = null;
        this.applyFilters();
        this.render();
    }

    /**
     * Apply current filters
     */
    private applyFilters(): void {
        this.filteredTemplates = this.templates.filter(template => {
            // Search term filter (name, description, type, and tags)
            if (this.searchTerm) {
                const matchesBasic =
                    template.type.toLowerCase().includes(this.searchTerm) ||
                    template.label.toLowerCase().includes(this.searchTerm) ||
                    template.description.toLowerCase().includes(this.searchTerm);
                
                // Search in tags
                const matchesTags = (template as any).tags && 
                    (template as any).tags.some((tag: string) => 
                        tag.toLowerCase().includes(this.searchTerm)
                    );

                if (!matchesBasic && !matchesTags) {
                    return false;
                }
            }

            // Category filter
            if (this.selectedCategory && template.category !== this.selectedCategory) {
                return false;
            }

            // Filter out dependent plugins from root-level palette
            // Only show plugins that can appear at root level
            if (!template.canAppearAtRoot) {
                return false;
            }

            return true;
        });
    }

    /**
     * Render palette
     */
    render(): void {
        try {
            console.log('🎨🎨🎨 [PluginPalette] render() ENTRY POINT');
            console.log('🎨 [PluginPalette] this.container:', this.container);
            console.log('🎨 [PluginPalette] Templates count:', this.filteredTemplates.length);

            console.log('🎨 [PluginPalette] Clearing container...');
            this.container.innerHTML = '';

            console.log('🎨 [PluginPalette] Setting container styles...');
            this.container.className = 'plugin-palette';
            this.container.style.display = 'flex';
            this.container.style.flexDirection = 'column';
            this.container.style.height = '100%';
            this.container.style.overflow = 'hidden';

            // Check if we have templates
            if (this.filteredTemplates.length === 0) {
                console.warn('⚠️ [PluginPalette] No templates! Showing empty state.');
                this.container.innerHTML = `
                    <div style="padding: 48px 16px; text-align: center; color: #999;">
                        <i class="fas fa-exclamation-triangle" style="font-size: 24px;"></i>
                        <p style="margin: 12px 0 0; font-size: 13px;">No plugins loaded</p>
                    </div>
                `;
                return;
            }

            console.log('🎨 [PluginPalette] Creating search box...');
            const searchBox = this.createSearchBox();
            searchBox.style.flexShrink = '0';
            this.container.appendChild(searchBox);
            console.log('✅ [PluginPalette] Search box added');

            console.log('🎨 [PluginPalette] Creating categories container...');
            const categoriesContainer = document.createElement('div');
            categoriesContainer.className = 'categories-container';
            categoriesContainer.style.flex = '1';
            categoriesContainer.style.overflowY = 'auto';
            categoriesContainer.style.overflowX = 'hidden';

            // Group by category (use CURRENT templates, not hardcoded!)
            console.log('🎨 [PluginPalette] Getting categories from current templates...');
            const categories = this.getCategories();
            console.log('🎨 [PluginPalette] Categories to render:', categories);

            let renderedCount = 0;
            categories.forEach(category => {
                console.log(`🔍 [PluginPalette] Processing category "${category}"...`);

                const categoryTemplates = this.filteredTemplates.filter(t => t.category === category);
                console.log(`🔍 [PluginPalette] Filtering "${category}": found ${categoryTemplates.length} templates`);

                if (categoryTemplates.length === 0) {
                    console.log(`⚠️ [PluginPalette] Skipping empty category "${category}"`);
                    return;
                }

                console.log(`🎨 [PluginPalette] Creating section for "${category}" with ${categoryTemplates.length} plugins`);

                try {
                    const categorySection = this.createCategorySection(category, categoryTemplates);
                    categoriesContainer.appendChild(categorySection);
                    renderedCount++;
                    console.log(`✅ [PluginPalette] Added category "${category}" to DOM`);
                } catch (err) {
                    console.error(`❌ [PluginPalette] Failed to create section for "${category}":`, err);
                }
            });

            console.log('🎨 [PluginPalette] Appending categories container to main container...');
            this.container.appendChild(categoriesContainer);

            console.log(`✅✅✅ [PluginPalette] Render COMPLETE! ${renderedCount} categories, ${this.filteredTemplates.length} total plugins`);
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
        searchContainer.style.padding = '12px';
        searchContainer.style.borderBottom = '1px solid #ddd';

        const input = document.createElement('input');
        input.type = 'text';
        input.placeholder = 'Search plugins...';
        input.value = this.searchTerm;
        input.style.width = '100%';
        input.style.padding = '8px';
        input.style.border = '1px solid #ddd';
        input.style.borderRadius = '4px';
        input.style.fontSize = '14px';

        input.oninput = () => {
            this.search(input.value);
        };

        searchContainer.appendChild(input);
        return searchContainer;
    }

    /**
     * Create category section
     */
    private createCategorySection(category: string, templates: PluginTemplate[]): HTMLElement {
        const section = document.createElement('div');
        section.className = 'plugin-category-section';
        section.style.borderBottom = '1px solid #eee';

        // Category header
        const header = document.createElement('div');
        header.className = 'category-header';
        header.dataset.category = category;
        header.style.padding = '12px';
        header.style.fontWeight = '600';
        header.style.cursor = 'pointer';
        header.style.background = '#f9f9f9';
        header.style.display = 'flex';
        header.style.justifyContent = 'space-between';
        header.style.alignItems = 'center';

        const title = document.createElement('span');
        title.textContent = category.charAt(0).toUpperCase() + category.slice(1);
        header.appendChild(title);

        const count = document.createElement('span');
        count.style.fontSize = '12px';
        count.style.color = '#666';
        count.textContent = templates.length.toString();
        header.appendChild(count);

        // Toggle collapse on click
        header.onclick = () => {
            this.toggleCategoryCollapse(category);
        };

        section.appendChild(header);

        // Plugin items
        if (!this.collapsedCategories.has(category)) {
            const itemsContainer = document.createElement('div');
            itemsContainer.dataset.pluginCategory = category;

            templates.forEach(template => {
                const item = this.createPluginItem(template);
                itemsContainer.appendChild(item);
            });

            section.appendChild(itemsContainer);
        }

        return section;
    }

    /**
     * Toggle category collapse
     */
    private toggleCategoryCollapse(category: string): void {
        if (this.collapsedCategories.has(category)) {
            this.collapsedCategories.delete(category);
        } else {
            this.collapsedCategories.add(category);
        }
        this.render();
    }

    /**
     * Create plugin item (draggable)
     */
    private createPluginItem(template: PluginTemplate): HTMLElement {
        const item = document.createElement('div');
        item.className = 'plugin-item';
        item.draggable = true;
        item.tabIndex = 0;
        item.dataset.pluginType = template.type;
        item.style.padding = '10px 12px';
        item.style.margin = '4px 8px';
        item.style.background = 'white';
        // Use VisualPluginMetadata color if available
        const borderColor = template.color || '#ddd';
        item.style.border = `1px solid ${borderColor}`;
        item.style.borderLeft = `3px solid ${borderColor}`; // Left border accent with color
        item.style.borderRadius = '4px';
        item.style.cursor = 'grab';
        item.style.transition = 'all 0.2s';

        // Apply accessibility metadata (Apple HIG compliance)
        // Note: We need to get accessibility metadata from plugin manager
        // For now, use basic accessibility attributes
        item.setAttribute('role', 'button');
        item.setAttribute('aria-label', template.label || template.type);
        if (template.tooltip) {
            item.setAttribute('aria-description', template.tooltip);
            item.title = template.tooltip; // Fallback for browsers without aria-description support
        } else if (template.description) {
            item.setAttribute('aria-description', template.description);
            item.title = template.description;
        }

        // Icon + Label
        const content = document.createElement('div');
        content.style.display = 'flex';
        content.style.alignItems = 'center';
        content.style.gap = '8px';

        const icon = document.createElement('span');
        icon.textContent = this.getIconForType(template.type);
        icon.style.fontSize = '16px';
        content.appendChild(icon);

        const label = document.createElement('span');
        label.textContent = template.label;
        label.style.fontSize = '13px';
        label.style.fontWeight = '500';
        content.appendChild(label);

        // Add tag badges if available
        const tags = (template as any).tags || [];
        if (tags.length > 0) {
            const tagsContainer = document.createElement('div');
            tagsContainer.style.display = 'flex';
            tagsContainer.style.flexWrap = 'wrap';
            tagsContainer.style.gap = '4px';
            tagsContainer.style.marginTop = '6px';
            tagsContainer.style.fontSize = '10px';
            
            tags.slice(0, 3).forEach((tag: string) => {
                const tagBadge = document.createElement('span');
                tagBadge.textContent = tag;
                tagBadge.style.padding = '2px 6px';
                tagBadge.style.background = 'var(--gray-100)';
                tagBadge.style.borderRadius = '3px';
                tagBadge.style.color = 'var(--gray-700)';
                tagBadge.style.fontWeight = '500';
                tagsContainer.appendChild(tagBadge);
            });
            
            if (tags.length > 3) {
                const moreBadge = document.createElement('span');
                moreBadge.textContent = `+${tags.length - 3}`;
                moreBadge.style.padding = '2px 6px';
                moreBadge.style.background = 'var(--gray-200)';
                moreBadge.style.borderRadius = '3px';
                moreBadge.style.color = 'var(--gray-600)';
                tagsContainer.appendChild(moreBadge);
            }
            
            content.appendChild(tagsContainer);
        }

        item.appendChild(content);

        // Show parent requirement tooltip for dependent plugins
        const requiredParents = (template as any).requiredParentTypes || [];
        if (requiredParents.length > 0) {
            const parentTooltip = `Requires parent: ${requiredParents.join(', ')}`;
            if (!item.title) {
                item.title = parentTooltip;
            } else {
                item.title = `${item.title}. ${parentTooltip}`;
            }
            item.setAttribute('aria-description', parentTooltip);
            
            // Add visual indicator
            const indicator = document.createElement('span');
            indicator.textContent = '🔗';
            indicator.style.marginLeft = '4px';
            indicator.style.fontSize = '10px';
            indicator.title = parentTooltip;
            content.appendChild(indicator);
        }

        // Mark dependent plugins as disabled (if filtering is enabled)
        if (template.canAppearAtRoot === false) {
            item.classList.add('dependent-plugin');
            item.style.opacity = '0.6';
            item.style.cursor = 'not-allowed';
            item.setAttribute('aria-disabled', 'true');
            if (!item.title) {
                item.title = `This plugin requires a parent element`;
            }
        }

        // Highlight allowed children when master plugin is selected
        if (this.selectedMasterPlugin) {
            const masterAllowedChildren = this.getMasterPluginAllowedChildren(this.selectedMasterPlugin);
            if (masterAllowedChildren.includes(template.type)) {
                item.style.border = '2px solid #4CAF50';
                item.style.boxShadow = '0 0 8px rgba(76, 175, 80, 0.4)';
                item.style.background = '#f1f8f4';
            }
        }

        // Hover effect
        item.onmouseenter = () => {
            item.style.background = '#f5f5f5';
            item.style.borderColor = '#FFC107';
        };
        item.onmouseleave = () => {
            item.style.background = 'white';
            item.style.borderColor = '#ddd';
        };

        // Drag events
        item.ondragstart = (e) => {
            this.onDragStart(e, template);
            item.style.cursor = 'grabbing';
        };

        item.ondragend = () => {
            item.style.cursor = 'grab';
        };

        // Keyboard: Enter to add
        item.onkeydown = (e) => {
            if (e.key === 'Enter' && this.onAddNodeCallback) {
                const newNode = createNodeFromTemplate(template);
                this.onAddNodeCallback(newNode);
            } else if (e.key === 'ArrowDown') {
                e.preventDefault();
                const next = item.nextElementSibling as HTMLElement;
                next?.focus();
            } else if (e.key === 'ArrowUp') {
                e.preventDefault();
                const prev = item.previousElementSibling as HTMLElement;
                prev?.focus();
            }
        };

        return item;
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

