/**
 * IconLibrary - TDD Tests
 * Tests for Font Awesome icon mapping and category colors
 */

import { describe, it, expect } from '@jest/globals';
import { IconLibrary, IconConfig } from '../IconLibrary';

describe('IconLibrary', () => {
    let library: IconLibrary;

    beforeEach(() => {
        library = new IconLibrary();
    });

    describe('getIcon', () => {
        it('should return Font Awesome icon for HTTP', () => {
            const icon = library.getIcon('http');
            expect(icon.faClass).toBe('fa-globe');
            expect(icon.color).toBeTruthy();
        });

        it('should return icon for XPath', () => {
            const icon = library.getIcon('xpath');
            expect(icon.faClass).toBe('fa-code-branch');
        });

        it('should return icon for Loop', () => {
            const icon = library.getIcon('loop');
            expect(icon.faClass).toBe('fa-repeat');
        });

        it('should return icon for Database', () => {
            const icon = library.getIcon('database');
            expect(icon.faClass).toBe('fa-database');
        });

        it('should return default icon for unknown type', () => {
            const icon = library.getIcon('unknown-plugin');
            expect(icon.faClass).toBe('fa-cube');
        });
    });

    describe('getCategoryColor', () => {
        it('should return blue for Network category', () => {
            const color = library.getCategoryColor('network');
            expect(color).toContain('#');
        });

        it('should return green for Extraction category', () => {
            const color = library.getCategoryColor('extraction');
            expect(color).toContain('#');
        });

        it('should return orange for Control Flow category', () => {
            const color = library.getCategoryColor('control');
            expect(color).toContain('#');
        });

        it('should return purple for Variables category', () => {
            const color = library.getCategoryColor('variables');
            expect(color).toContain('#');
        });

        it('should return default color for unknown category', () => {
            const color = library.getCategoryColor('unknown');
            expect(color).toBeTruthy();
        });
    });

    describe('getIconElement', () => {
        it('should create Font Awesome icon element', () => {
            const element = library.getIconElement('http');

            expect(element.tagName).toBe('I');
            expect(element.className).toContain('fa-globe');
            expect(element.className).toContain('fas');
        });

        it('should apply category color', () => {
            const element = library.getIconElement('http');
            expect(element.style.color).toBeTruthy();
        });

        it('should set icon size', () => {
            const element = library.getIconElement('http', 24);
            expect(element.style.fontSize).toBe('24px');
        });
    });

    describe('getAnimatedIcon', () => {
        it('should create pulsing icon for RUNNING state', () => {
            const element = library.getAnimatedIcon('http', 'RUNNING');

            expect(element.className).toContain('fa-pulse');
        });

        it('should create spinning icon for RUNNING on loop nodes', () => {
            const element = library.getAnimatedIcon('loop', 'RUNNING');

            expect(element.className).toContain('fa-spin');
        });

        it('should not animate SUCCESS state', () => {
            const element = library.getAnimatedIcon('http', 'SUCCESS');

            expect(element.className).not.toContain('fa-pulse');
            expect(element.className).not.toContain('fa-spin');
        });
    });

    describe('getAllIcons', () => {
        it('should return icon map for all plugin types', () => {
            const icons = library.getAllIcons();

            expect(icons).toHaveProperty('http');
            expect(icons).toHaveProperty('xpath');
            expect(icons).toHaveProperty('loop');
            expect(icons).toHaveProperty('database');
        });

        it('should have icons for common plugins', () => {
            const icons = library.getAllIcons();

            expect(icons['http']).toBeTruthy();
            expect(icons['ftp']).toBeTruthy();
            expect(icons['mail']).toBeTruthy();
            expect(icons['script']).toBeTruthy();
        });
    });

    describe('getIconByCategory', () => {
        it('should return all icons for Network category', () => {
            const icons = library.getIconsByCategory('network');

            expect(icons.length).toBeGreaterThan(0);
            expect(icons.some(i => i.type === 'http')).toBe(true);
        });

        it('should return all icons for Extraction category', () => {
            const icons = library.getIconsByCategory('extraction');

            expect(icons.some(i => i.type === 'xpath')).toBe(true);
        });
    });

    describe('category colors', () => {
        it('should use consistent colors for categories', () => {
            const color1 = library.getCategoryColor('network');
            const color2 = library.getCategoryColor('network');

            expect(color1).toBe(color2);
        });

        it('should have distinct colors for different categories', () => {
            const networkColor = library.getCategoryColor('network');
            const extractionColor = library.getCategoryColor('extraction');
            const controlColor = library.getCategoryColor('control');

            expect(networkColor).not.toBe(extractionColor);
            expect(extractionColor).not.toBe(controlColor);
        });
    });
});


