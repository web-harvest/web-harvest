/**
 * RightSidebar Integration Tests
 * Tests RightSidebar section switching and panel integration
 */

import { describe, it, expect, beforeEach, afterEach } from '@jest/globals';
import { RightSidebar } from '../RightSidebar';

describe('RightSidebar Integration', () => {
    let sidebar: RightSidebar;
    let container: HTMLElement;

    beforeEach(() => {
        container = document.createElement('div');
        container.id = 'rightSidebar';
        document.body.appendChild(container);

        sidebar = new RightSidebar('rightSidebar');
    });

    afterEach(() => {
        if (container.parentNode) {
            document.body.removeChild(container);
        }
    });

    describe('section switching', () => {
        it('should switch to properties section', () => {
            sidebar.showSection('properties');
            expect(sidebar.getActiveSection()).toBe('properties');
        });

        it('should switch to execution section', () => {
            sidebar.showSection('execution');
            expect(sidebar.getActiveSection()).toBe('execution');
        });

        it('should switch to data section', () => {
            sidebar.showSection('data');
            expect(sidebar.getActiveSection()).toBe('data');
        });

        it('should switch to monitoring section', () => {
            sidebar.showSection('monitoring');
            expect(sidebar.getActiveSection()).toBe('monitoring');
        });

        it('should hide previous section when switching', () => {
            sidebar.showSection('properties');
            const propertiesSection = sidebar.getPropertiesContainer()?.closest('[data-section="properties"]') as HTMLElement;
            
            sidebar.showSection('execution');
            const executionSection = container.querySelector('[data-section="execution"]') as HTMLElement;
            
            expect(propertiesSection?.style.display).toBe('none');
            expect(executionSection?.style.display).toBe('flex');
        });
    });

    describe('container access', () => {
        it('should provide properties container', () => {
            const propsContainer = sidebar.getPropertiesContainer();
            expect(propsContainer).toBeDefined();
            expect(propsContainer?.id).toBe('propertyPanel');
        });

        it('should provide data section', () => {
            const dataSection = sidebar.getDataSection();
            expect(dataSection).toBeDefined();
            expect(dataSection?.dataset.section).toBe('data');
        });

        it('should provide monitoring section', () => {
            const monitoringSection = sidebar.getMonitoringSection();
            expect(monitoringSection).toBeDefined();
            expect(monitoringSection?.dataset.section).toBe('monitoring');
        });
    });

    describe('visibility control', () => {
        it('should show sidebar', () => {
            sidebar.show(true);
            expect(sidebar.isVisible()).toBe(true);
        });

        it('should hide sidebar', () => {
            sidebar.show(false);
            expect(sidebar.isVisible()).toBe(false);
        });
    });
});


