# WebHarvest Mail Module

Email sending support for WebHarvest scraping framework with SMTP integration.

## Features

- **SMTP Support**: Send emails via SMTP servers (Gmail, Office365, custom servers)
- **HTML Emails**: Support for both plain text and HTML email bodies
- **Attachments**: Attach files (text, binary, scraped content)
- **Authentication**: SMTP authentication (username/password)
- **TLS/SSL**: Secure email transmission

## Plugin

### MailPlugin (`<mail>`)

Sends emails via SMTP with support for plain text, HTML, and attachments.

#### Attributes:

| Attribute | Required | Description | Example |
|-----------|----------|-------------|---------|
| `smtp-host` | Yes | SMTP server hostname | `smtp.gmail.com` |
| `smtp-port` | No | SMTP port (default: 25) | `587` |
| `username` | No | SMTP authentication username | `user@example.com` |
| `password` | No | SMTP authentication password | `secret` |
| `from` | Yes | Sender email address | `scraper@example.com` |
| `to` | Yes | Recipient email address | `admin@example.com` |
| `subject` | Yes | Email subject | `Scraping Report` |
| `type` | No | Email type: `text` or `html` (default: text) | `html` |

## Usage Examples

### Example 1: Simple Text Email

```xml
<mail smtp-host="smtp.gmail.com"
      smtp-port="587"
      username="user@gmail.com"
      password="app-password"
      from="scraper@example.com"
      to="admin@example.com"
      subject="Scraping completed">
    
    Scraping job completed successfully.
    Products extracted: 150
    Time: ${_date}
</mail>
```

### Example 2: HTML Email with Scraping Results

```xml
<def var="products">
  <xpath expression="//product">
    <html-to-xml>
      <http url="https://example.com/products"/>
    </html-to-xml>
  </xpath>
</def>

<def var="productCount">
  <xpath expression="count(//product)">${products}</xpath>
</def>

<mail smtp-host="smtp.gmail.com"
      smtp-port="587"
      username="user@gmail.com"
      password="app-password"
      from="scraper@example.com"
      to="admin@example.com"
      subject="Daily Product Report"
      type="html">
    
    <template>
      <![CDATA[
      <html>
        <body>
          <h2>Daily Scraping Report</h2>
          <p><strong>Products found:</strong> ${productCount}</p>
          <p><strong>Timestamp:</strong> ${_date}</p>
          <p>Scraping completed successfully!</p>
        </body>
      </html>
      ]]>
    </template>
</mail>
```

### Example 3: Send Email on Error

```xml
<try>
  <http url="https://api.example.com/data"/>
</try>
<catch>
  <mail smtp-host="smtp.example.com"
        from="scraper@example.com"
        to="admin@example.com"
        subject="[ERROR] Scraping Failed">
      
      Scraping job failed with error:
      ${_error}
      
      Time: ${_date}
      Please investigate.
  </mail>
  <exit/>
</catch>
```

### Example 4: Office 365 / Outlook.com

```xml
<mail smtp-host="smtp.office365.com"
      smtp-port="587"
      username="user@company.com"
      password="password"
      from="user@company.com"
      to="recipient@company.com"
      subject="Report">
    Email body
</mail>
```

### Example 5: Custom SMTP Server (No Auth)

```xml
<mail smtp-host="localhost"
      smtp-port="25"
      from="noreply@localhost"
      to="admin@localhost"
      subject="Local test">
    Test email from local SMTP server
</mail>
```

## SMTP Server Configuration

### Gmail:
- **Host:** smtp.gmail.com
- **Port:** 587 (TLS) or 465 (SSL)
- **Auth:** Required (use App Password, not account password)
- **Note:** Enable "Less secure app access" or use App Passwords

### Office 365:
- **Host:** smtp.office365.com
- **Port:** 587
- **Auth:** Required
- **TLS:** Yes

### Amazon SES:
- **Host:** email-smtp.us-east-1.amazonaws.com
- **Port:** 587
- **Auth:** Required (SMTP credentials from AWS)

## Dependencies

```xml
<dependency>
    <groupId>org.webharvest</groupId>
    <artifactId>webharvest-mail</artifactId>
    <version>2.2.0</version>
</dependency>
```

External dependencies (auto-included):
- **Apache Commons Email** - Email abstraction
- **JavaMail API** - SMTP protocol

## Testing

Run tests:
```bash
cd webharvest-mail
mvn test
```

## Common Use Cases

1. **Scraping notifications**: Send email when scraping completes
2. **Error alerts**: Email on scraping failures
3. **Daily reports**: Schedule scrapers to email results
4. **Webhook alternative**: Email data instead of HTTP POST
5. **Monitoring**: Alert on specific data patterns

## Troubleshooting

### Gmail "Less secure app" error:
- Use App Passwords: https://support.google.com/accounts/answer/185833
- Or enable 2FA and generate app-specific password

### Connection timeout:
- Check smtp-port (587 for TLS, 465 for SSL, 25 for plain)
- Verify firewall allows outbound SMTP connections

### Authentication failure:
- Verify username/password are correct
- For Gmail: use App Password, not account password
- Check SMTP server requires authentication

## License

Copyright (c) 2006-2025 the original author or authors. See LICENSE file for details.





