import type { VisualNode } from '../../types';

export type ControlDetailType = 'else' | 'catch';

export interface IfPartition {
    elseNode: VisualNode | null;
    primaryChildren: VisualNode[];
}

export interface TryPartition {
    catchNodes: VisualNode[];
    primaryChildren: VisualNode[];
}

export function createControlDetailNode(type: ControlDetailType): VisualNode {
    return {
        id: generateDetailId(type),
        type,
        attributes: {},
        body: null,
        children: []
    };
}

export function partitionIfChildren(node: VisualNode): IfPartition {
    const primaryChildren: VisualNode[] = [];
    let elseNode: VisualNode | null = null;

    (node.children || []).forEach(child => {
        if (child.type === 'else' && !elseNode) {
            elseNode = child;
        } else {
            primaryChildren.push(child);
        }
    });

    return { elseNode, primaryChildren };
}

export function partitionTryChildren(node: VisualNode): TryPartition {
    const catchNodes: VisualNode[] = [];
    const primaryChildren: VisualNode[] = [];

    (node.children || []).forEach(child => {
        if (child.type === 'catch') {
            catchNodes.push(child);
        } else {
            primaryChildren.push(child);
        }
    });

    return { catchNodes, primaryChildren };
}

function generateDetailId(prefix: string): string {
    if (typeof crypto !== 'undefined' && typeof crypto.randomUUID === 'function') {
        return `${prefix}-${crypto.randomUUID()}`;
    }
    return `${prefix}-${Date.now()}-${Math.random().toString(36).slice(2, 8)}`;
}
