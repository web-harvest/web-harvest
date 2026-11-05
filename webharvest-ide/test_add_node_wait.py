#!/usr/bin/env python3
import json
import time
import sys
import websocket

model_received = False
action_sent = False

def on_message(ws, message):
    global model_received, action_sent
    data = json.loads(message)
    
    if data.get('type') == 'modelUpdate':
        model_received = True
        print(f"✅ Model update received!")
        if 'model' in data and 'workflow' in data['model']:
            root_nodes = data['model']['workflow'].get('rootNodes', [])
            print(f"📊 Workflow has {len(root_nodes)} root nodes")
            if root_nodes:
                print(f"   Root nodes: {[n.get('nodeId', 'N/A') for n in root_nodes]}")
        
        # Now we can send action
        if not action_sent:
            action_sent = True
            time.sleep(1)  # Wait a bit
            
            action = {
                "type": "action",
                "action": {
                    "type": "addNode",
                    "parameters": {
                        "pluginType": "http",
                        "position": {"x": 300, "y": 200},
                        "attributes": {
                            "url": "https://example.com",
                            "method": "GET"
                        }
                    }
                }
            }
            
            print(f"\n📤 Sending action to add HTTP node...")
            print(f"   {json.dumps(action, indent=2)}")
            ws.send(json.dumps(action))
    
    elif data.get('type') == 'actionResult':
        if data.get('success'):
            print(f"\n✅✅✅ Action succeeded! Node ID: {data.get('nodeId', 'N/A')}")
            print(f"   You should see the HTTP node appear on the canvas in your browser!")
        else:
            print(f"\n❌ Action failed: {data.get('error', 'Unknown error')}")
        ws.close()
    
    elif data.get('type') == 'error':
        error_msg = data.get('error', 'Unknown error')
        if 'Model not available yet' in error_msg:
            print(f"⚠️  {error_msg}")
            print("   Please open Visual Designer in browser first!")
            print("   Go to: http://localhost:8080")
            print("   Then switch to Visual mode")
        else:
            print(f"❌ Error: {error_msg}")
        ws.close()

def on_error(ws, error):
    print(f"❌ WebSocket error: {error}")

def on_close(ws, close_status_code, close_msg):
    print("\n🔌 WebSocket closed")
    if not model_received:
        print("\n💡 Tip: Open Visual Designer in browser first:")
        print("   1. Go to http://localhost:8080")
        print("   2. Switch to Visual mode")
        print("   3. Then run this script again")
    sys.exit(0)

def on_open(ws):
    print("✅ Connected to WebSocket as agent")
    print("⏳ Waiting for model from frontend...")
    print("   (Make sure Visual Designer is open in browser)")

if __name__ == "__main__":
    ws_url = "ws://localhost:8080/ws/visual/model?clientType=agent"
    print(f"🔌 Connecting to {ws_url}...\n")
    
    ws = websocket.WebSocketApp(
        ws_url,
        on_open=on_open,
        on_message=on_message,
        on_error=on_error,
        on_close=on_close
    )
    
    ws.run_forever()
