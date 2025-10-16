/**
 * WebHarvest IDE - Modern Web-Based Configuration Editor
 * v2.2.0 - October 2025
 */

// Global state
let currentTheme = 'vs-dark';
let currentExecutionId = null;
let webSocket = null;

/**
 * Embedded example configurations
 */
const EMBEDDED_EXAMPLES = {
    'price_monitoring.xml': {
        title: 'Price Monitoring System',
        icon: 'dollar-sign',
        category: 'E-commerce',
        content: `<?xml version="1.0" encoding="UTF-8"?>
<config xmlns="http://org.webharvest/schema/2.1/core" charset="UTF-8">
    <!-- Business Scenario: Monitor book prices on books.toscrape.com -->
    <!-- Extract product titles, prices, and availability -->
    
    <def var="url">https://books.toscrape.com/</def>
    
    <def var="html">
        <http url="\${url}"/>
    </def>
    
    <def var="page">
        <html-to-xml>
            <get var="html"/>
        </html-to-xml>
    </def>
    
    <!-- Extract all product titles -->
    <def var="titles">
        <xpath expression="//article[@class='product_pod']//h3/a/@title">
            <get var="page"/>
        </xpath>
    </def>
    
    <!-- Extract all prices -->
    <def var="prices">
        <xpath expression="//article[@class='product_pod']//p[@class='price_color']/text()">
            <get var="page"/>
        </xpath>
    </def>
    
    <!-- Extract availability status -->
    <def var="availability">
        <xpath expression="//article[@class='product_pod']//p[contains(@class, 'availability')]/text()">
            <get var="page"/>
        </xpath>
    </def>
    
    <!-- Count total products -->
    <def var="productCount">
        <xpath expression="count(//article[@class='product_pod'])">
            <get var="page"/>
        </xpath>
    </def>
    
    <!-- Generate JSON output -->
    <def var="jsonOutput">
        <template>{
  "monitoring_date": "\${sys.datetime('yyyy-MM-dd HH:mm:ss')}",
  "source_url": "\${url}",
  "total_products": \${productCount},
  "status": "SUCCESS"
}</template>
    </def>
    
    <!-- Save results -->
    <file action="write" path="price_monitoring.json">
        <get var="jsonOutput"/>
    </file>
</config>`
    },

    'product_catalog_crawler.xml': {
        title: 'Multi-Level Catalog Crawler',
        icon: 'sitemap',
        category: 'Web Scraping',
        content: `<?xml version="1.0" encoding="UTF-8"?>
<config xmlns="http://org.webharvest/schema/2.1/core" charset="UTF-8">
    <!-- Business Scenario: Crawl multiple pages of product catalog -->
    <!-- Extract data from category pages and product details -->
    
    <def var="baseUrl">https://books.toscrape.com</def>
    <def var="categoryUrl">\${baseUrl}/catalogue/category/books_1/index.html</def>
    
    <!-- Level 1: Get category page -->
    <def var="categoryHtml">
        <http url="\${categoryUrl}"/>
    </def>
    
    <def var="categoryPage">
        <html-to-xml>
            <get var="categoryHtml"/>
        </html-to-xml>
    </def>
    
    <!-- Extract product links -->
    <def var="productLinks">
        <xpath expression="//article[@class='product_pod']//h3/a/@href">
            <get var="categoryPage"/>
        </xpath>
    </def>
    
    <!-- Count products found -->
    <def var="productsFound">
        <xpath expression="count(//article[@class='product_pod'])">
            <get var="categoryPage"/>
        </xpath>
    </def>
    
    <!-- Extract first product title as sample -->
    <def var="sampleTitle">
        <xpath expression="(//article[@class='product_pod']//h3/a/@title)[1]">
            <get var="categoryPage"/>
        </xpath>
    </def>
    
    <!-- Extract first product price as sample -->
    <def var="samplePrice">
        <xpath expression="(//article[@class='product_pod']//p[@class='price_color']/text())[1]">
            <get var="categoryPage"/>
        </xpath>
    </def>
    
    <!-- Generate crawl summary JSON -->
    <def var="crawlSummary">
        <template>{
  "crawl_date": "\${sys.datetime('yyyy-MM-dd HH:mm:ss')}",
  "category_url": "\${categoryUrl}",
  "products_found": \${productsFound},
  "sample_product": {
    "title": "\${sampleTitle}",
    "price": "\${samplePrice}"
  },
  "status": "COMPLETED"
}</template>
    </def>
    
    <!-- Save summary -->
    <file action="write" path="crawl_summary.json">
        <get var="crawlSummary"/>
    </file>
</config>`
    },

    'product_details_extractor.xml': {
        title: 'Product Details Extraction',
        icon: 'info-circle',
        category: 'E-commerce',
        content: `<?xml version="1.0" encoding="UTF-8"?>
<config xmlns="http://org.webharvest/schema/2.1/core" charset="UTF-8">
    <!-- Business Scenario: Extract detailed product information -->
    <!-- Get prices, ratings, availability, and descriptions -->
    
    <def var="url">https://books.toscrape.com/catalogue/a-light-in-the-attic_1000/index.html</def>
    
    <def var="html">
        <http url="\${url}"/>
    </def>
    
    <def var="page">
        <html-to-xml>
            <get var="html"/>
        </html-to-xml>
    </def>
    
    <!-- Extract product title -->
    <def var="productTitle">
        <xpath expression="//div[@class='col-sm-6 product_main']//h1/text()">
            <get var="page"/>
        </xpath>
    </def>
    
    <!-- Extract price -->
    <def var="productPrice">
        <xpath expression="//p[@class='price_color']/text()">
            <get var="page"/>
        </xpath>
    </def>
    
    <!-- Extract availability -->
    <def var="productAvailability">
        <xpath expression="//p[contains(@class, 'availability')]/text()">
            <get var="page"/>
        </xpath>
    </def>
    
    <!-- Extract rating -->
    <def var="productRating">
        <xpath expression="//p[contains(@class, 'star-rating')]/@class">
            <get var="page"/>
        </xpath>
    </def>
    
    <!-- Extract description -->
    <def var="productDescription">
        <xpath expression="//div[@id='product_description']/following-sibling::p/text()">
            <get var="page"/>
        </xpath>
    </def>
    
    <!-- Generate product JSON -->
    <def var="productJson">
        <template>{
  "extraction_date": "\${sys.datetime('yyyy-MM-dd HH:mm:ss')}",
  "product": {
    "title": "\${productTitle}",
    "price": "\${productPrice}",
    "availability": "\${productAvailability}",
    "rating": "\${productRating}",
    "description_length": \${productDescription.length()}
  },
  "status": "EXTRACTED"
}</template>
    </def>
    
    <!-- Save product data -->
    <file action="write" path="product_details.json">
        <get var="productJson"/>
    </file>
</config>`
    },

    'category_analyzer.xml': {
        title: 'Category Analysis Tool',
        icon: 'chart-bar',
        category: 'Analytics',
        content: `<?xml version="1.0" encoding="UTF-8"?>
<config xmlns="http://org.webharvest/schema/2.1/core" charset="UTF-8">
    <!-- Business Scenario: Analyze product distribution across categories -->
    <!-- Extract category data and generate statistics -->
    
    <def var="baseUrl">https://books.toscrape.com</def>
    
    <def var="html">
        <http url="\${baseUrl}"/>
    </def>
    
    <def var="page">
        <html-to-xml>
            <get var="html"/>
        </html-to-xml>
    </def>
    
    <!-- Extract all category names -->
    <def var="categories">
        <xpath expression="//ul[@class='nav nav-list']//ul//a/text()">
            <get var="page"/>
        </xpath>
    </def>
    
    <!-- Count total products on main page -->
    <def var="totalProducts">
        <xpath expression="count(//article[@class='product_pod'])">
            <get var="page"/>
        </xpath>
    </def>
    
    <!-- Calculate average price -->
    <def var="prices">
        <xpath expression="//p[@class='price_color']/text()">
            <get var="page"/>
        </xpath>
    </def>
    
    <!-- Extract first 5 product titles as sample -->
    <def var="sampleProducts">
        <xpath expression="//article[@class='product_pod'][position() &lt;= 5]//h3/a/@title">
            <get var="page"/>
        </xpath>
    </def>
    
    <!-- Generate analytics JSON -->
    <def var="analytics">
        <template>{
  "analysis_date": "\${sys.datetime('yyyy-MM-dd HH:mm:ss')}",
  "website": "\${baseUrl}",
  "metrics": {
    "total_products_displayed": \${totalProducts},
    "categories_count": "50+",
    "sample_products_extracted": 5
  },
  "status": "ANALYZED"
}</template>
    </def>
    
    <!-- Save analytics -->
    <file action="write" path="category_analytics.json">
        <get var="analytics"/>
    </file>
</config>`
    },

    'competitive_price_tracker.xml': {
        title: 'Competitive Price Tracking',
        icon: 'balance-scale',
        category: 'Business Intelligence',
        content: `<?xml version="1.0" encoding="UTF-8"?>
<config xmlns="http://org.webharvest/schema/2.1/core" charset="UTF-8">
    <!-- Business Scenario: Track competitor prices and find best deals -->
    <!-- Compare prices across multiple products -->
    
    <def var="storeUrl">https://books.toscrape.com/</def>
    
    <def var="html">
        <http url="\${storeUrl}"/>
    </def>
    
    <def var="page">
        <html-to-xml>
            <get var="html"/>
        </html-to-xml>
    </def>
    
    <!-- Use XQuery to find cheapest products -->
    <def var="priceAnalysis">
        <xquery>
            <xq-param name="content" type="node()">
                <get var="page"/>
            </xq-param>
            <xq-expression>
                <![CDATA[
                declare variable $content as node() external;
                
                <price_analysis>
                {
                    for $product in $content//article[@class="product_pod"]
                    let $title := normalize-space($product//h3/a/@title)
                    let $priceText := normalize-space($product//p[@class="price_color"])
                    let $price := number(substring-after($priceText, "£"))
                    where $price > 0
                    order by $price ascending
                    return
                        <product>
                            <title>{$title}</title>
                            <price>{$price}</price>
                            <price_formatted>{$priceText}</price_formatted>
                        </product>
                }
                </price_analysis>
                ]]>
            </xq-expression>
        </xquery>
    </def>
    
    <!-- Extract cheapest product -->
    <def var="cheapestTitle">
        <xpath expression="(//product/title/text())[1]">
            <get var="priceAnalysis"/>
        </xpath>
    </def>
    
    <def var="cheapestPrice">
        <xpath expression="(//product/price_formatted/text())[1]">
            <get var="priceAnalysis"/>
        </xpath>
    </def>
    
    <!-- Count products analyzed -->
    <def var="productsAnalyzed">
        <xpath expression="count(//product)">
            <get var="priceAnalysis"/>
        </xpath>
    </def>
    
    <!-- Generate competitive analysis JSON -->
    <def var="competitiveReport">
        <template>{
  "report_date": "\${sys.datetime('yyyy-MM-dd HH:mm:ss')}",
  "store_url": "\${storeUrl}",
  "products_analyzed": \${productsAnalyzed},
  "cheapest_product": {
    "title": "\${cheapestTitle}",
    "price": "\${cheapestPrice}"
  },
  "recommendation": "Monitor for price changes",
  "status": "COMPLETE"
}</template>
    </def>
    
    <!-- Save report -->
    <file action="write" path="competitive_analysis.json">
        <get var="competitiveReport"/>
    </file>
</config>`
    },

    'inventory_tracker.xml': {
        title: 'Inventory Availability Tracker',
        icon: 'boxes',
        category: 'Inventory Management',
        content: `<?xml version="1.0" encoding="UTF-8"?>
<config xmlns="http://org.webharvest/schema/2.1/core" charset="UTF-8">
    <!-- Business Scenario: Track product availability across catalog -->
    <!-- Monitor in-stock vs out-of-stock items -->
    
    <def var="catalogUrl">https://books.toscrape.com/</def>
    
    <def var="html">
        <http url="\${catalogUrl}"/>
    </def>
    
    <def var="page">
        <html-to-xml>
            <get var="html"/>
        </html-to-xml>
    </def>
    
    <!-- Extract availability information -->
    <def var="inventoryStatus">
        <xquery>
            <xq-param name="content" type="node()">
                <get var="page"/>
            </xq-param>
            <xq-expression>
                <![CDATA[
                declare variable $content as node() external;
                
                let $allProducts := $content//article[@class="product_pod"]
                let $inStock := $allProducts//p[contains(@class, 'availability') and contains(., 'In stock')]
                let $total := count($allProducts)
                let $available := count($inStock)
                let $percentage := if ($total > 0) then round($available div $total * 100) else 0
                
                return
                <inventory>
                    <total_items>{$total}</total_items>
                    <in_stock>{$available}</in_stock>
                    <availability_rate>{$percentage}</availability_rate>
                </inventory>
                ]]>
            </xq-expression>
        </xquery>
    </def>
    
    <!-- Extract metrics -->
    <def var="totalItems">
        <xpath expression="//total_items/text()">
            <get var="inventoryStatus"/>
        </xpath>
    </def>
    
    <def var="inStockItems">
        <xpath expression="//in_stock/text()">
            <get var="inventoryStatus"/>
        </xpath>
    </def>
    
    <def var="availabilityRate">
        <xpath expression="//availability_rate/text()">
            <get var="inventoryStatus"/>
        </xpath>
    </def>
    
    <!-- Generate inventory report JSON -->
    <def var="inventoryReport">
        <template>{
  "report_date": "\${sys.datetime('yyyy-MM-dd HH:mm:ss')}",
  "catalog_url": "\${catalogUrl}",
  "inventory": {
    "total_items": \${totalItems},
    "in_stock": \${inStockItems},
    "availability_rate": "\${availabilityRate}%"
  },
  "alert": \${availabilityRate} < 90 ? "LOW_STOCK_WARNING" : "NORMAL",
  "status": "TRACKED"
}</template>
    </def>
    
    <!-- Save inventory report -->
    <file action="write" path="inventory_report.json">
        <get var="inventoryReport"/>
    </file>
</config>`
    },

    'bestseller_finder.xml': {
        title: 'Bestseller Identification',
        icon: 'star',
        category: 'Market Research',
        content: `<?xml version="1.0" encoding="UTF-8"?>
<config xmlns="http://org.webharvest/schema/2.1/core" charset="UTF-8">
    <!-- Business Scenario: Identify bestsellers by ratings -->
    <!-- Extract highly-rated products for trend analysis -->
    
    <def var="url">https://books.toscrape.com/</def>
    
    <def var="html">
        <http url="\${url}"/>
    </def>
    
    <def var="page">
        <html-to-xml>
            <get var="html"/>
        </html-to-xml>
    </def>
    
    <!-- Find 5-star products using XQuery -->
    <def var="bestsellers">
        <xquery>
            <xq-param name="content" type="node()">
                <get var="page"/>
            </xq-param>
            <xq-expression>
                <![CDATA[
                declare variable $content as node() external;
                
                <bestsellers>
                {
                    for $product in $content//article[@class="product_pod"]
                    let $rating := $product//p[contains(@class, 'star-rating')]/@class
                    let $title := normalize-space($product//h3/a/@title)
                    let $price := normalize-space($product//p[@class="price_color"])
                    where contains($rating, 'Five') or contains($rating, 'Four')
                    return
                        <product>
                            <title>{$title}</title>
                            <price>{$price}</price>
                            <rating>{$rating}</rating>
                        </product>
                }
                </bestsellers>
                ]]>
            </xq-expression>
        </xquery>
    </def>
    
    <!-- Count bestsellers -->
    <def var="bestsellerCount">
        <xpath expression="count(//product)">
            <get var="bestsellers"/>
        </xpath>
    </def>
    
    <!-- Get first bestseller details -->
    <def var="topProduct">
        <xpath expression="(//product/title/text())[1]">
            <get var="bestsellers"/>
        </xpath>
    </def>
    
    <def var="topPrice">
        <xpath expression="(//product/price/text())[1]">
            <get var="bestsellers"/>
        </xpath>
    </def>
    
    <!-- Generate bestseller report JSON -->
    <def var="bestsellerReport">
        <template>{
  "analysis_date": "\${sys.datetime('yyyy-MM-dd HH:mm:ss')}",
  "source": "\${url}",
  "bestsellers": {
    "total_found": \${bestsellerCount},
    "top_product": {
      "title": "\${topProduct}",
      "price": "\${topPrice}"
    }
  },
  "criteria": "4-5 star ratings",
  "status": "IDENTIFIED"
}</template>
    </def>
    
    <!-- Save report -->
    <file action="write" path="bestseller_report.json">
        <get var="bestsellerReport"/>
    </file>
</config>`
    },

    'price_comparison_tool.xml': {
        title: 'Price Comparison Engine',
        icon: 'calculator',
        category: 'Price Intelligence',
        content: `<?xml version="1.0" encoding="UTF-8"?>
<config xmlns="http://org.webharvest/schema/2.1/core" charset="UTF-8">
    <!-- Business Scenario: Compare prices and find best value -->
    <!-- Calculate statistics and identify outliers -->
    
    <def var="catalogUrl">https://books.toscrape.com/</def>
    
    <def var="html">
        <http url="\${catalogUrl}"/>
    </def>
    
    <def var="page">
        <html-to-xml>
            <get var="html"/>
        </html-to-xml>
    </def>
    
    <!-- Calculate price statistics using XQuery -->
    <def var="priceStats">
        <xquery>
            <xq-param name="content" type="node()">
                <get var="page"/>
            </xq-param>
            <xq-expression>
                <![CDATA[
                declare variable $content as node() external;
                
                let $prices := 
                    for $priceText in $content//article[@class="product_pod"]//p[@class="price_color"]
                    let $price := number(substring-after(normalize-space($priceText), "£"))
                    where $price > 0
                    return $price
                
                let $count := count($prices)
                let $sum := sum($prices)
                let $avg := if ($count > 0) then round($sum div $count * 100) div 100 else 0
                let $min := min($prices)
                let $max := max($prices)
                
                return
                <statistics>
                    <count>{$count}</count>
                    <average>{$avg}</average>
                    <minimum>{$min}</minimum>
                    <maximum>{$max}</maximum>
                    <range>{$max - $min}</range>
                </statistics>
                ]]>
            </xq-expression>
        </xquery>
    </def>
    
    <!-- Extract statistics -->
    <def var="productCount">
        <xpath expression="//count/text()">
            <get var="priceStats"/>
        </xpath>
    </def>
    
    <def var="avgPrice">
        <xpath expression="//average/text()">
            <get var="priceStats"/>
        </xpath>
    </def>
    
    <def var="minPrice">
        <xpath expression="//minimum/text()">
            <get var="priceStats"/>
        </xpath>
    </def>
    
    <def var="maxPrice">
        <xpath expression="//maximum/text()">
            <get var="priceStats"/>
        </xpath>
    </def>
    
    <def var="priceRange">
        <xpath expression="//range/text()">
            <get var="priceStats"/>
        </xpath>
    </def>
    
    <!-- Generate comparison report JSON -->
    <def var="comparisonReport">
        <template>{
  "report_date": "\${sys.datetime('yyyy-MM-dd HH:mm:ss')}",
  "catalog": "\${catalogUrl}",
  "price_statistics": {
    "products_analyzed": \${productCount},
    "average_price": "£\${avgPrice}",
    "minimum_price": "£\${minPrice}",
    "maximum_price": "£\${maxPrice}",
    "price_range": "£\${priceRange}"
  },
  "business_insight": "Price spread indicates diverse catalog",
  "status": "COMPARED"
}</template>
    </def>
    
    <!-- Save comparison report -->
    <file action="write" path="price_comparison.json">
        <get var="comparisonReport"/>
    </file>
</config>`
    },

    'multi_page_scraper.xml': {
        title: 'Multi-Page Catalog Scraper',
        icon: 'layer-group',
        category: 'Advanced Scraping',
        content: `<?xml version="1.0" encoding="UTF-8"?>
<config xmlns="http://org.webharvest/schema/2.1/core" charset="UTF-8">
    <!-- Business Scenario: Scrape multiple pages of catalog -->
    <!-- Extract data from pagination and aggregate results -->
    
    <def var="baseUrl">https://books.toscrape.com/catalogue</def>
    <def var="totalPages">3</def>
    
    <def var="allProductTitles"></def>
    <def var="allPrices"></def>
    <def var="pagesScraped">0</def>
    
    <!-- Loop through pages -->
    <loop maxloops="\${totalPages}">
        <def var="pageNum">\${_index + 1}</def>
        <def var="pageUrl">\${baseUrl}/page-\${pageNum}.html</def>
        
        <def var="pageHtml">
            <http url="\${pageUrl}"/>
        </def>
        
        <def var="page">
            <html-to-xml>
                <get var="pageHtml"/>
            </html-to-xml>
        </def>
        
        <!-- Extract titles from current page -->
        <def var="pageTitles">
            <xpath expression="//article[@class='product_pod']//h3/a/@title">
                <get var="page"/>
            </xpath>
        </def>
        
        <!-- Extract prices from current page -->
        <def var="pagePrices">
            <xpath expression="//article[@class='product_pod']//p[@class='price_color']/text()">
                <get var="page"/>
            </xpath>
        </def>
        
        <!-- Aggregate (simplified - just count) -->
        <def var="pagesScraped">\${_index + 1}</def>
    </loop>
    
    <!-- Generate multi-page scraping report JSON -->
    <def var="scrapingReport">
        <template>{
  "scraping_date": "\${sys.datetime('yyyy-MM-dd HH:mm:ss')}",
  "base_url": "\${baseUrl}",
  "pages_scraped": \${pagesScraped},
  "total_pages_requested": \${totalPages},
  "method": "Multi-page pagination scraping",
  "status": "COMPLETED"
}</template>
    </def>
    
    <!-- Save scraping report -->
    <file action="write" path="multi_page_scraping.json">
        <get var="scrapingReport"/>
    </file>
</config>`
    },

    'product_data_api.xml': {
        title: 'Product Data API Formatter',
        icon: 'database',
        category: 'Data Export',
        content: `<?xml version="1.0" encoding="UTF-8"?>
<config xmlns="http://org.webharvest/schema/2.1/core" charset="UTF-8">
    <!-- Business Scenario: Extract product data and format as JSON API response -->
    <!-- Suitable for feeding into other systems or dashboards -->
    
    <def var="sourceUrl">https://books.toscrape.com/</def>
    
    <def var="html">
        <http url="\${sourceUrl}"/>
    </def>
    
    <def var="page">
        <html-to-xml>
            <get var="html"/>
        </html-to-xml>
    </def>
    
    <!-- Build structured product data with XQuery -->
    <def var="productData">
        <xquery>
            <xq-param name="content" type="node()">
                <get var="page"/>
            </xq-param>
            <xq-expression>
                <![CDATA[
                declare variable $content as node() external;
                
                <api_response>
                    <metadata>
                        <timestamp>{current-dateTime()}</timestamp>
                        <source>books.toscrape.com</source>
                        <version>1.0</version>
                    </metadata>
                    <products>
                    {
                        for $product at $pos in $content//article[@class="product_pod"]
                        let $title := normalize-space($product//h3/a/@title)
                        let $price := normalize-space($product//p[@class="price_color"])
                        let $rating := normalize-space($product//p[contains(@class, 'star-rating')]/@class)
                        let $available := if ($product//p[contains(@class, 'availability') and contains(., 'In stock')]) then 'true' else 'false'
                        where $pos <= 10
                        return
                            <product>
                                <id>{$pos}</id>
                                <title>{$title}</title>
                                <price>{$price}</price>
                                <rating>{replace($rating, 'star-rating ', '')}</rating>
                                <in_stock>{$available}</in_stock>
                            </product>
                    }
                    </products>
                </api_response>
                ]]>
            </xq-expression>
        </xquery>
    </def>
    
    <!-- Extract summary stats -->
    <def var="productsExtracted">
        <xpath expression="count(//products/product)">
            <get var="productData"/>
        </xpath>
    </def>
    
    <!-- Get first product as example -->
    <def var="firstProductTitle">
        <xpath expression="(//product/title/text())[1]">
            <get var="productData"/>
        </xpath>
    </def>
    
    <def var="firstProductPrice">
        <xpath expression="(//product/price/text())[1]">
            <get var="productData"/>
        </xpath>
    </def>
    
    <!-- Generate API-formatted JSON -->
    <def var="apiResponse">
        <template>{
  "api_version": "1.0",
  "timestamp": "\${sys.datetime('yyyy-MM-dd HH:mm:ss')}",
  "source_url": "\${sourceUrl}",
  "data": {
    "products_extracted": \${productsExtracted},
    "sample_product": {
      "title": "\${firstProductTitle}",
      "price": "\${firstProductPrice}"
    }
  },
  "status": "SUCCESS"
}</template>
    </def>
    
    <!-- Save API response -->
    <file action="write" path="product_api.json">
        <get var="apiResponse"/>
    </file>
    
    <!-- Also save full XML data -->
    <file action="write" path="product_data.xml">
        <get var="productData"/>
    </file>
</config>`
    },

    'quotes_scraping.xml': {
        title: 'Quotes Content Scraper',
        icon: 'quote-left',
        category: 'Content Extraction',
        content: `<?xml version="1.0" encoding="UTF-8"?>
<config xmlns="http://org.webharvest/schema/2.1/core" charset="UTF-8">
    <!-- Business Scenario: Extract quotes and authors for content database -->
    <!-- Scrape structured content from quotes.toscrape.com -->
    
    <def var="url">https://quotes.toscrape.com/</def>
    
    <def var="html">
        <http url="\${url}"/>
    </def>
    
    <def var="page">
        <html-to-xml>
            <get var="html"/>
        </html-to-xml>
    </def>
    
    <!-- Extract all quotes (use /text() to get text content only) -->
    <def var="quotes">
        <xpath expression="//div[@class='quote']//span[@class='text']/text()">
            <get var="page"/>
        </xpath>
    </def>
    
    <!-- Extract all authors (use /text() to get text content only) -->
    <def var="authors">
        <xpath expression="//div[@class='quote']//small[@class='author']/text()">
            <get var="page"/>
        </xpath>
    </def>
    
    <!-- Count quotes -->
    <def var="quoteCount">
        <xpath expression="count(//div[@class='quote'])">
            <get var="page"/>
        </xpath>
    </def>
    
    <!-- Generate quotes JSON -->
    <def var="quotesJson">
        <template>{
  "extraction_date": "\${sys.datetime('yyyy-MM-dd HH:mm:ss')}",
  "source_url": "\${url}",
  "content": {
    "total_quotes": \${quoteCount},
    "quotes_preview": "First quote extracted successfully",
    "authors_extracted": true
  },
  "status": "EXTRACTED"
}</template>
    </def>
    
    <!-- Save quotes data -->
    <file action="write" path="quotes_data.json">
        <get var="quotesJson"/>
    </file>
    
    <!-- Save quotes text -->
    <file action="write" path="quotes.txt" charset="UTF-8">
        <get var="quotes"/>
    </file>
</config>`
    },

    'xquery_examples.xml': {
        title: 'XQuery Processing',
        icon: 'code',
        category: 'XML Processing',
        content: `<?xml version="1.0" encoding="UTF-8"?>
<config xmlns="http://org.webharvest/schema/2.1/core" charset="UTF-8">
    <!-- Business Scenario: Complex data transformation with XQuery -->
    <!-- Demonstrate XQuery capabilities for business logic -->
    
    <def var="xqueryResult">
        <xquery>
            <xq-param name="logicvalue" type="boolean">true</xq-param>
            <xq-param name="revenue" type="double">150000</xq-param>
            <xq-param name="quarters" type="integer*">
                35000
                42000
                38000
                35000
            </xq-param>
            <xq-param name="companyName" type="string">TechCorp</xq-param>
            <xq-param name="salesData">
                <![CDATA[
               <sales>
                    <region name="North" revenue="50000"/>
                    <region name="South" revenue="45000"/>
                    <region name="East" revenue="35000"/>
                    <region name="West" revenue="20000"/>
               </sales>
            ]]>
            </xq-param>
            <xq-expression>
                <![CDATA[
                declare namespace math ="java:java.lang.Math";
                declare namespace fn ="f:ns";

                declare variable $logicvalue as xs:boolean external;
                declare variable $revenue as xs:double external;
                declare variable $quarters as xs:integer* external;
                declare variable $companyName as xs:string external;
                declare variable $salesData as node()* external;

                declare function fn:calculateGrowth($values as xs:integer*) as node()* {
                    for $val at $pos in $values
                    return
                        <quarter>
                            <period>{$pos}</period>
                            <revenue>{$val}</revenue>
                            <growth_index>{math:sqrt($val)}</growth_index>
                        </quarter>
                };

                <business_report>
                    <company>{$companyName}</company>
                    <annual_revenue>{$revenue}</annual_revenue>
                    <target_met>{if ($logicvalue and $revenue > 100000) then 'YES' else 'NO'}</target_met>
                    <quarterly_performance>
                        { fn:calculateGrowth($quarters) }
                    </quarterly_performance>
                    <regional_breakdown>
                        { for $region in $salesData//region
                          let $rev := number($region/@revenue)
                          let $share := round($rev div $revenue * 100)
                          return
                            <region>
                                <name>{string($region/@name)}</name>
                                <revenue>{$rev}</revenue>
                                <market_share>{$share}%</market_share>
                            </region>
                        }
                    </regional_breakdown>
                </business_report>
                ]]>
            </xq-expression>
        </xquery>
    </def>
    
    <!-- Extract key metrics -->
    <def var="targetMet">
        <xpath expression="//target_met/text()">
            <get var="xqueryResult"/>
        </xpath>
    </def>
    
    <def var="quarterCount">
        <xpath expression="count(//quarter)">
            <get var="xqueryResult"/>
        </xpath>
    </def>
    
    <def var="regionCount">
        <xpath expression="count(//regional_breakdown/region)">
            <get var="xqueryResult"/>
        </xpath>
    </def>
    
    <!-- Generate XQuery results JSON -->
    <def var="xqueryJson">
        <template>{
  "processing_date": "\${sys.datetime('yyyy-MM-dd HH:mm:ss')}",
  "analysis": {
    "target_met": "\${targetMet}",
    "quarters_analyzed": \${quarterCount},
    "regions_analyzed": \${regionCount}
  },
  "capabilities_demonstrated": ["XQuery", "Java functions", "Math operations", "Complex transformations"],
  "status": "PROCESSED"
}</template>
    </def>
    
    <!-- Save results -->
    <file action="write" path="xquery_results.json">
        <get var="xqueryJson"/>
    </file>
    
    <!-- Save full XML -->
    <file action="write" path="xquery_full.xml">
        <get var="xqueryResult"/>
    </file>
</config>`
    }
};

const app = {
    editor: null,
    tabs: [],
    activeTabId: null,
    currentExecutionId: null,
    webSocket: null,
