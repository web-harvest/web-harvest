/**
 * Utility helpers for sanitising XML strings prior to validation/execution.
 */

/**
 * Removes BOM/leading whitespace before the XML declaration so that parsers
 * never see stray characters in the prolog (which would trigger
 * "Content is not allowed in prolog" errors).
 */
export function sanitizeXmlProlog(xml: string): string {
    if (!xml) {
        return '';
    }

    let sanitized = xml;

    // Strip UTF-8 BOM if present
    if (sanitized.charCodeAt(0) === 0xFEFF) {
        sanitized = sanitized.slice(1);
    }

    // If XML declaration exists later in the string, trim everything before it
    const xmlDeclIndex = sanitized.indexOf('<?xml');
    if (xmlDeclIndex > 0) {
        sanitized = sanitized.slice(xmlDeclIndex);
    } else if (xmlDeclIndex === -1) {
        // No XML declaration – still trim leading whitespace to avoid parser noise
        sanitized = sanitized.replace(/^[\u0000-\u001F\s]+/u, '');
    }

    // Ensure there isn't any whitespace preceding the declaration
    sanitized = sanitized.replace(/^[\u0000-\u001F\s]+(?=<\?xml)/u, '');

    return sanitized;
}

/**
 * Convenience helper that trims leading whitespace and BOM characters while
 * preserving indentation for the rest of the document.
 */
export function trimLeadingNoise(xml: string): string {
    if (!xml) {
        return '';
    }
    return sanitizeXmlProlog(xml);
}

/**
 * Removes Visual Designer metadata attributes (e.g. data-width / data-height)
 * so generated XML stays schema-compliant when sent to the backend.
 */
export function stripVisualDesignerMetadata(xml: string): string {
    if (!xml) {
        return '';
    }

    return xml.replace(/\sdata-(width|height)="[^"]*"/g, '');
}

/**
 * Convenience helper used before sending XML to the backend. Strips Visual
 * metadata attributes and sanitises the XML prolog in a single step.
 */
export function prepareXmlForBackend(xml: string): string {
    return sanitizeXmlProlog(stripVisualDesignerMetadata(xml));
}

