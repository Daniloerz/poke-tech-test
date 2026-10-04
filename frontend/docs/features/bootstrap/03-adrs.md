# Frontend — Architecture Decision Records

## ADR-001 — How the browser reaches the API

### Context / Problem

The frontend (port 5173 in development, 3000 in Docker) and the backend (port 8080) are different origins. A browser blocks cross-origin calls unless the server allows them.

### Options considered

#### Option A — CORS in the backend
- The browser calls `http://localhost:8080` directly; Spring Security allows the frontend origin.
- One more security setting in the backend, and the list of allowed origins changes per environment.

#### Option B — Same origin through a proxy
- The frontend always calls a relative path (`/api/v1/...`). In development Vite forwards `/api` to the backend; in Docker nginx does it.
- No CORS, no backend change, the same code in every environment.

### Decision

Option B.

### Rationale

It is the simplest setup with nothing to configure in the backend, and the API URL never appears in the frontend code.

### Consequences

- The Vite proxy target is configurable with `API_PROXY_TARGET` (default `http://localhost:8080`).
- In Docker, nginx serves the static build and proxies `/api/` to the `backend` service by its Compose name.
