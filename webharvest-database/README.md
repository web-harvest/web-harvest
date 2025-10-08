# Web-Harvest Database Module

Database support for Web-Harvest scraping framework with connection pooling and named connections.

## Features

- **Named Connections**: Define multiple database connections with unique identifiers
- **Connection Pooling**: Automatic connection pooling for better performance
- **Multiple Databases**: Support for multiple concurrent database connections
- **Backward Compatible**: Supports both named and inline connection configurations

## Plugins

### 1. ConnectionPlugin (`<connection>`)

Defines a named database connection with connection pooling.

#### Attributes:

| Attribute | Required | Description | Example |
|-----------|----------|-------------|---------|
| `id` | Yes | Connection identifier | `mainDB` |
| `driver` | Yes | JDBC driver class name | `org.postgresql.Driver` |
| `url` | Yes | JDBC connection URL | `jdbc:postgresql://localhost/mydb` |
| `username` | No | Database username | `dbuser` |
| `password` | No | Database password | `secret` |
| `poolSize` | No | Connection pool size (default: 10) | `20` |

#### Example:

```xml
<connection id="mainDB" 
            driver="org.postgresql.Driver"
            url="jdbc:postgresql://localhost:5432/mydb"
            username="dbuser"
            password="secret"
            poolSize="10"/>
```

### 2. DatabasePlugin (`<database>`)

Executes SQL queries using named or inline connections.

#### Attributes:

| Attribute | Required | Description | Example |
|-----------|----------|-------------|---------|
| `connection` | No* | Named connection ID | `mainDB` |
| `driver` | No* | JDBC driver (inline mode) | `org.h2.Driver` |
| `url` | No* | JDBC URL (inline mode) | `jdbc:h2:mem:test` |
| `username` | No | Database username | `sa` |
| `password` | No | Database password | `` |
| `sql` | No** | SQL query | `SELECT * FROM users` |

\* Either `connection` OR (`driver` + `url`) is required  
\*\* SQL can be in attribute or element body

## Usage Examples

### Example 1: Named Connection (Recommended)

```xml
<config>
    <!-- Define connection once -->
    <connection id="mainDB" 
                driver="org.postgresql.Driver"
                url="jdbc:postgresql://localhost/mydb"
                username="user"
                password="pass"/>
    
    <!-- Use it multiple times -->
    <database connection="mainDB" 
              sql="SELECT * FROM users"/>
    
    <database connection="mainDB" 
              sql="INSERT INTO logs VALUES ('action', NOW())"/>
</config>
```

### Example 2: Multiple Named Connections

```xml
<config>
    <!-- Production database -->
    <connection id="prodDB" 
                driver="org.postgresql.Driver"
                url="jdbc:postgresql://prod-server/production"
                username="prod_user"
                password="prod_pass"
                poolSize="20"/>
    
    <!-- Reporting database -->
    <connection id="reportDB"
                driver="com.mysql.jdbc.Driver"
                url="jdbc:mysql://report-server/reports"
                username="report_user"
                password="report_pass"
                poolSize="5"/>
    
    <!-- Query production -->
    <database connection="prodDB" 
              sql="SELECT * FROM orders WHERE date = CURRENT_DATE"/>
    
    <!-- Insert into reports -->
    <database connection="reportDB" 
              sql="INSERT INTO daily_stats SELECT ..."/>
</config>
```

### Example 3: Inline Connection (Backward Compatibility)

```xml
<config>
    <!-- Old style - direct connection parameters -->
    <database driver="org.h2.Driver"
              url="jdbc:h2:mem:testdb"
              username="sa"
              password=""
              sql="SELECT * FROM table"/>
</config>
```

### Example 4: SQL in Body

```xml
<config>
    <connection id="myDB" driver="org.h2.Driver" url="jdbc:h2:mem:test"/>
    
    <database connection="myDB">
        SELECT u.name, u.email, o.total
        FROM users u
        JOIN orders o ON u.id = o.user_id
        WHERE o.date &gt;= '2025-01-01'
        ORDER BY o.total DESC
        LIMIT 100
    </database>
</config>
```

## Supported Databases

The module supports any JDBC-compliant database:

- **PostgreSQL**: `org.postgresql.Driver`
- **MySQL**: `com.mysql.jdbc.Driver` or `com.mysql.cj.jdbc.Driver`
- **H2**: `org.h2.Driver`
- **Oracle**: `oracle.jdbc.driver.OracleDriver`
- **SQL Server**: `com.microsoft.sqlserver.jdbc.SQLServerDriver`
- **SQLite**: `org.sqlite.JDBC`

## Architecture

```
webharvest-database/
├─ org.webharvest.runtime.database/
│  ├─ ConnectionFactory (interface)
│  ├─ StandaloneConnectionPool (connection pooling)
│  ├─ JNDIConnectionFactory (Java EE support)
│  ├─ ConnectionProxy
│  ├─ DriverManager (driver management)
│  └─ DefaultDriverManager
└─ org.webharvest.plugin.db/
   ├─ ConnectionPlugin (named connections)
   └─ DatabasePlugin (SQL execution)
```

## Benefits

1. **Connection Pooling**: Automatic connection reuse for better performance
2. **Named Connections**: Define once, use many times
3. **Multiple Databases**: Support for multiple concurrent connections
4. **Clean Configuration**: Separate connection definition from usage
5. **Backward Compatible**: Works with existing inline connection syntax

## Migration Guide

### From Inline to Named Connections:

**Before:**
```xml
<database driver="org.h2.Driver" url="jdbc:h2:mem:db" sql="SELECT * FROM t1"/>
<database driver="org.h2.Driver" url="jdbc:h2:mem:db" sql="SELECT * FROM t2"/>
<database driver="org.h2.Driver" url="jdbc:h2:mem:db" sql="SELECT * FROM t3"/>
```

**After:**
```xml
<connection id="myDB" driver="org.h2.Driver" url="jdbc:h2:mem:db"/>

<database connection="myDB" sql="SELECT * FROM t1"/>
<database connection="myDB" sql="SELECT * FROM t2"/>
<database connection="myDB" sql="SELECT * FROM t3"/>
```

**Benefits:**
- Connection pooling (reuses connections)
- DRY principle (define connection once)
- Easier maintenance (change connection in one place)
- Better performance (connection reuse)

## Testing

Run tests:
```bash
cd webharvest-database
mvn test
```

Current test coverage: **29 tests, 0 failures**

## License

Copyright (c) 2006-2025 Web-Harvest Team. See LICENSE file for details.

