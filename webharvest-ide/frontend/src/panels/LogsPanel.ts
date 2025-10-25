/**
 * LogsPanel - Implementation (TDD Green Phase)
 * Manages execution logs display
 */

import type { LogEntry, LogType } from '../types';

export class LogsPanel {
    private logs: LogEntry[] = [];
    private container: HTMLElement;
    private listElement: HTMLElement;

    constructor(containerId: string) {
        const container = document.getElementById(containerId);

        if (!container) {
            throw new Error(`Container with ID "${containerId}" not found`);
        }

        this.container = container;
        this.listElement = this.createListElement();
        this.container.appendChild(this.listElement);
    }

    /**
     * Create logs list container
     */
    private createListElement(): HTMLElement {
        const list = document.createElement('div');
        list.className = 'logs-list';
        list.style.overflowY = 'auto';
        list.style.maxHeight = '100%';
        return list;
    }

    /**
     * Generate unique ID for log
     */
    private generateId(): string {
        return `log-${Date.now()}-${Math.random().toString(36).substr(2, 9)}`;
    }

    /**
     * Add new log entry
     */
    addLog(type: LogType, message: string): void {
        const entry: LogEntry = {
            id: this.generateId(),
            type,
            message,
            timestamp: Date.now(),
        };

        this.logs.push(entry);
        this.renderLog(entry);
        this.autoScroll();
        this.updateBadge();
    }

    /**
     * Update logs badge counter
     */
    private updateBadge(): void {
        const badge = document.getElementById('logsBadge');
        if (badge) {
            badge.textContent = this.logs.length.toString();
        }
    }

    /**
     * Render single log entry
     */
    private renderLog(entry: LogEntry): void {
        const logElement = document.createElement('div');
        logElement.className = `log-entry log-${entry.type}`;
        logElement.dataset.logId = entry.id;

        const time = new Date(entry.timestamp).toLocaleTimeString();

        logElement.innerHTML = `
      <span class="log-time">${time}</span>
      <span class="log-message">${this.escapeHTML(entry.message)}</span>
    `;

        this.listElement.appendChild(logElement);
    }

    /**
     * Escape HTML in message
     */
    private escapeHTML(text: string): string {
        const div = document.createElement('div');
        div.textContent = text;
        return div.innerHTML;
    }

    /**
     * Auto-scroll to bottom
     */
    private autoScroll(): void {
        this.listElement.scrollTop = this.listElement.scrollHeight;
    }

    /**
     * Clear all logs
     */
    clear(): void {
        this.logs = [];
        this.listElement.innerHTML = '';
        this.updateBadge();
    }

    /**
     * Get all logs
     */
    getLogs(): LogEntry[] {
        return [...this.logs]; // Return copy
    }

    /**
     * Filter logs by type
     */
    filterByType(type: LogType): LogEntry[] {
        return this.logs.filter(log => log.type === type);
    }

    /**
     * Get log count
     */
    getLogCount(): number {
        return this.logs.length;
    }

    /**
     * Find log by ID
     */
    findById(id: string): LogEntry | null {
        return this.logs.find(log => log.id === id) || null;
    }

    /**
     * Remove log by ID
     */
    removeById(id: string): boolean {
        const index = this.logs.findIndex(log => log.id === id);

        if (index === -1) {
            return false;
        }

        this.logs.splice(index, 1);

        // Remove from DOM
        const logElement = this.listElement.querySelector(`[data-log-id="${id}"]`);
        if (logElement) {
            logElement.remove();
        }

        return true;
    }

    /**
     * Get logs after timestamp
     */
    getLogsSince(timestamp: number): LogEntry[] {
        return this.logs.filter(log => log.timestamp > timestamp);
    }

    /**
     * Export logs as text
     */
    exportAsText(): string {
        return this.logs
            .map(log => {
                const time = new Date(log.timestamp).toISOString();
                return `[${time}] [${log.type.toUpperCase()}] ${log.message}`;
            })
            .join('\n');
    }
}

