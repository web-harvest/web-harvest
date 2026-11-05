// Browser runtime test using Puppeteer (if available) or manual instructions
const http = require('http');
const fs = require('fs');
const path = require('path');

console.log('🌐 Browser Runtime Test\n');

// Check if server is running
const checkServer = () => {
    return new Promise((resolve, reject) => {
        const req = http.get('http://localhost:8080/index.html', (res) => {
            let data = '';
            res.on('data', chunk => data += chunk);
            res.on('end', () => {
                if (res.statusCode === 200) {
                    resolve(true);
                } else {
                    reject(new Error(`Server returned ${res.statusCode}`));
                }
            });
        });
        req.on('error', reject);
        req.setTimeout(2000, () => {
            req.destroy();
            reject(new Error('Server timeout'));
        });
    });
};

checkServer()
    .then(() => {
        console.log('✅ HTTP Server is running on http://localhost:8080');
        console.log('\n📋 Manual Test Instructions:');
        console.log('1. Open http://localhost:8080/index.html in your browser');
        console.log('2. Open browser console (F12)');
        console.log('3. Check for any JavaScript errors');
        console.log('4. Try clicking "➕ Dodaj Etykietę" button');
        console.log('5. Try creating a connection between labels');
        console.log('6. Verify that arrows appear at the end of connections');
        console.log('\n🔍 Quick Console Checks:');
        console.log('   - Check: typeof app !== "undefined"');
        console.log('   - Check: app.nodes.size');
        console.log('   - Check: app.connections.size');
        console.log('   - Check: document.querySelector("#arrowhead")');
    })
    .catch(err => {
        console.log('❌ Server check failed:', err.message);
        console.log('\n💡 Start server manually:');
        console.log('   cd design-system && python3 -m http.server 8080');
    });

// Try to use Puppeteer if available
try {
    const puppeteer = require('puppeteer');
    
    (async () => {
        console.log('\n🤖 Running automated browser test...\n');
        
        const browser = await puppeteer.launch({ headless: true });
        const page = await browser.newPage();
        
        // Set console listener
        page.on('console', msg => {
            const type = msg.type();
            const text = msg.text();
            if (type === 'error') {
                console.log(`❌ Console Error: ${text}`);
            } else if (type === 'warning') {
                console.log(`⚠️  Console Warning: ${text}`);
            }
        });
        
        // Listen for page errors
        page.on('pageerror', error => {
            console.log(`❌ Page Error: ${error.message}`);
        });
        
        try {
            await page.goto('http://localhost:8080/index.html', { 
                waitUntil: 'networkidle2',
                timeout: 5000 
            });
            
            console.log('✅ Page loaded successfully');
            
            // Check if app is initialized
            const appExists = await page.evaluate(() => {
                return typeof app !== 'undefined';
            });
            
            if (appExists) {
                console.log('✅ App object exists');
                
                // Check nodes and connections
                const stats = await page.evaluate(() => {
                    return {
                        nodes: app.nodes ? app.nodes.size : 0,
                        connections: app.connections ? app.connections.size : 0,
                        hasMarker: !!document.querySelector('#arrowhead')
                    };
                });
                
                console.log(`📊 Stats: ${stats.nodes} nodes, ${stats.connections} connections`);
                console.log(`🎯 Marker exists: ${stats.hasMarker ? '✅' : '❌'}`);
                
                // Try to add a node
                await page.click('#btn-add-node');
                await page.waitForTimeout(500);
                
                const statsAfter = await page.evaluate(() => {
                    return {
                        nodes: app.nodes ? app.nodes.size : 0
                    };
                });
                
                if (statsAfter.nodes > stats.nodes) {
                    console.log('✅ Add node button works');
                } else {
                    console.log('⚠️  Add node button may not be working');
                }
                
            } else {
                console.log('❌ App object not found');
            }
            
        } catch (error) {
            console.log(`❌ Error during test: ${error.message}`);
        }
        
        await browser.close();
        console.log('\n✅ Automated test complete');
    })();
    
} catch (e) {
    console.log('\n💡 Puppeteer not available - using manual testing');
    console.log('   Install with: npm install puppeteer');
}




