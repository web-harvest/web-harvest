/**
 * TabManager - Implementation (TDD Green Phase)
 * All tests in __tests__/TabManager.test.ts should pass!
 */

import type { Tab } from '../types';

export class TabManager {
    private tabs: Tab[] = [];
    private activeTabId: string | null = null;

    /**
     * Generate unique ID for tab
     */
    private generateId(): string {
        return `tab-${Date.now()}-${Math.random().toString(36).substr(2, 9)}`;
    }

    /**
     * Add new tab with optional name
     */
    addTab(name: string = 'untitled.xml'): Tab {
        const tab: Tab = {
            id: this.generateId(),
            name,
            content: '',
            isDirty: false,
        };

        this.tabs.push(tab);
        this.activeTabId = tab.id;

        return tab;
    }

    /**
     * Remove tab by ID
     */
    removeTab(tabId: string): boolean {
        const index = this.tabs.findIndex(t => t.id === tabId);

        if (index === -1) {
            return false;
        }

        // Remove the tab
        this.tabs.splice(index, 1);

        // If we removed the active tab, switch to another
        if (this.activeTabId === tabId) {
            if (this.tabs.length > 0) {
                // Switch to the tab at the same index, or the last tab if we removed the last one
                const newIndex = Math.min(index, this.tabs.length - 1);
                this.activeTabId = this.tabs[newIndex].id;
            } else {
                this.activeTabId = null;
            }
        }

        return true;
    }

    /**
     * Switch to tab by ID
     */
    switchTab(tabId: string): boolean {
        const tab = this.tabs.find(t => t.id === tabId);

        if (!tab) {
            return false;
        }

        this.activeTabId = tabId;
        return true;
    }

    /**
     * Get currently active tab
     */
    getActiveTab(): Tab | null {
        if (!this.activeTabId) {
            return null;
        }

        return this.tabs.find(t => t.id === this.activeTabId) || null;
    }

    /**
     * Get all tabs
     */
    getAllTabs(): Tab[] {
        return [...this.tabs]; // Return copy to prevent external modification
    }

    /**
     * Update tab content
     */
    updateTabContent(tabId: string, content: string): boolean {
        const tab = this.tabs.find(t => t.id === tabId);

        if (!tab) {
            return false;
        }

        tab.content = content;
        tab.isDirty = true;

        return true;
    }

    /**
     * Mark tab as dirty (unsaved changes)
     */
    markDirty(tabId: string): boolean {
        const tab = this.tabs.find(t => t.id === tabId);

        if (!tab) {
            return false;
        }

        tab.isDirty = true;
        return true;
    }

    /**
     * Mark tab as clean (saved)
     */
    markClean(tabId: string): boolean {
        const tab = this.tabs.find(t => t.id === tabId);

        if (!tab) {
            return false;
        }

        tab.isDirty = false;
        return true;
    }

    /**
     * Find tab by name
     */
    findTabByName(name: string): Tab | null {
        return this.tabs.find(t => t.name === name) || null;
    }

    /**
     * Get tab count
     */
    getTabCount(): number {
        return this.tabs.length;
    }

    /**
     * Check if tab exists
     */
    hasTab(tabId: string): boolean {
        return this.tabs.some(t => t.id === tabId);
    }

    /**
     * Rename tab
     */
    renameTab(tabId: string, newName: string): boolean {
        const tab = this.tabs.find(t => t.id === tabId);

        if (!tab) {
            return false;
        }

        tab.name = newName;
        return true;
    }
}

