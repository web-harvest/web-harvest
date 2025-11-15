import { describe, it, expect, beforeAll, beforeEach } from '@jest/globals';
import { NodeResizeHandler } from '../NodeResizeHandler';
import type { VisualNode } from '../types';

const createRect = (x: number, y: number, width: number, height: number) => ({
    x,
    y,
    width,
    height,
    top: y,
    left: x,
    right: x + width,
    bottom: y + height,
    toJSON: () => ({ x, y, width, height, top: y, left: x, right: x + width, bottom: y + height })
});

describe('NodeResizeHandler', () => {
    let handler: NodeResizeHandler;
    const mockNode: VisualNode = {
        id: 'node-1',
        type: 'constant',
        attributes: {},
        children: [],
        body: null
    };

    beforeAll(() => {
        class MockResizeObserver {
            callback: ResizeObserverCallback;
            constructor(callback: ResizeObserverCallback) {
                this.callback = callback;
            }
            observe() {
                // no-op
            }
            disconnect() {
                // no-op
            }
        }
        (global as unknown as { ResizeObserver: typeof ResizeObserver }).ResizeObserver = MockResizeObserver as unknown as typeof ResizeObserver;
    });

    beforeEach(() => {
        handler = new NodeResizeHandler();
        document.body.innerHTML = '';
    });

    it('aligns handle container to host element when no surface is present', () => {
        const host = document.createElement('div');
        document.body.appendChild(host);

        Object.defineProperty(host, 'getBoundingClientRect', {
            value: () => createRect(0, 0, 240, 120)
        });

        handler.addResizeHandles(host, mockNode);
        const container = host.querySelector('.resize-handles-container') as HTMLElement | null;
        expect(container).not.toBeNull();
        expect(container!.style.top).toBe('0px');
        expect(container!.style.left).toBe('0px');
        expect(container!.style.width).toBe('240px');
        expect(container!.style.height).toBe('120px');
    });

    it('aligns handle container to nested surface offsets', () => {
        const host = document.createElement('div');
        const surface = document.createElement('div');
        surface.className = 'visual-node__surface';
        host.appendChild(surface);
        document.body.appendChild(host);

        Object.defineProperty(host, 'getBoundingClientRect', {
            value: () => createRect(50, 25, 320, 200)
        });

        Object.defineProperty(surface, 'getBoundingClientRect', {
            value: () => createRect(70, 55, 260, 140)
        });

        handler.addResizeHandles(host, mockNode);
        const container = host.querySelector('.resize-handles-container') as HTMLElement | null;
        expect(container).not.toBeNull();
        expect(container!.style.top).toBe('30px'); // 55 - 25
        expect(container!.style.left).toBe('20px'); // 70 - 50
        expect(container!.style.width).toBe('260px');
        expect(container!.style.height).toBe('140px');
    });

    it('aligns to explicit handle anchor inside the surface with rounded bounds', () => {
        const host = document.createElement('div');
        const surface = document.createElement('div');
        surface.className = 'visual-node__surface';
        host.appendChild(surface);

        const anchor = document.createElement('div');
        anchor.dataset.handleAnchor = 'true';
        surface.appendChild(anchor);

        document.body.appendChild(host);

        Object.defineProperty(host, 'getBoundingClientRect', {
            value: () => createRect(10, 10, 400, 250)
        });

        Object.defineProperty(anchor, 'getBoundingClientRect', {
            value: () => createRect(42.4, 38.6, 312.2, 180.8)
        });

        handler.addResizeHandles(host, mockNode);
        const container = host.querySelector('.resize-handles-container') as HTMLElement | null;
        expect(container).not.toBeNull();
        expect(container!.style.top).toBe('29px');
        expect(container!.style.left).toBe('32px');
        expect(container!.style.width).toBe('312px');
        expect(container!.style.height).toBe('181px');
    });
});

