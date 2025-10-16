# 📤 SourceForge Upload Instructions

This document covers both **release artifacts** (JAR files) and **documentation** upload to SourceForge.

---

## 🎯 Release Publishing (JARs + Release Notes)

### 🚀 Quick Upload (Automated)

```bash
./publish-release.sh
```

**What it does:**
- ✅ Automatically extracts version from `pom.xml` (no hardcoded versions!)
- ✅ Checks if JARs exist for IDE and CLI
- ✅ Verifies if version already exists on SourceForge
- ✅ Asks for permission before overwriting existing releases
- ✅ Generates release notes from `CHANGELOG.md`
- ✅ Uploads to SourceForge File Release System (FRS)
- ✅ Creates version-specific directory automatically

**Example output:**
```
📖 Reading version from pom.xml...
✅ Detected version: 2.2.0
🚀 Publishing WebHarvest 2.2.0 to SourceForge...
📁 Target path: /home/frs/project/web-harvest/webhervest/2.2.0/

✅ Found JAR files:
   - IDE: webharvest-ide/target/webharvest-ide-2.2.0.jar (25M)
   - CLI: webharvest-cli/target/webharvest-cli-2.2.0.jar (12M)

🔍 Checking if version 2.2.0 already exists on SourceForge...
⚠️  WARNING: Version 2.2.0 already exists on SourceForge!

Existing files:
  webharvest-ide-2.2.0.jar (25M)
  webharvest-cli-2.2.0.jar (12M)
  RELEASE-NOTES.txt (5K)

❓ Do you want to OVERWRITE the existing release? (yes/no):
```

### 📋 Prerequisites

1. **Build artifacts first:**
   ```bash
   mvn clean install
   ```

2. **SSH key configured:**
   ```bash
   ls ~/.ssh/sourceforge_rsa
   # Key must exist and be added to SourceForge account
   ```

3. **CHANGELOG.md updated:**
   - Release notes are extracted from `CHANGELOG.md`
   - Ensure version section exists: `## [2.2.0] - Date`

### 🔧 Manual Upload (Advanced)

If you need more control:

```bash
# 1. Set version in pom.xml (if not already set)
VERSION="2.2.0"

# 2. Build JARs
mvn clean install

# 3. Upload manually via rsync
rsync -avz -e "ssh -i ~/.ssh/sourceforge_rsa" \
  webharvest-ide/target/webharvest-ide-${VERSION}.jar \
  webharvest-cli/target/webharvest-cli-${VERSION}.jar \
  rbala@frs.sourceforge.net:/home/frs/project/web-harvest/webhervest/${VERSION}/
```

### 📊 Post-Upload Steps

1. **Set default download** (via SourceForge web UI):
   - Go to: https://sourceforge.net/projects/web-harvest/files/webhervest/2.2.0/
   - Click ℹ️ icon next to IDE JAR
   - Set as default for: Windows, Mac, Linux, BSD

2. **Verify downloads work:**
   ```bash
   curl -I https://sourceforge.net/projects/web-harvest/files/latest/download
   ```

3. **Update project homepage** (if needed):
   - Update latest version number
   - Add release announcement

---

## 📚 Documentation Upload

### 📂 Documentation Structure

WebHarvest documentation consists of:

```
docs/web/              → Website documentation (in SVN)
    ├── index.html           # Homepage
    ├── ide.html             # IDE guide
    ├── plugins.html         # Plugin reference
    ├── css/                 # Stylesheets
    ├── documentation/       # User guides & tutorials
    ├── examples/            # XML example configs
    └── js/                  # Scripts

         ↓  upload-docs.sh

SourceForge
https://web-harvest.sourceforge.io/
```

**Important Notes:**
- ✅ **Website ONLY** - no javadoc published (users generate it themselves)
- ✅ **Direct upload** from `docs/web/` (in SVN)
- ❌ **Obsolete**: `web-harvest-docs-upload/` directory should be **deleted** if exists
- ❌ **Obsolete**: `docs/target/site/` not used for publishing

**Javadoc Policy:**
- Users can generate javadoc locally: `mvn javadoc:javadoc`
- If needed, javadoc can be published as separate artifact in SourceForge Files section
- Website is for end-user documentation only (guides, examples, tutorials)

### 🚀 Quick Upload (Automated)

```bash
./upload-docs.sh                # Normal upload (updates existing files)
./upload-docs.sh --dry-run      # Preview only (no upload)
./upload-docs.sh --clean-remote # Delete all remote files first (DANGEROUS!)
```

**Normal mode** (default):
- Updates/adds files from `docs/web/`
- Keeps existing files on server
- Safe for incremental updates

**Dry-run mode** (`--dry-run`):
- Shows what would be uploaded
- No actual changes made
- Good for verification

**Clean-remote mode** (`--clean-remote`) - ⚠️ **DANGEROUS**:
- Deletes ALL files from remote website first
- Creates automatic backup before deletion
- Requires typing "DELETE" to confirm
- Use ONLY when starting fresh with new structure

## 🔧 Manual Upload via SFTP

### 1. Connect to SFTP Server
```bash
sftp rbala@web.sourceforge.net
```

### 2. Navigate to Target Directory
```bash
cd /home/project-web/web-harvest/htdocs
```

### 3. Upload Files
```bash
# Upload all files from web-harvest-docs-upload/ directory
put -r web-harvest-docs-upload/* .
```

### 4. Verify Files Were Uploaded
```bash
ls -la
```

### 5. Exit Connection
```bash
quit
```

## 📁 Files to Upload Structure

```
web-harvest-docs-upload/
├── index.html           # Homepage
├── api/                 # Javadoc API documentation
├── css/                 # Stylesheets
├── js/                  # JavaScript files
├── images/              # Images
└── examples/            # Code examples (now in module test resources)
```

## ✅ Post-Upload Verification

### 1. Access Website
Visit: https://web-harvest.sourceforge.io/

### 2. Verify All Pages Load
- Homepage
- API documentation
- Examples

### 3. Test Navigation
- Menu links
- Internal page links
- API cross-references

### 4. Verify Code Examples Display Correctly
- Syntax highlighting
- Code formatting
- Download links

## 🚨 Troubleshooting

### Release Publishing Issues

**❌ "Could not extract version from pom.xml"**
- **Cause:** Script can't find `<version>` tag in main pom.xml
- **Fix:** Ensure you're running script from project root directory
- **Verify:** `grep -m 1 "<version>" pom.xml` should return version

**❌ "JAR not found"**
- **Cause:** Artifacts not built yet
- **Fix:** Run `mvn clean install` first
- **Check:** `ls -lh webharvest-*/target/*.jar`

**❌ SSH connection fails**
- **Cause:** SSH key not found or not configured
- **Fix:** 
  ```bash
  # Check if key exists
  ls -lh ~/.ssh/sourceforge_rsa
  
  # Test connection
  ssh -i ~/.ssh/sourceforge_rsa rbala@frs.sourceforge.net
  
  # If key doesn't exist, generate and add to SourceForge:
  # https://sourceforge.net/auth/shell_services
  ```

**❌ "Permission denied" on SourceForge**
- **Cause:** User doesn't have file release permissions
- **Fix:** Check project membership: https://sourceforge.net/projects/web-harvest/admin/members/
- **Required role:** Developer or Admin

**⚠️ Version already exists warning**
- **Expected behavior:** Script warns before overwriting
- **Options:**
  - Type `yes` to overwrite (useful for hotfixes)
  - Type `no` to cancel
  - Update version in `pom.xml` for new release

**❌ Release notes empty or generic**
- **Cause:** CHANGELOG.md doesn't have section for current version
- **Fix:** Add section to CHANGELOG.md:
  ```markdown
  ## [2.2.0] - October 16, 2025
  
  ### Added
  - Feature description
  
  ### Fixed
  - Bug fix description
  ```

### Documentation Upload Issues

**❌ Permission Issues**
- Verify you have permissions to `/home/project-web/web-harvest/htdocs` directory
- Check if you're a member of the web-harvest project team
- Contact SourceForge support if needed

**❌ Upload Failures**
- Check network connectivity: `ping web.sourceforge.net`
- Verify SFTP credentials
- Try uploading smaller batches
- Check available disk space

**❌ Missing Files**
- Verify all files were copied
- Check directory structure
- Ensure all CSS and JS files were uploaded
- Verify file permissions (should be 644)

**⚠️ Clean-Remote Mode Issues**

**❌ "Could not create backup"**
- **Cause:** Remote directory empty or no permissions
- **Effect:** Warning shown, but continues with deletion
- **Safe:** Backup failure doesn't stop the process

**❌ "Could not delete remote files"**
- **Cause:** SSH connection or permission issues
- **Fix:** Check SSH key and permissions
- **Impact:** Script exits, no files deleted

**⚠️ Clean-remote confirmation**
- **Expected:** Must type exactly "DELETE" (case-sensitive)
- **Cancel:** Any other input cancels operation
- **Example:**
  ```
  ❓ Are you ABSOLUTELY SURE? (type 'DELETE' to confirm): DELETE
  ✅ Proceeding...
  
  ❓ Are you ABSOLUTELY SURE? (type 'DELETE' to confirm): yes
  ❌ Clean cancelled. Files NOT deleted.
  ```

---

## 📊 Complete Release Checklist

### Before Release

- [ ] All tests pass: `mvn clean test`
- [ ] Version updated in `pom.xml`
- [ ] CHANGELOG.md updated with release notes
- [ ] README.md reflects current version
- [ ] Documentation up to date
- [ ] No uncommitted changes in SVN

### Build & Upload

- [ ] Build artifacts: `mvn clean install`
- [ ] Verify JARs: `ls -lh webharvest-*/target/*.jar`
- [ ] Test IDE locally: `java -jar webharvest-ide/target/webharvest-ide-2.2.0.jar`
- [ ] Test CLI locally: `java -jar webharvest-cli/target/webharvest-cli-2.2.0.jar --help`
- [ ] Upload release: `./publish-release.sh`
- [ ] Upload documentation: `./upload-docs.sh`

### After Upload

- [ ] Verify files on SourceForge: https://sourceforge.net/projects/web-harvest/files/webhervest/2.2.0/
- [ ] Set default download for platforms
- [ ] Test download link works
- [ ] Verify documentation: https://web-harvest.sourceforge.io/
- [ ] Test IDE examples from website
- [ ] Update project description (if major release)
- [ ] Announce release (mailing list, forum)
- [ ] Tag release in SVN: `svn copy ^/trunk ^/tags/v2.2.0`

## 🔍 Debugging

### 1. Check SFTP Logs
```bash
sftp -v rbala@web.sourceforge.net
```

### 2. List Remote Files
```bash
sftp> ls -la /home/project-web/web-harvest/htdocs
```

### 3. Compare Local vs Remote
```bash
# Local
find web-harvest-docs-upload -type f | wc -l

# Remote (via SFTP)
sftp> cd /home/project-web/web-harvest/htdocs
sftp> ls -laR | grep "^-" | wc -l
```

---

## 🎯 Quick Reference

### Documentation Commands

```bash
# Preview documentation locally
open docs/web/index.html             # Mac
xdg-open docs/web/index.html         # Linux  
start docs\web\index.html            # Windows

# Upload to SourceForge (normal mode)
./upload-docs.sh                     # Uploads docs/web/

# Dry-run (preview only, no upload)
./upload-docs.sh --dry-run

# Clean remote and upload fresh (DANGEROUS!)
./upload-docs.sh --clean-remote      # Deletes ALL remote files first

# Generate plugin documentation
cd docs/scripts
python3 generate_all_plugin_docs.py  # Updates 54 plugin pages

# Generate javadoc (optional, for local use only)
mvn javadoc:javadoc                  # Creates */target/site/apidocs/
```

### Common Commands

```bash
# Full release workflow (from scratch)
mvn clean install                    # Build all artifacts
./publish-release.sh                 # Upload JARs to SourceForge
./upload-docs.sh                     # Upload documentation

# Check what version will be published
grep -m 1 "<version>" pom.xml | sed -e 's/.*<version>\(.*\)<\/version>.*/\1/' | tr -d '[:space:]'

# Test SSH connection
ssh -i ~/.ssh/sourceforge_rsa rbala@frs.sourceforge.net

# List existing releases
ssh -i ~/.ssh/sourceforge_rsa rbala@frs.sourceforge.net "ls -lh /home/frs/project/web-harvest/webhervest/"

# Download and test published JAR
wget https://sourceforge.net/projects/web-harvest/files/webhervest/2.2.0/webharvest-ide-2.2.0.jar/download -O test.jar
java -jar test.jar
```

### File Locations

| What | Where |
|------|-------|
| **Main POM** | `pom.xml` (root) - contains version |
| **IDE JAR** | `webharvest-ide/target/webharvest-ide-{VERSION}.jar` |
| **CLI JAR** | `webharvest-cli/target/webharvest-cli-{VERSION}.jar` |
| **Changelog** | `CHANGELOG.md` - source for release notes |
| **SSH Key** | `~/.ssh/sourceforge_rsa` |
| **Release Script** | `./publish-release.sh` |
| **Docs Script** | `./upload-docs.sh` |
| **Docs Website** | `docs/web/` (in SVN, uploaded to SF) |
| **Javadoc** | `*/target/site/apidocs/` (local only, NOT published) |
| **Obsolete** | ~~`web-harvest-docs-upload/`~~ (delete if exists) |

### SourceForge URLs

| Resource | URL |
|----------|-----|
| **Project** | https://sourceforge.net/projects/web-harvest/ |
| **Files** | https://sourceforge.net/projects/web-harvest/files/webhervest/ |
| **Website** | https://web-harvest.sourceforge.io/ |
| **SVN** | https://svn.code.sf.net/p/web-harvest/code/ |
| **Admin** | https://sourceforge.net/projects/web-harvest/admin/ |
| **SSH Keys** | https://sourceforge.net/auth/shell_services |

---

## 📝 Changelog

### 2025-10-16: Documentation Upload & Cleanup Enhancement
- ✅ **BREAKING**: Removed dependency on `web-harvest-docs-upload/` directory (deleted from SVN)
- ✅ **POLICY CHANGE**: Javadoc NOT published to website (users generate locally)
- ✅ Script now uploads directly from `docs/web/` (source documentation in SVN)
- ✅ No Maven build step required for documentation upload
- ✅ Added dry-run mode: `./upload-docs.sh --dry-run`
- ✅ Added clean-remote mode: `./upload-docs.sh --clean-remote` (DANGEROUS!)
- ✅ Clean-remote creates automatic backup before deletion
- ✅ Requires typing "DELETE" to confirm remote clean
- ✅ Added file statistics preview before upload
- ✅ Added confirmation prompt before upload
- ✅ Added post-upload verification
- ✅ Simplified workflow: just `./upload-docs.sh`
- ✅ Moved Python scripts to `docs/scripts/` directory
- ✅ Cleaned up project root (removed 15 obsolete files)

**Actions Completed:** 
1. ✅ Deleted `web-harvest-docs-upload/` directory (733 files)
2. ✅ Deleted `V3_MIGRATION_GUIDE.md` (obsolete v3.0 docs)
3. ✅ Moved 4 Python scripts to `docs/scripts/`
4. ✅ Removed 15 temporary files (.txt, .log, .batch)

**Rationale:**
- Javadoc = 670+ files, 7-8MB (not needed on website)
- Users can generate: `mvn javadoc:javadoc`
- Website for end-user docs only (guides, examples, tutorials)
- Python scripts belong in docs/ directory (logical organization)

### 2025-10-16: Enhanced Release Automation
- ✅ Version now extracted from `pom.xml` (no hardcoding!)
- ✅ Added check for existing releases with user confirmation
- ✅ Release notes auto-generated from `CHANGELOG.md`
- ✅ Comprehensive error handling and user feedback
- ✅ Updated documentation with troubleshooting guide

### 2025-10-08: Initial Scripts
- Initial `publish-release.sh` and `upload-docs.sh` scripts
- Basic documentation for SFTP upload

---

**Last Updated**: October 16, 2025  
**SourceForge Project**: https://sourceforge.net/projects/web-harvest/  
**Maintained by**: Robert Bala (rbala)
