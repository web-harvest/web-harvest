import type { ValidationError } from './editors/SchemaDrivenEditor';

export interface PropertyPanelHeader {
    title: string;
    subtitle?: string;
    icon?: string;
}

export interface PropertyPanelFooterActions {
    onApply?: () => void;
    onCancel?: () => void;
    disableApply?: boolean;
    applyLabel?: string;
    cancelLabel?: string;
    hasChanges?: boolean;
}

const FOCUSABLE_SELECTOR = [
    'button',
    '[href]',
    'input',
    'select',
    'textarea',
    '[tabindex]:not([tabindex="-1"])'
].join(',');

export class PropertyPanelShell {
    private headerEl: HTMLElement;
    private bodyEl: HTMLElement;
    private footerEl: HTMLElement;
    private validationEl: HTMLElement;
    private onApply?: () => void;
    private onCancel?: () => void;
    private applyButton?: HTMLButtonElement;
    private cancelButton?: HTMLButtonElement;
    private focusableElements: HTMLElement[] = [];
    private titleElement?: HTMLElement;
    private dirtyIndicator?: HTMLElement;

    constructor(private readonly root: HTMLElement) {
        this.root.classList.add('property-panel-shell');
        this.root.setAttribute('role', 'form');
        this.root.tabIndex = -1;

        this.headerEl = document.createElement('header');
        this.headerEl.className = 'property-panel-shell__header';

        this.validationEl = document.createElement('div');
        this.validationEl.className = 'property-panel-shell__validation';
        this.validationEl.setAttribute('role', 'alert');
        this.validationEl.setAttribute('aria-live', 'polite');

        this.bodyEl = document.createElement('div');
        this.bodyEl.className = 'property-panel-shell__body';

        this.footerEl = document.createElement('footer');
        this.footerEl.className = 'property-panel-shell__footer';

        this.root.innerHTML = '';
        this.root.appendChild(this.headerEl);
        this.root.appendChild(this.validationEl);
        this.root.appendChild(this.bodyEl);
        this.root.appendChild(this.footerEl);

        this.root.addEventListener('keydown', (event) => this.handleKeydown(event));
    }

    reset(): void {
        this.headerEl.innerHTML = '';
        this.bodyEl.innerHTML = '';
        this.footerEl.innerHTML = '';
        this.validationEl.innerHTML = '';
        this.focusableElements = [];
        this.applyButton = undefined;
        this.cancelButton = undefined;
        this.titleElement = undefined;
        this.dirtyIndicator = undefined;
    }

    setHeader(header: PropertyPanelHeader): void {
        this.headerEl.innerHTML = '';
        const titleRow = document.createElement('div');
        titleRow.className = 'property-panel-shell__title-row';

        if (header.icon) {
            const icon = document.createElement('span');
            icon.className = `property-panel-shell__icon ${header.icon}`;
            icon.setAttribute('aria-hidden', 'true');
            titleRow.appendChild(icon);
        }

        const title = document.createElement('h2');
        title.textContent = header.title;
        title.className = 'property-panel-shell__title';
        this.titleElement = title;
        titleRow.appendChild(title);

        this.headerEl.appendChild(titleRow);

        if (header.subtitle) {
            const subtitle = document.createElement('p');
            subtitle.textContent = header.subtitle;
            subtitle.className = 'property-panel-shell__subtitle';
            this.headerEl.appendChild(subtitle);
        }
    }

    setBody(content: HTMLElement): void {
        this.bodyEl.innerHTML = '';
        this.bodyEl.appendChild(content);
        this.refreshFocusableElements();
    }

    setFooterActions(actions: PropertyPanelFooterActions): void {
        this.footerEl.innerHTML = '';
        this.onApply = actions.onApply;
        this.onCancel = actions.onCancel;

        const leftGroup = document.createElement('div');
        leftGroup.className = 'property-panel-shell__footer-group';

        const rightGroup = document.createElement('div');
        rightGroup.className = 'property-panel-shell__footer-group';

        if (actions.onCancel) {
            this.cancelButton = document.createElement('button');
            this.cancelButton.type = 'button';
            this.cancelButton.className = 'property-panel-shell__button property-panel-shell__button--ghost';
            this.cancelButton.textContent = actions.cancelLabel || 'Cancel';
            this.cancelButton.addEventListener('click', () => actions.onCancel?.());
            leftGroup.appendChild(this.cancelButton);
        }

        if (actions.onApply) {
            this.applyButton = document.createElement('button');
            this.applyButton.type = 'button';
            let buttonClass = 'property-panel-shell__button property-panel-shell__button--primary';
            if (actions.hasChanges) {
                buttonClass += ' property-panel-shell__button--has-changes';
            }
            this.applyButton.className = buttonClass;
            this.applyButton.textContent = actions.applyLabel || 'Apply';
            this.applyButton.disabled = Boolean(actions.disableApply);
            this.applyButton.addEventListener('click', () => actions.onApply?.());
            rightGroup.appendChild(this.applyButton);
        }

        this.footerEl.appendChild(leftGroup);
        this.footerEl.appendChild(rightGroup);
        this.refreshFocusableElements();
    }

    showValidationSummary(errors: ValidationError[]): void {
        this.validationEl.innerHTML = '';
        if (errors.length === 0) {
            this.validationEl.style.display = 'none';
            return;
        }
        this.validationEl.style.display = 'block';

        const heading = document.createElement('div');
        heading.className = 'property-panel-shell__validation-title';
        heading.textContent = errors.length === 1 ? '1 validation warning' : `${errors.length} validation warnings`;
        this.validationEl.appendChild(heading);

        const list = document.createElement('ul');
        list.className = 'property-panel-shell__validation-list';
        errors.forEach(error => {
            const item = document.createElement('li');
            item.textContent = `${error.field}: ${error.message}`;
            list.appendChild(item);
        });
        this.validationEl.appendChild(list);
    }

    setApplyDisabled(disabled: boolean): void {
        if (this.applyButton) {
            this.applyButton.disabled = disabled;
        }
    }

    /**
     * Set dirty indicator visibility in header
     */
    setDirtyIndicator(dirty: boolean): void {
        if (!this.titleElement) {
            return;
        }

        if (dirty && !this.dirtyIndicator) {
            // Create dirty indicator
            this.dirtyIndicator = document.createElement('span');
            this.dirtyIndicator.className = 'property-panel-shell__dirty-indicator';
            this.dirtyIndicator.setAttribute('aria-label', 'Unsaved changes');
            this.dirtyIndicator.setAttribute('title', 'You have unsaved changes');
            this.titleElement.appendChild(this.dirtyIndicator);
        } else if (!dirty && this.dirtyIndicator) {
            // Remove dirty indicator
            this.dirtyIndicator.remove();
            this.dirtyIndicator = undefined;
        }
    }

    focusFirstField(): void {
        this.refreshFocusableElements();
        this.focusableElements[0]?.focus();
    }

    private handleKeydown(event: KeyboardEvent): void {
        if (event.key === 'Escape') {
            this.onCancel?.();
            event.stopPropagation();
            return;
        }

        if ((event.metaKey || event.ctrlKey) && event.key === 'Enter') {
            this.onApply?.();
            event.preventDefault();
            return;
        }

        if (event.key === 'Tab') {
            this.handleFocusTrap(event);
        }
    }

    private handleFocusTrap(event: KeyboardEvent): void {
        if (this.focusableElements.length === 0) {
            return;
        }
        const first = this.focusableElements[0];
        const last = this.focusableElements[this.focusableElements.length - 1];

        if (event.shiftKey && document.activeElement === first) {
            last.focus();
            event.preventDefault();
        } else if (!event.shiftKey && document.activeElement === last) {
            first.focus();
            event.preventDefault();
        }
    }

    private refreshFocusableElements(): void {
        const elements = this.root.querySelectorAll<HTMLElement>(FOCUSABLE_SELECTOR);
        this.focusableElements = Array.from(elements).filter(el => !el.hasAttribute('disabled'));
    }
}


