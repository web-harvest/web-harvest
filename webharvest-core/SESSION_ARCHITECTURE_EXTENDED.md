# Session & Token Architecture - Extended Requirements

**Updated**: October 11, 2025  
**Status**: In Implementation

## 🎯 Extended Requirements (Critical Additions)

### 1. Pause/Resume Support
**Requirement**: Sessions must support pausing and resuming, not just stop/cancel.

**New Status Flow**:
```
PENDING → RUNNING ⟷ PAUSED → COMPLETED
                           ↘ FAILED
                           ↘ CANCELLED
```

**Use Cases**:
- Long-running scrapes that need temporary pause
- Rate limiting (pause when quota exceeded)
- Manual user intervention
- Resource management (pause low-priority jobs)

### 2. Prometheus Metrics Exporter
**Requirement**: Built-in Prometheus exporter for monitoring.

**Metrics to Export**:
```
# Session metrics
webharvest_sessions_total{status="running|completed|failed"}
webharvest_sessions_duration_seconds{status="completed"}
webharvest_session_elements_processed_total
webharvest_session_processing_rate

# Token metrics
webharvest_tokens_http_requests_total{client_id="..."}
webharvest_tokens_bytes_downloaded_total{client_id="..."}
webharvest_tokens_cpu_seconds_total{client_id="..."}

# System metrics
webharvest_thread_pool_active
webharvest_thread_pool_queue_size
```

### 3. Event Reporting System
**Requirement**: Publish events for all significant processing milestones.

**Event Types**:
```java
- SessionStartedEvent
- SessionPausedEvent
- SessionResumedEvent
- SessionCompletedEvent
- SessionFailedEvent
- PluginExecutedEvent
- HttpRequestEvent
- TokenThresholdEvent
- QuotaExceededEvent
```

**Integration**: Use existing Guava EventBus + new EventPublisher facade

### 4. SDK Facade Pattern
**Requirement**: Clean, versioned SDK for external plugin developers.

**Architecture**:
```
webharvest-sdk/  (new module)
├── WebHarvestSDK.java         (main facade)
├── SessionSDK.java            (session management)
├── MetricsSDK.java            (metrics access)
├── EventSDK.java              (event subscription)
└── README.md                  (SDK documentation)
```

**Usage Example**:
```java
// External plugin developer code
WebHarvestSDK sdk = WebHarvestSDK.create();

// Execute with callbacks
SessionHandle handle = sdk.sessions()
    .execute(config)
    .withClientId("my-plugin")
    .onProgress(metrics -> log.info("Progress: " + metrics))
    .onPause(() -> log.info("Paused"))
    .onResume(() -> log.info("Resumed"))
    .onComplete(result -> log.info("Done"))
    .start();

// Control execution
handle.pause();
handle.resume();
handle.cancel();

// Access metrics
SessionMetrics metrics = handle.getMetrics();
TokenUsage tokens = handle.getTokenUsage();
```

---

## 📐 Updated Architecture

### Extended Session Status
```java
public enum SessionStatus {
    PENDING,
    RUNNING,
    PAUSED,      // NEW!
    COMPLETED,
    FAILED,
    CANCELLED
}
```

### New Components

#### 1. MetricsCollector (Core)
```java
public interface MetricsCollector {
    void recordSessionStarted(String sessionId, String clientId);
    void recordSessionCompleted(String sessionId, Duration duration);
    void recordSessionFailed(String sessionId, Throwable error);
    void recordPluginExecuted(String pluginName, Duration duration);
    void recordHttpRequest(String sessionId, int statusCode, long bytes);
    void recordTokenUsage(String clientId, TokenUsage usage);
}
```

#### 2. PrometheusExporter (Core)
```java
@Singleton
public class PrometheusExporter implements MetricsCollector {
    private final Counter sessionsTotal;
    private final Summary sessionDuration;
    private final Counter httpRequestsTotal;
    // ... Prometheus metrics
    
    public String exportMetrics() {
        // Return Prometheus text format
    }
    
    public void startHttpServer(int port) {
        // Expose /metrics endpoint
    }
}
```

#### 3. EventPublisher (Core)
```java
public interface EventPublisher {
    void publish(SessionEvent event);
    void subscribe(Class<? extends SessionEvent> eventType, 
                   EventHandler handler);
    void unsubscribe(EventHandler handler);
}

// Event hierarchy
public abstract class SessionEvent {
    private final String sessionId;
    private final Instant timestamp;
}

public class SessionPausedEvent extends SessionEvent {
    private final SessionMetrics metricsAtPause;
}

public class SessionResumedEvent extends SessionEvent {
    private final Duration pauseDuration;
}
```

#### 4. PausableExecutor (Core)
```java
public interface PausableExecutor {
    void pause();
    void resume();
    boolean isPaused();
    void awaitResume() throws InterruptedException;
}

// Integration with ScraperSession
public interface ScraperSession extends PausableExecutor {
    // ... existing methods ...
    
    @Override
    void pause();
    
    @Override
    void resume();
    
    @Override
    boolean isPaused();
}
```

#### 5. WebHarvestSDK (Facade)
```java
public class WebHarvestSDK {
    private final SessionSDK sessions;
    private final MetricsSDK metrics;
    private final EventSDK events;
    
    public static WebHarvestSDK create() {
        return new WebHarvestSDK();
    }
    
    public SessionSDK sessions() {
        return sessions;
    }
    
    public MetricsSDK metrics() {
        return metrics;
    }
    
    public EventSDK events() {
        return events;
    }
}

public interface SessionSDK {
    ExecutionBuilder execute(Config config);
    Optional<SessionHandle> getSession(String sessionId);
    List<SessionHandle> getSessions(String clientId);
}

public interface SessionHandle {
    String getSessionId();
    SessionStatus getStatus();
    SessionMetrics getMetrics();
    TokenUsage getTokenUsage();
    
    void pause();
    void resume();
    void cancel();
    
    void awaitCompletion() throws InterruptedException;
}
```

---

## 🔄 Updated Implementation Plan

### Phase 1: Core Session Management (Current)
- [x] SessionStatus, SessionMetrics
- [x] ScraperSession interface
- [IN PROGRESS] ScraperSessionImpl
- [ ] **Add PAUSED status support**

### Phase 2: Pause/Resume Implementation
- [ ] PausableExecutor interface
- [ ] Integrate with Scraper.execute()
- [ ] Thread interruption handling
- [ ] State persistence during pause
- [ ] Resume from saved state

### Phase 3: Metrics & Prometheus
- [ ] MetricsCollector interface
- [ ] PrometheusExporter implementation
- [ ] Metrics HTTP endpoint (/metrics)
- [ ] Integration with SessionMetrics
- [ ] TokenTracker → Prometheus integration

### Phase 4: Event System
- [ ] SessionEvent hierarchy
- [ ] EventPublisher implementation
- [ ] Integration with existing EventBus
- [ ] Event handlers for all lifecycle transitions

### Phase 5: SDK Facade
- [ ] Create webharvest-sdk module
- [ ] WebHarvestSDK main facade
- [ ] SessionSDK, MetricsSDK, EventSDK
- [ ] Fluent builder APIs
- [ ] SDK documentation & examples

### Phase 6: Integration & Testing
- [ ] Full integration tests
- [ ] Pause/Resume tests
- [ ] Prometheus metrics tests
- [ ] Event publishing tests
- [ ] SDK usage examples

---

## 📊 Prometheus Metrics Specification

### Session Metrics
```
# HELP webharvest_sessions_total Total number of sessions by status
# TYPE webharvest_sessions_total counter
webharvest_sessions_total{status="pending"} 5
webharvest_sessions_total{status="running"} 12
webharvest_sessions_total{status="paused"} 3
webharvest_sessions_total{status="completed"} 1543
webharvest_sessions_total{status="failed"} 27
webharvest_sessions_total{status="cancelled"} 8

# HELP webharvest_session_duration_seconds Session execution duration
# TYPE webharvest_session_duration_seconds summary
webharvest_session_duration_seconds_sum 45621.3
webharvest_session_duration_seconds_count 1543

# HELP webharvest_session_elements_processed Total elements processed
# TYPE webharvest_session_elements_processed counter
webharvest_session_elements_processed_total{client_id="client-123"} 125000
```

### Token Metrics
```
# HELP webharvest_tokens_http_requests HTTP requests per client
# TYPE webharvest_tokens_http_requests counter
webharvest_tokens_http_requests_total{client_id="client-123"} 5420

# HELP webharvest_tokens_bytes_downloaded Bytes downloaded per client
# TYPE webharvest_tokens_bytes_downloaded counter
webharvest_tokens_bytes_downloaded_total{client_id="client-123"} 524288000

# HELP webharvest_tokens_cpu_seconds CPU time per client
# TYPE webharvest_tokens_cpu_seconds counter
webharvest_tokens_cpu_seconds_total{client_id="client-123"} 142.5
```

### System Metrics
```
# HELP webharvest_thread_pool_active Active threads in pool
# TYPE webharvest_thread_pool_active gauge
webharvest_thread_pool_active 8

# HELP webharvest_thread_pool_queue_size Queued tasks
# TYPE webharvest_thread_pool_queue_size gauge
webharvest_thread_pool_queue_size 3
```

---

## 🎯 SDK Usage Examples

### Example 1: Basic Execution with Pause/Resume
```java
WebHarvestSDK sdk = WebHarvestSDK.create();

SessionHandle session = sdk.sessions()
    .execute(config)
    .withClientId("my-app")
    .withOptions(SessionOptions.builder()
        .timeout(Duration.ofMinutes(30))
        .build())
    .onProgress(metrics -> {
        if (metrics.getProcessedElements() > 1000) {
            session.pause(); // Pause after 1000 elements
        }
    })
    .start();

// Later...
session.resume(); // Continue from where it paused
```

### Example 2: Event Subscription
```java
sdk.events().subscribe(SessionPausedEvent.class, event -> {
    System.out.println("Session paused: " + event.getSessionId());
    System.out.println("Elements processed: " + 
        event.getMetricsAtPause().getProcessedElements());
});

sdk.events().subscribe(TokenThresholdEvent.class, event -> {
    if (event.getClientId().equals("client-123")) {
        System.err.println("Client approaching quota limit!");
    }
});
```

### Example 3: Metrics Monitoring
```java
MetricsSDK metrics = sdk.metrics();

// Get current metrics
SystemMetrics system = metrics.getSystemMetrics();
System.out.println("Active sessions: " + system.getActiveSessions());
System.out.println("Thread pool: " + system.getThreadPoolActive());

// Export for Prometheus
String prometheusMetrics = metrics.exportPrometheus();
```

---

## 🔧 Technical Implementation Details

### Pause/Resume Mechanism
```java
public class ScraperSessionImpl implements ScraperSession {
    private final ReentrantLock pauseLock = new ReentrantLock();
    private final Condition resumeCondition = pauseLock.newCondition();
    private volatile boolean paused = false;
    
    @Override
    public void pause() {
        if (status != SessionStatus.RUNNING) {
            return;
        }
        pauseLock.lock();
        try {
            paused = true;
            status = SessionStatus.PAUSED;
            eventPublisher.publish(new SessionPausedEvent(sessionId, metrics));
        } finally {
            pauseLock.unlock();
        }
    }
    
    @Override
    public void resume() {
        pauseLock.lock();
        try {
            if (paused) {
                paused = false;
                status = SessionStatus.RUNNING;
                resumeCondition.signalAll();
                eventPublisher.publish(new SessionResumedEvent(sessionId));
            }
        } finally {
            pauseLock.unlock();
        }
    }
    
    public void checkPausePoint() throws InterruptedException {
        pauseLock.lock();
        try {
            while (paused) {
                resumeCondition.await();
            }
        } finally {
            pauseLock.unlock();
        }
    }
}
```

### Integration with Plugin Execution
```java
// AbstractPlugin integration
protected Variable execute(DynamicScopeContext context) {
    // Check pause before execution
    if (context.getSession() != null) {
        context.getSession().checkPausePoint();
    }
    
    Variable result = doExecute(context);
    
    // Record metric
    context.getSession().getMetrics().incrementProcessedElements();
    
    return result;
}
```

---

## 📦 New Module Structure

```
webharvest-sdk/
├── pom.xml
├── README.md
├── src/
│   ├── main/java/org/webharvest/sdk/
│   │   ├── WebHarvestSDK.java
│   │   ├── SessionSDK.java
│   │   ├── MetricsSDK.java
│   │   ├── EventSDK.java
│   │   ├── SessionHandle.java
│   │   ├── ExecutionBuilder.java
│   │   └── examples/
│   │       ├── BasicUsageExample.java
│   │       ├── PauseResumeExample.java
│   │       └── MetricsExample.java
│   └── test/java/org/webharvest/sdk/
│       ├── WebHarvestSDKTest.java
│       └── IntegrationTest.java
└── docs/
    ├── SDK_GUIDE.md
    ├── API_REFERENCE.md
    └── MIGRATION_GUIDE.md
```

---

## ✅ Success Criteria (Updated)

1. ✅ Basic session management (PENDING → RUNNING → COMPLETED)
2. ⏳ **Pause/Resume support** (NEW)
3. ⏳ **Prometheus metrics exporter** (NEW)
4. ⏳ **Event publishing system** (NEW)
5. ⏳ **SDK facade for external plugins** (NEW)
6. ✅ Thread-safe implementation
7. ✅ >55% test coverage
8. ✅ Comprehensive JavaDoc
9. ⏳ SDK documentation & examples (NEW)

---

**This extended architecture provides enterprise-grade features while maintaining clean separation of concerns and ease of use for external plugin developers.**

