#!/usr/bin/env python3
"""
Test visibility of connection points in runtime
"""

from selenium import webdriver
from selenium.webdriver.chrome.options import Options
from selenium.webdriver.common.by import By
from selenium.webdriver.support.ui import WebDriverWait
from selenium.webdriver.support import expected_conditions as EC
import time

def test_connection_points_visibility():
    options = Options()
    # options.add_argument('--headless')
    options.add_argument('--no-sandbox')
    options.add_argument('--disable-dev-shm-usage')
    options.add_argument('--window-size=1920,1080')
    
    driver = webdriver.Chrome(options=options)
    
    try:
        print("🌐 Opening IDE...")
        driver.get('http://localhost:8080/')
        time.sleep(5)
        
        # Load config
        print("📝 Loading configuration...")
        result = driver.execute_script('''
            const xml = '<config><http-request id="test1"/><http-request id="test2"/><http-request id="test3"/></config>';
            if (window.app && window.app._instance) {
                window.app._instance.loadConfiguration(xml);
            }
            return 'loaded';
        ''')
        print(f"   Config loaded: {result}")
        time.sleep(3)
        
        # Check if connection points renderer exists
        print("\n🔍 Checking ConnectionPointsRenderer...")
        renderer_info = driver.execute_script('''
            const designer = window.app?._instance?.visualDesigner || window.app?.visualDesigner;
            if (!designer) return {exists: false, error: 'No designer'};
            
            return {
                exists: !!designer.connectionPointsRenderer,
                hasModelManager: !!designer.modelManager,
                hasSvg: !!designer.svgElement,
                selectedNodes: designer.selectedNodeIds || [],
                svgId: designer.svgElement?.id || 'no-id',
                svgInDom: !!designer.svgElement?.parentElement
            };
        ''')
        print(f"   Renderer exists: {renderer_info.get('exists', False)}")
        print(f"   Has model manager: {renderer_info.get('hasModelManager', False)}")
        print(f"   Has SVG: {renderer_info.get('hasSvg', False)}")
        print(f"   SVG in DOM: {renderer_info.get('svgInDom', False)}")
        
        if not renderer_info.get('exists'):
            print("❌ ConnectionPointsRenderer does not exist!")
            return False
        
        # Get all nodes
        nodes_info = driver.execute_script('''
            const designer = window.app?._instance?.visualDesigner || window.app?.visualDesigner;
            if (!designer) return {error: 'No designer'};
            
            const nodes = designer.getAllNodes();
            return {
                count: nodes.length,
                nodeIds: nodes.map(n => n.id).slice(0, 5)
            };
        ''')
        print(f"\n📊 Nodes: {nodes_info.get('count', 0)} total")
        if nodes_info.get('count', 0) == 0:
            print("❌ No nodes found!")
            return False
        
        # Select first node
        print(f"\n🎯 Selecting node: {nodes_info.get('nodeIds', [])[0] if nodes_info.get('nodeIds') else 'unknown'}...")
        select_result = driver.execute_script('''
            const designer = window.app?._instance?.visualDesigner || window.app?.visualDesigner;
            if (!designer) return {error: 'No designer'};
            
            const nodes = designer.getAllNodes();
            if (nodes.length === 0) return {error: 'No nodes'};
            
            const nodeId = nodes[0].id;
            designer.selectNode(nodeId);
            
            return {
                selected: nodeId,
                totalNodes: nodes.length,
                rendererExists: !!designer.connectionPointsRenderer,
                selectedNodes: designer.selectedNodeIds || []
            };
        ''')
        print(f"   Selected: {select_result.get('selected', 'unknown')}")
        print(f"   Selected nodes array: {select_result.get('selectedNodes', [])}")
        time.sleep(2)
        
        # Check browser console logs
        print("\n📋 Browser console logs (ConnectionPointsRenderer related):")
        try:
            console_logs = driver.get_log('browser')
            connection_logs = [log for log in console_logs if 'ConnectionPointsRenderer' in log.get('message', '')]
            if connection_logs:
                for log in connection_logs[-20:]:
                    print(f"   {log.get('level', 'INFO')}: {log.get('message', '')}")
            else:
                print("   ⚠️  No ConnectionPointsRenderer logs found")
        except Exception as e:
            print(f"   ⚠️  Could not get console logs: {e}")
        
        # Check for connection points in SVG
        print("\n🔍 Checking for connection points in SVG...")
        circles_info = driver.execute_script('''
            const svg = document.querySelector('svg');
            if (!svg) return {error: 'No SVG'};
            
            const circles = svg.querySelectorAll('circle.connection-point');
            const allCircles = Array.from(circles).map(c => ({
                id: c.getAttribute('data-point-id'),
                nodeId: c.getAttribute('data-node-id'),
                cx: parseFloat(c.getAttribute('cx') || '0'),
                cy: parseFloat(c.getAttribute('cy') || '0'),
                r: parseFloat(c.getAttribute('r') || '0'),
                fill: c.getAttribute('fill'),
                stroke: c.getAttribute('stroke'),
                visible: c.style.display !== 'none',
                opacity: c.style.opacity || c.getAttribute('opacity') || '1'
            }));
            
            // Also check all circles in SVG
            const allCirclesInSvg = svg.querySelectorAll('circle');
            
            return {
                connectionPointsCount: circles.length,
                totalCirclesInSvg: allCirclesInSvg.length,
                circles: allCircles.slice(0, 10),
                svgChildren: svg.children.length,
                svgWidth: svg.getAttribute('width'),
                svgHeight: svg.getAttribute('height')
            };
        ''')
        print(f"   Connection points found: {circles_info.get('connectionPointsCount', 0)}")
        print(f"   Total circles in SVG: {circles_info.get('totalCirclesInSvg', 0)}")
        print(f"   SVG children: {circles_info.get('svgChildren', 0)}")
        print(f"   SVG size: {circles_info.get('svgWidth', '?')} x {circles_info.get('svgHeight', '?')}")
        
        if circles_info.get('connectionPointsCount', 0) > 0:
            print(f"\n   ✅ First few connection points:")
            for circle in circles_info.get('circles', [])[:5]:
                print(f"      - {circle.get('nodeId')}/{circle.get('id')}: ({circle.get('cx')}, {circle.get('cy')}), fill={circle.get('fill')}, visible={circle.get('visible')}")
        else:
            print("   ❌ No connection points found in SVG!")
            
            # Check visible nodes in renderer
            visible_info = driver.execute_script('''
                const designer = window.app?._instance?.visualDesigner || window.app?.visualDesigner;
                if (!designer || !designer.connectionPointsRenderer) return {error: 'No renderer'};
                
                // Try to access visibleNodes
                try {
                    const visibleNodes = designer.connectionPointsRenderer.visibleNodes;
                    return {
                        visibleNodesCount: visibleNodes ? visibleNodes.size : 0,
                        visibleNodes: visibleNodes ? Array.from(visibleNodes) : []
                    };
                } catch (e) {
                    return {error: String(e)};
                }
            ''')
            print(f"\n   Visible nodes in renderer: {visible_info}")
            
            # Check model for connection points
            model_info = driver.execute_script('''
                const designer = window.app?._instance?.visualDesigner || window.app?.visualDesigner;
                if (!designer || !designer.modelManager) return {error: 'No model manager'};
                
                const model = designer.modelManager.getModel();
                if (!model) return {error: 'No model'};
                
                const selectedNodeId = designer.selectedNodeIds && designer.selectedNodeIds[0];
                if (!selectedNodeId) return {error: 'No selected node'};
                
                const nodeState = model.workflow.allNodes.get(selectedNodeId);
                if (!nodeState) return {error: 'Node state not found'};
                
                const inputs = nodeState.connectionPoints.inputs || [];
                const outputs = nodeState.connectionPoints.outputs || [];
                
                return {
                    selectedNodeId: selectedNodeId,
                    inputPoints: inputs.length,
                    outputPoints: outputs.length,
                    totalPoints: inputs.length + outputs.length,
                    firstInput: inputs[0] ? {
                        id: inputs[0].id,
                        side: inputs[0].side,
                        position: inputs[0].position
                    } : null,
                    firstOutput: outputs[0] ? {
                        id: outputs[0].id,
                        side: outputs[0].side,
                        position: outputs[0].position
                    } : null
                };
            ''')
            print(f"\n   Model info for selected node:")
            print(f"      Selected node: {model_info.get('selectedNodeId', 'unknown')}")
            print(f"      Input points: {model_info.get('inputPoints', 0)}")
            print(f"      Output points: {model_info.get('outputPoints', 0)}")
            print(f"      Total points: {model_info.get('totalPoints', 0)}")
        
        # Take screenshot
        print("\n📸 Taking screenshot...")
        driver.save_screenshot('/tmp/connection_points_test.png')
        print("   Screenshot saved to /tmp/connection_points_test.png")
        
        success = circles_info.get('connectionPointsCount', 0) > 0
        if success:
            print("\n✅ SUCCESS: Connection points are visible!")
        else:
            print("\n❌ FAILED: Connection points are NOT visible!")
        
        input("\nPress Enter to close browser...")
        
        return success
        
    except Exception as e:
        print(f"\n❌ Error: {e}")
        import traceback
        traceback.print_exc()
        return False
    finally:
        driver.quit()

if __name__ == '__main__':
    test_connection_points_visibility()

