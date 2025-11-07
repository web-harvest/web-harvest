import type { VisualNode } from '../../types';

export type HttpDetailType = 'http-header' | 'http-param';

export interface HttpNodePartitions {
    headers: VisualNode[];
    params: VisualNode[];
}

/**
 * Create a new http detail node (header or parameter) with default structure.
 */
export function createHttpDetailNode(type: HttpDetailType): VisualNode {
    return {
        id: generateHttpDetailId(type),
        type,
        attributes: {
            name: ''
        },
        body: '',
        children: []
    };
}

/**
 * Partition the children of an HTTP node into headers and params for easy rendering.
 */
export function partitionHttpChildren(node: VisualNode | null | undefined): HttpNodePartitions {
    const headers: VisualNode[] = [];
    const params: VisualNode[] = [];

    if (!node || !Array.isArray(node.children)) {
        return { headers, params };
    }

    node.children.forEach(child => {
        if (!child || typeof child.type !== 'string') {
            return;
        }

        const type = child.type.toLowerCase();
        if (type === 'http-header') {
            headers.push(child);
        } else if (type === 'http-param') {
            params.push(child);
        }
    });

    return { headers, params };
}

/**
 * Extract display-ready values for a http detail node.
 */
export function summarizeHttpDetail(detail: VisualNode): { name: string; value: string } {
    const name = (detail.attributes?.name || '').toString().trim();
    const value = (detail.body || '').toString().trim();
    return {
        name,
        value
    };
}

function generateHttpDetailId(prefix: string): string {
    if (typeof crypto !== 'undefined' && typeof crypto.randomUUID === 'function') {
        return `${prefix}-${crypto.randomUUID()}`;
    }

    return `${prefix}-${Date.now()}-${Math.random().toString(36).slice(2, 8)}`;
}
