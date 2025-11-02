/**
 * NodeMetricsOverlay - TDD Tests
 * Tests for metrics overlay on running nodes
 */

import { describe, it, expect, beforeEach } from '@jest/globals';
import { NodeMetricsOverlay, NodeMetrics } from '../NodeMetricsOverlay';

describe('NodeMetricsOverlay', () => {
    let overlay: NodeMetricsOverlay;
    let mockElement: HTMLElement;

    beforeEach(() => {
        overlay = new NodeMetricsOverlay();
        mockElement = document.createElement('div');
        mockElement.className = 'visual-node';
        mockElement.dataset.nodeId = 'test-node-1';
    });

    describe('showMetrics', () => {
        it('should display progress bar', () => {
            const metrics: NodeMetrics = {
                progress: 65,
                elapsedTime: 2300,
                dataProcessed: 131072, // 128 KB
                httpRequests: 3
            };

            overlay.showMetrics(mockElement, metrics);

            const progressBar = mockElement.querySelector('.metrics-progress');
            expect(progressBar).not.toBeNull();
        });

        it('should format elapsed time correctly', () => {
            const metrics: NodeMetrics = {
                progress: 50,
                elapsedTime: 2300 // 2.3 seconds
            };

            overlay.showMetrics(mockElement, metrics);

            const metricsPanel = mockElement.querySelector('.metrics-panel');
            expect(metricsPanel?.textContent).toContain('2.3s');
        });

        it('should format data size in KB', () => {
            const metrics: NodeMetrics = {
                progress: 75,
                dataProcessed: 131072 // 128 KB
            };

            overlay.showMetrics(mockElement, metrics);

            const metricsPanel = mockElement.querySelector('.metrics-panel');
            expect(metricsPanel?.textContent).toContain('128');
            expect(metricsPanel?.textContent).toContain('KB');
        });

        it('should format data size in MB for large values', () => {
            const metrics: NodeMetrics = {
                progress: 90,
                dataProcessed: 2097152 // 2 MB
            };

            overlay.showMetrics(mockElement, metrics);

            const metricsPanel = mockElement.querySelector('.metrics-panel');
            expect(metricsPanel?.textContent).toContain('2.0');
            expect(metricsPanel?.textContent).toContain('MB');
        });

        it('should display HTTP request count', () => {
            const metrics: NodeMetrics = {
                progress: 80,
                httpRequests: 5
            };

            overlay.showMetrics(mockElement, metrics);

            const metricsPanel = mockElement.querySelector('.metrics-panel');
            expect(metricsPanel?.textContent).toContain('5 req');
        });

        it('should display memory usage if provided', () => {
            const metrics: NodeMetrics = {
                progress: 60,
                memoryUsed: 524288 // 512 KB
            };

            overlay.showMetrics(mockElement, metrics);

            const metricsPanel = mockElement.querySelector('.metrics-panel');
            expect(metricsPanel?.textContent).toContain('512');
        });

        it('should update progress bar width', () => {
            const metrics: NodeMetrics = {
                progress: 65
            };

            overlay.showMetrics(mockElement, metrics);

            const progressFill = mockElement.querySelector('.metrics-progress-fill') as HTMLElement;
            expect(progressFill?.style.width).toBe('65%');
        });

        it('should calculate ETA if progress > 0', () => {
            const metrics: NodeMetrics = {
                progress: 50,
                elapsedTime: 5000, // 5 seconds elapsed at 50% = 5s remaining
                estimatedTotal: 10000
            };

            overlay.showMetrics(mockElement, metrics);

            const metricsPanel = mockElement.querySelector('.metrics-panel');
            expect(metricsPanel?.textContent).toContain('ETA');
        });
    });

    describe('hideMetrics', () => {
        it('should remove metrics overlay from node', () => {
            const metrics: NodeMetrics = { progress: 50 };
            overlay.showMetrics(mockElement, metrics);

            overlay.hideMetrics(mockElement);

            const metricsPanel = mockElement.querySelector('.metrics-panel');
            expect(metricsPanel).toBeNull();
        });
    });

    describe('updateProgress', () => {
        it('should update only progress bar without full re-render', () => {
            const metrics: NodeMetrics = { progress: 30 };
            overlay.showMetrics(mockElement, metrics);

            overlay.updateProgress(mockElement, 75);

            const progressFill = mockElement.querySelector('.metrics-progress-fill') as HTMLElement;
            expect(progressFill?.style.width).toBe('75%');
        });
    });

    describe('formatters', () => {
        it('should format bytes to human-readable size', () => {
            expect(overlay.formatBytes(1024)).toBe('1.0 KB');
            expect(overlay.formatBytes(1536)).toBe('1.5 KB');
            expect(overlay.formatBytes(1048576)).toBe('1.0 MB');
            expect(overlay.formatBytes(500)).toBe('500 B');
        });

        it('should format milliseconds to seconds with decimal', () => {
            expect(overlay.formatTime(1500)).toBe('1.5s');
            expect(overlay.formatTime(500)).toBe('0.5s');
            expect(overlay.formatTime(10000)).toBe('10.0s');
        });

        it('should format milliseconds to minutes for long durations', () => {
            expect(overlay.formatTime(65000)).toBe('1m 5s');
            expect(overlay.formatTime(125000)).toBe('2m 5s');
        });
    });

    describe('throughput calculations', () => {
        it('should calculate data throughput per second', () => {
            const throughput = overlay.calculateThroughput(131072, 2000); // 128KB in 2s = 65,536 bytes/s
            expect(throughput).toBeCloseTo(65536, 0);
        });

        it('should handle zero time gracefully', () => {
            const throughput = overlay.calculateThroughput(1024, 0);
            expect(throughput).toBe(0);
        });
    });

    describe('ETA estimation', () => {
        it('should estimate time remaining based on progress', () => {
            const eta = overlay.estimateETA(50, 5000); // 50% done in 5s = 5s remaining
            expect(eta).toBe(5000);
        });

        it('should return 0 for completed tasks', () => {
            const eta = overlay.estimateETA(100, 10000);
            expect(eta).toBe(0);
        });

        it('should return 0 for 0% progress', () => {
            const eta = overlay.estimateETA(0, 1000);
            expect(eta).toBe(0);
        });
    });
});


