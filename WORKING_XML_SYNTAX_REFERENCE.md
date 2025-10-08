# Web-Harvest XML Syntax Reference (VERIFIED WORKING)

**Source**: webharvest-core/src/test/resources/integration-tests/new_plugins/  
**Date**: October 8, 2025  
**Status**: Extracted from PASSING integration tests

---

## ✅ VERIFIED WORKING SYNTAX

### Variables

**Define Variable** (`<def>`):
```xml
<def var="greeting">Hello World!</def>
<def var="url">https://example.com</def>
```

**Get Variable** (`<get>`):
```xml
<get var="greeting"/>
```

**Set Variable** (`<set-var>`):
```xml
<!-- Attribute: var, value -->
<set-var var="testVar" value="Hello World"/>
<set-var var="numberVar" value="42"/>
<set-var var="expressionVar" value="${1 + 2}"/>
```

### HTTP

**HTTP Request** (`<http>`):
```xml
<def var="response">
    <http url="https://httpbin.org/get"/>
</def>
```

**With Variable Interpolation**:
```xml
<def var="url">https://httpbin.org/get</def>
<def var="response">
    <http url="${url}"/>
</def>
```

### HTML/XML Processing

**HTML to XML** (`<html-to-xml>`):
```xml
<def var="htmlContent">&lt;html&gt;&lt;body&gt;&lt;h1&gt;Test&lt;/h1&gt;&lt;/body&gt;&lt;/html&gt;</def>
<def var="xmlContent">
    <html-to-xml>
        <get var="htmlContent"/>
    </html-to-xml>
</def>
```

**XPath** (`<xpath>`):
```xml
<def var="title">
    <xpath expression="//h1">
        <get var="xmlContent"/>
    </xpath>
</def>
```

### File Operations

**Write File** (`<file>`):
```xml
<file action="write" path="target/test-output/output.txt">
    <get var="response"/>
</file>
```

**With Template**:
```xml
<file action="write" path="target/test-output/output.txt">
    <template>Title: ${title}</template>
</file>
```

### Template

**Variable Interpolation** (`<template>`):
```xml
<template>
    Variable testVar: ${testVar}
    Variable numberVar: ${numberVar}
</template>
```

---

## 📋 COMPLETE EXAMPLE (VERIFIED WORKING)

```xml
<?xml version="1.0" encoding="UTF-8"?>
<config xmlns="http://org.webharvest/schema/2.1/core">
    <!-- Define URL -->
    <def var="url">https://httpbin.org/get</def>
    
    <!-- Make HTTP request -->
    <def var="response">
        <http url="${url}"/>
    </def>
    
    <!-- Parse HTML -->
    <def var="htmlContent">&lt;html&gt;&lt;body&gt;&lt;h1&gt;Test&lt;/h1&gt;&lt;/body&gt;&lt;/html&gt;</def>
    
    <def var="xmlContent">
        <html-to-xml>
            <get var="htmlContent"/>
        </html-to-xml>
    </def>
    
    <!-- Extract with XPath -->
    <def var="title">
        <xpath expression="//h1">
            <get var="xmlContent"/>
        </xpath>
    </def>
    
    <!-- Output results -->
    <file action="write" path="target/test-output/output.txt">
        <template>Title extracted: ${title}</template>
    </file>
</config>
```

---

## ❌ COMMON MISTAKES

### Wrong: `<var-def>` (old syntax)
```xml
❌ <var-def name="url">...</var-def>
✅ <def var="url">...</def>
```

### Wrong: `<var>` (old syntax)
```xml
❌ <var name="url"/>
✅ <get var="url"/>
```

### Wrong: `<set-var name="...">` (incorrect attribute)
```xml
❌ <set-var name="test">value</set-var>
✅ <set-var var="test" value="value"/>
```

---

## 📁 All Available Examples

**Location**: `webharvest-core/src/test/resources/integration-tests/new_plugins/`

**Files** (43 examples):
- 01_constant_plugin_example.xml
- 02_text_plugin_example.xml
- 03_var_plugin_example.xml
- 04_def_plugin_example.xml
- 05_http_plugin_example.xml
- 06_html_to_xml_plugin_example.xml
- 07_xpath_plugin_example.xml
- 08_regexp_plugin_example.xml
- 09_json_to_xml_plugin_example.xml
- 10_xml_to_json_plugin_example.xml
- 11_if_else_plugin_example.xml
- 12_loop_plugin_example.xml
- 13_while_plugin_example.xml
- 14_tokenize_plugin_example.xml
- 15_script_plugin_example.xml
- 16_xslt_plugin_example.xml
- 17_xquery_plugin_example.xml
- 18_include_plugin_example.xml
- 19_try_catch_plugin_example.xml
- 20_setvar_plugin_example.xml
- 21_getvar_plugin_example.xml
- ... (and more)

**These are VERIFIED to work in integration tests!**

---

**Status**: Reference complete  
**Use this**: For IDE default config and documentation

