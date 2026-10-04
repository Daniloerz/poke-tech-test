# Frontend — Implementation Plan

## Structure

```text
frontend/
├── index.html, package.json, vite.config.js, .yarnrc.yml
├── Dockerfile, nginx.conf, .dockerignore
└── src/
    ├── main.jsx                 entry: router + auth provider
    ├── App.jsx                  routes
    ├── index.css                global styles (variables, layout, buttons, forms)
    ├── api/                     client.js (fetch wrapper, errors, token), one file per backend resource
    ├── auth/                    AuthContext.jsx (login state), RequireAuth.jsx (protected routes)
    ├── hooks/                   useApi.js (loading / error / data for one request)
    ├── components/              Layout, Pagination, StatusMessage, PokemonCard, EvolutionTree
    └── pages/                   one component per route
```

## Data flow

```text
page → useApi(() => api function) → client.js → fetch('/api/v1/...') → proxy → backend
```

- `client.js` adds the JSON headers and the `Authorization` header, turns Problem Details into an `ApiError` (`status`, `detail`, `errors`, `localId`), and, on a `401` with a token, logs the user out.
- `useApi` keeps `loading`, `error` and `data` for one request and offers `reload()`.
- Forms call the api functions directly in their submit handler.

## State

- Server data stays in the page that shows it (no global store).
- The only shared state is the login: `AuthContext` (token, username, `login`, `logout`), with the token in `sessionStorage`.
- Pagination lives in the URL (`?page=2`), so back/forward and refresh keep the page.

## Run

| Mode | How | API calls |
|---|---|---|
| Development | `corepack yarn dev` (port 5173) | Vite proxy `/api` → `http://localhost:8080` (`API_PROXY_TARGET`) |
| Docker | `docker compose up --build` (port 3000) | nginx proxy `/api` → `backend:8080` |

## Validation

- `corepack yarn build` without errors or warnings.
- Manual check in the browser of every screen and its loading, error and empty states; no warnings in the console.
- `docker compose up --build` with the four services.
