/**
 * ContextMenu - Right-click context menu for visual nodes
 */

import type { VisualNode } from '../types';

export interface ContextMenuAction {
    id: string;
    label: string;
    icon?: string;
    disabled?: boolean;
    onClick: () => void;
}

export interface ContextMenuOptions {
    actions: ContextMenuAction[];
    x: number;
    y: number;
    onClose?: () => void;
}

export class ContextMenu {
    private menuElement: HTMLElement | null = null;
    private closeHandler: ((event: MouseEvent) => void) | null = null;
    private options: ContextMenuOptions | null = null;

    /**
     * Show context menu at specified position
     */
    show(options: ContextMenuOptions): void {
        this.hide(); // Hide any existing menu

        this.options = options;
        this.menuElement = document.createElement('div');
        this.menuElement.className = 'context-menu';
        this.menuElement.style.cssText = `
            position: fixed;
            left: ${options.x}px;
            top: ${options.y}px;
            background: white;
            border: 1px solid #e0e0e0;
            border-radius: 8px;
            box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
            z-index: 10000;
            min-width: 180px;
            padding: 4px;
            font-size: 13px;
        `;

        options.actions.forEach(action => {
            const item = document.createElement('div');
            item.className = 'context-menu-item';
            item.dataset.actionId = action.id;
            item.style.cssText = `
                display: flex;
                align-items: center;
                gap: 8px;
                padding: 8px 12px;
                cursor: ${action.disabled ? 'not-allowed' : 'pointer'};
                color: ${action.disabled ? '#999' : '#333'};
                opacity: ${action.disabled ? 0.5 : 1};
                border-radius: 4px;
                transition: background 0.15s;
            `;

            if (action.icon) {
                const icon = document.createElement('i');
                icon.className = `fas ${action.icon}`;
                icon.style.width = '16px';
                icon.style.textAlign = 'center';
                item.appendChild(icon);
            }

            const label = document.createElement('span');
            label.textContent = action.label;
            item.appendChild(label);

            if (!action.disabled) {
                item.addEventListener('click', (e) => {
                    e.stopPropagation();
                    action.onClick();
                    this.hide();
                });

                item.addEventListener('mouseenter', () => {
                    item.style.background = '#f5f5f5';
                });

                item.addEventListener('mouseleave', () => {
                    item.style.background = 'transparent';
                });
            }

            this.menuElement!.appendChild(item);
        });

        document.body.appendChild(this.menuElement);

        // Close on outside click
        this.closeHandler = (event: MouseEvent) => {
            if (this.menuElement && !this.menuElement.contains(event.target as Node)) {
                this.hide();
            }
        };

        // Use setTimeout to avoid immediate close on the click that opened the menu
        setTimeout(() => {
            document.addEventListener('click', this.closeHandler!);
            document.addEventListener('contextmenu', this.closeHandler!);
        }, 0);

        // Close on Escape key
        const escapeHandler = (event: KeyboardEvent) => {
            if (event.key === 'Escape') {
                this.hide();
            }
        };
        document.addEventListener('keydown', escapeHandler);
        this.options.onClose = () => {
            document.removeEventListener('keydown', escapeHandler);
        };
    }

    /**
     * Hide context menu
     */
    hide(): void {
        if (this.closeHandler) {
            document.removeEventListener('click', this.closeHandler);
            document.removeEventListener('contextmenu', this.closeHandler);
            this.closeHandler = null;
        }

        if (this.menuElement) {
            this.menuElement.remove();
            this.menuElement = null;
        }

        if (this.options?.onClose) {
            this.options.onClose();
            this.options = null;
        }
    }

    /**
     * Check if menu is visible
     */
    isVisible(): boolean {
        return this.menuElement !== null;
    }
}


