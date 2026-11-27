# Variable, Event and Payload Call-Site Audit

**Status:** Bounded source/call-site audit; not a claim of exhaustive external-consumer coverage.
**Inspected source revision:** `main` at `c8830f0fe2f1e283abea95318d2c6fd9a644d0a4`.
**Related design:** [WebHarvest Data, Stream and Semantic Contract Architecture](WEBHARVEST_DATA_AND_STREAM_CONTRACT_ARCHITECTURE.md).
**Related baseline:** [WebHarvest Data Contract Runtime Audit](WEBHARVEST_DATA_CONTRACT_RUNTIME_AUDIT.md).

## 1. Executive findings

This pass traced representative producer/consumer paths from plugin execution through HTTP, files, database results, Variable conversion and session events.

The most consequential findings are:

1. **There are two distinct in-process event paths.** The older `org.webharvest.events` path routes `HarvesterEvent` through a Harvester-scoped Guava EventBus. The newer `org.webharvest.runtime.session.events` path publishes session/plugin lifecycle events through the EventBus carried by `ScraperSession` and `DynamicScopeContext`. They are not one unified event contract.
2. **The newer event path is not a durable event store.** `AbstractPlugin` calls `EventBus.post()`; `ScraperSessionImpl` posts lifecycle events; the full-stream integration test creates an in-memory EventBus and an in-memory collector. The inspected path contains no durable append, event serialization, checkpoint, replay reader or retention policy. The “event sourcing” wording in class/test documentation overstates what this implementation demonstrates.
3. **Successful plugin events expose the raw result Variable.** A subscriber can retain a mutable/large wrapped value, including a byte array. There is no payload reference, redaction contract or retention boundary in this event type.
4. **HTTP response handling is fully materialized.** `HttpServiceImpl` calls `EntityUtils.toByteArray(entity)`; `HttpPlugin` returns the complete response as a `NodeVariable` wrapping a byte array for binary content.
5. **File binary support is incomplete and asymmetric.** Binary writes are supported, but `FilePlugin.handleFileRead()` explicitly returns `EmptyVariable` for `type="binary"`.
6. **List/binary aggregation is memory-based.** `ListVariable.toBinary()` accumulates all bytes in a `ByteArrayOutputStream`; `FilePlugin.appendBinary()` repeatedly allocates and copies a growing byte array.
7. **Database query results are materialized as XML text.** `DatabasePlugin.executeSql()` reads every row into a `StringBuilder` and returns one `NodeVariable`; the inspected implementation does not expose a cursor/stream result contract.
8. **Variable is also used as a resource-handle carrier.** `ConnectionPlugin.ConnectionFactoryVariable` extends `NodeVariable` while retaining a `StandaloneConnectionPool` and connection credentials; `getNamedConnection()` recovers it by concrete subclass check. This is a legacy context convention, not a data payload. It reinforces that `Variable` must not become the ownership or lifecycle abstraction for the new runtime.

These are current implementation facts on the inspected revision. They justify a separate payload/stream contract boundary; they do not select a transport, storage engine, media framework or third-party library.

## 2. Event path inventory

### 2.1 Legacy Harvester-scoped events

**FACT —** `webharvest-core/src/main/java/org/webharvest/events/DefaultHandlerHolder.java`

- Stores registered handlers in a `LinkedList<EventHandler<?>>`.
- On `onBeforeScraping(Harvester)`, obtains an EventBus from a Guice Provider, registers handlers and associates the bus with the Harvester in a Registry.
- On `onAfterScraping(Harvester)`, unregisters handlers and removes the association.

**FACT —** `webharvest-core/src/main/java/org/webharvest/events/HarvesterEventSink.java`

- Resolves the EventBus by `event.getHarvester()`.
- Calls `eventBus.post(event)`.
- Throws if the event or scope bus is missing.

**FACT —** `EventSink.java` describes listener dispatch, not a durable store. Its API exposes only `publish(event)`.

**INFERENCE —** This is scoped in-process notification infrastructure. It must not be conflated with the newer session/plugin lifecycle event family or used as evidence of persistent event sourcing.

### 2.2 Session/plugin lifecycle events

**FACT —** `webharvest-core/src/main/java/org/webharvest/runtime/session/ScraperSessionImpl.java`

- The default constructor creates a new Guava `EventBus`.
- The constructor posts `SessionCreatedEvent`.
- `markRunning`, `markCompleted`, `markFailed` and cancellation post corresponding session lifecycle events.
- The EventBus is an in-memory object supplied to the session; no durable event repository is part of this class.

**FACT —** `webharvest-core/src/main/java/org/webharvest/runtime/DynamicScopeContext.java` exposes `getEventBus()` and `setEventBus()`. The context documentation says the reference is set once and supports calls from multiple threads.

**FACT —** `webharvest-core/src/main/java/org/webharvest/plugin/AbstractPlugin.java`

- At entry to `execute()`, reads the EventBus and ScraperSession from the context.
- Posts `PluginExecutionStartedEvent`.
- On exception, posts `PluginExecutionFailedEvent` with the Throwable and elapsed time.
- On success, posts `PluginExecutionCompletedEvent` with the returned Variable and elapsed time.
- Event publication failures are caught and logged; plugin execution is not failed solely because event publication failed.
- Records plugin timing into session metrics in the success/failure paths.

**FACT —** `PluginExecutionCompletedEvent.java` retains the exact `Variable` reference supplied to its constructor and returns it from `getResult()`. Its `toString()` only reports whether a result exists, not its contents.

**FACT —** `SessionEvent.java` records `Instant.now()` at event-object construction. This is event creation time, not source/capture time or ingestion time.

**FACT —** The standard runtime's `WebHarvestServiceImpl` owns one EventBus instance and passes it to all sessions and execution contexts created by that service. The IDE's `ExecutionManager.createSessionForContext()` creates a separate EventBus per IDE job. `ScraperModule` also binds a Guava EventBus in `ScrapingScope`, and `EventBusTypeListener` registers Guice-created subscriber objects on the injector's scoped bus. These are composition-dependent in-process buses, not one repository-wide event transport. Plugin-owned subscribers such as the database pool and browser session state explicitly register on `context.getEventBus()`.

**FACT —** The IDE's `SessionHistoryServlet` reads `ExecutionManager.getCompletedExecutions()`; the manager stores completed `ExecutionJob` instances in an in-memory map. The inspected history path is process-local retention, not a durable event log or replay facility.

**BOUNDARY —** The test class name and comments use “event sourcing”, but the test's collector is only a `CopyOnWriteArrayList` registered on a test-local Guava EventBus. No inspected production subscriber persists the plugin event stream. This conclusion is bounded to the inspected event/session/history paths; it is not a claim about arbitrary external host integrations.

**FACT —** `webharvest-core/src/test/java/org/webharvest/runtime/session/events/EventSourcingFullStreamBug24IntegrationTest.java` creates `new EventBus()`, registers a test collector backed by `CopyOnWriteArrayList`, executes test plugins and asserts that the collector received start/completion/failure notifications and ordering. The test does not write an event log or reconstruct execution from persisted events.

**FACT —** `ScraperSession.java` explicitly documents that built-in processed-element, HTTP and plugin counters are not automatically connected to runtime events (issue #168); only timing is recorded by the built-in service.

**INFERENCE —** The current contract is best named *in-process execution lifecycle notifications*. Durable event sourcing, replay, event delivery guarantees and cross-process event transport remain unimplemented/unproven by this inspected path.

### 2.3 A concrete lifecycle subscriber

**FACT —** `webharvest-database/src/main/java/org/webharvest/runtime/database/StandaloneConnectionPool.java`

- `registerForSession(EventBus)` registers the pool as a subscriber.
- `SessionCompletedEvent`, `SessionFailedEvent` and `SessionCancelledEvent` handlers call `closeAllConnections()`.
- `closeAllConnections()` releases physical connections and unregisters the pool from that EventBus.

**FACT —** `DatabasePlugin.java` registers an inline connection pool with `context.getEventBus()` when available. It then relies on session lifecycle events for physical connection cleanup; without a session EventBus it closes the pool directly in its own finally path.

**INFERENCE —** Session events currently participate in resource lifecycle management, not only monitoring. Any future event abstraction or replacement must preserve deterministic cleanup on success, failure and cancellation, and must define behavior when event dispatch fails or the session terminates abnormally.

## 3. Payload producer/consumer call-site map

| Path | Current representation and behavior | Consequence / limitation |
|---|---|---|
| `HttpServiceImpl.executeRequest()` | Reads the complete HTTP entity with `EntityUtils.toByteArray()`; returns a response backed by `byte[]`. | Whole response is buffered before plugin-level processing; no streaming response handle or backpressure. |
| `HttpPlugin.doExecute()` | Detects binary content and returns `new NodeVariable(binaryData)`; text content becomes `NodeVariable(String)`. | Payload type is inferred from content type and represented as arbitrary wrapped object; no explicit media type/length/checksum/ownership contract on Variable. |
| `FilePlugin.handleFileRead()` | Text mode calls `Files.readAllBytes()` and constructs a String; binary mode returns `EmptyVariable`. | Text read materializes full file; binary read is explicitly not implemented. |
| `FilePlugin.handleFileWrite()` | Binary mode converts body to byte array and writes via `FileOutputStream`; text mode converts body to String then bytes. | Binary write is supported but still consumes a fully materialized Variable/byte array; text path is lossy for non-text data. |
| `FilePlugin.appendBinary()` | Iterates `body.toList()`, obtains each `toBinary()`, repeatedly allocates a larger array and copies old+new bytes. | Full materialization and repeated copying; potentially quadratic copying cost as fragments accumulate. |
| `ListVariable.toBinary()` | Appends every child byte array into `ByteArrayOutputStream`, then returns a new byte array. | Full aggregate must fit in memory; no demand, cancellation or bounded-buffer contract. |
| `ListVariable.toString()` | Joins non-empty child values with newline delimiters. | Flattening is not a reversible serialization of arbitrary structured values. |
| `NodeVariable` | Stores arbitrary `Object`; `toBinary()` returns the same wrapped byte[] reference when data is a byte array; `getWrappedObject()` returns the raw object. | No defensive ownership boundary; callers may share/mutate the same array. |
| `DatabasePlugin.executeSql()` | Reads a SELECT ResultSet row-by-row but appends all values into one XML `StringBuilder`; returns one `NodeVariable`. | Row iteration is incremental internally, but output is fully materialized; column values are coerced to String; no typed row schema/cursor/stream result. |
| `JsonToXmlPlugin.doExecute()` / `XmlToJsonPlugin.doExecute()` | Convert whole body strings into in-memory JSON/XML structures and return serialized text in `NodeVariable`. | Transformation boundary is whole-document materialization, not incremental parsing or streaming transformation. |

### 3.1 Additional Variable semantics relevant to adapters

**FACT —** `NodeVariable.java`

- `toString()` decodes byte arrays as UTF-8 by default.
- `toBinary()` returns the wrapped byte array directly when the wrapped object is `byte[]`; otherwise it stringifies then encodes.
- `getWrappedObject()` returns the original `data` reference.
- `toList()` derives a list from iterator semantics; it is not a typed record collection.

**FACT —** `ListVariable.java`

- Owns a mutable `ArrayList<Variable>`.
- `toList()` and `getList()` expose that backing collection.
- `addVariable()` mutates the collection and flattens nested `ListVariable` children by adding their elements.
- `toBinary()` concatenates child byte arrays without framing or element boundaries.

**INFERENCE —** A migration adapter cannot safely infer semantic type, framing, ownership, immutability or missing-value meaning from these conversions. Compatibility adapters should preserve existing behavior while new APIs make these properties explicit.

### 3.2 Numeric conversion and null/empty semantics

**FACT —** `webharvest-core/src/main/java/org/webharvest/runtime/variables/Variable.java`

- `toInt()`, `toLong()` and `toDouble()` inspect `getWrappedObject()`.
- If the wrapped object is actually `null`, the methods return `0`, `0L` and `0d` respectively.
- A wrapped `Number` is narrowed/coerced using `intValue()`, `longValue()` or `doubleValue()`; this includes silent truncation for floating-to-integral conversions and narrowing overflow semantics inherited from `Number`.
- A wrapped `Boolean` maps to 1/0.
- Other values are parsed from trimmed `toString()`; malformed or empty strings throw the JDK parsing exception.
- `toBoolean()` also maps a genuinely null wrapped object to false, but its non-Boolean string semantics are separately defined.

**FACT —** `NodeVariable.java` and `EmptyVariable.java`

- `new NodeVariable(null)` preserves a genuine null wrapped object; `isEmpty()` returns true, while its string/binary representations are empty.
- `EmptyVariable.INSTANCE` is a different representation: `getWrappedObject()` returns the empty String, not null.
- Consequently, the current numeric methods return zero for `NodeVariable(null)`, but throw `NumberFormatException` for `EmptyVariable.INSTANCE` because they attempt to parse an empty string.
- An empty string in `NodeVariable("")` also throws during numeric parsing.

**FACT —** `CommonUtil.createVariable(Object)` normalizes both Java `null` and an empty String to `EmptyVariable.INSTANCE`. It also normalizes an incoming Variable when it is already EmptyVariable or reports `isEmpty()`.

**FACT —** The Variable collection paths add another inconsistency:

- `NodeVariable.isEmpty()` returns true for null, empty CharSequence, empty Iterator/Iterable and empty `XmlNodeWrapper`. It does not treat an empty `byte[]` as empty, even though its string representation is empty. Therefore `isEmpty()` is a content/shape heuristic, not a reliable null/missing predicate.
- `ListVariable(Iterable)` wraps non-Variable members in NodeVariable and drops every member whose `isEmpty()` is true. This can discard null, empty text, empty collections and empty XML wrappers during construction.
- `ListVariable.addVariable(null)` instead stores `EmptyVariable.INSTANCE`; it does not apply the constructor's empty-member filtering. Other empty Variables can also be added and retained.
- `ListVariable.isEmpty()` returns true when all stored members are empty, even when the underlying list is non-empty. Its inherited numeric conversions parse the list's joined string representation; the existing tests rely on this behavior for one-element numeric lists.

**IMPORTANT CORRECTION —** The “null becomes zero” behavior is not the general factory-path behavior. It occurs when a Variable directly preserves a null wrapped object (for example, explicit `new NodeVariable(null)`). The common `CommonUtil.createVariable(null)` path collapses null into EmptyVariable, whose numeric conversion currently throws. Constructor, factory and collection paths therefore do not share one consistent empty/null contract.

**FACT —** Test coverage confirms and preserves parts of this inconsistency:

- `VariableTest` explicitly asserts zero for all three `NodeVariable(null)` conversions and covers malformed non-empty text as `NumberFormatException`; it does not characterize numeric conversion of EmptyVariable.
- `NodeVariableTest` asserts that null and empty-string NodeVariables are both empty, while retaining different wrapped objects.
- `ListVariableTest.testConstructorWithEmptyVariables` explicitly expects empty members to be filtered by the iterable constructor. `testAddNullVariable` explicitly expects null to be retained as EmptyVariable. `testToInt`, `testToLong` and `testToDouble` preserve numeric conversion of one-element lists.
- `CommonUtilTest` currently has only URL-reading and URL-validation tests; it does not characterize `createVariable` normalization.

These tests encode current behavior, not necessarily a desirable semantic contract. Any cleanup must first replace accidental path-dependent behavior with an explicit, reviewed contract and a characterization matrix.

**REQUIRED CHARACTERIZATION MATRIX — before implementation**

| Input / construction path | Current observed behavior | Required decision/test |
|---|---|---|
| `new NodeVariable(null)` | Preserves null; empty string/binary form; numeric conversions return zero. | Decide whether this is explicit null or missing; strict conversion must not silently manufacture zero. |
| `EmptyVariable.INSTANCE` | Wrapped object is empty String; numeric conversion throws `NumberFormatException`. | Define whether this represents absent value, empty text, or a legacy combined state. |
| `new NodeVariable("")` | Distinct wrapped String; `isEmpty()` true; numeric conversion throws. | Decide whether empty text is valid data distinct from absence. |
| `CommonUtil.createVariable(null/"")` | Both become the same EmptyVariable singleton. | Decide whether factory normalization is retained or split into explicit null and empty states. |
| Numeric zero (`0`, `0L`, `0d`, `"0"`) | Present numeric/string values convert to zero. | Must remain distinguishable from all absent/empty states. |
| `new NodeVariable(new byte[0])` | `isEmpty()` false, although `toString()` is empty; numeric parsing fails. | Define empty binary payload semantics independently of null/missing. |
| `new ListVariable(iterable)` containing empty members | Constructor drops members for which `isEmpty()` is true. | Decide whether empty members are data and whether constructor filtering is intentional. |
| `list.addVariable(null)` | Retains one EmptyVariable member; list itself reports empty. | Align or explicitly document this behavior against iterable construction. |
| One-element numeric ListVariable | Inherited numeric conversion parses its joined string and succeeds. | Preserve only if list-to-scalar conversion is an intentional public contract. |
| Multi-element numeric ListVariable | Joined text contains delimiters/newlines and ordinarily fails scalar parsing. | Specify scalar conversion behavior for collections; do not infer first/last/sum. |
| Undefined context binding | `ScraperContext.getVar(name)` returns Java `null`. | Keep missing binding distinct from a defined empty Variable. |
| Defined binding assigned Java null | Context stores `EmptyVariable.INSTANCE`; subsequent `getVar(name)` is non-null. | Characterize this separately from an undefined binding. |
| SetVar arithmetic with malformed/missing operand | Current addition/subtraction path substitutes zero; tests assert this. | Review as a DSL compatibility decision; do not inherit behavior accidentally from Variable conversions. |
| Numeric-looking String copied to JSR-223 | Adapter attempts Integer/Double conversion before exposing the binding to the script engine. | Preserve/alter only through an explicit scripting-boundary contract. |
| Malformed non-empty text | JDK parsing exception. | Preserve distinction between invalid data and absent data. |
| Boolean and Number inputs | Boolean maps to 1/0; Number uses Java narrowing/conversion semantics. | Treat as separate compatibility decisions; do not change as collateral cleanup. |

This matrix is a test-design requirement, not an approval to change the current API. It should be implemented as characterization tests before any production refactor, including factory, constructor, collection, scripting and optional-module paths.

**REPOSITORY INVENTORY LIMITATION —** The recursive Git tree establishes repository scope, not a complete symbol scan. The follow-up source inventory in section 4.1 fetched 65 targeted Core production Java files and all 49 non-Core module production Java files, plus the docs generator. It still does not scan every Core package, test source, resource, generated file, documentation example or downstream application.

**FACT —** The targeted production-source pass (65 Core boundary/plugin files and 49 non-Core module files) did not reveal direct production calls to `Variable.toInt()`, `toLong()` or `toDouble()` outside the methods themselves. This is bounded evidence, not proof of no call sites in the remaining Core packages, generated code, scripts, reflection paths or downstream applications.

**INFERENCE —** The first correction should be to define a consistent distinction between *no numeric value* and the numeric value zero. It should not be implemented by changing only the three null branches while leaving the factory-normalized EmptyVariable path undocumented.

**DESIGN CANDIDATE —** A coherent API can offer three explicit operations per numeric type:

- Strict conversion (`toInt()`, `toLong()`, `toDouble()`): return a number only when conversion is valid; absence/empty input must fail rather than silently become zero. Preserve existing parsing behavior for malformed non-empty strings unless a separately reviewed exception contract is adopted.
- Optional conversion (`toOptionalInt()`, etc.): return empty for a Variable that represents no value/empty content; return a present zero for an actual numeric zero; invalid non-empty text remains a conversion error.
- Explicit fallback (`toIntOrDefault(defaultValue)`, etc.): use the supplied fallback only for absence/empty content, not for malformed non-empty data. This prevents a default from masking corrupt input.

The current `EmptyVariable`/factory normalization means null and empty String have already been collapsed on the common path. An Optional conversion can therefore represent “no numeric value” for that combined state, but it cannot recover whether the original Java input was null or `""`. If preserving that distinction is a requirement, it needs a separate null/missing representation or a deliberate change to `CommonUtil.createVariable`; that is a broader compatibility decision and is not required merely to stop returning zero.

**DESIGN ASSESSMENT —** The classic Null Object approach is not the right fix if the sentinel makes numeric conversion return zero. That would make absence look like a valid measurement and reproduce the original defect under a different class name. `EmptyVariable` is already a sentinel, but its numeric conversion currently fails rather than manufacturing zero.

- Keep an undefined binding as absence from the context (`getVar(name) == null`); do not materialize it as a numeric zero or a `Variable` sentinel by default.
- Keep a present numeric zero distinct from no numeric value.
- Treat empty text, empty collections and empty binary payloads as separate content-shape cases; `isEmpty()` is currently too broad and inconsistent to define numeric absence by itself.
- Add a dedicated `NullVariable` only if the public contract must preserve explicit Java null separately from empty text after factory/context normalization. If introduced, it must not pretend to be numeric zero.
- `OptionalInt`/`OptionalLong`/`OptionalDouble` can express the result of a conversion, but they are not a replacement for the underlying value-state model. Invalid non-empty data should remain a conversion error, not become `Optional.empty()` or a fallback.

**COMPATIBILITY GATE —** Before changing existing methods, obtain a complete in-repository call-site inventory (including optional modules, tests, scripts, IDE and public API examples), identify downstream embedding use where possible, and characterize direct constructors versus factory/list normalization. Existing callers may rely on the current zero fallback even though no direct production call was found in the bounded sample. Do not silently change Boolean-to-number conversion, Number narrowing/truncation, overflow, whitespace parsing or exception behavior as part of this null-semantics correction.

**UNKNOWN —** Whether downstream applications or user scripts intentionally depend on `NodeVariable(null).toInt()/toLong()/toDouble()` returning zero is not established by repository inspection. Also unknown: whether preserving null versus empty String is a supported public semantic requirement.

### 3.2.1 Independent numeric coercion paths

The three `Variable` conversion methods are not the only numeric-conversion contract in Core. At least two independent conversion paths were confirmed in the inspected source.

**FACT —** `SetVarPlugin.evaluateArithmeticExpression()` implements its own arithmetic parsing and fallback policy:

- For the supported `${left + right}` and `${left - right}` cases, operands are parsed with `Double.parseDouble()`; this does not call `Variable.toDouble()`.
- A missing left-hand variable (`context.getVar(leftVar) == null`) remains initialized to numeric zero.
- A present but non-numeric left-hand value is caught as `NumberFormatException` and replaced with zero. A non-numeric right-hand operand is treated the same way.
- The arithmetic result is always converted using `(int) result` and then returned as a String-backed `NodeVariable`. Fractional results are truncated; the current branch does not preserve decimal results.
- An outer catch converts any other evaluation failure into `EmptyVariable.INSTANCE`.
- `SetVarPlugin.doExecute()` routes a body containing any of `+`, `-`, `*` or `/` to this evaluator. The evaluator itself only handles the simple two-operand addition and subtraction cases. Multiplication and division therefore fall through to `BaseTemplater.evaluateToVariable()` when the script engine is available; this is separately asserted by `SetVarResidualOpsBranchContractTest`.
- The arithmetic shortcut is only applied to body content. A non-empty `value` attribute takes precedence and is evaluated through the templater path instead.

**FACT —** This behavior is explicitly locked by `SetVarArithmeticNonNumericBranchContractTest`: non-numeric left and right operands produce arithmetic as if the operand were zero, and subtraction with a missing left variable starts from zero. These are existing tests, not merely undocumented implementation details.

**IMPLICATION —** Changing `Variable.toInt()/toDouble()` will not correct or alter this SetVar behavior. The arithmetic evaluator needs its own separately reviewed contract. It must distinguish (a) missing operand, (b) explicitly empty operand, (c) malformed non-empty operand and (d) valid numeric operand. Whether any of these should default to zero is a product/DSL compatibility decision, not a side effect of the Variable conversion API.

**FACT —** `ScraperContext` distinguishes an undefined variable from a defined empty variable at the context lookup level:

- `getVar(name)` returns Java `null` when no binding exists.
- `setLocalVar(name, null)` stores `EmptyVariable.INSTANCE` through `ObjectUtils.defaultIfNull`.
- The Object-valued setter routes through `CommonUtil.createVariable`, which also collapses Java null and empty String into EmptyVariable.
- Therefore `getVar(name) == null` means undefined binding, whereas a non-null `EmptyVariable.INSTANCE` means a defined binding with the current combined empty/null representation.

This distinction is important for migration: a missing binding is not the same thing as a Variable whose payload is null or empty.

**FACT —** `JSRScriptEngineAdapter.copyVariables()` introduces another independent coercion at the scripting boundary. It unwraps every Variable using `getWrappedObject()`; when the wrapped value is a String, it trims it and attempts Integer parsing, then Double parsing, leaving non-numeric and empty strings as strings. Thus a string such as `"5"` can reach the script engine as a numeric object, while malformed/empty strings remain text. This behavior is separate from both `Variable.toDouble()` and SetVar's arithmetic evaluator.

**FACT —** `ScriptPlugin` public examples also perform conversion inside user scripts with `parseInt(...toString())` and `parseFloat(...toString())`. This is script-language behavior and is not controlled by the Java Variable conversion API.

**REQUIRED FOLLOW-UP —** Expand the migration audit beyond the three Variable methods. At minimum, characterize SetVar arithmetic, JSR-223 binding conversion, template evaluation, and script examples independently. Do not centralize them behind one helper until their intentionally different semantics have been identified and approved.

### 3.2.2 Emptiness and truthiness are consumer-specific

The null/empty audit also needs to distinguish the predicates used by individual consumers. `Variable.isEmpty()` is not a universal definition of “no value”, and `Variable.toBoolean()` is not the truthiness implementation used by every DSL construct.

**FACT —** `Variable.toBoolean()` and `IfPlugin.isTruthyConditionValue()` implement different string rules:

- `Variable.toBoolean()` returns the wrapped Boolean directly; for every other non-null wrapped value it stringifies, trims, and returns true only for case-insensitive exact values `true`, `yes` and `1`. Thus `Integer(1)` is true while `Double(1.0)` is false, and other strings (including `"2"` and `"random"`) are false.
- `IfPlugin.isTruthyConditionValue()` handles Boolean and Number wrapped objects directly. Numeric zero and NaN are false; non-zero numbers are true. For other values it trims the string, treats empty, `false` and `null` as false, parses numeric strings so zero is false, and treats every other non-empty string as true.
- Therefore `new NodeVariable("random").toBoolean()` is false while the same non-empty string is truthy under the If plugin's value rule. A refactor that replaces the If-specific predicate with `Variable.toBoolean()` would change DSL behavior.

**FACT —** `NodeVariable.isEmpty()` is shape-dependent rather than a general emptiness test:

- It recognizes null, empty CharSequence, exhausted Iterator/Iterable and empty `XmlNodeWrapper`.
- It does not recognize zero-length arrays. In particular, direct `new NodeVariable(new byte[0])` and `new NodeVariable(new Object[0])` report non-empty. The latter can nevertheless produce an empty iterator/list through `toIterator()` / `toList()`.
- The common factory path differs: `CommonUtil.createVariable(Object[])` converts the array to a `ListVariable`, whose constructor filters members using `isEmpty()`. Direct construction and factory construction therefore need separate characterization.
- Whitespace-only text is non-empty because `NodeVariable.isEmpty()` checks length, not trimmed content.

**FACT —** Consumers apply different policies to the same Variable state:

- `LoopPlugin` skips values using `isEmpty()` only when `empty="true"`; the default is false. Its tests explicitly preserve an EmptyVariable source item under `empty="false"`, skip it under `empty="true"`, and preserve whitespace-only typed values even when skipping is enabled.
- `DefaultPluginBodyExecutor.executeChildrenInternal()` first omits null and explicit `EmptyVariable` child results, then constructs a `ListVariable`. That constructor additionally removes every member whose `isEmpty()` is true. Thus a child returning `NodeVariable("")` is filtered during aggregation, while an empty byte-array NodeVariable is not filtered by this predicate.
- `CallPlugin.normalizeReturnValue()` applies yet another rule: for a `ListVariable`, it returns the last non-empty item and returns EmptyVariable if none exists. This is not a generic list-to-scalar conversion.
- `BaseTemplater.evaluateToVariable()` omits null script-engine results from its intermediate result list and then passes the joined result through `CommonUtil.createVariable()`. An all-null/empty interpolation can therefore collapse to EmptyVariable. `evaluateToString()` further maps any result for which `isEmpty()` is true to Java null.
- `GetVarPlugin` treats `context.getVar(name) == null` as an undefined binding and throws, but returns a defined `EmptyVariable` unchanged. The public `<get>` boundary therefore distinguishes missing from defined-empty values.
- `ReturnPlugin` stores the explicit return value and sets `returnPluginCalled=true` even when the value is empty. `CallPlugin` checks that flag and deliberately returns the explicit value even when it is EmptyVariable; this distinguishes “no `<return>`” from “explicit `<return>` with empty value”.
- `ScriptPlugin` passes the script-engine result through `CommonUtil.createVariable()`, so Java null results normalize to EmptyVariable on this path. Its documented return mapping also groups script-language null/undefined outputs as EmptyVariable.

**TEST COVERAGE / GAP —** Existing tests establish parts of these contracts: `VariableTest.testToBooleanWithStringRandom`, the If plugin's false/zero/empty cases, the Loop plugin's EmptyVariable and whitespace cases, and the ListVariable constructor/addition asymmetry. The inspected tests do not establish one cross-consumer truthiness contract, nor do they characterize direct empty Object[] construction versus factory construction, empty byte[] aggregation, or null script results across both templater APIs.

**IMPLICATION —** Do not globally redefine `isEmpty()`, route every consumer through `toBoolean()`, or normalize all empty results at the body-executor boundary as part of numeric-conversion cleanup. First specify separate contracts for payload emptiness, missing binding, DSL truthiness, loop filtering, child-result aggregation and function return normalization.

### 3.2.3 Compatibility matrix: value states at API and DSL boundaries

This matrix records current behavior separately from the proposed direction. It is a characterization and migration input, **not an approved breaking-change contract**.

| Input / state | Current representation or boundary behavior | Current numeric conversion / consumer behavior | Compatibility implication |
|---|---|---|---|
| Undefined context binding | `ScraperContext.getVar(name)` returns Java `null`; `containsVar(name)` is implemented as `getVar(name) != null`. | There is no Variable instance to convert. `<get var="..."/>` throws when lookup returns null. | Preserve “not bound” as distinct from a defined empty value. A future Optional API must not silently turn undefined into numeric zero. |
| Java null passed to context setter | `setLocalVar(String, Variable)` stores `EmptyVariable.INSTANCE`; the Object overload also routes through `CommonUtil.createVariable(null)`. | `EmptyVariable` wraps an empty String, so `toInt()/toLong()/toDouble()` attempt to parse empty text and throw `NumberFormatException`. | Context setter null is not represented as `NodeVariable(null)`; overload selection and direct construction matter. |
| Direct `new NodeVariable(null)` | Retains a null wrapped object; `isEmpty()` is true; `toString()` is empty. Passing this Variable overload to `setLocalVar` retains that NodeVariable instance. | The three numeric methods return zero because their null-wrapped-object branch is explicit. | This is the specific silent-null-to-zero path. Replacing it with strict conversion is behavior-breaking for direct Java API consumers. |
| Empty String via `CommonUtil.createVariable("")` | Normalized to `EmptyVariable.INSTANCE`. | Numeric conversions parse empty text and throw. | Factory normalization makes the same conceptual “empty” value behave differently from direct `NodeVariable(null)` for numeric conversion. |
| Direct `new NodeVariable("")` | Retains a String-backed NodeVariable; `isEmpty()` is true. | Numeric conversions parse empty text and throw. | Do not assume every empty NodeVariable was normalized by the factory. |
| Whitespace-only String | Factory retains it as NodeVariable because factory checks zero-length String, not trimmed emptiness. `NodeVariable.isEmpty()` is false. | Numeric conversion trims first; whitespace-only input then throws. Loop `empty="true"` does not filter it. | Whitespace is present text at the value boundary, even if a particular parser rejects it. |
| Empty `byte[]` | Factory retains it as NodeVariable; `NodeVariable.isEmpty()` is false. | `toString()` is empty, while binary payload length is zero. | String emptiness cannot stand in for binary payload emptiness. |
| Empty `Object[]` / empty Collection | Factory creates an empty `ListVariable`; its `isEmpty()` is true. Direct `NodeVariable(new Object[0])` is not empty by `NodeVariable.isEmpty()`. | List conversion/normalization depends on whether the value is represented as ListVariable or NodeVariable-wrapped collection. | Factory and direct-constructor paths are observably different. |
| Numeric zero | A present Number or numeric text remains a value. | Numeric conversion returns zero; Optional conversion must therefore distinguish present zero from absent/empty. | Never use numeric zero as the representation of absence. |
| Boolean false / true | Boolean remains a typed wrapped object unless normalized elsewhere. | Numeric conversion maps false/true to 0/1. | Preserve Boolean coercion if introducing strict/optional APIs; do not silently replace it with text-only parsing. |
| Malformed non-empty numeric text | Remains a non-empty NodeVariable. | Numeric conversion throws the relevant JDK parse exception. | A defaulting API should not swallow malformed input unless explicitly named/documented as a lenient parse. |

**FACT —** The two `ScraperContext.setLocalVar` overloads are not equivalent for every input. The Object overload always invokes `CommonUtil.createVariable`; the Variable overload stores the supplied Variable, except Java null is replaced by EmptyVariable. Thus direct `NodeVariable(null)` survives the Variable overload, while passing Java null does not create that representation.

**FACT —** The current `GetVarPluginTest.testGetVarPluginNonExistentVariable` does not execute `GetVarPlugin`. It calls `context.getVar("nonexistent")` directly and accepts either null or empty. The production plugin's `doExecute()` explicitly throws when the lookup is null. The existing test therefore does not characterize the public `<get>` behavior and should not be used as evidence that the plugin returns empty for missing variables.

**TEST GAP —** The inspected `ReturnPluginTest` verifies that the plugin returns EmptyVariable, but does not assert that it stores an explicit empty return value and sets the return flag. The inspected `CallPluginTest` is mostly metadata/configuration and smoke coverage; it does not prove the end-to-end distinction between no `<return>`, `<return/>`, and a non-empty explicit return. These are compatibility-critical because CallPlugin uses the flag to distinguish explicit empty return from absence of an explicit return.

**PROPOSED DIRECTION — NOT YET APPROVED:**

1. Keep binding presence separate from Variable payload state. Do not introduce a numeric sentinel for missing.
2. Keep the existing conversion methods' behavior during a compatibility phase, or introduce clearly named strict methods rather than silently changing `toInt()/toLong()/toDouble()`.
3. If Optional conversions are added, empty/missing conversion should be represented as absence, while an actual numeric zero remains present zero. Malformed non-empty values should still report conversion failure.
4. Add explicit defaulting methods only when their contract says whether the default applies to absent/empty values, malformed values, or both. Prefer separate method names for strict parse versus lenient fallback.
5. Do not introduce `NullVariable` solely to fix numeric conversion. It is justified only if public contracts must preserve distinctions currently collapsed by `CommonUtil.createVariable()` or context setters (for example, Java null versus empty String). That decision needs a separate compatibility analysis.

**REMAINING UNKNOWN —** The repository tests characterize several implementation details but do not establish the downstream Java/API and external XML consumer population relying on zero-for-null, factory normalization, missing-`<get>` failure, or explicit empty function returns. No downstream compatibility claim can be made from this repository-only evidence.

### 3.2.4 Explicit function return: value selection versus control-flow termination

A separate control-flow discrepancy surfaced while tracing the explicit-empty return contract. It must not be silently folded into a Variable conversion change, but it affects the meaning of the return flag documented above.

**FACT —** `ReturnPlugin` documentation says that `<return>` “immediately exits” the function. Its inspected implementation, however, executes its child content, stores the result in `functionResult`, sets `returnPluginCalled=true` (and a function-specific flag when found), then returns `EmptyVariable.INSTANCE`. It does not throw `ScraperExecutionExitException` or another visible control-flow signal.

**FACT —** `DefaultPluginBodyExecutor.executeChildrenInternal()` iterates through all child configurations. It stops sibling execution when a child throws `ScraperExecutionExitException` or `InterruptedException`; otherwise it records a non-empty result and proceeds to the next child. The inspected method does not check `returnPluginCalled` or `returnCalled...` after executing a child. `executeChildPlugin()` simply calls `childPlugin.execute(context)` and returns the result.

**FACT —** `CallPlugin` checks the explicit-return flag only after `executor.executeBody(functionConfig, context)` has completed. The flag selects the stored explicit return value (including EmptyVariable), but this check occurs after the body executor has finished its sibling loop.

**INFERENCE —** In this execution path, the flag appears to select which value the function returns but does not itself terminate execution of later sibling plugins. This conflicts with the documented “immediately exits” behavior unless another outer execution path intercepts the flag; no such interception was found in the inspected `DefaultPluginBodyExecutor` path.

**TEST GAP —** `ReturnPluginTest` only asserts that executing the plugin returns EmptyVariable. The repository also contains `FunctionCallIntegrationTest`, which executes real XML function definitions/calls and covers simple explicit return, no explicit return, repeated calls, return normalization and explicit-empty return precedence. However, those integration cases do not assert that a sibling after `<return>` is skipped, nor do they cover a nested function call after an enclosing function has executed `<return>`. The separate `CallPluginTest` is primarily unit-level. Add characterization tests for sibling termination and nested-call flag isolation before deciding whether implementation or documentation is authoritative.

**SCOPE —** Track this as a separate function-control-flow issue. Do not solve it by changing `Variable.isEmpty()`, by globally changing body aggregation, or by changing numeric conversion semantics.

### 3.2.5 Return-flag naming and dynamic-scope leakage

A deeper trace of the flags used by `CallPlugin` and `ReturnPlugin` found a second, independent defect candidate: the function-specific flag protocol is inconsistent, and the generic flag is inherited through nested scopes.

**FACT —** `ScraperContext` stores variables in one shared `centralReferenceTable` mapping names to stacks. `executeWithinNewContext()` pushes a `ScopeFrame`; it does not create a child context or copy only selected variables. `getVar(name)` reads the current top of the shared variable stack, so parent-scope values remain visible in a nested scope unless shadowed. On scope exit, variables created in the nested frame are removed and overridden parent values are restored.

**FACT —** Before executing a function body, `CallPlugin` creates `returnCalled{CapitalizedFunctionName}` with value `false` and clears `functionResult` in the nested scope. It does not initialize or shadow the generic `returnPluginCalled` flag. After the body finishes, it reads both the function-specific flag and the generic flag; either being `true` is sufficient to select `functionResult` as the explicit return.

**FACT —** `ReturnPlugin` sets the generic `returnPluginCalled` flag to `true`. Its best-effort function-specific lookup searches for a context variable whose name starts with `__returnCalled_`, which does not match the `returnCalled{FunctionName}` name created by `CallPlugin`. In addition, `VariableName` requires names to match `^[a-zA-Z][a-zA-Z0-9_]*$`; a leading-underscore key cannot be created through the normal `ScraperContext.setLocalVar()` API. The function-specific flag path is therefore inconsistent with both the caller's naming convention and the context's variable-name contract.

**INFERENCE —** The generic flag is the effective return signal. Because it is not reset in the nested function scope, a nested function can inherit a parent scope's existing `returnPluginCalled=true`. If that nested function does not execute its own `<return>`, `CallPlugin` can still interpret the inherited flag as its own explicit return and select its locally cleared `functionResult` (EmptyVariable), discarding the nested function's actual body result. One reachable scenario under the current sibling-execution behavior is a function that executes `<return>` and then executes a later sibling `<call>` before its body completes.

**TEST GAP —** Add a characterization case in which an outer function executes `<return>` and a later sibling invokes an inner function that has no explicit return. Assert separately whether the later sibling should execute (control-flow contract) and whether the inner function returns its own last expression (flag isolation). Also test a nested function that does execute its own `<return>`, plus recursive/re-entrant calls. These cases should distinguish body termination from return-value selection.

**SCOPE —** This is a function-scope/control-flow issue, not a numeric-conversion or general Variable emptiness issue. Do not repair it by changing `Variable.isEmpty()` or global body aggregation. The likely contract is per-invocation return state, but the exact mechanism should be selected only after the characterization tests establish legacy expectations.
### 3.3 Optional integration modules

The bounded source scan also inspected the in-tree Database, FTP, Mail, WebBrowser and ZIP module implementations. These are optional modules, not evidence that the same capabilities belong in Core.

| Module / source path | Observed payload or lifecycle behavior | Limitation |
|---|---|---|
| FTP — `webharvest-ftp/.../FtpPlugin.java`, `handleFtpGet()` | Retrieves into `ByteArrayOutputStream`, converts to `byte[]`, returns `NodeVariable(byte[])`. | Whole remote file is buffered in memory. |
| FTP — `FtpGetPlugin.java` | Retrieves into `ByteArrayOutputStream`, then converts downloaded bytes to String using context charset. | The dedicated GET plugin does not preserve arbitrary binary content. |
| FTP — `FtpPutPlugin.java` and `FtpPlugin.handleFtpPut()` | Converts body Variable to String, then to bytes and uploads from `ByteArrayInputStream`. | Binary body values can be corrupted by string conversion; upload is fully materialized. |
| Mail — `MailPlugin.createDataSourceOfVariable()` | Calls `variable.toBinary(charset)` and creates `ByteArrayDataSource`. | Attachment payload is fully materialized as byte array. |
| ZIP — `ZipPlugin.doExecute()` | Writes the entire archive into `ByteArrayOutputStream` and returns `NodeVariable(byte[])`. | Archive size is memory-bounded only by process memory; no artifact sink/stream output. |
| ZIP — `ZipEntryPlugin.doExecute()` | Writes `bodyResult.toBinary(charset)` into the active ZipOutputStream stored in a context Variable. | Entry body must already be represented by a fully materialized Variable/byte array. |

**INFERENCE —** Optional modules currently repeat the same in-memory payload pattern as Core. A future neutral payload contract should allow these modules to opt into streaming/artifact references without moving FTP, mail, ZIP or browser implementations into Core.

## 4. Core plugin source sweep: Variable boundaries

A bounded source sweep inspected all **47 Java source files** directly under `webharvest-core/src/main/java/org/webharvest/plugin/core/` at the inspected revision for Variable conversion, collection and wrapped-object operations. This is a package-level scan, not an exhaustive repository-wide or downstream-consumer inventory.

The sweep confirms that Variable is the common execution return/input type across control-flow and transformation plugins, but meaningful conversion/shape changes cluster at several boundaries:

| Source path / method | Observed behavior | Why it matters |
|---|---|---|
| `plugin/core/LoopInputSourceResolver.java` — `unwrapSingleChildResult()`, `flatten()` | Removes one body-executor aggregation layer and preserves direct ListVariable members and their Variable types. | This is an example of a careful compatibility boundary; preserve this behavior in migration characterization. |
| `plugin/core/LoopPlugin.java` — `doExecute()` | Runs each source item in a nested scope, but appends each non-empty body result via `bodyResult.toString()` to one StringBuilder and returns a NodeVariable string. | A loop can accept typed list members but its aggregate output becomes flattened text. It is not a typed map/collect operation. |
| `plugin/core/CallPlugin.java` — `normalizeReturnValue()` | For ListVariable, selects the last non-empty item; for NodeVariable wrapping a Java List, returns one item or joins multiple items with spaces; also normalizes bracketed string forms. | Function-call return normalization is intentionally legacy-specific and can discard list structure. |
| `plugin/core/XPathPlugin.java` — CSS evaluation path | Collects matching element text values into ListVariable of NodeVariable strings. | XPath/CSS result is a list of extracted text values, not a typed XML-node/result-set contract. |
| `plugin/core/TokenizePlugin.java` and `RegexpPlugin.java` | Build ListVariable values from token/match strings. | These are list-producing text transformations; element boundaries exist only in the Variable collection. |
| `plugin/core/WhilePlugin.java` | Aggregates iteration body results into a ListVariable on its result path. | Differs from LoopPlugin's string-concatenating output; superficially similar iteration plugins do not share one output shape. |
| `plugin/core/XQueryPlugin.java` | Converts a Variable to String XML, reparses it with Saxon to bind node() parameters; falls back to string on conversion failure. | Structured values cross a stringify/reparse boundary and can be copied/materialized. |
| `plugin/core/JsonToXmlPlugin.java` and `XmlToJsonPlugin.java` | Convert body values to String before parsing and serialize the transformed whole document into NodeVariable. | Current JSON/XML conversion is whole-document materialization. |
| `plugin/core/IfPlugin.java` | Inspects `getWrappedObject()` to implement truthiness, in addition to string/numeric handling. | Consumers can depend on the concrete wrapped Java type, not merely Variable's public conversion methods. |

Other Core plugins use Variable primarily as an execution/return carrier, scope value or body aggregation type (for example Def, GetVar, SetVar, Var, Config, Function, Return, Catch, Try, Script, Template, Text and List). The 47-file sweep does not prove that no additional conversion paths exist in other packages, scripting adapters, optional modules, IDE code or external applications.

**Architectural implication:** Preserve legacy behavior through explicit adapters and characterization tests. Do not silently normalize Loop and While to one behavior, change CallPlugin's return normalization, or assume every ListVariable is a homogeneous typed collection.

## 4.1 Bounded repository source inventory

A follow-up tree-driven source pass used the recursive Git tree for the inspected `main` SHA and fetched source files by exact repository path, rather than relying on GitHub Code Search.

- The recursive tree contains 2,123 entries and 768 Java files (663 under `webharvest-core`; the remaining modules are CLI, Database, FTP, IDE, Mail, WebBrowser and ZIP).
- Core has 308 production Java files and 355 test Java files in this snapshot. The 65-file targeted production pass therefore leaves 243 Core production files outside this boundary-focused inspection; do not describe the Core-wide scan as complete.
- The targeted Core pass fetched 65 production Java files covering all Core plugin classes plus the Variable, context, scripting and templating boundaries. The previously documented 47-file direct Core plugin-package sweep remains a narrower package-level count.
- The non-Core pass fetched all 49 production Java files under the optional modules/CLI/IDE source roots, plus the documentation generator source. It confirmed additional Variable boundaries in the CLI argument-to-context path, IDE variable extraction/stringification, Database connection handles/results, FTP text/binary handling, Mail attachments, WebBrowser results and ZIP context-held output streams.
- This is a source-file inventory for the selected boundaries, not a full symbol-level audit of every Core package, test source, generated code, public API consumer or downstream embedding application. GitHub Code Search returned zero/incomplete results for known symbols; those results are not evidence that no other call sites exist.


**Additional boundary observations from the same source pass:**

- **CLI input:** `WebHarvestCLI` injects parsed command-line variables through `context.setLocalVar(name, String)`. That overload calls `CommonUtil.createVariable()`, so command-line empty strings enter the context as `EmptyVariable`, not as a distinct empty-text value.
- **IDE output:** `ExecutionManager.extractVariables()` first iterates the context and stringifies each `Variable` with `toString()`; it filters `EmptyVariable` only when the name is a function definition. Its secondary probe path skips blank strings. The two extraction paths therefore do not apply one uniform empty-value policy. The iterator path also truncates displayed values to 5,000 characters.
- **JSR-223 boundary:** `JSRScriptEngineAdapter.copyVariables()` copies `getWrappedObject()` and separately attempts trimmed String-to-Integer/Double conversion. Empty Strings remain Strings, null wrapped objects remain null, and malformed non-empty strings remain Strings. This is independent of `Variable.toDouble()` and the SetVar arithmetic fallback.
- **Template boundary:** `BaseTemplater.evaluateToString()` maps any result for which `isEmpty()` is true to Java `null`; the Variable-returning path uses `CommonUtil.createVariable()`. A template string boundary can therefore collapse more states than a caller expecting a literal empty string may anticipate.
- **Optional integrations:** `ConnectionPlugin` stores a JDBC pool/credentials inside a `NodeVariable` subclass; Database returns serialized XML/text; FTP GET and PUT take different text/binary paths; Mail converts attachments through `toBinary()`; WebBrowser returns strings; ZIP stores an active `ZipOutputStream` inside a context `NodeVariable`. These are concrete in-repository consumers of the broad Variable carrier, not reasons to move those integrations into Core.

## 5. Design constraints derived from evidence

The next data/payload API design should be evaluated against these constraints:

1. **Separate value metadata from payload access.** Contract identity/version, media type, schema, units, provenance and quality cannot be inferred from `Variable`.
2. **Represent large payloads without mandatory materialization.** Support bounded streaming or managed artifact references; byte arrays remain a valid small-payload adapter, not the universal transport.
3. **Make ownership explicit.** Define who opens, consumes, closes, retains, copies and deletes a payload. A raw mutable byte[] reference is not an ownership model.
4. **Specify bounded flow control.** Demand/backpressure, maximum buffering, cancellation, timeout, failure and terminal completion must be explicit. A generic Iterator is not sufficient.
5. **Keep source and derived artifacts distinct.** Transformations should be able to emit a derived result with source reference, transformation identity/version and relevant timestamps without replacing the original payload.
6. **Keep runtime notifications distinct from domain events.** Session/plugin lifecycle events may carry references and execution metadata, but domain event schemas and durable event persistence are separate concerns.
7. **Preserve resource cleanup semantics.** Database pool cleanup currently depends on session terminal notifications; stream/artifact resources need deterministic cleanup on completion, failure, cancellation and runtime shutdown.
8. **Do not force media frameworks into Core.** Core should own neutral payload/stream lifecycle contracts only where proven necessary; codecs, capture protocols, AI inference and vendor integrations remain provider modules.

## 5. Remaining unknowns and next inspections

- Full Variable consumer inventory across every Core package, all test sources, generated code, public APIs and downstream embedding applications remains incomplete. The bounded production-source inventory above covers the targeted Core boundaries and all non-Core module production Java files; GitHub Code Search (connector and REST) returned zero/incomplete results for known symbols, which is not evidence of no additional call sites.
- The full subscriber inventory for both EventBus families, including all `@Subscribe` methods and runtime registration sites, remains to be completed.
- The exact configured Guava EventBus exception handler, subscriber failure policy and behavior during shutdown require a focused audit.
- HTTP response size limits, timeout behavior, maximum in-memory response size and service lifetime require a separate resource-budget audit.
- File plugin binary read behavior has no confirmed compatibility contract from this pass; existing tests and documented XML examples must be checked before changing it.
- Database result materialization limits, BLOB handling and transaction/cursor ownership need separate tests/audit.
- Public/external consumers and persisted artifacts are not covered by repository source inspection.
- Function return sibling termination and per-invocation flag isolation require the characterization tests described in sections 3.2.4–3.2.5; no test execution was performed as part of this source-only pass.

## 6. Review conclusion

This evidence is sufficient to reject two unsafe assumptions: that the existing session event stream is already a durable event-sourcing subsystem, and that current HTTP/file/list payload handling is already suitable for large or continuous data streams.

It is not sufficient to implement a replacement API. The next step is a bounded contract proposal with explicit ownership, streaming, cancellation, bounded buffering, artifact references and legacy Variable adapters, followed by characterization tests. No production code change is authorized by this audit.
