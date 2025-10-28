/**
 * Integration tests for XQuery plugin execution
 * Tests REAL execution behavior against backend
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

describe('XQuery Integration Tests', () => {

    it('should execute simple xquery with string parameter', async () => {
        const xml = `<?xml version="1.0" encoding="UTF-8"?>
<config xmlns="http://org.webharvest/schema/2.1/core">
    <def var="result">
        <xquery>
            <xq-param name="name" type="string">World</xq-param>
            <xq-expression>
                <![CDATA[
                declare variable $name as xs:string external;
                concat("Hello ", $name)
                ]]>
            </xq-expression>
        </xquery>
    </def>
</config>`;

        const response = await fetch(`${API_BASE}/api/execute`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                configXml: xml,
                configName: 'xquery-string-test.xml'
            }),
        });

        expect(response.ok).toBe(true);
        const result = await response.json();
        expect(result.executionId).toBeDefined();
    }, 30000);

    it('should execute xquery with integer parameter', async () => {
        const xml = `<?xml version="1.0" encoding="UTF-8"?>
<config xmlns="http://org.webharvest/schema/2.1/core">
    <def var="result">
        <xquery>
            <xq-param name="num" type="integer">42</xq-param>
            <xq-expression>
                <![CDATA[
                declare variable $num as xs:integer external;
                $num * 2
                ]]>
            </xq-expression>
        </xquery>
    </def>
</config>`;

        const response = await fetch(`${API_BASE}/api/execute`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                configXml: xml,
                configName: 'xquery-integer-test.xml'
            }),
        });

        expect(response.ok).toBe(true);
    }, 30000);

    it('should execute xquery with boolean parameter (KNOWN BUG)', async () => {
        // This test documents the known bug with boolean parameters
        const xml = `<?xml version="1.0" encoding="UTF-8"?>
<config xmlns="http://org.webharvest/schema/2.1/core">
    <def var="result">
        <xquery>
            <xq-param name="flag" type="boolean">true</xq-param>
            <xq-expression>
                <![CDATA[
                declare variable $flag as xs:boolean external;
                if ($flag) then "yes" else "no"
                ]]>
            </xq-expression>
        </xquery>
    </def>
</config>`;

        const response = await fetch(`${API_BASE}/api/execute`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                configXml: xml,
                configName: 'xquery-boolean-test.xml'
            }),
        });

        // Currently FAILS with Saxon error
        // TODO: Fix XQueryPlugin to properly convert boolean string to xs:boolean
        expect(response.ok).toBe(true);

    }, 30000);

    it('should execute xquery with node parameter', async () => {
        const xml = `<?xml version="1.0" encoding="UTF-8"?>
<config xmlns="http://org.webharvest/schema/2.1/core">
    <def var="result">
        <xquery>
            <xq-param name="doc" type="node()">
                <![CDATA[
                <items>
                    <item>one</item>
                    <item>two</item>
                </items>
                ]]>
            </xq-param>
            <xq-expression>
                <![CDATA[
                declare variable $doc as node() external;
                count($doc//item)
                ]]>
            </xq-expression>
        </xquery>
    </def>
</config>`;

        const response = await fetch(`${API_BASE}/api/execute`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                configXml: xml,
                configName: 'xquery-node-test.xml'
            }),
        });

        expect(response.ok).toBe(true);
    }, 30000);
});


