#!/bin/bash

# Web-Harvest Documentation Upload Script
# 
# Uploads complete documentation to SourceForge web hosting
# 
# Features:
# - Statistics preview (file count, size)
# - Dry-run mode to preview changes
# - Backup of previous version
# - Verification after upload
# - Automatic version detection from pom.xml
#
# Usage: 
#   ./upload-docs.sh                # Upload documentation
#   ./upload-docs.sh --dry-run      # Preview what would be uploaded
#   ./upload-docs.sh --clean-remote # Delete all remote files first (DANGEROUS!)

set -e

# Configuration
DOCS_DIR="docs/web"              # Website documentation (NO javadoc)
SF_USER="rbala"
SF_HOST="web.sourceforge.net"
SF_PATH="/home/project-web/web-harvest/htdocs"
SSH_KEY="~/.ssh/sourceforge_rsa"

# Note: We do NOT publish javadoc to the website
# - Users can generate it themselves: mvn javadoc:javadoc
# - If needed, javadoc can be added as artifact to SourceForge Files section
# - Website is for end-user documentation only

# Parse arguments
DRY_RUN=false
CLEAN_REMOTE=false

for arg in "$@"; do
    case $arg in
        --dry-run|-n)
            DRY_RUN=true
            echo "🔍 DRY-RUN MODE: No files will be uploaded"
            echo ""
            ;;
        --clean-remote)
            CLEAN_REMOTE=true
            ;;
        *)
            echo "❌ Unknown option: $arg"
            echo ""
            echo "Usage:"
            echo "  ./upload-docs.sh                # Upload documentation"
            echo "  ./upload-docs.sh --dry-run      # Preview only"
            echo "  ./upload-docs.sh --clean-remote # Delete remote files first (DANGEROUS!)"
    exit 1
            ;;
    esac
done

echo "📚 Web-Harvest Documentation Upload"
echo "===================================="
echo ""

# Extract version from pom.xml
echo "📖 Reading version from pom.xml..."
VERSION=$(grep -m 1 "<version>" pom.xml | sed -e 's/.*<version>\(.*\)<\/version>.*/\1/' | tr -d '[:space:]')

if [ -z "$VERSION" ]; then
    echo "⚠️  Warning: Could not extract version from pom.xml"
    VERSION="unknown"
else
    echo "✅ Detected version: ${VERSION}"
fi
echo ""

# Check if documentation directory exists
if [ ! -d "$DOCS_DIR" ]; then
    echo "❌ Error: Documentation directory '${DOCS_DIR}' not found!"
    echo ""
    echo "💡 This directory should exist in SVN checkout."
    echo "   Make sure you're in the project root directory."
    echo ""
    exit 1
fi

echo "📁 Documentation source: ${DOCS_DIR}"
echo "   Type: Website documentation (user guides, examples, tutorials)"
echo "   Note: Javadoc NOT included (users generate it themselves)"
echo ""

# Collect statistics
echo "📊 Analyzing documentation directory..."
FILE_COUNT=$(find "$DOCS_DIR" -type f | wc -l | tr -d ' ')
TOTAL_SIZE=$(du -sh "$DOCS_DIR" | cut -f1)
HTML_COUNT=$(find "$DOCS_DIR" -type f -name "*.html" | wc -l | tr -d ' ')
CSS_COUNT=$(find "$DOCS_DIR" -type f -name "*.css" | wc -l | tr -d ' ')
JS_COUNT=$(find "$DOCS_DIR" -type f -name "*.js" | wc -l | tr -d ' ')
XML_COUNT=$(find "$DOCS_DIR" -type f -name "*.xml" | wc -l | tr -d ' ')

echo "📁 Documentation Statistics:"
echo "   Total files:  ${FILE_COUNT}"
echo "   Total size:   ${TOTAL_SIZE}"
echo "   HTML files:   ${HTML_COUNT}"
echo "   CSS files:    ${CSS_COUNT}"
echo "   JS files:     ${JS_COUNT}"
echo "   XML examples: ${XML_COUNT}"
echo ""

# Main content preview
echo "📄 Main content:"
if [ -f "$DOCS_DIR/index.html" ]; then
    echo "   ✅ index.html (homepage)"
else
    echo "   ⚠️  index.html NOT FOUND"
fi

if [ -d "$DOCS_DIR/documentation" ]; then
    DOC_COUNT=$(find "$DOCS_DIR/documentation" -name "*.html" | wc -l | tr -d ' ')
    echo "   ✅ documentation/ (${DOC_COUNT} HTML files)"
else
    echo "   ⚠️  documentation/ directory NOT FOUND"
fi

if [ -d "$DOCS_DIR/examples" ]; then
    EXAMPLE_COUNT=$(find "$DOCS_DIR/examples" -name "*.xml" | wc -l | tr -d ' ')
    echo "   ✅ examples/ (${EXAMPLE_COUNT} XML files)"
else
    echo "   ⚠️  examples/ directory NOT FOUND"
fi

echo ""

# SSH key check
if [ ! -f ~/.ssh/sourceforge_rsa ]; then
    echo "⚠️  Warning: SSH key not found at ~/.ssh/sourceforge_rsa"
    echo "   You may need to enter password for SFTP connection."
    echo ""
fi

# Dry-run mode
if [ "$DRY_RUN" = true ]; then
    echo "🔍 DRY-RUN: This is what would be uploaded:"
    echo ""
    echo "   Source:      ${DOCS_DIR}/"
    echo "   Destination: ${SF_USER}@${SF_HOST}:${SF_PATH}"
    echo "   Files:       ${FILE_COUNT} files (${TOTAL_SIZE})"
    echo ""
    echo "💡 To perform actual upload, run without --dry-run flag"
    exit 0
fi

# Confirmation prompt
echo "🚨 Ready to upload ${FILE_COUNT} files (${TOTAL_SIZE}) to SourceForge"
echo "📁 Destination: ${SF_HOST}:${SF_PATH}"
echo ""
read -p "❓ Continue with upload? (yes/no): " -r
echo

if [[ ! $REPLY =~ ^[Yy][Ee][Ss]$ ]]; then
    echo "❌ Upload cancelled by user."
    exit 0
fi

echo "✅ Starting upload..."
echo ""

# Clean remote directory if requested
if [ "$CLEAN_REMOTE" = true ]; then
    echo ""
    echo "⚠️  WARNING: --clean-remote mode does NOT work with SourceForge!"
    echo ""
    echo "SourceForge uses a 'restricted Shell Account' that only allows"
    echo "file transfers (SFTP/rsync), not SSH commands like 'rm -rf'."
    echo ""
    echo "💡 SOLUTION: Use normal mode instead:"
    echo "   ./upload-docs.sh"
    echo ""
    echo "Normal mode uses 'rsync --delete' which achieves the same result:"
    echo "  • Removes remote files not in docs/web/ (old javadoc, obsolete files)"
    echo "  • Uploads new/updated files"
    echo "  • Safe and reliable"
    echo ""
    echo "═══════════════════════════════════════════════════════════════"
    echo ""
    read -p "❓ Continue anyway and attempt clean-remote? (yes/no): " -r
    echo
    
    if [[ ! $REPLY =~ ^[Yy][Ee][Ss]$ ]]; then
        echo "❌ Cancelled. Use: ./upload-docs.sh (normal mode)"
        exit 0
    fi
    
    echo ""
    echo "🚨🚨🚨 DANGER ZONE 🚨🚨🚨"
    echo "═══════════════════════════════════════════════════════════════"
    echo ""
    echo "⚠️  Attempting to DELETE ALL FILES from SourceForge website!"
    echo "   (This will likely FAIL due to restricted Shell Account)"
    echo ""
    echo "📁 Remote path: ${SF_HOST}:${SF_PATH}"
    echo "🌐 Website:     https://web-harvest.sourceforge.io/"
    echo ""
    echo "This will:"
    echo "  • Delete all HTML, CSS, JS, image files"
    echo "  • Delete all documentation and examples"
    echo "  • Make the website temporarily inaccessible"
    echo "  • Cannot be undone without backup!"
    echo ""
    echo "⚠️  Use this ONLY when:"
    echo "  • You want to start fresh with new structure"
    echo "  • You have backup or can regenerate all files"
    echo "  • You know what you're doing!"
    echo ""
    echo "═══════════════════════════════════════════════════════════════"
    echo ""
    
    read -p "❓ Are you ABSOLUTELY SURE you want to delete ALL remote files? (type 'DELETE' to confirm): " -r
    echo
    
    if [[ ! $REPLY == "DELETE" ]]; then
        echo "❌ Clean cancelled. Files NOT deleted."
        echo "💡 To upload without cleaning, run: ./upload-docs.sh"
        exit 0
    fi
    
    echo ""
    echo "💾 Creating backup before deletion..."
    TIMESTAMP=$(date +%Y%m%d_%H%M%S)
    BACKUP_FILE="../htdocs_backup_before_clean_${TIMESTAMP}.tar.gz"
    
    ssh -i ~/.ssh/sourceforge_rsa "${SF_USER}@${SF_HOST}" \
        "cd ${SF_PATH} && tar -czf ${BACKUP_FILE} . 2>/dev/null && echo 'Backup created: ${BACKUP_FILE}'" || {
        echo "⚠️  Warning: Could not create backup (remote directory may be empty)"
    }
    
    echo ""
    echo "🗑️  Deleting all remote files..."
    ssh -i ~/.ssh/sourceforge_rsa "${SF_USER}@${SF_HOST}" \
        "cd ${SF_PATH} && rm -rf * .??*" 2>/dev/null || {
        echo "❌ Error: Could not delete remote files"
        exit 1
    }
    
    echo "✅ Remote directory cleaned!"
    echo "💾 Backup available at: ${SF_HOST}:${BACKUP_FILE}"
    echo ""
fi

# Upload using rsync with SSH key
echo "📤 Uploading files to SourceForge..."
echo "🔐 You may be prompted for SSH key passphrase..."
echo ""

RSYNC_OPTS="-avz --delete"
if command -v pv &> /dev/null; then
    # Use pv for progress if available
    rsync $RSYNC_OPTS -e "ssh -i ~/.ssh/sourceforge_rsa" \
        "$DOCS_DIR/" "${SF_USER}@${SF_HOST}:${SF_PATH}/" \
        | pv -lep -s $FILE_COUNT > /dev/null
else
    # Standard rsync with progress
    rsync $RSYNC_OPTS --progress -e "ssh -i ~/.ssh/sourceforge_rsa" \
        "$DOCS_DIR/" "${SF_USER}@${SF_HOST}:${SF_PATH}/"
fi

# Check if upload was successful
if [ $? -eq 0 ]; then
    echo ""
    echo "✅ Documentation uploaded successfully!"
    echo ""
    
    # Verify upload
    echo "🔍 Verifying upload..."
    REMOTE_COUNT=$(ssh -i ~/.ssh/sourceforge_rsa "${SF_USER}@${SF_HOST}" \
        "find ${SF_PATH} -type f 2>/dev/null | wc -l" 2>/dev/null | tr -d ' ')
    
    if [ -n "$REMOTE_COUNT" ]; then
        echo "   Remote files: ${REMOTE_COUNT}"
        echo "   Local files:  ${FILE_COUNT}"
        
        if [ "$REMOTE_COUNT" -ge "$FILE_COUNT" ]; then
            echo "   ✅ File count verified"
        else
            echo "   ⚠️  Warning: Remote count lower than expected"
        fi
    else
        echo "   ⚠️  Could not verify remote file count"
    fi
    
    echo ""
    echo "🌐 Your documentation is now available at:"
    echo "   https://web-harvest.sourceforge.io/"
    echo ""
    echo "📋 Next steps:"
    echo "   1. Visit https://web-harvest.sourceforge.io/ to verify"
    echo "   2. Check that examples load correctly"
    echo "   3. Test API documentation navigation"
    echo "   4. Verify all images and CSS are working"
    echo ""
    echo "🎉 Upload complete!"
else
    echo ""
    echo "❌ Upload failed!"
    echo ""
    echo "💡 Troubleshooting:"
    echo "   - Check SSH key: ls -lh ~/.ssh/sourceforge_rsa"
    echo "   - Test connection: ssh -i ~/.ssh/sourceforge_rsa ${SF_USER}@${SF_HOST}"
    echo "   - Check permissions: https://sourceforge.net/projects/web-harvest/admin/members/"
    echo ""
    exit 1
fi
