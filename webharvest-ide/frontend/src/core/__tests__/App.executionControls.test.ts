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
      <div class="sidebar-section">
        <div class="sidebar-nav">
          <button class="sidebar-nav-btn" data-panel="properties" id="propertiesTabBtn"></button>
          <button class="sidebar-nav-btn" data-panel="run" id="runTabBtn"></button>
          <button class="sidebar-nav-btn" data-panel="logs"></button>
          <button class="sidebar-nav-btn" data-panel="results"></button>
          <button class="sidebar-nav-btn" data-panel="variables"></button>
          <button class="sidebar-nav-btn" data-panel="session"></button>
        </div>
        <div class="sidebar-content">
          <div id="propertyPanel" class="sidebar-panel"></div>
          <div id="runPanel" class="sidebar-panel">
            <div class="panel-header panel-header--run">
              <h3>Run & Test</h3>
            </div>
            <div class="panel-body panel-body--run" id="runPanelBody">
              <div id="executionControls" class="run-panel-host">
                <button id="canvasRunBtn"></button>
                <button id="canvasValidateBtn"></button>
                <div id="executionActionButtons" style="display:none;">
                  <button id="pauseBtn"></button>
                  <button id="resumeBtn" style="display:none;"></button>
                  <button id="stopBtn"></button>
                </div>
              </div>
            </div>
          </div>
          <div id="logsPanel" class="sidebar-panel"><div id="logs"></div></div>
          <div id="resultsPanel" class="sidebar-panel"><div id="results"></div></div>
          <div id="variablesPanel" class="sidebar-panel"><div id="variables"></div></div>
          <div id="sessionPanel" class="sidebar-panel"><div id="session"></div></div>
        </div>
      </div>
      <span id="logsBadge">0</span>
      <span id="varsBadge">0</span>
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
        jest.spyOn((app as any).visualDesigner, 'getNodes').mockReturnValue([
            { id: 'node-1', type: 'http', attributes: {}, body: null, children: [] }
        ]);
        jest.spyOn((app as any).visualDesigner, 'generateXML').mockReturnValue('<config></config>');
        jest.spyOn((app as any).visualDesigner, 'getCurrentConfiguration').mockReturnValue({ filename: 'visual-workflow.xml' });
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

    it('mounts run panel into the sidebar column', () => {
        const host = document.getElementById('executionControls');
        expect(host?.classList.contains('run-panel-host')).toBe(true);
        expect(host?.parentElement?.id).toBe('runPanelBody');
        const runPanel = host?.closest('#runPanel');
        expect(runPanel).toBeTruthy();
        const navButton = document.querySelector('[data-panel="run"]');
        expect(navButton).toBeTruthy();
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

    it('records run history and updates status when execution completes', async () => {
        const payload = {
            inputJson: '{"foo":"bar"}',
            correlationId: 'corr-1',
            idempotencyKey: 'idem-1',
            taskDomainJson: '',
        };

        await (app as any).handleRunFromPanel(payload, 'workflow', null);
        await flushAll();

        const tab = (app as any).tabManager.getActiveTab();
        expect(tab?.runPanelState?.history?.length).toBe(1);
        const entryId = tab?.runPanelState?.history?.[0].id;
        expect(tab?.runPanelState?.history?.[0].scope).toBe('workflow');

        const wsManager = (app as any).wsManager;
        const updateCallbacks = wsManager.updateCallbacks;
        expect(updateCallbacks.length).toBeGreaterThan(0);

        jest.spyOn(wsManager, 'getExecutionId').mockReturnValue('exec-123');
        updateCallbacks[0]({
            status: 'COMPLETED',
            progress: 100,
            message: 'done',
        });

        const updatedEntry = tab?.runPanelState?.history?.[0];
        expect(updatedEntry?.status).toBe('COMPLETED');
        expect(updatedEntry?.executionId).toBe('exec-123');

        // ensure history mapping cleaned
        (app as any).executionHistoryIndex.delete('exec-123');
        expect(entryId).toBe(updatedEntry?.id);
    });

    it('restores fields and reruns a previous history entry', async () => {
        const payload = {
            inputJson: '{"alpha":1}',
            correlationId: 'corr-rerun',
            idempotencyKey: 'idem-rerun',
            taskDomainJson: '{"task_x":"domainA"}'
        };

        const runConfigSpy = jest.spyOn(app, 'runConfig').mockResolvedValue(undefined as any);

        await (app as any).handleRunFromPanel(payload, 'workflow', null);
        await flushAll();

        const tab = (app as any).tabManager.getActiveTab();
        const entryId = tab?.runPanelState?.history?.[0].id;
        expect(entryId).toBeTruthy();

        await (app as any).handleRunHistoryRerun(entryId);
        await flushAll();

        expect(runConfigSpy).toHaveBeenCalled();
        const lastCall = runConfigSpy.mock.calls[runConfigSpy.mock.calls.length - 1]?.[0];
        expect(lastCall?.payload).toEqual(payload);

        const historyLength = tab?.runPanelState?.history?.length ?? 0;
        expect(historyLength).toBeGreaterThan(1);

        runConfigSpy.mockRestore();
    });
});

