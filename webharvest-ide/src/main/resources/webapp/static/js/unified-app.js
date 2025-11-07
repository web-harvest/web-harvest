/**
 * Unified WebHarvest IDE - Clean Implementation
 * Apple HIG compliant, professional, maintainable
 */

const unifiedApp = {
    // State
    tabs: [],
    activeTabId: null,
    viewMode: 'xml',  // 'xml' or 'visual'
    editor: null,
    plugins: [],

    /**
     * Initialize application
     */
    async init() {
        console.log('🚀 Initializing Unified IDE...');

        // Create initial tab
        this.addTab();

        // Initialize Monaco Editor
        this.initMonaco();

        // Load plugins
        await this.loadPlugins();

        // Setup event listeners
        this.setupEvents();

        console.log('✅ Unified IDE initialized');
    },

    /**
     * Initialize Monaco Editor
     */
    initMonaco() {
        require.config({ paths: { vs: 'https://cdnjs.cloudflare.com/ajax/libs/monaco-editor/0.44.0/min/vs' } });

        require(['vs/editor/editor.main'], () => {
            const tab = this.getActiveTab();

            this.editor = monaco.editor.create(document.getElementById('monaco-editor'), {
                value: tab ? tab.content : this.getDefaultXML(),
                language: 'xml',
                theme: 'vs-dark',
                automaticLayout: true,
                fontSize: 14,
                minimap: { enabled: true },
                wordWrap: 'on'
            });

            // Update cursor position
            this.editor.onDidChangeCursorPosition((e) => {
                document.getElementById('cursor-pos').textContent =
                    `Ln ${e.position.lineNumber}, Col ${e.position.column}`;
            });

            // Auto-save to tab
            this.editor.onDidChangeModelContent(() => {
                const tab = this.getActiveTab();
                if (tab) {
                    tab.content = this.editor.getValue();
                }
            });

            console.log('✅ Monaco Editor ready');
        });
    },

    /**
     * Load plugins from API
     */
    async loadPlugins() {
        console.log('📦 Loading plugins from API...');

        try {
            const response = await fetch('/api/plugins');
            const data = await response.json();
            this.plugins = data.plugins || [];

            console.log(`✅ Loaded ${this.plugins.length} plugins`);

            // Render plugin list
            this.renderPlugins();

        } catch (error) {
            console.error('❌ Failed to load plugins:', error);
            document.getElementById('plugin-list').innerHTML = `
                <div class="empty-state">
                    <i class="fas fa-exclamation-triangle"></i>
                    <p>Failed to load plugins</p>
                    <small>${error.message}</small>
                </div>
            `;
        }
    },

    /**
     * Render plugin list in palette
     */
    renderPlugins() {
        const container = document.getElementById('plugin-list');

        if (this.plugins.length === 0) {
            container.innerHTML = '<div class="empty-state"><p>No plugins found</p></div>';
            return;
        }

        let html = '';
        for (const plugin of this.plugins) {
            html += `
                <div class="plugin-item" draggable="true" data-plugin="${plugin.id}">
                    <div class="plugin-header">
                        <div class="plugin-icon">
                            <i class="fas ${plugin.icon}"></i>
                        </div>
                        <div class="plugin-name">${plugin.name}</div>
                    </div>
                    <div class="plugin-desc">${plugin.description}</div>
                </div>
            `;
        }

        container.innerHTML = html;
        console.log('✅ Rendered plugin palette');
    },

    /**
     * Switch between XML and Visual modes
     */
    setViewMode(mode) {
        console.log(`Switching to ${mode} mode`);
        this.viewMode = mode;

        const leftSidebar = document.getElementById('left-sidebar');
        const xmlEditor = document.getElementById('xml-editor');
        const visualCanvas = document.getElementById('visual-canvas');
        const xmlSidebar = document.getElementById('xml-sidebar');
        const visualSidebar = document.getElementById('visual-sidebar');

        if (mode === 'visual') {
            // Parse XML
            const tab = this.getActiveTab();
            if (tab && this.editor) {
                tab.content = this.editor.getValue();

                try {
                    const parser = new SimpleXMLParser();
                    tab.visualNodes = parser.parse(tab.content);
                    console.log(`✅ Parsed ${tab.visualNodes.length} nodes`);
                } catch (e) {
                    console.error('Parse error:', e);
                    tab.visualNodes = [];
                }
            }

            // Show Visual UI
            leftSidebar.style.display = 'flex';
            leftSidebar.setAttribute('data-visible', 'true');
            xmlEditor.style.display = 'none';
            visualCanvas.style.display = 'block';
            xmlSidebar.style.display = 'none';
            visualSidebar.style.display = 'block';

            // Render nodes
            this.renderVisualNodes(tab.visualNodes || []);

        } else {
            // Generate XML if needed
            const tab = this.getActiveTab();
            if (tab && tab.visualNodes && tab.visualNodes.length > 0) {
                try {
                    const generator = new SimpleXMLGenerator();
                    tab.content = '<?xml version="1.0" encoding="UTF-8"?>\n<config>\n' +
                        generator.generate(tab.visualNodes, 1) +
                        '</config>';
                    if (this.editor) {
                        this.editor.setValue(tab.content);
                    }
                    console.log('✅ Generated XML');
                } catch (e) {
                    console.error('Generation error:', e);
                }
            }

            // Show XML UI
            leftSidebar.style.display = 'none';
            leftSidebar.setAttribute('data-visible', 'false');
            xmlEditor.style.display = 'flex';
            visualCanvas.style.display = 'none';
            xmlSidebar.style.display = 'flex';
            visualSidebar.style.display = 'none';
        }

        // Update buttons
        document.querySelectorAll('.view-btn').forEach(btn => {
            btn.classList.toggle('active', btn.dataset.view === mode);
        });

        // Update status
        document.getElementById('view-mode-indicator').textContent = mode.toUpperCase();

        console.log(`✅ Switched to ${mode} mode`);
    },

    /**
     * Render visual nodes
     */
    renderVisualNodes(nodes) {
        const container = document.getElementById('canvas-area');

        if (!container) return;

        if (!this.visualRenderer) {
            this.visualRenderer = new VisualRenderer('canvas-area');
        }

        this.visualRenderer.render(nodes);
    },

    /**
     * Tab management
     */
    addTab() {
        const id = 'tab-' + Date.now();
        const tab = {
            id: id,
            name: `config-${this.tabs.length + 1}.xml`,
            content: this.getDefaultXML(),
            viewMode: 'xml',
            visualNodes: []
        };

        this.tabs.push(tab);
        this.activeTabId = id;
        this.renderTabs();

        if (this.editor) {
            this.editor.setValue(tab.content);
        }
    },

    getActiveTab() {
        return this.tabs.find(t => t.id === this.activeTabId);
    },

    renderTabs() {
        const container = document.getElementById('tabs-container');
        container.innerHTML = this.tabs.map(tab => `
            <div class="tab ${tab.id === this.activeTabId ? 'active' : ''}" 
                 onclick="unifiedApp.switchTab('${tab.id}')">
                <span>${tab.name}</span>
                <i class="fas fa-times tab-close" onclick="event.stopPropagation(); unifiedApp.closeTab('${tab.id}')"></i>
            </div>
        `).join('');
    },

    switchTab(id) {
        this.activeTabId = id;
        const tab = this.getActiveTab();
        if (tab && this.editor) {
            this.editor.setValue(tab.content);
            this.setViewMode(tab.viewMode);
        }
        this.renderTabs();
    },

    closeTab(id) {
        const index = this.tabs.findIndex(t => t.id === id);
        if (index > -1) {
            this.tabs.splice(index, 1);
            if (this.tabs.length === 0) {
                this.addTab();
            } else {
                this.activeTabId = this.tabs[0].id;
                this.switchTab(this.activeTabId);
            }
        }
    },

    /**
     * Actions
     */
    newFile() {
        this.addTab();
    },

    openFile() {
        alert('Open file - TODO');
    },

    saveFile() {
        const tab = this.getActiveTab();
        if (tab) {
            console.log('Saving:', tab.name, tab.content.length, 'chars');
            alert('Save functionality - TODO');
        }
    },

    run() {
        const tab = this.getActiveTab();
        if (tab) {
            console.log('Running config:', tab.name);
            alert('Run functionality - using existing WebSocket code');
        }
    },

    validate() {
        alert('Validate - TODO');
    },

    showPanel(name) {
        document.querySelectorAll('.panel-tab').forEach(t => {
            t.classList.toggle('active', t.dataset.panel === name);
        });
        document.querySelectorAll('.panel').forEach(p => {
            p.classList.toggle('active', p.id === name + '-panel');
        });
    },

    /**
     * Setup events
     */
    setupEvents() {
        // Keyboard shortcuts
        document.addEventListener('keydown', (e) => {
            if ((e.metaKey || e.ctrlKey) && e.key === 's') {
                e.preventDefault();
                this.saveFile();
            }
            if ((e.metaKey || e.ctrlKey) && e.key === 'r') {
                e.preventDefault();
                this.run();
            }
        });
    },

    /**
     * Default XML template
     */
    getDefaultXML() {
        return `<?xml version="1.0" encoding="UTF-8"?>
<config>
    <def var="url">https://example.com</def>
    
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
</config>`;
    }
};

// Initialize on DOM ready
document.addEventListener('DOMContentLoaded', () => {
    unifiedApp.init();
});







