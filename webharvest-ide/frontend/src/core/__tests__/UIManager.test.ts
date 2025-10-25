/**
 * UIManager - TDD Tests
 */

import { describe, it, expect, beforeEach } from '@jest/globals';
import { UIManager } from '../UIManager';

describe('UIManager', () => {
    let manager: UIManager;

    beforeEach(() => {
        // Create DOM elements
        document.body.innerHTML = `
      <div id="editor"></div>
      <div id="visual-canvas"></div>
      <div id="plugin-sidebar"></div>
      <div class="sidebar-section"></div>
      <div id="propertyPanel"></div>
    `;

        manager = new UIManager();
    });

    describe('showEditor', () => {
        it('should show editor', () => {
            manager.showEditor(true);
            const editor = document.getElementById('editor');
            expect(editor?.style.display).toBe('block');
        });

        it('should hide editor', () => {
            manager.showEditor(false);
            const editor = document.getElementById('editor');
            expect(editor?.style.display).toBe('none');
        });
    });

    describe('switchToXMLMode', () => {
        it('should show editor and right sidebar', () => {
            manager.switchToXMLMode();

            expect(document.getElementById('editor')?.style.display).toBe('block');
            expect(document.getElementById('visual-canvas')?.style.display).toBe('none');
            expect(document.getElementById('plugin-sidebar')?.style.display).toBe('none');
        });
    });

    describe('switchToVisualMode', () => {
        it('should show canvas and plugin sidebar', () => {
            manager.switchToVisualMode();

            expect(document.getElementById('editor')?.style.display).toBe('none');
            expect(document.getElementById('visual-canvas')?.style.display).toBe('flex');
            expect(document.getElementById('plugin-sidebar')?.style.display).toBe('flex');
        });
    });
});

