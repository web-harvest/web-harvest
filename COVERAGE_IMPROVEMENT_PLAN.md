# Coverage Improvement Plan
**Date**: October 15, 2025  
**Goal**: Increase webharvest-core coverage from 56% to >70%

---

## Current Status (r793)

### webharvest-core
- **Current**: 56% instruction coverage (20,216 / 35,792)
- **Target**: >70% (25,054 instructions)
- **Gap**: ~4,838 instructions needed

### Progress
- ✅ **plugin.factory**: 0% → ~80% (+387 instructions) - **DONE** (r793)
  - Added 35 comprehensive tests for DefaultPluginFactory
  - All edge cases covered

---

## Priority Targets (Highest Impact)

### 1. org.webharvest.plugin.core (Largest Package)
- **Current**: 51% coverage
- **Missed**: 9,597 instructions
- **Potential**: If increased to 60% → +1,800 instructions
- **Strategy**: Focus on most-used plugins lacking tests

### 2. org.webharvest (Root Package)
- **Current**: 9% coverage
- **Missed**: 1,680 instructions
- **Potential**: If increased to 50% → +690 instructions
- **Strategy**: Test core utility classes (Registry, Cache, etc.)

### 3. org.webharvest.definition
- **Current**: 50% coverage
- **Missed**: 878 instructions
- **Potential**: If increased to 70% → +350 instructions
- **Strategy**: Configuration parsing and validation

### 4. org.webharvest.events
- **Current**: 15% coverage
- **Missed**: 180 instructions
- **Potential**: If increased to 60% → +80 instructions
- **Strategy**: Event system tests

---

## Detailed Package Analysis

| Package | Current % | Missed | Priority | Estimated Gain |
|---------|-----------|--------|----------|----------------|
| **plugin.core** | 51% | 9,597 | 🔴 HIGH | 1,800 |
| **org.webharvest** | 9% | 1,680 | 🔴 HIGH | 690 |
| **definition** | 50% | 878 | 🟡 MEDIUM | 350 |
| **utils** | 61% | 1,020 | 🟡 MEDIUM | 200 |
| **events** | 15% | 180 | 🟡 MEDIUM | 80 |
| **plugin.factory** | ~80% | ~100 | ✅ DONE | 387 |

**Total Expected Gain**: ~3,507 instructions (reaches 67%)

---

## Implementation Strategy

### Phase 1: Core Plugins (plugin.core)
**Target**: 51% → 60% (+1,800 instructions)

Focus on high-value, frequently-used plugins with low coverage:
1. **Loop/While plugins** - Iteration logic
2. **Template plugin** - Dynamic content
3. **Script plugin** - Scripting engine integration
4. **Include plugin** - Configuration composition

**Approach**:
- Integration tests using XML configurations
- Test common use cases and edge cases
- Verify error handling

### Phase 2: Root Package (org.webharvest)
**Target**: 9% → 50% (+690 instructions)

Test core infrastructure classes:
1. **Registry/AbstractRegistry** - Plugin/variable registration
2. **Cache/ThreadLocalCache** - Caching mechanisms
3. **Transformer/Harvest** - Main entry points
4. **Harvester** - Configuration execution

**Approach**:
- Unit tests for each utility class
- Mock dependencies where appropriate
- Focus on public API

### Phase 3: Definition Package
**Target**: 50% → 70% (+350 instructions)

Test configuration handling:
1. **Config parsing** - XML to object model
2. **Validation** - Schema validation
3. **Element definitions** - Plugin configurations

**Approach**:
- Test valid and invalid configurations
- Verify error messages
- Check edge cases

### Phase 4: Events Package
**Target**: 15% → 60% (+80 instructions)

Test event system:
1. **Event bus** - Publication/subscription
2. **Event types** - All event classes
3. **Listeners** - Event handling

**Approach**:
- Test event flow
- Verify listener registration
- Check thread safety

---

## External Modules (Secondary Goals)

### webharvest-cli
- **Current**: 41%
- **Target**: 60%
- **Strategy**: Command-line parsing, execution flow

### webharvest-ide
- **Current**: Failing tests (WebSocket issues)
- **Target**: Fix failures first, then increase coverage
- **Strategy**: Mock WebSocket, test IDE components

### External Plugins
- **database**: 41% → 60%
- **ftp**: 9% → 40%
- **mail**: 68% → 75%
- **webbrowser**: 51% → 60%
- **Strategy**: Integration tests for each plugin

---

## Testing Guidelines

### Test Quality Standards
1. **Meaningful tests** - Test behavior, not implementation
2. **Edge cases** - Null, empty, invalid inputs
3. **Error handling** - Verify exceptions and messages
4. **Integration tests** - Test plugin interactions
5. **No flaky tests** - Stable, repeatable results

### Test Naming
- `[MethodName][Scenario]Test` for unit tests
- `[Feature][Integration]Test` for integration tests
- Clear, descriptive names

### Coverage Metrics
- **Instruction coverage**: Primary metric
- **Branch coverage**: Secondary metric
- **Line coverage**: For tracking progress

---

## Estimated Timeline

- **Phase 1** (plugin.core): 2-3 hours → +1,800 instructions (64%)
- **Phase 2** (org.webharvest): 1-2 hours → +690 instructions (66%)
- **Phase 3** (definition): 1 hour → +350 instructions (67%)
- **Phase 4** (events): 30 minutes → +80 instructions (68%)

**Total to 70%**: Additional ~1,000 instructions from various packages

**Estimated Total Time**: 6-8 hours

---

## Success Criteria

### Minimum Requirements
- ✅ webharvest-core >70% instruction coverage
- ✅ All new tests passing
- ✅ No regression in existing tests
- ✅ Code quality maintained

### Stretch Goals
- 🎯 webharvest-core >75% instruction coverage
- 🎯 webharvest-cli >60%
- 🎯 All external plugins >50%

---

## Completed Work

### r793: plugin.factory Package
- **Tests Added**: 35
- **Coverage**: 0% → ~80%
- **Instructions Covered**: ~387
- **Status**: ✅ COMPLETE

---

## Next Steps

1. **Immediate**: Run full test suite with coverage to verify current state
2. **Phase 1**: Begin plugin.core tests (highest impact)
3. **Monitor**: Track coverage after each test batch
4. **Adjust**: Refine strategy based on results

---

**Status**: IN PROGRESS  
**Last Updated**: October 15, 2025 (r793)

