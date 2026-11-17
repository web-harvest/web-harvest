> **Historical reference — not current operational guidance.** This document describes the WebHarvest 2.2.0 documentation/design state. Verify current source, tests, plugin inventory and UI before relying on any claim.

# WebHarvest Plugin Documentation Generation

**English Documentation for Plugin Documentation Generators**

## 📋 Overview

WebHarvest 2.2 includes automated documentation generators that create HTML documentation pages for all 54 plugins by extracting JavaDoc comments from source code.

### Plugin Architecture

- **47 Core Plugins** - Built-in plugins with `@CorePlugin` annotation (in `webharvest-core`)
- **8 External Plugins** - Separate modules: mail, ftp, zip, webbrowser (separate dependencies)
- **Total: 55 Plugins**

## 🚀 Quick Start

### Generate All Documentation with SEO (Recommended)

```bash
cd docs/scripts
python3 regenerate_all_with_seo.py
```

Or from project root:
```bash
python3 docs/scripts/regenerate_all_with_seo.py
```

This master script runs all generators with SEO optimizations:
1. Core plugin pages (47 files)
2. External plugin pages (8 files)
3. Plugins index page

**Output:**
- `docs/web/documentation/2.2.0/index.html` - Main index with all 55 plugins
- `docs/web/documentation/2.2.0/*.html` - 47 core plugin pages
- `docs/web/documentation/2.2.0/external/*.html` - 8 external plugin pages

### View Documentation

```bash
cd docs/web
python3 -m http.server 8000
# Open: http://localhost:8000/documentation/2.2.0/
```

## 📂 Generator Scripts

**Location:** All scripts are in `docs/scripts/` directory

### Maintenance Scripts

#### `fix_javadoc_examples.py` (r830)

Fixes Javadoc examples by adding `<config>` wrapper to all plugin source code examples.

**Purpose:** Ensure all examples in plugin Javadoc are complete, valid XML configurations

**Usage:**
```bash
python3 docs/scripts/fix_javadoc_examples.py
```

**What it does:**
- Scans all `*Plugin.java` files (core + external)
- Finds `<pre>{@code ...}` blocks with WebHarvest XML
- Wraps with `<?xml version...>` and `<config xmlns...>` tags
- Preserves Javadoc formatting

**Fixed:** 46 core + 8 external plugin files

#### `fix_html_examples.py` (r831)

Fixes HTML tutorial pages by adding `<config>` wrapper to XML examples.

**Purpose:** Ensure tutorial examples can be copy-pasted directly to IDE

**Usage:**
```bash
python3 docs/scripts/fix_html_examples.py
```

**What it does:**
- Scans tutorial HTML pages
- Finds `<code class="language-xml">` blocks
- Wraps with config tags (HTML encoded)
- Preserves HTML formatting

**Fixed:** 4 tutorial pages (css-selectors, file-advanced-patterns, rate-limiting, tutorial-form-submit)

### Documentation Generators

### 1. `generate_core_plugin_docs.py`

Generates HTML documentation for **46 core plugins**.

**Input:**
- Java source files: `webharvest-core/src/main/java/org/webharvest/plugin/core/*Plugin.java`
- Extracts JavaDoc comments from class-level documentation

**Output:**
- `docs/web/documentation/2.2.0/*.html` (46 files)

**Categories:**
- **CRITICAL** (9 plugins): http, xpath, regexp, tokenize, while, function, call, html-to-xml, file
- **IMPORTANT** (11 plugins): def, get-var, set-var, loop, script, template, json-to-xml, xml-to-json, if, xquery, include
- **COMMON** (10 plugins): empty, sleep, exit, try, catch, return, text, case, constant, var
- **ADVANCED** (16 plugins): config, value-of, xml, xq-expression, xq-param, xslt, stylesheet, http-header, http-param, call-param, else, regexp-pattern, regexp-source, regexp-result, list, body-processor

**Usage:**
```bash
cd docs/scripts
python3 generate_core_plugin_docs.py
```

### 2. `generate_external_plugin_docs.py`

Generates HTML documentation for **8 external plugins**.

**Input:**
- Java source files in:
  - `webharvest-mail/src/main/java/`
  - `webharvest-ftp/src/main/java/`
  - `webharvest-zip/src/main/java/`
  - `webharvest-webbrowser/src/main/java/`

**Output:**
- `docs/web/documentation/2.2.0/external/*.html` (8 files)

**Plugins:**
- mail, ftp, ftp-get, ftp-put, ftp-list, zip, zip-entry, web-browser

**Usage:**
```bash
cd docs/scripts
python3 generate_external_plugin_docs.py
```

### 3. `generate_plugins_index.py`

Generates the main plugins index page with all 54 plugins organized by category.

**Input:**
- Reads plugin categories from `generate_core_plugin_docs.py`
- External plugin definitions (hardcoded)

**Output:**
- `docs/web/documentation/2.2.0/index.html`

**Features:**
- Displays all 54 plugins in organized categories
- Visual distinction between core and external plugins
- Links to individual plugin documentation pages
- Summary statistics

**Usage:**
```bash
cd docs/scripts
python3 generate_plugins_index.py
```

### 4. `generate_all_plugin_docs.py` (Master Script)

Runs all three generators in sequence.

**Usage:**
```bash
python3 generate_all_plugin_docs.py
```

**Output:**
```
============================================================
🚀 WebHarvest Complete Plugin Documentation Generator
============================================================

📌 This will generate documentation for:
   • 46 CORE plugins (built-in, @CorePlugin)
   • 8 EXTERNAL plugins (mail, ftp, zip, web-browser)
   • TOTAL: 54 plugin pages

[... runs all generators ...]

✅ ALL PLUGIN DOCUMENTATION GENERATED SUCCESSFULLY!
============================================================

📂 Output locations:
   • Index page: docs/web/plugins/index.html
   • Core plugins: docs/web/plugins/*.html (46 files)
   • External plugins: docs/web/plugins/2.2/*.html (8 files)

🌐 View documentation:
   cd docs/web && python3 -m http.server 8000
   Then open: http://localhost:8000/plugins/

📋 Complete plugin reference:
   • CRITICAL: 9 plugins
   • IMPORTANT: 11 plugins
   • COMMON: 10 plugins
   • ADVANCED: 16 plugins
   • EXTERNAL: 8 plugins
```

## 🔧 How It Works

### JavaDoc Parsing

The generators extract documentation from JavaDoc comments in plugin source files:

```java
/**
 * HTTP Plugin - Executes HTTP requests
 * 
 * <p>
 * Performs HTTP GET/POST/PUT/DELETE requests with full support for headers,
 * parameters, cookies, authentication, and more.
 * </p>
 * 
 * <h4>Example 1: Simple GET Request</h4>
 * <pre>{@code
 * <http url="https://example.com" method="GET"/>
 * }</pre>
 * 
 * <h3>Important Notes</h3>
 * <ul>
 *   <li><strong>Timeout</strong>: Default is 30 seconds</li>
 *   <li><strong>Redirects</strong>: Follows by default</li>
 * </ul>
 * 
 * @version 2.2.0
 * @since 1.0
 */
@CorePlugin(elementName = "http")
public class HttpPlugin extends AbstractPlugin {
    // ...
}
```

### Generated HTML Structure

Each plugin page includes:
- **Header**: Plugin name, description, category badge
- **Overview**: Main description from JavaDoc
- **Usage Examples**: Extracted from `<h4>Example` sections
- **Important Notes**: From `<h3>Important Notes</h3>` section
- **Related Processors**: Links to related plugins
- **Footer**: Standard site footer

### Styling

All pages use:
- `docs/web/css/enterprise.css` - Main stylesheet
- Font Awesome icons
- Monaco-style code windows for examples
- Responsive grid layout

## 📝 Adding New Plugins

### Core Plugin

1. Create Java file in `webharvest-core/src/main/java/org/webharvest/plugin/core/`
2. Add `@CorePlugin` annotation
3. Add comprehensive JavaDoc with examples
4. Add plugin class name to appropriate category in `generate_core_plugin_docs.py`:

```python
CATEGORIES = {
    'CRITICAL': [..., 'YourNewPlugin'],
    ...
}

PLUGIN_NAMES = {
    'YourNewPlugin': 'your-plugin',
    ...
}
```

5. Run: `python3 generate_all_plugin_docs.py`

### External Plugin

1. Add plugin information to `generate_plugins_index.py`:

```python
EXTERNAL_PLUGINS = {
    'your-plugin': {
        'title': 'Your Plugin',
        'description': 'Plugin description',
        'icon': 'icon-name',
        'path': '2.2/your-plugin.html'
    },
    ...
}
```

2. Add to `generate_external_plugin_docs.py`:

```python
PLUGINS = {
    'webharvest-yourplugin': ['YourPlugin'],
    ...
}

PLUGIN_NAMES = {
    'YourPlugin': 'your-plugin',
    ...
}
```

3. Run: `python3 generate_all_plugin_docs.py`

## 🎨 Customization

### Plugin Descriptions

Edit descriptions in `generate_plugins_index.py`:

```python
PLUGIN_DESCRIPTIONS = {
    'your-plugin': ('Title', 'Description', 'font-awesome-icon'),
    ...
}
```

### Categories

Modify categories in `generate_core_plugin_docs.py`:

```python
CATEGORY_INFO = {
    'YOUR_CATEGORY': ('Description', 'icon', '#color'),
    ...
}
```

## 🐛 Troubleshooting

### "Plugin not found" error

**Problem**: Generator can't find Java source file

**Solution**: Verify file path and plugin class name match

### Missing examples in generated docs

**Problem**: JavaDoc doesn't follow expected format

**Solution**: Use this JavaDoc structure:

```java
/**
 * <h4>Example 1: Title</h4>
 * <pre>{@code
 * <your-plugin attr="value"/>
 * }</pre>
 */
```

### Index page missing plugins

**Problem**: Plugin not in CATEGORIES or EXTERNAL_PLUGINS

**Solution**: Add plugin to appropriate dictionary

## 📊 Statistics

- **Total Plugins**: 54
- **Core Plugins**: 46 (9 CRITICAL, 11 IMPORTANT, 10 COMMON, 16 ADVANCED)
- **External Plugins**: 8
- **Documentation Pages**: 55 (54 plugins + 1 index)
- **Generation Time**: ~2 seconds

## 🔗 Related Files

- `PLUGINS.md` - Plugin usage guide (Polish/English)
- `CORE_PLUGINS_INVENTORY.md` - Complete plugin inventory
- `docs/web/plugins/index.html` - Generated index page
- `docs/web/css/enterprise.css` - Stylesheet

## 🔍 SEO Optimization

### Complete SEO Regeneration (`regenerate_all_with_seo.py`)

**Purpose**: Regenerates all documentation with SEO optimizations
**Usage**: `python3 docs/scripts/regenerate_all_with_seo.py`

**SEO Features Applied**:
- **Meta Tags**: Descriptions, keywords, robots directives
- **Open Graph**: Facebook/social media optimization
- **Twitter Cards**: Twitter sharing optimization  
- **Structured Data**: JSON-LD schema markup
- **Canonical URLs**: Duplicate content prevention
- **Alt Texts**: Descriptive image alt attributes
- **Sitemap**: XML sitemap with 91 URLs
- **Robots.txt**: Search engine crawling instructions

### Generate Sitemap (`generate_sitemap.py`)

**Purpose**: Creates sitemap.xml for search engine indexing
**Usage**: `python3 docs/scripts/generate_sitemap.py`

**What it does**:
- Generates XML sitemap with 91 URLs
- Includes all main pages, documentation, and processors
- Sets proper priorities and change frequencies
- Optimized for search engine crawling

## 📄 License

All documentation generators are part of WebHarvest and licensed under BSD License.

---

**Last Updated**: January 15, 2025  
**WebHarvest Version**: 2.2.0

