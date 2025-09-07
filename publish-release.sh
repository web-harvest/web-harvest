#!/bin/bash

# Web-Harvest Release Publisher
# Publishes IDE and CLI JAR files to SourceForge

set -e

VERSION="2.1.0"
PROJECT_NAME="web-harvest"
SF_USER="rbala"
SF_HOST="frs.sourceforge.net"
SF_PATH="/home/frs/project/${PROJECT_NAME}/webhervest/${VERSION}/"

echo "🚀 Publishing Web-Harvest ${VERSION} to SourceForge..."
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

# Create temporary directory for upload
TEMP_DIR=$(mktemp -d)
echo "📂 Created temp directory: $TEMP_DIR"

# Copy JAR files to temp directory
cp "$IDE_JAR" "$TEMP_DIR/"
cp "$CLI_JAR" "$TEMP_DIR/"

# Create release notes
cat > "$TEMP_DIR/RELEASE-NOTES.txt" << EOF
Web-Harvest ${VERSION} Release Notes
====================================

Major Updates:
- Migrated XML namespace from web-harvest.sourceforge.net to org.webharvest
- Updated all XSD schemas with new namespace structure
- Enhanced website with modern design and consistent branding
- Added comprehensive API documentation
- Published examples to website with proper permissions
- Updated IDE welcome screen with modern styling
- Fixed all IDE links to point to correct URLs
- Synchronized color palette across all documentation

Files:
- webharvest-ide-${VERSION}.jar: IDE application
- webharvest-cli-${VERSION}.jar: Command-line interface

System Requirements:
- Java 8 or higher
- Minimum 512MB RAM
- 50MB disk space

Installation:
1. Download the appropriate JAR file for your needs
2. For IDE: java -jar webharvest-ide-${VERSION}.jar
3. For CLI: java -jar webharvest-cli-${VERSION}.jar --help

Documentation: https://web-harvest.sourceforge.net/
Examples: https://web-harvest.sourceforge.net/examples/

Release Date: $(date)
EOF

echo "📝 Created release notes"

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
