import type { AttributeDefinition, AttributeUiMetadata } from '../../../types';

export interface PropertyField {
    element: HTMLElement;
    getValue(): string;
    setValue(value: string): void;
    focus(): void;
    validate(): string | null;
}

export interface CreateFieldOptions {
    attribute: AttributeDefinition;
    metadata?: AttributeUiMetadata;
    value: string;
    onChange(value: string): void;
}

const DEFAULT_GROUP_LABEL = 'General';

export function createField(options: CreateFieldOptions): PropertyField {
    const { attribute, metadata, value, onChange } = options;
    const fieldKind = pickFieldKind(attribute, metadata);

    switch (fieldKind) {
        case 'BOOLEAN':
            return createBooleanField(attribute, metadata, value, onChange);
        case 'SELECT':
            return createSelectField(attribute, metadata, value, onChange);
        case 'NUMBER':
            return createNumberField(attribute, metadata, value, onChange);
        case 'TEXTAREA':
        case 'CODE':
        case 'JSON':
            return createTextareaField(attribute, metadata, value, onChange);
        default:
            return createTextField(attribute, metadata, value, onChange);
    }
}

function createFieldContainer(attribute: AttributeDefinition, metadata?: AttributeUiMetadata) {
    const container = document.createElement('div');
    container.className = 'schema-driven-field';
    container.style.display = 'flex';
    container.style.flexDirection = 'column';
    container.style.gap = '6px';
    container.style.padding = '8px 0';

    const label = document.createElement('label');
    label.textContent = metadata?.localizationKey || attribute.name;
    label.style.fontSize = '13px';
    label.style.fontWeight = '600';
    label.style.color = 'var(--text-primary)';
    label.style.display = 'flex';
    label.style.alignItems = 'center';
    label.style.gap = '6px';

    if (attribute.required) {
        const badge = document.createElement('span');
        badge.textContent = '•';
        badge.style.color = 'var(--magenta)';
        badge.setAttribute('aria-hidden', 'true');
        label.appendChild(badge);
    }

    const helper = document.createElement('div');
    helper.className = 'field-helper';
    helper.style.fontSize = '11px';
    helper.style.color = 'var(--gray-600)';
    helper.style.marginTop = '-4px';
    helper.style.display = metadata?.helperText ? 'block' : 'none';
    helper.textContent = metadata?.helperText || '';

    const error = document.createElement('div');
    error.className = 'field-error';
    error.style.fontSize = '11px';
    error.style.color = 'var(--error)';
    error.style.minHeight = '14px';

    const metadataBadge = createMetadataBadge(metadata);

    if (metadataBadge) {
        label.appendChild(metadataBadge);
    }

    container.appendChild(label);
    if (metadata?.helperText) {
        container.appendChild(helper);
    }
    return { container, helper, error };
}

function createMetadataBadge(metadata?: AttributeUiMetadata): HTMLElement | null {
    if (!metadata?.advanced && !metadata?.expressionCapable) {
        return null;
    }
    const badge = document.createElement('span');
    badge.style.fontSize = '10px';
    badge.style.fontWeight = '600';
    badge.style.padding = '2px 6px';
    badge.style.borderRadius = '999px';
    badge.style.border = '1px solid var(--gray-400)';
    badge.style.color = 'var(--gray-600)';
    badge.style.textTransform = 'uppercase';
    badge.textContent = metadata.expressionCapable ? 'Expression' : 'Advanced';
    badge.title = metadata.expressionCapable ? 'Supports expression builder' : 'Advanced option';
    return badge;
}

function createTextField(
    attribute: AttributeDefinition,
    metadata: AttributeUiMetadata | undefined,
    value: string,
    onChange: (value: string) => void
): PropertyField {
    const { container, helper, error } = createFieldContainer(attribute, metadata);
    const input = document.createElement('input');
    input.type = 'text';
    input.value = value ?? '';
    input.placeholder = metadata?.placeholder ?? '';
    input.className = 'schema-driven-input';
    input.style.padding = '8px 10px';
    input.style.border = '1px solid var(--gray-300)';
    input.style.borderRadius = 'var(--radius-sm)';
    input.style.fontSize = '13px';

    input.addEventListener('input', () => onChange(input.value));

    container.appendChild(input);
    container.appendChild(error);

    return buildFieldAPI(container, input, attribute, metadata, helper, error);
}

function createTextareaField(
    attribute: AttributeDefinition,
    metadata: AttributeUiMetadata | undefined,
    value: string,
    onChange: (value: string) => void
): PropertyField {
    const { container, helper, error } = createFieldContainer(attribute, metadata);
    const textarea = document.createElement('textarea');
    textarea.value = value ?? '';
    textarea.placeholder = metadata?.placeholder ?? '';
    textarea.rows = metadata?.fieldKind === 'JSON' || metadata?.fieldKind === 'CODE' ? 8 : 4;
    textarea.style.padding = '8px 10px';
    textarea.style.border = '1px solid var(--gray-300)';
    textarea.style.borderRadius = 'var(--radius-sm)';
    textarea.style.fontFamily = metadata?.fieldKind === 'JSON' || metadata?.fieldKind === 'CODE'
        ? 'var(--font-mono)'
        : 'inherit';
    textarea.style.fontSize = '13px';

    textarea.addEventListener('input', () => onChange(textarea.value));

    container.appendChild(textarea);
    container.appendChild(error);
    return buildFieldAPI(container, textarea, attribute, metadata, helper, error);
}

function createNumberField(
    attribute: AttributeDefinition,
    metadata: AttributeUiMetadata | undefined,
    value: string,
    onChange: (value: string) => void
): PropertyField {
    const { container, helper, error } = createFieldContainer(attribute, metadata);
    const input = document.createElement('input');
    input.type = 'number';
    input.value = value ?? '';
    input.placeholder = metadata?.placeholder ?? '';
    input.style.padding = '8px 10px';
    input.style.border = '1px solid var(--gray-300)';
    input.style.borderRadius = 'var(--radius-sm)';
    input.style.fontSize = '13px';

    if (attribute.minValue !== undefined) {
        input.min = String(attribute.minValue);
    }
    if (attribute.maxValue !== undefined) {
        input.max = String(attribute.maxValue);
    }

    input.addEventListener('input', () => onChange(input.value));

    container.appendChild(input);
    container.appendChild(error);
    return buildFieldAPI(container, input, attribute, metadata, helper, error);
}

function createSelectField(
    attribute: AttributeDefinition,
    metadata: AttributeUiMetadata | undefined,
    value: string,
    onChange: (value: string) => void
): PropertyField {
    const { container, helper, error } = createFieldContainer(attribute, metadata);
    const select = document.createElement('select');
    select.style.padding = '8px 10px';
    select.style.border = '1px solid var(--gray-300)';
    select.style.borderRadius = 'var(--radius-sm)';
    select.style.fontSize = '13px';

    if (!attribute.required) {
        const emptyOption = document.createElement('option');
        emptyOption.value = '';
        emptyOption.textContent = metadata?.placeholder ?? 'Select…';
        select.appendChild(emptyOption);
    }

    (attribute.allowedValues || []).forEach(optionValue => {
        const option = document.createElement('option');
        option.value = optionValue;
        option.textContent = optionValue;
        select.appendChild(option);
    });

    select.value = value ?? '';
    select.addEventListener('change', () => onChange(select.value));

    container.appendChild(select);
    container.appendChild(error);
    return buildFieldAPI(container, select, attribute, metadata, helper, error);
}

function createBooleanField(
    attribute: AttributeDefinition,
    metadata: AttributeUiMetadata | undefined,
    value: string,
    onChange: (value: string) => void
): PropertyField {
    const { container, helper, error } = createFieldContainer(attribute, metadata);
    const wrapper = document.createElement('label');
    wrapper.style.display = 'inline-flex';
    wrapper.style.alignItems = 'center';
    wrapper.style.gap = '8px';

    const checkbox = document.createElement('input');
    checkbox.type = 'checkbox';
    checkbox.checked = value === 'true';
    checkbox.addEventListener('change', () => onChange(checkbox.checked ? 'true' : 'false'));

    const text = document.createElement('span');
    text.textContent = metadata?.placeholder || attribute.description || 'Enabled';
    text.style.fontSize = '13px';

    wrapper.appendChild(checkbox);
    wrapper.appendChild(text);
    container.appendChild(wrapper);
    container.appendChild(error);

    return {
        element: container,
        focus: () => checkbox.focus(),
        getValue: () => (checkbox.checked ? 'true' : 'false'),
        setValue: (val: string) => {
            checkbox.checked = val === 'true';
        },
        validate: () => {
            error.textContent = '';
            return null;
        }
    };
}

function buildFieldAPI(
    container: HTMLElement,
    input: HTMLInputElement | HTMLTextAreaElement | HTMLSelectElement,
    attribute: AttributeDefinition,
    metadata: AttributeUiMetadata | undefined,
    helper: HTMLElement,
    error: HTMLElement
): PropertyField {
    return {
        element: container,
        focus: () => input.focus(),
        getValue: () => input.value,
        setValue: (val: string) => {
            input.value = val ?? '';
        },
        validate: () => {
            const validationError = validateAttributeValue(attribute, metadata, input.value);
            error.textContent = validationError || '';
            if (helper && metadata?.helperText) {
                helper.style.color = validationError ? 'var(--error)' : 'var(--gray-600)';
            }
            input.setAttribute('aria-invalid', validationError ? 'true' : 'false');
            return validationError;
        }
    };
}

function validateAttributeValue(
    attribute: AttributeDefinition,
    metadata: AttributeUiMetadata | undefined,
    value: string
): string | null {
    const trimmed = value?.trim() ?? '';
    if (attribute.required && !trimmed) {
        return 'This field is required.';
    }
    if (!trimmed) {
        return null;
    }

    if (attribute.type === 'number') {
        const num = Number(trimmed);
        if (Number.isNaN(num)) {
            return 'Enter a valid number.';
        }
        if (attribute.minValue !== undefined && num < attribute.minValue) {
            return `Minimum allowed value is ${attribute.minValue}.`;
        }
        if (attribute.maxValue !== undefined && num > attribute.maxValue) {
            return `Maximum allowed value is ${attribute.maxValue}.`;
        }
    }

    if (attribute.pattern) {
        try {
            const regex = new RegExp(attribute.pattern);
            if (!regex.test(trimmed)) {
                return 'Value does not match the required pattern.';
            }
        } catch {
            // Ignore invalid regex from metadata
        }
    }

    if (attribute.allowedValues && attribute.allowedValues.length > 0) {
        if (!attribute.allowedValues.includes(trimmed)) {
            return `Value must be one of: ${attribute.allowedValues.join(', ')}`;
        }
    }

    if (metadata?.minLength !== undefined && trimmed.length < metadata.minLength) {
        return `Minimum length is ${metadata.minLength} characters.`;
    }

    if (metadata?.maxLength !== undefined && trimmed.length > metadata.maxLength) {
        return `Maximum length is ${metadata.maxLength} characters.`;
    }

    return null;
}

function pickFieldKind(attribute: AttributeDefinition, metadata?: AttributeUiMetadata) {
    if (metadata?.fieldKind) {
        return metadata.fieldKind;
    }

    if (attribute.type === 'boolean') {
        return 'BOOLEAN';
    }

    if (attribute.type === 'number') {
        return 'NUMBER';
    }

    if (attribute.allowedValues && attribute.allowedValues.length > 0) {
        return 'SELECT';
    }

    if (attribute.type === 'xpath') {
        return 'EXPRESSION';
    }

    return 'TEXT';
}

export { DEFAULT_GROUP_LABEL };


