/**
 * CanvasSearch - TDD Tests
 * Tests for canvas search and filter functionality
 */

import { describe, it, expect, beforeEach } from '@jest/globals';
import { CanvasSearch, SearchResult } from '../CanvasSearch';
import type { VisualNode } from '../../types';

describe('CanvasSearch', () => {
    let search: CanvasSearch;
    let mockNodes: VisualNode[];

    beforeEach(() => {
        search = new CanvasSearch();

        mockNodes = [
            {
                id: 'node-1',
                type: 'http',
                attributes: { url: 'http://api.example.com' },
                body: null,
                children: []
            },
            {
                id: 'node-2',
                type: 'xpath',
                attributes: { expression: '//title' },
                body: null,
                children: []
            },
            {
                id: 'node-3',
                type: 'def',
                attributes: { var: 'pageTitle' },
                body: null,
                children: []
            },
            {
                id: 'node-4',
                type: 'http',
                attributes: { url: 'http://example.org' },
                body: null,
                children: []
            }
        ];
    });

    describe('search', () => {
        it('should find nodes by plugin type', () => {
            const results = search.search(mockNodes, 'http');

            expect(results.length).toBe(2);
            expect(results.every(r => r.node.type === 'http')).toBe(true);
        });

        it('should find nodes by attribute value', () => {
            const results = search.search(mockNodes, 'example.com');

            expect(results.length).toBeGreaterThan(0);
            expect(results[0].node.attributes.url).toContain('example.com');
        });

        it('should find nodes by variable name', () => {
            const results = search.search(mockNodes, 'pageTitle');

            expect(results.length).toBe(1);
            expect(results[0].node.attributes.var).toBe('pageTitle');
        });

        it('should be case-insensitive', () => {
            const results = search.search(mockNodes, 'HTTP');

            expect(results.length).toBe(2);
        });

        it('should return empty array for no matches', () => {
            const results = search.search(mockNodes, 'nonexistent');

            expect(results.length).toBe(0);
        });

        it('should return match context', () => {
            const results = search.search(mockNodes, 'example.com');

            expect(results[0].matchType).toBeDefined();
            expect(results[0].matchedText).toBe('example.com');
        });
    });

    describe('searchByType', () => {
        it('should filter nodes by type', () => {
            const results = search.searchByType(mockNodes, 'http');

            expect(results.length).toBe(2);
        });

        it('should return empty for non-existent type', () => {
            const results = search.searchByType(mockNodes, 'nonexistent');

            expect(results.length).toBe(0);
        });
    });

    describe('searchByAttribute', () => {
        it('should search attribute values', () => {
            const results = search.searchByAttribute(mockNodes, 'url', 'example.com');

            expect(results.length).toBeGreaterThan(0);
        });

        it('should support partial matches', () => {
            const results = search.searchByAttribute(mockNodes, 'url', 'api');

            expect(results.length).toBeGreaterThan(0);
            expect(results[0].node.attributes.url).toContain('api');
        });
    });

    describe('highlightMatches', () => {
        it('should add highlight class to matched nodes', () => {
            const mockCanvas = document.createElement('div');

            mockNodes.forEach(node => {
                const el = document.createElement('div');
                el.dataset.nodeId = node.id;
                el.className = 'visual-node';
                mockCanvas.appendChild(el);
            });

            const results = search.search(mockNodes, 'http');
            search.highlightMatches(mockCanvas, results);

            const highlighted = mockCanvas.querySelectorAll('.search-match');
            expect(highlighted.length).toBe(2);
        });

        it('should remove previous highlights', () => {
            const mockCanvas = document.createElement('div');

            mockNodes.forEach(node => {
                const el = document.createElement('div');
                el.dataset.nodeId = node.id;
                el.className = 'visual-node search-match';
                mockCanvas.appendChild(el);
            });

            search.clearHighlights(mockCanvas);

            const highlighted = mockCanvas.querySelectorAll('.search-match');
            expect(highlighted.length).toBe(0);
        });
    });

    describe('getCurrentMatch', () => {
        it('should track current match index', () => {
            const results = search.search(mockNodes, 'http');
            search.setCurrentMatch(0);

            expect(search.getCurrentMatch()).toBe(0);
        });

        it('should wrap around at end', () => {
            const results = search.search(mockNodes, 'http');
            search.setResults(results);

            search.nextMatch();
            search.nextMatch();
            search.nextMatch(); // Should wrap to 0

            expect(search.getCurrentMatch()).toBe(0);
        });
    });

    describe('nextMatch', () => {
        it('should move to next match', () => {
            const results = search.search(mockNodes, 'http');
            search.setResults(results);

            search.nextMatch();

            expect(search.getCurrentMatch()).toBe(1);
        });

        it('should emit navigation event', (done) => {
            const results = search.search(mockNodes, 'http');
            search.setResults(results);

            search.onNavigate((result) => {
                expect(result.node.id).toBe('node-4');
                done();
            });

            search.nextMatch();
        });
    });

    describe('previousMatch', () => {
        it('should move to previous match', () => {
            const results = search.search(mockNodes, 'http');
            search.setResults(results);
            search.setCurrentMatch(1);

            search.previousMatch();

            expect(search.getCurrentMatch()).toBe(0);
        });

        it('should wrap to end from beginning', () => {
            const results = search.search(mockNodes, 'http');
            search.setResults(results);
            search.setCurrentMatch(0);

            search.previousMatch();

            expect(search.getCurrentMatch()).toBe(results.length - 1);
        });
    });

    describe('filter', () => {
        it('should return only matching node IDs', () => {
            const filtered = search.filter(mockNodes, 'http');

            expect(filtered.length).toBe(2);
            expect(filtered).toContain('node-1');
            expect(filtered).toContain('node-4');
        });

        it('should return all node IDs for empty query', () => {
            const filtered = search.filter(mockNodes, '');

            expect(filtered.length).toBe(mockNodes.length);
        });
    });

    describe('getResultsSummary', () => {
        it('should return search statistics', () => {
            const results = search.search(mockNodes, 'http');
            const summary = search.getResultsSummary(results);

            expect(summary.total).toBe(2);
            expect(summary.byType).toHaveProperty('http');
        });

        it('should count matches by type', () => {
            const results = search.search(mockNodes, 'example');
            const summary = search.getResultsSummary(results);

            expect(summary.byType['http']).toBeGreaterThan(0);
        });
    });

    describe('searchHistory', () => {
        it('should track search history', () => {
            search.search(mockNodes, 'http');
            search.search(mockNodes, 'xpath');

            const history = search.getSearchHistory();

            expect(history.length).toBe(2);
            expect(history).toContain('http');
            expect(history).toContain('xpath');
        });

        it('should limit history size', () => {
            for (let i = 0; i < 20; i++) {
                search.search(mockNodes, `query-${i}`);
            }

            const history = search.getSearchHistory();

            expect(history.length).toBeLessThanOrEqual(10);
        });
    });
});


