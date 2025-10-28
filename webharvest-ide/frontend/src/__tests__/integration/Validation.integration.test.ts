/**
 * Integration tests for configuration validation
 * Tests REAL validation behavior against backend
 */

import fetch from 'node-fetch';

const API_BASE = 'http://localhost:8080';

// Wait for server to be ready
beforeAll(async () => {
    const maxRetries = 30;
    for (let i = 0; i < maxRetries; i++) {
        try {
            const response = await fetch(`${API_BASE}/api/plugins`);
            if (response.ok) {
                return;
            }
        } catch (e) {
            // Server not ready yet
        }
        await new Promise(resolve => setTimeout(resolve, 1000));
    }
    throw new Error('Server failed to start');
}, 60000);

describe('Validation Integration Tests (TDD)', () => {

    describe('Invalid Attributes', () => {

        it('should reject unknown attribute on def plugin', async () => {
            const xml = `<?xml version="1.0" encoding="UTF-8"?>
<config xmlns="http://org.webharvest/schema/2.1/core">
    <def var="test" badattr="invalid">value</def>
</config>`;

            const response = await fetch(`${API_BASE}/api/config/validate`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/xml' },
                body: xml,
            });

            expect(response.ok).toBe(true);
            const result = await response.json();

            // EXPECTED: validation should FAIL
            expect(result.valid).toBe(false);
            expect(result.message).toContain('badattr');
        });

        it('should reject invalid var name (var2 instead of var)', async () => {
            const xml = `<?xml version="1.0" encoding="UTF-8"?>
<config xmlns="http://org.webharvest/schema/2.1/core">
    <def var2="test">value</def>
</config>`;

            const response = await fetch(`${API_BASE}/api/config/validate`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/xml' },
                body: xml,
            });

            const result = await response.json();

            // EXPECTED: validation should FAIL (var2 is not a valid attribute)
            expect(result.valid).toBe(false);
            expect(result.message).toMatch(/var2|required.*var/i);
        });

        it('should reject unknown attribute on xpath plugin', async () => {
            const xml = `<?xml version="1.0" encoding="UTF-8"?>
<config xmlns="http://org.webharvest/schema/2.1/core">
    <def var="test">
        <xpath expression="//h1" sss="invalid">
            <var-def name="content">test</var-def>
        </xpath>
    </def>
</config>`;

            const response = await fetch(`${API_BASE}/api/config/validate`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/xml' },
                body: xml,
            });

            const result = await response.json();

            // EXPECTED: validation should FAIL
            expect(result.valid).toBe(false);
            expect(result.message).toContain('sss');
        });
    });

    describe('Invalid Plugins', () => {

        it('should reject unknown plugin name', async () => {
            const xml = `<?xml version="1.0" encoding="UTF-8"?>
<config xmlns="http://org.webharvest/schema/2.1/core">
    <unknown-plugin/>
</config>`;

            const response = await fetch(`${API_BASE}/api/config/validate`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/xml' },
                body: xml,
            });

            const result = await response.json();

            // EXPECTED: validation should FAIL
            expect(result.valid).toBe(false);
            expect(result.message).toMatch(/unknown-plugin|not found/i);
        });
    });

    describe('Invalid Content', () => {

        it('should reject random text outside tags', async () => {
            const xml = `<?xml version="1.0" encoding="UTF-8"?>
<config xmlns="http://org.webharvest/schema/2.1/core">
    <def var="test">value</def>
    random text here
</config>`;

            const response = await fetch(`${API_BASE}/api/config/validate`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/xml' },
                body: xml,
            });

            const result = await response.json();

            // EXPECTED: validation should FAIL
            expect(result.valid).toBe(false);
            expect(result.message).toMatch(/text|content|invalid/i);
        });
    });

    describe('Context-Specific Plugins', () => {

        it('should reject http-header outside http plugin', async () => {
            const xml = `<?xml version="1.0" encoding="UTF-8"?>
<config xmlns="http://org.webharvest/schema/2.1/core">
    <http-header name="User-Agent" value="test"/>
</config>`;

            const response = await fetch(`${API_BASE}/api/config/validate`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/xml' },
                body: xml,
            });

            const result = await response.json();

            // EXPECTED: validation should FAIL
            expect(result.valid).toBe(false);
            expect(result.message).toMatch(/http-header.*context|parent|http/i);
        });

        it('should accept http-header inside http plugin', async () => {
            const xml = `<?xml version="1.0" encoding="UTF-8"?>
<config xmlns="http://org.webharvest/schema/2.1/core">
    <http url="https://example.com">
        <http-header name="User-Agent" value="test"/>
    </http>
</config>`;

            const response = await fetch(`${API_BASE}/api/config/validate`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/xml' },
                body: xml,
            });

            const result = await response.json();

            // EXPECTED: validation should PASS
            expect(result.valid).toBe(true);
        });
    });

    describe('Valid Configurations', () => {

        it('should accept valid simple configuration', async () => {
            const xml = `<?xml version="1.0" encoding="UTF-8"?>
<config xmlns="http://org.webharvest/schema/2.1/core">
    <def var="test">value</def>
</config>`;

            const response = await fetch(`${API_BASE}/api/config/validate`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/xml' },
                body: xml,
            });

            const result = await response.json();

            expect(result.valid).toBe(true);
        });

        it('should accept valid http configuration', async () => {
            const xml = `<?xml version="1.0" encoding="UTF-8"?>
<config xmlns="http://org.webharvest/schema/2.1/core">
    <def var="content">
        <http url="https://example.com">
            <http-header name="User-Agent" value="WebHarvest"/>
        </http>
    </def>
</config>`;

            const response = await fetch(`${API_BASE}/api/config/validate`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/xml' },
                body: xml,
            });

            const result = await response.json();

            expect(result.valid).toBe(true);
        });
    });
});


