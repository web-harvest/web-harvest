#!/usr/bin/env python3
"""
WebHarvest Core Plugin Documentation Generator
Generates HTML documentation pages from JavaDoc comments in core plugin classes.

Core Plugins: 46 built-in plugins with @CorePlugin annotation
"""

import re
import os
from pathlib import Path
from typing import Dict, List, Tuple
import html

# Plugin categories and their plugins
CATEGORIES = {
    'CRITICAL': ['HttpPlugin', 'XPathPlugin', 'RegexpPlugin', 'TokenizePlugin', 
                 'WhilePlugin', 'FunctionPlugin', 'CallPlugin', 'HtmlToXmlPlugin', 'FilePlugin'],
    'IMPORTANT': ['DefPlugin', 'GetVarPlugin', 'SetVarPlugin', 'LoopPlugin', 
                  'ScriptPlugin', 'TemplatePlugin', 'JsonToXmlPlugin', 'XmlToJsonPlugin', 'IfPlugin', 'XQueryPlugin', 'IncludePlugin'],
    'COMMON': ['EmptyPlugin', 'SleepPlugin', 'ExitPlugin', 'TryPlugin', 'CatchPlugin',
               'ReturnPlugin', 'TextPlugin', 'CasePlugin', 'ConstantPlugin', 'VarPlugin', 'LogPlugin'],
    'ADVANCED': ['ValueOfPlugin', 'XMLPlugin', 'XQExpressionPlugin', 
                 'XQParamPlugin', 'XsltPlugin', 'StylesheetPlugin', 'HttpHeaderPlugin',
                 'HttpParamPlugin', 'CallParamPlugin', 'ElsePlugin', 'RegexpPatternPlugin',
                 'RegexpSourcePlugin', 'RegexpResultPlugin', 'ListPlugin', 'BodyProcessorPlugin']
}

# Plugin name mappings (Plugin class name -> element name)
PLUGIN_NAMES = {
    'HttpPlugin': 'http',
    'XPathPlugin': 'xpath',
    'RegexpPlugin': 'regexp',
    'HtmlToXmlPlugin': 'html-to-xml',
    'JsonToXmlPlugin': 'json-to-xml',
    'XmlToJsonPlugin': 'xml-to-json',
    'XQueryPlugin': 'xquery',
    'TokenizePlugin': 'tokenize',
    'WhilePlugin': 'while',
    'FunctionPlugin': 'function',
    'CallPlugin': 'call',
    'DefPlugin': 'def',
    'GetVarPlugin': 'get-var',
    'SetVarPlugin': 'set-var',
    'LoopPlugin': 'loop',
    'ScriptPlugin': 'script',
    'TemplatePlugin': 'template',
    'IfPlugin': 'if',
    'EmptyPlugin': 'empty',
    'SleepPlugin': 'sleep',
    'ExitPlugin': 'exit',
    'TryPlugin': 'try',
    'CatchPlugin': 'catch',
    'ReturnPlugin': 'return',
    'TextPlugin': 'text',
    'CasePlugin': 'case',
    'ConstantPlugin': 'constant',
    'VarPlugin': 'var',
    'LogPlugin': 'log',
    'ValueOfPlugin': 'value-of',
    'XMLPlugin': 'xml',
    'XQExpressionPlugin': 'xq-expression',
    'XQParamPlugin': 'xq-param',
    'XsltPlugin': 'xslt',
    'StylesheetPlugin': 'stylesheet',
    'HttpHeaderPlugin': 'http-header',
    'HttpParamPlugin': 'http-param',
    'CallParamPlugin': 'call-param',
    'ElsePlugin': 'else',
    'FilePlugin': 'file',
    'IncludePlugin': 'include',
    'RegexpPatternPlugin': 'regexp-pattern',
    'RegexpSourcePlugin': 'regexp-source',
    'RegexpResultPlugin': 'regexp-result',
    'ListPlugin': 'list',
    'BodyProcessorPlugin': 'body-processor'
}

def extract_javadoc(java_file: Path) -> str:
    """Extract JavaDoc comment from Java file."""
    content = java_file.read_text(encoding='utf-8')
    
    # Find class-level JavaDoc (/** ... */ before @CorePlugin or public class)
    match = re.search(r'/\*\*(.*?)\*/', content, re.DOTALL)
    if match:
        return match.group(1)
    return ""

def parse_javadoc(javadoc: str) -> Dict:
    """Parse JavaDoc into structured data."""
    result = {
        'title': '',
        'description': '',
        'full_javadoc': javadoc,  # Store full JavaDoc for conversion
        'examples': [],
        'parameters': [],
        'notes': [],
        'related': [],
        'version': '2.2.0',
        'since': '1.0'
    }
    
    # Extract title (first sentence or h3)
    title_match = re.search(r'^\s*\*\s*(.+?)(?:\s+-\s|\n|\.|<)', javadoc, re.MULTILINE)
    if title_match:
        result['title'] = title_match.group(1).strip()
    
    # Extract description (first paragraph before <h3>)
    desc_match = re.search(r'<p>(.*?)</p>', javadoc, re.DOTALL)
    if desc_match:
        result['description'] = clean_html(desc_match.group(1).strip())
    
    # Try both example patterns:
    # Pattern 1: Individual h4 headers with examples (new format: "Example 1: Description")
    # Allow for JavaDoc comment markers (* ) between tags
    example_pattern = r'<h4>(Example \d+:.*?)</h4>.*?<pre>\{@code\s*(.*?)\}</pre>'
    for match in re.finditer(example_pattern, javadoc, re.DOTALL):
        result['examples'].append({
            'title': match.group(1).strip(),
            'code': clean_code(match.group(2).strip())
        })
    
    # Pattern 1b: Alternative h3 headers with examples
    if not result['examples']:
        example_pattern_alt = r'<h3>(Example \d+:.*?)</h3>.*?<pre>\{@code\s*(.*?)\}</pre>'
        for match in re.finditer(example_pattern_alt, javadoc, re.DOTALL):
            result['examples'].append({
                'title': match.group(1).strip(),
                'code': clean_code(match.group(2).strip())
            })
    
    # Pattern 2: Single large Usage Examples block (if no h4 examples found)
    if not result['examples']:
        usage_match = re.search(r'<h3>Usage Examples</h3>.*?<pre>\{@code\s*(.*?)\}</pre>', javadoc, re.DOTALL)
        if usage_match:
            result['examples'].append({
                'title': 'Examples',  # Changed from 'Usage Examples' to avoid duplication
                'code': clean_code(usage_match.group(1).strip())
            })
    
    # Pattern 3: Single Usage block (alternative pattern)
    if not result['examples']:
        usage_match = re.search(r'<h3>Usage</h3>.*?<pre>\{@code\s*(.*?)\}</pre>', javadoc, re.DOTALL)
        if usage_match:
            result['examples'].append({
                'title': 'Usage Examples',
                'code': clean_code(usage_match.group(1).strip())
            })
    
    # Extract parameters (ul/li in Parameters section)
    params_match = re.search(r'<h3>Parameters</h3>.*?<ul>(.*?)</ul>', javadoc, re.DOTALL)
    if params_match:
        for param in re.findall(r'<li><strong>(.*?)</strong>(.*?)</li>', params_match.group(1), re.DOTALL):
            result['parameters'].append({
                'name': param[0].strip(),
                'description': clean_html(param[1].strip())
            })
    
    # Also check for Sub-elements section and add to parameters
    subelems_match = re.search(r'<h3>Sub-elements</h3>.*?<ul>(.*?)</ul>', javadoc, re.DOTALL)
    if subelems_match:
        for subelem in re.findall(r'<li>\{@code <(.*?)>\}(.*?)</li>', subelems_match.group(1), re.DOTALL):
            result['parameters'].append({
                'name': f'<{subelem[0]}>',
                'description': clean_html(subelem[1].strip().lstrip(' -'))
            })
    
    # Extract notes (ul/li in Important Notes section)
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
    # Remove * at line starts (including lines that are just "*")
    code = re.sub(r'^\s*\*\s?', '', code, flags=re.MULTILINE)
    # Remove excessive blank lines
    code = re.sub(r'\n\s*\n\s*\n+', '\n\n', code)
    # Clean up any remaining standalone asterisks
    code = re.sub(r'^\s*\*\s*$', '', code, flags=re.MULTILINE)
    return code.strip()

def generate_html(processor_name: str, element_name: str, data: Dict, category: str) -> str:
    """Generate HTML page for processor."""
    
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
                    <pre><code class="language-xml">{html.escape(example['code'])}</code></pre>
                </div>
            </div>
"""
    
    # Build parameters HTML
    params_html = ""
    if data['parameters']:
        params_html = "<h2 class=\"section-title\">Parameters</h2><ul style=\"line-height: 1.8;\">"
        for param in data['parameters']:
            params_html += f"<li><strong>{param['name']}</strong>: {param['description']}</li>"
        params_html += "</ul>"
    
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
            related_html += f"""
                <a href="{rel}.html" class="related-link">
                    <i class="fas fa-code"></i> &lt;{rel}&gt;
                </a>"""
        related_html += "</div>"
    
    # Generate SEO-optimized meta tags
    seo_description = f"WebHarvest {element_name} plugin: {data['title']}. Core processor for web scraping and data extraction with XML configuration. Examples, usage, and documentation."
    seo_keywords = f"webharvest {element_name}, {element_name} plugin, web scraping, data extraction, XML configuration, core processor, webharvest plugin documentation"
    
    html_content = f"""<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>&lt;{element_name}&gt; - {data['title']} | WebHarvest v{data['version']}</title>
    <meta name="description" content="{seo_description}" />
    <meta name="keywords" content="{seo_keywords}" />
    <meta name="robots" content="index, follow" />
    <meta name="author" content="WebHarvest Team" />
    
    <!-- Open Graph / Facebook -->
    <meta property="og:type" content="article" />
    <meta property="og:title" content="&lt;{element_name}&gt; - {data['title']}" />
    <meta property="og:description" content="{data['description'][:160]}..." />
    <meta property="og:url" content="https://webharvest.sourceforge.net/documentation/2.2.0/{element_name}.html" />
    
    <!-- Twitter -->
    <meta property="twitter:card" content="summary" />
    <meta property="twitter:title" content="&lt;{element_name}&gt; - {data['title']}" />
    <meta property="twitter:description" content="{data['description'][:160]}..." />
    
    <!-- Structured Data -->
    <script type="application/ld+json">
    {{
      "@context": "https://schema.org",
      "@type": "TechArticle",
      "headline": "&lt;{element_name}&gt; - {data['title']}",
      "description": "{data['description']}",
      "author": {{
        "@type": "Organization",
        "name": "WebHarvest Team"
      }},
      "datePublished": "2025-01-01",
      "dateModified": "2025-01-01",
      "publisher": {{
        "@type": "Organization",
        "name": "WebHarvest",
        "logo": {{
          "@type": "ImageObject",
          "url": "https://webharvest.sourceforge.net/images/logo.png"
        }}
      }},
      "mainEntityOfPage": {{
        "@type": "WebPage",
        "@id": "https://webharvest.sourceforge.net/documentation/2.2.0/{element_name}.html"
      }},
      "about": {{
        "@type": "SoftwareApplication",
        "name": "WebHarvest",
        "applicationCategory": "DeveloperApplication"
      }}
    }}
    </script>
    <link rel="stylesheet" href="../../css/enterprise.css">
    <link rel="stylesheet" href="../../css/prism.css">
    <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.4.0/css/all.min.css">
    <style>
        /* Override enterprise.css background for code blocks */
        .window-content pre[class*="language-"] {{
            background: #272822 !important;
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
        }}
        .subsection-title {{
            font-size: 1.25rem;
            font-weight: 600;
            margin-top: var(--spacing-6);
            margin-bottom: var(--spacing-3);
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
            border: 2px solid var(--gray-300);
            border-radius: var(--radius-lg);
            text-align: center;
            text-decoration: none;
            color: var(--text-primary);
            font-weight: 600;
            transition: all 0.3s ease;
        }}
        .related-link:hover {{
            background: var(--gray-50);
            border-color: var(--gray-400);
            transform: translateY(-2px);
        }}
    </style>
</head>
<body>
    <header class="site-header">
        <div class="container header-inner">
            <a href="../../index.html" class="brand" style="text-decoration: none; color: inherit; display: flex; align-items: center; gap: var(--spacing-3);">
                <img src="../../images/logo.png" alt="WebHarvest Logo - Enterprise Web Scraping Framework" class="logo" />
                <span class="project-name">WebHarvest</span>
            </a>
            <nav class="main-nav">
                <a href="../../about.html">About</a>
                <a href="../getting-started.html" style="background: linear-gradient(135deg, var(--lime), #8BC34A); color: white; padding: 0.5rem 1rem; border-radius: var(--radius-md); font-weight: 700;"><i class="fas fa-rocket"></i> Get Started</a>
                <a href="../../plugins.html">Plugins</a>
                <a href="../../ide.html">IDE</a>
                <a href="../../cli.html">CLI</a>
                <a href="../../examples/">Examples</a>
                <a href="../">Docs</a>
                <a href="../../news.html">News</a>
                <a href="../../support.html">Support</a>
                <a class="cta" href="../../index.html#download">Download</a>
            </nav>
        </div>
    </header>

    <!-- Hero -->
    <section class="hero">
        <div class="container hero-inner">
            <div class="hero-content">
                <h1>&lt;{element_name}&gt;</h1>
                <p class="lead">{data['title']}</p>
                <p class="hero-description">
                    <span style="display: inline-block; margin-right: 1rem;"><i class="fas fa-cube"></i> Core</span>
                    <span style="display: inline-block;">v{data['version']}</span>
                </p>
            </div>
        </div>
    </section>

    <div class="processor-content">
        <section class="section">
            <h2 class="section-title">Overview</h2>
            <p>{data['description']}</p>
        </section>

        {f'''<section class="section">
            <h2 class="section-title">Usage Examples</h2>
            {examples_html}
        </section>''' if examples_html.strip() else ''}

        {f'''<section class="section">
            {params_html}
        </section>''' if params_html.strip() else ''}

        {f'''<section class="section">
            {notes_html}
        </section>''' if notes_html.strip() else ''}

        {f'''<section class="section">
            {related_html}
        </section>''' if related_html.strip() else ''}
    </div>

    <footer class="site-footer">
        <div class="container footer-content">
            <div class="footer-links">
                <a href="../../about.html">About</a>
                <a href="../">Plugins</a>
                <a href="../">Documentation</a>
                <a href="../../examples/">Examples</a>
                <a href="../../support.html">Support</a>
                <a href="https://sourceforge.net/projects/web-harvest/">SourceForge</a>
            </div>
            <div class="footer-legal">
                <p>Copyright © 2006-2025 WebHarvest Contributors. All rights reserved.</p>
            </div>
        </div>
    </footer>
    <script src="../../js/prism.js"></script>
</body>
</html>"""
    
    return html_content

def main():
    """Main generation function."""
    print("🚀 WebHarvest Core Plugin Documentation Generator")
    print("=" * 60)
    print("📌 Generating docs for CORE PLUGINS (built-in, @CorePlugin)")
    print("📌 For external plugins, use: generate_plugin_docs.py")
    
    # Paths
    base_dir = Path(__file__).parent.parent.parent  # Project root (2 levels up from docs/scripts/)
    plugins_dir = base_dir / "webharvest-core" / "src" / "main" / "java" / "org" / "webharvest" / "plugin" / "core"
    output_dir = base_dir / "docs" / "web" / "documentation" / "2.2.0"
    
    if not plugins_dir.exists():
        print(f"❌ Plugins directory not found: {plugins_dir}")
        return
    
    output_dir.mkdir(parents=True, exist_ok=True)
    
    generated_count = 0
    
    # Process all categories
    for category, plugins in CATEGORIES.items():
        print(f"\n📁 {category} ({len(plugins)} core plugins)")
        
        for plugin_class in plugins:
            java_file = plugins_dir / f"{plugin_class}.java"
            
            if not java_file.exists():
                print(f"  ⚠️  {plugin_class}: File not found")
                continue
            
            element_name = PLUGIN_NAMES.get(plugin_class, plugin_class.replace('Plugin', '').lower())
            
            # Extract and parse JavaDoc
            javadoc = extract_javadoc(java_file)
            data = parse_javadoc(javadoc)
            
            # Generate HTML
            html = generate_html(plugin_class, element_name, data, category)
            
            # Write file
            output_file = output_dir / f"{element_name}.html"
            output_file.write_text(html, encoding='utf-8')
            
            print(f"  ✅ {element_name}.html ({len(data['examples'])} examples)")
            generated_count += 1
    
    print("\n" + "=" * 60)
    print(f"✅ Generated {generated_count} core plugin HTML pages successfully!")
    print(f"📂 Output directory: {output_dir}")
    print(f"\n💡 Total: {generated_count}/47 core plugins documented")

if __name__ == "__main__":
    main()

