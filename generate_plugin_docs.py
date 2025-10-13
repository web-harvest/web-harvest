#!/usr/bin/env python3
"""
WebHarvest Plugin Documentation Generator
Generates HTML documentation pages from JavaDoc comments in external plugin classes.
"""

import re
from pathlib import Path
from typing import Dict, List

# External plugins and their processors
PLUGINS = {
    'webharvest-mail': ['MailPlugin'],
    'webharvest-ftp': ['FtpPlugin', 'FtpGetPlugin', 'FtpPutPlugin', 'FtpListPlugin'],
    'webharvest-zip': ['ZipPlugin', 'ZipEntryPlugin'],
    'webharvest-webbrowser': ['WebBrowserPlugin']
}

# Plugin name mappings (Plugin class name -> element name)
PLUGIN_NAMES = {
    'MailPlugin': 'mail',
    'FtpPlugin': 'ftp',
    'FtpGetPlugin': 'ftp-get',
    'FtpPutPlugin': 'ftp-put',
    'FtpListPlugin': 'ftp-list',
    'ZipPlugin': 'zip',
    'ZipEntryPlugin': 'zip-entry',
    'WebBrowserPlugin': 'web-browser'
}

def extract_javadoc(java_file: Path) -> str:
    """Extract JavaDoc comment from Java file."""
    content = java_file.read_text(encoding='utf-8')
    
    # Find class-level JavaDoc (/** ... */ before @CorePlugin or public class)
    match = re.search(r'/\*\*(.*?)\*/', content, re.DOTALL)
    if match:
        return match.group(1)
    return ""

def clean_html(text: str) -> str:
    """Clean HTML tags from JavaDoc text."""
    # Remove * at line starts
    text = re.sub(r'^\s*\*\s*', '', text, flags=re.MULTILINE)
    # Convert {@code ...} to <code>...</code>
    text = re.sub(r'\{@code (.*?)\}', r'<code>\1</code>', text)
    # Convert {@link ...} to text
    text = re.sub(r'\{@link (.*?)\}', r'\1', text)
    return text.strip()

def clean_code(code: str) -> str:
    """Clean code from JavaDoc formatting."""
    # Remove * at line starts
    code = re.sub(r'^\s*\*\s*', '', code, flags=re.MULTILINE)
    # Remove excessive blank lines
    code = re.sub(r'\n\s*\n\s*\n+', '\n\n', code)
    return code.strip()

def parse_javadoc(javadoc: str) -> Dict:
    """Parse JavaDoc into structured data."""
    result = {
        'title': '',
        'description': '',
        'examples': [],
        'notes': [],
        'related': [],
        'version': '2.2.0',
        'since': '2.0'
    }
    
    # Extract title (first sentence)
    title_match = re.search(r'^\s*\*\s*(.+?)(?:\s+-\s|\n|\.|<)', javadoc, re.MULTILINE)
    if title_match:
        result['title'] = title_match.group(1).strip()
    
    # Extract description (first paragraph)
    desc_match = re.search(r'<p>(.*?)</p>', javadoc, re.DOTALL)
    if desc_match:
        result['description'] = clean_html(desc_match.group(1).strip())
    
    # Extract examples (h4 Example + pre)
    example_pattern = r'<h4>(Example \d+:.*?)</h4>\s*<pre>\{@code\s*(.*?)\}</pre>'
    for match in re.finditer(example_pattern, javadoc, re.DOTALL):
        result['examples'].append({
            'title': match.group(1).strip(),
            'code': clean_code(match.group(2).strip())
        })
    
    # Extract notes
    notes_match = re.search(r'<h3>Important Notes</h3>.*?<ul>(.*?)</ul>', javadoc, re.DOTALL)
    if notes_match:
        for note in re.findall(r'<li><strong>(.*?)</strong>(.*?)</li>', notes_match.group(1), re.DOTALL):
            result['notes'].append(clean_html(f"{note[0]}: {note[1].strip()}"))
    
    # Extract related processors
    related_match = re.search(r'<h3>Related Processors</h3>.*?<ul>(.*?)</ul>', javadoc, re.DOTALL)
    if related_match:
        for rel in re.findall(r'\{@code <(.*?)>\}', related_match.group(1)):
            result['related'].append(rel)
    
    # Extract version/since
    version_match = re.search(r'@version\s+([\d.]+)', javadoc)
    if version_match:
        result['version'] = version_match.group(1)
    
    since_match = re.search(r'@since\s+([\d.]+)', javadoc)
    if since_match:
        result['since'] = since_match.group(1)
    
    return result

def generate_html(element_name: str, data: Dict, plugin_type: str) -> str:
    """Generate HTML page for plugin."""
    
    # Build examples HTML
    examples_html = ""
    for i, example in enumerate(data['examples'], 1):
        examples_html += f"""
            <h3 class="subsection-title">{example['title']}</h3>
            <div class="code-window">
                <div class="window-header">
                    <div class="window-controls">
                        <span class="control red"></span>
                        <span class="control yellow"></span>
                        <span class="control green"></span>
                    </div>
                    <span class="window-title">example-{i}.xml</span>
                </div>
                <div class="window-content">
                    <pre><code class="language-xml">{example['code']}</code></pre>
                </div>
            </div>
"""
    
    # Build notes HTML
    notes_html = ""
    if data['notes']:
        notes_html = "<h2 class=\"section-title\">Important Notes</h2><ul style=\"line-height: 1.8;\">"
        for note in data['notes']:
            notes_html += f"<li>{note}</li>"
        notes_html += "</ul>"
    
    # Build related links HTML
    related_html = ""
    if data['related']:
        related_html = "<h2 class=\"section-title\">Related Processors</h2><div class=\"related-links\">"
        for rel in data['related']:
            # Link to processors/ or same directory based on element name
            if rel in ['http', 'xpath', 'file', 'html-to-xml', 'loop', 'def']:
                link_path = f"../processors/{rel}.html"
            else:
                link_path = f"{rel}.html"
            related_html += f"""
                <a href="{link_path}" class="related-link">
                    <i class="fas fa-code"></i> &lt;{rel}&gt;
                </a>"""
        related_html += "</div>"
    
    html = f"""<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>&lt;{element_name}&gt; - {data['title']} | WebHarvest v{data['version']}</title>
    <link rel="stylesheet" href="../css/enterprise.css">
    <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.4.0/css/all.min.css">
    <style>
        .processor-header {{
            background: var(--gradient-primary);
            color: white;
            padding: var(--spacing-8) var(--spacing-4);
            margin: 0 var(--spacing-4);
            border-radius: var(--radius-2xl);
        }}
        .processor-title {{
            font-size: 2.5rem;
            font-weight: 800;
            margin-bottom: var(--spacing-2);
        }}
        .processor-tag {{
            display: inline-block;
            padding: 0.375rem 0.75rem;
            background: rgba(255, 255, 255, 0.2);
            border-radius: var(--radius-md);
            font-size: 0.875rem;
            font-weight: 600;
            margin-right: var(--spacing-2);
        }}
        .processor-content {{
            max-width: 900px;
            margin: var(--spacing-8) auto;
            padding: 0 var(--spacing-4);
        }}
        .section {{
            margin-bottom: var(--spacing-8);
        }}
        .section-title {{
            font-size: 1.75rem;
            font-weight: 700;
            margin-bottom: var(--spacing-4);
            color: var(--purple);
        }}
        .subsection-title {{
            font-size: 1.25rem;
            font-weight: 600;
            margin-top: var(--spacing-6);
            margin-bottom: var(--spacing-3);
            color: var(--magenta);
        }}
        .related-links {{
            display: grid;
            grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
            gap: var(--spacing-3);
            margin-top: var(--spacing-4);
        }}
        .related-link {{
            padding: var(--spacing-3);
            background: white;
            border: 2px solid var(--purple);
            border-radius: var(--radius-lg);
            text-align: center;
            text-decoration: none;
            color: var(--purple);
            font-weight: 600;
            transition: all 0.3s ease;
        }}
        .related-link:hover {{
            background: var(--purple);
            color: white;
            transform: translateY(-2px);
        }}
    </style>
</head>
<body>
    <header class="site-header">
        <div class="container">
            <a href="../index.html" class="brand" style="text-decoration: none; color: inherit; display: flex; align-items: center; gap: var(--spacing-3);">
                <img src="../images/logo.png" alt="WebHarvest" class="logo" />
                <span class="project-name">WebHarvest</span>
            </a>
            <nav class="main-nav">
                <a href="../processors/">Processors</a>
                <a href="../plugins.html" class="active">Plugins</a>
                <a href="../ide.html">IDE</a>
                <a href="../examples/">Examples</a>
                <a href="../documentation.html">Docs</a>
                <a href="../news.html">News</a>
                <a href="../support.html">Support</a>
                <a class="cta" href="../index.html#download">Download</a>
            </nav>
        </div>
    </header>

    <div class="processor-header">
        <div class="container">
            <h1 class="processor-title">&lt;{element_name}&gt;</h1>
            <p style="font-size: 1.125rem; margin-bottom: var(--spacing-3);">{data['title']}</p>
            <div>
                <span class="processor-tag"><i class="fas fa-plug"></i> EXTERNAL PLUGIN</span>
                <span class="processor-tag">{plugin_type}</span>
                <span class="processor-tag">v{data['version']}</span>
            </div>
        </div>
    </div>

    <div class="processor-content">
        <section class="section">
            <h2 class="section-title">Overview</h2>
            <p>{data['description']}</p>
        </section>

        <section class="section">
            <h2 class="section-title">Usage Examples</h2>
            {examples_html}
        </section>

        <section class="section">
            {notes_html}
        </section>

        <section class="section">
            {related_html}
        </section>
    </div>

    <footer class="site-footer">
        <div class="container">
            <div class="footer-content">
                <div class="footer-section">
                    <h4>WebHarvest v{data['version']}</h4>
                    <p>Open-source web scraping framework</p>
                    <p class="footer-author">
                        Created by Vladimir Nikić<br>
                        Maintained by <a href="https://www.linkedin.com/in/rbala/" target="_blank" class="author-link">Robert Bala</a>
                    </p>
                </div>
                <div class="footer-section">
                    <h4>Resources</h4>
                    <ul>
                        <li><a href="../documentation.html">Documentation</a></li>
                        <li><a href="../examples/">Examples</a></li>
                        <li><a href="../processors/">Processors</a></li>
                        <li><a href="../plugins.html">Plugins</a></li>
                    </ul>
                </div>
                <div class="footer-section">
                    <h4>Community</h4>
                    <ul>
                        <li><a href="https://sourceforge.net/projects/web-harvest/" target="_blank">SourceForge</a></li>
                        <li><a href="https://sourceforge.net/p/web-harvest/discussion/" target="_blank">Forum</a></li>
                        <li><a href="https://sourceforge.net/p/web-harvest/bugs/" target="_blank">Bug Tracker</a></li>
                        <li><a href="../support.html">Support</a></li>
                    </ul>
                </div>
            </div>
            <div class="footer-bottom">
                <p>&copy; 2006-2025 WebHarvest Team. Licensed under BSD License.</p>
            </div>
        </div>
    </footer>
</body>
</html>"""
    
    return html

def main():
    """Main generation function."""
    print("🚀 WebHarvest Plugin Documentation Generator")
    print("=" * 60)
    
    # Paths
    base_dir = Path(__file__).parent
    output_dir = base_dir / "docs" / "web" / "plugins"
    
    output_dir.mkdir(parents=True, exist_ok=True)
    
    generated_count = 0
    
    # Process all plugins
    for plugin_module, plugin_classes in PLUGINS.items():
        print(f"\n📁 {plugin_module} ({len(plugin_classes)} processors)")
        
        for plugin_class in plugin_classes:
            # Find Java file
            java_file = base_dir / plugin_module / "src" / "main" / "java" / "org" / "webharvest" / "plugin" / plugin_module.replace('webharvest-', '') / f"{plugin_class}.java"
            
            if not java_file.exists():
                print(f"  ⚠️  {plugin_class}: File not found at {java_file}")
                continue
            
            element_name = PLUGIN_NAMES.get(plugin_class, plugin_class.replace('Plugin', '').lower())
            
            # Extract and parse JavaDoc
            javadoc = extract_javadoc(java_file)
            data = parse_javadoc(javadoc)
            
            # Generate HTML
            html = generate_html(element_name, data, plugin_module.replace('webharvest-', '').upper())
            
            # Write file
            output_file = output_dir / f"{element_name}.html"
            output_file.write_text(html, encoding='utf-8')
            
            print(f"  ✅ {element_name}.html ({len(data['examples'])} examples)")
            generated_count += 1
    
    print("\n" + "=" * 60)
    print(f"✅ Generated {generated_count} plugin HTML pages successfully!")
    print(f"📂 Output directory: {output_dir}")

if __name__ == "__main__":
    main()





