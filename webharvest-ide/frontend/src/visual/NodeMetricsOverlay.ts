/**
 * NodeMetricsOverlay - Implementation
 * Displays detailed execution metrics on running nodes
 */

export interface NodeMetrics {
    progress?: number; // 0-100
    elapsedTime?: number; // milliseconds
    dataProcessed?: number; // bytes
    httpRequests?: number;
    memoryUsed?: number; // bytes
    estimatedTotal?: number; // milliseconds
    throughput?: number; // bytes per second
}

export class NodeMetricsOverlay {
    /**
     * Show metrics overlay on node
     */
    showMetrics(element: HTMLElement, metrics: NodeMetrics): void {
        // Remove existing metrics panel
        this.hideMetrics(element);

        // Create metrics panel
        const panel = document.createElement('div');
        panel.className = 'metrics-panel';
        panel.style.cssText = `
            position: absolute;
            bottom: 8px;
            left: 8px;
            right: 8px;
            background: rgba(255, 255, 255, 0.95);
            border: 1px solid var(--gray-200);
            border-radius: 4px;
            padding: 8px;
            font-size: 11px;
            z-index: 5;
            box-shadow: 0 2px 4px rgba(0,0,0,0.1);
        `;

        // Progress bar
        if (metrics.progress !== undefined) {
            const progressBar = this.createProgressBar(metrics.progress);
            panel.appendChild(progressBar);
        }

        // Metrics row
        const metricsRow = document.createElement('div');
        metricsRow.style.cssText = `
            display: flex;
            gap: 12px;
            margin-top: 6px;
            color: var(--gray-700);
            flex-wrap: wrap;
        `;

        // Elapsed time
        if (metrics.elapsedTime !== undefined) {
            const timeSpan = document.createElement('span');
            timeSpan.innerHTML = `⏱ ${this.formatTime(metrics.elapsedTime)}`;
            metricsRow.appendChild(timeSpan);
        }

        // Data processed
        if (metrics.dataProcessed !== undefined) {
            const dataSpan = document.createElement('span');
            dataSpan.innerHTML = `📊 ${this.formatBytes(metrics.dataProcessed)}`;
            metricsRow.appendChild(dataSpan);
        }

        // HTTP requests
        if (metrics.httpRequests !== undefined) {
            const httpSpan = document.createElement('span');
            httpSpan.innerHTML = `🔄 ${metrics.httpRequests} req`;
            metricsRow.appendChild(httpSpan);
        }

        // Memory usage
        if (metrics.memoryUsed !== undefined) {
            const memSpan = document.createElement('span');
            memSpan.innerHTML = `💾 ${this.formatBytes(metrics.memoryUsed)}`;
            metricsRow.appendChild(memSpan);
        }

        // ETA
        if (metrics.progress !== undefined && metrics.progress > 0 && metrics.progress < 100 && metrics.elapsedTime !== undefined) {
            const eta = this.estimateETA(metrics.progress, metrics.elapsedTime);
            if (eta > 0) {
                const etaSpan = document.createElement('span');
                etaSpan.innerHTML = `⏳ ETA ${this.formatTime(eta)}`;
                etaSpan.style.color = 'var(--purple)';
                metricsRow.appendChild(etaSpan);
            }
        }

        panel.appendChild(metricsRow);

        // Ensure node has relative positioning
        if (element.style.position !== 'absolute' && element.style.position !== 'relative') {
            element.style.position = 'relative';
        }

        element.appendChild(panel);
    }

    /**
     * Hide metrics overlay
     */
    hideMetrics(element: HTMLElement): void {
        const panel = element.querySelector('.metrics-panel');
        if (panel) {
            panel.remove();
        }
    }

    /**
     * Update only the progress bar (efficient update)
     */
    updateProgress(element: HTMLElement, progress: number): void {
        const progressFill = element.querySelector('.metrics-progress-fill') as HTMLElement;
        if (progressFill) {
            progressFill.style.width = `${progress}%`;

            // Update progress text
            const progressText = element.querySelector('.metrics-progress-text');
            if (progressText) {
                progressText.textContent = `${Math.round(progress)}%`;
            }
        }
    }

    /**
     * Create progress bar element
     */
    private createProgressBar(progress: number): HTMLElement {
        const container = document.createElement('div');
        container.className = 'metrics-progress';
        container.style.cssText = `
            position: relative;
            height: 20px;
            background: var(--gray-100);
            border-radius: 10px;
            overflow: hidden;
        `;

        const fill = document.createElement('div');
        fill.className = 'metrics-progress-fill';
        fill.style.cssText = `
            height: 100%;
            width: ${progress}%;
            background: linear-gradient(90deg, var(--lime), var(--yellow));
            border-radius: 10px;
            transition: width 0.3s ease;
            animation: shimmer 2s linear infinite;
        `;

        const text = document.createElement('div');
        text.className = 'metrics-progress-text';
        text.textContent = `${Math.round(progress)}%`;
        text.style.cssText = `
            position: absolute;
            top: 50%;
            left: 50%;
            transform: translate(-50%, -50%);
            font-size: 11px;
            font-weight: 600;
            color: var(--gray-700);
            z-index: 1;
        `;

        container.appendChild(fill);
        container.appendChild(text);

        return container;
    }

    /**
     * Format bytes to human-readable size
     */
    formatBytes(bytes: number): string {
        if (bytes === 0) return '0 B';
        if (bytes < 1024) return `${bytes} B`;

        const kb = bytes / 1024;
        if (kb < 1024) {
            return `${kb.toFixed(1)} KB`;
        }

        const mb = kb / 1024;
        return `${mb.toFixed(1)} MB`;
    }

    /**
     * Format milliseconds to human-readable time
     */
    formatTime(ms: number): string {
        if (ms < 1000) {
            return `${(ms / 1000).toFixed(1)}s`;
        }

        const seconds = Math.floor(ms / 1000);
        if (seconds < 60) {
            return `${(ms / 1000).toFixed(1)}s`;
        }

        const minutes = Math.floor(seconds / 60);
        const remainingSeconds = seconds % 60;
        return `${minutes}m ${remainingSeconds}s`;
    }

    /**
     * Calculate data throughput (bytes per second)
     */
    calculateThroughput(dataProcessed: number, elapsedTime: number): number {
        if (elapsedTime === 0) return 0;
        return (dataProcessed / elapsedTime) * 1000; // Convert to per second
    }

    /**
     * Estimate time remaining (ETA)
     */
    estimateETA(progress: number, elapsedTime: number): number {
        if (progress === 0 || progress >= 100) return 0;

        // Calculate time per percent
        const timePerPercent = elapsedTime / progress;

        // Calculate remaining time
        const remainingPercent = 100 - progress;
        return Math.round(timePerPercent * remainingPercent);
    }
}


