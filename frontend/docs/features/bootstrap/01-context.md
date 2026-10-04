# Frontend — Bootstrap

## Goal

A small React application that consumes the backend API and covers the four user stories: browse the catalog (US01), see a detail (US02), synchronize a Pokemon (US03) and manage the local copies (US04), plus login and registration.

The exercise asks for a responsive, user-centric interface, standard CRUD operations, clean component organization and efficient state management. The user asked for the most basic version that follows good practices, so it can be explained quickly.

## Rules (CLAUDE.md section 0)

- JavaScript, React, Vite, React Router, CSS Modules, `fetch` with small custom hooks. Interface in English.
- No frontend tests: the check is `yarn build` plus a manual review in the browser, with no warnings in the console.
- No state or data-fetching libraries; no UI library.

## Screens

| Route | Screen | Access | Story |
|---|---|---|---|
| `/pokemon` | Catalog (paginated cards) | Public | US01 |
| `/pokemon/:idOrName` | Detail (image, stats, description, evolution chain) + "Save to My Pokemon" | Public (save needs login) | US02, US03 |
| `/login`, `/register` | Login and registration | Public | Auth |
| `/my-pokemon` | Local Pokemon (paginated list, edit, delete) | Login | US04 |
| `/my-pokemon/:id/edit` | Edit proprietary fields | Login | US04 |

## Assumptions

- The backend contract is the one documented in `backend/docs/features/*/02-implementation-plan.md`; the frontend does not duplicate backend validation, it shows the backend errors.
- Mobile-first, simple CSS; no design system.
