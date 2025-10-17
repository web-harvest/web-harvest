# XPath Best Practices in Web-Harvest

## Common Issue: Element vs Text Content

### Problem
When using XPath expressions like `//title` or `//h1`, Web-Harvest returns the **entire element** including tags:
```
Result: <h1>Herman Melville - Moby-Dick</h1>
```

### Solution
Use `/text()` or `string()` function to extract **text content only**:
```
Result: Herman Melville - Moby-Dick
```

---

## XPath Expression Patterns

### ✅ **Extracting Text Content**
When you want text only (for display, templates, variables):

```xml
<!-- INCORRECT (returns element with tags): -->
<xpath expression="//title">
    <get var="page"/>
</xpath>
<!-- Result: <title>My Page</title> -->

<!-- CORRECT (returns text only): -->
<xpath expression="//title/text()">
    <get var="page"/>
</xpath>
<!-- Result: My Page -->

<!-- ALTERNATIVE (using string function): -->
<xpath expression="string(//title)">
    <get var="page"/>
</xpath>
<!-- Result: My Page -->
```

### ✅ **Multiple Elements**
```xml
<!-- INCORRECT: -->
<xpath expression="//h1 | //h2 | //h3">

<!-- CORRECT: -->
<xpath expression="//h1/text() | //h2/text() | //h3/text()">
```

### ✅ **Nested Paths**
```xml
<!-- INCORRECT: -->
<xpath expression="//div[@id='content']//span">

<!-- CORRECT: -->
<xpath expression="//div[@id='content']//span/text()">
```

### ✅ **Extracting from Child Elements**
```xml
<loop item="row">
    <xpath expression="//row">
        <get var="dbResult"/>
    </xpath>
    <body>
        <product>
            <!-- INCORRECT: -->
            <id><xpath expression="id"><get var="row"/></xpath></id>
            
            <!-- CORRECT: -->
            <id><xpath expression="id/text()"><get var="row"/></xpath></id>
        </product>
    </body>
</loop>
```

---

## When NOT to Use /text()

### ℹ️ **Node Selection for Further Processing**
```xml
<!-- OK - selecting nodes for iteration: -->
<loop item="row">
    <xpath expression="//row">  <!-- Returns node objects -->
        <get var="xmlData"/>
    </xpath>
    <body>
        <!-- Process each row node -->
    </body>
</loop>

<!-- OK - getting element for further XPath: -->
<def var="contentDiv">
    <xpath expression="//div[@id='content']">  <!-- Returns element -->
        <get var="page"/>
    </xpath>
</def>
```

### ℹ️ **Attribute Extraction**
```xml
<!-- Attributes don't need /text(): -->
<xpath expression="//a/@href">
    <get var="page"/>
</xpath>
```

---

## Quick Reference

| Goal | XPath Expression | Example |
|------|------------------|---------|
| Get text content | `//element/text()` | `//h1/text()` → "Title" |
| Get text (function) | `string(//element)` | `string(//h1)` → "Title" |
| Get element | `//element` | `//h1` → `<h1>Title</h1>` |
| Get attribute | `//element/@attr` | `//a/@href` → "url" |
| Multiple text | `//e1/text() \| //e2/text()` | Multiple elements |

---

## Examples

### ✅ **Good Example: Text Extraction**
```xml
<def var="title">
    <xpath expression="//title/text()">
        <get var="page"/>
    </xpath>
</def>

<template>Page Title: ${title}</template>
<!-- Output: Page Title: My Page Title -->
```

### ❌ **Bad Example: Element Extraction**
```xml
<def var="title">
    <xpath expression="//title">  <!-- Missing /text() -->
        <get var="page"/>
    </xpath>
</def>

<template>Page Title: ${title}</template>
<!-- Output: Page Title: <title>My Page Title</title> -->
```

---

## Testing Your XPath

You can test XPath expressions using the `<file>` plugin:

```xml
<def var="test">
    <xpath expression="//h1/text()">
        <get var="page"/>
    </xpath>
</def>

<file action="write" path="test_output.txt">
    Result: ${test}
</file>
```

Check `test_output.txt` to verify the result matches your expectations.

---

## Summary

**General Rule:** If you're extracting content for display, templates, or text processing, **always use `/text()`** or `string()` function.

**Exception:** Only use `//element` (without `/text()`) when you need the element node for further XPath processing or iteration.

---

*Updated: October 10, 2025*  
*Version: 2.2.1*

