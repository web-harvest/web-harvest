# Session Management API Reference

**Version**: 2.2.0  
**Package**: `org.webharvest.runtime.session`, `org.webharvest.runtime.client`, `org.webharvest.runtime.tracking`, `org.webharvest.runtime.service`

---

## Package: org.webharvest.runtime.session

### SessionStatus (enum)

```java
public enum SessionStatus {
    PENDING,      // Created but not started
    RUNNING,      // Currently executing
    COMPLETED,    // Finished successfully
    FAILED,       // Terminated with error
    CANCELLED;    // Cancelled by user/system
    
    boolean isTerminal();
    boolean isSuccessful();
    boolean isActive();
}
```

### SessionMetrics

```java
public class SessionMetrics {
    // Timing
    Instant getStartTime();
    Instant getEndTime();
    Duration getDuration();
    
    // Progress
    long getProcessedElements();
    long incrementProcessedElements();
    long incrementProcessedElements(long count);
    
    // Performance
    double getProcessingRate();  // elements per second
    
    // Internal
    void markEnded();
    void markEnded(Instant endTime);
}
```

### ScraperSession (interface)

```java
public interface ScraperSession {
    // Identification
    String getSessionId();
    String getClientId();
    
    // State
    SessionStatus getStatus();
    SessionMetrics getMetrics();
    Config getConfig();
    DynamicScopeContext getContext();
    Throwable getError();
    
    // Control
    boolean cancel();
    
    // Blocking wait
    void awaitCompletion() throws InterruptedException;
    boolean awaitCompletion(long timeoutMillis) throws InterruptedException;
}
```

### ScraperSessionImpl

```java
public class ScraperSessionImpl implements ScraperSession {
    // Constructors
    public ScraperSessionImpl(String sessionId, String clientId, Config config);
    public ScraperSessionImpl(String sessionId, String clientId, Config config, EventBus eventBus);
    
    // Lifecycle management (package-private)
    public void markRunning(DynamicScopeContext context);
    public void markCompleted();
    public void markFailed(Throwable error);
}
```

### SessionRegistry (interface)

```java
public interface SessionRegistry {
    // Registration
    void register(ScraperSession session);
    boolean unregister(String sessionId);
    void clear();
    
    // Lookup
    Optional<ScraperSession> getSession(String sessionId);
    List<ScraperSession> getSessionsByClientId(String clientId);
    List<ScraperSession> getAllSessions();
    
    // Utility
    int size();
    boolean isEmpty();
}
```

### SessionRegistryImpl

```java
public class SessionRegistryImpl implements SessionRegistry {
    public SessionRegistryImpl();
    public SessionRegistryImpl(int initialCapacity);
}
```

---

## Package: org.webharvest.runtime.client

### ClientContext

```java
public final class ClientContext {
    // Accessors
    String getClientId();
    Map<String, Object> getMetadata();
    Object getMetadata(String key);
    String getMetadataString(String key);
    boolean hasMetadata(String key);
    
    // Factory methods
    static Builder builder();
    static ClientContext of(String clientId);
    
    // Builder
    public static class Builder {
        Builder clientId(String clientId);
        Builder metadata(String key, Object value);
        Builder metadata(Map<String, Object> metadata);
        ClientContext build();
    }
}
```

---

## Package: org.webharvest.runtime.tracking

### ResourceType (enum)

```java
public enum ResourceType {
    HTTP_REQUEST,     // Count of HTTP requests
    HTTP_BYTES,       // Bytes downloaded
    CPU_TIME,         // Milliseconds of CPU time
    MEMORY_PEAK;      // Peak memory in bytes
    
    String getMetricName();
    String getUnit();
    String getDescription();
}
```

### TokenUsage

```java
public final class TokenUsage {
    // Generic access
    long get(ResourceType type);
    Map<ResourceType, Long> getAll();
    
    // Convenience accessors
    long getHttpRequests();
    long getHttpBytes();
    long getCpuTimeMillis();
    long getMemoryPeakBytes();
    
    // Utility
    boolean isEmpty();
}
```

### TokenTracker (interface)

```java
public interface TokenTracker {
    // Identification
    String getSessionId();
    String getClientId();
    
    // Recording
    void record(ResourceType type, long amount);
    
    // Querying
    TokenUsage getUsage();
    
    // Management
    void reset();
}
```

### TokenTrackerImpl

```java
public class TokenTrackerImpl implements TokenTracker {
    public TokenTrackerImpl(String sessionId, String clientId);
}
```

---

## Package: org.webharvest.runtime.service

### WebHarvestService (interface)

```java
public interface WebHarvestService {
    // Execution
    ScraperSession executeAsync(Config config, 
                                ClientContext client, 
                                SessionOptions options);
    
    // Session management
    Optional<ScraperSession> getSession(String sessionId);
    List<ScraperSession> listSessions(String clientId);
    List<ScraperSession> listAllSessions();
    boolean cancelSession(String sessionId);
    
    // Lifecycle
    void shutdown() throws InterruptedException;
    void shutdownNow();
    boolean isShutdown();
}
```

### WebHarvestServiceImpl

```java
public class WebHarvestServiceImpl implements WebHarvestService {
    // Constructors
    public WebHarvestServiceImpl();
    public WebHarvestServiceImpl(ExecutorService executorService);
    public WebHarvestServiceImpl(ExecutorService executorService,
                                  SessionRegistry sessionRegistry,
                                  EventBus eventBus);
}
```

### SessionOptions

```java
public final class SessionOptions {
    // Accessors
    Duration getTimeout();
    boolean hasTimeout();
    boolean isTrackTokens();
    long getMaxElements();
    boolean hasMaxElements();
    
    // Constants
    static final SessionOptions DEFAULT;
    
    // Builder
    static Builder builder();
    
    public static class Builder {
        Builder timeout(Duration timeout);
        Builder trackTokens(boolean trackTokens);
        Builder maxElements(long maxElements);
        SessionOptions build();
    }
}
```

---

## Package: org.webharvest.runtime.session.events

### SessionEvent (abstract)

```java
public abstract class SessionEvent {
    ScraperSession getSession();
    String getSessionId();
    String getClientId();
    Instant getTimestamp();
}
```

### Event Types

```java
public final class SessionCreatedEvent extends SessionEvent { }

public final class SessionStartedEvent extends SessionEvent { }

public final class SessionCompletedEvent extends SessionEvent {
    SessionMetrics getMetrics();
}

public final class SessionFailedEvent extends SessionEvent {
    Throwable getError();
}

public final class SessionCancelledEvent extends SessionEvent { }
```

---

## Complete Example

```java
import org.webharvest.runtime.service.*;
import org.webharvest.runtime.session.*;
import org.webharvest.runtime.client.ClientContext;
import org.webharvest.definition.Config;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;

public class WebHarvestExample {
    
    public static void main(String[] args) throws Exception {
        // Create service
        WebHarvestService service = new WebHarvestServiceImpl();
        
        // Load configuration
        Config config = ConfigFactory.create(new File("scraper.xml"));
        
        // Create client context
        ClientContext client = ClientContext.builder()
            .clientId("my-app")
            .metadata("version", "1.0")
            .build();
        
        // Configure options
        SessionOptions options = SessionOptions.builder()
            .timeout(Duration.ofMinutes(30))
            .trackTokens(true)
            .build();
        
        // Execute
        ScraperSession session = service.executeAsync(config, client, options);
        System.out.println("Session ID: " + session.getSessionId());
        
        // Wait for completion
        session.awaitCompletion();
        
        // Check result
        if (session.getStatus().isSuccessful()) {
            System.out.println("Success!");
            System.out.println("Duration: " + session.getMetrics().getDuration());
            System.out.println("Elements: " + session.getMetrics().getProcessedElements());
            
            DynamicScopeContext context = session.getContext();
            Variable result = context.getVar("result");
            System.out.println("Result: " + result);
        } else {
            System.err.println("Failed: " + session.getError().getMessage());
        }
        
        // Cleanup
        service.shutdown();
    }
}
```

---

## Session History API (IDE v2.2.0)

### Overview

The IDE provides a REST API for retrieving execution history. All completed sessions are stored in memory and can be queried for auditing, performance analysis, and debugging.

### Endpoint: GET /api/sessions/history

**URL**: `http://localhost:8080/api/sessions/history`

**Query Parameters**:
- `limit` (integer, optional): Max sessions to return (default: 20, max: 100)
- `offset` (integer, optional): Skip first N sessions for pagination (default: 0)
- `status` (string, optional): Filter by status (COMPLETED, FAILED, RUNNING, etc.)

**Response Format**:
```json
{
  "sessions": [
    {
      "sessionId": "abc123-def456-...",
      "timestamp": "2025-10-13T12:00:00Z",
      "status": "COMPLETED",
      "duration": 150,
      "configName": "scraper.xml",
      "elementsProcessed": 25,
      "processingRate": 166.67
    }
  ],
  "total": 42,
  "limit": 20,
  "offset": 0
}
```

**Example Usage**:
```bash
# Get last 20 sessions
curl http://localhost:8080/api/sessions/history

# Get only completed sessions
curl http://localhost:8080/api/sessions/history?status=COMPLETED

# Pagination
curl http://localhost:8080/api/sessions/history?limit=50&offset=50
```

**UI Access**:
1. Open IDE: http://localhost:8080
2. Click "Session" tab in right sidebar
3. Click "View All Sessions" button
4. Browse session history with full metrics

---

**See also:**
- [Session History Guide](../SESSION_HISTORY.md)
- [User Guide](SESSION_USER_GUIDE.md)
- [Architecture Documentation](SESSION_TOKEN_ARCHITECTURE.md)
- JavaDoc: `mvn javadoc:javadoc`






