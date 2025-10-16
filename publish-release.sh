#!/bin/bash

# Web-Harvest Release Publisher
# Publishes IDE and CLI JAR files to SourceForge
# 
# Features:
# - Automatically extracts version from pom.xml
# - Checks if version already exists on SourceForge
# - Asks for permission before overwriting existing release
# - Generates comprehensive release notes from CHANGELOG.md

set -e

PROJECT_NAME="web-harvest"
SF_USER="rbala"
SF_HOST="frs.sourceforge.net"

# Extract version from main pom.xml
echo "📖 Reading version from pom.xml..."
VERSION=$(grep -m 1 "<version>" pom.xml | sed -e 's/.*<version>\(.*\)<\/version>.*/\1/' | tr -d '[:space:]')

if [ -z "$VERSION" ]; then
    echo "❌ Error: Could not extract version from pom.xml"
    exit 1
fi

echo "✅ Detected version: ${VERSION}"

SF_PATH="/home/frs/project/${PROJECT_NAME}/webhervest/${VERSION}/"

echo "🚀 Publishing WebHarvest ${VERSION} to SourceForge..."
echo "📁 Target path: ${SF_PATH}"

# Check if JAR files exist
IDE_JAR="webharvest-ide/target/webharvest-ide-${VERSION}.jar"
CLI_JAR="webharvest-cli/target/webharvest-cli-${VERSION}.jar"

if [ ! -f "$IDE_JAR" ]; then
    echo "❌ IDE JAR not found: $IDE_JAR"
    echo "💡 Run 'mvn clean install' first"
    exit 1
fi

if [ ! -f "$CLI_JAR" ]; then
    echo "❌ CLI JAR not found: $CLI_JAR"
    echo "💡 Run 'mvn clean install' first"
    exit 1
fi

echo "✅ Found JAR files:"
echo "   - IDE: $IDE_JAR ($(du -h "$IDE_JAR" | cut -f1))"
echo "   - CLI: $CLI_JAR ($(du -h "$CLI_JAR" | cut -f1))"

# Check if version already exists on SourceForge
echo ""
echo "🔍 Checking if version ${VERSION} already exists on SourceForge..."

# Test SSH connection and check if directory exists
if ssh -i ~/.ssh/sourceforge_rsa -o ConnectTimeout=10 "${SF_USER}@${SF_HOST}" "test -d ${SF_PATH}" 2>/dev/null; then
    echo ""
    echo "⚠️  WARNING: Version ${VERSION} already exists on SourceForge!"
    echo "📁 Path: ${SF_PATH}"
    echo ""
    echo "Existing files:"
    ssh -i ~/.ssh/sourceforge_rsa "${SF_USER}@${SF_HOST}" "ls -lh ${SF_PATH}" 2>/dev/null | grep -E "\.(jar|txt)$" || echo "  (could not list files)"
    echo ""
    
    # Ask for confirmation
    read -p "❓ Do you want to OVERWRITE the existing release? (yes/no): " -r
    echo
    
    if [[ ! $REPLY =~ ^[Yy][Ee][Ss]$ ]]; then
        echo "❌ Upload cancelled by user."
        echo "💡 To publish a new version, update the version in pom.xml first."
        exit 1
    fi
    
    echo "✅ User confirmed overwrite. Proceeding with upload..."
else
    echo "✅ Version ${VERSION} does not exist yet. Creating new release..."
fi

# Create temporary directory for upload
TEMP_DIR=$(mktemp -d)
echo "📂 Created temp directory: $TEMP_DIR"

# Copy JAR files to temp directory
cp "$IDE_JAR" "$TEMP_DIR/"
cp "$CLI_JAR" "$TEMP_DIR/"

# Generate release notes from CHANGELOG.md
echo "📝 Generating release notes from CHANGELOG.md..."

if [ -f "CHANGELOG.md" ]; then
    # Extract section for current version from CHANGELOG.md
    # This extracts content between ## [VERSION] and next ## heading
    VERSION_SECTION=$(awk "/^## \[${VERSION}\]/,/^## \[/" CHANGELOG.md | sed '$d')
    
    if [ -n "$VERSION_SECTION" ]; then
        cat > "$TEMP_DIR/RELEASE-NOTES.txt" << EOF
WebHarvest ${VERSION} Release Notes
====================================

${VERSION_SECTION}

Files Included:
- webharvest-ide-${VERSION}.jar: Web-based IDE with embedded Jetty server
- webharvest-cli-${VERSION}.jar: Command-line interface

System Requirements:
- Java 8 or higher
- Minimum 512MB RAM
- 100MB disk space

Installation:

IDE (Web Interface):
  java -jar webharvest-ide-${VERSION}.jar
  # Opens browser at http://localhost:8080

CLI (Command Line):
  java -jar webharvest-cli-${VERSION}.jar --help
  java -jar webharvest-cli-${VERSION}.jar config.xml

Documentation: https://web-harvest.sourceforge.io/
SourceForge: https://sourceforge.net/projects/web-harvest/
SVN Repository: https://svn.code.sf.net/p/web-harvest/code/

Release Date: $(date +"%B %d, %Y")
EOF
        echo "✅ Release notes generated from CHANGELOG.md"
    else
        echo "⚠️  Warning: Could not find version ${VERSION} in CHANGELOG.md"
        echo "💡 Using generic release notes"
        
        cat > "$TEMP_DIR/RELEASE-NOTES.txt" << EOF
WebHarvest ${VERSION} Release Notes
====================================

For detailed release notes, see CHANGELOG.md in the repository.

Files Included:
- webharvest-ide-${VERSION}.jar: Web-based IDE
- webharvest-cli-${VERSION}.jar: Command-line interface

System Requirements:
- Java 8 or higher
- Minimum 512MB RAM
- 100MB disk space

Installation:
  IDE: java -jar webharvest-ide-${VERSION}.jar
  CLI: java -jar webharvest-cli-${VERSION}.jar --help

Documentation: https://web-harvest.sourceforge.io/
Release Date: $(date +"%B %d, %Y")
EOF
    fi
else
    echo "⚠️  Warning: CHANGELOG.md not found"
    echo "💡 Using minimal release notes"
    
    cat > "$TEMP_DIR/RELEASE-NOTES.txt" << EOF
WebHarvest ${VERSION} Release Notes
====================================

Files Included:
- webharvest-ide-${VERSION}.jar
- webharvest-cli-${VERSION}.jar

Documentation: https://web-harvest.sourceforge.io/
Release Date: $(date +"%B %d, %Y")
EOF
fi

echo "📝 Release notes created: $TEMP_DIR/RELEASE-NOTES.txt"

# Upload files to SourceForge
echo "🌐 Uploading files to SourceForge..."
echo "🔐 You will be prompted for your SourceForge password and SSH key passphrase."

# Upload using rsync over SSH
rsync -avz --progress \
    -e "ssh -i ~/.ssh/sourceforge_rsa" \
    "$TEMP_DIR/" \
    "${SF_USER}@${SF_HOST}:${SF_PATH}"

if [ $? -eq 0 ]; then
    echo "✅ Upload successful!"
    echo "🌐 Files are now available at:"
    echo "   https://sourceforge.net/projects/${PROJECT_NAME}/files/webhervest/${VERSION}/"
    echo ""
    echo "📋 Uploaded files:"
    echo "   - webharvest-ide-${VERSION}.jar"
    echo "   - webharvest-cli-${VERSION}.jar"
    echo "   - RELEASE-NOTES.txt"
else
    echo "❌ Upload failed!"
    exit 1
fi

# Cleanup
rm -rf "$TEMP_DIR"
echo "🧹 Cleaned up temporary files"

echo ""
echo "🎉 Release ${VERSION} published successfully!"
echo "📖 Next steps:"
echo "   1. Verify files are accessible on SourceForge"
echo "   2. Update project description if needed"
echo "   3. Announce the release to users"
