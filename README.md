# Web-Harvest 2.1.0-SNAPSHOT

## 🚀 **New version with fixed bugs and new features**

Web-Harvest is a powerful web scraping and data extraction tool that has been significantly improved in this version.

## ✨ **New features in version 2.1.0-SNAPSHOT**

### 🔧 **Fixed bugs**
- ✅ **Fixed EventBus bugs in IDE** - resolved `ClassCastException` issues in event handling
- ✅ **Enhanced HTML parsing** - added advanced options for `html-to-xml` processor
- ✅ **Backward compatibility** - examples without namespace work correctly
- ✅ **Fixed IOC issues** - hybrid approach to Inversion of Control

### 🌟 **New business examples**
- **`modern_web_scraping.xml`** - advanced scraping techniques with better HTML parsing
- **`ecommerce_monitoring.xml`** - price monitoring and alert system
- **`social_media_analytics.xml`** - sentiment analysis and engagement metrics
- **`api_integration.xml`** - REST API integration and data processing
- **`data_processing_pipeline.xml`** - advanced data analysis and reporting
- **`improved_nytimes.xml`** - enhanced parsing techniques for complex pages

### 🛠 **Enhanced processors**

#### **html-to-xml processor - new options:**
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

**Options:**
- `advancedxmlescape="true"` - advanced XML character escaping
- `specialentities="true"` - support for special HTML entities
- `unicodechars="true"` - Unicode character support
- `allowmultiwordattributes="true"` - allows multi-word attributes
- `omitunknowntags="true"` - omits unknown tags
- `treatunknowntagsascontent="true"` - treats unknown tags as content
- `omitdeprtags="true"` - omits deprecated tags
- `treatdeprtagsascontent="true"` - treats deprecated tags as content
- `omitcomments="true"` - omits HTML comments
- `omittermlenvelope="true"` - omits XML envelope

## 🚀 **Quick start**

### **Installation**
```bash
# Clone repository
git clone https://github.com/web-harvest/web-harvest.git
cd web-harvest

# Build project
mvn clean install

# Run IDE
java -jar webharvest-ide/target/webharvest-ide-2.1.0-SNAPSHOT.jar
```

### **Command line execution**
```bash
# Run example
java -cp "webharvest-ide/target/webharvest-ide-2.1.0-SNAPSHOT.jar" org.webharvest.CommandLine config=examples/modern_web_scraping.xml
```

## 📖 **Documentation**

### **Basic concepts**

#### **1. XML Configuration**
```xml
<?xml version="1.0" encoding="UTF-8"?>
<config xmlns="http://web-harvest.sourceforge.net/schema/1.0/config">
    <!-- Your configuration here -->
</config>
```

#### **2. Basic processors**

**HTTP Request:**
```xml
<http url="https://example.com">
    <http-header name="User-Agent">Web-Harvest Bot</http-header>
    <http-param name="search">web scraping</http-param>
</http>
```

**HTML to XML:**
```xml
<html-to-xml advancedxmlescape="true" specialentities="true">
    <http url="https://example.com"/>
</html-to-xml>
```

**XPath Extraction:**
```xml
<xpath expression="//div[@class='content']/h1">
    <html-to-xml advancedxmlescape="true">
        <http url="https://example.com"/>
    </html-to-xml>
</xpath>
```

**File Output:**
```xml
<file path="output.xml" action="write">
    <template>
        <![CDATA[
        <results>
            <title>${title}</title>
            <content>${content}</content>
        </results>
        ]]>
    </template>
</file>
```

### **Advanced features**

#### **1. Loops and conditions**
```xml
<loop item="item" index="i">
    <list>
        <constant>Item 1</constant>
        <constant>Item 2</constant>
        <constant>Item 3</constant>
    </list>
    <body>
        <if condition="${i < 2}">
            <then>
                <file path="item_${i}.txt" action="write">
                    <template>${item}</template>
                </file>
            </then>
        </if>
    </body>
</loop>
```

#### **2. Error handling**
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

#### **3. JSON processing**
```xml
<var-def name="jsonData">
    <http url="https://api.example.com/data"/>
</var-def>

<var-def name="xmlData">
    <json-to-xml>
        <script>jsonData</script>
    </json-to-xml>
</var-def>
```

#### **4. XQuery Processing**
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
                return
                <item>
                    <name>{$item/name}</name>
                    <price>{$item/price}</price>
                </item>
            }
        </results>
        ]]>
    </xq-expression>
</xquery>
```

## 🎯 **Usage examples**

### **1. E-commerce price monitoring**
```xml
<config xmlns="http://web-harvest.sourceforge.net/schema/1.0/config">
    <var-def name="productUrl">https://shop.example.com/product/123</var-def>
    <var-def name="priceThreshold">100.00</var-def>
    
    <var-def name="productData">
        <html-to-xml advancedxmlescape="true" specialentities="true">
            <http url="${productUrl}"/>
        </html-to-xml>
    </var-def>
    
    <var-def name="currentPrice">
        <xpath expression="//span[@class='price']">
            <script>productData</script>
        </xpath>
    </var-def>
    
    <if condition="${currentPrice < priceThreshold}">
        <then>
            <file path="price_alert.txt" action="write">
                <template>
                    <![CDATA[
                    PRICE ALERT! 
                    Product: ${productUrl}
                    Current Price: $${currentPrice}
                    Threshold: $${priceThreshold}
                    ]]>
                </template>
            </file>
        </then>
    </if>
</config>
```

### **2. Social media sentiment analysis**
```xml
<config xmlns="http://web-harvest.sourceforge.net/schema/1.0/config">
    <var-def name="hashtag">#technology</var-def>
    
    <!-- Get data from API -->
    <var-def name="socialData">
        <http url="https://api.social.com/posts?hashtag=${hashtag}"/>
    </var-def>
    
    <!-- Convert JSON to XML -->
    <var-def name="postsXml">
        <json-to-xml>
            <script>socialData</script>
        </json-to-xml>
    </var-def>
    
    <!-- Analyze sentiment -->
    <var-def name="sentimentAnalysis">
        <xquery>
            <xq-param name="posts">
                <script>postsXml</script>
            </xq-param>
            <xq-expression>
                <![CDATA[
                declare variable $posts as node() external;
                
                let $positive := count($posts//post[sentiment = "positive"])
                let $negative := count($posts//post[sentiment = "negative"])
                let $total := count($posts//post)
                
                return
                <sentiment_analysis>
                    <total_posts>{$total}</total_posts>
                    <positive_percentage>{round($positive div $total * 100)}</positive_percentage>
                    <negative_percentage>{round($negative div $total * 100)}</negative_percentage>
                </sentiment_analysis>
                ]]>
            </xq-expression>
        </xquery>
    </var-def>
    
    <!-- Save results -->
    <file path="sentiment_report.xml" action="write">
        <script>sentimentAnalysis</script>
    </file>
</config>
```

## 🔧 **Troubleshooting**

### **Common problems and solutions**

#### **1. Error: "Character reference is an invalid XML character"**
**Problem:** HTML contains invalid XML characters (e.g. `&#27`)

**Solution:** Use advanced `html-to-xml` options:
```xml
<html-to-xml 
    advancedxmlescape="true" 
    specialentities="true" 
    unicodechars="true">
    <http url="https://problematic-site.com"/>
</html-to-xml>
```

#### **2. Error: "File doesn't exist!" when saving**
**Problem:** `FileProcessor` by default tries to read the file

**Solution:** Add `action="write"`:
```xml
<file path="output.xml" action="write">
    <template>Your content</template>
</file>
```

#### **3. Error: "Unexpected configuration element"**
**Problem:** Missing namespace in configuration

**Solution:** Add namespace to `<config>` element:
```xml
<config xmlns="http://web-harvest.sourceforge.net/schema/1.0/config">
    <!-- Your configuration -->
</config>
```

## 🧪 **Testing**

### **Running tests**
```bash
# All tests
mvn test

# Core tests only
mvn test -pl webharvest-core

# IDE tests only
mvn test -pl webharvest-ide
```

### **Testing examples**
```bash
# Test simple example
java -cp "webharvest-ide/target/webharvest-ide-2.1.0-SNAPSHOT.jar" org.webharvest.CommandLine config=examples/simple_test.xml

# Test advanced example
java -cp "webharvest-ide/target/webharvest-ide-2.1.0-SNAPSHOT.jar" org.webharvest.CommandLine config=examples/modern_web_scraping.xml
```

## 📊 **Performance**

### **Optimization for large data**
1. **Use `omitunknowntags="true"`** - omits unknown HTML tags
2. **Use `omitcomments="true"`** - omits HTML comments
3. **Use `omittermlenvelope="true"`** - omits XML envelope
4. **Filter data early** - use XPath to select only needed elements

### **Optimization example:**
```xml
<html-to-xml 
    advancedxmlescape="true" 
    specialentities="true" 
    omitunknowntags="true"
    omitcomments="true"
    omittermlenvelope="true">
    <http url="https://large-site.com"/>
</html-to-xml>
```

## 🤝 **Contributing**

### **How to add a new processor**
1. Create a class implementing `Processor`
2. Add `@Autoscanned` and `@Definition` annotations
3. Add unit tests
4. Update documentation

### **How to add a new example**
1. Create XML file in `examples/` directory
2. Add link in `webharvest-ide/src/main/resources/org/webharvest/gui/resources/welcome.html`
3. Test the example
4. Add documentation

## 📝 **Changelog**

### **v2.1.0-SNAPSHOT (2025-09-06)**
- ✅ Fixed EventBus bugs in IDE
- ✅ Added advanced options for html-to-xml processor
- ✅ Implemented backward compatibility
- ✅ Added 6 new business examples
- ✅ Fixed IOC issues
- ✅ Enhanced error handling
- ✅ Added comprehensive documentation

## 📄 **License**

Web-Harvest is available under Apache 2.0 license. See `LICENSE` file for details.

## 📞 **Support**

- **Issues:** [GitHub Issues](https://github.com/web-harvest/web-harvest/issues)
- **Documentation:** [Wiki](https://github.com/web-harvest/web-harvest/wiki)
- **Examples:** `examples/` directory in repository

---

**Web-Harvest 2.1.0-SNAPSHOT** - Powerful web scraping and data extraction tool! 🚀

