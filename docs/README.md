# Web-Harvest 2.2.0 Documentation

This directory contains the complete documentation for Web-Harvest 2.2.0, including:

- **Web Documentation** (`web/`) - Interactive HTML documentation with modern design
- **API Documentation** - Generated from source code using Maven
- **Plugin Reference** - Complete reference for all 54 plugins (46 core + 8 external)
- **Examples** - Real-world configuration examples
- **User Guide** - Step-by-step tutorials and guides
- **Function Execution Guide** (`FUNCTION_EXECUTION.md`) - Comprehensive guide to function definition, calling, parameters, return values, and system variables
- **Validation Rules** (`VALIDATION_RULES.md`) - Configuration validation rules, context-specific plugins, and special cases
- **Property Editor Guide** (`PROPERTY_EDITOR_GUIDE.md`) - User guide for the Property Editor with Apply/Cancel pattern, visual indicators, and keyboard shortcuts

## Quick Start

### Viewing the Documentation

1. **Web Documentation**: Open `web/index.html` in your browser
2. **Getting Started**: See `web/documentation/getting-started.html`
3. **Plugin Reference**: See `web/documentation/2.2.0/index.html`
4. **Architecture**: See `web/documentation/architecture.html`

### Building the Documentation

```bash
# Generate complete documentation
mvn clean site

# Generate and package documentation
mvn clean package

# Deploy to FTP server (configure FTP settings in pom.xml)
mvn clean package -Pdeploy-ftp
```

## Documentation Structure

```
docs/
├── web/                          # Interactive web documentation
│   ├── index.html               # Main landing page
│   ├── css/                     # Stylesheets
│   ├── js/                      # JavaScript files
│   ├── images/                  # Images and graphics
│   ├── documentation/           # Documentation hub
│   │   ├── index.html           # Documentation index
│   │   ├── getting-started.html # Getting started tutorial
│   │   ├── architecture.html    # System architecture
│   │   └── 2.2.0/               # Plugin documentation v2.2.0
│   │       ├── index.html       # 54 plugins index
│   │       ├── *.html           # 46 core plugin pages
│   │       └── external/        # 8 external plugin pages
│   ├── examples/                # Example configurations
│   └── api/                     # API documentation (placeholder)
├── FUNCTION_EXECUTION.md        # Function execution and system variables guide
├── scripts/                      # Documentation generation scripts
│   ├── generate_all_plugin_docs.py       # Master script (runs all)
│   ├── generate_core_plugin_docs.py      # Core plugins (46 files)
│   ├── generate_external_plugin_docs.py  # External plugins (8 files)
│   └── generate_plugins_index.py         # Plugin index page
├── src/main/java/               # Documentation generators (Maven)
├── src/main/assembly/           # Maven assembly descriptors
├── pom.xml                      # Maven configuration
└── README.md                    # This file
```

## Features

### Web Documentation
- **Modern Design**: Responsive, mobile-friendly interface
- **Interactive Examples**: Copy-to-clipboard code examples
- **Search & Filter**: Find processors and examples quickly
- **Syntax Highlighting**: Beautiful code syntax highlighting
- **Dark Mode**: Optional dark theme support

### Plugin Reference
- **Complete Coverage**: All 54 plugins documented (46 core + 8 external)
- **Attribute Details**: Full attribute descriptions and types
- **Code Examples**: Real-world usage examples
- **Categories**: Organized by functionality (Critical, Important, Common, Advanced, External)
- **Auto-generated**: Generated from JavaDoc using Python scripts

### Examples
- **Real-world Scenarios**: Business-focused examples
- **Copy & Paste**: Ready-to-use configurations
- **Multiple Categories**: E-commerce, analytics, automation
- **Progressive Complexity**: From simple to advanced

## Customization

### Adding New Plugins
1. Add plugin class with `@CorePlugin` or `@Definition` annotation
2. Run `python3 scripts/generate_all_plugin_docs.py` to regenerate documentation
3. Documentation is auto-generated from JavaDoc comments

**Generating Plugin Documentation:**
```bash
# From docs/ directory
cd scripts
python3 generate_all_plugin_docs.py

# Or from project root
python3 docs/scripts/generate_all_plugin_docs.py
```

This will generate:
- 46 core plugin pages in `web/documentation/2.2.0/*.html`
- 8 external plugin pages in `web/documentation/2.2.0/external/*.html`
- Main plugin index in `web/documentation/2.2.0/index.html`

### Adding New Examples
1. Create XML configuration in `../examples/`
2. Add example card in `web/examples/index.html`
3. Update example metadata and tags

### Styling
- Modify `web/css/enterprise.css` for main styles
- Modify `web/css/prism.css` for syntax highlighting
- Use CSS custom properties for easy theming

## Deployment

### Local Development
```bash
# Serve documentation locally
cd web
python -m http.server 8000
# Open http://localhost:8000
```

### FTP Deployment
1. Configure FTP settings in `pom.xml`:
```xml
<properties>
    <ftp.server>your-ftp-server.com</ftp.server>
    <ftp.username>your-username</ftp.username>
    <ftp.password>your-password</ftp.password>
    <ftp.remotedir>/public_html/docs</ftp.remotedir>
</properties>
```

2. Deploy:
```bash
mvn clean package -Pdeploy-ftp
```

### Web Server Deployment
1. Build documentation:
```bash
mvn clean site
```

2. Copy `target/site/` contents to your web server
3. Ensure proper MIME types for `.html` files

## Contributing

### Adding Documentation
1. Fork the repository
2. Create a feature branch
3. Add your documentation
4. Test with `mvn clean site`
5. Submit a pull request

### Documentation Standards
- Use clear, concise language
- Include code examples
- Test all examples
- Follow the existing style guide
- Update this README if needed

## Troubleshooting

### Common Issues

**Maven site generation fails:**
- Ensure all dependencies are available
- Check Java version compatibility
- Verify Maven configuration

**Web documentation not loading:**
- Check file paths and permissions
- Ensure web server is configured correctly
- Verify CSS and JS files are accessible

**Plugin documentation missing:**
- Run `python3 scripts/generate_all_plugin_docs.py` from docs/ directory
- Check plugin JavaDoc comments
- Verify Python scripts in `docs/scripts/` directory

### Getting Help

- Check the main project README
- Review Maven logs for errors
- Open an issue on GitHub
- Contact the development team

## License

This documentation is part of Web-Harvest and is licensed under the Apache License 2.0.

## Version History

- **2.2.0** - Current version with modernized plugin architecture and comprehensive documentation
- **2.1.0** - Previous version with enhanced documentation
- **2.0.x** - Earlier versions with basic documentation

---

For more information about Web-Harvest, visit the main project repository or the official website.





































