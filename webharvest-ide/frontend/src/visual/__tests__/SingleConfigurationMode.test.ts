/**
 * TDD Tests for Single Configuration Mode (Visual Designer)
 * 
 * New UX (inspired by Conductor):
 * - Visual mode works with ONE configuration at a time
 * - No tabs in Visual mode
 * - Track isDirty flag (unsaved changes)
 * - Prompt before loading new configuration
 * - Save/Discard/Cancel dialog
 * 
 * Benefits:
 * - Simpler UX
 * - No tab management complexity
 * - Clear "current state" 
 * - Prevents accidental data loss
 */

import { VisualDesigner } from '../VisualDesigner';
import { App } from '../../core/App';
import type { VisualNode } from '../../types';

describe('Single Configuration Mode (TDD - Conductor-style)', () => {
    let app: App;
    let designer: VisualDesigner;

    beforeEach(() => {
        // Setup DOM
        document.body.innerHTML = `
            <div id="canvas-area"></div>
            <div id="plugin-palette"></div>
            <div id="editor"></div>
        `;

        designer = new VisualDesigner('canvas-area');
    });

    describe('Visual Mode - Single Configuration', () => {
        it('should work with one configuration at a time', () => {
            const config1: VisualNode[] = [
                { id: 'n1', type: 'http', attributes: {}, body: null, children: [] }
            ];

            designer.loadNodes(config1);
            expect(designer.getNodes()).toHaveLength(1);

            // Load new configuration (replaces previous)
            const config2: VisualNode[] = [
                { id: 'n2', type: 'xpath', attributes: {}, body: null, children: [] }
            ];

            designer.loadNodes(config2);
            expect(designer.getNodes()).toHaveLength(1);
            expect(designer.getNodes()[0].id).toBe('n2');
        });

        it('should NOT maintain tabs in Visual mode', () => {
            // Visual mode = no tabs
            // Only XML mode has tabs

            const hasVisualTabs = designer.hasTabs();
            expect(hasVisualTabs).toBe(false);
        });

        it('should have single "current configuration" state', () => {
            const config: VisualNode[] = [
                { id: 'n1', type: 'http', attributes: {}, body: null, children: [] }
            ];

            designer.loadNodes(config);

            const current = designer.getCurrentConfiguration();
            expect(current).toBeTruthy();
            expect(current?.nodes).toHaveLength(1);
        });
    });

    describe('Dirty Flag Tracking', () => {
        it('should track isDirty when nodes are added', () => {
            expect(designer.isDirty()).toBe(false);

            const node: VisualNode = {
                id: 'n1',
                type: 'http',
                attributes: {},
                body: null,
                children: []
            };

            designer.addNode(node);

            expect(designer.isDirty()).toBe(true);
        });

        it('should track isDirty when nodes are modified', () => {
            const node: VisualNode = {
                id: 'n1',
                type: 'http',
                attributes: { url: 'https://api.com' },
                body: null,
                children: []
            };

            designer.loadNodes([node]);
            designer.markClean(); // Fresh load

            expect(designer.isDirty()).toBe(false);

            // Modify node
            node.attributes.url = 'https://newapi.com';
            designer.updateNode('n1', node);

            expect(designer.isDirty()).toBe(true);
        });

        it('should track isDirty when nodes are deleted', () => {
            const node: VisualNode = {
                id: 'n1',
                type: 'http',
                attributes: {},
                body: null,
                children: []
            };

            designer.loadNodes([node]);
            designer.markClean();

            designer.removeNode('n1');

            expect(designer.isDirty()).toBe(true);
        });

        it('should reset isDirty after save', () => {
            const node: VisualNode = {
                id: 'n1',
                type: 'http',
                attributes: {},
                body: null,
                children: []
            };

            designer.addNode(node);
            expect(designer.isDirty()).toBe(true);

            designer.markClean();
            expect(designer.isDirty()).toBe(false);
        });
    });

    describe('Load Configuration Prompts', () => {
        it('should prompt before loading new config if current is dirty', () => {
            // Setup current config
            designer.loadNodes([
                { id: 'n1', type: 'http', attributes: {}, body: null, children: [] }
            ]);
            designer.markClean();

            // Modify (make dirty)
            designer.addNode({
                id: 'n2',
                type: 'xpath',
                attributes: {},
                body: null,
                children: []
            });

            expect(designer.isDirty()).toBe(true);

            // Try to load new config
            const shouldPrompt = designer.shouldPromptBeforeLoad();
            expect(shouldPrompt).toBe(true);
        });

        it('should NOT prompt if configuration is clean', () => {
            designer.loadNodes([
                { id: 'n1', type: 'http', attributes: {}, body: null, children: [] }
            ]);
            designer.markClean();

            const shouldPrompt = designer.shouldPromptBeforeLoad();
            expect(shouldPrompt).toBe(false);
        });

        it('should show Save/Discard/Cancel dialog', () => {
            const dialog = designer.showUnsavedChangesDialog();

            expect(dialog).toBeTruthy();
            expect(dialog.title).toContain('Unsaved Changes');
            expect(dialog.buttons).toHaveLength(3);
            expect(dialog.buttons).toContainEqual(expect.objectContaining({ label: 'Save' }));
            expect(dialog.buttons).toContainEqual(expect.objectContaining({ label: 'Discard' }));
            expect(dialog.buttons).toContainEqual(expect.objectContaining({ label: 'Cancel' }));
        });

        it('should cancel load if user clicks Cancel', async () => {
            designer.loadNodes([
                { id: 'n1', type: 'http', attributes: {}, body: null, children: [] }
            ]);
            designer.markClean();
            designer.addNode({ id: 'n2', type: 'xpath', attributes: {}, body: null, children: [] });

            // Mock user clicks "Cancel"
            const mockDialog = jest.fn().mockResolvedValue('cancel');
            (designer as any).showDialog = mockDialog;

            const newConfig = [
                { id: 'n3', type: 'def', attributes: {}, body: null, children: [] }
            ];

            await designer.loadNodesWithPrompt(newConfig);

            // Should NOT load new config (still has n1, n2)
            expect(designer.getNodes()).toHaveLength(2);
        });

        it('should discard changes if user clicks Discard', async () => {
            designer.loadNodes([
                { id: 'n1', type: 'http', attributes: {}, body: null, children: [] }
            ]);
            designer.markClean();
            designer.addNode({ id: 'n2', type: 'xpath', attributes: {}, body: null, children: [] });

            // Mock user clicks "Discard"
            const mockDialog = jest.fn().mockResolvedValue('discard');
            (designer as any).showDialog = mockDialog;

            const newConfig = [
                { id: 'n3', type: 'def', attributes: {}, body: null, children: [] }
            ];

            await designer.loadNodesWithPrompt(newConfig);

            // Should load new config (only n3)
            expect(designer.getNodes()).toHaveLength(1);
            expect(designer.getNodes()[0].id).toBe('n3');
            expect(designer.isDirty()).toBe(false);
        });

        it('should save before loading if user clicks Save', async () => {
            designer.loadNodes([
                { id: 'n1', type: 'http', attributes: {}, body: null, children: [] }
            ]);
            designer.markClean();
            designer.addNode({ id: 'n2', type: 'xpath', attributes: {}, body: null, children: [] });

            const saveSpy = jest.fn().mockResolvedValue(true);
            (designer as any).saveConfiguration = saveSpy;

            // Mock user clicks "Save"
            const mockDialog = jest.fn().mockResolvedValue('save');
            (designer as any).showDialog = mockDialog;

            const newConfig = [
                { id: 'n3', type: 'def', attributes: {}, body: null, children: [] }
            ];

            await designer.loadNodesWithPrompt(newConfig);

            // Should have saved old config
            expect(saveSpy).toHaveBeenCalled();

            // Then loaded new config
            expect(designer.getNodes()).toHaveLength(1);
            expect(designer.getNodes()[0].id).toBe('n3');
        });
    });

    describe('Configuration File Management', () => {
        it('should track current configuration filename', () => {
            designer.setCurrentFilename('my-workflow.xml');

            expect(designer.getCurrentFilename()).toBe('my-workflow.xml');
        });

        it('should clear filename when creating new configuration', () => {
            designer.setCurrentFilename('existing.xml');
            designer.newConfiguration();

            expect(designer.getCurrentFilename()).toBeNull();
            expect(designer.getNodes()).toHaveLength(0);
            expect(designer.isDirty()).toBe(false);
        });

        it('should update filename after save as', async () => {
            designer.loadNodes([
                { id: 'n1', type: 'http', attributes: {}, body: null, children: [] }
            ]);

            await designer.saveConfigurationAs('new-workflow.xml');

            expect(designer.getCurrentFilename()).toBe('new-workflow.xml');
            expect(designer.isDirty()).toBe(false);
        });
    });

    describe('Browser Warning on Unsaved Changes', () => {
        it('should warn user before closing browser tab if dirty', () => {
            designer.loadNodes([
                { id: 'n1', type: 'http', attributes: {}, body: null, children: [] }
            ]);
            designer.markClean();
            designer.addNode({ id: 'n2', type: 'xpath', attributes: {}, body: null, children: [] });

            // Mock beforeunload event
            const event = new Event('beforeunload') as any;
            event.preventDefault = jest.fn();

            const shouldWarn = designer.handleBeforeUnload(event);

            expect(shouldWarn).toBe(true);
            expect(event.returnValue).toBeTruthy(); // Browser shows warning
        });

        it('should NOT warn if configuration is clean', () => {
            designer.loadNodes([
                { id: 'n1', type: 'http', attributes: {}, body: null, children: [] }
            ]);
            designer.markClean();

            const event = new Event('beforeunload') as any;
            const shouldWarn = designer.handleBeforeUnload(event);

            expect(shouldWarn).toBe(false);
        });
    });

    describe('Integration with XML Mode', () => {
        it('should sync Visual → XML when switching modes', () => {
            const nodes: VisualNode[] = [
                { id: 'n1', type: 'http', attributes: { url: 'https://api.com' }, body: null, children: [] }
            ];

            designer.loadNodes(nodes);

            const xml = designer.generateXML();

            expect(xml).toContain('<http');
            expect(xml).toContain('url="https://api.com"');
        });

        it('should warn about unsaved Visual changes before switching to XML', () => {
            designer.loadNodes([
                { id: 'n1', type: 'http', attributes: {}, body: null, children: [] }
            ]);
            designer.markClean();

            // Add node (dirty)
            designer.addNode({ id: 'n2', type: 'xpath', attributes: {}, body: null, children: [] });

            const shouldPrompt = designer.shouldPromptBeforeModeSwitch();
            expect(shouldPrompt).toBe(true);
        });
    });

    describe('Auto-Save (Optional)', () => {
        it('should auto-save to localStorage every 30 seconds if dirty', () => {
            jest.useFakeTimers();

            designer.enableAutoSave(true);

            designer.loadNodes([
                { id: 'n1', type: 'http', attributes: {}, body: null, children: [] }
            ]);

            // After 30 seconds
            jest.advanceTimersByTime(30000);

            const saved = localStorage.getItem('webharvest-autosave');
            expect(saved).toBeTruthy();

            jest.useRealTimers();
        });

        it('should restore from autosave on startup', () => {
            const nodes: VisualNode[] = [
                { id: 'n1', type: 'http', attributes: { url: 'https://saved.com' }, body: null, children: [] }
            ];

            localStorage.setItem('webharvest-autosave', JSON.stringify(nodes));

            designer.restoreFromAutoSave();

            expect(designer.getNodes()).toHaveLength(1);
            expect(designer.getNodes()[0].attributes.url).toBe('https://saved.com');
        });
    });
});




