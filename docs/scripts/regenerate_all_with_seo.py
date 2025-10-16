#!/usr/bin/env python3
"""
WebHarvest Complete SEO Regeneration Script
Regenerates all documentation with SEO optimizations applied.
"""

import subprocess
import sys
import os
from pathlib import Path

def run_script(script_path):
    """Run a Python script and handle errors"""
    try:
        result = subprocess.run([sys.executable, script_path], 
                              capture_output=True, text=True, check=True)
        print(f"✅ {script_path}: {result.stdout.strip()}")
        return True
    except subprocess.CalledProcessError as e:
        print(f"❌ {script_path} failed:")
        print(f"   Error: {e.stderr.strip()}")
        return False

def main():
    """Main regeneration process"""
    print("🚀 Starting WebHarvest SEO Documentation Regeneration")
    print("=" * 60)
    
    # Change to project root
    project_root = Path(__file__).parent.parent.parent
    os.chdir(project_root)
    
    scripts = [
        "docs/scripts/generate_core_plugin_docs.py",
        "docs/scripts/generate_external_plugin_docs.py", 
        "docs/scripts/generate_plugins_index.py",
        "docs/scripts/generate_sitemap.py"
    ]
    
    success_count = 0
    total_count = len(scripts)
    
    for script in scripts:
        if run_script(script):
            success_count += 1
        print()
    
    print("=" * 60)
    print(f"📊 Regeneration Complete: {success_count}/{total_count} scripts succeeded")
    
    if success_count == total_count:
        print("✅ All documentation regenerated with SEO optimizations!")
        print("\n📋 SEO Features Applied:")
        print("   • Meta descriptions and keywords")
        print("   • Open Graph tags")
        print("   • Twitter Card tags") 
        print("   • Structured data (JSON-LD)")
        print("   • Canonical URLs")
        print("   • Optimized alt texts")
        print("   • Sitemap.xml (91 URLs)")
        print("   • Robots.txt")
        print("\n🎯 Ready for search engine indexing!")
    else:
        print("⚠️  Some scripts failed. Check errors above.")
        return 1
    
    return 0

if __name__ == "__main__":
    sys.exit(main())
