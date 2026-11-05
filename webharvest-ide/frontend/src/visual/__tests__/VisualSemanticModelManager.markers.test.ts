/**
 * Testy dla VisualSemanticModelManager - obsługa markerów workflow-start i workflow-end
 */

import { VisualSemanticModelManager } from '../VisualSemanticModelManager';
import { VisualDesigner } from '../VisualDesigner';
import { PluginManager } from '../../core/PluginManager';
import type { Point } from '../VisualSemanticModel';

describe('VisualSemanticModelManager - Markers', () => {
    let manager: VisualSemanticModelManager;
    let mockDesigner: Partial<VisualDesigner>;
    let pluginManager: PluginManager;

    beforeEach(() => {
        // Mock designer with marker positions
        mockDesigner = {
            startMarkerPosition: { x: 170, y: 80 },
            endMarkerPosition: { x: 170, y: 1300 }
        } as any;

        pluginManager = new PluginManager();
        manager = new VisualSemanticModelManager(mockDesigner as VisualDesigner, pluginManager);
    });

    describe('getConnectionPointAbsolutePosition for markers', () => {
        it('should return bottom center for workflow-start output', () => {
            const point = manager.getConnectionPointAbsolutePosition('workflow-start', 'marker-output');
            
            expect(point).not.toBeNull();
            expect(point!.x).toBe(170); // Same X as marker center
            expect(point!.y).toBe(110); // 80 + 30 (MARKER_RADIUS)
        });

        it('should return top center for workflow-start input', () => {
            const point = manager.getConnectionPointAbsolutePosition('workflow-start', 'marker-input');
            
            expect(point).not.toBeNull();
            expect(point!.x).toBe(170);
            expect(point!.y).toBe(50); // 80 - 30 (MARKER_RADIUS)
        });

        it('should return top center for workflow-end input', () => {
            const point = manager.getConnectionPointAbsolutePosition('workflow-end', 'marker-input');
            
            expect(point).not.toBeNull();
            expect(point!.x).toBe(170);
            expect(point!.y).toBe(1270); // 1300 - 30 (MARKER_RADIUS)
        });

        it('should return bottom center for workflow-end output', () => {
            const point = manager.getConnectionPointAbsolutePosition('workflow-end', 'marker-output');
            
            expect(point).not.toBeNull();
            expect(point!.x).toBe(170);
            expect(point!.y).toBe(1330); // 1300 + 30 (MARKER_RADIUS)
        });

        it('should handle default case for workflow-start', () => {
            const point = manager.getConnectionPointAbsolutePosition('workflow-start', 'unknown-point');
            
            expect(point).not.toBeNull();
            // Should default to bottom center (output)
            expect(point!.x).toBe(170);
            expect(point!.y).toBe(110);
        });

        it('should handle default case for workflow-end', () => {
            const point = manager.getConnectionPointAbsolutePosition('workflow-end', 'unknown-point');
            
            expect(point).not.toBeNull();
            // Should default to top center (input)
            expect(point!.x).toBe(170);
            expect(point!.y).toBe(1270);
        });

        it('should return null if marker position is not available', () => {
            (mockDesigner as any).startMarkerPosition = null;
            
            const point = manager.getConnectionPointAbsolutePosition('workflow-start', 'marker-output');
            
            expect(point).toBeNull();
        });
    });
});

