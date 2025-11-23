/**
 * Value formatting utilities for property editor
 * Handles formatting and parsing of different data types based on attribute metadata
 */

export interface FormatOptions {
    type?: 'string' | 'number' | 'boolean' | 'url' | 'xpath' | 'enum' | 'file_path' | 'regex' | 'integer' | 'long' | 'double';
    format?: string; // Format string from AttributeUiMetadata
    minValue?: number;
    maxValue?: number;
    allowedValues?: string[];
}

/**
 * Format a value for display based on type and format options
 */
export function formatValueForDisplay(value: string | null | undefined, options: FormatOptions): string {
    if (value === null || value === undefined || value === '') {
        return '';
    }

    const trimmed = value.trim();
    if (!trimmed) {
        return '';
    }

    // Handle format string if provided
    if (options.format) {
        return applyFormatString(trimmed, options.format, options.type);
    }

    // Type-based formatting
    switch (options.type) {
        case 'number':
            return formatNumber(trimmed);
        case 'url':
            return formatUrl(trimmed);
        case 'file_path':
            return formatFilePath(trimmed);
        case 'regex':
            return formatRegex(trimmed);
        case 'boolean':
            return trimmed === 'true' ? 'Yes' : trimmed === 'false' ? 'No' : trimmed;
        case 'enum':
            return formatEnum(trimmed, options.allowedValues);
        case 'xpath':
            return formatXPath(trimmed);
        default:
            return trimmed;
    }
}

/**
 * Parse a formatted value back to raw string
 */
export function parseFormattedValue(formatted: string, options: FormatOptions): string {
    if (!formatted || !formatted.trim()) {
        return '';
    }

    // Remove formatting characters based on type
    switch (options.type) {
        case 'number':
            // Remove thousand separators, keep decimal point
            return formatted.replace(/[^\d.-]/g, '');
        case 'url':
        case 'file_path':
        case 'regex':
        case 'xpath':
            return formatted.trim();
        default:
            return formatted.trim();
    }
}

/**
 * Validate and format value as user types (for real-time feedback)
 */
export function formatValueOnInput(value: string, options: FormatOptions): string {
    if (!value) {
        return '';
    }

    switch (options.type) {
        case 'number':
            // Allow partial input (e.g., "12." while typing)
            const numMatch = value.match(/^-?\d*\.?\d*$/);
            if (numMatch) {
                return value;
            }
            // If invalid, return previous valid state
            return value.slice(0, -1);
        case 'url':
            // Allow partial URLs
            return value;
        default:
            return value;
    }
}

/**
 * Apply format string to value
 */
function applyFormatString(value: string, format: string, type?: string): string {
    // Common format patterns
    switch (format.toLowerCase()) {
        case 'json':
            return formatJson(value);
        case 'xml':
            return formatXml(value);
        case 'regex':
            return formatRegex(value);
        case 'url':
            return formatUrl(value);
        case 'number':
        case 'integer':
        case 'decimal':
            return formatNumber(value);
        case 'percentage':
            return formatPercentage(value);
        case 'currency':
            return formatCurrency(value);
        case 'date':
        case 'datetime':
            return formatDate(value, format);
        default:
            // Try to parse as pattern (e.g., "###-###-####" for phone)
            if (format.includes('#')) {
                return applyPatternFormat(value, format);
            }
            return value;
    }
}

/**
 * Format number with thousand separators
 */
function formatNumber(value: string): string {
    const num = parseFloat(value);
    if (isNaN(num)) {
        return value;
    }

    // Check if it's an integer
    if (Number.isInteger(num)) {
        return num.toLocaleString('en-US');
    }

    // Format decimal with appropriate precision
    const parts = value.split('.');
    const integerPart = parts[0] ? parseInt(parts[0], 10).toLocaleString('en-US') : '0';
    const decimalPart = parts[1] || '';

    return decimalPart ? `${integerPart}.${decimalPart}` : integerPart;
}

/**
 * Format URL with validation indicator
 */
function formatUrl(value: string): string {
    // Basic URL validation
    try {
        new URL(value);
        return value;
    } catch {
        // Invalid URL, return as-is but could add visual indicator
        return value;
    }
}

/**
 * Format file path
 */
function formatFilePath(value: string): string {
    // Normalize path separators for display
    return value.replace(/\\/g, '/');
}

/**
 * Format regex pattern
 */
function formatRegex(value: string): string {
    // Escape special characters for display if needed
    return value;
}

/**
 * Format enum value
 */
function formatEnum(value: string, allowedValues?: string[]): string {
    if (allowedValues && allowedValues.includes(value)) {
        return value;
    }
    return value;
}

/**
 * Format XPath expression
 */
function formatXPath(value: string): string {
    // Could add syntax highlighting or formatting
    return value;
}

/**
 * Format JSON with indentation
 */
function formatJson(value: string): string {
    try {
        const parsed = JSON.parse(value);
        return JSON.stringify(parsed, null, 2);
    } catch {
        return value;
    }
}

/**
 * Format XML with indentation
 */
function formatXml(value: string): string {
    // Basic XML formatting
    return value;
}

/**
 * Format percentage
 */
function formatPercentage(value: string): string {
    const num = parseFloat(value);
    if (isNaN(num)) {
        return value;
    }
    return `${num.toFixed(2)}%`;
}

/**
 * Format currency
 */
function formatCurrency(value: string): string {
    const num = parseFloat(value);
    if (isNaN(num)) {
        return value;
    }
    return new Intl.NumberFormat('en-US', {
        style: 'currency',
        currency: 'USD'
    }).format(num);
}

/**
 * Format date/datetime
 */
function formatDate(value: string, format: string): string {
    try {
        const date = new Date(value);
        if (isNaN(date.getTime())) {
            return value;
        }

        if (format.toLowerCase() === 'date') {
            return date.toLocaleDateString('en-US');
        } else {
            return date.toLocaleString('en-US');
        }
    } catch {
        return value;
    }
}

/**
 * Apply pattern format (e.g., "###-###-####")
 */
function applyPatternFormat(value: string, pattern: string): string {
    let result = '';
    let valueIndex = 0;

    for (let i = 0; i < pattern.length && valueIndex < value.length; i++) {
        if (pattern[i] === '#') {
            result += value[valueIndex++];
        } else {
            result += pattern[i];
        }
    }

    return result;
}

/**
 * Check if value matches expected format
 */
export function validateFormat(value: string, options: FormatOptions): { valid: boolean; message?: string } {
    if (!value || !value.trim()) {
        if (options.type === 'number' && options.minValue !== undefined) {
            return { valid: false, message: 'This field is required' };
        }
        return { valid: true };
    }

    switch (options.type) {
        case 'number': {
            const num = parseFloat(value);
            if (isNaN(num)) {
                return { valid: false, message: 'Enter a valid number' };
            }
            if (options.minValue !== undefined && num < options.minValue) {
                return { valid: false, message: `Minimum value is ${options.minValue}` };
            }
            if (options.maxValue !== undefined && num > options.maxValue) {
                return { valid: false, message: `Maximum value is ${options.maxValue}` };
            }
            return { valid: true };
        }
        case 'url': {
            try {
                new URL(value);
                return { valid: true };
            } catch {
                return { valid: false, message: 'Enter a valid URL' };
            }
        }
        case 'regex': {
            try {
                new RegExp(value);
                return { valid: true };
            } catch {
                return { valid: false, message: 'Enter a valid regular expression' };
            }
        }
        case 'enum': {
            if (options.allowedValues && !options.allowedValues.includes(value)) {
                return { valid: false, message: `Must be one of: ${options.allowedValues.join(', ')}` };
            }
            return { valid: true };
        }
        default:
            return { valid: true };
    }
}

