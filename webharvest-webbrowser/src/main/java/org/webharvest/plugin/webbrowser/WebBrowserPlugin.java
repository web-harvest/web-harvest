/*
 * Copyright (c) 2006-2025 the original author or authors.
 *
 * Redistribution and use of this software in source and binary forms,
 * with or without modification, are permitted provided that the following
 * conditions are met:
 *
 * * Redistributions of source code must retain the above
 *   copyright notice, this list of conditions and the
 *   following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above
 *   copyright notice, this list of conditions and the
 *   following disclaimer in the documentation and/or other
 *   materials provided with the distribution.
 *
 * * The name of Web-Harvest may not be used to endorse or promote
 *   products derived from this software without specific prior
 *   written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUPTION OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

package org.webharvest.plugin.webbrowser;

import org.htmlunit.BrowserVersion;
import org.htmlunit.NicelyResynchronizingAjaxController;
import org.htmlunit.WebClient;
import org.htmlunit.html.HtmlPage;

import org.webharvest.plugin.AbstractPlugin;
import org.webharvest.plugin.PluginConfiguration;
import org.webharvest.plugin.PluginConfigurationSchema;
import org.webharvest.plugin.PluginMetadata;
import org.webharvest.plugin.annotations.CorePlugin;
import org.webharvest.plugin.exceptions.PluginException;
import org.webharvest.plugin.exceptions.PluginValidationException;
import org.webharvest.runtime.DynamicScopeContext;
import org.webharvest.runtime.session.ScraperSession;
import org.webharvest.runtime.session.events.SessionCancelledEvent;
import org.webharvest.runtime.session.events.SessionCompletedEvent;
import org.webharvest.runtime.session.events.SessionFailedEvent;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import org.webharvest.runtime.variables.EmptyVariable;
import org.webharvest.runtime.variables.NodeVariable;
import org.webharvest.runtime.variables.Variable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;

/**
 * WebBrowser plugin for browser automation with JavaScript and AJAX support.
 * 
 * <h3>Bug #33 Fix (v2.2.1): JavaScript and AJAX Support</h3>
 * <p>
 * <strong>History:</strong> This feature existed in old architecture (r655, 2015)
 * using PhantomJS, but was lost during refactoring to new plugin system. 
 * This implementation restores the functionality using HtmlUnit.
 * </p>
 * 
 * <h3>Features:</h3>
 * <ul>
 * <li><strong>load</strong>: Load URL with full JavaScript execution and AJAX handling</li>
 * <li><strong>execute-script</strong>: Execute custom JavaScript on loaded page</li>
 * <li><strong>get-content</strong>: Get page HTML after JavaScript/AJAX execution</li>
 * <li><strong>close</strong>: Close browser instance</li>
 * </ul>
 * 
 * <h3>Attributes:</h3>
 * <ul>
 * <li><code>action</code>: Action to perform (load|execute-script|get-content|close)</li>
 * <li><code>url</code>: URL to load (required for 'load' action)</li>
 * <li><code>page</code>: Named page reference (optional, default page if not specified)</li>
 * <li><code>wait</code>: Wait time in ms for AJAX/JavaScript (default: 1000ms)</li>
 * <li><code>javascript-enabled</code>: Enable/disable JavaScript (default: true)</li>
 * </ul>
 * 
 * <h3>Usage Examples</h3>
 * 
 * <h4>Example 1: Render JavaScript-heavy page</h4>
 * <pre>{@code
 * <?xml version="1.0" encoding="UTF-8"?>
 * <config xmlns="http://org.webharvest/schema/2.1/core">
 * <!-- Regular HTTP can't see JavaScript content -->
 * <def var="staticHtml">
 *     <http url="https://spa-app.example.com"/>
 * </def>
 * <!-- Result: Just loading spinner HTML, no actual data -->
 * 
 * <!-- WebBrowser executes JavaScript -->
 * <def var="renderedHtml">
 *     <web-browser action="load" url="https://spa-app.example.com" wait="3000"/>
 *     <web-browser action="get-content"/>
 * </def>
 * <!-- Result: Fully rendered page with AJAX data -->
 * 
 * <def var="products">
 *     <xpath expression="//div[@class='product']">
 *         <html-to-xml>${renderedHtml}</html-to-xml>
 *     </xpath>
 * </def>
 * </config>
 * }</pre>
 * 
 * <h4>Example 2: Single Page Application (SPA) scraping</h4>
 * <pre>{@code
 * <?xml version="1.0" encoding="UTF-8"?>
 * <config xmlns="http://org.webharvest/schema/2.1/core">
 * <!-- Load SPA page -->
 * <web-browser action="load" url="https://spa.example.com/products" page="main" wait="4000"/>
 * 
 * <!-- Get rendered content -->
 * <def var="spaContent">
 *     <web-browser action="get-content" page="main"/>
 * </def>
 * 
 * <!-- Extract data with XPath -->
 * <loop item="product">
 *     <xpath expression="//div[@class='product-card']">
 *         <html-to-xml>${spaContent}</html-to-xml>
 *     </xpath>
 *     
 *     <def var="title">
 *         <xpath expression=".//h3/text()">${product}</xpath>
 *     </def>
 * </loop>
 * 
 * <!-- Close browser -->
 * <web-browser action="close" page="main"/>
 * </config>
 * }</pre>
 * 
 * <h4>Example 3: Execute JavaScript on page</h4>
 * <pre>{@code
 * <?xml version="1.0" encoding="UTF-8"?>
 * <config xmlns="http://org.webharvest/schema/2.1/core">
 * <!-- Load page -->
 * <web-browser action="load" url="http://example.com" page="main" wait="2000"/>
 * 
 * <!-- Click "Load More" button via JavaScript -->
 * <web-browser action="execute-script" page="main">
 *   document.querySelector('#load-more-button').click();
 * </web-browser>
 * 
 * <!-- Wait for AJAX -->
 * <sleep milliseconds="2000"/>
 * 
 * <!-- Get updated content -->
 * <def var="moreContent">
 *     <web-browser action="get-content" page="main"/>
 * </def>
 * 
 * <web-browser action="close" page="main"/>
 * </config>
 * }</pre>
 * 
 * <h3>When to Use</h3>
 * <ul>
 *   <li><strong>Use web-browser:</strong> JavaScript-rendered content, SPAs, AJAX, infinite scroll</li>
 *   <li><strong>Use http:</strong> Static HTML, APIs, better performance</li>
 * </ul>
 * 
 * <h3>Important Notes</h3>
 * <ul>
 *   <li><strong>Performance:</strong> Slower than {@code <http>} due to JavaScript execution</li>
 *   <li><strong>Memory:</strong> Browser engine requires more memory</li>
 *   <li><strong>Wait time:</strong> Adjust wait parameter if content not loaded</li>
 *   <li><strong>Named pages:</strong> Reuse browser instance with page attribute</li>
 * </ul>
 * 
 * <h3>Related Processors</h3>
 * <ul>
 *   <li>{@code <http>} - HTTP requests (faster, no JavaScript)</li>
 *   <li>{@code <html-to-xml>} - Parse rendered HTML</li>
 *   <li>{@code <xpath>} - Extract data from rendered page</li>
 * </ul>
 * 
 * @author WebHarvest Team
 * @since 2.1.0
 * @version 2.2.1
 */
@CorePlugin(elementName = "web-browser")
public class WebBrowserPlugin extends AbstractPlugin {

    private static final Logger LOGGER = LoggerFactory.getLogger(WebBrowserPlugin.class);

    private static final String ACTION_ATT_NAME = "action";
    private static final String URL_ATT_NAME = "url";
    private static final String PAGE_ATT_NAME = "page";
    private static final String WAIT_ATT_NAME = "wait";
    private static final String JAVASCRIPT_ENABLED_ATT_NAME = "javascript-enabled";

    // Browser state belongs to one scraper session, never to the JVM globally.
    private static final Map<String, BrowserSessionState> SESSION_PAGES = new ConcurrentHashMap<>();

    // Compatibility for callers that construct a context without a ScraperSession.
    // Weak keys avoid retaining those contexts after their execution is discarded.
    private static final Map<DynamicScopeContext, BrowserSessionState> CONTEXT_PAGES =
            Collections.synchronizedMap(new WeakHashMap<>());

    private static BrowserSessionState getBrowserSessionState(DynamicScopeContext context) {
        if (context == null) {
            throw new IllegalArgumentException("Execution context cannot be null");
        }

        ScraperSession session = context.getSession();
        if (session == null || session.getSessionId() == null || session.getSessionId().trim().isEmpty()) {
            synchronized (CONTEXT_PAGES) {
                return CONTEXT_PAGES.computeIfAbsent(context, ignored -> new BrowserSessionState(null, null));
            }
        }

        String sessionId = session.getSessionId();
        BrowserSessionState state = SESSION_PAGES.computeIfAbsent(
                sessionId, ignored -> new BrowserSessionState(sessionId, context.getEventBus()));
        // Covers a terminal transition racing with state registration.
        if (session.getStatus().isTerminal()) {
            state.close();
        }
        return state;
    }

    private static final class BrowserSessionState {
        private final String sessionId;
        private final EventBus eventBus;
        private final Map<String, WebClient> namedPages = new ConcurrentHashMap<>();
        private WebClient defaultPage;
        private boolean closed;

        private BrowserSessionState(String sessionId, EventBus eventBus) {
            this.sessionId = sessionId;
            this.eventBus = eventBus;
            if (eventBus != null && sessionId != null) {
                eventBus.register(this);
            }
        }

        private synchronized WebClient get(String pageName) {
            if (closed) {
                return null;
            }
            return pageName != null && !pageName.isEmpty() ? namedPages.get(pageName) : defaultPage;
        }

        private synchronized void put(String pageName, WebClient client) {
            if (closed) {
                client.close();
                return;
            }
            WebClient previous;
            if (pageName != null && !pageName.isEmpty()) {
                previous = namedPages.put(pageName, client);
            } else {
                previous = defaultPage;
                defaultPage = client;
            }
            if (previous != null) {
                previous.close();
            }
        }

        private synchronized WebClient remove(String pageName) {
            if (closed) {
                return null;
            }
            if (pageName != null && !pageName.isEmpty()) {
                return namedPages.remove(pageName);
            }
            WebClient previous = defaultPage;
            defaultPage = null;
            return previous;
        }

        private void close() {
            synchronized (this) {
                if (closed) {
                    return;
                }
                closed = true;
                if (defaultPage != null) {
                    defaultPage.close();
                    defaultPage = null;
                }
                for (WebClient client : namedPages.values()) {
                    client.close();
                }
                namedPages.clear();
            }
            if (eventBus != null && sessionId != null) {
                eventBus.unregister(this);
                SESSION_PAGES.remove(sessionId, this);
            }
        }

        @Subscribe
        public void onSessionCompleted(SessionCompletedEvent event) {
            if (sessionId != null && sessionId.equals(event.getSessionId())) {
                close();
            }
        }

        @Subscribe
        public void onSessionFailed(SessionFailedEvent event) {
            if (sessionId != null && sessionId.equals(event.getSessionId())) {
                close();
            }
        }

        @Subscribe
        public void onSessionCancelled(SessionCancelledEvent event) {
            if (sessionId != null && sessionId.equals(event.getSessionId())) {
                close();
            }
        }
    }

    private static final PluginMetadata METADATA = new PluginMetadata() {
        @Override
        public String getId() {
            return "web-browser";
        }

        @Override
        public String getName() {
            return "Web Browser Plugin";
        }

        @Override
        public String getDescription() {
            return "Plugin for web browser automation with JavaScript and AJAX support (HtmlUnit)";
        }

        @Override
        public String getVersion() {
            return "2.2.1";
        }

        @Override
        public String getClassName() {
            return WebBrowserPlugin.class.getName();
        }

        @Override
        public boolean isCorePlugin() {
            return true;
        }

        @Override
        public String getElementName() {
            return "web-browser";
        }

        @Override
        public String getNamespace() {
            return "http://org.webharvest/schema/2.1/core";
        }

        @Override
        public String[] getDependencies() {
            return new String[0];
        }

        @Override
        public String[] getTags() {
            return new String[]{"browser", "javascript", "ajax", "automation"};
        }

        @Override
        public String getCategory() {
            return "Browser";
        }

        @Override
        public String getAuthor() {
            return "Web-Harvest Team";
        }

        @Override
        public PluginConfigurationSchema getConfigurationSchema() {
            return new PluginConfigurationSchema() {
                @Override
                public AttributeDefinition[] getAttributeDefinitions() {
                    return new AttributeDefinition[0];
                }

                @Override
                public String[] getRequiredAttributes() {
                    return new String[]{ACTION_ATT_NAME};
                }

                @Override
                public String[] getOptionalAttributes() {
                    return new String[]{URL_ATT_NAME, PAGE_ATT_NAME, WAIT_ATT_NAME, JAVASCRIPT_ENABLED_ATT_NAME};
                }

                @Override
                public String getVersion() {
                    return "2.2.1";
                }

                @Override
                public boolean allowsBody() {
                    return true;
                }

                @Override
                public String[] getRequiredSubElements() {
                    return new String[0];
                }

                @Override
                public String[] getAllowedSubElements() {
                    return new String[0];
                }

                @Override
                public AttributeDefinition getAttribute(String name) {
                    return null;
                }

                @Override
                public void validate(PluginConfiguration configuration) throws PluginValidationException {
                    if (configuration == null) {
                        throw new PluginValidationException("Configuration cannot be null");
                    }
                    
                    String[] required = getRequiredAttributes();
                    for (String attr : required) {
                        if (configuration.getProperty(attr) == null || configuration.getProperty(attr).trim().isEmpty()) {
                            throw new PluginValidationException("Required attribute '" + attr + "' is missing or empty");
                        }
                    }
                }
            };
        }
    };

    @Override
    public PluginMetadata getMetadata() {
        return METADATA;
    }

    public PluginConfigurationSchema getConfigurationSchema() {
        return METADATA.getConfigurationSchema();
    }

    @Override
    protected void doInitialize(PluginConfiguration configuration) throws PluginException {
        doValidateConfiguration(configuration);
    }

    @Override
    protected void doValidateConfiguration(PluginConfiguration configuration) throws PluginValidationException {
        if (configuration == null) {
            throw new PluginValidationException("Configuration cannot be null");
        }
        getConfigurationSchema().validate(configuration);
    }

    @Override
    protected Variable doExecute(DynamicScopeContext context) throws InterruptedException {
        PluginConfiguration config = getConfiguration();
        
        String action = config.getProperty(ACTION_ATT_NAME);
        String url = config.getProperty(URL_ATT_NAME, "");
        String pageName = config.getProperty(PAGE_ATT_NAME);
        String waitStr = config.getProperty(WAIT_ATT_NAME, "1000");
        String javascriptEnabledStr = config.getProperty(JAVASCRIPT_ENABLED_ATT_NAME, "true");
        
        int waitTime;
        try {
            waitTime = Integer.parseInt(waitStr);
        } catch (NumberFormatException e) {
            waitTime = 1000;
        }
        
        boolean javascriptEnabled = "true".equalsIgnoreCase(javascriptEnabledStr) 
            || "yes".equalsIgnoreCase(javascriptEnabledStr);
        
        LOGGER.debug("WebBrowser Plugin - Action: " + action + ", URL: " + url + 
            ", Page: " + pageName + ", Wait: " + waitTime + "ms");
        
        try {
            return executeAction(action, url, pageName, waitTime, javascriptEnabled, context);
        } catch (Exception e) {
            LOGGER.error("Error in webbrowser plugin", e);
            throw new RuntimeException("Error in webbrowser plugin: " + e.getMessage(), e);
        }
    }

    private Variable executeAction(String action, String url, String pageName, 
                                   int waitTime, boolean javascriptEnabled,
                                   DynamicScopeContext context) 
            throws PluginException, InterruptedException {
        
        if ("load".equalsIgnoreCase(action)) {
            return handleLoad(url, pageName, waitTime, javascriptEnabled, context);
        } else if ("execute-script".equalsIgnoreCase(action)) {
            return handleExecuteScript(pageName, waitTime, context);
        } else if ("get-content".equalsIgnoreCase(action)) {
            return handleGetContent(pageName, context);
        } else if ("close".equalsIgnoreCase(action)) {
            return handleClose(pageName, context);
        } else {
            throw new PluginException("Unsupported WebBrowser action: " + action + 
                ". Supported: load, execute-script, get-content, close");
        }
    }

    /**
     * Load URL with JavaScript and AJAX support.
     * 
     * <p>Bug #33 Fix: Uses HtmlUnit to fully execute JavaScript and wait for AJAX.</p>
     */
    private Variable handleLoad(String url, String pageName, int waitTime, boolean javascriptEnabled,
                                DynamicScopeContext context) throws PluginException {
        LOGGER.info("WebBrowser load: " + url + " (page: " + pageName + 
            ", wait: " + waitTime + "ms, js: " + javascriptEnabled + ")");
        
        if (url == null || url.trim().isEmpty()) {
            throw new PluginException("URL is required for load action");
        }
        
        // Validate URL format
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            throw new PluginException("Invalid URL format: " + url);
        }
        
        try {
            // Create WebClient with JavaScript support
            WebClient webClient = createWebClient(javascriptEnabled);
            
            // Load page
            LOGGER.debug("Loading page: {}", url);
            HtmlPage page = webClient.getPage(url);
            
            // Wait for JavaScript/AJAX to complete
            if (javascriptEnabled && waitTime > 0) {
                LOGGER.debug("Waiting {}ms for JavaScript/AJAX", waitTime);
                webClient.waitForBackgroundJavaScript(waitTime);
                Thread.sleep(waitTime); // Additional wait for async operations
            }
            
            // Store page in the owning scraper session only.
            getBrowserSessionState(context).put(pageName, webClient);
            LOGGER.debug("Stored browser page in the current scraper session: {}",
                    pageName != null && !pageName.isEmpty() ? pageName : "default");
            
            String content = page.asXml();
            LOGGER.debug("Page loaded successfully, content length: {} chars", content.length());
            
            return new NodeVariable(content);
            
        } catch (IOException e) {
            throw new PluginException("Failed to load page: " + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new PluginException("Interrupted while waiting for page load", e);
        }
    }

    /**
     * Execute JavaScript on loaded page.
     * 
     * <p>Bug #33 Fix: Executes custom JavaScript and returns result.</p>
     */
    private Variable handleExecuteScript(String pageName, int waitTime, DynamicScopeContext context) 
            throws PluginException, InterruptedException {
        LOGGER.info("WebBrowser execute-script (page: {})", pageName);
        
        // Get WebClient
        WebClient webClient = getBrowserSessionState(context).get(pageName);
        if (webClient == null) {
            throw new PluginException("Page '" + (pageName != null ? pageName : "default") + 
                "' not found. Use 'load' action first.");
        }
        
        // Get JavaScript code from body content
        Variable bodyVariable = super.executeBody(context);
        String javascriptCode = bodyVariable != null ? bodyVariable.toString() : "";
        
        if (javascriptCode.trim().isEmpty()) {
            throw new PluginException("JavaScript code is required for execute-script action");
        }
        
        try {
            // Get current page
            HtmlPage page = (HtmlPage) webClient.getCurrentWindow().getEnclosedPage();
            
            // Execute JavaScript
            LOGGER.debug("Executing JavaScript: {}", 
                javascriptCode.substring(0, Math.min(100, javascriptCode.length())));
            Object result = page.executeJavaScript(javascriptCode).getJavaScriptResult();
            
            // Wait for any triggered AJAX
            if (waitTime > 0) {
                LOGGER.debug("Waiting {}ms for AJAX after script execution", waitTime);
                webClient.waitForBackgroundJavaScript(waitTime);
                Thread.sleep(waitTime);
            }
            
            String resultStr = result != null ? result.toString() : "";
            LOGGER.debug("JavaScript execution result: {}", resultStr);
            
            return new NodeVariable(resultStr);
            
        } catch (Exception e) {
            throw new PluginException("Failed to execute JavaScript: " + e.getMessage(), e);
        }
    }

    /**
     * Get current page content (after JavaScript/AJAX).
     * 
     * <p>Bug #33 Fix: Returns fully rendered HTML after all dynamic content loaded.</p>
     */
    private Variable handleGetContent(String pageName, DynamicScopeContext context) throws PluginException {
        LOGGER.info("WebBrowser get-content (page: {})", pageName);
        
        WebClient webClient = getBrowserSessionState(context).get(pageName);
        if (webClient == null) {
            throw new PluginException("Page '" + (pageName != null ? pageName : "default") + 
                "' not found. Use 'load' action first.");
        }
        
        try {
            HtmlPage page = (HtmlPage) webClient.getCurrentWindow().getEnclosedPage();
            String content = page.asXml();
            
            LOGGER.debug("Retrieved content, length: {} chars", content.length());
            
            return new NodeVariable(content);
            
        } catch (Exception e) {
            throw new PluginException("Failed to get content: " + e.getMessage(), e);
        }
    }

    /**
     * Close browser instance.
     */
    private Variable handleClose(String pageName, DynamicScopeContext context) throws PluginException {
        LOGGER.info("WebBrowser close (page: {})", pageName);
        
        BrowserSessionState state = getBrowserSessionState(context);
        WebClient webClient = state.remove(pageName);
        if (webClient != null) {
            webClient.close();
            LOGGER.debug("Closed browser page in the current scraper session: {}",
                    pageName != null && !pageName.isEmpty() ? pageName : "default");
        } else {
            LOGGER.warn("Page '{}' not found, nothing to close", 
                pageName != null ? pageName : "default");
        }
        
        return EmptyVariable.INSTANCE;
    }

    /**
     * Create configured WebClient.
     * 
     * <p>Bug #33 Fix: Configured for JavaScript/AJAX support.</p>
     */
    private WebClient createWebClient(boolean javascriptEnabled) {
        // Use Chrome browser version for best compatibility
        WebClient webClient = new WebClient(BrowserVersion.CHROME);
        
        // Configure JavaScript
        webClient.getOptions().setJavaScriptEnabled(javascriptEnabled);
        
        // Configure AJAX controller
        webClient.setAjaxController(new NicelyResynchronizingAjaxController());
        
        // Disable CSS (faster, not needed for scraping)
        webClient.getOptions().setCssEnabled(false);
        
        // Throw exceptions on failing status codes (404, 500, etc.)
        webClient.getOptions().setThrowExceptionOnFailingStatusCode(true);
        
        // Don't throw exceptions on script errors (many sites have non-critical errors)
        webClient.getOptions().setThrowExceptionOnScriptError(false);
        
        // Disable redirects (we want to handle them explicitly)
        webClient.getOptions().setRedirectEnabled(true);
        
        // Set timeout (30 seconds)
        webClient.getOptions().setTimeout(30000);
        
        LOGGER.debug("Created WebClient (JavaScript: {}, Browser: Chrome)", javascriptEnabled);
        
        return webClient;
    }

    /**
     * Cleanup method - close all open browsers.
     */
    public static void closeAllPages() {
        LOGGER.info("Closing all WebBrowser pages (explicit global shutdown)");
        for (BrowserSessionState state : SESSION_PAGES.values()) {
            state.close();
        }
        SESSION_PAGES.clear();

        synchronized (CONTEXT_PAGES) {
            for (BrowserSessionState state : CONTEXT_PAGES.values()) {
                state.close();
            }
            CONTEXT_PAGES.clear();
        }
        LOGGER.info("All WebBrowser pages closed");
    }
}
