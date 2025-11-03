# WebHarvest Documentation Index

**Version**: 2.3.1  
**Last Updated**: November 3, 2025

---

## 📚 Core Documentation

### Quick Start

1. **[README.md](README.md)** - Project overview
   - What is WebHarvest
   - Features and capabilities
   - Installation and basic usage

2. **[USER_GUIDE.md](USER_GUIDE.md)** - Complete user manual
   - IDE usage guide (XML + Visual Designer)
   - Getting started with WebHarvest
   - Examples and best practices
   - Troubleshooting

3. **[DEVELOPER_GUIDE.md](DEVELOPER_GUIDE.md)** - Developer documentation
   - Architecture overview
   - Build instructions
   - Testing guidelines
   - API documentation

### Essential References

4. **[CHANGELOG.md](CHANGELOG.md)** - Version history
   - All notable changes by version
   - Breaking changes and migration guides

5. **[CONTRIBUTING.md](CONTRIBUTING.md)** - Contribution guide
   - Guidelines, code style, pull request process

6. **[PLUGINS.md](PLUGINS.md)** - Plugin reference
   - Available plugins with examples
   - Context-specific plugins
   - Custom plugin development

7. **[BUGS_FIXED.md](BUGS_FIXED.md)** - Bug database
   - All fixed bugs with explanations
   - Root cause analysis and TDD details

---

## 📖 Technical Guides

8. **[HTTPCLIENT5_MIGRATION_GUIDE.md](HTTPCLIENT5_MIGRATION_GUIDE.md)** - HttpClient 5.x migration
9. **[PAUSE_RESUME_GUIDE.md](PAUSE_RESUME_GUIDE.md)** - Session management
10. **[ENHANCED_METRICS_GUIDE.md](ENHANCED_METRICS_GUIDE.md)** - Metrics and monitoring
11. **[WORKING_XML_SYNTAX_REFERENCE.md](WORKING_XML_SYNTAX_REFERENCE.md)** - XML syntax reference
12. **[XPATH_BEST_PRACTICES.md](XPATH_BEST_PRACTICES.md)** - XPath best practices
13. **[PLUGIN_DOCS_GENERATION.md](PLUGIN_DOCS_GENERATION.md)** - Plugin documentation generation

## 🚀 Deployment & Operations

14. **[SFTP-UPLOAD-INSTRUCTIONS.md](SFTP-UPLOAD-INSTRUCTIONS.md)** - SourceForge deployment
15. **[SOURCEFORGE-SETUP.md](SOURCEFORGE-SETUP.md)** - SourceForge project setup
16. **[OCTOBER_2025_RELEASE_NOTES.md](OCTOBER_2025_RELEASE_NOTES.md)** - Release notes
17. **[XQUERY_FIX_SUMMARY.md](XQUERY_FIX_SUMMARY.md)** - XQuery fix documentation

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
- **USER_GUIDE.md** - Complete user manual (IDE + Visual Designer)
- **README.md** - Quick start
- **PLUGINS.md** - Plugin reference
- **WORKING_XML_SYNTAX_REFERENCE.md** - XML syntax guide
- **XPATH_BEST_PRACTICES.md** - XPath patterns

### For Contributors
- **CONTRIBUTING.md** - Contribution guidelines
- **DEVELOPER_GUIDE.md** - Development setup
- **BUGS_FIXED.md** - Bug database

### For Maintainers
- **SFTP-UPLOAD-INSTRUCTIONS.md** - Deployment
- **SOURCEFORGE-SETUP.md** - Infrastructure setup
- **HTTPCLIENT5_MIGRATION_GUIDE.md** - Migration guides

---

## 🗂️ Module-Specific Documentation

### webharvest-core
- Module contains additional technical documentation
- See `webharvest-core/*.md` for details
- **[Semantic Metadata](docs/semantic-metadata/)** - Plugin metadata catalog and schema

### Other Modules
- **webharvest-database/** - Database plugin README
- **webharvest-ftp/** - FTP plugin README
- **webharvest-mail/** - Mail plugin README
- **webharvest-zip/** - Zip plugin README
- **webharvest-webbrowser/** - Web browser plugin README

### webharvest-ide
- All IDE documentation consolidated into **[USER_GUIDE.md](USER_GUIDE.md)**
- Includes IDE user guide, Visual Designer guide, troubleshooting

---

## 📋 Quick Links

| Document | Purpose | Audience |
|----------|---------|----------|
| README | Project overview | Everyone |
| USER_GUIDE | Complete user manual | Users |
| CHANGELOG | Version history | Everyone |
| CONTRIBUTING | Contribution guide | Contributors |
| DEVELOPER_GUIDE | Development docs | Developers |
| PLUGINS | Plugin reference | Everyone |
| BUGS_FIXED | Bug database | QA, Developers |
| HTTPCLIENT5_MIGRATION_GUIDE | HttpClient migration | Developers |
| PAUSE_RESUME_GUIDE | Session management | Users |
| ENHANCED_METRICS_GUIDE | Monitoring | Users, DevOps |
| WORKING_XML_SYNTAX_REFERENCE | XML syntax | Users |
| XPATH_BEST_PRACTICES | XPath guide | Users |
| PLUGIN_DOCS_GENERATION | Doc generation | Contributors |
| SFTP-UPLOAD-INSTRUCTIONS | Deployment guide | Maintainers |

---

## 🔄 Documentation Maintenance

### Latest Consolidation - November 3, 2025
- **Reduced:** 61 .md files → 24 core files (61% reduction)
- **Archived:** All SUMMARY, COMPLETE, PLAN, STATUS files (~30 files)
- **Archived:** Session summaries and implementation status files from webharvest-ide/ (~20 files)
- **Created:** DOCUMENTATION_STATUS.md (current status tracker)
- **Result:** Clean, organized documentation structure

### Core Files (24 total)
- **Essential (6):** README, USER_GUIDE, DEVELOPER_GUIDE, CHANGELOG, CONTRIBUTING, PLUGINS
- **Reference (3):** BUGS_FIXED, COMPONENT_LIBRARY, CONDUCTOR_ANALYSIS_AND_ROADMAP
- **Guides (6):** HTTPCLIENT5_MIGRATION, PAUSE_RESUME, ENHANCED_METRICS, XML_SYNTAX, XPATH_BEST_PRACTICES, PLUGIN_DOCS_GENERATION
- **Operations (4):** SFTP-UPLOAD, SOURCEFORGE-SETUP, RELEASE_NOTES, XQUERY_FIX
- **Utilities (2):** BUILD_AND_TEST, DOCS_UPLOAD_SIMPLIFICATION
- **Metadata (3):** DOCUMENTATION_INDEX, DOCUMENTATION_STATUS, RELEASE_SCRIPTS_ENHANCEMENT

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

**Total Documentation Files**: 24 (streamlined and current)  
**Python Scripts**: 4 (in docs/scripts/)  
**Shell Scripts**: 2 (publish-release.sh, upload-docs.sh)  
**Archived Files**: 170+ (historical reference in docs/archive/)  
**Status**: Clean, organized, and up-to-date  
**Last Review**: November 3, 2025


