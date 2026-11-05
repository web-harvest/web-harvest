#!/usr/bin/env python3
import json
import time
import sys
import websocket

def on_message(ws, message):
    data = json.loads(message)
    print(f"📥 Received: {json.dumps(data, indent=2)}")
    
    if data.get('type') == 'modelUpdate':
        print("✅ Model update received!")
        if 'model' in data and 'workflow' in data['model']:
            root_nodes = data['model']['workflow'].get('rootNodes', [])
            print(f"📊 Workflow has {len(root_nodes)} root nodes")
            if root_nodes:
                print(f"   Root nodes: {[n.get('nodeId', 'N/A') for n in root_nodes]}")
    elif data.get('type') == 'actionResult':
        if data.get('success'):
            print(f"✅ Action succeeded! Node ID: {data.get('nodeId', 'N/A')}")
        else:
            print(f"❌ Action failed: {data.get('error', 'Unknown error')}")
        ws.close()
    elif data.get('type') == 'error':
        print(f"❌ Error: {data.get('error', 'Unknown error')}")
        if 'Actions must be executed via WebSocket' in str(data.get('error', '')):
            print("ℹ️  Note: Actions need to be executed via WebSocket (not REST API)")
        ws.close()

def on_error(ws, error):
    print(f"❌ WebSocket error: {error}")

def on_close(ws, close_status_code, close_msg):
    print("🔌 WebSocket closed")
    sys.exit(0)

def on_open(ws):
    print("✅ Connected to WebSocket as agent")
    
    # Wait for model snapshot
    print("⏳ Waiting for model snapshot...")
    time.sleep(2)
    
    # Add a simple HTTP node
    action = {
        "type": "action",
        "action": {
            "type": "addNode",
            "parameters": {
                "pluginType": "http",
                "position": {
                    "x": 200,
                    "y": 100
                },
                "attributes": {
                    "url": "https://example.com",
                    "method": "GET"
                }
            }
        }
    }
    
    print(f"📤 Sending action: {json.dumps(action, indent=2)}")
    ws.send(json.dumps(action))

if __name__ == "__main__":
    ws_url = "ws://localhost:8080/ws/visual/model?clientType=agent"
    print(f"🔌 Connecting to {ws_url}...")
    
    ws = websocket.WebSocketApp(
        ws_url,
        on_open=on_open,
        on_message=on_message,
        on_error=on_error,
        on_close=on_close
    )
    
    # Run for 15 seconds max
    ws.run_forever()
