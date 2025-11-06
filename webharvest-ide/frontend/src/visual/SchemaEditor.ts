/**
 * SchemaEditor - Implementation
 * Generates smart property editors based on plugin schema
 */

import type { Plugin, AttributeDefinition } from '../types';

export interface EditorField {
    name: string;
    type: string;
    element: HTMLElement;
    required: boolean;
}

export interface ValidationError {
    field: string;
    message: string;
}

export class SchemaEditor {
    private container: HTMLElement | null = null;
    private fields: Map<string, EditorField> = new Map();
    private plugin: Plugin | null = null;
    private onChangeCallback: ((attributes: Record<string, string>) => void) | null = null;

    /**
     * Generate editor form based on plugin schema
     */
    generateEditor(
        container: HTMLElement,
        plugin: Plugin,
        currentAttributes: Record<string, string>
    ): void {
        this.container = container;
        this.plugin = plugin;
        this.fields.clear();

        // Clear container
        container.innerHTML = '';

        // Create form
        const form = document.createElement('div');
        form.className = 'schema-editor-form';
        form.style.cssText = `
            display: flex;
            flex-direction: column;
            gap: 16px;
            padding: 16px;
        `;

        // Generate fields for each attribute
        plugin.schema.attributes.forEach(attrDef => {
            const field = this.createField(attrDef, currentAttributes[attrDef.name] || attrDef.defaultValue || '');
            form.appendChild(field.element);
            this.fields.set(attrDef.name, field);
        });

        container.appendChild(form);
    }

    /**
     * Create form field based on attribute definition
     */
    private createField(attrDef: AttributeDefinition, value: string): EditorField {
        const fieldContainer = document.createElement('div');
        fieldContainer.className = 'editor-field';
        fieldContainer.style.cssText = `
            display: flex;
            flex-direction: column;
            gap: 6px;
        `;

        // Label with tooltip
        const label = document.createElement('label');
        label.textContent = attrDef.name + (attrDef.required ? ' *' : '');
        label.style.cssText = `
            font-size: 13px;
            font-weight: 600;
            color: var(--text-primary);
        `;
        // Add tooltip with description if available
        if (attrDef.description) {
            label.title = attrDef.description;
            label.style.cursor = 'help';
        }
        fieldContainer.appendChild(label);

        // Description
        if (attrDef.description) {
            const description = document.createElement('div');
            description.textContent = attrDef.description;
            description.style.cssText = `
                font-size: 11px;
                color: var(--gray-600);
                margin-top: -4px;
            `;
            // Also add tooltip for description
            description.title = attrDef.description;
            fieldContainer.appendChild(description);
        }

        // Input element
        let inputElement: HTMLInputElement | HTMLSelectElement | HTMLTextAreaElement;

        switch (attrDef.type) {
            case 'boolean':
                inputElement = this.createCheckbox(attrDef, value);
                break;

            case 'number':
                inputElement = this.createNumberInput(attrDef, value);
                break;

            case 'url':
                inputElement = this.createUrlInput(attrDef, value);
                break;

            case 'enum':
                inputElement = this.createSelect(attrDef, value);
                break;

            case 'xpath':
                inputElement = this.createTextarea(attrDef, value);
                break;

            case 'string':
            default:
                inputElement = this.createTextInput(attrDef, value);
                break;
        }

        inputElement.name = attrDef.name;
        inputElement.addEventListener('input', () => this.handleChange());
        inputElement.addEventListener('change', () => this.handleChange());

        fieldContainer.appendChild(inputElement);

        return {
            name: attrDef.name,
            type: attrDef.type,
            element: fieldContainer,
            required: attrDef.required
        };
    }

    /**
     * Create text input
     */
    private createTextInput(attrDef: AttributeDefinition, value: string): HTMLInputElement {
        const input = document.createElement('input');
        input.type = 'text';
        input.value = value;
        input.required = attrDef.required;
        input.placeholder = attrDef.defaultValue || '';
        this.applyInputStyles(input);
        return input;
    }

    /**
     * Create URL input
     */
    private createUrlInput(attrDef: AttributeDefinition, value: string): HTMLInputElement {
        const input = document.createElement('input');
        input.type = 'url';
        input.value = value;
        input.required = attrDef.required;
        input.placeholder = 'http://example.com';
        this.applyInputStyles(input);
        return input;
    }

    /**
     * Create number input
     */
    private createNumberInput(attrDef: AttributeDefinition, value: string): HTMLInputElement {
        const input = document.createElement('input');
        input.type = 'number';
        input.value = value;
        input.required = attrDef.required;
        this.applyInputStyles(input);
        return input;
    }

    /**
     * Create checkbox
     */
    private createCheckbox(attrDef: AttributeDefinition, value: string): HTMLInputElement {
        const input = document.createElement('input');
        input.type = 'checkbox';
        input.checked = value === 'true';
        input.required = attrDef.required;
        input.style.cssText = `
            width: 18px;
            height: 18px;
            cursor: pointer;
        `;

        return input;
    }

    /**
     * Create select dropdown
     */
    private createSelect(attrDef: AttributeDefinition, value: string): HTMLSelectElement {
        const select = document.createElement('select');
        select.required = attrDef.required;

        if (attrDef.allowedValues) {
            attrDef.allowedValues.forEach(optionValue => {
                const option = document.createElement('option');
                option.value = optionValue;
                option.textContent = optionValue;
                option.selected = optionValue === value;
                select.appendChild(option);
            });
        }

        this.applyInputStyles(select);
        return select;
    }

    /**
     * Create textarea
     */
    private createTextarea(attrDef: AttributeDefinition, value: string): HTMLTextAreaElement {
        const textarea = document.createElement('textarea');
        textarea.value = value;
        textarea.required = attrDef.required;
        textarea.rows = 3;
        this.applyInputStyles(textarea);
        return textarea;
    }

    /**
     * Apply consistent styling to input elements
     */
    private applyInputStyles(element: HTMLInputElement | HTMLSelectElement | HTMLTextAreaElement): void {
        element.style.cssText = `
            padding: 8px 12px;
            border: 1px solid var(--gray-300);
            border-radius: 6px;
            font-size: 13px;
            font-family: inherit;
            transition: all 0.2s;
        `;

        element.addEventListener('focus', () => {
            element.style.borderColor = 'var(--magenta)';
            element.style.outline = 'none';
            element.style.boxShadow = '0 0 0 3px rgba(216, 27, 96, 0.1)';
        });

        element.addEventListener('blur', () => {
            element.style.borderColor = 'var(--gray-300)';
            element.style.boxShadow = 'none';
        });
    }

    /**
     * Validate all fields
     */
    validate(): ValidationError[] {
        const errors: ValidationError[] = [];

        if (!this.plugin || !this.container) {
            return errors;
        }

        this.plugin.schema.attributes.forEach(attrDef => {
            const field = this.fields.get(attrDef.name);
            if (!field) return;

            const input = field.element.querySelector('input, select, textarea') as
                HTMLInputElement | HTMLSelectElement | HTMLTextAreaElement;

            if (!input) return;

            const value = input.type === 'checkbox'
                ? (input as HTMLInputElement).checked.toString()
                : input.value;

            // Required field validation
            if (attrDef.required && !value) {
                errors.push({
                    field: attrDef.name,
                    message: `${attrDef.name} is required`
                });
                this.markFieldError(field.element);
            }

            // Type-specific validation
            if (value) {
                switch (attrDef.type) {
                    case 'url':
                        if (!this.isValidUrl(value)) {
                            errors.push({
                                field: attrDef.name,
                                message: `${attrDef.name} must be a valid URL`
                            });
                            this.markFieldError(field.element);
                        }
                        break;

                    case 'number':
                        if (isNaN(Number(value))) {
                            errors.push({
                                field: attrDef.name,
                                message: `${attrDef.name} must be a number`
                            });
                            this.markFieldError(field.element);
                        }
                        break;
                }
            }

            // Clear error if valid
            if (errors.length === 0 || !errors.some(e => e.field === attrDef.name)) {
                this.clearFieldError(field.element);
            }
        });

        return errors;
    }

    /**
     * Mark field as having error
     */
    private markFieldError(fieldElement: HTMLElement): void {
        const input = fieldElement.querySelector('input, select, textarea') as HTMLElement;
        if (input) {
            input.style.borderColor = 'var(--magenta)';
            input.style.background = '#ffe6f0';
        }
    }

    /**
     * Clear field error state
     */
    private clearFieldError(fieldElement: HTMLElement): void {
        const input = fieldElement.querySelector('input, select, textarea') as HTMLElement;
        if (input) {
            input.style.borderColor = 'var(--gray-300)';
            input.style.background = '';
        }
    }

    /**
     * Validate URL format
     */
    private isValidUrl(value: string): boolean {
        try {
            new URL(value);
            return true;
        } catch {
            return false;
        }
    }

    /**
     * Get current attribute values
     */
    getValue(): Record<string, string> {
        const values: Record<string, string> = {};

        this.fields.forEach((field, name) => {
            const input = field.element.querySelector('input, select, textarea') as
                HTMLInputElement | HTMLSelectElement | HTMLTextAreaElement;

            if (input) {
                if (input.type === 'checkbox') {
                    values[name] = (input as HTMLInputElement).checked.toString();
                } else {
                    values[name] = input.value;
                }
            }
        });

        return values;
    }

    /**
     * Register onChange callback
     */
    onChange(callback: (attributes: Record<string, string>) => void): void {
        this.onChangeCallback = callback;
    }

    /**
     * Handle field change
     */
    private handleChange(): void {
        if (this.onChangeCallback) {
            const values = this.getValue();
            this.onChangeCallback(values);
        }
    }

    /**
     * Clear editor
     */
    clear(): void {
        if (this.container) {
            this.container.innerHTML = '';
        }
        this.fields.clear();
        this.plugin = null;
    }
}


