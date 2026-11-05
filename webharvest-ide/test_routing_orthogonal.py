#!/usr/bin/env python3
"""
Test orthogonal routing w runtime przez Selenium Chrome
Sprawdza czy linie są TYLKO poziome i pionowe (orthogonal)
"""

import time
from selenium import webdriver
from selenium.webdriver.chrome.options import Options
from selenium.webdriver.common.by import By
from selenium.webdriver.support.ui import WebDriverWait
from selenium.webdriver.support import expected_conditions as EC
from selenium.common.exceptions import TimeoutException

IDE_URL = "http://localhost:8080"

TEST_CONFIG_XML = """<config xmlns="http://org.webharvest/schema/2.1/core">
    <def var="url1">https://httpbin.org/html</def>
    <def var="html1">
        <http url="${url1}"/>
    </def>
    <def var="url2">https://httpbin.org/json</def>
    <def var="json2">
        <http url="${url2}"/>
    </def>
</config>"""

def load_config_via_selenium(driver):
    """Load configuration via Selenium - używa tego samego podejścia co test_semantic_connection_point_selection.py"""
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
            const xmlContent = {repr(TEST_CONFIG_XML)};
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
        import traceback
        traceback.print_exc()
        return False

def check_orthogonal_routing(driver):
    """Check if all connection paths are orthogonal (only horizontal/vertical segments) and use center points"""
    try:
        script = """
        // Try multiple selectors for SVG
        let svg = document.querySelector('svg.connection-layer');
        if (!svg) svg = document.querySelector('svg');
        if (!svg) {
            // Try to find SVG in canvas
            const canvas = document.querySelector('.visual-canvas, #visual-canvas, .editor-section');
            if (canvas) svg = canvas.querySelector('svg');
        }
        if (!svg) {
            // Wait a bit and try again
            return { error: 'No SVG found. Available elements: ' + document.querySelectorAll('svg').length + ', waiting...' };
        }
        
        // CRITICAL: Check if connections use center points from semantic model
        const designer = window.app?._instance?.visualDesigner || window.app?.visualDesigner;
        const model = designer?.modelManager?.getModel();
        const centerPointConnections = [];
        const nonCenterConnections = [];
        if (model) {
            const connections = Array.from(model.workflow.connections.values());
            connections.forEach(conn => {
                const fromNode = model.workflow.allNodes.get(conn.fromNodeId);
                const toNode = model.workflow.allNodes.get(conn.toNodeId);
                
                // For workflow-start/end, check if they have connection points
                let fromPoint = null;
                let toPoint = null;
                
                if (fromNode) {
                    if (conn.fromNodeId === 'workflow-start') {
                        // workflow-start uses special connection points
                        fromPoint = { id: conn.anchors.from.connectionPointId || 'workflow-start-output' };
                    } else {
                        fromPoint = (fromNode.connectionPoints.outputs || []).find(
                            p => p.id === conn.anchors.from.connectionPointId
                        );
                    }
                }
                
                if (toNode) {
                    if (conn.toNodeId === 'workflow-end') {
                        // workflow-end uses special connection points
                        toPoint = { id: conn.anchors.to.connectionPointId || 'workflow-end-input' };
                    } else {
                        toPoint = (toNode.connectionPoints.inputs || []).find(
                            p => p.id === conn.anchors.to.connectionPointId
                        );
                    }
                }
                
                if (fromPoint && toPoint) {
                    const fromIsCenter = fromPoint.id.includes('-center') || conn.fromNodeId === 'workflow-start';
                    const toIsCenter = toPoint.id.includes('-center') || conn.toNodeId === 'workflow-end';
                    const isCenterConnection = fromIsCenter && toIsCenter;
                    
                    centerPointConnections.push({
                        connectionId: conn.connectionId,
                        fromNodeId: conn.fromNodeId,
                        toNodeId: conn.toNodeId,
                        fromIsCenter: fromIsCenter,
                        toIsCenter: toIsCenter,
                        fromPointId: fromPoint.id,
                        toPointId: toPoint.id,
                        isCenterConnection: isCenterConnection
                    });
                    
                    if (!isCenterConnection) {
                        nonCenterConnections.push({
                            connectionId: conn.connectionId,
                            fromNodeId: conn.fromNodeId,
                            toNodeId: conn.toNodeId,
                            fromPointId: fromPoint.id,
                            toPointId: toPoint.id
                        });
                    }
                }
            });
        }
        
        const paths = svg.querySelectorAll('path.connection-line');
        const results = {
            totalConnections: paths.length,
            orthogonalPaths: 0,
            diagonalPaths: 0,
            issues: []
        };
        
        paths.forEach((path, index) => {
            const pathData = path.getAttribute('d') || '';
            const connectionId = path.getAttribute('data-connection-id') || `conn-${index}`;
            
            // Parse path data to extract segments
            // Check if path uses H (horizontal) and V (vertical) commands
            const hasH = pathData.includes(' H ') || pathData.includes('H ');
            const hasV = pathData.includes(' V ') || pathData.includes('V ');
            const hasL = pathData.includes(' L ') || pathData.includes('L ');
            
            // Count segments
            const hCount = (pathData.match(/[^a-zA-Z]H[^a-zA-Z]/g) || []).length;
            const vCount = (pathData.match(/[^a-zA-Z]V[^a-zA-Z]/g) || []).length;
            const lCount = (pathData.match(/[^a-zA-Z]L[^a-zA-Z]/g) || []).length;
            
            // Check if path is orthogonal
            // Orthogonal paths should use H and V, or L commands that are only horizontal/vertical
            let isOrthogonal = true;
            let diagonalSegments = [];
            
            if (lCount > 0) {
                // If using L commands, check each segment
                const commands = pathData.match(/[MLHV][^MLHV]*/g) || [];
                let prevPoint = null;
                
                for (let i = 0; i < commands.length; i++) {
                    const cmd = commands[i];
                    if (cmd.startsWith('M')) {
                        // Move command - extract coordinates
                        const coords = cmd.substring(1).trim().split(/[,\\s]+/).map(Number).filter(n => !isNaN(n));
                        if (coords.length >= 2) {
                            prevPoint = { x: coords[0], y: coords[1] };
                        }
                    } else if (cmd.startsWith('L')) {
                        // Line command - check if segment is orthogonal
                        const coords = cmd.substring(1).trim().split(/[,\\s]+/).map(Number).filter(n => !isNaN(n));
                        if (coords.length >= 2 && prevPoint) {
                            const currPoint = { x: coords[0], y: coords[1] };
                            const dx = Math.abs(currPoint.x - prevPoint.x);
                            const dy = Math.abs(currPoint.y - prevPoint.y);
                            
                            // Check if segment is diagonal (both dx and dy are non-zero and significant)
                            if (dx > 0.1 && dy > 0.1) {
                                isOrthogonal = false;
                                diagonalSegments.push({
                                    from: prevPoint,
                                    to: currPoint,
                                    dx: dx,
                                    dy: dy
                                });
                            }
                            
                            prevPoint = currPoint;
                        }
                    } else if (cmd.startsWith('H')) {
                        // Horizontal - always orthogonal
                        const coords = cmd.substring(1).trim().split(/[,\\s]+/).map(Number).filter(n => !isNaN(n));
                        if (coords.length >= 1 && prevPoint) {
                            prevPoint = { x: coords[0], y: prevPoint.y };
                        }
                    } else if (cmd.startsWith('V')) {
                        // Vertical - always orthogonal
                        const coords = cmd.substring(1).trim().split(/[,\\s]+/).map(Number).filter(n => !isNaN(n));
                        if (coords.length >= 1 && prevPoint) {
                            prevPoint = { x: prevPoint.x, y: coords[0] };
                        }
                    }
                }
            }
            
            if (isOrthogonal) {
                results.orthogonalPaths++;
            } else {
                results.diagonalPaths++;
                results.issues.push({
                    connectionId,
                    issue: 'Contains diagonal segments',
                    diagonalSegments: diagonalSegments,
                    pathData: pathData.substring(0, 200)
                });
            }
        });
        
        return {
            ...results,
            centerPointConnections: centerPointConnections,
            nonCenterConnections: nonCenterConnections,
            allUseCenterPoints: centerPointConnections.length > 0 && centerPointConnections.every(c => c.isCenterConnection)
        };
        """
        
        result = driver.execute_script(script)
        return result
    except Exception as e:
        return { 'error': str(e) }

def main():
    print("🔍 Testing orthogonal routing in runtime...")
    
    # Setup Chrome
    chrome_options = Options()
    chrome_options.add_argument('--headless')
    chrome_options.add_argument('--no-sandbox')
    chrome_options.add_argument('--disable-dev-shm-usage')
    chrome_options.add_argument('--window-size=1920,1080')
    
    driver = webdriver.Chrome(options=chrome_options)
    
    try:
        # Step 1: Open IDE
        print("\n[1/3] Opening IDE...")
        driver.get(IDE_URL)
        time.sleep(3)
        
        # Step 2: Load configuration
        print("\n[2/3] Loading configuration...")
        if not load_config_via_selenium(driver):
            print("❌ Failed to load configuration")
            return False
        
        time.sleep(3)  # Wait for connections to render
        
        # Step 3: Check orthogonal routing
        print("\n[3/3] Checking orthogonal routing...")
        result = check_orthogonal_routing(driver)
        
        if result.get('error'):
            print(f"❌ Error: {result['error']}")
            return False
        
        print(f"\n📊 Orthogonal Routing Analysis:")
        print(f"  Total connections: {result.get('totalConnections', 0)}")
        print(f"  Orthogonal paths: {result.get('orthogonalPaths', 0)}")
        print(f"  Diagonal paths: {result.get('diagonalPaths', 0)}")
        
        # Check center points usage
        centerPointConnections = result.get('centerPointConnections', [])
        nonCenterConnections = result.get('nonCenterConnections', [])
        allUseCenterPoints = result.get('allUseCenterPoints', False)
        print(f"\n📌 Center Points Usage:")
        centerCount = sum(1 for c in centerPointConnections if c.get('isCenterConnection', False))
        print(f"  Connections using center points: {centerCount}/{len(centerPointConnections)}")
        if nonCenterConnections:
            print(f"  ⚠️  Connections NOT using center points: {len(nonCenterConnections)}")
            for conn in nonCenterConnections:
                print(f"    - {conn.get('connectionId')}: {conn.get('fromNodeId')} -> {conn.get('toNodeId')}")
                print(f"      From point: {conn.get('fromPointId')}")
                print(f"      To point: {conn.get('toPointId')}")
        
        issues = result.get('issues', [])
        if issues:
            print(f"\n  ❌ Routing issues found: {len(issues)}")
            for issue in issues:
                print(f"    - {issue.get('connectionId')}: {issue.get('issue')}")
                if issue.get('diagonalSegments'):
                    for seg in issue.get('diagonalSegments', [])[:2]:  # Show first 2
                        from_pt = seg.get('from', {})
                        to_pt = seg.get('to', {})
                        print(f"      Diagonal: ({from_pt.get('x', 0):.1f}, {from_pt.get('y', 0):.1f}) -> ({to_pt.get('x', 0):.1f}, {to_pt.get('y', 0):.1f})")
                print(f"      Path data: {issue.get('pathData', '')[:100]}...")
            return False
        
        if not allUseCenterPoints:
            print(f"\n  ❌ Not all connections use center points!")
            return False
        
        if result.get('diagonalPaths', 0) > 0:
            print(f"\n  ❌ Some paths have diagonal segments!")
            return False
        
        print(f"\n  ✅ All paths are orthogonal and use center points!")
        return True
        
    finally:
        driver.quit()

if __name__ == '__main__':
    success = main()
    exit(0 if success else 1)

