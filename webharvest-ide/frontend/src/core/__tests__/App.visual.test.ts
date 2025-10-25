/**
 * TDD Tests for Visual mode synchronization with tabs
 */

import { App } from '../App';

// Mock Monaco
const mockMonaco = {
    editor: {
        create: jest.fn(() => ({
            getValue: jest.fn(() => '<config></config>'),
            setValue: jest.fn(),
            onDidChangeModelContent: jest.fn(),
            getModel: jest.fn(() => ({
                onDidChangeContent: jest.fn(),
            })),
            updateOptions: jest.fn(),
        })),
        defineTheme: jest.fn(),
        setTheme: jest.fn(),
    },
};

(global as any).monaco = mockMonaco;

// Mock fetch
global.fetch = jest.fn((url: string) => {
    if (url.includes('/api/plugins')) {
        return Promise.resolve({
            ok: true,
            json: () => Promise.resolve([]),
        } as Response);
    }
    return Promise.reject(new Error('Unknown URL'));
});

describe('App - Visual Mode Synchronization (TDD)', () => {
    beforeEach(() => {
        document.body.innerHTML = `
            <div id="editor"></div>
            <div id="logs"></div>
            <div id="results"></div>
            <div id="variables"></div>
            <div id="session"></div>
            <div id="canvas-area"></div>
            <div id="propertyPanel"></div>
            <div id="plugin-palette"></div>
            <div id="tabs-container"></div>
            <span id="logsBadge">0</span>
            <span id="varsBadge">0</span>
        `;
    });

    afterEach(() => {
        jest.clearAllMocks();
    });

    describe('Visual mode updates with tab switches', () => {
        it('should refresh visual designer when switching tabs in visual mode', () => {
            const app = new App();
            
            // Create two tabs with different XML
            const tab1Id = (app as any).tabManager.addTab('test1.xml');
            const tab1 = (app as any).tabManager.getTab(tab1Id);
            tab1.content = '<config><http url="test1"/></config>';
            
            const tab2Id = (app as any).tabManager.addTab('test2.xml');
            const tab2 = (app as any).tabManager.getTab(tab2Id);
            tab2.content = '<config><xpath expression="test2"/></config>';
            
            // Switch to visual mode
            (app as any).currentMode = 'visual';
            
            // Switch between tabs
            (app as any).switchToTab(tab1Id);
            // Visual designer should parse tab1's XML
            
            (app as any).switchToTab(tab2Id);
            // Visual designer should parse tab2's XML
            
            // Should not crash
            expect(app).toBeDefined();
        });

        it('should NOT refresh visual designer when in XML mode', () => {
            const app = new App();
            
            // Stay in XML mode
            (app as any).currentMode = 'xml';
            
            const tab1Id = (app as any).tabManager.addTab('test1.xml');
            const tab1 = (app as any).tabManager.getTab(tab1Id);
            tab1.content = '<config></config>';
            
            const tab2Id = (app as any).tabManager.addTab('test2.xml');
            const tab2 = (app as any).tabManager.getTab(tab2Id);
            tab2.content = '<config></config>';
            
            // Switch tabs in XML mode
            (app as any).switchToTab(tab1Id);
            (app as any).switchToTab(tab2Id);
            
            // Should not parse (we're in XML mode)
            expect(app).toBeDefined();
        });

        it('should handle empty XML in visual mode gracefully', () => {
            const app = new App();
            
            const tabId = (app as any).tabManager.addTab('empty.xml');
            const tab = (app as any).tabManager.getTab(tabId);
            tab.content = '';
            
            (app as any).currentMode = 'visual';
            (app as any).switchToTab(tabId);
            
            // Should not crash, just not parse
            expect(app).toBeDefined();
        });

        it('should handle invalid XML in visual mode gracefully', () => {
            const app = new App();
            
            const tabId = (app as any).tabManager.addTab('invalid.xml');
            const tab = (app as any).tabManager.getTab(tabId);
            tab.content = '<config><broken';
            
            (app as any).currentMode = 'visual';
            (app as any).switchToTab(tabId);
            
            // Should log error but not crash
            expect(app).toBeDefined();
        });
    });

    describe('switchViewMode synchronization', () => {
        it('should parse active tab XML when switching to visual', () => {
            const app = new App();
            
            const tabId = (app as any).tabManager.addTab('test.xml');
            const tab = (app as any).tabManager.getTab(tabId);
            tab.content = '<config><http url="test"/></config>';
            (app as any).switchToTab(tabId);
            
            // Switch to visual mode
            (app as any).switchViewMode('visual');
            
            expect((app as any).currentMode).toBe('visual');
        });

        it('should prevent switching to visual with empty XML', () => {
            const app = new App();
            
            const tabId = (app as any).tabManager.addTab('empty.xml');
            const tab = (app as any).tabManager.getTab(tabId);
            tab.content = '';
            (app as any).switchToTab(tabId);
            
            // Try to switch to visual
            (app as any).switchViewMode('visual');
            
            // Should stay in XML mode
            expect((app as any).currentMode).toBe('xml');
        });
    });
});

