# Web-Harvest 2.1.0 Documentation

This directory contains the complete documentation for Web-Harvest 2.1.0, including:

- **Web Documentation** (`web/`) - Interactive HTML documentation with modern design
- **API Documentation** - Generated from source code using Maven
- **Processor Reference** - Complete reference for all processors
- **Examples** - Real-world configuration examples
- **User Guide** - Step-by-step tutorials and guides

## Quick Start

### Viewing the Documentation

1. **Web Documentation**: Open `web/index.html` in your browser
2. **API Documentation**: Run `mvn site` to generate API docs
3. **Processor Reference**: See `web/processors/index.html`

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
│   ├── index.html               # Main documentation page
│   ├── css/                     # Stylesheets
│   ├── js/                      # JavaScript files
│   ├── images/                  # Images and graphics
│   ├── processors/              # Processor reference
│   ├── examples/                # Example configurations
│   └── api/                     # API documentation
├── src/main/java/               # Documentation generators
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

### Processor Reference
- **Complete Coverage**: All 50+ processors documented
- **Attribute Details**: Full attribute descriptions and types
- **Code Examples**: Real-world usage examples
- **Categories**: Organized by functionality
- **Auto-generated**: Generated from source code annotations

### Examples
- **Real-world Scenarios**: Business-focused examples
- **Copy & Paste**: Ready-to-use configurations
- **Multiple Categories**: E-commerce, analytics, automation
- **Progressive Complexity**: From simple to advanced

## Customization

### Adding New Processors
1. Add processor class with `@Definition` annotation
2. Run `mvn clean site` to regenerate documentation
3. Update processor reference if needed

### Adding New Examples
1. Create XML configuration in `../examples/`
2. Add example card in `web/examples/index.html`
3. Update example metadata and tags

### Styling
- Modify `web/css/style.css` for main styles
- Modify `web/css/processors.css` for processor-specific styles
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

**Processor reference missing:**
- Run `mvn clean compile` first
- Check processor annotations
- Verify classpath includes all modules

### Getting Help

- Check the main project README
- Review Maven logs for errors
- Open an issue on GitHub
- Contact the development team

## License

This documentation is part of Web-Harvest and is licensed under the Apache License 2.0.

## Version History

- **2.1.0-SNAPSHOT** - Current version with enhanced documentation
- **2.0.x** - Previous versions with basic documentation

---

For more information about Web-Harvest, visit the main project repository or the official website.



