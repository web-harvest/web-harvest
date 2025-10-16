# WebHarvest Documentation Index

**Version**: 2.2.0  
**Last Updated**: October 16, 2025

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

8. **[XQUERY_FIX_SUMMARY.md](XQUERY_FIX_SUMMARY.md)** - XQuery node() parameter fix (r822-r825)
   - Critical fix for XQParamPlugin and XQueryPlugin
   - TDD approach with integration tests
   - Complete technical analysis and examples

9. **[OCTOBER_2025_RELEASE_NOTES.md](OCTOBER_2025_RELEASE_NOTES.md)** - Current release notes
   - Release highlights
   - Major features and improvements
   - Known issues and workarounds

---

## 📖 Technical Guides

### Migration Guides

9. **[HTTPCLIENT5_MIGRATION_GUIDE.md](HTTPCLIENT5_MIGRATION_GUIDE.md)** - HttpClient 5.x migration
   - Migration from HttpClient 4.x to 5.x
   - API changes
   - Testing approach

### User Guides

10. **[PAUSE_RESUME_GUIDE.md](PAUSE_RESUME_GUIDE.md)** - Session management
    - Pause and resume scraper execution
    - Session state management
    - Use cases and examples

11. **[ENHANCED_METRICS_GUIDE.md](ENHANCED_METRICS_GUIDE.md)** - Metrics and monitoring
    - Execution metrics
    - Performance monitoring
    - Event sourcing

12. **[WORKING_XML_SYNTAX_REFERENCE.md](WORKING_XML_SYNTAX_REFERENCE.md)** - XML syntax reference
    - Configuration file syntax
    - Plugin usage patterns
    - Examples and best practices

---

## 🚀 Deployment & Operations

13. **[SFTP-UPLOAD-INSTRUCTIONS.md](SFTP-UPLOAD-INSTRUCTIONS.md)** - SourceForge deployment
    - Release publishing (JARs + release notes)
    - Documentation upload (website only, no javadoc)
    - Troubleshooting guide
    - Complete release checklist

14. **[SOURCEFORGE-SETUP.md](SOURCEFORGE-SETUP.md)** - SourceForge project setup
    - Project configuration
    - Web hosting setup
    - File release system

15. **[RELEASE_SCRIPTS_ENHANCEMENT.md](RELEASE_SCRIPTS_ENHANCEMENT.md)** - Release automation improvements
    - Dynamic version extraction from pom.xml
    - Overwrite protection for existing releases
    - Automatic release notes from CHANGELOG.md

16. **[DOCS_UPLOAD_SIMPLIFICATION.md](DOCS_UPLOAD_SIMPLIFICATION.md)** - Documentation upload policy
    - Javadoc policy (not published to website)
    - Simplified upload workflow
    - Directory structure clarification

---

## 📂 Scripts & Automation

17. **[docs/scripts/](docs/scripts/)** - Documentation generation scripts
    - `generate_all_plugin_docs.py` - Master script (all plugins)
    - `generate_core_plugin_docs.py` - 46 core plugins
    - `generate_external_plugin_docs.py` - 8 external plugins
    - `generate_plugins_index.py` - Plugin index page

18. **[publish-release.sh](publish-release.sh)** - Release publishing script
    - Uploads JARs to SourceForge Files
    - Dynamic version from pom.xml
    - Overwrite protection

19. **[upload-docs.sh](upload-docs.sh)** - Documentation upload script
    - Uploads docs/web/ to SourceForge website
    - Supports --dry-run and --clean-remote modes

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
| HTTPCLIENT5_MIGRATION_GUIDE | HttpClient migration | Developers |
| PAUSE_RESUME_GUIDE | Session management | Advanced Users |
| ENHANCED_METRICS_GUIDE | Monitoring | DevOps, Developers |
| WORKING_XML_SYNTAX_REFERENCE | XML syntax | Users, Developers |
| PLUGIN_DOCS_GENERATION | Doc generation | Contributors |
| SFTP-UPLOAD-INSTRUCTIONS | Deployment guide | Release Managers |
| RELEASE_SCRIPTS_ENHANCEMENT | Release automation | Maintainers |
| DOCS_UPLOAD_SIMPLIFICATION | Docs upload policy | Maintainers |
| docs/scripts/*.py | Plugin doc generation | Contributors |
| publish-release.sh | Release publishing | Release Managers |
| upload-docs.sh | Website upload | Release Managers |

---

## 🔄 Documentation Maintenance

### Latest Cleanup
- **Date**: October 16, 2025
- **Action**: Major consolidation + infrastructure cleanup
- **Result**: Reduced from 45 to 19 files, organized scripts

### Changes Made
1. **Deleted 20 duplicates** - Files already archived in `docs/archive/`
2. **Archived 7 session summaries** - Historical session reports moved to archive
3. **Consolidated bug documentation** - Bug details merged into BUGS_FIXED.md
4. **Removed obsolete guides** - V3_MIGRATION_GUIDE.md (v3.0 doesn't exist)
5. **Removed obsolete directories** - web-harvest-docs-upload/ (733 javadoc files)
6. **Organized Python scripts** - Moved to docs/scripts/ (4 files)
7. **Enhanced release scripts** - publish-release.sh, upload-docs.sh
8. **Cleaned temporary files** - 15 files (.txt, .log, .batch) from project root

### Files Removed
- Architecture proposals and analyses (archived)
- Session summaries and reports (archived)
- Implementation summaries (consolidated)
- Design documents (archived - designs completed)
- Migration status files (archived - migrations complete)
- Old architecture documentation (archived)

### Kept (Essential - 19 files)
- Core project documentation (README, CHANGELOG, CONTRIBUTING, DEVELOPER_GUIDE)
- Plugin documentation (PLUGINS, PLUGIN_DOCS_GENERATION)
- Bug tracking (BUGS_FIXED)
- Current release (OCTOBER_2025_RELEASE_NOTES)
- Technical guides (4 migration/user guides)
- Deployment & automation (5 files: SFTP-UPLOAD-INSTRUCTIONS, SOURCEFORGE-SETUP, RELEASE_SCRIPTS_ENHANCEMENT, DOCS_UPLOAD_SIMPLIFICATION, DOCUMENTATION_CONSOLIDATION_SUMMARY)
- Planning docs (2 files: DOCUMENTATION_CONSOLIDATION_PLAN, COVERAGE_IMPROVEMENT_PLAN)
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

**Total Documentation Files**: 19 (streamlined and current)  
**Python Scripts**: 4 (in docs/scripts/)  
**Shell Scripts**: 2 (publish-release.sh, upload-docs.sh)  
**Archived Files**: 43+ (historical reference)  
**Status**: Clean, organized, and up-to-date  
**Last Review**: October 16, 2025


