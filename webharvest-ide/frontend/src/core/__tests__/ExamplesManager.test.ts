/**
 * ExamplesManager - TDD Tests
 */

import { describe, it, expect, beforeEach } from '@jest/globals';
import { ExamplesManager } from '../ExamplesManager';

// Mock fetch
global.fetch = jest.fn((url) => {
    if (url === '/examples/test.xml') {
        return Promise.resolve({
            ok: true,
            text: () => Promise.resolve('<config><var name="test">value</var></config>'),
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

            // Should not fetch again
            expect(manager.getExamples()).toHaveLength(6);
        });
    });

    describe('loadExampleContent', () => {
        it('should load example XML content', async () => {
            const content = await manager.loadExampleContent('test.xml');

            expect(content).toContain('<config>');
            expect(content).toContain('<var');
            expect(fetch).toHaveBeenCalledWith('/examples/test.xml');
        });
    });

    describe('findExample', () => {
        it('should find example by filename', async () => {
            await manager.loadExamples();

            const example = manager.findExample('simple_example.xml');
            expect(example?.name).toBe('Simple Example');
        });
    });
});


