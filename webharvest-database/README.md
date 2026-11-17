# WebHarvest Database Module

Optional JDBC integration module for WebHarvest. It is not part of the default Core runtime and must be added explicitly by applications that need database access.

## XML contract

Both plugin elements use the Core namespace: `http://org.webharvest/schema/2.1/core`.

### Named connection

Define a connection once, then reference it by its `id`:

```xml
<config xmlns="http://org.webharvest/schema/2.1/core">
  <connection id="main" driver="org.postgresql.Driver"
              url="jdbc:postgresql://localhost/mydb"
              username="dbuser" password="secret"/>
  <database connection="main" sql="SELECT id, name FROM users"/>
</config>
```

### Inline connection

For a single operation, provide both `driver` and `url` directly:

```xml
<database xmlns="http://org.webharvest/schema/2.1/core"
          driver="org.h2.Driver" url="jdbc:h2:mem:example"
          username="sa" password="" sql="SELECT 1 AS result"/>
```

SQL may be supplied using the `sql` attribute or as the element body. Exactly one connection mode is allowed:

- named mode: `connection` only;
- inline mode: both `driver` and `url`.

Do not combine named and inline connection attributes.

## Attributes

| Element | Attribute | Requirement | Meaning |
|---|---|---|---|
| `connection` | `id` | required | Name used by `database connection="..."` |
| `connection` | `driver` | required | JDBC driver class |
| `connection` | `url` | required | JDBC URL |
| `connection` | `username`, `password` | optional | JDBC credentials |
| `connection` | `poolSize` | legacy, ignored | Retained temporarily for configuration compatibility; no configurable pool is implemented |
| `database` | `connection` | named mode | Previously defined connection ID |
| `database` | `driver`, `url` | inline mode | JDBC driver class and URL; both required together |
| `database` | `username`, `password` | inline mode | Optional JDBC credentials |
| `database` | `sql` | optional | SQL statement; alternatively provide element body |

## Runtime behavior and limits

- The module supports JDBC drivers supplied by the application; it does not bundle vendor database drivers.
- `StandaloneConnectionPool` reuses a single connection for a given driver/URL/username within its factory and releases it at session completion/failure. It is not a configurable multi-connection pool and is not thread-safe.
- The legacy `poolSize` attribute is accepted but has no effect. Do not rely on it for concurrency or throughput control.
- SELECT results are returned as `<results><row>...</row></results>`; text values are XML-escaped. Non-query statements return `Rows affected: N`.
- This module's tests use H2 only as a test-scoped embedded JDBC fixture. H2 is not a runtime dependency.

## Build and test

From the repository root:

```bash
mvn --batch-mode --no-transfer-progress -pl webharvest-database -am test
mvn --batch-mode --no-transfer-progress -pl webharvest-database -am verify
```

The module remains optional; the normal Core build does not pull in this module or H2.

## License

See the repository root `LICENSE`, `NOTICE` and module licensing notices.
