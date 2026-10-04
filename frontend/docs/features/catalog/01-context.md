# Catalog — Context

## Goal

Browse the PokeAPI catalog (US01) and open the detail of one Pokemon (US02). From the detail, a logged-in user can save the Pokemon to the local database (US03).

## Acceptance criteria

| ID | Criterion | Backend contract |
|---|---|---|
| AC-01 | `/pokemon` shows a responsive grid of cards: sprite, name, number, types, weight in kg and the first moves. | US01 |
| AC-02 | Previous / Next buttons and "Page X of Y"; the page is in the URL (`?page=`). | US01 AC-04 |
| AC-03 | Clicking a card opens `/pokemon/:name`. | — |
| AC-04 | The detail shows the official artwork, types, height, weight, the six base stats as bars, the description and the evolution chain (with branches); each stage links to its own detail. | US02 |
| AC-05 | Logged in: a "Save to My Pokemon" button. On success it opens the edit page of the new local record; if it already exists, a message links to it (`localId` of the `409`). Not logged in: a link to log in instead. | US03 |
| AC-06 | Loading, error (including "PokeAPI is not available") and "not found" states are shown with clear messages. | US01 AC-08, US02 AC-10 |
