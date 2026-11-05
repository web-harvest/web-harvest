#!/usr/bin/env python3
"""
Test wizualny routingu połączeń
Sprawdza czy routing jest czytelny i uporządkowany
"""

import time
from selenium import webdriver
from selenium.webdriver.chrome.options import Options
from selenium.webdriver.common.by import By
from selenium.webdriver.support.ui import WebDriverWait
from selenium.webdriver.support import expected_conditions as EC

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
    """Load configuration via JavaScript"""
    try:
        script = f"""
        const xml = `{TEST_CONFIG_XML}`;
        const parser = new DOMParser();
        const doc = parser.parseFromString(xml, 'text/xml');
        const nodes = [];
        
        function parseElement(element) {{
            const node = {{
                id: element.getAttribute('var') || element.tagName,
                type: element.tagName,
                attributes: {{}},
                body: null,
                children: []
            }};
            
            for (let attr of element.attributes) {{
                if (attr.name !== 'var') {{
                    node.attributes[attr.name] = attr.value;
                }}
            }}
            
            if (element.children.length > 0) {{
                for (let child of element.children) {{
                    node.children.push(parseElement(child));
                }}
            }} else {{
                node.body = element.textContent.trim() || null;
            }}
            
            return node;
        }}
        
        const root = doc.documentElement;
        for (let child of root.children) {{
            nodes.push(parseElement(child));
        }}
        
        const designer = window.app?._instance;
        if (designer && designer.loadNodes) {{
            designer.loadNodes(nodes);
            return true;
        }}
        return false;
        """
        
        result = driver.execute_script(script)
        time.sleep(2)  # Wait for rendering
        return result
    except Exception as e:
        print(f"❌ Error loading config: {e}")
        return False

def check_routing_quality(driver):
    """Check routing quality by analyzing SVG paths"""
    try:
        script = """
        const svg = document.querySelector('svg.connection-layer');
        if (!svg) return { error: 'No SVG found' };
        
        const paths = svg.querySelectorAll('path.connection-line');
        const results = {
            totalConnections: paths.length,
            paths: [],
            issues: []
        };
        
        paths.forEach((path, index) => {
            const pathData = path.getAttribute('d') || '';
            const connectionId = path.getAttribute('data-connection-id') || `conn-${index}`;
            
            // Parse path data to count points
            const commands = pathData.match(/[ML][^ML]*/g) || [];
            const pointCount = commands.length;
            
            // Check if path is simple (orthogonal routing should have 2-6 points)
            const isSimple = pointCount >= 2 && pointCount <= 6;
            
            // Check for very long paths (might indicate routing issues)
            const isReasonable = pointCount <= 10;
            
            results.paths.push({
                connectionId,
                pointCount,
                isSimple,
                isReasonable,
                pathData: pathData.substring(0, 100)  // First 100 chars
            });
            
            if (!isSimple) {
                results.issues.push({
                    connectionId,
                    issue: `Too many points: ${pointCount} (expected 2-6)`,
                    severity: 'warning' if isReasonable else 'error'
                });
            }
        });
        
        return results;
        """
        
        result = driver.execute_script(script)
        return result
    except Exception as e:
        return { 'error': str(e) }

def main():
    print("🔍 Testing routing quality...")
    
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
            return
        
        time.sleep(3)  # Wait for connections to render
        
        # Step 3: Check routing quality
        print("\n[3/3] Checking routing quality...")
        result = check_routing_quality(driver)
        
        if result.get('error'):
            print(f"❌ Error: {result['error']}")
            return
        
        print(f"\n📊 Routing Analysis:")
        print(f"  Total connections: {result.get('totalConnections', 0)}")
        
        paths = result.get('paths', [])
        if paths:
            simple_count = sum(1 for p in paths if p.get('isSimple', False))
            reasonable_count = sum(1 for p in paths if p.get('isReasonable', False))
            
            print(f"  Simple paths (2-6 points): {simple_count}/{len(paths)}")
            print(f"  Reasonable paths (≤10 points): {reasonable_count}/{len(paths)}")
            
            print(f"\n  Path details:")
            for path in paths:
                status = "✅" if path.get('isSimple') else "⚠️" if path.get('isReasonable') else "❌"
                print(f"    {status} {path.get('connectionId')}: {path.get('pointCount')} points")
        
        issues = result.get('issues', [])
        if issues:
            print(f"\n  ⚠️  Issues found: {len(issues)}")
            for issue in issues:
                print(f"    {issue.get('severity', 'warning').upper()}: {issue.get('connectionId')} - {issue.get('issue')}")
        else:
            print(f"\n  ✅ No routing issues found!")
        
        # Take screenshot for visual inspection
        screenshot_path = '/tmp/routing_test.png'
        driver.save_screenshot(screenshot_path)
        print(f"\n  📸 Screenshot saved to: {screenshot_path}")
        
    finally:
        driver.quit()

if __name__ == '__main__':
    main()





