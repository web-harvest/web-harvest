#!/usr/bin/env python3
"""
WebHarvest Complete Plugin Documentation Generator
Generates HTML documentation for ALL plugins (core + external)

Usage:
    python3 generate_all_plugin_docs.py

Generates:
    - 46 core plugin pages (http, xpath, loop, etc.)
    - 8 external plugin pages (mail, ftp, zip, web-browser)
    - Total: 54 plugin documentation pages
"""

import subprocess
import sys
from pathlib import Path

def run_script(script_name: str) -> bool:
    """Run a Python script and return success status."""
    script_path = Path(__file__).parent / script_name
    
    if not script_path.exists():
        print(f"❌ Script not found: {script_name}")
        return False
    
    print(f"\n{'='*60}")
    print(f"Running: {script_name}")
    print('='*60)
    
    result = subprocess.run([sys.executable, str(script_path)])
    
    return result.returncode == 0

def main():
    print("=" * 60)
    print("🚀 WebHarvest Complete Plugin Documentation Generator")
    print("=" * 60)
    print()
    print("📌 This will generate documentation for:")
    print("   • 46 Core plugins (built-in, @CorePlugin)")
    print("   • 8 External plugins (mail, ftp, zip, web-browser)")
    print("   • Total: 54 plugin pages")
    print()
    
    success = True
    
    # Step 1: Generate core plugin docs
    if not run_script("generate_core_plugin_docs.py"):
        print("\n❌ Failed to generate core plugin documentation")
        success = False
    
    # Step 2: Generate external plugin docs
    if not run_script("generate_external_plugin_docs.py"):
        print("\n❌ Failed to generate external plugin documentation")
        success = False
    
    # Step 3: Generate plugins index page
    if not run_script("generate_plugins_index.py"):
        print("\n❌ Failed to generate plugins index page")
        success = False
    
    # Summary
    print("\n" + "=" * 60)
    if success:
        print("✅ ALL PLUGIN DOCUMENTATION GENERATED SUCCESSFULLY!")
        print("=" * 60)
        print()
        print("📂 Output locations:")
        print("   • Index page: docs/web/documentation/2.2.0/index.html")
        print("   • Core plugins: docs/web/documentation/2.2.0/*.html (46 files)")
        print("   • External plugins: docs/web/documentation/2.2.0/external/*.html (8 files)")
        print()
        print("🌐 View documentation:")
        print("   cd docs/web && python3 -m http.server 8000")
        print("   Then open: http://localhost:8000/documentation/2.2.0/")
        print()
        print("📋 Complete plugin reference:")
        print("   • Core: 46 plugins")
        print("   • External: 8 plugins")
        print("   • Total: 54 plugins")
    else:
        print("❌ SOME ERRORS OCCURRED - CHECK OUTPUT ABOVE")
        print("=" * 60)
        sys.exit(1)

if __name__ == "__main__":
    main()

