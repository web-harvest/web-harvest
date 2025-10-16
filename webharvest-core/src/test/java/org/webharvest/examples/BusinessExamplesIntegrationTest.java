package org.webharvest.examples;

import org.testng.annotations.Test;
import org.webharvest.runtime.DynamicScopeContext;
import org.webharvest.runtime.variables.Variable;

import static org.testng.Assert.*;

/**
 * Comprehensive integration tests for business scenario examples.
 * These tests verify that all embedded IDE examples execute correctly,
 * set proper variables, and return expected business results.
 * 
 * Each test represents a real business use case and validates:
 * - Configuration parsing
 * - HTTP requests
 * - HTML/XML processing  
 * - XPath/XQuery execution
 * - Variable assignment
 * - Expected output values
 * 
 * Uses v2.2 Scraper API (ScraperTestBase) instead of deprecated getHarvester().
 * These same tests are reused for CLI verification.
 */
public class BusinessExamplesIntegrationTest extends ScraperTestBase {

    /**
     * Test 1: Price Monitoring System
     * Business: Monitor product prices for competitive intelligence
     * Variables: url, html, page, titles, prices, availability, productCount, jsonOutput
     */
    @Test(groups = "network", description = "Price monitoring extracts prices and availability")
    public void testPriceMonitoringSystem() throws Exception {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<config xmlns=\"http://org.webharvest/schema/2.1/core\" charset=\"UTF-8\">\n" +
            "    <def var=\"url\">https://books.toscrape.com/</def>\n" +
            "    \n" +
            "    <def var=\"html\">\n" +
            "        <http url=\"${url}\"/>\n" +
            "    </def>\n" +
            "    \n" +
            "    <def var=\"page\">\n" +
            "        <html-to-xml>\n" +
            "            <get var=\"html\"/>\n" +
            "        </html-to-xml>\n" +
            "    </def>\n" +
            "    \n" +
            "    <def var=\"titles\">\n" +
            "        <xpath expression=\"//article[@class='product_pod']//h3/a/@title\">\n" +
            "            <get var=\"page\"/>\n" +
            "        </xpath>\n" +
            "    </def>\n" +
            "    \n" +
            "    <def var=\"prices\">\n" +
            "        <xpath expression=\"//article[@class='product_pod']//p[@class='price_color']/text()\">\n" +
            "            <get var=\"page\"/>\n" +
            "        </xpath>\n" +
            "    </def>\n" +
            "    \n" +
            "    <def var=\"productCount\">\n" +
            "        <xpath expression=\"count(//article[@class='product_pod'])\">\n" +
            "            <get var=\"page\"/>\n" +
            "        </xpath>\n" +
            "    </def>\n" +
            "</config>";
        
        DynamicScopeContext context = executeXmlString(xml);
        
        // Verify URL variable
        Variable url = context.getVar("url");
        assertNotNull(url, "url variable must be set");
        assertEquals(url.toString(), "https://books.toscrape.com/");
        
        // Verify HTML was fetched
        Variable html = context.getVar("html");
        assertNotNull(html, "html variable must be set");
        assertTrue(html.toString().length() > 1000, 
                  "HTML should be substantial, got: " + html.toString().length() + " chars");
        assertTrue(html.toString().contains("Books to Scrape"), 
                  "HTML should contain site title");
        
        // Verify page was converted to XML
        Variable page = context.getVar("page");
        assertNotNull(page, "page variable must be set");
        assertTrue(page.toString().contains("<article"), 
                  "Page should contain article elements");
        
        // Verify titles were extracted
        Variable titles = context.getVar("titles");
        assertNotNull(titles, "titles variable must be set");
        assertTrue(titles.toString().length() > 0, 
                  "Titles should not be empty");
        
        // Verify prices were extracted  
        Variable prices = context.getVar("prices");
        assertNotNull(prices, "prices variable must be set");
        assertTrue(prices.toString().contains("£"), 
                  "Prices should contain currency symbol");
        
        // Verify product count
        Variable productCount = context.getVar("productCount");
        assertNotNull(productCount, "productCount variable must be set");
        int count = Integer.parseInt(productCount.toString().trim());
        assertTrue(count >= 10 && count <= 30, 
                  "Product count should be realistic (10-30), got: " + count);
    }

    /**
     * Test 2: Multi-Level Catalog Crawler
     * Business: Crawl category pages and extract product links
     * Variables: baseUrl, categoryUrl, categoryHtml, categoryPage, productLinks, productsFound, sampleTitle, samplePrice
     */
    @Test(groups = "network", description = "Catalog crawler extracts products from category")
    public void testCatalogCrawler() throws Exception {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<config xmlns=\"http://org.webharvest/schema/2.1/core\" charset=\"UTF-8\">\n" +
            "    <def var=\"baseUrl\">https://books.toscrape.com</def>\n" +
            "    <def var=\"categoryUrl\">${baseUrl}/catalogue/category/books_1/index.html</def>\n" +
            "    \n" +
            "    <def var=\"categoryHtml\">\n" +
            "        <http url=\"${categoryUrl}\"/>\n" +
            "    </def>\n" +
            "    \n" +
            "    <def var=\"categoryPage\">\n" +
            "        <html-to-xml>\n" +
            "            <get var=\"categoryHtml\"/>\n" +
            "        </html-to-xml>\n" +
            "    </def>\n" +
            "    \n" +
            "    <def var=\"productsFound\">\n" +
            "        <xpath expression=\"count(//article[@class='product_pod'])\">\n" +
            "            <get var=\"categoryPage\"/>\n" +
            "        </xpath>\n" +
            "    </def>\n" +
            "    \n" +
            "    <def var=\"sampleTitle\">\n" +
            "        <xpath expression=\"(//article[@class='product_pod']//h3/a/@title)[1]\">\n" +
            "            <get var=\"categoryPage\"/>\n" +
            "        </xpath>\n" +
            "    </def>\n" +
            "    \n" +
            "    <def var=\"samplePrice\">\n" +
            "        <xpath expression=\"(//article[@class='product_pod']//p[@class='price_color']/text())[1]\">\n" +
            "            <get var=\"categoryPage\"/>\n" +
            "        </xpath>\n" +
            "    </def>\n" +
            "</config>";
        
        DynamicScopeContext context = executeXmlString(xml);
        
        // Verify all variables are set
        Variable baseUrl = context.getVar("baseUrl");
        assertNotNull(baseUrl, "baseUrl must be set");
        assertEquals(baseUrl.toString(), "https://books.toscrape.com");
        
        Variable categoryUrl = context.getVar("categoryUrl");
        assertNotNull(categoryUrl, "categoryUrl must be set");
        assertTrue(categoryUrl.toString().contains("/category/books_1/"), 
                  "Category URL should contain category path");
        
        Variable categoryHtml = context.getVar("categoryHtml");
        assertNotNull(categoryHtml, "categoryHtml must be fetched");
        assertTrue(categoryHtml.toString().length() > 1000);
        
        Variable productsFound = context.getVar("productsFound");
        assertNotNull(productsFound, "productsFound must be set");
        int count = Integer.parseInt(productsFound.toString().trim());
        assertTrue(count >= 10, "Should find at least 10 products, got: " + count);
        
        Variable sampleTitle = context.getVar("sampleTitle");
        assertNotNull(sampleTitle, "sampleTitle must be set");
        assertTrue(sampleTitle.toString().length() > 3, 
                  "Sample title should be meaningful");
        
        Variable samplePrice = context.getVar("samplePrice");
        assertNotNull(samplePrice, "samplePrice must be set");
        assertTrue(samplePrice.toString().contains("£"), 
                  "Sample price should contain currency");
    }

    /**
     * Test 3: Product Details Extraction
     * Business: Extract detailed product information for pricing database
     * Variables: url, html, page, productTitle, productPrice, productAvailability, productRating
     */
    @Test(groups = "network", description = "Product details extracts title, price, availability, rating")
    public void testProductDetailsExtraction() throws Exception {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<config xmlns=\"http://org.webharvest/schema/2.1/core\" charset=\"UTF-8\">\n" +
            "    <def var=\"url\">https://books.toscrape.com/catalogue/a-light-in-the-attic_1000/index.html</def>\n" +
            "    \n" +
            "    <def var=\"html\">\n" +
            "        <http url=\"${url}\"/>\n" +
            "    </def>\n" +
            "    \n" +
            "    <def var=\"page\">\n" +
            "        <html-to-xml>\n" +
            "            <get var=\"html\"/>\n" +
            "        </html-to-xml>\n" +
            "    </def>\n" +
            "    \n" +
            "    <def var=\"productTitle\">\n" +
            "        <xpath expression=\"//div[@class='col-sm-6 product_main']//h1/text()\">\n" +
            "            <get var=\"page\"/>\n" +
            "        </xpath>\n" +
            "    </def>\n" +
            "    \n" +
            "    <def var=\"productPrice\">\n" +
            "        <xpath expression=\"//p[@class='price_color']/text()\">\n" +
            "            <get var=\"page\"/>\n" +
            "        </xpath>\n" +
            "    </def>\n" +
            "    \n" +
            "    <def var=\"productAvailability\">\n" +
            "        <xpath expression=\"//p[contains(@class, 'availability')]/text()\">\n" +
            "            <get var=\"page\"/>\n" +
            "        </xpath>\n" +
            "    </def>\n" +
            "    \n" +
            "    <def var=\"productRating\">\n" +
            "        <xpath expression=\"//p[contains(@class, 'star-rating')]/@class\">\n" +
            "            <get var=\"page\"/>\n" +
            "        </xpath>\n" +
            "    </def>\n" +
            "</config>";
        
        DynamicScopeContext context = executeXmlString(xml);
        
        // Verify product title
        Variable productTitle = context.getVar("productTitle");
        assertNotNull(productTitle, "productTitle must be extracted");
        assertTrue(productTitle.toString().contains("Light in the Attic"), 
                  "Product title should match book name");
        
        // Verify product price
        Variable productPrice = context.getVar("productPrice");
        assertNotNull(productPrice, "productPrice must be extracted");
        assertTrue(productPrice.toString().contains("£"), 
                  "Price should have currency symbol");
        String priceText = productPrice.toString().trim();
        assertTrue(priceText.matches("£\\d+\\.\\d{2}"), 
                  "Price should be formatted as £XX.XX, got: " + priceText);
        
        // Verify availability
        Variable productAvailability = context.getVar("productAvailability");
        assertNotNull(productAvailability, "productAvailability must be extracted");
        String availText = productAvailability.toString().trim();
        assertTrue(availText.contains("In stock") || availText.contains("available"), 
                  "Availability should indicate stock status, got: " + availText);
        
        // Verify rating
        Variable productRating = context.getVar("productRating");
        assertNotNull(productRating, "productRating must be extracted");
        assertTrue(productRating.toString().contains("star-rating"), 
                  "Rating should contain star-rating class");
    }

    /**
     * Test 4: Category Analysis Tool
     * Business: Analyze product distribution and pricing across catalog
     * Variables: baseUrl, html, page, categories, totalProducts, prices, sampleProducts
     */
    @Test(groups = "network", description = "Category analyzer extracts product statistics")
    public void testCategoryAnalyzer() throws Exception {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<config xmlns=\"http://org.webharvest/schema/2.1/core\" charset=\"UTF-8\">\n" +
            "    <def var=\"baseUrl\">https://books.toscrape.com</def>\n" +
            "    \n" +
            "    <def var=\"html\">\n" +
            "        <http url=\"${baseUrl}\"/>\n" +
            "    </def>\n" +
            "    \n" +
            "    <def var=\"page\">\n" +
            "        <html-to-xml>\n" +
            "            <get var=\"html\"/>\n" +
            "        </html-to-xml>\n" +
            "    </def>\n" +
            "    \n" +
            "    <def var=\"totalProducts\">\n" +
            "        <xpath expression=\"count(//article[@class='product_pod'])\">\n" +
            "            <get var=\"page\"/>\n" +
            "        </xpath>\n" +
            "    </def>\n" +
            "    \n" +
            "    <def var=\"prices\">\n" +
            "        <xpath expression=\"//p[@class='price_color']/text()\">\n" +
            "            <get var=\"page\"/>\n" +
            "        </xpath>\n" +
            "    </def>\n" +
            "    \n" +
            "    <def var=\"sampleProducts\">\n" +
            "        <xpath expression=\"//article[@class='product_pod'][position() &lt;= 5]//h3/a/@title\">\n" +
            "            <get var=\"page\"/>\n" +
            "        </xpath>\n" +
            "    </def>\n" +
            "</config>";
        
        DynamicScopeContext context = executeXmlString(xml);
        
        // Verify baseUrl
        Variable baseUrl = context.getVar("baseUrl");
        assertNotNull(baseUrl, "baseUrl must be set");
        
        // Verify HTML fetched
        Variable html = context.getVar("html");
        assertNotNull(html, "html must be fetched");
        assertTrue(html.toString().contains("Books to Scrape"));
        
        // Verify total products counted
        Variable totalProducts = context.getVar("totalProducts");
        assertNotNull(totalProducts, "totalProducts must be counted");
        int count = Integer.parseInt(totalProducts.toString().trim());
        assertTrue(count >= 10 && count <= 30, 
                  "Expected 10-30 products, got: " + count);
        
        // Verify prices extracted
        Variable prices = context.getVar("prices");
        assertNotNull(prices, "prices must be extracted");
        assertTrue(prices.toString().contains("£"));
        
        // Verify sample products extracted
        Variable sampleProducts = context.getVar("sampleProducts");
        assertNotNull(sampleProducts, "sampleProducts must be extracted");
        assertTrue(sampleProducts.toString().length() > 10, 
                  "Sample products should contain titles");
    }

    /**
     * Test 5: Competitive Price Tracker
     * Business: Find cheapest products for competitive pricing strategy
     * Variables: storeUrl, html, page, priceAnalysis, cheapestTitle, cheapestPrice, productsAnalyzed
     */
    @Test(groups = "network", description = "Price tracker finds cheapest products using XQuery")
    public void testCompetitivePriceTracker() throws Exception {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<config xmlns=\"http://org.webharvest/schema/2.1/core\" charset=\"UTF-8\">\n" +
            "    <def var=\"storeUrl\">https://books.toscrape.com/</def>\n" +
            "    \n" +
            "    <def var=\"html\">\n" +
            "        <http url=\"${storeUrl}\"/>\n" +
            "    </def>\n" +
            "    \n" +
            "    <def var=\"page\">\n" +
            "        <html-to-xml>\n" +
            "            <get var=\"html\"/>\n" +
            "        </html-to-xml>\n" +
            "    </def>\n" +
            "    \n" +
            "    <def var=\"priceAnalysis\">\n" +
            "        <xquery>\n" +
            "            <xq-param name=\"content\" type=\"node()\">\n" +
            "                <get var=\"page\"/>\n" +
            "            </xq-param>\n" +
            "            <xq-expression>\n" +
            "                <![CDATA[\n" +
            "                declare variable $content as node() external;\n" +
            "                \n" +
            "                <price_analysis>\n" +
            "                {\n" +
            "                    for $product in $content//article[@class=\"product_pod\"]\n" +
            "                    let $title := normalize-space($product//h3/a/@title)\n" +
            "                    let $priceText := normalize-space($product//p[@class=\"price_color\"])\n" +
            "                    let $price := number(substring-after($priceText, \"£\"))\n" +
            "                    where $price > 0\n" +
            "                    order by $price ascending\n" +
            "                    return\n" +
            "                        <product>\n" +
            "                            <title>{$title}</title>\n" +
            "                            <price>{$price}</price>\n" +
            "                            <price_formatted>{$priceText}</price_formatted>\n" +
            "                        </product>\n" +
            "                }\n" +
            "                </price_analysis>\n" +
            "                ]]>\n" +
            "            </xq-expression>\n" +
            "        </xquery>\n" +
            "    </def>\n" +
            "    \n" +
            "    <def var=\"cheapestTitle\">\n" +
            "        <xpath expression=\"(//product/title/text())[1]\">\n" +
            "            <get var=\"priceAnalysis\"/>\n" +
            "        </xpath>\n" +
            "    </def>\n" +
            "    \n" +
            "    <def var=\"cheapestPrice\">\n" +
            "        <xpath expression=\"(//product/price_formatted/text())[1]\">\n" +
            "            <get var=\"priceAnalysis\"/>\n" +
            "        </xpath>\n" +
            "    </def>\n" +
            "    \n" +
            "    <def var=\"productsAnalyzed\">\n" +
            "        <xpath expression=\"count(//product)\">\n" +
            "            <get var=\"priceAnalysis\"/>\n" +
            "        </xpath>\n" +
            "    </def>\n" +
            "</config>";
        
        DynamicScopeContext context = executeXmlString(xml);
        
        // Verify XQuery result
        Variable priceAnalysis = context.getVar("priceAnalysis");
        assertNotNull(priceAnalysis, "priceAnalysis must be generated");
        assertTrue(priceAnalysis.toString().contains("<product>"), 
                  "Price analysis should contain product elements");
        
        // Verify cheapest product found
        Variable cheapestTitle = context.getVar("cheapestTitle");
        assertNotNull(cheapestTitle, "cheapestTitle must be extracted");
        assertTrue(cheapestTitle.toString().length() > 3, 
                  "Cheapest title should be meaningful");
        
        Variable cheapestPrice = context.getVar("cheapestPrice");
        assertNotNull(cheapestPrice, "cheapestPrice must be extracted");
        assertTrue(cheapestPrice.toString().contains("£"), 
                  "Cheapest price should have currency");
        
        // Verify products were analyzed
        Variable productsAnalyzed = context.getVar("productsAnalyzed");
        assertNotNull(productsAnalyzed, "productsAnalyzed must be counted");
        int analyzed = Integer.parseInt(productsAnalyzed.toString().trim());
        assertTrue(analyzed >= 10, 
                  "Should analyze at least 10 products, got: " + analyzed);
    }

    /**
     * Test 6: Inventory Availability Tracker
     * Business: Monitor stock levels for inventory management
     * Variables: catalogUrl, html, page, inventoryStatus, totalItems, inStockItems, availabilityRate
     */
    @Test(groups = "network", description = "Inventory tracker monitors stock availability")
    public void testInventoryTracker() throws Exception {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<config xmlns=\"http://org.webharvest/schema/2.1/core\" charset=\"UTF-8\">\n" +
            "    <def var=\"catalogUrl\">https://books.toscrape.com/</def>\n" +
            "    \n" +
            "    <def var=\"html\">\n" +
            "        <http url=\"${catalogUrl}\"/>\n" +
            "    </def>\n" +
            "    \n" +
            "    <def var=\"page\">\n" +
            "        <html-to-xml>\n" +
            "            <get var=\"html\"/>\n" +
            "        </html-to-xml>\n" +
            "    </def>\n" +
            "    \n" +
            "    <def var=\"inventoryStatus\">\n" +
            "        <xquery>\n" +
            "            <xq-param name=\"content\" type=\"node()\">\n" +
            "                <get var=\"page\"/>\n" +
            "            </xq-param>\n" +
            "            <xq-expression>\n" +
            "                <![CDATA[\n" +
            "                declare variable $content as node() external;\n" +
            "                \n" +
            "                let $allProducts := $content//article[@class=\"product_pod\"]\n" +
            "                let $inStock := $allProducts//p[contains(@class, 'availability') and contains(., 'In stock')]\n" +
            "                let $total := count($allProducts)\n" +
            "                let $available := count($inStock)\n" +
            "                let $percentage := if ($total > 0) then round($available div $total * 100) else 0\n" +
            "                \n" +
            "                return\n" +
            "                <inventory>\n" +
            "                    <total_items>{$total}</total_items>\n" +
            "                    <in_stock>{$available}</in_stock>\n" +
            "                    <availability_rate>{$percentage}</availability_rate>\n" +
            "                </inventory>\n" +
            "                ]]>\n" +
            "            </xq-expression>\n" +
            "        </xquery>\n" +
            "    </def>\n" +
            "    \n" +
            "    <def var=\"totalItems\">\n" +
            "        <xpath expression=\"//total_items/text()\">\n" +
            "            <get var=\"inventoryStatus\"/>\n" +
            "        </xpath>\n" +
            "    </def>\n" +
            "    \n" +
            "    <def var=\"inStockItems\">\n" +
            "        <xpath expression=\"//in_stock/text()\">\n" +
            "            <get var=\"inventoryStatus\"/>\n" +
            "        </xpath>\n" +
            "    </def>\n" +
            "    \n" +
            "    <def var=\"availabilityRate\">\n" +
            "        <xpath expression=\"//availability_rate/text()\">\n" +
            "            <get var=\"inventoryStatus\"/>\n" +
            "        </xpath>\n" +
            "    </def>\n" +
            "</config>";
        
        DynamicScopeContext context = executeXmlString(xml);
        
        // Verify inventory analysis was performed
        Variable inventoryStatus = context.getVar("inventoryStatus");
        assertNotNull(inventoryStatus, "inventoryStatus must be calculated");
        assertTrue(inventoryStatus.toString().contains("<inventory>"));
        
        // Verify total items counted
        Variable totalItems = context.getVar("totalItems");
        assertNotNull(totalItems, "totalItems must be extracted");
        int total = Integer.parseInt(totalItems.toString().trim());
        assertTrue(total >= 10, "Should have at least 10 items, got: " + total);
        
        // Verify in-stock items counted
        Variable inStockItems = context.getVar("inStockItems");
        assertNotNull(inStockItems, "inStockItems must be extracted");
        int inStock = Integer.parseInt(inStockItems.toString().trim());
        assertTrue(inStock > 0, "Should have in-stock items, got: " + inStock);
        assertTrue(inStock <= total, "In-stock should not exceed total");
        
        // Verify availability rate calculated
        Variable availabilityRate = context.getVar("availabilityRate");
        assertNotNull(availabilityRate, "availabilityRate must be calculated");
        int rate = Integer.parseInt(availabilityRate.toString().trim());
        assertTrue(rate >= 0 && rate <= 100, 
                  "Availability rate should be 0-100%, got: " + rate);
    }

    /**
     * Test 5: Bestseller Identification
     * Business: Identify high-rated products for marketing campaigns
     * Variables: url, html, page, bestsellers, bestsellerCount, topProduct, topPrice
     */
    @Test(groups = "network", description = "Bestseller finder identifies highly-rated products")
    public void testBestsellerFinder() throws Exception {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<config xmlns=\"http://org.webharvest/schema/2.1/core\" charset=\"UTF-8\">\n" +
            "    <def var=\"url\">https://books.toscrape.com/</def>\n" +
            "    \n" +
            "    <def var=\"html\">\n" +
            "        <http url=\"${url}\"/>\n" +
            "    </def>\n" +
            "    \n" +
            "    <def var=\"page\">\n" +
            "        <html-to-xml>\n" +
            "            <get var=\"html\"/>\n" +
            "        </html-to-xml>\n" +
            "    </def>\n" +
            "    \n" +
            "    <def var=\"bestsellers\">\n" +
            "        <xquery>\n" +
            "            <xq-param name=\"content\" type=\"node()\">\n" +
            "                <get var=\"page\"/>\n" +
            "            </xq-param>\n" +
            "            <xq-expression>\n" +
            "                <![CDATA[\n" +
            "                declare variable $content as node() external;\n" +
            "                \n" +
            "                <bestsellers>\n" +
            "                {\n" +
            "                    for $product in $content//article[@class=\"product_pod\"]\n" +
            "                    let $rating := $product//p[contains(@class, 'star-rating')]/@class\n" +
            "                    let $title := normalize-space($product//h3/a/@title)\n" +
            "                    let $price := normalize-space($product//p[@class=\"price_color\"])\n" +
            "                    where contains($rating, 'Five') or contains($rating, 'Four')\n" +
            "                    return\n" +
            "                        <product>\n" +
            "                            <title>{$title}</title>\n" +
            "                            <price>{$price}</price>\n" +
            "                            <rating>{$rating}</rating>\n" +
            "                        </product>\n" +
            "                }\n" +
            "                </bestsellers>\n" +
            "                ]]>\n" +
            "            </xq-expression>\n" +
            "        </xquery>\n" +
            "    </def>\n" +
            "    \n" +
            "    <def var=\"bestsellerCount\">\n" +
            "        <xpath expression=\"count(//product)\">\n" +
            "            <get var=\"bestsellers\"/>\n" +
            "        </xpath>\n" +
            "    </def>\n" +
            "    \n" +
            "    <def var=\"topProduct\">\n" +
            "        <xpath expression=\"(//product/title/text())[1]\">\n" +
            "            <get var=\"bestsellers\"/>\n" +
            "        </xpath>\n" +
            "    </def>\n" +
            "    \n" +
            "    <def var=\"topPrice\">\n" +
            "        <xpath expression=\"(//product/price/text())[1]\">\n" +
            "            <get var=\"bestsellers\"/>\n" +
            "        </xpath>\n" +
            "    </def>\n" +
            "</config>";
        
        DynamicScopeContext context = executeXmlString(xml);
        
        // Verify bestsellers were found
        Variable bestsellers = context.getVar("bestsellers");
        assertNotNull(bestsellers, "bestsellers must be generated");
        assertTrue(bestsellers.toString().contains("<product>"), 
                  "Should find bestseller products");
        
        // Verify count
        Variable bestsellerCount = context.getVar("bestsellerCount");
        assertNotNull(bestsellerCount, "bestsellerCount must be set");
        int count = Integer.parseInt(bestsellerCount.toString().trim());
        assertTrue(count > 0, "Should find some bestsellers, got: " + count);
        
        // Verify top product extracted
        Variable topProduct = context.getVar("topProduct");
        assertNotNull(topProduct, "topProduct must be extracted");
        assertTrue(topProduct.toString().length() > 3);
        
        Variable topPrice = context.getVar("topPrice");
        assertNotNull(topPrice, "topPrice must be extracted");
        assertTrue(topPrice.toString().contains("£"));
    }

    /**
     * Test 6: Price Comparison Engine
     * Business: Calculate price statistics for business intelligence
     * Variables: catalogUrl, html, page, priceStats, productCount, avgPrice, minPrice, maxPrice, priceRange
     */
    @Test(groups = "network", description = "Price comparison calculates min/max/avg statistics")
    public void testPriceComparisonEngine() throws Exception {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<config xmlns=\"http://org.webharvest/schema/2.1/core\" charset=\"UTF-8\">\n" +
            "    <def var=\"catalogUrl\">https://books.toscrape.com/</def>\n" +
            "    \n" +
            "    <def var=\"html\">\n" +
            "        <http url=\"${catalogUrl}\"/>\n" +
            "    </def>\n" +
            "    \n" +
            "    <def var=\"page\">\n" +
            "        <html-to-xml>\n" +
            "            <get var=\"html\"/>\n" +
            "        </html-to-xml>\n" +
            "    </def>\n" +
            "    \n" +
            "    <def var=\"priceStats\">\n" +
            "        <xquery>\n" +
            "            <xq-param name=\"content\" type=\"node()\">\n" +
            "                <get var=\"page\"/>\n" +
            "            </xq-param>\n" +
            "            <xq-expression>\n" +
            "                <![CDATA[\n" +
            "                declare variable $content as node() external;\n" +
            "                \n" +
            "                let $prices := \n" +
            "                    for $priceText in $content//article[@class=\"product_pod\"]//p[@class=\"price_color\"]\n" +
            "                    let $price := number(substring-after(normalize-space($priceText), \"£\"))\n" +
            "                    where $price > 0\n" +
            "                    return $price\n" +
            "                \n" +
            "                let $count := count($prices)\n" +
            "                let $sum := sum($prices)\n" +
            "                let $avg := if ($count > 0) then round($sum div $count * 100) div 100 else 0\n" +
            "                let $min := min($prices)\n" +
            "                let $max := max($prices)\n" +
            "                \n" +
            "                return\n" +
            "                <statistics>\n" +
            "                    <count>{$count}</count>\n" +
            "                    <average>{$avg}</average>\n" +
            "                    <minimum>{$min}</minimum>\n" +
            "                    <maximum>{$max}</maximum>\n" +
            "                    <range>{$max - $min}</range>\n" +
            "                </statistics>\n" +
            "                ]]>\n" +
            "            </xq-expression>\n" +
            "        </xquery>\n" +
            "    </def>\n" +
            "    \n" +
            "    <def var=\"productCount\">\n" +
            "        <xpath expression=\"//count/text()\">\n" +
            "            <get var=\"priceStats\"/>\n" +
            "        </xpath>\n" +
            "    </def>\n" +
            "    \n" +
            "    <def var=\"avgPrice\">\n" +
            "        <xpath expression=\"//average/text()\">\n" +
            "            <get var=\"priceStats\"/>\n" +
            "        </xpath>\n" +
            "    </def>\n" +
            "    \n" +
            "    <def var=\"minPrice\">\n" +
            "        <xpath expression=\"//minimum/text()\">\n" +
            "            <get var=\"priceStats\"/>\n" +
            "        </xpath>\n" +
            "    </def>\n" +
            "    \n" +
            "    <def var=\"maxPrice\">\n" +
            "        <xpath expression=\"//maximum/text()\">\n" +
            "            <get var=\"priceStats\"/>\n" +
            "        </xpath>\n" +
            "    </def>\n" +
            "    \n" +
            "    <def var=\"priceRange\">\n" +
            "        <xpath expression=\"//range/text()\">\n" +
            "            <get var=\"priceStats\"/>\n" +
            "        </xpath>\n" +
            "    </def>\n" +
            "</config>";
        
        DynamicScopeContext context = executeXmlString(xml);
        
        // Verify statistics were calculated
        Variable priceStats = context.getVar("priceStats");
        assertNotNull(priceStats, "priceStats must be calculated");
        assertTrue(priceStats.toString().contains("<statistics>"));
        
        // Verify all metrics extracted
        Variable productCount = context.getVar("productCount");
        assertNotNull(productCount, "productCount must be extracted");
        int count = Integer.parseInt(productCount.toString().trim());
        assertTrue(count >= 10);
        
        Variable avgPrice = context.getVar("avgPrice");
        assertNotNull(avgPrice, "avgPrice must be calculated");
        double avg = Double.parseDouble(avgPrice.toString().trim());
        assertTrue(avg > 0 && avg < 100, 
                  "Average price should be reasonable, got: £" + avg);
        
        Variable minPrice = context.getVar("minPrice");
        assertNotNull(minPrice, "minPrice must be found");
        double min = Double.parseDouble(minPrice.toString().trim());
        assertTrue(min > 0);
        
        Variable maxPrice = context.getVar("maxPrice");
        assertNotNull(maxPrice, "maxPrice must be found");
        double max = Double.parseDouble(maxPrice.toString().trim());
        assertTrue(max > min, "Max should be greater than min");
        
        Variable priceRange = context.getVar("priceRange");
        assertNotNull(priceRange, "priceRange must be calculated");
        double range = Double.parseDouble(priceRange.toString().trim());
        assertTrue(range > 0, "Price range should be positive");
        assertEquals(range, max - min, 0.01, "Range should equal max - min");
    }

    /**
     * Test 7: Multi-Page Scraper
     * Business: Scrape multiple catalog pages for comprehensive data collection
     * Variables: baseUrl, totalPages, pagesScraped, pageNum, pageUrl, page
     */
    @Test(groups = "network", description = "Multi-page scraper crawls through pagination")
    public void testMultiPageScraper() throws Exception {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<config xmlns=\"http://org.webharvest/schema/2.1/core\" charset=\"UTF-8\">\n" +
            "    <def var=\"baseUrl\">https://books.toscrape.com/catalogue</def>\n" +
            "    <def var=\"totalPages\">2</def>\n" +
            "    \n" +
            "    <def var=\"pagesScraped\">0</def>\n" +
            "    <def var=\"lastPageUrl\"></def>\n" +
            "    \n" +
            "    <loop maxloops=\"${totalPages}\">\n" +
            "        <def var=\"pageNum\">${_index + 1}</def>\n" +
            "        <def var=\"pageUrl\">${baseUrl}/page-${pageNum}.html</def>\n" +
            "        <def var=\"lastPageUrl\">${pageUrl}</def>\n" +
            "        \n" +
            "        <def var=\"pageHtml\">\n" +
            "            <http url=\"${pageUrl}\"/>\n" +
            "        </def>\n" +
            "        \n" +
            "        <def var=\"page\">\n" +
            "            <html-to-xml>\n" +
            "                <get var=\"pageHtml\"/>\n" +
            "            </html-to-xml>\n" +
            "        </def>\n" +
            "        \n" +
            "        <def var=\"pagesScraped\">${_index + 1}</def>\n" +
            "    </loop>\n" +
            "</config>";
        
        DynamicScopeContext context = executeXmlString(xml);
        
        // Verify loop executed
        Variable pagesScraped = context.getVar("pagesScraped");
        assertNotNull(pagesScraped, "pagesScraped must be set");
        int scraped = Integer.parseInt(pagesScraped.toString().trim());
        assertEquals(scraped, 2, "Should scrape exactly 2 pages");
        
        // Verify last page URL was set
        Variable lastPageUrl = context.getVar("lastPageUrl");
        assertNotNull(lastPageUrl, "lastPageUrl must be set");
        assertTrue(lastPageUrl.toString().contains("page-2.html"), 
                  "Last page should be page-2, got: " + lastPageUrl.toString());
        
        // Verify page variable (from last iteration)
        Variable page = context.getVar("page");
        assertNotNull(page, "page variable must be set");
        assertTrue(page.toString().contains("<article"), 
                  "Page should contain product articles");
    }

    /**
     * Test 8: Product Data API Formatter
     * Business: Format scraped data as JSON API for system integration
     * Variables: sourceUrl, html, page, productData, productsExtracted, firstProductTitle, firstProductPrice
     */
    @Test(groups = "network", description = "API formatter creates structured product data")
    public void testProductDataAPIFormatter() throws Exception {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<config xmlns=\"http://org.webharvest/schema/2.1/core\" charset=\"UTF-8\">\n" +
            "    <def var=\"sourceUrl\">https://books.toscrape.com/</def>\n" +
            "    \n" +
            "    <def var=\"html\">\n" +
            "        <http url=\"${sourceUrl}\"/>\n" +
            "    </def>\n" +
            "    \n" +
            "    <def var=\"page\">\n" +
            "        <html-to-xml>\n" +
            "            <get var=\"html\"/>\n" +
            "        </html-to-xml>\n" +
            "    </def>\n" +
            "    \n" +
            "    <def var=\"productData\">\n" +
            "        <xquery>\n" +
            "            <xq-param name=\"content\" type=\"node()\">\n" +
            "                <get var=\"page\"/>\n" +
            "            </xq-param>\n" +
            "            <xq-expression>\n" +
            "                <![CDATA[\n" +
            "                declare variable $content as node() external;\n" +
            "                \n" +
            "                <api_response>\n" +
            "                    <products>\n" +
            "                    {\n" +
            "                        for $product at $pos in $content//article[@class=\"product_pod\"]\n" +
            "                        let $title := normalize-space($product//h3/a/@title)\n" +
            "                        let $price := normalize-space($product//p[@class=\"price_color\"])\n" +
            "                        let $rating := normalize-space($product//p[contains(@class, 'star-rating')]/@class)\n" +
            "                        where $pos <= 10\n" +
            "                        return\n" +
            "                            <product>\n" +
            "                                <id>{$pos}</id>\n" +
            "                                <title>{$title}</title>\n" +
            "                                <price>{$price}</price>\n" +
            "                                <rating>{replace($rating, 'star-rating ', '')}</rating>\n" +
            "                            </product>\n" +
            "                    }\n" +
            "                    </products>\n" +
            "                </api_response>\n" +
            "                ]]>\n" +
            "            </xq-expression>\n" +
            "        </xquery>\n" +
            "    </def>\n" +
            "    \n" +
            "    <def var=\"productsExtracted\">\n" +
            "        <xpath expression=\"count(//products/product)\">\n" +
            "            <get var=\"productData\"/>\n" +
            "        </xpath>\n" +
            "    </def>\n" +
            "    \n" +
            "    <def var=\"firstProductTitle\">\n" +
            "        <xpath expression=\"(//product/title/text())[1]\">\n" +
            "            <get var=\"productData\"/>\n" +
            "        </xpath>\n" +
            "    </def>\n" +
            "    \n" +
            "    <def var=\"firstProductPrice\">\n" +
            "        <xpath expression=\"(//product/price/text())[1]\">\n" +
            "            <get var=\"productData\"/>\n" +
            "        </xpath>\n" +
            "    </def>\n" +
            "</config>";
        
        DynamicScopeContext context = executeXmlString(xml);
        
        // Verify API data structure created
        Variable productData = context.getVar("productData");
        assertNotNull(productData, "productData must be generated");
        assertTrue(productData.toString().contains("<api_response>"));
        assertTrue(productData.toString().contains("<products>"));
        
        // Verify product extraction count
        Variable productsExtracted = context.getVar("productsExtracted");
        assertNotNull(productsExtracted, "productsExtracted must be counted");
        int extracted = Integer.parseInt(productsExtracted.toString().trim());
        assertTrue(extracted >= 10 && extracted <= 10, 
                  "Should extract exactly 10 products, got: " + extracted);
        
        // Verify first product details
        Variable firstProductTitle = context.getVar("firstProductTitle");
        assertNotNull(firstProductTitle, "firstProductTitle must be extracted");
        assertTrue(firstProductTitle.toString().length() > 5, 
                  "First product title should be meaningful");
        
        Variable firstProductPrice = context.getVar("firstProductPrice");
        assertNotNull(firstProductPrice, "firstProductPrice must be extracted");
        assertTrue(firstProductPrice.toString().contains("£"), 
                  "First product price should have currency");
    }

    /**
     * Test 7: Quotes Content Scraper  
     * Business: Extract quotes and authors for content management system
     * Variables: url, html, page, quotes, authors, quoteCount
     */
    @Test(groups = "network", description = "Quotes scraper extracts content and authors")
    public void testQuotesScraper() throws Exception {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<config xmlns=\"http://org.webharvest/schema/2.1/core\" charset=\"UTF-8\">\n" +
            "    <def var=\"url\">https://quotes.toscrape.com/</def>\n" +
            "    \n" +
            "    <def var=\"html\">\n" +
            "        <http url=\"${url}\"/>\n" +
            "    </def>\n" +
            "    \n" +
            "    <def var=\"page\">\n" +
            "        <html-to-xml>\n" +
            "            <get var=\"html\"/>\n" +
            "        </html-to-xml>\n" +
            "    </def>\n" +
            "    \n" +
            "    <def var=\"quotes\">\n" +
            "        <xpath expression=\"//div[@class='quote']//span[@class='text']/text()\">\n" +
            "            <get var=\"page\"/>\n" +
            "        </xpath>\n" +
            "    </def>\n" +
            "    \n" +
            "    <def var=\"authors\">\n" +
            "        <xpath expression=\"//div[@class='quote']//small[@class='author']/text()\">\n" +
            "            <get var=\"page\"/>\n" +
            "        </xpath>\n" +
            "    </def>\n" +
            "    \n" +
            "    <def var=\"quoteCount\">\n" +
            "        <xpath expression=\"count(//div[@class='quote'])\">\n" +
            "            <get var=\"page\"/>\n" +
            "        </xpath>\n" +
            "    </def>\n" +
            "</config>";
        
        DynamicScopeContext context = executeXmlString(xml);
        
        // Verify quotes extracted
        Variable quotes = context.getVar("quotes");
        assertNotNull(quotes, "quotes must be extracted");
        String quotesText = quotes.toString();
        assertFalse(quotesText.contains("<span"), 
                   "Quotes should be TEXT only, not contain HTML tags");
        assertTrue(quotesText.length() > 50, 
                  "Should extract substantial quotes content");
        
        // Verify authors extracted
        Variable authors = context.getVar("authors");
        assertNotNull(authors, "authors must be extracted");
        String authorsText = authors.toString();
        assertFalse(authorsText.contains("<small"), 
                   "Authors should be TEXT only, not contain HTML tags");
        assertTrue(authorsText.length() > 10);
        
        // Verify quote count
        Variable quoteCount = context.getVar("quoteCount");
        assertNotNull(quoteCount, "quoteCount must be counted");
        int count = Integer.parseInt(quoteCount.toString().trim());
        assertTrue(count >= 5 && count <= 20, 
                  "Should find 5-20 quotes, got: " + count);
    }

    /**
     * Test 8: XQuery Business Report
     * Business: Complex XQuery transformations for business reporting
     * Variables: xqueryResult, targetMet, quarterCount, regionCount
     */
    @Test(description = "XQuery processes business data with math functions")
    public void testXQueryBusinessReport() throws Exception {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<config xmlns=\"http://org.webharvest/schema/2.1/core\" charset=\"UTF-8\">\n" +
            "    <def var=\"xqueryResult\">\n" +
            "        <xquery>\n" +
            "            <xq-param name=\"logicvalue\" type=\"boolean\">true</xq-param>\n" +
            "            <xq-param name=\"revenue\" type=\"double\">150000</xq-param>\n" +
            "            <xq-param name=\"quarters\" type=\"integer*\">\n" +
            "                35000\n" +
            "                42000\n" +
            "                38000\n" +
            "                35000\n" +
            "            </xq-param>\n" +
            "            <xq-param name=\"companyName\" type=\"string\">TechCorp</xq-param>\n" +
            "            <xq-expression>\n" +
            "                <![CDATA[\n" +
            "                declare namespace math =\"java:java.lang.Math\";\n" +
            "\n" +
            "                declare variable $logicvalue as xs:boolean external;\n" +
            "                declare variable $revenue as xs:double external;\n" +
            "                declare variable $quarters as xs:integer* external;\n" +
            "                declare variable $companyName as xs:string external;\n" +
            "\n" +
            "                <business_report>\n" +
            "                    <company>{$companyName}</company>\n" +
            "                    <annual_revenue>{$revenue}</annual_revenue>\n" +
            "                    <target_met>{if ($logicvalue and $revenue > 100000) then 'YES' else 'NO'}</target_met>\n" +
            "                    <quarterly_performance>\n" +
            "                        { for $val at $pos in $quarters\n" +
            "                          return\n" +
            "                            <quarter>\n" +
            "                                <period>{$pos}</period>\n" +
            "                                <revenue>{$val}</revenue>\n" +
            "                                <growth_index>{math:sqrt($val)}</growth_index>\n" +
            "                            </quarter>\n" +
            "                        }\n" +
            "                    </quarterly_performance>\n" +
            "                </business_report>\n" +
            "                ]]>\n" +
            "            </xq-expression>\n" +
            "        </xquery>\n" +
            "    </def>\n" +
            "    \n" +
            "    <def var=\"targetMet\">\n" +
            "        <xpath expression=\"//target_met/text()\">\n" +
            "            <get var=\"xqueryResult\"/>\n" +
            "        </xpath>\n" +
            "    </def>\n" +
            "    \n" +
            "    <def var=\"quarterCount\">\n" +
            "        <xpath expression=\"count(//quarter)\">\n" +
            "            <get var=\"xqueryResult\"/>\n" +
            "        </xpath>\n" +
            "    </def>\n" +
            "</config>";
        
        DynamicScopeContext context = executeXmlString(xml);
        
        // Verify XQuery result generated
        Variable xqueryResult = context.getVar("xqueryResult");
        assertNotNull(xqueryResult, "xqueryResult must be generated");
        assertTrue(xqueryResult.toString().contains("<business_report>"));
        assertTrue(xqueryResult.toString().contains("<company>TechCorp</company>"));
        assertTrue(xqueryResult.toString().contains("150000"));
        
        // Verify target met calculated
        Variable targetMet = context.getVar("targetMet");
        assertNotNull(targetMet, "targetMet must be calculated");
        assertEquals(targetMet.toString().trim(), "YES", 
                    "Target should be met (revenue 150000 > 100000)");
        
        // Verify quarters processed
        Variable quarterCount = context.getVar("quarterCount");
        assertNotNull(quarterCount, "quarterCount must be counted");
        int quarters = Integer.parseInt(quarterCount.toString().trim());
        assertEquals(quarters, 4, "Should process 4 quarters");
    }
}

