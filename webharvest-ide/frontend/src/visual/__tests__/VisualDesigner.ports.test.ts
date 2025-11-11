/**
 * VisualDesigner port & connection behaviour (sanity coverage)
 */

import { describe, it, expect } from '@jest/globals';
import { VisualDesigner } from '../VisualDesigner';
import type { VisualNode } from '../../types';

const resetDom = () => {
    document.body.innerHTML = '<div id="canvas-area" style="width:800px;height:600px"></div>';
};

const createDesigner = (): VisualDesigner => {
    resetDom();
    return new VisualDesigner('canvas-area');
};

const createNode = (id: string, type: string, x = 100, y = 100): VisualNode => ({
    id,
    type,
    attributes: {},
    body: null,
    children: [],
    position: { x, y }
});

describe('VisualDesigner – ports', () => {
    it('renders input and output ports for a basic node', () => {
        const designer = createDesigner();
        designer.addNode(createNode('node-1', 'def'));
        designer.render();

        const nodeElement = document.querySelector('[data-node-id="node-1"]') as HTMLElement;
        expect(nodeElement).toBeTruthy();
        expect(nodeElement.querySelector('.port-input')).toBeTruthy();
        expect(nodeElement.querySelector('.port-output')).toBeTruthy();
    });
});

describe('VisualDesigner – manual connections', () => {
    it('creates a manual connection via API helper', () => {
        const designer = createDesigner();
        designer.addNode(createNode('node-a', 'def'));
        designer.addNode(createNode('node-b', 'xpath', 300, 100));

        const connection = designer.createManualConnection('node-a', 'node-b');
        expect(connection.from).toBe('node-a');
        expect(connection.to).toBe('node-b');
        expect(designer.getManualConnections()).toHaveLength(1);
    });

    it('prevents cycles when creating manual connections', () => {
        const designer = createDesigner();
        designer.addNode(createNode('node-a', 'def'));
        designer.addNode(createNode('node-b', 'xpath', 300, 100));
        designer.createManualConnection('node-a', 'node-b');

        expect(() => designer.createManualConnection('node-b', 'node-a')).toThrow();
    });
});
