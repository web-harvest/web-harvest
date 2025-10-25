# XML Conversion - Test Plan

**Goal:** Prove that XML ↔ Visual conversion is lossless and correct

---

## 🧪 Test Cases

### Test 1: Simple Linear (simple_test.xml)
```xml
<config>
  <def var="content">
    <html-to-xml>
      <http url="https://httpbin.org/html"/>
    </html-to-xml>
  </def>
  <def var="title">
    <xpath expression="//h1">
      <get var="content"/>
    </xpath>
  </def>
</config>
```

**Expected Visual Structure:**
```
def (var="content")
└─ html-to-xml
   └─ http (url="...")

def (var="title")
└─ xpath (expression="//h1")
   └─ get (var="content")
```

**Expected Node Count:** 6 nodes  
**Expected Root Nodes:** 2 (both def)  
**Expected Max Depth:** 3 levels

---

### Test 2: Loop with Body (functions.xml)
```xml
<function name="download-multipage-list">
  <return>
    <while condition="${pageUrl.length() != 0}" maxloops="${maxloops}">
      <def var="content">
        <html-to-xml>
          <http url="${pageUrl}"/>
        </html-to-xml>
      </def>
    </while>
  </return>
</function>
```

**Expected Visual:**
```
function (name="download-multipage-list")
└─ return
   └─ while (condition="...", maxloops="...")
      └─ def (var="content")
         └─ html-to-xml
            └─ http (url="...")
```

**Expected Max Depth:** 6 levels

---

### Test 3: Template with CDATA (data_processing_pipeline.xml)
```xml
<template>
  <![CDATA[
  <?xml version="1.0"?>
  <data>
    <value>${variable}</value>
  </data>
  ]]>
</template>
```

**Expected:**
- Node type: template
- Body: Full CDATA content (preserved exactly)
- No children

---

### Test 4: XQuery with Special Elements
```xml
<xquery>
  <xq-param name="employees">
    <get var="xmlData"/>
  </xq-param>
  <xq-expression>
    <![CDATA[
    declare variable $employees external;
    count($employees//employee)
    ]]>
  </xq-expression>
</xquery>
```

**Expected:**
```
xquery
├─ xq-param (name="employees")
│  └─ get (var="xmlData")
└─ xq-expression (body=CDATA content)
```

---

### Test 5: HTTP with Headers
```xml
<http url="https://api.example.com" method="POST">
  <http-header name="Authorization" value="Bearer token"/>
  <http-header name="Content-Type" value="application/json"/>
  <http-param name="query" value="search"/>
</http>
```

**Expected:**
```
http (url="...", method="POST")
├─ http-header (name="Authorization", value="...")
├─ http-header (name="Content-Type", value="...")
└─ http-param (name="query", value="...")
```

**Validation:**
- http-header ONLY allowed in http
- http-param ONLY allowed in http

---

## ✅ Acceptance Criteria

### For Each Test Case:
1. ✅ **Parse XML → Visual**
   - Correct number of nodes
   - Correct parent-child relationships
   - All attributes extracted
   - Body content preserved

2. ✅ **Generate Visual → XML**
   - Valid XML structure
   - All attributes present
   - Correct nesting
   - Body content with CDATA if needed

3. ✅ **Roundtrip (XML → Visual → XML)**
   - Output XML matches input XML (semantically)
   - Order preserved
   - Attributes identical
   - Whitespace may differ (OK)

4. ✅ **Validation**
   - Required attributes checked
   - Allowed children validated
   - Required children verified
   - Error messages clear

---

## 🚀 Implementation Steps

### Step 1: XMLParser.ts ✅ DONE
- processElement() recursive parsing
- extractAttributes() with type conversion
- extractBody() with CDATA support
- Error collection

### Step 2: XMLGenerator.ts ✅ DONE
- buildElement() recursive generation
- Attribute sorting
- CDATA wrapping for body
- formatXML() indentation

### Step 3: Test Suite (NEXT)
```typescript
// Test file: XMLConversion.test.ts
describe('XML Conversion', () => {
  test('Parse simple_test.xml', () => {
    const xml = loadExample('simple_test.xml');
    const result = parser.parse(xml);
    
    expect(result.errors).toHaveLength(0);
    expect(result.nodes).toHaveLength(6);
    // ... more assertions
  });
  
  test('Roundtrip conversion', () => {
    const original = loadExample('simple_test.xml');
    const parsed = parser.parse(original);
    const generated = generator.generate(parsed.nodes);
    const reparsed = parser.parse(generated);
    
    expect(reparsed.nodes).toEqual(parsed.nodes);
  });
});
```

### Step 4: Integration (NEXT)
- Add XML view mode (Monaco Editor)
- Implement view switching
- Bidirectional sync
- Validation UI

---

## 🎯 Current Status

✅ **Specification complete**  
✅ **XMLParser implemented**  
✅ **XMLGenerator implemented**  
⏳ **Testing with real examples** (NEXT)  
⏳ **UI integration** (AFTER testing)

**Next:** Test parsers with all 25 XML examples from docs/web/examples/! 🧪

