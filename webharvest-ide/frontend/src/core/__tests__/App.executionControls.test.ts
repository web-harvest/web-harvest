import { beforeEach, describe, expect, it, jest } from '@jest/globals';
import { App } from '../App';

const flushAll = async (): Promise<void> => {
    await Promise.resolve();
    await Promise.resolve();
};

describe('App execution control integration', () => {
    let app: App;

    beforeEach(async () => {
        // Minimal DOM required by App and panels
        document.body.innerHTML = `
      <div id="tabs-container"></div>
      <div id="tabs"></div>
      <div id="visual-toolbar">
        <span id="current-filename">untitled.xml</span>
        <span id="dirty-indicator"></span>
      </div>
      <div id="editor"></div>
      <div id="visual-canvas"></div>
      <div id="canvas-area"></div>
      <div id="plugin-sidebar"></div>
      <div id="plugin-palette"></div>
      <div id="propertyPanel"></div>
      <div id="logs"></div>
      <div id="results"></div>
      <div id="variables"></div>
      <div id="session"></div>
      <span id="logsBadge">0</span>
      <span id="varsBadge">0</span>
      <div id="executionControls" style="display:flex;">
        <button id="canvasRunBtn"></button>
        <button id="canvasValidateBtn"></button>
        <div id="executionActionButtons" style="display:none;">
          <button id="pauseBtn"></button>
          <button id="resumeBtn" style="display:none;"></button>
          <button id="stopBtn"></button>
        </div>
      </div>
    `;

        // Mock Monaco editor used during initialization
        (global as any).monaco = {
            editor: {
                create: jest.fn().mockReturnValue({
                    getValue: jest.fn().mockReturnValue(''),
                    setValue: jest.fn(),
                    onDidChangeModelContent: jest.fn(),
                    getModel: jest.fn().mockReturnValue({ onDidChangeContent: jest.fn() }),
                    updateOptions: jest.fn(),
                })
            }
        };

        // Mock fetch for metadata, validation, and execution endpoints
        (global as any).fetch = jest.fn((url: string) => {
            if (url.includes('/api/plugins/metadata')) {
                return Promise.resolve({
                    ok: true,
                    json: async () => ({ plugins: [], version: 'test' })
                }) as Response;
            }

            if (url.includes('/api/config/validate')) {
                return Promise.resolve({
                    ok: true,
                    json: async () => ({ valid: true })
                }) as Response;
            }

            if (url.includes('/api/execute')) {
                return Promise.resolve({
                    ok: true,
                    json: async () => ({ executionId: 'exec-123' })
                }) as Response;
            }

            return Promise.resolve({
                ok: true,
                json: async () => ({})
            }) as Response;
        });

        app = new App();
        await flushAll();

        // Stub heavy operations we do not exercise here
        jest.spyOn((app as any).visualDesigner, 'getAllNodes').mockReturnValue([]);
        jest.spyOn((app as any).visualDesigner, 'clearExecutionStates').mockImplementation(() => { });
        jest.spyOn((app as any).visualDesigner, 'setWorkflowStatus').mockImplementation(() => { });
        jest.spyOn((app as any).executionStateManager, 'registerNodes').mockImplementation(() => { });
        jest.spyOn((app as any).wsManager, 'connect').mockResolvedValue(undefined);
        jest.spyOn((app as any).wsManager, 'getExecutionId').mockReturnValue('exec-123');
        jest.spyOn((app as any).wsManager, 'send').mockImplementation(() => { });
        jest.spyOn((app as any).logsPanel, 'clear').mockImplementation(() => { });
        jest.spyOn((app as any).resultsPanel, 'clear').mockImplementation(() => { });
        jest.spyOn((app as any).variablesPanel, 'clear').mockImplementation(() => { });
        jest.spyOn((app as any).logsPanel, 'addLog').mockImplementation(() => { });
    });

    it('enables execution controls during successful run', async () => {
        const uiManager = (app as any).uiManager;
        const setExecutionStateSpy = jest.spyOn(uiManager, 'setExecutionState');
        const showActionsSpy = jest.spyOn(uiManager, 'showExecutionActionButtons');

        await app.runExecution('<config></config>');

        expect(setExecutionStateSpy).toHaveBeenCalledWith('running');
        expect(showActionsSpy).toHaveBeenCalledWith(true);
        expect((document.getElementById('canvasRunBtn') as HTMLButtonElement).disabled).toBe(true);
    });

    it('resets execution controls when stop is invoked', async () => {
        const uiManager = (app as any).uiManager;
        jest.spyOn(uiManager, 'setExecutionState');
        jest.spyOn(uiManager, 'showExecutionActionButtons');

        await app.runExecution('<config></config>');
        app.stopExecution();

        const runBtn = document.getElementById('canvasRunBtn') as HTMLButtonElement;
        const resumeBtn = document.getElementById('resumeBtn') as HTMLButtonElement;

        expect(runBtn.disabled).toBe(false);
        expect(resumeBtn.style.display).toBe('none');
    });

    it('returns controls to idle state when execution fails to start', async () => {
        const uiManager = (app as any).uiManager;
        const setExecutionStateSpy = jest.spyOn(uiManager, 'setExecutionState');
        const showActionsSpy = jest.spyOn(uiManager, 'showExecutionActionButtons');

        // Force execute call to fail
        (global as any).fetch = jest.fn((url: string) => {
            if (url.includes('/api/plugins/metadata')) {
                return Promise.resolve({ ok: true, json: async () => ({ plugins: [], version: 'test' }) }) as Response;
            }
            if (url.includes('/api/execute')) {
                return Promise.resolve({ ok: false, json: async () => ({ error: 'boom' }) }) as Response;
            }
            return Promise.resolve({ ok: true, json: async () => ({}) }) as Response;
        });

        await app.runExecution('<config></config>');

        const lastState = setExecutionStateSpy.mock.calls[setExecutionStateSpy.mock.calls.length - 1][0];
        expect(lastState).toBe('idle');
        expect(showActionsSpy).toHaveBeenCalledWith(false);
        expect((document.getElementById('canvasRunBtn') as HTMLButtonElement).disabled).toBe(false);
    });
});

