import { describe, it, expect, beforeEach, afterEach, jest } from '@jest/globals';
import { VisualDesigner } from '../VisualDesigner';
import type { VisualNode } from '../../types';

describe('VisualDesigner layout controls', () => {
    let container: HTMLElement;
    let designer: VisualDesigner;

    beforeEach(() => {
        container = document.createElement('div');
        container.id = 'layout-canvas';
        document.body.appendChild(container);
        designer = new VisualDesigner('layout-canvas');
    });

    afterEach(() => {
        document.body.innerHTML = '';
    });

    it('resets manual root nodes back into auto layout flow', () => {
        const nodes: VisualNode[] = [
            {
                id: 'root-1',
                type: 'var',
                attributes: {},
                body: null,
                children: [],
                position: { x: 400, y: 120 },
                isManualPositioned: true
            },
            {
                id: 'root-2',
                type: 'def',
                attributes: {},
                body: null,
                children: []
            }
        ];

        designer.loadNodes(nodes);

        const before = designer.getNodes();
        expect(before[0].position?.x).toBe(400);
        expect(before[1].position?.x).toBeGreaterThan(800);

        const updateSpy = jest.fn();
        designer.addEventListener('nodeUpdated', updateSpy);

        const resetResult = designer.resetNodePosition('root-1');
        expect(resetResult).toBe(true);
        expect(updateSpy).toHaveBeenCalled();

        const after = designer.getNodes();
        expect(after[0].isManualPositioned).toBe(false);
        expect(after[0].position?.x).toBe(50);
        expect(after[0].position?.y).toBe(50);

        const minExpected = (after[0].position?.x || 0) + 250 + 180; // DEFAULT_NODE_WIDTH + HORIZONTAL_GAP
        expect(after[1].position?.x).toBeGreaterThanOrEqual(minExpected);
    });

    it('returns false when attempting to reset a missing node', () => {
        const resetResult = designer.resetNodePosition('missing');
        expect(resetResult).toBe(false);
    });

    it('expands canvas stage to accommodate distant nodes', () => {
        const nodes: VisualNode[] = [
            {
                id: 'root-1',
                type: 'var',
                attributes: {},
                body: null,
                children: [],
                position: { x: 1600, y: 900 }
            }
        ];

        designer.loadNodes(nodes);

        const stage = container.querySelector('.canvas-stage') as HTMLElement | null;
        expect(stage).not.toBeNull();

        const width = parseFloat(stage!.style.width);
        const height = parseFloat(stage!.style.height);

        expect(width).toBeGreaterThanOrEqual(1850);
        expect(height).toBeGreaterThanOrEqual(1040);
    });
});

