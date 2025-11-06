/**
 * KeyboardShortcutManager - Manages keyboard shortcuts from VisualPluginMetadata
 * Implements Apple HIG compliance for keyboard navigation
 * 
 * @author Robert Bala
 * @since 2.3.0
 */

import type { Plugin } from '../types';
import type { PluginManager } from './PluginManager';

export interface ShortcutHandler {
    (pluginId: string, event: KeyboardEvent): boolean; // Returns true if handled
}

export class KeyboardShortcutManager {
    private pluginManager: PluginManager | null = null;
    private handlers: Map<string, ShortcutHandler> = new Map();
    private globalShortcuts: Map<string, () => void> = new Map();
    private isEnabled: boolean = true;

    constructor(pluginManager?: PluginManager | null) {
        this.pluginManager = pluginManager || null;
        this.setupGlobalShortcuts();
    }

    /**
     * Set plugin manager for accessing keyboard shortcuts from metadata
     */
    setPluginManager(pluginManager: PluginManager): void {
        this.pluginManager = pluginManager;
    }

    /**
     * Enable/disable keyboard shortcuts
     */
    setEnabled(enabled: boolean): void {
        this.isEnabled = enabled;
    }

    /**
     * Register handler for plugin-specific shortcuts
     */
    registerHandler(pluginId: string, handler: ShortcutHandler): void {
        this.handlers.set(pluginId, handler);
    }

    /**
     * Register global shortcut handler
     */
    registerGlobalShortcut(key: string, handler: () => void): void {
        this.globalShortcuts.set(this.normalizeKey(key), handler);
    }

    /**
     * Setup default global shortcuts (Apple HIG compliant)
     */
    private setupGlobalShortcuts(): void {
        // Standard Apple HIG shortcuts will be registered here
        // These are handled separately in App.ts for now
    }

    /**
     * Handle keyboard event - checks both plugin-specific and global shortcuts
     */
    handleKeyDown(event: KeyboardEvent): boolean {
        if (!this.isEnabled) {
            return false;
        }

        // Ignore when typing in inputs/textareas
        const target = event.target as HTMLElement;
        const tag = target?.tagName?.toLowerCase();
        if (tag === 'input' || tag === 'textarea' || target?.isContentEditable) {
            return false;
        }

        // Check global shortcuts first
        const keyString = this.buildKeyString(event);
        const globalHandler = this.globalShortcuts.get(keyString);
        if (globalHandler) {
            globalHandler();
            event.preventDefault();
            event.stopPropagation();
            return true;
        }

        // Check plugin-specific shortcuts from metadata
        if (this.pluginManager) {
            const plugins = this.pluginManager.getPlugins();
            
            for (const plugin of plugins) {
                if (!plugin.keyboardShortcuts || plugin.keyboardShortcuts.length === 0) {
                    continue;
                }

                for (const shortcut of plugin.keyboardShortcuts) {
                    if (this.matchesShortcut(event, shortcut.key)) {
                        // Check if shortcut is global or context-specific
                        if (shortcut.isGlobal) {
                            // Global shortcut - check if we have a handler
                            const handler = this.handlers.get(plugin.id);
                            if (handler) {
                                const handled = handler(plugin.id, event);
                                if (handled) {
                                    event.preventDefault();
                                    event.stopPropagation();
                                    return true;
                                }
                            }
                        } else {
                            // Context-specific shortcut - only if plugin is selected/focused
                            // This will be handled by the visual designer or property panel
                            const handler = this.handlers.get(plugin.id);
                            if (handler) {
                                const handled = handler(plugin.id, event);
                                if (handled) {
                                    event.preventDefault();
                                    event.stopPropagation();
                                    return true;
                                }
                            }
                        }
                    }
                }
            }
        }

        return false;
    }

    /**
     * Build key string from keyboard event (e.g., "Cmd+N", "Ctrl+Shift+H")
     */
    private buildKeyString(event: KeyboardEvent): string {
        const parts: string[] = [];
        
        if (event.metaKey) {
            parts.push('Cmd');
        } else if (event.ctrlKey) {
            parts.push('Ctrl');
        }
        
        if (event.altKey) {
            parts.push('Alt');
        }
        
        if (event.shiftKey) {
            parts.push('Shift');
        }
        
        // Map special keys
        let key = event.key;
        if (key === ' ') {
            key = 'Space';
        } else if (key === '=' || key === '+') {
            key = 'Equal';
        } else if (key === '-') {
            key = 'Minus';
        } else if (key.length === 1) {
            key = key.toUpperCase();
        }
        
        parts.push(key);
        
        return parts.join('+');
    }

    /**
     * Normalize key string for comparison (e.g., "Cmd+N" -> "cmd+n")
     */
    private normalizeKey(key: string): string {
        return key.toLowerCase().replace(/\s+/g, '');
    }

    /**
     * Check if keyboard event matches shortcut string
     */
    private matchesShortcut(event: KeyboardEvent, shortcutKey: string): boolean {
        const normalized = this.normalizeKey(shortcutKey);
        const eventKey = this.buildKeyString(event).toLowerCase();
        
        // Handle variations (Cmd vs Ctrl)
        const isMac = navigator.platform.toUpperCase().indexOf('MAC') >= 0;
        let normalizedEvent = eventKey;
        let normalizedShortcut = normalized;
        
        if (isMac) {
            // On Mac, convert Ctrl to Cmd for comparison
            normalizedEvent = normalizedEvent.replace(/ctrl\+/g, 'cmd+');
            normalizedShortcut = normalizedShortcut.replace(/ctrl\+/g, 'cmd+');
        } else {
            // On Windows/Linux, convert Cmd to Ctrl for comparison
            normalizedEvent = normalizedEvent.replace(/cmd\+/g, 'ctrl+');
            normalizedShortcut = normalizedShortcut.replace(/cmd\+/g, 'ctrl+');
        }
        
        return normalizedShortcut === normalizedEvent;
    }

    /**
     * Get all keyboard shortcuts for a plugin
     */
    getPluginShortcuts(pluginId: string): Array<{ key: string; action: string; isGlobal: boolean }> {
        if (!this.pluginManager) {
            return [];
        }

        const plugin = this.pluginManager.findPlugin(pluginId);
        if (!plugin || !plugin.keyboardShortcuts) {
            return [];
        }

        return plugin.keyboardShortcuts.map(s => ({
            key: s.key,
            action: s.action,
            isGlobal: s.isGlobal || false
        }));
    }

    /**
     * Get all available keyboard shortcuts (for help menu)
     */
    getAllShortcuts(): Array<{ pluginId: string; pluginName: string; shortcuts: Array<{ key: string; action: string; isGlobal: boolean }> }> {
        if (!this.pluginManager) {
            return [];
        }

        const result: Array<{ pluginId: string; pluginName: string; shortcuts: Array<{ key: string; action: string; isGlobal: boolean }> }> = [];
        const plugins = this.pluginManager.getPlugins();

        for (const plugin of plugins) {
            if (plugin.keyboardShortcuts && plugin.keyboardShortcuts.length > 0) {
                result.push({
                    pluginId: plugin.id,
                    pluginName: plugin.name,
                    shortcuts: plugin.keyboardShortcuts.map(s => ({
                        key: s.key,
                        action: s.action,
                        isGlobal: s.isGlobal || false
                    }))
                });
            }
        }

        return result;
    }
}

