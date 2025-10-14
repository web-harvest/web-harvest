# Session Continuation Prompt - Maven Dependencies Cleanup

## Context Summary

### What Was Done (October 11, 2025, r742)

**Last completed task: IDE XPath Examples Fixed**
- Fixed all IDE built-in examples to use correct XPath syntax (`/text()` for text extraction)
- Updated 5 JavaScript functions in `app.js` (13 XPath expressions)
- Updated 3 XML example files in `webapp/examples/`
- Created `IDEExamplesIntegrationTest.java` (3/3 tests PASS)
- Updated CHANGELOG.md and CHANGELOG_IDE.md
- **Commit r742** completed successfully
- **All tests pass: 2427/2427 ✅**
- **SVN status: clean** (no uncommitted changes)

### Previous Major Achievements
- r741: Fixed XPath examples in docs/web/examples/ and web-harvest-docs-upload/examples/
- r740: Created XPATH_BEST_PRACTICES.md guide
- Fixed critical bugs: HttpPlugin dependency injection, BeanShell type conversion
- Migrated from old architecture to ULTRA-LEAN architecture (v2.2.x)
- 100% test pass rate maintained throughout

---

## New Task: Maven Dependencies Cleanup & Update

### Objective
Analyze and refactor Maven dependencies across all WebHarvest modules to:
1. **Move module-specific dependencies** from `webharvest-core` to their respective modules
2. **Remove unused dependencies** 
3. **Update dependency versions** to latest stable (EXCLUDING httpclient - do not touch!)
4. **Verify build and tests** after each change

### Key Requirements
⚠️ **CRITICAL:**
- **DO NOT TOUCH httpclient** - user explicitly excluded it
- **Build & test after EACH change** - don't batch multiple changes
- **Commit after EACH successful change** - one logical change per commit
- **Use TDD approach** - if tests fail, write unit test first, then fix
- **All 2427 tests MUST pass** after each change

### Project Structure
```
web-harvest/ (r742)
├── pom.xml (parent)
├── webharvest-core/pom.xml        ← PRIMARY FOCUS (cleanup here)
├── webharvest-cli/pom.xml
├── webharvest-ide/pom.xml
├── webharvest-database/pom.xml    ← Move database deps here
├── webharvest-mail/pom.xml        ← Move mail deps here
├── webharvest-ftp/pom.xml         ← Move FTP deps here
├── webharvest-zip/pom.xml         ← Move zip deps here
├── webharvest-webbrowser/pom.xml
└── docs/pom.xml
```

---

## Execution Plan

### Phase 1: Inventory & Analysis (START HERE)

**Commands to run:**
```bash
cd /Users/robertbala/Documents/Svn/web-harvest

# Generate dependency reports
cd webharvest-core
mvn dependency:tree > /tmp/core-deps-tree.txt
mvn dependency:analyze > /tmp/core-deps-analyze.txt

# Check each module
for module in webharvest-database webharvest-mail webharvest-ftp webharvest-zip; do
    cd /Users/robertbala/Documents/Svn/web-harvest/$module
    mvn dependency:tree > /tmp/${module}-deps.txt
done
```

**Analyze webharvest-core/pom.xml:**
```bash
# Read full dependencies section
grep -A 200 "<dependencies>" webharvest-core/pom.xml | less
```

**Identify candidates for moving:**
- `commons-dbutils` → webharvest-database
- `commons-email` → webharvest-mail  
- `commons-net` → webharvest-ftp

**Identify potential duplicates/unused:**
- `commons-lang` vs `commons-lang3` (both present?)
- `saxon` (appears twice?)
- `cglib-nodep` (only used in tests?)
- `commons-collections` (unused?)
- `reflections` (unused?)

---

### Phase 2: Move Database Dependencies

**Step-by-step:**

1. **Verify usage in database module:**
```bash
grep -r "import org.apache.commons.dbutils" webharvest-database/src/
```

2. **Add to webharvest-database/pom.xml:**
```xml
<dependency>
    <groupId>commons-dbutils</groupId>
    <artifactId>commons-dbutils</artifactId>
    <version>${commons-dbutils.version}</version>
</dependency>
```

3. **Remove from webharvest-core/pom.xml**

4. **Build & verify:**
```bash
cd /Users/robertbala/Documents/Svn/web-harvest
mvn clean install -DskipTests
cd webharvest-core && mvn test
cd ../webharvest-database && mvn test
cd .. && mvn test  # Run all tests
```

5. **If all tests pass (2427/2427), commit:**
```bash
svn commit -m "Dependencies: Moved commons-dbutils from core to database module

- commons-dbutils is only used in webharvest-database
- Removed from webharvest-core/pom.xml dependencies
- Added to webharvest-database/pom.xml
- All tests pass: 2427/2427

This improves module separation and reduces unnecessary dependencies in core."
```

---

### Phase 3: Move Mail Dependencies

**Repeat similar process for:**
- `commons-email` → webharvest-mail
- Verify with grep, move dependency, build, test, commit

---

### Phase 4: Move FTP Dependencies

**Repeat for:**
- `commons-net` → webharvest-ftp

---

### Phase 5: Remove Unused Dependencies

**For each suspected unused dependency:**

1. **Check usage:**
```bash
# Example for commons-collections
grep -r "import org.apache.commons.collections" webharvest-core/src/main/
grep -r "import org.apache.commons.collections" webharvest-core/src/test/

# If no results, likely unused
```

2. **Check transitive dependencies:**
```bash
mvn dependency:tree | grep commons-collections
# If shown as transitive, don't need explicit declaration
```

3. **Remove from pom.xml**

4. **Build & test:**
```bash
mvn clean install
mvn test
```

5. **If successful, commit with explanation**

**Candidates to check:**
- commons-lang (if commons-lang3 covers all uses)
- cglib-nodep
- commons-collections
- reflections
- Duplicate saxon entries

---

### Phase 6: Update Dependency Versions

⚠️ **EXCLUDING httpclient!**

**Use Maven versions plugin:**
```bash
mvn versions:display-dependency-updates | grep -v httpclient
```

**Update one at a time:**

1. **Check Maven Central for latest stable:**
   - jsoup: 1.17.2 → latest?
   - jackson-databind: 2.15.2 → latest?
   - commons-text → latest?
   - commons-lang3 → latest?
   - etc.

2. **Update in pom.xml**

3. **Build & test**

4. **Commit each update separately**

**Example commit:**
```
Dependencies: Updated jsoup from 1.17.2 to 1.18.1

- jsoup updated to latest stable version
- All tests pass: 2427/2427
- No breaking changes detected
```

---

## Important Notes

### User Preferences (Memories)
- [[memory:8336419]] Documentation in English
- [[memory:8336415]] Project uses SVN (not git)
- [[memory:8336412]] Test outputs go to module's target/ directory
- [[memory:7122435]] After code changes: compile first, then run via appropriate method

### TDD Approach Required
If any test fails:
1. Identify the problem
2. Write a unit test that reproduces the issue
3. Run test to confirm it fails
4. Fix the issue
5. Run unit test (should pass)
6. Run integration tests (should pass)
7. Commit

### Commit Guidelines
- Use English for commit messages
- One logical change per commit
- Include test results in commit message
- Reference issue/feature if applicable

### Verification Commands
```bash
# After each change, run:
mvn clean install          # Build all modules
mvn test                   # All tests (should be 2427/2427)
mvn verify                 # Full verification

# Check SVN status
svn status                 # Should show only intended changes

# View diff before commit
svn diff webharvest-core/pom.xml
```

---

## Expected Outcomes

By end of session, achieve:
- ✅ Module-specific dependencies moved to correct modules
- ✅ Unused dependencies removed from core
- ✅ Dependency versions updated (except httpclient)
- ✅ All modules build successfully
- ✅ All tests pass: 2427/2427
- ✅ Clean commits with proper documentation
- ✅ Updated DEPENDENCIES_CLEANUP_PLAN.md with results

---

## Files to Reference

1. **DEPENDENCIES_CLEANUP_PLAN.md** - Detailed analysis plan
2. **webharvest-core/pom.xml** - Primary file to clean up
3. **pom.xml** (parent) - Check for version properties
4. Module pom.xml files - Where to move dependencies

## Commands Quick Reference

```bash
# Navigate to project
cd /Users/robertbala/Documents/Svn/web-harvest

# Analyze dependencies
mvn dependency:tree
mvn dependency:analyze
mvn versions:display-dependency-updates

# Check usage in code
grep -r "import package.name" webharvest-core/src/

# Build & test
mvn clean install -DskipTests  # Fast build
mvn test                       # Run tests
mvn clean verify               # Full verification

# SVN operations
svn status                     # Check status
svn diff pom.xml              # View changes
svn commit -F /tmp/msg.txt    # Commit with message file
```

---

## Start Here

```
Hi! I'm continuing work on the WebHarvest project.

Previous session (r742) successfully fixed IDE XPath examples. All tests pass (2427/2427), SVN is clean.

New task: Maven dependencies cleanup and version updates.

I've reviewed DEPENDENCIES_CLEANUP_PLAN.md. Let me start with Phase 1:

1. Generate dependency analysis reports
2. Identify dependencies in webharvest-core that should be moved to specific modules
3. Check for unused dependencies

Starting analysis now...
```

---

**This prompt provides complete context for seamless continuation!** 🚀

