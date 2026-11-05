#!/usr/bin/env python3
"""
Test collisions and auto-layout functionality
- Tests node collisions (overlapping nodes)
- Tests label collisions
- Tests path collisions (lines passing through nodes)
- Tests automatic rerouting
- Tests automatic node repositioning
"""

import time
import json
from selenium import webdriver
from selenium.webdriver.chrome.options import Options
from selenium.webdriver.common.by import By
from selenium.webdriver.common.action_chains import ActionChains
from selenium.webdriver.support.ui import WebDriverWait
from selenium.webdriver.support import expected_conditions as EC

def test_collisions_and_auto_layout():
    """Test collision detection and auto-layout"""
    options = Options()
    options.add_argument('--headless')
    options.add_argument('--no-sandbox')
    options.add_argument('--disable-dev-shm-usage')
    driver = webdriver.Chrome(options=options)
    
    try:
        print("🌐 Opening IDE...")
        driver.get('http://localhost:8080')
        time.sleep(5)
        
        # Switch to visual mode
        driver.execute_script('''
            if (window.app && window.app.switchToVisualMode) {
                window.app.switchToVisualMode();
            }
        ''')
        time.sleep(2)
        
        # Load config with multiple nodes
        config_xml = '''<config xmlns="http://org.webharvest/schema/2.1/core">
            <def var="url1">https://httpbin.org/html</def>
            <def var="html1"><http url="${url1}"/></def>
            <def var="url2">https://httpbin.org/json</def>
            <def var="json1"><http url="${url2}"/></def>
            <def var="url3">https://httpbin.org/xml</def>
            <def var="xml1"><http url="${url3}"/></def>
        </config>'''
        
        print("📥 Loading configuration...")
        driver.execute_script(f'''
            const xmlContent = {repr(config_xml)};
            if (window.app && window.app.visualDesigner && window.app._instance && window.app._instance.xmlParser) {{
                const nodes = window.app._instance.xmlParser.parse(xmlContent);
                window.app.visualDesigner.loadNodes(nodes);
            }}
        ''')
        time.sleep(5)
        
        # Get initial node positions
        initial_positions = driver.execute_script('''
            const nodes = window.app?.visualDesigner?.getNodes() || [];
            return nodes.map(node => ({
                id: node.id,
                x: node.position?.x || 0,
                y: node.position?.y || 0
            }));
        ''')
        print(f"📍 Initial node positions: {len(initial_positions)} nodes")
        for pos in initial_positions:
            print(f"   - {pos['id']}: ({pos['x']}, {pos['y']})")
        
        # Test 1: Check semantic model for collision information
        print("\n🔍 Test 1: Checking semantic model for collision information...")
        model_info = driver.execute_script('''
            const manager = window.app?.visualDesigner?._modelManager;
            if (!manager) return {error: 'No model manager'};
            
            const model = manager.getModel();
            if (!model) return {error: 'No model'};
            
            const connections = Array.from(model.workflow.connections.values());
            const collisionInfo = connections.map(conn => ({
                connectionId: conn.connectionId,
                hasCollisions: conn.path.spatialMetadata.hasCollisions || false,
                passesThroughNodes: conn.path.spatialMetadata.passesThroughNodes || [],
                labelCollisions: (conn.path.spatialMetadata.labelCollisions || []).length,
                turningPointsCount: conn.path.spatialMetadata.turningPointsCount || 0,
                segmentsCount: conn.path.spatialMetadata.segmentCount || 0,
                totalLength: conn.path.spatialMetadata.totalLength || 0
            }));
            
            return {
                connectionsCount: connections.length,
                collisionInfo: collisionInfo,
                nodesWithCollisions: Array.from(model.workflow.allNodes.values())
                    .filter(node => node.spatialCollisions?.isOverlapping)
                    .map(node => node.nodeId)
            };
        ''')
        
        print(f"   ✅ Model has {model_info.get('connectionsCount', 0)} connections")
        print(f"   ✅ Nodes with collisions: {len(model_info.get('nodesWithCollisions', []))}")
        
        collisions_found = 0
        for conn_info in model_info.get('collisionInfo', []):
            if conn_info.get('hasCollisions') or conn_info.get('passesThroughNodes'):
                collisions_found += 1
                print(f"   ⚠️  Connection {conn_info['connectionId']} has collisions:")
                if conn_info.get('passesThroughNodes'):
                    print(f"      - Passes through nodes: {conn_info['passesThroughNodes']}")
                if conn_info.get('labelCollisions', 0) > 0:
                    print(f"      - Label collisions: {conn_info['labelCollisions']}")
        
        if collisions_found == 0:
            print("   ✅ No collisions detected in initial layout")
        
        # Test 2: Simulate dragging a node to create collision
        print("\n🖱️  Test 2: Simulating node drag to create collision...")
        nodes = driver.execute_script('''
            return window.app?.visualDesigner?.getNodes() || [];
        ''')
        
        if len(nodes) >= 2:
            # Get first two nodes
            node1_id = nodes[0]['id']
            node2_id = nodes[1]['id']
            
            # Get node1 position
            node1_pos = driver.execute_script(f'''
                const node = window.app?.visualDesigner?.getNodes()?.find(n => n.id === {repr(node1_id)});
                return node?.position || {{x: 0, y: 0}};
            ''')
            
            # Get node2 position
            node2_pos = driver.execute_script(f'''
                const node = window.app?.visualDesigner?.getNodes()?.find(n => n.id === {repr(node2_id)});
                return node?.position || {{x: 0, y: 0}};
            ''')
            
            print(f"   📍 Node1 ({node1_id}) at ({node1_pos['x']}, {node1_pos['y']})")
            print(f"   📍 Node2 ({node2_id}) at ({node2_pos['x']}, {node2_pos['y']})")
            
            # Move node1 to overlap with node2
            overlap_x = node2_pos['x']
            overlap_y = node2_pos['y']
            
            print(f"   🎯 Moving node1 to ({overlap_x}, {overlap_y}) to create collision...")
            driver.execute_script(f'''
                const designer = window.app?.visualDesigner;
                if (designer) {{
                    designer.moveNode({repr(node1_id)}, {{x: {overlap_x}, y: {overlap_y}}});
                }}
            ''')
            time.sleep(2)
            
            # Check if collision was detected and resolved
            final_positions = driver.execute_script('''
                const nodes = window.app?.visualDesigner?.getNodes() || [];
                return nodes.map(node => ({
                    id: node.id,
                    x: node.position?.x || 0,
                    y: node.position?.y || 0
                }));
            ''')
            
            node1_final = next((n for n in final_positions if n['id'] == node1_id), None)
            node2_final = next((n for n in final_positions if n['id'] == node2_id), None)
            
            if node1_final:
                print(f"   📍 Node1 final position: ({node1_final['x']}, {node1_final['y']})")
                if node1_final['x'] != overlap_x or node1_final['y'] != overlap_y:
                    print("   ✅ Collision detected and resolved! Node was moved automatically.")
                else:
                    print("   ⚠️  Node position unchanged - collision may not have been detected")
        
        # Test 3: Check path collisions
        print("\n🔍 Test 3: Checking for path collisions (lines passing through nodes)...")
        path_collisions = driver.execute_script('''
            const manager = window.app?.visualDesigner?._modelManager;
            if (!manager) return {error: 'No model manager'};
            
            const model = manager.getModel();
            if (!model) return {error: 'No model'};
            
            const connections = Array.from(model.workflow.connections.values());
            const pathCollisions = [];
            
            for (const conn of connections) {
                const passesThrough = conn.path.spatialMetadata.passesThroughNodes || [];
                if (passesThrough.length > 0) {
                    pathCollisions.push({
                        connectionId: conn.connectionId,
                        from: conn.fromNodeId,
                        to: conn.toNodeId,
                        passesThrough: passesThrough,
                        hasReroute: conn.path.spatialMetadata.segments && conn.path.spatialMetadata.segments.length > 2
                    });
                }
            }
            
            return {
                totalConnections: connections.length,
                pathCollisions: pathCollisions
            };
        ''')
        
        print(f"   📊 Total connections: {path_collisions.get('totalConnections', 0)}")
        print(f"   ⚠️  Path collisions: {len(path_collisions.get('pathCollisions', []))}")
        
        for collision in path_collisions.get('pathCollisions', []):
            print(f"   ⚠️  Connection {collision['connectionId']} passes through: {collision['passesThrough']}")
            if collision.get('hasReroute'):
                print(f"      ✅ Path was rerouted (has {len(collision.get('segments', []))} segments)")
        
        # Test 4: Check detailed path information
        print("\n📐 Test 4: Checking detailed path information...")
        path_details_result = driver.execute_script('''
            const manager = window.app?.visualDesigner?._modelManager;
            if (!manager) return {error: 'No model manager'};
            
            const model = manager.getModel();
            if (!model) return {error: 'No model'};
            
            const connections = Array.from(model.workflow.connections.values());
            return connections.slice(0, 3).map(conn => ({
                connectionId: conn.connectionId,
                from: conn.fromNodeId,
                to: conn.toNodeId,
                startPoint: conn.path.spatialMetadata.startPoint,
                endPoint: conn.path.spatialMetadata.endPoint,
                turningPointsCount: conn.path.spatialMetadata.turningPointsCount,
                turningPoints: conn.path.spatialMetadata.turningPoints || [],
                segmentsCount: conn.path.spatialMetadata.segmentCount,
                segments: conn.path.spatialMetadata.segments || [],
                totalLength: conn.path.spatialMetadata.totalLength,
                hasCollisions: conn.path.spatialMetadata.hasCollisions || false
            }));
        ''')
        
        path_details = path_details_result if isinstance(path_details_result, list) else []
        for detail in path_details:
            print(f"   🔗 Connection {detail['connectionId']}:")
            print(f"      - From: {detail['from']} → To: {detail['to']}")
            print(f"      - Start: ({detail['startPoint']['x']:.1f}, {detail['startPoint']['y']:.1f})")
            print(f"      - End: ({detail['endPoint']['x']:.1f}, {detail['endPoint']['y']:.1f})")
            print(f"      - Length: {detail['totalLength']:.1f}px")
            print(f"      - Segments: {detail['segmentsCount']}")
            print(f"      - Turning points: {detail['turningPointsCount']} ({len(detail['turningPoints'])} coordinates)")
            if detail.get('hasCollisions'):
                print(f"      - ⚠️  Has collisions!")
        
        print("\n✅ All collision and auto-layout tests completed!")
        
    except Exception as e:
        print(f"❌ Error: {e}")
        import traceback
        traceback.print_exc()
    finally:
        driver.quit()

if __name__ == '__main__':
    test_collisions_and_auto_layout()

