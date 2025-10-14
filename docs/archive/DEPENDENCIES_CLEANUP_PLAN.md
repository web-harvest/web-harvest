# Dependencies Cleanup & Update Plan

## Session Context
Date: October 11, 2025
Current revision: r742
Status: Ready to start

## Objective
Analyze and refactor Maven dependencies across all modules:
1. Move module-specific dependencies from `webharvest-core` to their respective modules
2. Remove unused dependencies
3. Update dependency versions to latest stable (excluding httpclient for now)
4. Verify build and tests after each change

## Current Project Structure

```
web-harvest/
├── pom.xml (parent)
├── webharvest-core/pom.xml
├── webharvest-cli/pom.xml
├── webharvest-ide/pom.xml
├── webharvest-database/pom.xml
├── webharvest-mail/pom.xml
├── webharvest-ftp/pom.xml
├── webharvest-zip/pom.xml
├── webharvest-webbrowser/pom.xml
└── docs/pom.xml
```

## Phase 1: Analyze webharvest-core/pom.xml

### Identified Dependencies (from quick scan)
```xml
<!-- Core dependencies -->
- cglib:cglib-nodep
- org.slf4j:slf4j-api
- org.jsoup:jsoup (1.17.2)
- org.json:json
- com.fasterxml.jackson.core:jackson-databind (2.15.2)
- org.ccil.cowan.tagsoup:tagsoup (1.2.1)
- org.apache.tika:tika-core
- org.apache.commons:commons-text
- org.apache.commons:commons-lang3
- commons-lang:commons-lang (OLD - duplicate with commons-lang3?)
- commons-beanutils:commons-beanutils
- commons-io:commons-io

<!-- Suspicious - should be moved? -->
- commons-dbutils:commons-dbutils → webharvest-database?
- commons-collections:commons-collections
- org.apache.commons:commons-email → webharvest-mail?
- commons-net:commons-net → webharvest-ftp?

<!-- Others -->
- org.reflections:reflections
- net.sourceforge.saxon:saxon (appears twice?)
- org.beanshell:bsh
```

## Phase 2: Module-Specific Analysis

### webharvest-database
Expected dependencies:
- commons-dbutils (move from core)
- JDBC drivers (if any)

### webharvest-mail
Expected dependencies:
- commons-email (move from core)
- javax.mail (if any)

### webharvest-ftp
Expected dependencies:
- commons-net (move from core)

### webharvest-zip
Expected dependencies:
- (analyze current pom.xml)

### webharvest-webbrowser
Expected dependencies:
- (analyze current pom.xml)

## Phase 3: Dependency Version Updates

Check for updates (excluding httpclient):
- [ ] jsoup: 1.17.2 → latest?
- [ ] jackson-databind: 2.15.2 → latest?
- [ ] commons-text: check current → latest
- [ ] commons-lang3: check current → latest
- [ ] commons-io: check current → latest
- [ ] tagsoup: 1.2.1 → latest?
- [ ] slf4j: check current → latest
- [ ] guice: check current → latest
- [ ] mockito: check current → latest
- [ ] testng: check current → latest

## Phase 4: Identify Unused Dependencies

For each dependency in core, verify usage:
```bash
# Example command to check if dependency is used
grep -r "import org.apache.commons.dbutils" webharvest-core/src/main/java/
```

Candidates for removal:
- [ ] commons-lang (if commons-lang3 is used)
- [ ] cglib-nodep (if not used by tests)
- [ ] commons-collections (if not used)
- [ ] reflections (if not used)
- [ ] Duplicate saxon entries

## Phase 5: Execution Plan

### Step 1: Create dependency inventory
```bash
cd webharvest-core
mvn dependency:tree > /tmp/core-deps-before.txt
mvn dependency:analyze > /tmp/core-analysis.txt
```

### Step 2: Move database dependencies
1. Add commons-dbutils to webharvest-database/pom.xml
2. Remove commons-dbutils from webharvest-core/pom.xml
3. Build: `mvn clean install -DskipTests`
4. Test: `mvn test`
5. Commit if successful

### Step 3: Move mail dependencies
1. Add commons-email to webharvest-mail/pom.xml
2. Remove commons-email from webharvest-core/pom.xml
3. Build & test
4. Commit if successful

### Step 4: Move FTP dependencies
1. Add commons-net to webharvest-ftp/pom.xml
2. Remove commons-net from webharvest-core/pom.xml
3. Build & test
4. Commit if successful

### Step 5: Remove unused dependencies
1. Identify unused deps using `mvn dependency:analyze`
2. Verify with grep in source code
3. Remove one at a time
4. Build & test after each removal
5. Commit each successful removal

### Step 6: Update versions
1. Check Maven Central for latest versions
2. Update one dependency at a time
3. Build & test after each update
4. Commit each successful update

### Step 7: Final verification
```bash
mvn clean install
mvn test
mvn verify
```

## Commands Reference

### Analyze dependencies
```bash
# Show dependency tree
mvn dependency:tree

# Analyze for unused/undeclared
mvn dependency:analyze

# Check for updates (use versions-maven-plugin)
mvn versions:display-dependency-updates

# Verify usage in code
grep -r "import org.apache.commons.dbutils" webharvest-core/src/
```

### Build & Test
```bash
# Clean build all modules
mvn clean install -DskipTests

# Run tests for specific module
cd webharvest-core && mvn test

# Run all tests
mvn test

# Full verification
mvn clean verify
```

## Success Criteria

- [ ] All module-specific dependencies moved to correct modules
- [ ] All unused dependencies removed from core
- [ ] All dependency versions updated (except httpclient)
- [ ] All modules build successfully
- [ ] All tests pass (2427/2427)
- [ ] No dependency conflicts
- [ ] Proper commit messages documenting changes

## Notes

- **Do NOT touch httpclient** - user explicitly excluded it
- **Build & test after each change** - don't batch changes
- **Commit frequently** - one logical change per commit
- **Use TDD approach** - if tests fail, investigate and fix
- **Check for transitive dependencies** - removing a dep might break if it was provided transitively

## Related Documentation

- Maven Central: https://search.maven.org/
- Dependency analysis: `mvn dependency:analyze`
- Version updates: `mvn versions:display-dependency-updates`

---

**Ready to start in new session!**

