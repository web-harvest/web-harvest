import type { AttributeDefinition, VisualNode } from '../../types';
import type { DetailSectionDefinition } from './MasterDetailRegistry';
import { MasterDetailRegistry } from './MasterDetailRegistry';

export interface MasterDetailEditorOptions {
    node: VisualNode;
    sections: DetailSectionDefinition[];
    registry: MasterDetailRegistry;
    onChange: (node: VisualNode) => void;
}

export class MasterDetailEditor {
    private readonly sections: DetailSectionDefinition[];
    private readonly registry: MasterDetailRegistry;
    private readonly onChange: (node: VisualNode) => void;
    private node: VisualNode;
    private root!: HTMLElement;

    constructor(options: MasterDetailEditorOptions) {
        this.node = this.cloneNode(options.node);
        this.sections = options.sections;
        this.registry = options.registry;
        this.onChange = options.onChange;
    }

    render(): HTMLElement {
        this.root = document.createElement('div');
        this.root.className = 'detail-editor';
        this.renderSections();
        return this.root;
    }

    private renderSections(): void {
        if (!this.root) {
            return;
        }

        this.root.innerHTML = '';

        const partition = this.registry.partitionChildren(this.node);

        this.sections.forEach(section => {
            const detailNodes = partition.sections[section.detailType] || [];
            const element = this.renderSection(section, detailNodes);
            this.root.appendChild(element);
        });
    }

    private renderSection(section: DetailSectionDefinition, details: VisualNode[]): HTMLElement {
        const sectionEl = document.createElement('div');
        sectionEl.className = 'http-detail-section';

        const header = document.createElement('div');
        header.className = 'http-detail-section__header';

        const title = document.createElement('span');
        title.className = 'http-detail-section__title';
        title.textContent = `${section.sectionTitle} (${details.length})`;

        const actions = document.createElement('div');
        actions.className = 'http-detail-section__actions';

        const addButton = document.createElement('button');
        addButton.type = 'button';
        addButton.className = 'http-detail-action';
        addButton.innerHTML = '<i class="fas fa-plus"></i> ' + section.addLabel;
        
        // Disable "Add" button if maxOccurs constraint is reached
        const currentCount = details.length;
        const maxOccurs = section.maxOccurs;
        if (maxOccurs >= 0 && currentCount >= maxOccurs) {
            addButton.disabled = true;
            addButton.title = `Maximum of ${maxOccurs} ${section.sectionTitle.toLowerCase()} allowed`;
        }
        
        addButton.addEventListener('click', (event) => {
            event.stopPropagation();
            if (!addButton.disabled) {
                this.addDetail(section.detailType);
            }
        });

        actions.appendChild(addButton);
        
        // Show warning if minOccurs constraint is not met
        const minOccurs = section.minOccurs;
        if (minOccurs > 0 && currentCount < minOccurs) {
            const warning = document.createElement('div');
            warning.className = 'http-detail-warning';
            warning.style.cssText = 'color: var(--warning-color, #F59E0B); font-size: 12px; margin-top: 8px; padding: 8px; background: var(--warning-bg, #FEF3C7); border-radius: 4px;';
            warning.textContent = `Warning: At least ${minOccurs} ${section.sectionTitle.toLowerCase()} required`;
            sectionEl.appendChild(warning);
        }
        header.appendChild(title);
        header.appendChild(actions);
        sectionEl.appendChild(header);

        const list = document.createElement('div');
        list.className = 'http-detail-list';

        if (details.length === 0) {
            const empty = document.createElement('div');
            empty.className = 'http-detail-empty';
            empty.textContent = section.emptyLabel;
            list.appendChild(empty);
        } else {
            details.forEach((detail, index) => {
                const row = this.renderDetailRow(section, detail, index, details.length);
                list.appendChild(row);
            });
        }

        sectionEl.appendChild(list);
        return sectionEl;
    }

    private renderDetailRow(
        section: DetailSectionDefinition,
        detail: VisualNode,
        index: number,
        total: number
    ): HTMLElement {
        const row = document.createElement('div');
        row.className = 'http-detail-row';
        row.dataset.detailId = detail.id;

        const primaryCell = document.createElement('div');
        primaryCell.className = 'http-detail-cell';

        if (section.primaryAttribute) {
            const primaryMeta = this.findAttributeDefinition(section, section.primaryAttribute);
            const primaryField = this.createDetailField(
                `${section.detailType}-${detail.id}-primary`,
                section.primaryAttributeLabel ?? section.primaryAttribute,
                detail.attributes?.[section.primaryAttribute] ?? '',
                {
                    description: section.primaryAttributeDescription ?? primaryMeta?.description,
                    type: primaryMeta?.type,
                    allowedValues: primaryMeta?.allowedValues
                }
            );
            this.bindAttributeInput(primaryField.input, detail.id, section.primaryAttribute);
            primaryCell.appendChild(primaryField.wrapper);
        } else {
            const summary = this.registry.summarizeDetail(detail);
            const label = document.createElement('span');
            label.className = summary.isEmpty ? 'http-detail-row__label http-detail-row__label--empty' : 'http-detail-row__label';
            label.textContent = summary.primaryText;
            primaryCell.appendChild(label);
        }

        row.appendChild(primaryCell);

        const secondaryCell = document.createElement('div');
        secondaryCell.className = 'http-detail-cell';

        if (section.usesBody) {
            const bodyField = this.createDetailField(
                `${section.detailType}-${detail.id}-body`,
                'Value',
                detail.body ?? '',
                {
                    multiline: true
                }
            );
            bodyField.input.addEventListener('input', () => {
                this.updateDetailBody(detail.id, bodyField.input.value);
            });
            secondaryCell.appendChild(bodyField.wrapper);
        } else if (section.secondaryAttribute) {
            const secondaryInitialValue =
                detail.attributes?.[section.secondaryAttribute] ??
                (typeof detail.body === 'string' ? detail.body : '');

            const secondaryMeta = this.findAttributeDefinition(section, section.secondaryAttribute);
            const secondaryField = this.createDetailField(
                `${section.detailType}-${detail.id}-secondary`,
                section.secondaryAttributeLabel ?? section.secondaryAttribute,
                secondaryInitialValue,
                {
                    description: section.secondaryAttributeDescription ?? secondaryMeta?.description,
                    type: secondaryMeta?.type,
                    allowedValues: secondaryMeta?.allowedValues
                }
            );
            this.bindAttributeInput(
                secondaryField.input,
                detail.id,
                section.secondaryAttribute,
                (nextValue) => {
                    if (typeof detail.body === 'string') {
                        this.updateDetailBody(detail.id, nextValue);
                    }
                }
            );
            secondaryCell.appendChild(secondaryField.wrapper);
        } else {
            const summary = this.registry.summarizeDetail(detail);
            const valueEl = document.createElement('span');
            valueEl.className = summary.isEmpty ? 'http-detail-row__value http-detail-row__value--empty' : 'http-detail-row__value';
            valueEl.textContent = summary.secondaryText ?? '—';
            secondaryCell.appendChild(valueEl);
        }

        row.appendChild(secondaryCell);

        const additionalCell = this.renderAdditionalAttributes(section, detail);
        if (additionalCell) {
            row.appendChild(additionalCell);
        }

        const actions = document.createElement('div');
        actions.className = 'http-detail-row__actions';

        const duplicateBtn = this.createActionButton('fas fa-clone', 'Duplicate', () => this.duplicateDetail(detail.id));
        duplicateBtn.classList.add('secondary');

        const deleteBtn = this.createActionButton('fas fa-trash', 'Remove', () => this.removeDetail(detail.id));
        deleteBtn.classList.add('danger');

        const moveUpBtn = this.createActionButton('fas fa-arrow-up', 'Move up', () => this.moveDetail(detail.id, 'up'));
        moveUpBtn.disabled = index === 0;

        const moveDownBtn = this.createActionButton('fas fa-arrow-down', 'Move down', () => this.moveDetail(detail.id, 'down'));
        moveDownBtn.disabled = index === total - 1;

        actions.appendChild(moveUpBtn);
        actions.appendChild(moveDownBtn);
        actions.appendChild(duplicateBtn);
        actions.appendChild(deleteBtn);
        row.appendChild(actions);

        return row;
    }

    private renderAdditionalAttributes(section: DetailSectionDefinition, detail: VisualNode): HTMLElement | null {
        const attributes = section.attributeDefinitions?.filter(attr => {
            if (!attr?.name) {
                return false;
            }
            return attr.name !== section.primaryAttribute && attr.name !== section.secondaryAttribute;
        }) ?? [];

        if (attributes.length === 0) {
            return null;
        }

        const cell = document.createElement('div');
        cell.className = 'http-detail-cell';

        const grid = document.createElement('div');
        grid.className = 'detail-field-grid';

        attributes.forEach(attr => {
            const attrId = `${section.detailType}-${detail.id}-${attr.name}`;
            const currentValue = detail.attributes?.[attr.name] ?? attr.defaultValue ?? '';
            const field = this.createDetailField(
                attrId,
                this.formatAttributeLabel(attr.name),
                currentValue,
                {
                    description: attr.description,
                    type: attr.type,
                    allowedValues: attr.allowedValues
                }
            );

            this.bindAttributeInput(field.input, detail.id, attr.name);
            grid.appendChild(field.wrapper);
        });

        cell.appendChild(grid);
        return cell;
    }

    private bindAttributeInput(
        input: HTMLInputElement | HTMLTextAreaElement | HTMLSelectElement,
        detailId: string,
        attributeName: string,
        afterUpdate?: (value: string) => void
    ): void {
        const isCheckbox = input instanceof HTMLInputElement && input.type === 'checkbox';
        const isSelect = input instanceof HTMLSelectElement;
        const eventName = isCheckbox || isSelect ? 'change' : 'input';

        input.addEventListener(eventName, () => {
            const nextValue = isCheckbox
                ? ((input as HTMLInputElement).checked ? 'true' : 'false')
                : input.value;
            this.updateDetailAttribute(detailId, attributeName, nextValue);
            afterUpdate?.(nextValue);
        });
    }

    private findAttributeDefinition(
        section: DetailSectionDefinition,
        attributeName?: string
    ): AttributeDefinition | undefined {
        if (!attributeName) {
            return undefined;
        }
        return section.attributeDefinitions.find(attr => attr.name === attributeName);
    }

    private formatAttributeLabel(name: string): string {
        return name
            .replace(/[-_]/g, ' ')
            .replace(/\s+/g, ' ')
            .trim()
            .replace(/^\w/, char => char.toUpperCase());
    }

    private createActionButton(icon: string, title: string, handler: () => void): HTMLButtonElement {
        const button = document.createElement('button');
        button.type = 'button';
        button.className = 'http-detail-row__action';
        button.innerHTML = `<i class="${icon}"></i>`;
        button.title = title;
        button.addEventListener('click', (event) => {
            event.stopPropagation();
            if (!button.disabled) {
                handler();
            }
        });
        return button;
    }

    private addDetail(detailType: string): void {
        const newDetail = this.registry.createDetailNode(detailType);
        if (!newDetail) {
            return;
        }
        const definition = this.registry.getDetailDefinition(detailType);
        if (definition?.secondaryAttribute && typeof newDetail.body === 'string') {
            newDetail.body = newDetail.attributes?.[definition.secondaryAttribute] ?? '';
        }
        this.node = this.cloneNode({
            ...this.node,
            children: [...(this.node.children || []), newDetail]
        });
        this.emitChange();
        this.renderSections();
    }

    private createDetailField(
        id: string,
        labelText: string,
        value: string,
        options: {
            description?: string;
            multiline?: boolean;
            type?: AttributeDefinition['type'];
            allowedValues?: string[];
        } = {}
    ): { wrapper: HTMLElement; input: HTMLInputElement | HTMLTextAreaElement | HTMLSelectElement } {
        const wrapper = document.createElement('label');
        wrapper.className = 'detail-field';
        wrapper.setAttribute('for', id);

        const label = document.createElement('span');
        label.className = 'detail-field__label';
        label.textContent = labelText;
        wrapper.appendChild(label);

        if (options.description) {
            wrapper.title = options.description;
        }

        const { multiline = false, type, allowedValues } = options;
        let input: HTMLInputElement | HTMLTextAreaElement | HTMLSelectElement;

        if (type === 'boolean') {
            const checkbox = document.createElement('input');
            checkbox.type = 'checkbox';
            checkbox.id = id;
            checkbox.className = 'detail-input detail-input--checkbox';
            checkbox.checked = value === 'true' || value === '1';
            input = checkbox;
        } else if (type === 'enum' && Array.isArray(allowedValues) && allowedValues.length > 0) {
            const select = document.createElement('select');
            select.id = id;
            select.className = 'detail-input detail-input--select';
            allowedValues.forEach(optionValue => {
                const option = document.createElement('option');
                option.value = optionValue;
                option.textContent = optionValue;
                select.appendChild(option);
            });
            if (value && allowedValues.includes(value)) {
                select.value = value;
            }
            input = select;
        } else if (multiline || type === 'xpath') {
            const textArea = document.createElement('textarea');
            textArea.id = id;
            textArea.className = 'detail-input detail-input--textarea';
            textArea.placeholder = options.description || '';
            textArea.value = value ?? '';
            input = textArea;
        } else {
            const textInput = document.createElement('input');
            textInput.id = id;
            textInput.className = 'detail-input';
            if (type === 'number') {
                textInput.type = 'number';
            } else if (type === 'url') {
                textInput.type = 'url';
            } else {
                textInput.type = 'text';
            }
            textInput.placeholder = options.description || labelText;
            textInput.value = value ?? '';
            input = textInput;
        }

        if (input instanceof HTMLSelectElement && (!value || !input.value)) {
            if (allowedValues && allowedValues.length > 0) {
                input.value = allowedValues[0];
            }
        }

        wrapper.appendChild(input);

        return { wrapper, input };
    }

    private duplicateDetail(detailId: string): void {
        const children = [...(this.node.children || [])];
        const index = children.findIndex(child => child.id === detailId);
        if (index === -1) {
            return;
        }

        const original = children[index];
        const duplicate = this.registry.createDetailNode(original.type);
        if (!duplicate) {
            return;
        }

        duplicate.attributes = {
            ...original.attributes
        };
        duplicate.body = original.body;
        duplicate.children = original.children ? original.children.map(child => this.cloneNode(child)) : [];

        children.splice(index + 1, 0, duplicate);

        this.node = this.cloneNode({
            ...this.node,
            children
        });

        this.emitChange();
        this.renderSections();
    }

    private removeDetail(detailId: string): void {
        const updatedChildren = (this.node.children || []).filter(child => child.id !== detailId);
        this.node = this.cloneNode({
            ...this.node,
            children: updatedChildren
        });
        this.emitChange();
        this.renderSections();
    }

    private moveDetail(detailId: string, direction: 'up' | 'down'): void {
        const children = [...(this.node.children || [])];
        const index = children.findIndex(child => child.id === detailId);
        if (index === -1) {
            return;
        }

        const swapIndex = direction === 'up' ? index - 1 : index + 1;
        if (swapIndex < 0 || swapIndex >= children.length) {
            return;
        }

        const [moved] = children.splice(index, 1);
        children.splice(swapIndex, 0, moved);

        this.node = this.cloneNode({
            ...this.node,
            children
        });
        this.emitChange();
        this.renderSections();
    }

    private updateDetailAttribute(detailId: string, attribute: string, value: string): void {
        const children = (this.node.children || []).map(child => {
            if (child.id !== detailId) {
                return child;
            }
            return {
                ...child,
                attributes: {
                    ...child.attributes,
                    [attribute]: value
                }
            };
        });

        this.node = this.cloneNode({
            ...this.node,
            children
        });
        this.emitChange();
    }

    private updateDetailBody(detailId: string, value: string): void {
        const children = (this.node.children || []).map(child => {
            if (child.id !== detailId) {
                return child;
            }
            return {
                ...child,
                body: value
            };
        });

        this.node = this.cloneNode({
            ...this.node,
            children
        });
        this.emitChange();
    }

    private emitChange(): void {
        this.onChange(this.cloneNode(this.node));
    }

    private cloneNode(node: VisualNode): VisualNode {
        if (typeof structuredClone === 'function') {
            return structuredClone(node);
        }
        return JSON.parse(JSON.stringify(node));
    }
}
