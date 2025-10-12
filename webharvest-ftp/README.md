# WebHarvest FTP Module

FTP/FTPS file transfer support for WebHarvest scraping framework.

## Features

- **FTP Operations**: Get, put, list files on FTP servers
- **FTPS Support**: Secure FTP over SSL/TLS
- **Recursive Operations**: List directories recursively
- **Passive Mode**: Firewall-friendly passive FTP
- **Authentication**: Username/password authentication

## Plugins

### 1. FtpPlugin (`<ftp>`)

Main FTP connection container - defines connection parameters for child operations.

#### Attributes:

| Attribute | Required | Description | Example |
|-----------|----------|-------------|---------|
| `host` | Yes | FTP server hostname | `ftp.example.com` |
| `port` | No | FTP port (default: 21) | `21` |
| `username` | Yes | FTP username | `ftpuser` |
| `password` | Yes | FTP password | `secret` |
| `passive` | No | Use passive mode (default: true) | `true` |

### 2. FtpGetPlugin (`<ftp-get>`)

Download files from FTP server.

#### Attributes:

| Attribute | Required | Description | Example |
|-----------|----------|-------------|---------|
| `path` | Yes | Remote file path | `/data/file.txt` |
| `type` | No | Transfer type: `text` or `binary` | `binary` |

### 3. FtpPutPlugin (`<ftp-put>`)

Upload files to FTP server.

#### Attributes:

| Attribute | Required | Description | Example |
|-----------|----------|-------------|---------|
| `path` | Yes | Remote file path (destination) | `/upload/data.xml` |
| `type` | No | Transfer type: `text` or `binary` | `binary` |

### 4. FtpListPlugin (`<ftp-list>`)

List files in FTP directory.

#### Attributes:

| Attribute | Required | Description | Example |
|-----------|----------|-------------|---------|
| `path` | Yes | Remote directory path | `/data/` |

## Usage Examples

### Example 1: Download File from FTP

```xml
<ftp host="ftp.example.com" 
     username="user" 
     password="pass">
    
    <def var="fileContent">
        <ftp-get path="/data/products.xml"/>
    </def>
    
    <!-- Save locally -->
    <file path="products.xml" action="write">
        ${fileContent}
    </file>
</ftp>
```

### Example 2: Upload Scraped Data to FTP

```xml
<!-- Scrape data -->
<def var="scrapedData">
    <http url="https://example.com/api/data"/>
</def>

<!-- Upload to FTP server -->
<ftp host="ftp.backup.com" 
     username="backup_user" 
     password="backup_pass"
     port="21">
    
    <ftp-put path="/backups/data-${_date}.json">
        ${scrapedData}
    </ftp-put>
</ftp>
```

### Example 3: Download Binary Files (Images)

```xml
<ftp host="ftp.images.com" 
     username="imguser" 
     password="imgpass">
    
    <!-- Download image -->
    <def var="imageData">
        <ftp-get path="/images/logo.png" type="binary"/>
    </def>
    
    <!-- Save to local file -->
    <file path="logo.png" action="write" type="binary">
        <get-var var="imageData"/>
    </file>
</ftp>
```

### Example 4: List FTP Directory

```xml
<ftp host="ftp.example.com" 
     username="user" 
     password="pass">
    
    <!-- List all files in directory -->
    <def var="fileList">
        <ftp-list path="/data/"/>
    </def>
    
    <!-- Process each file -->
    <loop item="filename">
        ${fileList}
        
        <ftp-get path="/data/${filename}"/>
    </loop>
</ftp>
```

### Example 5: FTPS (Secure FTP over SSL)

```xml
<ftp host="ftps.example.com" 
     port="990"
     username="secure_user" 
     password="secure_pass">
    
    <ftp-get path="/secure/data.xml"/>
</ftp>
```

## Supported FTP Servers

- **Pure-FTPd**
- **vsftpd**
- **ProFTPD**
- **FileZilla Server**
- **Windows IIS FTP**
- Any RFC 959 compliant FTP server

## Dependencies

```xml
<dependency>
    <groupId>org.webharvest</groupId>
    <artifactId>webharvest-ftp</artifactId>
    <version>2.2.0</version>
</dependency>
```

External dependencies (auto-included):
- **Apache Commons Net** - FTP protocol implementation

## Common Use Cases

1. **Backup scraped data**: Upload results to FTP backup server
2. **Download source files**: Get CSV/XML files from FTP for processing
3. **Archive old data**: Move processed files to FTP archive
4. **Data exchange**: Upload/download files to/from partner FTP servers
5. **Distributed scraping**: Coordinate scrapers via FTP file locks

## Testing

Run tests:
```bash
cd webharvest-ftp
mvn test
```

## Troubleshooting

### Connection refused:
- Verify FTP server is running
- Check port (default: 21)
- Verify firewall allows FTP connections

### Passive mode issues:
- Try setting `passive="false"` for active mode
- Check firewall allows FTP data connections

### Permission denied:
- Verify username/password are correct
- Check user has read/write permissions on remote path

## License

Copyright (c) 2006-2025 WebHarvest Team. See LICENSE file for details.

