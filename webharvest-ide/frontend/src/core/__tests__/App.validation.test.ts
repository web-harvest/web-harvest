/**
 * TDD Tests for Validation Feature
 * Frontend validation integration
 */

import { App } from '../App';

// Mock Monaco
const mockMonaco = {
    editor: {
        create: jest.fn(() => ({
            getValue: jest.fn(() => '<config></config>'),
            setValue: jest.fn(),
            onDidChangeModelContent: jest.fn(),
            getModel: jest.fn(() => ({})),
            updateOptions: jest.fn(),
        })),
        defineTheme: jest.fn(),
        setTheme: jest.fn(),
    },
};

(global as any).monaco = mockMonaco;

describe('App - Validation (TDD)', () => {
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
        `;

        // Mock fetch for validation
        (global as any).fetch = jest.fn((url: string) => {
            if (url.includes('/api/plugins')) {
                return Promise.resolve({
                    ok: true,
                    json: () => Promise.resolve({ plugins: [] }),
                } as any);
            }
            return Promise.reject(new Error('Unknown URL'));
        });
    });

    describe('validateConfig()', () => {
        it('should call validation API', async () => {
            (global.fetch as jest.Mock).mockImplementation((url) => {
                if (url.includes('/api/config/validate')) {
                    return Promise.resolve({
                        ok: true,
                        json: () => Promise.resolve({
                            valid: true,
                            errors: [],
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
            const tab = (app as any).tabManager.getActiveTab();
            tab.content = '<config><def var="test">value</def></config>';

            await app.validateConfig();

            expect(global.fetch).toHaveBeenCalledWith(
                '/api/config/validate',
                expect.objectContaining({
                    method: 'POST',
                    headers: { 'Content-Type': 'application/xml' },
                })
            );
        });

        it('should display success message when valid', async () => {
            (global.fetch as jest.Mock).mockImplementation((url) => {
                if (url.includes('/api/config/validate')) {
                    return Promise.resolve({
                        ok: true,
                        json: () => Promise.resolve({
                            valid: true,
                            errors: [],
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
            await app.validateConfig();

            const logsDiv = document.getElementById('logs');
            expect(logsDiv?.textContent).toContain('valid');
        });

        it('should display ALL errors when invalid', async () => {
            (global.fetch as jest.Mock).mockImplementation((url) => {
                if (url.includes('/api/config/validate')) {
                    return Promise.resolve({
                        ok: true,
                        json: () => Promise.resolve({
                            valid: false,
                            errors: [
                                {
                                    line: 3,
                                    message: "Missing required attribute 'url'",
                                    type: 'MISSING_ATTRIBUTE',
                                    plugin: 'http',
                                },
                                {
                                    line: 4,
                                    message: "Missing required attribute 'expression'",
                                    type: 'MISSING_ATTRIBUTE',
                                    plugin: 'xpath',
                                },
                            ],
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
            await app.validateConfig();

            const logsDiv = document.getElementById('logs');
            const content = logsDiv?.textContent || '';

            // Should show BOTH errors
            expect(content).toContain('Line 3');
            expect(content).toContain('url');
            expect(content).toContain('Line 4');
            expect(content).toContain('expression');
        });
    });

    describe('runConfig() with validation', () => {
        it('should validate before running', async () => {
            (global.fetch as jest.Mock).mockImplementation((url) => {
                if (url.includes('/api/config/validate')) {
                    return Promise.resolve({
                        ok: true,
                        json: () => Promise.resolve({
                            valid: true,
                            errors: [],
                        }),
                    } as any);
                }
                if (url.includes('/api/execute')) {
                    return Promise.resolve({
                        ok: true,
                        json: () => Promise.resolve({
                            executionId: 'test-123',
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
            const tab = (app as any).tabManager.getActiveTab();
            tab.content = '<config><def var="test">value</def></config>';

            await app.runConfig();

            // Should have validated first
            expect(global.fetch).toHaveBeenCalledWith(
                '/api/config/validate',
                expect.anything()
            );
        });

        it('should NOT execute if validation fails', async () => {
            (global.fetch as jest.Mock).mockImplementation((url) => {
                if (url.includes('/api/config/validate')) {
                    return Promise.resolve({
                        ok: true,
                        json: () => Promise.resolve({
                            valid: false,
                            errors: [
                                {
                                    line: 3,
                                    message: "Invalid XML",
                                    type: 'SYNTAX_ERROR',
                                    plugin: null,
                                },
                            ],
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
            const tab = (app as any).tabManager.getActiveTab();
            tab.content = '<invalid>';

            await app.runConfig();

            // Should NOT have called /api/execute
            expect(global.fetch).not.toHaveBeenCalledWith(
                '/api/execute',
                expect.anything()
            );
        });

        it('should show validation errors in logs', async () => {
            (global.fetch as jest.Mock).mockImplementation((url) => {
                if (url.includes('/api/config/validate')) {
                    return Promise.resolve({
                        ok: true,
                        json: () => Promise.resolve({
                            valid: false,
                            errors: [
                                {
                                    line: 5,
                                    message: "Unknown plugin 'badplugin'",
                                    type: 'UNKNOWN_PLUGIN',
                                    plugin: 'badplugin',
                                },
                            ],
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
            await app.runConfig();

            const logsDiv = document.getElementById('logs');
            const content = logsDiv?.textContent || '';

            expect(content).toContain('Line 5');
            expect(content).toContain('badplugin');
        });
    });
});





