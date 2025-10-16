#!/usr/bin/env python3
"""
WebHarvest Plugins Index Generator
Generates the main plugins/index.html page with all 54 plugins organized by category.
"""

from pathlib import Path

# Import categories from core plugin generator
from generate_core_plugin_docs import CATEGORIES, PLUGIN_NAMES

# External plugins
EXTERNAL_PLUGINS = {
    'mail': {
        'title': 'Mail',
        'description': 'Send emails with attachments via SMTP',
        'icon': 'envelope',
        'path': 'external/mail.html'
    },
    'ftp': {
        'title': 'FTP',
        'description': 'FTP connection and operations',
        'icon': 'server',
        'path': 'external/ftp.html'
    },
    'ftp-get': {
        'title': 'FTP Get',
        'description': 'Download files from FTP server',
        'icon': 'download',
        'path': 'external/ftp-get.html'
    },
    'ftp-put': {
        'title': 'FTP Put',
        'description': 'Upload files to FTP server',
        'icon': 'upload',
        'path': 'external/ftp-put.html'
    },
    'ftp-list': {
        'title': 'FTP List',
        'description': 'List files on FTP server',
        'icon': 'list',
        'path': 'external/ftp-list.html'
    },
    'zip': {
        'title': 'ZIP',
        'description': 'Create and extract ZIP archives',
        'icon': 'file-archive',
        'path': 'external/zip.html'
    },
    'zip-entry': {
        'title': 'ZIP Entry',
        'description': 'Access individual ZIP archive entries',
        'icon': 'file-zipper',
        'path': 'external/zip-entry.html'
    },
    'web-browser': {
        'title': 'Web Browser',
        'description': 'Automated browser interactions',
        'icon': 'window-maximize',
        'path': 'external/web-browser.html'
    }
}

# Plugin descriptions (enhanced)
PLUGIN_DESCRIPTIONS = {
    'http': ('HTTP', 'HTTP requests with full client features', 'globe'),
    'xpath': ('XPath', 'Query XML/HTML with XPath expressions', 'code-branch'),
    'regexp': ('Regex', 'Pattern matching with regular expressions', 'search'),
    'tokenize': ('Tokenize', 'Split text into tokens', 'scissors'),
    'while': ('While', 'While loop iteration', 'rotate'),
    'function': ('Function', 'Define reusable functions', 'function'),
    'call': ('Call', 'Call defined functions', 'phone'),
    'html-to-xml': ('HTML-to-XML', 'Parse HTML to clean XML', 'code'),
    'file': ('File', 'Read, write and list files', 'file'),
    'def': ('Def', 'Define variables', 'box'),
    'get-var': ('Get Var', 'Retrieve variable values', 'hand-pointer'),
    'set-var': ('Set Var', 'Set variable values', 'edit'),
    'loop': ('Loop', 'Iterate over collections', 'repeat'),
    'script': ('Script', 'Execute JavaScript/Groovy/BeanShell', 'terminal'),
    'template': ('Template', 'Generate dynamic content', 'file-code'),
    'json-to-xml': ('JSON-to-XML', 'Convert JSON to XML', 'file-import'),
    'xml-to-json': ('XML-to-JSON', 'Convert XML to JSON', 'file-export'),
    'if': ('If', 'Conditional execution', 'question-circle'),
    'xquery': ('XQuery', 'Advanced XQuery 3.1 queries', 'database'),
    'include': ('Include', 'Include external configurations', 'file-import'),
    'empty': ('Empty', 'Test for empty values', 'circle'),
    'sleep': ('Sleep', 'Pause execution', 'clock'),
    'exit': ('Exit', 'Exit configuration', 'door-open'),
    'try': ('Try', 'Exception handling', 'shield-alt'),
    'catch': ('Catch', 'Catch exceptions', 'hand-paper'),
    'return': ('Return', 'Return from function', 'undo'),
    'text': ('Text', 'Text manipulation', 'font'),
    'case': ('Case', 'Switch-case statement', 'code-branch'),
    'constant': ('Constant', 'Define constants', 'lock'),
    'var': ('Var', 'Variable placeholder', 'dollar-sign'),
    'config': ('Config', 'Configuration element', 'cog'),
    'value-of': ('Value Of', 'Extract node values', 'hand-point-right'),
    'xml': ('XML', 'XML processing', 'file-code'),
    'xq-expression': ('XQ Expression', 'XQuery expression body', 'code'),
    'xq-param': ('XQ Param', 'XQuery parameters', 'sliders'),
    'xslt': ('XSLT', 'XSLT transformations', 'magic'),
    'stylesheet': ('Stylesheet', 'XSLT stylesheet', 'palette'),
    'http-header': ('HTTP Header', 'HTTP request headers', 'heading'),
    'http-param': ('HTTP Param', 'HTTP parameters', 'sliders'),
    'call-param': ('Call Param', 'Function call parameters', 'list'),
    'else': ('Else', 'Else clause for if', 'code-branch'),
    'regexp-pattern': ('Regexp Pattern', 'Regex pattern definition', 'asterisk'),
    'regexp-source': ('Regexp Source', 'Regex source text', 'file-alt'),
    'regexp-result': ('Regexp Result', 'Regex match results', 'check'),
    'list': ('List', 'Create lists of items', 'list-ul'),
    'body-processor': ('Body Processor', 'Process body content', 'file-lines'),
}

# Category icons and descriptions
CATEGORY_INFO = {
    'CRITICAL': ('Essential plugins for basic scraping', 'star'),
    'IMPORTANT': ('Key plugins for advanced workflows', 'flag'),
    'COMMON': ('Commonly used utility plugins', 'check-circle'),
    'ADVANCED': ('Advanced and specialized plugins', 'cogs'),
}

def generate_plugin_card(element_name: str, path: str = None) -> str:
    """Generate HTML for a single plugin card."""
    if path is None:
        path = f"{element_name}.html"
    
    title, desc, icon = PLUGIN_DESCRIPTIONS.get(element_name, (element_name.title(), 'Plugin', 'plug'))
    
    return f"""        <a href="{path}" class="processor-card">
          <div class="processor-icon">
            <i class="fas fa-{icon}"></i>
          </div>
          <h4>{title}</h4>
          <p>{desc}</p>
        </a>"""

def generate_category_section(category_name: str, plugins: list) -> str:
    """Generate HTML for a category section."""
    desc, icon = CATEGORY_INFO[category_name]
    
    # Generate plugin cards
    cards = []
    for plugin_class in plugins:
        element_name = PLUGIN_NAMES.get(plugin_class, plugin_class.replace('Plugin', '').lower())
        cards.append(generate_plugin_card(element_name))
    
    cards_html = '\n'.join(cards)
    
    # Format category name properly (e.g., "CRITICAL" -> "Critical Plugins")
    formatted_name = f"{category_name.capitalize()} Plugins"
    
    return f"""    <div class="category">
      <h2>
        <span class="category-icon">
          <i class="fas fa-{icon}"></i>
        </span>
        {formatted_name} <span style="font-size: 0.8em; font-weight: 400; color: var(--text-secondary);">({len(plugins)} plugins)</span>
      </h2>
      <p style="color: var(--text-secondary); margin-bottom: 2rem;">{desc}</p>
      <div class="processors-grid">
{cards_html}
      </div>
    </div>
"""

def generate_external_section() -> str:
    """Generate HTML for external plugins section."""
    cards = []
    for element_name, info in EXTERNAL_PLUGINS.items():
        card = f"""        <a href="{info['path']}" class="processor-card external-plugin">
          <div class="processor-icon">
            <i class="fas fa-{info['icon']}"></i>
          </div>
          <h4>{info['title']}</h4>
          <p>{info['description']}</p>
          <span class="external-badge">EXTERNAL</span>
        </a>"""
        cards.append(card)
    
    cards_html = '\n'.join(cards)
    
    return f"""    <div class="category">
      <h2>
        <span class="category-icon">
          <i class="fas fa-plug"></i>
        </span>
        External Plugins <span style="font-size: 0.8em; font-weight: 400; color: var(--text-secondary);">({len(EXTERNAL_PLUGINS)} plugins)</span>
      </h2>
      <p style="color: var(--text-secondary); margin-bottom: 2rem;">External plugin modules (require separate dependencies)</p>
      <div class="processors-grid">
{cards_html}
      </div>
    </div>
"""

def generate_index_html() -> str:
    """Generate complete index.html content."""
    
    # Count total plugins
    total_core = sum(len(plugins) for plugins in CATEGORIES.values())
    total_external = len(EXTERNAL_PLUGINS)
    total = total_core + total_external
    
    # Generate category sections
    sections = []
    for category_name, plugins in CATEGORIES.items():
        sections.append(generate_category_section(category_name, plugins))
    
    # Add external plugins section
    sections.append(generate_external_section())
    
    sections_html = '\n\n'.join(sections)
    
    html = f"""<!doctype html>
<html lang="en">

<head>
  <meta charset="utf-8" />
  <meta name="viewport" content="width=device-width, initial-scale=1" />
  <title>All Plugins - WebHarvest v2.2.0</title>
  <link rel="stylesheet" href="../../css/enterprise.css" />
  <link href="https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700;800&display=swap" rel="stylesheet">
  <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.4.0/css/all.min.css">
  <style>
    .external-plugin {{
      position: relative;
      border-color: var(--magenta);
    }}
    .external-badge {{
      position: absolute;
      top: 0.5rem;
      right: 0.5rem;
      background: var(--magenta);
      color: white;
      padding: 0.25rem 0.5rem;
      border-radius: 4px;
      font-size: 0.7rem;
      font-weight: 600;
    }}
  </style>
</head>

<body>

  <!-- Header -->
  <header class="site-header">
    <div class="container header-inner">
      <a href="../../index.html" class="brand"
        style="text-decoration: none; color: inherit; display: flex; align-items: center; gap: var(--spacing-3);">
        <img src="../../images/logo.png" alt="WebHarvest" class="logo" />
        <span class="project-name">WebHarvest</span>
      </a>
      <nav class="main-nav">
        <a href="../../about.html">About</a>
        <a href="../getting-started.html" style="color: var(--lime); font-weight: 600;"><i class="fas fa-rocket"></i> Get Started</a>
        <a href="../../plugins.html">Plugins</a>
        <a href="../../ide.html">IDE</a>
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
        <h1>WebHarvest<br />Plugins</h1>
        <p class="lead">Complete plugin reference</p>
        <p class="hero-description">
          {total} plugins for web scraping: {total_core} built-in core plugins + {total_external} external modules
          for mail, FTP, ZIP, and browser automation.
        </p>
      </div>
    </div>
  </section>

  <!-- Quick Reference -->
  <section class="container" style="margin-top: 4rem;">
    <div class="feature-card" style="max-width: 900px; margin: 0 auto; background: var(--gray-50);">
      <h3 style="margin-bottom: 1rem;"><i class="fas fa-lightbulb"></i> Plugin Architecture</h3>
      <p style="color: var(--text-secondary); margin-bottom: 1.5rem;">
        WebHarvest 2.2 uses a modern plugin architecture with automatic discovery and dependency injection:
      </p>
      <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 2rem; margin-bottom: 1.5rem;">
        <div>
          <h4 style="font-size: 1rem; margin-bottom: 0.5rem;">
            <i class="fas fa-star"></i> Core Plugins ({total_core})
          </h4>
          <p style="color: var(--text-secondary); font-size: 0.9rem;">
            Built-in plugins with <code>@CorePlugin</code> annotation, included in webharvest-core. No external dependencies required.
          </p>
        </div>
        <div>
          <h4 style="font-size: 1rem; margin-bottom: 0.5rem;">
            <i class="fas fa-plug"></i> External Plugins ({total_external})
          </h4>
          <p style="color: var(--text-secondary); font-size: 0.9rem;">
            Separate plugin modules (mail, ftp, zip, webbrowser) requiring additional dependencies. Add as Maven/Gradle dependencies.
          </p>
        </div>
      </div>
      <div style="margin-top: 1.5rem;">
        <a href="../../architecture.html" class="btn btn-primary">
          <i class="fas fa-sitemap"></i> View Architecture
        </a>
        <a href="../" class="btn btn-outline" style="margin-left: 1rem;">
          <i class="fas fa-book"></i> Documentation
        </a>
      </div>
    </div>
  </section>

  <!-- All Plugins by Category -->
  <section class="plugins-all container" style="margin-top: 4rem; margin-bottom: 4rem;">

{sections_html}

  </section>

  <!-- Footer -->
  <footer class="site-footer">
    <div class="container footer-content">
      <div class="footer-links">
        <a href="../../about.html">About</a>
        <a href="./">Plugins</a>
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

</body>

</html>"""
    
    return html

def main():
    """Main generation function."""
    print("=" * 60)
    print("🚀 WebHarvest Plugins Index Generator")
    print("=" * 60)
    
    # Count plugins
    total_core = sum(len(plugins) for plugins in CATEGORIES.values())
    total_external = len(EXTERNAL_PLUGINS)
    total = total_core + total_external
    
    print(f"\n📊 Plugin Summary:")
    for category, plugins in CATEGORIES.items():
        print(f"   • {category}: {len(plugins)} plugins")
    print(f"   • EXTERNAL: {total_external} plugins")
    print(f"   • TOTAL: {total} plugins")
    
    # Generate HTML
    html = generate_index_html()
    
    # Write file
    base_dir = Path(__file__).parent
    output_file = base_dir / "docs" / "web" / "documentation" / "2.2.0" / "index.html"
    output_file.parent.mkdir(parents=True, exist_ok=True)
    output_file.write_text(html, encoding='utf-8')
    
    print(f"\n✅ Generated index.html successfully!")
    print(f"📂 Output: {output_file}")
    print(f"\n🌐 View at: http://localhost:8000/documentation/2.2.0/")
    print("=" * 60)

if __name__ == "__main__":
    main()

