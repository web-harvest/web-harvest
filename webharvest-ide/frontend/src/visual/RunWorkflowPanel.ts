import type {
    RunHistoryEntry,
    RunHistoryScope,
    RunHistoryStatus,
    RunPanelPayload,
    RunPanelState,
} from '../types';

interface SelectedNodeContext {
    id: string;
    label: string;
}

interface RunWorkflowPanelCallbacks {
    onRun: (payload: RunPanelPayload, scope: RunHistoryScope, node?: SelectedNodeContext | null) => Promise<void>;
    onValidate: () => void;
    onStateChange: (state: RunPanelState) => void;
    onRestore: (entryId: string) => void;
    onRerun: (entryId: string) => void;
    onClearHistory: () => void;
}

const DEFAULT_STATE: RunPanelState = {
    inputJson: '',
    correlationId: '',
    idempotencyKey: '',
    taskDomainJson: '',
    history: [],
};

const escapeHtml = (value: string): string =>
    value
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;')
        .replace(/'/g, '&#39;');

const formatJsonForDisplay = (value: string | undefined | null): string => {
    if (!value || value.trim().length === 0) {
        return '—';
    }
    try {
        const parsed = JSON.parse(value);
        return JSON.stringify(parsed, null, 2);
    } catch (_error) {
        return value;
    }
};

export class RunWorkflowPanel {
    private container: HTMLElement;
    private callbacks: RunWorkflowPanelCallbacks;
    private state: RunPanelState = { ...DEFAULT_STATE, history: [] };
    private selectedNode: SelectedNodeContext | null = null;
    private historySelectionContext: SelectedNodeContext | null = null;
    private currentScope: RunHistoryScope = 'workflow';
    private suppressStateEvents = false;
    private executionState: 'idle' | 'running' | 'paused' = 'idle';

    private inputJsonEl!: HTMLTextAreaElement;
    private correlationEl!: HTMLInputElement;
    private idempotencyEl!: HTMLInputElement;
    private taskDomainEl!: HTMLTextAreaElement;
    private errorEl!: HTMLDivElement;
    private historyListEl!: HTMLDivElement;
    private runBtn!: HTMLButtonElement;
    private validateBtn!: HTMLButtonElement;
    private scopeWorkflowBtn!: HTMLButtonElement;
    private scopeTaskBtn!: HTMLButtonElement;

    constructor(containerId: string, callbacks: RunWorkflowPanelCallbacks) {
        const container = document.getElementById(containerId);
        if (!container) {
            throw new Error(`RunWorkflowPanel: container #${containerId} not found`);
        }

        this.container = container;
        this.callbacks = callbacks;
        this.render();
        this.bindElements();
        this.attachEventListeners();
        this.applyScopeToUI();
    }

    syncState(state: RunPanelState | undefined | null): void {
        const nextState: RunPanelState = state
            ? {
                  inputJson: state.inputJson ?? '',
                  correlationId: state.correlationId ?? '',
                  idempotencyKey: state.idempotencyKey ?? '',
                  taskDomainJson: state.taskDomainJson ?? '',
                  history: Array.isArray(state.history) ? state.history.slice(0, 50) : [],
              }
            : { ...DEFAULT_STATE };

        this.suppressStateEvents = true;
        this.state = nextState;
        this.inputJsonEl.value = nextState.inputJson ?? '';
        this.correlationEl.value = nextState.correlationId ?? '';
        this.idempotencyEl.value = nextState.idempotencyKey ?? '';
        this.taskDomainEl.value = nextState.taskDomainJson ?? '';
        this.renderHistory(nextState.history);
        this.clearError();
        this.suppressStateEvents = false;
    }

    updateSelection(node: SelectedNodeContext | null): void {
        this.selectedNode = node;
        this.historySelectionContext = null;
        if (!node) {
            // revert to workflow scope if task scope was active
            if (this.currentScope === 'task') {
                this.currentScope = 'workflow';
                this.applyScopeToUI();
            }
            this.scopeTaskBtn.disabled = true;
            this.scopeTaskBtn.title = 'Select a node to enable single-task runs';
        } else {
            this.scopeTaskBtn.disabled = false;
            this.scopeTaskBtn.title = `Run selected task (${node.label})`;
            if (this.currentScope === 'task') {
                this.scopeTaskBtn.setAttribute('aria-pressed', 'true');
            }
        }
    }

    setExecutionState(state: 'idle' | 'running' | 'paused'): void {
        this.executionState = state;
        const isRunning = state === 'running' || state === 'paused';
        this.runBtn.disabled = isRunning;
        this.runBtn.style.opacity = isRunning ? '0.5' : '1';
        this.runBtn.style.cursor = isRunning ? 'not-allowed' : 'pointer';
        this.runBtn.title = isRunning ? 'Execution in progress…' : 'Run configuration (⌘R)';

        this.validateBtn.disabled = isRunning;
        this.validateBtn.style.opacity = isRunning ? '0.5' : '1';
        this.validateBtn.style.cursor = isRunning ? 'not-allowed' : 'pointer';
        this.validateBtn.title = isRunning ? 'Validation disabled during execution' : 'Validate configuration (⌘K)';
    }

    setScope(scope: RunHistoryScope, context?: SelectedNodeContext | null): void {
        this.currentScope = scope;
        this.historySelectionContext = scope === 'task' ? context ?? null : null;
        this.applyScopeToUI();
    }

    updateHistoryEntry(entry: RunHistoryEntry): void {
        const idx = this.state.history.findIndex(item => item.id === entry.id);
        if (idx !== -1) {
            this.state.history[idx] = entry;
        } else {
            this.state.history.unshift(entry);
        }
        this.renderHistory(this.state.history);
    }

    showError(message: string): void {
        this.errorEl.textContent = message;
        this.errorEl.hidden = false;
    }

    clearError(): void {
        this.errorEl.textContent = '';
        this.errorEl.hidden = true;
    }

    private render(): void {
        this.container.classList.add('run-panel-host');
        this.container.innerHTML = `
            <div class="run-panel">
                <div class="run-panel__stack">
                    <div class="run-panel__head">
                        <div class="run-panel__title">
                            <i class="fas fa-bolt"></i>
                            <span>Run & Test</span>
                        </div>
                        <div class="run-panel__scope" role="group" aria-label="Run scope">
                            <button type="button" class="run-panel__scope-btn" data-scope="workflow">Workflow</button>
                            <button type="button" class="run-panel__scope-btn" data-scope="task" disabled>Selected Task</button>
                        </div>
                    </div>
                    <div class="run-panel__error run-panel__error--hidden" id="runPanelError"></div>
                    <div class="run-panel__hint">
                        Run immediately with the defaults, or adjust payload and advanced settings before executing.
                    </div>
                    <div class="run-panel__field">
                        <label for="runPanelInputJson">Workflow input (JSON)</label>
                        <textarea id="runPanelInputJson" class="run-panel__textarea" spellcheck="false" rows="6" placeholder="{ }"></textarea>
                        <div class="run-panel__field-actions">
                            <button type="button" class="run-panel__mini-btn" id="runPanelFormatBtn">
                                <i class="fas fa-wand-magic-sparkles"></i> Format
                            </button>
                            <button type="button" class="run-panel__mini-btn" id="runPanelClearInputBtn">
                                <i class="fas fa-eraser"></i> Clear
                            </button>
                        </div>
                    </div>
                    <details class="run-panel__advanced" id="runPanelAdvanced">
                        <summary>
                            <i class="fas fa-sliders-h" aria-hidden="true"></i>
                            <span>Advanced options</span>
                        </summary>
                        <div class="run-panel__advanced-body">
                            <div class="run-panel__field-group">
                                <div class="run-panel__field">
                                    <label for="runPanelCorrelationId">Correlation ID</label>
                                    <input id="runPanelCorrelationId" type="text" placeholder="Optional" />
                                </div>
                                <div class="run-panel__field">
                                    <label for="runPanelIdempotencyKey">Idempotency Key</label>
                                    <input id="runPanelIdempotencyKey" type="text" placeholder="Optional" />
                                </div>
                            </div>
                            <div class="run-panel__field">
                                <label for="runPanelTaskDomain">Task → Domain map (JSON)</label>
                                <textarea id="runPanelTaskDomain" class="run-panel__textarea" spellcheck="false" rows="3" placeholder="{ }"></textarea>
                            </div>
                        </div>
                    </details>
                    <div class="run-panel__controls">
                        <button id="canvasRunBtn" class="run-panel__btn run-panel__btn--primary">
                            <i class="fas fa-play"></i> Run Workflow
                        </button>
                        <button id="canvasValidateBtn" class="run-panel__btn run-panel__btn--secondary">
                            <i class="fas fa-check"></i> Validate
                        </button>
                        <div id="executionActionButtons" class="run-panel__execution-actions" style="display: none;">
                            <button id="pauseBtn" class="run-panel__btn run-panel__btn--ghost" title="Pause">
                                <i class="fas fa-pause"></i>
                            </button>
                            <button id="resumeBtn" class="run-panel__btn run-panel__btn--ghost" title="Resume" style="display: none;">
                                <i class="fas fa-play"></i>
                            </button>
                            <button id="runPanelStopBtn" class="run-panel__btn run-panel__btn--ghost" title="Stop">
                                <i class="fas fa-stop"></i>
                            </button>
                        </div>
                    </div>
                </div>
                <div class="run-panel__history">
                    <div class="run-panel__history-header">
                        <span>History</span>
                        <button type="button" class="run-panel__mini-btn" id="runPanelHistoryClearBtn">
                            <i class="fas fa-trash"></i> Clear
                        </button>
                    </div>
                    <div id="runPanelHistoryList" class="run-panel__history-list"></div>
                </div>
            </div>
        `;
    }

    private bindElements(): void {
        this.inputJsonEl = this.container.querySelector('#runPanelInputJson') as HTMLTextAreaElement;
        this.correlationEl = this.container.querySelector('#runPanelCorrelationId') as HTMLInputElement;
        this.idempotencyEl = this.container.querySelector('#runPanelIdempotencyKey') as HTMLInputElement;
        this.taskDomainEl = this.container.querySelector('#runPanelTaskDomain') as HTMLTextAreaElement;
        this.errorEl = this.container.querySelector('#runPanelError') as HTMLDivElement;
        this.historyListEl = this.container.querySelector('#runPanelHistoryList') as HTMLDivElement;
        this.runBtn = this.container.querySelector('#canvasRunBtn') as HTMLButtonElement;
        this.validateBtn = this.container.querySelector('#canvasValidateBtn') as HTMLButtonElement;
        this.scopeWorkflowBtn = this.container.querySelector('[data-scope="workflow"]') as HTMLButtonElement;
        this.scopeTaskBtn = this.container.querySelector('[data-scope="task"]') as HTMLButtonElement;

        const stopBtn = this.container.querySelector('#runPanelStopBtn') as HTMLButtonElement | null;
        if (stopBtn) {
            stopBtn.addEventListener('click', () => {
                (window as any).app?.stopExecution?.();
            });
        }
    }

    private attachEventListeners(): void {
        this.inputJsonEl.addEventListener('input', () => this.handleInputChange('inputJson', this.inputJsonEl.value));
        this.correlationEl.addEventListener('input', () => this.handleInputChange('correlationId', this.correlationEl.value));
        this.idempotencyEl.addEventListener('input', () => this.handleInputChange('idempotencyKey', this.idempotencyEl.value));
        this.taskDomainEl.addEventListener('input', () => this.handleInputChange('taskDomainJson', this.taskDomainEl.value));

        const formatBtn = this.container.querySelector('#runPanelFormatBtn') as HTMLButtonElement;
        formatBtn.addEventListener('click', () => this.handleFormatJson());

        const clearInputBtn = this.container.querySelector('#runPanelClearInputBtn') as HTMLButtonElement;
        clearInputBtn.addEventListener('click', () => this.handleClearInput());

        const clearHistoryBtn = this.container.querySelector('#runPanelHistoryClearBtn') as HTMLButtonElement;
        clearHistoryBtn.addEventListener('click', () => this.callbacks.onClearHistory());

        this.runBtn.addEventListener('click', () => this.handleRun());
        this.validateBtn.addEventListener('click', () => this.callbacks.onValidate());

        this.scopeWorkflowBtn.addEventListener('click', () => {
            this.currentScope = 'workflow';
            this.applyScopeToUI();
        });
        this.scopeTaskBtn.addEventListener('click', () => {
            if (this.scopeTaskBtn.disabled) {
                return;
            }
            this.currentScope = 'task';
            this.applyScopeToUI();
        });
    }

    private handleInputChange<K extends keyof RunPanelPayload>(key: K, value: string): void {
        if (this.suppressStateEvents) {
            return;
        }

        this.state = {
            ...this.state,
            [key]: value,
        };

        this.callbacks.onStateChange({ ...this.state });
    }

    private handleFormatJson(): void {
        try {
            const parsed = this.inputJsonEl.value.trim() ? JSON.parse(this.inputJsonEl.value) : {};
            const pretty = JSON.stringify(parsed, null, 2);
            this.inputJsonEl.value = pretty;
            this.handleInputChange('inputJson', pretty);
            this.clearError();
        } catch (error) {
            this.showError('Input JSON is invalid and cannot be formatted.');
        }
    }

    private handleClearInput(): void {
        this.inputJsonEl.value = '';
        this.handleInputChange('inputJson', '');
        this.clearError();
    }

    private async handleRun(): Promise<void> {
        if (this.executionState === 'running' || this.executionState === 'paused') {
            return;
        }

        const payload = this.collectPayload();
        if (!payload) {
            return;
        }

        if (this.historySelectionContext) {
            this.historySelectionContext = null;
        }

        const scope = this.currentScope === 'task' && this.selectedNode ? 'task' : 'workflow';
        try {
            await this.callbacks.onRun(payload, scope, scope === 'task' ? this.selectedNode : null);
            this.clearError();
        } catch (error) {
            console.error('[RunWorkflowPanel] onRun failed', error);
            this.showError((error as Error).message ?? 'Failed to start execution.');
        }
    }

    private collectPayload(): RunPanelPayload | null {
        this.clearError();
        try {
            if (this.inputJsonEl.value.trim().length > 0) {
                JSON.parse(this.inputJsonEl.value);
            }
        } catch (error) {
            this.showError('Workflow input must be valid JSON.');
            return null;
        }

        if (this.taskDomainEl.value.trim().length > 0) {
            try {
                JSON.parse(this.taskDomainEl.value);
            } catch (error) {
                this.showError('Task → Domain map must be valid JSON.');
                return null;
            }
        }

        return {
            inputJson: this.inputJsonEl.value,
            correlationId: this.correlationEl.value || undefined,
            idempotencyKey: this.idempotencyEl.value || undefined,
            taskDomainJson: this.taskDomainEl.value,
        };
    }

    private renderHistory(history: RunHistoryEntry[]): void {
        if (history.length === 0) {
            this.historyListEl.innerHTML = `
                <div class="run-panel__history-empty">
                    <i class="fas fa-clock"></i>
                    <span>No runs yet. Start a workflow to populate history.</span>
                </div>
            `;
            return;
        }

        const items = history
            .slice(0, 15)
            .map(entry => this.renderHistoryEntry(entry))
            .join('');

        this.historyListEl.innerHTML = items;
        this.historyListEl.querySelectorAll('[data-action="restore"]').forEach(btn => {
            btn.addEventListener('click', event => {
                const target = event.currentTarget as HTMLElement;
                const entryId = target.dataset.entryId;
                if (entryId) {
                    this.callbacks.onRestore(entryId);
                }
            });
        });
        this.historyListEl.querySelectorAll('[data-action="rerun"]').forEach(btn => {
            btn.addEventListener('click', event => {
                const target = event.currentTarget as HTMLElement;
                const entryId = target.dataset.entryId;
                if (entryId) {
                    this.callbacks.onRerun(entryId);
                }
            });
        });
        this.historyListEl.querySelectorAll('[data-action="details"]').forEach(btn => {
            btn.addEventListener('click', event => {
                const target = event.currentTarget as HTMLElement;
                this.toggleHistoryDetails(target);
            });
        });
    }

    private renderHistoryEntry(entry: RunHistoryEntry): string {
        const formattedTime = new Date(entry.createdAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
        const scopeLabel =
            entry.scope === 'task'
                ? `Task${entry.nodeLabel ? ` • ${escapeHtml(entry.nodeLabel)}` : ''}`
                : 'Workflow';
        const statusChip = entry.status
            ? `<span class="run-panel__history-status run-panel__history-status--${entry.status.toLowerCase()}">${entry.status}</span>`
            : '';
        const detailsId = `run-history-details-${entry.id}`;
        const correlation = entry.payload.correlationId ? escapeHtml(entry.payload.correlationId) : '—';
        const idempotency = entry.payload.idempotencyKey ? escapeHtml(entry.payload.idempotencyKey) : '—';
        const executionId = entry.executionId ? escapeHtml(entry.executionId) : '—';
        const taskDomain = formatJsonForDisplay(entry.payload.taskDomainJson ?? '');
        const inputJson = formatJsonForDisplay(entry.payload.inputJson ?? '');

        const taskDomainSection =
            taskDomain.trim() === '—'
                ? ''
                : `
                <div class="run-panel__history-json">
                    <label>Task → Domain map</label>
                    <pre>${escapeHtml(taskDomain)}</pre>
                </div>
            `;

        return `
            <div class="run-panel__history-item" data-entry-id="${escapeHtml(entry.id)}">
                <div class="run-panel__history-top">
                    <div class="run-panel__history-meta">
                        <span class="run-panel__history-time">${formattedTime}</span>
                        <span class="run-panel__history-scope">${scopeLabel}</span>
                        ${statusChip}
                    </div>
                    <div class="run-panel__history-actions">
                        <button type="button" class="run-panel__mini-btn" data-action="restore" data-entry-id="${escapeHtml(entry.id)}">
                            <i class="fas fa-rotate-left"></i> Restore
                        </button>
                        <button type="button" class="run-panel__mini-btn" data-action="rerun" data-entry-id="${escapeHtml(entry.id)}">
                            <i class="fas fa-play-circle"></i> Run
                        </button>
                        <button type="button" class="run-panel__mini-btn" data-action="details" data-entry-id="${escapeHtml(entry.id)}" aria-expanded="false" aria-controls="${escapeHtml(detailsId)}">
                            <i class="fas fa-eye"></i> Details
                        </button>
                    </div>
                </div>
                <div class="run-panel__history-details" id="${escapeHtml(detailsId)}" hidden>
                    <dl class="run-panel__history-detail-grid">
                        <div class="run-panel__history-detail">
                            <dt>Correlation ID</dt>
                            <dd>${correlation}</dd>
                        </div>
                        <div class="run-panel__history-detail">
                            <dt>Idempotency Key</dt>
                            <dd>${idempotency}</dd>
                        </div>
                        <div class="run-panel__history-detail">
                            <dt>Execution ID</dt>
                            <dd>${executionId}</dd>
                        </div>
                        ${
                            entry.scope === 'task' && entry.nodeId
                                ? `<div class="run-panel__history-detail">
                                        <dt>Task Node</dt>
                                        <dd>${escapeHtml(entry.nodeLabel ?? entry.nodeId)}</dd>
                                   </div>`
                                : ''
                        }
                    </dl>
                    <div class="run-panel__history-json">
                        <label>Workflow input</label>
                        <pre>${escapeHtml(inputJson)}</pre>
                    </div>
                    ${taskDomainSection}
                </div>
            </div>
        `;
    }

    private toggleHistoryDetails(button: HTMLElement): void {
        const item = button.closest('.run-panel__history-item') as HTMLElement | null;
        if (!item) {
            return;
        }
        const details = item.querySelector('.run-panel__history-details') as HTMLDivElement | null;
        if (!details) {
            return;
        }
        const isHidden = details.hasAttribute('hidden');
        if (isHidden) {
            details.removeAttribute('hidden');
            button.setAttribute('aria-expanded', 'true');
            button.innerHTML = `<i class="fas fa-eye-slash"></i> Hide`;
        } else {
            details.setAttribute('hidden', '');
            button.setAttribute('aria-expanded', 'false');
            button.innerHTML = `<i class="fas fa-eye"></i> Details`;
        }
    }

    private applyScopeToUI(): void {
        const buttons = [this.scopeWorkflowBtn, this.scopeTaskBtn];
        buttons.forEach(btn => btn.classList.remove('active'));

        const taskContext = this.selectedNode ?? this.historySelectionContext;
        const hasTaskContext = Boolean(taskContext);

        this.scopeTaskBtn.disabled = !hasTaskContext;
        this.scopeTaskBtn.title = hasTaskContext
            ? `Run selected task (${taskContext?.label ?? taskContext?.id})`
            : 'Select a node to enable single-task runs';

        if (!hasTaskContext && this.currentScope === 'task') {
            this.currentScope = 'workflow';
        }

        if (this.currentScope === 'task') {
            this.scopeTaskBtn.classList.add('active');
            const label = taskContext?.label ?? 'Selected Task';
            this.runBtn.innerHTML = `<i class="fas fa-vial"></i> Run ${escapeHtml(label)}`;
        } else {
            this.scopeWorkflowBtn.classList.add('active');
            this.runBtn.innerHTML = `<i class="fas fa-play"></i> Run Workflow`;
        }
    }
}

export type { RunWorkflowPanelCallbacks, SelectedNodeContext, RunHistoryEntry, RunHistoryStatus };

