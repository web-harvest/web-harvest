#!/usr/bin/env python3
"""
WebHarvest Sitemap Generator
Generates sitemap.xml for all HTML pages in the documentation website.
"""

import os
from pathlib import Path
from datetime import datetime
import xml.etree.ElementTree as ET

def generate_sitemap():
    """Generate sitemap.xml for all HTML pages"""
    
    # Base URL
    base_url = "https://webharvest.sourceforge.net"
    
    # Create root element
    urlset = ET.Element("urlset")
    urlset.set("xmlns", "http://www.sitemaps.org/schemas/sitemap/0.9")
    
    # Add main pages
    main_pages = [
        ("", "1.0", "daily"),  # index.html
        ("about.html", "0.9", "monthly"),
        ("plugins.html", "0.9", "weekly"),
        ("ide.html", "0.9", "weekly"),
        ("cli.html", "0.9", "weekly"),
        ("support.html", "0.8", "monthly"),
        ("news.html", "0.8", "weekly"),
    ]
    
    for page, priority, changefreq in main_pages:
        add_url(urlset, f"{base_url}/{page}", priority, changefreq)
    
    # Add documentation pages
    doc_base = f"{base_url}/documentation"
    
    # Documentation index and main pages
    doc_pages = [
        ("", "0.9", "weekly"),  # documentation/index.html
        ("getting-started.html", "0.9", "monthly"),
        ("architecture.html", "0.8", "monthly"),
        ("debugging-guide.html", "0.8", "monthly"),
        ("pause-resume.html", "0.7", "monthly"),
        ("session-management.html", "0.7", "monthly"),
        ("rate-limiting-guide.html", "0.7", "monthly"),
        ("token-tracking.html", "0.7", "monthly"),
        ("settings-cli.html", "0.7", "monthly"),
        ("css-selectors-guide.html", "0.7", "monthly"),
        ("file-advanced-patterns.html", "0.7", "monthly"),
        ("session-history.html", "0.7", "monthly"),
        ("tutorial-form-submit-pagination.html", "0.8", "monthly"),
        ("bug-fixes-v2.2.0.html", "0.6", "monthly"),
    ]
    
    for page, priority, changefreq in doc_pages:
        add_url(urlset, f"{doc_base}/{page}", priority, changefreq)
    
    # Add processor documentation (core plugins)
    core_processors = [
        "http", "xpath", "regexp", "tokenize", "while", "function", "call", 
        "html-to-xml", "file", "def", "get-var", "set-var", "loop", "script", 
        "template", "if", "empty", "sleep", "exit", "try", "catch", "return", 
        "text", "case", "constant", "var", "json-to-xml", "xml-to-json", 
        "xquery", "include", "config", "value-of", "xml", "xq-expression", 
        "xq-param", "xslt", "stylesheet", "http-header", "http-param", 
        "call-param", "else", "regexp-pattern", "regexp-source", "regexp-result", 
        "list", "body-processor"
    ]
    
    for processor in core_processors:
        add_url(urlset, f"{doc_base}/2.2.0/{processor}.html", "0.7", "monthly")
    
    # Add external plugins
    external_plugins = [
        "ftp", "ftp-get", "ftp-list", "ftp-put", "mail", "web-browser", 
        "zip", "zip-entry"
    ]
    
    for plugin in external_plugins:
        add_url(urlset, f"{doc_base}/2.2.0/external/{plugin}.html", "0.6", "monthly")
    
    # Add examples
    examples = [
        "amazon_test.xml", "api_integration.xml", "canon.xml", "canon_with_namespace.xml",
        "crawler.xml", "data_processing_pipeline.xml", "ecommerce_monitoring.xml",
        "flickr.xml", "functions.xml", "google_images.xml", "improved_nytimes.xml",
        "modern_web_scraping.xml", "nytimes.xml", "product_catalog.xml",
        "social_media_analytics.xml", "xquery.xml", "yahoomail.xml"
    ]
    
    for example in examples:
        add_url(urlset, f"{base_url}/examples/{example}", "0.5", "monthly")
    
    # Write sitemap
    tree = ET.ElementTree(urlset)
    ET.indent(tree, space="  ", level=0)
    
    sitemap_path = Path("docs/web/sitemap.xml")
    tree.write(sitemap_path, encoding="utf-8", xml_declaration=True)
    
    print(f"✅ Generated sitemap.xml with {len(urlset)} URLs")
    print(f"📁 Location: {sitemap_path}")
    print(f"📄 Main pages: 7 (index, about, plugins, ide, cli, support, news)")

def add_url(urlset, url, priority, changefreq):
    """Add a URL entry to the sitemap"""
    url_elem = ET.SubElement(urlset, "url")
    
    loc_elem = ET.SubElement(url_elem, "loc")
    loc_elem.text = url
    
    lastmod_elem = ET.SubElement(url_elem, "lastmod")
    lastmod_elem.text = datetime.now().strftime("%Y-%m-%d")
    
    changefreq_elem = ET.SubElement(url_elem, "changefreq")
    changefreq_elem.text = changefreq
    
    priority_elem = ET.SubElement(url_elem, "priority")
    priority_elem.text = priority

if __name__ == "__main__":
    generate_sitemap()
