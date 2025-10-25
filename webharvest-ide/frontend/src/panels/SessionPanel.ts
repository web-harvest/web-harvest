/**
 * SessionPanel - Simple Implementation
 * Displays session information
 */

import type { SessionInfo, TokenUsage } from '../types';

export class SessionPanel {
    private container: HTMLElement;
    private sessionInfo: SessionInfo | null = null;
    private tokenUsage: TokenUsage | null = null;
    private sessionHistory: any[] = []; // Store loaded history

    constructor(containerId: string) {
        const container = document.getElementById(containerId);

        if (!container) {
            throw new Error(`Container with ID "${containerId}" not found`);
        }

        this.container = container;
    }

    /**
     * Set session info (accepts null to clear)
     */
    setSessionInfo(info: SessionInfo | null): void {
        this.sessionInfo = info;
        this.render();
    }

    /**
     * Set token usage (accepts null to clear)
     */
    setTokenUsage(usage: TokenUsage | null): void {
        this.tokenUsage = usage;
        this.render();
    }

    /**
     * Clear panel
     */
    clear(): void {
        this.sessionInfo = null;
        this.tokenUsage = null;
        this.container.innerHTML = '';
    }

    /**
     * Get session info
     */
    getSessionInfo(): SessionInfo | null {
        return this.sessionInfo ? { ...this.sessionInfo } : null;
    }

    /**
     * Get token usage
     */
    getTokenUsage(): TokenUsage | null {
        return this.tokenUsage ? { ...this.tokenUsage } : null;
    }

    /**
     * Render session information (matches old app.js renderSessionInfo)
     */
    private render(): void {
        console.log('[SessionPanel] render() called');
        console.log('[SessionPanel] sessionInfo:', this.sessionInfo);
        console.log('[SessionPanel] tokenUsage:', this.tokenUsage);

        // ALWAYS clear first
        this.container.innerHTML = '';

        // If no data at all, show empty state with View History button
        if (!this.sessionInfo && !this.tokenUsage) {
            console.log('[SessionPanel] No data, showing empty state');
            this.container.innerHTML = `
                <div class="empty-state">
                    <i class="fas fa-info-circle"></i>
                    <p>No session active</p>
                    <small>Session details will appear after execution</small>
                </div>
                <button class="btn btn-secondary" onclick="window.app.loadSessionHistory()" style="margin-top: 16px; width: 100%;">
                    <i class="fas fa-history"></i> View History
                </button>`;
            return;
        }

        const content = document.createElement('div');
        content.style.padding = '16px';

        // Session Info
        if (this.sessionInfo) {
            console.log('[SessionPanel] Rendering session info...');
            const section = document.createElement('div');
            section.style.marginBottom = '16px';

            const title = document.createElement('h3');
            title.textContent = 'Session Information';
            title.style.fontSize = '14px';
            title.style.marginBottom = '8px';
            section.appendChild(title);

            const info = [
                ['Session ID', this.sessionInfo.sessionId],
                ['Status', this.sessionInfo.status],
                ['Duration', this.formatDuration(this.sessionInfo.duration)],
                ['Elements Processed', this.sessionInfo.elementsProcessed],
                ['Processing Rate', this.sessionInfo.processingRate ? `${this.sessionInfo.processingRate.toFixed(2)} elem/s` : 'N/A'],
            ];

            info.forEach(([label, value]) => {
                const row = document.createElement('div');
                row.style.marginBottom = '4px';
                row.style.fontSize = '12px';
                row.innerHTML = `<strong>${label}:</strong> ${value || 'N/A'}`;
                section.appendChild(row);
            });

            content.appendChild(section);
        }

        // HTTP Metrics
        if (this.sessionInfo?.httpMetrics) {
            const section = document.createElement('div');
            section.style.marginTop = '16px';

            const title = document.createElement('h3');
            title.textContent = 'HTTP Metrics';
            title.style.fontSize = '14px';
            title.style.marginBottom = '8px';
            section.appendChild(title);

            const http = this.sessionInfo.httpMetrics;
            const info = [
                ['Requests', http.requests],
                ['Downloaded', `${(http.bytesDownloaded / 1024).toFixed(2)} KB`],
                ['Uploaded', `${(http.bytesUploaded / 1024).toFixed(2)} KB`],
                ['Avg Response Time', `${http.avgResponseTime} ms`],
            ];

            info.forEach(([label, value]) => {
                const row = document.createElement('div');
                row.style.marginBottom = '4px';
                row.style.fontSize = '12px';
                row.innerHTML = `<strong>${label}:</strong> ${value}`;
                section.appendChild(row);
            });

            content.appendChild(section);
        }

        // Plugin Breakdown
        if (this.sessionInfo?.pluginBreakdown) {
            const section = document.createElement('div');
            section.style.marginTop = '16px';

            const title = document.createElement('h3');
            title.textContent = 'Plugin Breakdown';
            title.style.fontSize = '14px';
            title.style.marginBottom = '8px';
            section.appendChild(title);

            Object.entries(this.sessionInfo.pluginBreakdown).forEach(([plugin, timeMs]) => {
                const row = document.createElement('div');
                row.style.marginBottom = '4px';
                row.style.fontSize = '12px';
                row.innerHTML = `<strong>${plugin}:</strong> ${timeMs}ms`;
                section.appendChild(row);
            });

            content.appendChild(section);
        }

        // Token Usage
        if (this.tokenUsage) {
            const section = document.createElement('div');
            section.style.marginTop = '16px';

            const title = document.createElement('h3');
            title.textContent = 'Token Usage';
            title.style.fontSize = '14px';
            title.style.marginBottom = '8px';
            section.appendChild(title);

            const info = [
                ['HTTP Requests', this.tokenUsage.httpRequests || 0],
                ['HTTP Bytes', this.tokenUsage.httpBytes ? `${(this.tokenUsage.httpBytes / 1024).toFixed(2)} KB` : '0 KB'],
                ['CPU Time', `${this.tokenUsage.cpuTimeMs || 0} ms`],
                ['Memory Peak', this.tokenUsage.memoryPeakBytes ? `${(this.tokenUsage.memoryPeakBytes / 1024 / 1024).toFixed(2)} MB` : '0 MB'],
            ];

            info.forEach(([label, value]) => {
                const row = document.createElement('div');
                row.style.marginBottom = '4px';
                row.style.fontSize = '12px';
                row.innerHTML = `<strong>${label}:</strong> ${value}`;
                section.appendChild(row);
            });

            content.appendChild(section);
        }

        // Add "View All Sessions" button at bottom
        const viewAllBtn = document.createElement('button');
        viewAllBtn.className = 'btn btn-secondary';
        viewAllBtn.style.width = '100%';
        viewAllBtn.style.marginTop = '16px';
        viewAllBtn.innerHTML = '<i class="fas fa-history"></i> View All Sessions';
        viewAllBtn.onclick = () => (window as any).app.loadSessionHistory();
        content.appendChild(viewAllBtn);

        this.container.appendChild(content);
        console.log('[SessionPanel] Rendered successfully to DOM');
    }

    /**
     * Format duration in ms to readable string
     */
    private formatDuration(duration: number | undefined): string {
        if (!duration) return 'N/A';

        const seconds = Math.floor(duration / 1000);
        const minutes = Math.floor(seconds / 60);
        const hours = Math.floor(minutes / 60);

        if (hours > 0) {
            return `${hours}h ${minutes % 60}m ${seconds % 60}s`;
        } else if (minutes > 0) {
            return `${minutes}m ${seconds % 60}s`;
        } else {
            return `${seconds}s`;
        }
    }

    /**
     * Load session history from API (matches old app.js)
     */
    async loadSessionHistory(): Promise<void> {
        console.log('[SessionPanel] Loading session history...');

        try {
            const response = await fetch('/api/sessions/history?limit=20');

            if (!response.ok) {
                console.error('[SessionPanel] HTTP error:', response.status);
                return;
            }

            const data = await response.json();
            console.log('[SessionPanel] Loaded', data.total, 'sessions');

            this.sessionHistory = data.sessions || [];
            this.renderSessionHistory();

        } catch (error) {
            console.error('[SessionPanel] Error loading history:', error);
        }
    }

    /**
     * Render session history list
     */
    private renderSessionHistory(): void {
        console.log('[SessionPanel] Rendering history:', this.sessionHistory.length, 'sessions');

        // ALWAYS clear first
        this.container.innerHTML = '';

        if (!this.sessionHistory || this.sessionHistory.length === 0) {
            this.container.innerHTML = `
                <div class="empty-state">
                    <i class="fas fa-history"></i>
                    <p>No sessions found</p>
                    <small>Session history will appear after executions</small>
                </div>`;
            return;
        }

        const content = document.createElement('div');
        content.style.padding = '16px';

        const title = document.createElement('h3');
        title.textContent = `Session History (${this.sessionHistory.length})`;
        title.style.fontSize = '16px';
        title.style.marginBottom = '16px';
        content.appendChild(title);

        // Render each session as a clickable item
        this.sessionHistory.forEach((session, index) => {
            const item = document.createElement('div');
            item.className = 'session-history-item';
            item.style.padding = '12px';
            item.style.marginBottom = '8px';
            item.style.background = '#f9f9f9';
            item.style.borderRadius = '8px';
            item.style.border = '1px solid #e0e0e0';
            item.style.cursor = 'pointer';
            item.style.transition = 'all 0.2s';

            item.onmouseover = () => {
                item.style.background = '#f0f0f0';
                item.style.borderColor = '#FFC107';
            };
            item.onmouseout = () => {
                item.style.background = '#f9f9f9';
                item.style.borderColor = '#e0e0e0';
            };

            item.onclick = () => this.showSessionDetails(index);

            const statusIcon = session.status === 'COMPLETED' ? 'fa-check-circle' :
                session.status === 'FAILED' ? 'fa-times-circle' : 'fa-clock';
            const statusColor = session.status === 'COMPLETED' ? '#4CAF50' :
                session.status === 'FAILED' ? '#F44336' : '#FFC107';

            const timestamp = new Date(session.timestamp).toLocaleString();
            const duration = session.duration < 1000 ?
                `${session.duration}ms` :
                `${(session.duration / 1000).toFixed(2)}s`;

            item.innerHTML = `
                <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 8px;">
                    <div style="display: flex; align-items: center; gap: 8px;">
                        <i class="fas ${statusIcon}" style="color: ${statusColor};"></i>
                        <strong>${session.configName || 'Unknown'}</strong>
                    </div>
                    <span style="font-size: 11px; color: #666;">${timestamp}</span>
                </div>
                <div style="display: flex; gap: 16px; font-size: 12px; color: #666;">
                    <span><i class="fas fa-fingerprint"></i> ${session.sessionId.substring(0, 8)}...</span>
                    <span><i class="fas fa-clock"></i> ${duration}</span>
                    <span><i class="fas fa-cubes"></i> ${session.elementsProcessed || 0} elements</span>
                </div>
            `;

            content.appendChild(item);
        });

        // Add refresh button
        const refreshBtn = document.createElement('button');
        refreshBtn.className = 'btn btn-secondary';
        refreshBtn.style.width = '100%';
        refreshBtn.style.marginTop = '16px';
        refreshBtn.innerHTML = '<i class="fas fa-sync"></i> Refresh';
        refreshBtn.onclick = () => this.loadSessionHistory();
        content.appendChild(refreshBtn);

        this.container.appendChild(content);
        console.log('[SessionPanel] History rendered to DOM');
    }

    /**
     * Show details of a specific session
     */
    async showSessionDetails(index: number): Promise<void> {
        if (!this.sessionHistory || !this.sessionHistory[index]) {
            console.warn('[SessionPanel] Session not found:', index);
            return;
        }

        const session = this.sessionHistory[index];
        console.log('[SessionPanel] Showing details:', session.sessionId);

        // Temporarily set as current session to reuse render logic
        const originalSession = this.sessionInfo;
        const originalToken = this.tokenUsage;

        this.sessionInfo = session;
        this.tokenUsage = session.tokenUsage || null;

        // ALWAYS clear first
        this.container.innerHTML = '';

        // Add back button
        const backBtn = document.createElement('button');
        backBtn.className = 'btn btn-secondary';
        backBtn.style.marginBottom = '16px';
        backBtn.innerHTML = '<i class="fas fa-arrow-left"></i> Back to History';
        backBtn.onclick = () => {
            this.sessionInfo = originalSession;
            this.tokenUsage = originalToken;
            this.renderSessionHistory();
        };
        this.container.appendChild(backBtn);

        // Render session details using existing logic
        const content = document.createElement('div');
        content.style.padding = '16px';

        // Title
        const title = document.createElement('h3');
        title.textContent = 'Session Details';
        title.style.fontSize = '16px';
        title.style.marginBottom = '16px';
        content.appendChild(title);

        // Session info (reuse render logic)
        const section = document.createElement('div');
        section.style.marginBottom = '16px';

        const subTitle = document.createElement('h4');
        subTitle.textContent = 'Session Information';
        subTitle.style.fontSize = '14px';
        subTitle.style.marginBottom = '8px';
        section.appendChild(subTitle);

        const duration = session.duration < 1000 ?
            `${session.duration}ms` :
            `${(session.duration / 1000).toFixed(2)}s`;

        const info = [
            ['Session ID', session.sessionId],
            ['Config', session.configName || 'N/A'],
            ['Status', session.status],
            ['Duration', duration],
            ['Elements Processed', session.elementsProcessed || 0],
            ['Processing Rate', session.processingRate ? `${session.processingRate.toFixed(2)} elem/s` : 'N/A'],
            ['Timestamp', new Date(session.timestamp).toLocaleString()],
        ];

        info.forEach(([label, value]) => {
            const row = document.createElement('div');
            row.style.marginBottom = '4px';
            row.style.fontSize = '12px';
            row.innerHTML = `<strong>${label}:</strong> ${value}`;
            section.appendChild(row);
        });

        content.appendChild(section);

        // HTTP Metrics if available
        if (session.httpMetrics) {
            const httpSection = document.createElement('div');
            httpSection.style.marginTop = '16px';

            const httpTitle = document.createElement('h4');
            httpTitle.textContent = 'HTTP Metrics';
            httpTitle.style.fontSize = '14px';
            httpTitle.style.marginBottom = '8px';
            httpSection.appendChild(httpTitle);

            const http = session.httpMetrics;
            const httpInfo = [
                ['Requests', http.requests],
                ['Downloaded', `${(http.bytesDownloaded / 1024).toFixed(2)} KB`],
                ['Uploaded', `${(http.bytesUploaded / 1024).toFixed(2)} KB`],
                ['Avg Response Time', `${http.avgResponseTime} ms`],
            ];

            httpInfo.forEach(([label, value]) => {
                const row = document.createElement('div');
                row.style.marginBottom = '4px';
                row.style.fontSize = '12px';
                row.innerHTML = `<strong>${label}:</strong> ${value}`;
                httpSection.appendChild(row);
            });

            content.appendChild(httpSection);
        }

        // Plugin Breakdown if available
        if (session.pluginBreakdown) {
            const pluginSection = document.createElement('div');
            pluginSection.style.marginTop = '16px';

            const pluginTitle = document.createElement('h4');
            pluginTitle.textContent = 'Plugin Breakdown';
            pluginTitle.style.fontSize = '14px';
            pluginTitle.style.marginBottom = '8px';
            pluginSection.appendChild(pluginTitle);

            Object.entries(session.pluginBreakdown).forEach(([plugin, timeMs]) => {
                const row = document.createElement('div');
                row.style.marginBottom = '4px';
                row.style.fontSize = '12px';
                row.innerHTML = `<strong>${plugin}:</strong> ${timeMs}ms`;
                pluginSection.appendChild(row);
            });

            content.appendChild(pluginSection);
        }

        this.container.appendChild(content);
        console.log('[SessionPanel] Details rendered to DOM');
    }
}

