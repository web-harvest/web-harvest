/**
 * ExamplesManager - TDD Tests
 */

import { describe, it, expect, beforeEach, jest } from '@jest/globals';
import { ExamplesManager, DEFAULT_EXAMPLES } from '../ExamplesManager';

const RAW_XML = '\uFEFF  \n<?xml version="1.0"?><config><var name="test">value</var></config>';

// Mock fetch
global.fetch = jest.fn((url) => {
    if (url === '/examples/test.xml') {
        return Promise.resolve({
            ok: true,
            text: () => Promise.resolve(RAW_XML),
        });
    }
    return Promise.reject(new Error('Not found'));
}) as jest.Mock;

describe('ExamplesManager', () => {
    let manager: ExamplesManager;

    beforeEach(() => {
        manager = new ExamplesManager();
        (fetch as jest.Mock).mockClear();
    });

    describe('loadExamples', () => {
        it('should return list of examples', async () => {
            const examples = await manager.loadExamples();

            expect(examples.length).toBeGreaterThan(0);
            expect(examples[0].file).toBeDefined();
            expect(examples[0].name).toBeDefined();
        });

        it('should cache examples after first load', async () => {
            await manager.loadExamples();
            await manager.loadExamples();

            // Should not fetch again and should expose full catalog
            expect(manager.getExamples()).toHaveLength(DEFAULT_EXAMPLES.length);
        });
    });

    describe('loadExampleContent', () => {
        it('should load example XML content', async () => {
            const content = await manager.loadExampleContent('test.xml');

            expect(content.startsWith('<?xml')).toBe(true);
            expect(content).toContain('<var');
            expect(fetch).toHaveBeenCalledWith('/examples/test.xml');
        });
    });

    describe('findExample', () => {
        it('should find example by filename', async () => {
            await manager.loadExamples();

            const example = manager.findExample('advanced_search_example.xml');
            expect(example?.name).toBe('Advanced Search');
        });
    });
});





