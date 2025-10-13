# WebHarvest ZIP Module

ZIP archive creation and extraction support for WebHarvest scraping framework.

## Features

- **Create ZIP Archives**: Package multiple files into ZIP archives
- **Extract ZIP Files**: Unpack ZIP archives
- **Compression**: Standard ZIP compression
- **Multiple Entries**: Add multiple files to single archive
- **Binary Support**: Handle both text and binary files

## Plugins

### 1. ZipPlugin (`<zip>`)

Creates ZIP archive from multiple entries.

#### Attributes:

| Attribute | Required | Description | Example |
|-----------|----------|-------------|---------|
| `path` | Yes | Output ZIP file path | `archive.zip` |

### 2. ZipEntryPlugin (`<zip-entry>`)

Defines a single entry in ZIP archive. Must be used within `<zip>`.

#### Attributes:

| Attribute | Required | Description | Example |
|-----------|----------|-------------|---------|
| `name` | Yes | Entry name inside ZIP | `data/products.xml` |
| `type` | No | Entry type: `text` or `binary` | `text` |

## Usage Examples

### Example 1: Create ZIP Archive

```xml
<zip path="output/scraped-data.zip">
    <!-- Add text file -->
    <zip-entry name="products.xml">
        <http url="https://example.com/products"/>
    </zip-entry>
    
    <!-- Add JSON file -->
    <zip-entry name="data.json">
        <http url="https://api.example.com/data"/>
    </zip-entry>
</zip>
```

### Example 2: Archive Multiple Scraped Pages

```xml
<def var="pages">1,2,3,4,5</def>

<zip path="pages-archive.zip">
    <loop item="page" delimiter=",">
        ${pages}
        
        <def var="pageContent">
            <http url="https://example.com/page/${page}"/>
        </def>
        
        <zip-entry name="page-${page}.html">
            ${pageContent}
        </zip-entry>
    </loop>
</zip>
```

### Example 3: Archive Binary Files (Images)

```xml
<zip path="images.zip">
    <loop item="imageUrl">
        <xpath expression="//img/@src">
            <html-to-xml>
                <http url="https://example.com"/>
            </html-to-xml>
        </xpath>
        
        <def var="imageData">
            <http url="${imageUrl}"/>
        </def>
        
        <zip-entry name="images/image-${_index}.jpg" type="binary">
            <get-var var="imageData"/>
        </zip-entry>
    </loop>
</zip>
```

### Example 4: Daily Backup Archive

```xml
<!-- Scrape multiple sources -->
<def var="source1">
    <http url="https://example.com/data1"/>
</def>

<def var="source2">
    <http url="https://example.com/data2"/>
</def>

<!-- Create dated backup archive -->
<def var="date">
    <script>new Date().toISOString().split('T')[0]</script>
</def>

<zip path="backups/backup-${date}.zip">
    <zip-entry name="source1.xml">${source1}</zip-entry>
    <zip-entry name="source2.xml">${source2}</zip-entry>
    <zip-entry name="metadata.txt">
        Backup created: ${_date}
        Sources: 2
        Status: Complete
    </zip-entry>
</zip>
```

### Example 5: Archive with Directory Structure

```xml
<zip path="export.zip">
    <!-- Create directory structure inside ZIP -->
    <zip-entry name="data/products.xml">
        <http url="https://example.com/products"/>
    </zip-entry>
    
    <zip-entry name="data/users.xml">
        <http url="https://example.com/users"/>
    </zip-entry>
    
    <zip-entry name="logs/scraping.log">
        Scraping started: ${_date}
        Status: Complete
    </zip-entry>
</zip>
```

## Dependencies

```xml
<dependency>
    <groupId>org.webharvest</groupId>
    <artifactId>webharvest-zip</artifactId>
    <version>2.2.0</version>
</dependency>
```

## Common Use Cases

1. **Data export**: Package scraped data for distribution
2. **Backup**: Create compressed backups of scraping results
3. **Email attachments**: Create ZIP archives to email
4. **FTP upload**: Package data before uploading to FTP
5. **Archival**: Long-term storage of scraping results

## Important Notes

- **Compression**: Standard ZIP compression (DEFLATE algorithm)
- **Path separators**: Use forward slashes (/) for ZIP entry paths
- **Binary files**: Use `type="binary"` for images, PDFs, etc.
- **Memory**: Entire ZIP is built in memory - consider size limits
- **Overwrite**: Output file is overwritten if it exists

## Testing

Run tests:
```bash
cd webharvest-zip
mvn test
```

## License

Copyright (c) 2006-2025 WebHarvest Team. See LICENSE file for details.





