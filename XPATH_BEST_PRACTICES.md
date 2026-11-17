# XPath and CSS Selector Best Practices

This guide documents a small set of behaviors supported by the current Core implementation and tests. It is not a complete XPath reference.

**Verification baseline:** `main` at `52c9bcbf6103d24d2b1beed45e33df2bd168aadf` (2026-11-17).

## How the Core plugin receives input

The Core `xpath` plugin evaluates its `expression` against content produced by its body. The current implementation compiles the expression with Saxon and converts the result through `XmlUtil.createListOfXmlNodes`. The optional `type="css"` path uses Jsoup CSS selectors.

A common variable-backed input is:

```xml
<def var="page"><![CDATA[<html><body><h1>Test Title</h1></body></html>]]></def>

<def var="heading">
    <xpath expression="//h1/text()">
        <get var="page"/>
    </xpath>
</def>
```

## Choose an expression for the value you need

### Direct text child nodes

Use `text()` when you specifically want direct text-node children of the selected element:

```xml
<xpath expression="//h1/text()">
    <get var="page"/>
</xpath>
```

This does not mean “all descendant text” for every possible markup shape. For example, with `<h1>Windy <em>Farm</em></h1>`, `text()` selects the direct text nodes, not the nested `em` element's text node.

### XPath string value

Use `string(...)` when you want the XPath string-value of the selected node:

```xml
<xpath expression="string(//h1)">
    <get var="page"/>
</xpath>
```

For the simple fixture above, both `//h1/text()` and `string(//h1)` are asserted to produce `Test Title` by the deterministic Core integration test.

### Select elements for further processing

Select elements without `text()` when the next step needs the node structure:

```xml
<xpath expression="//row">
    <get var="xmlData"/>
</xpath>
```

Do not assume that a selected node is always serialized as markup in the resulting variable. The exact result representation depends on the expression result and the runtime conversion; verify the behavior needed by the consuming step.

### Select attributes

Attribute expressions do not use `text()`:

```xml
<xpath expression="//a/@href">
    <get var="page"/>
</xpath>
```

### Select multiple paths

XPath union can combine node selections:

```xml
<xpath expression="//h1/text() | //h2/text()">
    <get var="page"/>
</xpath>
```

The result is a sequence of selected nodes; do not assume it is a single scalar string.

## CSS selector mode

The Core plugin also has a CSS selector path. Set `type="css"` and provide a Jsoup selector expression:

```xml
<xpath type="css" expression="a.link">
    <get var="page"/>
</xpath>
```

Do not mix CSS selector syntax with XPath expressions. The default mode is XPath; CSS is selected explicitly with `type="css"`.

## Evidence and limits

The current integration test is:

`webharvest-core/src/test/java/org/webharvest/plugin/core/integration/XPathWithGetVarIntegrationTest.java`

- `testXPathTextNode` asserts that `//h1/text()` produces exactly `Test Title` for its fixture.
- `testXPathStringFunction` asserts the same for `string(//h1)`.
- `testXPathElementVsText` only checks that the result is non-empty and prints diagnostic output. It does **not** assert whether `//h1` includes markup, so this guide makes no claim about that representation.

The unit tests in `XPathPluginTest` and `XPathPluginAdditionalTest` include configuration and mocked execution checks; non-null return assertions there are not substitutes for end-to-end result assertions.

For additional behavior, inspect `webharvest-core/src/main/java/org/webharvest/plugin/core/XPathPlugin.java` and add a deterministic runtime assertion before documenting a new guarantee.

