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
        container.style.gap = '0';
        container.style.padding = '0';
        container.style.maxWidth = '100%';

        const attributes = plugin.schema?.attributes || [];
        if (attributes.length === 0) {
            container.textContent = 'No editable properties.';
            return;
        }

        const attributeUi = plugin.attributeUi || plugin.ui?.fields || {};
        const grouped = this.groupAttributes(attributes, attributeUi);

        // Faza 5: Progressive disclosure - podziel na Basic i Advanced
        const basicAttributes: typeof grouped = [];
        const advancedAttributes: typeof grouped = [];

        grouped.forEach(group => {
            const basicGroup = { ...group, attributes: [] as typeof group.attributes };
            const advancedGroup = { ...group, attributes: [] as typeof group.attributes };

            group.attributes.forEach(entry => {
                // Faza 5: Sprawdź czy atrybut jest advanced (z metadata lub nazwy)
                const metadata = attributeUi[entry.definition.name];
                const isAdvanced = (metadata?.advanced) ||
                                   entry.definition.name.toLowerCase().includes('advanced') ||
                                   entry.definition.name.toLowerCase().includes('debug') ||
                                   entry.definition.name.toLowerCase().includes('internal');
                if (isAdvanced) {
                    advancedGroup.attributes.push(entry);
                } else {
                    basicGroup.attributes.push(entry);
                }
            });

            if (basicGroup.attributes.length > 0) {
                basicAttributes.push(basicGroup);
            }
            if (advancedGroup.attributes.length > 0) {
                advancedAttributes.push(advancedGroup);
            }
        });

        // Render Basic attributes
        basicAttributes.forEach((group, groupIndex) => {
            const section = document.createElement('section');
            section.className = 'schema-driven-group schema-driven-group--basic';
            section.style.marginBottom = groupIndex < basicAttributes.length - 1 ? '16px' : '0';

            if (group.label !== DEFAULT_GROUP_LABEL || basicAttributes.length > 1) {
                const heading = document.createElement('h3');
                heading.textContent = group.label;
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

        // Faza 5: Render Advanced attributes w collapsible sekcji
        if (advancedAttributes.length > 0) {
            const advancedSection = document.createElement('section');
            advancedSection.className = 'schema-driven-group schema-driven-group--advanced';
            
            const advancedToggle = document.createElement('button');
            advancedToggle.type = 'button';
            advancedToggle.className = 'schema-driven-advanced-toggle';
            advancedToggle.textContent = 'Show Advanced Properties';
            advancedToggle.style.cssText = `
                width: 100%;
                padding: 10px;
                margin-top: 16px;
                background: rgba(15, 23, 42, 0.04);
                border: 1px solid rgba(15, 23, 42, 0.1);
                border-radius: 6px;
                cursor: pointer;
                font-size: 13px;
                color: #6b7280;
                transition: all 0.2s ease;
            `;
            
            const advancedContent = document.createElement('div');
            advancedContent.className = 'schema-driven-advanced-content';
            advancedContent.style.display = 'none';
            advancedContent.style.marginTop = '12px';

            let isExpanded = false;
            advancedToggle.addEventListener('click', () => {
                isExpanded = !isExpanded;
                advancedContent.style.display = isExpanded ? 'block' : 'none';
                advancedToggle.textContent = isExpanded ? 'Hide Advanced Properties' : 'Show Advanced Properties';
                advancedToggle.style.background = isExpanded ? 'rgba(15, 23, 42, 0.08)' : 'rgba(15, 23, 42, 0.04)';
            });

            advancedAttributes.forEach((group, groupIndex) => {
                const section = document.createElement('section');
                section.className = 'schema-driven-group';
                section.style.marginBottom = groupIndex < advancedAttributes.length - 1 ? '16px' : '0';

                if (group.label !== DEFAULT_GROUP_LABEL || advancedAttributes.length > 1) {
                    const heading = document.createElement('h3');
                    heading.textContent = group.label;
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

                advancedContent.appendChild(section);
            });

            advancedSection.appendChild(advancedToggle);
            advancedSection.appendChild(advancedContent);
            container.appendChild(advancedSection);
        }
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


