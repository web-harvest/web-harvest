/**
 * IconLibrary - Implementation
 * Font Awesome icon mapping with category-specific colors
 * Supports metadata-driven icons from PluginManager when available
 */

import type { NodeExecutionState } from './NodeStateRenderer';
import type { PluginManager } from '../core/PluginManager';

export interface IconConfig {
    faClass: string;
    color: string;
    category: string;
    type: string;
}

export class IconLibrary {
    private pluginManager: PluginManager | null = null;

    private readonly ICON_MAP: Record<string, { faClass: string; category: string }> = {
        // Network plugins
        'http': { faClass: 'fa-globe', category: 'network' },
        'ftp': { faClass: 'fa-folder-open', category: 'network' },
        'mail': { faClass: 'fa-envelope', category: 'network' },
        'smtp': { faClass: 'fa-paper-plane', category: 'network' },
        'sftp': { faClass: 'fa-lock', category: 'network' },

        // Extraction plugins
        'xpath': { faClass: 'fa-code-branch', category: 'extraction' },
        'xquery': { faClass: 'fa-code', category: 'extraction' },
        'regexp': { faClass: 'fa-search', category: 'extraction' },
        'html-to-xml': { faClass: 'fa-file-code', category: 'extraction' },
        'json-to-xml': { faClass: 'fa-exchange-alt', category: 'extraction' },
        'xml-to-json': { faClass: 'fa-exchange-alt', category: 'extraction' },

        // Control flow plugins
        'loop': { faClass: 'fa-repeat', category: 'control' },
        'while': { faClass: 'fa-sync', category: 'control' },
        'if': { faClass: 'fa-code-branch', category: 'control' },
        'try': { faClass: 'fa-shield-alt', category: 'control' },
        'catch': { faClass: 'fa-exclamation-triangle', category: 'control' },
        'case': { faClass: 'fa-sitemap', category: 'control' },
        'switch': { faClass: 'fa-sitemap', category: 'control' },

        // Variables plugins
        'def': { faClass: 'fa-save', category: 'variables' },
        'get-var': { faClass: 'fa-download', category: 'variables' },
        'var': { faClass: 'fa-cube', category: 'variables' },

        // Processing plugins
        'script': { faClass: 'fa-code', category: 'scripting' },
        'template': { faClass: 'fa-file-alt', category: 'processing' },
        'xslt': { faClass: 'fa-random', category: 'processing' },

        // Storage plugins
        'file': { faClass: 'fa-file', category: 'storage' },
        'database': { faClass: 'fa-database', category: 'storage' },

        // Utilities
        'log': { faClass: 'fa-terminal', category: 'utilities' },
        'sleep': { faClass: 'fa-clock', category: 'utilities' },
        'return': { faClass: 'fa-reply', category: 'utilities' },
        'exit': { faClass: 'fa-door-open', category: 'utilities' }
    };

    private readonly CATEGORY_COLORS: Record<string, string> = {
        'network': '#2196F3', // Blue
        'extraction': '#4CAF50', // Green
        'control': '#FF9800', // Orange
        'variables': '#9C27B0', // Purple
        'scripting': '#E91E63', // Pink
        'processing': '#00BCD4', // Cyan
        'storage': '#795548', // Brown
        'utilities': '#607D8B' // Blue Gray
    };

    /**
     * Set plugin manager for metadata-driven icons
     */
    setPluginManager(manager: PluginManager | null): void {
        this.pluginManager = manager;
    }

    /**
     * Get icon configuration for plugin type
     * Uses metadata from PluginManager if available, falls back to hardcoded map
     */
    getIcon(pluginType: string): IconConfig {
        // Try to get icon from metadata first
        if (this.pluginManager) {
            const plugin = this.pluginManager.findPlugin(pluginType);
            if (plugin) {
                const iconClass = plugin.icon || 'fa-puzzle-piece';
                const category = plugin.paletteGroup || plugin.group || plugin.category || 'General';
                const color = plugin.color || this.CATEGORY_COLORS[category.toLowerCase()] || '#607D8B';
                
                return {
                    faClass: iconClass.startsWith('fa-') ? iconClass : `fa-${iconClass}`,
                    color: color,
                    category: category.toLowerCase(),
                    type: pluginType
                };
            }
        }

        // Fallback to hardcoded map
        const type = pluginType.toLowerCase();
        const iconData = this.ICON_MAP[type];

        if (!iconData) {
            return {
                faClass: 'fa-cube',
                color: this.CATEGORY_COLORS['utilities'],
                category: 'utilities',
                type: pluginType
            };
        }

        return {
            faClass: iconData.faClass,
            color: this.CATEGORY_COLORS[iconData.category] || '#607D8B',
            category: iconData.category,
            type: pluginType
        };
    }

    /**
     * Get color for category
     */
    getCategoryColor(category: string): string {
        return this.CATEGORY_COLORS[category.toLowerCase()] || '#607D8B';
    }

    /**
     * Create icon element
     */
    getIconElement(pluginType: string, size: number = 16): HTMLElement {
        const iconConfig = this.getIcon(pluginType);

        const icon = document.createElement('i');
        icon.className = `fas ${iconConfig.faClass}`;
        icon.style.color = iconConfig.color;
        icon.style.fontSize = `${size}px`;

        return icon;
    }

    /**
     * Create animated icon for execution state
     */
    getAnimatedIcon(pluginType: string, state: NodeExecutionState, size: number = 16): HTMLElement {
        const icon = this.getIconElement(pluginType, size);

        if (state === 'RUNNING') {
            // Add animation based on plugin type
            if (pluginType === 'loop' || pluginType === 'while') {
                icon.classList.add('fa-spin');
            } else {
                icon.classList.add('fa-pulse');
            }
        }

        return icon;
    }

    /**
     * Get all icon mappings
     */
    getAllIcons(): Record<string, IconConfig> {
        const icons: Record<string, IconConfig> = {};

        Object.keys(this.ICON_MAP).forEach(type => {
            icons[type] = this.getIcon(type);
        });

        return icons;
    }

    /**
     * Get icons by category
     */
    getIconsByCategory(category: string): IconConfig[] {
        const icons: IconConfig[] = [];

        Object.keys(this.ICON_MAP).forEach(type => {
            const iconConfig = this.getIcon(type);
            if (iconConfig.category === category.toLowerCase()) {
                icons.push(iconConfig);
            }
        });

        return icons;
    }

    /**
     * Create icon with badge (for notifications, counts)
     */
    getIconWithBadge(pluginType: string, badgeText: string, size: number = 16): HTMLElement {
        const wrapper = document.createElement('div');
        wrapper.style.cssText = 'position: relative; display: inline-block;';

        const icon = this.getIconElement(pluginType, size);
        wrapper.appendChild(icon);

        const badge = document.createElement('span');
        badge.textContent = badgeText;
        badge.style.cssText = `
            position: absolute;
            top: -8px;
            right: -8px;
            background: var(--magenta);
            color: white;
            border-radius: 10px;
            padding: 2px 6px;
            font-size: 10px;
            font-weight: 600;
        `;
        wrapper.appendChild(badge);

        return wrapper;
    }
}


