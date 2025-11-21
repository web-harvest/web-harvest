import { describe, it, expect } from '@jest/globals';
import { XMLParser } from '../XMLParser';

describe('XMLParser legacy conversions', () => {
    it('converts legacy <body> elements', () => {
        const parser = new XMLParser();
        const xml = '<config><body><var name="x">1</var></body></config>';
        const nodes = parser.parse(xml);

        expect(nodes[0].type).toBe('fragment');
        expect(parser.getLegacyNotices()).toEqual(
            expect.arrayContaining([
                expect.objectContaining({
                    legacyTag: 'body',
                    replacement: 'fragment'
                })
            ])
        );
    });

    it('converts legacy search_results to template', () => {
        const parser = new XMLParser();
        const xml = '<config><search_results><search_result/></search_results></config>';
        const nodes = parser.parse(xml);

        expect(nodes[0].type).toBe('template');
        expect(nodes[0].children[0].type).toBe('template-entry');
        const notices = parser.getLegacyNotices();
        expect(notices.some(notice => notice.replacement === 'template')).toBe(true);
    });
});


