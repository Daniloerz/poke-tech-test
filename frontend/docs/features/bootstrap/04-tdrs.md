# Frontend — Technical Decision Records

## TDR-001 — Stack and versions

### Context / Problem

We need a small, modern React setup that is easy to explain and runs with the local Node.js 22.19 (the global Node must not change).

### Options considered

#### Option A — React 19 + Vite 8 + React Router 8
- Latest versions.
- React Router 8 requires Node 22.22 or newer; the local Node is 22.19.

#### Option B — React 19 + Vite 8 + React Router 7
- React Router 7 needs Node 20 or newer and has the same API used here (`BrowserRouter`, `Routes`, `Route`, `Link`, `useParams`, `useSearchParams`).

### Decision

Option B: React 19.3, React DOM 19.3, React Router 7.18, Vite 8.3 with `@vitejs/plugin-react` 6.1, Yarn 4 through `corepack yarn` with the `node-modules` linker. JavaScript, no TypeScript (user decision).

### Rationale

It runs on the existing Node without global changes, and React Router 7 is a stable, widely documented version.

### Consequences

- Upgrading to React Router 8 later only needs a newer Node; the code uses the common API.
- The `node-modules` linker keeps a normal `node_modules` folder, which every tool understands (Yarn's default Plug'n'Play needs editor setup).

---

## TDR-002 — State and data fetching

### Context / Problem

Pages load data from the API and must show loading, error and empty states. Only the login is shared between pages.

### Options considered

#### Option A — A library (Redux, Zustand, React Query)
- Caching, retries, global store.
- Extra concepts and dependencies for a small app (CLAUDE.md section 15 asks to avoid them without a real need).

#### Option B — React only: one context for the login and one small hook for requests
- `AuthContext` holds the token; `useApi` holds `loading`, `error`, `data` for one request; server data stays in the page.

### Decision

Option B.

### Rationale

It covers every need of the screens with plain React, and each piece is a few lines that can be explained in an interview.

### Consequences

- No client cache: going back to a page loads it again (the backend already caches PokeAPI in Redis, so it is fast).

---

## TDR-003 — Where to keep the JWT

### Context / Problem

The token must survive a page refresh, and it should be exposed as little as possible.

### Options considered

#### Option A — Memory only
- Most secure; lost on every refresh (the user logs in again).

#### Option B — `localStorage`
- Survives refreshes and browser restarts; stays available to any script for a long time.

#### Option C — `sessionStorage`
- Survives refreshes; removed when the tab is closed. Same XSS exposure as `localStorage`, but for less time.

### Decision

Option C.

### Rationale

A good balance for a demo: no login after each refresh, and the token disappears with the tab. The token also expires after one hour on the backend.

### Consequences

- The username shown in the header is read from the token (`sub` claim); the token is not verified in the browser, the backend does that.
- On any `401` with a token, the frontend removes it and the user logs in again.

---

## TDR-004 — Styles

### Context / Problem

The interface must be responsive and readable without a design system.

### Options considered

#### Option A — A UI library (MUI) or Tailwind
- Fast to build; one more dependency and its own concepts.

#### Option B — Plain CSS: one global file plus CSS Modules per component
- Built into Vite; class names are local to each component; no dependency.

### Decision

Option B.

### Rationale

No dependency, and the styles are easy to read next to each component.

### Consequences

- `index.css` defines colors as CSS variables, the page layout, buttons and forms; components only add their own layout.

---

## TDR-005 — Docker image

### Context / Problem

The frontend must run with `docker compose up --build` next to the backend.

### Options considered

#### Option A — Run the Vite dev server in the container
- One stage; not meant for production (slower, development features on).

#### Option B — Multi-stage: build with Node, serve the static files with nginx
- Small final image; nginx also proxies `/api` (ADR-001) and returns `index.html` for client-side routes.

### Decision

Option B, with the `nginxinc/nginx-unprivileged` image (nginx as a non-root user, port 8080).

### Rationale

Standard way to serve a single-page app; non-root like the backend image.

### Consequences

- Published on `FRONTEND_HOST_PORT` (default `3000`).
