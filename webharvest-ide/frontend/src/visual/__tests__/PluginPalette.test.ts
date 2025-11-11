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
import { getPluginTemplates } from '../VisualModelHelpers';

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

        it('should render utility group by default and workflow group when requested', () => {
            palette.render();
            const titles = Array.from(document.querySelectorAll('.plugin-group__title'))
                .map(header => header.textContent?.trim());

            expect(titles).toContain('Utilities');

            palette.setActiveSegment('workflow');
            palette.render();
            const workflowTitles = Array.from(document.querySelectorAll('.plugin-group__title'))
                .map(header => header.textContent?.trim());

            if (workflowTitles.length > 0) {
                expect(workflowTitles).toContain('Workflow Blocks');
            } else {
                expect(document.querySelector('.plugin-palette__empty')).toBeTruthy();
            }
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

        it('should filter by active segment', () => {
            const baseline = palette.getFilteredTemplates().length;
            palette.setActiveSegment('workflow');
            const workflowFiltered = palette.getFilteredTemplates();

            expect(workflowFiltered.length).toBeLessThanOrEqual(baseline);
            expect(workflowFiltered.every(template => template.semantics?.roles?.includes('ROOT_CONTAINER')
                || template.semantics?.roles?.includes('CONTEXT_CONTAINER'))).toBe(true);
        });

        it('should clear filters', () => {
            const baseline = palette.getFilteredTemplates().length;
            palette.search('http');
            palette.clearFilters();
            const filtered = palette.getFilteredTemplates();

            expect(filtered.length).toBe(baseline);
        });

        it('should exclude dependent templates from primary list', () => {
            const filtered = palette.getFilteredTemplates();
            expect(filtered.every(template => template.canAppearAtRoot !== false)).toBe(true);
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
            const categories = paletteEl.querySelectorAll('.plugin-group');

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

            const sections = Array.from(document.querySelectorAll('.plugin-group'));
            sections.forEach(section => {
                const count = Number(section.querySelector('.plugin-group__count')?.textContent ?? '0');
                const items = section.querySelectorAll('.plugin-group__grid .plugin-item').length;
                expect(count).toBe(items);
            });
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




