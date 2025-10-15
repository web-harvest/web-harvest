# WebHarvest Documentation Index

**Version**: 2.2.0  
**Last Updated**: October 15, 2025

---

## 📚 Core Documentation

### Essential Reading

1. **[README.md](README.md)** - Project overview and quick start
   - What is WebHarvest
   - Features and capabilities
   - Installation and basic usage
   - Critical bug fixes and IDE overview

2. **[CHANGELOG.md](CHANGELOG.md)** - Version history and release notes
   - All notable changes by version
   - Breaking changes and migration guides
   - Bug fixes and improvements

3. **[CONTRIBUTING.md](CONTRIBUTING.md)** - How to contribute
   - Contribution guidelines
   - Code style and standards
   - Pull request process

4. **[DEVELOPER_GUIDE.md](DEVELOPER_GUIDE.md)** - Developer documentation
   - Architecture overview
   - Build instructions
   - Testing guidelines
   - API documentation

5. **[PLUGINS.md](PLUGINS.md)** - Plugin documentation
   - Available plugins
   - Plugin usage examples
   - Custom plugin development

6. **[PLUGIN_DOCS_GENERATION.md](PLUGIN_DOCS_GENERATION.md)** - Plugin documentation generation
   - How to generate plugin documentation
   - Documentation structure
   - Adding new plugins to docs

---

## 🐛 Bug Tracking & Release Notes

7. **[BUGS_FIXED.md](BUGS_FIXED.md)** - Comprehensive bug database
   - All fixed bugs with detailed explanations
   - Root cause analysis
   - Test coverage for each fix
   - TDD implementation details

8. **[OCTOBER_2025_RELEASE_NOTES.md](OCTOBER_2025_RELEASE_NOTES.md)** - Current release notes
   - Release highlights
   - Major features and improvements
   - Known issues and workarounds

---

## 📖 Technical Guides

### Migration Guides

9. **[V3_MIGRATION_GUIDE.md](V3_MIGRATION_GUIDE.md)** - Version 3.x migration guide
   - Breaking changes in v3.x
   - Migration steps
   - Code examples

10. **[HTTPCLIENT5_MIGRATION_GUIDE.md](HTTPCLIENT5_MIGRATION_GUIDE.md)** - HttpClient 5.x migration
    - Migration from HttpClient 4.x to 5.x
    - API changes
    - Testing approach

### User Guides

11. **[PAUSE_RESUME_GUIDE.md](PAUSE_RESUME_GUIDE.md)** - Session management
    - Pause and resume scraper execution
    - Session state management
    - Use cases and examples

12. **[ENHANCED_METRICS_GUIDE.md](ENHANCED_METRICS_GUIDE.md)** - Metrics and monitoring
    - Execution metrics
    - Performance monitoring
    - Event sourcing

13. **[WORKING_XML_SYNTAX_REFERENCE.md](WORKING_XML_SYNTAX_REFERENCE.md)** - XML syntax reference
    - Configuration file syntax
    - Plugin usage patterns
    - Examples and best practices

---

## 🚀 Deployment & Operations

14. **[SFTP-UPLOAD-INSTRUCTIONS.md](SFTP-UPLOAD-INSTRUCTIONS.md)** - SourceForge deployment
    - SFTP upload instructions
    - File structure on SourceForge
    - Deployment process

15. **[SOURCEFORGE-SETUP.md](SOURCEFORGE-SETUP.md)** - SourceForge project setup
    - Project configuration
    - Web hosting setup
    - File release system

---

## 📖 Documentation Categories

### For Users
- **README.md** - Start here
- **PLUGINS.md** - Plugin usage
- **CHANGELOG.md** - What's new
- **WORKING_XML_SYNTAX_REFERENCE.md** - XML syntax

### For Contributors
- **CONTRIBUTING.md** - How to contribute
- **DEVELOPER_GUIDE.md** - Development setup
- **BUGS_FIXED.md** - Bug database and fixes

### For Maintainers
- **OCTOBER_2025_RELEASE_NOTES.md** - Current release
- **ENHANCED_METRICS_GUIDE.md** - Monitoring
- **SFTP-UPLOAD-INSTRUCTIONS.md** - Deployment
- **Migration Guides** - Version upgrades

---

## 🗂️ Module-Specific Documentation

### webharvest-core
- Module contains additional technical documentation
- See `webharvest-core/*.md` for details

### Other Modules
- **webharvest-database/** - Database plugin README
- **webharvest-ftp/** - FTP plugin README
- **webharvest-mail/** - Mail plugin README
- **webharvest-zip/** - Zip plugin README
- **webharvest-webbrowser/** - Web browser plugin README
- **webharvest-ide/** - IDE-specific documentation

---

## 📋 Quick Links

| Document | Purpose | Audience |
|----------|---------|----------|
| README | Project overview | Everyone |
| CHANGELOG | Version history | Users, Developers |
| CONTRIBUTING | Contribution guide | Contributors |
| DEVELOPER_GUIDE | Development docs | Developers |
| PLUGINS | Plugin reference | Users, Developers |
| BUGS_FIXED | Bug database | QA, Developers |
| OCTOBER_2025_RELEASE_NOTES | Current release | Everyone |
| V3_MIGRATION_GUIDE | Version 3.x migration | Upgraders |
| HTTPCLIENT5_MIGRATION_GUIDE | HttpClient migration | Developers |
| PAUSE_RESUME_GUIDE | Session management | Advanced Users |
| ENHANCED_METRICS_GUIDE | Monitoring | DevOps, Developers |
| WORKING_XML_SYNTAX_REFERENCE | XML syntax | Users, Developers |
| PLUGIN_DOCS_GENERATION | Doc generation | Contributors |
| SFTP/SOURCEFORGE | Deployment | Release Managers |

---

## 🔄 Documentation Maintenance

### Latest Cleanup
- **Date**: October 15, 2025
- **Action**: Major consolidation - removed 28 duplicate/obsolete files
- **Result**: Reduced from 45 to 17 files (62% reduction)

### Changes Made
1. **Deleted 20 duplicates** - Files already archived in `docs/archive/`
2. **Archived 7 session summaries** - Historical session reports moved to archive
3. **Consolidated bug documentation** - Bug #24 and Bug #39 details merged into BUGS_FIXED.md
4. **Removed Polish duplicate** - GENERUJ_DOKUMENTACJE_PLUGINOW.md (English version kept)
5. **Cleaned obsolete files** - Completed tasks and plans archived

### Files Removed
- Architecture proposals and analyses (archived)
- Session summaries and reports (archived)
- Implementation summaries (consolidated)
- Design documents (archived - designs completed)
- Migration status files (archived - migrations complete)
- Old architecture documentation (archived)

### Kept (Essential - 17 files)
- Core project documentation (README, CHANGELOG, CONTRIBUTING, DEVELOPER_GUIDE)
- Plugin documentation (PLUGINS, PLUGIN_DOCS_GENERATION)
- Bug tracking (BUGS_FIXED)
- Current release (OCTOBER_2025_RELEASE_NOTES)
- Technical guides (3 migration/user guides)
- Deployment instructions (2 files)
- This index file

### Archived (docs/archive/)
- 43+ historical documentation files
- Session summaries and reports
- Completed implementation plans
- Old architecture documentation
- Design proposals

---

## 📝 Documentation Standards

### Language
- All documentation in **English** (project standard)
- Code comments in English
- SVN commit messages in English

### Format
- Markdown (.md) for all documentation
- Clear headers and sections
- Code examples where appropriate
- Tables for comparisons

### Updates
- CHANGELOG.md updated with each release
- README.md updated for major features
- BUGS_FIXED.md updated when bugs are fixed
- Module docs updated for module-specific changes
- This index updated when documentation structure changes

---

## 📂 Archive Policy

Historical documentation is moved to `docs/archive/` when:
- Implementation/migration is complete
- Session work is finished
- Document superseded by newer version
- Content consolidated into main documentation

Archive preserves historical context while keeping root clean and navigable.

---

**Total Documentation Files**: 17 (streamlined and current)  
**Archived Files**: 43+ (historical reference)  
**Status**: Clean, organized, and up-to-date  
**Last Review**: October 15, 2025


