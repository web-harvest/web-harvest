// Simple runtime test for design-system
const fs = require('fs');
const path = require('path');

console.log('🧪 Testing design-system runtime...\n');

// Check if files exist
const files = ['index.html', 'app.js', 'styles.css', 'connection-router.js', 'collision-detector.js'];
files.forEach(file => {
    const filePath = path.join(__dirname, file);
    if (fs.existsSync(filePath)) {
        const stats = fs.statSync(filePath);
        console.log(`✅ ${file} exists (${stats.size} bytes)`);
    } else {
        console.log(`❌ ${file} NOT FOUND`);
    }
});

// Check for syntax errors in app.js
console.log('\n📝 Checking JavaScript syntax...');
try {
    const appJs = fs.readFileSync(path.join(__dirname, 'app.js'), 'utf8');
    
    // Check for common issues
    const issues = [];
    
    // Check for duplicate const declarations in same scope
    const constMatches = appJs.match(/const\s+(\w+)\s*=/g);
    if (constMatches) {
        const constVars = constMatches.map(m => m.match(/const\s+(\w+)/)[1]);
        const duplicates = constVars.filter((v, i) => constVars.indexOf(v) !== i);
        if (duplicates.length > 0) {
            // Check if they're in the same function scope
            const lines = appJs.split('\n');
            const varPositions = {};
            lines.forEach((line, idx) => {
                duplicates.forEach(dup => {
                    if (line.includes(`const ${dup}`)) {
                        if (!varPositions[dup]) varPositions[dup] = [];
                        varPositions[dup].push(idx + 1);
                    }
                });
            });
            
            Object.keys(varPositions).forEach(varName => {
                if (varPositions[varName].length > 1) {
                    issues.push(`⚠️  Variable '${varName}' declared multiple times at lines: ${varPositions[varName].join(', ')}`);
                }
            });
        }
    }
    
    // Check for basic syntax issues
    if (appJs.includes('const const ') || appJs.includes('let let ') || appJs.includes('var var ')) {
        issues.push('⚠️  Found duplicate declaration keywords');
    }
    
    if (issues.length === 0) {
        console.log('✅ No obvious syntax issues found');
    } else {
        console.log('⚠️  Potential issues:');
        issues.forEach(issue => console.log(`   ${issue}`));
    }
    
    // Try to parse with eval (basic check)
    try {
        // Just check if it can be parsed, don't execute
        new Function(appJs);
        console.log('✅ JavaScript syntax is valid');
    } catch (e) {
        console.log(`❌ JavaScript syntax error: ${e.message}`);
    }
    
} catch (e) {
    console.log(`❌ Error reading app.js: ${e.message}`);
}

// Check HTML structure
console.log('\n📄 Checking HTML structure...');
try {
    const html = fs.readFileSync(path.join(__dirname, 'index.html'), 'utf8');
    
    const checks = [
        { name: 'SVG element', test: html.includes('<svg id="connections-svg"') },
        { name: 'Marker definition', test: html.includes('id="arrowhead"') },
        { name: 'App.js script', test: html.includes('app.js') },
        { name: 'Canvas container', test: html.includes('id="canvas-container"') },
    ];
    
    checks.forEach(check => {
        if (check.test) {
            console.log(`✅ ${check.name} found`);
        } else {
            console.log(`❌ ${check.name} NOT FOUND`);
        }
    });
    
} catch (e) {
    console.log(`❌ Error reading index.html: ${e.message}`);
}

console.log('\n✅ Runtime check complete!');
console.log('💡 To test in browser, open: http://localhost:8080/index.html');




