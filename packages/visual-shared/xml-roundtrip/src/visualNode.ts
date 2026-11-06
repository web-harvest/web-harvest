export interface VisualNode {
    id: string;
    type: string;
    attributes: Record<string, string>;
    body: string | null;
    children: VisualNode[];
    position?: { x: number; y: number };
    dimensions?: { width: number; height: number };
    isManualPositioned?: boolean;
}
