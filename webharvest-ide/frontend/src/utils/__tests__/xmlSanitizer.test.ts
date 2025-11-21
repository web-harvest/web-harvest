import { describe, expect, it } from '@jest/globals';
import { prepareXmlForBackend, sanitizeXmlProlog, stripVisualDesignerMetadata } from '../xmlSanitizer';

describe('xmlSanitizer', () => {
    it('removes BOM and whitespace before XML declaration', () => {
        const dirty = '\uFEFF   \n\t<?xml version="1.0"?><config></config>';
        const cleaned = sanitizeXmlProlog(dirty);
        expect(cleaned.startsWith('<?xml')).toBe(true);
        expect(cleaned).toContain('<config></config>');
    });

    it('trims leading whitespace when XML declaration is missing', () => {
        const dirty = '\n\n<config></config>';
        const cleaned = sanitizeXmlProlog(dirty);
        expect(cleaned.startsWith('<config')).toBe(true);
    });

    it('strips visual designer metadata attributes', () => {
        const xml = '<log dest="out" data-width="320" data-height="200"/>';
        const cleaned = stripVisualDesignerMetadata(xml);
        expect(cleaned).toBe('<log dest="out"/>');
    });

    it('prepares XML for backend by stripping metadata and sanitising prolog', () => {
        const xml = '\uFEFF  <?xml version="1.0"?><log dest="out" data-width="200"/>';
        const cleaned = prepareXmlForBackend(xml);
        expect(cleaned.startsWith('<?xml')).toBe(true);
        expect(cleaned.includes('data-width')).toBe(false);
    });
});

