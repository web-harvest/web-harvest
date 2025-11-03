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
import { ExecutionStateManager } from './ExecutionStateManager';
import { LogsPanel } from '../panels/LogsPanel';
import { ResultsPanel } from '../panels/ResultsPanel';
import { VariablesPanel } from '../panels/VariablesPanel';
import { SessionPanel } from '../panels/SessionPanel';
import { VisualDesigner } from '../visual/VisualDesigner';
import { PropertyPanel } from '../visual/PropertyPanel';
import { PluginPalette } from '../visual/PluginPalette';
import { XMLParser } from '../visual/XMLParser';
import { XMLGenerator } from '../visual/XMLGenerator';
import type { ViewMode } from '../types';

export class App {
    private tabManager: TabManager;
    private wsManager: WebSocketManager;
    private uiManager: UIManager;
    private pluginManager: PluginManager;
    private examplesManager: ExamplesManager;
    private executionStateManager: ExecutionStateManager;
    private editor!: MonacoEditor;
    private logsPanel: LogsPanel;
    private resultsPanel: ResultsPanel;
    private variablesPanel: VariablesPanel;
    private sessionPanel: SessionPanel;
    private visualDesigner: VisualDesigner;
    private propertyPanel: PropertyPanel;
    private pluginPalette: PluginPalette;
    private xmlParser: XMLParser;
    private xmlGenerator: XMLGenerator;
    private currentMode: ViewMode = 'visual'; // Start in Visual mode (Conductor-style)
    private gridEnabled: boolean = false;

    constructor() {
        console.log('🚀 Initializing WebHarvest IDE (TypeScript + Apple HIG)...');

        // Initialize managers
        this.tabManager = new TabManager();
        this.wsManager = new WebSocketManager();
        this.uiManager = new UIManager();
        this.pluginManager = new PluginManager();
        this.examplesManager = new ExamplesManager();
        this.executionStateManager = new ExecutionStateManager();
        this.editor = new MonacoEditor('editor');
        this.logsPanel = new LogsPanel('logs');
        this.resultsPanel = new ResultsPanel('results');
        this.variablesPanel = new VariablesPanel('variables');
        this.sessionPanel = new SessionPanel('session');
        // Note: visualDesigner initialized after pluginManager loads in initialize()
        this.visualDesigner = new VisualDesigner('canvas-area');
        this.propertyPanel = new PropertyPanel('propertyPanel');
        this.pluginPalette = new PluginPalette('plugin-palette');
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
            console.log('🚀 [App] Starting initialization...');

            // 1. Initialize Monaco Editor
            console.log('🚀 [App] Step 1: Initializing Monaco Editor...');
            await this.initializeEditor();

            // 2. Load plugins (backend metadata)
            console.log('🚀 [App] Step 2: Loading plugin metadata...');
            await this.loadPlugins();

            // 3. Initialize Plugin Palette (fetch from API)
            console.log('🚀 [App] Step 3: Initializing Plugin Palette...');
            await this.pluginPalette.init();
            console.log('🚀 [App] Step 3 complete: Plugin Palette initialized');

            // 4. Setup event handlers
            console.log('🚀 [App] Step 4: Setting up event handlers...');
            this.setupEventHandlers();

            // 5. Setup drag & drop
            console.log('🚀 [App] Step 5: Setting up drag & drop...');
            this.setupDragAndDrop();

            // 5. Setup modal handlers
            this.setupModalHandlers();

            // 6. Create initial tab (for XML mode when needed)
            const initialTab = this.tabManager.addTab('untitled.xml');
            this.renderTabs();
            this.switchToTab(initialTab.id);

            // 7. Set initial mode to VISUAL (Conductor-style!)
            console.log('🎨 [App] Setting default mode to VISUAL...');
            this.uiManager.switchToVisualMode();
            this.updateModeToggleButtons('visual'); // Update toolbar

            // Update filename in unified toolbar
            this.updateUnifiedToolbar();

            console.log('✅ WebHarvest IDE initialized successfully (Conductor-style Visual mode)!');
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
                    this.updateUnifiedToolbar(); // Update dirty indicator
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

            // Initialize semantic validator in VisualDesigner
            this.visualDesigner.setPluginManager(this.pluginManager);

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
            if (this.currentMode === 'visual' && this.visualDesigner && Array.isArray(update.nodeUpdates) && update.nodeUpdates.length > 0) {
                update.nodeUpdates.forEach((nodeUpdate) => {
                    this.executionStateManager.updateNodeState(nodeUpdate);
                    const nodeState = this.executionStateManager.getNodeState(nodeUpdate.nodeId);

                    if (!nodeState) {
                        console.warn(`[App] Received execution update for unknown node: ${nodeUpdate.nodeId}`);
                        return;
                    }

                    this.visualDesigner.updateNodeExecutionState(nodeUpdate.nodeId, nodeState.status);

                    if (nodeState.metrics) {
                        this.visualDesigner.updateNodeMetrics(nodeUpdate.nodeId, nodeState.metrics);
                    }
                });
            }

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

            // Handle terminal statuses (COMPLETED, FAILED, ERROR, STOPPED)
            const terminalStatuses = ['COMPLETED', 'FAILED', 'ERROR', 'STOPPED'];
            if (terminalStatuses.includes(update.status)) {
                console.log(`${update.status}! Processing results...`);

                // Hide execution action buttons when execution ends
                if (this.currentMode === 'visual') {
                    this.uiManager.showExecutionActionButtons(false);
                }
            }

            // Handle COMPLETED status specifically
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
            console.log(`📡 [App] Received 'nodeSelected' event for: ${nodeId}`);
            const node = this.visualDesigner.findNode(nodeId);
            if (node) {
                console.log(`✅ [App] Found node, calling propertyPanel.showNode():`, node);
                this.propertyPanel.showNode(node).catch(err => {
                    console.error('❌ [App] Error showing node in PropertyPanel:', err);
                });
            } else {
                console.warn(`⚠️ [App] Node not found: ${nodeId}`);
            }
        });

        // Visual Designer → XML (when node added)
        this.visualDesigner.addEventListener('nodeAdded', (node: any) => {
            console.log('Node added, syncing to XML:', node.type);
            if (this.currentMode === 'visual') {
                this.syncVisualToXML();
                this.updateUnifiedToolbar(); // Update dirty indicator
            }
        });

        // Visual Designer → Toolbar update (on any change)
        this.visualDesigner.addEventListener('nodeRemoved', () => {
            if (this.currentMode === 'visual') {
                this.updateUnifiedToolbar();
            }
        });

        this.visualDesigner.addEventListener('nodeUpdated', () => {
            if (this.currentMode === 'visual') {
                this.updateUnifiedToolbar();
            }
        });

        // Property Panel → Visual Designer
        this.propertyPanel.onChange((updatedNode) => {
            this.visualDesigner.updateNode(updatedNode.id, updatedNode);

            // Sync to XML immediately
            if (this.currentMode === 'visual') {
                this.syncVisualToXML();
            }
        });

        // Plugin Palette → Visual Designer
        this.pluginPalette.onAddNode((newNode) => {
            this.visualDesigner.addNode(newNode);
            this.logsPanel.addLog('success', `Added ${newNode.type} node`);

            // Sync to XML immediately
            if (this.currentMode === 'visual') {
                this.syncVisualToXML();
            }
        });

        this.visualDesigner.addEventListener('saveRequested', async (payload: any) => {
            try {
                const xml = typeof payload?.xml === 'string' ? payload.xml : this.visualDesigner.generateXML();
                const providedFilename: string | undefined = payload?.filename;

                if (providedFilename && providedFilename.trim().length > 0) {
                    this.downloadXml(xml, providedFilename);
                    this.visualDesigner.markClean();
                    this.updateUnifiedToolbar();
                    this.logsPanel.addLog('success', `💾 Saved: ${providedFilename}`);
                    return;
                }

                const suggestedName = this.visualDesigner.getCurrentConfiguration()?.filename || 'visual-workflow.xml';
                const chosenName = window.prompt('Enter filename for this configuration:', suggestedName);

                if (!chosenName) {
                    this.logsPanel.addLog('info', '💡 Save cancelled');
                    return;
                }

                await this.visualDesigner.saveConfigurationAs(chosenName);
            } catch (error) {
                console.error('❌ [App] Failed to handle saveRequested event:', error);
                this.logsPanel.addLog('error', '❌ Failed to save configuration');
            }
        });

        this.visualDesigner.addEventListener('saveAsRequested', (payload: any) => {
            try {
                const filename = (payload?.filename && payload.filename.trim().length > 0)
                    ? payload.filename
                    : 'visual-workflow.xml';
                const xml = typeof payload?.xml === 'string' ? payload.xml : this.visualDesigner.generateXML();

                this.downloadXml(xml, filename);
                this.visualDesigner.markClean();
                this.updateUnifiedToolbar();
                this.logsPanel.addLog('success', `💾 Saved as: ${filename}`);
            } catch (error) {
                console.error('❌ [App] Failed to handle saveAsRequested event:', error);
                this.logsPanel.addLog('error', '❌ Failed to save configuration');
            }
        });

        console.log('✅ Event handlers configured');

        // Keyboard shortcuts (Visual mode only)
        document.addEventListener('keydown', (e: KeyboardEvent) => {
            // Ignore when typing in inputs/textareas
            const target = e.target as HTMLElement;
            const tag = target?.tagName?.toLowerCase();
            if (tag === 'input' || tag === 'textarea' || target?.isContentEditable) return;

            const meta = e.metaKey || e.ctrlKey;
            // Delete selected
            if (e.key === 'Backspace' || e.key === 'Delete') {
                if (this.currentMode === 'visual') {
                    this.visualDesigner.deleteSelectedNodes();
                    e.preventDefault();
                }
            }
            // Select All
            if (meta && e.key.toLowerCase() === 'a') {
                if (this.currentMode === 'visual') {
                    this.visualDesigner.selectAll();
                    e.preventDefault();
                }
            }
            // Zoom
            if (meta && (e.key === '=' || e.key === '+')) {
                if (this.currentMode === 'visual') {
                    this.visualDesigner.zoomIn();
                    e.preventDefault();
                }
            }
            if (meta && e.key === '-') {
                if (this.currentMode === 'visual') {
                    this.visualDesigner.zoomOut();
                    e.preventDefault();
                }
            }
            if (meta && e.key === '0') {
                if (this.currentMode === 'visual') {
                    this.visualDesigner.resetView();
                    e.preventDefault();
                }
            }
        });
    }

    // =========================
    // Public UI actions (toolbar)
    // =========================
    zoomIn(): void {
        if (this.currentMode === 'visual') this.visualDesigner.zoomIn();
    }

    zoomOut(): void {
        if (this.currentMode === 'visual') this.visualDesigner.zoomOut();
    }

    zoomReset(): void {
        if (this.currentMode === 'visual') this.visualDesigner.resetView();
    }

    zoomToFit(): void {
        // Basic implementation: reset view for now
        if (this.currentMode === 'visual') this.visualDesigner.resetView();
    }

    autoLayout(type: 'grid' | 'vertical' | 'horizontal'): void {
        console.log(`🔧 [App] autoLayout(${type}) called, currentMode: ${this.currentMode}`);
        if (this.currentMode === 'visual' && this.visualDesigner) {
            console.log(`✅ [App] Calling visualDesigner.autoLayout(${type})`);
            this.visualDesigner.autoLayout(type);
        } else {
            console.warn(`⚠️ [App] autoLayout failed - mode: ${this.currentMode}, visualDesigner: ${!!this.visualDesigner}`);
        }
    }

    toggleGrid(): void {
        console.log(`🔧 [App] toggleGrid() called, currentMode: ${this.currentMode}`);
        if (this.currentMode !== 'visual' || !this.visualDesigner) {
            console.warn(`⚠️ [App] toggleGrid failed - mode: ${this.currentMode}, visualDesigner: ${!!this.visualDesigner}`);
            return;
        }
        this.gridEnabled = !this.gridEnabled;
        this.visualDesigner.setGridSnap(this.gridEnabled, 20);
        console.log(`📐 Grid snap ${this.gridEnabled ? 'ENABLED' : 'DISABLED'}`);
    }

    toggleMinimap(): void {
        console.log(`🔧 [App] toggleMinimap() called, currentMode: ${this.currentMode}`);
        if (this.currentMode !== 'visual' || !this.visualDesigner) {
            console.warn(`⚠️ [App] toggleMinimap failed - mode: ${this.currentMode}, visualDesigner: ${!!this.visualDesigner}`);
            return;
        }
        const currentlyVisible = this.visualDesigner.isMinimapVisible();
        this.visualDesigner.showMinimap(!currentlyVisible);
        console.log(`🗺️ Minimap ${!currentlyVisible ? 'ENABLED' : 'DISABLED'}`);
    }

    private downloadXml(xml: string, filename: string): void {
        const blob = new Blob([xml], { type: 'text/xml' });
        const url = URL.createObjectURL(blob);

        const anchor = document.createElement('a');
        anchor.href = url;
        anchor.download = filename;
        anchor.style.display = 'none';

        document.body.appendChild(anchor);
        anchor.click();
        anchor.remove();

        URL.revokeObjectURL(url);
    }

    /**
     * Sync visual nodes to XML (real-time)
     */
    private syncVisualToXML(): void {
        try {
            const xml = this.visualDesigner.generateXML();

            const activeTab = this.tabManager.getActiveTab();
            if (activeTab) {
                this.tabManager.updateTabContent(activeTab.id, xml);
                this.tabManager.markDirty(activeTab.id);
                this.editor.setValue(xml);
            }
        } catch (error) {
            console.error('Failed to sync visual to XML:', error);
        }
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

            if (this.currentMode === 'visual' && this.visualDesigner) {
                const nodesForTracking = this.visualDesigner.getAllNodes();
                this.executionStateManager.registerNodes(nodesForTracking);
                this.visualDesigner.clearExecutionStates();
            } else {
                this.executionStateManager.reset();
            }

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

            // Show execution action buttons (Pause/Resume/Stop)
            if (this.currentMode === 'visual') {
                this.uiManager.showExecutionActionButtons(true);
            }

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
    async saveFile(): Promise<void> {
        if (this.currentMode === 'visual') {
            await this.visualDesigner.requestSave();
            return;
        }

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
     * Switch mode (Conductor-style unified toolbar)
     */
    async switchMode(mode: 'xml' | 'visual'): Promise<void> {
        console.log(`🔄 [App] Switching to ${mode} mode...`);

        // If switching away from Visual and has unsaved changes
        if (this.currentMode === 'visual' && mode === 'xml' && this.visualDesigner.isDirty()) {
            const confirmed = confirm('You have unsaved changes. Discard and switch to XML mode?');
            if (!confirmed) {
                console.log('ℹ️ Mode switch cancelled');
                return;
            }
        }

        // Switch data model
        this.switchViewMode(mode);

        // Update UI
        if (mode === 'visual') {
            this.uiManager.switchToVisualMode();
        } else {
            this.uiManager.switchToXMLMode();
        }

        // Update toolbar buttons (active state)
        this.updateModeToggleButtons(mode);

        this.currentMode = mode;
        console.log(`✅ [App] Switched to ${mode} mode`);
    }

    /**
     * Update mode toggle buttons (active state)
     */
    private updateModeToggleButtons(activeMode: 'xml' | 'visual'): void {
        const xmlBtn = document.getElementById('btn-xml-mode');
        const visualBtn = document.getElementById('btn-visual-mode');

        if (xmlBtn && visualBtn) {
            if (activeMode === 'xml') {
                xmlBtn.style.background = '#007bff';
                xmlBtn.style.color = 'white';
                visualBtn.style.background = 'transparent';
                visualBtn.style.color = '#666';
            } else {
                xmlBtn.style.background = 'transparent';
                xmlBtn.style.color = '#666';
                visualBtn.style.background = '#007bff';
                visualBtn.style.color = 'white';
            }
        }
    }

    /**
     * New configuration (works in both modes)
     */
    async newConfiguration(): Promise<void> {
        if (this.currentMode === 'visual') {
            await this.newVisualConfiguration();
        } else {
            // XML mode: create new tab
            const newTab = this.tabManager.addTab('untitled.xml');
            this.renderTabs();
            this.switchToTab(newTab.id);
            this.logsPanel.addLog('info', '✨ New configuration created');
        }
    }

    /**
     * Toggle Visual Mode (legacy - for backwards compatibility)
     */
    async toggleVisualMode(): Promise<void> {
        const newMode = this.currentMode === 'xml' ? 'visual' : 'xml';

        // Check for unsaved changes before switching
        if (newMode === 'xml' && this.visualDesigner.shouldPromptBeforeModeSwitch()) {
            const confirmed = confirm('You have unsaved changes in Visual mode. Discard and switch to XML?');
            if (!confirmed) {
                console.log('ℹ️ Mode switch cancelled by user');
                return;
            }
        }

        // Switch data model
        this.switchViewMode(newMode);

        // Switch UI
        if (newMode === 'visual') {
            this.uiManager.switchToVisualMode();
            this.updateVisualToolbar();
        } else {
            this.uiManager.switchToXMLMode();
        }

        this.currentMode = newMode;
    }

    /**
     * Update unified toolbar (filename + dirty indicator)
     */
    updateUnifiedToolbar(): void {
        const filenameElement = document.getElementById('current-filename');
        const dirtyIndicator = document.getElementById('dirty-indicator');

        let filename = 'untitled.xml';
        let isDirty = false;

        if (this.currentMode === 'visual') {
            const config = this.visualDesigner.getCurrentConfiguration();
            filename = config?.filename || 'untitled.xml';
            isDirty = config?.isDirty || false;
        } else {
            const tab = this.tabManager.getActiveTab();
            filename = tab?.name || 'untitled.xml';
            isDirty = tab?.isDirty || false;
        }

        if (filenameElement) {
            filenameElement.textContent = filename;
        }

        if (dirtyIndicator) {
            dirtyIndicator.style.display = isDirty ? 'inline' : 'none';
        }
    }

    /**
     * Update Visual mode toolbar (DEPRECATED - use updateUnifiedToolbar)
     */
    updateVisualToolbar(): void {
        this.updateUnifiedToolbar();
    }

    /**
     * Save Visual configuration
     */
    async saveVisualConfiguration(): Promise<void> {
        await this.visualDesigner.requestSave();
    }

    /**
     * Save Visual configuration as...
     */
    async saveVisualConfigurationAs(): Promise<void> {
        const suggested = this.visualDesigner.getCurrentConfiguration()?.filename || 'visual-workflow.xml';
        const filename = window.prompt('Enter filename:', suggested);
        if (!filename) {
            this.logsPanel.addLog('info', '💡 Save As cancelled');
            return;
        }

        await this.visualDesigner.saveConfigurationAs(filename);
    }

    /**
     * Create new Visual configuration
     */
    async newVisualConfiguration(): Promise<void> {
        // Check for unsaved changes
        if (this.visualDesigner.isDirty()) {
            const action = confirm(
                'You have unsaved changes. Create new configuration?\n\nClick OK to discard changes, Cancel to keep working.'
            );

            if (!action) {
                console.log('ℹ️ New configuration cancelled');
                return;
            }
        }

        // Create new
        this.visualDesigner.newConfiguration();
        this.updateVisualToolbar();
        this.logsPanel.addLog('info', '✨ New configuration created');
    }

    /**
     * Open file in Visual mode
     */
    async openFileInVisual(): Promise<void> {
        const input = document.createElement('input');
        input.type = 'file';
        input.accept = '.xml';

        input.addEventListener('change', async (event) => {
            const target = event.target as HTMLInputElement;
            const file = target.files?.[0];

            if (!file) {
                return;
            }

            const reader = new FileReader();

            reader.onload = async (loadEvent) => {
                const content = loadEvent?.target?.result;

                if (typeof content !== 'string') {
                    this.logsPanel.addLog('error', '❌ Failed to read configuration file');
                    return;
                }

                try {
                    const nodes = this.xmlParser.parse(content);
                    const loaded = await this.visualDesigner.loadNodesWithPrompt(nodes);

                    if (!loaded) {
                        this.logsPanel.addLog('info', 'ℹ️ Load cancelled');
                        return;
                    }

                    this.visualDesigner.setCurrentFilename(file.name);
                    this.visualDesigner.markClean();
                    this.updateUnifiedToolbar();

                    if (this.currentMode === 'visual') {
                        this.syncVisualToXML();
                    }

                    this.logsPanel.addLog('success', `✅ Loaded: ${file.name}`);
                } catch (error) {
                    console.error('❌ [App] Failed to import configuration:', error);
                    this.logsPanel.addLog('error', '❌ Failed to load configuration');
                }
            };

            reader.onerror = (err) => {
                console.error('❌ [App] File read error:', err);
                this.logsPanel.addLog('error', '❌ Failed to load configuration');
            };

            reader.readAsText(file);
            target.value = '';
        });

        input.click();
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
        console.log(`🎯 [App] runConfig() called in ${this.currentMode} mode`);

        // Get XML content based on current mode
        let xmlContent: string;
        let configName: string;

        if (this.currentMode === 'visual') {
            // Generate XML from Visual Designer
            const nodes = this.visualDesigner.getNodes();
            if (nodes.length === 0) {
                this.logsPanel.addLog('warning', '⚠️ No nodes in canvas. Add plugins first.');
                return;
            }

            console.log('🎨 [App] Generating XML from', nodes.length, 'visual nodes...');

            // Wrap in <config> if needed
            xmlContent = this.visualDesigner.generateXML();

            const config = this.visualDesigner.getCurrentConfiguration();
            configName = config?.filename || 'visual-workflow.xml';

            console.log('✅ [App] Generated XML from Visual mode:', xmlContent.length, 'chars');
        } else {
            // XML mode: use tab content
            const tab = this.tabManager.getActiveTab();
            if (!tab) {
                this.logsPanel.addLog('error', '❌ No active tab');
                return;
            }

            xmlContent = tab.content;
            configName = tab.name;
        }

        // Validate first
        this.logsPanel.addLog('info', '🔍 Validating configuration...');

        try {
            const validationResponse = await fetch('/api/config/validate', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({
                    configXml: xmlContent,
                    configName: configName,
                }),
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

        // Run with generated/retrieved XML content
        await this.runExecution(xmlContent);
    }

    /**
     * Validate configuration (explicit validation - shows ALL errors)
     */
    async validateConfig(): Promise<void> {
        console.log(`🎯 [App] validateConfig() called in ${this.currentMode} mode`);

        // Get XML content based on current mode
        let xmlContent: string;

        if (this.currentMode === 'visual') {
            // Generate XML from Visual Designer
            const nodes = this.visualDesigner.getNodes();
            if (nodes.length === 0) {
                this.logsPanel.addLog('warning', '⚠️ No nodes in canvas. Add plugins first.');
                return;
            }

            xmlContent = this.visualDesigner.generateXML();
        } else {
            // XML mode: use tab content
            const tab = this.tabManager.getActiveTab();
            if (!tab) {
                this.logsPanel.addLog('error', '❌ No active tab');
                return;
            }

            xmlContent = tab.content;
        }

        this.logsPanel.addLog('info', '🔍 Validating configuration...');

        try {
            const response = await fetch('/api/config/validate', {
                method: 'POST',
                headers: { 'Content-Type': 'application/xml' },
                body: xmlContent,
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
     * Checks for duplicates before creating new tab!
     */
    async loadExampleDirect(event: Event): Promise<void> {
        const card = (event.target as HTMLElement).closest('.example-card') as HTMLElement;
        if (!card) return;

        const filename = card.dataset.example;
        if (!filename) return;

        try {
            // Load content
            const content = await this.examplesManager.loadExampleContent(filename);

            // Check if tab with this name already exists
            const existingTab = this.tabManager.findTabByName(filename);

            if (existingTab) {
                // Tab exists - ask user if they want to overwrite
                const shouldOverwrite = confirm(
                    `Tab "${filename}" is already open.\n\n` +
                    `Do you want to reload it with fresh content from examples?\n\n` +
                    `• OK = Reload the tab\n` +
                    `• Cancel = Switch to existing tab without reloading`
                );

                if (shouldOverwrite) {
                    // User confirmed - reload the tab
                    console.log('Reloading existing tab:', filename);
                    this.tabManager.updateTabContent(existingTab.id, content);
                    this.tabManager.markClean(existingTab.id);
                    this.switchToTab(existingTab.id);
                    this.logsPanel.addLog('success', `✅ Reloaded: ${filename}`);
                } else {
                    // User cancelled - just switch to existing tab without reloading
                    console.log('Switching to existing tab without reload:', filename);
                    this.switchToTab(existingTab.id);
                    this.logsPanel.addLog('info', `Switched to existing tab: ${filename}`);
                }
            } else {
                // Tab doesn't exist - create new one
                const newTab = this.tabManager.addTab(filename, content);
                this.tabManager.markClean(newTab.id); // It's from example, not dirty

                // Render tabs
                this.renderTabs();

                // Switch to new tab
                this.switchToTab(newTab.id);
                this.logsPanel.addLog('success', `✅ Loaded: ${filename} (new tab)`);
            }

            // Update Monaco with tab content
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
        // Hide execution action buttons when stopped
        if (this.currentMode === 'visual') {
            this.uiManager.showExecutionActionButtons(false);
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

