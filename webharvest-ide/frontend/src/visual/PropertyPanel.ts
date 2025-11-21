/**
 * PropertyPanel - Advanced Implementation with Schema-Driven Editors
 * Displays and edits properties of selected node using advanced editors
 */

import type { VisualNode, Plugin, AttributeDefinition, PluginSemanticsModel, UiToolDefinition } from '../types';
import type { ContainerSummary } from './HierarchyInspector';
import type { PluginManager } from '../core/PluginManager';
import { MasterDetailRegistry } from './detail/MasterDetailRegistry';
import { MasterDetailEditor } from './detail/MasterDetailEditor';

const HTTP_METHODS = ['GET', 'POST', 'PUT', 'PATCH', 'DELETE', 'HEAD', 'OPTIONS', 'TRACE'];
const METHODS_WITH_BODY = new Set(['POST', 'PUT', 'PATCH', 'DELETE', 'OPTIONS', 'TRACE']);
import { HttpEditor } from './editors/HttpEditor';

interface PropertyPanelOptions {
    autoLoad?: boolean;
}
import { ExpressionBuilder } from './ExpressionBuilder';
import { XPathTester } from './XPathTester';
import { RegexValidator } from './RegexValidator';
import { SchemaDrivenEditor, type FieldDecoratorContext, type ValidationError } from './editors/SchemaDrivenEditor';
import { PropertyPanelShell } from './PropertyPanelShell';

export class PropertyPanel {
    private container: HTMLElement;
    private currentNode: VisualNode | null = null;
    private originalNode: VisualNode | null = null;
    private onChangeCallback: ((node: VisualNode) => void) | null = null;
    private detailRegistry: MasterDetailRegistry | null = null;

    private pluginManager: PluginManager | null = null;

    // Advanced editors
    private expressionBuilder: ExpressionBuilder | null = null;
    private xpathTester: XPathTester | null = null;
    private regexValidator: RegexValidator | null = null;
    private schemaEditor: SchemaDrivenEditor | null = null;
    private schemaEditorActive = false;
    private shell: PropertyPanelShell;

    // Plugin metadata cache
    private pluginMetadata: Plugin[] = [];
    private metadataLoaded: boolean = false;

    // Available variables for expression builder
    private availableVariables: string[] = [];
    private currentFocusSummary: { nodeId: string; summary: ContainerSummary } | null = null;
    private currentForm: HTMLElement | null = null;
    private focusSummaryElement: HTMLElement | null = null;

    constructor(containerId: string, options?: PropertyPanelOptions | ((node: VisualNode) => void)) {
        const container = document.getElementById(containerId);

        if (!container) {
            throw new Error(`Container with ID "${containerId}" not found`);
        }

        this.container = container;
        this.shell = new PropertyPanelShell(this.container);
        this.schemaEditor = new SchemaDrivenEditor();

        let autoLoad = true;
        if (typeof options === 'function') {
            this.onChangeCallback = options;
        } else if (options) {
            autoLoad = options.autoLoad ?? true;
        }

        // Load plugin metadata asynchronously
        if (autoLoad) {
            this.loadPluginMetadata().catch(err => {
                console.warn('⚠️ [PropertyPanel] Failed to load plugin metadata:', err);
            });
        }
    }

    /**
     * Load plugin metadata from API
     */
    private async loadPluginMetadata(): Promise<void> {
        if (this.metadataLoaded) {
            return;
        }

        try {
            const rawPlugins = this.pluginManager ? this.pluginManager.getPlugins() : await this.fetchPluginMetadata();
            this.pluginMetadata = rawPlugins.map(plugin => this.normalisePluginMetadata(plugin));
            this.metadataLoaded = true;

            console.log(`✅ [PropertyPanel] Loaded ${this.pluginMetadata.length} plugin schemas`);
        } catch (error) {
            console.error('❌ [PropertyPanel] Error loading plugin metadata:', error);
            this.metadataLoaded = true; // Mark as loaded to prevent retry loops
        }
    }

    setPluginManager(manager: PluginManager): void {
        this.pluginManager = manager;
        this.metadataLoaded = false;
        this.detailRegistry = new MasterDetailRegistry(manager);
        void this.loadPluginMetadata();
    }

    private async fetchPluginMetadata(): Promise<Plugin[]> {
        const response = await fetch('/api/plugins/metadata');
        if (!response.ok) {
            throw new Error(`Failed to fetch plugin metadata: ${response.statusText}`);
        }
        const data = await response.json();
        return (data.plugins || []) as Plugin[];
    }

    private normalisePluginMetadata(plugin: Plugin): Plugin {
        const clone = this.clonePlugin(plugin);
        const semantics = this.normaliseSemantics(clone.semantics);

        const schema = clone.schema ? { ...clone.schema } : null;
        if (schema) {
            const attributes = this.normaliseAttributes(schema.attributes);
            const allowedSubElements = schema.allowedSubElements ?? semantics.allowedChildTypes;
            const requiredParents = schema.requiredParents ?? semantics.requiredParentTypes;
            const canHaveBody = schema.canHaveBody ?? schema.allowsBody ?? semantics.allowTextContent;
            const canHaveChildren = schema.canHaveChildren ?? (semantics.allowsChildren || allowedSubElements.length > 0);
            const canAppearAtRoot = schema.canAppearAtRoot ?? (requiredParents.length === 0 && !semantics.roles.includes('DEPENDENT'));
            const isContextSpecific = schema.isContextSpecific ?? requiredParents.length > 0;

            clone.schema = {
                ...schema,
                attributes,
                allowedSubElements,
                requiredParents,
                canHaveBody,
                canHaveChildren,
                canAppearAtRoot,
                isContextSpecific
            };
        }

        clone.semantics = semantics;
        return clone;
    }

    private normaliseSemantics(input?: PluginSemanticsModel): PluginSemanticsModel {
        const allowedRoles: PluginSemanticsModel['roles'][number][] = ['ROOT_CONTAINER', 'CONTEXT_CONTAINER', 'BODY_EVALUATOR', 'DEPENDENT', 'LEAF'];
        const ensureRole = (role?: string): PluginSemanticsModel['roles'][number] | null => {
            if (!role) return null;
            const upper = role.toUpperCase() as PluginSemanticsModel['roles'][number];
            return allowedRoles.includes(upper) ? upper : null;
        };

        const semantics: PluginSemanticsModel = input ? { ...input } : {
            roles: ['LEAF'],
            canAppearAtRoot: true,
            allowsChildren: false,
            acceptsAnyChild: false,
            allowBody: false,
            allowTextContent: false,
            allowedChildTypes: [],
            requiredParentTypes: []
        };

        const roles = Array.isArray(semantics.roles)
            ? semantics.roles.map(role => ensureRole(role)).filter((role): role is PluginSemanticsModel['roles'][number] => role !== null)
            : [];

        if (roles.length === 0) {
            const fallback = ensureRole((semantics as any).role);
            semantics.roles = fallback ? [fallback] : ['LEAF'];
        } else {
            semantics.roles = roles;
        }

        semantics.allowedChildTypes = Array.isArray(semantics.allowedChildTypes) ? semantics.allowedChildTypes : [];
        semantics.requiredParentTypes = Array.isArray(semantics.requiredParentTypes) ? semantics.requiredParentTypes : [];

        return semantics;
    }

    private normaliseAttributes(attributes: AttributeDefinition[]): AttributeDefinition[] {
        if (!Array.isArray(attributes)) {
            return [];
        }

        const allowedTypes: AttributeDefinition['type'][] = ['string', 'number', 'boolean', 'url', 'xpath', 'enum'];

        return attributes.map(attr => {
            const rawType = (attr.type || 'string').toString().toLowerCase();
            const normalisedType = allowedTypes.includes(rawType as AttributeDefinition['type'])
                ? rawType as AttributeDefinition['type']
                : 'string';

            return {
                ...attr,
                type: normalisedType
            };
        });
    }

    private clonePlugin(plugin: Plugin): Plugin {
        if (typeof structuredClone === 'function') {
            return structuredClone(plugin);
        }
        return JSON.parse(JSON.stringify(plugin));
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
        this.currentNode = this.deepCloneNode(node);
        this.originalNode = this.deepCloneNode(node);
        this.currentNode.attributes = this.currentNode.attributes || {};
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

        this.shell.reset();
        this.schemaEditorActive = false;

        const form = document.createElement('div');
        form.className = 'property-panel__form';
        form.style.maxHeight = 'calc(100vh - 220px)';
        form.style.overflowY = 'auto';
        this.currentForm = form;
        this.focusSummaryElement = null;

        // Type (read-only)
        const typeLabel = document.createElement('div');
        typeLabel.style.marginBottom = '12px';
        typeLabel.style.fontSize = '14px';
        typeLabel.innerHTML = `<strong style="color: var(--magenta);">Type:</strong> ${this.currentNode.type}`;
        form.appendChild(typeLabel);

        this.appendFocusSummarySection(form);

        // Try to use SchemaEditor if plugin schema is available
        const plugin = this.getPluginSchema(this.currentNode.type);

        this.shell.setHeader({
            title: plugin?.name || this.currentNode.type,
            subtitle: plugin?.description || undefined,
            icon: plugin?.icon
        });

        if (plugin) {
            const metaContainer = document.createElement('div');
            metaContainer.style.marginBottom = '16px';
            metaContainer.style.fontSize = '12px';
            metaContainer.style.lineHeight = '1.5';

            if (plugin.documentation?.summary) {
                const summaryEl = document.createElement('div');
                summaryEl.style.color = 'var(--gray-700)';
                summaryEl.innerHTML = `<strong>Summary:</strong> ${plugin.documentation.summary}`;
                // Add tooltip on hover
                summaryEl.title = plugin.documentation.summary;
                summaryEl.style.cursor = 'help';
                metaContainer.appendChild(summaryEl);
            }

            if (plugin.documentation?.url) {
                const linkEl = document.createElement('a');
                linkEl.href = plugin.documentation.url;
                linkEl.target = '_blank';
                linkEl.rel = 'noreferrer';
                linkEl.textContent = 'View documentation ↗';
                linkEl.style.display = 'inline-block';
                linkEl.style.marginTop = '4px';
                linkEl.style.color = 'var(--magenta)';
                linkEl.style.fontWeight = '600';
                metaContainer.appendChild(linkEl);
            }

            // Show examples in collapsible section
            if (plugin.documentation?.examples && plugin.documentation.examples.length > 0) {
                const examplesContainer = document.createElement('div');
                examplesContainer.style.marginTop = '12px';
                
                const examplesToggle = document.createElement('button');
                examplesToggle.textContent = 'Show Examples';
                examplesToggle.style.cssText = 'background: none; border: none; color: var(--magenta); cursor: pointer; font-size: 12px; padding: 4px 0; text-decoration: underline;';
                examplesToggle.type = 'button';
                
                const examplesContent = document.createElement('div');
                examplesContent.style.display = 'none';
                examplesContent.style.marginTop = '8px';
                examplesContent.style.padding = '8px';
                examplesContent.style.background = 'var(--gray-100)';
                examplesContent.style.borderRadius = 'var(--radius-sm)';
                examplesContent.style.fontFamily = 'monospace';
                examplesContent.style.fontSize = '11px';
                examplesContent.style.whiteSpace = 'pre-wrap';
                examplesContent.textContent = plugin.documentation.examples.join('\n\n');
                
                examplesToggle.addEventListener('click', () => {
                    const isVisible = examplesContent.style.display !== 'none';
                    examplesContent.style.display = isVisible ? 'none' : 'block';
                    examplesToggle.textContent = isVisible ? 'Show Examples' : 'Hide Examples';
                });
                
                examplesContainer.appendChild(examplesToggle);
                examplesContainer.appendChild(examplesContent);
                metaContainer.appendChild(examplesContainer);
            }

            // Show semantic roles and context
            const semanticRoles: string[] = Array.isArray(plugin.semantics?.roles)
                ? plugin.semantics.roles
                : (plugin.semantics?.role ? [plugin.semantics.role] : []);
            if (semanticRoles.length > 0) {
                const rolesWrapper = document.createElement('div');
                rolesWrapper.style.marginTop = '8px';
                rolesWrapper.style.display = 'flex';
                rolesWrapper.style.flexWrap = 'wrap';
                rolesWrapper.style.gap = '6px';

                semanticRoles.forEach(role => {
                    const roleEl = document.createElement('div');
                    roleEl.style.padding = '6px 8px';
                    roleEl.style.background = this.getRoleColor(role);
                    roleEl.style.color = 'white';
                    roleEl.style.borderRadius = '4px';
                    roleEl.style.fontSize = '11px';
                    roleEl.style.fontWeight = '600';
                    roleEl.textContent = this.getRoleLabel(role);
                    roleEl.title = `Semantic role: ${this.getRoleLabel(role)}`;
                    rolesWrapper.appendChild(roleEl);
                });

                metaContainer.appendChild(rolesWrapper);
            }

            // Show required parent types
            const requiredParents = plugin.semantics?.requiredParentTypes || plugin.dependencies || [];
            if (requiredParents.length > 0) {
                const depsEl = document.createElement('div');
                depsEl.style.marginTop = '8px';
                depsEl.style.padding = '6px 8px';
                depsEl.style.background = '#fff9e6';
                depsEl.style.border = '1px solid #FF9800';
                depsEl.style.borderRadius = '4px';
                depsEl.style.fontSize = '11px';
                depsEl.innerHTML = `<strong>⚠️ Requires parent:</strong> ${requiredParents.join(', ')}`;
                depsEl.title = `This plugin must be placed inside one of: ${requiredParents.join(', ')}`;
                metaContainer.appendChild(depsEl);
            }

            // Show allowed child types
            const allowedChildren = plugin.semantics?.allowedChildTypes || [];
            const acceptsAnyChild = plugin.semantics?.acceptsAnyChild || false;
            if (allowedChildren.length > 0 || acceptsAnyChild) {
                const childrenEl = document.createElement('div');
                childrenEl.style.marginTop = '8px';
                childrenEl.style.padding = '6px 8px';
                childrenEl.style.background = '#f1f8f4';
                childrenEl.style.border = '1px solid #4CAF50';
                childrenEl.style.borderRadius = '4px';
                childrenEl.style.fontSize = '11px';
                if (acceptsAnyChild) {
                    childrenEl.innerHTML = `<strong>✓ Accepts:</strong> Any child plugin`;
                } else {
                    childrenEl.innerHTML = `<strong>✓ Accepts children:</strong> ${allowedChildren.join(', ')}`;
                }
                childrenEl.title = acceptsAnyChild 
                    ? 'This plugin can contain any child plugin'
                    : `This plugin can contain: ${allowedChildren.join(', ')}`;
                metaContainer.appendChild(childrenEl);
            }

            if (metaContainer.children.length > 0) {
                metaContainer.style.padding = '12px';
                metaContainer.style.background = 'var(--gray-50)';
                metaContainer.style.border = '1px solid var(--gray-200)';
                metaContainer.style.borderRadius = 'var(--radius-md)';
                form.appendChild(metaContainer);
            }
        }

        if (plugin) {
            this.renderUiToolsSection(form, plugin);
        }

        const detailSections = this.detailRegistry ? this.detailRegistry.getSections(this.currentNode.type) : [];
        const isHttp = this.currentNode.type && this.currentNode.type.toLowerCase() === 'http';
        let handledAttributes = false;

        if (plugin && isHttp) {
            this.renderHttpRequestSection(form);
            handledAttributes = true;
        }

        if (!handledAttributes && plugin && plugin.schema && this.schemaEditor) {
            console.log(`✅ [PropertyPanel] Using SchemaEditor for plugin: ${this.currentNode.type}`);

            const schemaContainer = document.createElement('div');
            schemaContainer.className = 'schema-editor-wrapper';
            form.appendChild(schemaContainer);

            this.schemaEditor.setFieldDecorator((context) => this.decorateSchemaField(context));
            this.schemaEditor.generateEditor(schemaContainer, plugin, this.currentNode.attributes);
            this.schemaEditorActive = true;
            this.schemaEditor.onChange((attributes) => {
                if (this.currentNode) {
                    const updatedNode = this.deepCloneNode(this.currentNode);
                    updatedNode.attributes = { ...attributes };
                    this.currentNode = updatedNode;

                    const errors = this.schemaEditor?.validate() || [];
                    this.shell.showValidationSummary(errors);
                    this.shell.setApplyDisabled(errors.length > 0);
                    if (errors.length === 0) {
                        this.emitChange();
                    }
                }
            });

            const inputs = schemaContainer.querySelectorAll('input, select, textarea');
            inputs.forEach(input => {
                input.addEventListener('input', () => {
                    this.updateValidationState();
                });
                input.addEventListener('blur', () => {
                    this.updateValidationState();
                });
            });

            this.updateValidationState();
            handledAttributes = true;
        }

        if (!handledAttributes) {
            console.log(`⚠️ [PropertyPanel] Schema not available for ${this.currentNode.type}, using simple inputs`);

            if (Object.keys(this.currentNode.attributes).length > 0) {
                const attrsLabel = document.createElement('div');
                attrsLabel.innerHTML = '<strong>Attributes:</strong>';
                attrsLabel.style.marginBottom = '12px';
                attrsLabel.style.fontSize = '13px';
                form.appendChild(attrsLabel);

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

                    const isXPath = key.toLowerCase().includes('xpath') || key.toLowerCase() === 'expression';
                    const isRegex = key.toLowerCase().includes('regex') || key.toLowerCase().includes('pattern');
                    const hasExpression = typeof value === 'string' && value.includes('${');

                    const input = document.createElement('input');
                    input.type = 'text';
                    input.value = value;
                    input.className = 'detail-input';
                    input.onchange = () => {
                        if (this.currentNode && this.onChangeCallback) {
                            this.currentNode.attributes[key] = input.value;
                            this.onChangeCallback(this.deepCloneNode(this.currentNode));
                        }
                    };
                    fieldContainer.appendChild(input);

                    if (isXPath) {
                        this.addAdvancedEditorButton(fieldContainer, key, 'XPath Tester', 'xpath');
                    } else if (isRegex) {
                        this.addAdvancedEditorButton(fieldContainer, key, 'Regex Validator', 'regex');
                    } else if (hasExpression) {
                        this.addAdvancedEditorButton(fieldContainer, key, 'Expression Builder', 'expression');
                    }

                    form.appendChild(fieldContainer);
                });
            }

            const pluginForBody = this.getPluginSchema(this.currentNode.type);
            const allowsBody = pluginForBody?.semantics?.allowBody || pluginForBody?.semantics?.allowTextContent || 
                              pluginForBody?.schema?.canHaveBody || false;
            
            if (allowsBody && this.currentNode.body !== null) {
                const bodyLabel = document.createElement('div');
                bodyLabel.innerHTML = '<strong>Body:</strong>';
                bodyLabel.style.marginTop = '16px';
                bodyLabel.style.marginBottom = '8px';
                bodyLabel.style.fontSize = '13px';
                form.appendChild(bodyLabel);

                const bodyInput = document.createElement('textarea');
                bodyInput.value = this.currentNode.body || '';
                bodyInput.className = 'detail-input';
                bodyInput.style.minHeight = '100px';
                bodyInput.onchange = () => {
                    if (this.currentNode && this.onChangeCallback) {
                        this.currentNode.body = bodyInput.value;
                        this.onChangeCallback(this.deepCloneNode(this.currentNode));
                    }
                };

                if (bodyInput.value.includes('${')) {
                    const bodyContainer = document.createElement('div');
                    bodyContainer.appendChild(bodyInput);
                    this.addAdvancedEditorButton(bodyContainer, 'body', 'Expression Builder', 'expression');
                    form.appendChild(bodyContainer);
                } else {
                    form.appendChild(bodyInput);
                }
            } else if (!allowsBody && this.currentNode.body !== null) {
                const bodyInfo = document.createElement('div');
                bodyInfo.style.marginTop = '16px';
                bodyInfo.style.padding = '8px';
                bodyInfo.style.background = 'var(--gray-100)';
                bodyInfo.style.borderRadius = 'var(--radius-sm)';
                bodyInfo.style.fontSize = '12px';
                bodyInfo.style.color = 'var(--gray-600)';
                bodyInfo.textContent = 'This plugin does not support body content.';
                form.appendChild(bodyInfo);
            }
        }

        if (detailSections.length > 0 && this.detailRegistry) {
            const detailHeading = document.createElement('h3');
            detailHeading.className = 'detail-editor__heading';
            detailHeading.textContent = 'Detail Items';
            form.appendChild(detailHeading);

            const detailEditor = new MasterDetailEditor({
                node: this.deepCloneNode(this.currentNode),
                sections: detailSections,
                registry: this.detailRegistry,
                onChange: (updatedNode) => {
                    this.currentNode = this.deepCloneNode(updatedNode);
                    this.onChangeCallback?.(this.deepCloneNode(updatedNode));
                }
            });

            form.appendChild(detailEditor.render());
        }

        this.shell.setBody(form);
        this.shell.setFooterActions({
            onApply: () => this.handleApply(),
            onCancel: () => this.handleCancel(),
            disableApply: false
        });
        this.updateValidationState();
        this.shell.focusFirstField();
        console.log('✅ [PropertyPanel] render() complete');
    }

    private renderUiToolsSection(form: HTMLElement, plugin: Plugin): void {
        const tools = plugin.uiTools || plugin.visual?.ui?.tools;
        if (!tools || tools.length === 0) {
            return;
        }

        const section = document.createElement('section');
        section.className = 'property-panel__tools';

        const heading = document.createElement('h3');
        heading.textContent = 'Tools';
        heading.className = 'property-panel__tools-title';
        section.appendChild(heading);

        const list = document.createElement('div');
        list.className = 'property-panel__tools-list';

        tools.forEach(tool => {
            const button = document.createElement('button');
            button.type = 'button';
            button.className = 'property-panel__tool-button';
            button.textContent = tool.label || tool.id || 'Tool';
            if (tool.description) {
                button.title = tool.description;
            }
            button.addEventListener('click', () => this.invokeUiTool(tool));
            list.appendChild(button);
        });

        section.appendChild(list);
        form.appendChild(section);
    }

    private decorateSchemaField(context: FieldDecoratorContext): void {
        const container = context.field.element;
        const attrName = context.name.toLowerCase();
        const metadata = context.metadata;

        const needExpression =
            metadata?.expressionCapable ||
            metadata?.fieldKind === 'EXPRESSION' ||
            attrName.includes('expression');

        if (needExpression) {
            this.addAdvancedEditorButton(container, context.name, 'Expression Builder', 'expression');
        }

        if (attrName.includes('xpath')) {
            this.addAdvancedEditorButton(container, context.name, 'XPath Tester', 'xpath');
        }

        const needRegex =
            metadata?.format === 'regex' ||
            attrName.includes('regex') ||
            attrName.includes('pattern');

        if (needRegex) {
            this.addAdvancedEditorButton(container, context.name, 'Regex Validator', 'regex');
        }
    }

    private invokeUiTool(tool: UiToolDefinition): void {
        const toolId = (tool.id || tool.className || '').toLowerCase();

        if (toolId.includes('expression')) {
            this.openAdvancedEditor('body', 'expression');
            return;
        }

        if (toolId.includes('xpath')) {
            this.openAdvancedEditor('expression', 'xpath');
            return;
        }

        if (toolId.includes('regex')) {
            this.openAdvancedEditor('pattern', 'regex');
            return;
        }

        if (toolId.includes('http') && this.currentForm) {
            this.currentForm.querySelector('.http-request-section')?.scrollIntoView({ behavior: 'smooth' });
            return;
        }

        console.info('[PropertyPanel] No handler for tool', tool);
    }

    private updateValidationState(): void {
        if (!this.schemaEditor || !this.schemaEditorActive) {
            this.shell.showValidationSummary([]);
            this.shell.setApplyDisabled(false);
            return;
        }
        const errors = this.schemaEditor.validate();
        this.shell.showValidationSummary(errors);
        this.shell.setApplyDisabled(errors.length > 0);
    }

    private handleApply(): void {
        this.updateValidationState();
        if (this.schemaEditor && this.schemaEditor.validate().length > 0) {
            return;
        }
        this.emitChange();
        if (this.currentNode) {
            this.originalNode = this.deepCloneNode(this.currentNode);
        }
    }

    private handleCancel(): void {
        if (!this.originalNode) {
            return;
        }
        this.currentNode = this.deepCloneNode(this.originalNode);
        void this.render();
    }

    private emitChange(): void {
        if (!this.currentNode) {
            return;
        }
        this.onChangeCallback?.(this.deepCloneNode(this.currentNode));
    }

    updateFocusSummary(payload: { nodeId: string; summary: ContainerSummary } | null): void {
        if (payload) {
            this.currentFocusSummary = {
                nodeId: payload.nodeId,
                summary: payload.summary
            };
        } else {
            this.currentFocusSummary = null;
        }

        if (this.currentForm) {
            this.appendFocusSummarySection(this.currentForm);
        }
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

    private appendFocusSummarySection(form: HTMLElement): void {
        if (this.focusSummaryElement) {
            this.focusSummaryElement.remove();
            this.focusSummaryElement = null;
        }

        if (!this.currentNode || !this.currentFocusSummary || this.currentFocusSummary.nodeId !== this.currentNode.id) {
            return;
        }

        this.focusSummaryElement = this.createFocusSummaryPanel(this.currentFocusSummary.summary);
        form.appendChild(this.focusSummaryElement);
    }

    private createFocusSummaryPanel(summary: ContainerSummary): HTMLElement {
        const panel = document.createElement('section');
        panel.className = 'property-focus-summary';

        const titleRow = document.createElement('div');
        titleRow.className = 'property-focus-summary__title';
        titleRow.textContent = 'Focus overview';
        panel.appendChild(titleRow);

        const metricsRow = document.createElement('div');
        metricsRow.className = 'property-focus-summary__metrics';
        metricsRow.appendChild(this.createFocusChip('Children', summary.childCount.toString()));
        metricsRow.appendChild(this.createFocusChip('Nested containers', summary.containerCount.toString()));
        metricsRow.appendChild(this.createFocusChip('Leaves', summary.leafCount.toString()));
        metricsRow.appendChild(this.createFocusChip('Details', summary.detailCount.toString()));
        metricsRow.appendChild(this.createFocusChip('Execution', 'Idle'));
        panel.appendChild(metricsRow);

        if (summary.attributeHighlights.length > 0) {
            const highlightList = document.createElement('ul');
            highlightList.className = 'property-focus-summary__list';
            summary.attributeHighlights.forEach(({ key, value }) => {
                const item = document.createElement('li');
                item.className = 'property-focus-summary__list-item';
                item.textContent = `${key}: ${value}`;
                highlightList.appendChild(item);
            });
            panel.appendChild(highlightList);
        }

        if (summary.detailBreakdown.length > 0) {
            const detailGrid = document.createElement('div');
            detailGrid.className = 'property-focus-summary__detail-grid';

            summary.detailBreakdown.forEach(entry => {
                const card = document.createElement('div');
                card.className = 'property-focus-summary__detail-card';

                const heading = document.createElement('div');
                heading.className = 'property-focus-summary__detail-title';
                heading.textContent = `${entry.title} (${entry.count})`;
                card.appendChild(heading);

                const list = document.createElement('ul');
                list.className = 'property-focus-summary__detail-list';
                entry.preview.forEach(text => {
                    const li = document.createElement('li');
                    li.className = 'property-focus-summary__detail-item';
                    li.textContent = text;
                    list.appendChild(li);
                });
                const remaining = entry.count - entry.preview.length;
                if (remaining > 0) {
                    const li = document.createElement('li');
                    li.className = 'property-focus-summary__detail-item property-focus-summary__detail-item--muted';
                    li.textContent = `+${remaining} more…`;
                    list.appendChild(li);
                }
                card.appendChild(list);
                detailGrid.appendChild(card);
            });

            panel.appendChild(detailGrid);
        }

        return panel;
    }

    private createFocusChip(label: string, value: string): HTMLElement {
        const chip = document.createElement('div');
        chip.className = 'property-focus-summary__chip';

        const labelEl = document.createElement('span');
        labelEl.className = 'property-focus-summary__chip-label';
        labelEl.textContent = label;
        chip.appendChild(labelEl);

        const valueEl = document.createElement('span');
        valueEl.className = 'property-focus-summary__chip-value';
        valueEl.textContent = value;
        chip.appendChild(valueEl);

        return chip;
    }

    private renderHttpRequestSection(form: HTMLElement): void {
        if (!this.currentNode) {
            return;
        }

        this.currentNode.attributes = this.currentNode.attributes || {};
        const attrs = this.currentNode.attributes;

        const requestSection = document.createElement('div');
        requestSection.className = 'http-request-section';

        const methodField = this.createLabeledField('Method', 'Select HTTP method for the request.');
        const methodSelect = document.createElement('select');
        methodSelect.className = 'detail-input';

        HTTP_METHODS.forEach(method => {
            const option = document.createElement('option');
            option.value = method;
            option.textContent = method;
            methodSelect.appendChild(option);
        });

        let currentMethod = (attrs.method || 'GET').toString().toUpperCase();
        if (!HTTP_METHODS.includes(currentMethod)) {
            currentMethod = 'GET';
        }
        attrs.method = currentMethod;
        methodSelect.value = currentMethod;

        methodSelect.addEventListener('change', () => {
            const selected = methodSelect.value.toUpperCase();
            attrs.method = selected;
            this.onChangeCallback?.(this.deepCloneNode(this.currentNode!));
            const shouldShow = METHODS_WITH_BODY.has(selected);
            bodyField.container.style.display = shouldShow ? 'flex' : 'none';
        });

        methodField.control.appendChild(methodSelect);
        requestSection.appendChild(methodField.container);

        const urlField = this.createLabeledField('URL', 'Fully qualified HTTP or HTTPS URL.');
        const urlInput = document.createElement('input');
        urlInput.type = 'url';
        urlInput.required = true;
        urlInput.placeholder = 'https://service.example.com/resource';
        urlInput.className = 'detail-input';
        urlInput.value = attrs.url || '';
        urlInput.addEventListener('input', () => {
            attrs.url = urlInput.value;
            this.onChangeCallback?.(this.deepCloneNode(this.currentNode!));
        });

        urlField.control.appendChild(urlInput);
        requestSection.appendChild(urlField.container);

        const bodyField = this.createLabeledField('Body', 'Optional request payload for methods that support it.');
        const bodyInput = document.createElement('textarea');
        bodyInput.className = 'detail-input detail-input--textarea';
        bodyInput.placeholder = '{ "payload": true }';
        bodyInput.value = attrs.body || '';
        bodyInput.addEventListener('input', () => {
            attrs.body = bodyInput.value;
            this.onChangeCallback?.(this.deepCloneNode(this.currentNode!));
        });

        bodyField.control.appendChild(bodyInput);
        const shouldShowBody = METHODS_WITH_BODY.has(currentMethod);
        bodyField.container.style.display = shouldShowBody ? 'flex' : 'none';
        requestSection.appendChild(bodyField.container);

        form.appendChild(requestSection);
    }

    private createLabeledField(label: string, description?: string): { container: HTMLDivElement; control: HTMLDivElement } {
        const container = document.createElement('div');
        container.className = 'http-request-field';

        const labelEl = document.createElement('label');
        labelEl.textContent = label;
        container.appendChild(labelEl);

        if (description) {
            const descEl = document.createElement('small');
            descEl.textContent = description;
            container.appendChild(descEl);
        }

        const control = document.createElement('div');
        control.className = 'http-request-field__control';
        container.appendChild(control);

        return { container, control };
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

    private deepCloneNode(node: VisualNode): VisualNode {
        if (typeof structuredClone === 'function') {
            return structuredClone(node);
        }
        return JSON.parse(JSON.stringify(node));
    }

    /**
     * Get human-readable label for semantic role
     */
    private getRoleLabel(role: string): string {
        const labels: Record<string, string> = {
            'ROOT_CONTAINER': 'Root Container',
            'CONTEXT_CONTAINER': 'Context Container',
            'BODY_EVALUATOR': 'Body Evaluator',
            'DEPENDENT': 'Dependent',
            'LEAF': 'Leaf'
        };
        return labels[role] || role;
    }

    /**
     * Get color for semantic role
     */
    private getRoleColor(role: string): string {
        const colors: Record<string, string> = {
            'ROOT_CONTAINER': '#9C27B0',
            'CONTEXT_CONTAINER': '#2196F3',
            'BODY_EVALUATOR': '#4CAF50',
            'DEPENDENT': '#FF9800',
            'LEAF': '#757575'
        };
        return colors[role] || '#999';
    }
}





