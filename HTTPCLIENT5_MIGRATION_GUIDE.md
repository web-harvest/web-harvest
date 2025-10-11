# Apache HttpClient 5.x Migration Guide

**Date:** October 11, 2025  
**Revision:** r753  
**Version:** WebHarvest 2.2.1  

---

## Executive Summary

WebHarvest has successfully migrated from **Apache HttpClient 4.5.14** to **HttpClient5 5.5.1** in revision r753.

**Migration Type:** Major version upgrade (breaking API changes)  
**Approach:** Test-Driven Development (TDD)  
**Result:** ✅ All tests pass (2442/2442)  
**Impact:** Zero regressions, improved security and performance

---

## Why We Migrated

### Security Concerns

**HttpClient 4.x Status:**
- ⚠️ Maintenance mode only (limited updates)
- ⚠️ Security patches becoming rare
- ⚠️ Java compatibility issues with newer JDKs
- ⚠️ End of active development

**HttpClient 5.x Benefits:**
- ✅ Active development with regular security patches
- ✅ Modern security protocols support
- ✅ Better TLS/SSL handling
- ✅ Improved vulnerability response time

### Technical Improvements

**Performance:**
- HTTP/2 support
- Better connection pooling
- Improved async operations
- More efficient resource management

**Compatibility:**
- Full Java 11+ support
- Modern cryptography APIs
- Better module system integration

---

## API Changes Overview

### Package Names Changed

| HttpClient 4.x | HttpClient 5.x |
|----------------|----------------|
| `org.apache.http.*` | `org.apache.hc.core5.http.*` |
| `org.apache.http.client.*` | `org.apache.hc.client5.http.*` |
| `org.apache.http.impl.client.*` | `org.apache.hc.client5.http.impl.classic.*` |

### Class Changes

| HttpClient 4.x | HttpClient 5.x | Notes |
|----------------|----------------|-------|
| `HttpRequestBase` | `ClassicHttpRequest` | Base request type |
| `CloseableHttpClient` | `CloseableHttpClient` | ✅ Same (but different package) |
| `CloseableHttpResponse` | `CloseableHttpResponse` | ✅ Same (but different package) |
| `HttpClients` | `HttpClients` | ✅ Same (but different package) |
| `StringEntity` | `StringEntity` | ✅ Same (but different package) |
| `EntityUtils` | `EntityUtils` | ✅ Same (but different package) |

### Method Changes

| HttpClient 4.x | HttpClient 5.x | Change Type |
|----------------|----------------|-------------|
| `response.getStatusLine().getStatusCode()` | `response.getCode()` | Simplified |
| `response.getAllHeaders()` | `response.getHeaders()` | Renamed |
| `request.addHeader(name, value)` | `request.addHeader(name, value)` | ✅ Same |

---

## Migration Steps (What We Did)

### Step 1: Test Suite Creation (TDD - RED Phase)

Created comprehensive test coverage BEFORE making changes:

**HttpClient5MigrationTest.java** - 7 tests with real HTTP calls:
```java
@Test(groups = "network")
public void testBasicHttpGetWithHttpClient5() throws Exception {
    String url = "https://httpbin.org/get";
    HttpResponse response = httpService.get(url, null);
    assertEquals(response.getStatusCode(), 200);
}
```

**HttpClient5MigrationWithMockTest.java** - 8 tests with WireMock:
```java
@Test
public void testHttpClient5_BasicGet() throws Exception {
    // GIVEN: Mock HTTP server
    stubFor(get(urlEqualTo("/test"))
        .willReturn(aResponse()
            .withStatus(200)
            .withBody("{\"status\": \"ok\"}")));
    
    // WHEN: Execute request
    HttpResponse response = httpService.get(baseUrl + "/test", null);
    
    // THEN: Verify response
    assertEquals(response.getStatusCode(), 200);
}
```

**Why WireMock?**
- ✅ No network dependencies (tests work offline)
- ✅ No rate limiting issues (httpbin.org has 503 limits)
- ✅ Fast execution (no network latency)
- ✅ Reliable CI/CD integration
- ✅ Full control over test scenarios

### Step 2: Update Dependencies

**pom.xml (parent):**
```xml
<!-- OLD -->
<dependency>
    <groupId>org.apache.httpcomponents</groupId>
    <artifactId>httpclient</artifactId>
    <version>4.5.14</version>
</dependency>

<!-- NEW -->
<dependency>
    <groupId>org.apache.httpcomponents.client5</groupId>
    <artifactId>httpclient5</artifactId>
    <version>5.5.1</version>
</dependency>
```

**webharvest-core/pom.xml:**
```xml
<!-- Bug #41 Fix: Apache HttpComponents 5.x (migrated from 4.x in Oct 2025) -->
<dependency>
    <groupId>org.apache.httpcomponents.client5</groupId>
    <artifactId>httpclient5</artifactId>
</dependency>

<!-- Added for testing -->
<dependency>
    <groupId>com.github.tomakehurst</groupId>
    <artifactId>wiremock-jre8-standalone</artifactId>
    <version>2.35.0</version>
    <scope>test</scope>
</dependency>
```

### Step 3: Code Migration (TDD - GREEN Phase)

**HttpServiceImpl.java Changes:**

**1. Import Updates:**
```java
// OLD (4.x)
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

// NEW (5.x)
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ClassicHttpRequest;
import org.apache.hc.core5.http.Header;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.StringEntity;
```

**2. Method Signature Updates:**
```java
// OLD (4.x)
private void addHeaders(HttpRequestBase request, Map<String, String> headers) {
    // ...
}

private HttpResponse executeRequest(HttpRequestBase request) {
    // ...
}

// NEW (5.x)
private void addHeaders(ClassicHttpRequest request, Map<String, String> headers) {
    // ...
}

private HttpResponse executeRequest(ClassicHttpRequest request) {
    // ...
}
```

**3. API Call Updates:**
```java
// OLD (4.x)
int statusCode = response.getStatusLine().getStatusCode();
for (Header header : response.getAllHeaders()) {
    // ...
}

// NEW (5.x)
int statusCode = response.getCode();
for (Header header : response.getHeaders()) {
    // ...
}
```

**4. Documentation Update:**
```java
/**
 * Default implementation of {@link HttpService} using Apache HttpClient 5.x
 * 
 * <p>Migrated from HttpClient 4.5.14 to 5.5.1 in October 2025 for:</p>
 * <ul>
 *   <li>Latest security updates</li>
 *   <li>Improved performance</li>
 *   <li>Modern Java support</li>
 * </ul>
 * 
 * @version 2.2.1
 */
```

### Step 4: Verification (TDD - GREEN Phase)

**Test Results:**
```
✅ HttpClient5MigrationWithMockTest: 8/8 PASS (WireMock)
✅ HttpClient5MigrationTest: 7/7 PASS (network)
✅ HttpServiceImplBasicTest: 4/4 PASS
✅ All existing HTTP tests: PASS
✅ Total: 2442/2442 tests PASS
```

---

## What Stayed The Same

### No Changes to Public API

The `HttpService` interface remains **100% backward compatible**:

```java
public interface HttpService {
    HttpResponse get(String url, Map<String, String> headers) throws HttpServiceException;
    HttpResponse post(String url, String body, Map<String, String> headers) throws HttpServiceException;
    HttpResponse request(String method, String url, String body, Map<String, String> headers) throws HttpServiceException;
}
```

### No Changes to HttpResponse

The `HttpResponse` interface is **unchanged**:
- `getStatusCode()` - returns HTTP status
- `getBody()` - returns response as String
- `getBodyAsBinary()` - returns response as byte[] (Bug #39 fix)
- `getHeaders()` - returns all headers
- `getHeader(String)` - returns specific header
- `getContentType()` - returns Content-Type
- `getCharset()` - returns charset

### No Changes to HttpPlugin

The `HttpPlugin` continues to work without modifications:
- All existing configurations work
- All XML configs compatible
- All HttpPlugin tests pass
- Bug #39 fix (binary content) still works

---

## Testing Strategy

### Three-Layer Testing Approach

**Layer 1: Unit Tests with WireMock (Offline)**
- 8 comprehensive tests
- No network required
- Fast execution (<5 seconds)
- Reliable in CI/CD
- Test all scenarios (GET, POST, headers, binary, errors)

**Layer 2: Integration Tests (Network)**
- 7 tests with real HTTP calls
- Verify actual HTTP behavior
- Marked with `@Test(groups = "network")`
- Can be skipped in offline environments

**Layer 3: Existing Test Suite**
- All 2427 original tests
- Verify no regressions
- Cover entire HttpPlugin functionality
- Integration with other plugins

### Coverage

**HTTP Operations:**
- ✅ GET requests
- ✅ POST requests with body
- ✅ Custom headers
- ✅ Binary content (images, PDFs)
- ✅ Various status codes (200, 404, 500, etc.)
- ✅ Response headers parsing
- ✅ Empty responses
- ✅ Large responses (1MB+)
- ✅ Redirects

**Edge Cases:**
- ✅ Null headers
- ✅ Empty body
- ✅ Special characters
- ✅ Various charsets
- ✅ Error handling

---

## Benefits of Migration

### Security

| Aspect | HttpClient 4.x | HttpClient 5.x |
|--------|----------------|----------------|
| Active Development | ⚠️ Maintenance only | ✅ Active |
| Security Patches | ⚠️ Rare | ✅ Regular |
| Vulnerability Response | ⚠️ Slow | ✅ Fast |
| TLS Support | ⚠️ Limited | ✅ Modern |
| Java 11+ Support | ⚠️ Basic | ✅ Full |

### Performance

- **HTTP/2:** Full support for modern protocol
- **Connection Pooling:** Improved algorithms
- **Memory:** Better resource management
- **Async:** Enhanced async capabilities

### Future-Proofing

- ✅ Ready for HTTP/3 (QUIC)
- ✅ Modern Java features
- ✅ Active community
- ✅ Regular updates
- ✅ Long-term support

---

## Troubleshooting

### Common Migration Issues (We Solved)

**Issue 1: Compilation Errors**
```
error: package org.apache.http does not exist
```
**Solution:** Update all imports from `org.apache.http.*` to `org.apache.hc.*`

**Issue 2: Method Not Found**
```
error: cannot find symbol - method getStatusLine()
```
**Solution:** Use `response.getCode()` instead of `response.getStatusLine().getStatusCode()`

**Issue 3: Type Mismatch**
```
error: incompatible types: HttpRequestBase cannot be converted to ClassicHttpRequest
```
**Solution:** Change method signatures to use `ClassicHttpRequest`

### Network Test Failures

**Symptom:** Tests fail with 503 status code  
**Cause:** httpbin.org rate limiting  
**Solution:** Use WireMock tests (always reliable)

**Run only WireMock tests:**
```bash
mvn test -Dtest=HttpClient5MigrationWithMockTest
```

**Skip network tests:**
```bash
mvn test -DexcludedGroups=network
```

---

## Verification Checklist

### Before Migration
- [x] All tests passing with HttpClient 4.x (2427/2427)
- [x] Identified all HttpClient usage (1 file: HttpServiceImpl.java)
- [x] Created comprehensive test suite
- [x] Added WireMock for reliable testing

### During Migration
- [x] Updated parent POM dependency
- [x] Updated module POM dependency
- [x] Updated all imports in HttpServiceImpl
- [x] Updated method signatures
- [x] Updated API calls

### After Migration
- [x] All tests pass (2442/2442) ✅
- [x] WireMock tests pass (8/8) ✅
- [x] No compilation errors ✅
- [x] No runtime errors ✅
- [x] HttpPlugin still works ✅
- [x] Binary content still works (Bug #39) ✅
- [x] All modules build successfully ✅

---

## Testing Commands

### Run All Tests
```bash
mvn test
```

### Run Only HTTP Tests
```bash
mvn test -Dtest="*Http*Test"
```

### Run Only WireMock Tests (No Network)
```bash
mvn test -Dtest=HttpClient5MigrationWithMockTest
```

### Skip Network Tests
```bash
mvn test -DexcludedGroups=network
```

### Full Build
```bash
mvn clean install
```

---

## Files Modified

### Dependencies
1. `pom.xml` (parent) - Updated dependency version
2. `webharvest-core/pom.xml` - Updated dependency + added WireMock

### Implementation
3. `webharvest-core/src/main/java/org/webharvest/plugin/services/HttpServiceImpl.java`
   - Updated imports (10 changes)
   - Updated method signatures (2 changes)
   - Updated API calls (2 changes)
   - Updated documentation

### Tests (New)
4. `webharvest-core/src/test/java/org/webharvest/plugin/services/HttpClient5MigrationTest.java`
   - 7 comprehensive tests with network calls
   
5. `webharvest-core/src/test/java/org/webharvest/plugin/services/HttpClient5MigrationWithMockTest.java`
   - 8 comprehensive tests with WireMock
   - No network dependencies
   - Reliable and fast

---

## For Future Developers

### If You Need to Add HTTP Tests

**Use WireMock (Recommended):**
```java
import com.github.tomakehurst.wiremock.WireMockServer;
import static com.github.tomakehurst.wiremock.client.WireMock.*;

@BeforeMethod
public void setUp() {
    wireMockServer = new WireMockServer(options().dynamicPort());
    wireMockServer.start();
}

@Test
public void testSomething() {
    // Setup mock response
    stubFor(get(urlEqualTo("/test"))
        .willReturn(aResponse()
            .withStatus(200)
            .withBody("response")));
    
    // Execute test
    HttpResponse response = httpService.get(baseUrl + "/test", null);
    
    // Verify
    assertEquals(response.getStatusCode(), 200);
}
```

**Benefits:**
- No external dependencies
- Fast and reliable
- Works offline
- Perfect for CI/CD

### If You Need to Debug HTTP Issues

**Enable debug logging:**
```properties
# log4j.properties
log4j.logger.org.webharvest.plugin.services=DEBUG
log4j.logger.org.apache.hc=DEBUG
```

**Check logs:**
```
DEBUG HttpServiceImpl - Executing GET request to: http://example.com
DEBUG HttpServiceImpl - HTTP request completed with status code: 200, body size: 1234 bytes
```

---

## Compatibility Notes

### Backward Compatibility

**✅ FULL backward compatibility maintained:**
- All existing XML configurations work
- All existing code using HttpService works
- No API changes to public interfaces
- Bug #39 fix (binary content) still works
- All 2427 existing tests pass

### Forward Compatibility

**HttpClient 5.x is designed for:**
- HTTP/2 and HTTP/3 (future)
- Modern Java (11+, 17+, 21+)
- New security protocols
- Enhanced async operations

---

## Performance Impact

### Before vs After

**Build Time:**
- Before: ~55 seconds (mvn clean install)
- After: ~53 seconds (2-second improvement)

**Test Time:**
- Before: ~25 seconds (all tests)
- After: ~30 seconds (includes 15 new tests)

**Runtime:**
- No measurable difference (both very fast)
- HTTP/2 support available for future optimization

---

## References

### Official Documentation
- [HttpClient 5.x Migration Guide](https://hc.apache.org/httpcomponents-client-5.x/migration-guide/index.html)
- [HttpClient 5.x API Documentation](https://hc.apache.org/httpcomponents-client-5.x/current/httpclient5/apidocs/)
- [WireMock Documentation](http://wiremock.org/docs/)

### WebHarvest Documentation
- `COMPLETE_SESSION_REPORT_OCT11_2025.md` - Full session details
- `DEPENDENCY_CLEANUP_SESSION_SUMMARY.md` - Dependencies cleanup
- `CHANGELOG.md` - Version 2.2.1 release notes

### Related Commits
- r753 - HttpClient 5.x migration
- r754 - Documentation updates

---

## Success Metrics

| Metric | Target | Achieved |
|--------|--------|----------|
| Test Pass Rate | 100% | ✅ 100% (2442/2442) |
| Zero Regressions | Yes | ✅ Yes |
| Compilation Errors | 0 | ✅ 0 |
| Breaking Changes | 0 | ✅ 0 |
| New Tests Added | 10+ | ✅ 15 |
| Documentation | Complete | ✅ Complete |

---

## Conclusion

The migration from Apache HttpClient 4.5.14 to HttpClient5 5.5.1 was completed successfully using TDD methodology:

✅ **Security:** Critical component now on actively maintained version  
✅ **Quality:** 15 new tests ensure reliability  
✅ **Compatibility:** Zero breaking changes to public API  
✅ **Testing:** WireMock provides reliable, fast testing  
✅ **Documentation:** Complete migration guide for future reference  

**WebHarvest is now using modern, secure HTTP client infrastructure.**

---

*This migration ensures WebHarvest remains secure and compatible with modern web technologies.*

