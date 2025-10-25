/**
 * App - Main Application
 * Wires all components together
 */

import { TabManager } from './TabManager';
import { WebSocketManager } from './WebSocketManager';
import { MonacoEditor } from './MonacoEditor';
import { UIManager } from './UIManager';
import { PluginManager } from './PluginManager';
import { ExamplesManager } from './ExamplesManager';
import { LogsPanel } from '../panels/LogsPanel';
import { ResultsPanel } from '../panels/ResultsPanel';
import { VariablesPanel } from '../panels/VariablesPanel';
import { SessionPanel } from '../panels/SessionPanel';
import { VisualDesigner } from '../visual/VisualDesigner';
import { PropertyPanel } from '../visual/PropertyPanel';
import { XMLParser } from '../visual/XMLParser';
import { XMLGenerator } from '../visual/XMLGenerator';
import type { ViewMode } from '../types';

export class App {
    private tabManager: TabManager;
    private wsManager: WebSocketManager;
    private uiManager: UIManager;
    private pluginManager: PluginManager;
    private examplesManager: ExamplesManager;
    private editor!: MonacoEditor;
    private logsPanel: LogsPanel;
    private resultsPanel: ResultsPanel;
    private variablesPanel: VariablesPanel;
    private sessionPanel: SessionPanel;
    private visualDesigner: VisualDesigner;
    private propertyPanel: PropertyPanel;
    private xmlParser: XMLParser;
    private xmlGenerator: XMLGenerator;
    private currentMode: ViewMode = 'xml';

    constructor() {
        console.log('🚀 Initializing WebHarvest IDE (TypeScript + Apple HIG)...');

        // Initialize managers
        this.tabManager = new TabManager();
        this.wsManager = new WebSocketManager();
        this.uiManager = new UIManager();
        this.pluginManager = new PluginManager();
        this.examplesManager = new ExamplesManager();
        this.editor = new MonacoEditor('editor');
        this.logsPanel = new LogsPanel('logs');
        this.resultsPanel = new ResultsPanel('results');
        this.variablesPanel = new VariablesPanel('variables');
        this.sessionPanel = new SessionPanel('session');
        this.visualDesigner = new VisualDesigner('canvas-area');
        this.propertyPanel = new PropertyPanel('propertyPanel');
        this.xmlParser = new XMLParser();
        this.xmlGenerator = new XMLGenerator();

        // Initialize
        this.initialize();
    }

    /**
     * Initialize application
     */
    private async initialize(): Promise<void> {
        try {
            // 1. Initialize Monaco Editor
            await this.initializeEditor();

            // 2. Load plugins
            await this.loadPlugins();

            // 3. Setup event handlers
            this.setupEventHandlers();

            // 4. Setup drag & drop
            this.setupDragAndDrop();

            // 5. Setup modal handlers
            this.setupModalHandlers();

            // 6. Create initial tab
            const initialTab = this.tabManager.addTab('untitled.xml');
            this.renderTabs();
            this.switchToTab(initialTab.id);

            // 7. Set initial mode
            this.uiManager.switchToXMLMode();

            console.log('✅ WebHarvest IDE initialized successfully!');
        } catch (error) {
            console.error('❌ Initialization failed:', error);
        }
    }

    /**
     * Initialize Monaco Editor
     */
    private async initializeEditor(): Promise<void> {
        try {
            await this.editor.initialize();

            // Editor change handler
            this.editor.onChange((value) => {
                const tab = this.tabManager.getActiveTab();
                if (tab) {
                    this.tabManager.updateTabContent(tab.id, value);
                }
            });

            console.log('✅ Monaco Editor ready');
        } catch (error) {
            console.error('❌ Failed to initialize Monaco:', error);
            throw error;
        }
    }

    /**
     * Load plugins
     */
    private async loadPlugins(): Promise<void> {
        try {
            const plugins = await this.pluginManager.loadPlugins();
            this.uiManager.renderPluginPalette(plugins);
            console.log('✅ Plugin palette rendered');
        } catch (error) {
            console.error('❌ Failed to load plugins:', error);
        }
    }

    /**
     * Setup drag and drop
     */
    private setupDragAndDrop(): void {
        const canvasArea = document.getElementById('canvas-area');
        if (!canvasArea) return;

        // Drag start
        document.addEventListener('dragstart', (e) => {
            const target = e.target as HTMLElement;
            if (target.classList.contains('plugin-item')) {
                const pluginId = target.dataset.pluginId;
                if (pluginId && e.dataTransfer) {
                    e.dataTransfer.setData('plugin-id', pluginId);
                    e.dataTransfer.effectAllowed = 'copy';
                }
            }
        });

        // Drag over
        canvasArea.addEventListener('dragover', (e) => {
            e.preventDefault();
            if (e.dataTransfer) {
                e.dataTransfer.dropEffect = 'copy';
            }
        });

        // Drop
        canvasArea.addEventListener('drop', (e) => {
            e.preventDefault();
            const pluginId = e.dataTransfer?.getData('plugin-id');
            if (pluginId) {
                this.handlePluginDrop(pluginId, e.clientX, e.clientY);
            }
        });

        console.log('✅ Drag & drop configured');
    }

    /**
     * Handle plugin drop on canvas
     */
    private handlePluginDrop(pluginId: string, x: number, y: number): void {
        const plugin = this.pluginManager.findPlugin(pluginId);
        if (!plugin) return;

        // Create new node
        const node = {
            id: `node-${Date.now()}`,
            type: pluginId,
            attributes: {},
            body: null,
            children: [],
            position: { x, y },
        };

        this.visualDesigner.addNode(node);
        this.logsPanel.addLog('info', `Added: ${plugin.name}`);
    }

    /**
     * Setup modal handlers
     */
    private setupModalHandlers(): void {
        // Examples modal click handlers
        document.addEventListener('click', async (e) => {
            const target = e.target as HTMLElement;

            // Example card click
            if (target.closest('.example-card')) {
                const card = target.closest('.example-card') as HTMLElement;
                const exampleFile = card.dataset.example;
                if (exampleFile) {
                    await this.loadExample(exampleFile);
                }
            }

            // Modal close click
            if (target.id === 'examplesModal' || target.closest('.modal-close')) {
                this.uiManager.hideExamplesModal();
            }
        });

        console.log('✅ Modal handlers configured');
    }

    /**
     * Setup all event handlers
     */
    private setupEventHandlers(): void {
        // WebSocket updates (matches old app.js logic)
        this.wsManager.onUpdate((update) => {
            console.log('=== Execution update received ===');
            console.log('Status:', update.status);
            console.log('Message:', update.message);
            console.log('Progress:', update.progress);
            console.log('Results:', update.results);
            console.log('Full update object:', update);

            // Add log message if present
            if (update.message) {
                this.logsPanel.addLog('info', update.message);
            }

            // Handle COMPLETED status
            if (update.status === 'COMPLETED') {
                console.log('COMPLETED! Processing results...');

                // Get active tab to store data (like old app.js)
                const tab = this.tabManager.getActiveTab();

                // Results = Variables in WebHarvest
                if (update.results && Object.keys(update.results).length > 0) {
                    console.log('Displaying', Object.keys(update.results).length, 'results');
                    try {
                        // Store in tab (like old app.js)
                        if (tab) {
                            tab.results = update.results;
                        }

                        // Update both Results and Variables panels (they're the same data)
                        this.resultsPanel.setResults(update.results);
                        this.variablesPanel.setVariables(update.results);

                        // Switch to Results panel to show results (like old app.js)
                        this.switchSidebarPanel('results');
                    } catch (error) {
                        console.error('Failed to set results:', error);
                    }
                } else {
                    console.warn('No results in update!');
                }

                // Session info and token usage - STORE IN TAB (like old app.js)
                if (tab) {
                    if (update.sessionInfo) {
                        console.log('Storing session info in tab:', update.sessionInfo);
                        tab.sessionInfo = update.sessionInfo;
                    }
                    if (update.tokenUsage) {
                        console.log('Storing token usage in tab:', update.tokenUsage);
                        tab.tokenUsage = update.tokenUsage;
                    }

                    // Render session panel with tab data
                    this.sessionPanel.setSessionInfo(update.sessionInfo || null);
                    this.sessionPanel.setTokenUsage(update.tokenUsage || null);
                }
            }

            // Legacy: handle separate variables field
            if (update.variables) {
                this.variablesPanel.setVariables(update.variables);
            }
        });

        // Visual Designer → Property Panel
        this.visualDesigner.addEventListener('nodeSelected', (nodeId: string) => {
            const node = this.visualDesigner.findNode(nodeId);
            if (node) {
                this.propertyPanel.showNode(node);
            }
        });

        // Property Panel → Visual Designer
        this.propertyPanel.onChange((updatedNode) => {
            this.visualDesigner.updateNode(updatedNode.id, updatedNode);
        });

        console.log('✅ Event handlers configured');
    }

    /**
     * Switch view mode (XML ↔ Visual)
     */
    switchViewMode(mode: ViewMode): void {
        if (mode === this.currentMode) {
            return;
        }

        const tab = this.tabManager.getActiveTab();
        if (!tab) {
            return;
        }

        if (mode === 'visual') {
            // XML → Visual
            // Check if content is empty
            if (!tab.content || tab.content.trim() === '') {
                this.logsPanel.addLog('warning', 'Load an example or create XML first');
                return;
            }

            try {
                const nodes = this.xmlParser.parse(tab.content);
                this.visualDesigner.loadNodes(nodes);
                console.log('✅ Switched to Visual mode');
            } catch (error) {
                console.error('Failed to parse XML:', error);
                this.logsPanel.addLog('error', `Failed to parse XML: ${error}`);
                return;
            }
        } else {
            // Visual → XML
            try {
                const nodes = this.visualDesigner.getNodes();
                const xml = '<?xml version="1.0" encoding="UTF-8"?>\n<config>\n' +
                    this.xmlGenerator.generate(nodes, 1) +
                    '\n</config>';
                this.tabManager.updateTabContent(tab.id, xml);
                console.log('✅ Switched to XML mode');
            } catch (error) {
                console.error('Failed to generate XML:', error);
                this.logsPanel.addLog('error', `Failed to generate XML: ${error}`);
                return;
            }
        }

        this.currentMode = mode;
    }

    /**
     * Run execution (FULL IMPLEMENTATION - Correct JSON format)
     */
    async runExecution(xml: string): Promise<void> {
        this.logsPanel.clear();
        this.resultsPanel.clear();
        this.variablesPanel.clear();
        this.logsPanel.addLog('info', '🚀 Starting execution...');

        try {
            const tab = this.tabManager.getActiveTab();
            const configName = tab?.name || 'config.xml';

            // Call API with JSON payload (backend expects {"configXml": "...", "configName": "..."})
            const response = await fetch('/api/execute', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({
                    configXml: xml,
                    configName: configName,
                }),
            });

            if (!response.ok) {
                const error = await response.json();
                throw new Error(error.error || `HTTP ${response.status}`);
            }

            const data = await response.json();

            if (!data.executionId) {
                throw new Error('No execution ID returned');
            }

            this.logsPanel.addLog('success', `✅ Execution started: ${data.executionId}`);

            // Connect WebSocket for real-time updates
            await this.wsManager.connect(data.executionId);
            this.logsPanel.addLog('info', '🔌 Connected to execution stream');

        } catch (error) {
            console.error('❌ Failed to start execution:', error);
            this.logsPanel.addLog('error', `❌ Failed to start: ${error}`);
        }
    }

    /**
     * Get tab manager (for external access)
     */
    getTabManager(): TabManager {
        return this.tabManager;
    }

    /**
     * Get visual designer (for external access)
     */
    getVisualDesigner(): VisualDesigner {
        return this.visualDesigner;
    }

    /**
     * Get Monaco editor
     */
    getEditor(): MonacoEditor {
        return this.editor;
    }

    /**
     * Open file
     */
    openFile(): void {
        const input = document.createElement('input');
        input.type = 'file';
        input.accept = '.xml';
        input.onchange = (e) => {
            const file = (e.target as HTMLInputElement).files?.[0];
            if (file) {
                const reader = new FileReader();
                reader.onload = (e) => {
                    const content = e.target?.result as string;
                    const tab = this.tabManager.addTab(file.name);
                    this.tabManager.updateTabContent(tab.id, content);
                    this.editor.setValue(content);
                    this.logsPanel.addLog('success', `Loaded: ${file.name}`);
                };
                reader.readAsText(file);
            }
        };
        input.click();
    }

    /**
     * Save file
     */
    saveFile(): void {
        const tab = this.tabManager.getActiveTab();
        if (!tab) return;

        const blob = new Blob([tab.content], { type: 'text/xml' });
        const url = URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = tab.name;
        a.click();
        URL.revokeObjectURL(url);

        this.tabManager.markClean(tab.id);
        this.logsPanel.addLog('success', `Saved: ${tab.name}`);
    }

    /**
     * Toggle Visual Mode
     */
    toggleVisualMode(): void {
        const newMode = this.currentMode === 'xml' ? 'visual' : 'xml';

        // Switch data model
        this.switchViewMode(newMode);

        // Switch UI
        if (newMode === 'visual') {
            this.uiManager.switchToVisualMode();
        } else {
            this.uiManager.switchToXMLMode();
        }

        this.currentMode = newMode;
    }

    /**
     * Show examples modal
     */
    async showExamplesModal(): Promise<void> {
        this.uiManager.showExamplesModal();

        try {
            const examples = await this.examplesManager.loadExamples();
            this.uiManager.renderExamplesList(examples);
        } catch (error) {
            console.error('Failed to load examples:', error);
            this.logsPanel.addLog('error', 'Failed to load examples');
        }
    }

    /**
     * Load example
     */
    private async loadExample(filename: string): Promise<void> {
        try {
            const content = await this.examplesManager.loadExampleContent(filename);
            const tab = this.tabManager.addTab(filename);
            this.tabManager.updateTabContent(tab.id, content);
            this.editor.setValue(content);
            this.uiManager.updateFilename(filename);
            this.uiManager.hideExamplesModal();
            this.logsPanel.addLog('success', `Loaded: ${filename}`);
        } catch (error) {
            console.error('Failed to load example:', error);
            this.logsPanel.addLog('error', `Failed to load: ${filename}`);
        }
    }

    /**
     * Run configuration (with validation)
     */
    async runConfig(): Promise<void> {
        const tab = this.tabManager.getActiveTab();
        if (!tab) return;

        // Validate first
        this.logsPanel.addLog('info', '🔍 Validating configuration...');

        try {
            const validationResponse = await fetch('/api/config/validate', {
                method: 'POST',
                headers: { 'Content-Type': 'application/xml' },
                body: tab.content,
            });

            const validationResult = await validationResponse.json();

            if (!validationResult.valid) {
                this.logsPanel.addLog('error', '❌ Validation failed: ' + (validationResult.message || 'Unknown error'));
                this.logsPanel.addLog('error', '⚠️ Fix errors before running');
                return;
            }

            this.logsPanel.addLog('success', '✓ Configuration valid');
        } catch (error) {
            console.warn('Validation skipped:', error);
            // Continue anyway if validation service fails
        }

        await this.runExecution(tab.content);
    }

    /**
     * Validate configuration (explicit validation - shows ALL errors)
     */
    async validateConfig(): Promise<void> {
        const tab = this.tabManager.getActiveTab();
        if (!tab) return;

        this.logsPanel.addLog('info', '🔍 Validating configuration...');

        try {
            const response = await fetch('/api/config/validate', {
                method: 'POST',
                headers: { 'Content-Type': 'application/xml' },
                body: tab.content,
            });

            const data = await response.json();

            if (data.valid) {
                this.logsPanel.addLog('success', '✅ Configuration is valid!');
            } else {
                this.logsPanel.addLog('error', `❌ Found ${data.errors?.length || 0} validation error(s):`);

                // Display ALL errors
                if (data.errors && Array.isArray(data.errors)) {
                    data.errors.forEach((err: any) => {
                        const lineInfo = err.line ? `Line ${err.line}` : 'Unknown line';
                        const pluginInfo = err.plugin ? ` [${err.plugin}]` : '';
                        this.logsPanel.addLog('error', `   • ${lineInfo}${pluginInfo}: ${err.message}`);
                    });
                }

                this.logsPanel.addLog('warning', '⚠️ Fix errors before running');
            }
        } catch (error) {
            console.error('Validation error:', error);
            this.logsPanel.addLog('error', '❌ Validation service error: ' + (error as Error).message);
        }
    }

    /**
     * Render tabs in UI
     */
    private renderTabs(): void {
        const tabsContainer = document.getElementById('tabs');
        if (!tabsContainer) return;

        const tabs = this.tabManager.getAllTabs();
        const activeTab = this.tabManager.getActiveTab();

        let html = '';
        tabs.forEach(tab => {
            const isActive = tab.id === activeTab?.id;
            const dirtyIndicator = tab.isDirty ? '●' : '';

            html += `
                <div class="tab ${isActive ? 'active' : ''}" data-tab-id="${tab.id}">
                    <span class="tab-name" onclick="window.app.switchToTab('${tab.id}')">${tab.name} ${dirtyIndicator}</span>
                    <button class="tab-close" onclick="window.app.closeTab('${tab.id}')" title="Close">×</button>
                </div>
            `;
        });

        tabsContainer.innerHTML = html;
    }

    /**
     * Switch to tab (matches old app.js - updates ALL panels)
     */
    switchToTab(tabId: string): void {
        if (this.tabManager.switchTab(tabId)) {
            const tab = this.tabManager.getActiveTab();
            if (tab) {
                this.editor.setValue(tab.content);
                this.uiManager.updateFilename(tab.name);
                this.renderTabs();

                // Update panels with tab's data (like old app.js)
                if (tab.results) {
                    this.resultsPanel.setResults(tab.results);
                    this.variablesPanel.setVariables(tab.results);
                } else {
                    this.resultsPanel.clear();
                    this.variablesPanel.clear();
                }

                // Update session panel with tab's session data
                this.sessionPanel.setSessionInfo(tab.sessionInfo || null);
                this.sessionPanel.setTokenUsage(tab.tokenUsage || null);

                // If in visual mode, refresh visual designer with new tab's content
                if (this.currentMode === 'visual') {
                    console.log('[App] Tab switched in visual mode, refreshing canvas...');
                    const xml = tab.content.trim();
                    if (xml) {
                        try {
                            const nodes = this.xmlParser.parse(xml);
                            this.visualDesigner.loadNodes(nodes);
                            console.log('[App] Visual canvas updated with new tab content');
                        } catch (error) {
                            console.error('[App] Failed to parse XML for visual mode:', error);
                            this.logsPanel.addLog('error', 'Failed to parse XML: ' + (error as Error).message);
                        }
                    }
                }
            }
        }
    }

    /**
     * Add new tab
     */
    addTab(): void {
        const defaultXml = `<?xml version="1.0" encoding="UTF-8"?>
<config xmlns="http://org.webharvest/schema/2.1/core" charset="UTF-8">
    <!-- Your configuration goes here -->
    
</config>`;

        const tab = this.tabManager.addTab();
        this.tabManager.updateTabContent(tab.id, defaultXml);
        this.renderTabs();
        this.switchToTab(tab.id);
        this.logsPanel.addLog('info', `New tab: ${tab.name}`);
    }

    /**
     * Close tab (with confirmation if dirty)
     */
    closeTab(tabId: string): void {
        const tab = this.tabManager.getAllTabs().find(t => t.id === tabId);

        if (!tab) return;

        // Confirm if dirty
        if (tab.isDirty) {
            const confirmed = confirm(`Close "${tab.name}"?\n\nYou have unsaved changes that will be lost.`);
            if (!confirmed) {
                return;
            }
        }

        this.tabManager.removeTab(tabId);
        this.renderTabs();

        // Update editor with new active tab
        const activeTab = this.tabManager.getActiveTab();
        if (activeTab) {
            this.editor.setValue(activeTab.content);
            this.uiManager.updateFilename(activeTab.name);
        } else {
            this.editor.setValue('');
            this.uiManager.updateFilename('No tabs');
        }
    }

    /**
     * Switch sidebar panel (Logs/Results/Variables/Session)
     */
    switchSidebarPanel(panelName: string): void {
        // Update nav buttons
        document.querySelectorAll('.sidebar-nav-btn').forEach(btn => {
            btn.classList.toggle('active', (btn as HTMLElement).dataset.panel === panelName);
        });

        // Update panels
        document.querySelectorAll('.sidebar-panel').forEach(panel => {
            panel.classList.toggle('active', panel.id === panelName + 'Panel');
        });
    }

    /**
     * Clear logs
     */
    clearLogs(): void {
        this.logsPanel.clear();
    }

    /**
     * Export results
     */
    exportResults(): void {
        const json = this.resultsPanel.exportAsJSON();
        const blob = new Blob([json], { type: 'application/json' });
        const url = URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = 'results.json';
        a.click();
        URL.revokeObjectURL(url);
    }

    /**
     * Hide examples modal
     */
    hideExamplesModal(): void {
        this.uiManager.hideExamplesModal();
    }

    /**
     * Load example directly on click (Apple HIG - immediate action)
     * Creates NEW TAB for each example!
     */
    async loadExampleDirect(event: Event): Promise<void> {
        const card = (event.target as HTMLElement).closest('.example-card') as HTMLElement;
        if (!card) return;

        const filename = card.dataset.example;
        if (!filename) return;

        try {
            // Load content
            const content = await this.examplesManager.loadExampleContent(filename);

            // Create NEW tab (always!)
            const newTab = this.tabManager.addTab(filename);
            this.tabManager.updateTabContent(newTab.id, content);
            this.tabManager.markClean(newTab.id); // It's from example, not dirty

            // Render tabs
            this.renderTabs();

            // Switch to new tab
            this.switchToTab(newTab.id);

            // Update Monaco with new tab content
            this.editor.setValue(content);
            this.uiManager.updateFilename(filename);

            // Close modal
            this.uiManager.hideExamplesModal();

            // Success feedback
            this.logsPanel.addLog('success', `✅ Loaded: ${filename} (new tab)`);

        } catch (error) {
            console.error('Failed to load example:', error);
            this.logsPanel.addLog('error', `❌ Failed to load: ${filename}`);
        }
    }

    /**
     * Select example (for legacy button - deprecated)
     */
    selectExample(event: Event): void {
        // Deprecated - now using direct load
        this.loadExampleDirect(event);
    }

    /**
     * Load selected example (for legacy button - deprecated)
     */
    async loadSelectedExample(): Promise<void> {
        // Deprecated - now using direct load
        const selected = document.querySelector('.example-card.selected') as HTMLElement;
        if (selected) {
            await this.loadExampleDirect({ target: selected } as any);
        }
    }

    /**
     * Pause execution
     */
    pauseExecution(): void {
        const executionId = this.wsManager.getExecutionId();
        if (executionId) {
            this.wsManager.send({ action: 'pause' });
            this.logsPanel.addLog('warning', 'Pausing execution...');
        }
    }

    /**
     * Resume execution
     */
    resumeExecution(): void {
        const executionId = this.wsManager.getExecutionId();
        if (executionId) {
            this.wsManager.send({ action: 'resume' });
            this.logsPanel.addLog('info', 'Resuming execution...');
        }
    }

    /**
     * Stop execution
     */
    stopExecution(): void {
        const executionId = this.wsManager.getExecutionId();
        if (executionId) {
            this.wsManager.send({ action: 'stop' });
            this.logsPanel.addLog('error', 'Stopping execution...');
        }
    }

    /**
     * Hide tooltip
     */
    hideTooltip(): void {
        const tooltip = document.getElementById('tooltip');
        if (tooltip) {
            tooltip.style.display = 'none';
        }
    }

    /**
     * Load session history (calls SessionPanel method)
     */
    loadSessionHistory(): void {
        console.log('[App] Loading session history...');
        this.sessionPanel.loadSessionHistory();
    }
}

