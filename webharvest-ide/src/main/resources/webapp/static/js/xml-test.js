/**
 * Simple XML Conversion Tests
 * Run in browser console to verify XMLParser/XMLGenerator work
 */

async function testXMLConversion() {
    console.log('🧪 Starting XML Conversion Tests...');
    console.log('');

    // Test 1: Simple variable
    console.log('Test 1: Simple variable parsing');
    const xml1 = `<?xml version="1.0" encoding="UTF-8"?>
<config>
    <def var="test">hello</def>
</config>`;

    try {
        console.log('Input XML:', xml1);
        console.log('✅ Test 1 ready');
    } catch (e) {
        console.error('❌ Test 1 failed:', e);
    }

    console.log('');
    console.log('To run full tests:');
    console.log('1. XMLParser and XMLGenerator modules need to be loaded');
    console.log('2. PluginRegistry needs to be initialized');
    console.log('3. Then we can test parse/generate');
    console.log('');
    console.log('Next: Load these modules in switchViewMode()');
}

// Auto-run on page load (only in dev mode)
if (window.location.hostname === 'localhost') {
    console.log('Development mode detected');
    console.log('Run testXMLConversion() to test XML parsing');
}






