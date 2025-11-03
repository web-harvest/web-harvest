# WebHarvest Session Management - User Guide

**Version**: 2.2.0  
**Last Updated**: October 11, 2025

---

## 📖 Table of Contents

1. [Introduction](#introduction)
2. [Quick Start](#quick-start)
3. [Core Concepts](#core-concepts)
4. [Basic Usage](#basic-usage)
5. [Multi-Session Execution](#multi-session-execution)
6. [Token Tracking](#token-tracking)
7. [Event System](#event-system)
8. [Best Practices](#best-practices)
9. [API Reference](#api-reference)

---

## Introduction

WebHarvest 2.2.0 introduces comprehensive session management that enables:

- **Unique Session IDs** - Track and identify each scraper execution
- **Status Monitoring** - Real-time progress and lifecycle tracking
- **Multi-tenant Support** - Isolate sessions by client
- **Token Tracking** - Monitor resource consumption
- **Concurrent Execution** - Run multiple scrapers in parallel
- **Event System** - Subscribe to lifecycle events

All functionality is part of **webharvest-core** and ready to use out-of-the-box.

---

## Quick Start

### Basic Session Execution

```java
import org.webharvest.runtime.service.WebHarvestService;
import org.webharvest.runtime.service.WebHarvestServiceImpl;
import org.webharvest.runtime.session.ScraperSession;
import org.webharvest.definition.Config;

// Create service
WebHarvestService service = new WebHarvestServiceImpl();

// Load configuration
Config config = ConfigFactory.create(new File("scraper.xml"));

// Execute asynchronously
ScraperSession session = service.executeAsync(config, null, null);

// Wait for completion
session.awaitCompletion();

// Check result
if (session.getStatus().isSuccessful()) {
    System.out.println("Success!");
    System.out.println("Duration: " + session.getMetrics().getDuration());
} else {
    System.err.println("Failed: " + session.getError());
}

// Cleanup
service.shutdown();
```

---

## Core Concepts

### Session

A **session** represents a single execution of a WebHarvest configuration. Each session has:

- **Unique ID** (UUID) - for tracking and correlation
- **Status** - current lifecycle state
- **Metrics** - timing and performance data
- **Context** - runtime variables and state
- **Client ID** - optional tenant identifier

### Session Lifecycle

```
PENDING → RUNNING → COMPLETED (success)
                  → FAILED (error)
                  → CANCELLED (user/system)
```

### Session Registry

Central storage for all active and completed sessions. Enables:
- Lookup by session ID
- Filter by client ID
- List all sessions

---

## Basic Usage

### 1. Create WebHarvestService

```java
// Default configuration (cached thread pool)
WebHarvestService service = new WebHarvestServiceImpl();

// Custom thread pool
ExecutorService threadPool = Executors.newFixedThreadPool(5);
WebHarvestService service = new WebHarvestServiceImpl(threadPool);
```

### 2. Configure Client Context (Optional)

```java
import org.webharvest.runtime.client.ClientContext;

ClientContext client = ClientContext.builder()
    .clientId("my-application")
    .metadata("version", "1.0")
    .metadata("tier", "premium")
    .build();
```

### 3. Configure Session Options (Optional)

```java
import org.webharvest.runtime.service.SessionOptions;
import java.time.Duration;

SessionOptions options = SessionOptions.builder()
    .timeout(Duration.ofMinutes(30))
    .trackTokens(true)
    .maxElements(10000)
    .build();
```

### 4. Execute Configuration

```java
ScraperSession session = service.executeAsync(config, client, options);

String sessionId = session.getSessionId();
System.out.println("Session started: " + sessionId);
```

### 5. Monitor Progress

```java
while (!session.getStatus().isTerminal()) {
    SessionMetrics metrics = session.getMetrics();
    
    System.out.printf("Status: %s, Elements: %d, Duration: %s%n",
        session.getStatus(),
        metrics.getProcessedElements(),
        metrics.getDuration());
    
    Thread.sleep(1000);
}
```

### 6. Retrieve Results

```java
if (session.getStatus() == SessionStatus.COMPLETED) {
    DynamicScopeContext context = session.getContext();
    Variable result = context.getVar("result");
    System.out.println("Result: " + result);
}
```

---

## Multi-Session Execution

### Running Multiple Sessions Concurrently

```java
WebHarvestService service = new WebHarvestServiceImpl();

List<ScraperSession> sessions = new ArrayList<>();

// Submit multiple configurations
for (File configFile : configFiles) {
    Config config = ConfigFactory.create(configFile);
    ScraperSession session = service.executeAsync(config, client, options);
    sessions.add(session);
}

// Monitor all sessions
boolean allDone = false;
while (!allDone) {
    long active = sessions.stream()
        .filter(s -> !s.getStatus().isTerminal())
        .count();
    
    System.out.println("Active sessions: " + active);
    
    if (active == 0) {
        allDone = true;
    } else {
        Thread.sleep(1000);
    }
}

// Collect results
for (ScraperSession session : sessions) {
    if (session.getStatus().isSuccessful()) {
        System.out.println("Session " + session.getSessionId() + " succeeded");
    } else {
        System.err.println("Session " + session.getSessionId() + " failed");
    }
}
```

### Session Management by Client

```java
// Execute sessions for different clients
service.executeAsync(config1, ClientContext.of("client-A"), options);
service.executeAsync(config2, ClientContext.of("client-A"), options);
service.executeAsync(config3, ClientContext.of("client-B"), options);

// Get all sessions for specific client
List<ScraperSession> clientASessions = service.listSessions("client-A");
System.out.println("Client A has " + clientASessions.size() + " sessions");

// Monitor client resource usage
for (ScraperSession session : clientASessions) {
    SessionMetrics metrics = session.getMetrics();
    System.out.println("  Session " + session.getSessionId() + ": " + 
        metrics.getProcessedElements() + " elements");
}
```

---

## Token Tracking

### Enabling Token Tracking

```java
SessionOptions options = SessionOptions.builder()
    .trackTokens(true)  // Enable tracking
    .build();

ScraperSession session = service.executeAsync(config, client, options);
```

### Reading Token Usage

**Note:** In v2.2, token tracking infrastructure is in place but not yet fully integrated
with HTTP and plugin execution. Full integration coming in v2.3.

```java
// This is the API (implementation in v2.3)
TokenTracker tracker = session.getTokenTracker();  // To be added to ScraperSession
TokenUsage usage = tracker.getUsage();

System.out.println("HTTP Requests: " + usage.getHttpRequests());
System.out.println("Bytes Downloaded: " + usage.getHttpBytes());
System.out.println("CPU Time: " + usage.getCpuTimeMillis() + "ms");
```

---

## Event System

### Subscribing to Session Events

```java
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import org.webharvest.runtime.session.events.*;

// Get EventBus from session
EventBus eventBus = new EventBus();

// Register listener
eventBus.register(new Object() {
    
    @Subscribe
    public void onSessionStarted(SessionStartedEvent event) {
        System.out.println("▶ Session started: " + event.getSessionId());
    }
    
    @Subscribe
    public void onSessionCompleted(SessionCompletedEvent event) {
        SessionMetrics metrics = event.getMetrics();
        System.out.printf("✓ Session %s completed in %s%n",
            event.getSessionId(),
            metrics.getDuration());
    }
    
    @Subscribe
    public void onSessionFailed(SessionFailedEvent event) {
        System.err.printf("✗ Session %s failed: %s%n",
            event.getSessionId(),
            event.getError().getMessage());
    }
});

// Create service with this EventBus
WebHarvestService service = new WebHarvestServiceImpl(
    Executors.newCachedThreadPool(),
    new SessionRegistryImpl(),
    eventBus
);
```

### Future Kafka Integration (v2.3+)

```java
@Component
public class KafkaSessionEventBridge {
    
    @Subscribe
    public void onSessionCompleted(SessionCompletedEvent event) {
        // Publish to Kafka
        kafkaTemplate.send("webharvest.sessions.completed", 
            SessionEventDTO.from(event));
    }
    
    @Subscribe
    public void onSessionFailed(SessionFailedEvent event) {
        kafkaTemplate.send("webharvest.sessions.failed",
            SessionEventDTO.from(event));
    }
}
```

---

## Best Practices

### 1. Always Shutdown Service

```java
try {
    WebHarvestService service = new WebHarvestServiceImpl();
    // ... use service ...
} finally {
    service.shutdown();  // Graceful shutdown
}
```

### 2. Use Client Context for Multi-Tenant

```java
// Identify sessions by client
ClientContext client = ClientContext.builder()
    .clientId(userId)
    .metadata("tier", userTier)
    .build();

ScraperSession session = service.executeAsync(config, client, options);
```

### 3. Set Reasonable Timeouts

```java
SessionOptions options = SessionOptions.builder()
    .timeout(Duration.ofMinutes(30))  // Prevent runaway sessions
    .build();
```

### 4. Monitor and Clean Up

```java
// Periodic cleanup of old completed sessions
ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
scheduler.scheduleAtFixedRate(() -> {
    List<ScraperSession> all = service.listAllSessions();
    for (ScraperSession session : all) {
        if (session.getStatus().isTerminal()) {
            Duration age = Duration.between(
                session.getMetrics().getStartTime(),
                Instant.now()
            );
            if (age.toHours() > 24) {
                // Remove old sessions from registry
                registry.unregister(session.getSessionId());
            }
        }
    }
}, 1, 1, TimeUnit.HOURS);
```

### 5. Handle Errors Gracefully

```java
ScraperSession session = service.executeAsync(config, client, options);
session.awaitCompletion();

switch (session.getStatus()) {
    case COMPLETED:
        processResults(session.getContext());
        break;
    case FAILED:
        logError(session.getError());
        retryOrNotify(session);
        break;
    case CANCELLED:
        handleCancellation(session);
        break;
}
```

---

## API Reference

### WebHarvestService

Main service interface:

```java
public interface WebHarvestService {
    ScraperSession executeAsync(Config config, ClientContext client, SessionOptions options);
    Optional<ScraperSession> getSession(String sessionId);
    List<ScraperSession> listSessions(String clientId);
    List<ScraperSession> listAllSessions();
    boolean cancelSession(String sessionId);
    void shutdown() throws InterruptedException;
    void shutdownNow();
    boolean isShutdown();
}
```

### ScraperSession

Session interface:

```java
public interface ScraperSession {
    String getSessionId();
    String getClientId();
    SessionStatus getStatus();
    SessionMetrics getMetrics();
    Config getConfig();
    DynamicScopeContext getContext();
    Throwable getError();
    boolean cancel();
    void awaitCompletion() throws InterruptedException;
    boolean awaitCompletion(long timeoutMillis) throws InterruptedException;
}
```

### SessionMetrics

Metrics data:

```java
public class SessionMetrics {
    Instant getStartTime();
    Instant getEndTime();
    Duration getDuration();
    long getProcessedElements();
    double getProcessingRate();
}
```

### ClientContext

Client identification:

```java
public class ClientContext {
    String getClientId();
    Map<String, Object> getMetadata();
    Object getMetadata(String key);
    String getMetadataString(String key);
}
```

### SessionOptions

Execution configuration:

```java
public class SessionOptions {
    Duration getTimeout();
    boolean isTrackTokens();
    long getMaxElements();
}
```

---

## Migration from v2.1

### Old Way (Direct Scraper)

```java
// v2.1
Config config = Config.fromFile("scraper.xml");
Scraper scraper = new Scraper(config);
DynamicScopeContext context = new ScraperContext();
scraper.execute(context);
```

### New Way (Session-Based)

```java
// v2.2
WebHarvestService service = new WebHarvestServiceImpl();
Config config = ConfigFactory.create(new File("scraper.xml"));
ScraperSession session = service.executeAsync(config, null, null);
session.awaitCompletion();
```

### Backward Compatibility

**Good news:** Old way still works! No breaking changes in v2.2.

```java
// This still works exactly as before
Scraper scraper = new Scraper();
scraper.execute(context);
```

New session management is **opt-in** - use it when you need:
- Session tracking
- Multi-session execution
- Progress monitoring
- Event subscriptions

---

## Troubleshooting

### Session Not Found

```java
Optional<ScraperSession> session = service.getSession(sessionId);
if (!session.isPresent()) {
    // Session may have been unregistered after completion
    // Or session ID is incorrect
}
```

**Solution:** Keep session references if you need them after completion, or don't unregister until you're done.

### Session Hangs

If a session appears stuck:

```java
// Set timeout to prevent runaway sessions
SessionOptions options = SessionOptions.builder()
    .timeout(Duration.ofMinutes(30))
    .build();
```

### OutOfMemoryError with Many Sessions

Implement cleanup:

```java
// After session completes
if (session.getStatus().isTerminal()) {
    registry.unregister(session.getSessionId());
}
```

---

## What's Next (v2.3+)

Future releases will add:

- **Pause/Resume** - Pause long-running sessions
- **Prometheus Metrics** - Built-in metrics exporter
- **Quota Enforcement** - Limit resource usage per client
- **SDK Facade** - Simplified API for external plugins
- **Kafka Integration** - Event streaming to Kafka

---

## Support

- **API Documentation:** `mvn javadoc:javadoc` → `target/site/apidocs/`
- **Examples:** `webharvest-core/src/test/java/org/webharvest/runtime/service/WebHarvestServiceTest.java`
- **Source Code:** `webharvest-core/src/main/java/org/webharvest/runtime/`

---

**Happy Scraping with WebHarvest 2.2!** 🕷️✨






