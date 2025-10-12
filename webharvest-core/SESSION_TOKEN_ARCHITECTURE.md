# Session & Token Architecture - Implementation Plan

## Overview
This document describes the architecture for Session Management, Token Tracking, and Multi-threaded execution support in WebHarvest Core.

## Design Principles

1. **Core Framework** - All components are part of `webharvest-core`, not external plugins
2. **Test-Driven Development** - Tests first, implementation second
3. **Coverage Target** - Minimum 55% code coverage
4. **Documentation** - Comprehensive JavaDoc and markdown documentation
5. **Backward Compatibility** - Minimal breaking changes to existing API

## Architecture Diagram

```
┌─────────────────────────────────────────────────────────────┐
│                    WebHarvestService                         │
│  - executeAsync(Config, ClientContext, SessionOptions)       │
│  - getSession(sessionId)                                     │
│  - listSessions(clientId)                                    │
└────────────────┬────────────────────────────────────────────┘
                 │
                 ├──> SessionRegistry (manages all sessions)
                 │
                 ├──> QuotaService (validates client limits)
                 │
                 └──> ExecutorService (thread pool for sessions)
                          │
                          ▼
         ┌────────────────────────────────────┐
         │        ScraperSession              │
         │  - sessionId: String (UUID)        │
         │  - clientId: String                │
         │  - status: SessionStatus           │
         │  - metrics: SessionMetrics         │
         │  - tokenTracker: TokenTracker      │
         │  - context: DynamicScopeContext    │
         └────────────────────────────────────┘
                          │
                          ├──> TokenTracker (usage tracking)
                          │
                          └──> ScraperContext (execution context)
```

## Package Structure

```
org.webharvest.runtime.session/
  ├── ScraperSession.java              (interface)
  ├── ScraperSessionImpl.java          (implementation)
  ├── SessionStatus.java               (enum)
  ├── SessionRegistry.java             (interface)
  ├── SessionRegistryImpl.java         (implementation)
  ├── SessionOptions.java              (builder pattern)
  └── SessionMetrics.java              (metrics data)

org.webharvest.runtime.client/
  ├── ClientContext.java               (client info)
  ├── ClientContextBuilder.java        (builder)
  └── QuotaPolicy.java                 (quota limits)

org.webharvest.runtime.tracking/
  ├── TokenTracker.java                (interface)
  ├── TokenTrackerImpl.java            (implementation)
  ├── TokenUsage.java                  (usage data)
  ├── ResourceType.java                (enum: HTTP, CPU, MEMORY, etc.)
  └── TokenUsageEvent.java             (for tracking)

org.webharvest.runtime.service/
  ├── WebHarvestService.java           (main API)
  ├── WebHarvestServiceImpl.java       (implementation)
  └── QuotaService.java                (quota validation)
```

## Module Responsibility Matrix

| Component | Core | CLI | IDE |
|-----------|------|-----|-----|
| **Session Management** | ✅ Interfaces & Implementation | ✅ Progress reporting | ✅ GUI monitoring |
| **Token Tracking** | ✅ Full implementation | ✅ Text display | ✅ Visual charts |
| **Client Context** | ✅ Full implementation | ✅ Config loading | ✅ GUI config |
| **Session Registry** | ✅ In-memory registry | - | - |
| **Multi-session Executor** | ✅ Thread pool | ✅ Batch execution | ✅ Parallel UI |
| **Quota Service** | ✅ Validation logic | ✅ CLI warnings | ✅ GUI alerts |
| **Parallel Plugin** | ✅ Plugin implementation | - | - |

### Core Module
- All session, token, and client abstractions
- Thread pool management for multi-session execution
- Event-based progress tracking (using existing EventBus)
- Storage-agnostic session registry

### CLI Module
- Command-line session status reporting
- Progress bar integration
- Token usage display
- Batch execution commands

### IDE Module
- Visual session monitoring dashboard
- Real-time progress tracking UI
- Token usage graphs and charts
- Session history viewer

## Implementation Phases

### Phase 1: Session Management Foundation (TDD)
**Target: 2-3 hours**

#### 1.1 Core Interfaces
- [ ] `SessionStatus` enum (PENDING, RUNNING, COMPLETED, FAILED, CANCELLED)
- [ ] `SessionMetrics` (start time, duration, processed elements)
- [ ] `ScraperSession` interface
- [ ] `SessionRegistry` interface

#### 1.2 Tests First (TDD)
```java
// ScraperSessionTest.java
- testSessionIdGeneration()
- testSessionStatusTransitions()
- testSessionMetricsTracking()
- testSessionStartTime()
- testSessionDuration()

// SessionRegistryTest.java
- testRegisterSession()
- testGetSessionById()
- testListSessionsByClient()
- testUnregisterSession()
- testConcurrentAccess()
```

#### 1.3 Implementation
- [ ] `ScraperSessionImpl` - thread-safe implementation
- [ ] `SessionRegistryImpl` - ConcurrentHashMap-based registry
- [ ] Integration with existing `Scraper` class

### Phase 2: Client Context & Quota Management (TDD)
**Target: 1-2 hours**

#### 2.1 Core Interfaces
- [ ] `ClientContext` - client identification and metadata
- [ ] `QuotaPolicy` - resource limits
- [ ] `QuotaService` - validation logic

#### 2.2 Tests First
```java
// ClientContextTest.java
- testClientIdValidation()
- testClientMetadata()
- testQuotaPolicyAssociation()

// QuotaServiceTest.java
- testQuotaValidation()
- testQuotaExceeded()
- testNoQuotaLimit()
- testQuotaReset()
```

#### 2.3 Implementation
- [ ] `ClientContextBuilder` - fluent builder
- [ ] `QuotaServiceImpl` - validation implementation
- [ ] Integration with `WebHarvestService`

### Phase 3: Token Tracking System (TDD)
**Target: 2-3 hours**

#### 3.1 Core Interfaces
- [ ] `ResourceType` enum (HTTP_REQUEST, HTTP_BYTES, CPU_TIME, MEMORY_PEAK)
- [ ] `TokenUsage` - aggregated usage data
- [ ] `TokenTracker` interface
- [ ] `TokenUsageEvent` - tracking events

#### 3.2 Tests First
```java
// TokenTrackerTest.java
- testRecordHttpRequest()
- testRecordBytesDownloaded()
- testRecordCpuTime()
- testRecordMemoryUsage()
- testAggregateUsage()
- testConcurrentTracking()

// TokenUsageTest.java
- testUsageCalculation()
- testUsageAggregation()
- testUsageSnapshot()
```

#### 3.3 Implementation
- [ ] `TokenTrackerImpl` - thread-safe tracking
- [ ] Integration with `ScraperSession`
- [ ] Integration with HTTP plugins (intercept requests)
- [ ] Integration with plugin execution (measure CPU time)

### Phase 4: WebHarvestService API (TDD)
**Target: 2 hours**

#### 4.1 Core API
- [ ] `SessionOptions` - execution configuration
- [ ] `WebHarvestService` interface
- [ ] `WebHarvestServiceImpl` implementation

#### 4.2 Tests First
```java
// WebHarvestServiceTest.java
- testExecuteAsync()
- testGetSession()
- testListSessions()
- testCancelSession()
- testMultipleSessionsParallel()
- testQuotaEnforcement()
- testSessionIsolation()
```

#### 4.3 Implementation
- [ ] Thread pool configuration
- [ ] Session lifecycle management
- [ ] Exception handling and propagation
- [ ] Integration with existing `ScrapingHarvester`

### Phase 5: Multi-Session Parallelism (TDD)
**Target: 1-2 hours**

#### 5.1 Thread Pool Configuration
- [ ] Configurable thread pool size
- [ ] Thread naming for debugging
- [ ] Graceful shutdown handling

#### 5.2 Tests First
```java
// MultiSessionExecutionTest.java
- testParallelSessionExecution()
- testSessionIsolation()
- testThreadPoolLimits()
- testConcurrentContextAccess()
- testExceptionInOneSession()
```

#### 5.3 Implementation
- [ ] `ExecutorService` integration
- [ ] Session submission and tracking
- [ ] Concurrent session monitoring

### Phase 6: Parallel Execute Plugin (TDD)
**Target: 3-4 hours**

#### 6.1 Plugin Design
```xml
<parallel-execute max-threads="5" timeout="30s">
    <parallel-task id="task1">
        <http url="http://example.com/page1"/>
    </parallel-task>
    <parallel-task id="task2">
        <http url="http://example.com/page2"/>
    </parallel-task>
</parallel-execute>
```

#### 6.2 Tests First
```java
// ParallelExecutePluginTest.java
- testParallelTaskExecution()
- testResultAggregation()
- testTaskIsolation()
- testExceptionHandling()
- testTimeout()
- testMaxThreadsLimit()
```

#### 6.3 Implementation
- [ ] `ParallelExecutePlugin` - fork-join logic
- [ ] `ParallelTaskPlugin` - task wrapper
- [ ] Isolated context per task
- [ ] Result collection and merging

### Phase 7: Documentation (Parallel with Implementation)

#### 7.1 JavaDoc
- [ ] All public interfaces (100% coverage)
- [ ] All public methods with @param, @return, @throws
- [ ] Code examples in class-level JavaDoc
- [ ] Package-level documentation (package-info.java)

#### 7.2 Markdown Documentation
- [ ] `SESSION_MANAGEMENT_GUIDE.md` - User guide
- [ ] `TOKEN_TRACKING_GUIDE.md` - Token system explained
- [ ] `MULTI_THREADING_GUIDE.md` - Threading model
- [ ] `PARALLEL_PLUGIN_GUIDE.md` - Plugin usage examples
- [ ] Update `DEVELOPER_GUIDE.md`

## API Examples

### Example 1: Basic Session Execution

```java
// Create client context
ClientContext client = ClientContext.builder()
    .clientId("my-app")
    .metadata("version", "1.0")
    .quotaPolicy(QuotaPolicy.unlimited())
    .build();

// Create session options
SessionOptions options = SessionOptions.builder()
    .timeout(Duration.ofMinutes(30))
    .trackTokens(true)
    .build();

// Execute configuration asynchronously
Config config = configFactory.create(new File("scraper.xml"));
WebHarvestService service = new WebHarvestServiceImpl();
ScraperSession session = service.executeAsync(config, client, options);

// Monitor progress
while (session.getStatus() != SessionStatus.COMPLETED) {
    System.out.println("Progress: " + session.getMetrics().getProgress());
    Thread.sleep(1000);
}

// Get results
DynamicScopeContext context = session.getContext();
Variable result = context.getVar("result");
```

### Example 2: Multiple Parallel Sessions

```java
ExecutorService executor = Executors.newFixedThreadPool(5);
List<Future<ScraperSession>> futures = new ArrayList<>();

for (String configFile : configFiles) {
    Future<ScraperSession> future = executor.submit(() -> {
        Config config = configFactory.create(new File(configFile));
        return service.executeAsync(config, client, options);
    });
    futures.add(future);
}

// Wait for all sessions to complete
for (Future<ScraperSession> future : futures) {
    ScraperSession session = future.get();
    System.out.println("Session " + session.getSessionId() + 
        " completed with " + session.getTokenTracker().getUsage().getHttpRequests() + " requests");
}
```

### Example 3: Parallel Plugin Usage

```xml
<config>
    <!-- Scrape multiple pages in parallel -->
    <parallel-execute max-threads="10">
        <parallel-task id="page1">
            <http url="http://example.com/1"/>
        </parallel-task>
        <parallel-task id="page2">
            <http url="http://example.com/2"/>
        </parallel-task>
        <!-- Results available as ${page1}, ${page2} -->
    </parallel-execute>
    
    <!-- Process results sequentially -->
    <xpath expression="//title">
        <var name="page1"/>
    </xpath>
</config>
```

## Testing Strategy

### Unit Tests (70% of tests)
- All interfaces and implementations
- Edge cases and error handling
- Thread safety and concurrency
- Quota validation logic

### Integration Tests (25% of tests)
- Session lifecycle with real Scraper
- Token tracking during HTTP requests
- Multi-session parallel execution
- Parallel plugin with nested contexts

### Performance Tests (5% of tests)
- Large number of concurrent sessions
- Memory usage under load
- Thread pool efficiency
- Token tracking overhead

## Coverage Target

- **Minimum**: 55% overall coverage
- **Target**: 70% for core session management
- **Critical paths**: 90% (session lifecycle, token tracking)

## Migration Notes

### Backward Compatibility
- Existing `Scraper` API remains unchanged
- New API is opt-in (use `WebHarvestService` for sessions)
- Default behavior: no session tracking (zero overhead)
- CLI and IDE modules updated to use new API

### Deprecation Strategy
- No deprecations in Phase 1-3
- Consider deprecating direct `Scraper` usage in future (v2.3.0)

## Success Criteria

1. ✅ All tests pass with >55% coverage
2. ✅ Session management fully functional
3. ✅ Token tracking integrated with HTTP plugins
4. ✅ Multi-session parallelism working
5. ✅ Parallel plugin implemented and tested
6. ✅ Comprehensive documentation (JavaDoc + MD)
7. ✅ Zero performance regression for existing code
8. ✅ CLI updated with session commands
9. ✅ IDE updated with session monitoring

## Timeline Estimate

- **Phase 1**: 3 hours (Session Management)
- **Phase 2**: 2 hours (Client Context)
- **Phase 3**: 3 hours (Token Tracking)
- **Phase 4**: 2 hours (WebHarvestService)
- **Phase 5**: 2 hours (Multi-session)
- **Phase 6**: 4 hours (Parallel Plugin)
- **Phase 7**: 2 hours (Documentation)

**Total**: ~18 hours of focused development

## Next Steps

1. Create package structure
2. Write first test for `ScraperSession`
3. Implement `ScraperSessionImpl`
4. Continue TDD cycle through all phases
5. Integrate with existing codebase
6. Update CLI and IDE modules
7. Write comprehensive documentation

---
**Document Version**: 1.0  
**Last Updated**: October 11, 2025  
**Author**: AI Assistant (with Robert Bala)  
**Status**: Ready for Implementation


