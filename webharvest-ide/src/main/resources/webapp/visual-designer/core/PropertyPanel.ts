/**
 * Property Panel - WORKING property editor with forms
 * Generates dynamic forms from plugin schemas
 */

import type { VisualNode } from '../types/VisualNode';
import { renderIcon } from '../utils/IconRenderer.js';

export class PropertyPanel {
    private container: HTMLElement;
    private currentNode: VisualNode | null = null;
    private onPropertyChange: (nodeId: string, config: Record<string, any>, body: string | null) => void;

    constructor(
        containerId: string,
        onPropertyChange: (nodeId: string, config: Record<string, any>, body: string | null) => void
    ) {
        const element = document.getElementById(containerId);
        if (!element) {
            throw new Error(`Property container not found: ${containerId}`);
        }
        this.container = element;
        this.onPropertyChange = onPropertyChange;

        this.showEmpty();
        console.log('📝 Property Panel initialized');
    }

    /**
     * Show properties for selected node
     */
    public showProperties(node: VisualNode): void {
        this.currentNode = node;

        const html = `
      <div class="property-panel-content">
        <!-- Header with icon and title -->
        <div class="property-header">
          <div class="property-icon">${renderIcon(node.plugin.icon)}</div>
          <div class="property-title">
            <h3>${node.plugin.name}</h3>
            <p style="font-size: 13px; color: var(--text-secondary);">${node.plugin.description}</p>
          </div>
        </div>
        
        <!-- Attributes Form -->
        ${this.renderAttributesForm(node)}
        
        <!-- Body Editor (if plugin allows body) -->
        ${node.plugin.schema.allowsBody ? this.renderBodyEditor(node) : ''}
        
        <!-- Action Buttons -->
        <div class="property-actions">
          <button class="btn btn-primary" id="apply-properties">
            <i class="fas fa-check"></i> Apply
          </button>
          <button class="btn btn-secondary" id="cancel-properties">
            <i class="fas fa-times"></i> Cancel
          </button>
        </div>
      </div>
    `;

        this.container.innerHTML = html;
        this.setupEventHandlers();
    }

    /**
     * Render attributes form from schema
     */
    private renderAttributesForm(node: VisualNode): string {
        const attrs = node.plugin.schema.attributes || [];

        if (attrs.length === 0) {
            return '<p style="padding: var(--spacing-4); color: var(--text-secondary);">No configurable attributes</p>';
        }

        const fields = attrs.map(attr => this.renderAttributeField(attr, node.config[attr.name])).join('');

        return `
      <div class="property-section">
        <h4 style="margin-bottom: var(--spacing-3); font-weight: 600;">Attributes</h4>
        <div class="property-form">
          ${fields}
        </div>
      </div>
    `;
    }

    /**
     * Render single attribute field based on type
     */
    private renderAttributeField(attr: any, currentValue: any): string {
        const value = currentValue ?? attr.defaultValue ?? '';
        const requiredMark = attr.required ? '<span style="color: #F44336;">*</span>' : '';

        let input = '';

        switch (attr.type?.toLowerCase()) {
            case 'boolean':
                input = `
          <label class="checkbox-label">
            <input type="checkbox" 
                   name="${attr.name}" 
                   ${value === true || value === 'true' ? 'checked' : ''}
                   class="form-checkbox" />
            <span>${attr.description || attr.name}</span>
          </label>
        `;
                break;

            case 'enum':
                const options = (attr.allowedValues || attr.options || []).map((opt: string) =>
                    `<option value="${opt}" ${value === opt ? 'selected' : ''}>${opt}</option>`
                ).join('');
                input = `
          <label class="form-label">${attr.name} ${requiredMark}</label>
          <select name="${attr.name}" class="form-select" ${attr.required ? 'required' : ''}>
            ${!attr.required ? '<option value="">-- Select --</option>' : ''}
            ${options}
          </select>
          ${attr.description ? `<span class="form-hint">${attr.description}</span>` : ''}
        `;
                break;

            case 'integer':
            case 'number':
                input = `
          <label class="form-label">${attr.name} ${requiredMark}</label>
          <input type="number" 
                 name="${attr.name}" 
                 value="${value}"
                 ${attr.min !== undefined ? `min="${attr.min}"` : ''}
                 ${attr.max !== undefined ? `max="${attr.max}"` : ''}
                 ${attr.required ? 'required' : ''}
                 class="form-input" 
                 placeholder="${attr.description || ''}" />
          ${attr.description ? `<span class="form-hint">${attr.description}</span>` : ''}
        `;
                break;

            case 'url':
                input = `
          <label class="form-label">${attr.name} ${requiredMark}</label>
          <input type="url" 
                 name="${attr.name}" 
                 value="${value}"
                 ${attr.required ? 'required' : ''}
                 class="form-input" 
                 placeholder="${attr.description || 'https://example.com'}" />
          ${attr.description ? `<span class="form-hint">${attr.description}</span>` : ''}
        `;
                break;

            default: // string
                input = `
          <label class="form-label">${attr.name} ${requiredMark}</label>
          <input type="text" 
                 name="${attr.name}" 
                 value="${value}"
                 ${attr.pattern ? `pattern="${attr.pattern}"` : ''}
                 ${attr.required ? 'required' : ''}
                 class="form-input" 
                 placeholder="${attr.description || ''}" />
          ${attr.description ? `<span class="form-hint">${attr.description}</span>` : ''}
        `;
        }

        return `<div class="form-field">${input}</div>`;
    }

    /**
     * Render body content editor
     */
    private renderBodyEditor(node: VisualNode): string {
        return `
      <div class="property-section">
        <h4 style="margin-bottom: var(--spacing-3); font-weight: 600;">Body Content</h4>
        <textarea 
          id="node-body-editor"
          class="form-textarea"
          rows="8"
          placeholder="Enter content here...">${node.body || ''}</textarea>
        <span class="form-hint">Supports variable substitution with \${variableName}</span>
      </div>
    `;
    }

    /**
     * Setup event handlers
     */
    private setupEventHandlers(): void {
        const applyBtn = document.getElementById('apply-properties');
        const cancelBtn = document.getElementById('cancel-properties');

        applyBtn?.addEventListener('click', () => this.applyChanges());
        cancelBtn?.addEventListener('click', () => this.cancelChanges());

        // Real-time validation
        this.container.querySelectorAll('.form-input, .form-select, .form-checkbox').forEach(input => {
            input.addEventListener('input', () => this.validateForm());
        });
    }

    /**
     * Apply property changes
     */
    private applyChanges(): void {
        if (!this.currentNode) return;

        // Collect form values
        const config: Record<string, any> = {};

        this.container.querySelectorAll('.form-input, .form-select').forEach(input => {
            const element = input as HTMLInputElement;
            const name = element.name;
            let value: any = element.value;

            // Type conversion
            if (element.type === 'number') {
                value = value ? parseFloat(value) : null;
            } else if (element.type === 'checkbox') {
                value = (element as HTMLInputElement).checked;
            }

            if (value !== null && value !== '') {
                config[name] = value;
            }
        });

        // Get body content
        const bodyEditor = document.getElementById('node-body-editor') as HTMLTextAreaElement;
        const body = bodyEditor ? bodyEditor.value.trim() || null : null;

        // Validate required attributes
        const requiredAttrs = this.currentNode.plugin.schema.requiredAttributes || [];
        const missing = requiredAttrs.filter(attr => !config[attr]);

        if (missing.length > 0) {
            alert(`Missing required attributes: ${missing.join(', ')}`);
            return;
        }

        // Notify parent
        this.onPropertyChange(this.currentNode.id, config, body);

        console.log(`✅ Properties applied to ${this.currentNode.type}`);
        this.showSuccessMessage();
    }

    /**
     * Cancel changes
     */
    private cancelChanges(): void {
        if (this.currentNode) {
            this.showProperties(this.currentNode);
        }
    }

    /**
     * Validate form
     */
    private validateForm(): void {
        // TODO: Real-time validation
    }

    /**
     * Show empty state
     */
    public showEmpty(): void {
        this.container.innerHTML = `
      <div class="property-panel-empty">
        <div class="empty-state-icon">
          <i class="fas fa-mouse-pointer"></i>
        </div>
        <h3>No Selection</h3>
        <p>Select a node on the canvas to edit its properties</p>
      </div>
    `;
    }

    /**
     * Show success message
     */
    private showSuccessMessage(): void {
        const toast = document.createElement('div');
        toast.className = 'toast success';
        toast.innerHTML = `
      <i class="fas fa-check-circle"></i>
      <span>Properties saved successfully</span>
    `;

        const toastContainer = document.querySelector('.toast-container') || document.body;
        toastContainer.appendChild(toast);

        setTimeout(() => toast.remove(), 3000);
    }
}

