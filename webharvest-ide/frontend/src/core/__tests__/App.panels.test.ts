/**
 * TDD Tests for App panel initialization with correct IDs
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

const flushPromises = () => new Promise(resolve => setTimeout(resolve, 0));

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

describe('App - Panel Initialization (TDD)', () => {
    beforeEach(() => {
        // Setup DOM with CORRECT IDs (matching index.html)
        document.body.innerHTML = `
            <div id="editor"></div>
            <div id="visual-canvas"></div>
            <div id="plugin-sidebar"></div>
            <div id="canvas-area"></div>
            <div id="plugin-palette"></div>
            <div id="tabs"></div>
            <div id="tabs-container"></div>
            <div id="rightSidebar" class="sidebar-section"></div>
            <div class="sidebar-section">
                <div class="sidebar-nav">
                    <button class="sidebar-nav-btn" data-panel="properties" id="propertiesTabBtn" style="display: flex;">
                        <span>Properties</span>
                    </button>
                    <button class="sidebar-nav-btn" data-panel="run" id="runTabBtn">
                        <span>Run &amp; Test</span>
                    </button>
                    <button class="sidebar-nav-btn" data-panel="logs">
                        <span>Logs</span>
                        <span class="badge" id="logsBadge">0</span>
                    </button>
                    <button class="sidebar-nav-btn" data-panel="results">
                        <span>Results</span>
                    </button>
                    <button class="sidebar-nav-btn" data-panel="variables">
                        <span>Variables</span>
                        <span class="badge" id="varsBadge">0</span>
                    </button>
                </div>
                <div class="sidebar-content">
                    <div id="propertyPanel" class="sidebar-panel"></div>
                    <div id="runPanel" class="sidebar-panel">
                        <div class="panel-body panel-body--run" id="runPanelBody">
                            <div id="executionControls"></div>
                        </div>
                    </div>
                    <div id="logsPanel" class="sidebar-panel">
                        <div class="panel-body">
                            <div id="logs"></div>
                        </div>
                    </div>
                    <div id="resultsPanel" class="sidebar-panel">
                        <div class="panel-body">
                            <div id="results"></div>
                        </div>
                    </div>
                    <div id="variablesPanel" class="sidebar-panel">
                        <div class="panel-body">
                            <div id="variables"></div>
                        </div>
                    </div>
                </div>
            </div>
        `;

        (global as any).ResizeObserver = class {
            observe() { /* noop */ }
            unobserve() { /* noop */ }
            disconnect() { /* noop */ }
        };
    });

    describe('Sidebar visibility controls', () => {
        it('activates Properties panel by default in visual mode', async () => {
            const app = new App();
            await flushPromises();
            await flushPromises();

            const propertyPanel = document.getElementById('propertyPanel') as HTMLElement;
            const runPanel = document.getElementById('runPanel') as HTMLElement;
            const logsPanel = document.getElementById('logsPanel') as HTMLElement;

            expect(propertyPanel.style.display).toBe('flex');
            expect(propertyPanel.classList.contains('active')).toBe(true);
            expect(runPanel.style.display).toBe('none');
            expect(logsPanel.style.display).toBe('none');
        });

        it('switchSidebarPanel("logs") shows Logs panel and hides others', async () => {
            const app = new App();
            await flushPromises();
            await flushPromises();

            app.switchSidebarPanel('logs');

            const propertyPanel = document.getElementById('propertyPanel') as HTMLElement;
            const runPanel = document.getElementById('runPanel') as HTMLElement;
            const logsPanel = document.getElementById('logsPanel') as HTMLElement;

            expect(logsPanel.style.display).toBe('flex');
            expect(propertyPanel.style.display).toBe('none');
            expect(runPanel.style.display).toBe('none');
        });

        it('switching to XML mode hides Properties tab and focuses Run & Test', async () => {
            const app = new App();
            await flushPromises();
            await flushPromises();

            const confirmSpy = jest.spyOn(window, 'confirm').mockReturnValue(true);
            await app.switchMode('xml');
            await flushPromises();

            const propertiesTab = document.getElementById('propertiesTabBtn') as HTMLElement;
            const propertyPanel = document.getElementById('propertyPanel') as HTMLElement;
            const runPanel = document.getElementById('runPanel') as HTMLElement;

            expect(propertiesTab.style.display).toBe('none');
            expect(propertyPanel.style.display).toBe('none');
            expect(runPanel.style.display).toBe('flex');
            expect(runPanel.classList.contains('active')).toBe(true);

            confirmSpy.mockRestore();
        });
    });

    afterEach(() => {
        jest.clearAllMocks();
    });

    describe('Panel IDs must match HTML', () => {
        it('should find logs panel with id="logs"', () => {
            const logsContainer = document.getElementById('logs');
            expect(logsContainer).toBeTruthy();
            
            const app = new App();
            // Should not throw error
            expect(app).toBeDefined();
        });

        it('should find results panel with id="results"', () => {
            const resultsContainer = document.getElementById('results');
            expect(resultsContainer).toBeTruthy();
            
            const app = new App();
            expect(app).toBeDefined();
        });

        it('should find variables panel with id="variables"', () => {
            const variablesContainer = document.getElementById('variables');
            expect(variablesContainer).toBeTruthy();
            
            const app = new App();
            expect(app).toBeDefined();
        });

    });

    describe('Badge elements', () => {
        it('should find logsBadge element', () => {
            const badge = document.getElementById('logsBadge');
            expect(badge).toBeTruthy();
        });

        it('should find varsBadge element', () => {
            const badge = document.getElementById('varsBadge');
            expect(badge).toBeTruthy();
        });

        it('should update logsBadge when logs added', () => {
            const app = new App();
            const badge = document.getElementById('logsBadge');
            
            // Add logs via app's logsPanel
            (app as any).logsPanel.addLog('info', 'Test log 1');
            (app as any).logsPanel.addLog('info', 'Test log 2');
            (app as any).logsPanel.addLog('info', 'Test log 3');
            
            // Badge should be updated
            expect(badge?.textContent).toBe('3');
        });

        it('should update varsBadge when variables set', () => {
            const app = new App();
            const badge = document.getElementById('varsBadge');
            
            // Set variables via app's variablesPanel
            (app as any).variablesPanel.setVariables({
                var1: 'value1',
                var2: 'value2',
            });
            
            // Badge should be updated
            expect(badge?.textContent).toBe('2');
        });
    });

    describe('Panel rendering with correct containers', () => {
        it('should render results in correct container', () => {
            const app = new App();
            const resultsContainer = document.getElementById('results');
            
            // Set results
            (app as any).resultsPanel.setResults({
                configResult: 'test',
            });
            
            // Should render in #results, not #resultsContent
            expect(resultsContainer?.textContent).toContain('configResult');
        });

        it('should render variables in correct container', () => {
            const app = new App();
            const variablesContainer = document.getElementById('variables');
            
            // Set variables
            (app as any).variablesPanel.setVariables({
                testVar: 'testValue',
            });
            
            // Should render in #variables, not #variablesContent
            expect(variablesContainer?.textContent).toContain('testVar');
        });

        it('should render logs in correct container', () => {
            const app = new App();
            const logsContainer = document.getElementById('logs');
            
            // Add log
            (app as any).logsPanel.addLog('info', 'Test log message');
            
            // Should render in #logs, not #logsContent
            expect(logsContainer?.textContent).toContain('Test log message');
        });
    });
});

