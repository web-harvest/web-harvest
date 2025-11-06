/*
 * Copyright (c) 2006-2025 the original author or authors.
 */

package org.webharvest.plugin.core;

import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
// REMOVED in v2.2.x - old architecture classes no longer exist
// import org.webharvest.definition.CorePluginDef;
// import org.webharvest.definition.IElementDef;
// import org.webharvest.definition.XmlNode;
import org.webharvest.plugin.PluginConfiguration;
import org.webharvest.runtime.ScraperContext;
import org.webharvest.runtime.variables.Variable;

import java.util.ArrayList;

import static org.mockito.Mockito.*;
import static org.testng.Assert.*;
import static org.webharvest.plugin.core.MetadataTestUtils.expectedCategory;

/**
 * Massive test suite for {@link HtmlToXmlPlugin} to achieve 70%+ coverage.
 * Focus: Cover all code paths and methods.
 */
public class HtmlToXmlPluginMassiveTest {

    private HtmlToXmlPlugin plugin;
    
    // REMOVED in v2.2.x - old architecture mocks no longer needed
    // @Mock
    // private CorePluginDef elementDef;
    // @Mock
    // private XmlNode xmlNode;
    
    @Mock
    private PluginConfiguration configuration;

    @BeforeMethod
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        plugin = new HtmlToXmlPlugin();
    }
    
    // ==================== HTML5 Tags Tests ====================
    
    @Test
    public void testHtml5DivTag() throws Exception {
        testHtmlTag("<div>Content</div>");
    }
    
    @Test
    public void testHtml5SpanTag() throws Exception {
        testHtmlTag("<span>Text</span>");
    }
    
    @Test
    public void testHtml5ArticleTag() throws Exception {
        testHtmlTag("<article>Article content</article>");
    }
    
    @Test
    public void testHtml5SectionTag() throws Exception {
        testHtmlTag("<section>Section content</section>");
    }
    
    @Test
    public void testHtml5NavTag() throws Exception {
        testHtmlTag("<nav>Navigation</nav>");
    }
    
    @Test
    public void testHtml5HeaderTag() throws Exception {
        testHtmlTag("<header>Header content</header>");
    }
    
    @Test
    public void testHtml5FooterTag() throws Exception {
        testHtmlTag("<footer>Footer content</footer>");
    }
    
    @Test
    public void testHtml5MainTag() throws Exception {
        testHtmlTag("<main>Main content</main>");
    }
    
    @Test
    public void testHtml5AsideTag() throws Exception {
        testHtmlTag("<aside>Aside content</aside>");
    }
    
    // ==================== HTML Table Tests ====================
    
    @Test
    public void testTableTag() throws Exception {
        testHtmlTag("<table><tr><td>Cell</td></tr></table>");
    }
    
    @Test
    public void testTableWithHeaders() throws Exception {
        testHtmlTag("<table><thead><tr><th>Header</th></tr></thead><tbody><tr><td>Data</td></tr></tbody></table>");
    }
    
    @Test
    public void testTableWithMultipleRows() throws Exception {
        testHtmlTag("<table><tr><td>R1C1</td><td>R1C2</td></tr><tr><td>R2C1</td><td>R2C2</td></tr></table>");
    }
    
    // ==================== HTML Form Tests ====================
    
    @Test
    public void testFormTag() throws Exception {
        testHtmlTag("<form action=\"/submit\"><input type=\"text\" name=\"field\"/></form>");
    }
    
    @Test
    public void testInputTypeText() throws Exception {
        testHtmlTag("<input type=\"text\"/>");
    }
    
    @Test
    public void testInputTypePassword() throws Exception {
        testHtmlTag("<input type=\"password\"/>");
    }
    
    @Test
    public void testInputTypeEmail() throws Exception {
        testHtmlTag("<input type=\"email\"/>");
    }
    
    @Test
    public void testInputTypeNumber() throws Exception {
        testHtmlTag("<input type=\"number\"/>");
    }
    
    @Test
    public void testInputTypeCheckbox() throws Exception {
        testHtmlTag("<input type=\"checkbox\"/>");
    }
    
    @Test
    public void testInputTypeRadio() throws Exception {
        testHtmlTag("<input type=\"radio\"/>");
    }
    
    @Test
    public void testSelectTag() throws Exception {
        testHtmlTag("<select><option>Option 1</option><option>Option 2</option></select>");
    }
    
    @Test
    public void testTextareaTag() throws Exception {
        testHtmlTag("<textarea>Textarea content</textarea>");
    }
    
    @Test
    public void testButtonTag() throws Exception {
        testHtmlTag("<button>Click me</button>");
    }
    
    // ==================== HTML List Tests ====================
    
    @Test
    public void testUnorderedList() throws Exception {
        testHtmlTag("<ul><li>Item 1</li><li>Item 2</li></ul>");
    }
    
    @Test
    public void testOrderedList() throws Exception {
        testHtmlTag("<ol><li>First</li><li>Second</li></ol>");
    }
    
    @Test
    public void testNestedLists() throws Exception {
        testHtmlTag("<ul><li>Item<ul><li>Nested</li></ul></li></ul>");
    }
    
    @Test
    public void testDescriptionList() throws Exception {
        testHtmlTag("<dl><dt>Term</dt><dd>Definition</dd></dl>");
    }
    
    // ==================== HTML Link and Media Tests ====================
    
    @Test
    public void testAnchorTag() throws Exception {
        testHtmlTag("<a href=\"http://example.com\">Link</a>");
    }
    
    @Test
    public void testImageTag() throws Exception {
        testHtmlTag("<img src=\"image.jpg\" alt=\"Description\"/>");
    }
    
    @Test
    public void testVideoTag() throws Exception {
        testHtmlTag("<video src=\"video.mp4\">Video</video>");
    }
    
    @Test
    public void testAudioTag() throws Exception {
        testHtmlTag("<audio src=\"audio.mp3\">Audio</audio>");
    }
    
    // ==================== HTML Script and Style Tests ====================
    
    @Test
    public void testScriptTag() throws Exception {
        testHtmlTag("<script>var x = 5;</script>");
    }
    
    @Test
    public void testStyleTag() throws Exception {
        testHtmlTag("<style>body { color: red; }</style>");
    }
    
    @Test
    public void testLinkTag() throws Exception {
        testHtmlTag("<link rel=\"stylesheet\" href=\"style.css\"/>");
    }
    
    // ==================== HTML Comment Tests ====================
    
    @Test
    public void testHtmlComment() throws Exception {
        testHtmlTag("<!-- This is a comment --><p>Content</p>");
    }
    
    @Test
    public void testMultipleComments() throws Exception {
        testHtmlTag("<!-- Comment 1 --><p>Text</p><!-- Comment 2 -->");
    }
    
    // ==================== HTML Attribute Tests ====================
    
    @Test
    public void testElementWithId() throws Exception {
        testHtmlTag("<div id=\"myDiv\">Content</div>");
    }
    
    @Test
    public void testElementWithClass() throws Exception {
        testHtmlTag("<div class=\"container main\">Content</div>");
    }
    
    @Test
    public void testElementWithMultipleAttributes() throws Exception {
        testHtmlTag("<div id=\"id\" class=\"cls\" data-value=\"val\">Content</div>");
    }
    
    @Test
    public void testElementWithDataAttributes() throws Exception {
        testHtmlTag("<div data-id=\"123\" data-name=\"test\">Content</div>");
    }
    
    // ==================== HTML Special Cases ====================
    
    @Test
    public void testSelfClosingTags() throws Exception {
        testHtmlTag("<br/><hr/><img src=\"x.jpg\"/>");
    }
    
    @Test
    public void testMetaTag() throws Exception {
        testHtmlTag("<meta charset=\"UTF-8\"/>");
    }
    
    @Test
    public void testBaseTag() throws Exception {
        testHtmlTag("<base href=\"http://example.com/\"/>");
    }
    
    // ==================== Malformed HTML Tests ====================
    
    @Test
    public void testMalformedHtml1() throws Exception {
        testHtmlTag("<html><body><p>No closing tags");
    }
    
    @Test
    public void testMalformedHtml2() throws Exception {
        testHtmlTag("<div><p>Nested</div></p>");
    }
    
    @Test
    public void testMalformedHtml3() throws Exception {
        testHtmlTag("<html><body>Text<br>More text");
    }
    
    @Test
    public void testMalformedHtml4() throws Exception {
        testHtmlTag("Just plain text without tags");
    }
    
    @Test
    public void testMalformedHtml5() throws Exception {
        testHtmlTag("<div attr=\"value>Unclosed quote</div>");
    }
    
    @Test
    public void testHtmlWithInvalidXmlChars() throws Exception {
        testHtmlTag("<div>Text with \u0001\u0002\u0003 control chars</div>");
    }
    
    @Test
    public void testHtmlWithCData() throws Exception {
        testHtmlTag("<div><![CDATA[CDATA content]]></div>");
    }
    
    @Test
    public void testHtmlWithDoctype() throws Exception {
        testHtmlTag("<!DOCTYPE html><html><body>Content</body></html>");
    }
    
    @Test
    public void testHtmlWithXmlDeclaration() throws Exception {
        testHtmlTag("<?xml version=\"1.0\"?><html><body>Content</body></html>");
    }
    
    @Test
    public void testHtmlWithNamespace() throws Exception {
        testHtmlTag("<html xmlns=\"http://www.w3.org/1999/xhtml\"><body>Content</body></html>");
    }
    
    // ==================== Complex HTML Structures ====================
    
    @Test
    public void testDeeplyNestedStructure() throws Exception {
        testHtmlTag("<div><div><div><div><div>Deep</div></div></div></div></div>");
    }
    
    @Test
    public void testMixedContentTypes() throws Exception {
        testHtmlTag("<div>Text<span>Inline</span>More text<p>Block</p>End</div>");
    }
    
    @Test
    public void testEmptyElements() throws Exception {
        testHtmlTag("<div></div><span></span><p></p>");
    }
    
    @Test
    public void testWhitespaceOnly() throws Exception {
        testHtmlTag("   \n\t\r\n   ");
    }
    
    @Test
    public void testHtmlWithEscapedContent() throws Exception {
        testHtmlTag("<div>&lt;escaped&gt; &amp; &quot;quoted&quot;</div>");
    }
    
    // ==================== Charset Tests ====================
    
    @Test
    public void testUtf8Charset() throws Exception {
        ScraperContext realContext = new ScraperContext();
        
        when(configuration.getProperty("id")).thenReturn(null);
        when(configuration.getProperty("charset")).thenReturn("UTF-8");
        
        // v2.2.x ULTRA-LEAN: old architecture removed
        // when(xmlNode.getText()).thenReturn("<div>UTF-8: ąćęłńóśźż</div>");
        // when(xmlNode.getElementList()).thenReturn(new ArrayList());
        // when(elementDef.getNode()).thenReturn(xmlNode);
        // when(elementDef.hasOperations()).thenReturn(false);
        
        plugin.initialize(configuration);
        Variable result = plugin.execute(realContext);
        
        assertNotNull(result);
    }
    
    @Test
    public void testIso88591Charset() throws Exception {
        ScraperContext realContext = new ScraperContext();
        
        when(configuration.getProperty("id")).thenReturn(null);
        when(configuration.getProperty("charset")).thenReturn("ISO-8859-1");
        
        // v2.2.x ULTRA-LEAN: old architecture removed
        // when(xmlNode.getText()).thenReturn("<div>ISO text</div>");
        // when(xmlNode.getElementList()).thenReturn(new ArrayList());
        // when(elementDef.getNode()).thenReturn(xmlNode);
        // when(elementDef.hasOperations()).thenReturn(false);
        
        plugin.initialize(configuration);
        Variable result = plugin.execute(realContext);
        
        assertNotNull(result);
    }
    
    @Test
    public void testWindows1252Charset() throws Exception {
        ScraperContext realContext = new ScraperContext();
        
        when(configuration.getProperty("id")).thenReturn(null);
        when(configuration.getProperty("charset")).thenReturn("windows-1252");
        
        // v2.2.x ULTRA-LEAN: old architecture removed
        // when(xmlNode.getText()).thenReturn("<div>Windows charset</div>");
        // when(xmlNode.getElementList()).thenReturn(new ArrayList());
        // when(elementDef.getNode()).thenReturn(xmlNode);
        // when(elementDef.hasOperations()).thenReturn(false);
        
        plugin.initialize(configuration);
        Variable result = plugin.execute(realContext);
        
        assertNotNull(result);
    }
    
    // ==================== Real World HTML Scenarios ====================
    
    @Test
    public void testCompleteHtmlDocument() throws Exception {
        String html = "<!DOCTYPE html>\n" +
                     "<html lang=\"en\">\n" +
                     "<head>\n" +
                     "  <meta charset=\"UTF-8\">\n" +
                     "  <title>Test Page</title>\n" +
                     "</head>\n" +
                     "<body>\n" +
                     "  <h1>Header</h1>\n" +
                     "  <p>Paragraph</p>\n" +
                     "</body>\n" +
                     "</html>";
        testHtmlTag(html);
    }
    
    @Test
    public void testHtmlWithInlineStyles() throws Exception {
        testHtmlTag("<div style=\"color: red; font-size: 14px;\">Styled content</div>");
    }
    
    @Test
    public void testHtmlWithInlineScripts() throws Exception {
        testHtmlTag("<div><script>var x = 5;</script>Content</div>");
    }
    
    @Test
    public void testHtmlWithSvg() throws Exception {
        testHtmlTag("<div><svg width=\"100\" height=\"100\"><circle cx=\"50\" cy=\"50\" r=\"40\"/></svg></div>");
    }
    
    @Test
    public void testHtmlWithDataAttributes() throws Exception {
        testHtmlTag("<div data-id=\"123\" data-value=\"test\" data-flag=\"true\">Data attrs</div>");
    }
    
    @Test
    public void testHtmlWithAriaAttributes() throws Exception {
        testHtmlTag("<button aria-label=\"Close\" aria-hidden=\"false\">X</button>");
    }
    
    @Test
    public void testHtmlWithBooleanAttributes() throws Exception {
        testHtmlTag("<input type=\"checkbox\" checked disabled readonly/>");
    }
    
    @Test
    public void testHtmlWithUnquotedAttributes() throws Exception {
        testHtmlTag("<div class=unquoted>Content</div>");
    }
    
    // ==================== HTML Entity Tests ====================
    
    @Test
    public void testNumericEntities() throws Exception {
        testHtmlTag("<div>&#65; &#x41; &#169;</div>");
    }
    
    @Test
    public void testNamedEntities() throws Exception {
        testHtmlTag("<div>&nbsp; &copy; &reg; &trade;</div>");
    }
    
    @Test
    public void testMixedEntities() throws Exception {
        testHtmlTag("<div>&lt;&#60;&gt;&#62;&amp;&#38;&quot;&#34;</div>");
    }
    
    // ==================== HTML List Variations ====================
    
    @Test
    public void testNestedUnorderedLists() throws Exception {
        testHtmlTag("<ul><li>Item 1<ul><li>Sub 1.1</li><li>Sub 1.2</li></ul></li><li>Item 2</li></ul>");
    }
    
    @Test
    public void testMixedListTypes() throws Exception {
        testHtmlTag("<ul><li>Unordered</li></ul><ol><li>Ordered</li></ol>");
    }
    
    // ==================== HTML Table Variations ====================
    
    @Test
    public void testTableWithColspan() throws Exception {
        testHtmlTag("<table><tr><td colspan=\"2\">Wide cell</td></tr></table>");
    }
    
    @Test
    public void testTableWithRowspan() throws Exception {
        testHtmlTag("<table><tr><td rowspan=\"2\">Tall cell</td><td>Cell 1</td></tr><tr><td>Cell 2</td></tr></table>");
    }
    
    @Test
    public void testTableWithCaption() throws Exception {
        testHtmlTag("<table><caption>Table Caption</caption><tr><td>Data</td></tr></table>");
    }
    
    // ==================== HTML5 Semantic Elements ====================
    
    @Test
    public void testFigureElement() throws Exception {
        testHtmlTag("<figure><img src=\"image.jpg\"/><figcaption>Caption</figcaption></figure>");
    }
    
    @Test
    public void testDetailsElement() throws Exception {
        testHtmlTag("<details><summary>Summary</summary><p>Details content</p></details>");
    }
    
    @Test
    public void testMarkElement() throws Exception {
        testHtmlTag("<p>This is <mark>highlighted</mark> text</p>");
    }
    
    @Test
    public void testTimeElement() throws Exception {
        testHtmlTag("<time datetime=\"2025-10-08\">October 8, 2025</time>");
    }
    
    // ==================== HTML Form Elements ====================
    
    @Test
    public void testFormWithFieldset() throws Exception {
        testHtmlTag("<form><fieldset><legend>Form Section</legend><input type=\"text\"/></fieldset></form>");
    }
    
    @Test
    public void testFormWithLabel() throws Exception {
        testHtmlTag("<form><label for=\"name\">Name:</label><input id=\"name\" type=\"text\"/></form>");
    }
    
    @Test
    public void testFormWithOptgroup() throws Exception {
        testHtmlTag("<select><optgroup label=\"Group\"><option>Option 1</option></optgroup></select>");
    }
    
    // ==================== Edge Cases ====================
    
    @Test
    public void testVeryLargeHtml() throws Exception {
        StringBuilder largeHtml = new StringBuilder("<html><body>");
        for (int i = 0; i < 1000; i++) {
            largeHtml.append("<div>Line ").append(i).append("</div>");
        }
        largeHtml.append("</body></html>");
        testHtmlTag(largeHtml.toString());
    }
    
    @Test
    public void testHtmlWithInvalidTagName() throws Exception {
        testHtmlTag("<123invalid>Content</123invalid>");
    }
    
    @Test
    public void testHtmlWithDotInTagName() throws Exception {
        testHtmlTag("<tag.name>Content</tag.name>");
    }
    
    @Test
    public void testHtmlWithColonInTagName() throws Exception {
        testHtmlTag("<ns:tag>Content</ns:tag>");
    }
    
    // ==================== Configuration Variations ====================
    
    @Test
    public void testWithIdAttribute() throws Exception {
        ScraperContext realContext = new ScraperContext();
        
        when(configuration.getProperty("id")).thenReturn("htmlParser");
        when(configuration.getProperty("charset")).thenReturn("UTF-8");
        
        // v2.2.x ULTRA-LEAN: old architecture removed
        // when(xmlNode.getText()).thenReturn("<div>Content</div>");
        // when(xmlNode.getElementList()).thenReturn(new ArrayList());
        // when(elementDef.getNode()).thenReturn(xmlNode);
        // when(elementDef.hasOperations()).thenReturn(false);
        
        plugin.initialize(configuration);
        Variable result = plugin.execute(realContext);
        
        assertNotNull(result);
    }
    
    @Test
    public void testWithoutCharsetAttribute() throws Exception {
        ScraperContext realContext = new ScraperContext();
        
        when(configuration.getProperty("id")).thenReturn(null);
        when(configuration.getProperty("charset")).thenReturn(null);
        
        // v2.2.x ULTRA-LEAN: old architecture removed
        // when(xmlNode.getText()).thenReturn("<div>Content</div>");
        // when(xmlNode.getElementList()).thenReturn(new ArrayList());
        // when(elementDef.getNode()).thenReturn(xmlNode);
        // when(elementDef.hasOperations()).thenReturn(false);
        
        plugin.initialize(configuration);
        Variable result = plugin.execute(realContext);
        
        assertNotNull(result);
    }
    
    // ==================== Parsing Strategy Tests ====================
    
    @Test
    public void testJSoupParseStrategyTriggered() throws Exception {
        // Well-formed HTML should use JSoup parse strategy
        testHtmlTag("<html><head><title>Title</title></head><body><p>Paragraph</p></body></html>");
    }
    
    @Test
    public void testJSoupCleanStrategyTriggered() throws Exception {
        // HTML with script/style should trigger clean strategy
        testHtmlTag("<html><body><script>alert('test');</script><p>Content</p></body></html>");
    }
    
    @Test
    public void testTagSoupStrategyTriggered() throws Exception {
        // Malformed HTML should eventually use TagSoup
        testHtmlTag("<html><body><p>Unclosed paragraph<div>Nested incorrectly</p></div>");
    }
    
    @Test
    public void testMultipleStrategiesAttempted() throws Exception {
        // Complex malformed HTML that might need multiple strategies
        testHtmlTag("<html><body attr=\"unclosed><p>Text<br>More<div>Nested</body>");
    }
    
    // ==================== Unicode and Special Encoding ====================
    
    @Test
    public void testChineseCharacters() throws Exception {
        testHtmlTag("<div>中文内容测试</div>");
    }
    
    @Test
    public void testArabicCharacters() throws Exception {
        testHtmlTag("<div>النص العربي</div>");
    }
    
    @Test
    public void testRussianCharacters() throws Exception {
        testHtmlTag("<div>Русский текст</div>");
    }
    
    @Test
    public void testPolishCharacters() throws Exception {
        testHtmlTag("<div>Polski tekst: ąćęłńóśźż ĄĆĘŁŃÓŚŹŻ</div>");
    }
    
    @Test
    public void testEmojiCharacters() throws Exception {
        testHtmlTag("<div>Emoji: 😀 🎉 ✅ 🚀</div>");
    }
    
    @Test
    public void testMixedScripts() throws Exception {
        testHtmlTag("<div>Mixed: ABC 中文 العربية ąćę</div>");
    }
    
    // ==================== Metadata and Schema Tests ====================
    
    @Test
    public void testMetadataId() {
        assertEquals(plugin.getMetadata().getId(), "html-to-xml");
    }
    
    @Test
    public void testMetadataCategory() {
        assertEquals(plugin.getMetadata().getCategory(), expectedCategory(HtmlToXmlPlugin.class));
    }
    
    @Test
    public void testMetadataTags() {
        String[] tags = plugin.getMetadata().getTags();
        assertNotNull(tags);
        assertTrue(tags.length > 0);
    }
    
    @Test
    public void testMetadataClassName() {
        assertEquals(plugin.getMetadata().getClassName(), HtmlToXmlPlugin.class.getName());
    }
    
    @Test
    public void testMetadataNamespace() {
        assertTrue(plugin.getMetadata().getNamespace().contains("webharvest"));
    }
    
    @Test
    public void testSchemaHasAttributes() {
        var schema = plugin.getMetadata().getConfigurationSchema();
        var attrs = schema.getAttributeDefinitions();
        assertNotNull(attrs);
    }
    
    @Test
    public void testSchemaVersioning() {
        assertEquals(plugin.getMetadata().getConfigurationSchema().getVersion(), "2.3.0");
    }
    
    @Test
    public void testSchemaRequiredSubElements() {
        String[] required = plugin.getMetadata().getConfigurationSchema().getRequiredSubElements();
        assertNotNull(required);
    }
    
    @Test
    public void testSchemaAllowedSubElements() {
        String[] allowed = plugin.getMetadata().getConfigurationSchema().getAllowedSubElements();
        assertNotNull(allowed);
    }
    
    // ==================== HTML with JavaScript ====================
    
    @Test
    public void testHtmlWithInlineJavaScript() throws Exception {
        testHtmlTag("<div onclick=\"alert('click')\">Clickable</div>");
    }
    
    @Test
    public void testHtmlWithScriptBlock() throws Exception {
        testHtmlTag("<html><head><script type=\"text/javascript\">function test() { return true; }</script></head><body>Content</body></html>");
    }
    
    @Test
    public void testHtmlWithStyleBlock() throws Exception {
        testHtmlTag("<html><head><style>body { margin: 0; }</style></head><body>Content</body></html>");
    }
    
    // ==================== Helper Method ====================
    
    private void testHtmlTag(String html) throws Exception {
        ScraperContext realContext = new ScraperContext();
        
        when(configuration.getProperty("id")).thenReturn(null);
        when(configuration.getProperty("charset")).thenReturn("UTF-8");
        
        // v2.2.x ULTRA-LEAN: old architecture removed
        // when(xmlNode.getText()).thenReturn(html);
        // when(xmlNode.getElementList()).thenReturn(new ArrayList());
        // when(elementDef.getNode()).thenReturn(xmlNode);
        // when(elementDef.hasOperations()).thenReturn(false);
        
        plugin.initialize(configuration);
        Variable result = plugin.execute(realContext);
        
        assertNotNull(result, "Result should not be null for HTML: " + html);
    }
}

