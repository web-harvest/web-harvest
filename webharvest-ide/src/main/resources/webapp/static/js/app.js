/**
 * Web-Harvest IDE - Modern Web Application
 * 
 * Features:
 * - Monaco Editor (VS Code editor)
 * - Multi-tab editing
 * - Real-time execution updates via WebSocket
 * - Concurrent scraper execution
 */

const app = {
    editor: null,
    tabs: [],
    activeTabId: null,
    currentExecutionId: null,
    webSocket: null,

    /**
     * Initialize the IDE
     */
    init() {
        console.log('Initializing Web-Harvest IDE...');

        // First: Create example tabs (before Monaco)
        this.loadExampleTabs();

        // Then: Initialize Monaco with first tab content
        this.initMonaco();
        this.initEventListeners();

        console.log('Web-Harvest IDE ready with', this.tabs.length, 'example tabs!');
    },

    /**
     * Load example configurations as tabs
     */
    loadExampleTabs() {
        console.log('Loading example tabs...');

        // Example 1: Simple HTTP + XPath
        this.addTab('01_http_xpath.xml', this.getHttpXPathExample());
        console.log('  Added tab 1: HTTP + XPath');

        // Example 2: Quotes Scraping
        this.addTab('02_quotes_scraping.xml', this.getQuotesExample());
        console.log('  Added tab 2: Quotes Scraping');

        // Example 3: JSON API
        this.addTab('03_json_api.xml', this.getJsonApiExample());
        console.log('  Added tab 3: JSON API');

        // Example 4: Wikipedia
        this.addTab('04_wikipedia.xml', this.getWikipediaExample());
        console.log('  Added tab 4: Wikipedia');

        // Example 5: Complex Pipeline
        this.addTab('05_data_pipeline.xml', this.getDataPipelineExample());
        console.log('  Added tab 5: Data Pipeline');

        console.log('Total tabs loaded:', this.tabs.length);
    },

    /**
     * Initialize Monaco Editor
     */
    initMonaco() {
        require.config({ paths: { vs: 'https://cdnjs.cloudflare.com/ajax/libs/monaco-editor/0.44.0/min/vs' } });

        require(['vs/editor/editor.main'], () => {
            // Get content from active tab (if tabs were pre-loaded)
            const activeTab = this.tabs.find(t => t.id === this.activeTabId);
            const initialContent = activeTab ? activeTab.content : '<!-- Loading... -->';

            console.log('Creating Monaco Editor with content from tab:', this.activeTabId);
            console.log('Content length:', initialContent.length);

            this.editor = monaco.editor.create(document.getElementById('editor'), {
                value: initialContent,
                language: 'xml',
                theme: 'vs-dark',
                automaticLayout: true,
                minimap: { enabled: true },
                fontSize: 14,
                lineNumbers: 'on',
                renderWhitespace: 'selection',
                folding: true,
                wordWrap: 'on'
            });

            // Update cursor position
            this.editor.onDidChangeCursorPosition((e) => {
                document.getElementById('cursorPosition').textContent =
                    `Ln ${e.position.lineNumber}, Col ${e.position.column}`;
            });

            // Auto-save to tab
            this.editor.onDidChangeModelContent(() => {
                if (this.activeTabId) {
                    const tab = this.tabs.find(t => t.id === this.activeTabId);
                    if (tab) {
                        tab.content = this.editor.getValue();
                        tab.modified = true;
                        this.renderTabs();
                    }
                }
            });
        });
    },

    /**
     * Initialize event listeners
     */
    initEventListeners() {
        // Result tabs
        document.querySelectorAll('.result-tab').forEach(btn => {
            btn.addEventListener('click', (e) => {
                const tabName = e.currentTarget.dataset.tab;
                this.switchResultTab(tabName);
            });
        });

        // Keyboard shortcuts
        document.addEventListener('keydown', (e) => {
            // Ctrl+S / Cmd+S: Save
            if ((e.ctrlKey || e.metaKey) && e.key === 's') {
                e.preventDefault();
                this.saveFile();
            }
            // Ctrl+Enter / Cmd+Enter: Run
            if ((e.ctrlKey || e.metaKey) && e.key === 'Enter') {
                e.preventDefault();
                this.runConfig();
            }
            // Ctrl+T / Cmd+T: New Tab
            if ((e.ctrlKey || e.metaKey) && e.key === 't') {
                e.preventDefault();
                this.addTab();
            }
        });
    },

    /**
     * Add new tab
     */
    addTab(name = null, content = null) {
        const tabId = 'tab-' + Date.now() + '-' + this.tabs.length;
        const tabName = name || `config-${this.tabs.length + 1}.xml`;
        const tabContent = content || this.getHttpXPathExample();

        console.log('Adding tab:', tabName, '- Content length:', tabContent.length);

        const tab = {
            id: tabId,
            name: tabName,
            content: tabContent,
            modified: false,

            // v2.2: Per-tab execution state (Apple HIG - each document has own state)
            executionId: null,
            logs: [],
            results: {},
            sessionInfo: null,
            tokenUsage: null,
            status: 'IDLE',
            lastUpdate: null
        };

        this.tabs.push(tab);

        // Only switch if this is the first tab or explicitly requested
        if (this.tabs.length === 1 || !this.activeTabId) {
            this.activeTabId = tabId;
            console.log('  Set as active tab');
        }

        this.renderTabs();
    },

    /**
     * Get active tab (v2.2 helper)
     */
    getActiveTab() {
        return this.tabs.find(t => t.id === this.activeTabId);
    },

    /**
     * Switch to tab (v2.2: Restores per-tab state - Apple HIG)
     */
    switchTab(tabId) {
        console.log('Switching to tab:', tabId);

        // Save current tab content
        if (this.activeTabId && this.editor) {
            const currentTab = this.tabs.find(t => t.id === this.activeTabId);
            if (currentTab) {
                currentTab.content = this.editor.getValue();
                console.log('  Saved current tab content:', currentTab.name, currentTab.content.length, 'chars');
            }
        }

        // Switch to new tab
        this.activeTabId = tabId;
        const tab = this.tabs.find(t => t.id === tabId);

        if (tab) {
            console.log('  Loading tab:', tab.name, tab.content.length, 'chars');
            if (this.editor) {
                this.editor.setValue(tab.content);
                console.log('  Editor content updated');
            } else {
                console.warn('  Editor not ready yet, content will load when Monaco initializes');
            }

            // v2.2: Restore per-tab execution state (Apple HIG)
            console.log('  Restoring tab state:', {
                logs: tab.logs.length,
                results: Object.keys(tab.results).length,
                status: tab.status
            });
            this.renderLogs();
            this.renderResults();
            this.renderVariables();
            this.renderSessionInfo();
        }

        this.renderTabs();
    },

    /**
     * Close tab
     */
    closeTab(tabId, event) {
        event.stopPropagation();

        const index = this.tabs.findIndex(t => t.id === tabId);
        if (index === -1) return;

        // Confirm if modified
        const tab = this.tabs[index];
        if (tab.modified) {
            if (!confirm(`${tab.name} has unsaved changes. Close anyway?`)) {
                return;
            }
        }

        this.tabs.splice(index, 1);

        // Switch to another tab or create new one
        if (this.tabs.length === 0) {
            this.addTab();
        } else {
            const nextTab = this.tabs[Math.max(0, index - 1)];
            this.switchTab(nextTab.id);
        }

        this.renderTabs();
    },

    /**
     * Render tabs
     */
    renderTabs() {
        const tabsContainer = document.getElementById('tabs');
        tabsContainer.innerHTML = '';

        this.tabs.forEach(tab => {
            const tabEl = document.createElement('div');
            tabEl.className = 'tab' + (tab.id === this.activeTabId ? ' active' : '');
            tabEl.onclick = () => this.switchTab(tab.id);

            const name = document.createElement('span');
            name.textContent = tab.name + (tab.modified ? ' •' : '');

            const closeBtn = document.createElement('button');
            closeBtn.className = 'tab-close';
            closeBtn.innerHTML = '<i class="fas fa-times"></i>';
            closeBtn.onclick = (e) => this.closeTab(tab.id, e);

            tabEl.appendChild(name);
            tabEl.appendChild(closeBtn);
            tabsContainer.appendChild(tabEl);
        });
    },

    /**
     * Validate configuration
     */
    async validateConfig() {
        const xml = this.editor.getValue();
        this.setStatus('<i class="fas fa-spinner fa-spin"></i> Validating...', 'info');

        try {
            const response = await fetch('/api/config/validate', {
                method: 'POST',
                headers: { 'Content-Type': 'application/xml' },
                body: xml
            });

            const result = await response.json();

            if (result.valid) {
                this.setStatus('<i class="fas fa-check-circle text-success"></i> Configuration is valid!', 'success');
            } else {
                this.setStatus('<i class="fas fa-times-circle text-error"></i> Validation failed: ' + result.message, 'error');
            }
        } catch (error) {
            this.setStatus('<i class="fas fa-times-circle text-error"></i> Validation error: ' + error.message, 'error');
        }
    },

    /**
     * Run configuration (v2.2: Stores executionId in active tab)
     */
    async runConfig() {
        console.log('[1] runConfig() called');

        const tab = this.getActiveTab();
        console.log('[2] Active tab:', tab);
        if (!tab) {
            console.error('[ERROR] No active tab!');
            this.addLog('error', 'No active tab found');
            return;
        }

        const xml = this.editor.getValue();
        const tabName = tab.name;
        console.log('[3] Got XML, length:', xml.length, 'name:', tabName);

        // v2.2: Clear previous execution data in THIS tab
        tab.logs = [];
        tab.results = {};
        tab.sessionInfo = null;
        tab.tokenUsage = null;
        tab.status = 'RUNNING';

        this.setStatus('<i class="fas fa-spinner fa-spin"></i> Connecting...', 'info');
        this.showProgress(0);
        this.clearResults();
        this.addLog('info', '▶ Preparing to execute: ' + tabName);

        // IMPORTANT: Close any existing WebSocket before creating new one
        // This ensures each execution gets a fresh connection
        if (this.webSocket && this.webSocket.readyState !== WebSocket.CLOSED) {
            console.log('Closing existing WebSocket before new execution');
            this.webSocket.close();
            this.webSocket = null;
        }

        try {
            // FIRST: Create execution to get ID
            console.log('[DEBUG] About to send POST /api/execute');
            console.log('[DEBUG] configName:', tabName);
            console.log('[DEBUG] configXml length:', xml.length);

            const response = await fetch('/api/execute', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({
                    configXml: xml,
                    configName: tabName,
                    waitForWebSocket: true  // Signal to wait
                })
            });

            console.log('[DEBUG] fetch() returned, status:', response.status);
            console.log('[DEBUG] response ok:', response.ok);

            if (!response.ok) {
                throw new Error('Server returned error: ' + response.status);
            }

            const result = await response.json();
            console.log('[DEBUG] JSON parsed, executionId:', result.executionId);

            // v2.2: Store executionId in TAB, not globally
            tab.executionId = result.executionId;
            this.currentExecutionId = result.executionId;  // Keep for backward compat

            this.addLog('info', '⚙ Execution ID: ' + result.executionId.substring(0, 8) + '...');
            this.addLog('info', '🔌 Creating new WebSocket connection...');

            // SECOND: Create FRESH WebSocket for this execution
            await this.connectWebSocketSync(result.executionId);

            this.addLog('success', '✓ WebSocket connected, execution starting...');
            this.setStatus('<i class="fas fa-spinner fa-spin text-warning"></i> Running...', 'running');
            document.getElementById('executionControls').style.display = 'flex';

        } catch (error) {
            console.error('[ERROR] runConfig() failed:', error);
            console.error('[ERROR] Error type:', error.constructor.name);
            console.error('[ERROR] Error message:', error.message);
            console.error('[ERROR] Stack:', error.stack);

            tab.status = 'FAILED';
            this.setStatus('<i class="fas fa-times-circle text-error"></i> Execution failed: ' + error.message, 'error');
            this.addLog('error', '✗ Error: ' + error.message);
            this.addLog('error', '✗ Error type: ' + error.constructor.name);
            this.hideProgress();
        }
    },

    /**
     * Connect WebSocket synchronously (wait for connection)
     */
    connectWebSocketSync(executionId) {
        return new Promise((resolve, reject) => {
            const wsUrl = `ws://${window.location.host}/ws/execution?id=${executionId}`;

            console.log('🔍 [Safari Debug] Browser:', navigator.userAgent);
            console.log('🔍 [Safari Debug] WebSocket URL:', wsUrl);
            console.log('🔍 [Safari Debug] WebSocket support:', typeof WebSocket !== 'undefined');

            try {
                this.webSocket = new WebSocket(wsUrl);
                console.log('✓ [Safari Debug] WebSocket object created, readyState:', this.webSocket.readyState);
            } catch (err) {
                console.error('❌ [Safari Debug] Failed to create WebSocket:', err);
                this.addLog('error', '✗ WebSocket creation failed: ' + err.message);
                reject(err);
                return;
            }

            this.webSocket.onopen = () => {
                console.log('✓ [Safari Debug] WebSocket onopen event fired!');
                console.log('✓ WebSocket connected for execution:', executionId);
                this.addLog('success', '✓ [Debug] WebSocket connected!');
                resolve(); // Connection established!
            };

            this.webSocket.onmessage = (event) => {
                try {
                    const update = JSON.parse(event.data);
                    console.log('📨 WebSocket message received (', event.data.length, 'bytes)');
                    console.log('  Type:', update.type, 'Status:', update.status);

                    // Handle different message types
                    if (update.type === 'log') {
                        // Log message
                        console.log('  → Log:', update.level, update.message);
                        this.handleLogMessage(update);
                    } else {
                        // Status update
                        console.log('  → Status:', update.status);

                        // Use setTimeout to avoid blocking UI
                        setTimeout(() => {
                            try {
                                this.handleExecutionUpdate(update);
                            } catch (err) {
                                console.error('Error in handleExecutionUpdate:', err);
                                this.addLog('error', 'UI Error: ' + err.message);
                            }
                        }, 0);
                    }
                } catch (err) {
                    console.error('Error parsing WebSocket message:', err);
                    console.error('Raw message:', event.data);
                    this.addLog('error', 'Failed to parse server message: ' + err.message);
                }
            };

            this.webSocket.onerror = (error) => {
                console.error('❌ [Safari Debug] WebSocket onerror event:', error);
                console.error('❌ [Safari Debug] ReadyState:', this.webSocket.readyState);
                console.error('❌ [Safari Debug] URL:', this.webSocket.url);
                this.addLog('error', '✗ WebSocket connection error (check console)');
                reject(error);
            };

            this.webSocket.onclose = (event) => {
                console.log('🔌 [Safari Debug] WebSocket onclose event');
                console.log('🔌 WebSocket closed:', event.code, event.reason);
                console.log('🔌 [Safari Debug] Was clean:', event.wasClean);

                // Check if this was unexpected disconnect (not normal closure)
                if (event.code !== 1000 && event.code !== 1005) {
                    console.warn('⚠️ Unexpected WebSocket disconnect! Code:', event.code);
                    this.handleWebSocketDisconnect(executionId, event.code);
                }
            };

            // Timeout after 5 seconds
            setTimeout(() => {
                if (this.webSocket && this.webSocket.readyState !== WebSocket.OPEN) {
                    console.warn('⏱ [Safari Debug] WebSocket connection timeout!');
                    console.warn('⏱ [Safari Debug] Current readyState:', this.webSocket.readyState);
                    console.warn('⏱ [Safari Debug] Expected: 1 (OPEN), got:', this.webSocket.readyState);
                    this.addLog('error', '✗ WebSocket timeout - readyState: ' + this.webSocket.readyState);
                    reject(new Error('WebSocket connection timeout (readyState: ' + this.webSocket.readyState + ')'));
                }
            }, 5000);
        });
    },

    /**
     * Handle execution update from WebSocket
     */
    handleExecutionUpdate(update) {
        console.log('=== Execution update received ===');
        console.log('Status:', update.status);
        console.log('Message:', update.message);
        console.log('Progress:', update.progress);
        console.log('Results:', update.results);
        console.log('Full update object:', update);

        this.showProgress(update.progress || 0);

        switch (update.status) {
            case 'STARTED':
                this.addLog('info', '▶ ' + update.message);
                break;

            case 'RUNNING':
                this.addLog('info', '⚙ ' + update.message);
                // Show Pause button, hide Resume button (in case resuming)
                const pauseBtnRun = document.getElementById('pauseBtn');
                const resumeBtnRun = document.getElementById('resumeBtn');
                if (pauseBtnRun) pauseBtnRun.style.display = 'inline-block';
                if (resumeBtnRun) resumeBtnRun.style.display = 'none';
                break;

            case 'PAUSED':
                this.setStatus('<i class="fas fa-pause-circle text-warning"></i> Paused', 'paused');
                this.addLog('warning', '⏸ ' + update.message);
                // Show Resume button, hide Pause button
                const pauseBtn = document.getElementById('pauseBtn');
                const resumeBtn = document.getElementById('resumeBtn');
                if (pauseBtn) pauseBtn.style.display = 'none';
                if (resumeBtn) resumeBtn.style.display = 'inline-block';
                break;

            case 'COMPLETED':
                console.log('COMPLETED! Processing results...');
                this.setStatus('<i class="fas fa-check-circle text-success"></i> Completed!', 'success');
                this.addLog('success', '✓ ' + update.message);
                document.getElementById('executionControls').style.display = 'none';
                this.hideProgress();

                console.log('Checking results:', update.results);
                if (update.results && Object.keys(update.results).length > 0) {
                    console.log('Displaying', Object.keys(update.results).length, 'results');
                    this.displayResults(update.results);

                    // Switch to Results panel to show results
                    this.switchSidebarPanel('results');
                } else {
                    console.warn('No results in update!');
                    this.addLog('warning', '⚠ No results returned');
                }

                // v2.2: Store session info and token usage in active tab
                const tab = this.getActiveTab();
                if (tab) {
                    if (update.sessionInfo) {
                        console.log('Storing session info in tab:', update.sessionInfo);
                        tab.sessionInfo = update.sessionInfo;
                    }
                    if (update.tokenUsage) {
                        console.log('Storing token usage in tab:', update.tokenUsage);
                        tab.tokenUsage = update.tokenUsage;
                    }
                    // Render all panels
                    this.renderVariables();
                    this.renderSessionInfo();
                }

                // Close WebSocket after successful completion
                if (this.webSocket) {
                    console.log('Closing WebSocket after successful execution');
                    this.webSocket.close();
                    this.webSocket = null;
                }
                break;

            case 'FAILED':
                console.error('Execution FAILED:', update.message);
                this.setStatus('<i class="fas fa-times-circle text-error"></i> Failed', 'error');
                this.addLog('error', '✗ ' + update.message);
                document.getElementById('executionControls').style.display = 'none';
                this.hideProgress();

                // Close WebSocket after failure
                if (this.webSocket) {
                    console.log('Closing WebSocket after failed execution');
                    this.webSocket.close();
                    this.webSocket = null;
                }
                break;

            case 'STOPPED':
                this.setStatus('<i class="fas fa-stop-circle text-error"></i> Stopped', 'stopped');
                this.addLog('warning', '⏹ ' + update.message);
                document.getElementById('executionControls').style.display = 'none';
                this.hideProgress();

                // Close WebSocket after stop
                if (this.webSocket) {
                    console.log('Closing WebSocket after stopped execution');
                    this.webSocket.close();
                    this.webSocket = null;
                }
                break;

            default:
                console.warn('Unknown status:', update.status);
                this.addLog('info', update.message || 'Status: ' + update.status);
        }
    },

    /**
     * Pause execution
     */
    async pauseExecution() {
        if (!this.currentExecutionId) return;

        try {
            await fetch(`/api/execute/${this.currentExecutionId}/pause`, { method: 'POST' });
            this.addLog('warning', 'Pause requested...');
        } catch (error) {
            this.addLog('error', 'Failed to pause: ' + error.message);
        }
    },

    /**
     * Stop execution
     */
    async stopExecution() {
        if (!this.currentExecutionId) return;

        try {
            await fetch(`/api/execute/${this.currentExecutionId}/stop`, { method: 'POST' });
            this.addLog('warning', 'Stop requested...');
        } catch (error) {
            this.addLog('error', 'Failed to stop: ' + error.message);
        }
    },

    /**
     * Display execution results (v2.2: Stores in active tab)
     */
    displayResults(results) {
        console.log('=== displayResults called ===');
        console.log('Results type:', typeof results);
        console.log('Results keys:', results ? Object.keys(results).length : 0);

        // v2.2: Store in active tab (Apple HIG - per-document results)
        const tab = this.getActiveTab();
        if (tab) {
            tab.results = results || {};
            tab.status = 'COMPLETED';
            tab.lastUpdate = new Date();
        }

        // Render results
        this.renderResults();
    },



    /**
     * Switch result tab
     */
    switchResultTab(tabName) {
        document.querySelectorAll('.result-tab').forEach(btn => {
            btn.classList.toggle('active', btn.dataset.tab === tabName);
        });

        document.querySelectorAll('.result-panel').forEach(panel => {
            panel.classList.toggle('active', panel.id === tabName + 'Panel');
        });
    },

    /**
     * Show progress bar
     */
    showProgress(percent) {
        const progressBar = document.getElementById('progressBar');
        const progressFill = document.getElementById('progressFill');
        const progressText = document.getElementById('progressText');

        if (progressBar) progressBar.style.display = 'block';
        if (progressFill) progressFill.style.width = percent + '%';
        if (progressText) progressText.textContent = Math.round(percent) + '%';
    },

    /**
     * Hide progress bar
     */
    hideProgress() {
        const progressBar = document.getElementById('progressBar');
        if (progressBar) progressBar.style.display = 'none';
    },

    /**
     * Handle WebSocket disconnect
     */
    handleWebSocketDisconnect(executionId, code) {
        const codeMessages = {
            1001: 'Server is going away',
            1006: 'Connection lost abnormally',
            1008: 'Policy violation',
            1009: 'Message too large',
            1011: 'Server error'
        };

        const message = codeMessages[code] || `Connection closed (code: ${code})`;

        // Show notification to user
        this.showNotification(
            '⚠️ WebSocket Disconnected',
            `Connection was lost: ${message}\n\nClick Run again to execute a new configuration.`,
            'warning'
        );

        this.addLog('warning', `⚠️ WebSocket disconnected: ${message}`);
        this.setStatus('<i class="fas fa-exclamation-triangle text-warning"></i> Connection lost', 'warning');

        // Hide execution controls
        document.getElementById('executionControls').style.display = 'none';
        this.hideProgress();
    },

    /**
     * Show notification to user
     */
    showNotification(title, message, type = 'info') {
        // Create notification element
        const notification = document.createElement('div');
        notification.className = `notification notification-${type}`;
        notification.innerHTML = `
            <div class="notification-header">
                <strong>${title}</strong>
                <button class="notification-close" onclick="this.parentElement.parentElement.remove()">×</button>
            </div>
            <div class="notification-body">${message.replace(/\n/g, '<br>')}</div>
        `;

        // Add to page
        let container = document.getElementById('notificationContainer');
        if (!container) {
            container = document.createElement('div');
            container.id = 'notificationContainer';
            container.className = 'notification-container';
            document.body.appendChild(container);
        }

        container.appendChild(notification);

        // Auto-remove after 10 seconds (except errors)
        if (type !== 'error') {
            setTimeout(() => {
                if (notification.parentElement) {
                    notification.style.opacity = '0';
                    setTimeout(() => notification.remove(), 300);
                }
            }, 10000);
        }
    },

    /**
     * Handle log message from WebSocket
     */
    handleLogMessage(logMsg) {
        const levelMap = {
            'INFO': 'info',
            'SUCCESS': 'success',
            'WARNING': 'warning',
            'ERROR': 'error'
        };
        const type = levelMap[logMsg.level] || 'info';
        this.addLog(type, logMsg.message);
    },

    /**
     * Add log entry (v2.2: Stores in active tab)
     */
    addLog(type, message) {
        // v2.2: Store in active tab (Apple HIG - per-document logs)
        const tab = this.getActiveTab();
        if (tab) {
            const logEntry = {
                time: new Date().toLocaleTimeString(),
                type: type,
                message: message
            };
            tab.logs.push(logEntry);
        }

        // Render logs for active tab
        this.renderLogs();

        // Auto-show log panel if error
        if (type === 'error') {
            this.switchResultTab('log');
        }
    },

    /**
     * Clear results
     */
    clearResults() {
        // Clear results - with null checks for Safari compatibility
        const outputContent = document.getElementById('outputContent');
        if (outputContent) outputContent.textContent = 'Executing...';

        const variablesContent = document.getElementById('variablesContent');
        if (variablesContent) variablesContent.innerHTML = '';

        const logContent = document.getElementById('logContent');
        if (logContent) logContent.innerHTML = '';
    },

    /**
     * Set status message
     */
    setStatus(message, type) {
        const statusMessage = document.getElementById('statusMessage');
        if (statusMessage) statusMessage.innerHTML = message;
    },

    /**
     * Open file (placeholder)
     */
    openFile() {
        alert('File open dialog - to be implemented');
    },

    /**
     * Save file (placeholder)
     */
    saveFile() {
        const tab = this.tabs.find(t => t.id === this.activeTabId);
        if (tab) {
            tab.modified = false;
            this.renderTabs();
            this.setStatus('<i class="fas fa-check-circle text-success"></i> Saved!', 'success');
            setTimeout(() => {
                this.setStatus('<i class="fas fa-circle text-success"></i> Ready', 'ready');
            }, 2000);
        }
    },

    /**
     * Example 1: Simple HTTP + XPath
     */
    getHttpXPathExample() {
        return `<?xml version="1.0" encoding="UTF-8"?>
<config xmlns="http://org.webharvest/schema/2.1/core">
    <!-- Simple HTTP + XPath Example -->
    <!-- Fast test: httpbin.org (~3 seconds) -->
    
    <def var="url">https://httpbin.org/html</def>
    
    <def var="html">
        <http url="\${url}"/>
    </def>
    
    <def var="page">
        <html-to-xml>
            <get var="html"/>
        </html-to-xml>
    </def>
    
    <def var="title">
        <xpath expression="//title/text()">
            <get var="page"/>
        </xpath>
    </def>
    
    <def var="heading">
        <xpath expression="//h1/text()">
            <get var="page"/>
        </xpath>
    </def>
    
    <file action="write" path="target/test-output/http_xpath_result.txt">
        <template>Title: \${title}
Heading: \${heading}</template>
    </file>
</config>`;
    },

    /**
     * Example 2: Quotes Scraping
     */
    getQuotesExample() {
        return `<?xml version="1.0" encoding="UTF-8"?>
<config xmlns="http://org.webharvest/schema/2.1/core">
    <!-- Quotes Scraping - quotes.toscrape.com -->
    <!-- Made for scrapers - never blocks! (~10 seconds) -->
    
    <def var="url">https://quotes.toscrape.com/</def>
    
    <def var="html">
        <http url="\${url}"/>
    </def>
    
    <def var="page">
        <html-to-xml>
            <get var="html"/>
        </html-to-xml>
    </def>
    
    <!-- Extract all quotes (use /text() to get text content) -->
    <def var="quotes">
        <xpath expression="//div[@class='quote']//span[@class='text']/text()">
            <get var="page"/>
        </xpath>
    </def>
    
    <!-- Extract all authors (use /text() to get text content) -->
    <def var="authors">
        <xpath expression="//div[@class='quote']//small[@class='author']/text()">
            <get var="page"/>
        </xpath>
    </def>
    
    <file action="write" path="target/test-output/quotes.txt">
        <template>
=== QUOTES FROM \${url} ===

Quotes:
\${quotes}

Authors:
\${authors}
        </template>
    </file>
</config>`;
    },

    /**
     * Example 3: JSON API
     */
    getJsonApiExample() {
        return `<?xml version="1.0" encoding="UTF-8"?>
<config xmlns="http://org.webharvest/schema/2.1/core">
    <!-- JSON API Example - jsonplaceholder.typicode.com -->
    <!-- Public API - always available (~5 seconds) -->
    
    <def var="apiUrl">https://jsonplaceholder.typicode.com/users/1</def>
    
    <def var="jsonData">
        <http url="\${apiUrl}"/>
    </def>
    
    <def var="xmlData">
        <json-to-xml>
            <get var="jsonData"/>
        </json-to-xml>
    </def>
    
    <def var="userName">
        <xpath expression="//name/text()">
            <get var="xmlData"/>
        </xpath>
    </def>
    
    <def var="userEmail">
        <xpath expression="//email/text()">
            <get var="xmlData"/>
        </xpath>
    </def>
    
    <def var="userCity">
        <xpath expression="//address/city/text()">
            <get var="xmlData"/>
        </xpath>
    </def>
    
    <file action="write" path="target/test-output/user_data.txt">
        <template>
=== USER DATA ===
Name: \${userName}
Email: \${userEmail}
City: \${userCity}
        </template>
    </file>
</config>`;
    },

    /**
     * Example 4: Wikipedia
     */
    getWikipediaExample() {
        return `<?xml version="1.0" encoding="UTF-8"?>
<config xmlns="http://org.webharvest/schema/2.1/core">
    <!-- Wikipedia Example - polite scraping -->
    <!-- Educational use (~5 seconds) -->
    
    <def var="topic">Web_scraping</def>
    <def var="url">https://en.wikipedia.org/wiki/\${topic}</def>
    
    <def var="html">
        <http url="\${url}">
            <http-header name="User-Agent" value="Web-Harvest/2.2.0 Educational"/>
        </http>
    </def>
    
    <def var="page">
        <html-to-xml>
            <get var="html"/>
        </html-to-xml>
    </def>
    
    <def var="title">
        <xpath expression="//h1[@id='firstHeading']/text()">
            <get var="page"/>
        </xpath>
    </def>
    
    <def var="intro">
        <xpath expression="string(//div[@id='mw-content-text']//p[1])">
            <get var="page"/>
        </xpath>
    </def>
    
    <file action="write" path="target/test-output/wikipedia.txt">
        <template>
=== WIKIPEDIA: \${topic} ===

Title: \${title}

Intro: \${intro}
        </template>
    </file>
</config>`;
    },

    /**
     * Example 5: Data Pipeline
     */
    getDataPipelineExample() {
        return `<?xml version="1.0" encoding="UTF-8"?>
<config xmlns="http://org.webharvest/schema/2.1/core">
    <!-- Advanced Data Pipeline - Multiple APIs -->
    <!-- Shows: Multi-source aggregation (~15 seconds) -->
    
    <!-- Get user data -->
    <def var="userData">
        <http url="https://jsonplaceholder.typicode.com/users/1"/>
    </def>
    
    <!-- Get user's posts -->
    <def var="postsData">
        <http url="https://jsonplaceholder.typicode.com/posts?userId=1"/>
    </def>
    
    <!-- Parse both -->
    <def var="userXml">
        <json-to-xml>
            <get var="userData"/>
        </json-to-xml>
    </def>
    
    <def var="postsXml">
        <json-to-xml>
            <get var="postsData"/>
        </json-to-xml>
    </def>
    
    <!-- Extract user info (use /text() to get text content) -->
    <def var="name">
        <xpath expression="//name/text()">
            <get var="userXml"/>
        </xpath>
    </def>
    
    <def var="email">
        <xpath expression="//email/text()">
            <get var="userXml"/>
        </xpath>
    </def>
    
    <!-- Count posts (count function returns number) -->
    <def var="postCount">
        <xpath expression="count(//item)">
            <get var="postsXml"/>
        </xpath>
    </def>
    
    <!-- Get first post (use /text() to get text content) -->
    <def var="firstPost">
        <xpath expression="//item[1]/title/text()">
            <get var="postsXml"/>
        </xpath>
    </def>
    
    <!-- Generate report -->
    <file action="write" path="target/test-output/user_report.txt">
        <template>
=== USER ACTIVITY REPORT ===

User: \${name}
Email: \${email}
Posts: \${postCount}

Latest: \${firstPost}

Data aggregated from 2 API sources!
        </template>
    </file>
</config>`;
    },

    /**
     * Sidebar Panel Management (v2.2 - Apple HIG)
     */
    switchSidebarPanel(panelName) {
        console.log('Switching sidebar panel to:', panelName);

        // Update nav buttons
        document.querySelectorAll('.sidebar-nav-btn').forEach(btn => {
            btn.classList.toggle('active', btn.dataset.panel === panelName);
        });

        // Update panels
        document.querySelectorAll('.sidebar-panel').forEach(panel => {
            panel.classList.toggle('active', panel.id === panelName + 'Panel');
        });

        // Update active tab's state
        const tab = this.getActiveTab();
        if (tab) {
            tab.activeSidebarPanel = panelName;
        }
    },

    /**
     * Examples Modal Management
     */
    showExamplesModal() {
        console.log('Showing examples modal...');
        const modal = document.getElementById('examplesModal');
        modal.classList.add('active');
        this.loadExamplesList();
    },

    hideExamplesModal() {
        const modal = document.getElementById('examplesModal');
        modal.classList.remove('active');
    },

    async loadExamplesList() {
        const grid = document.getElementById('examplesGrid');

        // Example configurations
        const examples = [
            { file: 'simple_test.xml', title: 'Simple HTTP Test', icon: 'vial', category: 'Web & HTTP' },
            { file: 'amazon_test.xml', title: 'Amazon Scraper', icon: 'shopping-cart', category: 'Web & HTTP' },
            { file: 'xpath_test.xml', title: 'XPath Examples', icon: 'code-branch', category: 'Data Extraction' },
            { file: 'regexp_test.xml', title: 'Regex Matching', icon: 'search', category: 'Data Extraction' },
            { file: 'json_api.xml', title: 'JSON API', icon: 'file-code', category: 'Data Transformation' },
            { file: 'loop_pagination.xml', title: 'Loop & Pagination', icon: 'repeat', category: 'Control Flow' }
        ];

        grid.innerHTML = examples.map(ex => `
            <div class="example-card" data-file="${ex.file}" onclick="app.selectExample('${ex.file}')">
                <i class="fas fa-${ex.icon}"></i>
                <h4>${ex.title}</h4>
                <p>${ex.category}</p>
            </div>
        `).join('');
    },

    selectExample(filename) {
        document.querySelectorAll('.example-card').forEach(card => {
            card.classList.toggle('selected', card.dataset.file === filename);
        });
        this.selectedExample = filename;
    },

    async loadSelectedExample() {
        if (!this.selectedExample) {
            alert('Please select an example first');
            return;
        }

        try {
            const response = await fetch(`/examples/${this.selectedExample}`);
            const content = await response.text();
            this.addTab(this.selectedExample, content);
            this.hideExamplesModal();
            this.addLog('info', `Loaded example: ${this.selectedExample}`);
        } catch (error) {
            console.error('Failed to load example:', error);
            this.addLog('error', `Failed to load example: ${error.message}`);
        }
    },

    /**
     * Logs, Variables, Results Rendering
     */
    clearLogs() {
        const tab = this.getActiveTab();
        if (tab) {
            tab.logs = [];
            this.renderLogs();
            if (document.getElementById('logsBadge')) {
                document.getElementById('logsBadge').textContent = '0';
            }
        }
    },

    renderLogs() {
        const tab = this.getActiveTab();
        const container = document.getElementById('logsContent');
        const badge = document.getElementById('logsBadge');

        if (!container) return;

        if (!tab || !tab.logs || tab.logs.length === 0) {
            container.innerHTML = `
                <div class="empty-state">
                    <i class="fas fa-list-ul"></i>
                    <p>No logs yet</p>
                    <small>Logs will appear during execution</small>
                </div>`;
            // Update badge to 0 when no logs
            if (badge) badge.textContent = '0';
            return;
        }

        container.innerHTML = tab.logs.map(log => `
            <div class="log-entry ${log.type}">
                <span class="log-time">${log.time}</span>
                <span class="log-message">${this.escapeHtml(log.message)}</span>
            </div>
        `).join('');

        container.scrollTop = container.scrollHeight;

        // Update badge with log count
        if (badge) {
            badge.textContent = tab.logs.length;
        }
    },

    filterVariables(query) {
        const items = document.querySelectorAll('.variable-item');
        items.forEach(item => {
            const name = item.querySelector('.var-name').textContent;
            item.style.display = name.toLowerCase().includes(query.toLowerCase()) ? 'block' : 'none';
        });
    },

    renderResults() {
        console.log('[renderResults] Called');
        const tab = this.getActiveTab();
        console.log('[renderResults] Active tab:', tab);
        const container = document.getElementById('resultsContent');
        console.log('[renderResults] Container:', container);

        if (!container) {
            console.warn('[renderResults] Container not found!');
            return;
        }

        if (!tab || !tab.results || Object.keys(tab.results).length === 0) {
            console.log('[renderResults] No results to display');
            container.innerHTML = `
                <div class="empty-state">
                    <i class="fas fa-table"></i>
                    <p>No results yet</p>
                    <small>Results will appear after execution</small>
                </div>`;
            return;
        }

        const resultCount = Object.keys(tab.results).length;
        console.log('[renderResults] Rendering', resultCount, 'results');
        container.innerHTML = Object.entries(tab.results).map(([key, value]) => `
            <div class="result-item">
                <span class="result-key">${this.escapeHtml(key)}:</span>
                <span class="result-value">${this.escapeHtml(String(value))}</span>
            </div>
        `).join('');
        console.log('[renderResults] Done rendering');
    },

    renderVariables() {
        console.log('[renderVariables] Called');
        const tab = this.getActiveTab();
        const container = document.getElementById('variablesContent');
        const badge = document.getElementById('varsBadge');

        if (!container) {
            console.warn('[renderVariables] Container not found!');
            return;
        }

        if (!tab || !tab.results || Object.keys(tab.results).length === 0) {
            console.log('[renderVariables] No variables to display');
            container.innerHTML = `
                <div class="empty-state">
                    <i class="fas fa-box"></i>
                    <p>No variables yet</p>
                    <small>Variables will appear during execution</small>
                </div>`;
            if (badge) badge.textContent = '0';
            return;
        }

        const varCount = Object.keys(tab.results).length;
        console.log('[renderVariables] Rendering', varCount, 'variables');

        container.innerHTML = Object.entries(tab.results).map(([key, value]) => `
            <div class="var-item">
                <div class="var-name">${this.escapeHtml(key)}</div>
                <div class="var-value">${this.escapeHtml(String(value).substring(0, 200))}${String(value).length > 200 ? '...' : ''}</div>
            </div>
        `).join('');

        if (badge) badge.textContent = varCount;
        console.log('[renderVariables] Done, badge updated to:', varCount);
    },

    renderSessionInfo() {
        console.log('[renderSessionInfo] Called');
        const tab = this.getActiveTab();
        const container = document.getElementById('sessionContent');
        console.log('[renderSessionInfo] Container:', container);

        if (!container) {
            console.warn('[renderSessionInfo] Container not found!');
            return;
        }

        if (!tab || !tab.sessionInfo) {
            container.innerHTML = `
                <div class="empty-state">
                    <i class="fas fa-info-circle"></i>
                    <p>No session active</p>
                    <small>Session details will appear after execution</small>
                </div>
                <button class="btn btn-secondary" onclick="app.loadSessionHistory()">
                    <i class="fas fa-history"></i> View History
                </button>`;
            return;
        }

        const session = tab.sessionInfo;
        const duration = session.duration < 1000 ?
            `${session.duration}ms` :
            `${(session.duration / 1000).toFixed(2)}s`;

        container.innerHTML = `
            <div class="session-card">
                <h4>Current Session</h4>
                <div class="session-stat">
                    <span class="session-stat-label">ID:</span>
                    <span class="session-stat-value">${session.sessionId ? session.sessionId.substring(0, 8) + '...' : 'N/A'}</span>
                </div>
                <div class="session-stat">
                    <span class="session-stat-label">Status:</span>
                    <span class="session-stat-value status-${session.status ? session.status.toLowerCase() : 'unknown'}">
                        <i class="${this.getStatusIcon(session.status)}"></i> ${session.status || 'N/A'}
                    </span>
                </div>
                <div class="session-stat">
                    <span class="session-stat-label">Duration:</span>
                    <span class="session-stat-value">${duration}</span>
                </div>
                <div class="session-stat">
                    <span class="session-stat-label">Elements:</span>
                    <span class="session-stat-value">${session.elementsProcessed || 0}</span>
                </div>
                <div class="session-stat">
                    <span class="session-stat-label">Rate:</span>
                    <span class="session-stat-value">${session.processingRate ? session.processingRate.toFixed(2) : '0.00'} elem/s</span>
                </div>
            </div>`;

        // v2.2: HTTP Metrics (if available)
        if (session.httpMetrics) {
            const http = session.httpMetrics;
            const kbDown = (http.bytesDownloaded / 1024).toFixed(2);
            const kbUp = (http.bytesUploaded / 1024).toFixed(2);

            container.innerHTML += `
            <div class="session-card">
                <h4><i class="fas fa-network-wired"></i> HTTP Metrics</h4>
                <div class="session-stat">
                    <span class="session-stat-label">Requests:</span>
                    <span class="session-stat-value">${http.requests}</span>
                </div>
                <div class="session-stat">
                    <span class="session-stat-label">Downloaded:</span>
                    <span class="session-stat-value">${kbDown} KB</span>
                </div>
                <div class="session-stat">
                    <span class="session-stat-label">Uploaded:</span>
                    <span class="session-stat-value">${kbUp} KB</span>
                </div>
                <div class="session-stat">
                    <span class="session-stat-label">Avg Response:</span>
                    <span class="session-stat-value">${http.avgResponseTime} ms</span>
                </div>
            </div>`;
        }

        // v2.2: Plugin Breakdown (if available)
        if (session.pluginBreakdown) {
            const plugins = session.pluginBreakdown;
            const pluginList = Object.entries(plugins)
                .sort((a, b) => b[1] - a[1])  // Sort by time (longest first)
                .map(([name, timeMs]) => `
                    <div class="plugin-stat">
                        <span class="plugin-name"><i class="fas fa-puzzle-piece"></i> ${name}</span>
                        <span class="plugin-time">${timeMs}ms</span>
                        <div class="plugin-bar">
                            <div class="plugin-bar-fill" style="width: ${(timeMs / session.duration * 100).toFixed(1)}%"></div>
                        </div>
                    </div>
                `).join('');

            container.innerHTML += `
            <div class="session-card">
                <h4><i class="fas fa-chart-bar"></i> Plugin Breakdown</h4>
                <div class="plugin-breakdown">
                    ${pluginList}
                </div>
            </div>`;
        }

        container.innerHTML += `
            <button class="btn btn-secondary btn-block" onclick="app.loadSessionHistory()" style="margin-top: 1rem;">
                <i class="fas fa-history"></i> View All Sessions
            </button>
        `;
    },

    exportResults() {
        const tab = this.getActiveTab();
        if (!tab || !tab.results) {
            alert('No results to export');
            return;
        }

        const json = JSON.stringify(tab.results, null, 2);
        const blob = new Blob([json], { type: 'application/json' });
        const url = URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = `results-${tab.name}.json`;
        a.click();
        URL.revokeObjectURL(url);
    },

    showPluginHelp(pluginName) {
        const tooltip = document.getElementById('pluginTooltip');
        if (!tooltip) return;

        const title = document.getElementById('tooltipTitle');
        const description = document.getElementById('tooltipDescription');
        const link = document.getElementById('tooltipLink');

        const plugins = {
            'http': { title: 'HTTP Plugin', desc: 'HTTP/HTTPS request plugin', url: '/plugins/http.html' },
            'xpath': { title: 'XPath Plugin', desc: 'Extract data from XML using XPath', url: '/plugins/xpath.html' },
            'html-to-xml': { title: 'HTML to XML Plugin', desc: 'Convert HTML to XML', url: '/plugins/html-to-xml.html' }
        };

        const plugin = plugins[pluginName] || { title: pluginName, desc: 'Plugin documentation', url: '/plugins/' };

        title.textContent = plugin.title;
        description.textContent = plugin.desc;
        link.href = plugin.url;

        tooltip.style.display = 'block';
    },

    hideTooltip() {
        const tooltip = document.getElementById('pluginTooltip');
        if (tooltip) tooltip.style.display = 'none';
    },

    escapeHtml(text) {
        const div = document.createElement('div');
        div.textContent = text;
        return div.innerHTML;
    },

    /**
     * Load and display session execution history
     */
    async loadSessionHistory() {
        console.log('[loadSessionHistory] Loading session history...');

        try {
            const response = await fetch('/api/sessions/history?limit=20');

            if (!response.ok) {
                console.error('[loadSessionHistory] HTTP error:', response.status);
                alert('Failed to load session history');
                return;
            }

            const data = await response.json();
            console.log('[loadSessionHistory] Loaded', data.total, 'sessions');

            this.renderSessionHistory(data.sessions);

        } catch (error) {
            console.error('[loadSessionHistory] Error:', error);
            alert('Error loading session history: ' + error.message);
        }
    },

    /**
     * Render session history in the Session panel
     */
    renderSessionHistory(sessions) {
        console.log('[renderSessionHistory] Rendering', sessions.length, 'sessions');

        const container = document.getElementById('sessionContent');
        if (!container) {
            console.warn('[renderSessionHistory] Container not found!');
            return;
        }

        if (!sessions || sessions.length === 0) {
            container.innerHTML = `
                <div class="empty-state">
                    <i class="fas fa-history"></i>
                    <p>No session history</p>
                    <small>Execute some configurations to build history</small>
                </div>`;
            return;
        }

        let html = `
            <div class="session-history">
                <div class="session-history-header">
                    <h4><i class="fas fa-history"></i> Session History</h4>
                    <span class="session-count">${sessions.length} sessions</span>
                </div>
                <div class="session-history-list">`;

        sessions.forEach((session, index) => {
            const statusClass = session.status.toLowerCase();
            const statusIcon = this.getStatusIcon(session.status);
            const timestamp = new Date(session.timestamp).toLocaleString();
            const duration = session.duration < 1000 ?
                `${session.duration}ms` :
                `${(session.duration / 1000).toFixed(2)}s`;

            html += `
                <div class="session-history-item ${statusClass}" onclick="app.showSessionDetails(${index})">
                    <div class="session-history-header">
                        <span class="session-status">
                            <i class="${statusIcon}"></i> ${session.status}
                        </span>
                        <span class="session-time">${timestamp}</span>
                    </div>
                    <div class="session-history-details">
                        <div class="session-detail">
                            <i class="fas fa-file-code"></i>
                            <span>${session.configName}</span>
                        </div>
                        <div class="session-stats-row">
                            <div class="session-stat-mini">
                                <i class="fas fa-clock"></i>
                                ${duration}
                            </div>
                            <div class="session-stat-mini">
                                <i class="fas fa-cubes"></i>
                                ${session.elementsProcessed} elements
                            </div>
                            <div class="session-stat-mini">
                                <i class="fas fa-tachometer-alt"></i>
                                ${session.processingRate.toFixed(1)} elem/s
                            </div>
                        </div>
                    </div>
                    <div class="session-id-mini">
                        <i class="fas fa-fingerprint"></i>
                        ${session.sessionId.substring(0, 8)}...
                    </div>
                </div>`;
        });

        html += `
                </div>
                <button class="btn btn-secondary btn-block" onclick="app.loadSessionHistory()">
                    <i class="fas fa-sync"></i> Refresh
                </button>
            </div>`;

        container.innerHTML = html;

        // Store sessions for details view
        this.sessionHistory = sessions;
    },

    /**
     * Get icon for session status
     */
    getStatusIcon(status) {
        const icons = {
            'COMPLETED': 'fas fa-check-circle',
            'FAILED': 'fas fa-times-circle',
            'RUNNING': 'fas fa-spinner fa-spin',
            'PENDING': 'fas fa-clock',
            'CANCELLED': 'fas fa-ban'
        };
        return icons[status] || 'fas fa-question-circle';
    },

    /**
     * Show details for a specific session
     */
    /**
     * Pause current execution
     */
    async pauseExecution() {
        const tab = this.getActiveTab();
        if (!tab || !tab.executionId) {
            alert('No active execution to pause');
            return;
        }
        
        console.log('[pauseExecution] Pausing:', tab.executionId);
        
        try {
            const response = await fetch(`/api/execution/${tab.executionId}/pause`, {
                method: 'POST'
            });
            
            if (response.ok) {
                const result = await response.json();
                console.log('[pauseExecution] Paused:', result);
                this.addLog('info', '⏸ Execution paused');
            } else {
                console.error('[pauseExecution] Failed:', response.status);
                alert('Failed to pause execution');
            }
        } catch (error) {
            console.error('[pauseExecution] Error:', error);
            alert('Error pausing execution: ' + error.message);
        }
    },

    /**
     * Resume paused execution
     */
    async resumeExecution() {
        const tab = this.getActiveTab();
        if (!tab || !tab.executionId) {
            alert('No paused execution to resume');
            return;
        }
        
        console.log('[resumeExecution] Resuming:', tab.executionId);
        
        try {
            const response = await fetch(`/api/execution/${tab.executionId}/resume`, {
                method: 'POST'
            });
            
            if (response.ok) {
                const result = await response.json();
                console.log('[resumeExecution] Resumed:', result);
                this.addLog('info', '▶ Execution resumed');
            } else {
                console.error('[resumeExecution] Failed:', response.status);
                alert('Failed to resume execution');
            }
        } catch (error) {
            console.error('[resumeExecution] Error:', error);
            alert('Error resuming execution: ' + error.message);
        }
    },

    /**
     * Stop execution
     */
    async stopExecution() {
        const tab = this.getActiveTab();
        if (!tab || !tab.executionId) {
            alert('No execution to stop');
            return;
        }
        
        if (!confirm('Are you sure you want to stop this execution?')) {
            return;
        }
        
        console.log('[stopExecution] Stopping:', tab.executionId);
        
        try {
            const response = await fetch(`/api/execution/${tab.executionId}/stop`, {
                method: 'POST'
            });
            
            if (response.ok) {
                const result = await response.json();
                console.log('[stopExecution] Stopped:', result);
                this.addLog('warn', '⏹ Execution stopped by user');
            } else {
                console.error('[stopExecution] Failed:', response.status);
                alert('Failed to stop execution');
            }
        } catch (error) {
            console.error('[stopExecution] Error:', error);
            alert('Error stopping execution: ' + error.message);
        }
    },

    showSessionDetails(index) {
        if (!this.sessionHistory || !this.sessionHistory[index]) {
            console.warn('[showSessionDetails] Session not found:', index);
            return;
        }
        
        const session = this.sessionHistory[index];
        console.log('[showSessionDetails] Showing session:', session.sessionId);

        const container = document.getElementById('sessionContent');
        if (!container) return;

        const timestamp = new Date(session.timestamp).toLocaleString();
        const duration = session.duration < 1000 ?
            `${session.duration}ms` :
            `${(session.duration / 1000).toFixed(2)}s`;

        container.innerHTML = `
            <div class="session-details">
                <div class="session-details-header">
                    <button class="btn btn-sm" onclick="app.loadSessionHistory()">
                        <i class="fas fa-arrow-left"></i> Back to History
                    </button>
                    <h4><i class="fas fa-info-circle"></i> Session Details</h4>
                </div>
                
                <div class="session-card">
                    <h5>General</h5>
                    <div class="session-stat">
                        <span class="session-stat-label">ID:</span>
                        <span class="session-stat-value">${session.sessionId}</span>
                    </div>
                    <div class="session-stat">
                        <span class="session-stat-label">Config:</span>
                        <span class="session-stat-value">${session.configName}</span>
                    </div>
                    <div class="session-stat">
                        <span class="session-stat-label">Status:</span>
                        <span class="session-stat-value status-${session.status.toLowerCase()}">
                            <i class="${this.getStatusIcon(session.status)}"></i> ${session.status}
                        </span>
                    </div>
                    <div class="session-stat">
                        <span class="session-stat-label">Timestamp:</span>
                        <span class="session-stat-value">${timestamp}</span>
                    </div>
                </div>
                
                <div class="session-card">
                    <h5>Performance</h5>
                    <div class="session-stat">
                        <span class="session-stat-label">Duration:</span>
                        <span class="session-stat-value">${duration}</span>
                    </div>
                    <div class="session-stat">
                        <span class="session-stat-label">Elements:</span>
                        <span class="session-stat-value">${session.elementsProcessed}</span>
                    </div>
                    <div class="session-stat">
                        <span class="session-stat-label">Rate:</span>
                        <span class="session-stat-value">${session.processingRate.toFixed(2)} elem/s</span>
                    </div>
                </div>`;

        // v2.2: HTTP Metrics (if available)
        if (session.httpMetrics) {
            const http = session.httpMetrics;
            const kbDown = (http.bytesDownloaded / 1024).toFixed(2);
            const kbUp = (http.bytesUploaded / 1024).toFixed(2);

            container.innerHTML += `
                <div class="session-card">
                    <h5>HTTP Metrics</h5>
                    <div class="session-stat">
                        <span class="session-stat-label">Requests:</span>
                        <span class="session-stat-value">${http.requests}</span>
                    </div>
                    <div class="session-stat">
                        <span class="session-stat-label">Downloaded:</span>
                        <span class="session-stat-value">${kbDown} KB</span>
                    </div>
                    <div class="session-stat">
                        <span class="session-stat-label">Uploaded:</span>
                        <span class="session-stat-value">${kbUp} KB</span>
                    </div>
                    <div class="session-stat">
                        <span class="session-stat-label">Avg Response:</span>
                        <span class="session-stat-value">${http.avgResponseTime} ms</span>
                    </div>
                </div>`;
        }

        container.innerHTML += `
            </div>`;
    }
};

// Initialize IDE when DOM is ready
document.addEventListener('DOMContentLoaded', () => {
    app.init();
});

