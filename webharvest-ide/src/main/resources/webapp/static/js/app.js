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
        const tab = this.getActiveTab();
        if (!tab) return;

        const xml = this.editor.getValue();
        const tabName = tab.name;

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
            const response = await fetch('/api/execute', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({
                    configXml: xml,
                    configName: tabName,
                    waitForWebSocket: true  // Signal to wait
                })
            });

            const result = await response.json();

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
            tab.status = 'FAILED';
            this.setStatus('<i class="fas fa-times-circle text-error"></i> Execution failed: ' + error.message, 'error');
            this.addLog('error', '✗ Error: ' + error.message);
            this.hideProgress();
        }
    },

    /**
     * Connect WebSocket synchronously (wait for connection)
     */
    connectWebSocketSync(executionId) {
        return new Promise((resolve, reject) => {
            const wsUrl = `ws://${window.location.host}/ws/execution?id=${executionId}`;

            console.log('Connecting WebSocket to:', wsUrl);
            this.webSocket = new WebSocket(wsUrl);

            this.webSocket.onopen = () => {
                console.log('✓ WebSocket connected for execution:', executionId);
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
                console.error('❌ WebSocket error:', error);
                this.addLog('error', '✗ WebSocket connection error');
                reject(error);
            };

            this.webSocket.onclose = (event) => {
                console.log('🔌 WebSocket closed:', event.code, event.reason);

                // Check if this was unexpected disconnect (not normal closure)
                if (event.code !== 1000 && event.code !== 1005) {
                    console.warn('⚠️ Unexpected WebSocket disconnect! Code:', event.code);
                    this.handleWebSocketDisconnect(executionId, event.code);
                }
            };

            // Timeout after 5 seconds
            setTimeout(() => {
                if (this.webSocket.readyState !== WebSocket.OPEN) {
                    console.warn('WebSocket connection timeout');
                    reject(new Error('WebSocket connection timeout'));
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
                break;

            case 'PAUSED':
                this.setStatus('<i class="fas fa-pause-circle text-warning"></i> Paused', 'paused');
                this.addLog('warning', '⏸ ' + update.message);
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
                    // Render session info panel
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
     * Render results for active tab (v2.2: Apple HIG)
     */
    renderResults() {
        const tab = this.getActiveTab();

        try {
            // Show results tabs
            document.getElementById('resultsTabs').style.display = 'flex';
            document.getElementById('statusDisplay').style.display = 'none';

            const outputContent = document.getElementById('outputContent');
            const variablesContent = document.getElementById('variablesContent');

            if (!tab || !tab.results || Object.keys(tab.results).length === 0) {
                // No results - show empty state
                outputContent.innerHTML = '<p style="color: #666; padding: 20px;">No results yet. Click Run to execute this configuration.</p>';
                variablesContent.innerHTML = '<p style="color: #666; padding: 20px;">No variables yet.</p>';
                return;
            }

            // Display main output (configResult or first significant variable)
            const results = tab.results;
            const mainResult = results.configResult || results.html || results.response ||
                results.data || Object.values(results)[0] || 'No output';
            outputContent.textContent = mainResult;
            console.log('Output tab updated with main result (', mainResult.length, 'chars)');

            // Display variables
            variablesContent.innerHTML = '<p>Loading results...</p>';

            // Use setTimeout to avoid UI freeze
            setTimeout(() => {
                try {
                    variablesContent.innerHTML = '';

                    let varCount = 0;
                    const entries = Object.entries(results || {});
                    console.log('Processing', entries.length, 'variables...');

                    entries.forEach(([name, value]) => {
                        // Truncate if too long (max 300 chars for display)
                        const displayValue = value.length > 300
                            ? value.substring(0, 300) + `...\n(${value.length} total chars, truncated)`
                            : value;

                        const card = document.createElement('div');
                        card.className = 'variable-card';

                        const varName = document.createElement('div');
                        varName.className = 'variable-name';
                        varName.textContent = name;

                        const varValue = document.createElement('div');
                        varValue.className = 'variable-value';
                        varValue.textContent = displayValue;

                        card.appendChild(varName);
                        card.appendChild(varValue);
                        variablesContent.appendChild(card);
                        varCount++;
                    });

                    console.log('✓ Created', varCount, 'variable cards');
                    this.addLog('success', `✓ Displayed ${varCount} variables`);

                    // Switch to output tab to show main result
                    this.switchResultTab('output');

                } catch (err) {
                    console.error('Error creating variable cards:', err);
                    variablesContent.innerHTML = '<p class="error">Error displaying results: ' + err.message + '</p>';
                }
            }, 100); // Small delay to avoid UI freeze

        } catch (err) {
            console.error('Error in renderResults:', err);
            this.addLog('error', 'Failed to display results: ' + err.message);
        }
    },

    /**
     * Render session info for active tab (v2.2: New!)
     */
    renderSessionInfo() {
        const tab = this.getActiveTab();
        const sessionPanel = document.getElementById('sessionPanel');

        if (!sessionPanel) return;

        if (!tab || !tab.sessionInfo) {
            sessionPanel.innerHTML = `
                <div style="padding: 20px; color: #666;">
                    <p>No session information available.</p>
                    <p style="font-size: 0.9em; margin-top: 10px;">
                        Session data will appear here after running a configuration.
                    </p>
                </div>
            `;
            return;
        }

        const session = tab.sessionInfo;
        const tokens = tab.tokenUsage || {};

        sessionPanel.innerHTML = `
            <div class="session-info-container">
                <div class="session-info-card">
                    <h3><i class="fas fa-info-circle"></i> Session Information</h3>
                    <div class="info-grid">
                        <div class="info-item">
                            <label>Session ID:</label>
                            <span>${session.sessionId ? session.sessionId.substring(0, 8) + '...' : 'N/A'}</span>
                        </div>
                        <div class="info-item">
                            <label>Client ID:</label>
                            <span>${session.clientId || 'N/A'}</span>
                        </div>
                        <div class="info-item">
                            <label>Status:</label>
                            <span class="status-badge status-${session.status ? session.status.toLowerCase() : 'unknown'}">${session.status || 'UNKNOWN'}</span>
                        </div>
                        <div class="info-item">
                            <label>Duration:</label>
                            <span>${session.duration ? (session.duration / 1000).toFixed(2) + 's' : 'N/A'}</span>
                        </div>
                        <div class="info-item">
                            <label>Elements Processed:</label>
                            <span>${session.elementsProcessed || 0}</span>
                        </div>
                        <div class="info-item">
                            <label>Processing Rate:</label>
                            <span>${session.processingRate ? session.processingRate.toFixed(2) + ' elem/s' : 'N/A'}</span>
                        </div>
                    </div>
                </div>
                
                <div class="token-usage-card">
                    <h3><i class="fas fa-coins"></i> Token Usage</h3>
                    <div class="info-grid">
                        <div class="info-item">
                            <label>HTTP Requests:</label>
                            <span>${tokens.httpRequests || 0}</span>
                        </div>
                        <div class="info-item">
                            <label>HTTP Bytes:</label>
                            <span>${tokens.httpBytes ? (tokens.httpBytes / 1024).toFixed(2) + ' KB' : '0 KB'}</span>
                        </div>
                        <div class="info-item">
                            <label>CPU Time:</label>
                            <span>${tokens.cpuTimeMs || 0} ms</span>
                        </div>
                        <div class="info-item">
                            <label>Memory Peak:</label>
                            <span>${tokens.memoryPeakBytes ? (tokens.memoryPeakBytes / 1024 / 1024).toFixed(2) + ' MB' : '0 MB'}</span>
                        </div>
                    </div>
                </div>
            </div>
        `;
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

        progressBar.style.display = 'block';
        progressFill.style.width = percent + '%';
        progressText.textContent = Math.round(percent) + '%';
    },

    /**
     * Hide progress bar
     */
    hideProgress() {
        document.getElementById('progressBar').style.display = 'none';
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
     * Render logs for active tab (v2.2: Apple HIG)
     */
    renderLogs() {
        const tab = this.getActiveTab();
        const logContent = document.getElementById('logContent');

        if (!logContent) return;

        logContent.innerHTML = '';

        if (tab && tab.logs && tab.logs.length > 0) {
            tab.logs.forEach(log => {
                const entry = document.createElement('div');
                entry.className = 'log-entry ' + log.type;
                entry.textContent = `[${log.time}] ${log.message}`;
                logContent.appendChild(entry);
            });
            logContent.scrollTop = logContent.scrollHeight;
        } else {
            logContent.innerHTML = '<p style="color: #666; padding: 20px;">No logs yet. Click Run to execute this configuration.</p>';
        }
    },

    /**
     * Clear results
     */
    clearResults() {
        document.getElementById('outputContent').textContent = 'Executing...';
        document.getElementById('variablesContent').innerHTML = '';
        document.getElementById('logContent').innerHTML = '';
    },

    /**
     * Set status message
     */
    setStatus(message, type) {
        document.getElementById('statusMessage').innerHTML = message;
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
        
        if (!container) return;
        
        if (!tab || !tab.logs || tab.logs.length === 0) {
            container.innerHTML = `
                <div class="empty-state">
                    <i class="fas fa-list-ul"></i>
                    <p>No logs yet</p>
                    <small>Logs will appear during execution</small>
                </div>`;
            return;
        }
        
        container.innerHTML = tab.logs.map(log => `
            <div class="log-entry ${log.type}">
                <span class="log-time">${log.time}</span>
                <span class="log-message">${this.escapeHtml(log.message)}</span>
            </div>
        `).join('');
        
        container.scrollTop = container.scrollHeight;
        
        if (document.getElementById('logsBadge')) {
            document.getElementById('logsBadge').textContent = tab.logs.length;
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
        const tab = this.getActiveTab();
        const container = document.getElementById('resultsContent');
        
        if (!container) return;
        
        if (!tab || !tab.results || Object.keys(tab.results).length === 0) {
            container.innerHTML = `
                <div class="empty-state">
                    <i class="fas fa-table"></i>
                    <p>No results yet</p>
                    <small>Results will appear after execution</small>
                </div>`;
            return;
        }
        
        container.innerHTML = Object.entries(tab.results).map(([key, value]) => `
            <div class="result-item">
                <span class="result-key">${this.escapeHtml(key)}:</span>
                <span class="result-value">${this.escapeHtml(String(value))}</span>
            </div>
        `).join('');
    },

    renderSessionInfo() {
        const tab = this.getActiveTab();
        const container = document.getElementById('sessionContent');
        
        if (!container) return;
        
        if (!tab || !tab.sessionInfo) {
            container.innerHTML = `
                <div class="empty-state">
                    <i class="fas fa-info-circle"></i>
                    <p>No session active</p>
                    <small>Session details will appear after execution</small>
                </div>`;
            return;
        }
        
        const session = tab.sessionInfo;
        container.innerHTML = `
            <div class="session-card">
                <h4>Session</h4>
                <div class="session-stat">
                    <span class="session-stat-label">ID:</span>
                    <span class="session-stat-value">${session.sessionId || 'N/A'}</span>
                </div>
                <div class="session-stat">
                    <span class="session-stat-label">Status:</span>
                    <span class="session-stat-value">${session.status || 'N/A'}</span>
                </div>
            </div>
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
    }
};

// Initialize IDE when DOM is ready
document.addEventListener('DOMContentLoaded', () => {
    app.init();
});

