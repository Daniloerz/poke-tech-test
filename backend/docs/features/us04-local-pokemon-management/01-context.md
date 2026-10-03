# US04 — Local Data Modification

## Feature

List, update and delete the local Pokemon created by US03. Together with US03 this completes the CRUD on the local data.

## Goal

Let authenticated users manage the local copy: browse it, edit the proprietary fields (localized name, region, tags) and remove records, with robust validation.

## Functional context

The exercise says: "Enable update operations for any Pokemon currently stored within the local database. Ensure robust validation: provide 404 responses for missing records, 400 status codes for malformed payloads, and incorporate further defensive logic as required." It also asks for "comprehensive CRUD operations for the defined dataset".

CRUD on `local_pokemon`:

| Operation | Endpoint | Feature |
|---|---|---|
| Create | `POST /api/v1/local-pokemon` (synchronization) | US03 |
| Read one | `GET /api/v1/local-pokemon/{id}` | US03 |
| Read many | `GET /api/v1/local-pokemon?page&size` | US04 |
| Update | `PUT /api/v1/local-pokemon/{id}` | US04 |
| Delete | `DELETE /api/v1/local-pokemon/{id}` | US04 |

## Actors

- **Authenticated user.** All routes are protected.

## User stories

> As an authenticated user, I want to see the local Pokemon page by page, so that I can find the one I want to edit.

> As an authenticated user, I want to edit the localized name, region and tags of a local Pokemon, so that our own data is stored next to the PokeAPI data.

> As an authenticated user, I want to delete a local Pokemon, so that I can remove data we no longer need.

## Acceptance criteria

### List — `GET /api/v1/local-pokemon?page=0&size=20`

| ID | Criterion |
|---|---|
| AC-01 | Returns `200` with `{content, page, size, totalElements, totalPages}` (same page format as US01). Items have the US03 shape, ordered by local id. |
| AC-02 | Defaults `page=0`, `size=20`. `page < 0`, `size < 1`, `size > 50` or non-numeric values return `400`. A page after the last one returns an empty `content`. |

### Update — `PUT /api/v1/local-pokemon/{id}`

| ID | Criterion |
|---|---|
| AC-03 | Body `{"localizedName": "Pikachu", "region": "Kanto", "tags": ["electric-mouse", "mascot"]}` replaces the three proprietary fields and returns `200` with the updated record. |
| AC-04 | `updatedAt` changes when at least one value changes (a `PUT` with the same values writes nothing); `syncedAt` and the PokeAPI snapshot (`name`, `types`, measures, sprite) never change. |
| AC-05 | `localizedName` and `region` are optional: `null` (or missing) clears them. When present they are trimmed and must have 1 to 100 characters (not blank). |
| AC-06 | `tags` is required (use `[]` to clear). At most 10 tags; each tag has 1 to 30 letters, digits or hyphens. Tags are stored trimmed, in lower case and without duplicates, keeping the first order. |
| AC-07 | A body with any other field (for example `name` or `weightKg`) returns `400` naming the field (`is not a recognized field`): the PokeAPI snapshot is not editable, and silently ignoring the field would hide a client mistake. |
| AC-08 | A missing or malformed body returns `400`. A non-JSON content type returns `415`. |
| AC-09 | An id that does not exist returns `404`. A non-numeric id returns `400`. |

### Delete — `DELETE /api/v1/local-pokemon/{id}`

| ID | Criterion |
|---|---|
| AC-10 | Deletes the record and returns `204` with no body. After that, the same PokeAPI Pokemon can be synchronized again. |
| AC-11 | An id that does not exist returns `404`. A non-numeric id returns `400`. |

### Security

| ID | Criterion |
|---|---|
| AC-12 | Every route returns `401` without a valid token. |

## Business rules

- Only the proprietary fields are editable; the PokeAPI snapshot is read-only.
- Tags are case-insensitive labels: `Starter` and `starter` are the same tag.

## Error flows

- `400` invalid query, path, body or unknown field; `401` no token; `404` unknown id; `415` wrong content type.

## Dependencies

- US03 (table, model, read one), user authentication.

## Explicit assumptions

- `PUT` replaces the whole editable part (the three proprietary fields), so the request is simple and predictable (TDR-001).
- No optimistic locking: if two users edit the same record at the same time, the last write wins. Acceptable for this exercise; listed as a risk.
- If a record is deleted while another request updates it, the update returns `404` (the adapter maps Hibernate's stale-row error).
- Delete is a hard delete; the record can be recreated by synchronizing again.
- No filtering or search in the list (not asked).
