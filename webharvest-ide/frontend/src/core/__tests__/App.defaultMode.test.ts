/**
 * TDD Tests for Default Mode (Visual-first UX)
 * 
 * Requirement: IDE should start in Visual mode by default
 * Inspired by: Conductor (visual-first workflow editor)
 */

import { App } from '../App';

describe('App - Default Mode (TDD)', () => {
    beforeEach(() => {
        // Setup DOM
        document.body.innerHTML = `
            <div id="tabs-container"></div>
            <div id="visual-toolbar"></div>
            <div id="editor"></div>
            <div id="canvas-area"></div>
            <div id="plugin-sidebar"></div>
            <div id="plugin-palette"></div>
            <div id="propertyPanel"></div>
            <div id="logs"></div>
            <div id="results"></div>
            <div id="variables"></div>
            <div id="session"></div>
        `;

        // Mock Monaco
        (global as any).monaco = {
            editor: {
                create: jest.fn().mockReturnValue({
                    onDidChangeModelContent: jest.fn(),
                    getValue: jest.fn().mockReturnValue(''),
                    setValue: jest.fn(),
                    getModel: jest.fn(),
                    dispose: jest.fn()
                })
            }
        };

        // Mock fetch
        global.fetch = jest.fn().mockResolvedValue({
            ok: true,
            json: async () => ({ count: 0, plugins: [] })
        });
    });

    it('should start in Visual mode by default', async () => {
        const app = new App();

        // Wait for initialization
        await new Promise(resolve => setTimeout(resolve, 100));

        // Should be in visual mode
        expect((app as any).currentMode).toBe('visual');
    });

    it('should show Visual UI on startup', async () => {
        const app = new App();

        await new Promise(resolve => setTimeout(resolve, 100));

        // Visual UI should be visible
        const visualToolbar = document.getElementById('visual-toolbar');
        const tabsContainer = document.getElementById('tabs-container');

        expect(visualToolbar?.style.display).not.toBe('none');
        expect(tabsContainer?.style.display).toBe('none');
    });

    it('should show Plugin Palette on startup', async () => {
        const app = new App();

        await new Promise(resolve => setTimeout(resolve, 100));

        const pluginSidebar = document.getElementById('plugin-sidebar');
        expect(pluginSidebar?.style.display).not.toBe('none');
    });

    it('should hide Monaco Editor on startup', async () => {
        const app = new App();

        await new Promise(resolve => setTimeout(resolve, 100));

        const editor = document.getElementById('editor');
        expect(editor?.style.display).toBe('none');
    });

    it('should allow switching to XML mode after startup', async () => {
        const app = new App();

        await new Promise(resolve => setTimeout(resolve, 100));

        // Should start in visual
        expect((app as any).currentMode).toBe('visual');

        // Switch to XML
        await app.toggleVisualMode();

        // Should now be in XML mode
        expect((app as any).currentMode).toBe('xml');
    });

    it('should show tabs when switching to XML mode', async () => {
        const app = new App();

        await new Promise(resolve => setTimeout(resolve, 100));

        // Start in visual (tabs hidden)
        const tabsContainer = document.getElementById('tabs-container');
        expect(tabsContainer?.style.display).toBe('none');

        // Switch to XML
        await app.toggleVisualMode();

        // Tabs should now be visible
        expect(tabsContainer?.style.display).not.toBe('none');
    });
});

