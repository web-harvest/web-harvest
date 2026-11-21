# Function Execution and System Variables

## Overview

This document provides a comprehensive explanation of how function execution works in WebHarvest, including the mechanism for defining functions, calling them, passing parameters, and handling return values. It also documents all system-defined variables that are automatically created by the framework.

## Table of Contents

1. [Function Definition (`<function>`)](#function-definition)
2. [Function Call (`<call>`)](#function-call)
3. [Parameter Passing (`<call-param>`)](#parameter-passing)
4. [Return Values (`<return>`)](#return-values)
5. [System Variables](#system-variables)
6. [Execution Flow](#execution-flow)
7. [Examples](#examples)

---

## Function Definition (`<function>`)

### Purpose

The `<function>` element defines a reusable function that can be called from anywhere in the configuration. Functions are stored in the execution context and can be invoked using `<call>`.

### Syntax

```xml
<function name="functionName">
  <!-- Function body: any plugins -->
  <def var="param1"><call-param name="param1"/></def>
  <!-- ... more logic ... -->
  <return>${result}</return>  <!-- Optional: explicit return -->
</function>
```

### Implementation Details

**Class**: `org.webharvest.plugin.core.FunctionPlugin`

**Key Behavior**:
- When a `<function>` is executed, it **does NOT execute its body immediately**
- Instead, it stores its entire `PluginConfiguration` (representing the function's body and attributes) in the `DynamicScopeContext` under the function's name
- The function definition itself returns `EmptyVariable.INSTANCE`

**Code Flow**:
```java
@Override
protected Variable doExecute(DynamicScopeContext context) throws InterruptedException {
    String functionName = getConfiguration().getProperty("name", "");
    // Store function definition marker in context (EmptyVariable as placeholder)
    // The actual function configuration is stored in PluginConfigurationTree
    context.setLocalVar(functionName, EmptyVariable.INSTANCE);
    return EmptyVariable.INSTANCE; // Function definition itself returns empty
}
```

**Important Notes**:
- Functions must be **defined before they are called** (lexical order in XML)
- Functions are stored in the **same scope** as regular variables, but as `EmptyVariable` placeholders
- The actual function configuration is stored in `PluginConfigurationTree`, not in the context
- Multiple functions can be defined in the same configuration
- Functions can call other functions (including themselves recursively)
- **Function definitions are automatically filtered out** from the Variables panel in the IDE (they don't appear as user variables)

---

## Function Call (`<call>`)

### Purpose

The `<call>` element invokes a previously defined function, passing parameters and receiving a return value.

### Syntax

```xml
<call name="functionName">
  <call-param name="param1">value1</call-param>
  <call-param name="param2">value2</call-param>
</call>
```

### Implementation Details

**Class**: `org.webharvest.plugin.core.CallPlugin`

**Key Behavior**:
1. **Function Lookup**: Recursively searches the `PluginConfigurationTree` for a `<function>` element with the matching name
2. **Parameter Collection**: Executes each `<call-param>` child to get parameter values
3. **New Scope Creation**: Executes the function's body within a **new `DynamicScopeContext`** using `context.executeWithinNewContext()`
4. **Parameter Injection**: Parameters are passed to the new context using the naming convention `callParamName` (e.g., `callParamUserName`)
5. **Return Value Handling**: Checks for explicit `functionResult` (set by `<return>`) or uses the last expression's result

**Code Flow**:
```java
@Override
protected Variable doExecute(DynamicScopeContext context) throws InterruptedException {
    String functionName = getConfiguration().getProperty("name", "");
    PluginConfiguration functionConfig = findFunctionConfiguration(functionName, context);
    
    Map<String, Variable> callParams = collectCallParams(getConfiguration(), context);
    
    Variable functionResult = context.executeWithinNewContext(() -> {
        // Inject parameters into new scope
        for (Map.Entry<String, Variable> param : callParams.entrySet()) {
            String paramVarName = "callParam" + capitalize(param.getKey());
            context.setLocalVar(paramVarName, param.getValue());
        }
        
        // Clear functionResult for new scope
        context.setLocalVar("functionResult", EmptyVariable.INSTANCE);
        
        // Execute function body
        PluginBodyExecutor executor = new DefaultPluginBodyExecutor();
        Variable bodyResult = executor.executeBody(functionConfig, context);
        
        // Check for explicit return value
        Variable explicitReturn = context.getVar("functionResult");
        if (explicitReturn != null && !(explicitReturn instanceof EmptyVariable)) {
            return explicitReturn;
        }
        
        // Handle ListVariable or NodeVariable wrapping List
        // (extracts last non-empty item, converts List to string if needed)
        return normalizeReturnValue(bodyResult);
    });
    
    return functionResult;
}
```

**Important Notes**:
- Functions execute in an **isolated scope** - variables set inside a function do not affect the caller's scope
- Parameters are accessible inside the function via `<call-param name="paramName"/>`
- If a function is called but not defined, an error is thrown
- The function's return value becomes the result of the `<call>` element

---

## Parameter Passing (`<call-param>`)

### Purpose

The `<call-param>` element defines a parameter value for a function call. It can be used in two contexts:
1. **Inside `<call>`**: Defines the parameter value to pass to the function
2. **Inside `<function>`**: Retrieves the parameter value that was passed from the caller

### Syntax

**In `<call>` (defining parameter value)**:
```xml
<call name="greet">
  <call-param name="name">World</call-param>
</call>
```

**In `<function>` (retrieving parameter value)**:
```xml
<function name="greet">
  <def var="name"><call-param name="name"/></def>
  <template>Hello ${name}</template>
</function>
```

### Implementation Details

**Class**: `org.webharvest.plugin.core.CallParamPlugin`

**Key Behavior**:
- When used **inside `<call>`**: Executes its body to get the parameter value (e.g., `World` from `<call-param name="name">World</call-param>`)
- When used **inside `<function>`**: Retrieves the parameter value from the context (where `CallPlugin` set it with the `callParam` prefix)

**Code Flow**:
```java
@Override
protected Variable doExecute(DynamicScopeContext context) throws InterruptedException {
    String paramName = getConfiguration().getProperty("name", "");
    String capitalizedName = capitalize(paramName);
    String paramVarName = "callParam" + capitalizedName;
    
    Variable paramValue = context.getVar(paramVarName);
    
    if (paramValue != null) {
        // We're inside a function, retrieving a parameter value
        return paramValue;
    }
    
    // We're in a <call> element, executing body to get parameter value
    paramValue = super.getBodyTextContent(context); // CRITICAL: Use getBodyTextContent
    return paramValue != null ? paramValue : EmptyVariable.INSTANCE;
}
```

**Important Notes**:
- Parameter names are **case-sensitive**
- Parameters are passed using the naming convention `callParamName` (e.g., `callParamUserName` for parameter `userName`)
- The `getBodyTextContent()` method is used (not `executeBody()`) to extract simple text content, avoiding `ListVariable` wrapping
- Parameters can be complex values (not just strings) - any plugin result can be passed as a parameter

**Validation Rules**:
- `<call-param>` can appear in two contexts:
  1. **Directly in `<call>`**: Defines the parameter value to pass to the function
  2. **In `<def>` within `<function>`**: Retrieves the parameter value that was passed from the caller
- The validator checks if `<call-param>` in `<def>` is within a `<function>` context by traversing the DOM hierarchy
- This allows flexible parameter retrieval patterns like:
  ```xml
  <function name="greet">
    <def var="name"><call-param name="name"/></def>
    <template>Hello ${name}</template>
  </function>
  ```

---

## Return Values (`<return>`)

### Purpose

The `<return>` element explicitly sets the return value of a function and immediately exits the function. If no `<return>` is used, the function returns the result of its last expression.

### Syntax

```xml
<function name="findProduct">
  <if condition="${found}">
    <return>${product}</return>  <!-- Exit immediately -->
  </if>
  <return>NOT_FOUND</return>  <!-- Default return -->
</function>
```

### Implementation Details

**Class**: `org.webharvest.plugin.core.ReturnPlugin`

**Key Behavior**:
- Executes its body to get the return value
- Sets the `functionResult` variable in the context
- Returns `EmptyVariable.INSTANCE` (the `<return>` element itself doesn't produce output)

**Code Flow**:
```java
@Override
protected Variable doExecute(DynamicScopeContext context) throws InterruptedException {
    // Execute child elements to get return value
    Variable returnValue = executeChildElementsDirectly(context);
    
    // Set the return value in context for CallPlugin to pick up
    context.setLocalVar("functionResult", returnValue);
    
    return EmptyVariable.INSTANCE;
}
```

**Important Notes**:
- `<return>` can only be used **inside `<function>` definitions**
- If multiple `<return>` statements are executed, the **last one** wins
- If no `<return>` is used, `CallPlugin` uses the result of the last expression in the function body
- The return value can be any `Variable` type (`NodeVariable`, `ListVariable`, etc.)

---

## System Variables

System variables are automatically created by the WebHarvest framework during execution. They are distinguished from user-defined variables and are displayed separately in the IDE's Variables panel.

### Complete List

| Variable Name | Description | Source | When Created |
|--------------|-------------|--------|---------------|
| `configResult` | The final result of the entire configuration execution | `RootConfigurationExecutor` | Always (after root config execution) |
| `functionResult` | The explicit return value of the last executed function (set by `<return>`) | `ReturnPlugin` → `CallPlugin` | When a function is called and returns a value |
| `tryResult` | The result of the `<try>` block execution | `TryPlugin` | When a `<try>` block is executed |
| `listResult` | The result of the `<list>` plugin execution | `ListPlugin` | When a `<list>` plugin is executed |
| `sys` | System utilities object for common operations (e.g., `sys.fullUrl`) | `DynamicScopeContext` | Always available |
| `_index` | Current iteration index within a `<loop>` or `<while>` block | `LoopPlugin`, `WhilePlugin` | During loop/while iterations |
| `_item` | Current item within a `<loop>` block | `LoopPlugin` | During loop iterations |
| `error` | Error message in case of an exception within a `<try>` block | `TryPlugin` | When an exception occurs in `<try>` |
| `errorType` | Type of error in case of an exception within a `<try>` block | `TryPlugin` | When an exception occurs in `<try>` |
| `group0`, `group1`, ... | Regex capture groups from `<regexp>` plugin | `RegexpPlugin` | After regex execution |

### Variable Details

#### `configResult`

**Purpose**: Stores the final output of the entire configuration execution.

**Source**: `org.webharvest.runtime.RootConfigurationExecutor`

**When Created**: Always, after the root `<config>` element finishes executing.

**Code**:
```java
Variable result = bodyExecutor.executeBody(configuration, context);
context.setLocalVar("configResult", result != null ? result : EmptyVariable.INSTANCE);
```

**Usage**: This variable is typically empty if the configuration only defines functions (without executing them). It contains the result when the configuration has top-level plugins that produce output.

**Example**:
```xml
<config>
  <def var="data">
    <http url="https://example.com"/>
  </def>
  <!-- configResult now contains the HTTP response -->
</config>
```

#### `functionResult`

**Purpose**: Stores the explicit return value of a function call.

**Source**: `org.webharvest.plugin.core.ReturnPlugin` (sets it) → `org.webharvest.plugin.core.CallPlugin` (reads it)

**When Created**: When a `<return>` element is executed inside a function, or when a function returns its last expression.

**Code**:
```java
// In ReturnPlugin:
context.setLocalVar("functionResult", returnValue);

// In CallPlugin:
Variable explicitReturn = context.getVar("functionResult");
if (explicitReturn != null && !(explicitReturn instanceof EmptyVariable)) {
    return explicitReturn;
}
```

**Usage**: This variable is **local to the function's execution scope**. It is cleared at the start of each function call and set when `<return>` is executed.

**Example**:
```xml
<function name="greet">
  <return>Hello World</return>
</function>
<def var="result">
  <call name="greet"/>
</def>
<!-- result now contains "Hello World" -->
```

#### `tryResult`

**Purpose**: Stores the result of a `<try>` block execution.

**Source**: `org.webharvest.plugin.core.TryPlugin`

**When Created**: After a `<try>` block executes successfully (no exception).

**Code**:
```java
Variable result = executeChildElementsDirectly(context);
context.setLocalVar("tryResult", result);
```

**Usage**: Available in the same scope as the `<try>` block. If an exception occurs, `tryResult` is not set, but `error` and `errorType` are set instead.

**Example**:
```xml
<try>
  <def var="data"><http url="https://example.com"/></def>
</try>
<catch>
  <!-- tryResult is not set here, but error is -->
</catch>
<!-- tryResult contains the HTTP response if successful -->
```

#### `listResult`

**Purpose**: Stores the result of a `<list>` plugin execution.

**Source**: `org.webharvest.plugin.core.ListPlugin`

**When Created**: After a `<list>` plugin executes.

**Usage**: Contains the `ListVariable` created by the `<list>` plugin.

**Example**:
```xml
<def var="urls">
  <list>
    <text>https://example.com/page1</text>
    <text>https://example.com/page2</text>
  </list>
</def>
<!-- listResult contains the ListVariable with both URLs -->
```

#### `sys`

**Purpose**: Provides system utilities for common operations.

**Source**: `org.webharvest.runtime.DynamicScopeContext`

**When Created**: Always available in every execution context.

**Common Methods**:
- `sys.fullUrl(baseUrl, relativeUrl)` - Resolves relative URLs
- `sys.currentTime()` - Current timestamp
- `sys.random()` - Random number generator

**Example**:
```xml
<def var="fullUrl">
  <template>${sys.fullUrl("https://example.com", "/page")}</template>
</def>
<!-- fullUrl contains "https://example.com/page" -->
```

#### `_index`

**Purpose**: Current iteration index within a loop.

**Source**: `org.webharvest.plugin.core.LoopPlugin`, `org.webharvest.plugin.core.WhilePlugin`

**When Created**: During each iteration of a `<loop>` or `<while>` block.

**Usage**: Starts at 0 for the first iteration, increments for each iteration.

**Example**:
```xml
<loop item="item" maxloops="5">
  <template>Iteration ${_index}: ${item}</template>
</loop>
```

#### `_item`

**Purpose**: Current item within a `<loop>` block.

**Source**: `org.webharvest.plugin.core.LoopPlugin`

**When Created**: During each iteration of a `<loop>` block.

**Usage**: Contains the current item from the loop's source.

**Example**:
```xml
<loop item="product">
  <xpath expression="//product">...</xpath>
  <template>Processing: ${_item}</template>
</loop>
```

#### `error` and `errorType`

**Purpose**: Error information when an exception occurs in a `<try>` block.

**Source**: `org.webharvest.plugin.core.TryPlugin`

**When Created**: When an exception is caught in a `<try>` block.

**Usage**: Available only inside the `<catch>` block.

**Example**:
```xml
<try>
  <http url="https://invalid-url"/>
</try>
<catch>
  <template>Error: ${error} (Type: ${errorType})</template>
</catch>
```

#### `group0`, `group1`, etc.

**Purpose**: Regex capture groups from `<regexp>` plugin.

**Source**: `org.webharvest.plugin.core.RegexpPlugin`

**When Created**: After a `<regexp>` plugin executes successfully.

**Usage**: `group0` contains the full match, `group1` contains the first capture group, etc.

**Example**:
```xml
<regexp>
  <regexp-pattern>(\d{3})-(\d{3})-(\d{4})</regexp-pattern>
  <regexp-source>123-456-7890</regexp-source>
</regexp>
<!-- group0 = "123-456-7890", group1 = "123", group2 = "456", group3 = "7890" -->
```

---

## Execution Flow

### Function Definition Flow

```
1. XML Parser encounters <function name="myFunction">
2. FunctionPlugin.doExecute() is called
3. FunctionPlugin stores its PluginConfiguration in context.setLocalVar("myFunction", config)
4. FunctionPlugin returns EmptyVariable.INSTANCE
5. Function definition is complete (body not executed yet)
```

### Function Call Flow

```
1. XML Parser encounters <call name="myFunction">
2. CallPlugin.doExecute() is called
3. CallPlugin.findFunctionConfiguration() searches for function definition
4. CallPlugin.collectCallParams() executes <call-param> children to get parameter values
5. CallPlugin creates new DynamicScopeContext via context.executeWithinNewContext()
6. CallPlugin injects parameters: context.setLocalVar("callParamName", value)
7. CallPlugin clears functionResult: context.setLocalVar("functionResult", EmptyVariable.INSTANCE)
8. CallPlugin executes function body: executor.executeBody(functionConfig, context)
9. If <return> was executed, functionResult is set
10. CallPlugin checks for functionResult, or uses last expression result
11. CallPlugin normalizes return value (handles ListVariable/NodeVariable wrapping)
12. CallPlugin returns the function result
```

### Return Value Normalization

When a function returns a value, `CallPlugin` normalizes it to handle edge cases:

1. **ListVariable**: Extracts the last non-empty item
2. **NodeVariable wrapping List**: Converts the list to a space-separated string (or single item if list has one element)
3. **Regular NodeVariable**: Returns as-is

This prevents issues where `BaseTemplater.evaluateToVariable()` returns a `NodeVariable` wrapping a `java.util.List`, which would display as `[item1, item2]` instead of `item1 item2`.

---

## Examples

### Example 1: Simple Function with Return

```xml
<?xml version="1.0" encoding="UTF-8"?>
<config xmlns="http://org.webharvest/schema/2.1/core" charset="UTF-8">
  <function name="greet">
    <return>
      <template>Hello World</template>
    </return>
  </function>
  
  <def var="result">
    <call name="greet"/>
  </def>
</config>
```

**Result**: `result = "Hello World"`, `functionResult = "Hello World"` (inside function scope)

### Example 2: Function with Parameters

```xml
<?xml version="1.0" encoding="UTF-8"?>
<config xmlns="http://org.webharvest/schema/2.1/core" charset="UTF-8">
  <function name="greet">
    <def var="name"><call-param name="name"/></def>
    <return>
      <template>Hello ${name}</template>
    </return>
  </function>
  
  <def var="result">
    <call name="greet">
      <call-param name="name">World</call-param>
    </call>
  </def>
</config>
```

**Result**: `result = "Hello World"`

### Example 3: Function Without Explicit Return

```xml
<?xml version="1.0" encoding="UTF-8"?>
<config xmlns="http://org.webharvest/schema/2.1/core" charset="UTF-8">
  <function name="greet">
    <def var="name"><call-param name="name"/></def>
    <template>Hello ${name}</template>
  </function>
  
  <def var="result">
    <call name="greet">
      <call-param name="name">World</call-param>
    </call>
  </def>
</config>
```

**Result**: `result = "Hello World"` (last expression's result is returned)

### Example 4: Function with Multiple Parameters

```xml
<?xml version="1.0" encoding="UTF-8"?>
<config xmlns="http://org.webharvest/schema/2.1/core" charset="UTF-8">
  <function name="formatName">
    <def var="firstName"><call-param name="firstName"/></def>
    <def var="lastName"><call-param name="lastName"/></def>
    <return>
      <template>${firstName} ${lastName}</template>
    </return>
  </function>
  
  <def var="fullName">
    <call name="formatName">
      <call-param name="firstName">John</call-param>
      <call-param name="lastName">Doe</call-param>
    </call>
  </def>
</config>
```

**Result**: `fullName = "John Doe"`

### Example 5: Function Called Multiple Times

```xml
<?xml version="1.0" encoding="UTF-8"?>
<config xmlns="http://org.webharvest/schema/2.1/core" charset="UTF-8">
  <function name="square">
    <def var="x"><call-param name="x"/></def>
    <return>
      <script>parseInt(context.getVar("x")) * parseInt(context.getVar("x"))</script>
    </return>
  </function>
  
  <def var="result1">
    <call name="square">
      <call-param name="x">5</call-param>
    </call>
  </def>
  
  <def var="result2">
    <call name="square">
      <call-param name="x">10</call-param>
    </call>
  </def>
</config>
```

**Result**: `result1 = 25`, `result2 = 100`

### Example 6: System Variables in Action

```xml
<?xml version="1.0" encoding="UTF-8"?>
<config xmlns="http://org.webharvest/schema/2.1/core" charset="UTF-8">
  <def var="data">
    <http url="https://example.com"/>
  </def>
  
  <try>
    <def var="parsed">
      <xpath expression="//title/text()">${data}</xpath>
    </def>
  </try>
  <catch>
    <template>Error: ${error}</template>
  </catch>
  
  <!-- System variables available:
       - configResult: Result of entire config execution
       - tryResult: Result of <try> block (if successful)
       - error: Error message (if exception occurred)
       - sys: System utilities object
  -->
</config>
```

---

## Best Practices

1. **Always define functions before calling them** (lexical order in XML)
2. **Use explicit `<return>` for clarity** when the return value is important
3. **Use descriptive parameter names** to make function calls readable
4. **Leverage system variables** like `sys.fullUrl()` for common operations
5. **Handle errors gracefully** using `<try>`/`<catch>` blocks
6. **Document function purposes** using XML comments

---

## Troubleshooting

### Function Not Found

**Error**: `Function 'myFunction' not found`

**Cause**: Function was not defined before it was called, or function name is misspelled.

**Solution**: Ensure the `<function>` element appears before the `<call>` element in the XML.

### Parameter Not Found

**Error**: Parameter value is empty or incorrect

**Cause**: Parameter name mismatch between `<call-param>` and `<call-param>` inside function.

**Solution**: Ensure parameter names match exactly (case-sensitive).

### Return Value Has Extra Brackets

**Error**: Function returns `[item1, item2]` instead of `item1 item2`

**Cause**: `BaseTemplater` or `PluginBodyExecutor` returned a `NodeVariable` wrapping a `List`.

**Solution**: This is handled automatically by `CallPlugin`'s normalization logic. If it still occurs, check that the function body returns a single value, not a list.

---

## Related Documentation

- [Plugin Development Guide](web/documentation/plugin-development.html)
- [Variable System Architecture](web/documentation/architecture.html)
- [Legacy 2.2.0 Script Plugin Reference](web/documentation/2.2.0/script.html)

---

## Variable Filtering in IDE

### Function Definition Filtering

When variables are extracted from the execution context for display in the IDE's Variables panel, function definitions are automatically filtered out. This prevents function names (like `greet`, `myFunction`) from appearing as user variables.

**Implementation**: `ExecutionManager.extractVariables()` checks if a variable is an `EmptyVariable` and if a function with that name exists in the `PluginConfigurationTree`. If both conditions are true, the variable is skipped.

**Code**:
```java
// Skip function definitions: if value is EmptyVariable and a function with this name exists in config
if (value != null && value instanceof EmptyVariable) {
    if (isFunctionDefinition(configTree, key)) {
        LOG.debug("    → Skipping function definition: {}", key);
        continue;
    }
}
```

**Result**: Only actual user-defined variables and system variables are displayed in the Variables panel, making it easier to see the actual execution results.

---

**Last Updated**: 2025-01-15  
**Version**: 2.3.1

