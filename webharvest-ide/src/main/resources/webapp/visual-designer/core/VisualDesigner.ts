/**
 * WebHarvest Visual Designer - Main Application Class
 * 
 * This class orchestrates all components of the visual designer:
 * - Canvas (Rete.js)
 * - Plugin Palette
 * - Property Panel
 * - Configuration Synchronizer
 * 
 * @author WebHarvest Team
 * @version 2.2.0
 */

import { EventEmitter } from '../utils/EventEmitter';
import { PluginRegistry } from './PluginRegistry';
import { renderIcon } from '../utils/IconRenderer.js';
import { ContainerManager } from './ContainerManager.js';
import { OutlinePanel } from './OutlinePanel.js';
import { PropertyPanel } from './PropertyPanel.js';
import type { VisualNode } from '../types/VisualNode';

export interface VisualDesignerOptions {
  apiBaseUrl?: string;
}

export class VisualDesigner extends EventEmitter {
  private options: VisualDesignerOptions;
  private pluginRegistry: PluginRegistry | null = null;
  private outlinePanel: OutlinePanel | null = null;
  private propertyPanel: PropertyPanel | null = null;

  // UI Component references
  private paletteContainer: HTMLElement | null = null;
  private canvasContainer: HTMLElement | null = null;
  private propertyContainer: HTMLElement | null = null;

  // State
  private currentView: 'xml' | 'split' | 'visual' = 'visual';
  private zoomLevel: number = 1.0;
  private containerManager: ContainerManager = new ContainerManager();
  private nodeIdCounter: number = 1;

  constructor(_container: HTMLElement, options: VisualDesignerOptions = {}) {
    super();

    this.options = {
      apiBaseUrl: options.apiBaseUrl || '/api',
      ...options
    };

    console.log('🎨 Initializing Visual Designer...');

    this.initialize();
  }

  /**
   * Initialize all components
   */
  private async initialize(): Promise<void> {
    try {
      // Get UI element references
      this.paletteContainer = document.getElementById('palette-container');
      this.canvasContainer = document.getElementById('canvas-container');
      this.propertyContainer = document.getElementById('property-container');

      if (!this.paletteContainer || !this.canvasContainer || !this.propertyContainer) {
        throw new Error('Required UI elements not found');
      }

      // Setup event handlers
      this.setupEventHandlers();

      // Initialize outline panel
      this.outlinePanel = new OutlinePanel('outline-container', (nodeId) => {
        this.selectNode(nodeId);
      });

      // Initialize property panel
      this.propertyPanel = new PropertyPanel('property-container', (nodeId, config, body) => {
        this.updateNodeProperties(nodeId, config, body);
      });

      // Load plugin metadata from backend
      await this.loadPluginMetadata();

      // Populate plugin palette
      this.populatePluginPalette();

      // Setup canvas drop handling
      this.setupCanvasDropHandling();

      // Update status
      this.updateStatus('Ready', 'success');

      console.log('✅ Visual Designer initialization complete');

    } catch (error) {
      console.error('❌ Initialization failed:', error);
      this.updateStatus('Initialization failed', 'error');
      throw error;
    }
  }

  /**
   * Setup global event handlers
   */
  private setupEventHandlers(): void {
    // View switcher buttons
    document.querySelectorAll('.btn-view').forEach(button => {
      button.addEventListener('click', (e) => {
        const target = e.target as HTMLElement;
        const view = target.dataset.view as 'xml' | 'split' | 'visual';
        if (view) {
          this.switchView(view);
        }
      });
    });

    // Toolbar actions
    document.querySelectorAll('[data-action]').forEach(button => {
      button.addEventListener('click', (e) => {
        const target = e.target as HTMLElement;
        const action = target.dataset.action || target.closest('[data-action]')?.getAttribute('data-action');
        if (action) {
          this.handleToolbarAction(action);
        }
      });
    });

    // Plugin search
    const searchInput = document.getElementById('plugin-search') as HTMLInputElement;
    if (searchInput) {
      searchInput.addEventListener('input', (e) => {
        const query = (e.target as HTMLInputElement).value;
        this.filterPlugins(query);
      });
    }

    // Keyboard shortcuts
    document.addEventListener('keydown', (e) => {
      this.handleKeyboardShortcut(e);
    });

    console.log('✅ Event handlers registered');
  }

  /**
   * Load plugin metadata from backend
   */
  private async loadPluginMetadata(): Promise<void> {
    try {
      console.log('📡 Loading plugin metadata from backend...');

      const response = await fetch(`${this.options.apiBaseUrl}/plugins/metadata`);

      if (!response.ok) {
        throw new Error(`HTTP ${response.status}: ${response.statusText}`);
      }

      const data = await response.json();

      if (!data.plugins || !Array.isArray(data.plugins)) {
        throw new Error('Invalid response format: missing plugins array');
      }

      console.log(`✅ Loaded ${data.plugins.length} plugins`);

      // Initialize plugin registry
      this.pluginRegistry = new PluginRegistry(data.plugins);

    } catch (error) {
      console.warn('⚠️ Failed to load plugin metadata from backend:', error);
      console.log('📝 Using mock plugin data for development...');

      // Use mock data for development
      this.pluginRegistry = new PluginRegistry(this.getMockPlugins());
    }
  }

  /**
   * Populate the plugin palette UI
   */
  private populatePluginPalette(): void {
    if (!this.paletteContainer || !this.pluginRegistry) {
      console.error('❌ Cannot populate palette: missing container or registry');
      return;
    }

    const content = this.paletteContainer.querySelector('#palette-content');
    if (!content) return;

    // Get categories
    const categories = this.pluginRegistry.getCategories();

    if (categories.length === 0) {
      content.innerHTML = `
        <div class="palette-empty">
          <p>No plugins available</p>
        </div>
      `;
      return;
    }

    // Build category sections
    let html = '';

    for (const category of categories) {
      const plugins = this.pluginRegistry.getPluginsByCategory(category);

      html += `
        <div class="plugin-category">
          <div class="category-header">
            <span class="category-toggle">▼</span>
            <span class="category-name">${category}</span>
            <span class="category-count">(${plugins.length})</span>
          </div>
          <div class="category-plugins">
            ${plugins.map(plugin => `
              <div 
                class="plugin-item" 
                data-plugin="${plugin.elementName}"
                draggable="true"
                title="${plugin.description}"
              >
                <span class="plugin-icon">${renderIcon(plugin.icon) || '⚙️'}</span>
                <span class="plugin-name">${plugin.name}</span>
              </div>
            `).join('')}
          </div>
        </div>
      `;
    }

    content.innerHTML = html;

    // Setup category toggle handlers
    content.querySelectorAll('.category-header').forEach(header => {
      header.addEventListener('click', () => {
        const category = header.parentElement;
        category?.classList.toggle('collapsed');
      });
    });

    // Setup drag handlers
    content.querySelectorAll('.plugin-item').forEach(item => {
      item.addEventListener('dragstart', (e: Event) => {
        const dragEvent = e as DragEvent;
        const pluginType = (dragEvent.target as HTMLElement).dataset.plugin;
        if (dragEvent.dataTransfer && pluginType) {
          dragEvent.dataTransfer.setData('plugin-type', pluginType);
          dragEvent.dataTransfer.effectAllowed = 'copy';
        }
      });
    });

    console.log(`✅ Populated palette with ${categories.length} categories`);
  }

  /**
   * Filter plugins by search query
   */
  private filterPlugins(query: string): void {
    if (!this.pluginRegistry) return;

    const plugins = query.trim()
      ? this.pluginRegistry.search(query)
      : this.pluginRegistry.getAllPlugins();

    // TODO: Update palette to show filtered results
    console.log(`🔍 Search: "${query}" - Found ${plugins.length} plugins`);
  }

  /**
   * Switch between view modes
   */
  private switchView(view: 'xml' | 'split' | 'visual'): void {
    this.currentView = view;

    // Update button states
    document.querySelectorAll('.btn-view').forEach(btn => {
      btn.classList.toggle('active', btn.getAttribute('data-view') === view);
    });

    // TODO: Show/hide appropriate panels
    console.log(`📐 Switched to ${view} view`);

    this.updateStatus(`${view.charAt(0).toUpperCase() + view.slice(1)} View`, 'info');
  }

  /**
   * Handle toolbar actions
   */
  private handleToolbarAction(action: string): void {
    console.log(`🔧 Toolbar action: ${action}`);

    switch (action) {
      case 'undo':
        // TODO: Implement undo
        console.log('↶ Undo');
        break;

      case 'redo':
        // TODO: Implement redo
        console.log('↷ Redo');
        break;

      case 'zoom-in':
        this.zoom(0.1);
        break;

      case 'zoom-out':
        this.zoom(-0.1);
        break;

      case 'fit':
        this.zoomLevel = 1.0;
        this.updateZoomIndicator();
        // TODO: Fit canvas to view
        break;

      case 'auto-layout':
        // TODO: Implement auto-layout
        console.log('↻ Auto-layout');
        break;

      case 'run':
        // TODO: Implement execution
        console.log('▶ Run configuration');
        this.updateStatus('Running...', 'running');
        break;

      default:
        console.warn(`⚠️ Unknown action: ${action}`);
    }
  }

  /**
   * Handle keyboard shortcuts
   */
  private handleKeyboardShortcut(e: KeyboardEvent): void {
    const isMac = navigator.platform.toUpperCase().indexOf('MAC') >= 0;
    const modKey = isMac ? e.metaKey : e.ctrlKey;

    if (!modKey) return;

    // Check for shortcuts
    switch (e.key.toLowerCase()) {
      case 'k':
        e.preventDefault();
        console.log('⌘K - Quick add plugin');
        // TODO: Show quick add dialog
        break;

      case 'z':
        e.preventDefault();
        if (e.shiftKey) {
          this.handleToolbarAction('redo');
        } else {
          this.handleToolbarAction('undo');
        }
        break;

      case 's':
        e.preventDefault();
        console.log('⌘S - Save configuration');
        // TODO: Save configuration
        break;

      case '=':
      case '+':
        e.preventDefault();
        this.zoom(0.1);
        break;

      case '-':
        e.preventDefault();
        this.zoom(-0.1);
        break;

      case '0':
        e.preventDefault();
        this.zoomLevel = 1.0;
        this.updateZoomIndicator();
        break;
    }
  }

  /**
   * Zoom canvas
   */
  private zoom(delta: number): void {
    this.zoomLevel = Math.max(0.1, Math.min(3.0, this.zoomLevel + delta));
    this.updateZoomIndicator();
    // TODO: Apply zoom to canvas
  }

  /**
   * Update zoom indicator
   */
  private updateZoomIndicator(): void {
    const indicator = document.getElementById('zoom-indicator');
    if (indicator) {
      indicator.textContent = `${Math.round(this.zoomLevel * 100)}%`;
    }
  }

  /**
   * Update status bar
   */
  private updateStatus(message: string, type: 'success' | 'error' | 'info' | 'running' = 'info'): void {
    const statusText = document.querySelector('.status-text');
    const statusDot = document.querySelector('.status-dot') as HTMLElement;

    if (statusText) {
      statusText.textContent = message;
    }

    if (statusDot) {
      const colors = {
        success: '#43A047',
        error: '#E53935',
        info: '#7E57C2',
        running: '#FFC107'
      };
      statusDot.style.background = colors[type];
    }
  }

  /**
   * Get mock plugin data for development
   */
  private getMockPlugins(): any[] {
    return [
      {
        id: 'http',
        name: 'HTTP Request',
        elementName: 'http',
        description: 'Performs HTTP requests to retrieve web pages',
        category: 'HTTP & Web',
        icon: '🌐',
        version: '2.2.0',
        author: 'WebHarvest Team',
        tags: ['http', 'web', 'request'],
        helpUrl: 'https://web-harvest.sourceforge.io/documentation/plugin-http.html',
        schema: {
          attributes: [
            { name: 'url', type: 'string', required: true, description: 'Target URL' },
            { name: 'method', type: 'enum', required: false, defaultValue: 'GET', options: ['GET', 'POST', 'PUT', 'DELETE'], description: 'HTTP method' },
            { name: 'timeout', type: 'number', required: false, defaultValue: 30000, min: 0, max: 300000, description: 'Request timeout in milliseconds' }
          ],
          allowsBody: true,
          allowedSubElements: ['http-header', 'http-param']
        }
      },
      {
        id: 'xpath',
        name: 'XPath Query',
        elementName: 'xpath',
        description: 'Extract data using XPath expressions',
        category: 'Data Extraction',
        icon: '🔍',
        version: '2.2.0',
        author: 'WebHarvest Team',
        tags: ['xpath', 'xml', 'extraction'],
        helpUrl: 'https://web-harvest.sourceforge.io/documentation/plugin-xpath.html',
        schema: {
          attributes: [
            { name: 'expression', type: 'string', required: true, description: 'XPath expression' }
          ],
          allowsBody: true,
          allowedSubElements: []
        }
      },
      {
        id: 'def',
        name: 'Define Variable',
        elementName: 'def',
        description: 'Define or update a variable',
        category: 'Variables',
        icon: '📦',
        version: '2.2.0',
        author: 'WebHarvest Team',
        tags: ['variable', 'def', 'define'],
        helpUrl: 'https://web-harvest.sourceforge.io/documentation/plugin-def.html',
        schema: {
          attributes: [
            { name: 'var', type: 'string', required: true, description: 'Variable name' }
          ],
          allowsBody: true,
          allowedSubElements: []
        }
      },
      {
        id: 'loop',
        name: 'Loop',
        elementName: 'loop',
        description: 'Iterate over a list of items',
        category: 'Control Flow',
        icon: '🔁',
        version: '2.2.0',
        author: 'WebHarvest Team',
        tags: ['loop', 'iteration', 'control'],
        helpUrl: 'https://web-harvest.sourceforge.io/documentation/plugin-loop.html',
        schema: {
          attributes: [
            { name: 'item', type: 'string', required: false, description: 'Item variable name' },
            { name: 'index', type: 'string', required: false, description: 'Index variable name' }
          ],
          allowsBody: true,
          allowedSubElements: ['list', 'body']
        }
      },
      {
        id: 'if',
        name: 'Conditional',
        elementName: 'if',
        description: 'Conditional execution based on condition',
        category: 'Control Flow',
        icon: '🔀',
        version: '2.2.0',
        author: 'WebHarvest Team',
        tags: ['if', 'condition', 'control'],
        helpUrl: 'https://web-harvest.sourceforge.io/documentation/plugin-if.html',
        schema: {
          attributes: [
            { name: 'condition', type: 'string', required: true, description: 'Boolean condition' }
          ],
          allowsBody: true,
          allowedSubElements: ['then', 'else']
        }
      }
    ];
  }

  /**
   * Public API
   */

  public getCurrentView(): 'xml' | 'split' | 'visual' {
    return this.currentView;
  }

  public getZoomLevel(): number {
    return this.zoomLevel;
  }

  public getPluginRegistry(): PluginRegistry | null {
    return this.pluginRegistry;
  }

  /**
   * Setup canvas drop handling for drag-and-drop
   */
  private setupCanvasDropHandling(): void {
    if (!this.canvasContainer) return;

    // Prevent default drag behavior
    this.canvasContainer.addEventListener('dragover', (e: DragEvent) => {
      e.preventDefault();
      if (e.dataTransfer) {
        e.dataTransfer.dropEffect = 'copy';
      }
      this.canvasContainer?.classList.add('drag-over');
    });

    this.canvasContainer.addEventListener('dragleave', () => {
      this.canvasContainer?.classList.remove('drag-over');
    });

    // Handle drop
    this.canvasContainer.addEventListener('drop', (e: DragEvent) => {
      e.preventDefault();
      this.canvasContainer?.classList.remove('drag-over');

      if (!e.dataTransfer) return;

      const pluginType = e.dataTransfer.getData('plugin-type');
      if (!pluginType) return;

      // Get drop position relative to canvas
      const rect = this.canvasContainer!.getBoundingClientRect();
      const x = e.clientX - rect.left;
      const y = e.clientY - rect.top;

      this.addNodeToCanvas(pluginType, x, y);
    });

    console.log('✅ Canvas drop handling configured');
  }

  /**
   * Add a node to the canvas
   */
  private addNodeToCanvas(pluginType: string, x: number, y: number): void {
    const plugin = this.pluginRegistry?.getPlugin(pluginType);
    if (!plugin) {
      console.error(`Plugin not found: ${pluginType}`);
      return;
    }

    // Create node with all required VisualNode properties
    const nodeId = `node_${this.nodeIdCounter++}`;
    const node: VisualNode = {
      id: nodeId,
      type: pluginType,
      x: x,
      y: y,
      config: {},
      body: null,
      plugin: plugin,
      parentId: null,
      children: [],
      executionState: 'pending'
    };

    // Register with container manager
    this.containerManager.registerNode(node);

    // Render node
    this.renderNode(node);

    // Update stats and outline
    this.updateNodeCount();
    this.updateOutline();

    // Hide empty state
    const emptyState = this.canvasContainer?.querySelector('.canvas-empty-state');
    if (emptyState) {
      (emptyState as HTMLElement).style.display = 'none';
    }

    console.log(`✅ Added node: ${pluginType} at (${x}, ${y})`);
  }

  /**
   * Render a node on the canvas
   */
  private renderNode(node: VisualNode): void {
    if (!this.canvasContainer) return;

    const nodeElement = document.createElement('div');
    nodeElement.className = 'visual-node';
    nodeElement.id = node.id;
    nodeElement.style.left = `${node.x}px`;
    nodeElement.style.top = `${node.y}px`;
    nodeElement.draggable = true;

    // Build node HTML
    const configSummary = this.getNodeConfigSummary(node);

    nodeElement.innerHTML = `
      <div class="node-header">
        <span class="node-icon">${renderIcon(node.plugin.icon)}</span>
        <span class="node-title">${node.plugin.name}</span>
      </div>
      ${configSummary ? `<div class="node-body">${configSummary}</div>` : ''}
      <div class="node-ports">
        <div class="node-port">
          <div class="port-dot input"></div>
          <span>Input</span>
        </div>
        <div class="node-port">
          <span>Output</span>
          <div class="port-dot output"></div>
        </div>
      </div>
    `;

    // Make node draggable
    this.makeNodeDraggable(nodeElement, node);

    // Handle click for selection
    nodeElement.addEventListener('click', () => {
      this.selectNode(node.id);
    });

    this.canvasContainer.appendChild(nodeElement);
  }

  /**
   * Make node draggable on canvas
   */
  private makeNodeDraggable(element: HTMLElement, node: VisualNode): void {
    let isDragging = false;
    let startX = 0;
    let startY = 0;
    let offsetX = 0;
    let offsetY = 0;

    element.addEventListener('mousedown', (e: MouseEvent) => {
      if ((e.target as HTMLElement).closest('.port-dot')) {
        return; // Don't drag when clicking port
      }

      isDragging = true;
      startX = e.clientX;
      startY = e.clientY;
      offsetX = node.x;
      offsetY = node.y;

      element.style.zIndex = '100';
      element.style.cursor = 'grabbing';

      e.preventDefault();
    });

    document.addEventListener('mousemove', (e: MouseEvent) => {
      if (!isDragging) return;

      const dx = e.clientX - startX;
      const dy = e.clientY - startY;

      node.x = offsetX + dx;
      node.y = offsetY + dy;

      element.style.left = `${node.x}px`;
      element.style.top = `${node.y}px`;
    });

    document.addEventListener('mouseup', () => {
      if (isDragging) {
        isDragging = false;
        element.style.zIndex = '';
        element.style.cursor = 'move';
      }
    });
  }

  /**
   * Select a node
   */
  private selectNode(nodeId: string): void {
    // Clear previous selection
    document.querySelectorAll('.visual-node.selected').forEach(el => {
      el.classList.remove('selected');
    });

    // Select new node
    const nodeElement = document.getElementById(nodeId);
    if (nodeElement) {
      nodeElement.classList.add('selected');
    }

    // Show properties using PropertyPanel
    const node = this.containerManager['nodes'].get(nodeId);
    if (node && this.propertyPanel) {
      this.propertyPanel.showProperties(node);
    }
  }

  /**
   * Update node properties (callback from PropertyPanel)
   */
  private updateNodeProperties(nodeId: string, config: Record<string, any>, body: string | null): void {
    const node = this.containerManager['nodes'].get(nodeId);
    if (!node) return;

    node.config = config;
    node.body = body;

    // Update node display
    const nodeElement = document.getElementById(nodeId);
    if (nodeElement) {
      const bodyElement = nodeElement.querySelector('.node-body');
      if (bodyElement) {
        bodyElement.textContent = this.getNodeConfigSummary(node);
      }
    }

    this.updateOutline();
    console.log(`✅ Updated ${node.type}:`, config);
  }

  /**
   * Get node configuration summary
   */
  private getNodeConfigSummary(node: VisualNode): string {
    // Show first configured attribute
    const firstAttr = node.plugin.schema.attributes[0];
    if (firstAttr && node.config[firstAttr.name]) {
      return `${firstAttr.name}: ${node.config[firstAttr.name]}`;
    }
    return '';
  }

  /**
   * Update node count in status bar
   */
  private updateNodeCount(): void {
    const nodeCountElement = document.getElementById('node-count');
    if (nodeCountElement) {
      const count = this.containerManager['nodes'].size;
      nodeCountElement.textContent = `${count} node${count !== 1 ? 's' : ''}`;
    }
  }

  /**
   * Update outline panel
   */
  private updateOutline(): void {
    if (this.outlinePanel) {
      this.outlinePanel.update(this.containerManager['nodes']);
    }
  }
}

// VisualNode type is now imported from types/VisualNode.ts

