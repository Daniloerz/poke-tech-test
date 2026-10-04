# Auth — Context and Plan

## Goal

Log in and register against the backend JWT API, and protect the "My Pokemon" screens.

## Acceptance criteria

| ID | Criterion | Backend contract |
|---|---|---|
| AC-01 | `/login` has username and password fields and shows the demo users. Valid credentials save the token and return to the page the user wanted (or `/my-pokemon`). | user-auth AC-08 |
| AC-02 | Wrong credentials show "Invalid username or password." | user-auth AC-09 |
| AC-03 | `/register` creates the user, logs in automatically and opens `/my-pokemon`. Field errors from the backend are shown next to each field; a taken username shows the `409` message. | user-auth AC-01..AC-06 |
| AC-04 | The header shows the username and a "Log out" button when logged in, and "Log in" otherwise. | — |
| AC-05 | `/my-pokemon` routes redirect to `/login` without a token. A `401` from the API (expired token) logs the user out. | user-auth AC-14 |

## Plan

| File | Responsibility |
|---|---|
| `api/authApi.js` | `login`, `register` |
| `api/client.js` | Adds the token from `sessionStorage`; on `401` with a token calls the handler set by `AuthProvider` |
| `auth/AuthContext.jsx` | `AuthProvider` + `useAuth()`: `isAuthenticated`, `username` (from the token `sub`), `login`, `logout` |
| `auth/RequireAuth.jsx` | Route guard: `<Outlet />` or redirect to `/login` with the original path |
| `pages/LoginPage.jsx`, `pages/RegisterPage.jsx` | Forms |
| `components/Layout.jsx` | Header navigation and login state |
