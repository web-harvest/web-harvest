# Visual Mode - Test-Driven Development (TDD)

## Test Coverage

This document describes the TDD approach for the Visual Mode feature in WebHarvest IDE.

### Test Runner

Location: `/static/tests/test-runner.html`

**Access:** `http://localhost:8080/static/tests/test-runner.html`

### Test Suites

#### 1. SimpleXMLParser Tests
Tests for parsing XML configuration into VisualNode structure:
- ✅ Parse simple elements
- ✅ Parse nested elements
- ✅ Parse CDATA content
- ✅ Parse multiple attributes
- ✅ Handle empty elements

#### 2. SimpleXMLGenerator Tests
Tests for generating XML from VisualNode structure:
- ✅ Generate simple elements
- ✅ Generate nested elements
- ✅ Escape special characters in attributes
- ✅ Wrap body with CDATA for HTML content

#### 3. XML ↔ Visual Conversion Tests
Tests for round-trip conversion (most critical):
- ✅ Roundtrip simple config
- ✅ Roundtrip nested config
- ✅ Preserve all attributes
- ✅ Preserve body content
- ✅ Preserve CDATA sections

#### 4. VisualRenderer Tests
Tests for rendering VisualNodes on canvas:
- ✅ Create renderer instance
- ✅ Render empty nodes array
- ✅ Render single node
- 🔜 Render nested nodes (TODO)
- 🔜 Render with drag & drop (TODO)

#### 5. VisualDesigner.containerSummary (Jest, TypeScript)
Location: `frontend/src/visual/__tests__/VisualDesigner.containerSummary.test.ts`

- ✅ Verifies that container summary tiles render the metadata pills (Children / Containers / Leaves) and **do not** leak plugin-specific attributes onto the card.
- ✅ Asserts that clicking **Details** applies the same selection class used by the canvas (the test inspects `.visual-node__meta` and `node-actions` wiring).
- 🧪 Acts as the regression guard for the 320 × 160 px minimum footprint introduced in November 2025.

Run it with:

```bash
cd webharvest-ide/frontend
npm run test -- VisualDesigner.containerSummary
```

#### 6. VisualDesigner.drilldown (Jest, TypeScript)
Location: `frontend/src/visual/__tests__/VisualDesigner.drilldown.test.ts`

- ✅ Ensures scoped connections render when focusing a container and that start/end markers inside drill-down mode carry the same styling + dataset attributes as root markers (no more “contextual” blue border mismatch).
- ✅ Confirms the focus overlay chips reflect the child count, and warns if connection rendering would throw anchor errors.
- 🔄 Serves as the contract for card/selection parity when drilling into subprocesses—if the highlight ever desyncs, this suite fails first.

Run it with:

```bash
cd webharvest-ide/frontend
npm run test -- VisualDesigner.drilldown
```

### Running Tests

```bash
# Jest (modern suites)
cd webharvest-ide/frontend
npm run test -- VisualDesigner.containerSummary VisualDesigner.drilldown

# Legacy browser runner (still available for parser/generator specs)
cd webharvest-ide
java -jar target/webharvest-ide-2.3.1.jar
open http://localhost:8080/static/tests/test-runner.html
```

### Test Framework

Custom mini test framework (no external dependencies):
- **describe(name, func)** - Define test suite
- **it(name, func)** - Define test case
- **assert(condition, message)** - Basic assertion
- **assertEquals(actual, expected)** - Value equality
- **assertDeepEquals(actual, expected)** - Deep object equality
- **assertContains(haystack, needle)** - String contains

### TDD Workflow

1. **Write test first** (RED) ❌
2. **Implement feature** (GREEN) ✅
3. **Refactor** (REFACTOR) ♻️
4. **Repeat**

### Example Test

```javascript
runner.describe('SimpleXMLParser', (it) => {
    it('should parse simple element', () => {
        const parser = new SimpleXMLParser();
        const xml = '<config><var name="test">value</var></config>';
        const nodes = parser.parse(xml);
        
        assertEquals(nodes.length, 1, 'Should have 1 node');
        assertEquals(nodes[0].type, 'var', 'Node type should be var');
        assertEquals(nodes[0].attributes.name, 'test', 'Attribute name should be test');
    });
});
```

### Test Results Dashboard

The test runner provides:
- ✅ Total tests run
- ✅ Passed tests (green)
- ❌ Failed tests (red)
- ⏱️ Execution time per test
- 📋 Detailed error messages with stack traces

### Future Tests (TODO)

- [ ] Property panel tests
- [ ] Drag & drop tests
- [ ] Node connection tests
- [ ] Validation tests
- [ ] Save/Load visual config tests
- [ ] Plugin palette search tests
- [ ] Category filtering tests

### Integration with CI/CD

Tests can be automated using headless browser:

```bash
# Example with Puppeteer
npm install -g puppeteer
node run-tests-headless.js
```

### Test Coverage Goals

- **Parser/Generator:** 100% (critical for data integrity)
- **Renderer:** 80%+ (visual components)
- **Integration:** 90%+ (user workflows)

---

**Note:** All tests use vanilla JavaScript (no TypeScript) to match the current implementation approach for simpler integration with the existing IDE.









