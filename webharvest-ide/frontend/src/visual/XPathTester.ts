/**
 * XPathTester - Implementation
 * Interactive XPath editor with validation and result preview
 */

export interface XPathValidationResult {
    valid: boolean;
    errors: string[];
}

export interface XPathResult {
    matches: Node[];
    error?: string;
    count: number;
}

export interface XPathSuggestion {
    pattern: string;
    description: string;
    example: string;
}

export interface XPathLibrary {
    basic: XPathSuggestion[];
    attributes: XPathSuggestion[];
    text: XPathSuggestion[];
    advanced: XPathSuggestion[];
}

export class XPathTester {
    private container: HTMLElement | null = null;
    private xpathInput: HTMLTextAreaElement | null = null;
    private xmlInput: HTMLTextAreaElement | null = null;
    private resultsPanel: HTMLElement | null = null;
    private currentXML: Document | null = null;
    private onChangeCallback: ((xpath: string) => void) | null = null;
    private onValidationCallback: ((isValid: boolean) => void) | null = null;

    /**
     * Render XPath tester interface
     */
    render(container: HTMLElement, initialXPath: string = ''): void {
        this.container = container;
        container.innerHTML = '';
        container.className = 'xpath-tester';
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
        header.innerHTML = '<strong>XPath Tester</strong>';
        header.style.cssText = 'font-size: 14px; color: var(--text-primary);';
        container.appendChild(header);

        // XPath input section
        const xpathSection = this.createXPathInput(initialXPath);
        container.appendChild(xpathSection);

        // XML input section
        const xmlSection = this.createXMLInput();
        container.appendChild(xmlSection);

        // Test button
        const testButton = this.createTestButton();
        container.appendChild(testButton);

        // Results panel
        this.resultsPanel = this.createResultsPanel();
        container.appendChild(this.resultsPanel);

        // XPath library
        const library = this.createLibraryPanel();
        container.appendChild(library);
    }

    /**
     * Create XPath input section
     */
    private createXPathInput(value: string): HTMLElement {
        const section = document.createElement('div');
        section.style.cssText = 'display: flex; flex-direction: column; gap: 6px;';

        const label = document.createElement('label');
        label.textContent = 'XPath Expression:';
        label.style.cssText = 'font-size: 13px; font-weight: 600; color: var(--text-primary);';
        section.appendChild(label);

        this.xpathInput = document.createElement('textarea');
        this.xpathInput.className = 'xpath-input';
        this.xpathInput.value = value;
        this.xpathInput.rows = 2;
        this.xpathInput.placeholder = '//div[@class="example"]';
        this.xpathInput.style.cssText = `
            padding: 10px;
            border: 2px solid var(--gray-300);
            border-radius: 6px;
            font-family: 'Courier New', monospace;
            font-size: 13px;
            resize: vertical;
        `;

        this.xpathInput.addEventListener('input', () => {
            const xpath = this.xpathInput!.value;

            // Validate in real-time
            const validation = this.validateXPath(xpath);
            if (validation.valid) {
                this.xpathInput!.style.borderColor = 'var(--lime)';
            } else {
                this.xpathInput!.style.borderColor = 'var(--magenta)';
            }

            if (this.onValidationCallback) {
                this.onValidationCallback(validation.valid);
            }

            if (this.onChangeCallback) {
                this.onChangeCallback(xpath);
            }
        });

        section.appendChild(this.xpathInput);

        return section;
    }

    /**
     * Create XML input section
     */
    private createXMLInput(): HTMLElement {
        const section = document.createElement('div');
        section.style.cssText = 'display: flex; flex-direction: column; gap: 6px;';

        const label = document.createElement('label');
        label.textContent = 'Sample XML:';
        label.style.cssText = 'font-size: 13px; font-weight: 600; color: var(--text-primary);';
        section.appendChild(label);

        this.xmlInput = document.createElement('textarea');
        this.xmlInput.className = 'xml-input';
        this.xmlInput.rows = 8;
        this.xmlInput.placeholder = '<root><element>value</element></root>';
        this.xmlInput.style.cssText = `
            padding: 10px;
            border: 1px solid var(--gray-300);
            border-radius: 6px;
            font-family: 'Courier New', monospace;
            font-size: 12px;
            resize: vertical;
        `;

        section.appendChild(this.xmlInput);

        return section;
    }

    /**
     * Create test button
     */
    private createTestButton(): HTMLElement {
        const button = document.createElement('button');
        button.className = 'test-button';
        button.textContent = '▶ Test XPath';
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

        button.addEventListener('mouseenter', () => {
            button.style.transform = 'translateY(-1px)';
            button.style.boxShadow = '0 4px 8px rgba(164, 198, 57, 0.3)';
        });

        button.addEventListener('mouseleave', () => {
            button.style.transform = '';
            button.style.boxShadow = '';
        });

        return button;
    }

    /**
     * Create results panel
     */
    private createResultsPanel(): HTMLElement {
        const panel = document.createElement('div');
        panel.className = 'xpath-results';
        panel.style.cssText = `
            border: 1px solid var(--gray-200);
            border-radius: 6px;
            padding: 12px;
            background: var(--gray-50);
            min-height: 100px;
        `;

        const placeholder = document.createElement('div');
        placeholder.textContent = 'Results will appear here...';
        placeholder.style.cssText = 'color: var(--gray-600); font-size: 12px; font-style: italic;';
        panel.appendChild(placeholder);

        return panel;
    }

    /**
     * Create XPath library panel
     */
    private createLibraryPanel(): HTMLElement {
        const panel = document.createElement('div');
        panel.style.cssText = `
            border: 1px solid var(--gray-200);
            border-radius: 6px;
            padding: 12px;
            background: var(--gray-50);
        `;

        const title = document.createElement('div');
        title.innerHTML = '<strong>Common XPath Patterns:</strong>';
        title.style.cssText = 'font-size: 12px; margin-bottom: 8px; color: var(--text-primary);';
        panel.appendChild(title);

        const suggestions = this.getXPathSuggestions();
        const list = document.createElement('div');
        list.style.cssText = 'display: flex; flex-direction: column; gap: 4px;';

        suggestions.slice(0, 5).forEach(suggestion => {
            const item = document.createElement('div');
            item.innerHTML = `<code style="color: var(--purple); font-size: 11px;">${suggestion.pattern}</code> - ${suggestion.description}`;
            item.style.cssText = 'font-size: 11px; cursor: pointer; padding: 4px; border-radius: 4px;';

            item.addEventListener('click', () => {
                if (this.xpathInput) {
                    this.xpathInput.value = suggestion.pattern;
                    this.xpathInput.dispatchEvent(new Event('input'));
                }
            });

            item.addEventListener('mouseenter', () => {
                item.style.background = 'var(--gray-100)';
            });

            item.addEventListener('mouseleave', () => {
                item.style.background = '';
            });

            list.appendChild(item);
        });

        panel.appendChild(list);

        return panel;
    }

    /**
     * Run XPath test
     */
    private runTest(): void {
        if (!this.xpathInput || !this.xmlInput || !this.resultsPanel) return;

        const xpath = this.xpathInput.value.trim();
        const xmlText = this.xmlInput.value.trim();

        if (!xpath || !xmlText) {
            this.showError('Please provide both XPath and XML');
            return;
        }

        // Parse XML
        try {
            const parser = new DOMParser();
            this.currentXML = parser.parseFromString(xmlText, 'text/xml');

            // Check for parse errors
            const parseError = this.currentXML.querySelector('parsererror');
            if (parseError) {
                this.showError('XML Parse Error: ' + parseError.textContent);
                return;
            }
        } catch (error) {
            this.showError('Invalid XML: ' + (error as Error).message);
            return;
        }

        // Execute XPath
        const result = this.executeXPath(xpath);
        this.highlightResults(result);
    }

    /**
     * Set XML for testing
     */
    setXML(xml: string): void {
        if (this.xmlInput) {
            this.xmlInput.value = xml;
        }

        const parser = new DOMParser();
        this.currentXML = parser.parseFromString(xml, 'text/xml');
    }

    /**
     * Validate XPath expression
     */
    validateXPath(xpath: string): XPathValidationResult {
        const errors: string[] = [];

        if (!xpath || xpath.trim() === '') {
            return { valid: true, errors: [] };
        }

        try {
            // Try to parse XPath using browser's XPathEvaluator
            if (typeof XPathEvaluator !== 'undefined') {
                const evaluator = new XPathEvaluator();
                // This will throw if XPath is invalid
                evaluator.createExpression(xpath, null);
            } else {
                // Fallback: basic syntax checks
                const openBrackets = (xpath.match(/\[/g) || []).length;
                const closeBrackets = (xpath.match(/\]/g) || []).length;

                if (openBrackets !== closeBrackets) {
                    errors.push('Unmatched brackets in XPath expression');
                }

                const openParens = (xpath.match(/\(/g) || []).length;
                const closeParens = (xpath.match(/\)/g) || []).length;

                if (openParens !== closeParens) {
                    errors.push('Unmatched parentheses in XPath expression');
                }
            }
        } catch (error) {
            errors.push((error as Error).message);
        }

        return {
            valid: errors.length === 0,
            errors
        };
    }

    /**
     * Execute XPath against current XML
     */
    executeXPath(xpath: string): XPathResult {
        if (!this.currentXML) {
            return { matches: [], error: 'No XML document loaded', count: 0 };
        }

        // Validate first
        const validation = this.validateXPath(xpath);
        if (!validation.valid) {
            return { matches: [], error: validation.errors.join('; '), count: 0 };
        }

        try {
            const evaluator = new XPathEvaluator();
            const result = evaluator.evaluate(
                xpath,
                this.currentXML,
                null,
                XPathResult.ORDERED_NODE_SNAPSHOT_TYPE,
                null
            );

            const matches: Node[] = [];
            for (let i = 0; i < result.snapshotLength; i++) {
                const node = result.snapshotItem(i);
                if (node) {
                    matches.push(node);
                }
            }

            return { matches, count: matches.length };
        } catch (error) {
            return { matches: [], error: (error as Error).message, count: 0 };
        }
    }

    /**
     * Highlight results in UI
     */
    highlightResults(result: XPathResult): void {
        if (!this.resultsPanel) return;

        this.resultsPanel.innerHTML = '';

        if (result.error) {
            const errorDiv = document.createElement('div');
            errorDiv.className = 'xpath-error';
            errorDiv.textContent = '❌ ' + result.error;
            errorDiv.style.cssText = 'color: var(--magenta); font-size: 12px;';
            this.resultsPanel.appendChild(errorDiv);
            return;
        }

        // Match count
        const countDiv = document.createElement('div');
        countDiv.className = 'match-count';
        countDiv.textContent = `✓ ${result.count} match${result.count !== 1 ? 'es' : ''} found`;
        countDiv.style.cssText = `
            font-size: 13px;
            font-weight: 600;
            color: var(--lime);
            margin-bottom: 12px;
        `;
        this.resultsPanel.appendChild(countDiv);

        if (result.matches.length === 0) {
            const noMatches = document.createElement('div');
            noMatches.className = 'no-matches';
            noMatches.textContent = 'No elements matched the XPath expression.';
            noMatches.style.cssText = 'color: var(--gray-600); font-size: 12px; font-style: italic;';
            this.resultsPanel.appendChild(noMatches);
            return;
        }

        // Show matched elements
        const resultsList = document.createElement('div');
        resultsList.style.cssText = 'display: flex; flex-direction: column; gap: 8px;';

        result.matches.slice(0, 10).forEach((node, index) => {
            const item = document.createElement('div');
            item.style.cssText = `
                padding: 8px;
                background: white;
                border: 1px solid var(--gray-200);
                border-radius: 4px;
                font-family: monospace;
                font-size: 11px;
            `;

            const nodeInfo = this.formatNodeInfo(node, index + 1);
            item.innerHTML = nodeInfo;

            resultsList.appendChild(item);
        });

        if (result.matches.length > 10) {
            const more = document.createElement('div');
            more.textContent = `... and ${result.matches.length - 10} more`;
            more.style.cssText = 'color: var(--gray-600); font-size: 11px; font-style: italic;';
            resultsList.appendChild(more);
        }

        this.resultsPanel.appendChild(resultsList);

        // Highlight in XML
        this.highlightInXML(result.matches);
    }

    /**
     * Format node information
     */
    private formatNodeInfo(node: Node, index: number): string {
        if (node.nodeType === Node.ELEMENT_NODE) {
            const element = node as Element;
            const tag = element.tagName;
            const attrs = Array.from(element.attributes)
                .map(attr => `${attr.name}="${attr.value}"`)
                .join(' ');
            const text = element.textContent?.substring(0, 50) || '';

            return `<strong style="color: var(--purple);">${index}.</strong> &lt;${tag}${attrs ? ' ' + attrs : ''}&gt; ${text}${text.length >= 50 ? '...' : ''}`;
        } else if (node.nodeType === Node.ATTRIBUTE_NODE) {
            const attr = node as Attr;
            return `<strong style="color: var(--purple);">${index}.</strong> @${attr.name}="${attr.value}"`;
        } else {
            return `<strong style="color: var(--purple);">${index}.</strong> ${node.textContent?.substring(0, 50)}`;
        }
    }

    /**
     * Highlight matched elements in XML display
     */
    private highlightInXML(matches: Node[]): void {
        if (!this.xmlInput) return;

        // Create highlighted version
        const highlightedDiv = document.createElement('div');
        highlightedDiv.className = 'xml-highlighted';
        highlightedDiv.style.cssText = `
            margin-top: 12px;
            padding: 10px;
            background: white;
            border: 1px solid var(--lime);
            border-radius: 6px;
            font-family: monospace;
            font-size: 11px;
            max-height: 200px;
            overflow-y: auto;
        `;

        const title = document.createElement('div');
        title.textContent = 'Highlighted XML:';
        title.style.cssText = 'font-weight: 600; margin-bottom: 8px; color: var(--text-primary);';
        highlightedDiv.appendChild(title);

        let xmlText = this.xmlInput.value;

        // Simple highlighting by wrapping matched text
        matches.forEach(node => {
            if (node.nodeType === Node.ELEMENT_NODE) {
                const element = node as Element;
                const tagName = element.tagName;
                const pattern = new RegExp(`<${tagName}[^>]*>.*?</${tagName}>`, 'g');
                xmlText = xmlText.replace(pattern, (match) => `<mark style="background: rgba(164, 198, 57, 0.3);">${match}</mark>`);
            }
        });

        const pre = document.createElement('pre');
        pre.innerHTML = xmlText;
        pre.style.cssText = 'margin: 0; white-space: pre-wrap; word-wrap: break-word;';
        highlightedDiv.appendChild(pre);

        this.resultsPanel?.appendChild(highlightedDiv);
    }

    /**
     * Show error message
     */
    private showError(message: string): void {
        if (!this.resultsPanel) return;

        this.resultsPanel.innerHTML = '';
        const errorDiv = document.createElement('div');
        errorDiv.textContent = '❌ ' + message;
        errorDiv.style.cssText = 'color: var(--magenta); font-size: 12px;';
        this.resultsPanel.appendChild(errorDiv);
    }

    /**
     * Get matched elements
     */
    getMatchedElements(xpath: string): Element[] {
        const result = this.executeXPath(xpath);
        return result.matches.filter(node => node.nodeType === Node.ELEMENT_NODE) as Element[];
    }

    /**
     * Format XPath error message
     */
    formatXPathError(error: Error): string {
        let message = error.message;

        if (message.includes('bracket')) {
            message += '\n\nTip: Make sure all brackets [ ] are properly closed.';
        } else if (message.includes('parenthes')) {
            message += '\n\nTip: Check that all parentheses ( ) are balanced.';
        }

        return message;
    }

    /**
     * Get XPath suggestions
     */
    getXPathSuggestions(): XPathSuggestion[] {
        return [
            { pattern: '//element', description: 'Select all <element> nodes', example: '//div' },
            { pattern: '/root/element', description: 'Select direct child', example: '/html/body' },
            { pattern: '//element[@attr]', description: 'Elements with attribute', example: '//div[@class]' },
            { pattern: '//element[@attr="value"]', description: 'Attribute equals value', example: '//div[@id="main"]' },
            { pattern: '//element[position()]', description: 'Element at position', example: '//p[1]' },
            { pattern: '//element/text()', description: 'Text content', example: '//title/text()' },
            { pattern: '//element/@attr', description: 'Attribute value', example: '//a/@href' },
            { pattern: '//element[contains(@attr, "text")]', description: 'Attribute contains text', example: '//div[contains(@class, "btn")]' }
        ];
    }

    /**
     * Get XPath library categorized
     */
    getXPathLibrary(): XPathLibrary {
        return {
            basic: [
                { pattern: '//', description: 'Select anywhere', example: '//div' },
                { pattern: '/', description: 'Select from root', example: '/html' },
                { pattern: '.', description: 'Current node', example: './child' },
                { pattern: '..', description: 'Parent node', example: '../sibling' }
            ],
            attributes: [
                { pattern: '@attr', description: 'Select attribute', example: '@id' },
                { pattern: '[@attr]', description: 'Has attribute', example: '//div[@class]' },
                { pattern: '[@attr="value"]', description: 'Attribute equals', example: '[@id="main"]' }
            ],
            text: [
                { pattern: 'text()', description: 'Text content', example: '//p/text()' },
                { pattern: 'contains()', description: 'Text contains', example: 'contains(text(), "word")' },
                { pattern: 'normalize-space()', description: 'Trim whitespace', example: 'normalize-space(text())' }
            ],
            advanced: [
                { pattern: 'position()', description: 'Element position', example: '//p[position()=1]' },
                { pattern: 'last()', description: 'Last element', example: '//p[last()]' },
                { pattern: 'ancestor::', description: 'Ancestor axis', example: 'ancestor::div' },
                { pattern: 'following-sibling::', description: 'Next siblings', example: 'following-sibling::p' }
            ]
        };
    }

    /**
     * Highlight XPath syntax
     */
    highlightXPathSyntax(xpath: string): string {
        let highlighted = xpath;

        // Highlight axes
        highlighted = highlighted.replace(/(ancestor|descendant|following|preceding|child|parent)::/g,
            '<span style="color: var(--purple); font-weight: 600;">$1::</span>');

        // Highlight predicates
        highlighted = highlighted.replace(/(\[[^\]]+\])/g,
            '<span style="color: var(--lime);">$1</span>');

        // Highlight attributes
        highlighted = highlighted.replace(/(@\w+)/g,
            '<span style="color: var(--magenta);">$1</span>');

        return highlighted;
    }

    /**
     * Register onChange callback
     */
    onChange(callback: (xpath: string) => void): void {
        this.onChangeCallback = callback;
    }

    /**
     * Register validation callback
     */
    onValidationChange(callback: (isValid: boolean) => void): void {
        this.onValidationCallback = callback;
    }

    /**
     * Get current XPath value
     */
    getValue(): string {
        return this.xpathInput?.value || '';
    }

    /**
     * Clear tester
     */
    clear(): void {
        if (this.xpathInput) this.xpathInput.value = '';
        if (this.xmlInput) this.xmlInput.value = '';
        if (this.resultsPanel) this.resultsPanel.innerHTML = '';
        this.currentXML = null;
    }
}


