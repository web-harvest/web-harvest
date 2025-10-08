# Web-Harvest

## 🎉 **WEB-BASED IDE - October 2025**

### ✅ **Modern Web UI with Embedded Jetty**

Web-Harvest now features a modern, web-based IDE that runs as a standalone Java application with embedded HTTP server!

**Key Features:**
- 🌐 **Modern Web UI** - Monaco Editor (VS Code engine) for XML editing
- 🚀 **Single JAR Distribution** - Run with `java -jar webharvest-ide-2.2.0.jar`
- 🔄 **Real-time Execution** - WebSocket streaming of logs and progress
- 📊 **Rich Results Display** - Output, Variables, and Log tabs
- 📑 **Multi-tab Editing** - Work with multiple configurations simultaneously
- 🎨 **Professional Design** - Based on project documentation styling

**Quick Start:**
```bash
cd webharvest-ide
java -jar target/webharvest-ide-2.2.0.jar
# Opens automatically at http://localhost:8080
```

---

## 🔧 **Critical Core Improvements - October 2025**

### ✅ **ConfigPlugin Scope Fix**

**Problem:** Variables created by `<def>` elements were executed in nested scopes and cleaned up after configuration completed, making them inaccessible to IDE and other tools.

**Solution:** `ConfigPlugin.executeBody()` now executes children directly in ROOT scope instead of creating nested scopes.

**Impact:**
- ✅ Variables persist after configuration execution
- ✅ IDE can extract and display all variables
- ✅ All 2556 core tests still pass (100% - no regression!)
- ✅ All 4 IDE integration tests pass (100%)

### ✅ **Enhanced Event System**

`ScraperExecutionEndEvent` now includes the execution context, allowing tools to extract variables before scope cleanup.

---

[![Build Status](https://img.shields.io/badge/build-passing-brightgreen.svg)](https://sourceforge.net/projects/web-harvest/)
[![Coverage](https://img.shields.io/badge/coverage-63%25-yellow.svg)](https://sourceforge.net/projects/web-harvest/)
[![License](https://img.shields.io/badge/license-BSD-blue.svg)](http://www.opensource.org/licenses/bsd-license.php)
[![Java](https://img.shields.io/badge/java-11%2B-orange.svg)](https://www.oracle.com/java/)
[![SourceForge](https://img.shields.io/badge/SourceForge-web--harvest-orange.svg)](https://sourceforge.net/projects/web-harvest/)

**Web-Harvest** is an open-source web data extraction tool written in Java. It provides a powerful and flexible framework for collecting web pages and extracting useful data from them using XML-based configuration files.

## 🚀 Features

- **XML-based Configuration**: Define data extraction workflows using simple XML syntax
- **Rich Plugin Ecosystem**: 48+ built-in plugins for various data extraction tasks
- **Multiple Output Formats**: Support for XML, JSON, CSV, and custom formats
- **Scripting Support**: JavaScript, Groovy, and BeanShell integration
- **Database Integration**: NEW! Named connections with automatic connection pooling
  - Define connections once with `<connection id="myDB" .../>`
  - Reuse with `<database connection="myDB" sql="..."/>`
  - Support for multiple concurrent databases
  - Automatic connection pooling for better performance
- **HTTP Client**: Advanced HTTP request handling with authentication
- **XPath/XQuery Support**: Powerful XML querying capabilities
- **Template Engine**: Dynamic content generation and processing
- **Error Handling**: Comprehensive exception handling and logging
- **Extensible Architecture**: Easy plugin development and customization

## 📋 Table of Contents

- [Web-Based IDE](#web-based-ide)
- [Installation](#installation)
- [Quick Start](#quick-start)
- [Architecture](#architecture)
- [Plugin System](#plugin-system)
- [Configuration](#configuration)
- [Testing](#testing)
- [Examples](#examples)
- [API Documentation](#api-documentation)
- [Contributing](#contributing)
- [License](#license)

## 🖥️ Web-Based IDE

### Overview

Web-Harvest IDE is a modern, web-based development environment for creating and testing scraping configurations. It runs as a standalone Java application with an embedded Jetty server.

### Features

- **Monaco Editor** - Professional code editor with XML syntax highlighting and validation
- **Multi-Tab Interface** - Work with multiple configurations simultaneously
- **Real-Time Execution** - Live streaming of logs, progress, and results via WebSocket
- **Results Explorer** - Three-panel view:
  - **Output** - Main execution result (`configResult`)
  - **Variables** - All extracted variables with values
  - **Log** - Real-time execution logs with color coding
- **Example Configurations** - 5 pre-loaded examples to get started quickly
- **Auto-Open Browser** - Automatically opens in default browser on startup

### Running the IDE

```bash
# From project root
cd webharvest-ide
java -jar target/webharvest-ide-2.2.0.jar

# IDE opens automatically at http://localhost:8080
# If port 8080 is busy, it will try ports 8081-8179
```

### IDE Architecture

```
┌─────────────────────────────────────────┐
│  Browser (http://localhost:8080)        │
│  ├─ Monaco Editor (XML editing)         │
│  ├─ REST API (config validation)        │
│  └─ WebSocket (live execution updates)  │
└─────────────────────────────────────────┘
            ↓ HTTP/WebSocket
┌─────────────────────────────────────────┐
│  Embedded Jetty Server (Java)           │
│  ├─ ConfigServlet (/api/config)        │
│  ├─ ExecutionServlet (/api/execute)    │
│  ├─ ExecutionWebSocketServlet (/ws)     │
│  └─ StaticFileHandler (/webapp)         │
└─────────────────────────────────────────┘
            ↓
┌─────────────────────────────────────────┐
│  Web-Harvest Core Engine                │
│  └─ ExecutionManager (thread pool)      │
└─────────────────────────────────────────┘
```

### Implementation Details

- **Backend:** Jetty 11, Gson for JSON, SLF4J logging
- **Frontend:** Vanilla JavaScript, Monaco Editor, Font Awesome icons
- **Communication:** REST API for sync operations, WebSocket for streaming
- **Styling:** Based on `docs/web/` styling with Web-Harvest branding
- **Testing:** 4 professional integration tests with real Jetty WebSocket

---

## 🛠 Installation

### Prerequisites

- Java 8 or higher
- Maven 3.6+ (for building from source)

### Download

Download the latest release from [GitHub Releases](https://github.com/webharvest/webharvest/releases) or build from source:

```bash
git clone https://github.com/webharvest/webharvest.git
cd webharvest
mvn clean install
```

### Maven Dependency

```xml
<dependency>
    <groupId>net.sourceforge.web-harvest</groupId>
    <artifactId>webharvest-core</artifactId>
    <version>2.1.0</version>
</dependency>
```

## 🚀 Quick Start

### Basic Example

Create a simple configuration file `example.xml`:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<config>
    <http url="https://example.com">
        <xpath expression="//title" var="title"/>
        <xpath expression="//p" var="paragraphs"/>
    </http>
    <file path="output.xml">
        <template>
            <results>
                <title>${title}</title>
                <content>${paragraphs}</content>
            </results>
        </template>
    </file>
</config>
```

Run the scraper:

```bash
java -jar webharvest-cli.jar example.xml
```

### Programmatic Usage

```java
import org.webharvest.runtime.Scraper;
import org.webharvest.runtime.ScraperContext;
import org.webharvest.definition.Config;

// Load configuration
Config config = Config.fromFile("example.xml");

// Create scraper context
ScraperContext context = new ScraperContext();

// Execute scraping
Scraper scraper = new Scraper(config);
scraper.execute(context);

// Get results
String result = context.getVar("title").toString();
```

## 🏗 Architecture

Web-Harvest follows a modular, plugin-based architecture:

```
webharvest/
├── webharvest-core/          # Core framework and plugins
├── webharvest-cli/           # Command-line interface
├── webharvest-ide/           # IDE integration
└── docs/                     # Documentation
```

### Core Components

- **Plugin System**: Extensible plugin architecture with 47+ built-in plugins
- **Configuration Engine**: XML-based configuration parsing and validation
- **Runtime Engine**: Execution engine with context management
- **Variable System**: Dynamic variable handling and scoping
- **Template Engine**: Dynamic content generation
- **Scripting Engine**: JavaScript, Groovy, BeanShell support

## 🔌 Plugin System

Web-Harvest provides a rich set of built-in plugins:

### Core Plugins
- **HTTP Plugin**: Web page retrieval and HTTP operations
- **XPath Plugin**: XML/HTML data extraction using XPath
- **XQuery Plugin**: Advanced XML querying
- **Template Plugin**: Dynamic content generation
- **Script Plugin**: JavaScript/Groovy execution
- **File Plugin**: File I/O operations

### Data Processing Plugins
- **Regexp Plugin**: Regular expression matching
- **XML Plugin**: XML processing and transformation
- **JSON Plugin**: JSON processing and conversion
- **Text Plugin**: Text manipulation and processing

### Control Flow Plugins
- **If Plugin**: Conditional execution
- **While Plugin**: Loop execution
- **Try/Catch Plugin**: Exception handling
- **Function Plugin**: Function definition and calls

### Database Plugins
- **Database Plugin**: SQL database operations
- **Connection Management**: Database connection handling

### Utility Plugins
- **Variable Plugin**: Variable management
- **Sleep Plugin**: Execution delays
- **Exit Plugin**: Execution termination
- **Config Plugin**: Configuration management

## 📝 Configuration

### Configuration Schema

Web-Harvest uses XML Schema for configuration validation:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<config xmlns="http://org.webharvest/schema/2.1/core">
    <!-- Plugin configurations -->
</config>
```

### Plugin Configuration

Each plugin supports specific attributes and body content:

```xml
<http url="https://example.com" method="GET" timeout="30000">
    <http-header name="User-Agent" value="Web-Harvest/2.1.0"/>
    <http-param name="format" value="json"/>
    <xpath expression="//title" var="title"/>
</http>
```

### Variable Usage

Variables can be referenced using `${variableName}` syntax:

```xml
<template>
    <result>
        <title>${title}</title>
        <timestamp>${currentTime}</timestamp>
    </result>
</template>
```

## 🧪 Testing

### Test Coverage

Web-Harvest has comprehensive test coverage across all modules:

- **webharvest-core**: 2556 tests, 50%+ coverage
  - Unit tests for all core plugins
  - Integration tests using XML configurations
  - Metadata and validation tests
  
- **webharvest-database**: Integration tests for database plugins
- **webharvest-webbrowser**: Integration tests for browser automation
- **webharvest-ide**: 4 professional WebSocket integration tests

### Running Tests

```bash
# Run all tests
mvn test

# Run tests for specific module
cd webharvest-core
mvn test

# Run with coverage report
mvn clean test jacoco:report
# Report: target/site/jacoco/index.html
```

### IDE Integration Tests

The IDE has professional integration tests that verify:
- Variable extraction from configurations
- WebSocket communication
- Log streaming
- Execution lifecycle

```bash
cd webharvest-ide
mvn test
# Tests: ExecutionManagerWebSocketTest
```

**Test Results:**
```
✓ testSimpleDefVariables - Variable extraction
✓ testXPathPluginVariables - XPath processing
✓ testMultiVariablePipeline_CRITICAL - Full pipeline
✓ testLogStreaming - Log delivery
```

---

## 📚 Examples

### Web Scraping Example

```xml
<config>
    <http url="https://news.ycombinator.com">
        <xpath expression="//a[@class='storylink']" var="links"/>
        <xpath expression="//span[@class='score']" var="scores"/>
    </http>
    
    <file path="hackernews.xml">
        <template>
            <news>
                <#foreach link in links>
                <item>
                    <title>${link.text}</title>
                    <url>${link.@href}</url>
                    <score>${scores[link.index].text}</score>
                </item>
                </#foreach>
            </news>
        </template>
    </file>
</config>
```

### Database Integration Example

```xml
<config>
    <database connection="jdbc:mysql://localhost:3306/test" 
               jdbcclass="com.mysql.jdbc.Driver"
               username="user" password="pass">
        SELECT * FROM users WHERE active = 1
    </database>
    
    <http url="https://api.example.com/users" method="POST">
        <template>
            <users>
                <#foreach user in databaseResult>
                <user>
                    <id>${user.id}</id>
                    <name>${user.name}</name>
                    <email>${user.email}</email>
                </user>
                </#foreach>
            </users>
        </template>
    </http>
</config>
```

### Scripting Example

```xml
<config>
    <script language="javascript">
        var data = [];
        for (var i = 0; i < 10; i++) {
            data.push({
                id: i,
                value: Math.random() * 100
            });
        }
        context.setVar("generatedData", data);
    </script>
    
    <file path="output.json">
        <template>${generatedData}</template>
    </file>
</config>
```

## 📖 API Documentation

### Core Classes

#### `AbstractPlugin`
Base class for all plugins providing common functionality:

```java
public abstract class AbstractPlugin {
    protected abstract Variable doExecute(DynamicScopeContext context) 
        throws PluginException, InterruptedException;
    
    protected abstract void doValidateConfiguration(PluginConfiguration config) 
        throws PluginValidationException;
}
```

#### `DynamicScopeContext`
Runtime context for variable management and execution state:

```java
public class DynamicScopeContext {
    public void setVar(String name, Variable value);
    public Variable getVar(String name);
    public String getCharset();
}
```

#### `PluginConfiguration`
Configuration management for plugins:

```java
public interface PluginConfiguration {
    String getProperty(String name);
    String getProperty(String name, String defaultValue);
    void setProperty(String name, String value);
}
```

### Plugin Development

To create a custom plugin:

1. Extend `AbstractPlugin`
2. Implement required methods
3. Add `@CorePlugin` annotation
4. Define configuration schema

```java
@CorePlugin(elementName = "myplugin")
public class MyPlugin extends AbstractPlugin {
    
    @Override
    protected Variable doExecute(DynamicScopeContext context) 
            throws PluginException, InterruptedException {
        // Plugin implementation
        return new NodeVariable("result");
    }
    
    @Override
    protected void doValidateConfiguration(PluginConfiguration config) 
            throws PluginValidationException {
        // Configuration validation
    }
}
```

## 🤝 Contributing

We welcome contributions! Please see our [Contributing Guidelines](CONTRIBUTING.md) for details.

### Development Setup

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Add tests
5. Submit a pull request

### Code Quality

- Maintain 80%+ test coverage
- Follow Java coding standards
- Add Javadoc for public APIs
- Update documentation as needed

## 📄 License

This project is licensed under the BSD License - see the [LICENSE](LICENSE) file for details.

## 📞 Support

- **Documentation**: [Wiki](https://github.com/webharvest/webharvest/wiki)
- **Issues**: [GitHub Issues](https://github.com/webharvest/webharvest/issues)
- **Discussions**: [GitHub Discussions](https://github.com/webharvest/webharvest/discussions)

## 🙏 Acknowledgments

- **Original Author**: Vladimir Nikic
- **Contributors**: Alexander Wajda, Robert Bala, Piotr Dyraga, Maciej Czapiewski
- **Community**: All contributors and users who help improve Web-Harvest

---

**Web-Harvest** - Making web data extraction simple and powerful! 🕷️✨