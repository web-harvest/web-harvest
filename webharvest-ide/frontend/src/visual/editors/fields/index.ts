import type { AttributeDefinition, AttributeUiMetadata } from '../../../types';
import { formatValueForDisplay, parseFormattedValue, formatValueOnInput, validateFormat, type FormatOptions } from '../../../utils/valueFormatter';
import { highlightSyntax, removeSyntaxHighlighting, type SyntaxLanguage } from '../../../utils/syntaxHighlighter';

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
        case 'SLIDER':
            return createSliderField(attribute, metadata, value, onChange);
        case 'URL':
            return createUrlField(attribute, metadata, value, onChange);
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
    // Styles are now handled by CSS
    container.style.display = 'flex';
    container.style.flexDirection = 'column';

    const label = document.createElement('label');
    const labelText = metadata?.localizationKey || attribute.description || attribute.name;
    label.textContent = labelText;
    // Styles are now handled by CSS
    label.style.display = 'flex';
    label.style.alignItems = 'center';
    label.style.gap = '8px';
    
    // Add tooltip with full description if different from label
    if (attribute.description && attribute.description !== labelText) {
        label.title = attribute.description;
    }

    if (attribute.required) {
        const badge = document.createElement('span');
        badge.textContent = '•';
        badge.style.color = 'var(--magenta)';
        badge.setAttribute('aria-hidden', 'true');
        label.appendChild(badge);
    }

    const helper = document.createElement('div');
    helper.className = 'field-helper';
    // Styles are now handled by CSS
    helper.style.display = (metadata?.helperText || attribute.description) ? 'block' : 'none';
    helper.textContent = metadata?.helperText || (attribute.description && attribute.description !== labelText ? attribute.description : '');

    const error = document.createElement('div');
    error.className = 'field-error';
    // Styles are now handled by CSS

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
    input.type = getInputType(attribute.type, metadata);
    
    // Format value for display
    const formatOptions: FormatOptions = {
        type: attribute.type,
        format: metadata?.format,
        minValue: attribute.minValue,
        maxValue: attribute.maxValue,
        allowedValues: attribute.allowedValues
    };
    
    const displayValue = formatValueForDisplay(value, formatOptions);
    input.value = displayValue;
    input.placeholder = metadata?.placeholder ?? '';
    input.className = 'schema-driven-input';
    // Styles are now handled by CSS

    // Real-time validation
    let validationTimeout: number | null = null;
    input.addEventListener('input', () => {
        const rawValue = parseFormattedValue(input.value, formatOptions);
        const formatted = formatValueOnInput(input.value, formatOptions);
        if (formatted !== input.value) {
            input.value = formatted;
        }
        
        // Debounce validation
        if (validationTimeout !== null) {
            clearTimeout(validationTimeout);
        }
        validationTimeout = window.setTimeout(() => {
            const validation = validateFormat(rawValue, formatOptions);
            if (validation.valid) {
                input.style.borderColor = 'var(--gray-300)';
                input.style.boxShadow = 'none';
                error.textContent = '';
            } else {
                input.style.borderColor = 'var(--error)';
                input.style.boxShadow = '0 0 0 3px rgba(239, 68, 68, 0.1)';
                error.textContent = validation.message || '';
            }
        }, 300);
        
        onChange(rawValue);
    });

    // Visual feedback on focus
    input.addEventListener('focus', () => {
        input.style.borderColor = 'var(--magenta)';
        input.style.boxShadow = '0 0 0 3px rgba(139, 92, 246, 0.1)';
    });

    input.addEventListener('blur', () => {
        const rawValue = parseFormattedValue(input.value, formatOptions);
        const validation = validateFormat(rawValue, formatOptions);
        if (validation.valid) {
            input.style.borderColor = 'var(--gray-300)';
            input.style.boxShadow = 'none';
            // Re-format on blur for better display
            input.value = formatValueForDisplay(rawValue, formatOptions);
        } else {
            input.style.borderColor = 'var(--error)';
            input.style.boxShadow = '0 0 0 3px rgba(239, 68, 68, 0.1)';
        }
    });

    container.appendChild(input);
    container.appendChild(error);

    return buildFieldAPI(container, input, attribute, metadata, helper, error);
}

function getInputType(attributeType: string, metadata?: AttributeUiMetadata): string {
    // Use HTML5 input types when appropriate
    switch (attributeType) {
        case 'url':
            return 'url';
        case 'file_path':
            return 'text'; // Could use 'file' but that's for file picker
        default:
            // Check format for hints
            if (metadata?.format) {
                const format = metadata.format.toLowerCase();
                if (format === 'email') return 'email';
                if (format === 'tel') return 'tel';
            }
            return 'text';
    }
}

function createTextareaField(
    attribute: AttributeDefinition,
    metadata: AttributeUiMetadata | undefined,
    value: string,
    onChange: (value: string) => void
): PropertyField {
    const { container, helper, error } = createFieldContainer(attribute, metadata);
    
    // Create wrapper for syntax highlighting
    const wrapper = document.createElement('div');
    wrapper.style.position = 'relative';
    
    const textarea = document.createElement('textarea');
    
    const isJson = metadata?.fieldKind === 'JSON' || metadata?.format === 'json';
    const isCode = metadata?.fieldKind === 'CODE' || metadata?.format === 'code';
    const isExpression = metadata?.fieldKind === 'EXPRESSION' || attribute.type === 'xpath';
    
    // Determine syntax language
    let syntaxLanguage: SyntaxLanguage | null = null;
    if (isJson) {
        syntaxLanguage = 'json';
    } else if (isCode || metadata?.format === 'javascript') {
        syntaxLanguage = 'javascript';
    } else if (isExpression || attribute.type === 'xpath') {
        syntaxLanguage = attribute.type === 'xpath' ? 'xpath' : 'expression';
    }
    
    // Format JSON for display
    if (isJson && value) {
        try {
            const parsed = JSON.parse(value);
            textarea.value = JSON.stringify(parsed, null, 2);
        } catch {
            textarea.value = value ?? '';
        }
    } else {
        textarea.value = value ?? '';
    }
    
    textarea.placeholder = metadata?.placeholder ?? '';
    textarea.rows = isJson || isCode ? 8 : 4;
    textarea.className = 'schema-driven-input';
    // Styles are now handled by CSS
    if (isJson || isCode || isExpression) {
        textarea.style.fontFamily = 'var(--font-mono, "Monaco", "Menlo", "Courier New", monospace)';
    }

    // Setup syntax highlighting
    if (syntaxLanguage) {
        // Wait for textarea to be in DOM
        setTimeout(() => {
            highlightSyntax(textarea, syntaxLanguage!);
        }, 0);
    }
    
    // Real-time validation for JSON
    let validationTimeout: number | null = null;
    textarea.addEventListener('input', () => {
        const rawValue = textarea.value;
        
        // Update syntax highlighting
        if (syntaxLanguage) {
            // Re-highlight will be triggered by the highlightSyntax function's input listener
        }
        
        // Debounce validation
        if (validationTimeout !== null) {
            clearTimeout(validationTimeout);
        }
        validationTimeout = window.setTimeout(() => {
            if (isJson && rawValue.trim()) {
                try {
                    JSON.parse(rawValue);
                    textarea.style.borderColor = 'var(--gray-300)';
                    textarea.style.boxShadow = 'none';
                    error.textContent = '';
                } catch (e) {
                    textarea.style.borderColor = 'var(--error)';
                    textarea.style.boxShadow = '0 0 0 3px rgba(239, 68, 68, 0.1)';
                    error.textContent = `Invalid JSON: ${(e as Error).message}`;
                }
            } else {
                textarea.style.borderColor = 'var(--gray-300)';
                textarea.style.boxShadow = 'none';
                error.textContent = '';
            }
        }, 500);
        
        onChange(rawValue);
    });

    // Format JSON on blur
    if (isJson) {
        textarea.addEventListener('blur', () => {
            const rawValue = textarea.value.trim();
            if (rawValue) {
                try {
                    const parsed = JSON.parse(rawValue);
                    const formatted = JSON.stringify(parsed, null, 2);
                    textarea.value = formatted;
                    // Re-highlight after formatting
                    if (syntaxLanguage) {
                        setTimeout(() => {
                            removeSyntaxHighlighting(textarea);
                            highlightSyntax(textarea, syntaxLanguage!);
                        }, 0);
                    }
                    textarea.style.borderColor = 'var(--gray-300)';
                    textarea.style.boxShadow = 'none';
                    error.textContent = '';
                } catch {
                    // Keep invalid JSON visible for user to fix
                }
            }
        });
    }

    // Visual feedback on focus
    textarea.addEventListener('focus', () => {
        textarea.style.borderColor = 'var(--magenta)';
        textarea.style.boxShadow = '0 0 0 3px rgba(139, 92, 246, 0.1)';
    });

    textarea.addEventListener('blur', () => {
        if (!isJson || !textarea.value.trim()) {
            const validation = validateFormat(textarea.value, {
                type: attribute.type,
                format: metadata?.format
            });
            if (validation.valid) {
                textarea.style.borderColor = 'var(--gray-300)';
                textarea.style.boxShadow = 'none';
            } else {
                textarea.style.borderColor = 'var(--error)';
                textarea.style.boxShadow = '0 0 0 3px rgba(239, 68, 68, 0.1)';
            }
        }
    });

    wrapper.appendChild(textarea);
    container.appendChild(wrapper);
    container.appendChild(error);
    
    return {
        ...buildFieldAPI(container, textarea, attribute, metadata, helper, error),
        element: container,
        setValue: (val: string) => {
            textarea.value = val ?? '';
            if (syntaxLanguage) {
                setTimeout(() => {
                    removeSyntaxHighlighting(textarea);
                    highlightSyntax(textarea, syntaxLanguage!);
                }, 0);
            }
        }
    };
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
    input.step = attribute.type === 'double' ? 'any' : '1';
    
    // Format value for display
    const formatOptions: FormatOptions = {
        type: attribute.type,
        format: metadata?.format,
        minValue: attribute.minValue,
        maxValue: attribute.maxValue
    };
    
    const displayValue = formatValueForDisplay(value, formatOptions);
    input.value = parseFormattedValue(displayValue, formatOptions);
    input.placeholder = metadata?.placeholder ?? '';
    input.className = 'schema-driven-input';
    // Styles are now handled by CSS

    if (attribute.minValue !== undefined) {
        input.min = String(attribute.minValue);
    }
    if (attribute.maxValue !== undefined) {
        input.max = String(attribute.maxValue);
    }

    // Real-time validation
    let validationTimeout: number | null = null;
    input.addEventListener('input', () => {
        const rawValue = input.value;
        
        // Debounce validation
        if (validationTimeout !== null) {
            clearTimeout(validationTimeout);
        }
        validationTimeout = window.setTimeout(() => {
            const validation = validateFormat(rawValue, formatOptions);
            if (validation.valid) {
                input.style.borderColor = 'var(--gray-300)';
                input.style.boxShadow = 'none';
                error.textContent = '';
            } else {
                input.style.borderColor = 'var(--error)';
                input.style.boxShadow = '0 0 0 3px rgba(239, 68, 68, 0.1)';
                error.textContent = validation.message || '';
            }
        }, 300);
        
        onChange(rawValue);
    });

    // Visual feedback on focus
    input.addEventListener('focus', () => {
        input.style.borderColor = 'var(--magenta)';
        input.style.boxShadow = '0 0 0 3px rgba(139, 92, 246, 0.1)';
    });

    input.addEventListener('blur', () => {
        const validation = validateFormat(input.value, formatOptions);
        if (validation.valid) {
            input.style.borderColor = 'var(--gray-300)';
            input.style.boxShadow = 'none';
            // Re-format on blur
            const formatted = formatValueForDisplay(input.value, formatOptions);
            if (formatted !== input.value && !isNaN(parseFloat(input.value))) {
                input.value = parseFormattedValue(formatted, formatOptions);
            }
        } else {
            input.style.borderColor = 'var(--error)';
            input.style.boxShadow = '0 0 0 3px rgba(239, 68, 68, 0.1)';
        }
    });

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
    select.className = 'schema-driven-input';
    // Styles are now handled by CSS

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

function createUrlField(
    attribute: AttributeDefinition,
    metadata: AttributeUiMetadata | undefined,
    value: string,
    onChange: (value: string) => void
): PropertyField {
    const { container, helper, error } = createFieldContainer(attribute, metadata);
    const wrapper = document.createElement('div');
    wrapper.style.display = 'flex';
    wrapper.style.gap = '4px';
    wrapper.style.alignItems = 'stretch';

    const input = document.createElement('input');
    input.type = 'url';
    input.value = value ?? '';
    input.placeholder = metadata?.placeholder || 'https://example.com';
    input.className = 'schema-driven-input';
    input.style.flex = '1';
    input.style.padding = '8px 10px';
    input.style.border = '1px solid var(--gray-300)';
    input.style.borderRadius = 'var(--radius-sm)';
    input.style.fontSize = '13px';
    input.style.transition = 'border-color 0.2s ease, box-shadow 0.2s ease';

    // Add "Open" button for valid URLs
    const openButton = document.createElement('button');
    openButton.type = 'button';
    openButton.innerHTML = '<i class="fas fa-external-link-alt"></i>';
    openButton.title = 'Open URL in new tab';
    openButton.style.cssText = `
        padding: 8px 12px;
        border: 1px solid var(--gray-300);
        border-radius: var(--radius-sm);
        background: var(--gray-50);
        cursor: pointer;
        color: var(--gray-600);
        transition: all 0.2s ease;
    `;
    openButton.disabled = true;

    const updateOpenButton = () => {
        try {
            new URL(input.value);
            openButton.disabled = false;
            openButton.style.color = 'var(--magenta)';
            openButton.style.borderColor = 'var(--magenta)';
        } catch {
            openButton.disabled = true;
            openButton.style.color = 'var(--gray-400)';
            openButton.style.borderColor = 'var(--gray-300)';
        }
    };

    openButton.addEventListener('click', () => {
        if (input.value) {
            try {
                const url = new URL(input.value);
                window.open(url.toString(), '_blank', 'noopener,noreferrer');
            } catch {
                // Invalid URL, do nothing
            }
        }
    });

    // Real-time validation
    let validationTimeout: number | null = null;
    input.addEventListener('input', () => {
        updateOpenButton();
        
        // Debounce validation
        if (validationTimeout !== null) {
            clearTimeout(validationTimeout);
        }
        validationTimeout = window.setTimeout(() => {
            const validation = validateFormat(input.value, {
                type: 'url',
                format: metadata?.format
            });
            if (validation.valid) {
                input.style.borderColor = 'var(--gray-300)';
                input.style.boxShadow = 'none';
                error.textContent = '';
            } else {
                input.style.borderColor = 'var(--error)';
                input.style.boxShadow = '0 0 0 3px rgba(239, 68, 68, 0.1)';
                error.textContent = validation.message || '';
            }
        }, 300);
        
        onChange(input.value);
    });

    // Visual feedback on focus
    input.addEventListener('focus', () => {
        input.style.borderColor = 'var(--magenta)';
        input.style.boxShadow = '0 0 0 3px rgba(139, 92, 246, 0.1)';
    });

    input.addEventListener('blur', () => {
        updateOpenButton();
        const validation = validateFormat(input.value, {
            type: 'url',
            format: metadata?.format
        });
        if (validation.valid) {
            input.style.borderColor = 'var(--gray-300)';
            input.style.boxShadow = 'none';
        } else {
            input.style.borderColor = 'var(--error)';
            input.style.boxShadow = '0 0 0 3px rgba(239, 68, 68, 0.1)';
        }
    });

    updateOpenButton();

    wrapper.appendChild(input);
    wrapper.appendChild(openButton);
    container.appendChild(wrapper);
    container.appendChild(error);

    return buildFieldAPI(container, input, attribute, metadata, helper, error);
}

function createSliderField(
    attribute: AttributeDefinition,
    metadata: AttributeUiMetadata | undefined,
    value: string,
    onChange: (value: string) => void
): PropertyField {
    const { container, helper, error } = createFieldContainer(attribute, metadata);
    
    const wrapper = document.createElement('div');
    wrapper.style.display = 'flex';
    wrapper.style.alignItems = 'center';
    wrapper.style.gap = '12px';
    
    const slider = document.createElement('input');
    slider.type = 'range';
    slider.style.flex = '1';
    slider.style.height = '6px';
    slider.style.borderRadius = '3px';
    slider.style.background = 'var(--gray-200, #e5e7eb)';
    slider.style.outline = 'none';
    slider.style.transition = 'background 0.2s ease';
    
    const min = attribute.minValue ?? 0;
    const max = attribute.maxValue ?? 100;
    const step = attribute.type === 'double' ? 0.1 : 1;
    
    slider.min = String(min);
    slider.max = String(max);
    slider.step = String(step);
    slider.value = value || String(min);
    
    const valueDisplay = document.createElement('span');
    valueDisplay.style.cssText = `
        min-width: 60px;
        text-align: right;
        font-size: 13px;
        font-weight: 600;
        color: var(--gray-700, #374151);
    `;
    valueDisplay.textContent = slider.value;
    
    slider.addEventListener('input', () => {
        valueDisplay.textContent = slider.value;
        onChange(slider.value);
    });
    
    slider.addEventListener('mousemove', () => {
        const currentValue = parseFloat(slider.value);
        const percentage = ((currentValue - min) / (max - min)) * 100;
        const magenta = 'var(--magenta, #8b5cf6)';
        const gray = 'var(--gray-200, #e5e7eb)';
        slider.style.background = `linear-gradient(to right, ${magenta} 0%, ${magenta} ${percentage}%, ${gray} ${percentage}%, ${gray} 100%)`;
    });
    
    // Initialize background
    slider.dispatchEvent(new Event('mousemove'));
    
    wrapper.appendChild(slider);
    wrapper.appendChild(valueDisplay);
    container.appendChild(wrapper);
    container.appendChild(error);
    
    return {
        element: container,
        focus: () => slider.focus(),
        getValue: () => slider.value,
        setValue: (val: string) => {
            const num = parseFloat(val);
            if (!isNaN(num)) {
                slider.value = String(Math.max(min, Math.min(max, num)));
                valueDisplay.textContent = slider.value;
            }
        },
        validate: () => {
            const validation = validateFormat(slider.value, {
                type: attribute.type,
                minValue: attribute.minValue,
                maxValue: attribute.maxValue
            });
            error.textContent = validation.message || '';
            return validation.message || null;
        }
    };
}

function createBooleanField(
    attribute: AttributeDefinition,
    metadata: AttributeUiMetadata | undefined,
    value: string,
    onChange: (value: string) => void
): PropertyField {
    const { container, helper, error } = createFieldContainer(attribute, metadata);
    const wrapper = document.createElement('label');
    // Styles are now handled by CSS
    wrapper.style.display = 'inline-flex';
    wrapper.style.alignItems = 'center';
    wrapper.style.gap = '8px';

    const checkbox = document.createElement('input');
    checkbox.type = 'checkbox';
    checkbox.checked = value === 'true';
    checkbox.addEventListener('change', () => onChange(checkbox.checked ? 'true' : 'false'));

    const text = document.createElement('span');
    text.textContent = metadata?.placeholder || attribute.description || 'Enabled';
    // Styles are now handled by CSS

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

    // Use format validator for type-specific validation
    const formatOptions: FormatOptions = {
        type: attribute.type,
        format: metadata?.format,
        minValue: attribute.minValue,
        maxValue: attribute.maxValue,
        allowedValues: attribute.allowedValues
    };
    
    const formatValidation = validateFormat(trimmed, formatOptions);
    if (!formatValidation.valid && formatValidation.message) {
        return formatValidation.message;
    }

    // Additional validations
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

    if (metadata?.minLength !== undefined && trimmed.length < metadata.minLength) {
        return `Minimum length is ${metadata.minLength} characters.`;
    }

    if (metadata?.maxLength !== undefined && trimmed.length > metadata.maxLength) {
        return `Maximum length is ${metadata.maxLength} characters.`;
    }

    // JSON format validation
    if (metadata?.format === 'json' || metadata?.fieldKind === 'JSON') {
        try {
            JSON.parse(trimmed);
        } catch (e) {
            return `Invalid JSON: ${(e as Error).message}`;
        }
    }

    return null;
}

function pickFieldKind(attribute: AttributeDefinition, metadata?: AttributeUiMetadata) {
    // Metadata fieldKind takes precedence
    if (metadata?.fieldKind) {
        return metadata.fieldKind;
    }

    // Map backend types to field kinds
    switch (attribute.type) {
        case 'boolean':
            return 'BOOLEAN';
        case 'number':
        case 'integer':
        case 'long':
        case 'double':
            return 'NUMBER';
        case 'url':
            return 'URL';
        case 'file_path':
            return 'TEXT'; // Could be enhanced with file picker
        case 'regex':
            return 'TEXT'; // Could be enhanced with regex validator
        case 'xpath':
            return 'EXPRESSION';
        case 'enum':
            return 'SELECT';
        default:
            // Check format string for hints
            if (metadata?.format) {
                const format = metadata.format.toLowerCase();
                if (format === 'json') return 'JSON';
                if (format === 'code' || format === 'expression') return 'CODE';
            }
            return 'TEXT';
    }
}

export { DEFAULT_GROUP_LABEL };


