# Catalog — Implementation Plan

| File | Responsibility |
|---|---|
| `api/pokemonApi.js` | `fetchPokemonPage(page, size)`, `fetchPokemonDetail(idOrName)` |
| `api/localPokemonApi.js` | `syncPokemon(idOrName)` (used by the detail page) |
| `pages/CatalogPage.jsx` | Reads `?page`, loads the page with `useApi`, renders `PokemonCard` list and `Pagination` |
| `pages/PokemonDetailPage.jsx` | Loads the detail, renders image, stats, description, `EvolutionTree` and the save action |
| `components/PokemonCard.jsx` | One card of the grid |
| `components/EvolutionTree.jsx` | Recursive list: a node and its `evolvesTo` children |
| `components/Pagination.jsx` | Previous / Next, used by the catalog and by My Pokemon |
| `components/StatusMessage.jsx` | Loading / error / empty message with the right ARIA role |

Notes:

- Page size is 20 (the backend default).
- Stat bars use the stat value over 255 (the highest possible base stat) as width.
- Images have `alt` texts and `loading="lazy"`.
