/**
 * BreadcrumbNav - Implementation
 * Breadcrumb navigation for drilling into nested containers
 */

export interface BreadcrumbItem {
    id: string;
    label: string;
    level: number;
    nodeType?: string;
}

export class BreadcrumbNav {
    private container: HTMLElement | null = null;
    private trail: BreadcrumbItem[] = [];
    private onNavigateCallback: ((item: BreadcrumbItem) => void) | null = null;

    /**
     * Render breadcrumb trail
     */
    render(container: HTMLElement, trail: BreadcrumbItem[]): void {
        this.container = container;
        this.trail = [...trail];

        container.innerHTML = '';
        container.className = 'breadcrumb-nav';
        container.style.cssText = `
            display: flex;
            align-items: center;
            gap: 8px;
            padding: 8px 12px;
            background: var(--gray-50);
            border-bottom: 1px solid var(--gray-200);
            font-size: 13px;
        `;

        trail.forEach((item, index) => {
            // Breadcrumb item
            const itemElement = document.createElement('div');
            itemElement.className = 'breadcrumb-item';
            itemElement.dataset.itemId = item.id;
            itemElement.textContent = item.label;
            itemElement.style.cssText = `
                padding: 4px 8px;
                border-radius: 4px;
                cursor: pointer;
                transition: all 0.2s;
                color: var(--gray-700);
            `;

            // Highlight current (last) level
            if (index === trail.length - 1) {
                itemElement.classList.add('breadcrumb-current');
                itemElement.style.background = 'var(--purple)';
                itemElement.style.color = 'white';
                itemElement.style.fontWeight = '600';
                itemElement.style.cursor = 'default';
            } else {
                // Clickable for navigation
                itemElement.addEventListener('mouseenter', () => {
                    itemElement.style.background = 'var(--gray-100)';
                });

                itemElement.addEventListener('mouseleave', () => {
                    itemElement.style.background = '';
                });

                itemElement.addEventListener('click', () => {
                    this.navigateTo(item.id);
                });
            }

            container.appendChild(itemElement);

            // Separator (except after last item)
            if (index < trail.length - 1) {
                const separator = document.createElement('span');
                separator.className = 'breadcrumb-separator';
                separator.textContent = '>';
                separator.style.cssText = 'color: var(--gray-400); font-size: 12px;';
                container.appendChild(separator);
            }
        });
    }

    /**
     * Navigate to specific breadcrumb level
     */
    navigateTo(itemId: string): void {
        const item = this.trail.find(i => i.id === itemId);

        if (!item) {
            console.warn(`[BreadcrumbNav] Item not found: ${itemId}`);
            return;
        }

        // Trim trail to this level
        this.trail = this.trail.slice(0, item.level + 1);

        if (this.onNavigateCallback) {
            this.onNavigateCallback(item);
        }

        // Re-render
        if (this.container) {
            this.render(this.container, this.trail);
        }

        console.log(`[BreadcrumbNav] Navigated to ${item.label} (level ${item.level})`);
    }

    /**
     * Get current navigation level
     */
    getCurrentLevel(): number {
        if (this.trail.length === 0) return 0;
        return this.trail[this.trail.length - 1].level;
    }

    /**
     * Push new level to breadcrumb
     */
    push(item: BreadcrumbItem): void {
        this.trail.push(item);

        if (this.container) {
            this.render(this.container, this.trail);
        }

        console.log(`[BreadcrumbNav] Pushed ${item.label} (level ${item.level})`);
    }

    /**
     * Pop last level from breadcrumb
     */
    pop(): BreadcrumbItem | null {
        if (this.trail.length <= 1) {
            return null; // Don't pop root
        }

        const popped = this.trail.pop();

        if (this.container) {
            this.render(this.container, this.trail);
        }

        console.log(`[BreadcrumbNav] Popped ${popped?.label}`);

        return popped || null;
    }

    /**
     * Reset to root level
     */
    reset(): void {
        if (this.trail.length > 0) {
            this.trail = [this.trail[0]]; // Keep only root
        }

        if (this.container) {
            this.render(this.container, this.trail);
        }

        console.log('[BreadcrumbNav] Reset to root');
    }

    /**
     * Get current trail
     */
    getTrail(): BreadcrumbItem[] {
        return [...this.trail];
    }

    /**
     * Handle keyboard navigation
     */
    handleKeyboard(event: { key: string }): void {
        if (event.key === 'Escape') {
            // Navigate back one level
            const popped = this.pop();
            if (popped && this.onNavigateCallback && this.trail.length > 0) {
                this.onNavigateCallback(this.trail[this.trail.length - 1]);
            }
        }
    }

    /**
     * Register navigation callback
     */
    onNavigate(callback: (item: BreadcrumbItem) => void): void {
        this.onNavigateCallback = callback;
    }

    /**
     * Get parent level
     */
    getParent(): BreadcrumbItem | null {
        if (this.trail.length < 2) return null;
        return this.trail[this.trail.length - 2];
    }

    /**
     * Check if at root level
     */
    isAtRoot(): boolean {
        return this.trail.length <= 1;
    }
}


