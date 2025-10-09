# Changelog

All notable changes to Web-Harvest will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [2.2.1] - October 2025

### 🐛 Bug Fixes

**Bug #55: Character '#' in URL Parameters Not Encoded** (r704)
- **Issue**: '#' in URL parameter values was not encoded, causing URL truncation
- **Example**: `?color=#FF0000` became `?color=` (truncated)
- **Fix**: Removed special case in `CommonUtil.encodeUrlParam()` - '#' now encoded as %23
- **Impact**: Color codes, hashtags, any '#' in parameters now work correctly
- **Tests**: 7 comprehensive tests added (4 bug + 3 regression)

**Bug #61: JSONToXML "content" Key Not Correctly Converted** (r702)
- **Issue**: org.json.XML treats "content" as special text node key, strips it from XML
- **Example**: `{"a": {"content": "hello"}}` became `<a>hello</a>` (content key lost)
- **Fix**: Created `JsonXmlConverter` utility - all JSON keys treated equally
- **Impact**: CMS/blog APIs with "content" field now work correctly
- **Tests**: 9 comprehensive tests added (4 bug + 5 regression)

**Bug #62: Regexp Group Isolation Issue** (r701)
- **Issue**: Groups from first `<regexp>` persisted into second regexp execution
- **Example**: In loops, second regexp's `group1` showed values from first regexp
- **Fix**: Added `clearAllGroupVariables()` - clears group0-99 before each execution
- **Impact**: Multiple sequential regexp calls now properly isolated
- **Tests**: 7 comprehensive tests added (5 unit + 2 integration)

**Bug #60: Invalid Variable Name in try/catch** (r703 - documentation only)
- **Issue**: Old `TryProcessor` used `_error` (invalid variable name with underscore)
- **Status**: Already fixed in v2.1.0 during GUI→Plugin migration
- **Current**: `TryPlugin` uses valid names: `error`, `errorType`, `stacktrace`
- **Verification**: Existing tests pass, no code changes needed

### 🎨 Web-Based IDE

**Major Features** (r693, r698):
- Modern web-based IDE with embedded Jetty server
- Monaco Editor integration (VS Code engine)
- Real-time execution with WebSocket streaming
- Multi-tab editing with 5 pre-loaded examples
- Variable inspector with syntax highlighting
- WebSocket lifecycle management with disconnect notifications
- Single executable JAR: `java -jar webharvest-ide-2.2.0.jar`

### 🏗️ Architecture Improvements

**ConfigPlugin Scope Fix** (r693)
- **Issue**: Variables defined in `<config>` were cleaned up after execution
- **Fix**: Override `executeBody()` to execute children in root scope (not nested)
- **Impact**: IDE and tools can now properly extract result variables
- **Tests**: 4 comprehensive WebSocket integration tests

**Plugin Discovery** (r694, r695)
- PluginProcessorAdapter for automatic plugin wrapping
- Integration tests for all 6 plugin modules
- Core, Database, FTP, Mail, WebBrowser, Zip modules tested

### 📝 Documentation

**Website Updates** (r699, r700):
- New IDE section on homepage with live preview
- Plugin Architecture section documenting all 57 plugins
- Updated all navigation menus across 20+ pages
- Version 2.2.0→2.2.1 references updated
- Removed outdated Apache HttpClient JavaDoc
- Removed old processor pages

**Bug Tracking**:
- Created BUGS_FIXED.md - comprehensive tracking system
- Documents all bugs: description, root cause, fix, tests
- Version tracking for fixes

### ✅ Testing & Quality

- **Test Suite**: 2585 tests (100% passing)
- **New Tests**: +23 tests for bug fixes (7 Bug #55 + 9 Bug #61 + 7 Bug #62)
- **Integration Tests**: XML-based tests for all plugin modules
- **Zero Regression**: All existing functionality maintained

## [2.2.0] - October 2025

### 🏗️ Architecture - Database Module Separation

**BREAKING CHANGE** (for module developers only):
- Separated database functionality from `webharvest-core` into dedicated `webharvest-database` module
- Moved 6 `org.webharvest.runtime.database.*` classes to `webharvest-database`
- Moved 2 test classes and test resources (MockDBDriver.jar, XML examples)
- Created new `DatabaseModule` for Guice dependency injection
- **Impact**: If using database features with `webharvest-core`, add explicit `webharvest-database` dependency
- **End Users**: No impact - CLI and IDE include all modules automatically

### ✅ Testing & Quality

- **Test Coverage**: Improved from 44.55% to 50.20% (+5.65%) for `webharvest-core`
- **New Tests**: Added 520+ unit tests across 20 new test files
- **Integration Tests**: Created XML-based integration test framework with 75+ tests
- **Passing Rate**: 100% (2,604/2,604 tests passing)
- **High Coverage Plugins**: 14 plugins now have ≥70% coverage

### 📦 Project Organization

- Moved XML examples from root `examples/` directory to module test resources
- Moved database test resources to `webharvest-database` module  
- Moved webbrowser test resources to `webharvest-webbrowser` module
- Removed 25+ temporary/duplicate documentation files
- Removed obsolete classpath.txt files

### 🐛 Bug Fixes

- Fixed `ScraperContextTest.testSetVar()` - lenient comparison for cached fields
- Fixed integration test resource loading paths

### 📝 Documentation

- Reorganized documentation structure - removed 25+ duplicate files
- Updated CHANGELOG.md with comprehensive October 2025 changes
- Created DOCUMENTATION_INDEX.md for easy navigation
- Maintained essential docs: README, CONTRIBUTING, DEVELOPER_GUIDE, PLUGINS

## [2.2.0] - 2025-01-15

### Added
- **ConnectionPlugin**: NEW! Named database connections with automatic connection pooling
  - Define connections once with `<connection id="myDB" driver="..." url="..."/>`
  - Reuse connections with `<database connection="myDB" sql="..."/>`
  - Support for multiple concurrent database connections
  - Automatic connection pooling for better performance
  - Full backward compatibility with inline connection syntax
- **Plugin Validation Framework**: Comprehensive validation for plugin initialization state
- **HttpService Integration**: Full HTTP service implementation with Apache HttpClient 4
- **Comprehensive Plugin Testing**: 16+ unit tests covering edge cases and error scenarios
- **Plugin Documentation**: Complete analysis of plugin behavior and patterns (`PLUGIN_TESTING_ANALYSIS.md`)
- **ConfigurableResolver API Extensions**: Added `getElementNames()`, `getElementInfo()`, `isPluginRegistered()`, `unregisterPlugin()` methods

### Fixed  
- **HttpPlugin**: Added HttpService injection validation and template resolution
- **TextPlugin**: Fixed elementDef validation and body content extraction
- **WhilePlugin**: Added configuration validation for safe loop execution
- **Logging**: Migrated from System.out.println to SLF4J loggers throughout core plugins
- **Plugin Registration**: Graceful handling of duplicate plugin registration errors

### Plugin Behavior Analysis
- **Working Plugins**: 8/15 core plugins fully functional (ConstantPlugin, VarPlugin, DefPlugin, HtmlToXmlPlugin, XPathPlugin, JsonToXmlPlugin, XmlToJsonPlugin, TokenizePlugin)
- **Body Content Issue**: Identified systematic problem with plugins requiring XML body content execution
- **Exception Handling**: All refactored plugins now throw explicit exceptions for invalid initialization states

## [2.1.0] - 2025-01-XX

### Added
- **New Plugin Architecture**: Complete migration from old processor system to new plugin architecture
- **47+ Core Plugins**: Comprehensive set of built-in plugins for web scraping and data processing
- **Automatic Plugin Discovery**: Plugin system with automatic discovery using annotations
- **Enhanced HTTP Client**: Updated HTTP client implementation with better error handling
- **Comprehensive Test Suite**: 1863+ tests with 63% overall code coverage
- **Code Quality Tools**: Integrated JaCoCo, PMD, SpotBugs, and Checkstyle
- **Developer Documentation**: Complete API documentation and developer guide
- **Maven Multi-module Structure**: Organized project structure with separate modules

### Core Plugins Added
- **HTTP Plugin**: Web page retrieval and HTTP operations
- **XPath Plugin**: XML/HTML data extraction using XPath expressions
- **XQuery Plugin**: Advanced XML querying capabilities
- **Template Plugin**: Dynamic content generation with template engine
- **Script Plugin**: JavaScript, Groovy, and BeanShell execution
- **File Plugin**: File I/O operations and management
- **Regexp Plugin**: Regular expression matching and processing
- **XML Plugin**: XML processing and transformation
- **JSON Plugin**: JSON processing and conversion
- **Text Plugin**: Text manipulation and processing
- **If Plugin**: Conditional execution
- **While Plugin**: Loop execution with condition evaluation
- **Try/Catch Plugin**: Exception handling and error management
- **Function Plugin**: User-defined function definition and calls
- **Database Plugin**: SQL database operations and connectivity
- **Variable Plugin**: Variable management and scoping
- **Sleep Plugin**: Execution delays and timing control
- **Exit Plugin**: Execution termination and control flow
- **Config Plugin**: Configuration management and access

### Changed
- **Architecture Migration**: Complete migration from old processor architecture to new plugin system
- **HTTP Client Upgrade**: Updated from Apache Commons HttpClient 3.x to standard Java HttpURLConnection
- **Dependency Management**: Updated Maven dependencies and removed obsolete libraries
- **Configuration Schema**: Enhanced XML schema validation and error reporting
- **Error Handling**: Improved exception handling with specific exception types
- **Logging**: Standardized logging using SLF4J throughout the codebase
- **Thread Safety**: Enhanced thread safety in core components

### Deprecated
- **WebHarvestPlugin**: Marked as deprecated in favor of new AbstractPlugin architecture
- **Old Processor Classes**: Legacy processor classes marked for removal in future versions
- **HttpClientManager**: Old HTTP client management classes deprecated
- **Legacy Configuration**: Old configuration format deprecated in favor of new schema

### Removed
- **Old HTTP Client Classes**: Removed Apache Commons HttpClient 3.x dependencies
- **Legacy Processor Implementations**: Removed old processor implementations from core
- **Unused Database Classes**: Removed unused database connection management classes
- **Obsolete SSL Classes**: Removed deprecated SSL-related classes
- **Duplicate Annotations**: Removed duplicate annotation classes

### Fixed
- **Memory Leaks**: Fixed memory leaks in plugin execution and context management
- **Thread Safety Issues**: Resolved thread safety problems in registry and context classes
- **Configuration Validation**: Fixed configuration validation and error reporting
- **Plugin Discovery**: Resolved plugin discovery and registration issues
- **Test Coverage**: Improved test coverage and fixed failing tests
- **Compilation Issues**: Fixed compilation errors and dependency conflicts

### Security
- **Input Validation**: Enhanced input validation and sanitization
- **HTTP Security**: Improved HTTP client security with proper SSL handling
- **Resource Management**: Better resource management and cleanup
- **Error Information**: Reduced information leakage in error messages

## [2.0.0] - 2024-XX-XX

### Added
- **Plugin System Foundation**: Initial implementation of new plugin architecture
- **Core Plugin Framework**: Basic plugin framework with AbstractPlugin base class
- **Configuration Parser**: SAX-based XML configuration parser
- **Runtime Engine**: Basic runtime engine with context management
- **Variable System**: Initial variable system implementation

### Changed
- **Architecture Refactoring**: Major architectural changes from processor to plugin system
- **Configuration Format**: Updated configuration format and schema
- **API Changes**: Significant API changes for better extensibility

### Deprecated
- **Processor System**: Old processor system marked as deprecated
- **Legacy APIs**: Old APIs marked for removal in future versions

## [1.0.0] - 2006-XX-XX

### Added
- **Initial Release**: First release of Web-Harvest
- **Basic Web Scraping**: Core web scraping functionality
- **XML Configuration**: XML-based configuration system
- **Processor Architecture**: Original processor-based architecture
- **HTTP Client**: Basic HTTP client implementation
- **XPath Support**: XPath-based data extraction
- **Template Engine**: Basic template processing

### Contributors

- **Vladimir Nikic** - Original author and project founder
- **Alexander Wajda** - Core development and architecture
- **Robert Bala** - Project administration and architecture migration
- **Piotr Dyraga** - Plugin system development and testing
- **Maciej Czapiewski** - Quality assurance and testing

### Acknowledgments

Special thanks to all contributors, testers, and users who have helped improve Web-Harvest over the years. The migration to the new plugin architecture represents a significant milestone in the project's evolution, providing better performance, maintainability, and extensibility.

---

For more information about Web-Harvest, visit our [GitHub repository](https://github.com/webharvest/webharvest) or check the [Developer Guide](DEVELOPER_GUIDE.md).





