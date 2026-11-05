#!/usr/bin/env python3
"""
Test Semantic Model Connection Point Selection
Sprawdza czy model semantyczny poprawnie decyduje o wyborze strony connection point
na podstawie wszystkich elementów i powiązań w przestrzeni.

Test weryfikuje:
1. Czy semanticSelection metadata jest ustawiana w connection points
2. Czy wybór strony jest oparty na analizie przestrzennej
3. Czy warstwa wizualna renderuje zgodnie z decyzjami modelu
4. Czy WebSocket synchronizuje model z backendem
"""

import json
import time
import sys
import websocket
from selenium import webdriver
from selenium.webdriver.chrome.options import Options
from selenium.webdriver.common.by import By
from selenium.webdriver.support.ui import WebDriverWait
from selenium.webdriver.support import expected_conditions as EC
from selenium.common.exceptions import TimeoutException

# Configuration
IDE_URL = "http://localhost:8080"
WS_URL = "ws://localhost:8080/ws/visual/model?clientType=agent"

TEST_CONFIG_XML = """<config xmlns="http://org.webharvest/schema/2.1/core">
    <def var="url1">https://httpbin.org/html</def>
    <def var="html1">
        <http url="${url1}"/>
    </def>
    <def var="url2">https://httpbin.org/json</def>
    <def var="json2">
        <http url="${url2}"/>
    </def>
    <def var="url3">https://httpbin.org/xml</def>
    <def var="xml3">
        <http url="${url3}"/>
    </def>
</config>"""

class WebSocketClient:
    """WebSocket client for testing semantic model updates"""
    def __init__(self, url):
        self.url = url
        self.ws = None
        self.received_messages = []
        self.connected = False
        
    def connect(self):
        """Connect to WebSocket"""
        try:
            self.ws = websocket.WebSocketApp(
                self.url,
                on_open=self.on_open,
                on_message=self.on_message,
                on_error=self.on_error,
                on_close=self.on_close
            )
            # Run in separate thread
            import threading
            self.ws_thread = threading.Thread(target=self.ws.run_forever)
            self.ws_thread.daemon = True
            self.ws_thread.start()
            
            # Wait for connection
            timeout = 5
            start = time.time()
            while not self.connected and (time.time() - start) < timeout:
                time.sleep(0.1)
            
            if not self.connected:
                raise Exception("WebSocket connection timeout")
                
        except Exception as e:
            print(f"❌ WebSocket connection error: {e}")
            raise
            
    def on_open(self, ws):
        """WebSocket opened"""
        print("✅ WebSocket connected")
        self.connected = True
        
    def on_message(self, ws, message):
        """Receive message from WebSocket"""
        try:
            data = json.loads(message)
            self.received_messages.append(data)
            print(f"📨 Received WebSocket message: {data.get('type', 'unknown')}")
        except Exception as e:
            print(f"⚠️  Error parsing WebSocket message: {e}")
            
    def on_error(self, ws, error):
        """WebSocket error"""
        print(f"❌ WebSocket error: {error}")
        
    def on_close(self, ws, close_status_code, close_msg):
        """WebSocket closed"""
        print("🔌 WebSocket closed")
        self.connected = False
        
    def disconnect(self):
        """Disconnect WebSocket"""
        if self.ws:
            self.ws.close()
            
    def get_model_updates(self):
        """Get all model update messages"""
        return [msg for msg in self.received_messages if msg.get('type') == 'modelUpdate']

def setup_driver():
    """Setup Chrome driver"""
    options = Options()
    options.add_argument('--no-sandbox')
    options.add_argument('--disable-dev-shm-usage')
    options.add_argument('--window-size=1920,1080')
    # Don't run headless so we can see what's happening
    # options.add_argument('--headless')
    
    driver = webdriver.Chrome(options=options)
    return driver

def load_config_via_selenium(driver, config_xml):
    """Load configuration via Selenium"""
    try:
        print("📝 Loading configuration via Selenium...")
        driver.get(f"{IDE_URL}?nocache={int(time.time())}")
        time.sleep(3)
        
        # Switch to visual mode
        wait = WebDriverWait(driver, 15)
        try:
            # Try to find and click visual mode button
            visual_button = wait.until(
                EC.element_to_be_clickable((By.CSS_SELECTOR, "button[data-mode='visual'], .visual-mode-button, #visual-mode-btn"))
            )
            visual_button.click()
            time.sleep(2)
        except TimeoutException:
            # Try JavaScript approach
            driver.execute_script('''
                if (window.app && window.app.switchToVisualMode) {
                    window.app.switchToVisualMode();
                } else if (window.app?._instance?.switchToVisualMode) {
                    window.app._instance.switchToVisualMode();
                }
            ''')
            time.sleep(2)
        
        # Load XML configuration
        driver.execute_script(f'''
            const xmlContent = {repr(config_xml)};
            const parser = window.app?._instance?.xmlParser || window.app?.xmlParser;
            if (parser) {{
                const nodes = parser.parse(xmlContent);
                const designer = window.app?._instance?.visualDesigner || window.app?.visualDesigner;
                if (designer) {{
                    designer.loadNodes(nodes);
                    designer.render();
                }}
            }}
        ''')
        
        time.sleep(5)  # Wait for rendering
        
        print("✅ Configuration loaded")
        return True
        
    except Exception as e:
        print(f"❌ Error loading config: {e}")
        return False

def check_semantic_selection_metadata(driver):
    """Check if semantic selection metadata is properly set in connection points"""
    print("\n🔍 Checking semantic selection metadata...")
    
    result = driver.execute_script('''
        const designer = window.app?._instance?.visualDesigner || window.app?.visualDesigner;
        if (!designer || !designer.modelManager) {
            return {error: 'No model manager'};
        }
        
        // Enable debug logging
        window.__DEBUG_SEMANTIC_SELECTION__ = true;
        
        // Force rebuild model to ensure latest state
        // Use getModel() instead of buildModel() to ensure cache sync
        const model = designer.modelManager.getModel();
        if (!model) {
            return {error: 'No model'};
        }
        
        const nodes = Array.from(model.workflow.allNodes.values());
        const connections = Array.from(model.workflow.connections.values());
        
        // DEBUG: Check if updateConnectionPointSemanticSelection was called
        console.log('[TEST] Model has', nodes.length, 'nodes and', connections.length, 'connections');
        
        // DEBUG: Check cache BEFORE checking model
        const cacheForCheck = designer.modelManager.connectionPointsCache;
        let cacheSelectedCount = 0;
        let cacheTotalCount = 0;
        if (cacheForCheck) {
            for (const [nodeId, cachedPoints] of cacheForCheck.entries()) {
                cacheTotalCount += cachedPoints.length;
                const selected = cachedPoints.filter(p => p.semanticSelection && p.semanticSelection.isSelected);
                cacheSelectedCount += selected.length;
            }
        }
        console.log('[TEST] Cache has', cacheSelectedCount, 'selected points out of', cacheTotalCount, 'total points');
        
        // DEBUG: Check if semanticSelection is initialized at all
        let pointsWithInitializedMetadata = 0;
        let pointsWithSelectedMetadata = 0;
        nodes.forEach(node => {
            const allPoints = [...(node.connectionPoints.inputs || []), ...(node.connectionPoints.outputs || [])];
            allPoints.forEach(point => {
                if (point.semanticSelection !== undefined && point.semanticSelection !== null) {
                    pointsWithInitializedMetadata++;
                    if (point.semanticSelection.isSelected) {
                        pointsWithSelectedMetadata++;
                    }
                }
            });
        });
        const modelInfo = {
            totalNodes: nodes.length,
            totalConnections: connections.length,
            pointsWithInitializedMetadata: pointsWithInitializedMetadata,
            pointsWithSelectedMetadata: pointsWithSelectedMetadata,
            cacheSelectedCount: cacheSelectedCount,
            cacheTotalCount: cacheTotalCount
        };
        console.log('[TEST] Model info:', JSON.stringify(modelInfo));
        
        // DEBUG: Check cache directly (already checked above, but verify again)
        const cacheCheck = designer.modelManager.connectionPointsCache;
        const cacheInfo = {
            hasCache: !!cacheCheck,
            cacheSize: cacheCheck ? cacheCheck.size : 0,
            totalSelectedInCache: cacheSelectedCount,
            totalPointsInCache: cacheTotalCount,
            nodesWithSelectedPoints: 0
        };
        
        if (cacheCheck) {
            // Count nodes with selected points
            for (const [nodeId, cachedPoints] of cacheCheck.entries()) {
                const selected = cachedPoints.filter(p => p.semanticSelection && p.semanticSelection.isSelected);
                if (selected.length > 0) {
                    cacheInfo.nodesWithSelectedPoints++;
                }
            }
        }
        
        console.log('[TEST] Cache info:', JSON.stringify(cacheInfo));
        
        // Check first node's connection points in cache
        if (cacheCheck && nodes.length > 0 && connections.length > 0) {
            const firstConnection = connections[0];
            const fromNodeId = firstConnection.fromNodeId;
            const toNodeId = firstConnection.toNodeId;
            const cachedPointsFrom = cacheCheck.get(fromNodeId);
            const cachedPointsTo = cacheCheck.get(toNodeId);
            
            // Check FROM point
            if (cachedPointsFrom) {
                const fromPointId = firstConnection.anchors.from.connectionPointId;
                const pointInCache = cachedPointsFrom.find(p => p.id === fromPointId);
                if (pointInCache) {
                    console.log('[TEST] FROM Connection point', fromPointId, 'in cache:', JSON.stringify({
                        hasSemanticSelection: !!pointInCache.semanticSelection,
                        isSelected: pointInCache.semanticSelection?.isSelected,
                        reason: pointInCache.semanticSelection?.selectionReason,
                        selectedForConnections: pointInCache.semanticSelection?.selectedForConnections,
                        fullSemanticSelection: pointInCache.semanticSelection
                    }, null, 2));
                } else {
                    console.log('[TEST] FROM Connection point', fromPointId, 'NOT FOUND in cache for node', fromNodeId);
                    console.log('[TEST] Available points in cache:', cachedPointsFrom.map(p => p.id));
                }
            } else {
                console.log('[TEST] No cached points for FROM node', fromNodeId);
            }
            
            // Check TO point
            if (cachedPointsTo) {
                const toPointId = firstConnection.anchors.to.connectionPointId;
                const pointInCache = cachedPointsTo.find(p => p.id === toPointId);
                if (pointInCache) {
                    console.log('[TEST] TO Connection point', toPointId, 'in cache:', JSON.stringify({
                        hasSemanticSelection: !!pointInCache.semanticSelection,
                        isSelected: pointInCache.semanticSelection?.isSelected,
                        reason: pointInCache.semanticSelection?.selectionReason,
                        selectedForConnections: pointInCache.semanticSelection?.selectedForConnections,
                        fullSemanticSelection: pointInCache.semanticSelection
                    }, null, 2));
                } else {
                    console.log('[TEST] TO Connection point', toPointId, 'NOT FOUND in cache for node', toNodeId);
                    console.log('[TEST] Available points in cache:', cachedPointsTo.map(p => p.id));
                }
            } else {
                console.log('[TEST] No cached points for TO node', toNodeId);
            }
        }
        
        // DEBUG: Check model connection points
        if (connections.length > 0) {
            const firstConnection = connections[0];
            const fromNode = nodes.find(n => n.nodeId === firstConnection.fromNodeId);
            if (fromNode) {
                const fromPointInModel = fromNode.connectionPoints.outputs.find(
                    p => p.id === firstConnection.anchors.from.connectionPointId
                );
                if (fromPointInModel) {
                    console.log('[TEST] Connection point in model:', {
                        id: fromPointInModel.id,
                        hasSemanticSelection: 'semanticSelection' in fromPointInModel,
                        isSelected: fromPointInModel.semanticSelection?.isSelected,
                        reason: fromPointInModel.semanticSelection?.selectionReason
                    });
                }
            }
        }
        
        // Check connection points for semantic selection metadata
        const connectionPointsWithSelection = [];
        const connectionPointsWithoutSelection = [];
        
        // DEBUG: Check cache vs model (removed duplicate cacheInfo declaration)
        
        for (const node of nodes) {
            // Check input points
            for (const point of node.connectionPoints.inputs || []) {
                // DEBUG: Check if point has semanticSelection property at all
                const hasProperty = 'semanticSelection' in point;
                const hasValue = point.semanticSelection !== undefined && point.semanticSelection !== null;
                
                if (point.semanticSelection && point.semanticSelection.isSelected) {
                    connectionPointsWithSelection.push({
                        nodeId: node.nodeId,
                        pointId: point.id,
                        type: 'input',
                        side: point.side,
                        isSelected: point.semanticSelection.isSelected,
                        selectedForConnections: point.semanticSelection.selectedForConnections || [],
                        selectionReason: point.semanticSelection.selectionReason,
                        priority: point.semanticSelection.priority,
                        spatialContext: point.semanticSelection.spatialContext || {},
                        debug: { hasProperty, hasValue }
                    });
                } else {
                    connectionPointsWithoutSelection.push({
                        nodeId: node.nodeId,
                        pointId: point.id,
                        type: 'input',
                        side: point.side,
                        debug: { hasProperty, hasValue, semanticSelection: point.semanticSelection }
                    });
                }
            }
            
            // Check output points
            for (const point of node.connectionPoints.outputs || []) {
                // DEBUG: Check if point has semanticSelection property at all
                const hasProperty = 'semanticSelection' in point;
                const hasValue = point.semanticSelection !== undefined && point.semanticSelection !== null;
                
                if (point.semanticSelection && point.semanticSelection.isSelected) {
                    connectionPointsWithSelection.push({
                        nodeId: node.nodeId,
                        pointId: point.id,
                        type: 'output',
                        side: point.side,
                        isSelected: point.semanticSelection.isSelected,
                        selectedForConnections: point.semanticSelection.selectedForConnections || [],
                        selectionReason: point.semanticSelection.selectionReason,
                        priority: point.semanticSelection.priority,
                        spatialContext: point.semanticSelection.spatialContext || {},
                        debug: { hasProperty, hasValue }
                    });
                } else {
                    connectionPointsWithoutSelection.push({
                        nodeId: node.nodeId,
                        pointId: point.id,
                        type: 'output',
                        side: point.side,
                        debug: { hasProperty, hasValue, semanticSelection: point.semanticSelection }
                    });
                }
            }
        }
        
        // Check connections and their selected points
        const connectionDetails = [];
        for (const conn of connections) {
            const fromNode = nodes.find(n => n.nodeId === conn.fromNodeId);
            const toNode = nodes.find(n => n.nodeId === conn.toNodeId);
            
            let fromPoint = null;
            let toPoint = null;
            
            if (fromNode) {
                const allOutputs = fromNode.connectionPoints.outputs || [];
                fromPoint = allOutputs.find(p => p.id === conn.anchors.from.connectionPointId);
            }
            
            if (toNode) {
                const allInputs = toNode.connectionPoints.inputs || [];
                toPoint = allInputs.find(p => p.id === conn.anchors.to.connectionPointId);
            }
            
            connectionDetails.push({
                connectionId: conn.connectionId,
                fromNodeId: conn.fromNodeId,
                toNodeId: conn.toNodeId,
                fromPoint: fromPoint ? {
                    id: fromPoint.id,
                    side: fromPoint.side,
                    hasSemanticSelection: !!fromPoint.semanticSelection,
                    isSelected: fromPoint.semanticSelection?.isSelected || false,
                    selectionReason: fromPoint.semanticSelection?.selectionReason || 'none'
                } : null,
                toPoint: toPoint ? {
                    id: toPoint.id,
                    side: toPoint.side,
                    hasSemanticSelection: !!toPoint.semanticSelection,
                    isSelected: toPoint.semanticSelection?.isSelected || false,
                    selectionReason: toPoint.semanticSelection?.selectionReason || 'none'
                } : null
            });
        }
        
        return {
            totalNodes: nodes.length,
            totalConnections: connections.length,
            connectionPointsWithSelection: connectionPointsWithSelection,
            connectionPointsWithoutSelection: connectionPointsWithoutSelection,
            connectionDetails: connectionDetails,
            hasModel: true
        };
    ''')
    
    return result

def verify_rendering_matches_model(driver):
    """Verify that visual rendering matches semantic model decisions"""
    print("\n🎨 Verifying rendering matches model...")
    
    result = driver.execute_script('''
        const designer = window.app?._instance?.visualDesigner || window.app?.visualDesigner;
        if (!designer || !designer.modelManager) {
            return {error: 'No model manager'};
        }
        
        const model = designer.modelManager.getModel();
        if (!model) {
            return {error: 'No model'};
        }
        
        const connections = Array.from(model.workflow.connections.values());
        const svg = document.querySelector('svg');
        
        const renderingMatches = [];
        
        for (const conn of connections) {
            // Get connection point from model
            const fromNode = model.workflow.allNodes.get(conn.fromNodeId);
            const toNode = model.workflow.allNodes.get(conn.toNodeId);
            
            let modelFromSide = null;
            let modelToSide = null;
            
            if (fromNode) {
                const fromPoint = (fromNode.connectionPoints.outputs || []).find(
                    p => p.id === conn.anchors.from.connectionPointId
                );
                if (fromPoint) {
                    modelFromSide = fromPoint.side;
                }
            }
            
            if (toNode) {
                const toPoint = (toNode.connectionPoints.inputs || []).find(
                    p => p.id === conn.anchors.to.connectionPointId
                );
                if (toPoint) {
                    modelToSide = toPoint.side;
                }
            }
            
            // Check if SVG path exists and matches
            const pathId = `connection-${conn.connectionId}`;
            const svgPath = svg?.querySelector(`#${pathId}`) || 
                          svg?.querySelector(`[data-connection-id="${conn.connectionId}"]`);
            
            // Get path start and end points from SVG
            let svgStartPoint = null;
            let svgEndPoint = null;
            if (svgPath) {
                const pathData = svgPath.getAttribute('d');
                if (pathData) {
                    // Extract first and last points from path
                    // Support M, L, H (horizontal), V (vertical), C, Q, S, T commands
                    const allPoints = [];
                    let currentX = 0;
                    let currentY = 0;
                    const commands = pathData.match(/[MLHVZCQST][^MLHVZCQST]*/gi) || [];
                    
                    for (const cmd of commands) {
                        const cmdType = cmd[0].toUpperCase();
                        if (cmdType === 'Z') continue; // Close path
                        
                        const coords = cmd.substring(1).trim().split(/[,\\s]+/).filter(s => s);
                        
                        if (cmdType === 'M') {
                            // Move to - absolute
                            if (coords.length >= 2) {
                                currentX = parseFloat(coords[0]);
                                currentY = parseFloat(coords[1]);
                                if (!isNaN(currentX) && !isNaN(currentY)) {
                                    allPoints.push({x: currentX, y: currentY});
                                }
                            }
                        } else if (cmdType === 'L') {
                            // Line to - absolute
                            if (coords.length >= 2) {
                                currentX = parseFloat(coords[0]);
                                currentY = parseFloat(coords[1]);
                                if (!isNaN(currentX) && !isNaN(currentY)) {
                                    allPoints.push({x: currentX, y: currentY});
                                }
                            }
                        } else if (cmdType === 'H') {
                            // Horizontal line to - absolute X, Y stays same
                            if (coords.length >= 1) {
                                currentX = parseFloat(coords[0]);
                                if (!isNaN(currentX)) {
                                    allPoints.push({x: currentX, y: currentY});
                                }
                            }
                        } else if (cmdType === 'V') {
                            // Vertical line to - absolute Y, X stays same
                            if (coords.length >= 1) {
                                currentY = parseFloat(coords[0]);
                                if (!isNaN(currentY)) {
                                    allPoints.push({x: currentX, y: currentY});
                                }
                            }
                        } else {
                            // Other commands (C, Q, S, T) - extract first two coords as point
                            if (coords.length >= 2) {
                                const x = parseFloat(coords[0]);
                                const y = parseFloat(coords[1]);
                                if (!isNaN(x) && !isNaN(y)) {
                                    currentX = x;
                                    currentY = y;
                                    allPoints.push({x: currentX, y: currentY});
                                }
                            }
                        }
                    }
                    
                    if (allPoints.length > 0) {
                        svgStartPoint = allPoints[0];
                    }
                    if (allPoints.length > 1) {
                        svgEndPoint = allPoints[allPoints.length - 1];
                    }
                }
            }
            
            // Compare model anchor positions with SVG path
            const modelStart = conn.anchors.from.position;
            const modelEnd = conn.anchors.to.position;
            
            let startMatches = false;
            let endMatches = false;
            
            if (svgStartPoint && modelStart) {
                const diffX = Math.abs(svgStartPoint.x - modelStart.x);
                const diffY = Math.abs(svgStartPoint.y - modelStart.y);
                startMatches = diffX < 2 && diffY < 2; // 2px tolerance
            }
            
            if (svgEndPoint && modelEnd) {
                const diffX = Math.abs(svgEndPoint.x - modelEnd.x);
                const diffY = Math.abs(svgEndPoint.y - modelEnd.y);
                endMatches = diffX < 2 && diffY < 2; // 2px tolerance
                
                // DEBUG: Log mismatch details
                if (!endMatches && window.__DEBUG_SEMANTIC_SELECTION__) {
                    console.log(`[TEST] endMatch mismatch for ${conn.connectionId}:`, {
                        svgEndPoint: svgEndPoint,
                        modelEnd: modelEnd,
                        diffX: diffX.toFixed(2),
                        diffY: diffY.toFixed(2),
                        tolerance: 2
                    });
                }
            }
            
            renderingMatches.push({
                connectionId: conn.connectionId,
                fromNodeId: conn.fromNodeId,
                toNodeId: conn.toNodeId,
                modelFromSide: modelFromSide,
                modelToSide: modelToSide,
                hasSvgPath: !!svgPath,
                startMatches: startMatches,
                endMatches: endMatches,
                modelStart: modelStart,
                modelEnd: modelEnd,
                svgStart: svgStartPoint,
                svgEnd: svgEndPoint
            });
        }
        
        return {
            totalConnections: connections.length,
            renderingMatches: renderingMatches,
            allMatch: renderingMatches.every(m => m.startMatches && m.endMatches && m.hasSvgPath)
        };
    ''')
    
    return result

def main():
    """Main test function"""
    print("="*80)
    print("TEST: Semantic Model Connection Point Selection")
    print("="*80)
    print("\nTest weryfikuje:")
    print("1. Czy semanticSelection metadata jest ustawiana w connection points")
    print("2. Czy wybór strony jest oparty na analizie przestrzennej")
    print("3. Czy warstwa wizualna renderuje zgodnie z decyzjami modelu")
    print("4. Czy WebSocket synchronizuje model z backendem")
    print("\n" + "="*80 + "\n")
    
    driver = None
    ws_client = None
    
    try:
        # Step 1: Setup WebSocket
        print("[1/5] Setting up WebSocket connection...")
        ws_client = WebSocketClient(WS_URL)
        ws_client.connect()
        time.sleep(2)
        
        # Step 2: Setup Selenium
        print("\n[2/5] Setting up Selenium Chrome...")
        driver = setup_driver()
        
        # Enable debug logging for semantic selection
        driver.execute_script("window.__DEBUG_SEMANTIC_SELECTION__ = true;")
        
        # Step 3: Load configuration
        print("\n[3/5] Loading configuration...")
        if not load_config_via_selenium(driver, TEST_CONFIG_XML):
            print("❌ Failed to load configuration")
            return False
        
        # Step 4: Check semantic selection metadata
        print("\n[4/5] Checking semantic selection metadata...")
        
        # DEBUG: Check browser console for logs
        print("\n📋 Browser console logs (semanticSelection related, last 100):")
        console_logs = driver.get_log('browser')
        semantic_logs = [log for log in console_logs if 'semanticSelection' in log.get('message', '') or 'Updated semanticSelection' in log.get('message', '') or 'Synced' in log.get('message', '') or 'TEST' in log.get('message', '') or 'buildConnectionStates' in log.get('message', '') or 'Cache' in log.get('message', '')]
        if semantic_logs:
            for log in semantic_logs[-100:]:
                msg = log.get('message', '')
                # Only show relevant logs
                if any(keyword in msg for keyword in ['semanticSelection', 'Updated', 'Synced', 'TEST', 'Cache', 'buildConnectionStates', 'points have']):
                    print(f"  {log.get('level', 'INFO')}: {msg}")
        else:
            print("  ⚠️  No semanticSelection-related logs found in browser console")
        
        # Also check for any TEST logs
        test_logs = [log for log in console_logs if 'TEST' in log.get('message', '')]
        if test_logs:
            print(f"\n📋 Found {len(test_logs)} TEST logs:")
            for log in test_logs[-20:]:
                print(f"  {log.get('level', 'INFO')}: {log.get('message', '')}")
        
        metadata_result = check_semantic_selection_metadata(driver)
        
        if metadata_result.get('error'):
            print(f"❌ Error: {metadata_result['error']}")
            return False
        
        print(f"\n📊 Results:")
        print(f"  Total nodes: {metadata_result.get('totalNodes', 0)}")
        print(f"  Total connections: {metadata_result.get('totalConnections', 0)}")
        print(f"  Connection points WITH semantic selection: {len(metadata_result.get('connectionPointsWithSelection', []))}")
        print(f"  Connection points WITHOUT semantic selection: {len(metadata_result.get('connectionPointsWithoutSelection', []))}")
        
        # DEBUG: Show first few points without selection to understand the issue
        without_selection = metadata_result.get('connectionPointsWithoutSelection', [])[:3]
        if len(without_selection) > 0:
            print(f"\n  DEBUG: First 3 points without selection:")
            for point in without_selection:
                debug = point.get('debug', {})
                print(f"    - {point['nodeId']}/{point['pointId']}: hasProperty={debug.get('hasProperty', False)}, hasValue={debug.get('hasValue', False)}, semanticSelection={debug.get('semanticSelection', 'undefined')}")
        
        # Check if selected points have metadata
        selected_points = [p for p in metadata_result.get('connectionPointsWithSelection', []) if p.get('isSelected')]
        print(f"  Selected connection points: {len(selected_points)}")
        
        if len(selected_points) > 0:
            print("\n  ✅ Selected connection points have semantic selection metadata:")
            for point in selected_points[:5]:  # Show first 5
                print(f"    - Node: {point['nodeId']}, Point: {point['pointId']}, Side: {point['side']}")
                print(f"      Reason: {point.get('selectionReason', 'unknown')}, Priority: {point.get('priority', 0)}")
                print(f"      Used by connections: {len(point.get('selectedForConnections', []))}")
        else:
            print("\n  ⚠️  No connection points are marked as selected")
        
        # Check connection details
        connection_details = metadata_result.get('connectionDetails', [])
        print(f"\n  Connection details ({len(connection_details)} connections):")
        for conn in connection_details:
            from_point = conn.get('fromPoint') or {}
            to_point = conn.get('toPoint') or {}
            from_has_selection = from_point.get('hasSemanticSelection', False)
            to_has_selection = to_point.get('hasSemanticSelection', False)
            status = "✅" if (from_has_selection and to_has_selection) else "⚠️"
            print(f"    {status} {conn['fromNodeId']} -> {conn['toNodeId']}")
            if from_point:
                print(f"      From: side={from_point.get('side', 'unknown')}, selected={from_point.get('isSelected', False)}, reason={from_point.get('selectionReason', 'none')}")
            if to_point:
                print(f"      To: side={to_point.get('side', 'unknown')}, selected={to_point.get('isSelected', False)}, reason={to_point.get('selectionReason', 'none')}")
        
        # Step 5: Verify rendering matches model
        print("\n[5/5] Verifying rendering matches model...")
        rendering_result = verify_rendering_matches_model(driver)
        
        if rendering_result.get('error'):
            print(f"❌ Error: {rendering_result['error']}")
        else:
            print(f"\n📊 Rendering verification:")
            print(f"  Total connections: {rendering_result.get('totalConnections', 0)}")
            matches = rendering_result.get('renderingMatches', [])
            all_match = rendering_result.get('allMatch', False)
            
            matched = sum(1 for m in matches if m.get('startMatches') and m.get('endMatches') and m.get('hasSvgPath'))
            print(f"  Matched: {matched}/{len(matches)}")
            print(f"  All match: {'✅' if all_match else '❌'}")
            
            if not all_match:
                print("\n  ⚠️  Mismatches:")
                for m in matches:
                    if not (m.get('startMatches') and m.get('endMatches') and m.get('hasSvgPath')):
                        print(f"    - {m['connectionId']}: hasSvg={m.get('hasSvgPath')}, startMatch={m.get('startMatches')}, endMatch={m.get('endMatches')}")
        
        # Check WebSocket messages
        print("\n📡 WebSocket messages:")
        model_updates = ws_client.get_model_updates()
        print(f"  Model updates received: {len(model_updates)}")
        if len(model_updates) > 0:
            print("  ✅ Model is being synchronized via WebSocket")
        else:
            print("  ⚠️  No model updates received via WebSocket")
        
        # Final summary
        print("\n" + "="*80)
        print("TEST SUMMARY")
        print("="*80)
        
        has_metadata = len(selected_points) > 0
        has_rendering = all_match if rendering_result.get('allMatch') is not None else False
        has_websocket = len(model_updates) > 0
        
        print(f"\n✅ Semantic selection metadata: {'PASS' if has_metadata else 'FAIL'}")
        print(f"{'✅' if has_rendering else '❌'} Rendering matches model: {'PASS' if has_rendering else 'FAIL'}")
        print(f"{'✅' if has_websocket else '⚠️'} WebSocket synchronization: {'PASS' if has_websocket else 'PARTIAL'}")
        
        success = has_metadata and has_rendering
        print(f"\n{'✅ TEST PASSED' if success else '❌ TEST FAILED'}")
        print("="*80)
        
        # Keep browser open for inspection
        print("\nBrowser pozostaje otwarty. Naciśnij Enter aby zamknąć...")
        try:
            input()
        except EOFError:
            pass
        
        return success
        
    except Exception as e:
        print(f"\n❌ Test failed with error: {e}")
        import traceback
        traceback.print_exc()
        return False
        
    finally:
        if ws_client:
            ws_client.disconnect()
        if driver:
            driver.quit()

if __name__ == '__main__':
    success = main()
    sys.exit(0 if success else 1)

