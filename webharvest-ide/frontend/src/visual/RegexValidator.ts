/**
 * RegexValidator - Implementation
 * Interactive regex editor with pattern library and match highlighting
 */

export interface RegexTestResult {
    matches: Array<{
        text: string;
        index: number;
        length: number;
        groups?: string[];
    }>;
    totalMatches: number;
    error?: string;
}

export interface RegexPattern {
    name: string;
    pattern: string;
    description: string;
    example: string;
}

export interface RegexValidationResult {
    valid: boolean;
    error?: string;
}

export interface RegexFlags {
    global: boolean;
    caseInsensitive: boolean;
    multiline: boolean;
}

export class RegexValidator {
    private container: HTMLElement | null = null;
    private regexInput: HTMLInputElement | null = null;
    private testTextInput: HTMLTextAreaElement | null = null;
    private resultsPanel: HTMLElement | null = null;
    private flagCheckboxes: Map<string, HTMLInputElement> = new Map();
    private onChangeCallback: ((pattern: string, flags: string) => void) | null = null;

    private readonly PATTERN_LIBRARY: Record<string, RegexPattern> = {
        email: {
            name: 'Email',
            pattern: '^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$',
            description: 'Standard email address',
            example: 'user@example.com'
        },
        url: {
            name: 'URL',
            pattern: 'https?://[\\w.-]+(?:\\.[\\w\\.-]+)+[\\w\\-\\._~:/?#[\\]@!\\$&\'\\(\\)\\*\\+,;=.]+',
            description: 'HTTP/HTTPS URL',
            example: 'https://example.com/path'
        },
        phone: {
            name: 'Phone',
            pattern: '^\\+?[1-9]\\d{1,14}$',
            description: 'International phone number',
            example: '+1234567890'
        },
        date: {
            name: 'Date (YYYY-MM-DD)',
            pattern: '^\\d{4}-\\d{2}-\\d{2}$',
            description: 'ISO date format',
            example: '2025-10-29'
        },
        ipv4: {
            name: 'IPv4 Address',
            pattern: '^(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$',
            description: 'IPv4 address',
            example: '192.168.1.1'
        },
        uuid: {
            name: 'UUID',
            pattern: '^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$',
            description: 'UUID v4',
            example: '550e8400-e29b-41d4-a716-446655440000'
        }
    };

    /**
     * Render regex validator interface
     */
    render(container: HTMLElement, initialPattern: string = ''): void {
        this.container = container;
        container.innerHTML = '';
        container.className = 'regex-validator';
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
        header.innerHTML = '<strong>Regex Validator</strong>';
        header.style.cssText = 'font-size: 14px; color: var(--text-primary);';
        container.appendChild(header);

        // Regex input
        const regexSection = this.createRegexInput(initialPattern);
        container.appendChild(regexSection);

        // Flags
        const flagsSection = this.createFlagsSection();
        container.appendChild(flagsSection);

        // Test text input
        const textSection = this.createTestTextInput();
        container.appendChild(textSection);

        // Test button
        const testButton = this.createTestButton();
        container.appendChild(testButton);

        // Results
        this.resultsPanel = this.createResultsPanel();
        container.appendChild(this.resultsPanel);

        // Pattern library
        const library = this.createPatternLibrary();
        container.appendChild(library);
    }

    /**
     * Create regex input
     */
    private createRegexInput(value: string): HTMLElement {
        const section = document.createElement('div');
        section.style.cssText = 'display: flex; flex-direction: column; gap: 6px;';

        const label = document.createElement('label');
        label.textContent = 'Regular Expression:';
        label.style.cssText = 'font-size: 13px; font-weight: 600; color: var(--text-primary);';
        section.appendChild(label);

        this.regexInput = document.createElement('input');
        this.regexInput.className = 'regex-input';
        this.regexInput.type = 'text';
        this.regexInput.value = value;
        this.regexInput.placeholder = '\\d+|[a-z]+';
        this.regexInput.style.cssText = `
            padding: 10px;
            border: 2px solid var(--gray-300);
            border-radius: 6px;
            font-family: 'Courier New', monospace;
            font-size: 13px;
        `;

        this.regexInput.addEventListener('input', () => {
            const validation = this.validateRegex(this.regexInput!.value);

            if (validation.valid) {
                this.regexInput!.style.borderColor = 'var(--lime)';
            } else {
                this.regexInput!.style.borderColor = 'var(--magenta)';
            }

            if (this.onChangeCallback) {
                this.onChangeCallback(this.regexInput!.value, this.getFlagsString());
            }
        });

        section.appendChild(this.regexInput);

        return section;
    }

    /**
     * Create flags section
     */
    private createFlagsSection(): HTMLElement {
        const section = document.createElement('div');
        section.style.cssText = 'display: flex; gap: 16px; align-items: center;';

        const label = document.createElement('span');
        label.textContent = 'Flags:';
        label.style.cssText = 'font-size: 13px; font-weight: 600; color: var(--text-primary);';
        section.appendChild(label);

        const flags = [
            { name: 'g', label: 'Global', checked: true },
            { name: 'i', label: 'Case Insensitive', checked: false },
            { name: 'm', label: 'Multiline', checked: false }
        ];

        flags.forEach(flag => {
            const checkbox = this.createCheckbox(flag.name, flag.label, flag.checked);
            this.flagCheckboxes.set(flag.name, checkbox.querySelector('input') as HTMLInputElement);
            section.appendChild(checkbox);
        });

        return section;
    }

    /**
     * Create checkbox for flag
     */
    private createCheckbox(name: string, label: string, checked: boolean): HTMLElement {
        const wrapper = document.createElement('label');
        wrapper.style.cssText = 'display: flex; align-items: center; gap: 6px; cursor: pointer; font-size: 12px;';

        const input = document.createElement('input');
        input.type = 'checkbox';
        input.name = `flag-${name}`;
        input.checked = checked;
        input.style.cssText = 'cursor: pointer;';

        const span = document.createElement('span');
        span.textContent = label;

        wrapper.appendChild(input);
        wrapper.appendChild(span);

        return wrapper;
    }

    /**
     * Create test text input
     */
    private createTestTextInput(): HTMLElement {
        const section = document.createElement('div');
        section.style.cssText = 'display: flex; flex-direction: column; gap: 6px;';

        const label = document.createElement('label');
        label.textContent = 'Test Text:';
        label.style.cssText = 'font-size: 13px; font-weight: 600; color: var(--text-primary);';
        section.appendChild(label);

        this.testTextInput = document.createElement('textarea');
        this.testTextInput.className = 'test-text-input';
        this.testTextInput.rows = 4;
        this.testTextInput.placeholder = 'Enter text to test the regex against...';
        this.testTextInput.style.cssText = `
            padding: 10px;
            border: 1px solid var(--gray-300);
            border-radius: 6px;
            font-family: 'Courier New', monospace;
            font-size: 12px;
            resize: vertical;
        `;

        section.appendChild(this.testTextInput);

        return section;
    }

    /**
     * Create test button
     */
    private createTestButton(): HTMLElement {
        const button = document.createElement('button');
        button.textContent = '▶ Test Regex';
        button.style.cssText = `
            padding: 10px 20px;
            background: var(--lime);
            color: white;
            border: none;
            border-radius: 6px;
            font-weight: 600;
            cursor: pointer;
            transition: all 0.2s;
        `;

        button.addEventListener('click', () => {
            this.runTest();
        });

        return button;
    }

    /**
     * Create results panel
     */
    private createResultsPanel(): HTMLElement {
        const panel = document.createElement('div');
        panel.className = 'regex-results';
        panel.style.cssText = `
            border: 1px solid var(--gray-200);
            border-radius: 6px;
            padding: 12px;
            background: var(--gray-50);
            min-height: 80px;
        `;

        const placeholder = document.createElement('div');
        placeholder.textContent = 'Results will appear here...';
        placeholder.style.cssText = 'color: var(--gray-600); font-size: 12px; font-style: italic;';
        panel.appendChild(placeholder);

        return panel;
    }

    /**
     * Create pattern library panel
     */
    private createPatternLibrary(): HTMLElement {
        const panel = document.createElement('div');
        panel.className = 'pattern-library';
        panel.style.cssText = `
            border: 1px solid var(--gray-200);
            border-radius: 6px;
            padding: 12px;
            background: var(--gray-50);
        `;

        const title = document.createElement('div');
        title.innerHTML = '<strong>Pattern Library:</strong>';
        title.style.cssText = 'font-size: 12px; margin-bottom: 8px; color: var(--text-primary);';
        panel.appendChild(title);

        const grid = document.createElement('div');
        grid.style.cssText = 'display: grid; grid-template-columns: repeat(auto-fill, minmax(150px, 1fr)); gap: 8px;';

        Object.values(this.PATTERN_LIBRARY).forEach(pattern => {
            const item = document.createElement('div');
            item.textContent = pattern.name;
            item.title = pattern.description + '\nExample: ' + pattern.example;
            item.style.cssText = `
                padding: 6px 10px;
                background: white;
                border: 1px solid var(--gray-200);
                border-radius: 4px;
                font-size: 11px;
                cursor: pointer;
                text-align: center;
                transition: all 0.2s;
            `;

            item.addEventListener('click', () => {
                this.loadPattern(pattern);
            });

            item.addEventListener('mouseenter', () => {
                item.style.background = 'var(--purple)';
                item.style.color = 'white';
            });

            item.addEventListener('mouseleave', () => {
                item.style.background = 'white';
                item.style.color = '';
            });

            grid.appendChild(item);
        });

        panel.appendChild(grid);

        return panel;
    }

    /**
     * Validate regex pattern
     */
    validateRegex(pattern: string): RegexValidationResult {
        if (!pattern || pattern.trim() === '') {
            return { valid: true };
        }

        try {
            new RegExp(pattern);
            return { valid: true };
        } catch (error) {
            return {
                valid: false,
                error: (error as Error).message
            };
        }
    }

    /**
     * Test regex against text
     */
    testRegex(pattern: string, text: string, flags: string): RegexTestResult {
        const validation = this.validateRegex(pattern);
        if (!validation.valid) {
            return {
                matches: [],
                totalMatches: 0,
                error: validation.error
            };
        }

        try {
            const regex = new RegExp(pattern, flags);
            const matches: RegexTestResult['matches'] = [];

            if (flags.includes('g')) {
                // Global: find all matches
                let match;
                while ((match = regex.exec(text)) !== null) {
                    matches.push({
                        text: match[0],
                        index: match.index,
                        length: match[0].length,
                        groups: match.slice(1)
                    });
                }
            } else {
                // Non-global: find first match
                const match = text.match(regex);
                if (match) {
                    matches.push({
                        text: match[0],
                        index: match.index || 0,
                        length: match[0].length,
                        groups: match.slice(1)
                    });
                }
            }

            return {
                matches,
                totalMatches: matches.length
            };
        } catch (error) {
            return {
                matches: [],
                totalMatches: 0,
                error: (error as Error).message
            };
        }
    }

    /**
     * Run test with current inputs
     */
    private runTest(): void {
        if (!this.regexInput || !this.testTextInput || !this.resultsPanel) return;

        const pattern = this.regexInput.value;
        const text = this.testTextInput.value;
        const flags = this.getFlagsString();

        const result = this.testRegex(pattern, text, flags);
        this.highlightMatches(result);
    }

    /**
     * Highlight matches in results panel
     */
    highlightMatches(result: RegexTestResult): void {
        if (!this.resultsPanel || !this.testTextInput) return;

        this.resultsPanel.innerHTML = '';

        if (result.error) {
            const errorDiv = document.createElement('div');
            errorDiv.textContent = '❌ ' + result.error;
            errorDiv.style.cssText = 'color: var(--magenta); font-size: 12px;';
            this.resultsPanel.appendChild(errorDiv);
            return;
        }

        // Match count
        const countDiv = document.createElement('div');
        countDiv.className = 'match-count';
        countDiv.textContent = `✓ ${result.totalMatches} match${result.totalMatches !== 1 ? 'es' : ''} found`;
        countDiv.style.cssText = `
            font-size: 13px;
            font-weight: 600;
            color: var(--lime);
            margin-bottom: 12px;
        `;
        this.resultsPanel.appendChild(countDiv);

        // Highlighted text
        const highlightedDiv = document.createElement('div');
        highlightedDiv.className = 'highlighted-text';
        highlightedDiv.style.cssText = `
            padding: 10px;
            background: white;
            border: 1px solid var(--gray-200);
            border-radius: 6px;
            font-family: monospace;
            font-size: 12px;
            white-space: pre-wrap;
            word-wrap: break-word;
            margin-bottom: 12px;
        `;

        let highlightedText = this.testTextInput.value;
        let offset = 0;

        result.matches.forEach((match, index) => {
            const beforeMark = `<mark style="background: rgba(164, 198, 57, 0.3); padding: 2px 4px; border-radius: 3px;" title="Match ${index + 1}">`;
            const afterMark = '</mark>';

            const insertPos = match.index + offset;
            highlightedText =
                highlightedText.substring(0, insertPos) +
                beforeMark +
                highlightedText.substring(insertPos, insertPos + match.length) +
                afterMark +
                highlightedText.substring(insertPos + match.length);

            offset += beforeMark.length + afterMark.length;
        });

        highlightedDiv.innerHTML = highlightedText;
        this.resultsPanel.appendChild(highlightedDiv);

        // Capture groups (if any)
        if (result.matches.some(m => m.groups && m.groups.length > 0)) {
            const groupsDiv = document.createElement('div');
            groupsDiv.className = 'capture-groups';
            groupsDiv.style.cssText = `
                border: 1px solid var(--gray-200);
                border-radius: 6px;
                padding: 10px;
                background: white;
            `;

            const groupsTitle = document.createElement('div');
            groupsTitle.innerHTML = '<strong>Capture Groups:</strong>';
            groupsTitle.style.cssText = 'font-size: 12px; margin-bottom: 6px; color: var(--text-primary);';
            groupsDiv.appendChild(groupsTitle);

            result.matches.forEach((match, matchIndex) => {
                if (match.groups && match.groups.length > 0) {
                    const matchDiv = document.createElement('div');
                    matchDiv.style.cssText = 'margin-bottom: 8px;';

                    const matchLabel = document.createElement('div');
                    matchLabel.textContent = `Match ${matchIndex + 1}:`;
                    matchLabel.style.cssText = 'font-size: 11px; font-weight: 600; color: var(--purple);';
                    matchDiv.appendChild(matchLabel);

                    match.groups.forEach((group, groupIndex) => {
                        const groupItem = document.createElement('div');
                        groupItem.innerHTML = `  Group ${groupIndex + 1}: <code style="color: var(--lime);">${group}</code>`;
                        groupItem.style.cssText = 'font-size: 11px; font-family: monospace;';
                        matchDiv.appendChild(groupItem);
                    });

                    groupsDiv.appendChild(matchDiv);
                }
            });

            this.resultsPanel.appendChild(groupsDiv);
        }
    }

    /**
     * Get flags string
     */
    private getFlagsString(): string {
        let flags = '';

        if (this.flagCheckboxes.get('g')?.checked) flags += 'g';
        if (this.flagCheckboxes.get('i')?.checked) flags += 'i';
        if (this.flagCheckboxes.get('m')?.checked) flags += 'm';

        return flags;
    }

    /**
     * Get pattern library
     */
    getPatternLibrary(): Record<string, RegexPattern> {
        return { ...this.PATTERN_LIBRARY };
    }

    /**
     * Load pattern from library
     */
    loadPattern(pattern: RegexPattern): void {
        if (this.regexInput) {
            this.regexInput.value = pattern.pattern;
            this.regexInput.dispatchEvent(new Event('input'));
        }

        if (this.testTextInput) {
            this.testTextInput.value = pattern.example;
        }
    }

    /**
     * Set test text
     */
    setTestText(text: string): void {
        if (this.testTextInput) {
            this.testTextInput.value = text;
        }
    }

    /**
     * Explain regex pattern (break down components)
     */
    explainRegex(pattern: string): string[] {
        const explanations: string[] = [];

        // Anchors
        if (pattern.includes('^')) {
            explanations.push('^ = Start of line/string');
        }
        if (pattern.includes('$')) {
            explanations.push('$ = End of line/string');
        }

        // Character classes
        if (pattern.includes('\\d')) {
            explanations.push('\\d = Any digit (0-9)');
        }
        if (pattern.includes('\\w')) {
            explanations.push('\\w = Any word character (a-z, A-Z, 0-9, _)');
        }
        if (pattern.includes('\\s')) {
            explanations.push('\\s = Any whitespace');
        }
        if (pattern.includes('[a-z]')) {
            explanations.push('[a-z] = Any lowercase letter');
        }
        if (pattern.includes('[A-Z]')) {
            explanations.push('[A-Z] = Any uppercase letter');
        }
        if (pattern.includes('[0-9]')) {
            explanations.push('[0-9] = Any digit');
        }

        // Quantifiers
        if (pattern.includes('+')) {
            explanations.push('+ = One or more');
        }
        if (pattern.includes('*')) {
            explanations.push('* = Zero or more');
        }
        if (pattern.includes('?')) {
            explanations.push('? = Zero or one (optional)');
        }
        if (pattern.includes('{')) {
            explanations.push('{n,m} = Between n and m occurrences');
        }

        return explanations;
    }

    /**
     * Get current regex value
     */
    getValue(): string {
        return this.regexInput?.value || '';
    }

    /**
     * Get current flags
     */
    getFlags(): RegexFlags {
        return {
            global: this.flagCheckboxes.get('g')?.checked || false,
            caseInsensitive: this.flagCheckboxes.get('i')?.checked || false,
            multiline: this.flagCheckboxes.get('m')?.checked || false
        };
    }

    /**
     * Register onChange callback
     */
    onChange(callback: (pattern: string, flags: string) => void): void {
        this.onChangeCallback = callback;
    }

    /**
     * Clear validator
     */
    clear(): void {
        if (this.regexInput) this.regexInput.value = '';
        if (this.testTextInput) this.testTextInput.value = '';
        if (this.resultsPanel) this.resultsPanel.innerHTML = '';
    }
}


