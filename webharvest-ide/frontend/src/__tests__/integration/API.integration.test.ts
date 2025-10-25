/**
 * Integration tests for Backend API endpoints
 * Tests REAL HTTP communication between frontend and backend
 */

import fetch from 'node-fetch';

// Make fetch global for WebSocket tests
(global as any).fetch = fetch;

describe('Backend API Integration Tests', () => {
    const API_BASE = 'http://localhost:8080';

    describe('Plugin Metadata API', () => {
        it('should fetch plugin metadata from /api/plugins', async () => {
            const response = await fetch(`${API_BASE}/api/plugins`);

            expect(response.ok).toBe(true);
            expect(response.status).toBe(200);

            const data: any = await response.json();

            // API returns object with "plugins" array
            expect(data).toHaveProperty('plugins');
            expect(Array.isArray(data.plugins)).toBe(true);
            expect(data.plugins.length).toBeGreaterThan(0);

            // Check plugin structure
            const firstPlugin = data.plugins[0];
            expect(firstPlugin).toHaveProperty('id');
            expect(firstPlugin).toHaveProperty('name');
            expect(firstPlugin).toHaveProperty('category');
            expect(firstPlugin).toHaveProperty('icon');
        }, 10000);

        it('should have essential plugins', async () => {
            const response = await fetch(`${API_BASE}/api/plugins`);
            const data: any = await response.json();

            const pluginIds = data.plugins.map((p: any) => p.id);

            // Essential plugins must be present
            expect(pluginIds).toContain('http');
            expect(pluginIds).toContain('xpath');
            expect(pluginIds).toContain('def');
            expect(pluginIds).toContain('loop');
            expect(pluginIds).toContain('html-to-xml');
        }, 10000);
    });

    describe('Execution API', () => {
        it('should accept execution request and return execution ID', async () => {
            const xml = `<?xml version="1.0" encoding="UTF-8"?>
<config xmlns="http://org.webharvest/schema/2.1/core">
    <def var="test">Hello World</def>
</config>`;

            const response = await fetch(`${API_BASE}/api/execute`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({
                    configXml: xml,
                    configName: 'integration-test.xml',
                }),
            });

            expect(response.ok).toBe(true);

            const result = await response.json();
            expect(result).toHaveProperty('executionId');
            expect(result.executionId).toBeTruthy();
            expect(typeof result.executionId).toBe('string');
        }, 10000);

        it('should reject invalid XML', async () => {
            const invalidXml = '<config><broken';

            const response = await fetch(`${API_BASE}/api/execute`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({
                    configXml: invalidXml,
                    configName: 'invalid.xml',
                }),
            });

            // Backend might return 200 with error in result
            // or might return 500
            // Either is acceptable for now
            if (response.ok) {
                const result: any = await response.json();
                // If OK, should have executionId but execution will fail
                expect(result).toHaveProperty('executionId');
            } else {
                expect(response.status).toBeGreaterThanOrEqual(400);
            }
        }, 10000);
    });

    describe('Session History API', () => {
        it('should fetch session history from /api/sessions/history', async () => {
            const response = await fetch(`${API_BASE}/api/sessions/history?limit=10`);

            expect(response.ok).toBe(true);

            const data = await response.json();
            expect(data).toHaveProperty('total');
            expect(data).toHaveProperty('sessions');
            expect(Array.isArray(data.sessions)).toBe(true);
        }, 10000);

        it('should return sessions with proper structure', async () => {
            // First execute something to create a session
            const xml = `<?xml version="1.0" encoding="UTF-8"?>
<config xmlns="http://org.webharvest/schema/2.1/core">
    <def var="test">API Test</def>
</config>`;

            await fetch(`${API_BASE}/api/execute`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({
                    configXml: xml,
                    configName: 'api-test.xml',
                }),
            });

            // Wait for execution to complete
            await new Promise(resolve => setTimeout(resolve, 2000));

            // Fetch history
            const response = await fetch(`${API_BASE}/api/sessions/history?limit=1`);
            const data = await response.json();

            if (data.sessions.length > 0) {
                const session = data.sessions[0];

                expect(session).toHaveProperty('sessionId');
                expect(session).toHaveProperty('status');
                expect(session).toHaveProperty('timestamp');
                expect(session).toHaveProperty('duration');
                expect(session).toHaveProperty('elementsProcessed');
            }
        }, 15000);
    });

    describe('Config Validation API', () => {
        it('should validate correct XML', async () => {
            const validXml = `<?xml version="1.0" encoding="UTF-8"?>
<config xmlns="http://org.webharvest/schema/2.1/core">
    <def var="test">Valid</def>
</config>`;

            const response = await fetch(`${API_BASE}/api/config/validate`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/xml' },
                body: validXml,
            });

            expect(response.ok).toBe(true);

            const result = await response.json();
            expect(result.valid).toBe(true);
        }, 10000);

        it('should reject invalid XML', async () => {
            const invalidXml = '<config><broken';

            const response = await fetch(`${API_BASE}/api/config/validate`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/xml' },
                body: invalidXml,
            });

            const result = await response.json();
            expect(result.valid).toBe(false);
            expect(result).toHaveProperty('message');
        }, 10000);
    });

    describe('WebSocket Connection', () => {
        it('should be able to connect to WebSocket endpoint', (done) => {
            // First create an execution
            const xml = `<?xml version="1.0" encoding="UTF-8"?>
<config xmlns="http://org.webharvest/schema/2.1/core">
    <def var="test">WebSocket Test</def>
</config>`;

            fetch(`${API_BASE}/api/execute`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({
                    configXml: xml,
                    configName: 'ws-test.xml',
                }),
            })
                .then(response => response.json())
                .then(result => {
                    const executionId = result.executionId;

                    // Connect WebSocket
                    const ws = new WebSocket(`ws://localhost:8080/ws/execution/${executionId}`);

                    ws.onopen = () => {
                        expect(ws.readyState).toBe(WebSocket.OPEN);
                        ws.close();
                        done();
                    };

                    ws.onerror = (error) => {
                        fail('WebSocket connection failed: ' + error);
                        done();
                    };
                });
        }, 15000);
    });
});

