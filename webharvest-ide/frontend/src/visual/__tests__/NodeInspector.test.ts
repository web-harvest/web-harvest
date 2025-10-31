/**
 * NodeInspector - TDD Tests
 * Tests for full observability inspector panel
 */

import { describe, it, expect, beforeEach } from '@jest/globals';
import { NodeInspector, InspectorData } from '../NodeInspector';

describe('NodeInspector', () => {
    let inspector: NodeInspector;
    let mockContainer: HTMLElement;

    beforeEach(() => {
        inspector = new NodeInspector();

        mockContainer = document.createElement('div');
        mockContainer.id = 'inspector-container';
        document.body.appendChild(mockContainer);
    });

    afterEach(() => {
        document.body.removeChild(mockContainer);
    });

    describe('show', () => {
        it('should render inspector panel', () => {
            const data: InspectorData = {
                nodeId: 'node-1',
                pluginType: 'http',
                status: 'SUCCESS',
                variables: {
                    httpResponse: '<html>...</html>',
                    statusCode: '200'
                }
            };

            inspector.show(mockContainer, data);

            const panel = mockContainer.querySelector('.inspector-panel');
            expect(panel).not.toBeNull();
        });

        it('should display node information', () => {
            const data: InspectorData = {
                nodeId: 'node-1',
                pluginType: 'http',
                status: 'SUCCESS'
            };

            inspector.show(mockContainer, data);

            const info = mockContainer.querySelector('.inspector-info');
            expect(info?.textContent).toContain('http');
        });

        it('should display variable values', () => {
            const data: InspectorData = {
                nodeId: 'node-1',
                pluginType: 'http',
                status: 'SUCCESS',
                variables: {
                    httpResponse: '<html>Example</html>',
                    statusCode: '200'
                }
            };

            inspector.show(mockContainer, data);

            const vars = mockContainer.querySelector('.inspector-variables');
            expect(vars?.textContent).toContain('httpResponse');
            expect(vars?.textContent).toContain('statusCode');
        });

        it('should display data preview with syntax highlighting', () => {
            const data: InspectorData = {
                nodeId: 'node-1',
                pluginType: 'http',
                status: 'SUCCESS',
                dataPreview: {
                    type: 'xml',
                    content: '<html><title>Test</title></html>'
                }
            };

            inspector.show(mockContainer, data);

            const preview = mockContainer.querySelector('.data-preview');
            expect(preview).not.toBeNull();
        });

        it('should display performance breakdown', () => {
            const data: InspectorData = {
                nodeId: 'node-1',
                pluginType: 'http',
                status: 'SUCCESS',
                performance: {
                    parseTime: 123,
                    networkTime: 456,
                    processingTime: 789
                }
            };

            inspector.show(mockContainer, data);

            const perf = mockContainer.querySelector('.performance-breakdown');
            expect(perf).not.toBeNull();
            expect(perf?.textContent).toContain('123');
        });

        it('should display error stack trace', () => {
            const data: InspectorData = {
                nodeId: 'node-1',
                pluginType: 'http',
                status: 'ERROR',
                error: {
                    message: 'Connection timeout',
                    stackTrace: 'at HttpPlugin.execute(HttpPlugin.java:45)\nat Runtime.run(...)'
                }
            };

            inspector.show(mockContainer, data);

            const error = mockContainer.querySelector('.error-details');
            expect(error?.textContent).toContain('Connection timeout');
            expect(error?.textContent).toContain('HttpPlugin.java:45');
        });

        it('should display HTTP request/response details', () => {
            const data: InspectorData = {
                nodeId: 'node-1',
                pluginType: 'http',
                status: 'SUCCESS',
                httpDetails: {
                    requestUrl: 'http://example.com',
                    requestMethod: 'GET',
                    responseStatus: 200,
                    responseHeaders: {
                        'content-type': 'text/html'
                    },
                    responseTime: 234
                }
            };

            inspector.show(mockContainer, data);

            const http = mockContainer.querySelector('.http-details');
            expect(http?.textContent).toContain('GET');
            expect(http?.textContent).toContain('200');
            expect(http?.textContent).toContain('234');
        });
    });

    describe('hide', () => {
        it('should hide inspector panel', () => {
            const data: InspectorData = {
                nodeId: 'node-1',
                pluginType: 'http',
                status: 'SUCCESS'
            };

            inspector.show(mockContainer, data);
            inspector.hide();

            const panel = mockContainer.querySelector('.inspector-panel');
            expect(panel).toBeNull();
        });
    });

    describe('formatDataPreview', () => {
        it('should syntax highlight XML', () => {
            const highlighted = inspector.formatDataPreview('xml', '<root><element>value</element></root>');

            expect(highlighted).toContain('<span');
            expect(highlighted).toContain('element');
        });

        it('should syntax highlight JSON', () => {
            const highlighted = inspector.formatDataPreview('json', '{"key": "value"}');

            expect(highlighted).toContain('key');
        });

        it('should handle plain text', () => {
            const formatted = inspector.formatDataPreview('text', 'Plain text content');

            expect(formatted).toBe('Plain text content');
        });

        it('should truncate large content', () => {
            const longText = 'x'.repeat(10000);
            const formatted = inspector.formatDataPreview('text', longText);

            expect(formatted.length).toBeLessThan(10000);
            expect(formatted).toContain('...');
        });
    });

    describe('tabs', () => {
        it('should show tabs for different sections', () => {
            const data: InspectorData = {
                nodeId: 'node-1',
                pluginType: 'http',
                status: 'SUCCESS',
                variables: { test: 'value' },
                dataPreview: { type: 'xml', content: '<root/>' },
                performance: { parseTime: 100 }
            };

            inspector.show(mockContainer, data);

            const tabs = mockContainer.querySelectorAll('.inspector-tab');
            expect(tabs.length).toBeGreaterThan(1);
        });

        it('should switch between tabs', () => {
            const data: InspectorData = {
                nodeId: 'node-1',
                pluginType: 'http',
                status: 'SUCCESS',
                variables: { test: 'value' },
                dataPreview: { type: 'xml', content: '<root/>' }
            };

            inspector.show(mockContainer, data);
            inspector.switchTab('data');

            const activeTab = mockContainer.querySelector('.inspector-tab.active');
            expect(activeTab?.textContent).toContain('Data');
        });
    });

    describe('position', () => {
        it('should position near node', () => {
            const data: InspectorData = {
                nodeId: 'node-1',
                pluginType: 'http',
                status: 'SUCCESS'
            };

            inspector.showNearNode(mockContainer, data, { x: 300, y: 200 });

            const panel = mockContainer.querySelector('.inspector-panel') as HTMLElement;
            expect(panel.style.left).toBeTruthy();
            expect(panel.style.top).toBeTruthy();
        });

        it('should avoid canvas edges', () => {
            const data: InspectorData = {
                nodeId: 'node-1',
                pluginType: 'http',
                status: 'SUCCESS'
            };

            inspector.showNearNode(mockContainer, data, { x: 1900, y: 1400 });

            // Should reposition to stay in view
            const panel = mockContainer.querySelector('.inspector-panel') as HTMLElement;
            const left = parseInt(panel.style.left);
            expect(left).toBeLessThan(1800);
        });
    });
});


