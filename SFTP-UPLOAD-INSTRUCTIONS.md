# 📤 SourceForge Documentation Upload Instructions

## 🚀 Quick Upload (Automated)

```bash
./upload-docs.sh
```

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

### Permission Issues
- Verify you have permissions to `/home/project-web/web-harvest/htdocs` directory
- Check if you're a member of the web-harvest project team
- Contact SourceForge support if needed

### Upload Failures
- Check network connectivity
- Verify SFTP credentials
- Try uploading smaller batches
- Check available disk space

### Missing Files
- Verify all files were copied
- Check directory structure
- Ensure all CSS and JS files were uploaded
- Verify file permissions (should be 644)

## 📊 Upload Checklist

- [ ] Generated fresh documentation (`mvn site`)
- [ ] Verified local documentation looks correct
- [ ] Connected to SourceForge SFTP
- [ ] Uploaded all files
- [ ] Verified upload completed
- [ ] Tested website accessibility
- [ ] Checked all links work
- [ ] Verified examples display correctly

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

**Last Updated**: October 8, 2025  
**SourceForge Project**: https://sourceforge.net/projects/web-harvest/
