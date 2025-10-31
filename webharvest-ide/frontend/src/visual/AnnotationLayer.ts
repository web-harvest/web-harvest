/**
 * AnnotationLayer - Implementation
 * Freeform annotations (text, boxes, arrows, sticky notes)
 */

export interface Annotation {
    id: string;
    type: 'text' | 'box' | 'arrow' | 'note';
    position: { x: number; y: number };
    content: string;
    style?: {
        color?: string;
        background?: string;
        width?: number;
        height?: number;
    };
}

export class AnnotationLayer {
    private annotations: Map<string, Annotation> = new Map();

    addAnnotation(annotation: Omit<Annotation, 'id'>): Annotation {
        const fullAnnotation: Annotation = {
            ...annotation,
            id: `annotation-${Date.now()}-${Math.random().toString(36).substr(2, 9)}`
        };

        this.annotations.set(fullAnnotation.id, fullAnnotation);
        return fullAnnotation;
    }

    removeAnnotation(id: string): boolean {
        return this.annotations.delete(id);
    }

    getAnnotations(): Annotation[] {
        return Array.from(this.annotations.values());
    }

    updateAnnotation(id: string, updates: Partial<Annotation>): boolean {
        const annotation = this.annotations.get(id);
        if (!annotation) return false;

        this.annotations.set(id, { ...annotation, ...updates });
        return true;
    }

    clear(): void {
        this.annotations.clear();
    }
}


