#!/usr/bin/env python3
"""
Test wizualnego renderowania linii w Selenium Chrome
Sprawdza czy linie są rysowane prawidłowo:
1. Linie wychodzą z dokładnie środkowych punktów
2. Routing jest orthogonal (tylko poziome/pionowe)
3. Linie są widoczne i poprawnie połączone
"""

import time
import sys
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
    <def var="url3">https://httpbin.org/xml</def>
    <def var="xml3">
        <http url="${url3}"/>
    </def>
</config>"""

def load_config_via_selenium(driver):
    """Load configuration via Selenium"""
    try:
        print("📝 Loading configuration via Selenium...")
        driver.get(f"{IDE_URL}?nocache={int(time.time())}")
        time.sleep(5)  # Wait longer for page to load
        
        # Switch to visual mode - try multiple approaches
        print("   Switching to visual mode...")
        wait = WebDriverWait(driver, 20)
        
        # First, try to find and click visual mode button
        visual_mode_switched = False
        try:
            # Try various selectors for visual mode button
            selectors = [
                "button[data-mode='visual']",
                ".visual-mode-button",
                "#visual-mode-btn",
                "button:contains('Visual')",
                "[data-action='visual']",
                ".mode-switcher button[data-mode='visual']"
            ]
            
            for selector in selectors:
                try:
                    visual_button = wait.until(
                        EC.element_to_be_clickable((By.CSS_SELECTOR, selector))
                    )
                    driver.execute_script("arguments[0].scrollIntoView(true);", visual_button)
                    time.sleep(0.5)
                    visual_button.click()
                    time.sleep(2)
                    visual_mode_switched = True
                    print(f"   ✅ Clicked visual mode button (selector: {selector})")
                    break
                except:
                    continue
        except Exception as e:
            print(f"   ⚠️ Could not click visual button: {e}")
        
        # Always use UIManager to ensure visual mode
        print("   Using UIManager to switch to visual mode...")
        driver.execute_script('''
            const app = window.app?._instance || window.app;
            if (app && app.uiManager) {
                // Force hide editor and show canvas
                app.uiManager.showEditor(false);
                app.uiManager.showVisualCanvas(true);
                console.log('UIManager: Editor hidden, Canvas shown');
            }
            // Also call switchToVisualMode if available
            if (app && app.switchToVisualMode) {
                app.switchToVisualMode();
            }
        ''')
        time.sleep(3)
        
        # Verify we're in visual mode - check that XML editor is HIDDEN and visual canvas is VISIBLE
        is_visual_mode = driver.execute_script('''
            // Check if visual mode is active
            const canvas = document.getElementById("visual-canvas") || 
                          document.querySelector("#canvas-area, .visual-canvas, [id*='canvas']");
            const visualDesigner = window.app?._instance?.visualDesigner;
            
            // Check if XML editor is visible (should be HIDDEN in visual mode)
            const editor = document.getElementById("editor");
            const editorVisible = editor ? window.getComputedStyle(editor).display !== 'none' && 
                                           window.getComputedStyle(editor).visibility !== 'hidden' : false;
            
            // Check if visual canvas is visible (should be VISIBLE in visual mode)
            const canvasVisible = canvas ? window.getComputedStyle(canvas).display !== 'none' && 
                                            window.getComputedStyle(canvas).visibility !== 'hidden' : false;
            
            // Check current mode
            const app = window.app?._instance || window.app;
            const currentMode = app?.currentMode || (app?.isVisualMode ? 'visual' : 'xml');
            
            return {
                hasCanvas: !!canvas,
                hasVisualDesigner: !!visualDesigner,
                canvasVisible: canvasVisible,
                editorVisible: editorVisible,
                currentMode: currentMode,
                isVisualMode: !editorVisible && canvasVisible && !!visualDesigner
            };
        ''')
        print(f"   Visual mode check: {is_visual_mode}")
        
        if not is_visual_mode.get('isVisualMode'):
            print("   ⚠️ Editor still visible or canvas hidden. Retrying...")
            # Force switch using UIManager again
            driver.execute_script('''
                const app = window.app?._instance || window.app;
                if (app && app.uiManager) {
                    app.uiManager.showEditor(false);
                    app.uiManager.showVisualCanvas(true);
                }
            ''')
            time.sleep(2)
            
            # Final check
            is_visual_mode = driver.execute_script('''
                const canvas = document.getElementById("visual-canvas");
                const editor = document.getElementById("editor");
                const editorVisible = editor ? window.getComputedStyle(editor).display !== 'none' : false;
                const canvasVisible = canvas ? window.getComputedStyle(canvas).display !== 'none' : false;
                return {
                    isVisualMode: !editorVisible && canvasVisible,
                    editorVisible: editorVisible,
                    canvasVisible: canvasVisible
                };
            ''')
            print(f"   Final check: {is_visual_mode}")
            
            if not is_visual_mode.get('isVisualMode'):
                print("   ❌ WARNING: May not be in visual mode, but continuing test...")
        
        time.sleep(2)
        
        # Load configuration - use the same approach as test_semantic_connection_point_selection.py
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
        time.sleep(3)
        
        print("✅ Configuration loaded")
        return True
    except Exception as e:
        print(f"❌ Error loading configuration: {e}")
        return False

def verify_visual_rendering(driver):
    """Verify visual rendering of connections"""
    try:
        print("\n🎨 Verifying visual rendering...")
        
        # Enable debug mode
        driver.execute_script('window.__DEBUG_SEMANTIC_SELECTION__ = true;')
        
        # Wait for designer to be ready
        time.sleep(2)
        
        # Get semantic model - use the same approach as test_semantic_connection_point_selection.py
        model = driver.execute_script('''
            const designer = window.app?._instance?.visualDesigner || window.app?.visualDesigner;
            if (!designer || !designer.modelManager) {
                return null;
            }
            
            // Enable debug logging
            window.__DEBUG_SEMANTIC_SELECTION__ = true;
            
            // Force rebuild model to ensure latest state
            const model = designer.modelManager.getModel();
            if (!model) {
                return null;
            }
            
            // Convert to JSON-serializable format
            const nodes = Array.from(model.workflow.allNodes.values());
            const connections = Array.from(model.workflow.connections.values());
            
            return {
                nodes: nodes,
                connections: connections
            };
        ''')
        
        if not model:
            print("❌ Could not get semantic model")
            print("   Debugging...")
            # Debug: check what's available
            debug_info = driver.execute_script('''
                return {
                    hasApp: !!window.app,
                    hasInstance: !!(window.app && window.app._instance),
                    hasVisualDesigner: !!(window.app?._instance?.visualDesigner || window.app?.visualDesigner),
                    hasModelManager: !!(window.app?._instance?.visualDesigner?.modelManager || window.app?.visualDesigner?.modelManager)
                };
            ''')
            print(f"   Debug info: {debug_info}")
            return False
        
        # Get all SVG paths
        paths = driver.execute_script('''
            const svg = document.querySelector("svg, #visual-canvas svg, canvas svg");
            if (!svg) return [];
            
            const paths = Array.from(svg.querySelectorAll("path.connection-line, path[data-connection-id]"));
            return paths.map(p => ({
                id: p.getAttribute("data-connection-id") || p.id || "",
                d: p.getAttribute("d") || "",
                stroke: p.getAttribute("stroke") || window.getComputedStyle(p).stroke,
                strokeWidth: p.getAttribute("stroke-width") || window.getComputedStyle(p).strokeWidth,
                visible: window.getComputedStyle(p).visibility !== "hidden" && window.getComputedStyle(p).display !== "none"
            }));
        ''')
        
        # Get all connection points - try multiple selectors
        connection_points = driver.execute_script('''
            // Try multiple ways to find connection points
            let container = document.getElementById("connection-points-layer");
            if (!container) {
                // Try to find by class or other attributes
                container = document.querySelector(".connection-points-layer, #connection-points-layer, [id*='connection-points']");
            }
            
            if (!container) {
                // Try to find all divs with connection point data
                const allPoints = Array.from(document.querySelectorAll("div[data-point-id], div[class*='connection-point'], div[id*='connection-point']"));
                return allPoints.map(p => ({
                    id: p.getAttribute("data-point-id") || p.id || "",
                    left: parseInt(p.style.left) || p.offsetLeft || 0,
                    top: parseInt(p.style.top) || p.offsetTop || 0,
                    width: parseInt(p.style.width) || p.offsetWidth || 0,
                    height: parseInt(p.style.height) || p.offsetHeight || 0,
                    visible: window.getComputedStyle(p).visibility !== "hidden" && window.getComputedStyle(p).display !== "none",
                    isCenter: (p.getAttribute("data-point-id") || p.id || "").includes("-center")
                }));
            }
            
            const points = Array.from(container.querySelectorAll("div[data-point-id], div[class*='connection-point'], div[id*='connection-point']"));
            return points.map(p => ({
                id: p.getAttribute("data-point-id") || p.id || "",
                left: parseInt(p.style.left) || p.offsetLeft || 0,
                top: parseInt(p.style.top) || p.offsetTop || 0,
                width: parseInt(p.style.width) || p.offsetWidth || 0,
                height: parseInt(p.style.height) || p.offsetHeight || 0,
                visible: window.getComputedStyle(p).visibility !== "hidden" && window.getComputedStyle(p).display !== "none",
                isCenter: (p.getAttribute("data-point-id") || p.id || "").includes("-center")
            }));
        ''')
        
        print(f"\n📊 Visual Rendering Analysis:")
        print(f"  SVG paths found: {len(paths)}")
        print(f"  Connection points found: {len(connection_points)}")
        
        # Model is already converted to JSON-serializable format
        connections = model.get('connections', [])
        if not isinstance(connections, list):
            connections = []
        
        print(f"  Connections in model: {len(connections)}")
        
        # Verify each connection
        issues = []
        verified = 0
        
        if connections:
            for conn in connections:
                conn_id = conn.get('connectionId', '') or conn.get('id', '')
                from_node = conn.get('fromNodeId', '') or conn.get('from', '')
                to_node = conn.get('toNodeId', '') or conn.get('to', '')
                
                # Find corresponding SVG path
                path = next((p for p in paths if p['id'] == conn_id or conn_id in p['id']), None)
                
                if not path:
                    issues.append(f"Connection {conn_id}: No SVG path found")
                    continue
                
                if not path['visible']:
                    issues.append(f"Connection {conn_id}: Path is not visible")
                    continue
                
                # Check if path data is valid
                if not path['d'] or len(path['d']) < 10:
                    issues.append(f"Connection {conn_id}: Invalid path data")
                    continue
                
                # Verify path starts and ends at center points
                path_start, path_end = driver.execute_script(f'''
                    const d = {repr(path['d'])};
                    const commands = d.match(/[MmLlHhVv][^MmLlHhVv]*/g) || [];
                    if (commands.length === 0) return [null, null];
                    
                    // Get first point (M command)
                    const firstCmd = commands[0];
                    const firstMatch = firstCmd.match(/[MmLl]\\s+([\\d.]+)\\s+([\\d.]+)/);
                    const firstPoint = firstMatch ? {{x: parseFloat(firstMatch[1]), y: parseFloat(firstMatch[2])}} : null;
                    
                    // Get last point
                    let lastPoint = null;
                    let currentPoint = firstPoint;
                    for (let i = 1; i < commands.length; i++) {{
                        const cmd = commands[i];
                        if (cmd.startsWith("H")) {{
                            const x = parseFloat(cmd.substring(1).trim());
                            currentPoint = {{x: x, y: currentPoint.y}};
                        }} else if (cmd.startsWith("V")) {{
                            const y = parseFloat(cmd.substring(1).trim());
                            currentPoint = {{x: currentPoint.x, y: y}};
                        }} else if (cmd.startsWith("L")) {{
                            const match = cmd.match(/L\\s+([\\d.]+)\\s+([\\d.]+)/);
                            if (match) {{
                                currentPoint = {{x: parseFloat(match[1]), y: parseFloat(match[2])}};
                            }}
                        }}
                        lastPoint = currentPoint;
                    }}
                    
                    return [firstPoint, lastPoint || firstPoint];
                ''')
                
                # Find center points for this connection
                from_center = next((p for p in connection_points if p['isCenter'] and from_node in p['id']), None)
                to_center = next((p for p in connection_points if p['isCenter'] and to_node in p['id']), None)
                
                if from_center and path_start:
                    from_center_pos = {
                        'x': from_center['left'] + from_center['width'] / 2,
                        'y': from_center['top'] + from_center['height'] / 2
                    }
                    dist_start = ((path_start['x'] - from_center_pos['x'])**2 + (path_start['y'] - from_center_pos['y'])**2)**0.5
                    if dist_start > 5:  # 5px tolerance
                        issues.append(f"Connection {conn_id}: Path start ({path_start['x']:.1f}, {path_start['y']:.1f}) is {dist_start:.1f}px away from center point ({from_center_pos['x']:.1f}, {from_center_pos['y']:.1f})")
                
                if to_center and path_end:
                    to_center_pos = {
                        'x': to_center['left'] + to_center['width'] / 2,
                        'y': to_center['top'] + to_center['height'] / 2
                    }
                    dist_end = ((path_end['x'] - to_center_pos['x'])**2 + (path_end['y'] - to_center_pos['y'])**2)**0.5
                    if dist_end > 5:  # 5px tolerance
                        issues.append(f"Connection {conn_id}: Path end ({path_end['x']:.1f}, {path_end['y']:.1f}) is {dist_end:.1f}px away from center point ({to_center_pos['x']:.1f}, {to_center_pos['y']:.1f})")
                
                # Verify path is orthogonal
                is_orthogonal = driver.execute_script(f'''
                    const d = {repr(path['d'])};
                    const commands = d.match(/[MmLlHhVv][^MmLlHhVv]*/g) || [];
                    let prevPoint = null;
                    let hasDiagonal = false;
                    
                    for (let cmd of commands) {{
                        if (cmd.startsWith("M") || cmd.startsWith("m")) {{
                            const match = cmd.match(/[Mm]\\s+([\\d.]+)\\s+([\\d.]+)/);
                            if (match) {{
                                prevPoint = {{x: parseFloat(match[1]), y: parseFloat(match[2])}};
                            }}
                        }} else if (cmd.startsWith("H") || cmd.startsWith("h")) {{
                            // Horizontal - always orthogonal
                            const x = parseFloat(cmd.substring(1).trim());
                            if (prevPoint) prevPoint.x = x;
                        }} else if (cmd.startsWith("V") || cmd.startsWith("v")) {{
                            // Vertical - always orthogonal
                            const y = parseFloat(cmd.substring(1).trim());
                            if (prevPoint) prevPoint.y = y;
                        }} else if (cmd.startsWith("L") || cmd.startsWith("l")) {{
                            const match = cmd.match(/[Ll]\\s+([\\d.]+)\\s+([\\d.]+)/);
                            if (match && prevPoint) {{
                                const currPoint = {{x: parseFloat(match[1]), y: parseFloat(match[2])}};
                                const dx = Math.abs(currPoint.x - prevPoint.x);
                                const dy = Math.abs(currPoint.y - prevPoint.y);
                                if (dx > 0.1 && dy > 0.1) {{
                                    hasDiagonal = true;
                                }}
                                prevPoint = currPoint;
                            }}
                        }}
                    }}
                    
                    return !hasDiagonal;
                ''')
                
                if not is_orthogonal:
                    issues.append(f"Connection {conn_id}: Path contains diagonal segments")
                
                if not issues or not any(i.startswith(f"Connection {conn_id}") for i in issues):
                    verified += 1
        
        # Take screenshot
        screenshot_path = '/tmp/visual_rendering_test.png'
        driver.save_screenshot(screenshot_path)
        print(f"\n📸 Screenshot saved to: {screenshot_path}")
        
        # Print results
        print(f"\n✅ Verified connections: {verified}/{len(connections)}")
        if issues:
            print(f"\n❌ Issues found: {len(issues)}")
            for issue in issues[:10]:  # Show first 10 issues
                print(f"  - {issue}")
            if len(issues) > 10:
                print(f"  ... and {len(issues) - 10} more issues")
        else:
            print("\n✅ All connections render correctly!")
        
        return len(issues) == 0
        
    except Exception as e:
        print(f"❌ Error verifying visual rendering: {e}")
        import traceback
        traceback.print_exc()
        return False

def main():
    print("🎨 Testing visual rendering in Selenium Chrome...")
    
    # Setup Chrome - NOT headless so we can see what's happening
    chrome_options = Options()
    # chrome_options.add_argument('--headless')  # Commented out for visual inspection
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
        
        time.sleep(5)  # Wait for connections to render
        
        # Step 3: Verify visual rendering
        print("\n[3/3] Verifying visual rendering...")
        success = verify_visual_rendering(driver)
        
        if success:
            print("\n✅ Visual rendering test PASSED")
            print("📸 Check screenshot at /tmp/visual_rendering_test.png")
        else:
            print("\n❌ Visual rendering test FAILED")
            print("📸 Check screenshot at /tmp/visual_rendering_test.png for details")
        
        # Keep browser open for manual inspection
        print("\n🔍 Browser will stay open for 30 seconds for manual inspection...")
        time.sleep(30)
        
        return success
        
    except Exception as e:
        print(f"❌ Test failed with error: {e}")
        import traceback
        traceback.print_exc()
        return False
    finally:
        driver.quit()

if __name__ == "__main__":
    success = main()
    sys.exit(0 if success else 1)

