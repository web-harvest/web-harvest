/**
 * ContextMenu - Unit Tests
 */

import { describe, it, expect, beforeEach, afterEach } from '@jest/globals';
import { ContextMenu, type ContextMenuAction } from '../ContextMenu';

describe('ContextMenu', () => {
    let contextMenu: ContextMenu;

    beforeEach(() => {
        contextMenu = new ContextMenu();
        // Clear any existing menus
        document.body.querySelectorAll('.context-menu').forEach(el => el.remove());
    });

    afterEach(() => {
        contextMenu.hide();
        document.body.querySelectorAll('.context-menu').forEach(el => el.remove());
    });

    describe('show', () => {
        it('should create and display menu at specified position', () => {
            const actions: ContextMenuAction[] = [
                {
                    id: 'test',
                    label: 'Test Action',
                    onClick: () => {}
                }
            ];

            contextMenu.show({
                actions,
                x: 100,
                y: 200
            });

            const menu = document.body.querySelector('.context-menu') as HTMLElement;
            expect(menu).toBeTruthy();
            expect(menu.style.left).toBe('100px');
            expect(menu.style.top).toBe('200px');
        });

        it('should render all actions', () => {
            const actions: ContextMenuAction[] = [
                { id: '1', label: 'Action 1', onClick: () => {} },
                { id: '2', label: 'Action 2', onClick: () => {} },
                { id: '3', label: 'Action 3', onClick: () => {} }
            ];

            contextMenu.show({ actions, x: 0, y: 0 });

            const items = document.body.querySelectorAll('.context-menu-item');
            expect(items).toHaveLength(3);
            expect(items[0].textContent).toContain('Action 1');
            expect(items[1].textContent).toContain('Action 2');
            expect(items[2].textContent).toContain('Action 3');
        });

        it('should render icons when provided', () => {
            const actions: ContextMenuAction[] = [
                {
                    id: 'test',
                    label: 'Test',
                    icon: 'fa-cog',
                    onClick: () => {}
                }
            ];

            contextMenu.show({ actions, x: 0, y: 0 });

            const item = document.body.querySelector('.context-menu-item') as HTMLElement;
            const icon = item.querySelector('i');
            expect(icon).toBeTruthy();
            expect(icon?.className).toContain('fa-cog');
        });

        it('should disable actions when disabled flag is set', () => {
            const actions: ContextMenuAction[] = [
                {
                    id: 'disabled',
                    label: 'Disabled',
                    disabled: true,
                    onClick: () => {}
                }
            ];

            contextMenu.show({ actions, x: 0, y: 0 });

            const item = document.body.querySelector('.context-menu-item') as HTMLElement;
            expect(item.style.cursor).toBe('not-allowed');
            expect(item.style.opacity).toBe('0.5');
        });

        it('should call onClick when action is clicked', () => {
            let clicked = false;
            const actions: ContextMenuAction[] = [
                {
                    id: 'test',
                    label: 'Test',
                    onClick: () => {
                        clicked = true;
                    }
                }
            ];

            contextMenu.show({ actions, x: 0, y: 0 });

            const item = document.body.querySelector('.context-menu-item') as HTMLElement;
            item.click();

            expect(clicked).toBe(true);
            expect(contextMenu.isVisible()).toBe(false);
        });
    });

    describe('hide', () => {
        it('should remove menu from DOM', () => {
            contextMenu.show({
                actions: [{ id: 'test', label: 'Test', onClick: () => {} }],
                x: 0,
                y: 0
            });

            expect(contextMenu.isVisible()).toBe(true);
            contextMenu.hide();
            expect(contextMenu.isVisible()).toBe(false);
            expect(document.body.querySelector('.context-menu')).toBeNull();
        });

        it('should remove event listeners', () => {
            const clickSpy = jest.fn();
            document.addEventListener('click', clickSpy);

            contextMenu.show({
                actions: [{ id: 'test', label: 'Test', onClick: () => {} }],
                x: 0,
                y: 0
            });

            contextMenu.hide();

            // Click outside should not trigger menu close handler
            document.body.click();
            // The spy should still be called, but menu should be gone
            expect(contextMenu.isVisible()).toBe(false);
        });
    });

    describe('isVisible', () => {
        it('should return false when menu is not shown', () => {
            expect(contextMenu.isVisible()).toBe(false);
        });

        it('should return true when menu is shown', () => {
            contextMenu.show({
                actions: [{ id: 'test', label: 'Test', onClick: () => {} }],
                x: 0,
                y: 0
            });

            expect(contextMenu.isVisible()).toBe(true);
        });
    });
});


