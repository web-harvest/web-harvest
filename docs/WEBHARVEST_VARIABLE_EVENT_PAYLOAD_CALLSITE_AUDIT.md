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

**REPOSITORY INVENTORY LIMITATION —** The recursive Git tree for the audited `main` snapshot contains 2,123 entries, including 768 Java source/test files across the CLI, Core, Database, FTP, IDE, Mail, WebBrowser, ZIP and docs roots. This establishes the scope of the repository, not a complete symbol scan. GitHub code-search requests for the conversion and factory symbols returned empty/incomplete results. The production-file inspection recorded here remains a bounded source inspection; do not report the call-site inventory as complete until a repository-wide content scan has been run against all relevant source, test, resource and documentation files.

**FACT —** The inspected production files `CommonUtil`, `JSRScriptEngineAdapter`, `ScriptPlugin`, `BaseTemplater`, `IfPlugin`, `CallPlugin`, `WhilePlugin`, `SleepPlugin`, `HttpPlugin`, `FilePlugin`, `LoopPlugin`, `FunctionPlugin`, `CasePlugin`, `XPathPlugin` and `ListPlugin` did not show direct calls to `Variable.toInt()`, `toLong()` or `toDouble()`. This is a representative inspection, not an exhaustive call-site proof. GitHub code-search requests for these symbols returned empty/incomplete results, so they cannot establish absence of other call sites.

**INFERENCE —** The first correction should be to define a consistent distinction between *no numeric value* and the numeric value zero. It should not be implemented by changing only the three null branches while leaving the factory-normalized EmptyVariable path undocumented.

**DESIGN CANDIDATE —** A coherent API can offer three explicit operations per numeric type:

- Strict conversion (`toInt()`, `toLong()`, `toDouble()`): return a number only when conversion is valid; absence/empty input must fail rather than silently become zero. Preserve existing parsing behavior for malformed non-empty strings unless a separately reviewed exception contract is adopted.
- Optional conversion (`toOptionalInt()`, etc.): return empty for a Variable that represents no value/empty content; return a present zero for an actual numeric zero; invalid non-empty text remains a conversion error.
- Explicit fallback (`toIntOrDefault(defaultValue)`, etc.): use the supplied fallback only for absence/empty content, not for malformed non-empty data. This prevents a default from masking corrupt input.

The current `EmptyVariable`/factory normalization means null and empty String have already been collapsed on the common path. An Optional conversion can therefore represent “no numeric value” for that combined state, but it cannot recover whether the original Java input was null or `""`. If preserving that distinction is a requirement, it needs a separate null/missing representation or a deliberate change to `CommonUtil.createVariable`; that is a broader compatibility decision and is not required merely to stop returning zero.

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

- Full Variable consumer inventory across all Core plugins, optional modules, scripting engines, IDE, public APIs and downstream embedding applications remains incomplete. GitHub code-search connector queries returned no results for known symbols; those empty results are not evidence of no call sites.
- The full subscriber inventory for both EventBus families, including all `@Subscribe` methods and runtime registration sites, remains to be completed.
- The exact configured Guava EventBus exception handler, subscriber failure policy and behavior during shutdown require a focused audit.
- HTTP response size limits, timeout behavior, maximum in-memory response size and service lifetime require a separate resource-budget audit.
- File plugin binary read behavior has no confirmed compatibility contract from this pass; existing tests and documented XML examples must be checked before changing it.
- Database result materialization limits, BLOB handling and transaction/cursor ownership need separate tests/audit.
- Public/external consumers and persisted artifacts are not covered by repository source inspection.

## 6. Review conclusion

This evidence is sufficient to reject two unsafe assumptions: that the existing session event stream is already a durable event-sourcing subsystem, and that current HTTP/file/list payload handling is already suitable for large or continuous data streams.

It is not sufficient to implement a replacement API. The next step is a bounded contract proposal with explicit ownership, streaming, cancellation, bounded buffering, artifact references and legacy Variable adapters, followed by characterization tests. No production code change is authorized by this audit.
