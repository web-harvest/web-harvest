/**
 * TabManager - TDD Tests
 * Test FIRST, then implement!
 */

import { describe, it, expect, beforeEach } from '@jest/globals';
import { TabManager } from '../TabManager';

describe('TabManager', () => {
    let manager: TabManager;

    beforeEach(() => {
        manager = new TabManager();
    });

    describe('addTab', () => {
        it('should create a new tab with default name', () => {
            const tab = manager.addTab();

            expect(tab).toBeDefined();
            expect(tab.name).toBe('untitled.xml');
            expect(tab.content).toContain('<config');
            expect(tab.isDirty).toBe(false);
        });

        it('should create a new tab with custom name', () => {
            const tab = manager.addTab('test.xml');

            expect(tab.name).toBe('test.xml');
        });

        it('should assign unique IDs to tabs', () => {
            const tab1 = manager.addTab();
            const tab2 = manager.addTab();

            expect(tab1.id).not.toBe(tab2.id);
        });

        it('should automatically switch to new tab', () => {
            const tab = manager.addTab();

            expect(manager.getActiveTab()?.id).toBe(tab.id);
        });

        it('should add tab to tabs list', () => {
            manager.addTab('tab1.xml');
            manager.addTab('tab2.xml');

            const tabs = manager.getAllTabs();
            expect(tabs).toHaveLength(2);
        });
    });

    describe('removeTab', () => {
        it('should remove existing tab', () => {
            const tab = manager.addTab();

            const result = manager.removeTab(tab.id);

            expect(result).toBe(true);
            expect(manager.getAllTabs()).toHaveLength(0);
        });

        it('should return false for non-existing tab', () => {
            const result = manager.removeTab('non-existing');

            expect(result).toBe(false);
        });

        it('should switch to next tab when removing active tab', () => {
            const tab1 = manager.addTab('tab1.xml');
            const tab2 = manager.addTab('tab2.xml');

            manager.switchTab(tab1.id);
            manager.removeTab(tab1.id);

            expect(manager.getActiveTab()?.id).toBe(tab2.id);
        });

        it('should clear active tab when removing last tab', () => {
            const tab = manager.addTab();

            manager.removeTab(tab.id);

            expect(manager.getActiveTab()).toBeNull();
        });
    });

    describe('switchTab', () => {
        it('should switch to existing tab', () => {
            const tab1 = manager.addTab('tab1.xml');
            const tab2 = manager.addTab('tab2.xml');

            const result = manager.switchTab(tab1.id);

            expect(result).toBe(true);
            expect(manager.getActiveTab()?.id).toBe(tab1.id);
        });

        it('should return false for non-existing tab', () => {
            const result = manager.switchTab('non-existing');

            expect(result).toBe(false);
        });
    });

    describe('getActiveTab', () => {
        it('should return null when no tabs exist', () => {
            expect(manager.getActiveTab()).toBeNull();
        });

        it('should return active tab', () => {
            const tab = manager.addTab();

            expect(manager.getActiveTab()?.id).toBe(tab.id);
        });
    });

    describe('getAllTabs', () => {
        it('should return empty array when no tabs exist', () => {
            expect(manager.getAllTabs()).toEqual([]);
        });

        it('should return all tabs', () => {
            manager.addTab('tab1.xml');
            manager.addTab('tab2.xml');
            manager.addTab('tab3.xml');

            expect(manager.getAllTabs()).toHaveLength(3);
        });
    });

    describe('updateTabContent', () => {
        it('should update tab content', () => {
            const tab = manager.addTab();

            const result = manager.updateTabContent(tab.id, '<config></config>');

            expect(result).toBe(true);
            expect(manager.getActiveTab()?.content).toBe('<config></config>');
        });

        it('should mark tab as dirty when content changes', () => {
            const tab = manager.addTab();

            manager.updateTabContent(tab.id, '<config></config>');

            expect(manager.getActiveTab()?.isDirty).toBe(true);
        });

        it('should return false for non-existing tab', () => {
            const result = manager.updateTabContent('non-existing', 'content');

            expect(result).toBe(false);
        });
    });

    describe('markDirty / markClean', () => {
        it('should mark tab as dirty', () => {
            const tab = manager.addTab();

            manager.markDirty(tab.id);

            expect(manager.getActiveTab()?.isDirty).toBe(true);
        });

        it('should mark tab as clean', () => {
            const tab = manager.addTab();

            manager.markDirty(tab.id);
            manager.markClean(tab.id);

            expect(manager.getActiveTab()?.isDirty).toBe(false);
        });

        it('should return false for non-existing tab', () => {
            expect(manager.markDirty('non-existing')).toBe(false);
            expect(manager.markClean('non-existing')).toBe(false);
        });
    });
});

