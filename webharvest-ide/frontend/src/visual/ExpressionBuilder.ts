/**
 * ExpressionBuilder - Implementation
 * Visual builder for ${variable} expressions with autocomplete and preview
 */

export interface FunctionDefinition {
    name: string;
    parameters: string[];
    description: string;
    example: string;
}

export class ExpressionBuilder {
    private container: HTMLElement | null = null;
    private inputElement: HTMLTextAreaElement | null = null;
    private previewElement: HTMLElement | null = null;
    private autocompleteElement: HTMLElement | null = null;
    private availableVariables: Record<string, string> = {};
    private onChangeCallback: ((expression: string) => void) | null = null;
    private currentExpression: string = '';

    private readonly FUNCTIONS: FunctionDefinition[] = [
        {
            name: 'concat',
            parameters: ['str1', 'str2'],
            description: 'Concatenate two strings',
            example: 'concat(${firstName}, " ", ${lastName})'
        },
        {
            name: 'replace',
            parameters: ['str', 'find', 'replace'],
            description: 'Replace text in string',
            example: 'replace(${text}, "old", "new")'
        },
        {
            name: 'substring',
            parameters: ['str', 'start', 'end'],
            description: 'Extract substring',
            example: 'substring(${text}, 0, 10)'
        },
        {
            name: 'uppercase',
            parameters: ['str'],
            description: 'Convert to uppercase',
            example: 'uppercase(${name})'
        },
        {
            name: 'lowercase',
            parameters: ['str'],
            description: 'Convert to lowercase',
            example: 'lowercase(${NAME})'
        },
        {
            name: 'trim',
            parameters: ['str'],
            description: 'Remove whitespace',
            example: 'trim(${text})'
        },
        {
            name: 'length',
            parameters: ['str'],
            description: 'Get string length',
            example: 'length(${text})'
        }
    ];

    /**
     * Render expression builder
     */
    render(container: HTMLElement, initialExpression: string = ''): void {
        this.container = container;
        this.currentExpression = initialExpression;

        container.innerHTML = '';
        container.className = 'expression-builder';
        container.style.cssText = `
            display: flex;
            flex-direction: column;
            gap: 12px;
            padding: 16px;
            background: white;
            border-radius: 8px;
        `;

        // Header
        const header = document.createElement('div');
        header.innerHTML = '<strong>Expression Builder</strong>';
        header.style.cssText = 'font-size: 14px; color: var(--text-primary);';
        container.appendChild(header);

        // Input area
        this.inputElement = this.createInput(initialExpression);
        container.appendChild(this.inputElement);

        // Autocomplete popup (hidden by default)
        this.autocompleteElement = this.createAutocompletePopup();
        container.appendChild(this.autocompleteElement);

        // Sections container
        const sectionsContainer = document.createElement('div');
        sectionsContainer.style.cssText = `
            display: grid;
            grid-template-columns: 1fr 1fr;
            gap: 12px;
        `;

        // Variable suggestions
        const varSuggestions = this.createVariableSuggestions();
        sectionsContainer.appendChild(varSuggestions);

        // Function library
        const funcLibrary = this.createFunctionLibrary();
        sectionsContainer.appendChild(funcLibrary);

        container.appendChild(sectionsContainer);

        // Preview
        this.previewElement = this.createPreview();
        container.appendChild(this.previewElement);

        // Initialize
        this.updatePreview();
    }

    /**
     * Create input textarea
     */
    private createInput(value: string): HTMLTextAreaElement {
        const input = document.createElement('textarea');
        input.className = 'expression-input';
        input.value = value;
        input.rows = 3;
        input.placeholder = '${variable} or function(${variable})';
        input.style.cssText = `
            padding: 10px;
            border: 2px solid var(--gray-300);
            border-radius: 6px;
            font-family: 'Courier New', monospace;
            font-size: 13px;
            resize: vertical;
            transition: all 0.2s;
        `;

        input.addEventListener('input', () => {
            this.currentExpression = input.value;
            this.checkAutocomplete(input);
            this.updatePreview();
            if (this.onChangeCallback) {
                this.onChangeCallback(input.value);
            }
        });

        input.addEventListener('focus', () => {
            input.style.borderColor = 'var(--magenta)';
            input.style.outline = 'none';
        });

        input.addEventListener('blur', () => {
            input.style.borderColor = 'var(--gray-300)';
        });

        return input;
    }

    /**
     * Create autocomplete popup
     */
    private createAutocompletePopup(): HTMLElement {
        const popup = document.createElement('div');
        popup.className = 'autocomplete-popup';
        popup.style.cssText = `
            display: none;
            position: absolute;
            background: white;
            border: 1px solid var(--gray-300);
            border-radius: 6px;
            box-shadow: var(--shadow-md);
            max-height: 200px;
            overflow-y: auto;
            z-index: 1000;
        `;
        return popup;
    }

    /**
     * Check if autocomplete should be shown
     */
    private checkAutocomplete(input: HTMLTextAreaElement): void {
        const value = input.value;
        const cursorPos = input.selectionStart;

        // Check if user typed ${
        const beforeCursor = value.substring(0, cursorPos);
        if (beforeCursor.endsWith('${')) {
            this.showAutocomplete();
        } else {
            this.hideAutocomplete();
        }
    }

    /**
     * Show autocomplete with variable suggestions
     */
    private showAutocomplete(): void {
        if (!this.autocompleteElement) return;

        this.autocompleteElement.innerHTML = '';
        this.autocompleteElement.style.display = 'block';

        const variables = Object.keys(this.availableVariables);
        variables.forEach(varName => {
            const item = document.createElement('div');
            item.textContent = varName;
            item.style.cssText = `
                padding: 8px 12px;
                cursor: pointer;
                font-family: monospace;
                font-size: 12px;
            `;

            item.addEventListener('mouseenter', () => {
                item.style.background = 'var(--gray-100)';
            });

            item.addEventListener('mouseleave', () => {
                item.style.background = '';
            });

            item.addEventListener('click', () => {
                this.insertVariable(varName);
                this.hideAutocomplete();
            });

            this.autocompleteElement!.appendChild(item);
        });
    }

    /**
     * Hide autocomplete
     */
    private hideAutocomplete(): void {
        if (this.autocompleteElement) {
            this.autocompleteElement.style.display = 'none';
        }
    }

    /**
     * Create variable suggestions panel
     */
    private createVariableSuggestions(): HTMLElement {
        const panel = document.createElement('div');
        panel.className = 'variable-suggestions';
        panel.style.cssText = `
            border: 1px solid var(--gray-200);
            border-radius: 6px;
            padding: 12px;
            background: var(--gray-50);
        `;

        const title = document.createElement('div');
        title.innerHTML = '<strong>▼ Available Variables</strong>';
        title.style.cssText = 'font-size: 12px; margin-bottom: 8px; color: var(--text-primary);';
        panel.appendChild(title);

        const list = document.createElement('div');
        list.className = 'variable-list';
        list.style.cssText = 'font-size: 11px; color: var(--gray-700);';
        panel.appendChild(list);

        return panel;
    }

    /**
     * Create function library panel
     */
    private createFunctionLibrary(): HTMLElement {
        const panel = document.createElement('div');
        panel.className = 'function-library';
        panel.style.cssText = `
            border: 1px solid var(--gray-200);
            border-radius: 6px;
            padding: 12px;
            background: var(--gray-50);
        `;

        const title = document.createElement('div');
        title.innerHTML = '<strong>▼ Functions</strong>';
        title.style.cssText = 'font-size: 12px; margin-bottom: 8px; color: var(--text-primary);';
        panel.appendChild(title);

        const list = document.createElement('div');
        list.style.cssText = 'display: flex; flex-direction: column; gap: 4px;';

        this.FUNCTIONS.forEach(func => {
            const item = document.createElement('div');
            item.textContent = `• ${func.name}(${func.parameters.join(', ')})`;
            item.title = func.description;
            item.style.cssText = `
                font-size: 11px;
                font-family: monospace;
                cursor: pointer;
                color: var(--purple);
            `;

            item.addEventListener('click', () => {
                this.insertFunction(func.name, func.parameters);
            });

            list.appendChild(item);
        });

        panel.appendChild(list);

        return panel;
    }

    /**
     * Create preview panel
     */
    private createPreview(): HTMLElement {
        const panel = document.createElement('div');
        panel.className = 'expression-preview';
        panel.style.cssText = `
            border: 1px solid var(--gray-200);
            border-radius: 6px;
            padding: 12px;
            background: var(--gray-50);
        `;

        const title = document.createElement('div');
        title.innerHTML = '<strong>Preview:</strong>';
        title.style.cssText = 'font-size: 12px; margin-bottom: 8px; color: var(--text-primary);';
        panel.appendChild(title);

        const preview = document.createElement('div');
        preview.className = 'preview-value';
        preview.style.cssText = `
            font-family: monospace;
            font-size: 12px;
            color: var(--lime);
            white-space: pre-wrap;
        `;
        panel.appendChild(preview);

        return panel;
    }

    /**
     * Set available variables for autocomplete
     */
    setAvailableVariables(variables: Record<string, string>): void {
        this.availableVariables = variables;
        this.updateVariableList();
        this.updatePreview();
    }

    /**
     * Update variable list display
     */
    private updateVariableList(): void {
        if (!this.container) return;

        const list = this.container.querySelector('.variable-list');
        if (!list) return;

        list.innerHTML = '';

        Object.keys(this.availableVariables).forEach(varName => {
            const item = document.createElement('div');
            item.className = 'variable-item';
            item.textContent = `• ${varName}`;
            item.style.cssText = `
                font-family: monospace;
                cursor: pointer;
                padding: 2px 0;
                color: var(--purple);
            `;

            item.addEventListener('click', () => {
                this.insertVariable(varName);
            });

            list.appendChild(item);
        });
    }

    /**
     * Insert variable at cursor position
     */
    insertVariable(varName: string): void {
        if (!this.inputElement) return;

        const start = this.inputElement.selectionStart;
        const end = this.inputElement.selectionEnd;
        const value = this.inputElement.value;

        // If last characters are ${, replace them
        let newValue: string;
        if (value.substring(start - 2, start) === '${') {
            newValue = value.substring(0, start - 2) + '${' + varName + '}' + value.substring(end);
        } else {
            newValue = value.substring(0, start) + '${' + varName + '}' + value.substring(end);
        }

        this.inputElement.value = newValue;
        this.currentExpression = newValue;
        this.updatePreview();

        if (this.onChangeCallback) {
            this.onChangeCallback(newValue);
        }
    }

    /**
     * Insert function at cursor position
     */
    insertFunction(funcName: string, parameters: string[]): void {
        if (!this.inputElement) return;

        const start = this.inputElement.selectionStart;
        const end = this.inputElement.selectionEnd;
        const value = this.inputElement.value;

        const paramPlaceholders = parameters.map(p => `\${${p}}`).join(', ');
        const funcTemplate = `${funcName}(${paramPlaceholders})`;

        const newValue = value.substring(0, start) + funcTemplate + value.substring(end);

        this.inputElement.value = newValue;
        this.currentExpression = newValue;
        this.updatePreview();

        if (this.onChangeCallback) {
            this.onChangeCallback(newValue);
        }
    }

    /**
     * Update preview with evaluated expression
     */
    private updatePreview(): void {
        if (!this.previewElement) return;

        const previewValue = this.previewElement.querySelector('.preview-value');
        if (!previewValue) return;

        const evaluated = this.evaluatePreview();
        previewValue.textContent = evaluated;
    }

    /**
     * Evaluate expression for preview
     */
    evaluatePreview(): string {
        let result = this.currentExpression;

        // Replace ${variable} with actual values
        const variablePattern = /\$\{(\w+)\}/g;
        result = result.replace(variablePattern, (match, varName) => {
            if (varName in this.availableVariables) {
                return this.availableVariables[varName];
            }
            return `[undefined: ${varName}]`;
        });

        return result || '(empty)';
    }

    /**
     * Get syntax-highlighted expression
     */
    getHighlightedExpression(): string {
        const variablePattern = /\$\{(\w+)\}/g;
        return this.currentExpression.replace(
            variablePattern,
            '<span style="color: var(--purple); font-weight: 600;">${$1}</span>'
        );
    }

    /**
     * Filter variables by prefix
     */
    filterVariables(prefix: string): string[] {
        return Object.keys(this.availableVariables).filter(varName =>
            varName.toLowerCase().startsWith(prefix.toLowerCase())
        );
    }

    /**
     * Get available functions
     */
    getFunctions(): FunctionDefinition[] {
        return [...this.FUNCTIONS];
    }

    /**
     * Get current expression value
     */
    getValue(): string {
        return this.inputElement?.value || this.currentExpression;
    }

    /**
     * Validate expression
     */
    validate(): string[] {
        const errors: string[] = [];
        const value = this.getValue();

        // Check balanced braces
        const openCount = (value.match(/\$\{/g) || []).length;
        const closeCount = (value.match(/\}/g) || []).length;

        if (openCount !== closeCount) {
            errors.push('Unbalanced braces in expression');
        }

        return errors;
    }

    /**
     * Get warnings about undefined variables
     */
    getWarnings(): string[] {
        const warnings: string[] = [];
        const value = this.getValue();
        const variablePattern = /\$\{(\w+)\}/g;
        let match;

        while ((match = variablePattern.exec(value)) !== null) {
            const varName = match[1];
            if (!(varName in this.availableVariables)) {
                warnings.push(`Variable '${varName}' is not defined`);
            }
        }

        return warnings;
    }

    /**
     * Register onChange callback
     */
    onChange(callback: (expression: string) => void): void {
        this.onChangeCallback = callback;
    }

    /**
     * Clear builder
     */
    clear(): void {
        if (this.inputElement) {
            this.inputElement.value = '';
        }
        this.currentExpression = '';
        this.updatePreview();
    }
}


