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

        it('should find session panel with id="session"', () => {
            const sessionContainer = document.getElementById('session');
            expect(sessionContainer).toBeTruthy();
            
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

        it('should render session in correct container', () => {
            const app = new App();
            const sessionContainer = document.getElementById('session');
            
            // Set session info
            (app as any).sessionPanel.setSessionInfo({
                sessionId: 'test-123',
                status: 'COMPLETED',
                duration: 1000,
            });
            
            // Should render in #session, not #sessionContent
            expect(sessionContainer?.textContent).toContain('test-123');
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

