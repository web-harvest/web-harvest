/**
 * BreadcrumbNav - Implementation
 * Breadcrumb navigation for drilling into nested containers
 */

export interface BreadcrumbMetric {
    label: string;
    value: string;
}

export interface BreadcrumbItem {
    id: string;
    label: string;
    level: number;
    nodeType?: string;
    summary?: string;
    metrics?: BreadcrumbMetric[];
}

export class BreadcrumbNav {
    private container: HTMLElement | null = null;
    private trail: BreadcrumbItem[] = [];
    private onNavigateCallback: ((item: BreadcrumbItem) => void) | null = null;
    private iconResolver: ((item: BreadcrumbItem) => HTMLElement | null) | null = null;

    /**
     * Render breadcrumb trail
     */
    render(container: HTMLElement, trail: BreadcrumbItem[]): void {
        this.container = container;
        this.trail = [...trail];

        container.innerHTML = '';
        container.classList.add('breadcrumb-nav');

        const trailWrapper = document.createElement('div');
        trailWrapper.className = 'breadcrumb-nav__trail';
        container.appendChild(trailWrapper);

        trail.forEach((item, index) => {
            const isCurrent = index === trail.length - 1;
            const element = isCurrent ? document.createElement('span') : document.createElement('button');
            element.className = 'breadcrumb-item';
            element.dataset.itemId = item.id;
            element.title = item.summary ?? item.label;

            if (isCurrent) {
                element.classList.add('breadcrumb-item--current');
                element.classList.add('breadcrumb-current');
                element.setAttribute('aria-current', 'location');
            } else {
                (element as HTMLButtonElement).type = 'button';
                element.addEventListener('click', () => {
                    this.navigateTo(item.id);
                });
            }

            if (this.iconResolver) {
                const icon = this.iconResolver(item);
                if (icon) {
                    icon.classList.add('breadcrumb-item__icon');
                    element.appendChild(icon);
                }
            }

            const label = document.createElement('span');
            label.className = 'breadcrumb-item__label';
            label.textContent = item.label;
            element.appendChild(label);

            if (item.metrics && item.metrics.length > 0) {
                const metrics = document.createElement('div');
                metrics.className = 'breadcrumb-item__metrics';
                item.metrics.slice(0, 3).forEach(metric => {
                    const badge = document.createElement('span');
                    badge.className = 'breadcrumb-item__metric';
                    badge.dataset.metricLabel = metric.label;
                    badge.textContent = `${metric.label}: ${metric.value}`;
                    metrics.appendChild(badge);
                });
                element.appendChild(metrics);
            }

            trailWrapper.appendChild(element);

            if (index < trail.length - 1) {
                const separator = document.createElement('span');
                separator.className = 'breadcrumb-separator';
                separator.innerHTML = '<i class="fas fa-chevron-right"></i>';
                trailWrapper.appendChild(separator);
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
     * Provide resolver for icons
     */
    setIconResolver(resolver: ((item: BreadcrumbItem) => HTMLElement | null) | null): void {
        this.iconResolver = resolver;
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


