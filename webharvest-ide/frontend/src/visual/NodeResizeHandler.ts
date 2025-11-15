import type { VisualNode } from '../types';

type ResizeHandlePosition = 'n' | 'ne' | 'e' | 'se' | 's' | 'sw' | 'w' | 'nw';

interface Dimensions {
    width: number;
    height: number;
}

interface ResizeConstraints {
    minWidth?: number;
    maxWidth?: number;
    minHeight?: number;
    maxHeight?: number;
}

export class NodeResizeHandler {
    private handleContainers: Map<string, HTMLElement> = new Map();
    private resizeObservers: Map<string, ResizeObserver> = new Map();
    private constraints: Map<string, ResizeConstraints> = new Map();
    private onResizeCallback: ((nodeId: string, dimensions: Dimensions) => void) | null = null;

    setOnResize(callback: (nodeId: string, dimensions: Dimensions) => void): void {
        this.onResizeCallback = callback;
    }

    setConstraints(nodeId: string, constraints: ResizeConstraints): void {
        this.constraints.set(nodeId, constraints);
    }

    addResizeHandles(nodeElement: HTMLElement, node: VisualNode): void {
        this.removeResizeHandles(node.id);

        const anchor = this.findAnchorElement(nodeElement);
        const container = this.createHandleContainer(nodeElement, anchor, node.id);
        const positions: ResizeHandlePosition[] = ['n', 'ne', 'e', 'se', 's', 'sw', 'w', 'nw'];

        positions.forEach(position => {
            const handle = this.createHandle(position, node.id);
            this.attachHandleEvents(handle, position, nodeElement, node.id);
            container.appendChild(handle);
        });

        nodeElement.appendChild(container);
        this.handleContainers.set(node.id, container);
        this.observeForSizeChanges(node.id, nodeElement, anchor, container);
    }

    showHandlesForNodes(nodeIds: string[]): void {
        this.handleContainers.forEach((container, nodeId) => {
            container.style.display = nodeIds.includes(nodeId) ? 'block' : 'none';
        });
    }

    hideAllHandles(): void {
        this.handleContainers.forEach(container => {
            container.style.display = 'none';
        });
    }

    removeResizeHandles(nodeId: string): void {
        const container = this.handleContainers.get(nodeId);
        if (container) {
            container.remove();
            this.handleContainers.delete(nodeId);
        }

        const observer = this.resizeObservers.get(nodeId);
        if (observer) {
            observer.disconnect();
            this.resizeObservers.delete(nodeId);
        }
    }

    removeAllHandles(): void {
        this.handleContainers.forEach(container => container.remove());
        this.handleContainers.clear();
        this.resizeObservers.forEach(observer => observer.disconnect());
        this.resizeObservers.clear();
    }

    private findAnchorElement(nodeElement: HTMLElement): HTMLElement {
        const explicitAnchor = nodeElement.querySelector('[data-handle-anchor="true"]') as HTMLElement | null;
        if (explicitAnchor) {
            return explicitAnchor;
        }
        return (nodeElement.querySelector('.visual-node__surface') as HTMLElement | null) || nodeElement;
    }

    private createHandleContainer(host: HTMLElement, anchor: HTMLElement, nodeId: string): HTMLElement {
        const container = document.createElement('div');
        container.className = 'resize-handles-container';
        container.dataset.nodeId = nodeId;
        container.style.display = 'none';
        this.updateHandleContainerBounds(host, anchor, container);
        return container;
    }

    private createHandle(position: ResizeHandlePosition, nodeId: string): HTMLElement {
        const handle = document.createElement('button');
        handle.type = 'button';
        handle.className = `resize-handle resize-handle-${position}`;
        handle.dataset.position = position;
        handle.dataset.nodeId = nodeId;
        handle.setAttribute('aria-label', `Resize ${position}`);
        return handle;
    }

    private updateHandleContainerBounds(host: HTMLElement, anchor: HTMLElement, container: HTMLElement): void {
        const hostRect = host.getBoundingClientRect();
        const anchorRect = anchor.getBoundingClientRect();

        const topOffset = Math.round(anchorRect.top - hostRect.top);
        const leftOffset = Math.round(anchorRect.left - hostRect.left);
        const width = Math.round(anchorRect.width);
        const height = Math.round(anchorRect.height);

        container.style.top = `${topOffset}px`;
        container.style.left = `${leftOffset}px`;
        container.style.width = `${width}px`;
        container.style.height = `${height}px`;
    }

    private observeForSizeChanges(nodeId: string, host: HTMLElement, anchor: HTMLElement, container: HTMLElement): void {
        if (typeof ResizeObserver === 'undefined') {
            return;
        }

        const observer = new ResizeObserver(() => {
            this.updateHandleContainerBounds(host, anchor, container);
        });

        observer.observe(anchor);
        observer.observe(host);
        this.resizeObservers.set(nodeId, observer);
    }

    private attachHandleEvents(
        handle: HTMLElement,
        position: ResizeHandlePosition,
        host: HTMLElement,
        nodeId: string
    ): void {
        handle.addEventListener('mousedown', (event) => {
            event.preventDefault();
            event.stopPropagation();
            this.startResize(event, position, host, nodeId);
        });
    }

    private startResize(
        startEvent: MouseEvent,
        position: ResizeHandlePosition,
        host: HTMLElement,
        nodeId: string
    ): void {
        const startX = startEvent.clientX;
        const startY = startEvent.clientY;
        const hostRect = host.getBoundingClientRect();
        const computedStyle = window.getComputedStyle(host);
        const isAbsolute = computedStyle.position === 'absolute';

        const initialWidth = hostRect.width;
        const initialHeight = hostRect.height;
        const initialLeft = isAbsolute ? this.parseNumeric(host.style.left, host.offsetLeft) : host.offsetLeft;
        const initialTop = isAbsolute ? this.parseNumeric(host.style.top, host.offsetTop) : host.offsetTop;
        const rightEdge = initialLeft + initialWidth;
        const bottomEdge = initialTop + initialHeight;

        const constraints = this.constraints.get(nodeId) || {};
        const minWidth = constraints.minWidth ?? 140;
        const maxWidth = constraints.maxWidth ?? 800;
        const minHeight = constraints.minHeight ?? 90;
        const maxHeight = constraints.maxHeight ?? 640;

        const onMouseMove = (moveEvent: MouseEvent) => {
            moveEvent.preventDefault();
            const deltaX = moveEvent.clientX - startX;
            const deltaY = moveEvent.clientY - startY;

            let newWidth = initialWidth;
            let newHeight = initialHeight;

            if (position.includes('e')) {
                newWidth = initialWidth + deltaX;
            }
            if (position.includes('w')) {
                newWidth = initialWidth - deltaX;
            }
            if (position.includes('s')) {
                newHeight = initialHeight + deltaY;
            }
            if (position.includes('n')) {
                newHeight = initialHeight - deltaY;
            }

            newWidth = Math.min(Math.max(newWidth, minWidth), maxWidth);
            newHeight = Math.min(Math.max(newHeight, minHeight), maxHeight);

            let newLeft = initialLeft;
            let newTop = initialTop;

            if (position.includes('w') && isAbsolute) {
                newLeft = rightEdge - newWidth;
            }
            if (position.includes('n') && isAbsolute) {
                newTop = bottomEdge - newHeight;
            }

            host.style.width = `${newWidth}px`;
            host.style.height = `${newHeight}px`;

            if (isAbsolute) {
                host.style.left = `${newLeft}px`;
                host.style.top = `${newTop}px`;
            }
        };

        const onMouseUp = () => {
            document.removeEventListener('mousemove', onMouseMove);
            document.removeEventListener('mouseup', onMouseUp);
            document.body.style.userSelect = '';

            if (this.onResizeCallback) {
                const rect = host.getBoundingClientRect();
                this.onResizeCallback(nodeId, {
                    width: rect.width,
                    height: rect.height
                });
            }
        };

        document.addEventListener('mousemove', onMouseMove);
        document.addEventListener('mouseup', onMouseUp);
        document.body.style.userSelect = 'none';
    }

    private parseNumeric(value: string, fallback: number): number {
        const parsed = parseFloat(value);
        return Number.isFinite(parsed) ? parsed : fallback;
    }
}

