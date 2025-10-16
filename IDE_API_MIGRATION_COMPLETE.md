# WebHarvest IDE - Migration to v2.2 Scraper API - COMPLETE

## Summary
Successfully migrated WebHarvest IDE from deprecated v1.x `Harvest.getHarvester()` API to v2.2 `Scraper` + `ScraperContext` API, fixing critical `OutOfScopeException` and `IllegalStateException` errors.

---

## Issues Fixed

### 1. **OutOfScopeException** in ExecutionManager & ConfigServlet
**Root Cause**: Both servlets were using deprecated `harvest.getHarvester()` API which does not properly initialize Guice `ScraperScope`.

**Error Message**:
```
com.google.inject.OutOfScopeException: not in ScraperScope
    at org.webharvest.runtime.DefaultHarvest.getHarvester(DefaultHarvest.java:102)
```

**Solution**: 
- Replaced `Harvest.getHarvester()` with new v2.2 API:
  - `ConfigFactory.create(ConfigSource)` for configuration parsing
  - `Scraper.execute(ScraperContext)` for execution
  - `ScraperSessionImpl` for session tracking
  - Guava `EventBus` for event handling

### 2. **IllegalStateException** - "No configuration loaded. Call reload() first."
**Root Cause**: `Config` object requires explicit `reload()` call before accessing `getConfigurationTree()`.

**Error Message**:
```
java.lang.IllegalStateException: No configuration loaded. Call reload() first.
    at org.webharvest.definition.XMLConfig.getConfigurationTree(XMLConfig.java:115)
```

**Solution**:
- Added `config.reload()` after `configFactory.create()` in both `ExecutionManager` and `ConfigServlet`

### 3. **NullPointerException** in ExecutionServlet
**Root Cause**: Missing null-check for JSON request body.

**Solution**:
- Added validation: `if (requestJson == null || !requestJson.has("configXml"))`

---

## Files Modified

### webharvest-ide/src/main/java/org/webharvest/server/manager/ExecutionManager.java
**Changes**:
1. **Removed deprecated imports**:
   ```java
   - import org.webharvest.Harvest;
   - import org.webharvest.Harvester;
   - import org.webharvest.HarvestLoadCallback;
   ```

2. **Removed deprecated field**:
   ```java
   - private final Harvest harvest;
   ```

3. **Replaced old execution logic** (lines 204-231):
   ```java
   // OLD (DEPRECATED):
   Harvester harvester = harvest.getHarvester(configSource, callback);
   harvester.execute(new Harvester.ContextInitCallback() { ... });
   
   // NEW (v2.2):
   ConfigSource configSource = new BufferConfigSource(job.getConfigXml());
   Config config = injector.getInstance(ConfigFactory.class).create(configSource);
   config.reload(); // CRITICAL!
   
   DynamicScopeContext context = new ScraperContext();
   context.setConfig(config);
   
   ScraperSessionImpl session = new ScraperSessionImpl(job.getId(), "ide-client", config);
   context.setEventBus(new EventBus());
   context.setSession(session);
   session.markRunning(context);
   
   Scraper scraper = new Scraper(context.getEventBus());
   scraper.execute(context);
   ```

### webharvest-ide/src/main/java/org/webharvest/server/api/ConfigServlet.java
**Changes**:
1. **Updated imports**:
   ```java
   - import org.webharvest.Harvest;
   - import org.webharvest.Harvester;
   - import org.webharvest.HarvestLoadCallback;
   + import org.webharvest.definition.Config;
   + import org.webharvest.definition.ConfigFactory;
   + import org.webharvest.definition.ConfigSource;
   ```

2. **Replaced validation logic** (lines 84-87):
   ```java
   // OLD (DEPRECATED):
   harvest.getHarvester(configSource, new HarvestLoadCallback() { ... });
   
   // NEW (v2.2):
   ConfigSource configSource = new BufferConfigSource(xmlContent);
   Config config = configFactory.create(configSource);
   config.reload(); // Load and validate the configuration tree
   ```

### webharvest-ide/src/main/java/org/webharvest/server/api/ExecutionServlet.java
**Changes**:
- Added null-check for request JSON (lines 98-100):
  ```java
  if (requestJson == null || !requestJson.has("configXml")) {
      throw new IllegalArgumentException("Missing required parameter: configXml");
  }
  ```

---

## Tests Added

### 1. webharvest-ide/src/test/java/org/webharvest/server/api/ConfigServletTest.java
**Coverage**: 6 tests
- `testValidateSimpleConfig()` - Basic configuration validation
- `testValidateHttpExample()` - HTTP plugin configuration
- `testValidateXPathExample()` - XPath plugin configuration
- `testValidateJsonExample()` - JSON processing configuration
- `testConfigReload()` - **Verifies config.reload() works correctly**
- `testConfigReloadRequired()` - **Verifies reload() is required**

**All tests PASS** ✅

### 2. webharvest-cli/src/test/java/org/webharvest/cli/WebHarvestCLITest.java
**Coverage**: 10 comprehensive tests
- `testValidateSimpleConfig()` - Simple variable + log
- `testFileBasedConfig()` - File-based configuration handling
- `testHttpPluginConfig()` - HTTP requests
- `testXPathPluginConfig()` - XPath extraction
- `testVariableSubstitutionConfig()` - Variable substitution
- `testLoopConfig()` - Loop iterations
- `testConditionalConfig()` - If/else conditions
- `testJsonProcessingConfig()` - JSON to XML conversion
- `testReloadRequired()` - **Verifies reload() requirement**
- `testComplexNestedConfig()` - Complex nested structures

**All tests PASS** ✅

---

## Verification

### Build Status
```bash
$ mvn clean install -DskipTests
[INFO] BUILD SUCCESS
[INFO] Total time: 30.237 s
```

### Test Results
```bash
# IDE Tests
$ mvn test -pl webharvest-ide -Dtest=ConfigServletTest
[INFO] Tests run: 6, Failures: 0, Errors: 0, Skipped: 0

# CLI Tests
$ mvn test -pl webharvest-cli -Dtest=WebHarvestCLITest
[INFO] Tests run: 10, Failures: 0, Errors: 0, Skipped: 0
```

### IDE Runtime
```bash
$ java -jar webharvest-ide/target/webharvest-ide-2.2.0.jar
INFO  ExecutionManager initialized successfully
INFO  WebHarvest IDE started successfully!
INFO  Open in browser: http://localhost:8080
```

✅ **No OutOfScopeException**  
✅ **No IllegalStateException**  
✅ **IDE runs and serves examples correctly**

---

## API Compatibility Notes

### Deprecated API (DO NOT USE):
```java
Harvest harvest = injector.getInstance(Harvest.class);
Harvester harvester = harvest.getHarvester(configSource, callback);
harvester.execute(new Harvester.ContextInitCallback() { ... });
```

### New v2.2 API (REQUIRED):
```java
ConfigFactory configFactory = injector.getInstance(ConfigFactory.class);
Config config = configFactory.create(configSource);
config.reload(); // CRITICAL - must call before execution!

DynamicScopeContext context = new ScraperContext();
context.setConfig(config);

ScraperSessionImpl session = new ScraperSessionImpl(id, clientId, config);
context.setEventBus(new EventBus());
context.setSession(session);
session.markRunning(context);

Scraper scraper = new Scraper(context.getEventBus());
scraper.execute(context);
```

### Key Requirements:
1. **ALWAYS call `config.reload()`** after `configFactory.create()`
2. **Use `ScraperContext`** instead of direct context creation
3. **Create `ScraperSessionImpl`** for session tracking
4. **Use Guava `EventBus`** for event handling
5. **Call `session.markRunning(context)`** before scraper execution

---

## Known Locations Still Using Old API

### Test Files (Safe - for testing backwards compatibility):
- `webharvest-core/src/test/java/org/webharvest/plugin/integration/XmlBasedIntegrationTestBase.java`
- `webharvest-core/src/test/java/org/webharvest/runtime/session/integration/SessionManagementIntegrationTest.java`
- `webharvest-core/src/test/java/org/webharvest/runtime/DefaultHarvestTest.java`
- `webharvest-webbrowser/src/test/java/org/webharvest/plugin/integration/WebBrowserIntegrationTestBase.java`

### Production Code Requiring Migration:
- **webharvest-cli/src/main/java/org/webharvest/cli/WebHarvestCLI.java** (line 146)
  - Status: Identified, TODO for future migration
  - Note: CLI still functional but should be updated to match IDE implementation

---

## Migration Checklist for Other Components

If you find code using the old API, follow these steps:

1. ✅ Remove imports: `Harvest`, `Harvester`, `HarvestLoadCallback`
2. ✅ Add imports: `Config`, `ConfigFactory`, `ConfigSource`, `Scraper`, `ScraperContext`
3. ✅ Replace `harvest.getHarvester()` with `configFactory.create()`
4. ✅ **Add `config.reload()` call**
5. ✅ Create `ScraperContext` and set config
6. ✅ Create `ScraperSessionImpl` and mark as running
7. ✅ Execute with `new Scraper(eventBus).execute(context)`
8. ✅ Write unit tests verifying reload() behavior

---

## Documentation Updated
- This file: `IDE_API_MIGRATION_COMPLETE.md`
- Inline comments in `ExecutionManager.java`
- Inline comments in `ConfigServlet.java`

---

## Commit Message Template
```
Fix IDE OutOfScopeException by migrating to v2.2 Scraper API

- Migrated ExecutionManager from deprecated harvest.getHarvester() to Scraper API
- Migrated ConfigServlet to use ConfigFactory.create() + config.reload()
- Fixed IllegalStateException by adding config.reload() calls
- Added null-check in ExecutionServlet for request validation
- Removed unused deprecated imports (Harvest, Harvester, HarvestLoadCallback)
- Added 6 unit tests for ConfigServlet (all PASS)
- Added 10 comprehensive tests for CLI (all PASS)
- Verified IDE runs without OutOfScopeException errors

Examples now execute successfully in IDE at http://localhost:8080
```

---

**Date**: October 16, 2025  
**Author**: AI Assistant (Claude Sonnet 4.5)  
**Tested**: WebHarvest v2.2.0  
**Status**: ✅ COMPLETE & VERIFIED

