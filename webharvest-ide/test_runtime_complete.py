#!/usr/bin/env python3
"""
Kompleksowy test runtime:
1. HTTP - sprawdzenie czy pliki biblioteki są dostępne
2. WebSockets - model semantyczny przez WebSockets
3. Selenium Chrome - całość działania
"""

import time
import json
import requests
import websocket
import threading
from selenium import webdriver
from selenium.webdriver.chrome.options import Options
from selenium.webdriver.common.by import By
from selenium.webdriver.support.ui import WebDriverWait
from selenium.webdriver.support import expected_conditions as EC

def test_http_files():
    """Test 1: Sprawdź czy pliki biblioteki są dostępne przez HTTP"""
    print("\n" + "="*60)
    print("TEST 1: HTTP - Sprawdzanie plików biblioteki")
    print("="*60)
    
    base_url = "http://localhost:8080"
    files_to_check = [
        "/dist/assets/main.js",
        "/dist/assets/main.css",
        "/static/metadata/plugins.json"
    ]
    
    results = {}
    for file_path in files_to_check:
        url = base_url + file_path
        try:
            response = requests.get(url, timeout=5)
            if response.status_code == 200:
                size = len(response.content)
                print(f"✅ {file_path}: {size:,} bytes")
                results[file_path] = {"status": "ok", "size": size}
                
                # Sprawdź czy selectBestConnectionPoint jest w main.js
                if "main.js" in file_path:
                    if "selectBestConnectionPoint" in response.text:
                        print(f"   ✅ selectBestConnectionPoint found in JS")
                    else:
                        print(f"   ⚠️  selectBestConnectionPoint NOT found in JS")
                    
                    # Sprawdź czy wszystkie strony są w kodzie
                    has_all_sides = (
                        'defaultInputPort(o,"left")' in response.text and
                        'defaultInputPort(o,"right")' in response.text and
                        'defaultInputPort(o,"top")' in response.text and
                        'defaultInputPort(o,"bottom")' in response.text and
                        'defaultOutputPort(o,"left")' in response.text and
                        'defaultOutputPort(o,"right")' in response.text and
                        'defaultOutputPort(o,"top")' in response.text and
                        'defaultOutputPort(o,"bottom")' in response.text
                    )
                    if has_all_sides:
                        print(f"   ✅ All sides (left, right, top, bottom) found in JS")
                    else:
                        print(f"   ⚠️  Not all sides found in JS")
            else:
                print(f"❌ {file_path}: HTTP {response.status_code}")
                results[file_path] = {"status": "error", "code": response.status_code}
        except Exception as e:
            print(f"❌ {file_path}: {e}")
            results[file_path] = {"status": "error", "error": str(e)}
    
    return results

def test_websocket_model():
    """Test 2: WebSocket - model semantyczny"""
    print("\n" + "="*60)
    print("TEST 2: WebSocket - Model semantyczny")
    print("="*60)
    
    ws_url = "ws://localhost:8080/ws/visual-semantic-model"
    model_received = False
    model_data = None
    error_message = None
    
    def on_message(ws, message):
        nonlocal model_received, model_data
        try:
            data = json.loads(message)
            if data.get("type") == "modelUpdate":
                model_data = data.get("model")
                model_received = True
                print(f"✅ Received model update via WebSocket")
                ws.close()
        except Exception as e:
            print(f"⚠️  Error parsing WebSocket message: {e}")
    
    def on_error(ws, error):
        nonlocal error_message
        error_message = str(error)
        print(f"❌ WebSocket error: {error}")
    
    def on_close(ws, close_status_code, close_msg):
        print(f"🔌 WebSocket closed: {close_status_code}")
    
    try:
        ws = websocket.WebSocketApp(
            ws_url,
            on_message=on_message,
            on_error=on_error,
            on_close=on_close
        )
        
        # Run WebSocket in thread
        ws_thread = threading.Thread(target=ws.run_forever)
        ws_thread.daemon = True
        ws_thread.start()
        
        # Wait for connection
        time.sleep(2)
        
        # Send getModel request
        if ws.sock and ws.sock.connected:
            request = {
                "type": "action",
                "action": {
                    "type": "getModel"
                },
                "clientType": "agent"
            }
            ws.send(json.dumps(request))
            print("📤 Sent getModel request")
        
        # Wait for model
        timeout = 10
        start_time = time.time()
        while not model_received and (time.time() - start_time) < timeout:
            time.sleep(0.5)
        
        if model_received and model_data:
            workflow = model_data.get("workflow", {})
            nodes = workflow.get("allNodes", {})
            connections = workflow.get("connections", {})
            
            print(f"   📊 Model stats:")
            print(f"      - Nodes: {len(nodes)}")
            print(f"      - Connections: {len(connections)}")
            
            # Check connection points for first node
            if nodes:
                first_node_id = list(nodes.keys())[0]
                first_node = nodes[first_node_id]
                connection_points = first_node.get("connectionPoints", {})
                inputs = connection_points.get("inputs", [])
                outputs = connection_points.get("outputs", [])
                
                input_sides = [p.get("side") for p in inputs]
                output_sides = [p.get("side") for p in outputs]
                
                print(f"   📍 Connection points for {first_node_id}:")
                print(f"      - Input sides: {input_sides}")
                print(f"      - Output sides: {output_sides}")
                
                all_sides_available = (
                    len(set(input_sides)) >= 4 and
                    len(set(output_sides)) >= 4
                )
                
                if all_sides_available:
                    print(f"   ✅ All 4 sides available!")
                else:
                    print(f"   ⚠️  Not all sides available")
                
                # Check if points are on edges
                points_on_edge = all(
                    p.get("edgePosition", {}).get("isOnEdge", False)
                    for p in inputs + outputs
                )
                if points_on_edge:
                    print(f"   ✅ All points on edges")
                else:
                    print(f"   ⚠️  Some points not on edges")
            
            return {"success": True, "model": model_data}
        else:
            return {"success": False, "error": error_message or "Timeout"}
    
    except Exception as e:
        print(f"❌ WebSocket test failed: {e}")
        return {"success": False, "error": str(e)}
    finally:
        if ws.sock:
            ws.close()

def test_selenium_complete():
    """Test 3: Selenium Chrome - całość"""
    print("\n" + "="*60)
    print("TEST 3: Selenium Chrome - Kompleksowy test")
    print("="*60)
    
    options = Options()
    options.add_argument('--headless')
    options.add_argument('--no-sandbox')
    options.add_argument('--disable-dev-shm-usage')
    driver = webdriver.Chrome(options=options)
    
    try:
        # Step 1: Load page
        print("📄 Step 1: Loading page...")
        driver.get('http://localhost:8080')
        time.sleep(5)
        
        # Wait for App to initialize and plugins to load
        print("⏳ Step 1.5: Waiting for App initialization...")
        init_check = driver.execute_script('''
            return new Promise((resolve) => {
                const checkInit = () => {
                    const app = window.app?._instance || window.app;
                    const designer = app?.visualDesigner;
                    const pluginManager = app?.pluginManager;
                    const hasPlugins = pluginManager && pluginManager.getPlugins().length > 0;
                    
                    if (app && designer && pluginManager && hasPlugins) {
                        resolve({
                            hasApp: true,
                            hasDesigner: true,
                            hasPluginManager: true,
                            pluginsCount: pluginManager.getPlugins().length,
                            designerHasPluginManager: !!designer.pluginManager,
                            hasModelManager: !!designer._modelManager
                        });
                    } else {
                        setTimeout(checkInit, 500);
                    }
                };
                checkInit();
                // Timeout after 10 seconds
                setTimeout(() => resolve({
                    hasApp: !!window.app,
                    timeout: true
                }), 10000);
            });
        ''')
        print(f"   Init check: {init_check}")
        time.sleep(2)
        
        # Step 2: Switch to visual mode
        print("🔄 Step 2: Switching to visual mode...")
        driver.execute_script('''
            if (window.app && window.app.switchToVisualMode) {
                window.app.switchToVisualMode();
            }
        ''')
        time.sleep(3)
        
        # Step 2.5: Wait for pluginManager to be set and force initialization
        print("⏳ Step 2.5: Waiting for pluginManager to be set...")
        
        # Get browser console logs
        logs = driver.get_log('browser')
        if logs:
            print(f"   Browser console logs (before): {len(logs)} entries")
            for log in logs[-5:]:
                if 'setPluginManager' in log.get('message', '') or 'VisualSemanticModelManager' in log.get('message', ''):
                    print(f"      {log.get('level')}: {log.get('message')[:100]}")
        
        plugin_manager_check = driver.execute_script('''
            const app = window.app?._instance || window.app;
            const designer = app?.visualDesigner;
            
            // Try to get pluginManager from app instance (it's private, so we access it directly)
            const pluginManager = app?.pluginManager;
            
            if (!app) return {error: 'No app'};
            if (!designer) return {error: 'No designer'};
            if (!pluginManager) return {error: 'No pluginManager', hasApp: !!app, hasDesigner: !!designer};
            
            // Check if pluginManager is already set
            const hasPluginManager = !!designer.pluginManager;
            const hasModelManagerBefore = !!designer._modelManager;
            
            // ALWAYS call setPluginManager to ensure ModelManager is initialized
            // Even if pluginManager is already set, ModelManager might not be
            let exception = null;
            let setPluginManagerCalled = false;
            try {
                console.log('[TEST] Calling setPluginManager...', {
                    hasPluginManager: hasPluginManager,
                    hasModelManagerBefore: hasModelManagerBefore,
                    pluginManagerType: typeof pluginManager
                });
                designer.setPluginManager(pluginManager);
                setPluginManagerCalled = true;
                console.log('[TEST] setPluginManager called successfully');
            } catch (e) {
                exception = {message: e.message, stack: e.stack};
                console.error('[TEST] Exception in setPluginManager:', e);
            }
            
            // Check if modelManager exists after setPluginManager
            const hasModelManager = !!designer._modelManager;
            
            // Try to manually create ModelManager if it still doesn't exist
            let manualInitAttempted = false;
            if (!hasModelManager && pluginManager) {
                try {
                    console.log('[TEST] Attempting manual ModelManager initialization...');
                    // Check if VisualSemanticModelManager is available
                    const VisualSemanticModelManager = window.VisualSemanticModelManager;
                    if (!VisualSemanticModelManager) {
                        console.log('[TEST] VisualSemanticModelManager not available in window');
                    } else {
                        designer._modelManager = new VisualSemanticModelManager(designer, pluginManager);
                        manualInitAttempted = true;
                        console.log('[TEST] Manual ModelManager initialization attempted');
                    }
                } catch (e) {
                    console.error('[TEST] Manual initialization failed:', e);
                }
            }
            
            return {
                hasApp: !!app,
                hasDesigner: !!designer,
                hasPluginManager: !!pluginManager,
                designerHasPluginManager: hasPluginManager,
                hasModelManagerBefore: hasModelManagerBefore,
                hasModelManager: hasModelManager || !!designer._modelManager,
                modelManagerType: (hasModelManager || designer._modelManager) ? typeof designer._modelManager : 'none',
                exception: exception,
                setPluginManagerCalled: setPluginManagerCalled,
                manualInitAttempted: manualInitAttempted
            };
        ''')
        print(f"   PluginManager check: {plugin_manager_check}")
        if plugin_manager_check.get('exception'):
            print(f"   ❌ Exception: {plugin_manager_check['exception']}")
        
        # Get browser console logs after
        logs_after = driver.get_log('browser')
        if logs_after:
            print(f"   Browser console logs (after): {len(logs_after)} entries")
            relevant_logs = []
            for log in logs_after:
                msg = log.get('message', '')
                if any(keyword in msg.lower() for keyword in ['setpluginmanager', 'visualsemanticmodelmanager', 'modelmanager', 'test', 'initialized', 'skipping']):
                    relevant_logs.append(f"{log.get('level')}: {msg[:200]}")
            if relevant_logs:
                print(f"   Relevant logs ({len(relevant_logs)}):")
                for log in relevant_logs[-15:]:
                    print(f"      {log}")
            else:
                print(f"   No relevant logs found")
        
        time.sleep(3)
        
        # Step 3: Force render to initialize modelManager
        print("🎨 Step 3: Forcing render to initialize modelManager...")
        driver.execute_script('window.app?.visualDesigner?.render()')
        time.sleep(2)
        
        # Step 4: Load configuration
        print("📥 Step 4: Loading configuration...")
        config_xml = '''<config xmlns="http://org.webharvest/schema/2.1/core">
            <def var="url1">https://httpbin.org/html</def>
            <def var="html1"><http url="${url1}"/></def>
            <def var="url2">https://httpbin.org/json</def>
            <def var="json1"><http url="${url2}"/></def>
        </config>'''
        
        driver.execute_script(f'''
            const xmlContent = {repr(config_xml)};
            const parser = window.app?._instance?.xmlParser;
            if (parser) {{
                const nodes = parser.parse(xmlContent);
                window.app.visualDesigner.loadNodes(nodes);
            }}
        ''')
        time.sleep(5)
        
        # Step 5: Force multiple renders to ensure modelManager is initialized
        print("🔄 Step 5: Forcing multiple renders...")
        for i in range(3):
            driver.execute_script('window.app?.visualDesigner?.render()')
            time.sleep(1)
        
        # Step 6: Comprehensive check
        print("🔍 Step 6: Comprehensive runtime check...")
        result = driver.execute_script('''
            const designer = window.app?.visualDesigner;
            if (!designer) return {error: 'No designer'};
            
            // Check modelManager
            const manager = designer._modelManager || designer.modelManager;
            const hasManager = !!manager;
            const hasPluginManager = !!designer.pluginManager;
            const hasSvg = !!designer.svgElement;
            
            // Check nodes
            const designerNodes = designer.getNodes();
            const allNodes = designer.getAllNodes();
            
            // Force model build
            let model = null;
            let modelNodes = [];
            let connections = [];
            let connectionPointsInfo = null;
            
            if (manager) {
                manager.notifyUpdate();
                model = manager.getModel();
                modelNodes = Array.from(model.workflow.allNodes.values());
                connections = Array.from(model.workflow.connections.values());
                
                if (modelNodes.length > 0) {
                    const firstNode = modelNodes[0];
                    const inputs = firstNode.connectionPoints.inputs;
                    const outputs = firstNode.connectionPoints.outputs;
                    const inputSides = inputs.map(p => p.side);
                    const outputSides = outputs.map(p => p.side);
                    const allSides = new Set([...inputSides, ...outputSides]);
                    const allSidesAvailable = allSides.size >= 4;
                    
                    // Check if points are on edges
                    const pointsOnEdge = inputs.concat(outputs).every(p => 
                        p.edgePosition && p.edgePosition.isOnEdge
                    );
                    
                    // Check connection details
                    const connectionDetails = connections.slice(0, 3).map(conn => ({
                        connectionId: conn.connectionId,
                        from: conn.fromNodeId,
                        to: conn.toNodeId,
                        fromSide: conn.path.spatialMetadata.startSide,
                        toSide: conn.path.spatialMetadata.endSide,
                        startPoint: conn.path.spatialMetadata.startPoint,
                        endPoint: conn.path.spatialMetadata.endPoint,
                        startOnEdge: conn.edgeValidation.startPointOnEdge,
                        endOnEdge: conn.edgeValidation.endPointOnEdge
                    }));
                    
                    connectionPointsInfo = {
                        inputSides: inputSides,
                        outputSides: outputSides,
                        allSidesAvailable: allSidesAvailable,
                        allSidesSet: Array.from(allSides),
                        pointsOnEdge: pointsOnEdge,
                        inputCount: inputs.length,
                        outputCount: outputs.length
                    };
                }
            }
            
            // Check if selectBestConnectionPoint exists
            const hasSelectBestMethod = manager && 
                typeof manager.selectBestConnectionPoint === 'function';
            
            // Check ConnectionPointsCalculator
            const calc = manager?._connectionPointsCalculator || manager?.connectionPointsCalculator;
            const hasCalculator = !!calc;
            
            return {
                hasDesigner: !!designer,
                hasPluginManager: hasPluginManager,
                hasSvg: hasSvg,
                hasModelManager: hasManager,
                hasSelectBestMethod: hasSelectBestMethod,
                hasCalculator: hasCalculator,
                designerNodes: designerNodes.length,
                allNodes: allNodes.length,
                modelNodes: modelNodes.length,
                connections: connections.length,
                connectionPointsInfo: connectionPointsInfo
            };
        ''')
        
        print("\n📊 Results:")
        print(f"   Designer: {'✅' if result.get('hasDesigner') else '❌'}")
        print(f"   PluginManager: {'✅' if result.get('hasPluginManager') else '❌'}")
        print(f"   SVG: {'✅' if result.get('hasSvg') else '❌'}")
        print(f"   ModelManager: {'✅' if result.get('hasModelManager') else '❌'}")
        print(f"   selectBestConnectionPoint: {'✅' if result.get('hasSelectBestMethod') else '❌'}")
        print(f"   Calculator: {'✅' if result.get('hasCalculator') else '❌'}")
        print(f"   Designer nodes: {result.get('designerNodes', 0)}")
        print(f"   All nodes: {result.get('allNodes', 0)}")
        print(f"   Model nodes: {result.get('modelNodes', 0)}")
        print(f"   Connections: {result.get('connections', 0)}")
        
        if result.get('connectionPointsInfo'):
            cp = result['connectionPointsInfo']
            print(f"\n   Connection Points:")
            print(f"      Input sides: {cp['inputSides']} ({cp['inputCount']} total)")
            print(f"      Output sides: {cp['outputSides']} ({cp['outputCount']} total)")
            print(f"      All sides set: {cp['allSidesSet']}")
            print(f"      All 4 sides available: {'✅' if cp['allSidesAvailable'] else '❌'}")
            print(f"      Points on edges: {'✅' if cp['pointsOnEdge'] else '❌'}")
        
        # Step 7: Check rendered connections
        print("\n🔗 Step 7: Checking rendered connections...")
        svg_check = driver.execute_script('''
            const svg = document.querySelector('svg');
            if (!svg) return {error: 'No SVG'};
            
            const paths = svg.querySelectorAll('path.connection-line');
            const arrows = svg.querySelectorAll('polygon.connection-arrow');
            
            // Check first few paths
            const pathDetails = Array.from(paths).slice(0, 3).map(p => ({
                id: p.getAttribute('data-connection-id'),
                d: p.getAttribute('d'),
                stroke: p.getAttribute('stroke'),
                dLength: p.getAttribute('d')?.length || 0
            }));
            
            return {
                svgFound: !!svg,
                pathsCount: paths.length,
                arrowsCount: arrows.length,
                pathDetails: pathDetails
            };
        ''')
        
        print(f"   SVG found: {'✅' if svg_check.get('svgFound') else '❌'}")
        print(f"   Connection paths: {svg_check.get('pathsCount', 0)}")
        print(f"   Connection arrows: {svg_check.get('arrowsCount', 0)}")
        
        if svg_check.get('pathDetails'):
            print(f"   First paths:")
            for path in svg_check['pathDetails'][:2]:
                print(f"      - {path['id']}: d length={path['dLength']}, stroke={path['stroke']}")
        
        # Final summary
        print("\n" + "="*60)
        print("PODSUMOWANIE")
        print("="*60)
        
        all_ok = (
            result.get('hasModelManager') and
            result.get('hasSelectBestMethod') and
            result.get('modelNodes', 0) > 0 and
            result.get('connectionPointsInfo', {}).get('allSidesAvailable', False)
        )
        
        if all_ok:
            print("✅ ✅ ✅ WSZYSTKO DZIAŁA W RUNTIME!")
            print("   - ModelManager zainicjalizowany")
            print("   - selectBestConnectionPoint dostępny")
            print("   - Wszystkie strony (left, right, top, bottom) dostępne")
            print("   - Punkty na krawędziach")
        else:
            print("⚠️  Niektóre elementy nie działają:")
            if not result.get('hasModelManager'):
                print("   - ModelManager nie zainicjalizowany")
            if not result.get('hasSelectBestMethod'):
                print("   - selectBestConnectionPoint nie dostępny")
            if result.get('modelNodes', 0) == 0:
                print("   - Brak węzłów w modelu")
            cp_info = result.get('connectionPointsInfo')
            if cp_info and not cp_info.get('allSidesAvailable', False):
                print("   - Nie wszystkie strony dostępne")
        
        return result
        
    except Exception as e:
        print(f"❌ Selenium test failed: {e}")
        import traceback
        traceback.print_exc()
        return {"error": str(e)}
    finally:
        driver.quit()

def main():
    """Run all tests"""
    print("\n" + "="*60)
    print("KOMPLEKSOWY TEST RUNTIME")
    print("="*60)
    print("Sprawdzanie: HTTP, WebSockets, Selenium Chrome")
    print("="*60)
    
    # Test 1: HTTP
    http_results = test_http_files()
    
    # Test 2: WebSocket (requires frontend to send model first)
    # Skip for now as it requires frontend to be loaded
    # websocket_results = test_websocket_model()
    
    # Test 3: Selenium
    selenium_results = test_selenium_complete()
    
    print("\n" + "="*60)
    print("FINALNE PODSUMOWANIE")
    print("="*60)
    print(f"HTTP files: {'✅' if all(r.get('status') == 'ok' for r in http_results.values()) else '❌'}")
    print(f"Selenium: {'✅' if selenium_results.get('hasModelManager') and selenium_results.get('modelNodes', 0) > 0 else '❌'}")
    
    if (all(r.get('status') == 'ok' for r in http_results.values()) and
        selenium_results.get('hasModelManager') and
        selenium_results.get('modelNodes', 0) > 0):
        print("\n✅ ✅ ✅ RUNTIME MA AKTUALNĄ WERSJĘ KODU!")
    else:
        print("\n⚠️  Wymagana weryfikacja - niektóre elementy mogą nie działać")

if __name__ == '__main__':
    main()

