import type { VisualNode } from '../../types';
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
        addButton.addEventListener('click', (event) => {
            event.stopPropagation();
            this.addDetail(section.detailType);
        });

        actions.appendChild(addButton);
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
            const primaryField = this.createDetailField(
                `${section.detailType}-${detail.id}-primary`,
                section.primaryAttributeLabel ?? section.primaryAttribute,
                detail.attributes?.[section.primaryAttribute] ?? '',
                section.primaryAttributeDescription
            );
            primaryField.input.addEventListener('input', () => {
                this.updateDetailAttribute(detail.id, section.primaryAttribute!, primaryField.input.value);
            });
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
                undefined,
                true
            );
            bodyField.input.addEventListener('input', () => {
                this.updateDetailBody(detail.id, bodyField.input.value);
            });
            secondaryCell.appendChild(bodyField.wrapper);
        } else if (section.secondaryAttribute) {
            const secondaryInitialValue =
                detail.attributes?.[section.secondaryAttribute] ??
                (typeof detail.body === 'string' ? detail.body : '');

            const secondaryField = this.createDetailField(
                `${section.detailType}-${detail.id}-secondary`,
                section.secondaryAttributeLabel ?? section.secondaryAttribute,
                secondaryInitialValue,
                section.secondaryAttributeDescription
            );
            secondaryField.input.addEventListener('input', () => {
                const nextValue = secondaryField.input.value;
                this.updateDetailAttribute(detail.id, section.secondaryAttribute!, nextValue);
                if (typeof detail.body === 'string') {
                    this.updateDetailBody(detail.id, nextValue);
                }
            });
            secondaryCell.appendChild(secondaryField.wrapper);
        } else {
            const summary = this.registry.summarizeDetail(detail);
            const valueEl = document.createElement('span');
            valueEl.className = summary.isEmpty ? 'http-detail-row__value http-detail-row__value--empty' : 'http-detail-row__value';
            valueEl.textContent = summary.secondaryText ?? '—';
            secondaryCell.appendChild(valueEl);
        }

        row.appendChild(secondaryCell);

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
        description?: string,
        multiline: boolean = false
    ): { wrapper: HTMLElement; input: HTMLInputElement | HTMLTextAreaElement } {
        const wrapper = document.createElement('label');
        wrapper.className = 'detail-field';
        wrapper.setAttribute('for', id);

        const label = document.createElement('span');
        label.className = 'detail-field__label';
        label.textContent = labelText;
        wrapper.appendChild(label);

        if (description) {
            wrapper.title = description;
        }

        const input = multiline ? document.createElement('textarea') : document.createElement('input');
        input.id = id;
        input.className = multiline ? 'detail-input detail-input--textarea' : 'detail-input';
        if (!multiline) {
            const textInput = input as HTMLInputElement;
            textInput.type = 'text';
            textInput.placeholder = description || labelText;
        } else {
            const textArea = input as HTMLTextAreaElement;
            textArea.placeholder = description || '';
        }
        input.value = value.toString();

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
