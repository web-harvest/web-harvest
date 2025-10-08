#!/bin/bash

# Template footer content
FOOTER_TEMPLATE='    <footer class="footer">
        <div class="container">
            <div class="footer-content">
                <div class="footer-section">
                    <h3>Web-Harvest</h3>
                    <p>Advanced web scraping and data extraction tool for Java applications.</p>
                    <div class="social-links">
                        <a href="https://github.com/webharvest" target="_blank" rel="noopener"><i
                                class="fab fa-github"></i></a>
                        <a href="https://twitter.com/webharvest" target="_blank" rel="noopener"><i
                                class="fab fa-twitter"></i></a>
                        <a href="https://www.linkedin.com/in/rbala/" target="_blank" rel="noopener"><i
                                class="fab fa-linkedin"></i></a>
                    </div>
                </div>
                <div class="footer-section">
                    <h4>Documentation</h4>
                    <ul>
                        <li><a href="../../../api/org/webharvest/runtime/processors/package-summary.html">Processors</a></li>
                        <li><a href="../../../api/org/webharvest/runtime/RuntimeConfig.html">Configuration</a></li>
                        <li><a href="../../../examples/">Examples</a></li>
                        <li><a href="../../../api/org/webharvest/exception/package-summary.html">Error Handling</a></li>
                    </ul>
                </div>
                <div class="footer-section">
                    <h4>Downloads</h4>
                    <ul>
                        <li><a href="https://sourceforge.net/projects/web-harvest/files/webhervest/2.1.0/"
                                target="_blank" rel="noopener">Version 2.1.0 (Latest)</a></li>
                        <li><a href="https://sourceforge.net/projects/web-harvest/files/webhervest/v1.0/"
                                target="_blank" rel="noopener">Version 1.0</a></li>
                        <li><a href="https://sourceforge.net/projects/web-harvest/files/webhervest/" target="_blank"
                                rel="noopener">All Versions</a></li>
                        <li><a href="https://sourceforge.net/projects/web-harvest/" target="_blank"
                                rel="noopener">SourceForge Project</a></li>
                    </ul>
                </div>
                <div class="footer-section">
                    <h4>Resources</h4>
                    <ul>
                        <li><a href="../../../index.html#download">Quick Download</a></li>
                        <li><a href="../../../examples/">Examples</a></li>
                        <li><a href="#">Tutorials</a></li>
                        <li><a href="#">Support</a></li>
                    </ul>
                </div>
                <div class="footer-section">
                    <h4>Legal</h4>
                    <ul>
                        <li><a href="#">License</a></li>
                        <li><a href="#">Privacy Policy</a></li>
                        <li><a href="#">Terms of Service</a></li>
                    </ul>
                </div>
            </div>
            <div class="footer-bottom">
                <p>&copy; 2025 Web-Harvest. All rights reserved.</p>
            </div>
        </div>
    </footer>'

# Update remaining files
for file in docs/web/processors/2.2.0/individual/*.html; do
  if [ "$(basename "$file")" != "index.html" ] && [ "$(basename "$file")" != "web-browser.html" ] && [ "$(basename "$file")" != "http.html" ] && [ "$(basename "$file")" != "html-to-xml.html" ] && [ "$(basename "$file")" != "xpath.html" ]; then
    echo "Updating footer in: $file"
    # Replace footer section
    sed -i.bak '/<footer class="footer">/,/<\/footer>/c\
'"$FOOTER_TEMPLATE" "$file"
  fi
done

echo "Footer update completed!"
