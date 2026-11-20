# WebHarvest IDE local security model

The embedded IDE is a local desktop application, not an authenticated remote service.

## Listener and request boundary

- The embedded Jetty listener is intended to bind only to a loopback interface.
- Privileged HTTP routes under `/api/*` and WebSocket routes under `/ws/*` accept only loopback connections addressed through a loopback Host authority.
- Any supplied Origin must be a trusted local UI origin. State-changing HTTP requests and WebSocket handshakes require an Origin; read-only requests may omit it.
- The only separate development origin allowed by the server guard is `http://localhost:3000`.
- The Vite development server is pinned to port 3000 (`strictPort: true`) and proxies `/api` and `/ws` to the embedded server. Keep these requests same-origin through the proxy; wildcard CORS is intentionally disabled.
- Host and Origin checks are server-side defenses. Browser Local Network Access prompts or CORS behavior are not treated as authorization.

## Operational limits

- Do not expose the IDE through a LAN bind, firewall rule, reverse proxy, container port mapping, SSH tunnel, or public tunnel.
- Remote/server deployment is unsupported until a separate authentication and authorization model is designed.
- This boundary protects against remote websites and non-loopback network clients using the local browser/service path. It is not a sandbox against malicious code or processes already running as the local user.
- Workflow configuration may contain executable scripts. Only run configuration from sources you trust.
