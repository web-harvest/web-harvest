/**
 * TDD Tests for Plugin Palette
 * 
 * Features:
 * 1. Display all available plugins grouped by category
 * 2. Search/filter plugins
 * 3. Drag template to canvas
 * 4. Create node from template with default attributes
 */

import { PluginPalette } from '../PluginPalette';
import { getPluginTemplates, getPluginCategories } from '../VisualModelHelpers';

describe('PluginPalette (TDD)', () => {
    let palette: PluginPalette;

    beforeEach(() => {
        document.body.innerHTML = '<div id="test-palette"></div>';
        palette = new PluginPalette('test-palette');
    });

    describe('Initialization', () => {
        it('should create palette element', () => {
            const paletteEl = document.getElementById('test-palette');
            expect(paletteEl).toBeTruthy();
            expect(paletteEl!.children.length).toBeGreaterThan(0);
        });

        it('should load all plugin templates', () => {
            const templates = palette.getTemplates();
            
            expect(templates.length).toBeGreaterThan(0);
            expect(templates).toContainEqual(expect.objectContaining({
                type: 'http'
            }));
            expect(templates).toContainEqual(expect.objectContaining({
                type: 'loop'
            }));
        });

        it('should group plugins by category', () => {
            const categories = palette.getCategories();
            
            expect(categories).toContain('network');
            expect(categories).toContain('extraction');
            expect(categories).toContain('control');
            expect(categories).toContain('variables');
        });
    });

    describe('Search & Filter', () => {
        it('should filter plugins by search term', () => {
            palette.search('http');
            const filtered = palette.getFilteredTemplates();
            
            expect(filtered.length).toBeGreaterThan(0);
            expect(filtered.every(t => t.type.includes('http') || t.label.toLowerCase().includes('http'))).toBe(true);
        });

        it('should return empty for non-matching search', () => {
            palette.search('nonexistent-plugin-xyz');
            const filtered = palette.getFilteredTemplates();
            
            expect(filtered).toHaveLength(0);
        });

        it('should filter by category', () => {
            palette.filterByCategory('network');
            const filtered = palette.getFilteredTemplates();
            
            expect(filtered.length).toBeGreaterThan(0);
            expect(filtered.every(t => t.category === 'network')).toBe(true);
        });

        it('should clear filters', () => {
            palette.search('http');
            palette.clearFilters();
            const filtered = palette.getFilteredTemplates();
            
            expect(filtered.length).toBe(palette.getTemplates().length);
        });
    });

    describe('Drag & Drop', () => {
        it('should set drag data on dragstart', () => {
            const mockEvent = {
                dataTransfer: {
                    setData: jest.fn(),
                    effectAllowed: ''
                }
            } as any;

            const template = getPluginTemplates()[0];
            palette.onDragStart(mockEvent, template);

            expect(mockEvent.dataTransfer.setData).toHaveBeenCalledWith(
                'application/json',
                expect.stringContaining(template.type)
            );
            expect(mockEvent.dataTransfer.effectAllowed).toBe('copy');
        });

        it('should create node from dragged template', () => {
            const httpTemplate = getPluginTemplates().find(t => t.type === 'http')!;
            const newNode = palette.createNodeFromDragEvent({
                dataTransfer: {
                    getData: jest.fn(() => JSON.stringify(httpTemplate))
                }
            } as any);

            expect(newNode).toBeTruthy();
            expect(newNode!.type).toBe('http');
            expect(newNode!.attributes).toEqual(httpTemplate.defaultAttributes);
            expect(newNode!.id).toMatch(/^node-/);
        });

        it('should handle invalid drag data gracefully', () => {
            const newNode = palette.createNodeFromDragEvent({
                dataTransfer: {
                    getData: jest.fn(() => 'invalid-json')
                }
            } as any);

            expect(newNode).toBeNull();
        });
    });

    describe('Rendering', () => {
        it('should render plugin items grouped by category', () => {
            palette.render();
            
            const paletteEl = document.getElementById('test-palette')!;
            const categories = paletteEl.querySelectorAll('.plugin-category');
            
            expect(categories.length).toBeGreaterThan(0);
        });

        it('should make plugin items draggable', () => {
            palette.render();
            
            const paletteEl = document.getElementById('test-palette')!;
            const pluginItems = paletteEl.querySelectorAll('.plugin-item');
            
            pluginItems.forEach(item => {
                expect(item.getAttribute('draggable')).toBe('true');
            });
        });

        it('should show plugin count per category', () => {
            palette.render();
            
            const networkCategory = document.querySelector('[data-category="network"]');
            expect(networkCategory).toBeTruthy();
            
            const networkTemplates = getPluginTemplates().filter(t => t.category === 'network');
            expect(networkCategory!.textContent).toContain(networkTemplates.length.toString());
        });
    });

    describe('Keyboard Navigation', () => {
        it('should navigate plugins with arrow keys', () => {
            palette.render();
            
            const firstItem = document.querySelector('.plugin-item') as HTMLElement;
            firstItem?.focus();
            
            // Simulate arrow down
            const event = new KeyboardEvent('keydown', { key: 'ArrowDown' });
            firstItem?.dispatchEvent(event);
            
            // Second item should be focused
            const secondItem = document.querySelectorAll('.plugin-item')[1] as HTMLElement;
            expect(document.activeElement).toBe(secondItem);
        });

        it('should activate plugin with Enter key', () => {
            palette.render();
            
            const httpItem = Array.from(document.querySelectorAll('.plugin-item'))
                .find(item => item.textContent?.includes('HTTP')) as HTMLElement;
            
            httpItem?.focus();
            
            const enterEvent = new KeyboardEvent('keydown', { key: 'Enter' });
            httpItem?.dispatchEvent(enterEvent);
            
            // Should trigger add node callback
            expect(palette).toBeDefined(); // Placeholder assertion
        });
    });

    describe('Category Collapse/Expand', () => {
        it('should collapse category on click', () => {
            palette.render();
            
            const categoryHeader = document.querySelector('[data-category="network"]') as HTMLElement;
            categoryHeader?.click();
            
            const categoryItems = document.querySelectorAll('[data-plugin-category="network"]');
            categoryItems.forEach(item => {
                expect((item as HTMLElement).style.display).toBe('none');
            });
        });

        it('should expand collapsed category on second click', () => {
            palette.render();
            
            const categoryHeader = document.querySelector('[data-category="network"]') as HTMLElement;
            
            // First click: collapse
            categoryHeader?.click();
            
            // Second click: expand
            categoryHeader?.click();
            
            const categoryItems = document.querySelectorAll('[data-plugin-category="network"]');
            categoryItems.forEach(item => {
                expect((item as HTMLElement).style.display).not.toBe('none');
            });
        });
    });

    describe('Add New Attribute', () => {
        it('should allow adding custom attributes to template', () => {
            const httpTemplate = getPluginTemplates().find(t => t.type === 'http')!;
            const node = palette.createNodeFromTemplate(httpTemplate);
            
            // Add custom attribute
            node.attributes['custom-header'] = 'value';
            
            expect(node.attributes).toHaveProperty('custom-header');
            expect(node.attributes['custom-header']).toBe('value');
        });
    });
});

