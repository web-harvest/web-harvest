# WebHarvest IDE - TypeScript Frontend

## Overview

Modern, type-safe frontend for WebHarvest IDE built with TypeScript, following TDD (Test-Driven Development) practices.

## Technology Stack

- **TypeScript 5.3** - Type-safe JavaScript
- **Vite 5.0** - Ultra-fast build tool
- **Jest 29.7** - Testing framework
- **ESLint** - Code quality
- **Monaco Editor** - Code editor (VS Code's editor)

## Project Structure

```
frontend/
├── src/
│   ├── core/                    # Core functionality
│   │   ├── TabManager.ts        # ✅ Tab management (15 tests)
│   │   ├── App.ts               # 🔄 Main application
│   │   └── __tests__/
│   ├── visual/                  # Visual designer
│   │   ├── XMLParser.ts         # ✅ XML → Visual (13 tests)
│   │   ├── XMLGenerator.ts      # ✅ Visual → XML (20 tests)
│   │   ├── VisualDesigner.ts    # 🔄 Canvas & nodes
│   │   ├── PropertyPanel.ts     # 🔄 Node properties
│   │   └── __tests__/
│   ├── panels/                  # UI Panels
│   │   ├── LogsPanel.ts         # ✅ Logs display (18 tests)
│   │   ├── ResultsPanel.ts      # 🔄 Results tree
│   │   ├── VariablesPanel.ts    # 🔄 Variables list
│   │   ├── SessionPanel.ts      # 🔄 Session info
│   │   └── __tests__/
│   ├── types/                   # TypeScript definitions
│   │   └── index.ts             # All types (75+)
│   ├── styles/                  # CSS
│   │   └── main.css
│   └── main.ts                  # Entry point
├── index.html
├── package.json
├── tsconfig.json
├── jest.config.js
├── vite.config.ts
└── README.md
```

## Development

### Build Project

```bash
# From project root
mvn package -DskipTests -pl webharvest-ide -am
```

This will:
1. Install Node.js & npm (via frontend-maven-plugin)
2. Run `npm install`
3. Run `npm run build` 
4. Bundle frontend into `src/main/resources/webapp/dist/`

### Run Tests

```bash
cd frontend
npm test
```

### Development Server

```bash
cd frontend
npm run dev
```

Opens development server at `http://localhost:3000` with:
- Hot module replacement (HMR)
- Proxy to backend API (`http://localhost:8080`)
- WebSocket proxy

## Testing Philosophy

### TDD Approach

**Red → Green → Refactor**

1. **Write test first** (RED) ❌
2. **Implement feature** (GREEN) ✅
3. **Refactor** ♻️

### Test Coverage

- **Goal**: 80%+ coverage
- **Current**: ~95% for implemented components
- **Total Tests**: 66+ test cases

### Example

```typescript
// 1. Write test (RED)
it('should add new tab', () => {
  const tab = manager.addTab('test.xml');
  expect(tab.name).toBe('test.xml');
});

// 2. Implement (GREEN)
addTab(name: string): Tab {
  const tab = { id: generateId(), name, content: '' };
  this.tabs.push(tab);
  return tab;
}

// 3. Refactor ♻️
// (improve code quality while keeping tests green)
```

## Components Status

### ✅ Complete (66 tests)

- **TabManager** (15 tests)
  - Add/remove/switch tabs
  - Mark dirty/clean
  - Update content
  
- **XMLParser** (13 tests)
  - Parse simple/nested elements
  - Handle CDATA
  - Assign unique IDs
  
- **XMLGenerator** (20 tests)
  - Generate XML from nodes
  - Handle special characters
  - Proper indentation
  
- **LogsPanel** (18 tests)
  - Add/clear logs
  - Filter by type
  - Auto-scroll

### 🔄 In Progress

- ResultsPanel
- VariablesPanel
- SessionPanel
- WebSocketManager
- VisualDesigner
- PropertyPanel

## Integration with Backend

### API Endpoints

- `GET /api/plugins` - Get plugin metadata
- `POST /api/execute` - Start execution
- `GET /api/execution/{id}` - Get execution status
- `POST /api/execution/{id}/pause` - Pause execution
- `POST /api/execution/{id}/resume` - Resume execution

### WebSocket

- `ws://localhost:8080/ws/execution/{id}` - Real-time execution updates

## Type Safety

All components are fully typed:

```typescript
interface Tab {
  id: string;
  name: string;
  content: string;
  isDirty: boolean;
  filePath?: string;
  executionId?: string;
  sessionInfo?: SessionInfo;
  tokenUsage?: TokenUsage;
  visualNodes?: VisualNode[];
}
```

## Migration from Vanilla JS

This is a complete rewrite of the original `app.js` using TypeScript and modern practices:

- ✅ **Type safety** - Catch errors at compile time
- ✅ **Better IDE support** - Autocomplete, refactoring
- ✅ **Maintainability** - Clear interfaces
- ✅ **Testability** - Easy to mock and test
- ✅ **Scalability** - Ready for future features

## Contributing

1. Write tests first (TDD)
2. Follow TypeScript strict mode
3. Use ESLint for code quality
4. Maintain 80%+ test coverage
5. Document complex logic

## License

Same as WebHarvest project.








