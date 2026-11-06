export type VisualContentPart =
    | { kind: 'text'; value: string }
    | { kind: 'child'; nodeId: string };

export interface VisualNode {
    id: string;
    type: string;
    attributes: Record<string, string>;
    body: string | null;
    children: VisualNode[];
    /** Ordered mixed text/child content, present when body and children are interleaved. */
    contentSequence?: VisualContentPart[];
    position?: { x: number; y: number };
    dimensions?: { width: number; height: number };
    isManualPositioned?: boolean;
}
