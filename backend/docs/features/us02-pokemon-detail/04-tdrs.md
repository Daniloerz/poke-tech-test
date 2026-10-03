# US02 — Technical Decision Records

## TDR-001 — Evolution chain shape

### Context / Problem

PokeAPI returns the evolution chain as a recursive tree (`chain.evolves_to[].evolves_to[]`). Some families have branches (Eevee has 8, Wurmple splits at the second stage). We need a shape for the domain and the API.

### Options considered

#### Option A — Flat list of names
- `["eevee", "vaporeon", "jolteon", ...]`. Very simple for a linear chain.
- Loses the structure: the client cannot know who evolves into whom when there are branches.

#### Option B — List of stages (list of lists)
- `[["eevee"], ["vaporeon", "jolteon", ...]]`. Easy to draw as columns.
- Still loses the parent of each node when a stage has several parents (Wurmple: silcoon → beautifly, cascoon → dustox).

#### Option C — Recursive tree
- Each node has `evolvesTo`, the same as PokeAPI but with only the fields we need.
- Keeps all the information; the client draws it with a simple recursive component.

### Decision

Option C: `EvolutionNode(speciesId, name, imageUrl, evolvesTo)`.

### Rationale

It is the only option that is correct for every family, and it is still simple to map and to render.

### Consequences

- Mapping and JSON (de)serialization are recursive. Chains are at most 3 levels deep, so this is safe.
- `evolvesTo` is never null (empty list at the end of a branch), so clients do not need null checks.

---

## TDR-002 — Images of the evolution chain nodes

### Context / Problem

The evolution chain only has species names and URLs, no images. A detail view usually shows a small image for each stage.

### Options considered

#### Option A — No images; only id and name
- No extra work in the backend.
- The client would have to build image URLs itself (coupled to PokeAPI) or make one request per node.

#### Option B — One PokeAPI call per node to get its sprite
- Real sprite from PokeAPI.
- Up to 9 extra calls for Eevee on a cold cache, plus a mapping from species to default Pokemon.

#### Option C — Build the sprite URL from the species id
- `{spriteBaseUrl}/{speciesId}.png`, the same URL pattern PokeAPI uses for `sprites.front_default` of the default form.
- No extra calls. Depends on the sprites URL pattern, which is kept in one configurable property.

### Decision

Option C.

### Rationale

It gives the client everything it needs with zero extra calls. The species id equals the id of its default Pokemon, so the URL points to the same sprite PokeAPI returns.

### Consequences

- If PokeAPI changes its sprites location, only `pokeapi.sprite-base-url` changes.
- The species id is read from the end of the species URL (`.../pokemon-species/133/`).

---

## TDR-003 — Which description to show

### Context / Problem

`flavor_text_entries` has one entry per game version and language (Eevee has 33 English entries). The text contains `\n` and form feeds (`\f`) from the old game screens.

### Options considered

#### Option A — First English entry
- Stable (oldest game).
- Old texts are short and use old formatting (for example `STONEs`).

#### Option B — Last English entry
- Most recent game text, usually the best written.
- It may change if PokeAPI adds a new game (the cache TTL handles that).

#### Option C — Return all English entries
- Complete data.
- Large response with many near-duplicates; the client must choose.

### Decision

Option B, with every whitespace sequence (`\n`, `\f`, spaces) replaced by a single space and the text trimmed.

### Rationale

One clean, modern description is what the story asks for ("narrative description").

### Consequences

- The normalization lives in the PokeAPI mapper, because the format problem comes from PokeAPI.

---

## TDR-004 — Cache key version

### Context / Problem

US02 adds fields to the cached `Pokemon` (stats, artwork, height, species). Entries written by US01 do not have those fields. Read with the new class, they would give a detail with empty stats until the TTL (24 h) expires.

### Options considered

#### Option A — Flush Redis by hand after each deploy
- No code.
- Easy to forget; the bug would only appear with an old cache.

#### Option B — Cache version in the key prefix
- `poke-tech-test::v2::pokeapi-pokemon::eevee`. A model change bumps the version, so old entries are ignored and expire with their TTL.
- One constant to remember when a cached record changes.

### Decision

Option B, with the version as a constant in `CacheConfig`.

### Rationale

It makes the cache safe across model changes with one line, without manual steps.

### Consequences

- Rule: when a cached class (`Pokemon`, `PokemonCatalogPage`, `PokemonSpecies`, `EvolutionNode`) changes, bump the cache version.
- Old entries stay in Redis until their TTL ends; they are small and never read.
