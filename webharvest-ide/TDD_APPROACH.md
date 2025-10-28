# TDD Approach for Visual Designer Integration

**User Feedback:** "pamiętaj o tdd!!" ✅

**STOP doing:** Building features without tests  
**START doing:** Test-first development

---

## 🎯 Current Status

### What I Have:
- ✅ XMLParser.ts (written)
- ✅ XMLGenerator.ts (written)
- ✅ XMLConversionTests.ts (written, 8 test cases)
- ✅ test-runner.html (written)
- ❌ **Tests NOT running or verified!**

### What I Was Doing Wrong:
- Building UI first
- Adding features without testing
- Assuming code works without verification

---

## ✅ Correct TDD Process

### Phase 1: Test XML Converters (NOW!)
1. **Run existing tests** - verify they work
2. **Fix any failing tests**
3. **Add more test cases** (edge cases, errors)
4. **Only then** integrate with IDE

### Phase 2: Test Integration
1. **Write integration tests** - switchViewMode()
2. **Test tab state** - viewMode persistence
3. **Test roundtrip** - XML → Visual → XML
4. **Only then** add UI features

### Phase 3: Test Visual Rendering
1. **Write tests** for node rendering
2. **Test** property panel
3. **Test** drag & drop
4. **Only then** polish UI

---

## 📋 Immediate Action Plan

### Step 1: Run XML Conversion Tests (5 min)
```bash
# Access test runner
http://localhost:8080/visual-designer/tests/test-runner.html

# OR compile and check for errors
mvn test -pl webharvest-ide
```

### Step 2: Fix Failing Tests (30 min)
- Debug XMLParser
- Debug XMLGenerator
- Ensure 8/8 tests pass

### Step 3: Add More Tests (30 min)
- Error handling (invalid XML)
- Edge cases (empty config, namespaces)
- Complex nesting (loop > if > http)
- CDATA content

### Step 4: Integration Tests (1 hour)
```javascript
// Test: switchViewMode preserves content
test('XML to Visual preserves attributes', () => {
  const xml = '<http url="https://example.com" method="POST"/>';
  const nodes = xmlParser.parse(xml);
  expect(nodes[0].config.url).toBe('https://example.com');
  expect(nodes[0].config.method).toBe('POST');
});

// Test: Roundtrip conversion
test('Roundtrip XML→Visual→XML is lossless', () => {
  const original = getExampleXML();
  const nodes = xmlParser.parse(original);
  const generated = xmlGenerator.generate(nodes);
  expect(generated).toEqualXML(original);
});
```

---

## 🚨 What NOT To Do

- ❌ Don't add more UI without tests
- ❌ Don't assume code works
- ❌ Don't skip edge cases
- ❌ Don't integrate untested code

---

## ✅ What TO Do

- ✅ Test first, code second
- ✅ Run tests after every change
- ✅ Fix failing tests immediately
- ✅ Add tests for bugs found
- ✅ Test edge cases and errors

---

**Starting with test verification NOW!** 🧪


