# Web-Harvest Plugins & Extensions Guide

## 🔌 **Przewodnik po pluginach i rozszerzeniach**

Web-Harvest 2.1.0-SNAPSHOT oferuje rozbudowany system pluginów, który pozwala na łatwe rozszerzanie funkcjonalności.

## 📋 **Dostępne procesory**

### **🌐 HTTP & Web Processing**

#### **http** - HTTP Request Processor
```xml
<http url="https://example.com" method="get">
    <http-header name="User-Agent">Web-Harvest Bot</http-header>
    <http-param name="search">web scraping</http-param>
</http>
```

**Atrybuty:**
- `url` (wymagany) - URL do pobrania
- `method` - HTTP method (get, post, put, delete)
- `timeout` - timeout w milisekundach
- `followRedirects` - czy podążać za przekierowaniami (true/false)

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

**Atrybuty:**
- `advancedxmlescape` - zaawansowane escapowanie znaków XML
- `specialentities` - obsługa specjalnych encji HTML
- `unicodechars` - obsługa znaków Unicode
- `allowmultiwordattributes` - pozwala na atrybuty z wieloma słowami
- `omitunknowntags` - pomija nieznane tagi
- `treatunknowntagsascontent` - traktuje nieznane tagi jako zawartość
- `omitdeprtags` - pomija tagi deprecated
- `treatdeprtagsascontent` - traktuje tagi deprecated jako zawartość
- `omitcomments` - pomija komentarze HTML
- `omittermlenvelope` - pomija envelope XML

### **🔍 Data Extraction**

#### **xpath** - XPath Processor
```xml
<xpath expression="//div[@class='content']/h1">
    <html-to-xml advancedxmlescape="true">
        <http url="https://example.com"/>
    </html-to-xml>
</xpath>
```

**Atrybuty:**
- `expression` (wymagany) - wyrażenie XPath
- `namespaces` - definicje namespace'ów

#### **regexp** - Regular Expression Processor
```xml
<regexp pattern="&lt;title&gt;(.*?)&lt;/title&gt;">
    <http url="https://example.com"/>
</regexp>
```

**Atrybuty:**
- `pattern` (wymagany) - wzorzec regex
- `flags` - flagi regex (i, m, s, x)

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
<!-- Zapisywanie -->
<file path="output.xml" action="write">
    <template>
        <![CDATA[
        <results>
            <title>${title}</title>
        </results>
        ]]>
    </template>
</file>

<!-- Odczytywanie -->
<file path="input.xml" action="read"/>

<!-- Listowanie -->
<file path="directory/" action="list" listfiles="true" listdirs="true"/>
```

**Atrybuty:**
- `path` (wymagany) - ścieżka do pliku/katalogu
- `action` - akcja (read, write, append, list)
- `type` - typ pliku (text, binary)
- `charset` - kodowanie znaków
- `listfiles` - czy listować pliki (true/false)
- `listdirs` - czy listować katalogi (true/false)
- `listrecursive` - czy rekurencyjnie (true/false)

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

## 🔧 **Tworzenie własnych pluginów**

### **1. Struktura pluginu**

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
        
        // Logika pluginu
        String result = processData(attr1);
        
        return new NodeVariable(result);
    }
    
    private String processData(String input) {
        // Implementacja logiki
        return "Processed: " + input;
    }
}
```

### **2. Definicja pluginu**

```java
package com.example.webharvest.plugin;

import org.webharvest.runtime.processors.ProcessorDefinition;

public class MyPluginDefinition extends ProcessorDefinition {
    
    private String attr1;
    private String attr2;
    
    // Getters i setters
    public String getAttr1() { return attr1; }
    public void setAttr1(String attr1) { this.attr1 = attr1; }
    
    public String getAttr2() { return attr2; }
    public void setAttr2(String attr2) { this.attr2 = attr2; }
}
```

### **3. Rejestracja pluginu**

Plugin jest automatycznie rejestrowany dzięki adnotacji `@Autoscanned`. Upewnij się, że:

1. Klasa znajduje się w classpath
2. Ma adnotację `@Autoscanned`
3. Ma adnotację `@Definition` z poprawnymi parametrami
4. Ma adnotację `@TargetNamespace`

### **4. Testowanie pluginu**

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

## 📚 **Przykłady zaawansowanych pluginów**

### **1. Plugin do analizy sentymentu**

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
        // Prosta analiza sentymentu
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

**Użycie:**
```xml
<sentiment-analyzer>
    <template>This is a great product!</template>
</sentiment-analyzer>
```

### **2. Plugin do generowania raportów**

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
        // Implementacja generowania raportu
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

**Użycie:**
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

## 🚀 **Najlepsze praktyki**

### **1. Wydajność**
- Używaj `@Autoscanned` tylko gdy potrzebujesz automatycznej rejestracji
- Implementuj `InterruptedException` w długotrwałych operacjach
- Używaj `BaseTemplater` do ewaluacji wyrażeń

### **2. Obsługa błędów**
- Zawsze obsługuj wyjątki
- Zwracaj sensowne komunikaty błędów
- Używaj `NodeVariable` dla wyników

### **3. Dokumentacja**
- Dodaj Javadoc do wszystkich publicznych metod
- Opisz wszystkie atrybuty w `@Definition`
- Stwórz przykłady użycia

### **4. Testowanie**
- Napisz testy jednostkowe
- Testuj różne scenariusze
- Testuj obsługę błędów

## 📖 **Dodatkowe zasoby**

- **API Documentation:** `webharvest-ide/target/apidocs/index.html`
- **Przykłady:** Katalog `examples/`
- **Testy:** Katalog `src/test/java/`
- **GitHub:** [web-harvest/web-harvest](https://github.com/web-harvest/web-harvest)

---

**Web-Harvest Plugins Guide** - Rozszerzaj możliwości Web-Harvest! 🔌

