# Changelog - Semantic Model Alignment Implementation

## 2025-12-09 - Semantic Model Alignment Implementation

### Added

#### Backend

1. **Schema Version Synchronization**
   - Added `/api/plugins/metadata/version` endpoint in `PluginMetadataServlet`
   - Returns current schema version for frontend verification

2. **Execution Semantics**
   - Added `extractExecutionSemantics()` method in `PluginMetadataCatalog`
   - Execution semantics now included in `PluginDescriptor.extensions`
   - Exported via API in `PluginMetadataServlet`

3. **Ontology Endpoint**
   - Added `/api/plugins/ontology` endpoint in `PluginMetadataServlet`
   - Exports entity schemas and relationship constraints
   - Provides unified ontology model for LLM agents

4. **Port Types Defaults**
   - Added `addDefaultPortTypes()` method in `PluginMetadataServlet`
   - Ensures port types are always exported, even when `VisualPluginMetadata` is unavailable

#### Frontend

1. **Schema Version Management**
   - Added `checkSchemaVersion()` method in `PluginManager`
   - Added `refresh()` method with listener notifications
   - Added automatic version checking with configurable interval
   - Added `schemaVersion` field to `VisualSemanticModel` and `VisualSemanticModelForLLM`

2. **Extended Plugin Semantics**
   - Extended `PluginSemanticInfo` with full `roles` array
   - Added `executionSemantics` to `PluginSemanticInfo.semantics`
   - Added `variableSemantics` to `PluginSemanticInfo.semantics`
   - Added `masterDetailConstraints` to `PluginSemanticInfo`
   - Added `portCompatibility` matrix to `PluginSemanticInfo.visual`

3. **Variable Semantics Correlation**
   - Added `extractDefinedVariables()` method in `VariableDependencyAnalyzer`
   - Added `updateVariableSemanticsFromDependencies()` in `VisualSemanticModelManager`
   - Automatic correlation of `VariableDependency` with plugin semantics

4. **LLM-Friendly Format**
   - Added `toLLMFormat()` method in `VisualSemanticModelManager`
   - Converts all Map/Set to Object/Array for JSON serialization
   - Added detailed descriptions for all available actions

5. **Schema Extensions Unification**
   - Created `SchemaMapper` utility (`webharvest-ide/frontend/src/shared/metadata/SchemaMapper.ts`)
   - Always uses `extensions.schema.attributeDefinitions` as source of truth
   - Updated `PluginNormalizer` to use `mapSchemaFromExtensions()`

6. **VisualSemanticModelExporter Update**
   - Added `schemaVersion` field to exported model

### Changed

1. **PluginMetadataCatalog**
   - `extractSchemaExtensions()` now accepts `org.webharvest.plugin.semantic.PluginSemantics` parameter
   - Execution semantics extracted and added to extensions

2. **PluginMetadataServlet**
   - `buildMetadataFromDescriptor()` now includes execution semantics in semantics map
   - Always exports port types (defaults if not available)

3. **VisualSemanticModelManager**
   - `buildAvailablePluginsMap()` now includes full semantics mapping
   - `buildModel()` now updates variable semantics from dependencies
   - Added `toLLMFormat()` for LLM-friendly serialization

4. **PluginNormalizer**
   - Now uses `SchemaMapper.mapSchemaFromExtensions()` for schema normalization
   - Always prefers `extensions.schema.attributeDefinitions`

### Technical Details

#### API Endpoints

- `GET /api/plugins/metadata/version` - Returns schema version
- `GET /api/plugins/ontology` - Returns ontology schema

#### Type Changes

**PluginSemanticInfo** (extended):
```typescript
semantics: {
  roles: string[]; // Full array from backend
  executionSemantics?: {
    isContainer: boolean;
    isLeaf: boolean;
    executionOrder: 'sequential' | 'parallel' | 'conditional';
    canPause: boolean;
    canResume: boolean;
  };
  variableSemantics?: {
    definesVariables: string[];
    usesVariables: string[];
    variableScope: 'root' | 'parent';
  };
};
visual: {
  portCompatibility?: { [portType: string]: string[] };
};
masterDetailConstraints?: Array<{...}>;
```

**VisualSemanticModel** (extended):
```typescript
{
  schemaVersion: string | null; // Added
  // ... rest of model
}
```

### Migration Notes

All changes are backward compatible:
- Legacy schema formats still supported via fallback
- Default port types added automatically
- Execution semantics optional
- Variable semantics updated at runtime

### Testing

- ✅ Backend compilation successful
- ✅ Frontend compilation successful
- ⚠️ 3 existing test failures (unrelated to these changes):
  - `CallParamPluginTest.testCallParamPluginExecute`
  - `CallParamPluginTest.testCallParamPluginWithIdAttribute`
  - `HtmlToXmlPluginTest.testGetConfigurationSchema`

### Files Modified

**Backend**:
- `webharvest-core/src/main/java/org/webharvest/plugin/metadata/PluginMetadataCatalog.java`
- `webharvest-ide/src/main/java/org/webharvest/server/api/PluginMetadataServlet.java`

**Frontend**:
- `webharvest-ide/frontend/src/core/PluginManager.ts`
- `webharvest-ide/frontend/src/visual/VisualSemanticModel.ts`
- `webharvest-ide/frontend/src/visual/VisualSemanticModelManager.ts`
- `webharvest-ide/frontend/src/visual/VisualSemanticModelExporter.ts`
- `webharvest-ide/frontend/src/visual/VariableDependencyAnalyzer.ts`
- `webharvest-ide/frontend/src/shared/metadata/PluginNormalizer.ts`
- `webharvest-ide/frontend/src/shared/metadata/SchemaMapper.ts` (new)

### Documentation

- Created `SEMANTIC_MODEL_ALIGNMENT_IMPLEMENTATION.md` - Comprehensive implementation documentation
- Created `CHANGELOG_SEMANTIC_MODEL_ALIGNMENT.md` - This changelog





