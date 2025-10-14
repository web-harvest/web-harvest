# WebHarvest Core Plugins Inventory

**Date**: October 8, 2025  
**Total Core Plugins**: 47  
**Annotation**: @CorePlugin  
**Status**: Audit in progress

---

## 📋 Plugin List

Extracting from: `webharvest-core/src/main/java/org/webharvest/plugin/core/*Plugin.java`

| # | Plugin Class | Element Name | Status | Integration Test |
|---|--------------|--------------|--------|------------------|
| 1 | BodyPlugin | body | ? | ❌ |
| 2 | BodyProcessorPlugin | body-processor | ? | ❌ |
| 3 | CallParamPlugin | call-param | ? | ❌ |
| 4 | CallPlugin | call | ? | ❌ |
| 5 | CasePlugin | case | ? | ❌ |
| 6 | CatchPlugin | catch | ? | ❌ |
| 7 | ConfigPlugin | config | ? | ❌ |
| 8 | ConstantPlugin | constant | ? | ❌ |
| 9 | DefPlugin | **def** | ? | ❌ CRITICAL |
| 10 | ElsePlugin | else | ? | ❌ |
| 11 | EmptyPlugin | empty | ? | ❌ |
| 12 | ExitPlugin | exit | ? | ❌ |
| 13 | FilePlugin | file | ? | ✅ |
| 14 | FunctionPlugin | function | ? | ❌ |
| 15 | GetVarPlugin | **get** | ? | ❌ CRITICAL |
| 16 | HtmlToXmlPlugin | **html-to-xml** | ? | ✅ |
| 17 | HttpHeaderPlugin | http-header | ? | ❌ |
| 18 | HttpParamPlugin | http-param | ? | ❌ |
| 19 | HttpPlugin | **http** | ? | ✅ |
| 20 | IfPlugin | if | ? | ❌ |
| 21 | IncludePlugin | include | ? | ❌ |
| 22 | JsonToXmlPlugin | json-to-xml | ? | ❌ |
| 23 | ListPlugin | list | ? | ❌ |
| 24 | LoopPlugin | loop | ? | ✅ |
| 25 | RegexpPatternPlugin | regexp-pattern | ? | ❌ |
| 26 | RegexpPlugin | regexp | ? | ✅ |
| 27 | RegexpResultPlugin | regexp-result | ? | ❌ |
| 28 | RegexpSourcePlugin | regexp-source | ? | ❌ |
| 29 | ReturnPlugin | return | ? | ❌ |
| 30 | ScriptPlugin | script | ? | ✅ |
| 31 | SetVarPlugin | **set** | ? | ❌ CRITICAL |
| 32 | SleepPlugin | sleep | ? | ❌ |
| 33 | StylesheetPlugin | stylesheet | ? | ❌ |
| 34 | TemplatePlugin | template | ? | ✅ |
| 35 | TextPlugin | text | ? | ❌ |
| 36 | TokenizePlugin | tokenize | ? | ❌ |
| 37 | TryPlugin | try | ? | ❌ |
| 38 | ValueOfPlugin | value-of | ? | ❌ |
| 39 | VarPlugin | var | ? | ❌ |
| 40 | WhilePlugin | while | ? | ❌ |
| 41 | XMLPlugin | xml | ? | ❌ |
| 42 | XPathPlugin | **xpath** | ? | ✅ |
| 43 | XQExpressionPlugin | xq-expression | ? | ❌ |
| 44 | XQParamPlugin | xq-param | ? | ❌ |
| 45 | XQueryPlugin | xquery | ? | ❌ |
| 46 | XmlToJsonPlugin | xml-to-json | ? | ❌ |
| 47 | XsltPlugin | xslt | ? | ❌ |

**CRITICAL Plugins** (must work for basic scraping):
- `<def var>` - Define variables
- `<get var>` - Get variables
- `<set var>` - Set variables
- `<http>` - HTTP requests
- `<html-to-xml>` - Parse HTML
- `<xpath>` - Extract data

**Integration Tests**: 7/47 (15%)  
**Target**: 47/47 (100%)

---

## 🚨 Issues Found

### Issue 1: User tried `<var-def>` but it doesn't exist!

**Error**: `Unexpected configuration element: (http://org.webharvest/schema/2.1/core,var-def)`

**Cause**: User confusion - old syntax vs new syntax

**Old syntax** (WebHarvest 1.x):
```xml
<var-def name="url">https://example.com</var-def>
<var name="url"/>
```

**New syntax** (WebHarvest 2.1+):
```xml
<def var="url">https://example.com</def>
<get var="url"/>
```

**Solution**: Add migration guide in documentation!

### Issue 2: Duplicate "empty" plugin registration

**Error**: `Failed to register plugin EmptyProcessor: Plugin "empty" is already registered!`

**Cause**: Both old processor AND new plugin systems trying to register "empty"

**Solution**: Check AnnotatedPluginsPostProcessor vs CorePluginsPostProcessor

---

## 🎯 ACTION PLAN

### Step 1: Extract @CorePlugin Annotations (Today)

Create detailed inventory with actual element names from code.

### Step 2: Create Critical Plugin Tests (This Week)

**Priority 1 Tests** (6 plugins):
```java
1. DefPluginIntegrationTest
2. GetVarPluginIntegrationTest  
3. SetVarPluginIntegrationTest
4. HttpPluginIntegrationTest
5. HtmlToXmlPluginIntegrationTest
6. XPathPluginIntegrationTest
```

### Step 3: Create Remaining Tests (Next Week)

**Priority 2 Tests** (10 plugins):
- LoopPlugin, IfPlugin, WhilePlugin, TryPlugin
- FilePlugin, TemplatePlugin, IncludePlugin
- RegexpPlugin, JsonToXmlPlugin, XmlToJsonPlugin

**Priority 3 Tests** (31 remaining plugins)

### Step 4: Fix Registration Issues

- Resolve duplicate "empty" registration
- Ensure all plugins load correctly
- Verify namespace handling

---

## 📊 Progress Tracking

```
✅ Identified: 47 core plugins
❌ Integration tested: 7/47 (15%)
❌ All tests passing: Unknown
❌ Plugin registration verified: No

Target: 100% tested, 100% passing
```

---

**Next**: Extract detailed @CorePlugin data and create first test batch

