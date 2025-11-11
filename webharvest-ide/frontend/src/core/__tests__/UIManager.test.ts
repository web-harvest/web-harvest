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
      <div id="executionControls" style="display:none;">
        <button id="canvasRunBtn"></button>
        <button id="canvasValidateBtn"></button>
        <div id="executionActionButtons">
          <button id="pauseBtn"></button>
          <button id="resumeBtn" style="display:none;"></button>
        </div>
      </div>
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

    describe('setExecutionState', () => {
        it('enables controls when idle', () => {
            const runBtn = document.getElementById('canvasRunBtn') as HTMLButtonElement;
            const validateBtn = document.getElementById('canvasValidateBtn') as HTMLButtonElement;
            const pauseBtn = document.getElementById('pauseBtn') as HTMLButtonElement;
            const resumeBtn = document.getElementById('resumeBtn') as HTMLButtonElement;

            manager.setExecutionState('idle');

            expect(runBtn.disabled).toBe(false);
            expect(validateBtn.disabled).toBe(false);
            expect(pauseBtn.disabled).toBe(true);
            expect(resumeBtn.style.display).toBe('none');
        });

        it('disables run/validate while running', () => {
            const runBtn = document.getElementById('canvasRunBtn') as HTMLButtonElement;
            const validateBtn = document.getElementById('canvasValidateBtn') as HTMLButtonElement;
            const pauseBtn = document.getElementById('pauseBtn') as HTMLButtonElement;
            const resumeBtn = document.getElementById('resumeBtn') as HTMLButtonElement;

            manager.setExecutionState('running');

            expect(runBtn.disabled).toBe(true);
            expect(validateBtn.disabled).toBe(true);
            expect(pauseBtn.disabled).toBe(false);
            expect(resumeBtn.style.display).toBe('none');
        });

        it('shows resume button when paused', () => {
            const resumeBtn = document.getElementById('resumeBtn') as HTMLButtonElement;
            const pauseBtn = document.getElementById('pauseBtn') as HTMLButtonElement;

            manager.setExecutionState('paused');

            expect(pauseBtn.disabled).toBe(false);
            expect(resumeBtn.style.display).toBe('inline-flex');
        });
    });
});







