# Architecture Refactoring Proposal - Web-Harvest v3.0

**Date**: October 9, 2025  
**Status**: PROPOSAL (Not Implemented)  
**Target Version**: 3.0.0  
**Effort**: 2-3 weeks  
**Risk**: Medium-High

---

## Current Architecture (Hybrid - v2.2.x)

```
┌─────────────┐
│  config.xml │
└──────┬──────┘
       │
       ↓
┌──────────────────────────────────────┐
│  SAXConfigParser (SAX parser)        │
│  - Reads XML elements                │
│  - Creates XmlNode tree              │
└──────┬───────────────────────────────┘
       │
       ↓
┌──────────────────────────────────────┐
│  XmlNode (raw XML representation)    │
│  - Element name: "http"              │
│  - Attributes: {url: "...", ...}     │
│  - Children: List<XmlNode>           │
└──────┬───────────────────────────────┘
       │
       ↓
┌──────────────────────────────────────┐
│  DefinitionResolver                  │
│  - Maps element name → *Def class    │
│  - createElementDefinition()         │
└──────┬───────────────────────────────┘
       │
       ↓
┌──────────────────────────────────────┐
│  *Def Classes (30 classes)           │
│  - HttpDef, FileDef, XPathDef, ...   │
│  - Store XML data + processor class  │
│  - Extends WebHarvestPluginDef       │
└──────┬───────────────────────────────┘
       │
       ↓
┌──────────────────────────────────────┐
│  ProcessorFactory                    │
│  - def.createPlugin()                │
└──────┬───────────────────────────────┘
       │
       ↓
┌──────────────────────────────────────┐
│  Processor/Plugin                    │
│  - HttpPlugin, XPathPlugin, ...      │
│  - Actual execution logic            │
└──────────────────────────────────────┘
```

**Problems:**
- 🔴 Too many layers (6 steps!)
- 🔴 30+ *Def classes to maintain
- 🔴 Confusing for new developers
- 🔴 IElementDef has deprecated methods

---

## Proposed Architecture (Simplified - v3.0)

### Option A: Direct XML → Plugin

```
┌─────────────┐
│  config.xml │
└──────┬──────┘
       │
       ↓
┌──────────────────────────────────────┐
│  StreamingXmlParser (NEW!)           │
│  - SAX-based streaming parser        │
│  - Recognizes plugin elements        │
│  - Directly creates PluginConfig     │
└──────┬───────────────────────────────┘
       │
       ↓
┌──────────────────────────────────────┐
│  PluginConfiguration (exists)        │
│  - Element: "http"                   │
│  - Properties: {url: "...", ...}     │
│  - Children: List<PluginConfig>      │
└──────┬───────────────────────────────┘
       │
       ↓
┌──────────────────────────────────────┐
│  PluginRegistry (exists)             │
│  - lookup("http") → HttpPlugin.class │
└──────┬───────────────────────────────┘
       │
       ↓
┌──────────────────────────────────────┐
│  Plugin (exists)                     │
│  - new HttpPlugin()                  │
│  - plugin.initialize(config)         │
│  - plugin.execute(context)           │
└──────────────────────────────────────┘
```

**Benefits:**
- ✅ Only 3 layers (was 6)
- ✅ No *Def classes needed
- ✅ Cleaner, easier to understand
- ✅ Better performance
- ✅ Easier to add new plugins

**Code Example:**

```java
// NEW StreamingXmlParser.java
public class StreamingXmlParser implements ConfigParser {
    
    private PluginRegistry pluginRegistry;
    
    public PluginConfigurationTree parse(ConfigSource source) {
        SAXParser parser = SAXParserFactory.newInstance().newSAXParser();
        ConfigurationBuilder builder = new ConfigurationBuilder();
        
        parser.parse(source.getInputStream(), new DefaultHandler() {
            
            @Override
            public void startElement(String uri, String localName, ...) {
                // Look up plugin in registry
                PluginInfo info = pluginRegistry.lookup(localName, uri);
                
                if (info != null) {
                    // Create configuration directly!
                    Map<String, String> props = extractAttributes(attributes);
                    PluginConfiguration config = new DefaultPluginConfiguration(
                        localName, uri, localName, props
                    );
                    
                    builder.pushConfiguration(config);
                } else {
                    throw new ConfigurationException("Unknown element: " + localName);
                }
            }
            
            @Override
            public void endElement(String uri, String localName, ...) {
                builder.popConfiguration();
            }
        });
        
        return builder.build();
    }
}
```

---

### Option B: Generic ElementDefinition (DTO Pattern)

Keep SAXParser, but simplify *Def layer:

```java
// Single class instead of 30!
public class ElementDefinition implements IElementDef {
    private String elementName;
    private String namespace;
    private Map<String, String> attributes;
    private List<ElementDefinition> children;
    private Class<? extends Plugin> pluginClass;
    
    public ElementDefinition(XmlNode node, Class<? extends Plugin> pluginClass) {
        this.elementName = node.getName();
        this.namespace = node.getUri();
        this.attributes = node.getAttributes();
        this.children = convertChildren(node);
        this.pluginClass = pluginClass;
    }
    
    @Override
    public Plugin createPlugin() {
        Plugin plugin = pluginClass.newInstance();
        PluginConfiguration config = new DefaultPluginConfiguration(
            elementName, namespace, elementName, attributes
        );
        plugin.initialize(config);
        return plugin;
    }
}
```

**Benefits:**
- ✅ Eliminates 30 *Def classes
- ✅ Keeps SAXConfigParser (less risk)
- ✅ Still have definition layer (for caching, lazy loading)

**Challenges:**
- Must handle special cases (FunctionDef, ScriptDef have unique logic)
- Migration path for all 30 *Def classes

---

## Migration Strategy (Recommended)

### Phase 1: v2.2.x (Current - DONE)
- ✅ Document hybrid architecture
- ✅ Mark deprecated code with comments
- ✅ All plugins migrated to @CorePlugin

### Phase 2: v2.3.0 (Q1 2026)
- Add deprecation warnings for old @Definition usage
- Create experimental StreamingXmlParser (behind flag)
- A/B testing framework

### Phase 3: v2.4.0 (Q2 2026)
- Make new parser available via config option
- Parallel testing (both parsers available)
- Collect user feedback

### Phase 4: v3.0.0 (Q3 2026)
- New parser becomes default
- Old parser available as fallback (--use-legacy-parser)
- Update all documentation

### Phase 5: v3.5.0 (Q4 2026)
- Remove old parser entirely
- Remove all *Def classes
- Clean architecture achieved

---

## Alternative: Incremental Cleanup (Low Risk)

Instead of full refactoring, we can **clean up current architecture**:

### 1. Consolidate *Def Classes

Many *Def classes have identical code:

```java
// Before: 30 similar classes
public class HttpDef extends WebHarvestPluginDef { ... }
public class FileDef extends WebHarvestPluginDef { ... }
public class XPathDef extends WebHarvestPluginDef { ... }

// After: Use WebHarvestPluginDef directly with metadata
public class WebHarvestPluginDef {
    public WebHarvestPluginDef(XmlNode node, PluginMetadata metadata) {
        // Generic implementation
    }
}
```

**Effort**: 1 week  
**Risk**: Low  
**Benefit**: Remove ~20 *Def files

### 2. Remove Deprecated Methods

```java
// IElementDef.java
@Deprecated IElementDef[] getOperationDefs();  // Still used: 34 times
@Deprecated XmlNode getNode();                 // Used: 12 times
@Deprecated String getNamespaceURI();          // Used: 8 times
```

**Action**: 
- Find all usages
- Migrate to new methods (getElementDefs())
- Remove deprecated methods

**Effort**: 3-4 days  
**Risk**: Medium  
**Benefit**: Cleaner interface

### 3. Document Pattern

Add comprehensive documentation:

```java
/**
 * ARCHITECTURE NOTE: Hybrid Design Pattern
 * 
 * Web-Harvest uses a two-layer architecture:
 * 
 * Layer 1 (Parsing): XML → XmlNode → *Def classes
 *   Purpose: Parse XML configuration into object tree
 *   Components: SAXConfigParser, DefinitionResolver, *Def classes
 *   
 * Layer 2 (Execution): *Def → Plugin → Result
 *   Purpose: Execute scraping logic
 *   Components: @CorePlugin classes, AbstractPlugin
 *   
 * Why both layers?
 *   - *Def layer provides lazy loading and caching
 *   - Plugin layer provides modern, testable execution
 *   - Bridge: CorePluginWebHarvestPluginDef
 *   
 * Future: v3.0 will merge both layers with new direct parser
 * 
 * @see OLD_ARCHITECTURE_ANALYSIS.md for details
 */
```

**Effort**: 1 day  
**Risk**: Zero  
**Benefit**: Clear understanding for developers

---

## Recommendation

### For v2.2.1 (Now): 
**Option 4** - Keep current architecture, focus on:
- Coverage improvements ✅ (in progress)
- Bug fixes
- Documentation

### For v2.3-2.4 (Next 6 months):
**Incremental cleanup**:
- Remove unused *Def classes
- Migrate deprecated methods
- Add architecture documentation

### For v3.0 (2026):
**Option 1 or 3** - New parser:
- Design and implement StreamingXmlParser
- Gradual migration path
- Remove old architecture

---

## Proof of Concept - Simplified Parser

If you want to see how new parser would look:

```java
public class SimplifiedXmlParser implements ConfigParser {
    
    private final PluginRegistry pluginRegistry;
    
    @Override
    public ConfigurationTree parse(ConfigSource source) throws Exception {
        SAXParserFactory factory = SAXParserFactory.newInstance();
        factory.setNamespaceAware(true);
        
        SAXParser parser = factory.newSAXParser();
        ConfigurationTreeBuilder builder = new ConfigurationTreeBuilder(pluginRegistry);
        
        parser.parse(source.getInputStream(), new DefaultHandler() {
            
            @Override
            public void startElement(String uri, String localName, 
                                   String qName, Attributes attributes) {
                // Direct lookup in plugin registry
                PluginInfo pluginInfo = pluginRegistry.getPluginInfo(localName, uri);
                
                if (pluginInfo == null) {
                    throw new ConfigurationException("Unknown element: " + localName);
                }
                
                // Create configuration directly (NO *Def!)
                Map<String, String> props = new HashMap<>();
                for (int i = 0; i < attributes.getLength(); i++) {
                    props.put(attributes.getLocalName(i), attributes.getValue(i));
                }
                
                PluginConfiguration config = new DefaultPluginConfiguration(
                    UUID.randomUUID().toString(),
                    uri,
                    localName,
                    props
                );
                
                builder.pushElement(localName, config, pluginInfo);
            }
            
            @Override
            public void endElement(String uri, String localName, String qName) {
                builder.popElement();
            }
            
            @Override
            public void characters(char[] ch, int start, int length) {
                String text = new String(ch, start, length).trim();
                if (!text.isEmpty()) {
                    builder.addTextContent(text);
                }
            }
        });
        
        return builder.build();
    }
}
```

**This would eliminate:**
- ❌ All 30 *Def classes
- ❌ XmlNode (replaced by direct PluginConfiguration)
- ❌ WebHarvestPluginDef
- ❌ AbstractElementDef
- ❌ Deprecated IElementDef methods

**Effort**: ~800 lines of new code + migration

---

## Decision Matrix

| Option | Effort | Risk | Benefit | Timeline | Recommend? |
|--------|--------|------|---------|----------|------------|
| **1. New Parser** | 3 weeks | High | High | v3.0 | For v3.0 |
| **2. Generic *Def** | 2 weeks | Medium | Medium | v2.4 | Maybe |
| **3. Gradual Migration** | 6+ months | Low | High | v2.3-3.5 | ✅ YES |
| **4. Keep Current** | 0 | Zero | Zero | Now | ✅ For v2.2.x |

---

## My Recommendation

### For NOW (v2.2.1):
**Keep architecture as-is**, because:
1. It works perfectly (2747 tests pass)
2. No bugs related to architecture
3. Focus on coverage and bugfixes gives better ROI
4. HttpComponents 4.x migration just completed (r720)

### For LATER (v3.0):
**Plan new parser** with:
1. POC implementation (experimental branch)
2. Performance benchmarks
3. Migration guide for users
4. Gradual rollout

---

## Immediate Actions (If You Want Cleanup)

### Quick Wins - Low Risk Improvements:

**1. Add Architecture Diagram** (30 minutes)
```
docs/ARCHITECTURE.md with visual diagrams
```

**2. Deprecation Dates** (1 hour)
```java
@Deprecated(since = "2.1.0", forRemoval = true, removalVersion = "3.0.0")
public IElementDef[] getOperationDefs() { ... }
```

**3. Inline Documentation** (2 hours)
Add comments explaining hybrid pattern in key classes

**4. Developer Guide Update** (1 hour)
Section explaining the two-layer architecture

---

## Your Choice

What would you prefer?

**A) Focus on practical improvements (coverage, bugfixes)**
- Continue with coverage improvements
- Fix remaining bugs from GitHub/SourceForge
- Improve documentation
- **Effort**: Ongoing
- **Risk**: Low
- **Value**: High

**B) Plan architecture refactoring for v3.0**
- Create detailed refactoring plan
- Design new parser (POC)
- Estimate full migration effort
- **Effort**: 1 week planning
- **Risk**: None (just planning)
- **Value**: Future preparation

**C) Quick cleanup of current architecture**
- Remove unused deprecated methods
- Add deprecation warnings
- Improve documentation
- **Effort**: 1 week
- **Risk**: Low-Medium
- **Value**: Medium

**My recommendation: Option A**
- Biggest immediate value
- Lowest risk
- Architecture is working fine

---

**Let me know what you prefer and I'll proceed!**


