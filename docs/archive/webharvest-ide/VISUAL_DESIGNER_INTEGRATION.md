# Visual Designer - Java Integration Guide

## Architecture Overview

The Visual Designer is a **frontend application** (TypeScript/HTML/CSS) that runs **inside the existing WebHarvest IDE Java application**. It does NOT require a separate Node.js server in production.

### How It Works

```
┌─────────────────────────────────────────────────────────────┐
│  WebHarvest IDE JAR (java -jar webharvest-ide-2.2.0.jar)   │
│                                                             │
│  ┌─────────────────────────────────────────────────────┐  │
│  │  Embedded Jetty Server (Port 8080)                  │  │
│  │                                                       │  │
│  │  Static Resources:                                   │  │
│  │  ├─ /webapp/                  (existing web IDE)    │  │
│  │  └─ /webapp/visual-designer/  (NEW visual designer) │  │
│  │                                                       │  │
│  │  Servlets:                                           │  │
│  │  ├─ /api/config/*             (existing)            │  │
│  │  ├─ /api/execute/*            (existing)            │  │
│  │  └─ /api/plugins/metadata     (NEW)                 │  │
│  └─────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────┘

Browser:
  http://localhost:8080/visual-designer/
  → Jetty serves index.html + JS/CSS
  → JavaScript calls /api/plugins/metadata
  → Jetty servlet responds with plugin data
```

## Development Workflow

### Development Mode (Two Processes)

**Terminal 1 - Java Backend:**
```bash
cd webharvest-ide
mvn clean compile
java -jar target/webharvest-ide-2.2.0.jar
# Running on http://localhost:8080
```

**Terminal 2 - Frontend Dev Server:**
```bash
cd webharvest-ide
npm run dev
# Running on http://localhost:3000
# Proxies /api/* to http://localhost:8080
```

**Work in Browser:**
- Open `http://localhost:3000/` (Vite dev server)
- Hot reload on file changes
- API calls proxied to Java backend

### Production Mode (Single JAR)

```bash
cd webharvest-ide
mvn clean package

# This runs:
# 1. npm run build (Vite builds TypeScript → JavaScript)
# 2. Output → target/classes/webapp/visual-designer/
# 3. Maven packages everything into JAR

java -jar target/webharvest-ide-2.2.0.jar
# Everything served from http://localhost:8080
```

## File Structure

```
webharvest-ide/
├── pom.xml                               # Maven config (updated)
├── package.json                          # npm config
├── vite.config.js                        # Vite config
├── tsconfig.json                         # TypeScript config
│
├── src/main/
│   ├── java/
│   │   └── org/webharvest/server/
│   │       ├── WebHarvestServer.java    # Existing (no changes needed)
│   │       └── api/
│   │           └── PluginMetadataServlet.java  # NEW servlet
│   │
│   └── resources/webapp/
│       ├── index.html                    # Existing web IDE
│       ├── static/                       # Existing assets
│       └── visual-designer/              # NEW visual designer
│           ├── index.html
│           ├── styles/
│           │   ├── main.css
│           │   └── palette.css
│           ├── core/
│           │   ├── main.ts               # Entry point
│           │   ├── VisualDesigner.ts
│           │   └── PluginRegistry.ts
│           ├── components/               # (Phase 2)
│           ├── converters/               # (Phase 3)
│           └── utils/
│               └── EventEmitter.ts
│
└── target/
    └── classes/webapp/visual-designer/   # Built output (in JAR)
        ├── index.html
        ├── assets/
        │   ├── main-[hash].js            # Compiled from TypeScript
        │   └── main-[hash].css
        └── ...
```

## Maven Integration

### Updated pom.xml (add to `<build><plugins>`):

```xml
<!-- Frontend Build Plugin -->
<plugin>
    <groupId>com.github.eirslett</groupId>
    <artifactId>frontend-maven-plugin</artifactId>
    <version>1.15.0</version>
    <executions>
        <!-- Install Node & NPM -->
        <execution>
            <id>install node and npm</id>
            <goals>
                <goal>install-node-and-npm</goal>
            </goals>
            <phase>generate-resources</phase>
            <configuration>
                <nodeVersion>v18.19.0</nodeVersion>
                <npmVersion>10.2.3</npmVersion>
            </configuration>
        </execution>
        
        <!-- npm install -->
        <execution>
            <id>npm install</id>
            <goals>
                <goal>npm</goal>
            </goals>
            <phase>generate-resources</phase>
            <configuration>
                <arguments>install</arguments>
            </configuration>
        </execution>
        
        <!-- npm run build -->
        <execution>
            <id>npm run build</id>
            <goals>
                <goal>npm</goal>
            </goals>
            <phase>compile</phase>
            <configuration>
                <arguments>run build</arguments>
            </configuration>
        </execution>
    </executions>
    <configuration>
        <workingDirectory>${project.basedir}</workingDirectory>
    </configuration>
</plugin>
```

This plugin:
1. **Downloads Node.js** (no need to install globally)
2. **Runs `npm install`** (installs dependencies)
3. **Runs `npm run build`** (Vite builds TypeScript → JS)
4. **Output goes to** `target/classes/webapp/visual-designer/`
5. **Maven packages** into JAR as usual

## No Separate Server Needed

❌ **You do NOT need:**
- Separate Node.js server in production
- nginx or Apache
- Docker containers for frontend

✅ **You only need:**
- `java -jar webharvest-ide-2.2.0.jar`
- Everything is inside the JAR

## How Users Will Access It

```
Single JAR Distribution:

java -jar webharvest-ide-2.2.0.jar

Opens browser to http://localhost:8080/

User sees:
┌─────────────────────────────────────┐
│  🌺 WebHarvest IDE                  │
│                                     │
│  [Existing Web IDE] [Visual Editor] │ ← New tab
│                                     │
└─────────────────────────────────────┘

Click "Visual Editor" → 
  Navigates to /visual-designer/
  → Jetty serves index.html from JAR
  → Browser loads JavaScript
  → JavaScript calls /api/plugins/metadata
  → Visual designer works!
```

## Why This Works

1. **Vite is a build tool**, not a runtime server
2. **TypeScript compiles to JavaScript** at build time
3. **JavaScript runs in browser**, not on server
4. **Jetty serves static files** from JAR (HTML/JS/CSS)
5. **JavaScript calls REST API** (`/api/*`) back to Jetty

This is exactly how **VS Code**, **IntelliJ IDEA**, and other modern IDEs work - they bundle web frontends in native apps!

## Next Steps

1. ✅ Frontend structure created
2. ⏩ Create `PluginMetadataServlet.java` (backend)
3. ⏩ Update `pom.xml` with frontend-maven-plugin
4. ⏩ Test development workflow
5. ⏩ Test production build

---

**TL;DR:** Visual Designer is **frontend code** that gets **compiled into JAR** and **served by Jetty**. No separate Node.js server needed in production!


