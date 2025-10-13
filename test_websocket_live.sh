#!/bin/bash

echo "🧪 Testing LIVE IDE WebSocket on port 8080..."
echo ""

# Step 1: POST /api/execute
echo "1️⃣  Sending POST request..."
RESPONSE=$(curl -s -X POST http://localhost:8080/api/execute \
  -H "Content-Type: application/json" \
  -d '{
    "configName": "live-test.xml",
    "configXml": "<config xmlns=\"http://org.webharvest/schema/2.1/core\"><def var=\"test\">Live Test</def></config>"
  }')

echo "Response: $RESPONSE"
EXECUTION_ID=$(echo $RESPONSE | grep -o '"executionId":"[^"]*"' | cut -d'"' -f4)
echo "ExecutionId: $EXECUTION_ID"
echo ""

# Step 2: Connect WebSocket using wscat
echo "2️⃣  Connecting WebSocket..."
echo "ws://localhost:8080/ws/execution?id=$EXECUTION_ID"
echo ""
echo "Install wscat if needed: npm install -g wscat"
echo "Then run: wscat -c \"ws://localhost:8080/ws/execution?id=$EXECUTION_ID\""


