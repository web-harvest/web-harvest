#!/bin/bash

echo "🧪 Design System Runtime Check"
echo "=============================="
echo ""

# Check if server is running
if curl -s -o /dev/null -w "%{http_code}" http://localhost:8080/index.html | grep -q "200"; then
    echo "✅ HTTP Server is running on http://localhost:8080"
else
    echo "❌ HTTP Server is NOT running"
    echo "   Start with: cd design-system && python3 -m http.server 8080"
    exit 1
fi

echo ""
echo "📋 Test Results:"
echo ""

# Check HTML
echo "1. HTML Structure:"
if curl -s http://localhost:8080/index.html | grep -q 'id="connections-svg"'; then
    echo "   ✅ SVG element found"
else
    echo "   ❌ SVG element NOT found"
fi

if curl -s http://localhost:8080/index.html | grep -q 'id="arrowhead"'; then
    echo "   ✅ Marker definition found"
else
    echo "   ❌ Marker definition NOT found"
fi

# Check JavaScript files
echo ""
echo "2. JavaScript Files:"
for file in app.js connection-router.js collision-detector.js; do
    status=$(curl -s -o /dev/null -w "%{http_code}" "http://localhost:8080/$file")
    if [ "$status" = "200" ]; then
        size=$(curl -s "http://localhost:8080/$file" | wc -c)
        echo "   ✅ $file accessible ($size bytes)"
    else
        echo "   ❌ $file NOT accessible (HTTP $status)"
    fi
done

# Check CSS
echo ""
echo "3. CSS File:"
status=$(curl -s -o /dev/null -w "%{http_code}" "http://localhost:8080/styles.css")
if [ "$status" = "200" ]; then
    echo "   ✅ styles.css accessible"
else
    echo "   ❌ styles.css NOT accessible (HTTP $status)"
fi

echo ""
echo "=============================="
echo "✅ Runtime check complete!"
echo ""
echo "🌐 Open in browser: http://localhost:8080/index.html"
echo "📝 Open console (F12) and check for errors"
echo ""




