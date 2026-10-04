# My Pokemon — Context and Plan

## Goal

Manage the local copies (US03, US04): list them, edit the proprietary fields and delete them.

## Acceptance criteria

| ID | Criterion | Backend contract |
|---|---|---|
| AC-01 | `/my-pokemon` lists the local Pokemon page by page: sprite, name, localized name, region, tags, with "Edit" and "Delete" actions. | US04 AC-01 |
| AC-02 | Empty list: a message that explains how to add Pokemon from the catalog. | US04 AC-02 |
| AC-03 | "Delete" asks for confirmation, deletes and reloads the list. | US04 AC-10 |
| AC-04 | `/my-pokemon/:id/edit` shows the PokeAPI data as read-only and a form with localized name, region and tags (comma-separated). Empty fields clear the value. | US04 AC-03..AC-06 |
| AC-05 | Saving shows the backend field errors next to each field; on success it returns to the list. | US04 AC-05..AC-07 |
| AC-06 | An unknown id shows "not found". | US04 AC-09 |

## Plan

| File | Responsibility |
|---|---|
| `api/localPokemonApi.js` | `fetchLocalPokemonPage`, `fetchLocalPokemon`, `syncPokemon`, `updateLocalPokemon`, `deleteLocalPokemon` |
| `pages/LocalPokemonListPage.jsx` | List, pagination, delete |
| `pages/LocalPokemonEditPage.jsx` | Load one record, edit form, save |

Notes:

- Tags are typed as one comma-separated text and sent as a list; the backend normalizes them (lower case, no duplicates).
- An empty text field is sent as `null`, so the value is cleared.
