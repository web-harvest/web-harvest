/**
 * BreadcrumbNav - TDD Tests
 * Tests for breadcrumb navigation in nested containers
 */

import { describe, it, expect, beforeEach } from '@jest/globals';
import { BreadcrumbNav, BreadcrumbItem } from '../BreadcrumbNav';

describe('BreadcrumbNav', () => {
    let nav: BreadcrumbNav;
    let mockContainer: HTMLElement;

    beforeEach(() => {
        nav = new BreadcrumbNav();

        mockContainer = document.createElement('div');
        mockContainer.id = 'breadcrumb-container';
        document.body.appendChild(mockContainer);
    });

    afterEach(() => {
        document.body.removeChild(mockContainer);
    });

    describe('render', () => {
        it('should render breadcrumb trail', () => {
            const trail: BreadcrumbItem[] = [
                { id: 'root', label: 'Root', level: 0 },
                { id: 'loop-1', label: 'Loop #1', level: 1 },
                { id: 'if-1', label: 'If Condition', level: 2 }
            ];

            nav.render(mockContainer, trail);

            const items = mockContainer.querySelectorAll('.breadcrumb-item');
            expect(items.length).toBe(3);
        });

        it('should show separators between items', () => {
            const trail: BreadcrumbItem[] = [
                { id: 'root', label: 'Root', level: 0 },
                { id: 'loop-1', label: 'Loop #1', level: 1 }
            ];

            nav.render(mockContainer, trail);

            const separators = mockContainer.querySelectorAll('.breadcrumb-separator');
            expect(separators.length).toBe(1); // n-1 separators
        });

        it('should highlight current level', () => {
            const trail: BreadcrumbItem[] = [
                { id: 'root', label: 'Root', level: 0 },
                { id: 'loop-1', label: 'Loop #1', level: 1 }
            ];

            nav.render(mockContainer, trail);

            const items = mockContainer.querySelectorAll('.breadcrumb-item');
            const lastItem = items[items.length - 1];
            expect(lastItem.classList.contains('breadcrumb-current')).toBe(true);
        });
    });

    describe('navigate', () => {
        it('should navigate to clicked breadcrumb', (done) => {
            const trail: BreadcrumbItem[] = [
                { id: 'root', label: 'Root', level: 0 },
                { id: 'loop-1', label: 'Loop #1', level: 1 },
                { id: 'if-1', label: 'If Condition', level: 2 }
            ];

            nav.onNavigate((item) => {
                expect(item.id).toBe('loop-1');
                expect(item.level).toBe(1);
                done();
            });

            nav.render(mockContainer, trail);

            // Simulate click on second item
            nav.navigateTo('loop-1');
        });

        it('should emit navigation event with correct level', (done) => {
            nav.render(mockContainer, [{ id: 'root', label: 'Root', level: 0 }]);

            nav.onNavigate((item) => {
                expect(item.level).toBe(0);
                done();
            });

            nav.navigateTo('root');
        });
    });

    describe('getCurrentLevel', () => {
        it('should return current navigation level', () => {
            const trail: BreadcrumbItem[] = [
                { id: 'root', label: 'Root', level: 0 },
                { id: 'loop-1', label: 'Loop #1', level: 1 }
            ];

            nav.render(mockContainer, trail);

            expect(nav.getCurrentLevel()).toBe(1);
        });

        it('should return 0 for root level', () => {
            const trail: BreadcrumbItem[] = [
                { id: 'root', label: 'Root', level: 0 }
            ];

            nav.render(mockContainer, trail);

            expect(nav.getCurrentLevel()).toBe(0);
        });
    });

    describe('push', () => {
        it('should add new level to breadcrumb', () => {
            const trail: BreadcrumbItem[] = [
                { id: 'root', label: 'Root', level: 0 }
            ];

            nav.render(mockContainer, trail);
            nav.push({ id: 'loop-1', label: 'Loop #1', level: 1 });

            const items = mockContainer.querySelectorAll('.breadcrumb-item');
            expect(items.length).toBe(2);
        });
    });

    describe('pop', () => {
        it('should remove last breadcrumb level', () => {
            const trail: BreadcrumbItem[] = [
                { id: 'root', label: 'Root', level: 0 },
                { id: 'loop-1', label: 'Loop #1', level: 1 }
            ];

            nav.render(mockContainer, trail);

            const popped = nav.pop();

            expect(popped?.id).toBe('loop-1');

            const items = mockContainer.querySelectorAll('.breadcrumb-item');
            expect(items.length).toBe(1);
        });

        it('should not pop root level', () => {
            const trail: BreadcrumbItem[] = [
                { id: 'root', label: 'Root', level: 0 }
            ];

            nav.render(mockContainer, trail);

            const popped = nav.pop();

            expect(popped).toBeNull();
        });
    });

    describe('reset', () => {
        it('should reset to root level', () => {
            const trail: BreadcrumbItem[] = [
                { id: 'root', label: 'Root', level: 0 },
                { id: 'loop-1', label: 'Loop #1', level: 1 },
                { id: 'if-1', label: 'If Condition', level: 2 }
            ];

            nav.render(mockContainer, trail);
            nav.reset();

            expect(nav.getCurrentLevel()).toBe(0);

            const items = mockContainer.querySelectorAll('.breadcrumb-item');
            expect(items.length).toBe(1);
        });
    });

    describe('getTrail', () => {
        it('should return current breadcrumb trail', () => {
            const trail: BreadcrumbItem[] = [
                { id: 'root', label: 'Root', level: 0 },
                { id: 'loop-1', label: 'Loop #1', level: 1 }
            ];

            nav.render(mockContainer, trail);

            const currentTrail = nav.getTrail();
            expect(currentTrail.length).toBe(2);
            expect(currentTrail[1].id).toBe('loop-1');
        });
    });

    describe('keyboard navigation', () => {
        it('should navigate back on Escape key', (done) => {
            const trail: BreadcrumbItem[] = [
                { id: 'root', label: 'Root', level: 0 },
                { id: 'loop-1', label: 'Loop #1', level: 1 }
            ];

            nav.render(mockContainer, trail);

            nav.onNavigate((item) => {
                expect(item.id).toBe('root');
                done();
            });

            nav.handleKeyboard({ key: 'Escape' });
        });
    });
});


