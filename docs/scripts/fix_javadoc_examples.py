#!/usr/bin/env python3
"""
Fix Javadoc examples in plugins by adding <config> wrapper.

Problem: All examples in Javadoc start with <def>, <http>, etc.
         Missing: <config xmlns="http://org.webharvest/schema/2.1/core">

Solution: Wrap each {@code ...} example block with proper config tag.
"""

import os
import re
import sys

def fix_javadoc_example(match):
    """
    Fix a single {@code ...} example block by adding <config> wrapper.
    """
    pre_code = match.group(1)  # The "pre" part before {@code
    code_block = match.group(2)  # The code inside {@code ...}
    
    # Check if already has <config tag
    if '<config' in code_block or '<?xml version' in code_block:
        # Already has config wrapper
        return match.group(0)
    
    # Check if this is a code example (contains XML tags like <def, <http, etc.)
    if not any(tag in code_block for tag in ['<def', '<http', '<xpath', '<xquery', '<loop', '<script', '<template', '<file', '<get', '<set-var']):
        # Not a configuration example (might be output, JSON, etc.)
        return match.group(0)
    
    # Build wrapped code with proper Javadoc comment formatting
    result = f"{pre_code}{{@code\n"
    result += " * <?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
    result += " * <config xmlns=\"http://org.webharvest/schema/2.1/core\">\n"
    result += code_block  # Keep original formatting (already has " * " prefixes)
    result += "\n * </config>\n"
    result += " * }"
    
    return result


def fix_plugin_javadoc(file_path):
    """
    Fix all Javadoc examples in a plugin file.
    """
    print(f"Processing: {file_path}")
    
    with open(file_path, 'r', encoding='utf-8') as f:
        content = f.read()
    
    original_content = content
    
    # Pattern: <pre>{@code ... }</pre>
    # Capture the code block and add config wrapper
    pattern = r'(\* <pre>)\{@code\n(.*?)\n \* \}'
    
    content = re.sub(pattern, fix_javadoc_example, content, flags=re.DOTALL)
    
    if content != original_content:
        with open(file_path, 'w', encoding='utf-8') as f:
            f.write(content)
        print(f"  ✓ Fixed examples in {os.path.basename(file_path)}")
        return True
    else:
        print(f"  - No changes needed in {os.path.basename(file_path)}")
        return False


def main():
    """
    Fix all core and external plugin Javadoc files.
    """
    # Directories to process
    plugin_dirs = [
        'webharvest-core/src/main/java/org/webharvest/plugin/core',
        'webharvest-database/src/main/java/org/webharvest/plugin/db',
        'webharvest-ftp/src/main/java/org/webharvest/plugin/ftp',
        'webharvest-mail/src/main/java/org/webharvest/plugin/mail',
        'webharvest-webbrowser/src/main/java/org/webharvest/plugin/webbrowser',
        'webharvest-zip/src/main/java/org/webharvest/plugin/zip'
    ]
    
    fixed_count = 0
    total_count = 0
    
    for plugin_dir in plugin_dirs:
        if not os.path.exists(plugin_dir):
            print(f"Skipping {plugin_dir} (not found)")
            continue
            
        print(f"\n📁 Processing: {plugin_dir}")
        
        for filename in sorted(os.listdir(plugin_dir)):
            if filename.endswith('Plugin.java'):
                file_path = os.path.join(plugin_dir, filename)
                total_count += 1
                
                if fix_plugin_javadoc(file_path):
                    fixed_count += 1
    
    print(f"\n✓ Processed {total_count} plugin files")
    print(f"✓ Fixed {fixed_count} files with Javadoc examples")
    print(f"\nNext steps:")
    print(f"1. Review changes: svn diff")
    print(f"2. Rebuild documentation:")
    print(f"   cd docs/scripts && python3 generate_core_plugin_docs.py")
    print(f"   cd docs/scripts && python3 generate_external_plugin_docs.py")
    print(f"3. Commit changes: svn commit")


if __name__ == '__main__':
    main()

