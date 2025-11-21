import type { AttributeDefinition, AttributeUiMetadata, Plugin } from '../../types';
import { createField, DEFAULT_GROUP_LABEL, type PropertyField } from './fields';

export interface ValidationError {
    field: string;
    message: string;
}

export interface FieldDecoratorContext {
    name: string;
    metadata?: AttributeUiMetadata;
    field: PropertyField;
}

export type FieldDecorator = (context: FieldDecoratorContext) => void;

interface AttributeEntry {
    definition: AttributeDefinition;
    metadataGroup: string;
    order: number;
}

export class SchemaDrivenEditor {
    private container: HTMLElement | null = null;
    private plugin: Plugin | null = null;
    private currentAttributes: Record<string, string> = {};
    private fields: Map<string, PropertyField> = new Map();
    private changeCallback: ((attributes: Record<string, string>) => void) | null = null;
    private fieldDecorator?: FieldDecorator;

    generateEditor(
        container: HTMLElement,
        plugin: Plugin,
        currentAttributes: Record<string, string>
    ): void {
        this.container = container;
        this.plugin = plugin;
        this.currentAttributes = { ...currentAttributes };
        this.fields.clear();

        container.innerHTML = '';
        container.className = 'schema-driven-editor';
        container.style.display = 'flex';
        container.style.flexDirection = 'column';
        container.style.gap = '12px';

        const attributes = plugin.schema?.attributes || [];
        if (attributes.length === 0) {
            container.textContent = 'No editable properties.';
            return;
        }

        const attributeUi = plugin.attributeUi || plugin.ui?.fields || {};
        const grouped = this.groupAttributes(attributes, attributeUi);

        grouped.forEach(group => {
            const section = document.createElement('section');
            section.style.border = '1px solid var(--gray-150)';
            section.style.borderRadius = 'var(--radius-md)';
            section.style.padding = '12px 16px';
            section.style.background = 'var(--gray-0)';

            if (group.label !== DEFAULT_GROUP_LABEL || grouped.length > 1) {
                const heading = document.createElement('h3');
                heading.textContent = group.label;
                heading.style.fontSize = '13px';
                heading.style.fontWeight = '600';
                heading.style.margin = '0 0 8px';
                heading.style.textTransform = 'uppercase';
                heading.style.color = 'var(--gray-600)';
                section.appendChild(heading);
            }

            group.attributes.forEach(entry => {
                const value = this.currentAttributes[entry.definition.name]
                    ?? entry.definition.defaultValue
                    ?? '';

                const field = createField({
                    attribute: entry.definition,
                    metadata: attributeUi[entry.definition.name],
                    value,
                    onChange: newValue => this.handleFieldChange(entry.definition.name, newValue)
                });

                this.fields.set(entry.definition.name, field);
                section.appendChild(field.element);

                if (this.fieldDecorator) {
                    this.fieldDecorator({
                        name: entry.definition.name,
                        metadata: attributeUi[entry.definition.name],
                        field
                    });
                }
            });

            container.appendChild(section);
        });
    }

    onChange(callback: (attributes: Record<string, string>) => void): void {
        this.changeCallback = callback;
    }

    validate(): ValidationError[] {
        const errors: ValidationError[] = [];
        this.fields.forEach((field, attributeName) => {
            const message = field.validate();
            if (message) {
                errors.push({ field: attributeName, message });
            }
        });
        return errors;
    }

    private handleFieldChange(name: string, value: string): void {
        this.currentAttributes[name] = value;
        if (this.changeCallback) {
            this.changeCallback({ ...this.currentAttributes });
        }
    }

    setFieldDecorator(decorator?: FieldDecorator): void {
        this.fieldDecorator = decorator;
    }

    private groupAttributes(
        attributes: AttributeDefinition[],
        attributeUi: Record<string, any>
    ): Array<{ label: string; attributes: AttributeEntry[] }> {
        const groups = new Map<string, AttributeEntry[]>();
        const orders = new Map<string, number>();

        attributes.forEach(attr => {
            const metadata = attributeUi[attr.name];
            const groupName = metadata?.group || DEFAULT_GROUP_LABEL;
            const entry: AttributeEntry = {
                definition: attr,
                metadataGroup: groupName,
                order: metadata?.order ?? 0
            };

            if (!groups.has(groupName)) {
                groups.set(groupName, []);
                orders.set(groupName, entry.order);
            } else {
                const currentOrder = orders.get(groupName) ?? entry.order;
                orders.set(groupName, Math.min(currentOrder, entry.order));
            }

            groups.get(groupName)?.push(entry);
        });

        const result = Array.from(groups.entries()).map(([label, entries]) => ({
            label,
            attributes: entries.sort((a, b) => {
                if (a.order === b.order) {
                    return a.definition.name.localeCompare(b.definition.name);
                }
                return a.order - b.order;
            })
        }));

        result.sort((a, b) => {
            const orderA = orders.get(a.label) ?? 0;
            const orderB = orders.get(b.label) ?? 0;
            if (orderA === orderB) {
                return a.label.localeCompare(b.label);
            }
            return orderA - orderB;
        });

        return result;
    }
}


