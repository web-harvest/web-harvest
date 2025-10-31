#!/bin/bash
set -e

echo "🔧 WebHarvest IDE - Quick Build & Start"
echo "========================================"
echo ""

cd /Users/robertbala/Documents/Git/web-harvest-git

# Kill old instance
echo "🛑 Stopping old IDE instance..."
pkill -9 -f "webharvest-ide" 2>/dev/null || true
sleep 1

# Build
echo ""
echo "🏗️  Building project (this may take 2-3 minutes)..."
mvn clean package -DskipTests -pl webharvest-ide -am

# Start
echo ""
echo "🚀 Starting IDE..."
nohup java -jar webharvest-ide/target/webharvest-ide-2.3.1.jar > /tmp/ide.log 2>&1 &
sleep 5

echo ""
echo "✅ Build & Start Complete!"
echo ""
echo "📊 Summary:"
echo "  • PropertyPanel now uses advanced editors"
echo "  • SchemaEditor for smart field types"
echo "  • XPath Tester, Regex Validator, Expression Builder"
echo "  • Modal interfaces for advanced editing"
echo ""
echo "🌐 Open: http://localhost:8080"
echo "🔄 Hard Refresh: Cmd+Shift+R (IMPORTANT!)"
echo ""
echo "🧪 Test Steps:"
echo "  1. Switch to Visual mode"
echo "  2. Drag HTTP plugin to canvas"
echo "  3. Click the node"
echo "  4. Look for '🔧' buttons in Properties panel"
echo "  5. Click button to open advanced editor modal"
echo ""
echo "📝 Logs: tail -f /tmp/ide.log"

