/**
 * PropertyPanel - Simple Implementation
 * Displays and edits properties of selected node
 */

import type { VisualNode, Plugin } from '../types';

export class PropertyPanel {
    private container: HTMLElement;
    private currentNode: VisualNode | null = null;
    private onChangeCallback: ((node: VisualNode) => void) | null = null;

    constructor(containerId: string) {
        const container = document.getElementById(containerId);

        if (!container) {
            throw new Error(`Container with ID "${containerId}" not found`);
        }

        this.container = container;
    }

    /**
     * Show properties for node
     */
    showNode(node: VisualNode): void {
        this.currentNode = { ...node };
        this.render();
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
     * Render properties
     */
    private render(): void {
        if (!this.currentNode) {
            return;
        }

        this.container.innerHTML = '';

        const form = document.createElement('div');
        form.style.padding = '16px';

        // Type (read-only)
        const typeLabel = document.createElement('div');
        typeLabel.style.marginBottom = '12px';
        typeLabel.innerHTML = `<strong>Type:</strong> ${this.currentNode.type}`;
        form.appendChild(typeLabel);

        // Attributes
        const attrsLabel = document.createElement('div');
        attrsLabel.innerHTML = '<strong>Attributes:</strong>';
        attrsLabel.style.marginBottom = '8px';
        form.appendChild(attrsLabel);

        Object.entries(this.currentNode.attributes).forEach(([key, value]) => {
            const input = this.createInput(key, value);
            form.appendChild(input);
        });

        // Body
        if (this.currentNode.body !== null) {
            const bodyLabel = document.createElement('div');
            bodyLabel.innerHTML = '<strong>Body:</strong>';
            bodyLabel.style.marginTop = '12px';
            bodyLabel.style.marginBottom = '8px';
            form.appendChild(bodyLabel);

            const bodyInput = document.createElement('textarea');
            bodyInput.value = this.currentNode.body || '';
            bodyInput.style.width = '100%';
            bodyInput.style.minHeight = '80px';
            bodyInput.style.padding = '8px';
            bodyInput.style.border = '1px solid #ddd';
            bodyInput.style.borderRadius = '4px';
            bodyInput.onchange = () => {
                if (this.currentNode && this.onChangeCallback) {
                    this.currentNode.body = bodyInput.value;
                    this.onChangeCallback(this.currentNode);
                }
            };
            form.appendChild(bodyInput);
        }

        this.container.appendChild(form);
    }

    /**
     * Create input for attribute
     */
    private createInput(key: string, value: string): HTMLElement {
        const container = document.createElement('div');
        container.style.marginBottom = '8px';

        const label = document.createElement('label');
        label.textContent = key + ':';
        label.style.display = 'block';
        label.style.fontSize = '12px';
        label.style.marginBottom = '4px';
        container.appendChild(label);

        const input = document.createElement('input');
        input.type = 'text';
        input.value = value;
        input.style.width = '100%';
        input.style.padding = '6px';
        input.style.border = '1px solid #ddd';
        input.style.borderRadius = '4px';
        input.onchange = () => {
            if (this.currentNode && this.onChangeCallback) {
                this.currentNode.attributes[key] = input.value;
                this.onChangeCallback(this.currentNode);
            }
        };
        container.appendChild(input);

        return container;
    }
}

