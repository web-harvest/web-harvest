/**
 * AutoLayoutEngine - TDD Tests
 * Tests for automatic sequential flow with manual override
 */

import { describe, it, expect, beforeEach } from '@jest/globals';
import { AutoLayoutEngine } from '../AutoLayoutEngine';
import type { VisualNode } from '../../types';

describe('AutoLayoutEngine', () => {
    let engine: AutoLayoutEngine;
    let mockNodes: VisualNode[];

    beforeEach(() => {
        engine = new AutoLayoutEngine();
        
        mockNodes = [
            { id: 'node-1', type: 'http', attributes: {}, body: null, children: [] },
            { id: 'node-2', type: 'xpath', attributes: {}, body: null, children: [] },
            { id: 'node-3', type: 'def', attributes: {}, body: null, children: [] }
        ];
    });

    describe('autoConnect', () => {
        it('should create sequential connections', () => {
            const connections = engine.autoConnect(mockNodes);

            expect(connections.length).toBe(2);
            expect(connections[0].from).toBe('node-1');
            expect(connections[0].to).toBe('node-2');
        });

        it('should handle single node', () => {
            const connections = engine.autoConnect([mockNodes[0]]);
            expect(connections.length).toBe(0);
        });
    });

    describe('convertToManual', () => {
        it('should preserve existing connections', () => {
            engine.autoConnect(mockNodes);
            const manual = engine.convertToManual();

            expect(manual.length).toBe(2);
            expect(manual[0].type).toBe('sequential');
        });
    });

    describe('isAutoMode', () => {
        it('should track auto mode state', () => {
            expect(engine.isAutoMode()).toBe(true);
            
            engine.convertToManual();
            expect(engine.isAutoMode()).toBe(false);
        });
    });
});


