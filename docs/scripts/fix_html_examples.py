#!/usr/bin/env python3
"""
Fix HTML documentation examples by adding <config> wrapper.

Fixes tutorial pages where XML examples are missing proper config root element.
"""

import os
import re

def fix_html_example(content):
    """
    Fix XML examples in HTML by adding config wrapper.
    """
    # Pattern: <pre><code class="language-xml">...XML...</code></pre>
    pattern = r'(<pre><code class="language-xml">)(.*?)(</code></pre>)'
    
    def replace_example(match):
        pre_tag = match.group(1)
        xml_content = match.group(2)
        post_tag = match.group(3)
        
        # Check if already has config
        if '&lt;config' in xml_content or '<?xml version' in xml_content:
            return match.group(0)
        
        # Check if this is a WebHarvest configuration (has WebHarvest tags)
        if not any(tag in xml_content for tag in ['&lt;def', '&lt;http', '&lt;xpath', '&lt;xquery', '&lt;loop', '&lt;file']):
            return match.group(0)
        
        # Add config wrapper
        wrapped = f'{pre_tag}&lt;?xml version="1.0" encoding="UTF-8"?&gt;\n'
        wrapped += '&lt;config xmlns="http://org.webharvest/schema/2.1/core"&gt;\n'
        wrapped += f'{xml_content.strip()}\n'
        wrapped += '&lt;/config&gt;'
        wrapped += post_tag
        
        return wrapped
    
    return re.sub(pattern, replace_example, content, flags=re.DOTALL)


def fix_html_file(file_path):
    """
    Fix all XML examples in an HTML file.
    """
    print(f"Processing: {file_path}")
    
    with open(file_path, 'r', encoding='utf-8') as f:
        content = f.read()
    
    original_content = content
    content = fix_html_example(content)
    
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
    Fix tutorial pages.
    """
    tutorial_pages = [
        'docs/web/documentation/css-selectors-guide.html',
        'docs/web/documentation/file-advanced-patterns.html',
        'docs/web/documentation/rate-limiting-guide.html',
        'docs/web/documentation/tutorial-form-submit-pagination.html'
    ]
    
    fixed_count = 0
    
    for page in tutorial_pages:
        if os.path.exists(page):
            if fix_html_file(page):
                fixed_count += 1
        else:
            print(f"Skipping {page} (not found)")
    
    print(f"\n✓ Fixed {fixed_count} tutorial pages")
    print(f"\nVerify: http://localhost:8000/documentation/")


if __name__ == '__main__':
    main()

