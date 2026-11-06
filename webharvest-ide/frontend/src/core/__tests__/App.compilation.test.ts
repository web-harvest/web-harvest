/**
 * Compilation and Type Safety Tests
 * These tests should catch TypeScript errors that slip through
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
            json: () => Promise.resolve({ plugins: [], count: 0, version: '2.2.0' }),
        } as any);
    }
    if (url.includes('/api/config/validate')) {
        return Promise.resolve({
            ok: true,
            json: () => Promise.resolve({ valid: true }),
        } as any);
    }
    if (url.includes('/api/execute')) {
        return Promise.resolve({
            ok: true,
            json: () => Promise.resolve({ executionId: 'test-exec' }),
        } as any);
    }
    return Promise.resolve({
        ok: true,
        json: () => Promise.resolve({}),
    } as any);
});

describe('App - Compilation & Type Safety', () => {
    beforeEach(() => {
        document.body.innerHTML = `
            <div id="editor"></div>
            <div id="logs"></div>
            <div id="results"></div>
            <div id="variables"></div>
            <div id="canvas-area"></div>
            <div id="plugin-sidebar"></div>
            <div id="plugin-palette"></div>
        <div id="rightSidebar"></div>
            <div class="sidebar-section">
                <div class="sidebar-nav">
                    <button class="sidebar-nav-btn" data-panel="properties" id="propertiesTabBtn"></button>
                    <button class="sidebar-nav-btn" data-panel="run" id="runTabBtn"></button>
                    <button class="sidebar-nav-btn" data-panel="logs"></button>
                    <button class="sidebar-nav-btn" data-panel="results"></button>
                    <button class="sidebar-nav-btn" data-panel="variables"></button>
                </div>
                <div class="sidebar-content">
                    <div id="propertyPanel" class="sidebar-panel"></div>
                    <div id="runPanel" class="sidebar-panel">
                        <div class="panel-header panel-header--run"><h3>Run & Test</h3></div>
                        <div class="panel-body panel-body--run" id="runPanelBody">
                            <div id="executionControls" class="run-panel-host"></div>
                        </div>
                    </div>
                    <div id="logsPanel" class="sidebar-panel"></div>
                    <div id="resultsPanel" class="sidebar-panel"></div>
                    <div id="variablesPanel" class="sidebar-panel"></div>
                </div>
            </div>
            <div id="tabs-container"></div>
            <span id="logsBadge">0</span>
            <span id="varsBadge">0</span>
        `;
    });

    describe('All public methods exist and are callable', () => {
        it('should have toggleVisualMode method', () => {
            const app = new App();
            expect(typeof app.toggleVisualMode).toBe('function');

            // Should not throw
            expect(() => app.toggleVisualMode()).not.toThrow();
        });

        it('should have runExecution method that accepts string', async () => {
            const app = new App();
            expect(typeof app.runExecution).toBe('function');

            // Should accept XML string
            await expect(app.runExecution('<config></config>')).resolves.not.toThrow();
        });

        it('should have runConfig method with NO arguments', async () => {
            const app = new App();
            expect(typeof app.runConfig).toBe('function');

            // Should NOT require arguments
            await expect(app.runConfig()).resolves.not.toThrow();
        });

        it('should have validateConfig method', async () => {
            const app = new App();
            expect(typeof app.validateConfig).toBe('function');

            await expect(app.validateConfig()).resolves.not.toThrow();
        });

        it('should have openFile method', () => {
            const app = new App();
            expect(typeof app.openFile).toBe('function');
        });

        it('should have saveFile method', () => {
            const app = new App();
            expect(typeof app.saveFile).toBe('function');
        });

        it('should have showExamplesModal method', async () => {
            const app = new App();
            expect(typeof app.showExamplesModal).toBe('function');

            await expect(app.showExamplesModal()).resolves.not.toThrow();
        });

        it('should have closeTab method with 1 argument (tabId)', () => {
            const app = new App();
            expect(typeof app.closeTab).toBe('function');

            // closeTab should accept just tabId
            const tabId = (app as any).tabManager.addTab('test.xml');
            expect(() => app.closeTab(tabId)).not.toThrow();
        });

        it('should have switchToTab method', () => {
            const app = new App();
            expect(typeof app.switchToTab).toBe('function');

            const tabId = (app as any).tabManager.addTab('test.xml');
            expect(() => app.switchToTab(tabId)).not.toThrow();
        });

        it('should have addTab method', () => {
            const app = new App();
            expect(typeof app.addTab).toBe('function');

            expect(() => app.addTab()).not.toThrow();
        });
    });

    describe('window.app exposure', () => {
        it('should expose all methods to window after init', () => {
            const app = new App();

            // Simulate what main.ts does
            (window as any).app = {
                toggleVisualMode: () => app.toggleVisualMode(),
                runConfig: () => app.runConfig(),
                validateConfig: () => app.validateConfig(),
                openFile: () => app.openFile(),
                saveFile: () => app.saveFile(),
                showExamplesModal: () => app.showExamplesModal(),
                loadExampleDirect: (event: Event) => app.loadExampleDirect(event),
                addTab: () => app.addTab(),
                closeTab: (tabId: string) => app.closeTab(tabId),
                switchToTab: (tabId: string) => app.switchToTab(tabId),
                switchSidebarPanel: (panel: string) => app.switchSidebarPanel(panel),
                clearLogs: () => app.clearLogs(),
                exportResults: () => app.exportResults(),
                pauseExecution: () => app.pauseExecution(),
                resumeExecution: () => app.resumeExecution(),
                stopExecution: () => app.stopExecution(),
                hideTooltip: () => app.hideTooltip(),
            };

            // All should be functions
            expect(typeof (window as any).app.toggleVisualMode).toBe('function');
            expect(typeof (window as any).app.runConfig).toBe('function');
            expect(typeof (window as any).app.closeTab).toBe('function');
        });
    });

    describe('Validation before execution', () => {
        beforeEach(() => {
            (global.fetch as jest.Mock).mockReset();
            (global.fetch as jest.Mock).mockImplementation((url: string, options?: any) => {
                if (url.includes('/api/plugins')) {
                    return Promise.resolve({
                        ok: true,
                        json: () => Promise.resolve({ plugins: [], count: 0, version: '2.2.0' })
                    } as any);
                }
                if (url === '/api/config/validate') {
                    return Promise.resolve({
                        ok: true,
                        json: () => Promise.resolve({ valid: true })
                    } as any);
                }
                if (url === '/api/execute') {
                    return Promise.resolve({
                        ok: true,
                        json: () => Promise.resolve({ executionId: 'exec-1' })
                    } as any);
                }
                return Promise.resolve({
                    ok: true,
                    json: () => Promise.resolve({})
                } as any);
            });
        });

        it('should validate before running', async () => {
            const app = new App();
            await app.switchMode('xml');
            const tabId = (app as any).tabManager.addTab('config.xml');
            const tab = (app as any).tabManager.getAllTabs().find((t: any) => t.id === tabId);

            if (tab) {
                tab.content = '<config><def var="test">value</def></config>';
            }

            // runConfig should validate first
            await app.runConfig();

            // Should have called validation API
            const fetchMock = global.fetch as jest.Mock;
            const validateCall = fetchMock.mock.calls.find(([url]) => url === '/api/config/validate');
            expect(validateCall).toBeDefined();
            expect(validateCall?.[1]).toEqual(expect.objectContaining({ method: 'POST' }));
            const executeCall = fetchMock.mock.calls.find(([url]) => url === '/api/execute');
            expect(executeCall).toBeDefined();
        });

        it('should NOT run if validation fails', async () => {
            (global.fetch as jest.Mock).mockImplementation((url: string) => {
                if (url.includes('/api/config/validate')) {
                    return Promise.resolve({
                        ok: true,
                        json: () => Promise.resolve({
                            valid: false,
                            message: 'Invalid XML'
                        }),
                    } as any);
                }
                if (url.includes('/api/plugins')) {
                    return Promise.resolve({
                        ok: true,
                        json: () => Promise.resolve({ plugins: [] }),
                    } as any);
                }
                return Promise.reject(new Error('Unknown'));
            });

            const app = new App();
            await app.switchMode('xml');
            const tabId = (app as any).tabManager.addTab('config.xml');
            const tab = (app as any).tabManager.getAllTabs().find((t: any) => t.id === tabId);
            if (tab) {
                tab.content = '<invalid>';
            }

            await app.runConfig();

            // Should NOT have called /api/execute
            const fetchMock = global.fetch as jest.Mock;
            const executeCall = fetchMock.mock.calls.find(([requestUrl]) => requestUrl === '/api/execute');
            expect(executeCall).toBeUndefined();
        });
    });
});





