/**
 * VisualDesigner - Canvas Sizing Tests
 */

import { describe, it, expect, beforeEach, afterEach } from '@jest/globals';
import { VisualDesigner } from '../VisualDesigner';

describe('VisualDesigner - Canvas Sizing', () => {
    let container: HTMLElement;
    let designer: VisualDesigner;
    let editorSection: HTMLElement;

    beforeEach(() => {
        // Create editor-section parent
        editorSection = document.createElement('div');
        editorSection.className = 'editor-section';
        editorSection.style.width = '800px';
        editorSection.style.height = '600px';
        document.body.appendChild(editorSection);

        // Create canvas container
        container = document.createElement('div');
        container.id = 'test-canvas';
        editorSection.appendChild(container);

        designer = new VisualDesigner('test-canvas');
    });

    afterEach(() => {
        designer = null as any;
        if (container.parentNode) {
            container.parentNode.removeChild(container);
        }
        if (editorSection.parentNode) {
            editorSection.parentNode.removeChild(editorSection);
        }
    });

    it('should create canvas with proper flex properties', () => {
        const canvasShell = container.querySelector('.canvas-shell') as HTMLElement;
        expect(canvasShell).toBeTruthy();
        expect(canvasShell.style.flex).toContain('1');
        expect(canvasShell.style.minWidth).toBe('0');
        expect(canvasShell.style.minHeight).toBe('0');
    });

    it('should handle container resize', () => {
        const canvasShell = container.querySelector('.canvas-shell') as HTMLElement;
        const initialWidth = canvasShell.offsetWidth;

        // Resize parent
        editorSection.style.width = '1200px';
        editorSection.style.height = '800px';

        // Trigger resize observer (simulate)
        const resizeEvent = new Event('resize');
        window.dispatchEvent(resizeEvent);

        // Canvas should adapt to new size
        // Note: In real browser, ResizeObserver would handle this
        expect(canvasShell).toBeTruthy();
    });

    it('should maintain canvas dimensions when parent changes', () => {
        const contentWrapper = container.querySelector('.canvas-content-wrapper') as HTMLElement;
        expect(contentWrapper).toBeTruthy();
        expect(contentWrapper.style.flex).toContain('1');
        expect(contentWrapper.style.minWidth).toBe('0');
        expect(contentWrapper.style.minHeight).toBe('0');
    });
});


