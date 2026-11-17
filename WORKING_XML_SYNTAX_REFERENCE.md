# WebHarvest XML Syntax Reference

**Source fixtures**: `webharvest-core/src/test/resources/integration-tests/new_plugins/`  
**Reviewed against**: `main` at `72c7e21f27e73c87cfc0e1a55b285775a7fdec7c` (2026-11-17)  
**Evidence note**: Fixture presence is not proof that every example is exercised by a meaningful integration assertion. Coverage is being reconciled in [Issue #98](https://github.com/web-harvest/web-harvest/issues/98).

---

## Core syntax examples

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

**Update an existing variable** (`<set-var>`):
```xml
<!-- Current spelling -->
<set-var var="counter" value="${counter + 1}"/>

<!-- Legacy WebHarvest 1.0 spelling remains supported -->
<set-var name="counter" value="${counter + 1}"/>
```

The `name` and `var` attributes are both accepted. If both are supplied, they must have the same value. The plugin also accepts a body value; see `SetVarPlugin.java` for the exact contract.

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
    <xpath expression="//h1/text()">
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

## Complete network example (manual use)

This example calls `https://httpbin.org/get` and therefore requires network access. It is not a deterministic offline integration-test contract.

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
        <xpath expression="//h1/text()">
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

## Legacy and unsupported syntax

### Wrong: `<var-def>` (old syntax)
```xml
❌ <var-def name="url">...</var-def>
✅ <def var="url">...</def>
```

### Legacy: `<var>`

`<var>` remains registered for compatibility. It is not the preferred way to read or define variables in new configurations:
```xml
<!-- Legacy lookup/definition form -->
<var name="url">https://example.com</var>

<!-- Preferred explicit forms -->
<def var="url">https://example.com</def>
<get var="url"/>
```

### Supported legacy attribute: `name` on `<set-var>`

```xml
<!-- Both are supported; do not provide conflicting values -->
<set-var name="test" value="value"/>
<set-var var="test" value="value"/>
```

---

## 📁 All Available Examples

**Location**: `webharvest-core/src/test/resources/integration-tests/new_plugins/`

**Files** (43 current XML fixtures; coverage strength varies):
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
- 22_file_plugin_example.xml
- 23_sleep_plugin_example.xml
- 24_empty_plugin_example.xml
- 25_exit_plugin_example.xml
- 26_return_plugin_example.xml
- 27_case_plugin_example.xml
- 28_list_plugin_example.xml
- 29_valueof_plugin_example.xml
- 30_text_plugin_example.xml
- 31_url_plugin_example.xml
- 32_http_plugin_example.xml
- 33_xslt_plugin_example.xml
- 34_include_plugin_example.xml
- 35_def_plugin_example.xml
- 37_ftp_plugin_example.xml
- 38_mail_plugin_example.xml
- 39_webbrowser_plugin_example.xml
- 40_zip_plugin_example.xml
- 41_body_plugin_example.xml
- 42_bodyprocessor_plugin_example.xml
- 43_call_plugin_example.xml
- 44_callparam_plugin_example.xml

**Important:** These are available fixtures, not a claim that all 43 have meaningful runtime assertions. Consult Issue #98 for the current test-evidence matrix.

---

**Status**: Syntax reference; verify behavior against current tests and plugin source  
**Use this**: As a starting point for XML authoring, not as a substitute for runtime contract tests

