#!/usr/bin/env python3
"""
Test strzałek w połączeniach używając Selenium Chrome
"""
import time
from selenium import webdriver
from selenium.webdriver.chrome.service import Service
from selenium.webdriver.chrome.options import Options
from selenium.webdriver.common.by import By
from selenium.webdriver.support.ui import WebDriverWait
from selenium.webdriver.support import expected_conditions as EC
from selenium.common.exceptions import TimeoutException

def test_arrows():
    print("🧪 Test strzałek w połączeniach")
    print("=" * 50)
    
    # Setup Chrome options
    chrome_options = Options()
    chrome_options.add_argument('--headless')  # Run in background
    chrome_options.add_argument('--no-sandbox')
    chrome_options.add_argument('--disable-dev-shm-usage')
    chrome_options.add_argument('--window-size=1920,1080')
    
    driver = None
    try:
        # Start Chrome
        print("🚀 Uruchamianie Chrome...")
        driver = webdriver.Chrome(options=chrome_options)
        driver.set_page_load_timeout(10)
        
        # Open the page
        url = "http://localhost:8080/index.html"
        print(f"📄 Otwieranie: {url}")
        driver.get(url)
        time.sleep(2)  # Wait for page to load
        
        # Check if page loaded
        print("✅ Strona załadowana")
        
        # Check for JavaScript errors
        logs = driver.get_log('browser')
        errors = [log for log in logs if log['level'] == 'SEVERE']
        if errors:
            print(f"⚠️  Znaleziono {len(errors)} błędów JavaScript:")
            for error in errors[:5]:
                print(f"   {error['message']}")
        else:
            print("✅ Brak błędów JavaScript")
        
        # Check if app is initialized
        app_exists = driver.execute_script("return typeof app !== 'undefined';")
        if app_exists:
            print("✅ Obiekt 'app' istnieje")
        else:
            print("❌ Obiekt 'app' NIE istnieje")
            return
        
        # Add two nodes
        print("\n📝 Dodawanie etykiet...")
        add_button = driver.find_element(By.ID, "btn-add-node")
        add_button.click()
        time.sleep(0.5)
        add_button.click()
        time.sleep(0.5)
        
        # Check nodes count
        nodes_count = driver.execute_script("return app.nodes ? app.nodes.size : 0;")
        print(f"✅ Utworzono {nodes_count} etykiet")
        
        if nodes_count < 2:
            print("❌ Za mało etykiet do testu")
            return
        
        # Get node positions
        node_positions = driver.execute_script("""
            const nodes = [];
            app.nodes.forEach((node, id) => {
                const el = document.getElementById('node-' + id);
                if (el) {
                    const rect = el.getBoundingClientRect();
                    nodes.push({
                        id: id,
                        x: rect.left + rect.width / 2,
                        y: rect.top + rect.height / 2,
                        rect: {left: rect.left, top: rect.top, width: rect.width, height: rect.height}
                    });
                }
            });
            return nodes;
        """)
        
        print(f"📊 Pozycje etykiet: {len(node_positions)}")
        for node in node_positions:
            print(f"   Node {node['id']}: ({node['x']:.0f}, {node['y']:.0f})")
        
        if len(node_positions) < 2:
            print("❌ Za mało etykiet z pozycjami")
            return
        
        # Create connection between first two nodes
        print("\n🔗 Tworzenie połączenia...")
        node1_id = node_positions[0]['id']
        node2_id = node_positions[1]['id']
        
        # Find connection points
        connection_created = driver.execute_script(f"""
            try {{
                // Get connection points for node1 (right side)
                const node1El = document.getElementById('node-{node1_id}');
                const node2El = document.getElementById('node-{node2_id}');
                
                if (!node1El || !node2El) {{
                    return {{success: false, error: 'Nodes not found'}};
                }}
                
                // Create connection programmatically
                app.createConnection('{node1_id}', 'right', '{node2_id}', 'left');
                
                // Wait a bit for rendering
                return new Promise((resolve) => {{
                    setTimeout(() => {{
                        const connections = app.connections ? app.connections.size : 0;
                        resolve({{success: true, connections: connections}});
                    }}, 500);
                }});
            }} catch (e) {{
                return {{success: false, error: e.message}};
            }}
        """)
        
        time.sleep(1)  # Wait for rendering
        
        if connection_created.get('success'):
            print(f"✅ Połączenie utworzone (łącznie: {connection_created.get('connections', 0)})")
        else:
            print(f"❌ Błąd tworzenia połączenia: {connection_created.get('error', 'Unknown')}")
            return
        
        # Check for connection paths
        paths = driver.find_elements(By.CSS_SELECTOR, ".connection-line")
        print(f"📊 Znaleziono {len(paths)} ścieżek połączeń")
        
        if len(paths) == 0:
            print("❌ Brak ścieżek połączeń!")
            return
        
        # Check marker-end attribute
        for i, path in enumerate(paths):
            marker_end = path.get_attribute('marker-end')
            path_d = path.get_attribute('d')
            print(f"\n🔍 Ścieżka {i+1}:")
            print(f"   marker-end: {marker_end}")
            print(f"   path d: {path_d[:100]}..." if path_d and len(path_d) > 100 else f"   path d: {path_d}")
            
            if marker_end:
                print(f"   ✅ Marker-end ustawiony: {marker_end}")
            else:
                print(f"   ❌ Marker-end NIE ustawiony!")
        
        # Check if marker exists in SVG
        marker = driver.execute_script("""
            const marker = document.querySelector('#arrowhead');
            if (marker) {
                return {
                    exists: true,
                    id: marker.getAttribute('id'),
                    refX: marker.getAttribute('refX'),
                    refY: marker.getAttribute('refY'),
                    orient: marker.getAttribute('orient')
                };
            }
            return {exists: false};
        """)
        
        print(f"\n🎯 Marker w SVG:")
        if marker.get('exists'):
            print(f"   ✅ Marker istnieje")
            print(f"   refX: {marker.get('refX')}")
            print(f"   refY: {marker.get('refY')}")
            print(f"   orient: {marker.get('orient')}")
        else:
            print(f"   ❌ Marker NIE istnieje!")
        
        # Take screenshot
        screenshot_path = "/tmp/design-system-arrows-test.png"
        driver.save_screenshot(screenshot_path)
        print(f"\n📸 Zrzut ekranu zapisany: {screenshot_path}")
        
        print("\n" + "=" * 50)
        print("✅ Test zakończony")
        
    except TimeoutException:
        print("❌ Timeout - strona nie załadowała się w czasie")
    except Exception as e:
        print(f"❌ Błąd: {e}")
        import traceback
        traceback.print_exc()
    finally:
        if driver:
            driver.quit()
            print("🔒 Chrome zamknięty")

if __name__ == "__main__":
    test_arrows()




