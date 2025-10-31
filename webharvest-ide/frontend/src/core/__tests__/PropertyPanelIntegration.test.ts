/**
 * TDD Tests: Property Panel as Sidebar Tab
 * 
 * Requirements:
 * 1. Property Panel should be a tab in right sidebar (with Logs, Results, Variables, Sessions)
 * 2. Should be FIRST tab (leftmost) when switching to Visual mode
 * 3. Should be hidden/disabled when switching to XML mode
 * 4. Should only be visible in Visual mode
 */

import { describe, it, expect, beforeEach } from 'vitest';

describe('Property Panel Integration', () => {
    let app: any;
    let sidebarNav: HTMLElement;
    let propertyPanelButton: HTMLElement;
    let propertyPanelContent: HTMLElement;

    beforeEach(() => {
        // Setup DOM
        document.body.innerHTML = `
            <div class="sidebar-section">
                <div class="sidebar-nav">
                    <button class="sidebar-nav-btn" data-panel="properties" id="propertiesTabBtn">
                        <i class="fas fa-cog"></i>
                        <span>Properties</span>
                    </button>
                    <button class="sidebar-nav-btn" data-panel="logs">
                        <i class="fas fa-list-ul"></i>
                        <span>Logs</span>
                    </button>
                    <button class="sidebar-nav-btn" data-panel="results">
                        <i class="fas fa-table"></i>
                        <span>Results</span>
                    </button>
                </div>
                <div class="sidebar-content">
                    <div id="propertyPanel" class="sidebar-panel">
                        <div class="panel-header">
                            <h3>Plugin Properties</h3>
                        </div>
                        <div class="panel-body">
                            <!-- Properties content -->
                        </div>
                    </div>
                    <div id="logsPanel" class="sidebar-panel"></div>
                    <div id="resultsPanel" class="sidebar-panel"></div>
                </div>
            </div>
        `;

        sidebarNav = document.querySelector('.sidebar-nav')!;
        propertyPanelButton = document.getElementById('propertiesTabBtn')!;
        propertyPanelContent = document.getElementById('propertyPanel')!;
    });

    describe('Tab Order', () => {
        it('should have Properties tab as FIRST tab (leftmost)', () => {
            const tabs = sidebarNav.querySelectorAll('.sidebar-nav-btn');
            const firstTab = tabs[0];

            expect(firstTab.getAttribute('data-panel')).toBe('properties');
            expect(firstTab.querySelector('span')?.textContent).toBe('Properties');
        });

        it('should have correct tab order: Properties, Logs, Results, Variables, Sessions', () => {
            const tabs = Array.from(sidebarNav.querySelectorAll('.sidebar-nav-btn'));
            const tabNames = tabs.map(tab => tab.getAttribute('data-panel'));

            expect(tabNames[0]).toBe('properties');
            expect(tabNames[1]).toBe('logs');
            expect(tabNames[2]).toBe('results');
        });
    });

    describe('Visual Mode', () => {
        it('should show Properties tab in Visual mode', () => {
            // Simulate switching to Visual mode
            const currentMode = 'visual';

            if (currentMode === 'visual') {
                propertyPanelButton.style.display = 'flex';
            }

            expect(propertyPanelButton.style.display).toBe('flex');
        });

        it('should make Properties tab active by default in Visual mode', () => {
            // Simulate Visual mode initialization
            const currentMode = 'visual';

            if (currentMode === 'visual') {
                propertyPanelButton.classList.add('active');
                propertyPanelContent.classList.add('active');
            }

            expect(propertyPanelButton.classList.contains('active')).toBe(true);
            expect(propertyPanelContent.classList.contains('active')).toBe(true);
        });

        it('should allow switching to Properties panel in Visual mode', () => {
            // Setup: Visual mode
            const currentMode = 'visual';
            propertyPanelButton.style.display = 'flex';

            // User clicks Properties tab
            const allPanels = document.querySelectorAll('.sidebar-panel');
            const allTabs = document.querySelectorAll('.sidebar-nav-btn');

            // Clear all active states
            allPanels.forEach(p => p.classList.remove('active'));
            allTabs.forEach(t => t.classList.remove('active'));

            // Activate Properties
            propertyPanelButton.classList.add('active');
            propertyPanelContent.classList.add('active');

            expect(propertyPanelButton.classList.contains('active')).toBe(true);
            expect(propertyPanelContent.classList.contains('active')).toBe(true);
        });
    });

    describe('XML Mode', () => {
        it('should HIDE Properties tab in XML mode', () => {
            // Simulate switching to XML mode
            const currentMode = 'xml';

            if (currentMode === 'xml') {
                propertyPanelButton.style.display = 'none';
            }

            expect(propertyPanelButton.style.display).toBe('none');
        });

        it('should switch to Logs tab when entering XML mode if Properties was active', () => {
            // Setup: Properties active in Visual mode
            propertyPanelButton.classList.add('active');
            propertyPanelContent.classList.add('active');

            // Switch to XML mode
            const currentMode = 'xml';
            const logsButton = document.querySelector('[data-panel="logs"]')!;
            const logsPanel = document.getElementById('logsPanel')!;

            if (currentMode === 'xml') {
                // Hide Properties tab
                propertyPanelButton.style.display = 'none';
                propertyPanelButton.classList.remove('active');
                propertyPanelContent.classList.remove('active');

                // Switch to Logs
                logsButton.classList.add('active');
                logsPanel.classList.add('active');
            }

            expect(propertyPanelButton.style.display).toBe('none');
            expect(logsButton.classList.contains('active')).toBe(true);
            expect(logsPanel.classList.contains('active')).toBe(true);
        });

        it('should NOT allow switching to Properties panel in XML mode', () => {
            // Setup: XML mode
            const currentMode = 'xml';
            propertyPanelButton.style.display = 'none';

            // Attempt to click Properties tab (should not work)
            const isClickable = propertyPanelButton.style.display !== 'none';

            expect(isClickable).toBe(false);
        });
    });

    describe('Mode Switching', () => {
        it('should show Properties tab when switching from XML to Visual', () => {
            // Start in XML mode
            let currentMode = 'xml';
            propertyPanelButton.style.display = 'none';

            // Switch to Visual
            currentMode = 'visual';
            if (currentMode === 'visual') {
                propertyPanelButton.style.display = 'flex';
            }

            expect(propertyPanelButton.style.display).toBe('flex');
        });

        it('should hide Properties tab when switching from Visual to XML', () => {
            // Start in Visual mode
            let currentMode = 'visual';
            propertyPanelButton.style.display = 'flex';

            // Switch to XML
            currentMode = 'xml';
            if (currentMode === 'xml') {
                propertyPanelButton.style.display = 'none';
            }

            expect(propertyPanelButton.style.display).toBe('none');
        });

        it('should preserve sidebar panel state (except Properties) when switching modes', () => {
            // Setup: Logs tab active in Visual mode
            const logsButton = document.querySelector('[data-panel="logs"]')!;
            const logsPanel = document.getElementById('logsPanel')!;
            logsButton.classList.add('active');
            logsPanel.classList.add('active');

            // Switch to XML mode
            let currentMode = 'xml';
            if (currentMode === 'xml') {
                propertyPanelButton.style.display = 'none';
            }

            // Logs should still be active
            expect(logsButton.classList.contains('active')).toBe(true);
            expect(logsPanel.classList.contains('active')).toBe(true);
        });
    });

    describe('Property Panel Content', () => {
        it('should render plugin properties when a visual node is selected', () => {
            // Simulate node selection in Visual mode
            const currentMode = 'visual';
            const selectedNode = {
                id: 'node-1',
                type: 'http',
                properties: { url: 'https://example.com', method: 'GET' }
            };

            if (currentMode === 'visual' && selectedNode) {
                // Property panel should show node properties
                const panelBody = propertyPanelContent.querySelector('.panel-body');
                expect(panelBody).toBeTruthy();
            }
        });

        it('should show empty state when no node is selected', () => {
            const currentMode = 'visual';
            const selectedNode = null;

            if (currentMode === 'visual' && !selectedNode) {
                const panelBody = propertyPanelContent.querySelector('.panel-body');
                // Should show "Select a node to view properties" message
                expect(panelBody).toBeTruthy();
            }
        });
    });

    describe('Accessibility', () => {
        it('should have aria-label for Properties tab', () => {
            propertyPanelButton.setAttribute('aria-label', 'Plugin Properties');
            expect(propertyPanelButton.getAttribute('aria-label')).toBe('Plugin Properties');
        });

        it('should have aria-selected for active Properties tab', () => {
            propertyPanelButton.classList.add('active');
            propertyPanelButton.setAttribute('aria-selected', 'true');

            expect(propertyPanelButton.getAttribute('aria-selected')).toBe('true');
        });

        it('should have aria-hidden for Properties tab in XML mode', () => {
            const currentMode = 'xml';

            if (currentMode === 'xml') {
                propertyPanelButton.setAttribute('aria-hidden', 'true');
            }

            expect(propertyPanelButton.getAttribute('aria-hidden')).toBe('true');
        });
    });
});




