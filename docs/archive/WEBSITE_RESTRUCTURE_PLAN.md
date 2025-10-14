# Website Structure Reorganization Plan

## Overview
Comprehensive restructure of WebHarvest website to provide better content organization, dedicated landing pages for key components, and improved documentation structure.

---

## 🎯 Main Changes

### **1. Navigation & Home Page Updates**

#### Remove:
- ❌ **API Docs** - JavaDoc not needed on main site (users can generate or download separately)

#### Keep/Enhance:
- ✅ **Examples** - Already exists, keep current implementation
- ✅ **Processors** - Enhance with better core/plugin distinction
- ✅ **Download** - Keep prominent in navbar

#### Add New:
- 🆕 **IDE** - Link to dedicated IDE landing page
- 🆕 **Plugins** - Link to custom plugins guide
- 🆕 **Docs** - Versioned documentation structure
- 🆕 **News** - Project updates and announcements
- 🆕 **Support** - Links to SourceForge support system

---

## 📄 New Landing Pages

### **1. IDE Landing Page** (`docs/web/ide.html`)
**Purpose:** Comprehensive guide to WebHarvest IDE

**Content:**
- **Hero Section:** "Professional Web IDE for WebHarvest"
- **Installation:**
  - System requirements
  - Download instructions
  - Quick start guide
- **Features:** (with screenshots/examples)
  - Monaco Editor integration
  - Real-time execution feedback
  - Multi-tab workspace (Apple HIG model)
  - Per-tab logs, results, variables
  - Session management & metrics
  - WebSocket live updates
- **Usage Guide:**
  - Creating/editing configurations
  - Running scrapers
  - Debugging tips
  - Keyboard shortcuts
- **Architecture:**
  - Session API integration
  - Token tracking
  - Event system
- **CTA:** Download IDE, View Examples

---

### **2. Core Processors Page** (`docs/web/processors.html`)
**Purpose:** Emphasize role of core framework and built-in processors

**Content:**
- **Hero:** "42+ Built-in Processors for Web Data Extraction"
- **Core Framework:**
  - What is WebHarvest Core?
  - Why processors are fundamental
  - Architecture overview
- **Processor Categories:**
  - **HTTP & Web:** `<http>`, `<http-header>`, `<http-param>`
  - **Parsing:** `<html-to-xml>`, `<xq-query>`, `<xq-expression>`, `<xpath>`
  - **Control Flow:** `<loop>`, `<while>`, `<if>`, `<case>`, `<switch>`
  - **Variables:** `<var>`, `<var-def>`, `<list>`, `<list-add>`
  - **Files:** `<file>`, `<regexp>`, `<template>`
  - **Functions:** `<function>`, `<call>`, `<return>`
  - **Script:** `<script>`, `<groovy>`, `<javascript>`
  - **Output:** `<echo>`, `<file>`
- **Each Processor:**
  - Name, icon, description
  - Link to detailed docs
  - Quick example
- **CTA:** View Full Docs, Try in IDE

---

### **3. Custom Plugins Page** (`docs/web/plugins.html`)
**Purpose:** Guide for creating and using external plugins

**Content:**
- **Hero:** "Extend WebHarvest with Custom Plugins"
- **Plugin System Overview:**
  - What are plugins?
  - Core vs. External plugins
  - Why build plugins?
- **How It Works:**
  - **Auto-Discovery:** Classpath scanning, no manual registration
  - **XSD Generation:** Automatic schema generation for IDE support
  - **Dependency Injection:** Guice integration
  - **Annotation-Based:** `@PluginDef`, `@PluginAttr`
- **Building Plugins:**
  - Maven setup & dependencies
  - Creating processor class
  - Defining attributes
  - Packaging & distribution
- **Available External Plugins:**
  - **Database:** `webharvest-database` - SQL operations, JDBC support
  - **Mail:** `webharvest-mail` - Send/receive emails, SMTP/IMAP
  - **Zip:** `webharvest-zip` - Archive creation/extraction
  - **FTP:** `webharvest-ftp` - FTP/FTPS operations
  - **Web Browser:** `webharvest-webbrowser` - Browser automation
- **Each Plugin:**
  - Name, description, Maven coordinates
  - Example usage
  - GitHub/download link
- **CTA:** Build Your Plugin, View Examples

---

### **4. Documentation Hub** (`docs/web/documentation.html`)
**Purpose:** Organized access to versioned documentation

**Content:**
- **Hero:** "Comprehensive WebHarvest Documentation"
- **Current Version (v2.2):**
  - Getting Started Guide
  - Processor Reference
  - Plugin Development Guide
  - API Documentation (link to javadoc download/generation)
  - Session Management API
  - Token Tracking Guide
  - Event System Guide
  - Migration Guides (HttpClient 4→5, etc.)
- **Previous Versions:**
  - v2.1 docs
  - v2.0 docs
  - Legacy docs
- **Additional Resources:**
  - Examples repository
  - Video tutorials (if any)
  - Community guides
- **CTA:** View Latest Docs, Download Offline Docs

---

### **5. News Page** (`docs/web/news.html`)
**Purpose:** Project updates, release notes, announcements

**Content:**
- **Hero:** "WebHarvest News & Updates"
- **Latest News:**
  - v2.2 Release (October 2025)
    - Session Management API
    - IDE redesign (Apple HIG)
    - HttpClient 5 migration
    - 2,569 tests passing
  - v2.1 Release
  - Community milestones
- **Upcoming Features:**
  - Roadmap teaser
  - Feature requests in progress
- **Blog-style Layout:**
  - Chronological posts
  - Categories (releases, tutorials, community)
  - RSS feed (optional)
- **CTA:** Subscribe to Updates, View Changelog

---

### **6. Support Page** (`docs/web/support.html`)
**Purpose:** Central hub for getting help

**Content:**
- **Hero:** "Get Help with WebHarvest"
- **Community Support:**
  - SourceForge main support: https://sourceforge.net/projects/web-harvest/support
  - Bug tracker: https://sourceforge.net/p/web-harvest/bugs/
  - Feature requests: https://sourceforge.net/p/web-harvest/feature-requests/
- **Documentation:**
  - Link to docs hub
  - FAQ section
  - Common issues & solutions
- **Professional Support:**
  - Contact information (if applicable)
  - Consulting services (if applicable)
- **Contributing:**
  - How to report bugs
  - How to request features
  - How to contribute code/docs
- **CTA:** Report Bug, Request Feature, View Docs

---

## 🗂️ Updated Site Structure

```
docs/web/
├── index.html                    (Home - updated nav, remove API Docs link)
├── ide.html                      (NEW - IDE landing page)
├── processors.html               (UPDATED - emphasize core role)
├── plugins.html                  (NEW - custom plugins guide)
├── documentation.html            (NEW - versioned docs hub)
├── news.html                     (NEW - project updates)
├── support.html                  (NEW - support hub)
├── examples/                     (KEEP - already exists)
│   ├── index.html
│   └── *.xml
├── api/                          (KEEP - but not linked in main nav)
│   └── javadoc/
├── css/
│   ├── enterprise.css            (Main design system)
│   └── ...
└── js/
    └── ...
```

---

## 🎨 Design Consistency

All new pages will use:
- **enterprise.css** design system
- Consistent navbar (updated with new links)
- Same footer structure
- Magenta/yellow/purple/lime color palette
- Professional card-based layouts
- Responsive design
- Apple HIG-inspired spacing & typography

---

## 👤 Author Attribution

**Footer on all pages:**
```html
<footer class="site-footer">
  <div class="container footer-inner">
    <div class="footer-left">
      <p>© 2025 WebHarvest — Open-source web scraping framework</p>
    </div>
    <div class="footer-right">
      <a href="https://www.linkedin.com/in/rbala/" target="_blank" rel="noopener" class="author-link">
        <i class="fab fa-linkedin"></i> Robert Bala
      </a>
    </div>
  </div>
</footer>
```
**Style:** Subtle, professional, not overly prominent

---

## 🔗 Updated Main Navigation

```html
<nav class="main-nav">
  <a href="index.html">Home</a>
  <a href="processors.html">Core</a>
  <a href="plugins.html">Plugins</a>
  <a href="ide.html">IDE</a>
  <a href="examples/">Examples</a>
  <a href="documentation.html">Docs</a>
  <a href="news.html">News</a>
  <a href="support.html">Support</a>
  <a class="cta" href="#download">Download</a>
</nav>
```

---

## 📋 Implementation Order

1. **Phase 1:** Update home page navigation (remove API Docs, add new links)
2. **Phase 2:** Create IDE landing page (most important)
3. **Phase 3:** Update processors page (emphasize core)
4. **Phase 4:** Create plugins page
5. **Phase 5:** Create documentation hub
6. **Phase 6:** Create news page
7. **Phase 7:** Create support page
8. **Phase 8:** Update footer with LinkedIn link
9. **Phase 9:** Test all pages, links, responsiveness
10. **Phase 10:** Deploy & announce

---

## ✅ Success Criteria

- [ ] All pages use enterprise design system
- [ ] Navigation is consistent across all pages
- [ ] Clear distinction between Core (built-in) and Plugins (external)
- [ ] IDE has comprehensive landing page with installation guide
- [ ] Documentation is organized by version
- [ ] Support resources are easy to find
- [ ] News page showcases v2.2 achievements
- [ ] LinkedIn link is present but subtle
- [ ] All pages are responsive & accessible
- [ ] No broken links

---

## 📝 Notes

- Keep existing examples/ directory unchanged (it's already good)
- API javadoc stays, just not linked prominently in main nav
- Version selector for docs can be simple dropdown or tab interface
- News page can be static for now (manual updates)
- Consider RSS feed for news (optional, future enhancement)
- Mobile menu for navigation (hamburger on small screens)
- Consider adding search functionality (future enhancement)

---

**Status:** Ready for implementation
**Target:** WebHarvest v2.2 website launch
**Assigned:** AI Assistant
**Review:** Robert Bala





