import type { VisualNode } from '../types';
import { isContainerNode } from './VisualModelHelpers';
import type { MasterDetailRegistry } from './detail/MasterDetailRegistry';

export interface HierarchyStats {
    totalNodes: number;
    containerCount: number;
    leafCount: number;
}

export interface ContainerSummary {
    childCount: number;
    containerCount: number;
    leafCount: number;
    detailCount: number;
    typeFrequency: Array<{ type: string; count: number }>;
    attributeHighlights: Array<{ key: string; value: string }>;
    detailBreakdown: Array<{ title: string; count: number; preview: string[] }>;
}

export function analyzeHierarchy(nodes: VisualNode[]): HierarchyStats {
    const totals: HierarchyStats = {
        totalNodes: 0,
        containerCount: 0,
        leafCount: 0
    };

    const visit = (node: VisualNode): void => {
        totals.totalNodes += 1;
        const hasChildren = Array.isArray(node.children) && node.children.length > 0;
        if (isContainerNode(node.type) || hasChildren) {
            totals.containerCount += 1;
        } else {
            totals.leafCount += 1;
        }
        (node.children || []).forEach(visit);
    };

    nodes.forEach(visit);
    totals.leafCount = Math.max(totals.leafCount, 0);
    return totals;
}

export function analyzeContainer(
    node: VisualNode,
    detailRegistry?: MasterDetailRegistry | null
): ContainerSummary {
    const children = node.children || [];
    const detailIds = new Set<string>();
    const detailBreakdown: Array<{ title: string; count: number; preview: string[] }> = [];

    if (detailRegistry) {
        const partition = detailRegistry.partitionChildren(node);
        const sections = detailRegistry.getSections(node.type);
        Object.values(partition.sections).forEach(sectionNodes => {
            sectionNodes.forEach(detailNode => detailIds.add(detailNode.id));
        });

        sections.forEach(section => {
            const sectionDetails = partition.sections[section.detailType] || [];
            if (sectionDetails.length === 0) {
                return;
            }

            const preview = sectionDetails.slice(0, 2).map(detailNode => {
                const summary = detailRegistry.summarizeDetail(detailNode);
                if (summary.secondaryText) {
                    return `${summary.primaryText} → ${summary.secondaryText}`;
                }
                return summary.primaryText;
            });

            detailBreakdown.push({
                title: section.sectionTitle,
                count: sectionDetails.length,
                preview
            });
        });
    }

    let containerCount = 0;
    let leafCount = 0;

    const typeFrequencyMap = new Map<string, number>();

    children.forEach(child => {
        const isDetailChild = detailIds.has(child.id);
        if (!isDetailChild) {
            if (isContainerNode(child.type) || (child.children && child.children.length > 0)) {
                containerCount += 1;
            } else {
                leafCount += 1;
            }
        }
        typeFrequencyMap.set(child.type, (typeFrequencyMap.get(child.type) ?? 0) + 1);
    });

    const typeFrequency = Array.from(typeFrequencyMap.entries())
        .map(([type, count]) => ({ type, count }))
        .sort((a, b) => b.count - a.count);

    const attributeHighlights = extractAttributeHighlights(node);

    return {
        childCount: children.length,
        containerCount,
        leafCount,
        detailCount: detailIds.size,
        typeFrequency,
        attributeHighlights,
        detailBreakdown
    };
}

function extractAttributeHighlights(node: VisualNode): Array<{ key: string; value: string }> {
    const preferredKeys = [
        'name',
        'var',
        'condition',
        'maxloops',
        'method',
        'url',
        'expression',
        'id'
    ];

    const highlights: Array<{ key: string; value: string }> = [];

    preferredKeys.forEach(key => {
        const value = node.attributes?.[key];
        if (value && value.trim().length > 0) {
            highlights.push({ key, value: value.trim() });
        }
    });

    if (highlights.length < 2) {
        Object.keys(node.attributes || {})
            .filter(key => !preferredKeys.includes(key))
            .some(key => {
                const value = node.attributes?.[key];
                if (value && value.trim().length > 0) {
                    highlights.push({ key, value: value.trim() });
                    return highlights.length >= 2;
                }
                return false;
            });
    }

    return highlights.slice(0, 2);
}


