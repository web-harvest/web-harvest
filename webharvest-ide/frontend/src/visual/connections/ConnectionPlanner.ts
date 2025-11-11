import type { VisualNode } from '../../types';
import type { Connection } from '../VisualModelHelpers';

export function planConnections(nodes: VisualNode[], startNodeId: string, endNodeId: string): Connection[] {
    if (!nodes.length) {
        return [];
    }

    const connections: Connection[] = [];

    const nodesWithPosition = nodes.filter(node => node.position);
    const ordered = nodesWithPosition.length > 0
        ? [...nodesWithPosition].sort((a, b) => {
            const ax = a.position?.x ?? 0;
            const bx = b.position?.x ?? 0;
            if (ax !== bx) {
                return ax - bx;
            }
            const ay = a.position?.y ?? 0;
            const by = b.position?.y ?? 0;
            return ay - by;
        })
        : [...nodes];

    const first = ordered[0];
    const last = ordered[ordered.length - 1];

    connections.push({ from: startNodeId, to: first.id, type: 'sequential' });

    if (first.id !== last.id) {
        connections.push({ from: last.id, to: endNodeId, type: 'sequential' });
    } else {
        connections.push({ from: first.id, to: endNodeId, type: 'sequential' });
    }

    return connections;
}
