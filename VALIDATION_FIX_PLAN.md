# Validation Fix Plan

## Problem

ConfigFactory.create() is TOO PERMISSIVE:
- Accepts invalid attributes (badattr="...")  
- Accepts invalid var names (var2="..." instead of var="...")
- Accepts random text outside tags
- Does NOT validate against plugin schemas

## Root Cause

WebHarvest parser is very tolerant - it ignores unknown attributes and plugins.

## Solution

### 1. Create SchemaValidator class
- Parse XML DOM
- For each element, lookup plugin in registry
- Validate attributes against PluginConfigurationSchema
- Validate required attributes exist
- Validate no unknown attributes
- Validate context-specific plugins (e.g., http-header only in http)

### 2. Enhance ConfigServlet
- Call SchemaValidator BEFORE ConfigFactory
- Return detailed error list
- Each error has: line, message, type, plugin

### 3. Frontend displays ALL errors
- Loop through errors array
- Show: "Line X [plugin]: message"

## Implementation Status

❌ Not started (session timeout approaching)
⏳ Will implement in next session

## Notes

- xquery.xml error is RUNTIME (Saxon type conversion)
- Validation can only catch STRUCTURE errors
- Runtime errors appear during execution


