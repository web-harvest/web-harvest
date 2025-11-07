import type { Plugin, VisualNode } from '../../types';
import { createHttpDetailNode, partitionHttpChildren } from '../http/HttpNodeUtils';

interface HttpEditorOptions {
    node: VisualNode;
    plugin: Plugin;
    onChange: (node: VisualNode) => void;
}

type DetailNode = VisualNode & { type: 'http-header' | 'http-param' };

export class HttpEditor {
    private readonly onChange: (node: VisualNode) => void;
    private node: VisualNode;
    private readonly plugin: Plugin;

    private root!: HTMLElement;
    private bodyFieldWrapper!: HTMLDivElement;
    private headersContainer!: HTMLDivElement;
    private paramsContainer!: HTMLDivElement;

    private static readonly METHODS = [
        'GET',
        'POST',
        'PUT',
        'PATCH',
        'DELETE',
        'HEAD',
        'OPTIONS',
        'TRACE'
    ];

    constructor(options: HttpEditorOptions) {
        this.onChange = options.onChange;
        this.plugin = options.plugin;
        this.node = this.cloneNode(options.node);
    }

    render(): HTMLElement {
        this.root = document.createElement('div');
        this.root.className = 'http-editor';

        this.root.appendChild(this.renderRequestSection());
        this.root.appendChild(this.renderParametersSection());
        this.root.appendChild(this.renderHeadersSection());

        return this.root;
    }

    private renderRequestSection(): HTMLElement {
        const section = this.createSection('Request', 'Configure HTTP request basics.');

        const methodRow = this.createFieldRow('Method');
        const methodSelect = document.createElement('select');
        methodSelect.className = 'http-editor__select';

        HttpEditor.METHODS.forEach(method => {
            const option = document.createElement('option');
            option.value = method;
            option.textContent = method;
            methodSelect.appendChild(option);
        });

        const currentMethod = (this.node.attributes.method || 'GET').toUpperCase();
        methodSelect.value = HttpEditor.METHODS.includes(currentMethod) ? currentMethod : 'GET';

        methodSelect.addEventListener('change', () => {
            const selected = methodSelect.value.toUpperCase();
            this.node.attributes.method = selected;
            this.toggleBodyVisibility(selected);
            this.emitChange();
        });

        methodRow.field.appendChild(methodSelect);
        section.appendChild(methodRow.row);

        const urlRow = this.createFieldRow('URL', 'Fully qualified URL for the HTTP request.');
        const urlInput = document.createElement('input');
        urlInput.type = 'url';
        urlInput.required = true;
        urlInput.placeholder = 'https://service.example.com/resource';
        urlInput.className = 'http-editor__input';
        urlInput.value = this.node.attributes.url || '';

        urlInput.addEventListener('input', () => {
            this.node.attributes.url = urlInput.value;
            this.emitChange();
        });

        urlRow.field.appendChild(urlInput);
        section.appendChild(urlRow.row);

        const bodyRow = this.createFieldRow(
            'Body',
            'Optional request body for methods that support payloads.'
        );
        const bodyTextarea = document.createElement('textarea');
        bodyTextarea.rows = 4;
        bodyTextarea.className = 'http-editor__textarea';
        bodyTextarea.placeholder = '{ "payload": true }';
        bodyTextarea.value = this.node.attributes.body || '';

        bodyTextarea.addEventListener('input', () => {
            this.node.attributes.body = bodyTextarea.value;
            this.emitChange();
        });

        bodyRow.field.appendChild(bodyTextarea);
        this.bodyFieldWrapper = bodyRow.row;
        section.appendChild(bodyRow.row);

        this.toggleBodyVisibility(methodSelect.value.toUpperCase());

        return section;
    }

    private renderParametersSection(): HTMLElement {
        const section = this.createSection(
            'Query Parameters',
            'Attach key/value pairs as query parameters to the request URL.'
        );

        this.paramsContainer = document.createElement('div');
        this.paramsContainer.className = 'http-editor__list';
        section.appendChild(this.paramsContainer);

        const emptyState = document.createElement('div');
        emptyState.className = 'http-editor__empty';
        emptyState.textContent = 'No query parameters defined.';
        this.paramsContainer.appendChild(emptyState);

        this.renderParamRows();

        const addButton = document.createElement('button');
        addButton.type = 'button';
        addButton.className = 'http-editor__add-btn';
        addButton.textContent = 'Add Parameter';
        addButton.addEventListener('click', () => this.addParam());

        section.appendChild(addButton);
        return section;
    }

    private renderHeadersSection(): HTMLElement {
        const section = this.createSection(
            'Headers',
            'Define HTTP headers such as Authorization, Accept, or Content-Type.'
        );

        this.headersContainer = document.createElement('div');
        this.headersContainer.className = 'http-editor__list';
        section.appendChild(this.headersContainer);

        const emptyState = document.createElement('div');
        emptyState.className = 'http-editor__empty';
        emptyState.textContent = 'No headers defined.';
        this.headersContainer.appendChild(emptyState);

        this.renderHeaderRows();

        const addButton = document.createElement('button');
        addButton.type = 'button';
        addButton.className = 'http-editor__add-btn';
        addButton.textContent = 'Add Header';
        addButton.addEventListener('click', () => this.addHeader());

        section.appendChild(addButton);
        return section;
    }

    private renderParamRows(): void {
        this.paramsContainer.innerHTML = '';
        const { params } = partitionHttpChildren(this.node);
        const paramNodes = params.filter((child): child is DetailNode => child.type === 'http-param');

        if (paramNodes.length === 0) {
            const emptyState = document.createElement('div');
            emptyState.className = 'http-editor__empty';
            emptyState.textContent = 'No query parameters defined.';
            this.paramsContainer.appendChild(emptyState);
            return;
        }

        paramNodes.forEach(param => {
            const row = this.createDetailRow(param, 'Parameter', 'Value');
            this.paramsContainer.appendChild(row);
        });
    }

    private renderHeaderRows(): void {
        this.headersContainer.innerHTML = '';
        const { headers } = partitionHttpChildren(this.node);
        const headerNodes = headers.filter((child): child is DetailNode => child.type === 'http-header');

        if (headerNodes.length === 0) {
            const emptyState = document.createElement('div');
            emptyState.className = 'http-editor__empty';
            emptyState.textContent = 'No headers defined.';
            this.headersContainer.appendChild(emptyState);
            return;
        }

        headerNodes.forEach(header => {
            const row = this.createDetailRow(header, 'Header', 'Value');
            this.headersContainer.appendChild(row);
        });
    }

    private createDetailRow(node: DetailNode, namePlaceholder: string, valuePlaceholder: string): HTMLElement {
        const row = document.createElement('div');
        row.className = 'http-editor__detail-row';

        const nameInput = document.createElement('input');
        nameInput.type = 'text';
        nameInput.className = 'http-editor__input';
        nameInput.placeholder = namePlaceholder;
        nameInput.value = node.attributes.name || '';

        nameInput.addEventListener('input', () => {
            node.attributes.name = nameInput.value;
            this.emitChange();
        });

        const valueInput = document.createElement('input');
        valueInput.type = 'text';
        valueInput.className = 'http-editor__input';
        valueInput.placeholder = valuePlaceholder;
        valueInput.value = node.body || '';

        valueInput.addEventListener('input', () => {
            node.body = valueInput.value;
            this.emitChange();
        });

        const actions = document.createElement('div');
        actions.className = 'http-editor__detail-actions';

        const removeBtn = document.createElement('button');
        removeBtn.type = 'button';
        removeBtn.className = 'http-editor__remove-btn';
        removeBtn.textContent = 'Remove';
        removeBtn.addEventListener('click', () => this.removeDetail(node));

        actions.appendChild(removeBtn);

        row.appendChild(nameInput);
        row.appendChild(valueInput);
        row.appendChild(actions);
        return row;
    }

    private addParam(): void {
        const newNode = createHttpDetailNode('http-param') as DetailNode;
        this.node.children = [...this.node.children, newNode];
        this.renderParamRows();
        this.emitChange();
    }

    private addHeader(): void {
        const newNode = createHttpDetailNode('http-header') as DetailNode;
        this.node.children = [...this.node.children, newNode];
        this.renderHeaderRows();
        this.emitChange();
    }

    private removeDetail(node: DetailNode): void {
        this.node.children = this.node.children.filter(child => child.id !== node.id);

        if (node.type === 'http-param') {
            this.renderParamRows();
        } else {
            this.renderHeaderRows();
        }

        this.emitChange();
    }

    private toggleBodyVisibility(method: string): void {
        const methodsWithBody = new Set(['POST', 'PUT', 'PATCH', 'DELETE', 'OPTIONS', 'TRACE']);
        const shouldShow = methodsWithBody.has(method);
        if (this.bodyFieldWrapper) {
            this.bodyFieldWrapper.style.display = shouldShow ? 'flex' : 'none';
        }
    }

    private createSection(title: string, description?: string): HTMLElement {
        const section = document.createElement('section');
        section.className = 'http-editor__section';

        const header = document.createElement('div');
        header.className = 'http-editor__section-header';
        header.textContent = title;
        section.appendChild(header);

        if (description) {
            const sub = document.createElement('div');
            sub.className = 'http-editor__section-description';
            sub.textContent = description;
            section.appendChild(sub);
        }

        return section;
    }

    private createFieldRow(label: string, description?: string): { row: HTMLDivElement; field: HTMLDivElement } {
        const row = document.createElement('div');
        row.className = 'http-editor__field-row';

        const labelEl = document.createElement('label');
        labelEl.className = 'http-editor__label';
        labelEl.textContent = label;
        row.appendChild(labelEl);

        if (description) {
            const desc = document.createElement('div');
            desc.className = 'http-editor__description';
            desc.textContent = description;
            row.appendChild(desc);
        }

        const field = document.createElement('div');
        field.className = 'http-editor__field';
        row.appendChild(field);

        return { row, field };
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


