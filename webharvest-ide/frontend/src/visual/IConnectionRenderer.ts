/**
 * IConnectionRenderer - Wspólny interfejs dla rendererów połączeń
 */

import type { VisualConnectionState } from './VisualSemanticModel';

export interface IConnectionRenderer {
    svgElement: SVGSVGElement;
    renderAll(modelManager: any): void;
    renderConnection(conn: VisualConnectionState): void;
    clearAll(): void;
    updateSvgElement(newSvgElement: SVGSVGElement): void;
}




