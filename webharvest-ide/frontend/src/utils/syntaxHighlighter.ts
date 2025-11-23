/**
 * Lightweight syntax highlighting utility for Property Editor
 * Provides syntax highlighting for JSON, CODE, and EXPRESSION field types
 */

export type SyntaxLanguage = 'json' | 'javascript' | 'xml' | 'xpath' | 'expression';

/**
 * Highlight syntax in a textarea element
 * Uses a lightweight approach with overlay div for highlighting
 */
export function highlightSyntax(
    textarea: HTMLTextAreaElement,
    language: SyntaxLanguage
): void {
    const wrapper = textarea.parentElement;
    if (!wrapper) {
        return;
    }

    // Check if already initialized
    if (wrapper.classList.contains('syntax-highlight-wrapper')) {
        updateSyntaxHighlight(textarea, language);
        return;
    }

    wrapper.classList.add('syntax-highlight-wrapper');
    wrapper.style.position = 'relative';

    const highlightDiv = document.createElement('div');
    highlightDiv.className = 'syntax-highlight-overlay';
    highlightDiv.setAttribute('aria-hidden', 'true');
    
    // Copy textarea styles to overlay
    const textareaStyle = window.getComputedStyle(textarea);
    highlightDiv.style.cssText = `
        position: absolute;
        top: 0;
        left: 0;
        width: 100%;
        height: 100%;
        padding: ${textareaStyle.padding};
        margin: 0;
        border: ${textareaStyle.border};
        border-radius: ${textareaStyle.borderRadius};
        font-family: ${textareaStyle.fontFamily};
        font-size: ${textareaStyle.fontSize};
        line-height: ${textareaStyle.lineHeight};
        white-space: pre-wrap;
        word-wrap: break-word;
        overflow: hidden;
        pointer-events: none;
        background: ${textareaStyle.background};
        z-index: 1;
    `;

    // Make textarea text transparent, keep caret visible
    const originalBackground = textareaStyle.background;
    textarea.style.cssText += `
        position: relative;
        z-index: 2;
        background: transparent !important;
        color: transparent;
        caret-color: var(--text-primary, #333);
    `;

    wrapper.insertBefore(highlightDiv, textarea);

    // Update highlight function
    const updateHighlight = () => {
        const text = textarea.value;
        highlightDiv.innerHTML = highlightText(text, language);
        
        // Sync scroll
        highlightDiv.scrollTop = textarea.scrollTop;
        highlightDiv.scrollLeft = textarea.scrollLeft;
    };

    // Store update function on textarea for external updates
    (textarea as any).__updateSyntaxHighlight = updateHighlight;

    // Update on input and scroll
    textarea.addEventListener('input', updateHighlight);
    textarea.addEventListener('scroll', () => {
        highlightDiv.scrollTop = textarea.scrollTop;
        highlightDiv.scrollLeft = textarea.scrollLeft;
    });

    // Initial highlight
    updateHighlight();
}

/**
 * Update syntax highlighting for an already initialized textarea
 */
function updateSyntaxHighlight(
    textarea: HTMLTextAreaElement,
    language: SyntaxLanguage
): void {
    const updateFn = (textarea as any).__updateSyntaxHighlight;
    if (updateFn) {
        updateFn();
    }
}

/**
 * Highlight text with syntax colors
 */
function highlightText(text: string, language: SyntaxLanguage): string {
    switch (language) {
        case 'json':
            return highlightJSON(text);
        case 'javascript':
        case 'expression':
            return highlightJavaScript(text);
        case 'xml':
            return highlightXML(text);
        case 'xpath':
            return highlightXPath(text);
        default:
            return escapeHtml(text);
    }
}

/**
 * Highlight JSON syntax
 */
function highlightJSON(text: string): string {
    try {
        // Try to parse and format
        const parsed = JSON.parse(text);
        const formatted = JSON.stringify(parsed, null, 2);
        return highlightJSONTokens(formatted);
    } catch {
        // If invalid, still try to highlight what we can
        return highlightJSONTokens(text);
    }
}

function highlightJSONTokens(text: string): string {
    return text
        .replace(/("(?:[^"\\]|\\.)*")\s*:/g, '<span class="json-key">$1</span>:')
        .replace(/:\s*("(?:[^"\\]|\\.)*")/g, ': <span class="json-string">$1</span>')
        .replace(/:\s*(\d+\.?\d*)/g, ': <span class="json-number">$1</span>')
        .replace(/:\s*(true|false|null)/g, ': <span class="json-boolean">$1</span>')
        .replace(/([{}[\],])/g, '<span class="json-punctuation">$1</span>');
}

/**
 * Highlight JavaScript/Expression syntax
 */
function highlightJavaScript(text: string): string {
    return text
        .replace(/\b(function|var|let|const|if|else|for|while|return|new|this|typeof|instanceof)\b/g, 
            '<span class="js-keyword">$1</span>')
        .replace(/\b(true|false|null|undefined)\b/g, 
            '<span class="js-literal">$1</span>')
        .replace(/\b(\d+\.?\d*)\b/g, 
            '<span class="js-number">$1</span>')
        .replace(/(["'])(?:(?=(\\?))\2.)*?\1/g, 
            '<span class="js-string">$&</span>')
        .replace(/\$\{([^}]+)\}/g, 
            '<span class="js-expression">${$1}</span>');
}

/**
 * Highlight XML syntax
 */
function highlightXML(text: string): string {
    return text
        .replace(/&lt;(\/?)([\w-]+)([^&]*?)&gt;/g, 
            (match, closing, tag, attrs) => {
                const tagClass = closing ? 'xml-tag-close' : 'xml-tag-open';
                return `&lt;${closing}<span class="${tagClass}">${tag}</span>${highlightXMLAttributes(attrs)}&gt;`;
            })
        .replace(/&lt;!--([\s\S]*?)--&gt;/g, 
            '<span class="xml-comment">&lt;!--$1--&gt;</span>');
}

function highlightXMLAttributes(attrs: string): string {
    return attrs
        .replace(/(\w+)=/g, '<span class="xml-attr-name">$1</span>=')
        .replace(/="([^"]*)"/g, '="<span class="xml-attr-value">$1</span>"');
}

/**
 * Highlight XPath syntax
 */
function highlightXPath(text: string): string {
    return text
        .replace(/\/(\/?)([\w-]+)/g, 
            '<span class="xpath-separator">/$1</span><span class="xpath-element">$2</span>')
        .replace(/\[([^\]]+)\]/g, 
            '<span class="xpath-predicate">[$1]</span>')
        .replace(/@(\w+)/g, 
            '<span class="xpath-attribute">@$1</span>')
        .replace(/(\d+)/g, 
            '<span class="xpath-number">$1</span>');
}

/**
 * Escape HTML special characters
 */
function escapeHtml(text: string): string {
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
}

/**
 * Remove syntax highlighting from a textarea
 */
export function removeSyntaxHighlighting(textarea: HTMLTextAreaElement): void {
    const wrapper = textarea.parentElement;
    if (!wrapper || !wrapper.classList.contains('syntax-highlight-wrapper')) {
        return;
    }

    const overlay = wrapper.querySelector('.syntax-highlight-overlay');
    if (overlay) overlay.remove();
    
    wrapper.classList.remove('syntax-highlight-wrapper');
    wrapper.style.position = '';
    
    // Restore textarea styles
    textarea.style.background = '';
    textarea.style.color = '';
    textarea.style.position = '';
    textarea.style.zIndex = '';
    textarea.style.caretColor = '';
    
    delete (textarea as any).__updateSyntaxHighlight;
}

