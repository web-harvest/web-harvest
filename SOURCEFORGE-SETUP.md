# SourceForge Project Setup Guide

## 📋 Step 1: Project Information

- **Project Name**: Web-Harvest
- **Project URL**: https://sourceforge.net/projects/web-harvest/
- **Web URL**: https://web-harvest.sourceforge.io/
- **SVN**: https://svn.code.sf.net/p/web-harvest/code/

## 📋 Step 2: Verify Project Permissions

### Required Permissions
1. Navigate to: https://sourceforge.net/projects/web-harvest/admin/
2. Verify you are a team member
3. Check your role (Admin, Developer, etc.)

### Permission Levels
- **Admin**: Full access to all project areas
- **Developer**: Code commit, file release, web hosting
- **Member**: Limited access

## 🌐 Step 3: Web Hosting Setup

### Enable Web Hosting

1. Go to Project Admin → Features
2. Enable "Web" feature if not already enabled
3. Configure web hosting settings

### Web Directory Structure

```
/home/project-web/web-harvest/htdocs/
├── index.html          # Homepage
├── api/                # Javadoc
├── download/           # Downloads
├── examples/           # Examples
└── css/, js/, images/  # Assets
```

### SFTP Access

**Host**: `web.sourceforge.net`  
**User**: `YOUR_SOURCEFORGE_USERNAME`  
**Path**: `/home/project-web/web-harvest/htdocs/`

**Example**:
```bash
sftp rbala@web.sourceforge.net
cd /home/project-web/web-harvest/htdocs
```

## 📦 Step 4: File Release System

### Creating a Release

1. Build distribution packages:
```bash
mvn clean install
```

2. Navigate to: https://sourceforge.net/projects/web-harvest/files/

3. Create new folder for version (e.g., "2.2.0")

4. Upload files:
   - `webharvest-2.2.0.zip`
   - `webharvest-2.2.0.tar.gz`
   - `webharvest-cli-2.2.0.jar`
   - `webharvest-ide-2.2.0.jar`

5. Set default download for each platform

6. Add release notes

### Release File Types

- **Binary Distribution**: For end users
- **Source Distribution**: For developers
- **CLI JAR**: Command-line tool
- **IDE JAR**: GUI application
- **Javadoc**: API documentation

## 🔐 Step 5: SSH Key Setup

### Add SSH Key for SVN+SSH

1. Generate SSH key (if you don't have one):
```bash
ssh-keygen -t rsa -b 4096 -C "your_email@example.com"
```

2. Add public key to SourceForge:
   - Go to: https://sourceforge.net/auth/shell_services
   - Click "Edit SSH Keys for Shell/CVS"
   - Paste your public key (~/.ssh/id_rsa.pub)

3. Test connection:
```bash
ssh -T USERNAME@shell.sourceforge.net
```

### SVN Access Methods

**Read-Only (HTTPS)**:
```bash
svn checkout https://svn.code.sf.net/p/web-harvest/code/trunk web-harvest
```

**Read-Write (SVN+SSH)** - requires SSH key:
```bash
svn checkout svn+ssh://USERNAME@svn.code.sf.net/p/web-harvest/code/trunk web-harvest
```

## 📊 Step 6: Project Statistics

### Enable Statistics Tracking

1. Go to Project Admin → Statistics
2. Enable download statistics
3. Enable web statistics
4. Configure tracking options

### View Statistics

- **Downloads**: https://sourceforge.net/projects/web-harvest/files/stats/timeline
- **Web Traffic**: Project Admin → Statistics
- **SVN Activity**: Project Admin → Code → Statistics

## 🔔 Step 7: Communication Channels

### Mailing Lists

1. Create/manage lists at: https://sourceforge.net/p/web-harvest/mailman/
2. Recommended lists:
   - `web-harvest-users` - User discussions
   - `web-harvest-dev` - Developer discussions
   - `web-harvest-commits` - SVN commit notifications

### Issue Tracker

**URL**: https://sourceforge.net/p/web-harvest/tickets/

**Configuration**:
1. Go to Project Admin → Tools → Tickets
2. Configure ticket fields
3. Set up milestones
4. Define ticket workflow

## 📝 Step 8: Project Description

### Update Project Info

1. Go to Project Admin → Overview
2. Update:
   - Short description
   - Full description
   - Project categories
   - Tags/keywords
   - License information
   - Development status

### Recommended Tags
- web-scraping
- data-extraction
- java
- xml
- automation

## 🎯 Step 9: Verification Checklist

- [ ] Web hosting enabled and accessible
- [ ] SFTP access works
- [ ] SVN repository accessible
- [ ] SSH key added (for commit access)
- [ ] File release system configured
- [ ] Mailing lists created
- [ ] Issue tracker configured
- [ ] Project description updated
- [ ] Statistics enabled
- [ ] Documentation uploaded

## 🚨 Troubleshooting

### Cannot Access Web Hosting
- **Solution**: Verify you're a project member with Developer role or higher
- **Check**: Project Admin → Members

### SFTP Connection Fails
- **Solution**: Verify username and SSH key
- **Check**: https://sourceforge.net/auth/shell_services

### SVN Commit Fails
- **Solution**: Ensure you have commit permissions
- **Check**: Project Admin → Members → Your permissions

### File Upload Fails
- **Solution**: Check file size limits and available space
- **Limit**: 500 MB per file, check quota in Project Admin

---

**Last Updated**: October 8, 2025  
**SourceForge Project**: https://sourceforge.net/projects/web-harvest/  
**Support**: https://sourceforge.net/support
