/**
 * PropertyPanel - Advanced Implementation with Schema-Driven Editors
 * Displays and edits properties of selected node using advanced editors
 */

import type { VisualNode, Plugin } from '../types';
import { ExpressionBuilder } from './ExpressionBuilder';
import { XPathTester } from './XPathTester';
import { RegexValidator } from './RegexValidator';
import { SchemaEditor } from './SchemaEditor';

export class PropertyPanel {
    private container: HTMLElement;
    private currentNode: VisualNode | null = null;
    private onChangeCallback: ((node: VisualNode) => void) | null = null;

    // Advanced editors
    private expressionBuilder: ExpressionBuilder | null = null;
    private xpathTester: XPathTester | null = null;
    private regexValidator: RegexValidator | null = null;
    private schemaEditor: SchemaEditor | null = null;

    // Plugin metadata cache
    private pluginMetadata: Plugin[] = [];
    private metadataLoaded: boolean = false;

    // Available variables for expression builder
    private availableVariables: string[] = [];

    constructor(containerId: string) {
        const container = document.getElementById(containerId);

        if (!container) {
            throw new Error(`Container with ID "${containerId}" not found`);
        }

        this.container = container;
        this.schemaEditor = new SchemaEditor();

        // Load plugin metadata asynchronously
        this.loadPluginMetadata().catch(err => {
            console.warn('⚠️ [PropertyPanel] Failed to load plugin metadata:', err);
        });
    }

    /**
     * Load plugin metadata from API
     */
    private async loadPluginMetadata(): Promise<void> {
        if (this.metadataLoaded) {
            return;
        }

        try {
            const response = await fetch('/api/plugins/metadata');
            if (!response.ok) {
                throw new Error(`Failed to fetch plugin metadata: ${response.statusText}`);
            }

            const data = await response.json();
            this.pluginMetadata = data.plugins || [];
            this.metadataLoaded = true;

            console.log(`✅ [PropertyPanel] Loaded ${this.pluginMetadata.length} plugin schemas`);
        } catch (error) {
            console.error('❌ [PropertyPanel] Error loading plugin metadata:', error);
            this.metadataLoaded = true; // Mark as loaded to prevent retry loops
        }
    }

    /**
     * Get plugin schema for node type
     */
    private getPluginSchema(nodeType: string): Plugin | null {
        // Wait for metadata to load if not ready
        if (!this.metadataLoaded) {
            return null;
        }

        // Try exact match first (elementName)
        let plugin = this.pluginMetadata.find(p => {
            // Some plugins use 'id', some use 'elementName' in API response
            const id = (p as any).elementName || p.id;
            return id === nodeType || p.id === nodeType;
        });

        if (!plugin) {
            // Fallback: try case-insensitive match
            plugin = this.pluginMetadata.find(p => {
                const id = (p as any).elementName || p.id;
                return id.toLowerCase() === nodeType.toLowerCase() ||
                    p.id.toLowerCase() === nodeType.toLowerCase();
            });
        }

        return plugin || null;
    }

    /**
     * Update available variables for expression builder
     */
    setAvailableVariables(variables: string[]): void {
        this.availableVariables = variables;
    }

    /**
     * Show properties for node
     */
    async showNode(node: VisualNode): Promise<void> {
        console.log('🔧 [PropertyPanel] showNode() called for:', node.type, node);
        this.currentNode = { ...node };
        await this.render();
        console.log('🔧 [PropertyPanel] showNode() complete');
    }

    /**
     * Clear panel
     */
    clear(): void {
        this.currentNode = null;
        this.container.innerHTML = '';
    }

    /**
     * Register change callback
     */
    onChange(callback: (node: VisualNode) => void): void {
        this.onChangeCallback = callback;
    }

    /**
     * Get current node
     */
    getCurrentNode(): VisualNode | null {
        return this.currentNode ? { ...this.currentNode } : null;
    }

    /**
     * Render properties with advanced editors
     */
    private async render(): Promise<void> {
        if (!this.currentNode) {
            console.log('🔧 [PropertyPanel] render() - no current node');
            return;
        }

        console.log('🔧 [PropertyPanel] render() - node:', this.currentNode.type, this.currentNode.attributes);

        // Ensure metadata is loaded
        await this.loadPluginMetadata();

        this.container.innerHTML = '';

        const form = document.createElement('div');
        form.style.padding = '16px';
        form.style.maxHeight = 'calc(100vh - 200px)';
        form.style.overflowY = 'auto';

        // Type (read-only)
        const typeLabel = document.createElement('div');
        typeLabel.style.marginBottom = '12px';
        typeLabel.style.fontSize = '14px';
        typeLabel.innerHTML = `<strong style="color: var(--magenta);">Type:</strong> ${this.currentNode.type}`;
        form.appendChild(typeLabel);

        // Try to use SchemaEditor if plugin schema is available
        const plugin = this.getPluginSchema(this.currentNode.type);

        if (plugin && plugin.schema && this.schemaEditor) {
            console.log(`✅ [PropertyPanel] Using SchemaEditor for plugin: ${this.currentNode.type}`);

            // Use SchemaEditor for schema-driven form
            this.schemaEditor.generateEditor(form, plugin, this.currentNode.attributes);

            // Register onChange callback
            this.schemaEditor.onChange((attributes) => {
                if (this.currentNode && this.onChangeCallback) {
                    this.currentNode.attributes = attributes;
                    this.onChangeCallback(this.currentNode);
                }
            });

            this.container.appendChild(form);
            return;
        }

        // Fallback: Use simple inputs when schema not available
        console.log(`⚠️ [PropertyPanel] Schema not available for ${this.currentNode.type}, using simple inputs`);

        // Attributes with advanced editors
        if (Object.keys(this.currentNode.attributes).length > 0) {
            const attrsLabel = document.createElement('div');
            attrsLabel.innerHTML = '<strong>Attributes:</strong>';
            attrsLabel.style.marginBottom = '12px';
            attrsLabel.style.fontSize = '13px';
            form.appendChild(attrsLabel);

            // Fallback: simple text inputs
            Object.entries(this.currentNode.attributes).forEach(([key, value]) => {
                const fieldContainer = document.createElement('div');
                fieldContainer.style.marginBottom = '12px';

                const label = document.createElement('label');
                label.textContent = key + ':';
                label.style.display = 'block';
                label.style.fontSize = '13px';
                label.style.fontWeight = '600';
                label.style.marginBottom = '6px';
                fieldContainer.appendChild(label);

                // Detect field type
                let isXPath = key.toLowerCase().includes('xpath') || key.toLowerCase() === 'expression';
                let isRegex = key.toLowerCase().includes('regex') || key.toLowerCase().includes('pattern');
                let hasExpression = value.includes('${');

                const input = document.createElement('input');
                input.type = 'text';
                input.value = value;
                input.style.width = '100%';
                input.style.padding = '8px';
                input.style.border = '1px solid var(--gray-300)';
                input.style.borderRadius = 'var(--radius-md)';
                input.style.fontSize = '13px';
                input.onchange = () => {
                    if (this.currentNode && this.onChangeCallback) {
                        this.currentNode.attributes[key] = input.value;
                        this.onChangeCallback(this.currentNode);
                    }
                };
                fieldContainer.appendChild(input);

                // Add advanced editor button RIGHT AFTER input (in same fieldContainer)
                if (isXPath) {
                    console.log(`🔧 [PropertyPanel] Adding XPath button for field: ${key}`);
                    this.addAdvancedEditorButton(fieldContainer, key, 'XPath Tester', 'xpath');
                } else if (isRegex) {
                    console.log(`🔧 [PropertyPanel] Adding Regex button for field: ${key}`);
                    this.addAdvancedEditorButton(fieldContainer, key, 'Regex Validator', 'regex');
                } else if (hasExpression) {
                    console.log(`🔧 [PropertyPanel] Adding Expression button for field: ${key}`);
                    this.addAdvancedEditorButton(fieldContainer, key, 'Expression Builder', 'expression');
                } else {
                    console.log(`🔧 [PropertyPanel] No advanced button for field: ${key} (isXPath=${isXPath}, isRegex=${isRegex}, hasExpr=${hasExpression})`);
                }

                form.appendChild(fieldContainer);
            });
        }

        // Body
        if (this.currentNode.body !== null) {
            const bodyLabel = document.createElement('div');
            bodyLabel.innerHTML = '<strong>Body:</strong>';
            bodyLabel.style.marginTop = '16px';
            bodyLabel.style.marginBottom = '8px';
            bodyLabel.style.fontSize = '13px';
            form.appendChild(bodyLabel);

            const bodyInput = document.createElement('textarea');
            bodyInput.value = this.currentNode.body || '';
            bodyInput.style.width = '100%';
            bodyInput.style.minHeight = '100px';
            bodyInput.style.padding = '10px';
            bodyInput.style.border = '1px solid var(--gray-300)';
            bodyInput.style.borderRadius = 'var(--radius-md)';
            bodyInput.style.fontFamily = 'monospace';
            bodyInput.style.fontSize = '12px';
            bodyInput.onchange = () => {
                if (this.currentNode && this.onChangeCallback) {
                    this.currentNode.body = bodyInput.value;
                    this.onChangeCallback(this.currentNode);
                }
            };

            // If body contains ${}, add Expression Builder button
            if (bodyInput.value.includes('${')) {
                console.log('🔧 [PropertyPanel] Body contains ${}, adding Expression Builder button');
                // Create wrapper for body + button
                const bodyContainer = document.createElement('div');
                bodyContainer.appendChild(bodyInput);
                this.addAdvancedEditorButton(bodyContainer, 'body', 'Expression Builder', 'expression');
                form.appendChild(bodyContainer);
            } else {
                form.appendChild(bodyInput);
            }
        }

        console.log('🔧 [PropertyPanel] Appending form to container');
        this.container.appendChild(form);
        console.log('✅ [PropertyPanel] render() complete');
    }

    /**
     * Add button to open advanced editor
     */
    private addAdvancedEditorButton(
        container: HTMLElement,
        fieldName: string,
        buttonLabel: string,
        editorType: 'xpath' | 'regex' | 'expression'
    ): void {
        console.log(`🔧🔧 [PropertyPanel] addAdvancedEditorButton() called for ${fieldName} (${editorType})`);

        const buttonContainer = document.createElement('div');
        buttonContainer.style.marginTop = '6px';
        buttonContainer.style.marginBottom = '0px';
        buttonContainer.style.display = 'flex';
        buttonContainer.style.alignItems = 'center';
        buttonContainer.style.gap = '8px';

        const button = document.createElement('button');
        button.textContent = `🔧 ${buttonLabel}`;
        button.style.padding = '6px 12px';
        button.style.background = 'var(--lime)';
        button.style.color = 'white';
        button.style.border = 'none';
        button.style.borderRadius = 'var(--radius-md)';
        button.style.cursor = 'pointer';
        button.style.fontSize = '12px';
        button.style.fontWeight = '600';
        button.style.transition = 'all 0.2s';

        button.onmouseenter = () => {
            button.style.transform = 'scale(1.05)';
        };
        button.onmouseleave = () => {
            button.style.transform = 'scale(1)';
        };

        button.onclick = () => {
            console.log(`🔧 [PropertyPanel] Button clicked for ${fieldName}`);
            this.openAdvancedEditor(fieldName, editorType);
        };

        buttonContainer.appendChild(button);
        container.appendChild(buttonContainer);

        console.log(`✅ [PropertyPanel] Button added to DOM for ${fieldName}`);
    }

    /**
     * Open advanced editor modal
     */
    private openAdvancedEditor(fieldName: string, editorType: 'xpath' | 'regex' | 'expression'): void {
        console.log(`🔧 [PropertyPanel] openAdvancedEditor() - field: ${fieldName}, type: ${editorType}`);

        if (!this.currentNode) {
            console.error('🔧 [PropertyPanel] Cannot open editor - no current node');
            return;
        }

        // Create modal overlay
        const modal = document.createElement('div');
        modal.style.cssText = `
            position: fixed;
            top: 0;
            left: 0;
            width: 100%;
            height: 100%;
            background: rgba(0, 0, 0, 0.5);
            z-index: 10000;
            display: flex;
            align-items: center;
            justify-content: center;
        `;

        // Create modal content
        const modalContent = document.createElement('div');
        modalContent.style.cssText = `
            background: white;
            border-radius: var(--radius-lg);
            padding: 24px;
            max-width: 800px;
            width: 90%;
            max-height: 80vh;
            overflow-y: auto;
            box-shadow: var(--shadow-xl);
        `;

        // Header
        const header = document.createElement('div');
        header.style.cssText = `
            display: flex;
            justify-content: space-between;
            align-items: center;
            margin-bottom: 20px;
        `;

        const title = document.createElement('h3');
        title.textContent = editorType === 'xpath' ? 'XPath Tester' :
            editorType === 'regex' ? 'Regex Validator' : 'Expression Builder';
        title.style.cssText = 'margin: 0; color: var(--magenta); font-size: 18px;';

        const closeBtn = document.createElement('button');
        closeBtn.textContent = '✕';
        closeBtn.style.cssText = `
            background: none;
            border: none;
            font-size: 24px;
            cursor: pointer;
            color: var(--gray-600);
        `;
        closeBtn.onclick = () => modal.remove();

        header.appendChild(title);
        header.appendChild(closeBtn);
        modalContent.appendChild(header);

        // Editor container
        const editorContainer = document.createElement('div');
        editorContainer.id = `${editorType}-editor-modal`;
        modalContent.appendChild(editorContainer);

        // Get current value
        const currentValue = fieldName === 'body' ?
            (this.currentNode.body || '') :
            (this.currentNode.attributes[fieldName] || '');

        // Initialize appropriate editor
        if (editorType === 'xpath') {
            this.xpathTester = new XPathTester();
            this.xpathTester.render(editorContainer, currentValue);
            this.xpathTester.onChange((newValue) => {
                if (this.currentNode && this.onChangeCallback) {
                    if (fieldName === 'body') {
                        this.currentNode.body = newValue;
                    } else {
                        this.currentNode.attributes[fieldName] = newValue;
                    }
                    this.onChangeCallback(this.currentNode);
                }
            });
        } else if (editorType === 'regex') {
            this.regexValidator = new RegexValidator();
            this.regexValidator.render(editorContainer, currentValue);
            this.regexValidator.onChange((pattern: string, flags: string) => {
                if (this.currentNode && this.onChangeCallback) {
                    // Regex returns pattern + flags, use pattern only for now
                    const newValue = pattern;
                    if (fieldName === 'body') {
                        this.currentNode.body = newValue;
                    } else {
                        this.currentNode.attributes[fieldName] = newValue;
                    }
                    this.onChangeCallback(this.currentNode);
                }
            });
        } else if (editorType === 'expression') {
            this.expressionBuilder = new ExpressionBuilder();
            this.expressionBuilder.render(editorContainer, currentValue);
            // Set available variables as Record<string, string>
            const varsRecord: Record<string, string> = {};
            this.availableVariables.forEach(varName => {
                varsRecord[varName] = `value_of_${varName}`;
            });
            this.expressionBuilder.setAvailableVariables(varsRecord);
            this.expressionBuilder.onChange((newValue) => {
                if (this.currentNode && this.onChangeCallback) {
                    if (fieldName === 'body') {
                        this.currentNode.body = newValue;
                    } else {
                        this.currentNode.attributes[fieldName] = newValue;
                    }
                    this.onChangeCallback(this.currentNode);
                }
            });
        }

        // Add apply button
        const footer = document.createElement('div');
        footer.style.cssText = 'margin-top: 20px; text-align: right;';

        const applyBtn = document.createElement('button');
        applyBtn.textContent = 'Apply & Close';
        applyBtn.style.cssText = `
            padding: 10px 20px;
            background: var(--magenta);
            color: white;
            border: none;
            border-radius: var(--radius-md);
            cursor: pointer;
            font-weight: 600;
        `;
        applyBtn.onclick = async () => {
            // Re-render to show updated values
            await this.render();
            modal.remove();
        };

        footer.appendChild(applyBtn);
        modalContent.appendChild(footer);

        modal.appendChild(modalContent);
        document.body.appendChild(modal);

        // Close on overlay click
        modal.addEventListener('click', (e) => {
            if (e.target === modal) {
                modal.remove();
            }
        });
    }
}





