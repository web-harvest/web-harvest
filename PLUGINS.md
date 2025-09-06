# Web-Harvest Plugins & Extensions Guide

## 🔌 **Plugins and Extensions Guide**

Web-Harvest 2.1.0-SNAPSHOT offers an extensive plugin system that allows easy extension of functionality.

## 📋 **Available processors**

### **🌐 HTTP & Web Processing**

#### **http** - HTTP Request Processor
```xml
<http url="https://example.com" method="get">
    <http-header name="User-Agent">Web-Harvest Bot</http-header>
    <http-param name="search">web scraping</http-param>
</http>
```

**Attributes:**
- `url` (required) - URL to fetch
- `method` - HTTP method (get, post, put, delete)
- `timeout` - timeout in milliseconds
- `followRedirects` - whether to follow redirects (true/false)

#### **html-to-xml** - HTML to XML Converter
```xml
<html-to-xml 
    advancedxmlescape="true" 
    specialentities="true" 
    unicodechars="true" 
    allowmultiwordattributes="true" 
    omitunknowntags="true"
    treatunknowntagsascontent="true"
    omitdeprtags="true"
    treatdeprtagsascontent="true"
    omitcomments="true"
    omittermlenvelope="true">
    <http url="https://example.com"/>
</html-to-xml>
```

**Attributes:**
- `advancedxmlescape` - advanced XML character escaping
- `specialentities` - support for special HTML entities
- `unicodechars` - Unicode character support
- `allowmultiwordattributes` - allows multi-word attributes
- `omitunknowntags` - omits unknown tags
- `treatunknowntagsascontent` - treats unknown tags as content
- `omitdeprtags` - omits deprecated tags
- `treatdeprtagsascontent` - treats deprecated tags as content
- `omitcomments` - omits HTML comments
- `omittermlenvelope` - omits XML envelope

### **🔍 Data Extraction**

#### **xpath** - XPath Processor
```xml
<xpath expression="//div[@class='content']/h1">
    <html-to-xml advancedxmlescape="true">
        <http url="https://example.com"/>
    </html-to-xml>
</xpath>
```

**Attributes:**
- `expression` (required) - XPath expression
- `namespaces` - namespace definitions

#### **regexp** - Regular Expression Processor
```xml
<regexp pattern="&lt;title&gt;(.*?)&lt;/title&gt;">
    <http url="https://example.com"/>
</regexp>
```

**Attributes:**
- `pattern` (required) - regex pattern
- `flags` - regex flags (i, m, s, x)

#### **xquery** - XQuery Processor
```xml
<xquery>
    <xq-param name="data">
        <script>xmlData</script>
    </xq-param>
    <xq-expression>
        <![CDATA[
        declare variable $data as node() external;
        <results>
            {
                for $item in $data//item
                return <item>{$item/text()}</item>
            }
        </results>
        ]]>
    </xq-expression>
</xquery>
```

### **📁 File Operations**

#### **file** - File Processor
```xml
<!-- Writing -->
<file path="output.xml" action="write">
    <template>
        <![CDATA[
        <results>
            <title>${title}</title>
        </results>
        ]]>
    </template>
</file>

<!-- Reading -->
<file path="input.xml" action="read"/>

<!-- Listing -->
<file path="directory/" action="list" listfiles="true" listdirs="true"/>
```

**Attributes:**
- `path` (required) - path to file/directory
- `action` - action (read, write, append, list)
- `type` - file type (text, binary)
- `charset` - character encoding
- `listfiles` - whether to list files (true/false)
- `listdirs` - whether to list directories (true/false)
- `listrecursive` - whether recursively (true/false)

### **🔄 Data Processing**

#### **json-to-xml** - JSON to XML Converter
```xml
<json-to-xml>
    <http url="https://api.example.com/data"/>
</json-to-xml>
```

#### **xml-to-json** - XML to JSON Converter
```xml
<xml-to-json>
    <script>xmlData</script>
</xml-to-json>
```

#### **xslt** - XSLT Processor
```xml
<xslt>
    <template>
        <![CDATA[
        <?xml version="1.0" encoding="UTF-8"?>
        <xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
            <xsl:template match="/">
                <transformed>
                    <xsl:value-of select="//title"/>
                </transformed>
            </xsl:template>
        </xsl:stylesheet>
        ]]>
    </template>
    <script>xmlData</script>
</xslt>
```

### **📊 Control Flow**

#### **if** - Conditional Processor
```xml
<if condition="${price < 100}">
    <then>
        <file path="cheap_products.txt" action="write">
            <template>${productName} - $${price}</template>
        </file>
    </then>
    <else>
        <file path="expensive_products.txt" action="write">
            <template>${productName} - $${price}</template>
        </file>
    </else>
</if>
```

#### **loop** - Loop Processor
```xml
<loop item="item" index="i">
    <list>
        <constant>Item 1</constant>
        <constant>Item 2</constant>
    </list>
    <body>
        <file path="item_${i}.txt" action="write">
            <template>${item}</template>
        </file>
    </body>
</loop>
```

#### **while** - While Loop Processor
```xml
<while condition="${counter < 10}">
    <body>
        <var-def name="counter">
            <script>counter + 1</script>
        </var-def>
        <file path="iteration_${counter}.txt" action="write">
            <template>Iteration ${counter}</template>
        </file>
    </body>
</while>
```

#### **try** - Exception Handling
```xml
<try>
    <body>
        <http url="https://unreliable-site.com"/>
    </body>
    <catch>
        <file path="error.log" action="write">
            <template>Error: ${sys.lastException().getMessage()}</template>
        </file>
    </catch>
</try>
```

### **📧 Communication**

#### **mail** - Email Processor
```xml
<mail 
    host="smtp.gmail.com" 
    port="587" 
    username="user@gmail.com" 
    password="password"
    from="user@gmail.com"
    to="recipient@example.com"
    subject="Web-Harvest Report">
    <template>
        <![CDATA[
        <html>
            <body>
                <h1>Scraping Report</h1>
                <p>Found ${count} items</p>
            </body>
        </html>
        ]]>
    </template>
</mail>
```

### **🗄️ Database Operations**

#### **database** - Database Processor
```xml
<database 
    driver="com.mysql.jdbc.Driver"
    url="jdbc:mysql://localhost:3306/test"
    username="user"
    password="password">
    <db-param name="query">
        <template>SELECT * FROM products WHERE price < ?</template>
    </db-param>
    <db-param name="price">
        <constant>100</constant>
    </db-param>
</database>
```

### **📦 Archive Operations**

#### **zip** - ZIP Processor
```xml
<zip path="archive.zip" action="create">
    <file path="file1.txt" action="read"/>
    <file path="file2.txt" action="read"/>
</zip>
```

### **🌐 FTP Operations**

#### **ftp** - FTP Processor
```xml
<ftp 
    host="ftp.example.com"
    username="user"
    password="password"
    action="put"
    remoteFile="remote.txt"
    localFile="local.txt"/>
```

## 🔧 **Creating custom plugins**

### **1. Plugin structure**

```java
package com.example.webharvest.plugin;

import org.webharvest.runtime.processors.AbstractProcessor;
import org.webharvest.runtime.processors.Processor;
import org.webharvest.runtime.variables.Variable;
import org.webharvest.runtime.processors.DynamicScopeContext;
import org.webharvest.runtime.processors.ProcessorDefinition;
import org.webharvest.runtime.processors.plugins.Autoscanned;
import org.webharvest.runtime.processors.plugins.Definition;
import org.webharvest.runtime.processors.plugins.TargetNamespace;

@Autoscanned
@TargetNamespace({"http://web-harvest.sourceforge.net/schema/1.0/config"})
@Definition(
    value = "my-plugin",
    validAttributes = {"attr1", "attr2"},
    requiredAttributes = "attr1"
)
public class MyPluginProcessor extends AbstractProcessor<MyPluginDefinition> {
    
    @Override
    public Variable execute(DynamicScopeContext context) throws InterruptedException {
        String attr1 = BaseTemplater.evaluateToString(
            elementDef.getAttr1(), null, context);
        
        // Plugin logic
        String result = processData(attr1);
        
        return new NodeVariable(result);
    }
    
    private String processData(String input) {
        // Logic implementation
        return "Processed: " + input;
    }
}
```

### **2. Plugin definition**

```java
package com.example.webharvest.plugin;

import org.webharvest.runtime.processors.ProcessorDefinition;

public class MyPluginDefinition extends ProcessorDefinition {
    
    private String attr1;
    private String attr2;
    
    // Getters and setters
    public String getAttr1() { return attr1; }
    public void setAttr1(String attr1) { this.attr1 = attr1; }
    
    public String getAttr2() { return attr2; }
    public void setAttr2(String attr2) { this.attr2 = attr2; }
}
```

### **3. Plugin registration**

Plugin is automatically registered thanks to `@Autoscanned` annotation. Make sure that:

1. Class is in classpath
2. Has `@Autoscanned` annotation
3. Has `@Definition` annotation with correct parameters
4. Has `@TargetNamespace` annotation

### **4. Plugin testing**

```java
package com.example.webharvest.plugin;

import org.testng.annotations.Test;
import org.webharvest.runtime.processors.DynamicScopeContext;
import org.webharvest.runtime.processors.MyPluginDefinition;

public class MyPluginProcessorTest {
    
    @Test
    public void testPlugin() throws Exception {
        MyPluginProcessor processor = new MyPluginProcessor();
        MyPluginDefinition definition = new MyPluginDefinition();
        definition.setAttr1("test value");
        
        processor.setElementDef(definition);
        
        DynamicScopeContext context = new DynamicScopeContext();
        Variable result = processor.execute(context);
        
        assert result.toString().contains("Processed: test value");
    }
}
```

## 📚 **Advanced plugin examples**

### **1. Sentiment analysis plugin**

```java
@Autoscanned
@TargetNamespace({"http://web-harvest.sourceforge.net/schema/1.0/config"})
@Definition(value = "sentiment-analyzer")
public class SentimentAnalyzerProcessor extends AbstractProcessor<SentimentAnalyzerDefinition> {
    
    @Override
    public Variable execute(DynamicScopeContext context) throws InterruptedException {
        String text = BaseTemplater.evaluateToString(
            elementDef.getText(), null, context);
        
        String sentiment = analyzeSentiment(text);
        
        return new NodeVariable(sentiment);
    }
    
    private String analyzeSentiment(String text) {
        // Simple sentiment analysis
        String lowerText = text.toLowerCase();
        
        if (lowerText.contains("good") || lowerText.contains("great") || lowerText.contains("excellent")) {
            return "positive";
        } else if (lowerText.contains("bad") || lowerText.contains("terrible") || lowerText.contains("awful")) {
            return "negative";
        } else {
            return "neutral";
        }
    }
}
```

**Usage:**
```xml
<sentiment-analyzer>
    <template>This is a great product!</template>
</sentiment-analyzer>
```

### **2. Report generation plugin**

```java
@Autoscanned
@TargetNamespace({"http://web-harvest.sourceforge.net/schema/1.0/config"})
@Definition(
    value = "report-generator",
    validAttributes = {"format", "template"},
    requiredAttributes = "format"
)
public class ReportGeneratorProcessor extends AbstractProcessor<ReportGeneratorDefinition> {
    
    @Override
    public Variable execute(DynamicScopeContext context) throws InterruptedException {
        String format = BaseTemplater.evaluateToString(
            elementDef.getFormat(), null, context);
        String template = BaseTemplater.evaluateToString(
            elementDef.getTemplate(), null, context);
        
        String report = generateReport(format, template, context);
        
        return new NodeVariable(report);
    }
    
    private String generateReport(String format, String template, DynamicScopeContext context) {
        // Report generation implementation
        StringBuilder report = new StringBuilder();
        
        if ("html".equals(format)) {
            report.append("<html><body>");
            report.append(template);
            report.append("</body></html>");
        } else if ("json".equals(format)) {
            report.append("{");
            report.append("\"template\": \"").append(template).append("\"");
            report.append("}");
        }
        
        return report.toString();
    }
}
```

**Usage:**
```xml
<report-generator format="html" template="report.html">
    <template>
        <![CDATA[
        <h1>Scraping Report</h1>
        <p>Date: ${sys.datetime("yyyy-MM-dd")}</p>
        <p>Items found: ${itemCount}</p>
        ]]>
    </template>
</report-generator>
```

## 🚀 **Best practices**

### **1. Performance**
- Use `@Autoscanned` only when you need automatic registration
- Implement `InterruptedException` in long-running operations
- Use `BaseTemplater` for expression evaluation

### **2. Error handling**
- Always handle exceptions
- Return meaningful error messages
- Use `NodeVariable` for results

### **3. Documentation**
- Add Javadoc to all public methods
- Describe all attributes in `@Definition`
- Create usage examples

### **4. Testing**
- Write unit tests
- Test different scenarios
- Test error handling

## 📖 **Additional resources**

- **API Documentation:** `webharvest-ide/target/apidocs/index.html`
- **Examples:** `examples/` directory
- **Tests:** `src/test/java/` directory
- **GitHub:** [web-harvest/web-harvest](https://github.com/web-harvest/web-harvest)

---

**Web-Harvest Plugins Guide** - Extend Web-Harvest capabilities! 🔌

