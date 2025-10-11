# Contributing to WebHarvest

Thank you for your interest in contributing to WebHarvest! This document provides guidelines and information for contributors.

## Table of Contents

1. [Code of Conduct](#code-of-conduct)
2. [Getting Started](#getting-started)
3. [Development Setup](#development-setup)
4. [Contributing Process](#contributing-process)
5. [Code Standards](#code-standards)
6. [Testing Guidelines](#testing-guidelines)
7. [Documentation](#documentation)
8. [Release Process](#release-process)

## Code of Conduct

This project adheres to a code of conduct. By participating, you are expected to uphold this code. Please report unacceptable behavior to the project maintainers.

## Getting Started

### Prerequisites

- **Java 8 or higher**: Required for development
- **Maven 3.6+**: Required for building the project
- **SVN Client**: Required for version control (TortoiseSVN, command-line svn, etc.)
- **IDE**: IntelliJ IDEA, Eclipse, or VS Code recommended

### Checkout from SourceForge

1. Check out the repository from SourceForge SVN:
```bash
svn checkout https://svn.code.sf.net/p/web-harvest/code/trunk web-harvest
cd web-harvest
```

2. Or if you have commit access:
```bash
svn checkout svn+ssh://USERNAME@svn.code.sf.net/p/web-harvest/code/trunk web-harvest
```

## Development Setup

### Building the Project

```bash
# Clean and build all modules
mvn clean install

# Build specific module
mvn clean install -pl webharvest-core

# Skip tests (for faster builds during development)
mvn clean install -DskipTests
```

### Running Tests

```bash
# Run all tests
mvn test

# Run tests for specific module
mvn test -pl webharvest-core

# Run tests with coverage report
mvn clean test jacoco:report
```

### IDE Setup

#### IntelliJ IDEA

1. Import the project as a Maven project
2. Configure Java SDK to Java 8 or higher
3. Enable annotation processing
4. Configure code style settings (use project .editorconfig if available)

#### Eclipse

1. Import as Maven project
2. Install M2Eclipse plugin if not already installed
3. Configure Java compiler to 1.8 or higher
4. Enable annotation processing

## Contributing Process

### 1. Find or Create an Issue

- Check existing issues on SourceForge tracker
- Create a new ticket if needed
- Discuss your approach with maintainers

### 2. Make Your Changes

1. Update your working copy:
```bash
svn update
```

2. Create your changes following the code standards below

3. Test your changes:
```bash
mvn clean test
```

4. Ensure code quality:
```bash
mvn clean verify
```

### 3. Commit Your Changes

```bash
# Check what you've changed
svn status

# Add new files if any
svn add path/to/new/file

# Commit with clear message in English
svn commit -m "Add: Brief description of changes

Detailed explanation of what was changed and why.

Fixes: #ticket-number (if applicable)"
```

### 4. Submit Patch (if you don't have commit access)

If you don't have direct commit access:

1. Create a patch file:
```bash
svn diff > my-changes.patch
```

2. Submit the patch to SourceForge:
   - Go to https://sourceforge.net/p/web-harvest/tickets/
   - Create new ticket with your patch attached
   - Describe your changes

## Code Standards

### Java Code Style

- **Indentation**: 4 spaces (no tabs)
- **Line length**: Maximum 120 characters
- **Encoding**: UTF-8
- **Line endings**: LF (Unix style)

### Code Quality

All code must pass:

```bash
# PMD static analysis
mvn pmd:check

# SpotBugs analysis
mvn spotbugs:check

# Checkstyle
mvn checkstyle:check
```

### Naming Conventions

- **Classes**: PascalCase (e.g., `DatabasePlugin`)
- **Methods**: camelCase (e.g., `executeQuery()`)
- **Constants**: UPPER_SNAKE_CASE (e.g., `MAX_RETRIES`)
- **Packages**: lowercase (e.g., `org.webharvest.plugin`)

### Documentation

- All public APIs must have Javadoc
- Include `@since` tag for new features
- Document parameters and return values
- Provide usage examples where appropriate

### Language Requirements

- **Code**: All code, comments, and identifiers in English
- **Documentation**: All .md files in English
- **Commit Messages**: All SVN commit messages in English
- **Javadoc**: All API documentation in English

## Testing Guidelines

### Test Requirements

All contributions must include appropriate tests:

1. **Unit Tests**: For individual classes and methods
2. **Integration Tests**: For plugin functionality using XML configurations
3. **Test Coverage**: Aim for ≥70% for new code (50%+ is acceptable for legacy code)

### Writing Tests

```java
@Test
public void testFeatureName() {
    // Arrange - setup test data
    MyPlugin plugin = new MyPlugin();
    
    // Act - execute the code
    Variable result = plugin.execute(context);
    
    // Assert - verify results
    assertNotNull(result);
    assertEquals("expected", result.toString());
}
```

### Integration Tests

Create XML-based integration tests in `src/test/resources/integration-tests/`:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<config xmlns="http://org.webharvest/schema/2.1/core">
    <!-- Your test configuration -->
</config>
```

### Running Tests

```bash
# All tests
mvn test

# Specific test class
mvn test -Dtest=MyPluginTest

# Integration tests
mvn verify

# With coverage
mvn clean test jacoco:report
```

## Documentation

### Types of Documentation

1. **Code Documentation**: Javadoc for all public APIs
2. **User Documentation**: README.md, PLUGINS.md
3. **Developer Documentation**: DEVELOPER_GUIDE.md
4. **Change Documentation**: CHANGELOG.md

### Documentation Standards

- Write in clear, concise English
- Use Markdown formatting
- Include code examples
- Keep documentation up-to-date with code changes

### Updating Documentation

When making changes:
- Update relevant .md files
- Add entries to CHANGELOG.md
- Update Javadoc if API changes
- Include documentation changes in your commit

## Module Structure

### Core Modules

```
web-harvest/
├── webharvest-core/          Pure core (NO database)
├── webharvest-database/      Database functionality
├── webharvest-ftp/           FTP plugins
├── webharvest-mail/          Mail plugins
├── webharvest-webbrowser/    WebBrowser plugins
├── webharvest-zip/           Zip plugins
├── webharvest-cli/           Command-line interface
└── webharvest-ide/           GUI application
```

### Dependencies

- `webharvest-core` has NO database dependencies
- `webharvest-database` depends on `webharvest-core`
- Plugin modules depend on `webharvest-core`
- `webharvest-cli` and `webharvest-ide` depend on all modules

## Release Process

### Version Numbering

WebHarvest follows [Semantic Versioning](https://semver.org/):
- **MAJOR.MINOR.PATCH** (e.g., 2.2.0)
- MAJOR: Breaking changes
- MINOR: New features, backward compatible
- PATCH: Bug fixes

### Release Steps

1. Update version in all pom.xml files
2. Update CHANGELOG.md with release notes
3. Run full test suite: `mvn clean verify`
4. Build distribution: `mvn clean install`
5. Tag release in SVN:
```bash
svn copy https://svn.code.sf.net/p/web-harvest/code/trunk \
         https://svn.code.sf.net/p/web-harvest/code/tags/v2.2.0 \
         -m "Release 2.2.0"
```
6. Upload to SourceForge File Release System
7. Update project website
8. Announce release

## Getting Help

### Resources

- **SourceForge Project**: https://sourceforge.net/projects/web-harvest/
- **Issue Tracker**: https://sourceforge.net/p/web-harvest/tickets/
- **Mailing List**: web-harvest-users@lists.sourceforge.net
- **Documentation**: See DOCUMENTATION_INDEX.md

### Communication Channels

- **Tickets**: Bug reports and feature requests on SourceForge
- **Mailing List**: General questions and discussions
- **Patches**: Code contributions via SourceForge tickets

## Best Practices

### Before Committing

- [ ] Run `mvn clean test` - all tests pass
- [ ] Run `mvn verify` - code quality checks pass
- [ ] Update documentation if needed
- [ ] Write clear commit message in English
- [ ] Add appropriate tests for your changes

### Commit Message Format

```
Type: Brief description (max 72 characters)

Detailed explanation of what was changed and why.
Include any relevant ticket numbers.

- List specific changes
- Use bullet points for clarity

Fixes: #123 (if applicable)
```

Types: `Add`, `Fix`, `Update`, `Remove`, `Refactor`, `Docs`, `Test`

### Code Review Checklist

- [ ] Code follows project style guidelines
- [ ] Tests are included and passing
- [ ] Documentation is updated
- [ ] No unnecessary dependencies added
- [ ] Changes are backward compatible (or breaking changes documented)
- [ ] Code is properly commented
- [ ] Commit message is clear

## Common Tasks

### Adding a New Plugin

1. Create plugin class in appropriate module (e.g., `webharvest-core/src/main/java/org/webharvest/plugin/core/`)
2. Annotate with `@CorePlugin`
3. Implement required methods
4. Create unit tests
5. Create integration test with XML example
6. Add documentation to PLUGINS.md
7. Update CHANGELOG.md

### Fixing a Bug

1. Create test that reproduces the bug
2. Fix the bug
3. Verify test now passes
4. Run full test suite
5. Commit with clear message referencing ticket

### Improving Test Coverage

1. Check current coverage: `mvn jacoco:report`
2. Identify low-coverage areas
3. Add tests (unit or integration)
4. Verify coverage improvement
5. Submit patch or commit

## License

By contributing to WebHarvest, you agree that your contributions will be licensed under the BSD License.

---

## Questions?

If you have questions about contributing:
- Check DEVELOPER_GUIDE.md for technical details
- Post on the mailing list
- Create a ticket on SourceForge
- Contact project maintainers

**Thank you for contributing to WebHarvest!**

---

**Last Updated**: October 8, 2025  
**Version**: 2.2.0
