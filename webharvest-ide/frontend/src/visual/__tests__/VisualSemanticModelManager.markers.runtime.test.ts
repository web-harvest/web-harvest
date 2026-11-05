/**
 * Testy TDD dla VisualSemanticModelManager - edge cases i problemy z runtime
 * 
 * TDD Cycle: Red → Green → Refactor
 */

import { VisualSemanticModelManager } from '../VisualSemanticModelManager';
import { VisualDesigner } from '../VisualDesigner';
import { PluginManager } from '../../core/PluginManager';
import type { Point } from '../VisualSemanticModel';

describe('VisualSemanticModelManager - Markers Runtime Edge Cases (TDD)', () => {
    let manager: VisualSemanticModelManager;
    let mockDesigner: Partial<VisualDesigner>;
    let pluginManager: PluginManager;

    beforeEach(() => {
        pluginManager = new PluginManager();
    });

    describe('RED: Testy które powinny nie przejść (problemy z runtime)', () => {
        it('should handle undefined startMarkerPosition gracefully', () => {
            // RED: Test który sprawdza problem z runtime
            mockDesigner = {
                startMarkerPosition: undefined,
                endMarkerPosition: { x: 170, y: 1300 }
            } as any;

            manager = new VisualSemanticModelManager(mockDesigner as VisualDesigner, pluginManager);

            const point = manager.getConnectionPointAbsolutePosition('workflow-start', 'marker-output');
            
            // Powinno zwrócić null, nie rzucić błędu
            expect(point).toBeNull();
        });

        it('should handle null designer gracefully', () => {
            // RED: Test dla przypadku gdy designer jest null
            mockDesigner = null as any;

            // Konstruktor może nie rzucić błędu, ale metoda powinna obsłużyć null
            manager = new VisualSemanticModelManager(mockDesigner as VisualDesigner, pluginManager);
            
            // Metoda powinna zwrócić null gdy designer jest null
            const point = manager.getConnectionPointAbsolutePosition('workflow-start', 'marker-output');
            expect(point).toBeNull();
        });

        it('should handle marker positions set after manager creation', () => {
            // RED: Test dla przypadku gdy pozycje markerów są ustawiane po utworzeniu managera
            mockDesigner = {
                startMarkerPosition: undefined,
                endMarkerPosition: undefined
            } as any;

            manager = new VisualSemanticModelManager(mockDesigner as VisualDesigner, pluginManager);

            // Początkowo powinno zwrócić null
            let point = manager.getConnectionPointAbsolutePosition('workflow-start', 'marker-output');
            expect(point).toBeNull();

            // Ustawiamy pozycję po utworzeniu managera
            (mockDesigner as any).startMarkerPosition = { x: 170, y: 80 };

            // Po ustawieniu powinno zwrócić poprawną pozycję
            point = manager.getConnectionPointAbsolutePosition('workflow-start', 'marker-output');
            expect(point).not.toBeNull();
            expect(point!.x).toBe(170);
            expect(point!.y).toBe(110); // 80 + 30
        });

        it('should handle pointId variations correctly', () => {
            // RED: Test dla różnych wariantów pointId
            mockDesigner = {
                startMarkerPosition: { x: 170, y: 80 },
                endMarkerPosition: { x: 170, y: 1300 }
            } as any;

            manager = new VisualSemanticModelManager(mockDesigner as VisualDesigner, pluginManager);

            // Test różnych wariantów pointId
            const variants = [
                'marker-output',
                'output-default',
                'marker-input',
                'input-default',
                'unknown-point',
                '',
                'output',
                'input'
            ];

            variants.forEach(pointId => {
                const point = manager.getConnectionPointAbsolutePosition('workflow-start', pointId);
                
                if (pointId.includes('output') || pointId === '' || pointId === 'unknown-point') {
                    // Powinno zwrócić bottom center (default dla start)
                    expect(point).not.toBeNull();
                    expect(point!.x).toBe(170);
                    expect(point!.y).toBe(110);
                } else if (pointId.includes('input')) {
                    // Powinno zwrócić top center
                    expect(point).not.toBeNull();
                    expect(point!.x).toBe(170);
                    expect(point!.y).toBe(50);
                }
            });
        });

        it('should handle concurrent calls safely', () => {
            // RED: Test dla równoczesnych wywołań
            mockDesigner = {
                startMarkerPosition: { x: 170, y: 80 },
                endMarkerPosition: { x: 170, y: 1300 }
            } as any;

            manager = new VisualSemanticModelManager(mockDesigner as VisualDesigner, pluginManager);

            // Równoczesne wywołania nie powinny powodować błędów
            const results = Promise.all([
                Promise.resolve(manager.getConnectionPointAbsolutePosition('workflow-start', 'marker-output')),
                Promise.resolve(manager.getConnectionPointAbsolutePosition('workflow-end', 'marker-input')),
                Promise.resolve(manager.getConnectionPointAbsolutePosition('workflow-start', 'marker-input')),
                Promise.resolve(manager.getConnectionPointAbsolutePosition('workflow-end', 'marker-output'))
            ]);

            return results.then(points => {
                expect(points[0]).not.toBeNull();
                expect(points[1]).not.toBeNull();
                expect(points[2]).not.toBeNull();
                expect(points[3]).not.toBeNull();
            });
        });
    });

    describe('GREEN: Testy które powinny przejść (poprawne zachowanie)', () => {
        beforeEach(() => {
            mockDesigner = {
                startMarkerPosition: { x: 170, y: 80 },
                endMarkerPosition: { x: 170, y: 1300 }
            } as any;

            manager = new VisualSemanticModelManager(mockDesigner as VisualDesigner, pluginManager);
        });

        it('should return correct position when marker positions are set', () => {
            const point = manager.getConnectionPointAbsolutePosition('workflow-start', 'marker-output');
            
            expect(point).not.toBeNull();
            expect(point!.x).toBe(170);
            expect(point!.y).toBe(110);
        });

        it('should handle position updates correctly', () => {
            let point = manager.getConnectionPointAbsolutePosition('workflow-start', 'marker-output');
            expect(point!.y).toBe(110);

            // Aktualizujemy pozycję
            (mockDesigner as any).startMarkerPosition = { x: 200, y: 100 };

            point = manager.getConnectionPointAbsolutePosition('workflow-start', 'marker-output');
            expect(point!.x).toBe(200);
            expect(point!.y).toBe(130); // 100 + 30
        });
    });

    describe('REFACTOR: Testy dla ulepszonej implementacji', () => {
        it('should use consistent MARKER_RADIUS constant', () => {
            // REFACTOR: Sprawdzamy czy MARKER_RADIUS jest używany konsekwentnie
            mockDesigner = {
                startMarkerPosition: { x: 170, y: 80 },
                endMarkerPosition: { x: 170, y: 1300 }
            } as any;

            manager = new VisualSemanticModelManager(mockDesigner as VisualDesigner, pluginManager);

            const MARKER_RADIUS = 30;
            const startOutput = manager.getConnectionPointAbsolutePosition('workflow-start', 'marker-output');
            const startInput = manager.getConnectionPointAbsolutePosition('workflow-start', 'marker-input');

            expect(startOutput!.y - mockDesigner.startMarkerPosition!.y).toBe(MARKER_RADIUS);
            expect(mockDesigner.startMarkerPosition!.y - startInput!.y).toBe(MARKER_RADIUS);
        });

        it('should handle edge case: marker at origin (0,0)', () => {
            // REFACTOR: Test dla edge case
            mockDesigner = {
                startMarkerPosition: { x: 0, y: 0 },
                endMarkerPosition: { x: 0, y: 0 }
            } as any;

            manager = new VisualSemanticModelManager(mockDesigner as VisualDesigner, pluginManager);

            const point = manager.getConnectionPointAbsolutePosition('workflow-start', 'marker-output');
            
            expect(point).not.toBeNull();
            expect(point!.x).toBe(0);
            expect(point!.y).toBe(30); // 0 + 30
        });

        it('should handle edge case: negative marker positions', () => {
            // REFACTOR: Test dla negatywnych pozycji
            mockDesigner = {
                startMarkerPosition: { x: -100, y: -50 },
                endMarkerPosition: { x: -100, y: -50 }
            } as any;

            manager = new VisualSemanticModelManager(mockDesigner as VisualDesigner, pluginManager);

            const point = manager.getConnectionPointAbsolutePosition('workflow-start', 'marker-output');
            
            expect(point).not.toBeNull();
            expect(point!.x).toBe(-100);
            expect(point!.y).toBe(-20); // -50 + 30
        });
    });
});

