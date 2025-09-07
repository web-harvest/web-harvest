#!/bin/bash

# Script to upload Web-Harvest documentation to SourceForge SFTP
# Usage: ./upload-docs.sh

echo "🚀 Uploading Web-Harvest documentation to SourceForge..."

# Check if sftp is available
if ! command -v sftp &> /dev/null; then
    echo "❌ Error: sftp command not found. Please install OpenSSH client."
    exit 1
fi

# Check if documentation directory exists
if [ ! -d "web-harvest-docs-upload" ]; then
    echo "❌ Error: Documentation directory 'web-harvest-docs-upload' not found."
    echo "Please run the build process first."
    exit 1
fi

# Create SFTP batch file
cat > sftp-upload.batch << EOF
cd /home/project-web/web-harvest/htdocs
put -r web-harvest-docs-upload/* .
quit
EOF

echo "📁 Uploading files to /home/project-web/web-harvest/htdocs/..."
echo "🔐 You will be prompted for your SourceForge password."

# Upload using rsync with SSH key
echo "🔐 You will be prompted for your SSH key passphrase."
rsync -avz -e "ssh -i ~/.ssh/sourceforge_rsa" web-harvest-docs-upload/ rbala@web.sourceforge.net:/home/project-web/web-harvest/htdocs/

# Check if upload was successful
if [ $? -eq 0 ]; then
    echo "✅ Documentation uploaded successfully!"
    echo "🌐 Your documentation should be available at:"
    echo "   https://web-harvest.sourceforge.net/"
else
    echo "❌ Upload failed. Please check your credentials and try again."
    exit 1
fi

# Clean up
rm -f sftp-upload.batch

echo "🎉 Upload complete!"
