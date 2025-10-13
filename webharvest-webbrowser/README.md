# WebHarvest WebBrowser Module

JavaScript-enabled web browser support for WebHarvest scraping framework.

## Features

- **JavaScript Execution**: Render pages with JavaScript support
- **Dynamic Content**: Scrape AJAX-loaded and dynamically rendered content
- **Wait for Elements**: Wait for specific DOM elements to load
- **Screenshot Capture**: Take screenshots of rendered pages
- **Headless Mode**: Run browser in background (no GUI)

## Plugin

### WebBrowserPlugin (`<web-browser>`)

Renders web pages using embedded browser with JavaScript support.

#### Attributes:

| Attribute | Required | Description | Example |
|-----------|----------|-------------|---------|
| `url` | Yes | Target URL to render | `https://example.com` |
| `wait` | No | Wait time in milliseconds | `3000` |
| `width` | No | Browser viewport width | `1920` |
| `height` | No | Browser viewport height | `1080` |
| `screenshot` | No | Save screenshot to file path | `screenshot.png` |

## Usage Examples

### Example 1: Render JavaScript-Heavy Page

```xml
<!-- Regular HTTP can't see JavaScript content -->
<def var="staticHtml">
    <http url="https://spa-app.example.com"/>
</def>
<!-- Result: Just loading spinner HTML, no actual data -->

<!-- WebBrowser executes JavaScript -->
<def var="renderedHtml">
    <web-browser url="https://spa-app.example.com" wait="3000"/>
</def>
<!-- Result: Fully rendered page with AJAX data -->

<def var="products">
    <xpath expression="//div[@class='product']">
        <html-to-xml>${renderedHtml}</html-to-xml>
    </xpath>
</def>
```

### Example 2: Wait for AJAX Content

```xml
<def var="dynamicPage">
    <!-- Wait 5 seconds for AJAX to load -->
    <web-browser url="https://example.com/dynamic" wait="5000"/>
</def>

<def var="data">
    <xpath expression="//div[@id='ajax-content']">
        <html-to-xml>${dynamicPage}</html-to-xml>
    </xpath>
</def>
```

### Example 3: Take Screenshot

```xml
<!-- Capture screenshot of rendered page -->
<web-browser url="https://example.com" 
             wait="2000"
             width="1920"
             height="1080"
             screenshot="screenshots/page.png"/>

<!-- Screenshot saved to file, page HTML returned as result -->
```

### Example 4: Scrape Single Page Application (SPA)

```xml
<!-- React/Vue/Angular app -->
<def var="spaContent">
    <web-browser url="https://spa.example.com/products" wait="4000"/>
</def>

<loop item="product">
    <xpath expression="//div[@class='product-card']">
        <html-to-xml>${spaContent}</html-to-xml>
    </xpath>
    
    <def var="title">
        <xpath expression=".//h3/text()">${product}</xpath>
    </def>
    
    <file path="products/${title}.txt" action="write">
        ${product}
    </file>
</loop>
```

### Example 5: Infinite Scroll Pages

```xml
<!-- Page that loads more content on scroll -->
<def var="allContent">
    <web-browser url="https://example.com/infinite-scroll" 
                 wait="10000"/>  <!-- Wait for multiple scroll loads -->
</def>

<def var="items">
    <xpath expression="//div[@class='item']">
        <html-to-xml>${allContent}</html-to-xml>
    </xpath>
</def>
```

## When to Use WebBrowser vs HTTP

### Use `<http>` when:
- ✅ Page content is in initial HTML response
- ✅ No JavaScript required
- ✅ API endpoints (JSON, XML)
- ✅ Better performance needed

### Use `<web-browser>` when:
- ✅ Content loaded via AJAX/fetch
- ✅ Single Page Applications (React, Vue, Angular)
- ✅ JavaScript renders content
- ✅ Need screenshots
- ✅ Infinite scroll or lazy loading

## Dependencies

```xml
<dependency>
    <groupId>org.webharvest</groupId>
    <artifactId>webharvest-webbrowser</artifactId>
    <version>2.2.0</version>
</dependency>
```

External dependencies (auto-included):
- **HtmlUnit** - Headless browser with JavaScript support
- **Rhino** - JavaScript engine
- **Selenium WebDriver** (optional) - Alternative browser engine

## Performance Considerations

- **Slower than HTTP**: JavaScript execution adds overhead
- **Memory intensive**: Browser engine requires more memory
- **Wait times**: Increase wait parameter if content not loaded
- **Headless mode**: Runs without GUI (faster)

## Common Use Cases

1. **SPA scraping**: React, Vue, Angular applications
2. **AJAX content**: Dynamically loaded product lists, comments
3. **Infinite scroll**: Social media feeds, image galleries
4. **Screenshot capture**: Visual testing, archival
5. **Form automation**: Fill and submit JavaScript-powered forms

## Troubleshooting

### Content not appearing:
- Increase `wait` time (JavaScript may need more time to execute)
- Check browser console for JavaScript errors
- Verify page works in regular browser

### Screenshots blank:
- Increase wait time before screenshot
- Verify width/height are sufficient
- Check if page requires authentication

### Out of memory:
- Reduce number of concurrent web-browser operations
- Increase JVM heap size: `java -Xmx2G -jar webharvest-cli.jar`

## Testing

Run tests:
```bash
cd webharvest-webbrowser
mvn test
```

## License

Copyright (c) 2006-2025 the original author or authors. See LICENSE file for details.





