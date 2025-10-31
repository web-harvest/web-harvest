/**
 * CanvasSearch - Implementation
 * Search and filter functionality for workflow canvas
 */

import type { VisualNode } from '../types';

export interface SearchResult {
    node: VisualNode;
    matchType: 'type' | 'attribute' | 'variable' | 'body';
    matchedText: string;
    attributeName?: string;
}

export interface SearchSummary {
    total: number;
    byType: Record<string, number>;
    byCategory: Record<string, number>;
}

export class CanvasSearch {
    private currentResults: SearchResult[] = [];
    private currentMatchIndex: number = -1;
    private searchHistory: string[] = [];
    private readonly MAX_HISTORY = 10;
    private onNavigateCallback: ((result: SearchResult) => void) | null = null;

    /**
     * Search nodes by query
     */
    search(nodes: VisualNode[], query: string): SearchResult[] {
        if (!query || query.trim() === '') {
            return [];
        }

        const lowerQuery = query.toLowerCase();
        const results: SearchResult[] = [];

        // Add to history
        this.addToHistory(query);

        nodes.forEach(node => {
            // Search by type
            if (node.type.toLowerCase().includes(lowerQuery)) {
                results.push({
                    node,
                    matchType: 'type',
                    matchedText: node.type
                });
                return;
            }

            // Search by attributes
            for (const [attrName, attrValue] of Object.entries(node.attributes)) {
                if (attrValue.toLowerCase().includes(lowerQuery)) {
                    results.push({
                        node,
                        matchType: 'attribute',
                        matchedText: attrValue,
                        attributeName: attrName
                    });
                    return;
                }

                // Also search attribute names
                if (attrName.toLowerCase().includes(lowerQuery)) {
                    results.push({
                        node,
                        matchType: 'attribute',
                        matchedText: attrName,
                        attributeName: attrName
                    });
                    return;
                }
            }

            // Search by body content
            if (node.body && node.body.toLowerCase().includes(lowerQuery)) {
                results.push({
                    node,
                    matchType: 'body',
                    matchedText: node.body.substring(0, 50)
                });
                return;
            }

            // Search in children (recursive)
            if (node.children.length > 0) {
                const childResults = this.search(node.children, query);
                results.push(...childResults);
            }
        });

        console.log(`[CanvasSearch] Found ${results.length} matches for "${query}"`);

        return results;
    }

    /**
     * Search by plugin type only
     */
    searchByType(nodes: VisualNode[], type: string): VisualNode[] {
        const lowerType = type.toLowerCase();

        return nodes.filter(node => node.type.toLowerCase() === lowerType);
    }

    /**
     * Search by specific attribute
     */
    searchByAttribute(nodes: VisualNode[], attributeName: string, value: string): SearchResult[] {
        const lowerValue = value.toLowerCase();
        const results: SearchResult[] = [];

        nodes.forEach(node => {
            const attrValue = node.attributes[attributeName];
            if (attrValue && attrValue.toLowerCase().includes(lowerValue)) {
                results.push({
                    node,
                    matchType: 'attribute',
                    matchedText: attrValue,
                    attributeName
                });
            }
        });

        return results;
    }

    /**
     * Highlight matching nodes in canvas
     */
    highlightMatches(canvas: HTMLElement, results: SearchResult[]): void {
        // Clear previous highlights
        this.clearHighlights(canvas);

        // Highlight matched nodes
        results.forEach(result => {
            const nodeElement = canvas.querySelector(`[data-node-id="${result.node.id}"]`);
            if (nodeElement) {
                nodeElement.classList.add('search-match');
            }
        });

        console.log(`[CanvasSearch] Highlighted ${results.length} matches`);
    }

    /**
     * Clear all search highlights
     */
    clearHighlights(canvas: HTMLElement): void {
        const highlighted = canvas.querySelectorAll('.search-match');
        highlighted.forEach(element => {
            element.classList.remove('search-match');
            element.classList.remove('search-current');
        });
    }

    /**
     * Set search results
     */
    setResults(results: SearchResult[]): void {
        this.currentResults = results;
        this.currentMatchIndex = results.length > 0 ? 0 : -1;
    }

    /**
     * Get current match index
     */
    getCurrentMatch(): number {
        return this.currentMatchIndex;
    }

    /**
     * Set current match index
     */
    setCurrentMatch(index: number): void {
        if (index >= 0 && index < this.currentResults.length) {
            this.currentMatchIndex = index;
        }
    }

    /**
     * Navigate to next match
     */
    nextMatch(): void {
        if (this.currentResults.length === 0) return;

        this.currentMatchIndex = (this.currentMatchIndex + 1) % this.currentResults.length;

        if (this.onNavigateCallback) {
            this.onNavigateCallback(this.currentResults[this.currentMatchIndex]);
        }

        console.log(`[CanvasSearch] Next match: ${this.currentMatchIndex + 1}/${this.currentResults.length}`);
    }

    /**
     * Navigate to previous match
     */
    previousMatch(): void {
        if (this.currentResults.length === 0) return;

        this.currentMatchIndex--;
        if (this.currentMatchIndex < 0) {
            this.currentMatchIndex = this.currentResults.length - 1;
        }

        if (this.onNavigateCallback) {
            this.onNavigateCallback(this.currentResults[this.currentMatchIndex]);
        }

        console.log(`[CanvasSearch] Previous match: ${this.currentMatchIndex + 1}/${this.currentResults.length}`);
    }

    /**
     * Filter nodes (return only matching node IDs)
     */
    filter(nodes: VisualNode[], query: string): string[] {
        if (!query || query.trim() === '') {
            return nodes.map(n => n.id);
        }

        const results = this.search(nodes, query);
        return results.map(r => r.node.id);
    }

    /**
     * Get search results summary
     */
    getResultsSummary(results: SearchResult[]): SearchSummary {
        const summary: SearchSummary = {
            total: results.length,
            byType: {},
            byCategory: {}
        };

        results.forEach(result => {
            // Count by type
            const type = result.node.type;
            summary.byType[type] = (summary.byType[type] || 0) + 1;
        });

        return summary;
    }

    /**
     * Get search history
     */
    getSearchHistory(): string[] {
        return [...this.searchHistory];
    }

    /**
     * Add query to history
     */
    private addToHistory(query: string): void {
        // Remove if already exists
        const index = this.searchHistory.indexOf(query);
        if (index !== -1) {
            this.searchHistory.splice(index, 1);
        }

        // Add to front
        this.searchHistory.unshift(query);

        // Limit size
        if (this.searchHistory.length > this.MAX_HISTORY) {
            this.searchHistory = this.searchHistory.slice(0, this.MAX_HISTORY);
        }
    }

    /**
     * Clear search history
     */
    clearHistory(): void {
        this.searchHistory = [];
    }

    /**
     * Register navigation callback
     */
    onNavigate(callback: (result: SearchResult) => void): void {
        this.onNavigateCallback = callback;
    }

    /**
     * Get total results count
     */
    getResultsCount(): number {
        return this.currentResults.length;
    }

    /**
     * Check if has results
     */
    hasResults(): boolean {
        return this.currentResults.length > 0;
    }

    /**
     * Get current result
     */
    getCurrentResult(): SearchResult | null {
        if (this.currentMatchIndex >= 0 && this.currentMatchIndex < this.currentResults.length) {
            return this.currentResults[this.currentMatchIndex];
        }
        return null;
    }
}


